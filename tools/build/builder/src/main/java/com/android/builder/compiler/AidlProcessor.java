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

package com.android.builder.compiler;

import com.android.annotations.NonNull;
import com.android.builder.CommandLineRunner;
import com.android.builder.compiler.SourceGenerator.DisplayType;
import com.android.utils.ILogger;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class AidlProcessor implements SourceGenerator.Processor {

    private final String mAidlExecutable;
    private final String mFrameworkLocation;
    private final List<File> mImportFolders;
    private final CommandLineRunner mRunner;

    public AidlProcessor(@NonNull String aidlExecutable,
                         @NonNull String frameworkLocation,
                         @NonNull List<File> importFolders,
                         @NonNull CommandLineRunner runner) {
        mAidlExecutable = aidlExecutable;
        mFrameworkLocation = frameworkLocation;
        mImportFolders = importFolders;
        mRunner = runner;
    }

    @Override
    public String getSourceFileExtension() {
        return "aidl";
    }

    @Override
    public void process(File filePath, List<File> sourceFolders, File sourceOutputDir,
                        ILogger logger)
            throws IOException, InterruptedException {

        ArrayList<String> command = Lists.newArrayList();

        command.add(mAidlExecutable);

        command.add("-p" + mFrameworkLocation);
        command.add("-o" + sourceOutputDir.getAbsolutePath());
        // add all the source folders as import in case an aidl file in a source folder
        // imports a parcelable from another source folder.
        for (File sourceFolder : sourceFolders) {
            if (sourceFolder.isDirectory()) {
                command.add("-I" + sourceFolder.getAbsolutePath());
            }
        }

        // add all the library aidl folders to access parcelables that are in libraries
        for (File f : mImportFolders) {
            command.add("-I" + f.getAbsolutePath());
        }

        // set auto dependency file creation
        command.add("-a");

        command.add(filePath.getAbsolutePath());

        logger.info("aidl command: %s", command.toString());

        mRunner.runCmdLine(command);
    }

    @Override
    public void displayMessage(ILogger logger, DisplayType type, int count) {
        switch (type) {
            case FOUND:
                logger.info("Found %1$d AIDL files.", count);
                break;
            case COMPILING:
                if (count > 0) {
                    logger.info("Compiling %1$d AIDL files.", count);
                } else {
                    logger.info("No AIDL files to compile.");
                }
                break;
            case REMOVE_OUTPUT:
                logger.info("Found %1$d obsolete output files to remove.", count);
                break;
            case REMOVE_DEP:
                logger.info("Found %1$d obsolete dependency files to remove.", count);
                break;
        }
    }
}
