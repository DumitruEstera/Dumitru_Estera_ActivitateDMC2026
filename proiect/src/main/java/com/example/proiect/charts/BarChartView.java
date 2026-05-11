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

public class BarChartView extends View {

    public static class Bar {
        public final String label;
        public final double value;
        public Bar(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final int[] PALETTE = {
            Color.parseColor("#1976D2"),
            Color.parseColor("#388E3C"),
            Color.parseColor("#F57C00"),
            Color.parseColor("#D32F2F"),
            Color.parseColor("#7B1FA2"),
            Color.parseColor("#0097A7"),
            Color.parseColor("#5D4037"),
            Color.parseColor("#455A64"),
    };

    private final List<Bar> bars = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context) { super(context); init(); }
    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
        axisPaint.setColor(Color.parseColor("#6B7A90"));
        axisPaint.setStrokeWidth(dp(1.2f));
        gridPaint.setColor(Color.parseColor("#22000000"));
        gridPaint.setStrokeWidth(dp(0.8f));
        textPaint.setColor(Color.parseColor("#6B7A90"));
        textPaint.setTextSize(sp(11));
        valuePaint.setColor(Color.parseColor("#1A2F4B"));
        valuePaint.setTextSize(sp(11));
        valuePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Bar> data) {
        bars.clear();
        if (data != null) bars.addAll(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float padLeft = dp(36), padRight = dp(12), padTop = dp(16), padBottom = dp(34);
        float plotLeft = padLeft, plotRight = w - padRight;
        float plotTop = padTop, plotBottom = h - padBottom;

        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axisPaint);
        canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axisPaint);

        if (bars.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No data", (plotLeft + plotRight) / 2f,
                    (plotTop + plotBottom) / 2f, textPaint);
            return;
        }

        double max = 0;
        for (Bar b : bars) if (b.value > max) max = b.value;
        if (max <= 0) max = 1;
        double niceMax = LineChartView.niceCeil(max);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        int ySteps = 4;
        for (int i = 0; i <= ySteps; i++) {
            float y = plotBottom - (plotBottom - plotTop) * i / (float) ySteps;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
            canvas.drawText(LineChartView.formatNumber(niceMax * i / ySteps),
                    plotLeft - dp(4), y + sp(4), textPaint);
        }

        int n = bars.size();
        float slot = (plotRight - plotLeft) / n;
        float barW = slot * 0.65f;
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) {
            Bar b = bars.get(i);
            float cx = plotLeft + slot * i + slot / 2f;
            float top = (float) (plotBottom - (plotBottom - plotTop) * (b.value / niceMax));
            RectF r = new RectF(cx - barW / 2f, top, cx + barW / 2f, plotBottom);
            barPaint.setColor(PALETTE[i % PALETTE.length]);
            canvas.drawRoundRect(r, dp(3), dp(3), barPaint);

            if (b.value > 0) {
                canvas.drawText(LineChartView.formatNumber(b.value), cx,
                        Math.max(top - dp(4), plotTop + sp(11)), valuePaint);
            }
            String label = b.label == null ? "" : b.label;
            if (label.length() > 10) label = label.substring(0, 9) + "…";
            canvas.drawText(label, cx, plotBottom + dp(16), textPaint);
        }
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
}
