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

package com.android.chimpchat.hierarchyviewer;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.hierarchyviewerlib.device.DeviceBridge;
import com.android.hierarchyviewerlib.device.ViewNode;
import com.android.hierarchyviewerlib.device.Window;

import org.eclipse.swt.graphics.Point;

/**
 * Class for querying the view hierarchy of the device.
 */
public class HierarchyViewer {
    public static final String TAG = "hierarchyviewer";

    private IDevice mDevice;

    /**
     * Constructs the hierarchy viewer for the specified device.
     *
     * @param device The Android device to connect to.
     */
    public HierarchyViewer(IDevice device) {
        this.mDevice = device;
        setupViewServer();
    }

    private void setupViewServer() {
        DeviceBridge.setupDeviceForward(mDevice);
        if (!DeviceBridge.isViewServerRunning(mDevice)) {
            if (!DeviceBridge.startViewServer(mDevice)) {
                // TODO: Get rid of this delay.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                if (!DeviceBridge.startViewServer(mDevice)) {
                    Log.e(TAG, "Unable to debug device " + mDevice);
                    throw new RuntimeException("Could not connect to the view server");
                }
                return;
            }
        }
        DeviceBridge.loadViewServerInfo(mDevice);
    }

    /**
     * Find a view by id.
     *
     * @param id id for the view.
     * @return view with the specified ID, or {@code null} if no view found.
     */

    public ViewNode findViewById(String id) {
        ViewNode rootNode = DeviceBridge.loadWindowData(
                new Window(mDevice, "", 0xffffffff));
        if (rootNode == null) {
            throw new RuntimeException("Could not dump view");
        }
        return findViewById(id, rootNode);
    }

    /**
     * Find a view by ID, starting from the given root node
     * @param id ID of the view you're looking for
     * @param rootNode the ViewNode at which to begin the traversal
     * @return view with the specified ID, or {@code null} if no view found.
     */

    public ViewNode findViewById(String id, ViewNode rootNode) {
        if (rootNode.id.equals(id)) {
            return rootNode;
        }

        for (ViewNode child : rootNode.children) {
            ViewNode found = findViewById(id,child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Gets the window that currently receives the focus.
     *
     * @return name of the window that currently receives the focus.
     */
    public String getFocusedWindowName() {
        int id = DeviceBridge.getFocusedWindow(mDevice);
        Window[] windows = DeviceBridge.loadWindows(mDevice);
        for (Window w : windows) {
            if (w.getHashCode() == id)
                return w.getTitle();
        }
        return null;
    }

    /**
     * Gets the absolute x/y position of the view node.
     *
     * @param node view node to find position of.
     * @return point specifying the x/y position of the node.
     */
    public static Point getAbsolutePositionOfView(ViewNode node) {
        int x = node.left;
        int y = node.top;
        ViewNode p = node.parent;
        while (p != null) {
            x += p.left - p.scrollX;
            y += p.top - p.scrollY;
            p = p.parent;
        }
        return new Point(x, y);
    }

    /**
     * Gets the absolute x/y center of the specified view node.
     *
     * @param node view node to find position of.
     * @return absolute x/y center of the specified view node.
     */
    public static Point getAbsoluteCenterOfView(ViewNode node) {
        Point point = getAbsolutePositionOfView(node);
        return new Point(
                point.x + (node.width / 2), point.y + (node.height / 2));
    }

    /**
     * Gets the visibility of a given element.
     *
     * @param selector selector for the view.
     * @return True if the element is visible.
     */
    public boolean visible(ViewNode node) {
        boolean ret = (node != null)
                && node.namedProperties.containsKey("getVisibility()")
                && "VISIBLE".equalsIgnoreCase(
                        node.namedProperties.get("getVisibility()").value);
        return ret;

    }

    /**
     * Gets the text of a given element.
     *
     * @param selector selector for the view.
     * @return the text of the given element.
     */
    public String getText(ViewNode node) {
        if (node == null) {
            throw new RuntimeException("Node not found");
        }
        ViewNode.Property textProperty = node.namedProperties.get("text:mText");
        if (textProperty == null) {
            // give it another chance, ICS ViewServer returns mText
            textProperty = node.namedProperties.get("mText");
            if (textProperty == null) {
                throw new RuntimeException("No text property on node");
            }
        }
        return textProperty.value;
    }
}
