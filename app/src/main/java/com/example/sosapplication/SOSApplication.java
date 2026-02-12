package com.example.sosapplication;

import android.app.Application;
import android.util.Log;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class SOSApplication extends Application {
    
    private static final String TAG = "SOSApplication";
    
    // OneSignal App ID
    public static final String ONESIGNAL_APP_ID = "0d2df905-4641-48e5-b9df-c684735e89f1";
    
    // Backend API URL - replace with your actual backend URL
    public static final String BACKEND_API_URL = "https://sos-backend.example.com/api";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Initializing OneSignal...");
        
        // Enable verbose logging for debugging (remove in production)
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        
        // Initialize OneSignal with context
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        
        // Request notification permission for Android 13+
        OneSignal.getNotifications().requestPermission(true, null);
        
        Log.d(TAG, "OneSignal initialized successfully");
    }
}
