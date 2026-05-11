# SecurityGuard Mobile — Android Companion App

## Project Overview

**SecurityGuard Mobile** is an Android companion application for the Integrated Security Surveillance System (the bachelor's degree project). It allows security guards and administrators to monitor alarms, manage persons and zones, view camera locations on a map, and analyze detection statistics — all from their phone.

The app connects to the existing **FastAPI backend** via its REST API and displays data using native Android components in **Java (Android Studio)**.

---

## Minimum Requirements Checklist

|#|Requirement|How It Is Fulfilled|
|---|---|---|
|1|Minimum 5 Activities|9 activities: Login, Dashboard, Alarms List, Alarm Detail, Persons List, Person Detail, Camera Map, Statistics, Settings|
|2|Simple visual controls|TextView, EditText, Spinner, Button, CheckBox, ProgressBar, RatingBar, Switch|
|3|Complex visual controls|ListView, DatePicker, TimePicker, CalendarView|
|4|Custom adapter for ListView|Custom `AlarmAdapter` and `PersonAdapter` with multi-element row layouts|
|5|SharedPreferences|JWT token, notification toggles, filter preferences, theme setting|
|6|SQLite local database|Offline cache for alarms, persons, and access logs|
|7|Remote JSON parsing|Fetching and parsing JSON from all FastAPI endpoints (`/api/alarms`, `/api/persons`, `/api/logs`, etc.)|
|8|Google Maps markers/polygons|Camera positions as markers, restricted zones as polygons, alarm locations as colored pins|
|9|2D graphics / charts|Bar chart (detections per camera), line chart (detections over time), pie/donut chart (alarm type distribution)|

---

## Architecture

```
┌────────────────────────────────────────────┐
│              Android App (Java)            │
│                                            │
│  Activities ←→ Adapters ←→ SQLite Cache    │
│       ↕                                    │
│  ApiService  (Retrofit / HttpURLConnection)│
│       ↕                                    │
│  JSON Parsing (org.json / Gson)            │
└──────────────── ↕ ─────────────────────────┘
                  │  HTTPS
┌──────────────── ↕ ─────────────────────────┐
│       FastAPI Backend (existing)            │
│   /api/login, /api/alarms, /api/persons,   │
│   /api/zones, /api/cameras-db,             │
│   /api/logs, /api/statistics, etc.         │
└────────────────────────────────────────────┘
```

---

## Activity Descriptions

### 1. LoginActivity

**Purpose:** Authenticate the user against the backend and obtain a JWT token.

**Layout — `activity_login.xml`:**

- `ImageView` — app logo / security shield icon
- `TextView` — app title "SecurityGuard Mobile"
- `EditText` — username input (`android:inputType="text"`)
- `EditText` — password input (`android:inputType="textPassword"`)
- `Button` — "Login"
- `ProgressBar` — shown while the login request is in flight
- `TextView` — error message area (hidden by default)

**Functionality:**

- On button press, send a `POST` request to `/api/login` with `{ "username": "...", "password": "..." }`.
- Parse the JSON response to extract the JWT `token`.
- Save the token and username in **SharedPreferences**.
- On success, navigate to `DashboardActivity` with `Intent` and `finish()` the login screen.
- On failure, display the error message in the `TextView`.
- On app start, check SharedPreferences for an existing token; if found, skip login and go directly to the dashboard.

**Controls used:** EditText, Button, ProgressBar, TextView, ImageView

---

### 2. DashboardActivity

**Purpose:** Central hub showing a summary of the system status — unresolved alarm count, recent critical alerts, and quick-access buttons.

**Layout — `activity_dashboard.xml`:**

- `TextView` — greeting: "Hello, {username}" (from SharedPreferences)
- **Summary cards** (CardView or LinearLayout):
    - `TextView` + `TextView` — "Unresolved Alarms: {count}" (fetched from `/api/alarms/stats`)
    - `TextView` + `TextView` — "Critical Alerts: {count}"
    - `TextView` + `TextView` — "Total Detections (24h): {count}" (from `/api/logs/stats`)
    - `TextView` + `TextView` — "Registered Persons: {count}" (from `/api/statistics`)
- **Recent Alarms section:**
    - `ListView` — shows the 5 most recent unresolved alarms (custom adapter, see section 4)
- **Navigation buttons** (Button or ImageButton):
    - "All Alarms" → `AlarmsListActivity`
    - "Persons" → `PersonsListActivity`
    - "Camera Map" → `CameraMapActivity`
    - "Statistics" → `StatisticsActivity`
    - "Settings" → `SettingsActivity`
- `ProgressBar` — shown while loading dashboard data
- **Bottom Navigation or menu** for quick switching

**Functionality:**

- On `onCreate`, fetch data from three endpoints in parallel (or sequentially):
    - `GET /api/alarms/stats` → alarm counts
    - `GET /api/logs/stats?hours=24` → detection counts
    - `GET /api/statistics` → registered persons, cameras, etc.
    - `GET /api/alarms?status=unresolved&limit=5` → recent alarms for the ListView
- All requests include the `Authorization: Bearer {token}` header (token from SharedPreferences).
- Parse each JSON response and update the corresponding TextViews.
- Tapping a recent alarm opens `AlarmDetailActivity` via Intent with the alarm ID as an extra.
- Tapping a navigation button opens the corresponding Activity.

**Controls used:** TextView, Button, ListView (with custom adapter), ProgressBar, CardView

---

### 3. AlarmsListActivity

**Purpose:** Full list of alarms with filtering capabilities.

**Layout — `activity_alarms_list.xml`:**

- **Filter bar (horizontal LinearLayout):**
    - `Spinner` — filter by status: "All", "Unresolved", "Resolved", "False Alarm"
    - `Spinner` — filter by type: "All", "Face", "Fire", "Weapon", "HAR", "Unauthorized Zone"
    - `Spinner` — filter by severity: "All", "Critical", "High", "Medium", "Low"
- `Button` — "Filter by Date" → opens a `DatePickerDialog`
- `ListView` — alarm list with custom `AlarmAdapter`
- `ProgressBar` — shown while loading
- `TextView` — "No alarms found" (shown when list is empty)

**Functionality:**

- On load, call `GET /api/alarms?limit=50` and populate the ListView.
- When a Spinner selection changes, re-fetch with the selected filters as query parameters:
    - `GET /api/alarms?status={status}&type={type}&severity={severity}&date_from={date}&limit=50`
- "Filter by Date" button opens a **DatePickerDialog**; after picking a date, adds `date_from` and `date_to` to the query.
- Save the last selected filter values in **SharedPreferences** so they persist.
- Tapping an alarm row opens `AlarmDetailActivity` with the alarm `id` passed as an Intent extra.
- Pull-to-refresh with `SwipeRefreshLayout`.
- Cache fetched alarms in **SQLite** for offline viewing.

**Controls used:** Spinner (×3), Button, ListView, ProgressBar, TextView, DatePicker (dialog)

---

### 4. AlarmDetailActivity

**Purpose:** Show full details of a single alarm and allow the guard to resolve it or mark it as a false alarm.

**Layout — `activity_alarm_detail.xml`:**

- `TextView` — alarm type (e.g., "WEAPON DETECTED") with colored background
- `TextView` — severity badge (Critical / High / Medium / Low)
- `TextView` — timestamp
- `TextView` — camera ID
- `TextView` — description text
- `ImageView` — alarm snapshot (base64-decoded from the API response)
- `TextView` — "Detection Metadata" section header
- Multiple `TextView` pairs — key-value metadata (confidence, class, bbox, zone, etc.)
- `EditText` — notes field (multiline, for the guard to add comments)
- `RatingBar` — guard's assessment of alarm importance (1-5 stars, stored locally)
- **Action buttons:**
    - `Button` — "Mark Resolved" → `PATCH /api/alarms/{id}` with `{ "status": "resolved" }`
    - `Button` — "False Alarm" → `PATCH /api/alarms/{id}` with `{ "status": "false_alarm" }`
    - `Button` — "Save Notes" → `PATCH /api/alarms/{id}` with `{ "notes": "..." }`
- `ProgressBar` — shown during API calls

**Functionality:**

- Receive alarm ID from the Intent extras.
- Call `GET /api/alarms/{alarm_id}` and parse the JSON response.
- Populate all fields from the JSON.
- Decode the base64 snapshot string into a `Bitmap` and display it in the `ImageView`.
- "Mark Resolved" / "False Alarm" buttons send a `PATCH` request and navigate back to the list on success.
- RatingBar value is saved in **SQLite** (local-only data the server doesn't track).
- Guard notes are sent to the server and also cached in SQLite.

**Controls used:** TextView, ImageView, EditText, RatingBar, Button, ProgressBar

---

### 5. PersonsListActivity

**Purpose:** Browse and search registered persons in the facial recognition system.

**Layout — `activity_persons_list.xml`:**

- `EditText` — search bar (filter persons by name locally)
- `Spinner` — filter by department (populated dynamically from `/api/departments`)
- `ListView` — persons list with custom `PersonAdapter`
- `ProgressBar` — shown while loading
- `TextView` — "No persons found" empty state

**Custom PersonAdapter row layout — `item_person.xml`:**

- `TextView` — person name (bold)
- `TextView` — department
- `TextView` — employee ID
- `TextView` — face count badge (e.g., "3 faces registered")
- `TextView` — authorized zones (comma-separated, colored chips or plain text)

**Functionality:**

- On load, call `GET /api/persons` and `GET /api/departments`.
- Populate the department Spinner from the departments response.
- Populate the ListView using `PersonAdapter`.
- Search bar filters the list in real-time using `adapter.getFilter()`.
- Spinner filters by department (client-side filtering or re-fetch with params).
- Tapping a person opens `PersonDetailActivity` with the person `id` as an Intent extra.
- Cache persons in **SQLite** for offline access.

**Controls used:** EditText, Spinner, ListView (custom adapter), ProgressBar, TextView

---

### 6. PersonDetailActivity

**Purpose:** View a person's details and their recent access history.

**Layout — `activity_person_detail.xml`:**

- `TextView` — person name (large, bold)
- `TextView` — department
- `TextView` — employee ID
- `TextView` — "Authorized Zones:" followed by a list of zone names
- `TextView` — "Face images registered: {count}"
- `CheckBox` — "Favorite / Watch this person" (stored in SQLite, local-only feature)
- **Access History section:**
    - `ListView` — recent access events (from the API response `access_history` array)
    - Each row: `TextView` (timestamp) + `TextView` (camera) + `TextView` (status: authorized/unauthorized)
- `Button` — "Show on Map" → opens `CameraMapActivity` centered on the zones this person is authorized for

**Functionality:**

- Receive person ID from Intent extras.
- Call `GET /api/persons/{person_id}` and parse the response.
- Populate name, department, employee ID, authorized zones, face count.
- Populate the access history ListView.
- "Favorite" CheckBox state is stored in **SQLite** (local preference).
- "Show on Map" creates an Intent to `CameraMapActivity` with the person's authorized zone names as extras.

**Controls used:** TextView, CheckBox, ListView, Button

---

### 7. CameraMapActivity

**Purpose:** Display cameras and zones on a Google Map with alarm indicators.

**Layout — `activity_camera_map.xml`:**

- `Fragment` — Google Maps `SupportMapFragment` (full screen)
- `Switch` — "Show Zones" toggle (overlay zone polygons on/off)
- `Switch` — "Show Alarms" toggle (overlay alarm markers on/off)
- `Spinner` — filter map by zone: "All Zones", "Zone A", "Zone B", etc.
- `ProgressBar` — shown while loading camera/zone data

**Functionality:**

- On load, fetch camera and zone data:
    - `GET /api/cameras-db` → list of cameras with their locations and assigned zones
    - `GET /api/zones` → list of zones with `is_restricted` flag
    - `GET /api/alarms?status=unresolved&limit=20` → recent unresolved alarms
- **Camera markers:**
    - Place a marker for each camera using hardcoded GPS coordinates from `CameraCoordinates.java` (the backend does not store lat/lng).
    - Marker icon: green for normal zones, red for restricted zones.
    - Tapping a marker shows an info window with camera name, location, and zone.
- **Zone polygons:**
    - Draw polygons on the map for each zone.
    - Restricted zones: semi-transparent red fill.
    - Normal zones: semi-transparent blue fill.
    - Toggled on/off with the "Show Zones" Switch.
- **Alarm pins:**
    - Place alarm markers at the camera location where the alarm was triggered.
    - Red pin for unresolved, gray pin for resolved.
    - Tapping shows alarm type + description in the info window.
    - Toggled on/off with the "Show Alarms" Switch.
- Spinner filters map to show only cameras/alarms in the selected zone.
- If opened from `PersonDetailActivity`, automatically center and zoom on the relevant zones.

**Controls used:** Google Maps (markers + polygons), Switch (×2), Spinner, ProgressBar

---

### 8. StatisticsActivity

**Purpose:** Visualize detection and alarm statistics using 2D charts.

**Layout — `activity_statistics.xml`:**

- `Spinner` — time window selector: "Last 24h", "Last 48h", "Last 7 Days", "Last 30 Days"
- `CalendarView` — pick a specific date to view that day's statistics
- **Chart area (using Canvas or a charting library like MPAndroidChart):**
    - `View` (custom) — **Line chart**: detections over time (hourly or daily buckets)
    - `View` (custom) — **Bar chart**: detections per camera
    - `View` (custom) — **Pie/Donut chart**: alarm type distribution (face / fire / weapon / HAR / zone)
- **KPI cards below the charts:**
    - `TextView` — total detections
    - `TextView` — total alarms
    - `TextView` — most active camera
    - `TextView` — most common detection type
- `ProgressBar` — shown while loading

**Functionality:**

- On load, fetch statistics:
    - `GET /api/logs/stats?hours=24` → summary counts
    - `GET /api/logs/timeseries?hours=24` → time-bucketed data for the line chart
    - `GET /api/logs/breakdown?hours=24` → per-camera and per-type breakdowns
    - `GET /api/alarms/stats` → alarm counts
- When the Spinner changes, re-fetch with the new `hours` parameter.
- When a date is selected in the CalendarView, calculate `date_from` and `date_to` and fetch filtered data.
- **2D Graphics implementation:**
    - Create custom `View` subclasses (e.g., `LineChartView`, `BarChartView`, `PieChartView`).
    - Override `onDraw(Canvas canvas)` and use `Canvas`, `Paint`, and `Path` to draw axes, bars, lines, and arcs.
    - Alternatively, use the **MPAndroidChart** library for faster implementation.
- Parse the `timeseries.series` array to plot data points on the line chart.
- Parse `breakdown.by_camera` for the bar chart and `breakdown.by_type` for the pie chart.

**Controls used:** Spinner, CalendarView, custom Canvas Views (2D graphics), TextView, ProgressBar

---

### 9. SettingsActivity

**Purpose:** User preferences and app configuration.

**Layout — `activity_settings.xml`:**

- **Notification section:**
    - `Switch` — "Enable notifications"
    - `CheckBox` — "Face alerts"
    - `CheckBox` — "Fire alerts"
    - `CheckBox` — "Weapon alerts"
    - `CheckBox` — "Zone violation alerts"
    - `CheckBox` — "HAR alerts"
- **Display section:**
    - `Switch` — "Dark mode" (optional, just store the preference)
    - `Spinner` — "Default time window" for statistics: 24h / 48h / 7 days / 30 days
- **Data section:**
    - `Button` — "Clear local cache" (clears SQLite database)
    - `TextView` — cache size info
- **Account section:**
    - `TextView` — currently logged-in username (from SharedPreferences)
    - `TextView` — user role (admin / user)
    - `Button` — "Change Password" → dialog with EditText for new password, calls `PUT /api/users/me/password`
    - `Button` — "Logout" → clears SharedPreferences token, navigates to LoginActivity
- **About section:**
    - `TextView` — app version
    - `TextView` — "SecurityGuard Mobile — Integrated Security System Companion"

**Functionality:**

- All Switch and CheckBox states are stored in **SharedPreferences**.
- On toggle, immediately save to SharedPreferences.
- "Clear local cache" drops and recreates all SQLite tables.
- "Logout" clears the token from SharedPreferences, clears the SQLite cache, and starts `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
- "Change Password" opens an `AlertDialog` with an `EditText`, then sends the new password to the backend.

**Controls used:** Switch (×2), CheckBox (×5), Spinner, Button (×3), TextView

---

## Custom Adapters (Requirement 4)

### AlarmAdapter (extends BaseAdapter)

**Row layout — `item_alarm.xml`:**

- `ImageView` — alarm type icon (fire, weapon, face, zone, HAR — custom drawable per type)
- `View` — severity color stripe (left edge: red for critical, orange for high, yellow for medium, green for low)
- `TextView` — alarm description (bold, single line, ellipsized)
- `TextView` — camera ID (small, gray)
- `TextView` — timestamp (small, gray, formatted as "2h ago" or "May 2, 14:30")
- `TextView` — status badge ("UNRESOLVED" in red, "RESOLVED" in green, "FALSE ALARM" in gray)

**Methods to implement:**

- `getCount()`, `getItem(int position)`, `getItemId(int position)`
- `getView(int position, View convertView, ViewGroup parent)` — inflate `item_alarm.xml`, populate from `Alarm` object, use ViewHolder pattern for performance

### PersonAdapter (extends BaseAdapter)

**Row layout — `item_person.xml`:**

- `TextView` — person name
- `TextView` — department
- `TextView` — employee ID
- `TextView` — face count
- Implements `Filterable` for search functionality

---

## SharedPreferences Usage (Requirement 5)

**File name:** `"securityguard_prefs"`

|Key|Type|Description|
|---|---|---|
|`auth_token`|String|JWT token from login|
|`username`|String|Logged-in username|
|`user_role`|String|"admin" or "user"|
|`notif_enabled`|boolean|Master notification toggle|
|`notif_face`|boolean|Face alert notifications|
|`notif_fire`|boolean|Fire alert notifications|
|`notif_weapon`|boolean|Weapon alert notifications|
|`notif_zone`|boolean|Zone violation notifications|
|`notif_har`|boolean|HAR alert notifications|
|`filter_status`|String|Last selected alarm status filter|
|`filter_type`|String|Last selected alarm type filter|
|`filter_severity`|String|Last selected severity filter|
|`default_time_window`|int|Default hours for statistics (24/48/168/720)|
|`dark_mode`|boolean|Theme preference|
|`server_url`|String|Backend server URL (configurable)|

---

## SQLite Database (Requirement 6)

**Database name:** `securityguard_cache.db`  
**Version:** 1

### Tables

#### `alarms`

```sql
CREATE TABLE alarms (
    id             INTEGER PRIMARY KEY,
    type           TEXT,
    severity       TEXT,
    description    TEXT,
    camera_id      TEXT,
    status         TEXT,
    notes          TEXT,
    snapshot_b64   TEXT,
    metadata_json  TEXT,
    created_at     TEXT,
    resolved_at    TEXT,
    resolved_by    TEXT,
    local_rating   INTEGER DEFAULT 0,
    synced         INTEGER DEFAULT 1
);
```

#### `persons`

```sql
CREATE TABLE persons (
    id                INTEGER PRIMARY KEY,
    name              TEXT,
    department        TEXT,
    employee_id       TEXT,
    face_count        INTEGER,
    authorized_zones  TEXT,    -- JSON array stored as string
    is_favorite       INTEGER DEFAULT 0,
    synced            INTEGER DEFAULT 1
);
```

#### `access_logs`

```sql
CREATE TABLE access_logs (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id   INTEGER,
    camera_id   TEXT,
    timestamp   TEXT,
    status      TEXT,
    FOREIGN KEY (person_id) REFERENCES persons(id)
);
```

#### `cameras`

```sql
CREATE TABLE cameras (
    camera_id     TEXT PRIMARY KEY,
    name          TEXT,
    location      TEXT,
    zone_id       INTEGER,
    zone_name     TEXT,
    is_restricted INTEGER DEFAULT 0
);
-- Note: latitude/longitude are NOT stored here.
-- GPS coordinates are hardcoded in CameraCoordinates.java
```

### Helper class — `DatabaseHelper extends SQLiteOpenHelper`

- `onCreate()` — creates all tables
- `onUpgrade()` — drops and recreates
- CRUD methods: `insertAlarms(List<Alarm>)`, `getAllAlarms(filters)`, `getAlarmById(int)`, `updateAlarmRating(int, int)`, `insertPersons(List<Person>)`, `getAllPersons()`, `setFavorite(int, boolean)`, `clearAll()`

---

## Remote JSON Parsing (Requirement 7)

### API Endpoints Used

All requests include the header `Authorization: Bearer {token}`.

|Method|Endpoint|Description|Used In|
|---|---|---|---|
|POST|`/api/login`|Authenticate, get JWT token|LoginActivity|
|GET|`/api/alarms/stats`|Alarm count KPIs|DashboardActivity|
|GET|`/api/alarms?status=&type=&severity=&limit=&offset=`|List alarms with filters|AlarmsListActivity|
|GET|`/api/alarms/{id}`|Single alarm full detail|AlarmDetailActivity|
|PATCH|`/api/alarms/{id}`|Update alarm status/notes|AlarmDetailActivity|
|GET|`/api/persons`|List all registered persons|PersonsListActivity|
|GET|`/api/persons/{id}`|Person detail + access history|PersonDetailActivity|
|GET|`/api/departments`|List departments (for filter)|PersonsListActivity|
|GET|`/api/cameras-db`|List cameras with locations|CameraMapActivity|
|GET|`/api/zones`|List zones|CameraMapActivity|
|GET|`/api/logs/stats?hours=`|Detection count KPIs|DashboardActivity, StatisticsActivity|
|GET|`/api/logs/timeseries?hours=`|Time-bucketed detection data|StatisticsActivity|
|GET|`/api/logs/breakdown?hours=`|Per-camera and per-type breakdown|StatisticsActivity|
|GET|`/api/statistics`|Global registry stats|DashboardActivity, StatisticsActivity|
|PUT|`/api/users/me/password`|Change own password|SettingsActivity|

### JSON Parsing Implementation

Use `org.json.JSONObject` and `org.json.JSONArray` (built into Android) or **Gson** library:

```java
// Example: parsing an alarm from JSON
JSONObject json = new JSONObject(responseString);
Alarm alarm = new Alarm();
alarm.setId(json.getInt("id"));
alarm.setType(json.getString("type"));
alarm.setSeverity(json.getString("severity"));
alarm.setDescription(json.getString("description"));
alarm.setCameraId(json.optString("camera_id", ""));
alarm.setStatus(json.getString("status"));
alarm.setCreatedAt(json.getString("created_at"));
alarm.setSnapshot(json.optString("snapshot", null));

// Parse metadata (nested JSON)
if (json.has("metadata")) {
    JSONObject meta = json.getJSONObject("metadata");
    alarm.setConfidence(meta.optDouble("confidence", 0));
    alarm.setDetectedClass(meta.optString("class", ""));
}
```

### Networking

Use `HttpURLConnection` (no external dependency) or **Retrofit** + **Gson**:

```java
// ApiService interface for Retrofit
public interface ApiService {
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("/api/alarms")
    Call<AlarmsResponse> getAlarms(
        @Header("Authorization") String token,
        @Query("status") String status,
        @Query("type") String type,
        @Query("severity") String severity,
        @Query("limit") int limit
    );

    @GET("/api/alarms/{id}")
    Call<AlarmDetail> getAlarmDetail(
        @Header("Authorization") String token,
        @Path("id") int alarmId
    );

    @PATCH("/api/alarms/{id}")
    Call<StatusResponse> updateAlarm(
        @Header("Authorization") String token,
        @Path("id") int alarmId,
        @Body UpdateAlarmRequest request
    );
}
```

---

## Google Maps (Requirement 8)

### Setup

1. Add Google Maps SDK dependency in `build.gradle`.
2. Obtain a Google Maps API key from Google Cloud Console.
3. Add the key to `AndroidManifest.xml` inside `<meta-data>`.
4. Use `SupportMapFragment` in the layout.

### Markers (cameras)

Uses `CameraCoordinates` helper class (see Hardcoded Camera Coordinates section below) since the backend does not store GPS coordinates.

```java
for (Camera cam : cameras) {
    LatLng position = CameraCoordinates.getPosition(cam.getCameraId());
    MarkerOptions marker = new MarkerOptions()
        .position(position)
        .title(cam.getName())
        .snippet("Zone: " + cam.getZoneName())
        .icon(BitmapDescriptorFactory.defaultMarker(
            cam.isRestricted()
                ? BitmapDescriptorFactory.HUE_RED
                : BitmapDescriptorFactory.HUE_GREEN
        ));
    mMap.addMarker(marker);
}
```

### Polygons (zones)

```java
// Example: draw a restricted zone polygon
PolygonOptions zonePolygon = new PolygonOptions()
    .add(new LatLng(lat1, lng1), new LatLng(lat2, lng2), ...)
    .strokeColor(Color.RED)
    .fillColor(Color.argb(50, 255, 0, 0))  // semi-transparent red
    .strokeWidth(3);
mMap.addPolygon(zonePolygon);
```

### Alarm Pins

```java
for (Alarm alarm : unresolvedAlarms) {
    LatLng position = CameraCoordinates.getPosition(alarm.getCameraId());
    MarkerOptions pin = new MarkerOptions()
        .position(position)
        .title(alarm.getType().toUpperCase())
        .snippet(alarm.getDescription())
        .icon(BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_RED
        ));
    mMap.addMarker(pin);
}
```

### Hardcoded Camera Coordinates

The existing backend stores camera `location` as a text string (e.g., "Laptop Camera") and does **not** have `latitude` / `longitude` columns. To avoid modifying the backend, coordinates are hardcoded in the Android app using a `HashMap` that maps camera IDs to GPS positions.

Create a helper class `CameraCoordinates.java`:

```java
package com.example.proiect;

import com.google.android.gms.maps.model.LatLng;
import java.util.HashMap;
import java.util.Map;

public class CameraCoordinates {

    // Hardcoded GPS positions for each camera (demo purposes)
    // Points placed around Universitatea Politehnica Bucuresti as an example
    private static final Map<String, LatLng> CAMERA_POSITIONS = new HashMap<>();

    static {
        CAMERA_POSITIONS.put("CAM-01", new LatLng(44.43856, 26.04948));  // Main Entrance
        CAMERA_POSITIONS.put("CAM-02", new LatLng(44.43910, 26.04872));  // Parking Lot
        CAMERA_POSITIONS.put("CAM-03", new LatLng(44.43785, 26.04995));  // East Wing Hallway
        CAMERA_POSITIONS.put("CAM-04", new LatLng(44.43820, 26.05060));  // Server Room
        CAMERA_POSITIONS.put("CAM-05", new LatLng(44.43750, 26.04910));  // Back Gate
    }

    // Default center point for the map (center of all cameras)
    public static final LatLng MAP_CENTER = new LatLng(44.43824, 26.04957);
    public static final float DEFAULT_ZOOM = 17f;

    /**
     * Get the GPS position for a camera ID.
     * Returns a default position if the camera ID is not mapped.
     */
    public static LatLng getPosition(String cameraId) {
        return CAMERA_POSITIONS.getOrDefault(cameraId, MAP_CENTER);
    }

    /**
     * Check if a camera has a known position.
     */
    public static boolean hasPosition(String cameraId) {
        return CAMERA_POSITIONS.containsKey(cameraId);
    }

    /**
     * Get all mapped camera IDs and positions.
     */
    public static Map<String, LatLng> getAll() {
        return CAMERA_POSITIONS;
    }
}
```

Then use it in `CameraMapActivity` instead of reading coordinates from the API:

```java
// When placing camera markers on the map
for (Camera cam : camerasFromApi) {
    LatLng position = CameraCoordinates.getPosition(cam.getCameraId());

    MarkerOptions marker = new MarkerOptions()
        .position(position)
        .title(cam.getName())
        .snippet("Zone: " + cam.getZoneName())
        .icon(BitmapDescriptorFactory.defaultMarker(
            cam.isRestricted()
                ? BitmapDescriptorFactory.HUE_RED
                : BitmapDescriptorFactory.HUE_GREEN
        ));
    mMap.addMarker(marker);
}

// Center the map
mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
    CameraCoordinates.MAP_CENTER,
    CameraCoordinates.DEFAULT_ZOOM
));
```

**To customize:** replace the example coordinates with real GPS points around your actual building or campus. You can get coordinates easily by right-clicking on Google Maps in a browser and copying the latitude/longitude.

---

## 2D Graphics — Charts (Requirement 9)

### Option A: Custom Canvas Drawing (no library)

Create custom View subclasses that override `onDraw(Canvas canvas)`:

#### LineChartView

- X-axis: time buckets (from `/api/logs/timeseries`)
- Y-axis: detection count
- Draw axes with `canvas.drawLine()`
- Plot data points with `canvas.drawCircle()`
- Connect points with `canvas.drawPath()` using a `Path` object
- Fill area under the line with semi-transparent color

#### BarChartView

- One bar per camera (from `/api/logs/breakdown → by_camera`)
- Draw bars with `canvas.drawRect()`
- Color each bar differently
- Draw labels with `canvas.drawText()`

#### PieChartView

- Segments for each detection type (from `/api/logs/breakdown → by_type`)
- Draw arcs with `canvas.drawArc()`
- Color per type: red for face, orange for fire, purple for weapon, blue for HAR, yellow for zone
- Draw legend with colored squares and labels

### Option B: MPAndroidChart Library

Add to `build.gradle`:

```gradle
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

