/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.chimpchat.core;

/**
 * A class for holding information about view locations
 */
public class ChimpRect {
    public int left;
    public int top;
    public int right;
    public int bottom;

    /**
     * Creates an empty ChimpRect object. All coordinates are initialized to 0.
     */
    public ChimpRect() {}

    /**
     * Create a new ChimpRect with the given coordinates.
     * @param left   The X coordinate of the left side of the rectangle
     * @param top    The Y coordinate of the top of the rectangle
     * @param right  The X coordinate of the right side of the rectangle
     * @param bottom The Y coordinate of the bottom of the rectangle
     */
    public ChimpRect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * A comparison method to determine if the object is equivalent to other ChimpRects.
     * @param obj The object to compare it to
     * @return True if the object is an equivalent rectangle, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ChimpRect){
            ChimpRect r = (ChimpRect) obj;
            if (r != null) {
                return left == r.left && top == r.top && right == r.right
                        && bottom == r.bottom;
            }
        }
        return false;
    }

    /**
     * The width of the ChimpRect
     * @return the width of the rectangle
     */
    public int getWidth() {
        return right-left;
    }

    /**
     * The height of the ChimpRect
     * @return the height of the rectangle
     */
    public int getHeight() {
        return bottom-top;
    }

    /**
     * Returns a 2 item int array with the x, y coordinates of the center of the ChimpRect.
     * @return a 2 item int array. The first item is the x value of the center of the ChimpRect and
     * the second item is the y value.
     */
    public int[] getCenter() {
        int[] center = new int[2];
        center[0] = left + getWidth() / 2;
        center[1] = top + getHeight() / 2;
        return center;
    }

    /**
     * Returns a representation of the rectangle in string form
     * @return a string representation of the rectangle
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChimpRect ");
        sb.append("top: ").append(top).append(" ");
        sb.append("right: ").append(right).append(" ");
        sb.append("bottom: ").append(bottom).append(" ");
        sb.append("left: ").append(left).append(" ");
        return sb.toString();
    }
}
