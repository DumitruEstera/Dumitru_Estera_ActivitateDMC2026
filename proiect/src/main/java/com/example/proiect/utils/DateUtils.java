package com.example.proiect.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final SimpleDateFormat ISO_NO_MS = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private static final SimpleDateFormat ISO_MS = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    private static final SimpleDateFormat DISPLAY = new SimpleDateFormat(
            "MMM d, HH:mm", Locale.getDefault());

    static {
        ISO_NO_MS.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO_MS.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String relative(String iso) {
        Date d = parse(iso);
        if (d == null) return iso == null ? "" : iso;

        long diffMs = System.currentTimeMillis() - d.getTime();
        long min = diffMs / 60000L;

        if (min < 1) return "just now";
        if (min < 60) return min + "m ago";
        long hours = min / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 7) return days + "d ago";

        return DISPLAY.format(d);
    }

    private static Date parse(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        String trimmed = iso;
        int z = trimmed.indexOf('Z');
        if (z > 0) trimmed = trimmed.substring(0, z);
        int plus = trimmed.indexOf('+');
        if (plus > 0) trimmed = trimmed.substring(0, plus);
        try {
            if (trimmed.contains(".")) return ISO_MS.parse(trimmed);
            return ISO_NO_MS.parse(trimmed);
        } catch (Exception e) {
            return null;
        }
    }
}
