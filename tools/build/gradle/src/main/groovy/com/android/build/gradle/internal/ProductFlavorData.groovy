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
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

class ProductFlavorData {
    final ProductFlavorDsl productFlavor

    final SourceSet mainSource
    final AndroidSourceSet androidSourceSet

    final SourceSet testSource
    final AndroidSourceSet androidTestSourceSet

    Task assembleTask

    ProductFlavorData(ProductFlavorDsl productFlavor, SourceSet mainSource, SourceSet testSource,
                           Project project) {
        this.productFlavor = productFlavor

        this.mainSource = mainSource
        androidSourceSet = new AndroidSourceSet(mainSource, project)

        this.testSource = testSource
        androidTestSourceSet = new AndroidSourceSet(testSource, project)
    }


    public static String getFlavoredName(ProductFlavorData[] flavorDataArray, boolean capitalized) {
        StringBuilder builder = new StringBuilder()
        for (ProductFlavorData data : flavorDataArray) {
            builder.append(capitalized ?
                data.productFlavor.name.capitalize() :
                data.productFlavor.name)
        }

        return builder.toString()
    }
}
