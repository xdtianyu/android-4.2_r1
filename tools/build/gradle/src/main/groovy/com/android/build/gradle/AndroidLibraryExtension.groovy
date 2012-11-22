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

import com.android.build.gradle.internal.BuildTypeDsl
import com.android.builder.BuildType
import org.gradle.api.Action

class AndroidLibraryExtension extends BaseAndroidExtension {

    final BuildTypeDsl debug = new BuildTypeDsl(BuildType.DEBUG)
    final BuildTypeDsl release = new BuildTypeDsl(BuildType.RELEASE)

    AndroidLibraryExtension() {
        super()
    }

    void debug(Action<BuildTypeDsl> action) {
        action.execute(debug);
    }

    void release(Action<BuildTypeDsl> action) {
        action.execute(release);
    }
}
