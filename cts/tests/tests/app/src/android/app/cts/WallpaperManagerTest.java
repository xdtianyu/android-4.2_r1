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

package android.app.cts;

import android.app.WallpaperManager;
import android.test.AndroidTestCase;

public class WallpaperManagerTest extends AndroidTestCase {

    private WallpaperManager mWallpaperManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWallpaperManager = WallpaperManager.getInstance(mContext);
    }

    public void testSuggestDesiredDimensions() {
        mWallpaperManager.suggestDesiredDimensions(320, 480);
        int desiredMinimumWidth = mWallpaperManager.getDesiredMinimumWidth();
        int desiredMinimumHeight = mWallpaperManager.getDesiredMinimumHeight();
        assertEquals(320, desiredMinimumWidth);
        assertEquals(480, desiredMinimumHeight);
    }
}
