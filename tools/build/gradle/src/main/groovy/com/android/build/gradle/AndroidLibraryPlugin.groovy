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
import com.android.build.gradle.internal.ProductFlavorData
import com.android.build.gradle.internal.ProductionAppVariant
import com.android.build.gradle.internal.TestAppVariant
import com.android.builder.AndroidDependency
import com.android.builder.BuildType

import com.android.builder.VariantConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import com.android.builder.BundleDependency

class AndroidLibraryPlugin extends AndroidBasePlugin implements Plugin<Project> {

    private final static String DIR_BUNDLES = "bundles";

    AndroidLibraryExtension extension
    BuildTypeData debugBuildTypeData
    BuildTypeData releaseBuildTypeData

    @Override
    void apply(Project project) {
        super.apply(project)

        extension = project.extensions.create('android', AndroidLibraryExtension)
        setDefaultConfig(extension.defaultConfig)

        // create the source sets for the build type.
        // the ones for the main product flavors are handled by the base plugin.
        def debugSourceSet = project.sourceSets.add(BuildType.DEBUG)
        def releaseSourceSet = project.sourceSets.add(BuildType.RELEASE)

        // TODO remove when moving to custom source sets
        project.tasks.remove(project.tasks.getByName("debugClasses"))
        project.tasks.remove(project.tasks.getByName("compileDebugJava"))
        project.tasks.remove(project.tasks.getByName("processDebugResources"))
        project.tasks.remove(project.tasks.getByName("releaseClasses"))
        project.tasks.remove(project.tasks.getByName("compileReleaseJava"))
        project.tasks.remove(project.tasks.getByName("processReleaseResources"))

        debugBuildTypeData = new BuildTypeData(extension.debug, debugSourceSet, project)
        releaseBuildTypeData = new BuildTypeData(extension.release, releaseSourceSet, project)
        project.tasks.assemble.dependsOn debugBuildTypeData.assembleTask
        project.tasks.assemble.dependsOn releaseBuildTypeData.assembleTask

        createConfigurations()

        project.afterEvaluate {
            createAndroidTasks()
        }
    }

    void createConfigurations() {
        def debugConfig = project.configurations.add(BuildType.DEBUG)
        def releaseConfig = project.configurations.add(BuildType.RELEASE)
        debugConfig.extendsFrom(project.configurations.runtime)
        releaseConfig.extendsFrom(project.configurations.runtime)
        project.configurations["default"].extendsFrom(releaseConfig)

        // Adjust the pom scope mappings
        // TODO - this should be part of JavaBase plugin. Fix this in Gradle
        project.plugins.withType(MavenPlugin) {
            project.conf2ScopeMappings.addMapping(300, project.configurations.compile, "runtime")
            project.conf2ScopeMappings.addMapping(300, project.configurations.runtime, "runtime")
            project.conf2ScopeMappings.addMapping(300, releaseConfig, "runtime")
        }
    }

    void createAndroidTasks() {
        ProductionAppVariant testedVariant = createLibraryTasks(debugBuildTypeData)
        createLibraryTasks(releaseBuildTypeData)
        createTestTasks(testedVariant)
    }

