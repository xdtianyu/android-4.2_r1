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

import com.android.build.gradle.internal.AndroidDependencyImpl
import com.android.build.gradle.internal.AndroidSourceSet
import com.android.build.gradle.internal.ApplicationVariant
import com.android.build.gradle.internal.ProductFlavorData
import com.android.build.gradle.internal.ProductionAppVariant
import com.android.build.gradle.internal.TestAppVariant
import com.android.builder.AndroidBuilder
import com.android.builder.AndroidDependency
import com.android.builder.DefaultSdkParser
import com.android.builder.JarDependency
import com.android.builder.ProductFlavor
import com.android.builder.SdkParser
import com.android.builder.VariantConfiguration
import com.android.utils.ILogger
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.Compile

/**
 * Base class for all Android plugins
 */
abstract class AndroidBasePlugin {

    public final static String INSTALL_GROUP = "Install"

    private final Map<Object, AndroidBuilder> builders = [:]

    protected Project project
    protected File sdkDir
    private DefaultSdkParser androidSdkParser
    private AndroidLogger androidLogger

    private ProductFlavorData defaultConfigData
    protected SourceSet mainSourceSet
    protected SourceSet testSourceSet

    protected Task uninstallAll
    protected Task assembleTest

    abstract String getTarget()

    protected void apply(Project project) {
        this.project = project
        project.apply plugin: JavaBasePlugin

        mainSourceSet = project.sourceSets.add("main")
        testSourceSet = project.sourceSets.add("test")

        // TODO remove when moving to custom source sets
        project.tasks.remove(project.tasks.getByName("classes"))
        project.tasks.remove(project.tasks.getByName("compileJava"))
        project.tasks.remove(project.tasks.getByName("processResources"))
        project.tasks.remove(project.tasks.getByName("testClasses"))
        project.tasks.remove(project.tasks.getByName("compileTestJava"))
        project.tasks.remove(project.tasks.getByName("processTestResources"))

        project.tasks.assemble.description =
            "Assembles all variants of all applications and secondary packages."

        findSdk(project)

        uninstallAll = project.tasks.add("uninstallAll")
        uninstallAll.description = "Uninstall all applications."
        uninstallAll.group = INSTALL_GROUP
    }

    protected setDefaultConfig(ProductFlavor defaultConfig) {
        defaultConfigData = new ProductFlavorData(defaultConfig, mainSourceSet,
                testSourceSet, project)
    }

    ProductFlavorData getDefaultConfigData() {
        return defaultConfigData
    }

    SdkParser getSdkParser() {
        if (androidSdkParser == null) {
            androidSdkParser = new DefaultSdkParser(sdkDir.absolutePath)
        }

        return androidSdkParser;
    }

    ILogger getLogger() {
        if (androidLogger == null) {
            androidLogger = new AndroidLogger(project.logger)
        }

        return androidLogger
    }

    boolean isVerbose() {
        return project.logger.isEnabled(LogLevel.DEBUG)
    }

    AndroidBuilder getAndroidBuilder(ApplicationVariant variant) {
        AndroidBuilder androidBuilder = builders.get(variant)

        if (androidBuilder == null) {
            androidBuilder = variant.createBuilder(this)
            builders.put(variant, androidBuilder)
        }

        return androidBuilder
    }

    private void findSdk(Project project) {
        def localProperties = project.file("local.properties")
        if (localProperties.exists()) {
            Properties properties = new Properties()
            localProperties.withInputStream { instr ->
                properties.load(instr)
            }
            def sdkDirProp = properties.getProperty('sdk.dir')
            if (!sdkDirProp) {
                throw new RuntimeException("No sdk.dir property defined in local.properties file.")
            }
            sdkDir = new File(sdkDirProp)
        } else {
            def envVar = System.getenv("ANDROID_HOME")
            if (envVar != null) {
                sdkDir = new File(envVar)
            }
        }

        if (sdkDir == null) {
            throw new RuntimeException(
                    "SDK location not found. Define location with sdk.dir in the local.properties file or with an ANDROID_HOME environment variable.")
        }

        if (!sdkDir.directory) {
            throw new RuntimeException(
                    "The SDK directory '$sdkDir' specified in local.properties does not exist.")
        }
    }

