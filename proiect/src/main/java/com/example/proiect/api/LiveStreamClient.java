package com.example.proiect.api;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class LiveStreamClient {

    public interface Listener {
        void onFrame(String cameraId, byte[] jpegBytes, JSONObject fullMessage);
        void onAlertHints(String cameraId, JSONArray faces, JSONArray fire,
                          JSONArray weapon, JSONArray har, JSONArray plates);
        void onConnectionChanged(boolean connected);
    }

    private static final String TAG = "LiveStreamClient";
    private static final long[] BACKOFF_MS = { 1000L, 2000L, 5000L, 10000L };

    private final String wsUrl;
    private final Listener listener;
    private final OkHttpClient http;
    private final HandlerThread workerThread;
    private final Handler worker;
    private final AtomicBoolean shouldRun = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private volatile WebSocket socket;
    private int attempt = 0;

    public LiveStreamClient(String serverBaseUrl, Listener listener) {
        this.wsUrl = toWsUrl(serverBaseUrl);
        this.listener = listener;
        this.http = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        this.workerThread = new HandlerThread("LiveStream-Worker");
        this.workerThread.start();
        this.worker = new Handler(workerThread.getLooper());
    }

    private static String toWsUrl(String base) {
        if (base == null || base.isEmpty()) return "";
        String trimmed = base.replaceAll("/+$", "");
        if (trimmed.startsWith("https://")) {
            return "wss://" + trimmed.substring(8) + "/ws";
        }
        if (trimmed.startsWith("http://")) {
            return "ws://" + trimmed.substring(7) + "/ws";
        }
        return "ws://" + trimmed + "/ws";
    }

    public void connect() {
        if (shouldRun.getAndSet(true)) return;
        attempt = 0;
        worker.post(this::openSocket);
    }

    public void disconnect() {
        if (!shouldRun.getAndSet(false)) return;
        worker.post(() -> {
            if (socket != null) {
                socket.close(1000, "client-disconnect");
                socket = null;
            }
            updateConnected(false);
        });
    }

    public void shutdown() {
        disconnect();
        workerThread.quitSafely();
    }

    private void openSocket() {
        if (!shouldRun.get()) return;
        if (wsUrl.isEmpty()) return;
        Request req = new Request.Builder().url(wsUrl).build();
        socket = http.newWebSocket(req, new Listener0());
    }

    private void scheduleReconnect() {
        if (!shouldRun.get()) return;
        long delay = BACKOFF_MS[Math.min(attempt, BACKOFF_MS.length - 1)];
        attempt++;
        worker.postDelayed(this::openSocket, delay);
    }

    private void updateConnected(boolean state) {
        boolean prev = connected.getAndSet(state);
        if (prev != state) {
            listener.onConnectionChanged(state);
        }
    }

    private void handleMessage(String text) {
        try {
            JSONObject msg = new JSONObject(text);
            String type = msg.optString("type", "");
            if (!"video_frame".equals(type)) return;
            String cameraId = msg.optString("camera_id", "");
            String frameB64 = msg.optString("frame", "");
            if (cameraId.isEmpty() || frameB64.isEmpty()) return;
            byte[] jpeg;
            try {
                jpeg = Base64.decode(frameB64, Base64.DEFAULT);
            } catch (IllegalArgumentException bad) {
                return;
            }
            listener.onFrame(cameraId, jpeg, msg);

            JSONArray faces = msg.optJSONArray("face_results");
            JSONArray fire = msg.optJSONArray("fire_results");
            JSONArray weapon = msg.optJSONArray("weapon_results");
            JSONArray har = msg.optJSONArray("har_results");
            JSONArray plates = msg.optJSONArray("plate_results");
            if (faces != null || fire != null || weapon != null || har != null || plates != null) {
                listener.onAlertHints(cameraId, faces, fire, weapon, har, plates);
            }
        } catch (Exception e) {
            Log.w(TAG, "bad ws message", e);
        }
    }

    private final class Listener0 extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            attempt = 0;
            updateConnected(true);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            handleMessage(text);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            webSocket.close(1000, null);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            updateConnected(false);
            scheduleReconnect();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            Log.w(TAG, "ws failure: " + t.getMessage());
            updateConnected(false);
            scheduleReconnect();
        }
    }
}
