package com.example.proiect.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiect.R;
import com.example.proiect.adapters.LiveCameraAdapter;
import com.example.proiect.api.ApiClient;
import com.example.proiect.api.LiveStreamClient;
import com.example.proiect.models.LiveCamera;
import com.example.proiect.utils.PrefsManager;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraStreamActivity extends AppCompatActivity implements LiveStreamClient.Listener {

    private TextView connectionStatus;
    private View connectionDot;
    private RecyclerView list;
    private TextView empty;

    private LiveCameraAdapter adapter;
    private final List<LiveCamera> cameras = new ArrayList<>();
    private final Map<String, Integer> indexById = new HashMap<>();

    private PrefsManager prefs;
    private LiveStreamClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Runnable staleTick = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) adapter.notifyDataSetChanged();
            mainHandler.postDelayed(this, 2000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsManager(this);
        setContentView(R.layout.activity_camera_stream);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        connectionStatus = findViewById(R.id.live_connection_status);
        connectionDot = findViewById(R.id.live_connection_dot);
        list = findViewById(R.id.live_camera_list);
        empty = findViewById(R.id.live_empty);

        int span = getResources().getConfiguration().screenWidthDp >= 600 ? 2 : 1;
        list.setLayoutManager(new GridLayoutManager(this, span));
        adapter = new LiveCameraAdapter(this, cameras, cam -> {
            Toast.makeText(this, cam.cameraId, Toast.LENGTH_SHORT).show();
        });
        list.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        seedCameraList();
        client = new LiveStreamClient(prefs.getServerUrl(), this);
        client.connect();
        mainHandler.postDelayed(staleTick, 2000L);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mainHandler.removeCallbacks(staleTick);
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
    }

    private void seedCameraList() {
        final String baseUrl = prefs.getServerUrl();
        final String token = prefs.getAuthToken();
        bgExecutor.execute(() -> {
            try {
                ApiClient api = new ApiClient(baseUrl, token);
                ApiClient.ApiResponse resp = api.get("/api/cameras/list");
                if (!resp.isSuccess()) return;
                final List<LiveCamera> seeded = parseCameras(resp.body);
                mainHandler.post(() -> {
                    for (LiveCamera c : seeded) {
                        if (!indexById.containsKey(c.cameraId)) {
                            indexById.put(c.cameraId, cameras.size());
                            cameras.add(c);
                        }
                    }
                    refreshEmpty();
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception ignored) { }
        });
    }

    private static List<LiveCamera> parseCameras(String body) {
        List<LiveCamera> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;
        try {
            JSONArray array;
            String trimmed = body.trim();
            if (trimmed.startsWith("[")) {
                array = new JSONArray(trimmed);
            } else {
                JSONObject obj = new JSONObject(trimmed);
                if (obj.has("cameras")) array = obj.getJSONArray("cameras");
                else if (obj.has("data")) array = obj.getJSONArray("data");
                else return out;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String id = item.optString("camera_id",
                        item.optString("id", ""));
                if (id.isEmpty()) continue;
                LiveCamera c = new LiveCamera(id);
                c.location = item.optString("location", "");
                c.type = item.optString("type", "");
                c.active = item.optBoolean("active",
                        item.optBoolean("is_active", false));
                out.add(c);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private void refreshEmpty() {
        empty.setVisibility(cameras.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onFrame(String cameraId, byte[] jpegBytes, JSONObject fullMessage) {
        Bitmap bmp;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length, opts);
        } catch (Throwable t) {
            return;
        }
        if (bmp == null) return;
        final Bitmap finalBmp = bmp;
        mainHandler.post(() -> {
            Integer idx = indexById.get(cameraId);
            LiveCamera cam;
            if (idx == null) {
                cam = new LiveCamera(cameraId);
                indexById.put(cameraId, cameras.size());
                cameras.add(cam);
                refreshEmpty();
                adapter.notifyItemInserted(cameras.size() - 1);
            } else {
                cam = cameras.get(idx);
            }
            cam.latestFrame = finalBmp;
            cam.lastFrameAtMs = System.currentTimeMillis();
            cam.active = true;
            Integer pos = indexById.get(cameraId);
            if (pos != null) adapter.notifyItemChanged(pos);
        });
    }

    @Override
    public void onAlertHints(String cameraId, JSONArray faces, JSONArray fire,
                             JSONArray weapon, JSONArray har, JSONArray plates) {
        final String hint = buildHint(faces, fire, weapon, har, plates);
        if (hint == null) return;
        mainHandler.post(() -> {
            Integer idx = indexById.get(cameraId);
            if (idx == null) return;
            cameras.get(idx).latestHint = hint;
            adapter.notifyItemChanged(idx);
        });
    }

    private static String buildHint(JSONArray faces, JSONArray fire,
                                    JSONArray weapon, JSONArray har, JSONArray plates) {
        StringBuilder sb = new StringBuilder();
        appendCount(sb, "FACE", faces);
        appendCount(sb, "FIRE", fire);
        appendCount(sb, "WEAPON", weapon);
        appendCount(sb, "HAR", har);
        appendCount(sb, "PLATE", plates);
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void appendCount(StringBuilder sb, String label, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(String.format(Locale.US, "%s ×%d", label, arr.length()));
    }

    @Override
    public void onConnectionChanged(boolean connected) {
        mainHandler.post(() -> {
            if (connected) {
                connectionStatus.setText(R.string.live_status_live);
                connectionDot.setBackgroundColor(0xFF43A047);
            } else {
                connectionStatus.setText(R.string.live_status_offline);
                connectionDot.setBackgroundColor(0xFFC62828);
            }
        });
    }
}
