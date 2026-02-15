package com.example.sosapplication.utils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SOSNotificationService {
    
    private static final String TAG = "SOSNotificationService";
    private static final String ONESIGNAL_APP_ID = "0d2df905-4641-48e5-b9df-c684735e89f1";
    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";
    private static final String REST_API_KEY = "os_v2_app_buw7sbkgifeoloo7y2chgxuj6f5tizxmm3bu4vekj5hhn7qglpuj4elri73by2wwyr4cqcncxe3aqerec3bayvtalhareydiulowpla";
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int RADIUS_METERS = 200;
    
    // Set to true to send to ALL users (for testing)
    // Set to false to use location filter (production)
    private static final boolean SEND_TO_ALL_USERS = true;
    
    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;
    
    public SOSNotificationService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void sendSOSAlert(Location location, SOSCallback callback) {
        if (location == null) {
            Log.e(TAG, "Location is null, cannot send SOS");
            callback.onError("Location not available");
            return;
        }
        
        try {
            JSONObject payload = buildNotificationPayload(location);
            
            Log.d(TAG, "Sending SOS alert...");
            Log.d(TAG, "Payload: " + payload.toString());
            
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(ONESIGNAL_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Basic " + REST_API_KEY)
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to send SOS notification", e);
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        Log.d(TAG, "SOS notification sent successfully");
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        Log.e(TAG, "SOS notification failed: " + response.code() + " - " + responseBody);
                        mainHandler.post(() -> callback.onError("Server error: " + response.code() + " - " + responseBody));
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error building SOS notification", e);
            callback.onError(e.getMessage());
        }
    }
    
    private JSONObject buildNotificationPayload(Location location) throws Exception {
        JSONObject payload = new JSONObject();
        
        payload.put("app_id", ONESIGNAL_APP_ID);
        
        if (SEND_TO_ALL_USERS) {
            // Send to ALL subscribed users (for testing)
            payload.put("included_segments", new JSONArray().put("Subscribed Users"));
        } else {
            // Use location filter (production) - requires users to have location shared
            JSONArray filters = new JSONArray();
            JSONObject locationFilter = new JSONObject();
            locationFilter.put("field", "location");
            locationFilter.put("radius", RADIUS_METERS);
            locationFilter.put("lat", location.getLatitude());
            locationFilter.put("long", location.getLongitude());
            filters.put(locationFilter);
            payload.put("filters", filters);
        }
        
        // Notification content
        JSONObject headings = new JSONObject();
        headings.put("en", "🚨 SOS Alert!");
        headings.put("sk", "🚨 SOS Výstraha!");
        headings.put("uk", "🚨 SOS Сигнал!");
        headings.put("ru", "🚨 SOS Сигнал!");
        payload.put("headings", headings);
        
        JSONObject contents = new JSONObject();
        contents.put("en", "Someone needs help! Location: " + 
                String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
        contents.put("sk", "Niekto potrebuje pomoc! Poloha: " + 
                String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
        contents.put("uk", "Комусь потрібна допомога! Локація: " + 
                String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
        contents.put("ru", "Кому-то нужна помощь! Локация: " + 
                String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
        payload.put("contents", contents);
        
        // Custom data
        JSONObject data = new JSONObject();
        data.put("type", "SOS_ALERT");
        data.put("sender_lat", location.getLatitude());
        data.put("sender_long", location.getLongitude());
        data.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);
        
        // High priority
        payload.put("priority", 10);
        payload.put("ttl", 3600);
        
        // Android specific - sound and vibration
        payload.put("android_sound", "default");
        payload.put("android_led_color", "FFFF0000");
        payload.put("android_accent_color", "FFFF0000");
        
        return payload;
    }
    
    public interface SOSCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