Use `LineChart`, `BarChart`, and `PieChart` views directly in XML and populate with data sets.

**Recommendation:** Use **Option A** (custom Canvas) for at least one chart to clearly demonstrate the "2D graphics" requirement — the professor will see you drew it yourself.

---

## Data Model Classes

```java
public class Alarm {
    private int id;
    private String type;        // "face", "fire", "weapon", "har", "unauthorized_zone"
    private String severity;    // "critical", "high", "medium", "low"
    private String description;
    private String cameraId;
    private String status;      // "unresolved", "resolved", "false_alarm"
    private String notes;
    private String snapshot;    // base64 image string
    private String metadataJson;
    private String createdAt;
    private String resolvedAt;
    private String resolvedBy;
    private int localRating;    // local-only, 0-5
}

public class Person {
    private int id;
    private String name;
    private String department;
    private String employeeId;
    private int faceCount;
    private List<String> authorizedZones;
    private boolean isFavorite; // local-only
}

public class Camera {
    private String cameraId;
    private String name;
    private String location;
    // latitude & longitude are NOT from the API — use CameraCoordinates helper class
    private int zoneId;
    private String zoneName;
    private boolean isRestricted;
}

public class Zone {
    private int id;
    private String name;
    private String description;
    private boolean isRestricted;
}

public class AccessLogEntry {
    private String timestamp;
    private String cameraId;
    private String status;
    private double confidence;
}
```

