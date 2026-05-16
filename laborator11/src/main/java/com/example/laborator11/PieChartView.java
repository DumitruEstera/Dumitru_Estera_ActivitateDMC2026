package com.example.laborator11;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class PieChartView extends View {

    private List<String> names;
    private float[] values;

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private static final int[] COLORS = new int[]{
            Color.parseColor("#E53935"),
            Color.parseColor("#1E88E5"),
            Color.parseColor("#43A047"),
            Color.parseColor("#FB8C00"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#00ACC1"),
            Color.parseColor("#FDD835"),
            Color.parseColor("#6D4C41")
    };

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f);
        linePaint.setColor(Color.DKGRAY);
        linePaint.setStrokeWidth(2f);
    }

    public void setData(List<String> names, float[] values) {
        this.names = names;
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (values == null || values.length == 0) return;

        float total = 0f;
        for (float v : values) total += v;
        if (total <= 0) return;

        int w = getWidth();
        int h = getHeight();
        float padding = 60f;
        float legendHeight = (names.size() * 60f) + 40f;

        float diameter = Math.min(w - 2 * padding, h - legendHeight - 2 * padding);
        float left = (w - diameter) / 2f;
        float top = padding;
        oval.set(left, top, left + diameter, top + diameter);

        float startAngle = -90f;
        float cx = left + diameter / 2f;
        float cy = top + diameter / 2f;
        float radius = diameter / 2f;

        for (int i = 0; i < values.length; i++) {
            float sweep = (values[i] / total) * 360f;
            slicePaint.setColor(COLORS[i % COLORS.length]);
            canvas.drawArc(oval, startAngle, sweep, true, slicePaint);

            // Procent in interiorul feliei
            float midAngle = (float) Math.toRadians(startAngle + sweep / 2f);
            float tx = cx + (float) Math.cos(midAngle) * radius * 0.6f;
            float ty = cy + (float) Math.sin(midAngle) * radius * 0.6f;
            String percent = String.format("%.1f%%", (values[i] / total) * 100f);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(percent, tx, ty, textPaint);

            startAngle += sweep;
        }

        // Legenda
        float legendTop = top + diameter + 30f;
        textPaint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < names.size(); i++) {
            float y = legendTop + i * 60f;
            slicePaint.setColor(COLORS[i % COLORS.length]);
            canvas.drawRect(padding, y, padding + 40f, y + 40f, slicePaint);
            textPaint.setColor(Color.BLACK);
            String label = names.get(i) + " : " + values[i];
            canvas.drawText(label, padding + 60f, y + 32f, textPaint);
        }
    }
}
