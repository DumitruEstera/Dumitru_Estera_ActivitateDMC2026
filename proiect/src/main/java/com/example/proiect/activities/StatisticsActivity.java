package com.example.proiect.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.example.proiect.R;
import com.example.proiect.api.ApiClient;
import com.example.proiect.charts.BarChartView;
import com.example.proiect.charts.LineChartView;
import com.example.proiect.charts.PieChartView;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsActivity extends AppCompatActivity {

    private static final int[] HOUR_VALUES = { 24, 48, 168, 720 };

    private Spinner windowSpinner;
    private CalendarView calendar;
    private Button clearDateButton;
    private TextView kpiDetections, kpiAlarms, kpiTopCamera, kpiTopType;
    private LineChartView lineChart;
    private BarChartView barChart;
    private PieChartView pieChart;
    private ProgressBar progress;

    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile ApiClient activeClient;

    private int selectedHours = 24;
    @Nullable private String selectedDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) { finish(); return; }

        setContentView(R.layout.activity_statistics);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        windowSpinner = findViewById(R.id.stats_window_spinner);
        calendar = findViewById(R.id.stats_calendar);
        clearDateButton = findViewById(R.id.stats_btn_clear_date);
        kpiDetections = findViewById(R.id.stats_kpi_detections);
        kpiAlarms = findViewById(R.id.stats_kpi_alarms);
        kpiTopCamera = findViewById(R.id.stats_kpi_top_camera);
        kpiTopType = findViewById(R.id.stats_kpi_top_type);
        lineChart = findViewById(R.id.stats_line_chart);
        barChart = findViewById(R.id.stats_bar_chart);
        pieChart = findViewById(R.id.stats_pie_chart);
        progress = findViewById(R.id.stats_progress);

        ArrayAdapter<CharSequence> windowAdapter = ArrayAdapter.createFromResource(this,
                R.array.stats_window_labels, android.R.layout.simple_spinner_item);
        windowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        windowSpinner.setAdapter(windowAdapter);
        windowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                selectedHours = HOUR_VALUES[position];
                selectedDate = null;
                clearDateButton.setVisibility(View.GONE);
                loadStats();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        calendar.setOnDateChangeListener((view, year, month, day) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            clearDateButton.setVisibility(View.VISIBLE);
            loadStats();
        });

        clearDateButton.setOnClickListener(v -> {
            selectedDate = null;
            clearDateButton.setVisibility(View.GONE);
            loadStats();
        });

        loadStats();
    }

    private void loadStats() {
        progress.setVisibility(View.VISIBLE);
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();
        final int hours = selectedHours;
        final String date = selectedDate;

        executor.execute(() -> {
            ApiClient client = new ApiClient(baseUrl, token);
            activeClient = client;
            int authCode = 200;

            long totalDetections = -1;
            String topCamera = null;
            String topType = null;
            int totalAlarms = -1;
            List<LineChartView.Point> linePoints = new ArrayList<>();
            List<BarChartView.Bar> barEntries = new ArrayList<>();
            List<PieChartView.Slice> pieSlices = new ArrayList<>();

            int effectiveHours = (date == null) ? hours : hoursSinceStartOfDay(date);
            String hoursParam = "hours=" + effectiveHours;

            try {
                ApiClient.ApiResponse r = client.get("/api/logs/stats?" + hoursParam);
                if (r.isSuccess()) {
                    JSONObject j = r.asJson();
                    totalDetections = j.optLong("total_recent",
                            j.optLong("total",
                                    j.optLong("total_count", j.optLong("count", -1))));
                } else if (r.code == 401) authCode = 401;
            } catch (Exception ignored) { }

            try {
                ApiClient.ApiResponse r = client.get("/api/alarms/stats");
                if (r.isSuccess()) {
                    JSONObject j = r.asJson();
                    if (j.has("total") || j.has("total_count") || j.has("count")) {
                        totalAlarms = j.optInt("total",
                                j.optInt("total_count", j.optInt("count", -1)));
                    } else {
                        int unresolved = j.optInt("unresolved", 0);
                        int resolved = j.optInt("resolved", 0);
                        int falseAlarm = j.optInt("false_alarm", 0);
                        totalAlarms = unresolved + resolved + falseAlarm;
                    }
                } else if (r.code == 401) {
                    authCode = 401;
                } else {
                    try {
                        ApiClient.ApiResponse s = client.get("/api/statistics");
                        if (s.isSuccess()) {
                            totalAlarms = s.asJson().optInt("total_alarms", -1);
                        }
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }

            try {
                ApiClient.ApiResponse r = client.get("/api/logs/timeseries?" + hoursParam);
                if (r.isSuccess()) linePoints = parseTimeseries(r.body);
                else if (r.code == 401) authCode = 401;
            } catch (Exception ignored) { }

            try {
                ApiClient.ApiResponse r = client.get("/api/logs/breakdown?" + hoursParam);
                if (r.isSuccess()) {
                    JSONObject j = r.asJson();
                    barEntries = parseByCamera(j);
                    pieSlices = parseByType(j);
                    if (!barEntries.isEmpty()) topCamera = barEntries.get(0).label;
                    if (!pieSlices.isEmpty()) topType = pieSlices.get(0).label;
                } else if (r.code == 401) authCode = 401;
            } catch (Exception ignored) { }

            final long fDetections = totalDetections;
            final int fAlarms = totalAlarms;
            final String fTopCamera = topCamera;
            final String fTopType = topType;
            final List<LineChartView.Point> fLine = linePoints;
            final List<BarChartView.Bar> fBar = barEntries;
            final List<PieChartView.Slice> fPie = pieSlices;
            final int finalAuth = authCode;

            Session.postOrAuth(mainHandler, this, finalAuth, () -> {
                kpiDetections.setText(fDetections < 0 ? "—" : String.valueOf(fDetections));
                kpiAlarms.setText(fAlarms < 0 ? "—" : String.valueOf(fAlarms));
                kpiTopCamera.setText(isBlank(fTopCamera) ? "—" : fTopCamera);
                kpiTopType.setText(isBlank(fTopType) ? "—" : fTopType);
                lineChart.setData(fLine);
                barChart.setData(fBar);
                pieChart.setData(fPie);
                progress.setVisibility(View.GONE);
            });
        });
    }

    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }

    private static int hoursSinceStartOfDay(String yyyyMmDd) {
        try {
            String[] parts = yyyyMmDd.split("-");
            if (parts.length != 3) return 24;
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);
            Calendar c = Calendar.getInstance();
            c.set(year, month, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            long startMs = c.getTimeInMillis();
            long nowMs = System.currentTimeMillis();
            long endOfDayMs = startMs + 24L * 3_600_000L;
            long windowMs = Math.min(nowMs, endOfDayMs) - startMs;
            if (windowMs <= 0) return 24;
            return Math.max(1, Math.min(24, (int) Math.ceil(windowMs / 3_600_000.0)));
        } catch (Exception e) {
            return 24;
        }
    }

private static List<LineChartView.Point> parseTimeseries(String body) {
        List<LineChartView.Point> out = new ArrayList<>();
        try {
            JSONArray arr = arrayFrom(body, "series", "data", "buckets");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String label = o.optString("ts",
                        o.optString("label",
                                o.optString("timestamp",
                                        o.optString("bucket", ""))));
                double v = o.optDouble("count",
                        o.optDouble("value", o.optDouble("total", 0)));
                out.add(new LineChartView.Point(shortenTimeLabel(label), v));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static List<BarChartView.Bar> parseByCamera(JSONObject j) {
        List<BarChartView.Bar> out = new ArrayList<>();
        JSONArray arr = j.optJSONArray("by_camera");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String label = o.optString("camera_id",
                    o.optString("camera", o.optString("name", "?")));
            double v = o.optDouble("count",
                    o.optDouble("value", o.optDouble("total", 0)));
            out.add(new BarChartView.Bar(label, v));
        }
        return out;
    }

    private static final Map<String, Integer> TYPE_COLORS = new LinkedHashMap<String, Integer>() {{
        put("face",              Color.parseColor("#1976D2"));
        put("fire",              Color.parseColor("#F57C00"));
        put("weapon",            Color.parseColor("#7B1FA2"));
        put("har",               Color.parseColor("#0097A7"));
        put("unauthorized_zone", Color.parseColor("#D32F2F"));
    }};
    private static final int[] FALLBACK_PALETTE = {
            Color.parseColor("#388E3C"), Color.parseColor("#455A64"),
            Color.parseColor("#5D4037"), Color.parseColor("#FBC02D")
    };

    private static List<PieChartView.Slice> parseByType(JSONObject j) {
        List<PieChartView.Slice> out = new ArrayList<>();
        JSONArray arr = j.optJSONArray("by_type");
        if (arr == null) return out;
        int fbIdx = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String label = o.optString("type",
                    o.optString("name", "?"));
            double v = o.optDouble("count",
                    o.optDouble("value", o.optDouble("total", 0)));
            Integer color = TYPE_COLORS.get(label.toLowerCase(Locale.US));
            if (color == null) color = FALLBACK_PALETTE[fbIdx++ % FALLBACK_PALETTE.length];
            out.add(new PieChartView.Slice(label, v, color));
        }
        return out;
    }

    private static JSONArray arrayFrom(String body, String... keys) throws Exception {
        if (body == null || body.isEmpty()) return null;
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) return new JSONArray(trimmed);
        JSONObject obj = new JSONObject(trimmed);
        for (String k : keys) {
            if (obj.has(k) && obj.optJSONArray(k) != null) return obj.getJSONArray(k);
        }
        return null;
    }

    private static String shortenTimeLabel(String s) {
        if (s == null) return "";
        if (s.length() >= 16 && s.charAt(10) == 'T') {
            return s.substring(11, 16);
        }
        if (s.length() >= 10 && s.charAt(4) == '-') {
            return s.substring(5, 10);
        }
        return s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ApiClient c = activeClient;
        if (c != null) c.cancel();
        executor.shutdownNow();
    }
}
