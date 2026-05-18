package com.example.proiect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.example.proiect.R;
import com.example.proiect.adapters.AlarmAdapter;
import com.example.proiect.api.ApiClient;
import com.example.proiect.models.Alarm;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private TextView greeting;
    private TextView kpiUnresolved, kpiCritical, kpiDetections, kpiPersons;
    private ProgressBar progress;
    private ListView recentList;
    private TextView recentEmpty;

    private PrefsManager prefs;
    private AlarmAdapter recentAdapter;
    private final List<Alarm> recentAlarms = new ArrayList<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile ApiClient activeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(this);

        if (!prefs.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        greeting = findViewById(R.id.dashboard_greeting);
        kpiUnresolved = findViewById(R.id.dashboard_kpi_unresolved);
        kpiCritical = findViewById(R.id.dashboard_kpi_critical);
        kpiDetections = findViewById(R.id.dashboard_kpi_detections);
        kpiPersons = findViewById(R.id.dashboard_kpi_persons);
        progress = findViewById(R.id.dashboard_progress);
        recentList = findViewById(R.id.dashboard_recent_list);
        recentEmpty = findViewById(R.id.dashboard_recent_empty);

        greeting.setText(getString(R.string.dashboard_greeting_fmt, prefs.getUsername()));

        recentAdapter = new AlarmAdapter(this, recentAlarms);
        recentList.setAdapter(recentAdapter);
        recentList.setOnItemClickListener((parent, view, position, id) -> {
            Alarm a = recentAdapter.getItem(position);
            if (a != null) openAlarmDetail(a.getId());
        });

        wireNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.isLoggedIn()) {
            loadDashboardData();
        }
    }

    private void wireNavigation() {
        Button btnAlarms = findViewById(R.id.dashboard_btn_alarms);
        Button btnPersons = findViewById(R.id.dashboard_btn_persons);
        Button btnLive = findViewById(R.id.dashboard_btn_live);
        Button btnMap = findViewById(R.id.dashboard_btn_map);
        Button btnStats = findViewById(R.id.dashboard_btn_stats);
        Button btnSettings = findViewById(R.id.dashboard_btn_settings);

        View.OnClickListener comingSoon = v -> Toast.makeText(this,
                R.string.dashboard_coming_soon, Toast.LENGTH_SHORT).show();

        btnAlarms.setOnClickListener(v ->
                startActivity(new Intent(this, AlarmsListActivity.class)));
        btnPersons.setOnClickListener(v ->
                startActivity(new Intent(this, PersonsListActivity.class)));
        btnLive.setOnClickListener(v ->
                startActivity(new Intent(this, CameraStreamActivity.class)));
        btnMap.setOnClickListener(v ->
                startActivity(new Intent(this, CameraMapActivity.class)));
        btnStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsActivity.class)));
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void openAlarmDetail(int alarmId) {
        Intent intent = new Intent(this, AlarmDetailActivity.class);
        intent.putExtra(AlarmDetailActivity.EXTRA_ALARM_ID, alarmId);
        startActivity(intent);
    }

    private void loadDashboardData() {
        progress.setVisibility(View.VISIBLE);

        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        final int[] alarmsKpi = { -1, -1 };
        final boolean[] alarmsKpiLoaded = { false };

        executor.execute(() -> {
            ApiClient client = new ApiClient(baseUrl, token);
            activeClient = client;
            try {
                ApiClient.ApiResponse alarmsStats = client.get("/api/alarms/stats");
                final int unresolved;
                final int critical;
                if (alarmsStats.isSuccess()) {
                    JSONObject j = alarmsStats.asJson();
                    unresolved = readInt(j, "unresolved", "unresolved_count", "open");
                    critical = readInt(j, "critical_unresolved", "critical", "critical_count");
                    alarmsKpi[0] = unresolved;
                    alarmsKpi[1] = critical;
                    alarmsKpiLoaded[0] = true;
                } else {
                    unresolved = -1;
                    critical = -1;
                }
                Session.postOrAuth(mainHandler, this, alarmsStats.code, () -> {
                    kpiUnresolved.setText(formatCount(unresolved));
                    kpiCritical.setText(formatCount(critical));
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!Session.isAlive(this)) return;
                    kpiUnresolved.setText("—");
                    kpiCritical.setText("—");
                });
            }

            try {
                ApiClient.ApiResponse logsStats = client.get("/api/logs/stats?hours=24");
                final int total;
                if (logsStats.isSuccess()) {
                    JSONObject j = logsStats.asJson();
                    total = readInt(j, "total_recent", "total", "total_count", "count", "detections");
                } else {
                    total = -1;
                }
                Session.postOrAuth(mainHandler, this, logsStats.code,
                        () -> kpiDetections.setText(formatCount(total)));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!Session.isAlive(this)) return;
                    kpiDetections.setText("—");
                });
            }

            try {
                ApiClient.ApiResponse stats = client.get("/api/statistics");
                final int persons;
                final int fallbackUnresolved;
                final int fallbackCritical;
                if (stats.isSuccess()) {
                    JSONObject j = stats.asJson();
                    persons = readInt(j, "total_persons", "registered_persons",
                            "persons", "person_count");
                    fallbackUnresolved = readInt(j, "unresolved_alarms");
                    fallbackCritical = readInt(j, "critical_alarms");
                } else {
                    persons = -1;
                    fallbackUnresolved = -1;
                    fallbackCritical = -1;
                }
                Session.postOrAuth(mainHandler, this, stats.code, () -> {
                    kpiPersons.setText(formatCount(persons));
                    if (!alarmsKpiLoaded[0]) {
                        if (fallbackUnresolved >= 0) {
                            kpiUnresolved.setText(formatCount(fallbackUnresolved));
                        }
                        if (fallbackCritical >= 0) {
                            kpiCritical.setText(formatCount(fallbackCritical));
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!Session.isAlive(this)) return;
                    kpiPersons.setText("—");
                });
            }

            try {
                ApiClient.ApiResponse recent = client.get(
                        "/api/alarms?status=unresolved&limit=5");
                final List<Alarm> parsed = new ArrayList<>();
                if (recent.isSuccess()) {
                    parsed.addAll(parseAlarmsList(recent.body));
                }
                Session.postOrAuth(mainHandler, this, recent.code, () -> {
                    recentAlarms.clear();
                    recentAlarms.addAll(parsed);
                    recentAdapter.setAlarms(recentAlarms);
                    recentEmpty.setVisibility(parsed.isEmpty() ? View.VISIBLE : View.GONE);
                    progress.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!Session.isAlive(this)) return;
                    recentEmpty.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                });
            }
        });
    }

    private static List<Alarm> parseAlarmsList(String body) {
        List<Alarm> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;
        try {
            JSONArray array;
            String trimmed = body.trim();
            if (trimmed.startsWith("[")) {
                array = new JSONArray(trimmed);
            } else {
                JSONObject obj = new JSONObject(trimmed);
                if (obj.has("alarms")) {
                    array = obj.getJSONArray("alarms");
                } else if (obj.has("data")) {
                    array = obj.getJSONArray("data");
                } else if (obj.has("results")) {
                    array = obj.getJSONArray("results");
                } else {
                    return out;
                }
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                out.add(Alarm.fromJson(item));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static int readInt(JSONObject json, String... keys) {
        for (String k : keys) {
            if (json.has(k)) return json.optInt(k, -1);
        }
        return -1;
    }

    private static String formatCount(int value) {
        return value < 0 ? "—" : String.valueOf(value);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ApiClient c = activeClient;
        if (c != null) c.cancel();
        executor.shutdownNow();
    }
}
