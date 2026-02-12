package com.example.sosapplication.services;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.onesignal.OneSignal;

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

/**
 * Service for managing SOS alerts and location updates
 */
public class SOSAlertService {
    
    private static final String TAG = "SOSAlertService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final Context context;
    private final OkHttpClient client;
    private final String backendUrl;
    private final Handler mainHandler;
    
    private String userId;
    private String externalId;
    
    public interface SOSCallback {
        void onSuccess(int recipientsCount);
        void onError(String error);
    }
    
    public interface LocationUpdateCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public SOSAlertService(Context context, String backendUrl) {
        this.context = context;
        this.backendUrl = backendUrl;
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Generate unique user ID if not set
        this.userId = getOrCreateUserId();
        this.externalId = this.userId;
        
        // Login to OneSignal with external ID
        OneSignal.login(this.externalId);
    }
    
    /**
     * Get or create a unique user ID stored in SharedPreferences
     */
    private String getOrCreateUserId() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("sos_prefs", Context.MODE_PRIVATE);
        String storedId = prefs.getString("user_id", null);
        
        if (storedId == null) {
            storedId = "user_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            prefs.edit().putString("user_id", storedId).apply();
        }
        
        return storedId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getExternalId() {
        return externalId;
    }
    
    /**
     * Update user location on the backend server
     */
    public void updateLocation(Location location, LocationUpdateCallback callback) {
        if (location == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Location is null"));
            }
            return;
        }
        
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("external_id", externalId);
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            json.put("timestamp", java.time.Instant.now().toString());
            json.put("device_type", "android");
            
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(backendUrl + "/users/location")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to update location", e);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(e.getMessage()));
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Location updated successfully");
                        if (callback != null) {
                            mainHandler.post(() -> callback.onSuccess());
                        }
                    } else {
                        String error = "Server error: " + response.code();
                        Log.e(TAG, error);
                        if (callback != null) {
                            mainHandler.post(() -> callback.onError(error));
                        }
                    }
                    response.close();
                }
            });
            
            // Also update OneSignal tags with location
            OneSignal.getUser().addTag("latitude", String.valueOf(location.getLatitude()));
            OneSignal.getUser().addTag("longitude", String.valueOf(location.getLongitude()));
            OneSignal.getUser().addTag("last_update", String.valueOf(System.currentTimeMillis()));
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating location request", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }
    }
    
    /**
     * Trigger SOS alert to nearby users within specified radius
     */
    public void triggerSOSAlert(Location location, int radiusMeters, String message, SOSCallback callback) {
        if (location == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Location is null"));
            }
            return;
        }
        
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("external_id", externalId);
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            json.put("radius_meters", radiusMeters);
            json.put("message", message != null ? message : "SOS Alert! Someone nearby needs help!");
            
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(backendUrl + "/alerts/sos")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to send SOS alert", e);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(e.getMessage()));
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            JSONObject result = new JSONObject(responseBody);
                            int recipients = result.optInt("recipients_count", 0);
                            
                            Log.d(TAG, "SOS alert sent to " + recipients + " users");
                            if (callback != null) {
                                mainHandler.post(() -> callback.onSuccess(recipients));
                            }
                        } else {
                            String error = "Server error: " + response.code();
                            Log.e(TAG, error);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onError(error));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        if (callback != null) {
                            mainHandler.post(() -> callback.onError(e.getMessage()));
                        }
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating SOS request", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }
    }
    
    /**
     * Cancel an active SOS alert
     */
    public void cancelSOSAlert(SOSCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("action", "cancel");
            
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(backendUrl + "/alerts/cancel")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to cancel SOS alert", e);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(e.getMessage()));
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "SOS alert cancelled");
                        if (callback != null) {
                            mainHandler.post(() -> callback.onSuccess(0));
                        }
                    } else {
                        String error = "Server error: " + response.code();
                        if (callback != null) {
                            mainHandler.post(() -> callback.onError(error));
                        }
                    }
                    response.close();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating cancel request", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }
    }
}
