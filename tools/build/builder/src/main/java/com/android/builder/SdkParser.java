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

import com.android.annotations.NonNull;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.ILogger;

/**
 * A parser able to parse the SDK and return valuable information to the build system.
 *
 */
public interface SdkParser {

    /**
     * Resolves a target hash string and returns the corresponding {@link IAndroidTarget}
     * @param target the target hash string.
     * @param logger a logger object.
     * @return the target or null if no match is found.
     *
     * @throws RuntimeException if the SDK cannot parsed.
     *
     * @see IAndroidTarget#hashString()
     */
    IAndroidTarget resolveTarget(@NonNull String target, @NonNull ILogger logger);

    String getAnnotationsJar();

}
