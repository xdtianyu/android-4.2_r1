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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class PxUsageDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new PxUsageDetector();
    }

    public void testPx() throws Exception {
        assertEquals(
            "res/layout/now_playing_after.xml:41: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n" +
            "        android:layout_width=\"1px\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",
            lintFiles("res/layout/now_playing_after.xml"));
    }

    public void testSp() throws Exception {
        assertEquals(
            "res/layout/textsize.xml:11: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n" +
            "        android:textSize=\"14dp\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/layout/textsize.xml:16: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n" +
            "        android:textSize=\"14dip\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 2 warnings\n" +
            "",
            lintFiles("res/layout/textsize.xml"));
    }

    public void testStyles() throws Exception {
        assertEquals(
            "res/values/pxsp.xml:9: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n" +
            "        <item name=\"android:textSize\">50px</item>\n" +
            "                                      ^\n" +
            "res/values/pxsp.xml:17: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n" +
            "        <item name=\"android:paddingRight\"> 50px </item>\n" +
            "                                           ^\n" +
            "res/values/pxsp.xml:18: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n" +
            "        <item name=\"android:paddingTop\">50px</item>\n" +
            "                                        ^\n" +
            "res/values/pxsp.xml:6: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n" +
            "        <item name=\"android:textSize\">50dp</item>\n" +
            "                                      ^\n" +
            "res/values/pxsp.xml:12: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n" +
            "        <item name=\"android:textSize\"> 50dip </item>\n" +
            "                                       ^\n" +
            "0 errors, 5 warnings\n" +
            "",

            lintFiles("res/values/pxsp.xml"));
    }
}
