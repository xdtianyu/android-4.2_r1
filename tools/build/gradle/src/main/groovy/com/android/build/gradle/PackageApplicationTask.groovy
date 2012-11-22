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

import com.android.builder.packaging.DuplicateFileException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class PackageApplicationTask extends BaseAndroidTask {
    @OutputFile
    File outputFile

    @InputFile
    File resourceFile

    @InputFile
    File dexFile

    @InputDirectory @Optional
    File jniDir

    @TaskAction
    void generate() {

        try {
            getBuilder().packageApk(
                    getResourceFile().absolutePath,
                    getDexFile().absolutePath,
                    getJniDir()?.absolutePath,
                    getOutputFile().absolutePath)
        } catch (DuplicateFileException e) {
            def logger = getLogger()
            logger.error("Error: duplicate files during packaging of APK " + getOutputFile().absolutePath)
            logger.error("\tPath in archive: " + e.archivePath)
            logger.error("\tOrigin 1: " + e.file1)
            logger.error("\tOrigin 2: " + e.file2)
            throw new RuntimeException();
        }
    }
}
