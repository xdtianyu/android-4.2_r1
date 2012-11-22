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

package com.android.cts.nativescanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scanner of C++ gTest source files.
 *
 * It looks for test declarations and outputs a file following this format:
 *
 * class:TestClass1
 * method:testMethod1
 * method:testMethod2
 * class:TestClass2
 * method:testMethod1
 *
 */
class TestScanner {

    /** Directory to recursively scan for gTest test declarations. */
    private final File mSourceDir;

    private final String mTestSuite;

    TestScanner(File sourceDir, String testSuite) {
        mSourceDir = sourceDir;
        mTestSuite = testSuite;
    }

    public List<String> getTestNames() throws IOException {
        List<String> testNames = new ArrayList<String>();
        scanDir(mSourceDir, testNames);
        return testNames;
    }

    private void scanDir(File dir, List<String> testNames) throws FileNotFoundException {
        // Find both C++ files to find tests and directories to look for more tests!
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".cpp") || filename.endsWith(".cc")
                        || new File(dir, filename).isDirectory();
            }
        });

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                scanDir(file, testNames);
            } else {
                scanFile(file, testNames);
            }
        }
    }
    // We want to find lines like class SLObjectCreationTest : public ::testing::Test { ...
    // and extract the "SLObjectCreationTest" as group #1
    private static final Pattern CLASS_REGEX =
            Pattern.compile("\\s*class\\s+(\\w+).*");

    // We want to find lines like TEST_F(SLObjectCreationTest, testAudioPlayerFromFdCreation) { ...
    // and extract the "testAudioPlayerFromFdCreation" as group #1
    private static final Pattern METHOD_REGEX =
            Pattern.compile("\\s*TEST_F\\(\\w+,\\s*(\\w+)\\).*");

    private void scanFile(File file, List<String> testNames) throws FileNotFoundException {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher matcher = CLASS_REGEX.matcher(line);
                if (matcher.matches()) {
                    testNames.add("suite:" + mTestSuite);
                    testNames.add("case:" + matcher.group(1));
                    continue;
                }

                matcher = METHOD_REGEX.matcher(line);
                if (matcher.matches()) {
                    testNames.add("test:" + matcher.group(1));
                    continue;
                }
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}
