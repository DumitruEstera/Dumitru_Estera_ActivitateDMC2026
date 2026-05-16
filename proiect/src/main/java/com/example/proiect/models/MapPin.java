package com.example.proiect.models;

public class MapPin {
    private long id;
    private String label;
    private double latitude;
    private double longitude;
    private long createdAt;

    public MapPin() { }

    public MapPin(long id, String label, double latitude, double longitude, long createdAt) {
        this.id = id;
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
