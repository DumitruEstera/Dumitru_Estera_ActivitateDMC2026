package com.example.proiect.models;

import android.graphics.Bitmap;

public class LiveCamera {
    public String cameraId;
    public String location;
    public String type;
    public boolean active;
    public long lastFrameAtMs;
    public Bitmap latestFrame;
    public String latestHint;

    public LiveCamera(String cameraId) {
        this.cameraId = cameraId;
        this.location = "";
        this.type = "";
        this.active = false;
        this.lastFrameAtMs = 0L;
    }

    public boolean isStale(long nowMs, long thresholdMs) {
        return lastFrameAtMs > 0 && (nowMs - lastFrameAtMs) > thresholdMs;
    }
}
