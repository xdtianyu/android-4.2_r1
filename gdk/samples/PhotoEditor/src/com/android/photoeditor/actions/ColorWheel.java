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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.android.photoeditor.R;

/**
 * Wheel that has a draggable thumb to set and get the predefined color set.
 */
class ColorWheel extends View {

    /**
     * Listens to color changes.
     */
    public interface OnColorChangeListener {

        void onColorChanged(int color, boolean fromUser);
    }

    private static final float MATH_PI = (float) Math.PI;
    private static final float MATH_HALF_PI = MATH_PI / 2;

    // All angles used in this object are defined between PI and -PI.
    private static final float ANGLE_SPANNED = MATH_PI * 4 / 3;
    private static final float ANGLE_BEGIN = ANGLE_SPANNED / 2.0f;
    private static final float DEGREES_BEGIN = 360 - (float) Math.toDegrees(ANGLE_BEGIN);
    private static final float STROKE_WIDTH = 3.0f;

    private static final float THUMB_RADIUS_RATIO = 0.363f;
    private static final float INNER_RADIUS_RATIO = 0.173f;

    private static final int PADDING = 4;
    private static final int COLOR_METER_THICKNESS = 18;

    private final Drawable thumb;
    private final Paint fillPaint;
    private final Paint strokePaint;
    private final int thumbSize;
    private final int borderColor;
    private final int[] colorsDefined;
    private final float radiantInterval;
    private Bitmap background;
    private int thumbRadius;
    private int innerRadius;
    private int centerXY;
    private int colorIndex;
    private float angle;
    private boolean dragThumb;
    private OnColorChangeListener listener;

    public ColorWheel(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();
        thumbSize = (int) resources.getDimension(R.dimen.wheel_thumb_size);

        // Set the number of total colors and compute the radiant interval between colors.
        TypedArray colors = resources.obtainTypedArray(R.array.color_picker_wheel_colors);
        colorsDefined = new int[colors.length()];
        for (int c = 0; c < colors.length(); c++) {
            colorsDefined[c] = colors.getColor(c, 0x000000);
        }
        colors.recycle();

        radiantInterval = ANGLE_SPANNED / colorsDefined.length;

        thumb = resources.getDrawable(R.drawable.wheel_knot_selector);
        borderColor = resources.getColor(R.color.color_picker_border_color);

        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(STROKE_WIDTH);
        strokePaint.setStyle(Paint.Style.STROKE);
    }

    public void setColorIndex(int colorIndex) {
        if (updateColorIndex(colorIndex, false)) {
            updateThumbPositionByColorIndex();
        }
    }

    public int getColor() {
        return colorsDefined[colorIndex];
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        startAnimation(AnimationUtils.loadAnimation(getContext(),
                (visibility == VISIBLE) ? R.anim.wheel_show : R.anim.wheel_hide));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int wheelSize = Math.min(w, h) - PADDING;
        thumbRadius = (int) (wheelSize * THUMB_RADIUS_RATIO);
        innerRadius = (int) (wheelSize * INNER_RADIUS_RATIO);

        // The wheel would be centered at (centerXY, centerXY) and have outer-radius centerXY.
        centerXY = wheelSize / 2;
        updateThumbPositionByColorIndex();
    }

    private Bitmap prepareBackground() {
        int diameter = centerXY * 2;
        Bitmap bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // the colors to be selected.
        float radiantDegrees = (float) Math.toDegrees(radiantInterval);
        RectF drawBound = new RectF(0, 0, diameter, diameter);
        for (int c = 0; c < colorsDefined.length; c++) {
            fillPaint.setColor(colorsDefined[c]);
            canvas.drawArc(drawBound, DEGREES_BEGIN + radiantDegrees * c,
                    radiantDegrees, true, fillPaint);
        }

        // clear the inner area.
        fillPaint.setColor(Color.BLACK);
        fillPaint.setAlpha(160);
        canvas.drawCircle(centerXY, centerXY, centerXY - COLOR_METER_THICKNESS, fillPaint);

        // the border for the inner ball
        fillPaint.setColor(borderColor);
        canvas.drawCircle(centerXY, centerXY, innerRadius + STROKE_WIDTH, fillPaint);

        return bitmap;
    }

    private void drawBackground(Canvas canvas) {
        if (background == null) {
            background = prepareBackground();
        }
        canvas.drawBitmap(background, 0, 0, fillPaint);
    }