---

## Project File Structure

```
app/
├── src/main/java/com/example/proiect/
│   ├── activities/
│   │   ├── LoginActivity.java
│   │   ├── DashboardActivity.java
│   │   ├── AlarmsListActivity.java
│   │   ├── AlarmDetailActivity.java
│   │   ├── PersonsListActivity.java
│   │   ├── PersonDetailActivity.java
│   │   ├── CameraMapActivity.java
│   │   ├── StatisticsActivity.java
│   │   └── SettingsActivity.java
│   ├── adapters/
│   │   ├── AlarmAdapter.java
│   │   └── PersonAdapter.java
│   ├── api/
│   │   ├── ApiService.java          (Retrofit interface or HTTP helper)
│   │   └── ApiClient.java           (singleton, base URL, auth headers)
│   ├── database/
│   │   └── DatabaseHelper.java      (SQLiteOpenHelper)
│   ├── models/
│   │   ├── Alarm.java
│   │   ├── Person.java
│   │   ├── Camera.java
│   │   ├── Zone.java
│   │   └── AccessLogEntry.java
│   ├── charts/
│   │   ├── LineChartView.java       (custom Canvas view)
│   │   ├── BarChartView.java        (custom Canvas view)
│   │   └── PieChartView.java        (custom Canvas view)
│   └── utils/
│       ├── PrefsManager.java        (SharedPreferences wrapper)
│       ├── CameraCoordinates.java   (hardcoded GPS positions for cameras)
│       └── DateUtils.java           (timestamp formatting)
├── src/main/res/
│   ├── layout/
│   │   ├── activity_login.xml
│   │   ├── activity_dashboard.xml
│   │   ├── activity_alarms_list.xml
│   │   ├── activity_alarm_detail.xml
│   │   ├── activity_persons_list.xml
│   │   ├── activity_person_detail.xml
│   │   ├── activity_camera_map.xml
│   │   ├── activity_statistics.xml
│   │   ├── activity_settings.xml
│   │   ├── item_alarm.xml
│   │   └── item_person.xml
│   ├── drawable/
│   │   ├── ic_fire.xml
│   │   ├── ic_weapon.xml
│   │   ├── ic_face.xml
│   │   ├── ic_zone.xml
│   │   └── ic_har.xml
│   ├── values/
│   │   ├── strings.xml
│   │   └── colors.xml
│   └── ...
└── build.gradle
```

---

## Dependencies (build.gradle)

```gradle
dependencies {
    // Google Maps
    implementation 'com.google.android.gms:play-services-maps:18.2.0'

    // Networking (pick one)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.google.code.gson:gson:2.10.1'

    // UI
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    implementation 'androidx.cardview:cardview:1.0.0'

    // Charts (optional — if using MPAndroidChart instead of custom Canvas)
    // implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
```

---

## Implementation Order (Suggested)

1. **Models + DatabaseHelper** — define all data classes and SQLite schema first
2. **PrefsManager** — SharedPreferences wrapper
3. **ApiClient / ApiService** — networking layer
4. **LoginActivity** — get authentication working end-to-end
5. **DashboardActivity** — fetch and display summary data
6. **AlarmsListActivity + AlarmAdapter** — alarm list with filters
7. **AlarmDetailActivity** — full alarm view with resolve/false-alarm actions
8. **PersonsListActivity + PersonAdapter** — persons list with search
9. **PersonDetailActivity** — person detail with access history
10. **CameraMapActivity** — Google Maps with markers and polygons
11. **StatisticsActivity + Chart Views** — 2D graphics
12. **SettingsActivity** — preferences and logout