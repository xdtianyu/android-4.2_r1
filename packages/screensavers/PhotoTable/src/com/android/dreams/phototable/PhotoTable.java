/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.dreams.phototable;

import android.service.dreams.DreamService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.Random;

/**
 * A surface where photos sit.
 */
public class PhotoTable extends FrameLayout {
    private static final String TAG = "PhotoTable";
    private static final boolean DEBUG = false;

    class Launcher implements Runnable {
        private final PhotoTable mTable;
        public Launcher(PhotoTable table) {
            mTable = table;
        }

        @Override
        public void run() {
            mTable.scheduleNext(mDropPeriod);
            mTable.launch();
        }
    }

    private static final long MAX_SELECTION_TIME = 10000L;
    private static Random sRNG = new Random();

    private final Launcher mLauncher;
    private final LinkedList<View> mOnTable;
    private final int mDropPeriod;
    private final int mFastDropPeriod;
    private final int mNowDropDelay;
    private final float mImageRatio;
    private final float mTableRatio;
    private final float mImageRotationLimit;
    private final float mThrowRotation;
    private final float mThrowSpeed;
    private final boolean mTapToExit;
    private final int mTableCapacity;
    private final int mRedealCount;
    private final int mInset;
    private final PhotoSourcePlexor mPhotoSource;
    private final Resources mResources;
    private final Interpolator mThrowInterpolator;
    private final Interpolator mDropInterpolator;
    private DreamService mDream;
    private PhotoLaunchTask mPhotoLaunchTask;
    private boolean mStarted;
    private boolean mIsLandscape;
    private int mLongSide;
    private int mShortSide;
    private int mWidth;
    private int mHeight;
    private View mSelected;
    private long mSelectedTime;

    public PhotoTable(Context context, AttributeSet as) {
        super(context, as);
        mResources = getResources();
        mInset = mResources.getDimensionPixelSize(R.dimen.photo_inset);
        mDropPeriod = mResources.getInteger(R.integer.table_drop_period);
        mFastDropPeriod = mResources.getInteger(R.integer.fast_drop);
        mNowDropDelay = mResources.getInteger(R.integer.now_drop);
        mImageRatio = mResources.getInteger(R.integer.image_ratio) / 1000000f;
        mTableRatio = mResources.getInteger(R.integer.table_ratio) / 1000000f;
        mImageRotationLimit = (float) mResources.getInteger(R.integer.max_image_rotation);
        mThrowSpeed = mResources.getDimension(R.dimen.image_throw_speed);
        mThrowRotation = (float) mResources.getInteger(R.integer.image_throw_rotatioan);
        mTableCapacity = mResources.getInteger(R.integer.table_capacity);
        mRedealCount = mResources.getInteger(R.integer.redeal_count);
        mTapToExit = mResources.getBoolean(R.bool.enable_tap_to_exit);
        mThrowInterpolator = new SoftLandingInterpolator(
                mResources.getInteger(R.integer.soft_landing_time) / 1000000f,
                mResources.getInteger(R.integer.soft_landing_distance) / 1000000f);
        mDropInterpolator = new DecelerateInterpolator(
                (float) mResources.getInteger(R.integer.drop_deceleration_exponent));
        mOnTable = new LinkedList<View>();
        mPhotoSource = new PhotoSourcePlexor(getContext(),
                getContext().getSharedPreferences(PhotoTableDreamSettings.PREFS_NAME, 0));
        mLauncher = new Launcher(this);
        mStarted = false;
    }

    
    public void setDream(DreamService dream) {
        mDream = dream;
    }

    public boolean hasSelection() {
        return mSelected != null;
    }

    public View getSelected() {
        return mSelected;
    }

    public void clearSelection() {
        mSelected = null;
    }

    public void setSelection(View selected) {
        assert(selected != null);
        if (mSelected != null) {
            dropOnTable(mSelected);
        }
        mSelected = selected;
        mSelectedTime = System.currentTimeMillis();
        bringChildToFront(selected);
        pickUp(selected);
    }

    static float lerp(float a, float b, float f) {
        return (b-a)*f + a;
    }

    static float randfrange(float a, float b) {
        return lerp(a, b, sRNG.nextFloat());
    }

    static PointF randFromCurve(float t, PointF[] v) {
        PointF p = new PointF();
        if (v.length == 4 && t >= 0f && t <= 1f) {
            float a = (float) Math.pow(1f-t, 3f);
            float b = (float) Math.pow(1f-t, 2f) * t;
            float c = (1f-t) * (float) Math.pow(t, 2f);
            float d = (float) Math.pow(t, 3f);

            p.x = a * v[0].x + 3 * b * v[1].x + 3 * c * v[2].x + d * v[3].x;
            p.y = a * v[0].y + 3 * b * v[1].y + 3 * c * v[2].y + d * v[3].y;
        }
        return p;
    }

