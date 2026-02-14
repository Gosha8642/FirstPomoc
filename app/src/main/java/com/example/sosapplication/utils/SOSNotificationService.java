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
            
            Log.d(TAG, "Sending SOS alert to users within " + RADIUS_METERS + "m");
            
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
                    
                    if (response.isSuccessful()) {
                        Log.d(TAG, "SOS notification sent successfully");
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        Log.e(TAG, "SOS notification failed: " + response.code());
                        mainHandler.post(() -> callback.onError("Server error: " + response.code()));
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
        
        JSONArray filters = new JSONArray();
        JSONObject locationFilter = new JSONObject();
        locationFilter.put("field", "location");
        locationFilter.put("radius", RADIUS_METERS);
        locationFilter.put("lat", location.getLatitude());
        locationFilter.put("long", location.getLongitude());
        filters.put(locationFilter);
        payload.put("filters", filters);
        
        JSONObject headings = new JSONObject();
        headings.put("en", "🚨 SOS Alert Nearby!");
        headings.put("sk", "🚨 SOS Výstraha v blízkosti!");
        headings.put("uk", "🚨 SOS Сигнал поруч!");
        payload.put("headings", headings);
        
        JSONObject contents = new JSONObject();
        contents.put("en", "Someone nearby needs help! Tap to see location.");
        contents.put("sk", "Niekto v blízkosti potrebuje pomoc!");
        contents.put("uk", "Комусь поруч потрібна допомога!");
        payload.put("contents", contents);
        
        JSONObject data = new JSONObject();
        data.put("type", "SOS_ALERT");
        data.put("sender_lat", location.getLatitude());
        data.put("sender_long", location.getLongitude());
        data.put("timestamp", System.currentTimeMillis());
        payload.put("data", data);
        
        payload.put("priority", 10);
        
        return payload;
    }
    
    public interface SOSCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
