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

/**
 * A class that lets you select objects based on different criteria.
 * It operates similar to WebDriver's By class.
 */
public class By {
    /**
     * A method to let you select items by id.
     * @param id The string id of the object you want
     * @return a selector that will select the appropriate item by id
     */
    public static ISelector id(String id) {
        return new SelectorId(id);
    }

    /**
     * A method that lets you select items by accessibility ids.
     * @param windowId the windowId of the object you want to select.
     * @param accessibilityId the accessibility id of the object you want to select
     * @return a selector that will select the appropriate object by its accessibility ids.
     */
    public static ISelector accessibilityIds(int windowId, int accessibilityId){
        return new SelectorAccessibilityIds(windowId, accessibilityId);
    }

    public static IMultiSelector text(String searchText) {
        return new MultiSelectorText(searchText);
    }
}
