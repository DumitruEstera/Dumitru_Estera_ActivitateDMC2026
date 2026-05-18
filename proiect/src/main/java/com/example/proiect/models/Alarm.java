package com.example.proiect.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Alarm {
    private int id;
    private String type;
    private String severity;
    private String description;
    private String cameraId;
    private String status;
    private String notes;
    private String snapshot;
    private String metadataJson;
    private String createdAt;
    private String resolvedAt;
    private String resolvedBy;
    private int localRating;

    public static Alarm fromJson(JSONObject json) throws JSONException {
        Alarm a = new Alarm();
        a.id = json.optInt("id", 0);
        a.type = json.optString("type", "");
        a.severity = json.optString("severity", "");
        a.description = optStringOrEmpty(json, "description");
        a.cameraId = json.optString("camera_id", "");
        a.status = json.optString("status", "");
        a.notes = optStringOrEmpty(json, "notes");
        a.snapshot = json.optString("snapshot", null);
        a.createdAt = json.optString("created_at", "");
        a.resolvedAt = json.optString("resolved_at", "");
        a.resolvedBy = optStringOrEmpty(json, "resolved_by");
        JSONObject meta = json.optJSONObject("detection_metadata");
        if (meta == null) meta = json.optJSONObject("metadata");
        if (meta != null) {
            a.metadataJson = meta.toString();
        }
        return a;
    }

    private static String optStringOrEmpty(JSONObject json, String key) {
        if (json.isNull(key)) return "";
        return json.optString(key, "");
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getCameraId() { return cameraId; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getSnapshot() { return snapshot; }
    public String getMetadataJson() { return metadataJson; }
    public String getCreatedAt() { return createdAt; }
    public String getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public int getLocalRating() { return localRating; }

    public void setId(int id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setDescription(String description) { this.description = description; }
    public void setCameraId(String cameraId) { this.cameraId = cameraId; }
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public void setLocalRating(int localRating) { this.localRating = localRating; }
}
