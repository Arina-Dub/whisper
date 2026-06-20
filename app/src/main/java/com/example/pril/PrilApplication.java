package com.example.pril;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class PrilApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        AppPreferences prefs = new AppPreferences(this);
        if (prefs.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
