/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.builder;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * Implementation of SourceSet for testing that provides the default convention paths.
 */
class MockSourceSet implements SourceSet {

    public MockSourceSet(String root) {
        mRoot = root;
    }

    private final String mRoot;

    @Override
    public Set<File> getJavaResources() {
        return Collections.singleton(new File(mRoot, "resources"));
    }

    @Override
    public Iterable<File> getCompileClasspath() {
        return null;
    }

    @Override
    public File getAndroidResources() {
        return new File(mRoot, "res");
    }

    @Override
    public File getAndroidAssets() {
        return new File(mRoot, "assets");
    }

    @Override
    public File getAndroidManifest() {
        return new File(mRoot, "AndroidManifest.xml");
    }

    @Override
    public File getAidlSource() {
        return new File(mRoot, "aidl");
    }

    @Override
    public File getRenderscriptSource() {
        return new File(mRoot, "rs");
    }

    @Override
    public File getNativeSource() {
        return new File(mRoot, "jni");
    }
}
