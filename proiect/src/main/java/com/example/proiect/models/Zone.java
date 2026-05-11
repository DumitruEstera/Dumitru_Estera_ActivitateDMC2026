package com.example.proiect.models;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Zone {
    private int id;
    private String name;
    private String description;
    private boolean isRestricted;
    private final List<LatLng> polygon = new ArrayList<>();

    public static Zone fromJson(JSONObject json) throws JSONException {
        Zone z = new Zone();
        z.id = json.optInt("id", 0);
        z.name = json.optString("name", "");
        z.description = json.optString("description", "");
        z.isRestricted = json.optBoolean("is_restricted",
                json.optBoolean("restricted", false));

        JSONArray poly = json.optJSONArray("polygon");
        if (poly == null) poly = json.optJSONArray("coordinates");
        if (poly != null) {
            for (int i = 0; i < poly.length(); i++) {
                Object item = poly.opt(i);
                if (item instanceof JSONObject) {
                    JSONObject p = (JSONObject) item;
                    double lat = p.optDouble("latitude", p.optDouble("lat", Double.NaN));
                    double lng = p.optDouble("longitude",
                            p.optDouble("lng", p.optDouble("lon", Double.NaN)));
                    if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                        z.polygon.add(new LatLng(lat, lng));
                    }
                } else if (item instanceof JSONArray) {
                    JSONArray pair = (JSONArray) item;
                    if (pair.length() >= 2) {
                        double a = pair.optDouble(0, Double.NaN);
                        double b = pair.optDouble(1, Double.NaN);
                        if (!Double.isNaN(a) && !Double.isNaN(b)) {
                            z.polygon.add(new LatLng(b, a));
                        }
                    }
                }
            }
        }
        return z;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isRestricted() { return isRestricted; }
    public List<LatLng> getPolygon() { return polygon; }
}
