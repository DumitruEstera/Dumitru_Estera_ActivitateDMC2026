package com.example.proiect.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import com.example.proiect.R;
import com.example.proiect.adapters.AlarmAdapter;
import com.example.proiect.api.ApiClient;
import com.example.proiect.database.DatabaseHelper;
import com.example.proiect.models.Alarm;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmsListActivity extends AppCompatActivity {

    private static final List<String> STATUS_VALUES = Arrays.asList(
            "all", "unresolved", "resolved", "false_alarm");
    private static final List<String> TYPE_VALUES = Arrays.asList(
            "all", "face", "fire", "weapon", "har", "unauthorized_zone");
    private static final List<String> SEVERITY_VALUES = Arrays.asList(
            "all", "critical", "high", "medium", "low");

    private Spinner statusSpinner, typeSpinner, severitySpinner;
    private Button dateButton, timeButton, clearDateButton;
    private TextView dateLabel;
    private ListView listView;
    private TextView emptyView;
    private ProgressBar progress;
    private SwipeRefreshLayout swipe;

    private AlarmAdapter adapter;
    private final List<Alarm> alarms = new ArrayList<>();

    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String selectedDate;
    private int selectedHour = -1;
    private boolean spinnersInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_alarms_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        statusSpinner = findViewById(R.id.alarms_filter_status);
        typeSpinner = findViewById(R.id.alarms_filter_type);
        severitySpinner = findViewById(R.id.alarms_filter_severity);
        dateButton = findViewById(R.id.alarms_btn_date);
        timeButton = findViewById(R.id.alarms_btn_time);
        clearDateButton = findViewById(R.id.alarms_btn_clear_date);
        dateLabel = findViewById(R.id.alarms_date_label);
        listView = findViewById(R.id.alarms_list);
        emptyView = findViewById(R.id.alarms_empty);
        progress = findViewById(R.id.alarms_progress);
        swipe = findViewById(R.id.alarms_swipe);

        setupSpinner(statusSpinner, R.array.alarms_status_labels);
        setupSpinner(typeSpinner, R.array.alarms_type_labels);
        setupSpinner(severitySpinner, R.array.alarms_severity_labels);

        statusSpinner.setSelection(indexOf(STATUS_VALUES, prefs.getFilterStatus()));
        typeSpinner.setSelection(indexOf(TYPE_VALUES, prefs.getFilterType()));
        severitySpinner.setSelection(indexOf(SEVERITY_VALUES, prefs.getFilterSeverity()));

        AdapterView.OnItemSelectedListener filterListener =
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        if (!spinnersInitialized) return;
                        prefs.saveAlarmFilters(
                                STATUS_VALUES.get(statusSpinner.getSelectedItemPosition()),
                                TYPE_VALUES.get(typeSpinner.getSelectedItemPosition()),
                                SEVERITY_VALUES.get(severitySpinner.getSelectedItemPosition()));
                        loadAlarms();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                };
        statusSpinner.setOnItemSelectedListener(filterListener);
        typeSpinner.setOnItemSelectedListener(filterListener);
        severitySpinner.setOnItemSelectedListener(filterListener);
        spinnersInitialized = true;

        adapter = new AlarmAdapter(this, alarms);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Alarm a = adapter.getItem(position);
            if (a != null) openAlarmDetail(a.getId());
        });

        dateButton.setOnClickListener(v -> showDatePicker());
        timeButton.setOnClickListener(v -> showTimePicker());
        clearDateButton.setOnClickListener(v -> {
            selectedDate = null;
            selectedHour = -1;
            dateLabel.setText(R.string.alarms_date_any);
            clearDateButton.setVisibility(View.GONE);
            loadAlarms();
        });

        swipe.setOnRefreshListener(this::loadAlarms);

        loadAlarms();
    }

    private void setupSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    private static int indexOf(List<String> values, String value) {
        int idx = values.indexOf(value);
        return idx < 0 ? 0 : idx;
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
                            year, month + 1, day);
                    updateDateTimeLabel();
                    loadAlarms();
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        int initialHour = selectedHour >= 0 ? selectedHour : c.get(Calendar.HOUR_OF_DAY);
        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hour, minute) -> {
                    selectedHour = hour;
                    if (selectedDate == null) {
                        selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH) + 1,
                                c.get(Calendar.DAY_OF_MONTH));
                    }
                    updateDateTimeLabel();
                    loadAlarms();
                },
                initialHour, 0, true);
        dialog.show();
    }

    private void updateDateTimeLabel() {
        if (selectedDate == null && selectedHour < 0) {
            dateLabel.setText(R.string.alarms_date_any);
            clearDateButton.setVisibility(View.GONE);
            return;
        }
        String text;
        if (selectedHour >= 0) {
            text = getString(R.string.alarms_datetime_set_fmt,
                    selectedDate, selectedHour);
        } else {
            text = getString(R.string.alarms_date_set_fmt, selectedDate);
        }
        dateLabel.setText(text);
        clearDateButton.setVisibility(View.VISIBLE);
    }

    private void openAlarmDetail(int alarmId) {
        Intent intent = new Intent(this, AlarmDetailActivity.class);
        intent.putExtra(AlarmDetailActivity.EXTRA_ALARM_ID, alarmId);
        startActivity(intent);
    }

    private void loadAlarms() {
        if (!swipe.isRefreshing()) {
            progress.setVisibility(View.VISIBLE);
        }
        emptyView.setVisibility(View.GONE);

        final String url = buildUrl();
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        final DatabaseHelper db = DatabaseHelper.get(this);

        executor.execute(() -> {
            ApiClient client = new ApiClient(baseUrl, token);
            try {
                ApiClient.ApiResponse resp = client.get(url);
                final List<Alarm> parsed = new ArrayList<>();
                if (resp.isSuccess()) {
                    parsed.addAll(parseAlarmsList(resp.body));
                    db.replaceAlarms(parsed);
                }
                final boolean ok = resp.isSuccess();
                final int code = resp.code;
                Session.postOrAuth(mainHandler, this, code, () -> {
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    if (!ok) {
                        Toast.makeText(this,
                                getString(R.string.alarms_load_failed_fmt, code),
                                Toast.LENGTH_SHORT).show();
                    }
                    alarms.clear();
                    alarms.addAll(parsed);
                    adapter.setAlarms(alarms);
                    emptyView.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                final String msg = e.getMessage();
                final List<Alarm> cached = db.getAllAlarms();
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    Toast.makeText(this,
                            getString(R.string.alarms_offline_fmt, msg),
                            Toast.LENGTH_LONG).show();
                    alarms.clear();
                    alarms.addAll(cached);
                    adapter.setAlarms(alarms);
                    emptyView.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private String buildUrl() {
        StringBuilder sb = new StringBuilder("/api/alarms?limit=50");
        appendFilter(sb, "status", STATUS_VALUES.get(statusSpinner.getSelectedItemPosition()));
        appendFilter(sb, "type", TYPE_VALUES.get(typeSpinner.getSelectedItemPosition()));
        appendFilter(sb, "severity", SEVERITY_VALUES.get(severitySpinner.getSelectedItemPosition()));
        if (selectedDate != null) {
            String from, to;
            if (selectedHour >= 0) {
                from = String.format(Locale.US, "%sT%02d:00:00", selectedDate, selectedHour);
                to = String.format(Locale.US, "%sT%02d:59:59", selectedDate, selectedHour);
            } else {
                from = selectedDate + "T00:00:00";
                to = selectedDate + "T23:59:59";
            }
            sb.append("&date_from=").append(encode(from));
            sb.append("&date_to=").append(encode(to));
        }
        return sb.toString();
    }

    private static void appendFilter(StringBuilder sb, String key, String value) {
        if (value == null || value.isEmpty() || PrefsManager.FILTER_ALL.equals(value)) return;
        sb.append('&').append(key).append('=').append(encode(value));
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
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
                if (obj.has("alarms")) array = obj.getJSONArray("alarms");
                else if (obj.has("data")) array = obj.getJSONArray("data");
                else if (obj.has("results")) array = obj.getJSONArray("results");
                else return out;
            }
            for (int i = 0; i < array.length(); i++) {
                out.add(Alarm.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
