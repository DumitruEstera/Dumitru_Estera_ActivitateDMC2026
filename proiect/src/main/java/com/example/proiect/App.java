package com.example.proiect;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.proiect.utils.PrefsManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = new PrefsManager(this).getBool(PrefsManager.KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
