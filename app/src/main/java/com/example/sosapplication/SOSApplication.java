package com.example.sosapplication;

import android.app.Application;
import android.util.Log;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class SOSApplication extends Application {
    
    private static final String ONESIGNAL_APP_ID = "0d2df905-4641-48e5-b9df-c684735e89f1";
    private static final String TAG = "SOSApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Enable verbose logging for development
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        
        // Initialize OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        
        // Enable location sharing for geofencing
        OneSignal.getLocation().setShared(true);
        
        Log.d(TAG, "OneSignal initialized with App ID: " + ONESIGNAL_APP_ID);
    }
}
