package com.example.proiect.activities;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
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
import com.example.proiect.models.AccessLogEntry;
import com.example.proiect.models.Person;
import com.example.proiect.utils.DateUtils;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "person_id";

    private TextView nameView, departmentView, employeeView, faceCountView, zonesView;
    private CheckBox favoriteBox;
    private Button mapButton;
    private ListView historyList;
    private TextView historyEmpty;
    private ProgressBar progress;
    private ScrollView scroll;

    private int personId;
    private Person currentPerson;
    private final List<AccessLogEntry> history = new ArrayList<>();
    private AccessLogAdapter historyAdapter;

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

        personId = getIntent().getIntExtra(EXTRA_PERSON_ID, -1);
        if (personId <= 0) {
            Toast.makeText(this, R.string.person_invalid_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_person_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        setTitle(R.string.persons_title);

        nameView = findViewById(R.id.person_name);
        departmentView = findViewById(R.id.person_department);
        employeeView = findViewById(R.id.person_employee);
        faceCountView = findViewById(R.id.person_face_count);
        zonesView = findViewById(R.id.person_zones);
        favoriteBox = findViewById(R.id.person_favorite);
        mapButton = findViewById(R.id.person_btn_map);
        historyList = findViewById(R.id.person_history_list);
        historyEmpty = findViewById(R.id.person_history_empty);
        progress = findViewById(R.id.person_progress);
        scroll = findViewById(R.id.person_scroll);

        final DatabaseHelper db = DatabaseHelper.get(this);
        favoriteBox.setChecked(db.isPersonFavorite(personId));
        favoriteBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.setPersonFavorite(personId, isChecked);
            Toast.makeText(this,
                    isChecked ? R.string.person_favorite_added
                              : R.string.person_favorite_removed,
                    Toast.LENGTH_SHORT).show();
        });

        mapButton.setOnClickListener(v -> openOnMap());

        historyAdapter = new AccessLogAdapter(this, history);
        historyList.setAdapter(historyAdapter);

        loadPerson();
    }

    private void openOnMap() {
        if (currentPerson == null) return;
        android.content.Intent intent = new android.content.Intent(this,
                CameraMapActivity.class);
        List<String> zones = currentPerson.getAuthorizedZones();
        if (zones != null && !zones.isEmpty()) {
            intent.putExtra(CameraMapActivity.EXTRA_FOCUS_ZONES,
                    zones.toArray(new String[0]));
        }
        startActivity(intent);
    }

    private void loadPerson() {
        showLoading(true);
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        executor.execute(() -> {
            try {
                ApiClient client = new ApiClient(baseUrl, token);
                ApiClient.ApiResponse resp = client.get("/api/persons/" + personId);
                if (resp.isSuccess()) {
                    JSONObject json = resp.asJson();
                    final Person person = Person.fromJson(json);
                    final List<AccessLogEntry> logs = parseAccessHistory(json);
                    DatabaseHelper helper = DatabaseHelper.get(this);
                    helper.upsertPerson(person);
                    helper.replaceAccessLogs(personId, logs);
                    Session.postOrAuth(mainHandler, this, resp.code, () -> {
                        currentPerson = person;
                        renderPerson(person, logs);
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
                DatabaseHelper helper = DatabaseHelper.get(this);
                final Person cached = findCachedPerson(helper, personId);
                final List<AccessLogEntry> cachedLogs = helper.getAccessLogsFor(personId);
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    if (cached != null) {
                        currentPerson = cached;
                        renderPerson(cached, cachedLogs);
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

    private void renderPerson(Person person, List<AccessLogEntry> logs) {
        nameView.setText(TextUtils.isEmpty(person.getName())
                ? getString(R.string.persons_unknown_name) : person.getName());
        departmentView.setText(getString(R.string.person_department_fmt,
                TextUtils.isEmpty(person.getDepartment()) ? "—" : person.getDepartment()));
        employeeView.setText(getString(R.string.persons_employee_fmt,
                TextUtils.isEmpty(person.getEmployeeId()) ? "—" : person.getEmployeeId()));
        faceCountView.setText(getString(R.string.person_face_count_fmt,
                person.getFaceCount()));

        List<String> zones = person.getAuthorizedZones();
        if (zones == null || zones.isEmpty()) {
            zonesView.setText(R.string.person_zones_none);
        } else {
            zonesView.setText(TextUtils.join(" • ", zones));
        }

        history.clear();
        history.addAll(logs);
        historyAdapter.notifyDataSetChanged();
        historyEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private static Person findCachedPerson(DatabaseHelper helper, int id) {
        for (Person p : helper.getAllPersons()) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    private static List<AccessLogEntry> parseAccessHistory(JSONObject json) {
        List<AccessLogEntry> out = new ArrayList<>();
        JSONArray arr = json.optJSONArray("access_history");
        if (arr == null) arr = json.optJSONArray("history");
        if (arr == null) arr = json.optJSONArray("logs");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            try {
                out.add(AccessLogEntry.fromJson(item));
            } catch (Exception ignored) { }
        }
        return out;
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        scroll.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    static class AccessLogAdapter extends BaseAdapter {
        private final List<AccessLogEntry> data;
        private final LayoutInflater inflater;

        AccessLogAdapter(android.content.Context ctx, List<AccessLogEntry> data) {
            this.data = data;
            this.inflater = LayoutInflater.from(ctx);
        }

        @Override public int getCount() { return data.size(); }
        @Override public AccessLogEntry getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_access_log, parent, false);
                h = new Holder();
                h.timestamp = convertView.findViewById(R.id.access_timestamp);
                h.camera = convertView.findViewById(R.id.access_camera);
                h.status = convertView.findViewById(R.id.access_status);
                convertView.setTag(h);
            } else {
                h = (Holder) convertView.getTag();
            }

            AccessLogEntry e = data.get(position);
            String when = DateUtils.relative(e.getTimestamp());
            h.timestamp.setText(TextUtils.isEmpty(when) ? "—" : when);
            h.camera.setText(TextUtils.isEmpty(e.getCameraId()) ? "—" : e.getCameraId());

            String status = e.getStatus() == null ? "" : e.getStatus();
            h.status.setText(status.toUpperCase(Locale.US));
            int color;
            switch (status.toLowerCase(Locale.US)) {
                case "authorized": color = Color.parseColor("#388E3C"); break;
                case "unauthorized": color = Color.parseColor("#D32F2F"); break;
                default: color = Color.parseColor("#607D8B");
            }
            if (h.status.getBackground() instanceof GradientDrawable) {
                GradientDrawable g = (GradientDrawable) h.status.getBackground().mutate();
                g.setColor(color);
            } else {
                h.status.setBackgroundColor(color);
            }
            return convertView;
        }

        static class Holder {
            TextView timestamp;
            TextView camera;
            TextView status;
        }
    }
}
