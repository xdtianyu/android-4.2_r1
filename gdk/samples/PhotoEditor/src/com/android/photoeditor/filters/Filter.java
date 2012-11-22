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

package com.android.photoeditor.filters;

import com.android.photoeditor.Photo;

/**
 * Image filter for photo editing.
 */
public abstract class Filter {

    private boolean isValid;

    protected void validate() {
        isValid = true;
    }

    /**
     * Some filters, e.g. lighting filters, are initially invalid until set up with parameters while
     * others, e.g. sepia or flip filters, are initially valid without parameters.
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Processes the source bitmap and matrix and output the destination bitmap and matrix.
     *
     * @param src source photo as the input.
     * @param dst destination photo having the same dimension as source photo as the output.
     */
    public abstract void process(Photo src, Photo dst);
}
