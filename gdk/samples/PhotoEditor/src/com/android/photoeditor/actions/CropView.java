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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.RegionIterator;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.photoeditor.R;

import java.util.Vector;

/**
 * A view that track touch motions and adjust crop bounds accordingly.
 */
class CropView extends View {

    /**
     * Listener of crop bounds.
     */
    public interface OnCropChangeListener {

        void onCropChanged(RectF bounds, boolean fromUser);
    }

    private static final int TOUCH_AREA_NONE = 0;
    private static final int TOUCH_AREA_LEFT = 1;
    private static final int TOUCH_AREA_TOP = 2;
    private static final int TOUCH_AREA_RIGHT = 4;
    private static final int TOUCH_AREA_BOTTOM = 8;
    private static final int TOUCH_AREA_INSIDE = 15;
    private static final int TOUCH_AREA_OUTSIDE = 16;
    private static final int TOUCH_AREA_TOP_LEFT = 3;
    private static final int TOUCH_AREA_TOP_RIGHT = 6;
    private static final int TOUCH_AREA_BOTTOM_LEFT = 9;
    private static final int TOUCH_AREA_BOTTOM_RIGHT = 12;

    private static final int BORDER_COLOR = 0xFF008AFF;
    private static final int OUTER_COLOR = 0xA0000000;
    private static final int INDICATION_COLOR = 0xFFCC9900;
    private static final int TOUCH_AREA_SPAN = 20;
    private static final int TOUCH_AREA_SPAN2 = TOUCH_AREA_SPAN * 2;
    private static final float BORDER_WIDTH = 2.0f;

    private final Paint outerAreaPaint;
    private final Paint borderPaint;
    private final Paint highlightPaint;

    private final Drawable heightIndicator;
    private final Drawable widthIndicator;
    private final int indicatorSize;

    private RectF cropBounds;
    private RectF photoBounds;

    private OnCropChangeListener listener;

    private float lastX;
    private float lastY;
    private int currentTouchArea;

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();
        heightIndicator = resources.getDrawable(R.drawable.crop_height_holo);
        widthIndicator = resources.getDrawable(R.drawable.crop_width_holo);
        indicatorSize = (int) resources.getDimension(R.dimen.crop_indicator_size);

        outerAreaPaint = new Paint();
        outerAreaPaint.setStyle(Paint.Style.FILL);
        outerAreaPaint.setColor(OUTER_COLOR);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(BORDER_COLOR);
        borderPaint.setStrokeWidth(BORDER_WIDTH);

        highlightPaint = new Paint();
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setColor(INDICATION_COLOR);
        highlightPaint.setStrokeWidth(BORDER_WIDTH);

