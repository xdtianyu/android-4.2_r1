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

import com.android.photoeditor.FilterStack;
import com.android.photoeditor.R;
import com.android.photoeditor.animation.Rotate3DAnimation;
import com.android.photoeditor.filters.FlipFilter;

/**
 * An action handling flip effect.
 */
public class FlipAction extends FilterAction {

    private static final int ANIMATION_DURATION = 500;

    private boolean flipHorizontal;
    private boolean flipVertical;

    public FlipAction(FilterStack filterStack, ViewGroup tools) {
        super(filterStack, tools, R.string.flip_tooltip);
    }

    @Override
    public void onBegin() {
        final FlipFilter filter = new FlipFilter();

        touchView.setSwipeListener(new TouchView.SwipeListener() {

            @Override
            public void onSwipeDown() {
                setFlipAnimations(Rotate3DAnimation.Rotate.DOWN);
                flipFilterVertically(filter);
            }

            @Override
            public void onSwipeLeft() {
                setFlipAnimations(Rotate3DAnimation.Rotate.LEFT);
                flipFilterHorizontally(filter);
            }

            @Override
            public void onSwipeRight() {
                setFlipAnimations(Rotate3DAnimation.Rotate.RIGHT);
                flipFilterHorizontally(filter);
            }

            @Override
            public void onSwipeUp() {
                setFlipAnimations(Rotate3DAnimation.Rotate.UP);
                flipFilterVertically(filter);
            }
        });
        touchView.setVisibility(View.VISIBLE);

        flipHorizontal = false;
        flipVertical = false;
        setFlipAnimations(Rotate3DAnimation.Rotate.RIGHT);
        flipFilterHorizontally(filter);
    }

    @Override
    public void onEnd() {
    }

    private void setFlipAnimations(Rotate3DAnimation.Rotate rotate) {
        photoView.setTransitionAnimations(
                Rotate3DAnimation.getFlipAnimations(rotate, ANIMATION_DURATION));
    }

    private void flipFilterHorizontally(final FlipFilter filter) {
        flipHorizontal = !flipHorizontal;
        filter.setFlip(flipHorizontal, flipVertical);
        notifyFilterChanged(filter, true);
    }

    private void flipFilterVertically(final FlipFilter filter) {
        flipVertical = !flipVertical;
        filter.setFlip(flipHorizontal, flipVertical);
        notifyFilterChanged(filter, true);
    }
}
