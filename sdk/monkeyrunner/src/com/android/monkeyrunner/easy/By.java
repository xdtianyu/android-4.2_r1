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

package com.android.monkeyrunner.easy;

import com.google.common.base.Preconditions;

import com.android.chimpchat.hierarchyviewer.HierarchyViewer;
import com.android.hierarchyviewerlib.device.ViewNode;
import com.android.monkeyrunner.JythonUtils;
import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.PyObject;

/**
 * Select a view object based on some criteria.
 *
 * Currently supports the By.id criteria to search for an element by id.
 * In the future it will support other criteria such as:
 *   By.classid - search by class.
 *   By.hash - search by hashcode
 * and recursively searching under an already selected object.
 *
 * WARNING: This API is under development, expect the interface to change
 * without notice.
 *
 * TODO: Implement other selectors, like classid, hash, and so on.
 * TODO: separate java-only core from jython wrapper
 */
public class By extends PyObject implements ClassDictInit {
    public static void classDictInit(PyObject dict) {
        JythonUtils.convertDocAnnotationsForClass(By.class, dict);
    }

    private String id;

    By(String id) {
        this.id = id;
    }

    @MonkeyRunnerExported(doc = "Select an object by id.",
            args = { "id" },
            argDocs = { "The identifier of the object." })
    public static By id(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String id = ap.getString(0);
        return new By(id);
    }

    public static By id(String id) {
        return new By(id);
    }

    public ViewNode findView(HierarchyViewer viewer) {
        return viewer.findViewById(id);
    }


}
