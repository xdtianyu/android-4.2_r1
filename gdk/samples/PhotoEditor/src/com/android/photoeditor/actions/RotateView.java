/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.photoeditor.actions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * View that shows grids and handles touch-events to adjust angle of rotation.
 */
class RotateView extends View {

    /**
     * Listens to rotate changes.
     */
    public interface OnRotateChangeListener {

        void onAngleChanged(float degrees, boolean fromUser);

        void onStartTrackingTouch();

        void onStopTrackingTouch();
    }

    // All angles used are defined between PI and -PI.
    private static final float MATH_PI = (float) Math.PI;
    private static final float MATH_HALF_PI = MATH_PI / 2;
    private static final float RADIAN_TO_DEGREE = 180f / MATH_PI;

    private final Paint dashStrokePaint;
    private final Path grids = new Path();
    private final Path referenceLine = new Path();
    private final RectF referenceLineBounds = new RectF();

    private OnRotateChangeListener listener;
    private int centerX;
    private int centerY;
    private float maxRotatedAngle;
    private float minRotatedAngle;
    private float currentRotatedAngle;
    private float lastRotatedAngle;
    private float touchStartAngle;

    public RotateView(Context context, AttributeSet attrs) {
        super(context, attrs);

        dashStrokePaint = new Paint();
        dashStrokePaint.setAntiAlias(true);
        dashStrokePaint.setStyle(Paint.Style.STROKE);
        dashStrokePaint.setPathEffect(new DashPathEffect(new float[] {15.0f, 5.0f}, 1.0f));
    }

    public void setRotatedAngle(float degrees) {
        currentRotatedAngle = -degrees / RADIAN_TO_DEGREE;
        notifyAngleChange(false);
    }

    /**
     * Sets allowed degrees for rotation span before rotating the view.
     */
    public void setRotateSpan(float degrees) {
        if (degrees >= 360f) {
            maxRotatedAngle = Float.POSITIVE_INFINITY;
        } else {
            maxRotatedAngle = (degrees / RADIAN_TO_DEGREE) / 2;
        }
        minRotatedAngle = -maxRotatedAngle;
    }

    /**
     * Sets grid bounds to be drawn or null to hide grids right before the view is visible.
     */
    public void setGridBounds(RectF bounds) {
        grids.reset();
        referenceLine.reset();
        if (bounds != null) {
            float delta = bounds.width() / 4.0f;
            for (float x = bounds.left + delta; x < bounds.right; x += delta) {
                grids.moveTo(x, bounds.top);
                grids.lineTo(x, bounds.bottom);
            }
            delta = bounds.height() / 4.0f;
            for (float y = bounds.top + delta; y < bounds.bottom; y += delta) {
                grids.moveTo(bounds.left, y);
                grids.lineTo(bounds.right, y);
            }

            // Make reference line long enough to cross the bounds diagonally after being rotated.
            referenceLineBounds.set(bounds);
            float radius = (float) Math.hypot(centerX, centerY);
            delta = radius - centerX;
            referenceLine.moveTo(-delta, centerY);
            referenceLine.lineTo(getWidth() + delta, centerY);

            delta = radius - centerY;
            referenceLine.moveTo(centerX, -delta);
            referenceLine.lineTo(centerX, getHeight() + delta);
        }
        invalidate();
    }

    public void setOnAngleChangeListener(OnRotateChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!grids.isEmpty()) {
            dashStrokePaint.setStrokeWidth(2f);
            dashStrokePaint.setColor(0x99CCCCCC);
            canvas.drawPath(grids, dashStrokePaint);
        }

        if (!referenceLine.isEmpty()) {
            dashStrokePaint.setStrokeWidth(2f);
            dashStrokePaint.setColor(0x99FFCC77);
            canvas.save();
            canvas.clipRect(referenceLineBounds);
            canvas.rotate(-currentRotatedAngle * RADIAN_TO_DEGREE, centerX, centerY);
            canvas.drawPath(referenceLine, dashStrokePaint);
            canvas.restore();
        }
    }

    private float calculateAngle(MotionEvent ev) {
        float x = ev.getX() - centerX;
        float y = centerY - ev.getY();

        float angle;
        if (x == 0) {
            angle = (y >= 0) ? MATH_HALF_PI : -MATH_HALF_PI;
        } else {
            angle = (float) Math.atan(y / x);
        }

        if ((angle >= 0) && (x < 0)) {
            angle = angle - MATH_PI;
        } else if ((angle < 0) && (x < 0)) {
            angle = MATH_PI + angle;
        }
        return angle;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (isEnabled()) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastRotatedAngle = currentRotatedAngle;
                    touchStartAngle = calculateAngle(ev);

                    if (listener != null) {
                        listener.onStartTrackingTouch();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    float touchAngle = calculateAngle(ev);
                    float rotatedAngle = touchAngle - touchStartAngle + lastRotatedAngle;

                    if ((rotatedAngle > maxRotatedAngle) || (rotatedAngle < minRotatedAngle)) {
                        // Angles are out of range; restart rotating.
                        // TODO: Fix discontinuity around boundary.
                        lastRotatedAngle = currentRotatedAngle;
                        touchStartAngle = touchAngle;
                    } else {
                        currentRotatedAngle = rotatedAngle;
                        notifyAngleChange(true);
                        invalidate();
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (listener != null) {
                        listener.onStopTrackingTouch();
                    }
                    break;
            }
        }
        return true;
    }

    private void notifyAngleChange(boolean fromUser) {
        if (listener != null) {
            listener.onAngleChanged(-currentRotatedAngle * RADIAN_TO_DEGREE, fromUser);
        }
    }
}
