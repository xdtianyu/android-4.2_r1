
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
package com.android.chimpchat.core;

import com.android.chimpchat.ChimpManager;
import com.android.chimpchat.hierarchyviewer.HierarchyViewer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * ChimpDevice interface.
 */
public interface IChimpDevice {
    /**
     * Create a ChimpManager for talking to this device.
     *
     * @return the ChimpManager
     */
    ChimpManager getManager();

    /**
     * Dispose of any native resources this device may have taken hold of.
     */
    void dispose();

    /**
     * @return hierarchy viewer implementation for querying state of the view
     * hierarchy.
     */
    HierarchyViewer getHierarchyViewer();

    /**
     * Take the current screen's snapshot.
     * @return the snapshot image
     */
    IChimpImage takeSnapshot();

    /**
     * Reboot the device.
     *
     * @param into which bootloader to boot into.  Null means default reboot.
     */
    void reboot(@Nullable String into);

    /**
     * List properties of the device that we can inspect
     *
     * @return the list of property keys
     */
    Collection<String> getPropertyList();

    /**
     * Get device's property.
     *
     * @param key the property name
     * @return the property value
     */
    String getProperty(String key);

    /**
     * Get system property.
     *
     * @param key the name of the system property
     * @return  the property value
     */
    String getSystemProperty(String key);

    /**
     * Perform a touch of the given type at (x,y).
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param type the touch type
     */
    void touch(int x, int y, TouchPressType type);

    /**
     * Perform a press of a given type using a given key.
     *
     * @param keyName the name of the key to use
     * @param type the type of press to perform
     */
    void press(String keyName, TouchPressType type);


    /**
     * Perform a press of a given type using a given key.
     *
     * @param key the key to press
     * @param type the type of press to perform
     */
    void press(PhysicalButton key, TouchPressType type);

    /**
     * Perform a drag from one one location to another
     *
     * @param startx the x coordinate of the drag's starting point
     * @param starty the y coordinate of the drag's starting point
     * @param endx the x coordinate of the drag's end point
     * @param endy the y coordinate of the drag's end point
     * @param steps the number of steps to take when interpolating points
     * @param ms the duration of the drag
     */
    void drag(int startx, int starty, int endx, int endy, int steps, long ms);

    /**
     * Type a given string.
     *
     * @param string the string to type
     */
    void type(String string);

    /**
     * Execute a shell command.
     *
     * @param cmd the command to execute
     * @return the output of the command
     */
    String shell(String cmd);

    /**
     * Install a given package.
     *
     * @param path the path to the installation package
     * @return true if success
     */
    boolean installPackage(String path);

    /**
     * Uninstall a given package.
     *
     * @param packageName the name of the package
     * @return true if success
     */
    boolean removePackage(String packageName);

    /**
     * Start an activity.
     *
     * @param uri the URI for the Intent
     * @param action the action for the Intent
     * @param data the data URI for the Intent
     * @param mimeType the mime type for the Intent
     * @param categories the category names for the Intent
     * @param extras the extras to add to the Intent
     * @param component the component of the Intent
     * @param flags the flags for the Intent
     */
    void startActivity(@Nullable String uri, @Nullable String action,
            @Nullable String data, @Nullable String mimeType,
            Collection<String> categories, Map<String, Object> extras, @Nullable String component,
            int flags);

    /**
     * Send a broadcast intent to the device.
     *
     * @param uri the URI for the Intent
     * @param action the action for the Intent
     * @param data the data URI for the Intent
     * @param mimeType the mime type for the Intent
     * @param categories the category names for the Intent
     * @param extras the extras to add to the Intent
     * @param component the component of the Intent
     * @param flags the flags for the Intent
     */
    void broadcastIntent(@Nullable String uri, @Nullable String action,
            @Nullable String data, @Nullable String mimeType,
            Collection<String> categories, Map<String, Object> extras, @Nullable String component,
            int flags);

    /**
     * Run the specified package with instrumentation and return the output it
     * generates.
     *
     * Use this to run a test package using InstrumentationTestRunner.
     *
     * @param packageName The class to run with instrumentation. The format is
     * packageName/className. Use packageName to specify the Android package to
     * run, and className to specify the class to run within that package. For
     * test packages, this is usually testPackageName/InstrumentationTestRunner
     * @param args a map of strings to objects containing the arguments to pass
     * to this instrumentation.
     * @return A map of strings to objects for the output from the package.
     * For a test package, contains a single key-value pair: the key is 'stream'
     * and the value is a string containing the test output.
     */
    Map<String, Object> instrument(String packageName,
            Map<String, Object> args);

    /**
     * Wake up the screen on the device.
     */
    void wake();

    /**
     * List the possible view ID strings from the current applications resource file
     * @return the list of view id strings
     */
    Collection<String> getViewIdList();

    /**
     * Retrieve the view object for the view with the given id.
     * @return a view object for the view with the given id
     */
    IChimpView getView(ISelector selector);

    /**
     * Retrive the root view object.
     * @return the root view object.
     */
    IChimpView getRootView();

    /**
     * Retrieves the view objects that match the given selector
     * @return A list of views that match the given selector
     */
    Collection<IChimpView> getViews(IMultiSelector selector);
}