    protected String getRuntimeJars(ApplicationVariant variant) {
        AndroidBuilder androidBuilder = getAndroidBuilder(variant)

        return androidBuilder.runtimeClasspath.join(":")
    }

    protected ProcessManifestTask createProcessManifestTask(ApplicationVariant variant,
                                                        String manifestOurDir) {
        def processManifestTask = project.tasks.add("process${variant.name}Manifest",
                ProcessManifestTask)
        processManifestTask.plugin = this
        processManifestTask.variant = variant
        processManifestTask.configObjects = variant.configObjects
        processManifestTask.conventionMapping.inputManifests = { variant.config.manifestInputs }
        processManifestTask.conventionMapping.processedManifest = {
            project.file(
                    "$project.buildDir/${manifestOurDir}/$variant.dirName/AndroidManifest.xml")
        }

        return processManifestTask
    }

    protected CrunchResourcesTask createCrunchResTask(ApplicationVariant variant) {
        def crunchTask = project.tasks.add("crunch${variant.name}Res", CrunchResourcesTask)
        crunchTask.plugin = this
        crunchTask.variant = variant
        crunchTask.configObjects = variant.configObjects
        crunchTask.conventionMapping.resDirectories = { variant.config.resourceInputs }
        crunchTask.conventionMapping.outputDir = {
            project.file("$project.buildDir/res/$variant.dirName")
        }

        return crunchTask
    }

    protected GenerateBuildConfigTask createBuildConfigTask(ApplicationVariant variant,
                                                            ProcessManifestTask processManifestTask) {
        def generateBuildConfigTask = project.tasks.add(
                "generate${variant.name}BuildConfig", GenerateBuildConfigTask)
        if (processManifestTask != null) {
            // This is in case the manifest is generated
            generateBuildConfigTask.dependsOn processManifestTask
        }
        generateBuildConfigTask.plugin = this
        generateBuildConfigTask.variant = variant
        generateBuildConfigTask.configObjects = variant.configObjects
        generateBuildConfigTask.optionalJavaLines = variant.buildConfigLines
        generateBuildConfigTask.conventionMapping.sourceOutputDir = {
            project.file("$project.buildDir/source/${variant.dirName}")
        }
        return generateBuildConfigTask
    }

    protected ProcessResourcesTask createProcessResTask(ApplicationVariant variant,
                                                    ProcessManifestTask processManifestTask,
                                                    CrunchResourcesTask crunchTask) {
        def processResources = project.tasks.add("process${variant.name}Res", ProcessResourcesTask)
        processResources.dependsOn processManifestTask
        processResources.plugin = this
        processResources.variant = variant
        processResources.configObjects = variant.configObjects
        processResources.conventionMapping.manifestFile = { processManifestTask.processedManifest }
        // TODO: unify with generateBuilderConfig, and compileAidl somehow?
        processResources.conventionMapping.sourceOutputDir = {
            project.file("$project.buildDir/source/$variant.dirName")
        }
        processResources.conventionMapping.packageFile = {
            project.file(
                    "$project.buildDir/libs/${project.archivesBaseName}-${variant.baseName}.ap_")
        }
        if (variant.runProguard) {
            processResources.conventionMapping.proguardFile = {
                project.file("$project.buildDir/proguard/${variant.dirName}/rules.txt")
            }
        }

        if (crunchTask != null) {
            processResources.dependsOn crunchTask
            processResources.conventionMapping.crunchDir = { crunchTask.outputDir }
            processResources.conventionMapping.resDirectories = { crunchTask.resDirectories }
        } else {
            processResources.conventionMapping.resDirectories = { variant.config.resourceInputs }
        }

        processResources.aaptOptions = extension.aaptOptions
        return processResources
    }

