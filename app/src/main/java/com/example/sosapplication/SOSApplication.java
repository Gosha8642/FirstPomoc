package com.example.sosapplication;

import android.app.Application;
import android.util.Log;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class SOSApplication extends Application {

    private static final String TAG = "SOSApplication";
    public static final String ONESIGNAL_APP_ID = "0d2df905-4641-48e5-b9df-c684735e89f1";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Application starting...");

        try {
            // Enable verbose logging for debugging
            OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

            // Initialize OneSignal
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

            // Request notification permission for Android 13+
            OneSignal.getNotifications().requestPermission(true, null);

            Log.d(TAG, "OneSignal initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OneSignal", e);
        }
    }
}