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

import com.android.build.gradle.internal.AaptOptionsImpl
import com.android.build.gradle.internal.DexOptionsImpl
import com.android.build.gradle.internal.ProductFlavorDsl
import org.gradle.api.Action

/**
 * Base android extension for all android plugins.
 */
class BaseAndroidExtension {

    String target
    final ProductFlavorDsl defaultConfig = new ProductFlavorDsl("main");

    final AaptOptionsImpl aaptOptions = new AaptOptionsImpl()
    final DexOptionsImpl dexOptions = new DexOptionsImpl()

    BaseAndroidExtension() {
    }

    void defaultConfig(Action<ProductFlavorDsl> action) {
        action.execute(defaultConfig)
    }

    void aaptOptions(Action<AaptOptionsImpl> action) {
        action.execute(aaptOptions)
    }

    void dexOptions(Action<DexOptionsImpl> action) {
        action.execute(dexOptions)
    }
}
