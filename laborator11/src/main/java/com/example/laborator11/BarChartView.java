package com.example.laborator11;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class BarChartView extends View {

    private List<String> names;
    private float[] values;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(32f);
        axisPaint.setColor(Color.DKGRAY);
        axisPaint.setStrokeWidth(3f);
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

        float max = 0f;
        for (float v : values) if (v > max) max = v;
        if (max <= 0) return;

        int w = getWidth();
        int h = getHeight();
        float paddingLeft = 180f;
        float paddingRight = 100f;
        float paddingTop = 40f;
        float paddingBottom = 60f;

        float chartW = w - paddingLeft - paddingRight;
        float chartH = h - paddingTop - paddingBottom;

        // axe
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + chartH, axisPaint);
        canvas.drawLine(paddingLeft, paddingTop + chartH, paddingLeft + chartW, paddingTop + chartH, axisPaint);

        int n = values.length;
        float slot = chartH / n;
        float barHeight = slot * 0.6f;

        for (int i = 0; i < n; i++) {
            float cy = paddingTop + slot * i + slot / 2f;
            float barW = (values[i] / max) * chartW;
            float top = cy - barHeight / 2f;
            barPaint.setColor(COLORS[i % COLORS.length]);
            canvas.drawRect(paddingLeft, top, paddingLeft + barW, top + barHeight, barPaint);

            // eticheta stanga
            textPaint.setColor(Color.BLACK);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            if (names != null && i < names.size()) {
                canvas.drawText(names.get(i), paddingLeft - 15f, cy + 10f, textPaint);
            }

            // valoare la capatul barei
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(String.valueOf(values[i]), paddingLeft + barW + 10f, cy + 10f, textPaint);
        }
    }
}
