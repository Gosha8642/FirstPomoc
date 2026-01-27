package com.example.sosapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS = "language_prefs";
    private static final String KEY = "lang";

    public static void setLocale(Context context, String lang) {
        saveLanguage(context, lang);
        updateResources(context, lang);
    }

    public static void applyLocale(Context context) {
        String lang = getLanguage(context);
        updateResources(context, lang);
    }

    private static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY, lang).apply();
    }

    private static String getLanguage(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY, "en");
    }

    private static void updateResources(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        context.getResources().updateConfiguration(
                config,
                context.getResources().getDisplayMetrics()
        );
    }
}
