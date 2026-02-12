package com.example.sosapplication;

import android.app.Application;
import android.util.Log;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.example.sosapplication.services.NotificationClickHandler;

public class SOSApplication extends Application {
    
    private static final String TAG = "SOSApplication";
    
    // OneSignal App ID
    public static final String ONESIGNAL_APP_ID = "0d2df905-4641-48e5-b9df-c684735e89f1";
    
    // Backend API URL - Update this with your deployed backend URL
    // For testing: use your Emergent preview URL
    public static final String BACKEND_API_URL = "https://018eb327-ad33-4da9-8f84-e3b8ca930097.preview.emergentagent.com/api";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Initializing OneSignal...");
        
        // Enable verbose logging for debugging (remove in production)
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        
        // Initialize OneSignal with context
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        
        // Add notification click listener
        OneSignal.getNotifications().addClickListener(new NotificationClickHandler(this));
        
        // Request notification permission for Android 13+
        OneSignal.getNotifications().requestPermission(true, null);
        
        Log.d(TAG, "OneSignal initialized successfully");
    }
}
