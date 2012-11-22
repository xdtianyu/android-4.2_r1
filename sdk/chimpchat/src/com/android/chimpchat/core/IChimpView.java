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

import java.util.List;

/**
 * An interface for view introspection.
 */
public interface IChimpView {

    /**
     * Set the manager for this view to communicate through.
     */
    void setManager(ChimpManager manager);

    /**
     * Obtain the class of the view as a string
     */
    String getViewClass();

    /**
     * Obtain the text contained in the view
     */
    String getText();

    /**
     * Obtain the location of the view on the device screen
     */
    ChimpRect getLocation();

    /**
     * Obtain the checked status of this view.
     */
    boolean getChecked();

    /**
     * Obtain the enabled status of this view.
     */
    boolean getEnabled();

    /**
     * Obtain the selected status of this view.
     */
    boolean getSelected();

    /**
     * Set the selected status of the this  view
     */
    void setSelected(boolean selected);

    /**
     * Obtain the focused status of this view.
     */
    boolean getFocused();

    /**
     * Set the focused status of this view.
     */
    void setFocused(boolean focused);

    /**
     * Retrieve the parent of this view if it has one.
     */
    IChimpView getParent();

    /**
     * Get the children of this view as a list of IChimpViews.
     */
    List<IChimpView> getChildren();

    /**
     * Get the accessibility ids of this view.
     */
    int[] getAccessibilityIds();
}
