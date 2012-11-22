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

import com.google.common.collect.Lists;

/* A class for selecting objects by their accessibility ids */
public class SelectorAccessibilityIds implements ISelector {
    private int windowId;
    private int accessibilityId;

    /**
     * @param windowId the window id of the node you want to select
     * @param accessibilityId the accessibility id of the node you want to select
     */
    public SelectorAccessibilityIds(int windowId, int accessibilityId) {
        this.windowId = windowId;
        this.accessibilityId = accessibilityId;
    }

    /**
     * A method for selecting a view by the given accessibility ids.
     * @param manager The manager object used for interacting with the device.
     * @return The view with the given accessibility ids.
     */
    public IChimpView getView(ChimpManager manager) {
        ChimpView view = new ChimpView(ChimpView.ACCESSIBILITY_IDS,
                Lists.newArrayList(Integer.toString(windowId), Integer.toString(accessibilityId)));
        view.setManager(manager);
        return view;
    }
}
