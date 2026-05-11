package com.example.proiect.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.proiect.models.AccessLogEntry;
import com.example.proiect.models.Alarm;
import com.example.proiect.models.Camera;
import com.example.proiect.models.Person;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "securityguard_cache.db";
    public static final int DB_VERSION = 1;

    public static final String T_ALARMS = "alarms";
    public static final String T_PERSONS = "persons";
    public static final String T_LOGS = "access_logs";
    public static final String T_CAMERAS = "cameras";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper get(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_ALARMS + " ("
                + "id INTEGER PRIMARY KEY,"
                + "type TEXT,"
                + "severity TEXT,"
                + "description TEXT,"
                + "camera_id TEXT,"
                + "status TEXT,"
                + "notes TEXT,"
                + "snapshot_b64 TEXT,"
                + "metadata_json TEXT,"
                + "created_at TEXT,"
                + "resolved_at TEXT,"
                + "resolved_by TEXT,"
                + "local_rating INTEGER DEFAULT 0,"
                + "synced INTEGER DEFAULT 1)");

        db.execSQL("CREATE TABLE " + T_PERSONS + " ("
                + "id INTEGER PRIMARY KEY,"
                + "name TEXT,"
                + "department TEXT,"
                + "employee_id TEXT,"
                + "face_count INTEGER,"
                + "authorized_zones TEXT,"
                + "is_favorite INTEGER DEFAULT 0,"
                + "synced INTEGER DEFAULT 1)");

        db.execSQL("CREATE TABLE " + T_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "person_id INTEGER,"
                + "camera_id TEXT,"
                + "timestamp TEXT,"
                + "status TEXT)");

        db.execSQL("CREATE TABLE " + T_CAMERAS + " ("
                + "camera_id TEXT PRIMARY KEY,"
                + "name TEXT,"
                + "location TEXT,"
                + "latitude REAL,"
                + "longitude REAL,"
                + "zone_id INTEGER,"
                + "zone_name TEXT,"
                + "is_restricted INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String t : new String[] { T_ALARMS, T_PERSONS, T_LOGS, T_CAMERAS }) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
        }
        onCreate(db);
    }

    public synchronized void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String t : new String[] { T_ALARMS, T_PERSONS, T_LOGS, T_CAMERAS }) {
                db.delete(t, null, null);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---------- Alarms ----------

    public synchronized void replaceAlarms(List<Alarm> alarms) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Map<Integer, Integer> ratings = new HashMap<>();
            Cursor c = db.rawQuery(
                    "SELECT id, local_rating FROM " + T_ALARMS, null);
            try {
                while (c.moveToNext()) ratings.put(c.getInt(0), c.getInt(1));
            } finally { c.close(); }

            db.delete(T_ALARMS, null, null);
            for (Alarm a : alarms) {
                ContentValues cv = alarmToValues(a);
                Integer prev = ratings.get(a.getId());
                cv.put("local_rating", prev == null ? 0 : prev);
                db.insertWithOnConflict(T_ALARMS, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void upsertAlarm(Alarm a) {
        SQLiteDatabase db = getWritableDatabase();
        int existingRating = getAlarmRating(a.getId());
        ContentValues cv = alarmToValues(a);
        cv.put("local_rating", existingRating);
        db.insertWithOnConflict(T_ALARMS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized List<Alarm> getAllAlarms() {
        SQLiteDatabase db = getReadableDatabase();
        List<Alarm> out = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT id,type,severity,description,camera_id,status,notes,"
                        + "snapshot_b64,metadata_json,created_at,resolved_at,"
                        + "resolved_by,local_rating FROM " + T_ALARMS
                        + " ORDER BY created_at DESC", null);
        try {
            while (c.moveToNext()) out.add(alarmFromCursor(c));
        } finally { c.close(); }
        return out;
    }

    public synchronized Alarm getAlarmById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id,type,severity,description,camera_id,status,notes,"
                        + "snapshot_b64,metadata_json,created_at,resolved_at,"
                        + "resolved_by,local_rating FROM " + T_ALARMS
                        + " WHERE id=?", new String[] { String.valueOf(id) });
        try {
            if (c.moveToFirst()) return alarmFromCursor(c);
        } finally { c.close(); }
        return null;
    }

    public synchronized int getAlarmRating(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT local_rating FROM " + T_ALARMS + " WHERE id=?",
                new String[] { String.valueOf(id) });
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally { c.close(); }
    }

    public synchronized void setAlarmRating(int id, int rating) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("local_rating", rating);
        int n = db.update(T_ALARMS, cv, "id=?",
                new String[] { String.valueOf(id) });
        if (n == 0) {
            cv.put("id", id);
            db.insertWithOnConflict(T_ALARMS, null, cv,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public synchronized void updateAlarmStatus(int id, String status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        db.update(T_ALARMS, cv, "id=?", new String[] { String.valueOf(id) });
    }

    public synchronized void updateAlarmNotes(int id, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("notes", notes);
        db.update(T_ALARMS, cv, "id=?", new String[] { String.valueOf(id) });
    }

    // ---------- Persons ----------

    public synchronized void replacePersons(List<Person> persons) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Map<Integer, Integer> favorites = new HashMap<>();
            Cursor c = db.rawQuery(
                    "SELECT id, is_favorite FROM " + T_PERSONS, null);
            try {
                while (c.moveToNext()) favorites.put(c.getInt(0), c.getInt(1));
            } finally { c.close(); }

            db.delete(T_PERSONS, null, null);
            for (Person p : persons) {
                ContentValues cv = personToValues(p);
                Integer prev = favorites.get(p.getId());
                cv.put("is_favorite", prev == null ? 0 : prev);
                db.insertWithOnConflict(T_PERSONS, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void upsertPerson(Person p) {
        SQLiteDatabase db = getWritableDatabase();
        int fav = isPersonFavorite(p.getId()) ? 1 : 0;
        ContentValues cv = personToValues(p);
        cv.put("is_favorite", fav);
        db.insertWithOnConflict(T_PERSONS, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized List<Person> getAllPersons() {
        SQLiteDatabase db = getReadableDatabase();
        List<Person> out = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT id,name,department,employee_id,face_count,"
                        + "authorized_zones,is_favorite FROM " + T_PERSONS
                        + " ORDER BY name COLLATE NOCASE ASC", null);
        try {
            while (c.moveToNext()) out.add(personFromCursor(c));
        } finally { c.close(); }
        return out;
    }

    public synchronized boolean isPersonFavorite(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT is_favorite FROM " + T_PERSONS + " WHERE id=?",
                new String[] { String.valueOf(id) });
        try {
            return c.moveToFirst() && c.getInt(0) != 0;
        } finally { c.close(); }
    }

    public synchronized void setPersonFavorite(int id, boolean favorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_favorite", favorite ? 1 : 0);
        int n = db.update(T_PERSONS, cv, "id=?",
                new String[] { String.valueOf(id) });
        if (n == 0) {
            cv.put("id", id);
            db.insertWithOnConflict(T_PERSONS, null, cv,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ---------- Access logs ----------

    public synchronized void replaceAccessLogs(int personId, List<AccessLogEntry> logs) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(T_LOGS, "person_id=?",
                    new String[] { String.valueOf(personId) });
            for (AccessLogEntry e : logs) {
                ContentValues cv = new ContentValues();
                cv.put("person_id", personId);
                cv.put("camera_id", e.getCameraId());
                cv.put("timestamp", e.getTimestamp());
                cv.put("status", e.getStatus());
                db.insert(T_LOGS, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized List<AccessLogEntry> getAccessLogsFor(int personId) {
        SQLiteDatabase db = getReadableDatabase();
        List<AccessLogEntry> out = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT camera_id, timestamp, status FROM " + T_LOGS
                        + " WHERE person_id=? ORDER BY timestamp DESC",
                new String[] { String.valueOf(personId) });
        try {
            while (c.moveToNext()) {
                out.add(buildAccessLog(c.getString(0), c.getString(1), c.getString(2)));
            }
        } finally { c.close(); }
        return out;
    }

    // ---------- Cameras ----------

    public synchronized void replaceCameras(List<Camera> cameras) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(T_CAMERAS, null, null);
            for (Camera cam : cameras) {
                ContentValues cv = new ContentValues();
                cv.put("camera_id", cam.getCameraId());
                cv.put("name", cam.getName());
                cv.put("location", cam.getLocation());
                cv.put("latitude", cam.getLatitude());
                cv.put("longitude", cam.getLongitude());
                cv.put("zone_id", cam.getZoneId());
                cv.put("zone_name", cam.getZoneName());
                cv.put("is_restricted", cam.isRestricted() ? 1 : 0);
                db.insertWithOnConflict(T_CAMERAS, null, cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---------- helpers ----------

    private static ContentValues alarmToValues(Alarm a) {
        ContentValues cv = new ContentValues();
        cv.put("id", a.getId());
        cv.put("type", a.getType());
        cv.put("severity", a.getSeverity());
        cv.put("description", a.getDescription());
        cv.put("camera_id", a.getCameraId());
        cv.put("status", a.getStatus());
        cv.put("notes", a.getNotes());
        cv.put("snapshot_b64", a.getSnapshot());
        cv.put("metadata_json", a.getMetadataJson());
        cv.put("created_at", a.getCreatedAt());
        cv.put("resolved_at", a.getResolvedAt());
        cv.put("resolved_by", a.getResolvedBy());
        cv.put("synced", 1);
        return cv;
    }

    private static Alarm alarmFromCursor(Cursor c) {
        Alarm a = new Alarm();
        a.setId(c.getInt(0));
        a.setType(c.getString(1));
        a.setSeverity(c.getString(2));
        a.setDescription(c.getString(3));
        a.setCameraId(c.getString(4));
        a.setStatus(c.getString(5));
        a.setNotes(c.getString(6));
        a.setSnapshot(c.getString(7));
        a.setMetadataJson(c.getString(8));
        a.setCreatedAt(c.getString(9));
        a.setResolvedAt(c.getString(10));
        a.setResolvedBy(c.getString(11));
        a.setLocalRating(c.getInt(12));
        return a;
    }

    private static ContentValues personToValues(Person p) {
        ContentValues cv = new ContentValues();
        cv.put("id", p.getId());
        cv.put("name", p.getName());
        cv.put("department", p.getDepartment());
        cv.put("employee_id", p.getEmployeeId());
        cv.put("face_count", p.getFaceCount());
        cv.put("authorized_zones",
                new JSONArray(p.getAuthorizedZones() == null
                        ? new ArrayList<String>() : p.getAuthorizedZones()).toString());
        cv.put("synced", 1);
        return cv;
    }

    private static Person personFromCursor(Cursor c) {
        Person p = new Person();
        p.setId(c.getInt(0));
        p.setName(c.getString(1));
        p.setDepartment(c.getString(2));
        p.setEmployeeId(c.getString(3));
        p.setFaceCount(c.getInt(4));
        String zonesJson = c.getString(5);
        List<String> zones = new ArrayList<>();
        if (zonesJson != null && !zonesJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(zonesJson);
                for (int i = 0; i < arr.length(); i++) {
                    zones.add(arr.optString(i, ""));
                }
            } catch (Exception ignored) { }
        }
        p.setAuthorizedZones(zones);
        p.setFavorite(c.getInt(6) != 0);
        return p;
    }

    private static AccessLogEntry buildAccessLog(String cameraId, String timestamp, String status) {
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("camera_id", cameraId == null ? "" : cameraId);
            o.put("timestamp", timestamp == null ? "" : timestamp);
            o.put("status", status == null ? "" : status);
            return AccessLogEntry.fromJson(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
