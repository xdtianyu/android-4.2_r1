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

import com.android.chimpchat.core.TouchPressType;
import com.android.chimpchat.hierarchyviewer.HierarchyViewer;
import com.android.hierarchyviewerlib.device.ViewNode;
import com.android.monkeyrunner.JythonUtils;
import com.android.monkeyrunner.MonkeyDevice;
import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.eclipse.swt.graphics.Point;
import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyTuple;

import java.util.Set;

/**
 * Extends {@link MonkeyDevice} to support looking up views using a 'selector'.
 * Currently, only identifiers can be used as a selector. All methods on
 * MonkeyDevice can be used on this class in Python.
 *
 * WARNING: This API is under development, expect the interface to change
 * without notice.
 */
@MonkeyRunnerExported(doc = "MonkeyDevice with easier methods to refer to objects.")
public class EasyMonkeyDevice extends PyObject implements ClassDictInit {
    public static void classDictInit(PyObject dict) {
        JythonUtils.convertDocAnnotationsForClass(EasyMonkeyDevice.class, dict);
    }

    private MonkeyDevice mDevice;
    private HierarchyViewer mHierarchyViewer;

    private static final Set<String> EXPORTED_METHODS =
        JythonUtils.getMethodNames(EasyMonkeyDevice.class);

    @MonkeyRunnerExported(doc = "Creates EasyMonkeyDevice with an underlying MonkeyDevice.",
            args = { "device" },
            argDocs = { "MonkeyDevice to extend." })
    public EasyMonkeyDevice(MonkeyDevice device) {
        this.mDevice = device;
        this.mHierarchyViewer = device.getImpl().getHierarchyViewer();
    }

    @MonkeyRunnerExported(doc = "Sends a touch event to the selected object.",
            args = { "selector", "type" },
            argDocs = {
                    "The selector identifying the object.",
                    "The event type as returned by TouchPressType()." })
    public void touch(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        By selector = getSelector(ap, 0);
        String tmpType = ap.getString(1);
        TouchPressType type = TouchPressType.fromIdentifier(tmpType);
        Preconditions.checkNotNull(type, "Invalid touch type: " + tmpType);
        // TODO: try catch rethrow PyExc
        touch(selector, type);
    }

    public void touch(By selector, TouchPressType type) {
        Point p = getElementCenter(selector);
        mDevice.getImpl().touch(p.x, p.y, type);
    }

    @MonkeyRunnerExported(doc = "Types a string into the specified object.",
            args = { "selector", "text" },
            argDocs = {
                    "The selector identifying the object.",
                    "The text to type into the object." })
    public void type(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        By selector = getSelector(ap, 0);
        String text = ap.getString(1);
        type(selector, text);
    }

    public void type(By selector, String text) {
        Point p = getElementCenter(selector);
        mDevice.getImpl().touch(p.x, p.y, TouchPressType.DOWN_AND_UP);
        mDevice.getImpl().type(text);
    }

    @MonkeyRunnerExported(doc = "Locates the coordinates of the selected object.",
            args = { "selector" },
            argDocs = { "The selector identifying the object." },
            returns = "Tuple containing (x,y,w,h) location and size.")
    public PyTuple locate(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        By selector = getSelector(ap, 0);

        ViewNode node = selector.findView(mHierarchyViewer);
        Point p = HierarchyViewer.getAbsolutePositionOfView(node);
        PyTuple tuple = new PyTuple(
                new PyInteger(p.x),
                new PyInteger(p.y),
                new PyInteger(node.width),
                new PyInteger(node.height));
        return tuple;
    }

    @MonkeyRunnerExported(doc = "Checks if the specified object exists.",
            args = { "selector" },
            returns = "True if the object exists.",
            argDocs = { "The selector identifying the object." })
    public boolean exists(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        By selector = getSelector(ap, 0);
        return exists(selector);
    }

    public boolean exists(By selector) {
        ViewNode node = selector.findView(mHierarchyViewer);
        return node != null;
    }

    @MonkeyRunnerExported(doc = "Checks if the specified object is visible.",
            args = { "selector" },
            returns = "True if the object is visible.",
            argDocs = { "The selector identifying the object." })
    public boolean visible(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        By selector = getSelector(ap, 0);
        return visible(selector);
    }

    public boolean visible(By selector) {
        ViewNode node = selector.findView(mHierarchyViewer);
        return mHierarchyViewer.visible(node);
    }

    @MonkeyRunnerExported(doc = "Obtain the text in the selected input box.",
            args = { "selector" },
            argDocs = { "The selector identifying the object." },
            returns = "Text in the selected input box.")
    public String getText(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        By selector = getSelector(ap, 0);
        return getText(selector);
    }

    public String getText(By selector) {
        ViewNode node = selector.findView(mHierarchyViewer);
        return mHierarchyViewer.getText(node);
    }

    @MonkeyRunnerExported(doc = "Gets the id of the focused window.",
            returns = "The symbolic id of the focused window or None.")
    public String getFocusedWindowId(PyObject[] args, String[] kws) {
        return getFocusedWindowId();
    }

    public String getFocusedWindowId() {
        return mHierarchyViewer.getFocusedWindowName();
    }

    /**
     * Forwards unknown methods to the original MonkeyDevice object.
     */
    @Override
    public PyObject __findattr_ex__(String name) {
        if (!EXPORTED_METHODS.contains(name)) {
            return mDevice.__findattr_ex__(name);
        }
        return super.__findattr_ex__(name);
    }

    /**
     * Get the selector object from the argument parser.
     *
     * @param ap argument parser to get it from.
     * @param i argument index.
     * @return selector object.
     */
    private By getSelector(ArgParser ap, int i) {
        return (By)ap.getPyObject(i).__tojava__(By.class);
    }

    /**
     * Get the coordinates of the element's center.
     *
     * @param selector the element selector
     * @return the (x,y) coordinates of the center
     */
    private Point getElementCenter(By selector) {
        ViewNode node = selector.findView(mHierarchyViewer);
        if (node == null) {
            throw new PyException(Py.ValueError,
                    String.format("View not found: %s", selector));
        }

        Point p = HierarchyViewer.getAbsoluteCenterOfView(node);
        return p;
    }

}
