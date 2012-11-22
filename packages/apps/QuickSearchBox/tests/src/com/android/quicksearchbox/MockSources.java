/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox;

import android.content.ComponentName;

import java.util.Collection;
import java.util.HashMap;

/**
 * Mock implementation of {@link Sources}.
 */
public class MockSources implements Sources {

    private final HashMap<String, Source> mSources = new HashMap<String, Source>();

    public void addSource(Source source) {
        mSources.put(source.getName(), source);
    }

    public Source getSource(String name) {
        return mSources.get(name);
    }

    public Collection<Source> getSources() {
        return mSources.values();
    }

    public Source getWebSearchSource() {
        return null;
    }

    public void update() {
    }

    public Source createSourceFor(ComponentName component) {
        return null;
    }

}
