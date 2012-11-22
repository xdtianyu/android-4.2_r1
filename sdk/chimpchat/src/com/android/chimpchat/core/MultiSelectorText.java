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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A class for selecting objects by their text */
public class MultiSelectorText implements IMultiSelector {
    private static final Logger LOG = Logger.getLogger(ChimpView.class.getName());
    private String text;

    /**
     * @param text the text which to select objects by
     */
    public MultiSelectorText(String text) {
        this.text = text;
    }

    /**
     * A method for selecting views by the given text.
     * @return The collection of views that contain the given text
     */
    public Collection<IChimpView> getViews(ChimpManager manager) {
        String response;
        List<String> ids;
        try {
            response = manager.getViewsWithText(text);
            ids = Arrays.asList(response.split(" "));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error communicating with device: " + e.getMessage());
            return new ArrayList<IChimpView>();
        }
        /* We make sure this has an even number of results because we don't necessarily know how
         * many views with the given textthere are, but we know all of the views will return a pair
         * of accessibility ids */
        if (ids.size() % 2 == 0) {
            List<IChimpView> views = new ArrayList<IChimpView>();
            for (int i = 0; i < ids.size()/2; i++) {
                List<String> accessibilityIds =
                        Lists.newArrayList(ids.get(2 * i ), ids.get(2 * i + 1));
                ChimpView view = new ChimpView(ChimpView.ACCESSIBILITY_IDS, accessibilityIds);
                view.setManager(manager);
                views.add(view);
            }
            return views;
        }
        LOG.log(Level.SEVERE, "Error retrieving views: " + response);
        return Collections.emptyList();
    }
}
