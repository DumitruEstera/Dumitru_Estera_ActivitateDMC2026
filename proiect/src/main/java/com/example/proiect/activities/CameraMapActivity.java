package com.example.proiect.activities;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proiect.R;
import com.example.proiect.database.DatabaseHelper;
import com.example.proiect.models.MapLine;
import com.example.proiect.models.MapPin;
import com.example.proiect.utils.PrefsManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CameraMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Kept for source compatibility with callers that still pass this extra; ignored.
    public static final String EXTRA_FOCUS_ZONES = "focus_zones";

    private static final LatLng DEFAULT_CENTER = new LatLng(44.4268, 26.1025);
    private static final float HUE_NORMAL = BitmapDescriptorFactory.HUE_AZURE;
    private static final float HUE_SELECTED = BitmapDescriptorFactory.HUE_YELLOW;
    private static final int LINE_COLOR = Color.parseColor("#1976D2");

    private enum Mode { ADD, CONNECT, DELETE }

    private GoogleMap map;
    private MaterialButtonToggleGroup modeGroup;
    private TextView hintText;
    private Switch showLinesSwitch;
    private Switch showNamesSwitch;
    private Button clearBtn;

    private Mode currentMode = Mode.ADD;

    private final List<MapPin> pins = new ArrayList<>();
    private final Map<Long, Marker> markersByPinId = new HashMap<>();

    private final List<MapLine> lines = new ArrayList<>();
    private final Map<Long, Polyline> polylinesByLineId = new HashMap<>();

    private final List<Long> selectedPinIds = new ArrayList<>();

    private DatabaseHelper db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefsManager prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_camera_map);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        modeGroup = findViewById(R.id.map_mode_group);
        hintText = findViewById(R.id.map_hint);
        showLinesSwitch = findViewById(R.id.map_switch_lines);
        showNamesSwitch = findViewById(R.id.map_switch_names);
        clearBtn = findViewById(R.id.map_btn_clear);

        db = DatabaseHelper.get(this);
        pins.addAll(db.getAllMapPins());
        lines.addAll(db.getAllMapLines());

        modeGroup.check(R.id.map_mode_add);
        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.map_mode_add) setMode(Mode.ADD);
            else if (checkedId == R.id.map_mode_connect) setMode(Mode.CONNECT);
            else if (checkedId == R.id.map_mode_delete) setMode(Mode.DELETE);
        });

        showLinesSwitch.setOnCheckedChangeListener((b, checked) -> applyLineVisibility(checked));
        showNamesSwitch.setOnCheckedChangeListener((b, checked) -> refreshAllMarkerIcons());
        clearBtn.setOnClickListener(v -> confirmClearAll());

        SupportMapFragment frag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (frag != null) frag.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        map.setOnMapClickListener(this::onMapTapped);
        map.setOnMarkerClickListener(this::onMarkerClicked);
        map.setOnPolylineClickListener(this::onPolylineClicked);

        renderAllPins();
        renderAllLines();

        if (pins.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, 16f));
        } else {
            zoomToPins();
        }
    }

    // ---------- Mode handling ----------

    private void setMode(Mode mode) {
        currentMode = mode;
        clearSelection();
        switch (mode) {
            case ADD:     hintText.setText(R.string.map_hint_add); break;
            case CONNECT: hintText.setText(R.string.map_hint_connect); break;
            case DELETE:  hintText.setText(R.string.map_hint_delete); break;
        }
    }

    // ---------- Map / marker / line taps ----------

    private void onMapTapped(LatLng latLng) {
        if (currentMode == Mode.ADD) {
            promptForNewPin(latLng);
        }
    }

    private boolean onMarkerClicked(Marker marker) {
        Object tag = marker.getTag();
        if (!(tag instanceof Long)) return false;
        long pinId = (Long) tag;

        switch (currentMode) {
            case CONNECT:
                togglePinSelection(pinId);
                return true;
            case DELETE:
                confirmDeletePin(pinId, marker);
                return true;
            case ADD:
            default:
                // Consume so we don't auto-pan/open info window in Add mode.
                return true;
        }
    }

    private void onPolylineClicked(Polyline polyline) {
        if (currentMode != Mode.DELETE) return;
        Object tag = polyline.getTag();
        if (!(tag instanceof Long)) return;
        confirmDeleteLine((Long) tag);
    }

    // ---------- Connect mode ----------

    private void togglePinSelection(long pinId) {
        if (selectedPinIds.contains(pinId)) {
            selectedPinIds.remove(Long.valueOf(pinId));
            setMarkerSelected(pinId, false);
            return;
        }
        if (selectedPinIds.size() >= 2) clearSelection();

        selectedPinIds.add(pinId);
        setMarkerSelected(pinId, true);

        if (selectedPinIds.size() == 1) {
            Toast.makeText(this, R.string.map_pick_second, Toast.LENGTH_SHORT).show();
        } else if (selectedPinIds.size() == 2) {
            createLineBetweenSelected();
        }
    }

    private void createLineBetweenSelected() {
        if (selectedPinIds.size() != 2) return;
        long a = selectedPinIds.get(0);
        long b = selectedPinIds.get(1);

        if (lineExistsBetween(a, b)) {
            Toast.makeText(this, R.string.map_line_exists, Toast.LENGTH_SHORT).show();
            clearSelection();
            return;
        }

        long id = db.insertMapLine(a, b);
        if (id < 0) { clearSelection(); return; }
        MapLine line = new MapLine(id, a, b, System.currentTimeMillis());
        lines.add(line);
        addPolylineForLine(line);
        Toast.makeText(this, R.string.map_line_drawn, Toast.LENGTH_SHORT).show();
        clearSelection();
    }

    private boolean lineExistsBetween(long a, long b) {
        for (MapLine ln : lines) {
            if ((ln.getPinAId() == a && ln.getPinBId() == b)
                    || (ln.getPinAId() == b && ln.getPinBId() == a)) {
                return true;
            }
        }
        return false;
    }

    private void clearSelection() {
        for (long id : selectedPinIds) setMarkerSelected(id, false);
        selectedPinIds.clear();
    }

    private void setMarkerSelected(long pinId, boolean selected) {
        applyMarkerIcon(pinId, selected);
    }

    private void applyMarkerIcon(long pinId, boolean selected) {
        Marker m = markersByPinId.get(pinId);
        MapPin pin = findPin(pinId);
        if (m == null || pin == null) return;
        if (showNamesSwitch != null && showNamesSwitch.isChecked()) {
            m.setIcon(BitmapDescriptorFactory.fromBitmap(buildLabeledMarker(pin.getLabel(), selected)));
        } else {
            m.setIcon(BitmapDescriptorFactory.defaultMarker(selected ? HUE_SELECTED : HUE_NORMAL));
        }
        m.setAnchor(0.5f, 1.0f);
    }

    private void refreshAllMarkerIcons() {
        for (MapPin pin : pins) {
            boolean sel = selectedPinIds.contains(pin.getId());
            applyMarkerIcon(pin.getId(), sel);
        }
    }

    private Bitmap buildLabeledMarker(String rawLabel, boolean selected) {
        float d = getResources().getDisplayMetrics().density;
        String label = rawLabel == null ? "" : rawLabel;
        if (label.length() > 22) label = label.substring(0, 21) + "…";

        int pinColor = selected ? Color.parseColor("#F9A825") : Color.parseColor("#1976D2");

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(12f * d);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);

        float textW = textPaint.measureText(label);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textH = fm.descent - fm.ascent;

        float hPad = 8f * d;
        float vPad = 4f * d;
        float gap = 3f * d;
        float circleR = 8f * d;
        float tipH = 7f * d;
        float corner = 6f * d;

        int labelW = (int) Math.ceil(textW + 2 * hPad);
        int labelH = (int) Math.ceil(textH + 2 * vPad);
        int width = Math.max(labelW, (int) Math.ceil(circleR * 2)) + 2;
        int height = (int) Math.ceil(labelH + gap + circleR * 2 + tipH);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * d);
        borderPaint.setColor(pinColor);

        float lx = (width - labelW) / 2f;
        RectF labelRect = new RectF(lx, 0, lx + labelW, labelH);
        c.drawRoundRect(labelRect, corner, corner, bgPaint);
        c.drawRoundRect(labelRect, corner, corner, borderPaint);

        float textY = labelH / 2f - (fm.ascent + fm.descent) / 2f;
        c.drawText(label, width / 2f, textY, textPaint);

        Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(pinColor);
        float cx = width / 2f;
        float cy = labelH + gap + circleR;
        c.drawCircle(cx, cy, circleR, pinPaint);

        Path tip = new Path();
        tip.moveTo(cx - circleR * 0.6f, cy + circleR * 0.4f);
        tip.lineTo(cx + circleR * 0.6f, cy + circleR * 0.4f);
        tip.lineTo(cx, cy + circleR + tipH);
        tip.close();
        c.drawPath(tip, pinPaint);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        c.drawCircle(cx, cy, circleR * 0.35f, dotPaint);

        return bmp;
    }

    // ---------- Visibility ----------

    private void applyLineVisibility(boolean visible) {
        for (Polyline p : polylinesByLineId.values()) p.setVisible(visible);
    }

    // ---------- Pin / line deletion ----------

    private void confirmDeletePin(long pinId, Marker marker) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.map_delete)
                .setMessage(marker.getTitle())
                .setPositiveButton(R.string.map_delete, (d, w) -> deletePin(pinId, marker))
                .setNegativeButton(R.string.map_cancel, null)
                .show();
    }

    private void deletePin(long pinId, Marker marker) {
        // Drop any lines that reference this pin (DB + map).
        db.deleteLinesForPin(pinId);
        List<Long> doomedLineIds = new ArrayList<>();
        for (MapLine ln : lines) {
            if (ln.getPinAId() == pinId || ln.getPinBId() == pinId) {
                doomedLineIds.add(ln.getId());
            }
        }
        for (Long lid : doomedLineIds) {
            Polyline p = polylinesByLineId.remove(lid);
            if (p != null) p.remove();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getId() == lid) { lines.remove(i); break; }
            }
        }

        db.deleteMapPin(pinId);
        marker.remove();
        markersByPinId.remove(pinId);
        for (int i = 0; i < pins.size(); i++) {
            if (pins.get(i).getId() == pinId) { pins.remove(i); break; }
        }
        selectedPinIds.remove(Long.valueOf(pinId));
        Toast.makeText(this, R.string.map_pin_deleted, Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteLine(long lineId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.map_line_delete_title)
                .setPositiveButton(R.string.map_delete, (d, w) -> deleteLine(lineId))
                .setNegativeButton(R.string.map_cancel, null)
                .show();
    }

    private void deleteLine(long lineId) {
        db.deleteMapLine(lineId);
        Polyline p = polylinesByLineId.remove(lineId);
        if (p != null) p.remove();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getId() == lineId) { lines.remove(i); break; }
        }
        Toast.makeText(this, R.string.map_line_deleted, Toast.LENGTH_SHORT).show();
    }

    private void confirmClearAll() {
        if (pins.isEmpty() && lines.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.map_clear_pins)
                .setMessage(R.string.map_confirm_clear)
                .setPositiveButton(R.string.map_delete, (d, w) -> clearAllPins())
                .setNegativeButton(R.string.map_cancel, null)
                .show();
    }

    private void clearAllPins() {
        db.clearMapLines();
        db.clearMapPins();

        for (Polyline p : polylinesByLineId.values()) p.remove();
        polylinesByLineId.clear();
        lines.clear();

        for (Marker m : markersByPinId.values()) m.remove();
        markersByPinId.clear();
        pins.clear();

        selectedPinIds.clear();
        Toast.makeText(this, R.string.map_pins_cleared, Toast.LENGTH_SHORT).show();
    }

    // ---------- Pin creation ----------

    private void promptForNewPin(LatLng latLng) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.map_new_pin_hint);
        input.setText(getString(R.string.map_pin_default_name, pins.size() + 1));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.map_new_pin_title)
                .setView(input)
                .setPositiveButton(R.string.map_save, (DialogInterface d, int which) -> {
                    String label = input.getText().toString().trim();
                    if (label.isEmpty()) {
                        label = getString(R.string.map_pin_default_name, pins.size() + 1);
                    }
                    savePin(label, latLng);
                })
                .setNegativeButton(R.string.map_cancel, null)
                .show();
    }

    private void savePin(String label, LatLng latLng) {
        MapPin pin = new MapPin();
        pin.setLabel(label);
        pin.setLatitude(latLng.latitude);
        pin.setLongitude(latLng.longitude);
        pin.setCreatedAt(System.currentTimeMillis());

        long id = db.insertMapPin(pin);
        if (id < 0) return;
        pin.setId(id);
        pins.add(pin);
        addMarkerForPin(pin);
        Toast.makeText(this, R.string.map_pin_saved, Toast.LENGTH_SHORT).show();
    }

    // ---------- Rendering ----------

    private void renderAllPins() {
        if (map == null) return;
        for (Marker m : markersByPinId.values()) m.remove();
        markersByPinId.clear();
        for (MapPin pin : pins) addMarkerForPin(pin);
    }

    private void renderAllLines() {
        if (map == null) return;
        for (Polyline p : polylinesByLineId.values()) p.remove();
        polylinesByLineId.clear();
        for (MapLine ln : lines) addPolylineForLine(ln);
    }

    private void addMarkerForPin(MapPin pin) {
        if (map == null) return;
        Marker m = map.addMarker(new MarkerOptions()
                .position(new LatLng(pin.getLatitude(), pin.getLongitude()))
                .title(pin.getLabel())
                .icon(BitmapDescriptorFactory.defaultMarker(HUE_NORMAL)));
        if (m != null) {
            m.setTag(pin.getId());
            markersByPinId.put(pin.getId(), m);
            applyMarkerIcon(pin.getId(), selectedPinIds.contains(pin.getId()));
        }
    }

    private void addPolylineForLine(MapLine line) {
        if (map == null) return;
        MapPin a = findPin(line.getPinAId());
        MapPin b = findPin(line.getPinBId());
        if (a == null || b == null) return;

        Polyline p = map.addPolyline(new PolylineOptions()
                .add(new LatLng(a.getLatitude(), a.getLongitude()))
                .add(new LatLng(b.getLatitude(), b.getLongitude()))
                .color(LINE_COLOR)
                .width(6f)
                .clickable(true));
        if (p != null) {
            p.setTag(line.getId());
            p.setVisible(showLinesSwitch.isChecked());
            polylinesByLineId.put(line.getId(), p);
        }
    }

    private MapPin findPin(long id) {
        for (MapPin p : pins) if (p.getId() == id) return p;
        return null;
    }

    private void zoomToPins() {
        if (map == null || pins.isEmpty()) return;
        if (pins.size() == 1) {
            MapPin only = pins.get(0);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(only.getLatitude(), only.getLongitude()), 16f));
            return;
        }
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (MapPin pin : pins) b.include(new LatLng(pin.getLatitude(), pin.getLongitude()));
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 120));
        } catch (Exception ignored) { }
    }
}
