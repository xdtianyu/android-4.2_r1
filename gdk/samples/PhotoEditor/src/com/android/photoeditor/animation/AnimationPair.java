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

import android.view.animation.Animation;

/**
 * A pair of animations that will be played sequentially, e.g. fade-out and then fade-in.
 */
public class AnimationPair {

    private final Animation first;
    private final Animation second;

    AnimationPair(Animation first, Animation second) {
        this.first = first;
        this.second = second;
    }

    public Animation first() {
        return first;
    }

    public Animation second() {
        return second;
    }
}
