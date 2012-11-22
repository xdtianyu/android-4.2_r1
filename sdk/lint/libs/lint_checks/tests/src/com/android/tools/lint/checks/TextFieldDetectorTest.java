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
public class TextFieldDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new TextFieldDetector();
    }

    public void testField() throws Exception {
        assertEquals(
            "res/layout/note_edit.xml:50: Warning: This text field does not specify an inputType or a hint [TextFields]\n" +
            "        <EditText\n" +
            "        ^\n" +
            "0 errors, 1 warnings\n" +
            "",
            lintFiles("res/layout/note_edit.xml"));
    }
}
