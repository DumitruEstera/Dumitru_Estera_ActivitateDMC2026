package com.example.proiect.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proiect.R;
import com.example.proiect.api.ApiClient;
import com.example.proiect.database.DatabaseHelper;
import com.example.proiect.models.Alarm;
import com.example.proiect.models.Camera;
import com.example.proiect.models.Zone;
import com.example.proiect.utils.PrefsManager;
import com.example.proiect.utils.Session;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_FOCUS_ZONES = "focus_zones";

    private static final LatLng MOCK_CENTER = new LatLng(44.4268, 26.1025);
    private static final double MOCK_SPREAD = 0.004;

    private GoogleMap map;
    private Spinner zoneSpinner;
    private Switch zonesSwitch, alarmsSwitch;
    private ProgressBar progress;

    private final List<Camera> cameras = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<Alarm> unresolvedAlarms = new ArrayList<>();

    private final List<Marker> cameraMarkers = new ArrayList<>();
    private final List<Marker> alarmMarkers = new ArrayList<>();
    private final List<Polygon> zonePolygons = new ArrayList<>();

    private PrefsManager prefs;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean dataLoaded = false;
    private boolean mapReady = false;
    private String[] focusZones;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_camera_map);

        focusZones = getIntent().getStringArrayExtra(EXTRA_FOCUS_ZONES);

        zoneSpinner = findViewById(R.id.map_zone_spinner);
        zonesSwitch = findViewById(R.id.map_switch_zones);
        alarmsSwitch = findViewById(R.id.map_switch_alarms);
        progress = findViewById(R.id.map_progress);

        zonesSwitch.setOnCheckedChangeListener((b, c) -> applyVisibility());
        alarmsSwitch.setOnCheckedChangeListener((b, c) -> applyVisibility());
        zoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                applyVisibility();
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        SupportMapFragment frag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (frag != null) frag.getMapAsync(this);

        loadData();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        this.mapReady = true;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MOCK_CENTER, 16f));
        if (dataLoaded) renderMap();
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();

        executor.execute(() -> {
            ApiClient client = new ApiClient(baseUrl, token);
            final List<Camera> camsLoaded = new ArrayList<>();
            final List<Zone> zonesLoaded = new ArrayList<>();
            final List<Alarm> alarmsLoaded = new ArrayList<>();
            int authCode = 200;

            try {
                ApiClient.ApiResponse r = client.get("/api/cameras-db");
                if (r.isSuccess()) camsLoaded.addAll(parseCameras(r.body));
                else if (r.code == 401) authCode = 401;
            } catch (Exception ignored) { }

            try {
                ApiClient.ApiResponse r = client.get("/api/zones");
                if (r.isSuccess()) zonesLoaded.addAll(parseZones(r.body));
                else if (r.code == 401) authCode = 401;
            } catch (Exception ignored) { }

            try {
                ApiClient.ApiResponse r = client.get(
                        "/api/alarms?status=unresolved&limit=20");
                if (r.isSuccess()) alarmsLoaded.addAll(parseAlarms(r.body));
                else if (r.code == 401) authCode = 401;
            } catch (Exception ignored) { }

            assignMockCoordsIfMissing(camsLoaded);
            if (!camsLoaded.isEmpty()) {
                DatabaseHelper.get(this).replaceCameras(camsLoaded);
            }

            final int finalAuth = authCode;
            Session.postOrAuth(mainHandler, this, finalAuth, () -> {
                cameras.clear(); cameras.addAll(camsLoaded);
                zones.clear(); zones.addAll(zonesLoaded);
                unresolvedAlarms.clear(); unresolvedAlarms.addAll(alarmsLoaded);
                rebuildZoneSpinner();
                progress.setVisibility(View.GONE);
                dataLoaded = true;
                if (mapReady) renderMap();
            });
        });
    }

    private void rebuildZoneSpinner() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Zone z : zones) if (z.getName() != null && !z.getName().isEmpty()) names.add(z.getName());
        for (Camera c : cameras) if (c.getZoneName() != null && !c.getZoneName().isEmpty()) names.add(c.getZoneName());

        List<String> items = new ArrayList<>();
        items.add(getString(R.string.map_zones_all));
        items.addAll(names);

        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zoneSpinner.setAdapter(a);

        if (focusZones != null && focusZones.length > 0) {
            int idx = items.indexOf(focusZones[0]);
            if (idx > 0) zoneSpinner.setSelection(idx);
        }
    }

    private void renderMap() {
        if (map == null) return;
        clearOverlays();

        for (Camera c : cameras) {
            if (!c.hasCoordinates()) continue;
            float hue = c.isRestricted()
                    ? BitmapDescriptorFactory.HUE_RED
                    : BitmapDescriptorFactory.HUE_GREEN;
            Marker m = map.addMarker(new MarkerOptions()
                    .position(new LatLng(c.getLatitude(), c.getLongitude()))
                    .title(c.getName())
                    .snippet(getString(R.string.map_camera_snippet_fmt,
                            c.getZoneName() == null || c.getZoneName().isEmpty() ? "—" : c.getZoneName(),
                            c.getLocation() == null || c.getLocation().isEmpty() ? "—" : c.getLocation()))
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
            if (m != null) {
                m.setTag(c);
                cameraMarkers.add(m);
            }
        }

        for (Zone z : zones) {
            if (z.getPolygon() == null || z.getPolygon().size() < 3) continue;
            int stroke = z.isRestricted() ? Color.parseColor("#D32F2F")
                                          : Color.parseColor("#1976D2");
            int fill = z.isRestricted() ? Color.argb(60, 211, 47, 47)
                                        : Color.argb(50, 25, 118, 210);
            Polygon p = map.addPolygon(new PolygonOptions()
                    .addAll(z.getPolygon())
                    .strokeColor(stroke)
                    .fillColor(fill)
                    .strokeWidth(3));
            if (p != null) {
                p.setTag(z);
                zonePolygons.add(p);
            }
        }

        Map<String, Camera> byId = new HashMap<>();
        for (Camera c : cameras) byId.put(c.getCameraId(), c);
        for (Alarm a : unresolvedAlarms) {
            Camera c = byId.get(a.getCameraId());
            if (c == null || !c.hasCoordinates()) continue;
            Marker m = map.addMarker(new MarkerOptions()
                    .position(new LatLng(c.getLatitude(), c.getLongitude()))
                    .title((a.getType() == null ? "" : a.getType().toUpperCase(Locale.US))
                            + " — " + (a.getSeverity() == null ? "" : a.getSeverity()))
                    .snippet(a.getDescription() == null ? "" : a.getDescription())
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_ROSE)));
            if (m != null) {
                m.setTag(a);
                alarmMarkers.add(m);
            }
        }

        applyVisibility();
        zoomToContent();

        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Alarm) {
                Alarm a = (Alarm) tag;
                android.content.Intent intent = new android.content.Intent(
                        this, AlarmDetailActivity.class);
                intent.putExtra(AlarmDetailActivity.EXTRA_ALARM_ID, a.getId());
                startActivity(intent);
            }
        });
    }

    private void applyVisibility() {
        if (map == null) return;

        String selected = (String) zoneSpinner.getSelectedItem();
        boolean filterAll = selected == null || getString(R.string.map_zones_all).equals(selected);

        boolean showZones = zonesSwitch.isChecked();
        for (Polygon p : zonePolygons) {
            Zone z = (Zone) p.getTag();
            boolean inFilter = filterAll
                    || (z != null && selected.equalsIgnoreCase(z.getName()));
            p.setVisible(showZones && inFilter);
        }

        for (Marker m : cameraMarkers) {
            Camera c = (Camera) m.getTag();
            boolean inFilter = filterAll
                    || (c != null && c.getZoneName() != null
                        && selected.equalsIgnoreCase(c.getZoneName()));
            m.setVisible(inFilter);
        }

        boolean showAlarms = alarmsSwitch.isChecked();
        for (Marker m : alarmMarkers) {
            Alarm a = (Alarm) m.getTag();
            Camera c = a == null ? null : findCamera(a.getCameraId());
            boolean inFilter = filterAll
                    || (c != null && c.getZoneName() != null
                        && selected.equalsIgnoreCase(c.getZoneName()));
            m.setVisible(showAlarms && inFilter);
        }
    }

    private Camera findCamera(String id) {
        if (id == null) return null;
        for (Camera c : cameras) if (id.equals(c.getCameraId())) return c;
        return null;
    }

    private void zoomToContent() {
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        boolean any = false;
        for (Camera c : cameras) {
            if (c.hasCoordinates()) {
                b.include(new LatLng(c.getLatitude(), c.getLongitude()));
                any = true;
            }
        }
        for (Zone z : zones) {
            for (LatLng p : z.getPolygon()) {
                b.include(p);
                any = true;
            }
        }
        if (any) {
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 120));
            } catch (Exception ignored) { }
        }
    }

    private void clearOverlays() {
        for (Marker m : cameraMarkers) m.remove();
        for (Marker m : alarmMarkers) m.remove();
        for (Polygon p : zonePolygons) p.remove();
        cameraMarkers.clear();
        alarmMarkers.clear();
        zonePolygons.clear();
    }

    private static void assignMockCoordsIfMissing(List<Camera> list) {
        boolean anyMissing = false;
        for (Camera c : list) if (!c.hasCoordinates()) { anyMissing = true; break; }
        if (!anyMissing) return;
        int idx = 0;
        for (Camera c : list) {
            if (c.hasCoordinates()) continue;
            double angle = (idx * 137.508) * Math.PI / 180.0;
            double radius = MOCK_SPREAD * (0.4 + (idx % 4) * 0.2);
            c.setLatitude(MOCK_CENTER.latitude + Math.cos(angle) * radius);
            c.setLongitude(MOCK_CENTER.longitude + Math.sin(angle) * radius);
            c.setHasCoordinates(true);
            idx++;
        }
    }

    private static List<Camera> parseCameras(String body) {
        List<Camera> out = new ArrayList<>();
        try {
            JSONArray arr = extractArray(body, "cameras", "data", "results");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                out.add(Camera.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static List<Zone> parseZones(String body) {
        List<Zone> out = new ArrayList<>();
        try {
            JSONArray arr = extractArray(body, "zones", "data", "results");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                out.add(Zone.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static List<Alarm> parseAlarms(String body) {
        List<Alarm> out = new ArrayList<>();
        try {
            JSONArray arr = extractArray(body, "alarms", "data", "results");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                out.add(Alarm.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static JSONArray extractArray(String body, String... keys) throws Exception {
        if (body == null || body.isEmpty()) return null;
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) return new JSONArray(trimmed);
        JSONObject obj = new JSONObject(trimmed);
        for (String k : keys) {
            if (obj.has(k) && obj.optJSONArray(k) != null) return obj.getJSONArray(k);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
