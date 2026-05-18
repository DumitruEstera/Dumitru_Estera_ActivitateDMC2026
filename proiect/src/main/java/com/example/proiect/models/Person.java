package com.example.proiect.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Person {
    private int id;
    private String name;
    private String department;
    private String employeeId;
    private int faceCount;
    private List<String> authorizedZones = new ArrayList<>();
    private boolean isFavorite;

    public static Person fromJson(JSONObject json) throws JSONException {
        Person p = new Person();
        p.id = json.optInt("id", 0);
        p.name = optStringOrEmpty(json, "name");
        p.department = optStringOrEmpty(json, "department");
        p.employeeId = json.isNull("employee_id")
                ? optStringOrEmpty(json, "employeeId")
                : json.optString("employee_id", "");
        p.faceCount = json.optInt("face_count",
                json.optInt("faces_count",
                        json.optInt("faces", 0)));

        JSONArray zones = json.optJSONArray("authorized_zones");
        if (zones == null) zones = json.optJSONArray("zones");
        if (zones != null) {
            for (int i = 0; i < zones.length(); i++) {
                Object item = zones.opt(i);
                if (item instanceof String) {
                    p.authorizedZones.add((String) item);
                } else if (item instanceof JSONObject) {
                    JSONObject zo = (JSONObject) item;
                    String name = zo.optString("name", zo.optString("zone_name", ""));
                    if (!name.isEmpty()) p.authorizedZones.add(name);
                }
            }
        }
        return p;
    }

    private static String optStringOrEmpty(JSONObject json, String key) {
        if (json.isNull(key)) return "";
        return json.optString(key, "");
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getEmployeeId() { return employeeId; }
    public int getFaceCount() { return faceCount; }
    public List<String> getAuthorizedZones() { return authorizedZones; }
    public boolean isFavorite() { return isFavorite; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setFaceCount(int faceCount) { this.faceCount = faceCount; }
    public void setAuthorizedZones(List<String> authorizedZones) {
        this.authorizedZones = authorizedZones == null ? new ArrayList<>() : authorizedZones;
    }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
}
