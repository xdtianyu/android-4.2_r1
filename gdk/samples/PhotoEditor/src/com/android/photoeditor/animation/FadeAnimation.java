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

package com.android.photoeditor.animation;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Fading animation that either fades in or out the view by alpha animations.
 */
public class FadeAnimation {

    /**
     * Gets animations that fade out and then fade in the view.
     */
    public static AnimationPair getFadeOutInAnimations(int duration) {
        Animation fadeOut = new AlphaAnimation(1.0f, 0.5f);
        Animation fadeIn = new AlphaAnimation(0.5f, 1.0f);
        fadeOut.setDuration(duration);
        fadeIn.setDuration(duration);

        return new AnimationPair(fadeOut, fadeIn);
    }
}
