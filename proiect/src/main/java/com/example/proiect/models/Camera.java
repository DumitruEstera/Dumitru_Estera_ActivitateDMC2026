package com.example.proiect.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Camera {
    private String cameraId;
    private String name;
    private String location;
    private double latitude;
    private double longitude;
    private boolean hasCoordinates;
    private int zoneId;
    private String zoneName;
    private boolean isRestricted;

    public static Camera fromJson(JSONObject json) throws JSONException {
        Camera c = new Camera();
        c.cameraId = json.optString("camera_id",
                json.optString("id", json.optString("name", "")));
        c.name = json.optString("name",
                json.optString("camera_name", c.cameraId));
        c.location = json.optString("location", "");
        c.zoneId = json.optInt("zone_id", 0);
        c.zoneName = json.optString("zone_name",
                json.optString("zone", ""));
        c.isRestricted = json.optBoolean("is_restricted",
                json.optBoolean("restricted", false));

        boolean hasLat = json.has("latitude") || json.has("lat");
        boolean hasLng = json.has("longitude") || json.has("lng")
                || json.has("lon") || json.has("long");
        if (hasLat && hasLng) {
            c.latitude = json.optDouble("latitude", json.optDouble("lat", 0));
            c.longitude = json.optDouble("longitude",
                    json.optDouble("lng",
                            json.optDouble("lon",
                                    json.optDouble("long", 0))));
            c.hasCoordinates = true;
        }
        return c;
    }

    public String getCameraId() { return cameraId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean hasCoordinates() { return hasCoordinates; }
    public int getZoneId() { return zoneId; }
    public String getZoneName() { return zoneName; }
    public boolean isRestricted() { return isRestricted; }
}
