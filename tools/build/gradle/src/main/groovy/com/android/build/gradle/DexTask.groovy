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
package com.android.build.gradle

import com.android.builder.DexOptions
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class DexTask extends BaseAndroidTask {
    @OutputFile
    File outputFile

    @InputFiles
    Iterable<File> sourceFiles

    @InputFiles
    Iterable<File> libraries

    @Nested
    DexOptions dexOptions

    @TaskAction
    void generate() {
        List<String> files = new ArrayList<String>();
        for (File f : getSourceFiles()) {
            if (f != null && f.exists()) {
                files.add(f.absolutePath)
            }
        }

        List<String> libs = new ArrayList<String>();
        for (File f : getLibraries()) {
            if (f != null && f.exists()) {
                libs.add(f.absolutePath)
            }
        }

        getBuilder().convertBytecode(files, libs, getOutputFile().absolutePath, getDexOptions())
    }
}
