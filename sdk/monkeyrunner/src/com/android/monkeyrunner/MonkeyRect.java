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
package com.android.monkeyrunner;

import com.android.chimpchat.core.ChimpRect;

import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;

import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;

/*
 * A Jython wrap for the ChimpRect class that stores coordinate information for views
 */
@MonkeyRunnerExported(doc = "Represents the coordinates of a rectangular object")
public class MonkeyRect extends PyObject implements ClassDictInit {
    private static final Logger LOG = Logger.getLogger(MonkeyRect.class.getName());

    private ChimpRect rect;

    @MonkeyRunnerExported(doc = "The x coordinate of the left side of the rectangle")
    public int left;
    @MonkeyRunnerExported(doc = "The y coordinate of the top side of the rectangle")
    public int top;
    @MonkeyRunnerExported(doc = "The x coordinate of the right side of the rectangle")
    public int right;
    @MonkeyRunnerExported(doc = "The y coordinate of the bottom side of the rectangle")
    public int bottom;

    public static void classDictInit(PyObject dict) {
        JythonUtils.convertDocAnnotationsForClass(MonkeyRect.class, dict);
    }

    public MonkeyRect(ChimpRect rect) {
        this.rect = rect;
        this.left = rect.left;
        this.right = rect.right;
        this.top = rect.top;
        this.bottom = rect.bottom;
    }

    @MonkeyRunnerExported(doc = "Returns the width of the rectangle",
                          returns = "The width of the rectangle as an integer")
    public PyInteger getWidth() {
        return new PyInteger(right-left);
    }

    @MonkeyRunnerExported(doc = "Returns the height of the rectangle",
                          returns = "The height of the rectangle as an integer")
    public PyInteger getHeight() {
        return new PyInteger(bottom-top);
    }

    @MonkeyRunnerExported(doc = "Returns a two item list that contains the x and y value of " +
                          "the center of the rectangle",
                          returns = "The center coordinates as a two item list of integers")
    public PyList getCenter(){
        List<PyInteger> center = new LinkedList<PyInteger>();
        /* Center x coordinate */
        center.add(new PyInteger(left+(right-left)/2));
        /* Center y coordinate */
        center.add(new PyInteger(top+(bottom-top)/2));
        return new PyList(center);
    }
}
