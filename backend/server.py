from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime, timezone
import httpx
import math
from motor.motor_asyncio import AsyncIOMotorClient
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Initialize FastAPI app
app = FastAPI(
    title="SOS Alert Notification API",
    description="Backend API for geofenced push notifications",
    version="1.0.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Environment variables
ONESIGNAL_APP_ID = os.environ.get("ONESIGNAL_APP_ID", "0d2df905-4641-48e5-b9df-c684735e89f1")
ONESIGNAL_API_KEY = os.environ.get("ONESIGNAL_API_KEY")
MONGO_URL = os.environ.get("MONGO_URL", "mongodb://localhost:27017")
DB_NAME = os.environ.get("DB_NAME", "sos_alert")

# Global MongoDB client
db_client: Optional[AsyncIOMotorClient] = None
db = None


@app.on_event("startup")
async def startup_event():
    """Initialize database connection on app startup"""
    global db_client, db
    db_client = AsyncIOMotorClient(MONGO_URL)
    db = db_client[DB_NAME]
    
    # Create indexes
    await db.users.create_index("user_id", unique=True)
    await db.users.create_index([("latitude", 1), ("longitude", 1)])
    await db.notifications.create_index("alert_id")
    await db.notifications.create_index("created_at")
    
    print(f"Connected to MongoDB: {DB_NAME}")


@app.on_event("shutdown")
async def shutdown_event():
    """Close database connection on app shutdown"""
    if db_client:
        db_client.close()
        print("Closed MongoDB connection")


# Pydantic models
class LocationData(BaseModel):
    user_id: str
    latitude: float
    longitude: float
    timestamp: Optional[str] = None
    external_id: str
    device_type: str = "android"


class SOSRequest(BaseModel):
    user_id: str
    latitude: float
    longitude: float
    radius_meters: int = 200
    message: str = "SOS Alert! Someone nearby needs help!"
    external_id: str


class CancelRequest(BaseModel):
    user_id: str
    action: str = "cancel"


class NotificationResponse(BaseModel):
    notification_id: str
    recipients_count: int
    status: str


# Haversine distance calculation
def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    Calculate distance between two coordinates using Haversine formula.
    Returns distance in meters.
    """
    R = 6371000  # Earth's radius in meters
    
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    delta_lat = math.radians(lat2 - lat1)
    delta_lon = math.radians(lon2 - lon1)
    
    a = (math.sin(delta_lat / 2) ** 2 +
         math.cos(lat1_rad) * math.cos(lat2_rad) *
         math.sin(delta_lon / 2) ** 2)
    
    c = 2 * math.asin(math.sqrt(a))
    return R * c


@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.post("/api/users/location")
async def update_user_location(location_data: LocationData):
    """Update user's location in database"""
    try:
        result = await db.users.update_one(
            {"user_id": location_data.user_id},
            {
                "$set": {
                    "user_id": location_data.user_id,
                    "external_id": location_data.external_id,
                    "latitude": location_data.latitude,
                    "longitude": location_data.longitude,
                    "device_type": location_data.device_type,
                    "last_location_update": datetime.now(timezone.utc),
                    "is_active": True
                }
            },
            upsert=True
        )
        
        return {
            "status": "success",
            "user_id": location_data.user_id,
            "message": "Location updated successfully"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/users/{user_id}/location")
async def get_user_location(user_id: str):
    """Get user's current location"""
    user = await db.users.find_one(
        {"user_id": user_id},
        {"_id": 0, "latitude": 1, "longitude": 1, "last_location_update": 1}
    )
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return user


@app.get("/api/users/nearby")
async def get_nearby_users(latitude: float, longitude: float, radius_meters: int = 200):
    """Find all users within specified radius"""
    all_users = await db.users.find(
        {"is_active": True},
        {"_id": 0, "user_id": 1, "external_id": 1, "latitude": 1, "longitude": 1}
    ).to_list(length=1000)
    
    nearby_users = []
    
    for user in all_users:
        if user.get("latitude") and user.get("longitude"):
            distance = calculate_distance(
                latitude, longitude,
                user["latitude"], user["longitude"]
            )
            
            if distance <= radius_meters and user.get("external_id"):
                nearby_users.append({
                    "user_id": user["user_id"],
                    "external_id": user["external_id"],
                    "distance_meters": round(distance, 2)
                })
    
    nearby_users.sort(key=lambda x: x["distance_meters"])
    
    return {
        "center_latitude": latitude,
        "center_longitude": longitude,
        "radius_meters": radius_meters,
        "nearby_users_count": len(nearby_users),
        "nearby_users": nearby_users
    }


async def send_onesignal_notification(
    external_ids: List[str],
    title: str,
    message: str,
    data: Optional[dict] = None
) -> dict:
    """Send push notification through OneSignal REST API"""
    
    if not ONESIGNAL_API_KEY:
        return {"status": "error", "message": "OneSignal API key not configured"}
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Key {ONESIGNAL_API_KEY}"
    }
    
    payload = {
        "app_id": ONESIGNAL_APP_ID,
        "target_channel": "push",
        "include_aliases": {
            "external_id": external_ids
        },
        "headings": {"en": title},
        "contents": {"en": message},
        "android_accent_color": "FFFF3B30",
        "android_led_color": "FFFF3B30",
        "priority": 10,
        "ttl": 300
    }
    
    if data:
        payload["data"] = data
    
    # Add action buttons
    payload["buttons"] = [
        {"id": "help_coming", "text": "Иду на помощь"},
        {"id": "false_alarm", "text": "Ложная тревога"}
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            response = await client.post(
                "https://api.onesignal.com/notifications",
                json=payload,
                headers=headers
            )
            
            if response.status_code == 200:
                result = response.json()
                return {
                    "status": "success",
                    "notification_id": result.get("id", "unknown"),
                    "recipients": result.get("recipients", 0)
                }
            else:
                return {
                    "status": "error",
                    "message": response.text,
                    "status_code": response.status_code
                }
        except Exception as e:
            return {
                "status": "error",
                "message": str(e)
            }


@app.post("/api/alerts/sos", response_model=NotificationResponse)
async def trigger_sos_alert(sos_request: SOSRequest):
    """Trigger SOS alert: find nearby users and send notifications"""
    
    # Find nearby users (excluding the sender)
    all_users = await db.users.find(
        {"is_active": True, "user_id": {"$ne": sos_request.user_id}},
        {"_id": 0, "user_id": 1, "external_id": 1, "latitude": 1, "longitude": 1}
    ).to_list(length=1000)
    
    nearby_users = []
    
    for user in all_users:
        if user.get("latitude") and user.get("longitude") and user.get("external_id"):
            distance = calculate_distance(
                sos_request.latitude, sos_request.longitude,
                user["latitude"], user["longitude"]
            )
            
            if distance <= sos_request.radius_meters:
                nearby_users.append({
                    "user_id": user["user_id"],
                    "external_id": user["external_id"],
                    "distance_meters": round(distance, 2)
                })
    
    if not nearby_users:
        # Store alert even if no recipients
        await db.notifications.insert_one({
            "alert_id": "no_recipients",
            "sender_id": sos_request.user_id,
            "alert_type": "sos",
            "latitude": sos_request.latitude,
            "longitude": sos_request.longitude,
            "radius_meters": sos_request.radius_meters,
            "nearby_users_count": 0,
            "notification_status": "no_recipients",
            "created_at": datetime.now(timezone.utc),
            "recipients": []
        })
        
        return NotificationResponse(
            notification_id="no_recipients",
            recipients_count=0,
            status="no_recipients"
        )
    
    # Extract external IDs for notification
    external_ids = [user["external_id"] for user in nearby_users]
    
    # Send notification
    notification_result = await send_onesignal_notification(
        external_ids=external_ids,
        title="🆘 SOS Сигнал!",
        message=sos_request.message,
        data={
            "alert_type": "sos",
            "sender_id": sos_request.user_id,
            "latitude": str(sos_request.latitude),
            "longitude": str(sos_request.longitude),
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
    )
    
    # Store alert record
    alert_record = {
        "alert_id": notification_result.get("notification_id", "unknown"),
        "sender_id": sos_request.user_id,
        "alert_type": "sos",
        "latitude": sos_request.latitude,
        "longitude": sos_request.longitude,
        "radius_meters": sos_request.radius_meters,
        "nearby_users_count": len(nearby_users),
        "notification_status": notification_result.get("status"),
        "created_at": datetime.now(timezone.utc),
        "recipients": external_ids
    }
    
    await db.notifications.insert_one(alert_record)
    
    return NotificationResponse(
        notification_id=notification_result.get("notification_id", "unknown"),
        recipients_count=len(nearby_users),
        status=notification_result.get("status", "unknown")
    )


@app.post("/api/alerts/cancel")
async def cancel_sos_alert(cancel_request: CancelRequest):
    """Cancel an active SOS alert"""
    
    # Find the most recent alert from this user
    latest_alert = await db.notifications.find_one(
        {"sender_id": cancel_request.user_id, "alert_type": "sos"},
        sort=[("created_at", -1)]
    )
    
    if not latest_alert:
        return {"status": "no_active_alert", "message": "No active SOS alert found"}
    
    # Mark as cancelled
    await db.notifications.update_one(
        {"_id": latest_alert["_id"]},
        {"$set": {"cancelled": True, "cancelled_at": datetime.now(timezone.utc)}}
    )
    
    # Optionally send cancellation notification to recipients
    if latest_alert.get("recipients"):
        await send_onesignal_notification(
            external_ids=latest_alert["recipients"],
            title="✅ SOS Отменён",
            message="SOS сигнал был отменён отправителем",
            data={
                "alert_type": "sos_cancelled",
                "original_alert_id": str(latest_alert.get("alert_id", ""))
            }
        )
    
    return {"status": "success", "message": "SOS alert cancelled"}


@app.get("/api/alerts/history/{user_id}")
async def get_alert_history(user_id: str, limit: int = 20):
    """Get alert history for a user"""
    
    alerts = await db.notifications.find(
        {"sender_id": user_id},
        {"_id": 0}
    ).sort("created_at", -1).limit(limit).to_list(length=limit)
    
    return {
        "user_id": user_id,
        "total_alerts": len(alerts),
        "alerts": alerts
    }


@app.get("/api/stats")
async def get_stats():
    """Get system statistics"""
    
    total_users = await db.users.count_documents({})
    active_users = await db.users.count_documents({"is_active": True})
    total_alerts = await db.notifications.count_documents({})
    
    return {
        "total_users": total_users,
        "active_users": active_users,
        "total_alerts": total_alerts,
        "timestamp": datetime.now(timezone.utc).isoformat()
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
