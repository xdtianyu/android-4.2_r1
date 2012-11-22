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

import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.Photo;
import com.android.photoeditor.PhotoOutputCallback;
import com.android.photoeditor.PhotoView;
import com.android.photoeditor.R;
import com.android.photoeditor.SpinnerProgressDialog;
import com.android.photoeditor.filters.Filter;

/**
 * An action binding UI controls and filter operation for editing photo.
 */
public abstract class FilterAction {

    /**
     * Listens to when this FilterAction has done editing and should be ended.
     */
    public interface FilterActionListener {

        void onDone();
    }

    protected final FilterStack filterStack;
    protected final PhotoView photoView;
    protected final ScaleWheel scaleWheel;
    protected final ColorWheel colorWheel;
    protected final DoodleView doodleView;
    protected final TouchView touchView;
    protected final RotateView rotateView;
    protected final CropView cropView;
    private final ViewGroup tools;

    private Toast tooltip;
    private boolean pushedFilter;
    private OutputCallback lastOutputCallback;
    private FilterActionListener listener;

    public FilterAction(FilterStack filterStack, ViewGroup tools) {
        this.filterStack = filterStack;
        this.tools = tools;

        photoView = (PhotoView) tools.findViewById(R.id.photo_view);
        scaleWheel = (ScaleWheel) tools.findViewById(R.id.scale_wheel);
        colorWheel = (ColorWheel) tools.findViewById(R.id.color_wheel);
        doodleView = (DoodleView) tools.findViewById(R.id.doodle_view);
        touchView = (TouchView) tools.findViewById(R.id.touch_view);
        rotateView = (RotateView) tools.findViewById(R.id.rotate_view);
        cropView = (CropView) tools.findViewById(R.id.crop_view);
    }

    public FilterAction(FilterStack filterStack, ViewGroup tools, int tooltipId) {
        this(filterStack, tools);

        tooltip = Toast.makeText(tools.getContext(), tooltipId, Toast.LENGTH_SHORT);
    }

    protected void notifyFilterChanged(Filter filter, boolean output) {
        if (!pushedFilter && filter.isValid()) {
            filterStack.pushFilter(filter);
            pushedFilter = true;
        }
        if (pushedFilter && output) {
            // Notify the stack to output the changed top filter.
            lastOutputCallback = new OutputCallback();
            filterStack.topFilterChanged(lastOutputCallback);
        }
    }

    public void begin(FilterActionListener listener) {
        this.listener = listener;
        if (tooltip != null) {
            tooltip.show();
        }
        onBegin();
    }

    public void end() {
        onEnd();

        // Wait till last output callback is done before finishing.
        if ((lastOutputCallback == null) || lastOutputCallback.done) {
            finish();
        } else {
            final SpinnerProgressDialog progressDialog = SpinnerProgressDialog.show(tools);
            lastOutputCallback.runnableOnDone = new Runnable() {

                @Override
                public void run() {
                    progressDialog.dismiss();
                    finish();
                }
            };
        }
    }

    private void finish() {
        // Close the tooltip if it's still showing.
        if ((tooltip != null) && (tooltip.getView().getParent() != null)) {
            tooltip.cancel();
        }
        if (scaleWheel.getVisibility() == View.VISIBLE) {
            scaleWheel.setOnScaleChangeListener(null);
            scaleWheel.setVisibility(View.INVISIBLE);
        }
        if (colorWheel.getVisibility() == View.VISIBLE) {
            colorWheel.setOnColorChangeListener(null);
            colorWheel.setVisibility(View.INVISIBLE);
        }
        if (doodleView.getVisibility() == View.VISIBLE) {
            doodleView.setOnDoodleChangeListener(null);
            doodleView.setVisibility(View.INVISIBLE);
        }
        if (touchView.getVisibility() == View.VISIBLE) {
            touchView.setSingleTapListener(null);
            touchView.setSwipeListener(null);
            touchView.setVisibility(View.INVISIBLE);
        }
        if (rotateView.getVisibility() == View.VISIBLE) {
            rotateView.setOnAngleChangeListener(null);
            rotateView.setVisibility(View.INVISIBLE);
        }
        if (cropView.getVisibility() == View.VISIBLE) {
            cropView.setOnCropChangeListener(null);
            cropView.setVisibility(View.INVISIBLE);
        }
        photoView.clipPhoto(null);
        // Notify the listener that this action is done finishing.
        listener.onDone();
        listener = null;
        lastOutputCallback = null;
        pushedFilter = false;
    }

    /**
     * Called when the action is about to begin; subclasses should creates a specific filter and
     * binds the filter to necessary UI controls here.
     */
    protected abstract void onBegin();

    /**
     * Called when the action is about to end; subclasses could do specific ending operations here.
     */
    protected abstract void onEnd();

    /**
     * Output callback for top filter changes.
     */
    private class OutputCallback implements PhotoOutputCallback {

        private boolean done;
        private Runnable runnableOnDone;

        @Override
        public void onReady(Photo photo) {
            photoView.update(photo);
            done = true;

            if (runnableOnDone != null) {
                runnableOnDone.run();
            }
        }
    }
}
