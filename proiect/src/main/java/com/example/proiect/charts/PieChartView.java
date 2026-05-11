package com.example.proiect.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PieChartView extends View {

    public static class Slice {
        public final String label;
        public final double value;
        public final int color;
        public Slice(String label, double value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendSquare = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PieChartView(Context context) { super(context); init(); }
    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }

    private void init() {
        slicePaint.setStyle(Paint.Style.FILL);
        holePaint.setColor(Color.WHITE);
        holePaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.parseColor("#1A2F4B"));
        textPaint.setTextSize(sp(12));
        legendSquare.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Slice> data) {
        slices.clear();
        if (data != null) slices.addAll(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float pad = dp(12);
        float legendW = Math.min(dp(140), w * 0.42f);
        float pieAreaW = w - legendW - pad * 2;
        float diameter = Math.min(pieAreaW, h - pad * 2);
        float cx = pad + pieAreaW / 2f;
        float cy = h / 2f;
        float radius = diameter / 2f;
        RectF pie = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        double total = 0;
        for (Slice s : slices) total += Math.max(0, s.value);

        if (total <= 0) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No data", cx, cy, textPaint);
            return;
        }

        float start = -90f;
        for (Slice s : slices) {
            if (s.value <= 0) continue;
            float sweep = (float) (360.0 * s.value / total);
            slicePaint.setColor(s.color);
            canvas.drawArc(pie, start, sweep, true, slicePaint);
            start += sweep;
        }
        canvas.drawCircle(cx, cy, radius * 0.55f, holePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.valueOf((long) total), cx, cy + sp(4), textPaint);
        textPaint.setFakeBoldText(false);

        float lx = w - legendW + pad;
        float ly = pad;
        textPaint.setTextAlign(Paint.Align.LEFT);
        for (Slice s : slices) {
            if (s.value <= 0) continue;
            legendSquare.setColor(s.color);
            canvas.drawRect(lx, ly, lx + dp(12), ly + dp(12), legendSquare);
            double pct = 100.0 * s.value / total;
            String text = String.format(Locale.US, "%s — %d (%.0f%%)",
                    s.label == null ? "" : s.label, (long) s.value, pct);
            canvas.drawText(text, lx + dp(16), ly + dp(11), textPaint);
            ly += dp(20);
        }
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
