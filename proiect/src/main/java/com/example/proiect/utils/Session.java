package com.example.proiect.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.example.proiect.R;
import com.example.proiect.activities.LoginActivity;

public class Session {

    public static boolean handleUnauthorized(Activity activity, int httpCode) {
        if (httpCode != 401) return false;

        new PrefsManager(activity).clearLogin();
        Toast.makeText(activity, R.string.session_expired, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }

    public static void postOrAuth(Handler handler, Activity activity,
                                  int httpCode, Runnable onAuthorized) {
        handler.post(() -> {
            if (activity.isFinishing()) return;
            if (handleUnauthorized(activity, httpCode)) return;
            onAuthorized.run();
        });
    }

    public static void logout(Activity activity) {
        new PrefsManager(activity).clearLogin();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
