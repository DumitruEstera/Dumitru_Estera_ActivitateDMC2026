package com.example.proiect.models;

public class MapLine {
    private long id;
    private long pinAId;
    private long pinBId;
    private long createdAt;

    public MapLine() { }

    public MapLine(long id, long pinAId, long pinBId, long createdAt) {
        this.id = id;
        this.pinAId = pinAId;
        this.pinBId = pinBId;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPinAId() { return pinAId; }
    public void setPinAId(long pinAId) { this.pinAId = pinAId; }

    public long getPinBId() { return pinBId; }
    public void setPinBId(long pinBId) { this.pinBId = pinBId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
