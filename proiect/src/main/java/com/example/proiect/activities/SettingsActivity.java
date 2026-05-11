package com.example.proiect.activities;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.proiect.R;
import com.example.proiect.api.ApiClient;
import com.example.proiect.database.DatabaseHelper;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private static final int[] WINDOW_HOURS = { 24, 48, 168, 720 };

    private Switch notifEnabled, darkMode;
    private CheckBox notifFace, notifFire, notifWeapon, notifZone, notifHar;
    private Spinner windowSpinner;
    private TextView cacheInfo, usernameView, roleView, versionView;
    private Button clearCacheBtn, passwordBtn, logoutBtn;

    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) { finish(); return; }

        setContentView(R.layout.activity_settings);

        notifEnabled = findViewById(R.id.settings_notif_enabled);
        notifFace = findViewById(R.id.settings_notif_face);
        notifFire = findViewById(R.id.settings_notif_fire);
        notifWeapon = findViewById(R.id.settings_notif_weapon);
        notifZone = findViewById(R.id.settings_notif_zone);
        notifHar = findViewById(R.id.settings_notif_har);
        darkMode = findViewById(R.id.settings_dark_mode);
        windowSpinner = findViewById(R.id.settings_default_window);
        cacheInfo = findViewById(R.id.settings_cache_info);
        usernameView = findViewById(R.id.settings_username);
        roleView = findViewById(R.id.settings_role);
        versionView = findViewById(R.id.settings_version);
        clearCacheBtn = findViewById(R.id.settings_btn_clear_cache);
        passwordBtn = findViewById(R.id.settings_btn_password);
        logoutBtn = findViewById(R.id.settings_btn_logout);

        bindNotifications();
        bindDisplay();
        bindData();
        bindAccount();
        bindAbout();
    }

    private void bindNotifications() {
        notifEnabled.setChecked(prefs.getBool(PrefsManager.KEY_NOTIF_ENABLED, true));
        notifFace.setChecked(prefs.getBool(PrefsManager.KEY_NOTIF_FACE, true));
        notifFire.setChecked(prefs.getBool(PrefsManager.KEY_NOTIF_FIRE, true));
        notifWeapon.setChecked(prefs.getBool(PrefsManager.KEY_NOTIF_WEAPON, true));
        notifZone.setChecked(prefs.getBool(PrefsManager.KEY_NOTIF_ZONE, true));
        notifHar.setChecked(prefs.getBool(PrefsManager.KEY_NOTIF_HAR, true));

        applyMasterToggleState();

        notifEnabled.setOnCheckedChangeListener((v, checked) -> {
            prefs.setBool(PrefsManager.KEY_NOTIF_ENABLED, checked);
            applyMasterToggleState();
        });
        attachToggle(notifFace, PrefsManager.KEY_NOTIF_FACE);
        attachToggle(notifFire, PrefsManager.KEY_NOTIF_FIRE);
        attachToggle(notifWeapon, PrefsManager.KEY_NOTIF_WEAPON);
        attachToggle(notifZone, PrefsManager.KEY_NOTIF_ZONE);
        attachToggle(notifHar, PrefsManager.KEY_NOTIF_HAR);
    }

    private void applyMasterToggleState() {
        boolean enabled = notifEnabled.isChecked();
        notifFace.setEnabled(enabled);
        notifFire.setEnabled(enabled);
        notifWeapon.setEnabled(enabled);
        notifZone.setEnabled(enabled);
        notifHar.setEnabled(enabled);
    }

    private void attachToggle(CompoundButton cb, String key) {
        cb.setOnCheckedChangeListener((v, checked) -> prefs.setBool(key, checked));
    }

    private void bindDisplay() {
        darkMode.setChecked(prefs.getBool(PrefsManager.KEY_DARK_MODE, false));
        darkMode.setOnCheckedChangeListener((v, checked) -> {
            prefs.setBool(PrefsManager.KEY_DARK_MODE, checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(this,
                R.array.stats_window_labels, android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        windowSpinner.setAdapter(a);

        int currentHours = prefs.getDefaultTimeWindow();
        int idx = 0;
        for (int i = 0; i < WINDOW_HOURS.length; i++) {
            if (WINDOW_HOURS[i] == currentHours) { idx = i; break; }
        }
        windowSpinner.setSelection(idx);
        windowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setDefaultTimeWindow(WINDOW_HOURS[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
    }

    private void bindData() {
        updateCacheInfo();
        clearCacheBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_clear_cache)
                    .setMessage(R.string.settings_clear_cache_confirm)
                    .setPositiveButton(R.string.settings_clear, (d, w) -> {
                        prefs.clearCachedData();
                        DatabaseHelper.get(this).clearAll();
                        updateCacheInfo();
                        Toast.makeText(this, R.string.settings_cache_cleared,
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.settings_cancel, null)
                    .show();
        });
    }

    private void updateCacheInfo() {
        int entries = getSharedPreferences(PrefsManager.PREFS_NAME, MODE_PRIVATE)
                .getAll().size();
        cacheInfo.setText(getString(R.string.settings_cache_info_fmt, entries));
    }

    private void bindAccount() {
        String username = prefs.getUsername();
        String role = prefs.getUserRole();
        usernameView.setText(username == null || username.isEmpty() ? "—" : username);
        roleView.setText(getString(R.string.settings_role_fmt,
                role == null || role.isEmpty() ? "user" : role));

        passwordBtn.setOnClickListener(v -> showChangePasswordDialog());
        logoutBtn.setOnClickListener(v -> Session.logout(this));
    }

    private void bindAbout() {
        String name = "1.0";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            name = info.versionName;
        } catch (Exception ignored) { }
        versionView.setText(getString(R.string.settings_version_fmt, name));
    }

    private void showChangePasswordDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.settings_new_password_hint);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_change_password)
                .setView(input)
                .setPositiveButton(R.string.settings_save, (d, w) -> {
                    String pwd = input.getText().toString();
                    if (pwd.length() < 4) {
                        Toast.makeText(this, R.string.settings_password_too_short,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitPassword(pwd);
                })
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    private void submitPassword(String newPassword) {
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();
        passwordBtn.setEnabled(false);

        executor.execute(() -> {
            int code = 0;
            String error = null;
            try {
                ApiClient client = new ApiClient(baseUrl, token);
                JSONObject body = new JSONObject();
                body.put("new_password", newPassword);
                ApiClient.ApiResponse r = client.put("/api/users/me/password", body);
                code = r.code;
                if (!r.isSuccess()) {
                    try {
                        JSONObject err = r.asJson();
                        error = err.optString("detail",
                                err.optString("message", "HTTP " + r.code));
                    } catch (Exception ex) {
                        error = "HTTP " + r.code;
                    }
                }
            } catch (Exception e) {
                error = e.getMessage();
            }

            final int finalCode = code;
            final String finalError = error;
            Session.postOrAuth(mainHandler, this, finalCode, () -> {
                passwordBtn.setEnabled(true);
                if (finalError == null) {
                    Toast.makeText(this, R.string.settings_password_changed,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            getString(R.string.settings_password_failed_fmt, finalError),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