        currentTouchArea = TOUCH_AREA_NONE;
    }

    public void setOnCropChangeListener(OnCropChangeListener listener) {
        this.listener = listener;
    }

    private void notifyCropChange(boolean fromUser) {
        if (listener != null) {
            listener.onCropChanged(cropBounds, fromUser);
        }
    }

    public void setCropBounds(RectF bounds) {
        bounds.intersect(photoBounds);
        cropBounds = bounds;
        if (photoBounds.width() <= TOUCH_AREA_SPAN2) {
            cropBounds.left = photoBounds.left;
            cropBounds.right = photoBounds.right;
        }
        if (photoBounds.height() <= TOUCH_AREA_SPAN2) {
            cropBounds.top = photoBounds.top;
            cropBounds.bottom = photoBounds.bottom;
        }
        notifyCropChange(false);
        invalidate();
    }

    /**
     * Sets bounds to crop within.
     */
    public void setPhotoBounds(RectF bounds) {
        photoBounds = bounds;
    }

    public boolean fullPhotoCropped() {
        return cropBounds.contains(photoBounds);
    }

    private int detectTouchArea(float x, float y) {
        RectF area = new RectF();
        area.set(cropBounds);
        area.inset(-TOUCH_AREA_SPAN, -TOUCH_AREA_SPAN);
        if (!area.contains(x, y)) {
            return TOUCH_AREA_OUTSIDE;
        }

        // left
        area.set(cropBounds.left - TOUCH_AREA_SPAN, cropBounds.top  + TOUCH_AREA_SPAN,
                cropBounds.left + TOUCH_AREA_SPAN, cropBounds.bottom - TOUCH_AREA_SPAN);
        if (area.contains(x, y)) {
            return TOUCH_AREA_LEFT;
        }
        // right
        area.offset(cropBounds.width(), 0f);
        if (area.contains(x, y)) {
            return TOUCH_AREA_RIGHT;
        }
        // top
        area.set(cropBounds.left + TOUCH_AREA_SPAN, cropBounds.top - TOUCH_AREA_SPAN,
                cropBounds.right - TOUCH_AREA_SPAN, cropBounds.top + TOUCH_AREA_SPAN);
        if (area.contains(x, y)) {
            return TOUCH_AREA_TOP;
        }
        // bottom
        area.offset(0f, cropBounds.height());
        if (area.contains(x, y)) {
            return TOUCH_AREA_BOTTOM;
        }
        // top left
        area.set(cropBounds.left - TOUCH_AREA_SPAN, cropBounds.top - TOUCH_AREA_SPAN,
                cropBounds.left + TOUCH_AREA_SPAN, cropBounds.top + TOUCH_AREA_SPAN);
        if (area.contains(x, y)) {
            return TOUCH_AREA_TOP_LEFT;
        }
        // top right
        area.offset(cropBounds.width(), 0f);
        if (area.contains(x, y)) {
            return TOUCH_AREA_TOP_RIGHT;
        }
        // bottom right
        area.offset(0f, cropBounds.height());
        if (area.contains(x, y)) {
            return TOUCH_AREA_BOTTOM_RIGHT;
        }
        // bottom left
        area.offset(-cropBounds.width(), 0f);
        if (area.contains(x, y)) {
            return TOUCH_AREA_BOTTOM_LEFT;
        }
        return TOUCH_AREA_INSIDE;
    }

    private void performMove(float deltaX, float deltaY) {
        if (currentTouchArea == TOUCH_AREA_INSIDE){  // moving the rect.
            cropBounds.offset(deltaX, deltaY);
            if (cropBounds.left < photoBounds.left) {
                cropBounds.offset(photoBounds.left - cropBounds.left, 0f);
            } else if (cropBounds.right > photoBounds.right) {
                cropBounds.offset(photoBounds.right - cropBounds.right, 0f);
            }
            if (cropBounds.top < photoBounds.top) {
                cropBounds.offset(0f, photoBounds.top - cropBounds.top);
            } else if (cropBounds.bottom > photoBounds.bottom) {
                cropBounds.offset(0f, photoBounds.bottom - cropBounds.bottom);
            }
        } else {  // adjusting bounds.
            if ((currentTouchArea & TOUCH_AREA_LEFT) != 0) {
                cropBounds.left = Math.min(cropBounds.left + deltaX,
                        cropBounds.right - TOUCH_AREA_SPAN2);
            }
            if ((currentTouchArea & TOUCH_AREA_TOP) != 0) {
                cropBounds.top = Math.min(cropBounds.top + deltaY,
                        cropBounds.bottom - TOUCH_AREA_SPAN2);
            }
            if ((currentTouchArea & TOUCH_AREA_RIGHT) != 0) {
                cropBounds.right = Math.max(cropBounds.right + deltaX,
                        cropBounds.left + TOUCH_AREA_SPAN2);
            }
            if ((currentTouchArea & TOUCH_AREA_BOTTOM) != 0) {
                cropBounds.bottom = Math.max(cropBounds.bottom + deltaY,
                        cropBounds.top + TOUCH_AREA_SPAN2);
            }
            cropBounds.intersect(photoBounds);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (!isEnabled()) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentTouchArea = detectTouchArea(x, y);
                lastX = x;
                lastY = y;
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                performMove(x - lastX, y - lastY);

                lastX = x;
                lastY = y;
                notifyCropChange(true);
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                currentTouchArea = TOUCH_AREA_NONE;
                invalidate();
                break;
        }
        return true;
    }

    private void drawIndicator(Canvas canvas, Drawable indicator, float centerX, float centerY) {
        int left = (int) centerX - indicatorSize / 2;
        int top = (int) centerY - indicatorSize / 2;
        int right = left + indicatorSize;
        int bottom = top + indicatorSize;
        indicator.setBounds(left, top, right, bottom);
        indicator.draw(canvas);
    }

    private void drawIndicators(Canvas canvas) {
        drawIndicator(canvas, heightIndicator, cropBounds.centerX(), cropBounds.top);
        drawIndicator(canvas, heightIndicator, cropBounds.centerX(), cropBounds.bottom);
        drawIndicator(canvas, widthIndicator, cropBounds.left, cropBounds.centerY());
        drawIndicator(canvas, widthIndicator, cropBounds.right, cropBounds.centerY());
    }

    private void drawTouchHighlights(Canvas canvas) {
        if ((currentTouchArea & TOUCH_AREA_TOP) != 0) {
            canvas.drawLine(cropBounds.left, cropBounds.top, cropBounds.right, cropBounds.top,
                    highlightPaint);
        }
        if ((currentTouchArea & TOUCH_AREA_BOTTOM) != 0) {
            canvas.drawLine(cropBounds.left, cropBounds.bottom, cropBounds.right,
                    cropBounds.bottom, highlightPaint);
        }
        if ((currentTouchArea & TOUCH_AREA_LEFT) != 0) {
            canvas.drawLine(cropBounds.left, cropBounds.top, cropBounds.left, cropBounds.bottom,
                    highlightPaint);
        }
        if ((currentTouchArea & TOUCH_AREA_RIGHT) != 0) {
            canvas.drawLine(cropBounds.right, cropBounds.top, cropBounds.right, cropBounds.bottom,
                    highlightPaint);
        }
    }

    private void drawBounds(Canvas canvas) {
        Rect r = new Rect();
        photoBounds.roundOut(r);
        Region drawRegion = new Region(r);
        cropBounds.roundOut(r);
        drawRegion.op(r, Region.Op.DIFFERENCE);
        RegionIterator iter = new RegionIterator(drawRegion);
        while (iter.next(r)) {
            canvas.drawRect(r, outerAreaPaint);
        }

        canvas.drawRect(cropBounds, borderPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBounds(canvas);
        if (currentTouchArea != TOUCH_AREA_NONE) {
            drawTouchHighlights(canvas);
            drawIndicators(canvas);
        }
    }
}
