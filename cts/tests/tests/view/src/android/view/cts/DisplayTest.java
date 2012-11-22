/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.view.cts;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class DisplayTest extends AndroidTestCase {

    /**
     * Test the properties of Display, they are:
     * 1 index of this display
     * 2 height of this display in pixels
     * 3 width of this display in pixels
     * 4 orientation of this display
     * 5 pixel format of this display
     * 6 refresh rate of this display in frames per second
     * 7 Initialize a DisplayMetrics object from this display's data
     */
    public void testGetDisplayAttrs() {
        Context con = getContext();
        WindowManager windowManager = (WindowManager) con.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        assertEquals(Display.DEFAULT_DISPLAY, display.getDisplayId());
        assertTrue(0 < display.getHeight());
        assertTrue(0 < display.getWidth());
        display.getOrientation();
        assertTrue(0 < display.getPixelFormat());
        assertTrue(0 < display.getRefreshRate());

        DisplayMetrics outMetrics = new DisplayMetrics();
        outMetrics.setToDefaults();
        display.getMetrics(outMetrics);
        assertEquals(display.getHeight(), outMetrics.heightPixels);
        assertEquals(display.getWidth(), outMetrics.widthPixels);

        // The scale is in [0.1, 3], and density is the scale factor.
        assertTrue(0.1f <= outMetrics.density && outMetrics.density <= 3.0f);
        assertTrue(0.1f <= outMetrics.scaledDensity && outMetrics.density <= 3.0f);
        assertTrue(0 < outMetrics.xdpi);
        assertTrue(0 < outMetrics.ydpi);
    }
}
