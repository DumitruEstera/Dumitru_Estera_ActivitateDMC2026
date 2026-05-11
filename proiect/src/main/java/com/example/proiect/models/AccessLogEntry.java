package com.example.proiect.models;

import org.json.JSONException;
import org.json.JSONObject;

public class AccessLogEntry {
    private String timestamp;
    private String cameraId;
    private String status;
    private double confidence;

    public static AccessLogEntry fromJson(JSONObject json) throws JSONException {
        AccessLogEntry e = new AccessLogEntry();
        e.timestamp = json.optString("timestamp",
                json.optString("created_at", ""));
        e.cameraId = json.optString("camera_id",
                json.optString("camera", ""));
        e.status = json.optString("status",
                json.optString("access_status", ""));
        e.confidence = json.optDouble("confidence", 0.0);
        return e;
    }

    public String getTimestamp() { return timestamp; }
    public String getCameraId() { return cameraId; }
    public String getStatus() { return status; }
    public double getConfidence() { return confidence; }
}
