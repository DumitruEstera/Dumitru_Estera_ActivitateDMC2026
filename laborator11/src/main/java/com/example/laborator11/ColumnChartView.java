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

public class ColumnChartView extends View {

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

    public ColumnChartView(Context context) {
        super(context);
        init();
    }

    public ColumnChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColumnChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        float paddingLeft = 80f;
        float paddingRight = 40f;
        float paddingTop = 60f;
        float paddingBottom = 120f;

        float chartW = w - paddingLeft - paddingRight;
        float chartH = h - paddingTop - paddingBottom;
        float baseY = paddingTop + chartH;

        // axe
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, baseY, axisPaint);
        canvas.drawLine(paddingLeft, baseY, paddingLeft + chartW, baseY, axisPaint);

        int n = values.length;
        float slot = chartW / n;
        float barWidth = slot * 0.6f;

        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            float cx = paddingLeft + slot * i + slot / 2f;
            float barH = (values[i] / max) * chartH;
            float left = cx - barWidth / 2f;
            float top = baseY - barH;
            barPaint.setColor(COLORS[i % COLORS.length]);
            canvas.drawRect(left, top, left + barWidth, baseY, barPaint);

            // valoare deasupra
            textPaint.setColor(Color.BLACK);
            canvas.drawText(String.valueOf(values[i]), cx, top - 10f, textPaint);

            // eticheta sub bara
            if (names != null && i < names.size()) {
                canvas.drawText(names.get(i), cx, baseY + 40f, textPaint);
            }
        }
    }
}
