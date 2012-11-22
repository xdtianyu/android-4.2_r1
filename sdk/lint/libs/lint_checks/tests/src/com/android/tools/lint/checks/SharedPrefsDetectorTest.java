/*
 * Copyright (C) 2012 The Android Open Source Project
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
public class SharedPrefsDetectorTest  extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new SharedPrefsDetector();
    }

    public void test() throws Exception {
        assertEquals(
            "src/test/pkg/SharedPrefsTest.java:54: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n" +
            "        SharedPreferences.Editor editor = preferences.edit();\n" +
            "                                          ~~~~~~~~~~~~~~~~~~\n" +
            "src/test/pkg/SharedPrefsTest.java:62: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n" +
            "        SharedPreferences.Editor editor = preferences.edit();\n" +
            "                                          ~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 2 warnings\n" +
            "",

            lintProject("src/test/pkg/SharedPrefsTest.java.txt=>" +
                    "src/test/pkg/SharedPrefsTest.java"));
    }

    public void test2() throws Exception {
        // Regression test 1 for http://code.google.com/p/android/issues/detail?id=34322
        assertEquals(
            "src/test/pkg/SharedPrefsTest2.java:13: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n" +
            "        SharedPreferences.Editor editor = preferences.edit();\n" +
            "                                          ~~~~~~~~~~~~~~~~~~\n" +
            "src/test/pkg/SharedPrefsTest2.java:17: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n" +
            "        Editor editor = preferences.edit();\n" +
            "                        ~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 2 warnings\n",

            lintProject("src/test/pkg/SharedPrefsTest2.java.txt=>" +
                    "src/test/pkg/SharedPrefsTest2.java"));
    }

    public void test3() throws Exception {
        // Regression test 2 for http://code.google.com/p/android/issues/detail?id=34322
        assertEquals(
            "src/test/pkg/SharedPrefsTest3.java:13: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n" +
            "        Editor editor = preferences.edit();\n" +
            "                        ~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n",

            lintProject("src/test/pkg/SharedPrefsTest3.java.txt=>" +
                    "src/test/pkg/SharedPrefsTest3.java"));
    }

    public void test4() throws Exception {
        // Regression test 3 for http://code.google.com/p/android/issues/detail?id=34322
        assertEquals(
            "No warnings.",

            lintProject("src/test/pkg/SharedPrefsTest4.java.txt=>" +
                    "src/test/pkg/SharedPrefsTest4.java"));
    }
}