    protected CompileAidlTask createAidlTask(ApplicationVariant variant) {

        VariantConfiguration config = variant.config

        def compileTask = project.tasks.add("compile${variant.name}Aidl", CompileAidlTask)
        compileTask.plugin = this
        compileTask.variant = variant
        compileTask.configObjects = variant.configObjects

        List<Object> sourceList = new ArrayList<Object>();
        sourceList.add(config.defaultSourceSet.aidlSource)
        if (config.getType() != VariantConfiguration.Type.TEST) {
            sourceList.add(config.buildTypeSourceSet.aidlSource)
        }
        if (config.hasFlavors()) {
            for (com.android.builder.SourceSet flavorSourceSet : config.flavorSourceSets) {
                sourceList.add(flavorSourceSet.aidlSource)
            }
        }

        compileTask.sourceDirs = sourceList
        compileTask.importDirs = variant.config.aidlImports

        compileTask.conventionMapping.sourceOutputDir = {
            project.file("$project.buildDir/source/$variant.dirName")
        }

        return compileTask
    }

    protected void createCompileTask(ApplicationVariant variant,
                                     ApplicationVariant testedVariant,
                                     ProcessResourcesTask processResources,
                                     GenerateBuildConfigTask generateBuildConfigTask,
                                     CompileAidlTask aidlTask) {
        def compileTask = project.tasks.add("compile${variant.name}", Compile)
        compileTask.dependsOn processResources, generateBuildConfigTask, aidlTask

        VariantConfiguration config = variant.config

        List<Object> sourceList = new ArrayList<Object>();
        sourceList.add(((AndroidSourceSet) config.defaultSourceSet).sourceSet.java)
        sourceList.add({ processResources.sourceOutputDir })
        if (config.getType() != VariantConfiguration.Type.TEST) {
            sourceList.add(((AndroidSourceSet) config.buildTypeSourceSet).sourceSet.java)
        }
        if (config.hasFlavors()) {
            for (com.android.builder.SourceSet flavorSourceSet : config.flavorSourceSets) {
                sourceList.add(((AndroidSourceSet) flavorSourceSet).sourceSet.java)
            }
        }
        compileTask.source = sourceList.toArray()

        // if the test is for a full app, the tested runtimeClasspath is added to the classpath for
        // compilation only, not for packaging
        variant.packagedClasspath = project.files({config.compileClasspath})
        if (testedVariant != null &&
                testedVariant.config.type != VariantConfiguration.Type.LIBRARY) {
            compileTask.classpath = variant.packagedClasspath + testedVariant.runtimeClasspath
        } else {
            compileTask.classpath = variant.packagedClasspath
        }
        // TODO - dependency information for the compile classpath is being lost.
        // Add a temporary approximation
        compileTask.dependsOn project.configurations.compile.buildDependencies

        compileTask.conventionMapping.destinationDir = {
            project.file("$project.buildDir/classes/$variant.dirName")
        }
        compileTask.doFirst {
            compileTask.options.bootClasspath = getRuntimeJars(variant)
        }

        // Wire up the outputs
        variant.runtimeClasspath = compileTask.outputs.files + compileTask.classpath
        variant.resourcePackage = project.files({processResources.packageFile}) {
            builtBy processResources
        }
        variant.compileTask = compileTask
    }

