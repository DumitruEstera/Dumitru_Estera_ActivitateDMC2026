package com.example.proiect.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LineChartView extends View {

    public static class Point {
        public final String label;
        public final double value;
        public Point(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private final List<Point> points = new ArrayList<>();
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context context) { super(context); init(); }
    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }

    private void init() {
        axisPaint.setColor(Color.parseColor("#6B7A90"));
        axisPaint.setStrokeWidth(dp(1.2f));
        axisPaint.setStyle(Paint.Style.STROKE);

        gridPaint.setColor(Color.parseColor("#22000000"));
        gridPaint.setStrokeWidth(dp(0.8f));
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint.setColor(Color.parseColor("#1976D2"));
        linePaint.setStrokeWidth(dp(2.4f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setColor(Color.parseColor("#331976D2"));
        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setColor(Color.parseColor("#1976D2"));
        dotPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#6B7A90"));
        textPaint.setTextSize(sp(11));
    }

    public void setData(List<Point> data) {
        points.clear();
        if (data != null) points.addAll(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float padLeft = dp(36), padRight = dp(12), padTop = dp(12), padBottom = dp(28);
        float plotLeft = padLeft, plotRight = w - padRight;
        float plotTop = padTop, plotBottom = h - padBottom;

        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axisPaint);
        canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axisPaint);

        if (points.size() < 2) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No data", (plotLeft + plotRight) / 2f,
                    (plotTop + plotBottom) / 2f, textPaint);
            return;
        }

        double max = 0;
        for (Point p : points) if (p.value > max) max = p.value;
        if (max <= 0) max = 1;
        double niceMax = niceCeil(max);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        int ySteps = 4;
        for (int i = 0; i <= ySteps; i++) {
            float y = plotBottom - (plotBottom - plotTop) * i / (float) ySteps;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
            String label = formatNumber(niceMax * i / ySteps);
            canvas.drawText(label, plotLeft - dp(4), y + sp(4), textPaint);
        }

        Path line = new Path();
        Path fill = new Path();
        int n = points.size();
        for (int i = 0; i < n; i++) {
            float x = plotLeft + (plotRight - plotLeft) * i / (float) (n - 1);
            float y = (float) (plotBottom - (plotBottom - plotTop)
                    * (points.get(i).value / niceMax));
            if (i == 0) {
                line.moveTo(x, y);
                fill.moveTo(x, plotBottom);
                fill.lineTo(x, y);
            } else {
                line.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        float lastX = plotLeft + (plotRight - plotLeft);
        fill.lineTo(lastX, plotBottom);
        fill.close();
        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(line, linePaint);

        for (int i = 0; i < n; i++) {
            float x = plotLeft + (plotRight - plotLeft) * i / (float) (n - 1);
            float y = (float) (plotBottom - (plotBottom - plotTop)
                    * (points.get(i).value / niceMax));
            canvas.drawCircle(x, y, dp(3), dotPaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
        int labelStep = Math.max(1, n / 6);
        for (int i = 0; i < n; i += labelStep) {
            float x = plotLeft + (plotRight - plotLeft) * i / (float) (n - 1);
            String l = points.get(i).label == null ? "" : points.get(i).label;
            canvas.drawText(l, x, plotBottom + dp(16), textPaint);
        }
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }

    static double niceCeil(double v) {
        if (v <= 0) return 1;
        double pow = Math.pow(10, Math.floor(Math.log10(v)));
        double m = v / pow;
        double nice;
        if (m <= 1) nice = 1;
        else if (m <= 2) nice = 2;
        else if (m <= 5) nice = 5;
        else nice = 10;
        return nice * pow;
    }

    static String formatNumber(double v) {
        if (v >= 1000) return String.format("%.1fk", v / 1000.0);
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}
