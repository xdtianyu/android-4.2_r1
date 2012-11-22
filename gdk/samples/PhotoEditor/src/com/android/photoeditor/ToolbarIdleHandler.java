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

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler that controls idle/awake behaviors of toolbar's child views.
 */
class ToolbarIdleHandler {

    private static final String IDLE_VIEW_TAG = "fadeOnIdle";
    private static final int MAKE_IDLE = 1;
    private static final int TIMEOUT_IDLE = 8000;

    private final List<View> childViews = new ArrayList<View>();
    private final Handler mainHandler;
    private final Animation fadeIn;
    private final Animation fadeOut;
    private boolean idle;

    /**
     * Constructor should only be invoked after toolbar has done inflation and added all its child
     * views; then its child views could be found by findViewById calls.
     */
    public ToolbarIdleHandler(ViewGroup toolbar) {
        mainHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MAKE_IDLE:
                        if (!idle) {
                            idle = true;
                            for (View view : childViews) {
                                makeIdle(view);
                            }
                        }
                        break;
                }
            }
        };

        fadeIn = AnimationUtils.loadAnimation(toolbar.getContext(), R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(toolbar.getContext(), R.anim.fade_out);

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            String tag = (String) view.getTag();
            if ((tag != null) && tag.equals(IDLE_VIEW_TAG)) {
                childViews.add(view);
            }
        }
        // Alpha animations don't work well on scroll-view; apply them on container linear-layout.
        childViews.add(toolbar.findViewById(R.id.effects_container));
    }

    public void killIdle() {
        mainHandler.removeMessages(MAKE_IDLE);
        if (idle) {
            idle = false;
            for (View view : childViews) {
                makeAwake(view);
            }
        }
        mainHandler.sendEmptyMessageDelayed(MAKE_IDLE, TIMEOUT_IDLE);
    }

    private void makeAwake(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.startAnimation(fadeIn);
        }
    }

    private void makeIdle(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.startAnimation(fadeOut);
        }
    }
}
