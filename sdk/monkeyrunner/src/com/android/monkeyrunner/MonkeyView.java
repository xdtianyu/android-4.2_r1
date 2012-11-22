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

import com.google.common.base.Preconditions;

import com.android.chimpchat.core.IChimpView;

import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.PyBoolean;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

import java.util.List;
import java.util.logging.Logger;

/*
 * Jython wrapper for the ChimpView class
 */
@MonkeyRunnerExported(doc = "Represents a view object.")
public class MonkeyView extends PyObject implements ClassDictInit {
    private static final Logger LOG = Logger.getLogger(MonkeyView.class.getName());

    private IChimpView impl;

    public static void classDictInit(PyObject dict) {
        JythonUtils.convertDocAnnotationsForClass(MonkeyView.class, dict);
    }

    public MonkeyView(IChimpView impl) {
        this.impl = impl;
    }

    @MonkeyRunnerExported(doc = "Get the checked status of the view",
                          returns = "A boolean value for whether the item is checked or not")
    public PyBoolean getChecked(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new PyBoolean(impl.getChecked());
    }

    @MonkeyRunnerExported(doc = "Returns the class name of the view",
                          returns = "The class name of the view as a string")
    public PyString getViewClass(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new PyString(impl.getViewClass());
    }

    @MonkeyRunnerExported(doc = "Returns the text contained by the view",
                          returns = "The text contained in the view")
    public PyString getText(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new PyString(impl.getText());
    }

    @MonkeyRunnerExported(doc = "Returns the location of the view in the form of a MonkeyRect",
                          returns = "The location of the view as a MonkeyRect object")
    public MonkeyRect getLocation(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new MonkeyRect(impl.getLocation());
    }

    @MonkeyRunnerExported(doc = "Returns the enabled status of the view",
                          returns = "The enabled status of the view as a boolean")
    public PyBoolean getEnabled(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new PyBoolean(impl.getEnabled());
    }

    @MonkeyRunnerExported(doc = "Returns the selected status of the view",
                          returns = "The selected status of the view as a boolean")
    public PyBoolean getSelected(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new PyBoolean(impl.getSelected());
    }

    @MonkeyRunnerExported(doc = "Sets the selected status of the view",
                          args = {"selected"},
                          argDocs = { "The boolean value to set selected to" })
    public void setSelected(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        PyBoolean pySelected = (PyBoolean) ap.getPyObject(0, new PyBoolean(false));
        boolean selected = (Boolean) pySelected.__tojava__(Boolean.class);
        impl.setSelected(selected);
    }

    @MonkeyRunnerExported(doc = "Returns the focused status of the view",
                          returns = "The focused status of the view as a boolean")
    public PyBoolean getFocused(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return new PyBoolean(impl.getFocused());
    }

    @MonkeyRunnerExported(doc = "Sets the focused status of the view",
                          args = {"focused"},
                          argDocs = { "The boolean value to set focused to" })
    public void setFocused(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        PyBoolean pyFocused = (PyBoolean) ap.getPyObject(0, new PyBoolean(false));
        boolean focused = (Boolean) pyFocused.__tojava__(Boolean.class);
        impl.setFocused(focused);
    }

    @MonkeyRunnerExported(doc = "Returns the parent of the current view",
                          returns = "The parent of the view as a MonkeyView object")
    public MonkeyView getParent(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        MonkeyView parent = new MonkeyView(impl.getParent());
        return parent;
    }

    @MonkeyRunnerExported(doc = "Returns the children of the current view",
                          returns = "The children of the view as a list of MonkeyView objects")
    public PyList getChildren(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        List<IChimpView> chimpChildren = impl.getChildren();
        PyList children = new PyList();
        for (IChimpView child : chimpChildren) {
            children.append(new MonkeyView(child));
        }
        return children;
    }

    @MonkeyRunnerExported(doc = "Returns the accessibility ids of the current view",
                          returns = "The accessibility ids of the view as a list of ints")
    public PyList getAccessibilityIds(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        int[] ids = impl.getAccessibilityIds();
        PyList pyIds = new PyList();
        for (int i = 0; i < ids.length; i++) {
            pyIds.append(new PyInteger(ids[i]));
        }
        return pyIds;
    }

}
