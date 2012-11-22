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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/* A class for querying a view object by its id */
public class ChimpView implements IChimpView {
    private static final Logger LOG = Logger.getLogger(ChimpView.class.getName());
    public static final String ACCESSIBILITY_IDS = "accessibilityids";
    public static final String VIEW_ID = "viewid";

    private String viewType;
    private List<String> ids;
    private ChimpManager manager;

    public ChimpView(String viewType, List<String> ids) {
        this.viewType = viewType;
        this.ids = ids;
    }

    public void setManager(ChimpManager manager) {
        this.manager = manager;
    }

    private String queryView(String query) {
        try {
            return manager.queryView(viewType, ids, query);
        } catch(IOException e) {
            LOG.log(Level.SEVERE, "Error querying view: " + e.getMessage());
            return "";
        }
    }

    /**
     * Get the coordinates for the view with the given id.
     * @return a ChimpRect object with the coordinates for the corners of the view
     */
    public ChimpRect getLocation() {
        List<String> result = Lists.newArrayList(queryView("getlocation").split(" "));
        if (result.size() == 4) {
            try {
                int left = Integer.parseInt(result.get(0));
                int top = Integer.parseInt(result.get(1));
                int width = Integer.parseInt(result.get(2));
                int height = Integer.parseInt(result.get(3));
                return new ChimpRect(left, top, left+width, top+height);
            } catch (NumberFormatException e) {
                return new ChimpRect();
            }
        }
        return new ChimpRect();
    }

    /**
     * Retrieve the text contained by the view
     * @return the text contained by the view
     */
    public String getText() {
        return queryView("gettext");
    }

    /**
     * Get the class of the view
     * @return the class name of the view
     */
    public String getViewClass(){
        return queryView("getclass");
    }

    /**
     * Get the checked status of the view.
     * @return true if the view is checked, false otherwise
     */
    public boolean getChecked(){
      return Boolean.valueOf(queryView("getchecked").trim());
    }

    /**
     * Get whether the view is enabled or not.
     * @return true if the view is enabled, false otherwise
     */
    public boolean getEnabled(){
      return Boolean.valueOf(queryView("getenabled").trim());
    }

    /**
     * Get the selected status of the view.
     * @return true if the view is selected, false otherwise
     */
    public boolean getSelected(){
      return Boolean.valueOf(queryView("getselected").trim());
    }

    /**
     * Set the selected status of the view.
     * @param selected the select status to set for the view
     */
    public void setSelected(boolean selected) {
      queryView("setselected " + selected);
    }

    /**
     * Get the focused status of the view.
     * @return true if the view is focused, false otherwise
     */
    public boolean getFocused(){
      return Boolean.valueOf(queryView("getselected").trim());
    }

    /**
     * Set the focused status of the view.
     * @param focused the focus status to set for the view
     */
    public void setFocused(boolean focused) {
      queryView("setfocused " + focused);
    }

    /**
     * Get the parent of the view.
     * @return the parent of the view
     */
    public IChimpView getParent() {
        List<String> results = Lists.newArrayList(queryView("getparent").split(" "));
        if (results.size() == 2) {
            ChimpView parent = new ChimpView(ChimpView.ACCESSIBILITY_IDS, results);
            parent.setManager(manager);
            return parent;
        }
        return null;
    }

    /**
     * Gets the children of the view.
     * @return the children of the view as a List of IChimpViews
     */
    public List<IChimpView> getChildren() {
        List<String> results = Lists.newArrayList(queryView("getchildren").split(" "));
        /* We make sure this has an even number of results because we don't necessarily know how
         * many children there are, but we know all children will return a pair of accessibility ids
         */
        if (results.size() % 2 == 0) {
            List<IChimpView> children = new ArrayList<IChimpView>();
            for (int i = 0; i < results.size()/2; i++) {
                List<String> ids = Lists.newArrayList(results.get(2 * i), results.get(2 * i + 1));
                ChimpView child = new ChimpView(ChimpView.ACCESSIBILITY_IDS, ids);
                child.setManager(manager);
                children.add(child);
            }
            return children;
        }
        return new ArrayList<IChimpView>();
    }


    /**
     * Gets the accessibility ids of the current view
     * @return the accessibility ids of the current view. Its returned as a two-item array of ints
     * with first int being the window id, and the second int being the accessibility view id.
     */
    public int[] getAccessibilityIds() {
        List<String> results = Lists.newArrayList(queryView("getaccessibilityids").split(" "));
        if (results.size() == 2) {
            int[] accessibilityIds = new int[2];
            try {
                accessibilityIds[0] = Integer.parseInt(results.get(0));
                accessibilityIds[1] = Integer.parseInt(results.get(1));
                return accessibilityIds;
            } catch (NumberFormatException e) {
                LOG.log(Level.SEVERE, "Error retrieving accesibility ids: " + e.getMessage());
            }
        }
        int[] empty = {0,0};
        return empty;
    }

}