    protected void createTestTasks(TestAppVariant variant, ProductionAppVariant testedVariant) {
        // Add a task to process the manifest
        def processManifestTask = createProcessManifestTask(variant, "manifests")

        // Add a task to crunch resource files
        def crunchTask = createCrunchResTask(variant)

        if (testedVariant.config.type == VariantConfiguration.Type.LIBRARY) {
            // in this case the tested library must be fully built before test can be built!
            if (testedVariant.assembleTask != null) {
                processManifestTask.dependsOn testedVariant.assembleTask
                crunchTask.dependsOn testedVariant.assembleTask
            }
        }

        // Add a task to create the BuildConfig class
        def generateBuildConfigTask = createBuildConfigTask(variant, processManifestTask)

        // Add a task to generate resource source files
        def processResources = createProcessResTask(variant, processManifestTask, crunchTask)

        def compileAidl = createAidlTask(variant)

        // Add a task to compile the test application
        createCompileTask(variant, testedVariant, processResources, generateBuildConfigTask,
                compileAidl)

        addPackageTasks(variant, null)

        if (assembleTest != null) {
            assembleTest.dependsOn variant.assembleTask
        }

        // create the check task for this test
        def checkTask = project.tasks.add("check${testedVariant.name}", DefaultTask)
        checkTask.description = "Installs and runs the checks for Build ${testedVariant.name}."
        checkTask.group = JavaBasePlugin.VERIFICATION_GROUP

        checkTask.dependsOn testedVariant.assembleTask, variant.assembleTask
        project.tasks.check.dependsOn checkTask

        // now run the test.
        def runTestsTask = project.tasks.add("run${testedVariant.name}Tests", RunTestsTask)
        runTestsTask.description = "Runs the checks for Build ${testedVariant.name}. Must be installed on device."
        runTestsTask.group = JavaBasePlugin.VERIFICATION_GROUP
        runTestsTask.sdkDir = sdkDir
        runTestsTask.variant = variant
        checkTask.doLast { runTestsTask }

        // TODO: don't rely on dependsOn which isn't reliable for execution order.
        if (testedVariant.config.type == VariantConfiguration.Type.DEFAULT) {
            checkTask.dependsOn testedVariant.installTask, variant.installTask, runTestsTask, testedVariant.uninstallTask, variant.uninstallTask
        } else {
            checkTask.dependsOn variant.installTask, runTestsTask, variant.uninstallTask
        }
    }

    /**
     * Creates the packaging tasks for the given Variant.
     * @param variant the variant.
     * @param assembleTask an optional assembleTask to be used. If null a new one is created. The
     *                assembleTask is always set in the Variant.
     */
    protected void addPackageTasks(ApplicationVariant variant, Task assembleTask) {
        // Add a dex task
        def dexTaskName = "dex${variant.name}"
        def dexTask = project.tasks.add(dexTaskName, DexTask)
        dexTask.dependsOn variant.compileTask
        dexTask.plugin = this
        dexTask.variant = variant
        dexTask.conventionMapping.libraries = { variant.packagedClasspath }
        dexTask.conventionMapping.sourceFiles = { variant.compileTask.outputs.files }
        dexTask.conventionMapping.outputFile = {
            project.file(
                    "${project.buildDir}/libs/${project.archivesBaseName}-${variant.baseName}.dex")
        }
        dexTask.dexOptions = extension.dexOptions

        // Add a task to generate application package
        def packageApp = project.tasks.add("package${variant.name}", PackageApplicationTask)
        packageApp.dependsOn variant.resourcePackage, dexTask
        packageApp.plugin = this
        packageApp.variant = variant
        packageApp.configObjects = variant.configObjects

        def signedApk = variant.isSigned()

        def apkName = signedApk ?
            "${project.archivesBaseName}-${variant.baseName}-unaligned.apk" :
            "${project.archivesBaseName}-${variant.baseName}-unsigned.apk"

        packageApp.conventionMapping.outputFile = {
            project.file("$project.buildDir/apk/${apkName}")
        }
        packageApp.conventionMapping.resourceFile = { variant.resourcePackage.singleFile }
        packageApp.conventionMapping.dexFile = { dexTask.outputFile }

        def appTask = packageApp

        if (signedApk) {
            if (variant.zipAlign) {
                // Add a task to zip align application package
                def alignApp = project.tasks.add("zipalign${variant.name}", ZipAlignTask)
                alignApp.dependsOn packageApp
                alignApp.conventionMapping.inputFile = { packageApp.outputFile }
                alignApp.conventionMapping.outputFile = {
                    project.file(
                            "$project.buildDir/apk/${project.archivesBaseName}-${variant.baseName}.apk")
                }
                alignApp.sdkDir = sdkDir

                appTask = alignApp
            }

            // Add a task to install the application package
            def installTask = project.tasks.add("install${variant.name}", InstallTask)
            installTask.description = "Installs the " + variant.description
            installTask.group = INSTALL_GROUP
            installTask.dependsOn appTask
            installTask.conventionMapping.packageFile = { appTask.outputFile }
            installTask.sdkDir = sdkDir

            variant.installTask = installTask
        }

        // Add an assemble task
        if (assembleTask == null) {
            assembleTask = project.tasks.add("assemble${variant.name}")
            assembleTask.description = "Assembles the " + variant.description
            assembleTask.group = BasePlugin.BUILD_GROUP
        }
        assembleTask.dependsOn appTask
        variant.assembleTask = assembleTask

        // add an uninstall task
        def uninstallTask = project.tasks.add("uninstall${variant.name}", UninstallTask)
        uninstallTask.description = "Uninstalls the " + variant.description
        uninstallTask.group = INSTALL_GROUP
        uninstallTask.variant = variant
        uninstallTask.sdkDir = sdkDir

        variant.uninstallTask = uninstallTask
        uninstallAll.dependsOn uninstallTask
    }

