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

import com.android.build.gradle.internal.BuildTypeData
import com.android.build.gradle.internal.BuildTypeDsl
import com.android.build.gradle.internal.ProductFlavorData
import com.android.build.gradle.internal.ProductFlavorDsl
import com.android.build.gradle.internal.ProductionAppVariant
import com.android.build.gradle.internal.TestAppVariant
import com.android.builder.BuildType
import com.android.builder.VariantConfiguration
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin

class AndroidPlugin extends AndroidBasePlugin implements Plugin<Project> {
    private final Map<String, BuildTypeData> buildTypes = [:]
    private final Map<String, ProductFlavorData> productFlavors = [:]

    private AndroidExtension extension

    @Override
    void apply(Project project) {
        super.apply(project)

        def buildTypeContainer = project.container(BuildTypeDsl)
        def productFlavorContainer = project.container(ProductFlavorDsl)

        extension = project.extensions.create('android', AndroidExtension,
                buildTypeContainer, productFlavorContainer)
        setDefaultConfig(extension.defaultConfig)

        buildTypeContainer.whenObjectAdded { BuildType buildType ->
            addBuildType(buildType)
        }
        buildTypeContainer.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing build types is not implemented yet.")
        }

        buildTypeContainer.create(BuildType.DEBUG)
        buildTypeContainer.create(BuildType.RELEASE)

        productFlavorContainer.whenObjectAdded { ProductFlavorDsl productFlavor ->
            addProductFlavor(productFlavor)
        }

        productFlavorContainer.whenObjectRemoved {
            throw new UnsupportedOperationException(
                    "Removing product flavors is not implemented yet.")
        }

