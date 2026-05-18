package com.example.proiect.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiect.R;
import com.example.proiect.models.LiveCamera;

import java.util.List;

public class LiveCameraAdapter extends RecyclerView.Adapter<LiveCameraAdapter.VH> {

    public interface OnCameraClick {
        void onClick(LiveCamera camera);
    }

    private static final long STALE_THRESHOLD_MS = 4000L;

    private final Context context;
    private final List<LiveCamera> items;
    private final OnCameraClick clickListener;

    public LiveCameraAdapter(Context context, List<LiveCamera> items, OnCameraClick clickListener) {
        this.context = context;
        this.items = items;
        this.clickListener = clickListener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).cameraId.hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_live_camera, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LiveCamera cam = items.get(position);

        String title = cam.cameraId;
        if (!TextUtils.isEmpty(cam.location)) {
            title = title + " • " + cam.location;
        }
        h.title.setText(title);

        boolean stale = cam.isStale(System.currentTimeMillis(), STALE_THRESHOLD_MS);
        if (cam.latestFrame != null) {
            h.image.setImageBitmap(cam.latestFrame);
            h.progress.setVisibility(View.GONE);
            h.image.setAlpha(stale ? 0.45f : 1f);
        } else {
            h.image.setImageDrawable(null);
            h.image.setAlpha(1f);
            h.progress.setVisibility(View.VISIBLE);
        }

        int dotColor;
        String statusText;
        if (cam.latestFrame == null) {
            dotColor = 0xFF9E9E9E;
            statusText = "Waiting…";
        } else if (stale) {
            dotColor = 0xFFF9A825;
            statusText = "Stale";
        } else {
            dotColor = 0xFF43A047;
            statusText = "Live";
        }
        h.statusDot.setBackgroundColor(dotColor);
        h.statusText.setText(statusText);

        if (TextUtils.isEmpty(cam.latestHint)) {
            h.hint.setVisibility(View.GONE);
        } else {
            h.hint.setVisibility(View.VISIBLE);
            h.hint.setText(cam.latestHint);
        }

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(cam);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final ProgressBar progress;
        final TextView title;
        final TextView statusText;
        final View statusDot;
        final TextView hint;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.live_camera_image);
            progress = itemView.findViewById(R.id.live_camera_progress);
            title = itemView.findViewById(R.id.live_camera_title);
            statusText = itemView.findViewById(R.id.live_camera_status_text);
            statusDot = itemView.findViewById(R.id.live_camera_status_dot);
            hint = itemView.findViewById(R.id.live_camera_hint);
        }
    }
}