    private void drawHighlighter(Canvas canvas) {
        strokePaint.setColor(borderColor);
        int diameter = centerXY * 2;
        RectF drawBound = new RectF(0, 0, diameter, diameter);
        float radiantDegrees = (float) Math.toDegrees(radiantInterval);
        float startAngle = DEGREES_BEGIN + radiantDegrees * colorIndex;
        canvas.drawArc(drawBound, startAngle, radiantDegrees, false, strokePaint);
        drawBound.inset(COLOR_METER_THICKNESS, COLOR_METER_THICKNESS);
        canvas.drawArc(drawBound, startAngle, radiantDegrees, false, strokePaint);

        float lineAngle = ANGLE_BEGIN - radiantInterval * colorIndex;
        float cosAngle = (float) Math.cos(lineAngle);
        float sinAngle = (float) Math.sin(lineAngle);
        int innerRadius = centerXY - COLOR_METER_THICKNESS;
        canvas.drawLine(centerXY + centerXY * cosAngle, centerXY - centerXY * sinAngle,
                centerXY + innerRadius * cosAngle,
                centerXY - innerRadius * sinAngle, strokePaint);

        lineAngle -= radiantInterval;
        cosAngle = (float) Math.cos(lineAngle);
        sinAngle = (float) Math.sin(lineAngle);
        canvas.drawLine(centerXY + centerXY * cosAngle, centerXY - centerXY * sinAngle,
                centerXY + innerRadius * cosAngle,
                centerXY - innerRadius * sinAngle, strokePaint);
    }

    private void drawInnerCircle(Canvas canvas) {
        fillPaint.setColor(colorsDefined[colorIndex]);
        canvas.drawCircle(centerXY, centerXY, innerRadius, fillPaint);
    }

    private void drawThumb(Canvas canvas) {
        int thumbX = (int) (thumbRadius * Math.cos(angle) + centerXY);
        int thumbY = (int) (centerXY - thumbRadius * Math.sin(angle));
        int halfSize = thumbSize / 2;
        thumb.setBounds(thumbX - halfSize, thumbY - halfSize, thumbX + halfSize, thumbY + halfSize);
        thumb.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        drawInnerCircle(canvas);
        drawHighlighter(canvas);
        drawThumb(canvas);
    }

    private boolean updateAngle(float x, float y) {
        float angle;
        if (x == 0) {
            if (y >= 0) {
                angle = MATH_HALF_PI;
            } else {
                angle = -MATH_HALF_PI;
            }
        } else {
            angle = (float) Math.atan((double) y / x);
        }

        if (angle >= 0 && x < 0) {
            angle = angle - MATH_PI;
        } else if (angle < 0 && x < 0) {
            angle = MATH_PI + angle;
        }

        if (angle > ANGLE_BEGIN || angle <= ANGLE_BEGIN - ANGLE_SPANNED) {
            return false;
        }

        this.angle = angle;
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (isEnabled()) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    updateThumbState(
                            isHittingThumbArea(ev.getX() - centerXY, centerXY - ev.getY()));
                    break;

                case MotionEvent.ACTION_MOVE:
                    final float x = ev.getX() - centerXY;
                    final float y = centerXY - ev.getY();
                    if (!dragThumb && !updateThumbState(isHittingThumbArea(x, y))) {
                        // The thumb wasn't dragged and isn't being dragged, either.
                        break;
                    }

                    if (updateAngle(x, y)) {
                        int index = (int) ((ANGLE_BEGIN - angle) / radiantInterval);
                        if (updateColorIndex(index, true)) {
                            updateThumbPositionByColorIndex();
                        }
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    updateThumbState(false);
                    break;
            }
        }
        return true;
    }

    /**
     * Returns true if the user is hitting the correct thumb area.
     */
    private boolean isHittingThumbArea(float x, float y) {
        final float radius = (float) Math.sqrt((x * x) + (y * y));
        return (radius > innerRadius) && (radius < centerXY);
    }


    private boolean updateColorIndex(int index, boolean fromUser) {
        if (index < 0 || index >= colorsDefined.length) {
            return false;
        }
        if (colorIndex != index) {
            colorIndex = index;

            if (listener != null) {
                listener.onColorChanged(colorsDefined[colorIndex], fromUser);
            }
            return true;
        }
        return false;
    }

    /**
     * Set the thumb position according to the selected color.
     * The thumb will always be placed in the middle of the selected color.
     */
    private void updateThumbPositionByColorIndex() {
        angle = ANGLE_BEGIN - (colorIndex + 0.5f) * radiantInterval;
        invalidate();
    }

    private boolean updateThumbState(boolean dragThumb) {
        if (this.dragThumb == dragThumb) {
            // The state hasn't been changed; no need for updates.
            return false;
        }

        this.dragThumb = dragThumb;
        thumb.setState(dragThumb ? PRESSED_ENABLED_STATE_SET : ENABLED_STATE_SET);
        invalidate();
        return true;
    }
}
