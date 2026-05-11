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
        a.description = json.optString("description", "");
        a.cameraId = json.optString("camera_id", "");
        a.status = json.optString("status", "");
        a.notes = json.optString("notes", "");
        a.snapshot = json.optString("snapshot", null);
        a.createdAt = json.optString("created_at", "");
        a.resolvedAt = json.optString("resolved_at", "");
        a.resolvedBy = json.optString("resolved_by", "");
        if (json.has("metadata")) {
            a.metadataJson = json.optJSONObject("metadata") != null
                    ? json.getJSONObject("metadata").toString()
                    : json.optString("metadata", "");
        }
        return a;
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
