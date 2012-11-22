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
public class NamespaceDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new NamespaceDetector();
    }

    public void testCustom() throws Exception {
        assertEquals(
            "res/layout/customview.xml:16: Error: When using a custom namespace attribute in a library project, use the namespace \"http://schemas.android.com/apk/res-auto\" instead. [LibraryCustomView]\n" +
            "        foo:misc=\"Custom attribute\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "1 errors, 0 warnings\n" +
            "",

            lintProject(
                    "multiproject/library-manifest.xml=>AndroidManifest.xml",
                    "multiproject/library.properties=>project.properties",
                    "res/layout/customview.xml"
            ));
    }

    public void testCustomOk() throws Exception {
        assertEquals(
            "No warnings.",

            lintProject(
                    "multiproject/library-manifest.xml=>AndroidManifest.xml",

                    // Use a standard project properties instead: no warning since it's
                    // not a library project:
                    //"multiproject/library.properties=>project.properties",

                    "res/layout/customview.xml"
            ));
    }

    public void testCustomOk2() throws Exception {
        assertEquals(
            "No warnings.",

            lintProject(
                    "multiproject/library-manifest.xml=>AndroidManifest.xml",
                    "multiproject/library.properties=>project.properties",
                    // This project already uses the res-auto package
                    "res/layout/customview2.xml"
            ));
    }

    public void testTypo() throws Exception {
        assertEquals(
            "res/layout/wrong_namespace.xml:2: Warning: Unexpected namespace URI bound to the \"android\" prefix, was http://schemas.android.com/apk/res/andriod, expected http://schemas.android.com/apk/res/android [NamespaceTypo]\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/andriod\"\n" +
            "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",

            lintProject("res/layout/wrong_namespace.xml"));
    }

    public void testTypo2() throws Exception {
        assertEquals(
            "res/layout/wrong_namespace2.xml:2: Warning: URI is case sensitive: was \"http://schemas.android.com/apk/res/Android\", expected \"http://schemas.android.com/apk/res/android\" [NamespaceTypo]\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/Android\"\n" +
            "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",

            lintProject("res/layout/wrong_namespace2.xml"));
    }

    public void testTypo3() throws Exception {
        assertEquals(
            "res/layout/wrong_namespace3.xml:2: Warning: Unexpected namespace URI bound to the \"android\" prefix, was http://schemas.android.com/apk/res/androi, expected http://schemas.android.com/apk/res/android [NamespaceTypo]\n" +
            "<LinearLayout xmlns:a=\"http://schemas.android.com/apk/res/androi\"\n" +
            "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",

            lintProject("res/layout/wrong_namespace3.xml"));
    }

    public void testTypoOk() throws Exception {
        assertEquals(
                "No warnings.",

                lintProject("res/layout/wrong_namespace4.xml"));
    }

    public void testUnused() throws Exception {
        assertEquals(
            "res/layout/unused_namespace.xml:3: Warning: Unused namespace unused1 [UnusedNamespace]\n" +
            "    xmlns:unused1=\"http://schemas.android.com/apk/res/unused1\"\n" +
            "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/layout/unused_namespace.xml:4: Warning: Unused namespace unused2 [UnusedNamespace]\n" +
            "    xmlns:unused2=\"http://schemas.android.com/apk/res/unused1\"\n" +
            "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 2 warnings\n" +
            "",

            lintProject("res/layout/unused_namespace.xml"));
    }

    public void testUnusedOk() throws Exception {
        assertEquals(
                "No warnings.",

                lintProject("res/layout/layout1.xml"));
    }
}