    ProductionAppVariant createLibraryTasks(BuildTypeData buildTypeData) {
        ProductFlavorData defaultConfigData = getDefaultConfigData();

        def variantConfig = new VariantConfiguration(
                defaultConfigData.productFlavor, defaultConfigData.androidSourceSet,
                buildTypeData.buildType, buildTypeData.androidSourceSet,
                VariantConfiguration.Type.LIBRARY)

        ProductionAppVariant variant = new ProductionAppVariant(variantConfig)

        def prepareDependenciesTask = createPrepareDependenciesTask(variant)

        // Add a task to process the manifest(s)
        ProcessManifestTask processManifestTask = createProcessManifestTask(variant, DIR_BUNDLES)
        // TODO - move this
        processManifestTask.dependsOn prepareDependenciesTask

        // Add a task to create the BuildConfig class
        def generateBuildConfigTask = createBuildConfigTask(variant, null)

        // Add a task to generate resource source files
        def processResources = createProcessResTask(variant, processManifestTask,
                null /*crunchTask*/)

        def compileAidl = createAidlTask(variant)
        // TODO - move this
        compileAidl.dependsOn prepareDependenciesTask

        // Add a compile task
        createCompileTask(variant, null/*testedVariant*/, processResources, generateBuildConfigTask,
                compileAidl)

        // jar the classes.
        Jar jar = project.tasks.add("${buildTypeData.buildType.name}Jar", Jar);
        jar.from(variant.compileTask.outputs);
        // TODO: replace with proper ProcessResources task with properly configured SourceDirectorySet
        jar.from(defaultConfigData.androidSourceSet.javaResources);
        jar.from(buildTypeData.androidSourceSet.javaResources);

        jar.destinationDir = project.file("$project.buildDir/$DIR_BUNDLES/${variant.dirName}")
        jar.archiveName = "classes.jar"
        String packageName = variantConfig.getPackageFromManifest().replace('.', '/');
        jar.exclude(packageName + "/R.class")
        jar.exclude(packageName + "/R\$*.class")
        jar.exclude(packageName + "/Manifest.class")
        jar.exclude(packageName + "/Manifest\$*.class")

        // package the resources into the bundle folder
        Copy packageRes = project.tasks.add("package${variant.name}Res", Copy)
        // packageRes from 3 sources. the order is important to make sure the override works well.
        // TODO: fix the case of values -- need to merge the XML!
        packageRes.from(defaultConfigData.androidSourceSet.androidResources,
                buildTypeData.androidSourceSet.androidResources)
        packageRes.into(project.file("$project.buildDir/$DIR_BUNDLES/${variant.dirName}/res"))

        // package the aidl files into the bundle folder
        Copy packageAidl = project.tasks.add("package${variant.name}Aidl", Copy)
        // packageAidl from 3 sources. the order is important to make sure the override works well.
        // TODO: fix the case of values -- need to merge the XML!
        packageAidl.from(defaultConfigData.androidSourceSet.aidlSource,
                buildTypeData.androidSourceSet.aidlSource)
        packageAidl.into(project.file("$project.buildDir/$DIR_BUNDLES/${variant.dirName}/aidl"))

        Zip bundle = project.tasks.add("bundle${variant.name}", Zip)
        bundle.dependsOn jar, packageRes, packageAidl
        bundle.setDescription("Assembles a bundle containing the library in ${variant.name}.");
        bundle.destinationDir = project.file("$project.buildDir/libs")
        bundle.extension = "alb"
        if (variant.baseName != BuildType.RELEASE) {
            bundle.classifier = variant.baseName
        }
        bundle.from(project.file("$project.buildDir/$DIR_BUNDLES/${variant.dirName}"))

        project.artifacts.add(buildTypeData.buildType.name, bundle)

        buildTypeData.assembleTask.dependsOn bundle
        variant.assembleTask = bundle

        // configure the variant to be testable.
        variantConfig.output = new BundleDependency(
                project.file("$project.buildDir/$DIR_BUNDLES/${variant.dirName}")) {

            @Override
            List<AndroidDependency> getDependencies() {
                return variantConfig.directLibraries
            }
        };

        return variant
    }

    void createTestTasks(ProductionAppVariant testedVariant) {
        ProductFlavorData defaultConfigData = getDefaultConfigData();

        def testVariantConfig = new VariantConfiguration(
                defaultConfigData.productFlavor, defaultConfigData.androidTestSourceSet,
                debugBuildTypeData.buildType, null,
                VariantConfiguration.Type.TEST, testedVariant.config)
        // TODO: add actual dependencies
        testVariantConfig.setAndroidDependencies(null)

        def testVariant = new TestAppVariant(testVariantConfig,)
        createTestTasks(testVariant, testedVariant)
    }

    @Override
    String getTarget() {
        return extension.target
    }

    protected String getManifestOutDir() {
        return DIR_BUNDLES
    }
}
