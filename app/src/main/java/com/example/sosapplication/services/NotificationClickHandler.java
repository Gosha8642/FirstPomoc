package com.example.sosapplication.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;

import org.json.JSONObject;

/**
 * Handler for OneSignal notification clicks
 */
public class NotificationClickHandler implements INotificationClickListener {
    
    private static final String TAG = "NotificationClick";
    private final Context context;
    
    public NotificationClickHandler(Context context) {
        this.context = context;
    }
    
    @Override
    public void onClick(INotificationClickEvent event) {
        Log.d(TAG, "Notification clicked");
        
        try {
            // Get notification data
            JSONObject data = event.getNotification().getAdditionalData();
            String actionId = event.getResult().getActionId();
            
            if (data != null) {
                String alertType = data.optString("alert_type", "");
                String senderId = data.optString("sender_id", "");
                String latitude = data.optString("latitude", "");
                String longitude = data.optString("longitude", "");
                
                Log.d(TAG, "Alert type: " + alertType);
                Log.d(TAG, "Sender: " + senderId);
                Log.d(TAG, "Action: " + actionId);
                
                if ("sos".equals(alertType)) {
                    // Handle SOS alert click
                    handleSOSAlertClick(senderId, latitude, longitude, actionId);
                } else if ("sos_cancelled".equals(alertType)) {
                    // Handle SOS cancelled notification
                    Log.d(TAG, "SOS alert was cancelled");
                }
            }
            
            // Handle action button clicks
            if ("help_coming".equals(actionId)) {
                Log.d(TAG, "User responded: Help is coming");
                // TODO: Send response to backend
            } else if ("false_alarm".equals(actionId)) {
                Log.d(TAG, "User responded: False alarm");
                // TODO: Send response to backend
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling notification click", e);
        }
    }
    
    private void handleSOSAlertClick(String senderId, String latitude, String longitude, String actionId) {
        Log.d(TAG, "Handling SOS alert from " + senderId + " at " + latitude + ", " + longitude);
        
        // Launch the main activity with SOS location data
        Intent intent = new Intent(context, com.example.sosapplication.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("sos_alert", true);
        intent.putExtra("sos_sender", senderId);
        intent.putExtra("sos_latitude", latitude);
        intent.putExtra("sos_longitude", longitude);
        
        context.startActivity(intent);
    }
}
