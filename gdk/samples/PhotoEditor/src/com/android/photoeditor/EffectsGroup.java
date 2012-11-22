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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups effects in an accordion menu style that could either expand or fold effects.
 */
public class EffectsGroup extends LinearLayout {

    private static final int ANIMATION_INTERVAL = 75;

    private final Drawable downArrow;
    private final Drawable rightArrow;

    private boolean expandEffects;

    public EffectsGroup(Context context, AttributeSet attrs) {
        super(context, attrs);

        downArrow = context.getResources().getDrawable(R.drawable.arrow_down);
        rightArrow = context.getResources().getDrawable(R.drawable.arrow_right);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (expandEffects) {
            requestRectangleOnScreen(new Rect(0, 0, 0, getHeight()));
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final ImageView arrow = (ImageView) findViewById(R.id.group_arrow);
        final ViewGroup container = (ViewGroup) findViewById(R.id.grouped_effects);
        final List<View> effects = new ArrayList<View>();
        for (int i = 0; i < container.getChildCount(); i++) {
            effects.add(container.getChildAt(i));
        }

        findViewById(R.id.group_header).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (container.getVisibility() == VISIBLE) {
                    expandEffects = false;

                    int delay = 0;
                    for (int i = effects.size() - 1; i >= 0; i--) {
                        final View effect = effects.get(i);

                        postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                effect.setVisibility(GONE);
                            }
                        }, delay);
                        delay += ANIMATION_INTERVAL;
                    }

                    postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            container.setVisibility(GONE);
                            arrow.setImageDrawable(rightArrow);
                        }
                    }, delay - ANIMATION_INTERVAL);
                } else {
                    expandEffects = true;

                    arrow.setImageDrawable(downArrow);
                    container.setVisibility(VISIBLE);

                    int delay = 0;
                    for (int i = 0; i < effects.size(); i++) {
                        final View effect = effects.get(i);

                        postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                effect.setVisibility(VISIBLE);
                            }
                        }, delay);
                        delay += ANIMATION_INTERVAL;
                    }
                }
            }
        });
    }
}
