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
package com.android.build.gradle.internal

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

/**
 * Implementation of the AndroidBuilder SourceSet on top of a gradle SourceSet
 */
public class AndroidSourceSet implements com.android.builder.SourceSet {

    final SourceSet sourceSet
    private final String name
    private final Project project

    public AndroidSourceSet(SourceSet sourceSet, Project project) {
        this.sourceSet = sourceSet
        this.name = sourceSet.name
        this.project = project
    }

    @Override
    Set<File> getJavaResources() {
        return sourceSet.resources.srcDirs
    }

    @Override
    Iterable<File> getCompileClasspath() {
        return sourceSet.compileClasspath
    }

    @Override
    File getAndroidResources() {
        // FIXME: make this configurable by the SourceSet
        return project.file("src/$name/res")
    }

    @Override
    File getAndroidAssets() {
        // FIXME: make this configurable by the SourceSet
        return project.file("src/$name/assets")
    }

    @Override
    File getAndroidManifest() {
        // FIXME: make this configurable by the SourceSet
        return project.file("src/$name/AndroidManifest.xml")
    }

    @Override
    File getAidlSource() {
        // FIXME: make this configurable by the SourceSet
        return project.file("src/$name/aidl")
    }

    @Override
    File getRenderscriptSource() {
        // FIXME: make this configurable by the SourceSet
        return project.file("src/$name/rs")
    }

    @Override
    File getNativeSource() {
        // FIXME: make this configurable by the SourceSet
        return project.file("src/$name/jni")
    }
}