    private static PointF randInCenter(float i, float j, int width, int height) {
        log("randInCenter (" + i + ", " + j + ", " + width + ", " + height + ")");
        PointF p = new PointF();
        p.x = 0.5f * width + 0.15f * width * i;
        p.y = 0.5f * height + 0.15f * height * j;
        log("randInCenter returning " + p.x + "," + p.y);
        return p;
    }

    private static PointF randMultiDrop(int n, float i, float j, int width, int height) {
        log("randMultiDrop (" + n + "," + i + ", " + j + ", " + width + ", " + height + ")");
        final float[] cx = {0.3f, 0.3f, 0.5f, 0.7f, 0.7f};
        final float[] cy = {0.3f, 0.7f, 0.5f, 0.3f, 0.7f};
        n = Math.abs(n);
        float x = cx[n % cx.length];
        float y = cy[n % cx.length];
        PointF p = new PointF();
        p.x = x * width + 0.05f * width * i;
        p.y = y * height + 0.05f * height * j;
        log("randInCenter returning " + p.x + "," + p.y);
        return p;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (hasSelection()) {
                dropOnTable(getSelected());
                clearSelection();
            } else  {
                if (mTapToExit && mDream != null) {
                    mDream.finish();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        log("onLayout (" + left + ", " + top + ", " + right + ", " + bottom + ")");

        mHeight = bottom - top;
        mWidth = right - left;

        mLongSide = (int) (mImageRatio * Math.max(mWidth, mHeight));
        mShortSide = (int) (mImageRatio * Math.min(mWidth, mHeight));

        boolean isLandscape = mWidth > mHeight;
        if (mIsLandscape != isLandscape) {
            for (View photo: mOnTable) {
                if (photo == getSelected()) {
                    pickUp(photo);
                } else {
                    dropOnTable(photo);
                }
            }
            mIsLandscape = isLandscape;
        }
        start();
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    private class PhotoLaunchTask extends AsyncTask<Void, Void, View> {
        private final BitmapFactory.Options mOptions;

        public PhotoLaunchTask () {
            mOptions = new BitmapFactory.Options();
            mOptions.inTempStorage = new byte[32768];
        }

        @Override
        public View doInBackground(Void... unused) {
            log("load a new photo");
            final PhotoTable table = PhotoTable.this;

            LayoutInflater inflater = (LayoutInflater) table.getContext()
                   .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View photo = inflater.inflate(R.layout.photo, null);
            ImageView image = (ImageView) photo;
            Drawable[] layers = new Drawable[2];
            Bitmap decodedPhoto = table.mPhotoSource.next(mOptions,
                    table.mLongSide, table.mShortSide);
            int photoWidth = mOptions.outWidth;
            int photoHeight = mOptions.outHeight;
            if (mOptions.outWidth <= 0 || mOptions.outHeight <= 0) {
                photo = null;
            } else {
                decodedPhoto.setHasMipMap(true);
                layers[0] = new BitmapDrawable(table.mResources, decodedPhoto);
                layers[1] = table.mResources.getDrawable(R.drawable.frame);
                LayerDrawable layerList = new LayerDrawable(layers);
                layerList.setLayerInset(0, table.mInset, table.mInset,
                                        table.mInset, table.mInset);
                image.setImageDrawable(layerList);

                photo.setTag(R.id.photo_width, new Integer(photoWidth));
                photo.setTag(R.id.photo_height, new Integer(photoHeight));

                photo.setOnTouchListener(new PhotoTouchListener(table.getContext(),
                                                                table));
            }

            return photo;
        }

        @Override
        public void onPostExecute(View photo) {
            if (photo != null) {
                final PhotoTable table = PhotoTable.this;

                table.addView(photo, new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                       LayoutParams.WRAP_CONTENT));
                if (table.hasSelection()) {
                    table.bringChildToFront(table.getSelected());
                }
                int width = ((Integer) photo.getTag(R.id.photo_width)).intValue();
                int height = ((Integer) photo.getTag(R.id.photo_height)).intValue();

                log("drop it");
                table.throwOnTable(photo);

                if(table.mOnTable.size() < table.mTableCapacity) {
                    table.scheduleNext(table.mFastDropPeriod);
                }
            }
        }
    };

    public void launch() {
        log("launching");
        setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        if (hasSelection() &&
                (System.currentTimeMillis() - mSelectedTime) > MAX_SELECTION_TIME) {
            dropOnTable(getSelected());
            clearSelection();
        } else {
            log("inflate it");
            if (mPhotoLaunchTask == null ||
                mPhotoLaunchTask.getStatus() == AsyncTask.Status.FINISHED) {
                mPhotoLaunchTask = new PhotoLaunchTask();
                mPhotoLaunchTask.execute();
            }
        }
    }
    public void fadeAway(final View photo, final boolean replace) {
        // fade out of view
        mOnTable.remove(photo);
        photo.animate().cancel();
        photo.animate()
                .withLayer()
                .alpha(0f)
                .setDuration(1000)
                .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            removeView(photo);
                            recycle(photo);
                            if (replace) {
                                scheduleNext(mNowDropDelay);
                            }
                        }
                    });
    }

    public void moveToBackOfQueue(View photo) {
        // make this photo the last to be removed.
        bringChildToFront(photo);
        invalidate();
        mOnTable.remove(photo);
        mOnTable.offer(photo);
    }

    private void throwOnTable(final View photo) {
        mOnTable.offer(photo);
        log("start offscreen");
        int width = ((Integer) photo.getTag(R.id.photo_width));
        int height = ((Integer) photo.getTag(R.id.photo_height));
        photo.setRotation(mThrowRotation);
        photo.setX(-mLongSide);
        photo.setY(-mLongSide);

        dropOnTable(photo, mThrowInterpolator);
    }

    public void dropOnTable(final View photo) {
        dropOnTable(photo, mDropInterpolator);
    }

    public void dropOnTable(final View photo, final Interpolator interpolator) {
        float angle = randfrange(-mImageRotationLimit, mImageRotationLimit);
        PointF p = randMultiDrop(sRNG.nextInt(),
                                 (float) sRNG.nextGaussian(), (float) sRNG.nextGaussian(),
                                 mWidth, mHeight);
        float x = p.x;
        float y = p.y;

        log("drop it at " + x + ", " + y);

        float x0 = photo.getX();
        float y0 = photo.getY();
        float width = (float) ((Integer) photo.getTag(R.id.photo_width)).intValue();
        float height = (float) ((Integer) photo.getTag(R.id.photo_height)).intValue();

        x -= mLongSide / 2f;
        y -= mShortSide / 2f;
        log("fixed offset is " + x + ", " + y);

        float dx = x - x0;
        float dy = y - y0;

        float dist = (float) (Math.sqrt(dx * dx + dy * dy));
        int duration = (int) (1000f * dist / mThrowSpeed);
        duration = Math.max(duration, 1000);

        log("animate it");
        // toss onto table
        photo.animate()
                .scaleX(mTableRatio / mImageRatio)
                .scaleY(mTableRatio / mImageRatio)
                .rotation(angle)
                .x(x)
                .y(y)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .withEndAction(new Runnable() {
                        @Override
                            public void run() {
                            if (mOnTable.size() > mTableCapacity) {
                                while (mOnTable.size() > (mTableCapacity - mRedealCount)) {
                                    fadeAway(mOnTable.poll(), false);
                                }
                                // zero delay because we already waited duration ms
                                scheduleNext(0);
                            }
                        }
                    });
    }

    /** wrap all orientations to the interval [-180, 180). */
    private float wrapAngle(float angle) {
        float result = angle + 180;
        result = ((result % 360) + 360) % 360; // catch negative numbers
        result -= 180;
        return result;
    }

    private void pickUp(final View photo) {
        float photoWidth = photo.getWidth();
        float photoHeight = photo.getHeight();

        float scale = Math.min(getHeight() / photoHeight, getWidth() / photoWidth);

        log("target it");
        float x = (getWidth() - photoWidth) / 2f;
        float y = (getHeight() - photoHeight) / 2f;

        float x0 = photo.getX();
        float y0 = photo.getY();
        float dx = x - x0;
        float dy = y - y0;

        float dist = (float) (Math.sqrt(dx * dx + dy * dy));
        int duration = (int) (1000f * dist / 600f);
        duration = Math.max(duration, 500);

        photo.setRotation(wrapAngle(photo.getRotation()));

        log("animate it");
        // toss onto table
        photo.animate()
                .rotation(0f)
                .scaleX(scale)
                .scaleY(scale)
                .x(x)
                .y(y)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator(2f))
                .withEndAction(new Runnable() {
                        @Override
                            public void run() {
                            log("endtimes: " + photo.getX());
                        }
                    });
    }

    private void recycle(View photo) {
        ImageView image = (ImageView) photo;
        LayerDrawable layers = (LayerDrawable) image.getDrawable();
        BitmapDrawable bitmap = (BitmapDrawable) layers.getDrawable(0);
        bitmap.getBitmap().recycle();
    }

    public void start() {
        if (!mStarted) {
            log("kick it");
            mStarted = true;
            scheduleNext(mDropPeriod);
            launch();
        }
    }

    public void scheduleNext(int delay) {
        removeCallbacks(mLauncher);
        postDelayed(mLauncher, delay);
    }

    private static void log(String message) {
        if (DEBUG) {
            Log.i(TAG, message);
        }
    }
}
