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

/* A class for selecting objects by their id */
public class SelectorId implements ISelector {
    private String id;
    /**
     * @param id the id to select objects by
     */
    public SelectorId(String id){
        this.id = id;
    }

    /**
     * A method for selecting a view by the given id.
     * @return The view with the given id
     */
    public IChimpView getView(ChimpManager manager) {
        ChimpView view = new ChimpView(ChimpView.VIEW_ID, Lists.newArrayList(id));
        view.setManager(manager);
        return view;
    }
}
