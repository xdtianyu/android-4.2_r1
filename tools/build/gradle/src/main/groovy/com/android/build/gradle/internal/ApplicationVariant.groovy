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

import com.android.build.gradle.AndroidBasePlugin
import com.android.builder.AndroidBuilder
import com.android.builder.ProductFlavor
import com.android.builder.VariantConfiguration
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.Compile

/**
 * Represents something that can be packaged into an APK and installed.
 */
public abstract class ApplicationVariant {

    final VariantConfiguration config
    FileCollection runtimeClasspath
    FileCollection packagedClasspath
    FileCollection resourcePackage
    Compile compileTask
    Iterable<Object> configObjects
    Task assembleTask
    Task installTask
    Task uninstallTask

    ApplicationVariant(VariantConfiguration config) {
        this.config = config
    }

    Iterable<Object> getConfigObjects() {
        if (configObjects == null) {
            configObjects = config.configObjects
        }

        return configObjects
    }

    List<String> getBuildConfigLines() {
        return config.buildConfigLines
    }

    abstract String getDescription()

    abstract String getDirName()

    abstract String getBaseName()


    abstract boolean getZipAlign()

    abstract boolean isSigned()

    abstract boolean getRunProguard()

    abstract List<String> getRunCommand()

    abstract String getPackage()

    abstract AndroidBuilder createBuilder(AndroidBasePlugin androidBasePlugin)

    protected String getFlavoredName(boolean capitalized) {
        StringBuilder builder = new StringBuilder()
        for (ProductFlavor flavor : config.flavorConfigs) {
            builder.append(capitalized ? flavor.name.capitalize() : flavor.name)
        }

        return builder.toString()
    }
}
