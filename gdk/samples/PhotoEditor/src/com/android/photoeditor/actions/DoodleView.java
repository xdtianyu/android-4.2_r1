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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Vector;

/**
 * A view that track touch motions as paths and paint them as doodles.
 */
class DoodleView extends View {

    /**
     * Listener of doodle paths.
     */
    public interface OnDoodleChangeListener {

        void onLastPathChanged(Path path);
    }

    private final Vector<ColorPath> colorPaths = new Vector<ColorPath>();
    private final Paint paint = ColorPath.createPaint();

    private OnDoodleChangeListener listener;
    private RectF clipBounds;
    private float lastX;
    private float lastY;

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnDoodleChangeListener(OnDoodleChangeListener listener) {
        this.listener = listener;
    }

    public void startPath(int color) {
        // Remove last empty path before adding a new path.
        if (!colorPaths.isEmpty() && colorPaths.lastElement().path().isEmpty()) {
            colorPaths.remove(colorPaths.size() - 1);
        }
        colorPaths.add(new ColorPath(color, new Path()));
        colorPaths.lastElement().path().moveTo(lastX, lastY);
    }

    /**
     * Clears clip bounds and paths drawn.
     */
    public void clear() {
        colorPaths.clear();
        clipBounds = null;
    }

    /**
     * Clips bounds for paths being drawn.
     */
    public void clipBounds(RectF bounds) {
        clipBounds = bounds;
    }

    private void pathUpdated(Path path) {
        invalidate();
        if (listener != null) {
            listener.onLastPathChanged(path);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (isEnabled() && !colorPaths.isEmpty()) {
            Path path = colorPaths.lastElement().path();
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    pathUpdated(path);
                    lastX = x;
                    lastY = y;
                    break;

                case MotionEvent.ACTION_MOVE:
                    path.quadTo(lastX, lastY, (lastX + x) / 2, (lastY + y) / 2);
                    pathUpdated(path);
                    lastX = x;
                    lastY = y;
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    // Line to last position with offset to draw at least dots for single clicks.
                    path.lineTo(lastX + 1, lastY + 1);
                    pathUpdated(path);
                    break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        if (clipBounds != null) {
            canvas.clipRect(clipBounds);
        }
        for (ColorPath colorPath : colorPaths) {
            colorPath.draw(canvas, paint);
        }
        canvas.restore();
    }
}
