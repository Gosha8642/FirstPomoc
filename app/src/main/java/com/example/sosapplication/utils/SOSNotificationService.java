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
    private static final String REST_API_KEY = "os_v2_app_buw7sbkgifeoloo7y2chgxuj6ghr5bjaleguyeevcv5hl4lhrwi2wgcv32o7b6uxqutlbsmy55ee3kzhywtmbgh6qhf2nnupdjlu2yy";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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

            Log.d(TAG, "Sending SOS alert to ALL users");
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

                    Log.d(TAG, "Response: " + response.code() + " - " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "SOS notification sent successfully!");
                        mainHandler.post(() -> callback.onSuccess(responseBody));
                    } else {
                        Log.e(TAG, "SOS notification failed: " + response.code() + " " + responseBody);
                        mainHandler.post(() -> callback.onError("Error: " + response.code()));
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

        // Send to ALL subscribed users
        JSONArray segments = new JSONArray();
        segments.put("Subscribed Users");
        payload.put("included_segments", segments);

        // Notification title
        JSONObject headings = new JSONObject();
        headings.put("en", "🚨 SOS ALERT!");
        headings.put("sk", "🚨 SOS VÝSTRAHA!");
        headings.put("uk", "🚨 SOS СИГНАЛ!");
        headings.put("ru", "🚨 SOS СИГНАЛ!");
        payload.put("headings", headings);

        // Notification body with coordinates
        String coordsText = String.format("%.5f, %.5f", location.getLatitude(), location.getLongitude());
        
        JSONObject contents = new JSONObject();
        contents.put("en", "Someone needs help! Location: " + coordsText);
        contents.put("sk", "Niekto potrebuje pomoc! Poloha: " + coordsText);
        contents.put("uk", "Комусь потрібна допомога! Локація: " + coordsText);
        contents.put("ru", "Кому-то нужна помощь! Локация: " + coordsText);
        payload.put("contents", contents);

        // Custom data for app handling
        JSONObject data = new JSONObject();
        data.put("type", "SOS_ALERT");
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        data.put("time", System.currentTimeMillis());
        payload.put("data", data);

        // High priority for emergency
        payload.put("priority", 10);

        return payload;
    }

    public interface SOSCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
