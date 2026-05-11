package com.example.proiect.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {

    public static final String PREFS_NAME = "securityguard_prefs";

    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_SERVER_URL = "server_url";

    public static final String KEY_FILTER_STATUS = "filter_status";
    public static final String KEY_FILTER_TYPE = "filter_type";
    public static final String KEY_FILTER_SEVERITY = "filter_severity";

    public static final String FILTER_ALL = "all";

    public static final String KEY_NOTIF_ENABLED = "notif_enabled";
    public static final String KEY_NOTIF_FACE = "notif_face";
    public static final String KEY_NOTIF_FIRE = "notif_fire";
    public static final String KEY_NOTIF_WEAPON = "notif_weapon";
    public static final String KEY_NOTIF_ZONE = "notif_zone";
    public static final String KEY_NOTIF_HAR = "notif_har";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_DEFAULT_TIME_WINDOW = "default_time_window";

    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:8000";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.isEmpty();
    }

    public void saveLogin(String token, String username, String role) {
        prefs.edit()
                .putString(KEY_AUTH_TOKEN, token)
                .putString(KEY_USERNAME, username)
                .putString(KEY_USER_ROLE, role)
                .apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "");
    }

    public String getFilterStatus() {
        return prefs.getString(KEY_FILTER_STATUS, FILTER_ALL);
    }

    public String getFilterType() {
        return prefs.getString(KEY_FILTER_TYPE, FILTER_ALL);
    }

    public String getFilterSeverity() {
        return prefs.getString(KEY_FILTER_SEVERITY, FILTER_ALL);
    }

    public void saveAlarmFilters(String status, String type, String severity) {
        prefs.edit()
                .putString(KEY_FILTER_STATUS, status)
                .putString(KEY_FILTER_TYPE, type)
                .putString(KEY_FILTER_SEVERITY, severity)
                .apply();
    }

    public boolean isPersonFavorite(int personId) {
        return prefs.getBoolean("person_fav_" + personId, false);
    }

    public void setPersonFavorite(int personId, boolean favorite) {
        prefs.edit().putBoolean("person_fav_" + personId, favorite).apply();
    }

    public boolean getBool(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    public void setBool(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public int getDefaultTimeWindow() {
        return prefs.getInt(KEY_DEFAULT_TIME_WINDOW, 24);
    }

    public void setDefaultTimeWindow(int hours) {
        prefs.edit().putInt(KEY_DEFAULT_TIME_WINDOW, hours).apply();
    }

    public void clearCachedData() {
        SharedPreferences.Editor e = prefs.edit();
        for (String key : new java.util.ArrayList<>(prefs.getAll().keySet())) {
            if (key.startsWith("alarm_rating_") || key.startsWith("person_fav_")) {
                e.remove(key);
            }
        }
        e.remove(KEY_FILTER_STATUS);
        e.remove(KEY_FILTER_TYPE);
        e.remove(KEY_FILTER_SEVERITY);
        e.apply();
    }

    public int getAlarmRating(int alarmId) {
        return prefs.getInt("alarm_rating_" + alarmId, 0);
    }

    public void setAlarmRating(int alarmId, int rating) {
        prefs.edit().putInt("alarm_rating_" + alarmId, rating).apply();
    }

    public void clearLogin() {
        prefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_USERNAME)
                .remove(KEY_USER_ROLE)
                .apply();
    }
}
