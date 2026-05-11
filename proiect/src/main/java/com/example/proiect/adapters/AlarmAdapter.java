package com.example.proiect.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.proiect.R;
import com.example.proiect.models.Alarm;
import com.example.proiect.utils.DateUtils;

import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<Alarm> alarms;

    public AlarmAdapter(Context context, List<Alarm> alarms) {
        this.context = context;
        this.alarms = alarms;
        this.inflater = LayoutInflater.from(context);
    }

    public void setAlarms(List<Alarm> alarms) {
        this.alarms = alarms;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return alarms == null ? 0 : alarms.size();
    }

    @Override
    public Alarm getItem(int position) {
        return alarms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return alarms.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_alarm, parent, false);
            h = new ViewHolder();
            h.severityStripe = convertView.findViewById(R.id.item_alarm_severity_stripe);
            h.iconBg = convertView.findViewById(R.id.item_alarm_icon_bg);
            h.icon = convertView.findViewById(R.id.item_alarm_icon);
            h.description = convertView.findViewById(R.id.item_alarm_description);
            h.meta = convertView.findViewById(R.id.item_alarm_meta);
            h.status = convertView.findViewById(R.id.item_alarm_status);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Alarm alarm = alarms.get(position);

        String desc = alarm.getDescription();
        if (desc == null || desc.isEmpty()) {
            desc = alarm.getType().toUpperCase(Locale.US) + " detected";
        }
        h.description.setText(desc);

        String camera = alarm.getCameraId();
        String when = DateUtils.relative(alarm.getCreatedAt());
        StringBuilder meta = new StringBuilder();
        if (camera != null && !camera.isEmpty()) {
            meta.append(camera);
        }
        if (when != null && !when.isEmpty()) {
            if (meta.length() > 0) meta.append("  •  ");
            meta.append(when);
        }
        h.meta.setText(meta.toString());

        h.severityStripe.setBackgroundColor(severityColor(alarm.getSeverity()));
        tintCircle(h.iconBg, severityColor(alarm.getSeverity()));
        h.icon.setImageResource(typeIcon(alarm.getType()));

        String status = alarm.getStatus();
        h.status.setText(statusLabel(status));
        tintBadge(h.status, statusColor(status));

        return convertView;
    }

    private static int severityColor(String severity) {
        if (severity == null) return Color.parseColor("#4CAF50");
        switch (severity.toLowerCase(Locale.US)) {
            case "critical": return Color.parseColor("#D32F2F");
            case "high":     return Color.parseColor("#F57C00");
            case "medium":   return Color.parseColor("#FBC02D");
            default:         return Color.parseColor("#4CAF50");
        }
    }

    private static int statusColor(String status) {
        if (status == null) return Color.parseColor("#9E9E9E");
        switch (status.toLowerCase(Locale.US)) {
            case "unresolved":  return Color.parseColor("#D32F2F");
            case "resolved":    return Color.parseColor("#388E3C");
            case "false_alarm": return Color.parseColor("#9E9E9E");
            default:            return Color.parseColor("#607D8B");
        }
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        switch (status.toLowerCase(Locale.US)) {
            case "unresolved":  return "UNRESOLVED";
            case "resolved":    return "RESOLVED";
            case "false_alarm": return "FALSE ALARM";
            default:            return status.toUpperCase(Locale.US);
        }
    }

    private static int typeIcon(String type) {
        return R.drawable.ic_alarm;
    }

    private static void tintCircle(View v, int color) {
        if (v.getBackground() instanceof GradientDrawable) {
            GradientDrawable g = (GradientDrawable) v.getBackground().mutate();
            g.setColor(color);
        } else {
            v.setBackgroundColor(color);
        }
    }

    private static void tintBadge(TextView tv, int color) {
        if (tv.getBackground() instanceof GradientDrawable) {
            GradientDrawable g = (GradientDrawable) tv.getBackground().mutate();
            g.setColor(color);
        } else {
            tv.setBackgroundColor(color);
        }
    }

    private static class ViewHolder {
        View severityStripe;
        View iconBg;
        ImageView icon;
        TextView description;
        TextView meta;
        TextView status;
    }
}
