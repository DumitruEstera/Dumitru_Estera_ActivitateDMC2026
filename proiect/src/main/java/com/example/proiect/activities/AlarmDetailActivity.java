package com.example.proiect.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.example.proiect.R;
import com.example.proiect.api.ApiClient;
import com.example.proiect.database.DatabaseHelper;
import com.example.proiect.models.Alarm;
import com.example.proiect.utils.DateUtils;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ALARM_ID = "alarm_id";

    private TextView typeView, severityView, timestampView, cameraView, descriptionView;
    private TextView metadataHeader;
    private View metadataCard;
    private LinearLayout metadataContainer;
    private View snapshotCard;
    private ImageView snapshotView;
    private EditText notesInput;
    private Button btnSaveNotes, btnResolve, btnFalse;
    private ProgressBar progress;
    private ScrollView scroll;

    private int alarmId;
    private Alarm currentAlarm;
    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) {
            finish();
            return;
        }

        alarmId = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);
        if (alarmId <= 0) {
            Toast.makeText(this, R.string.detail_invalid_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_alarm_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        typeView = findViewById(R.id.detail_type);
        severityView = findViewById(R.id.detail_severity);
        timestampView = findViewById(R.id.detail_timestamp);
        cameraView = findViewById(R.id.detail_camera);
        descriptionView = findViewById(R.id.detail_description);
        metadataHeader = findViewById(R.id.detail_metadata_header);
        metadataCard = findViewById(R.id.detail_metadata_card);
        metadataContainer = findViewById(R.id.detail_metadata_container);
        snapshotCard = findViewById(R.id.detail_snapshot_card);
        snapshotView = findViewById(R.id.detail_snapshot);
        notesInput = findViewById(R.id.detail_notes);
        btnSaveNotes = findViewById(R.id.detail_btn_save_notes);
        btnResolve = findViewById(R.id.detail_btn_resolve);
        btnFalse = findViewById(R.id.detail_btn_false);
        progress = findViewById(R.id.detail_progress);
        scroll = findViewById(R.id.detail_scroll);

        setTitle(getString(R.string.detail_title_fmt, alarmId));

        btnSaveNotes.setOnClickListener(v -> saveNotes());
        btnResolve.setOnClickListener(v -> updateStatus("resolved"));
        btnFalse.setOnClickListener(v -> updateStatus("false_alarm"));

        loadAlarm();
    }

    private void loadAlarm() {
        showLoading(true);
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        executor.execute(() -> {
            try {
                ApiClient client = new ApiClient(baseUrl, token);
                ApiClient.ApiResponse resp = client.get("/api/alarms/" + alarmId);
                if (resp.isSuccess()) {
                    final Alarm alarm = Alarm.fromJson(resp.asJson());
                    DatabaseHelper.get(this).upsertAlarm(alarm);
                    final Bitmap snapshot = decodeSnapshot(alarm.getSnapshot());
                    Session.postOrAuth(mainHandler, this, resp.code, () -> {
                        currentAlarm = alarm;
                        renderAlarm(alarm, snapshot);
                        showLoading(false);
                    });
                } else {
                    final int code = resp.code;
                    Session.postOrAuth(mainHandler, this, code, () -> {
                        showLoading(false);
                        Toast.makeText(this,
                                getString(R.string.alarms_load_failed_fmt, code),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                final String msg = e.getMessage();
                final Alarm cached = DatabaseHelper.get(this).getAlarmById(alarmId);
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    if (cached != null) {
                        currentAlarm = cached;
                        Bitmap snap = decodeSnapshot(cached.getSnapshot());
                        renderAlarm(cached, snap);
                        showLoading(false);
                        Toast.makeText(this,
                                getString(R.string.alarms_offline_fmt, msg),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showLoading(false);
                        Toast.makeText(this,
                                getString(R.string.alarms_network_error_fmt, msg),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        });
    }

    private void renderAlarm(Alarm alarm, @Nullable Bitmap snapshot) {
        String type = alarm.getType() == null ? "" : alarm.getType();
        typeView.setText(type.toUpperCase(Locale.US) + " " + getString(R.string.detail_type_suffix));
        typeView.setBackgroundColor(typeColor(type));

        String severity = alarm.getSeverity() == null ? "" : alarm.getSeverity();
        severityView.setText(severity.toUpperCase(Locale.US));
        tintBadge(severityView, severityColor(severity));

        String when = DateUtils.relative(alarm.getCreatedAt());
        timestampView.setText(getString(R.string.detail_timestamp_fmt,
                alarm.getCreatedAt(), when));
        cameraView.setText(getString(R.string.detail_camera_fmt,
                TextUtils.isEmpty(alarm.getCameraId()) ? "—" : alarm.getCameraId()));
        descriptionView.setText(TextUtils.isEmpty(alarm.getDescription())
                ? getString(R.string.detail_no_description) : alarm.getDescription());

        if (!TextUtils.isEmpty(alarm.getNotes())) {
            notesInput.setText(alarm.getNotes());
        }

        renderMetadata(alarm.getMetadataJson());

        if (snapshot != null) {
            snapshotView.setImageBitmap(snapshot);
            snapshotCard.setVisibility(View.VISIBLE);
        } else {
            snapshotCard.setVisibility(View.GONE);
        }

        boolean unresolved = "unresolved".equalsIgnoreCase(alarm.getStatus());
        btnResolve.setEnabled(unresolved);
        btnFalse.setEnabled(unresolved);
    }

    private void renderMetadata(@Nullable String metadataJson) {
        metadataContainer.removeAllViews();
        if (TextUtils.isEmpty(metadataJson)) {
            metadataHeader.setVisibility(View.GONE);
            metadataCard.setVisibility(View.GONE);
            return;
        }
        try {
            JSONObject obj = new JSONObject(metadataJson);
            Iterator<String> keys = obj.keys();
            int rows = 0;
            while (keys.hasNext()) {
                String key = keys.next();
                String value = obj.opt(key) == null ? "" : String.valueOf(obj.opt(key));
                metadataContainer.addView(buildMetaRow(key, value));
                rows++;
            }
            int visible = rows > 0 ? View.VISIBLE : View.GONE;
            metadataHeader.setVisibility(visible);
            metadataCard.setVisibility(visible);
        } catch (Exception e) {
            metadataHeader.setVisibility(View.GONE);
            metadataCard.setVisibility(View.GONE);
        }
    }

    private View buildMetaRow(String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(4);
        rowLp.bottomMargin = dp(4);
        row.setLayoutParams(rowLp);

        TextView k = new TextView(this);
        LinearLayout.LayoutParams kLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k.setLayoutParams(kLp);
        k.setText(key);
        k.setTextColor(getResources().getColor(R.color.login_subtitle));
        k.setTextSize(13f);

        TextView v = new TextView(this);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 2f);
        v.setLayoutParams(vLp);
        v.setText(value);
        v.setGravity(Gravity.END);
        v.setTextColor(getResources().getColor(R.color.login_title));
        v.setTextSize(13f);

        row.addView(k);
        row.addView(v);
        return row;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Nullable
    private static Bitmap decodeSnapshot(@Nullable String base64) {
        if (TextUtils.isEmpty(base64)) return null;
        try {
            String clean = base64;
            int comma = clean.indexOf(',');
            if (clean.startsWith("data:") && comma > 0) {
                clean = clean.substring(comma + 1);
            }
            byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveNotes() {
        if (currentAlarm == null) return;
        final String notes = notesInput.getText().toString();
        DatabaseHelper.get(this).updateAlarmNotes(alarmId, notes);
        sendPatch(buildBody("notes", notes), R.string.detail_notes_saved, false);
    }

    private void updateStatus(String newStatus) {
        if (currentAlarm == null) return;
        DatabaseHelper.get(this).updateAlarmStatus(alarmId, newStatus);
        sendPatch(buildBody("status", newStatus), R.string.detail_status_updated, true);
    }

    private static JSONObject buildBody(String key, String value) {
        JSONObject body = new JSONObject();
        try {
            body.put(key, value);
        } catch (Exception ignored) { }
        return body;
    }

    private void sendPatch(JSONObject body, int successMsgRes, boolean finishOnSuccess) {
        showLoading(true);
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        executor.execute(() -> {
            try {
                ApiClient client = new ApiClient(baseUrl, token);
                ApiClient.ApiResponse resp = client.patch("/api/alarms/" + alarmId, body);
                final boolean ok = resp.isSuccess();
                final int code = resp.code;
                Session.postOrAuth(mainHandler, this, code, () -> {
                    showLoading(false);
                    if (ok) {
                        Toast.makeText(this, successMsgRes, Toast.LENGTH_SHORT).show();
                        if (finishOnSuccess) finish();
                    } else {
                        Toast.makeText(this,
                                getString(R.string.alarms_load_failed_fmt, code),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                final String msg = e.getMessage();
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this,
                            getString(R.string.alarms_network_error_fmt, msg),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        scroll.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private static int typeColor(String type) {
        if (type == null) return Color.parseColor("#1A2F4B");
        switch (type.toLowerCase(Locale.US)) {
            case "weapon":            return Color.parseColor("#7B1FA2");
            case "fire":              return Color.parseColor("#F57C00");
            case "face":              return Color.parseColor("#1976D2");
            case "har":               return Color.parseColor("#0097A7");
            case "unauthorized_zone": return Color.parseColor("#D32F2F");
            default:                  return Color.parseColor("#1A2F4B");
        }
    }

    private static int severityColor(String severity) {
        if (severity == null) return Color.parseColor("#4CAF50");
        switch (severity.toLowerCase(Locale.US)) {
            case "critical": return Color.parseColor("#D32F2F");
            case "high":     return Color.parseColor("#F57C00");
            case "medium":   return Color.parseColor("#FBC02D");
            default:         return Color.parseColor("#4CAF50");
        }
    }

    private static void tintBadge(TextView tv, int color) {
        if (tv.getBackground() instanceof GradientDrawable) {
            GradientDrawable g = (GradientDrawable) tv.getBackground().mutate();
            g.setColor(color);
        } else {
            tv.setBackgroundColor(color);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
