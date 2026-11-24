
package com.poolgeometry.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class TrajectoryView extends View {

    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trajectoryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trajectoryGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int segments = 7;

    private float p1x = 0.22f;
    private float p1y = 0.68f;
    private float p2x = 0.43f;
    private float p2y = 0.32f;

    private float left = 0.08f;
    private float top = 0.20f;
    private float right = 0.92f;
    private float bottom = 0.80f;

    public TrajectoryView(Context context) {
        super(context);

        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        cornerPaint.setColor(0xFF20E6A0);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dp(3.5f));
        cornerPaint.setStrokeCap(Paint.Cap.SQUARE);
        cornerPaint.setStrokeJoin(Paint.Join.MITER);

        trajectoryGlowPaint.setColor(0x667C3AED);
        trajectoryGlowPaint.setStyle(Paint.Style.STROKE);
        trajectoryGlowPaint.setStrokeWidth(dp(7));
        trajectoryGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        trajectoryGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        trajectoryPaint.setColor(0xFFF0ABFC);
        trajectoryPaint.setStyle(Paint.Style.STROKE);
        trajectoryPaint.setStrokeWidth(dp(2.5f));
        trajectoryPaint.setStrokeCap(Paint.Cap.ROUND);
        trajectoryPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setConfig(
            int numberOfSegments,
            float point1X,
            float point1Y,
            float point2X,
            float point2Y,
            float rectLeft,
            float rectTop,
            float rectRight,
            float rectBottom) {

        segments = Math.max(1, Math.min(30, numberOfSegments));

        p1x = point1X;
        p1y = point1Y;
        p2x = point2X;
        p2y = point2Y;

        left = clamp(rectLeft, 0f, 1f);
        top = clamp(rectTop, 0f, 1f);
        right = clamp(rectRight, left + 0.01f, 1f);
        bottom = clamp(rectBottom, top + 0.01f, 1f);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float screenWidth = getWidth();
        float screenHeight = getHeight();

        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        RectF tableBounds = new RectF(
                left * screenWidth,
                top * screenHeight,
                right * screenWidth,
                bottom * screenHeight
        );

        if (tableBounds.width() < dp(30) ||
                tableBounds.height() < dp(30)) {
            return;
        }

        drawCornerMarks(canvas, tableBounds);

        drawReflectedTrajectory(
                canvas,
                tableBounds,
                screenWidth,
                screenHeight
        );
    }

    private void drawCornerMarks(Canvas canvas, RectF bounds) {

        float markLength = Math.min(
                dp(30),
                Math.min(bounds.width(), bounds.height()) * 0.12f
        );

        Path marks = new Path();

        // Canto superior esquerdo
        marks.moveTo(bounds.left + markLength, bounds.top);
        marks.lineTo(bounds.left, bounds.top);
        marks.lineTo(bounds.left, bounds.top + markLength);

        // Canto superior direito
        marks.moveTo(bounds.right - markLength, bounds.top);
        marks.lineTo(bounds.right, bounds.top);
        marks.lineTo(bounds.right, bounds.top + markLength);

        // Canto inferior esquerdo
        marks.moveTo(bounds.left + markLength, bounds.bottom);
        marks.lineTo(bounds.left, bounds.bottom);
        marks.lineTo(bounds.left, bounds.bottom - markLength);

        // Canto inferior direito
        marks.moveTo(bounds.right - markLength, bounds.bottom);
        marks.lineTo(bounds.right, bounds.bottom);
        marks.lineTo(bounds.right, bounds.bottom - markLength);

        canvas.drawPath(marks, cornerPaint);
    }

    private void drawReflectedTrajectory(
            Canvas canvas,
            RectF bounds,
            float screenWidth,
            float screenHeight) {

        float startX = clamp(
                p1x * screenWidth,
                bounds.left,
                bounds.right
        );

        float startY = clamp(
                p1y * screenHeight,
                bounds.top,
                bounds.bottom
        );

        float targetX = clamp(
                p2x * screenWidth,
                bounds.left,
                bounds.right
        );

        float targetY = clamp(
                p2y * screenHeight,
                bounds.top,
                bounds.bottom
        );

        float dx = targetX - startX;
        float dy = targetY - startY;

        float magnitude = (float) Math.hypot(dx, dy);

        if (magnitude < 1f) {
            return;
        }

        dx /= magnitude;
        dy /= magnitude;

        Path trajectory = new Path();
        trajectory.moveTo(startX, startY);

        float x = startX;
        float y = startY;

        for (int i = 0; i < segments; i++) {

            float distanceToVertical = Float.POSITIVE_INFINITY;
            float distanceToHorizontal = Float.POSITIVE_INFINITY;

            if (dx > 0.0001f) {
                distanceToVertical = (bounds.right - x) / dx;
            } else if (dx < -0.0001f) {
                distanceToVertical = (bounds.left - x) / dx;
            }

            if (dy > 0.0001f) {
                distanceToHorizontal = (bounds.bottom - y) / dy;
            } else if (dy < -0.0001f) {
                distanceToHorizontal = (bounds.top - y) / dy;
            }

            float distance = Math.min(
                    distanceToVertical > 0.001f
                            ? distanceToVertical
                            : Float.POSITIVE_INFINITY,

                    distanceToHorizontal > 0.001f
                            ? distanceToHorizontal
                            : Float.POSITIVE_INFINITY
            );

            if (Float.isInfinite(distance) ||
                    Float.isNaN(distance)) {
                break;
            }

            x += dx * distance;
            y += dy * distance;

            x = clamp(x, bounds.left, bounds.right);
            y = clamp(y, bounds.top, bounds.bottom);

            trajectory.lineTo(x, y);

            boolean hitVertical =
                    Math.abs(distanceToVertical - distance) < 0.01f;

            boolean hitHorizontal =
                    Math.abs(distanceToHorizontal - distance) < 0.01f;

            if (hitVertical) {
                dx = -dx;
            }

            if (hitHorizontal) {
                dy = -dy;
            }
        }

        canvas.drawPath(trajectory, trajectoryGlowPaint);
        canvas.drawPath(trajectory, trajectoryPaint);
    }

    private float clamp(float value, float minimum, float maximum) {
        if (maximum < minimum) {
            maximum = minimum;
        }

        return Math.max(minimum, Math.min(maximum, value));
    }

    private float dp(float value) {
        return value * getResources()
                .getDisplayMetrics().density;
    }
}
