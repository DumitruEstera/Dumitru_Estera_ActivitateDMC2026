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
        e.timestamp = firstNonNullString(json, "detected_at", "timestamp", "created_at");
        e.cameraId = firstNonNullString(json, "camera_id", "camera");
        String status = firstNonNullString(json, "status", "access_status");
        e.status = status.isEmpty() ? "authorized" : status;
        e.confidence = json.optDouble("confidence", 0.0);
        return e;
    }

    private static String firstNonNullString(JSONObject json, String... keys) {
        for (String k : keys) {
            if (json.has(k) && !json.isNull(k)) {
                String v = json.optString(k, "");
                if (!v.isEmpty()) return v;
            }
        }
        return "";
    }

    public String getTimestamp() { return timestamp; }
    public String getCameraId() { return cameraId; }
    public String getStatus() { return status; }
    public double getConfidence() { return confidence; }
}
