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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.android.photoeditor.R;

/**
 * Wheel that has a draggable thumb to set and get the normalized scale value from 0 to 1.
 */
class ScaleWheel extends View {

    /**
     * Listens to scale changes.
     */
    public interface OnScaleChangeListener {

        void onProgressChanged(float progress, boolean fromUser);
    }

    private static final float MATH_PI = (float) Math.PI;
    private static final float MATH_HALF_PI = MATH_PI / 2;

    // Angles are defined between PI and -PI.
    private static final float ANGLE_SPANNED = MATH_PI * 4 / 3;
    private static final float ANGLE_BEGIN = ANGLE_SPANNED / 2.0f;

    private static final float THUMB_RADIUS_RATIO = 0.363f;
    private static final float INNER_RADIUS_RATIO = 0.24f;

    private final Drawable thumb;
    private final Drawable background;
    private final int thumbSize;
    private final Paint circlePaint;
    private final int maxProgress;
    private final float radiantInterval;
    private int thumbRadius;
    private int innerRadius;
    private int centerXY;
    private float angle;
    private int progress;
    private boolean dragThumb;
    private OnScaleChangeListener listener;

    public ScaleWheel(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();
        thumbSize = (int) resources.getDimension(R.dimen.wheel_thumb_size);

        // Set the maximum progress and compute the radiant interval between progress values.
        maxProgress = 100;
        radiantInterval = ANGLE_SPANNED / maxProgress;

        thumb = resources.getDrawable(R.drawable.wheel_knot_selector);
        background = resources.getDrawable(R.drawable.scale_wheel_background);
        background.setAlpha(160);

        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(resources.getColor(R.color.scale_wheel_interior_color));
    }

    public void setProgress(float progress) {
        if (updateProgress((int) (progress * maxProgress), false)) {
            updateThumbPositionByProgress();
        }
    }

    public void setOnScaleChangeListener(OnScaleChangeListener listener) {
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

        int wheelSize = Math.min(w, h);
        thumbRadius = (int) (wheelSize * THUMB_RADIUS_RATIO);
        innerRadius = (int) (wheelSize * INNER_RADIUS_RATIO);

        // The wheel would be centered at (centerXY, centerXY) and have outer-radius centerXY.
        centerXY = wheelSize / 2;
        updateThumbPositionByProgress();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        background.setBounds(0, 0, getWidth(), getHeight());
        background.draw(canvas);

        int thumbX = (int) (thumbRadius * Math.cos(angle) + centerXY);
        int thumbY = (int) (centerXY - thumbRadius * Math.sin(angle));
        int halfSize = thumbSize / 2;
        thumb.setBounds(thumbX - halfSize, thumbY - halfSize, thumbX + halfSize, thumbY + halfSize);
        thumb.draw(canvas);

        float radius = (progress * innerRadius) / maxProgress;
        canvas.drawCircle(centerXY, centerXY, radius, circlePaint);
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
                    float x = ev.getX() - centerXY;
                    float y = centerXY - ev.getY();
                    if (!dragThumb && !updateThumbState(isHittingThumbArea(x, y))) {
                        // The thumb wasn't dragged and isn't being dragged, either.
                        break;
                    }

                    if (updateAngle(x, y)) {
                        int progress = (int) ((ANGLE_BEGIN - angle) / radiantInterval);
                        if (updateProgress(progress, true)) {
                            invalidate();
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

    private boolean isHittingThumbArea(float x, float y) {
        double radius = Math.sqrt((x * x) + (y * y));
        return (radius > innerRadius) && (radius < centerXY);
    }

    private boolean updateAngle(float x, float y) {
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

        if ((angle > ANGLE_BEGIN) || (angle <= ANGLE_BEGIN - ANGLE_SPANNED)) {
            return false;
        }

        this.angle = angle;
        return true;
    }

    private boolean updateProgress(int progress, boolean fromUser) {
        if ((this.progress != progress) && (progress >= 0) && (progress <= maxProgress)) {
            this.progress = progress;

            if (listener != null) {
                listener.onProgressChanged((float) progress / maxProgress, fromUser);
            }
            return true;
        }
        return false;
    }

    private void updateThumbPositionByProgress() {
        angle = ANGLE_BEGIN - progress * radiantInterval;
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
