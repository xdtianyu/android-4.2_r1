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
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.builder.compiler.AidlProcessor;
import com.android.builder.compiler.SourceGenerator;
import com.android.builder.packaging.DuplicateFileException;
import com.android.builder.packaging.JavaResourceProcessor;
import com.android.builder.packaging.Packager;
import com.android.builder.packaging.PackagerException;
import com.android.builder.packaging.SealedPackageException;
import com.android.builder.signing.DebugKeyHelper;
import com.android.builder.signing.KeystoreHelper;
import com.android.builder.signing.KeytoolException;
import com.android.builder.signing.SigningInfo;
import com.android.manifmerger.ManifestMerger;
import com.android.manifmerger.MergerLog;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;
import com.android.utils.ILogger;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * This is the main builder class. It is given all the data to process the build (such as
 * {@link ProductFlavor}s, {@link BuildType} and dependencies) and use them when doing specific
 * build steps.
 *
 * To use:
 * create a builder with {@link #AndroidBuilder(SdkParser, ILogger, boolean)},
 * configure compile target with {@link #setTarget(String)}
 * configure build variant with {@link #setVariantConfig(VariantConfiguration)}
 *
 * then build steps can be done with
 * {@link #generateBuildConfig(String, java.util.List)}
 * {@link #processManifest(String)}
 * {@link #processResources(String, String, String, String, String, AaptOptions)}
 * {@link #convertBytecode(java.util.List, java.util.List, String, DexOptions)}
 * {@link #packageApk(String, String, String, String)}
 *
 * Java compilation is not handled but the builder provides the runtime classpath with
 * {@link #getRuntimeClasspath()}.
 */
public class AndroidBuilder {

    private final SdkParser mSdkParser;
    private final ILogger mLogger;
    private final CommandLineRunner mCmdLineRunner;
    private final boolean mVerboseExec;

    private IAndroidTarget mTarget;

    // config
    private VariantConfiguration mVariant;

    /**
     * Creates an AndroidBuilder
     * <p/>
     * This receives an {@link SdkParser} to provide the build with information about the SDK, as
     * well as an {@link ILogger} to display output.
     * <p/>
     * <var>verboseExec</var> is needed on top of the ILogger due to remote exec tools not being
     * able to output info and verbose messages separately.
     *
     * @param sdkParser
     * @param logger
     * @param verboseExec
     */
    public AndroidBuilder(
            @NonNull SdkParser sdkParser,
            @NonNull ILogger logger,
            boolean verboseExec) {
        mSdkParser = checkNotNull(sdkParser);
        mLogger = checkNotNull(logger);
        mVerboseExec = verboseExec;
        mCmdLineRunner = new CommandLineRunner(mLogger);
    }

    @VisibleForTesting
    AndroidBuilder(
            @NonNull SdkParser sdkParser,
            @NonNull CommandLineRunner cmdLineRunner,
            @NonNull ILogger logger,
            boolean verboseExec) {
        mSdkParser = checkNotNull(sdkParser);
        mCmdLineRunner = checkNotNull(cmdLineRunner);
        mLogger = checkNotNull(logger);
        mVerboseExec = verboseExec;
    }

    /**
     * Sets the compilation target hash string.
     *
     * @param target the compilation target
     *
     * @see IAndroidTarget#hashString()
     */
    public void setTarget(@NonNull String target) {
        checkNotNull(target, "target cannot be null.");

        mTarget = mSdkParser.resolveTarget(target, mLogger);

        if (mTarget == null) {
            throw new RuntimeException("Unknown target: " + target);
        }
    }

    /**
     * Sets the build variant configuration
     *
     * @param variant the configuration of the variant
     *
     */
    public void setVariantConfig(@NonNull VariantConfiguration variant) {
        mVariant = checkNotNull(variant, "variant cannot be null.");
    }

    /**
     * Returns the runtime classpath to be used during compilation.
     */
    public List<String> getRuntimeClasspath() {
        checkState(mTarget != null, "Target not set.");

        List<String> classpath = Lists.newArrayList();

        classpath.add(mTarget.getPath(IAndroidTarget.ANDROID_JAR));

        // add optional libraries if any
        IOptionalLibrary[] libs = mTarget.getOptionalLibraries();
        if (libs != null) {
            for (IOptionalLibrary lib : libs) {
                classpath.add(lib.getJarPath());
            }
        }

        // add annotations.jar if needed.
        if (mTarget.getVersion().getApiLevel() <= 15) {
            classpath.add(mSdkParser.getAnnotationsJar());
        }

        return classpath;
    }

    /**
     * Generate the BuildConfig class for the project.
     * @param sourceOutputDir directory where to put this. This is the source folder, not the
     *                        package folder.
     * @param additionalLines additional lines to put in the class. These must be valid Java lines.
     * @throws IOException
     */
    public void generateBuildConfig(
            @NonNull String sourceOutputDir,
            @Nullable List<String> additionalLines) throws IOException {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");

        String packageName;
        if (mVariant.getType() == VariantConfiguration.Type.TEST) {
            packageName = mVariant.getPackageName();
        } else {
            packageName = mVariant.getPackageFromManifest();
        }

        BuildConfigGenerator generator = new BuildConfigGenerator(
                sourceOutputDir, packageName, mVariant.getBuildType().isDebuggable());
        generator.generate(additionalLines);
    }

    /**
     * Pre-process resources. This crunches images and process 9-patches before they can
     * be packaged.
     * This is incremental.
     *
     * Call this directly if you don't care about checking whether the inputs have changed.
     * Otherwise, get the input first to check with {@link VariantConfiguration#getResourceInputs()}
     * and then call (or not), {@link #preprocessResources(String, java.util.List)}.
     *
     * @param resOutputDir where the processed resources are stored.
     * @throws IOException
     * @throws InterruptedException
     */
    public void preprocessResources(@NonNull String resOutputDir)
            throws IOException, InterruptedException {
        checkState(mVariant != null, "No Variant Configuration has been set.");

        List<File> inputs = mVariant.getResourceInputs();
        preprocessResources(resOutputDir, inputs);
    }
    /**
     * Pre-process resources. This crunches images and process 9-patches before they can
     * be packaged.
     * This is incremental.
     *
     * @param resOutputDir where the processed resources are stored.
     * @param inputs the input res folders
     * @throws IOException
     * @throws InterruptedException
     */
    public void preprocessResources(@NonNull String resOutputDir, List<File> inputs)
            throws IOException, InterruptedException {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");
        checkNotNull(resOutputDir, "resOutputDir cannot be null.");

        if (inputs == null || inputs.isEmpty()) {
            return;
        }

        // launch aapt: create the command line
        ArrayList<String> command = Lists.newArrayList();

        @SuppressWarnings("deprecation")
        String aaptPath = mTarget.getPath(IAndroidTarget.AAPT);

        command.add(aaptPath);
        command.add("crunch");

        if (mVerboseExec) {
            command.add("-v");
        }

        boolean runCommand = false;
        for (File input : inputs) {
            if (input.isDirectory()) {
                command.add("-S");
                command.add(input.getAbsolutePath());
                runCommand = true;
            }
        }

        if (!runCommand) {
            return;
        }

        command.add("-C");
        command.add(resOutputDir);

        mLogger.info("crunch command: %s", command.toString());

        mCmdLineRunner.runCmdLine(command);
    }

    /**
     * Merges all the manifest from the BuildType and ProductFlavor(s) into a single manifest.
     *
     * TODO: figure out the order. Libraries first or buildtype/flavors first?
     *
     * @param outManifestLocation the output location for the merged manifest
     */
    public void processManifest(@NonNull String outManifestLocation) {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");
        checkNotNull(outManifestLocation, "outManifestLocation cannot be null.");

        if (mVariant.getType() == VariantConfiguration.Type.TEST) {
            VariantConfiguration testedConfig = mVariant.getTestedConfig();
            if (testedConfig.getType() == VariantConfiguration.Type.LIBRARY) {
                try {
                    // create the test manifest, merge the libraries in it
                    File generatedTestManifest = File.createTempFile("manifestMerge", ".xml");

                    generateTestManifest(generatedTestManifest.getAbsolutePath());

                    mergeLibraryManifests(
                            generatedTestManifest,
                            mVariant.getDirectLibraries(),
                            new File(outManifestLocation));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                generateTestManifest(outManifestLocation);
            }
        } else {
            mergeManifest(mVariant, outManifestLocation);
        }
    }

    private void generateTestManifest(String outManifestLocation) {
        TestManifestGenerator generator = new TestManifestGenerator(outManifestLocation,
                mVariant.getPackageName(),
                mVariant.getTestedPackageName(),
                mVariant.getInstrumentationRunner());

        try {
            generator.generate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void mergeManifest(VariantConfiguration config, String outManifestLocation) {
        try {
            // gather the app manifests: main + buildType and Flavors.
            File mainManifest = config.getDefaultSourceSet().getAndroidManifest();

            List<File> subManifests = Lists.newArrayList();

            File typeLocation = config.getBuildTypeSourceSet().getAndroidManifest();
            if (typeLocation != null && typeLocation.isFile()) {
                subManifests.add(typeLocation);
            }

            for (SourceSet sourceSet : config.getFlavorSourceSets()) {
                File f = sourceSet.getAndroidManifest();
                if (f != null && f.isFile()) {
                    subManifests.add(f);
                }
            }

            // if no manifest to merge, just copy to location
            if (subManifests.isEmpty() && !config.hasLibraries()) {
                Files.copy(mainManifest, new File(outManifestLocation));
            } else {
                File outManifest = new File(outManifestLocation);

                // first merge the app manifest.
                if (!subManifests.isEmpty()) {
                    File mainManifestOut = outManifest;

                    // if there is also libraries, put this in a temp file.
                    if (config.hasLibraries()) {
                        // TODO find better way of storing intermediary file?
                        mainManifestOut = File.createTempFile("manifestMerge", ".xml");
                        mainManifestOut.deleteOnExit();
                    }

                    ManifestMerger merger = new ManifestMerger(MergerLog.wrapSdkLog(mLogger));
                    if (merger.process(
                            mainManifestOut,
                            mainManifest,
                            subManifests.toArray(new File[subManifests.size()])) == false) {
                        throw new RuntimeException();
                    }

                    // now the main manifest is the newly merged one
                    mainManifest = mainManifestOut;
                }

                if (config.hasLibraries()) {
                    // recursively merge all manifests starting with the leaves and up toward the
                    // root (the app)
                    mergeLibraryManifests(mainManifest, config.getDirectLibraries(),
                            new File(outManifestLocation));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Merges library manifests into a main manifest.
     * @param mainManifest the main manifest
     * @param directLibraries the libraries to merge
     * @param outManifest the output file
     * @throws IOException
     */
    private void mergeLibraryManifests(
            File mainManifest,
            Iterable<AndroidDependency> directLibraries,
            File outManifest) throws IOException {

        List<File> manifests = Lists.newArrayList();
        for (AndroidDependency library : directLibraries) {
            List<AndroidDependency> subLibraries = library.getDependencies();
            if (subLibraries == null || subLibraries.size() == 0) {
                manifests.add(library.getManifest());
            } else {
                File mergeLibManifest = File.createTempFile("manifestMerge", ".xml");
                mergeLibManifest.deleteOnExit();

                mergeLibraryManifests(
                        library.getManifest(), subLibraries, mergeLibManifest);

                manifests.add(mergeLibManifest);
            }
        }

        ManifestMerger merger = new ManifestMerger(MergerLog.wrapSdkLog(mLogger));
        if (merger.process(
                outManifest,
                mainManifest,
                manifests.toArray(new File[manifests.size()])) == false) {
            throw new RuntimeException();
        }
    }

    /**
     *
     * Process the resources and generate R.java and/or the packaged resources.
     *
     * Call this directly if you don't care about checking whether the inputs have changed.
     * Otherwise, get the input first to check with {@link VariantConfiguration#getResourceInputs()}
     * and then call (or not),
     * {@link #processResources(String, String, java.util.List, String, String, String, AaptOptions)}.

     * @param manifestFile the location of the manifest file
     * @param preprocessResDir the pre-processed folder
     * @param sourceOutputDir optional source folder to generate R.java
     * @param resPackageOutput optional filepath for packaged resources
     * @param proguardOutput optional filepath for proguard file to generate
     * @param options the {@link AaptOptions}
     * @throws IOException
     * @throws InterruptedException
     */
    public void processResources(
            @NonNull String manifestFile,
            @Nullable String preprocessResDir,
            @Nullable String sourceOutputDir,
            @Nullable String resPackageOutput,
            @Nullable String proguardOutput,
            @NonNull AaptOptions options) throws IOException, InterruptedException {
        List<File> inputs = mVariant.getResourceInputs();
        processResources(manifestFile, preprocessResDir, inputs, sourceOutputDir,
                resPackageOutput, proguardOutput, options);
    }

    /**
     * Process the resources and generate R.java and/or the packaged resources.
     *
     *
     * @param manifestFile the location of the manifest file
     * @param preprocessResDir the pre-processed folder
     * @param resInputs the res folder inputs
     * @param sourceOutputDir optional source folder to generate R.java
     * @param resPackageOutput optional filepath for packaged resources
     * @param proguardOutput optional filepath for proguard file to generate
     * @param options the {@link AaptOptions}
     * @throws IOException
     * @throws InterruptedException
     */
    public void processResources(
            @NonNull String manifestFile,
            @Nullable String preprocessResDir,
            @NonNull List<File> resInputs,
            @Nullable String sourceOutputDir,
            @Nullable String resPackageOutput,
            @Nullable String proguardOutput,
            @NonNull AaptOptions options) throws IOException, InterruptedException {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");
        checkNotNull(manifestFile, "manifestFile cannot be null.");
        checkNotNull(resInputs, "resInputs cannot be null.");
        checkNotNull(options, "options cannot be null.");
        // if both output types are empty, then there's nothing to do and this is an error
        checkArgument(sourceOutputDir != null || resPackageOutput != null,
                "No output provided for aapt task");

        // launch aapt: create the command line
        ArrayList<String> command = Lists.newArrayList();

        @SuppressWarnings("deprecation")
        String aaptPath = mTarget.getPath(IAndroidTarget.AAPT);

        command.add(aaptPath);
        command.add("package");

        if (mVerboseExec) {
            command.add("-v");
        }

        command.add("-f");
        command.add("--no-crunch");

        // inputs
        command.add("-I");
        command.add(mTarget.getPath(IAndroidTarget.ANDROID_JAR));

        command.add("-M");
        command.add(manifestFile);

        boolean useOverlay =  false;
        if (preprocessResDir != null) {
            File preprocessResFile = new File(preprocessResDir);
            if (preprocessResFile.isDirectory()) {
                command.add("-S");
                command.add(preprocessResDir);
            }
        }

        for (File resFolder : resInputs) {
            if (resFolder.isDirectory()) {
                command.add("-S");
                command.add(resFolder.getAbsolutePath());
            }
        }

        command.add("--auto-add-overlay");


        // TODO support 2+ assets folders.
//        if (typeAssetsLocation != null) {
//            command.add("-A");
//            command.add(typeAssetsLocation);
//        }
//
//        if (flavorAssetsLocation != null) {
//            command.add("-A");
//            command.add(flavorAssetsLocation);
//        }

        File mainAssetsLocation = mVariant.getDefaultSourceSet().getAndroidAssets();
        if (mainAssetsLocation != null && mainAssetsLocation.isDirectory()) {
            command.add("-A");
            command.add(mainAssetsLocation.getAbsolutePath());
        }

        // outputs

        if (sourceOutputDir != null) {
            command.add("-m");
            command.add("-J");
            command.add(sourceOutputDir);
        }

        if (mVariant.getType() != VariantConfiguration.Type.LIBRARY && resPackageOutput != null) {
            command.add("-F");
            command.add(resPackageOutput);

            if (proguardOutput != null) {
                command.add("-G");
                command.add(proguardOutput);
            }
        }

        // options controlled by build variants

        if (mVariant.getBuildType().isDebuggable()) {
            command.add("--debug-mode");
        }

        if (mVariant.getType() == VariantConfiguration.Type.DEFAULT) {
            String packageOverride = mVariant.getPackageOverride();
            if (packageOverride != null) {
                command.add("--rename-manifest-package");
                command.add(packageOverride);
                mLogger.verbose("Inserting package '%s' in AndroidManifest.xml", packageOverride);
            }

            boolean forceErrorOnReplace = false;

            ProductFlavor mergedFlavor = mVariant.getMergedFlavor();

            int versionCode = mergedFlavor.getVersionCode();
            if (versionCode != -1) {
                command.add("--version-code");
                command.add(Integer.toString(versionCode));
                mLogger.verbose("Inserting versionCode '%d' in AndroidManifest.xml", versionCode);
                forceErrorOnReplace = true;
            }

            String versionName = mergedFlavor.getVersionName();
            if (versionName != null) {
                command.add("--version-name");
                command.add(versionName);
                mLogger.verbose("Inserting versionName '%s' in AndroidManifest.xml", versionName);
                forceErrorOnReplace = true;
            }

            int minSdkVersion = mergedFlavor.getMinSdkVersion();
            if (minSdkVersion != -1) {
                command.add("--min-sdk-version");
                command.add(Integer.toString(minSdkVersion));
                mLogger.verbose("Inserting minSdkVersion '%d' in AndroidManifest.xml",
                        minSdkVersion);
                forceErrorOnReplace = true;
            }

            int targetSdkVersion = mergedFlavor.getTargetSdkVersion();
            if (targetSdkVersion != -1) {
                command.add("--target-sdk-version");
                command.add(Integer.toString(targetSdkVersion));
                mLogger.verbose("Inserting targetSdkVersion '%d' in AndroidManifest.xml",
                        targetSdkVersion);
                forceErrorOnReplace = true;
            }

            if (forceErrorOnReplace) {
                // TODO: force aapt to fail if replace of versionCode/Name or min/targetSdkVersion fails
                // Need to add the options to aapt first.
            }
        }

        // library specific options
        if (mVariant.getType() == VariantConfiguration.Type.LIBRARY) {
            command.add("--non-constant-id");
        } else {
            // only create the R class from library dependencies if this is not a library itself.
            String extraPackages = mVariant.getLibraryPackages();
            if (extraPackages != null) {
                command.add("--extra-packages");
                command.add(extraPackages);
            }
        }

        // AAPT options
        String ignoreAssets = options.getIgnoreAssets();
        if (ignoreAssets != null) {
            command.add("---ignore-assets");
            command.add(ignoreAssets);
        }

        List<String> noCompressList = options.getNoCompress();
        if (noCompressList != null) {
            for (String noCompress : noCompressList) {
                command.add("-0");
                command.add(noCompress);
            }
        }

        mLogger.info("aapt command: %s", command.toString());

        mCmdLineRunner.runCmdLine(command);
    }

    /**
     * compiles all AIDL files.
     *
     * Call this directly if you don't care about checking whether the imports have changed.
     * Otherwise, get the imports first to check with
     * {@link com.android.builder.VariantConfiguration#getAidlImports()}
     * and then call (or not), {@link #compileAidl(java.util.List, java.io.File, java.util.List)}.
     *
     * @param sourceFolders
     * @param sourceOutputDir
     * @throws IOException
     * @throws InterruptedException
     */
    public void compileAidl(@NonNull List<File> sourceFolders,
                            @NonNull File sourceOutputDir)
            throws IOException, InterruptedException {
        checkState(mVariant != null, "No Variant Configuration has been set.");

        compileAidl(sourceFolders, sourceOutputDir, mVariant.getAidlImports());
    }

    public void compileAidl(@NonNull List<File> sourceFolders,
                            @NonNull File sourceOutputDir,
                            @NonNull List<File> importFolders)
            throws IOException, InterruptedException {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");
        checkNotNull(sourceFolders, "sourceFolders cannot be null.");
        checkNotNull(sourceOutputDir, "sourceOutputDir cannot be null.");
        checkNotNull(importFolders, "importFolders cannot be null.");

        SourceGenerator compiler = new SourceGenerator(mLogger);

        @SuppressWarnings("deprecation")
        String aidlPath = mTarget.getPath(IAndroidTarget.AIDL);

        AidlProcessor processor = new AidlProcessor(
                aidlPath,
                mTarget.getPath(IAndroidTarget.ANDROID_AIDL),
                importFolders,
                mCmdLineRunner);

        compiler.processFiles(processor, sourceFolders, sourceOutputDir);
    }

    public void convertBytecode(
            @NonNull List<String> classesLocation,
            @NonNull List<String> libraries,
            @NonNull String outDexFile,
            @NonNull DexOptions dexOptions) throws IOException, InterruptedException {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");
        checkNotNull(classesLocation, "classesLocation cannot be null.");
        checkNotNull(libraries, "libraries cannot be null.");
        checkNotNull(outDexFile, "outDexFile cannot be null.");
        checkNotNull(dexOptions, "dexOptions cannot be null.");

        // launch dx: create the command line
        ArrayList<String> command = Lists.newArrayList();

        @SuppressWarnings("deprecation")
        String dxPath = mTarget.getPath(IAndroidTarget.DX);
        command.add(dxPath);

        command.add("--dex");

        if (mVerboseExec) {
            command.add("--verbose");
        }

        command.add("--output");
        command.add(outDexFile);

        // TODO: handle dependencies
        // TODO: handle dex options

        mLogger.verbose("Dex class inputs: " + classesLocation);

        command.addAll(classesLocation);

        mLogger.verbose("Dex library inputs: " + libraries);

        command.addAll(libraries);

        mCmdLineRunner.runCmdLine(command);
    }

    /**
     * Packages the apk.
     * @param androidResPkgLocation
     * @param classesDexLocation
     * @param jniLibsLocation
     * @param outApkLocation
     */
    public void packageApk(
            @NonNull String androidResPkgLocation,
            @NonNull String classesDexLocation,
            @Nullable String jniLibsLocation,
            @NonNull String outApkLocation) throws DuplicateFileException {
        checkState(mVariant != null, "No Variant Configuration has been set.");
        checkState(mTarget != null, "Target not set.");
        checkNotNull(androidResPkgLocation, "androidResPkgLocation cannot be null.");
        checkNotNull(classesDexLocation, "classesDexLocation cannot be null.");
        checkNotNull(outApkLocation, "outApkLocation cannot be null.");

        BuildType buildType = mVariant.getBuildType();

        SigningInfo signingInfo = null;
        try {
            if (buildType.isDebugSigned()) {
                String storeLocation = DebugKeyHelper.defaultDebugKeyStoreLocation();
                File storeFile = new File(storeLocation);
                if (storeFile.isDirectory()) {
                    throw new RuntimeException(
                            String.format("A folder is in the way of the debug keystore: %s",
                                    storeLocation));
                } else if (storeFile.exists() == false) {
                    if (DebugKeyHelper.createNewStore(
                            storeLocation, null /*storeType*/, mLogger) == false) {
                        throw new RuntimeException();
                    }
                }

                // load the key
                signingInfo = DebugKeyHelper.getDebugKey(storeLocation, null /*storeStype*/);
            } else if (mVariant.getMergedFlavor().isSigningReady()) {
                ProductFlavor flavor = mVariant.getMergedFlavor();
                signingInfo = KeystoreHelper.getSigningInfo(
                        flavor.getSigningStoreLocation(),
                        flavor.getSigningStorePassword(),
                        null, /*storeStype*/
                        flavor.getSigningKeyAlias(),
                        flavor.getSigningKeyPassword());
            }
        } catch (AndroidLocationException e) {
            throw new RuntimeException(e);
        } catch (KeytoolException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            // this shouldn't happen as we have checked ahead of calling getDebugKey.
            throw new RuntimeException(e);
        }

        try {
            Packager packager = new Packager(
                    outApkLocation, androidResPkgLocation, classesDexLocation,
                    signingInfo, mLogger);

            packager.setDebugJniMode(buildType.isDebugJniBuild());

            // figure out conflicts!
            JavaResourceProcessor resProcessor = new JavaResourceProcessor(packager);

            if (mVariant.getBuildTypeSourceSet() != null) {
                Set<File> buildTypeJavaResLocations =
                        mVariant.getBuildTypeSourceSet().getJavaResources();
                for (File buildTypeJavaResLocation : buildTypeJavaResLocations) {
                    if (buildTypeJavaResLocation != null &&
                            buildTypeJavaResLocation.isDirectory()) {
                        resProcessor.addSourceFolder(buildTypeJavaResLocation.getAbsolutePath());
                    }
                }
            }

            for (SourceSet sourceSet : mVariant.getFlavorSourceSets()) {

                Set<File> flavorJavaResLocations = sourceSet.getJavaResources();
                for (File flavorJavaResLocation : flavorJavaResLocations) {
                    if (flavorJavaResLocation != null && flavorJavaResLocation.isDirectory()) {
                        resProcessor.addSourceFolder(flavorJavaResLocation.getAbsolutePath());
                    }
                }
            }

            Set<File> mainJavaResLocations = mVariant.getDefaultSourceSet().getJavaResources();
            for (File mainJavaResLocation : mainJavaResLocations) {
                if (mainJavaResLocation != null && mainJavaResLocation.isDirectory()) {
                    resProcessor.addSourceFolder(mainJavaResLocation.getAbsolutePath());
                }
            }

            // add the resources from the jar files.
            List<JarDependency> jars = mVariant.getJars();
            if (jars != null) {
                for (JarDependency jar : jars) {
                    packager.addResourcesFromJar(new File(jar.getLocation()));
                }
            }

            // add the resources from the libs jar files
            List<AndroidDependency> libs = mVariant.getDirectLibraries();
            addLibJavaResourcesToPackager(packager, libs);

            // also add resources from library projects and jars
            if (jniLibsLocation != null) {
                packager.addNativeLibraries(jniLibsLocation);
            }

            packager.sealApk();
        } catch (PackagerException e) {
            throw new RuntimeException(e);
        } catch (SealedPackageException e) {
            throw new RuntimeException(e);
        }
    }

    private void addLibJavaResourcesToPackager(Packager packager, List<AndroidDependency> libs)
            throws PackagerException, SealedPackageException, DuplicateFileException {
        if (libs != null) {
            for (AndroidDependency lib : libs) {
                packager.addResourcesFromJar(lib.getJarFile());

                // recursively add the dependencies of this library.
                addLibJavaResourcesToPackager(packager, lib.getDependencies());
            }
        }
    }
}
