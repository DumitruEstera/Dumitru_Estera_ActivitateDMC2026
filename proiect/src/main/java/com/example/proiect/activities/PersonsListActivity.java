package com.example.proiect.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.example.proiect.R;
import com.example.proiect.adapters.PersonAdapter;
import com.example.proiect.api.ApiClient;
import com.example.proiect.database.DatabaseHelper;
import com.example.proiect.models.Person;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonsListActivity extends AppCompatActivity {

    private EditText searchInput;
    private Spinner deptSpinner;
    private ListView listView;
    private ProgressBar progress;
    private TextView emptyView;

    private PersonAdapter adapter;
    private final List<Person> persons = new ArrayList<>();
    private final List<String> departments = new ArrayList<>();

    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_persons_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        searchInput = findViewById(R.id.persons_search);
        deptSpinner = findViewById(R.id.persons_dept_spinner);
        listView = findViewById(R.id.persons_list);
        progress = findViewById(R.id.persons_progress);
        emptyView = findViewById(R.id.persons_empty);

        adapter = new PersonAdapter(this, persons);
        listView.setAdapter(adapter);
        adapter.registerDataSetObserver(new android.database.DataSetObserver() {
            @Override
            public void onChanged() {
                emptyView.setVisibility(adapter.getCount() == 0
                        ? View.VISIBLE : View.GONE);
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Person p = adapter.getItem(position);
            if (p != null) openPersonDetail(p.getId());
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(s.toString());
            }
        });

        deptSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                String filter = (selected == null
                        || getString(R.string.persons_dept_all).equals(selected))
                        ? "" : selected;
                adapter.setDepartmentFilter(filter);
                adapter.getFilter().filter(searchInput.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        rebuildDeptSpinner();
        loadData();
    }

    private void openPersonDetail(int personId) {
        android.content.Intent intent = new android.content.Intent(this,
                PersonDetailActivity.class);
        intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, personId);
        startActivity(intent);
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        final DatabaseHelper db = DatabaseHelper.get(this);

        executor.execute(() -> {
            ApiClient client = new ApiClient(baseUrl, token);
            final List<Person> loaded = new ArrayList<>();
            final List<String> depts = new ArrayList<>();
            String error = null;
            boolean networkFailed = false;
            int authCode = 200;

            try {
                ApiClient.ApiResponse personsResp = client.get("/api/persons");
                if (personsResp.isSuccess()) {
                    loaded.addAll(parsePersonsList(personsResp.body));
                    db.replacePersons(loaded);
                } else {
                    error = "Failed to load persons (HTTP " + personsResp.code + ")";
                    if (personsResp.code == 401) authCode = 401;
                }
            } catch (Exception e) {
                error = "Offline — showing cached data";
                networkFailed = true;
                loaded.addAll(db.getAllPersons());
            }

            try {
                ApiClient.ApiResponse deptResp = client.get("/api/departments");
                if (deptResp.isSuccess()) {
                    depts.addAll(parseDepartmentsList(deptResp.body));
                } else if (deptResp.code == 401) {
                    authCode = 401;
                }
            } catch (Exception ignored) { }

            if (depts.isEmpty()) {
                LinkedHashSet<String> distinct = new LinkedHashSet<>();
                for (Person p : loaded) {
                    if (p.getDepartment() != null && !p.getDepartment().isEmpty()) {
                        distinct.add(p.getDepartment());
                    }
                }
                depts.addAll(distinct);
            }

            final String errMsg = error;
            final int finalAuthCode = authCode;
            Session.postOrAuth(mainHandler, this, finalAuthCode, () -> {
                progress.setVisibility(View.GONE);
                if (errMsg != null) {
                    Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
                }
                persons.clear();
                persons.addAll(loaded);

                departments.clear();
                departments.addAll(depts);
                rebuildDeptSpinner();

                adapter.setPersons(persons);
                adapter.getFilter().filter(searchInput.getText().toString());
            });
        });
    }

    private void rebuildDeptSpinner() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.persons_dept_all));
        items.addAll(departments);
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deptSpinner.setAdapter(a);
    }

    private static List<Person> parsePersonsList(String body) {
        List<Person> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;
        try {
            JSONArray array = extractArray(body, "persons", "data", "results");
            if (array == null) return out;
            for (int i = 0; i < array.length(); i++) {
                out.add(Person.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static List<String> parseDepartmentsList(String body) {
        List<String> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;
        try {
            JSONArray array = extractArray(body, "departments", "data", "results");
            if (array == null) return out;
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                if (item instanceof String) {
                    out.add((String) item);
                } else if (item instanceof JSONObject) {
                    JSONObject obj = (JSONObject) item;
                    String name = obj.optString("name",
                            obj.optString("department", ""));
                    if (!name.isEmpty()) out.add(name);
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static JSONArray extractArray(String body, String... keys) throws Exception {
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) return new JSONArray(trimmed);
        JSONObject obj = new JSONObject(trimmed);
        for (String k : keys) {
            if (obj.has(k) && obj.optJSONArray(k) != null) {
                return obj.getJSONArray(k);
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
