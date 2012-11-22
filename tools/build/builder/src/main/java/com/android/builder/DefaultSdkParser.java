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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManager;
import com.android.utils.ILogger;

import java.io.File;

/**
 * Default implementation of {@link SdkParser} for a normal Android SDK distribution.
 */
public class DefaultSdkParser implements SdkParser {

    private final String mSdkLocation;
    private SdkManager mManager;

    public DefaultSdkParser(@NonNull String sdkLocation) {
        if (!sdkLocation.endsWith(File.separator)) {
            mSdkLocation = sdkLocation + File.separator;
        } else {
            mSdkLocation = sdkLocation;
        }
    }

    @Override
    public IAndroidTarget resolveTarget(String target, ILogger logger) {
        if (mManager == null) {
            mManager = SdkManager.createManager(mSdkLocation, logger);
            if (mManager == null) {
                throw new RuntimeException("failed to parse SDK!");
            }
        }

        return mManager.getTargetFromHashString(target);
    }

    @Override
    public String getAnnotationsJar() {
        return mSdkLocation + SdkConstants.FD_TOOLS +
                '/' + SdkConstants.FD_SUPPORT +
                '/' + SdkConstants.FN_ANNOTATIONS_JAR;
    }
}