    PrepareDependenciesTask createPrepareDependenciesTask(ProductionAppVariant variant) {
        // TODO - include variant specific dependencies too
        def compileClasspath = project.configurations.compile

        // TODO - shouldn't need to do this - fix this in Gradle
        ensureConfigured(compileClasspath)

        def prepareDependenciesTask = project.tasks.add("prepare${variant.name}Dependencies",
                PrepareDependenciesTask)

        // TODO - should be able to infer this
        prepareDependenciesTask.dependsOn compileClasspath

        // TODO - defer downloading until required
        // TODO - build the library dependency graph
        List<AndroidDependency> bundles = []
        List<JarDependency> jars = []
        Map<ModuleVersionIdentifier, Object> modules = [:]
        compileClasspath.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency dependency ->
            addDependency(dependency, prepareDependenciesTask, bundles, jars, modules)
        }

        variant.config.androidDependencies = bundles
        variant.config.jarDependencies = jars

        // TODO - filter bundles out of source set classpath

        return prepareDependenciesTask
    }

    def ensureConfigured(Configuration config) {
        config.allDependencies.withType(ProjectDependency).each { dep ->
            project.evaluationDependsOn(dep.dependencyProject.path)
            ensureConfigured(dep.projectConfiguration)
        }
    }

    def addDependency(ResolvedDependency dependency, PrepareDependenciesTask prepareDependenciesTask, Collection<AndroidDependency> bundles, Collection<JarDependency> jars, Map<ModuleVersionIdentifier, Object> modules) {
        def id = dependency.module.id
        List<AndroidDependency> bundlesForThisModule = modules[id]
        if (bundlesForThisModule == null) {
            bundlesForThisModule = []
            modules[id] = bundlesForThisModule

            def nestedBundles = []
            dependency.children.each { ResolvedDependency child ->
                addDependency(child, prepareDependenciesTask, nestedBundles, jars, modules)
            }

            dependency.moduleArtifacts.each { artifact ->
                if (artifact.type == 'alb') {
                    def explodedDir = project.file("$project.buildDir/exploded-bundles/$artifact.file.name")
                    bundlesForThisModule << new AndroidDependencyImpl(explodedDir, nestedBundles)
                    prepareDependenciesTask.add(artifact.file, explodedDir)
                } else {
                    // TODO - need the correct values for the boolean flags
                    jars << new JarDependency(artifact.file.absolutePath, true, true, true)
                }
            }

            if (bundlesForThisModule.empty && !nestedBundles.empty) {
                throw new GradleException("Module version $id depends on libraries but is not a library itself")
            }
        }

        bundles.addAll(bundlesForThisModule)
    }

}
