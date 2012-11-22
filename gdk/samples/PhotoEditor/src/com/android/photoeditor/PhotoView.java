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

package com.android.photoeditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

import com.android.photoeditor.animation.AnimationPair;

/**
 * Displays photo in the view. All its methods should be called from UI thread.
 */
public class PhotoView extends View {

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Matrix displayMatrix = new Matrix();
    private Photo photo;
    private RectF clipBounds;
    private AnimationPair transitions;

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (photo != null) {
            canvas.save();
            if (clipBounds != null) {
                canvas.clipRect(clipBounds);
            }
            canvas.concat(displayMatrix);
            canvas.drawBitmap(photo.bitmap(), 0, 0, paint);
            canvas.restore();
        }
    }

    /**
     * Maps x and y to a percentage position relative to displayed photo.
     */
    public PointF mapPhotoPoint(float x, float y) {
        if ((photo == null) || (photo.width() == 0) || (photo.height() == 0)) {
            return new PointF();
        }
        float[] point = new float[] {x, y};
        Matrix matrix = new Matrix();
        displayMatrix.invert(matrix);
        matrix.mapPoints(point);
        return new PointF(point[0] / photo.width(), point[1] / photo.height());
    }

    public void mapPhotoPath(Path src, Path dst) {
        // TODO: Use percentages representing paths for saving photo larger than previewed photo.
        Matrix matrix = new Matrix();
        displayMatrix.invert(matrix);
        src.transform(matrix, dst);
    }

    public RectF getPhotoDisplayBounds() {
        RectF bounds = getPhotoBounds();
        displayMatrix.mapRect(bounds);
        return bounds;
    }

    public RectF getPhotoBounds() {
        return (photo != null) ? new RectF(0, 0, photo.width(), photo.height()) : new RectF();
    }

    /**
     * Transforms display by replacing the display matrix of photo-view with the given matrix.
     */
    public void transformDisplay(Matrix matrix) {
        RectF bounds = getPhotoBounds();
        matrix.mapRect(bounds);
        displayMatrix.set(matrix);
        RectUtils.postCenterMatrix(bounds, this, displayMatrix);
        invalidate();
    }

    public void clipPhoto(RectF bounds) {
        clipBounds = bounds;
        invalidate();
    }

    /**
     * Updates the photo with animations (if any) and also updates photo display-matrix.
     */
    public void update(final Photo photo) {
        if (transitions == null) {
            setPhoto(photo);
            invalidate();
        } else if (getAnimation() != null) {
            // Clear old running transitions.
            clearTransitionAnimations();
            setPhoto(photo);
            invalidate();
        } else {
            // TODO: Use AnimationSet to chain two animations.
            transitions.first().setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    post(new Runnable() {

                        @Override
                        public void run() {
                            if ((transitions != null) && (animation == transitions.first())) {
                                startAnimation(transitions.second());
                            }
                        }
                    });
                }
            });
            transitions.second().setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                    setPhoto(photo);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    post(new Runnable() {

                        @Override
                        public void run() {
                            clearTransitionAnimations();
                        }
                    });
                }
            });
            startAnimation(transitions.first());
        }
    }

    private void setPhoto(Photo photo) {
        if (this.photo != null) {
            this.photo.clear();
            this.photo = null;
        }
        this.photo = photo;

        // Scale-down (if necessary) and center the photo for display.
        displayMatrix.reset();
        RectF bounds = getPhotoBounds();
        float scale = RectUtils.getDisplayScale(bounds, this);
        displayMatrix.setScale(scale, scale);
        displayMatrix.mapRect(bounds);
        RectUtils.postCenterMatrix(bounds, this, displayMatrix);
    }

    private void clearTransitionAnimations() {
        if (transitions != null) {
            transitions.first().setAnimationListener(null);
            transitions.second().setAnimationListener(null);
            transitions = null;
            clearAnimation();
        }
    }

    /**
     * Sets transition animations in the next update() to transit out the current bitmap and
     * transit in the new replacing one. Transition animations will be cleared once done.
     */
    public void setTransitionAnimations(AnimationPair transitions) {
        clearTransitionAnimations();
        this.transitions = transitions;
    }
}