        project.afterEvaluate {
            createAndroidTasks()
        }
    }

    private void addBuildType(BuildType buildType) {
        if (buildType.name.startsWith("test")) {
            throw new RuntimeException("BuildType names cannot start with 'test'")
        }
        if (productFlavors.containsKey(buildType.name)) {
            throw new RuntimeException("BuildType names cannot collide with ProductFlavor names")
        }

        def sourceSet = project.sourceSets.add(buildType.name)

        // TODO remove when moving to custom source sets
        project.tasks.remove(project.tasks.getByName("${buildType.name}Classes"))
        project.tasks.remove(project.tasks.getByName("compile${buildType.name.capitalize()}Java"))
        project.tasks.remove(project.tasks.getByName("process${buildType.name.capitalize()}Resources"))

        BuildTypeData buildTypeData = new BuildTypeData(buildType, sourceSet, project)
        project.tasks.assemble.dependsOn buildTypeData.assembleTask

        buildTypes[buildType.name] = buildTypeData
    }

    private void addProductFlavor(ProductFlavorDsl productFlavor) {
        if (productFlavor.name.startsWith("test")) {
            throw new RuntimeException("ProductFlavor names cannot start with 'test'")
        }
        if (buildTypes.containsKey(productFlavor.name)) {
            throw new RuntimeException("ProductFlavor names cannot collide with BuildType names")
        }

        def mainSourceSet = project.sourceSets.add(productFlavor.name)
        String testName = "test${productFlavor.name.capitalize()}"
        def testSourceSet = project.sourceSets.add(testName)

        // TODO remove when moving to custom source sets
        project.tasks.remove(project.tasks.getByName("${productFlavor.name}Classes"))
        project.tasks.remove(project.tasks.getByName("compile${productFlavor.name.capitalize()}Java"))
        project.tasks.remove(project.tasks.getByName("process${productFlavor.name.capitalize()}Resources"))
        project.tasks.remove(project.tasks.getByName("${testName}Classes"))
        project.tasks.remove(project.tasks.getByName("compile${testName.capitalize()}Java"))
        project.tasks.remove(project.tasks.getByName("process${testName.capitalize()}Resources"))

        ProductFlavorData productFlavorData = new ProductFlavorData(
                productFlavor, mainSourceSet, testSourceSet, project)

        productFlavors[productFlavor.name] = productFlavorData
    }

    private void createAndroidTasks() {
        if (productFlavors.isEmpty()) {
            createTasksForDefaultBuild()
        } else {
            // there'll be more than one test app, so we need a top level assembleTest
            assembleTest = project.tasks.add("assembleTest")
            assembleTest.group = BasePlugin.BUILD_GROUP
            assembleTest.description = "Assembles all the Test applications"

            // check whether we have multi flavor builds
            if (extension.flavorGroupList == null || extension.flavorGroupList.size() < 2) {
                productFlavors.values().each { ProductFlavorData productFlavorData ->
                    createTasksForFlavoredBuild(productFlavorData)
                }
            } else {
                // need to group the flavor per group.
                // First a map of group -> list(ProductFlavor)
                ArrayListMultimap<String, ProductFlavorData> map = ArrayListMultimap.create();
                productFlavors.values().each { ProductFlavorData productFlavorData ->
                    def flavor = productFlavorData.productFlavor
                    if (flavor.flavorGroup == null) {
                        throw new RuntimeException(
                                "Flavor ${flavor.name} has no flavor group.")
                    }
                    if (!extension.flavorGroupList.contains(flavor.flavorGroup)) {
                        throw new RuntimeException(
                                "Flavor ${flavor.name} has unknown group ${flavor.flavorGroup}.")
                    }

                    map.put(flavor.flavorGroup, productFlavorData)
                }

                // now we use the flavor groups to generate an ordered array of flavor to use
                ProductFlavorData[] array = new ProductFlavorData[extension.flavorGroupList.size()]
                createTasksForMultiFlavoredBuilds(array, 0, map)
            }
        }
    }

    private createTasksForMultiFlavoredBuilds(ProductFlavorData[] datas, int i,
                                              ListMultimap<String, ProductFlavorData> map) {
        if (i == datas.length) {
            createTasksForFlavoredBuild(datas)
            return
        }

        // fill the array at the current index
        def group = extension.flavorGroupList.get(i)
        def flavorList = map.get(group)
        for (ProductFlavorData flavor : flavorList) {
           datas[i] = flavor
            createTasksForMultiFlavoredBuilds(datas, i+1, map)
        }
    }

    /**
     * Creates Tasks for non-flavored build. This means assembleDebug, assembleRelease, and other
     * assemble<Type> are directly building the <type> build instead of all build of the given
     * <type>.
     */
    private createTasksForDefaultBuild() {

        BuildTypeData testData = buildTypes[extension.testBuildType]
        if (testData == null) {
            throw new RuntimeException("Test Build Type '$extension.testBuildType' does not exist.")
        }

        ProductionAppVariant testedVariant = null

        ProductFlavorData defaultConfigData = getDefaultConfigData();

        for (BuildTypeData buildTypeData : buildTypes.values()) {

            def variantConfig = new VariantConfiguration(
                    defaultConfigData.productFlavor, defaultConfigData.androidSourceSet,
                    buildTypeData.buildType, buildTypeData.androidSourceSet)

            ProductionAppVariant productionAppVariant = addVariant(variantConfig,
                    buildTypeData.assembleTask)

            if (buildTypeData == testData) {
                testedVariant = productionAppVariant
            }
        }

        assert testedVariant != null

        def testVariantConfig = new VariantConfiguration(
                defaultConfigData.productFlavor, defaultConfigData.androidTestSourceSet,
                testData.buildType, null,
                VariantConfiguration.Type.TEST, testedVariant.config)

        // TODO: add actual dependencies
        testVariantConfig.setAndroidDependencies(null)

        def testVariant = new TestAppVariant(testVariantConfig)
        createTestTasks(testVariant, testedVariant)
    }

    /**
     * Creates Task for a given flavor. This will create tasks for all build types for the given
     * flavor.
     * @param flavorDataList the flavor(s) to build.
     */
    private createTasksForFlavoredBuild(ProductFlavorData... flavorDataList) {

        BuildTypeData testData = buildTypes[extension.testBuildType]
        if (testData == null) {
            throw new RuntimeException("Test Build Type '$extension.testBuildType' does not exist.")
        }

        ProductionAppVariant testedVariant = null

        // assembleTask for this flavor(group)
        def assembleTask

        for (BuildTypeData buildTypeData : buildTypes.values()) {

            def variantConfig = new VariantConfiguration(
                    extension.defaultConfig, getDefaultConfigData().androidSourceSet,
                    buildTypeData.buildType, buildTypeData.androidSourceSet)

            for (ProductFlavorData data : flavorDataList) {
                variantConfig.addProductFlavor(data.productFlavor, data.androidSourceSet)
            }

            ProductionAppVariant productionAppVariant = addVariant(variantConfig, null)

            buildTypeData.assembleTask.dependsOn productionAppVariant.assembleTask

            if (assembleTask == null) {
                // create the task based on the name of the flavors.
                assembleTask = createAssembleTask(flavorDataList)
                project.tasks.assemble.dependsOn assembleTask
            }
            assembleTask.dependsOn productionAppVariant.assembleTask

            if (buildTypeData == testData) {
                testedVariant = productionAppVariant
            }
        }

        assert testedVariant != null

        def testVariantConfig = new VariantConfiguration(
                extension.defaultConfig, getDefaultConfigData().androidTestSourceSet,
                testData.buildType, null,
                VariantConfiguration.Type.TEST, testedVariant.config)

        for (ProductFlavorData data : flavorDataList) {
            testVariantConfig.addProductFlavor(data.productFlavor, data.androidTestSourceSet)
        }

        // TODO: add actual dependencies
        testVariantConfig.setAndroidDependencies(null)

        def testVariant = new TestAppVariant(testVariantConfig)
        createTestTasks(testVariant, testedVariant)
    }

    private Task createAssembleTask(ProductFlavorData[] flavorDataList) {
        def name = ProductFlavorData.getFlavoredName(flavorDataList, true)

        def assembleTask = project.tasks.add("assemble${name}")
        assembleTask.description = "Assembles all builds for flavor ${name}"
        assembleTask.group = "Build"

        return assembleTask
    }

    /**
     * Creates build tasks for a given variant.
     * @param variantConfig
     * @param assembleTask an optional assembleTask to be used. If null, a new one is created.
     * @return
     */
    private ProductionAppVariant addVariant(VariantConfiguration variantConfig, Task assembleTask) {

        def variant = new ProductionAppVariant(variantConfig)

        def prepareDependenciesTask = createPrepareDependenciesTask(variant)

        // Add a task to process the manifest(s)
        def processManifestTask = createProcessManifestTask(variant, "manifests")
        // TODO - move this
        processManifestTask.dependsOn prepareDependenciesTask

        // Add a task to crunch resource files
        def crunchTask = createCrunchResTask(variant)

        // Add a task to create the BuildConfig class
        def generateBuildConfigTask = createBuildConfigTask(variant, null)

        // Add a task to generate resource source files
        def processResources = createProcessResTask(variant, processManifestTask, crunchTask)

        def compileAidl = createAidlTask(variant)
        // TODO - move this
        compileAidl.dependsOn prepareDependenciesTask

        // Add a compile task
        createCompileTask(variant, null/*testedVariant*/, processResources, generateBuildConfigTask,
                compileAidl)

        addPackageTasks(variant, assembleTask)

        return variant;
    }

    @Override
    String getTarget() {
        return extension.target;
    }
}
