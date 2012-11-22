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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A Variant configuration.
 */
public class VariantConfiguration {

    private final static ManifestParser sManifestParser = new DefaultManifestParser();

    private final ProductFlavor mDefaultConfig;
    private final SourceSet mDefaultSourceSet;

    private final BuildType mBuildType;
    /** SourceSet for the BuildType. Can be null */
    private final SourceSet mBuildTypeSourceSet;

    private final List<ProductFlavor> mFlavorConfigs = Lists.newArrayList();
    private final List<SourceSet> mFlavorSourceSets = Lists.newArrayList();

    private final Type mType;
    /** Optional tested config in case type is Type#TEST */
    private final VariantConfiguration mTestedConfig;
    /** An optional output that is only valid if the type is Type#LIBRARY so that the test
     * for the library can use the library as if it was a normal dependency. */
    private AndroidDependency mOutput;

    private ProductFlavor mMergedFlavor;

    private List<JarDependency> mJars;

    /** List of direct library dependencies. Each object defines its own dependencies. */
    private final List<AndroidDependency> mDirectLibraries = Lists.newArrayList();

    /** list of all library dependencies in a flat list.
     * The order is based on the order needed to call aapt: earlier libraries override resources
     * of latter ones. */
    private final List<AndroidDependency> mFlatLibraries = Lists.newArrayList();

    public static enum Type {
        DEFAULT, LIBRARY, TEST;
    }

    /**
     * Creates the configuration with the base source set.
     *
     * This creates a config with a {@link Type#DEFAULT} type.
     *
     * @param defaultConfig
     * @param defaultSourceSet
     * @param buildType
     * @param buildTypeSourceSet
     */
    public VariantConfiguration(
            @NonNull ProductFlavor defaultConfig, @NonNull SourceSet defaultSourceSet,
            @NonNull BuildType buildType, @NonNull SourceSet buildTypeSourceSet) {
        this(defaultConfig, defaultSourceSet,
                buildType, buildTypeSourceSet,
                Type.DEFAULT, null /*testedConfig*/);
    }

    /**
     * Creates the configuration with the base source set for a given {@link Type}.
     *
     * @param defaultConfig
     * @param defaultSourceSet
     * @param buildType
     * @param buildTypeSourceSet
     * @param type
     */
    public VariantConfiguration(
            @NonNull ProductFlavor defaultConfig, @NonNull SourceSet defaultSourceSet,
            @NonNull BuildType buildType, @NonNull SourceSet buildTypeSourceSet,
            @NonNull Type type) {
        this(defaultConfig, defaultSourceSet,
                buildType, buildTypeSourceSet,
                type, null /*testedConfig*/);
    }

    /**
     * Creates the configuration with the base source set, and whether it is a library.
     * @param defaultConfig
     * @param defaultSourceSet
     * @param buildType
     * @param buildTypeSourceSet
     * @param type
     * @param testedConfig
     */
    public VariantConfiguration(
            @NonNull ProductFlavor defaultConfig, @NonNull SourceSet defaultSourceSet,
            @NonNull BuildType buildType, SourceSet buildTypeSourceSet,
            @NonNull Type type, @Nullable VariantConfiguration testedConfig) {
        mDefaultConfig = checkNotNull(defaultConfig);
        mDefaultSourceSet = checkNotNull(defaultSourceSet);
        mBuildType = checkNotNull(buildType);
        mBuildTypeSourceSet = buildTypeSourceSet;
        mType = checkNotNull(type);
        mTestedConfig = testedConfig;
        checkState(mType != Type.TEST || mTestedConfig != null);

        mMergedFlavor = mDefaultConfig;

        if (testedConfig != null &&
                testedConfig.mType == Type.LIBRARY &&
                testedConfig.mOutput != null) {
            mDirectLibraries.add(testedConfig.mOutput);
        }

        validate();
    }

    /**
     * Add a new configured ProductFlavor.
     *
     * If multiple flavors are added, the priority follows the order they are added when it
     * comes to resolving Android resources overlays (ie earlier added flavors supersedes
     * latter added ones).
     *
     * @param sourceSet the configured product flavor
     */
    public void addProductFlavor(@NonNull ProductFlavor productFlavor,
                                 @NonNull SourceSet sourceSet) {
        mFlavorConfigs.add(productFlavor);
        mFlavorSourceSets.add(sourceSet);
        mMergedFlavor = productFlavor.mergeOver(mMergedFlavor);
    }

    public void setJarDependencies(List<JarDependency> jars) {
        mJars = jars;
    }

    public List<JarDependency> getJars() {
        return mJars;
    }

    /**
     * Set the Library Project dependencies.
     * @param directLibraries list of direct dependencies. Each library object should contain
     *            its own dependencies.
     */
    public void setAndroidDependencies(@NonNull List<AndroidDependency> directLibraries) {
        if (directLibraries != null) {
            mDirectLibraries.addAll(directLibraries);
        }

        resolveIndirectLibraryDependencies(mDirectLibraries, mFlatLibraries);
    }

    public String getLibraryPackages() {
        if (mFlatLibraries.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (AndroidDependency dep : mFlatLibraries) {
            File manifest = dep.getManifest();
            String packageName = sManifestParser.getPackage(manifest);
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(packageName);
        }

        return sb.toString();
    }


    public void setOutput(AndroidDependency output) {
        mOutput = output;
    }

    public ProductFlavor getDefaultConfig() {
        return mDefaultConfig;
    }

    public SourceSet getDefaultSourceSet() {
        return mDefaultSourceSet;
    }

    public ProductFlavor getMergedFlavor() {
        return mMergedFlavor;
    }

    public BuildType getBuildType() {
        return mBuildType;
    }

    /**
     * The SourceSet for the BuildType. Can be null.
     */
    public SourceSet getBuildTypeSourceSet() {
        return mBuildTypeSourceSet;
    }

    public boolean hasFlavors() {
        return !mFlavorConfigs.isEmpty();
    }

    public Iterable<ProductFlavor> getFlavorConfigs() {
        return mFlavorConfigs;
    }

    public Iterable<SourceSet> getFlavorSourceSets() {
        return mFlavorSourceSets;
    }

    public boolean hasLibraries() {
        return !mDirectLibraries.isEmpty();
    }

    public List<AndroidDependency> getDirectLibraries() {
        return mDirectLibraries;
    }

    public Type getType() {
        return mType;
    }

    VariantConfiguration getTestedConfig() {
        return mTestedConfig;
    }

    /**
     * Resolves a given list of libraries, finds out if they depend on other libraries, and
     * returns a flat list of all the direct and indirect dependencies in the proper order (first
     * is higher priority when calling aapt).
     * @param directDependencies the libraries to resolve
     * @param outFlatDependencies where to store all the libraries.
     */
    @VisibleForTesting
    void resolveIndirectLibraryDependencies(List<AndroidDependency> directDependencies,
                                            List<AndroidDependency> outFlatDependencies) {
        if (directDependencies == null) {
            return;
        }
        // loop in the inverse order to resolve dependencies on the libraries, so that if a library
        // is required by two higher level libraries it can be inserted in the correct place
        for (int i = directDependencies.size() - 1  ; i >= 0 ; i--) {
            AndroidDependency library = directDependencies.get(i);

            // get its libraries
            List<AndroidDependency> dependencies = library.getDependencies();

            // resolve the dependencies for those libraries
            resolveIndirectLibraryDependencies(dependencies, outFlatDependencies);

            // and add the current one (if needed) in front (higher priority)
            if (outFlatDependencies.contains(library) == false) {
                outFlatDependencies.add(0, library);
            }
        }
    }

    /**
     * Returns the package name for this variant. This could be coming from the manifest or
     * could be overridden through the product flavors.
     * @return the package
     */
    public String getPackageName() {
        String packageName;

        if (mType == Type.TEST) {
            packageName = mMergedFlavor.getTestPackageName();
            if (packageName == null) {
                String testedPackage = mTestedConfig.getPackageName();

                packageName = testedPackage + ".test";
            }
        } else {
            packageName = getPackageOverride();
            if (packageName == null) {
                packageName = getPackageFromManifest();
            }
        }

        return packageName;
    }

    public String getTestedPackageName() {
        if (mType == Type.TEST) {
            if (mTestedConfig.mType == Type.LIBRARY) {
                return getPackageName();
            } else {
                return mTestedConfig.getPackageName();
            }
        }

        return null;
    }

    /**
     * Returns the package override values coming from the Product Flavor. If the package is not
     * overridden then this returns null.
     * @return the package override or null
     */
    public String getPackageOverride() {

        String packageName = mMergedFlavor.getPackageName();
        String packageSuffix = mBuildType.getPackageNameSuffix();

        if (packageSuffix != null && packageSuffix.length() > 0) {
            if (packageName == null) {
                packageName = getPackageFromManifest();
            }

            if (packageSuffix.charAt(0) == '.') {
                packageName = packageName + packageSuffix;
            } else {
                packageName = packageName + '.' + packageSuffix;
            }
        }

        return packageName;
    }

    private final static String DEFAULT_TEST_RUNNER = "android.test.InstrumentationTestRunner";

    public String getInstrumentationRunner() {
        String runner = mMergedFlavor.getTestInstrumentationRunner();
        return runner != null ? runner : DEFAULT_TEST_RUNNER;
    }

    /**
     * Reads the package name from the manifest.
     * @return
     */
    public String getPackageFromManifest() {
        File manifestLocation = mDefaultSourceSet.getAndroidManifest();
        return sManifestParser.getPackage(manifestLocation);
    }

    /**
     * Returns a list of object that represents the configuration. This can be used to compare
     * 2 different list of config objects to know whether the build is up to date or not.
     * @return
     */
    public Iterable<Object> getConfigObjects() {
        List<Object> list = Lists.newArrayListWithExpectedSize(mFlavorConfigs.size() + 2);
        list.add(mDefaultConfig);
        list.add(mBuildType);
        list.addAll(mFlavorConfigs);
        // TODO: figure out the deps in here.
//        list.addAll(mFlatLibraries);

        return list;
    }

    public List<File> getManifestInputs() {
        List<File> inputs = Lists.newArrayList();

        File defaultManifest = mDefaultSourceSet.getAndroidManifest();
        // this could not exist in a test project.
        if (defaultManifest != null && defaultManifest.isFile()) {
            inputs.add(defaultManifest);
        }

        if (mBuildTypeSourceSet != null) {
            File typeLocation = mBuildTypeSourceSet.getAndroidManifest();
            if (typeLocation != null && typeLocation.isFile()) {
                inputs.add(typeLocation);
            }
        }

        for (SourceSet sourceSet : mFlavorSourceSets) {
            File f = sourceSet.getAndroidManifest();
            if (f != null && f.isFile()) {
                inputs.add(f);
            }
        }

        List<AndroidDependency> libs = mDirectLibraries;
        for (AndroidDependency lib : libs) {
            File manifest = lib.getManifest();
            if (manifest != null && manifest.isFile()) {
                inputs.add(manifest);
            }
        }

        return inputs;
    }

    /**
     * Returns the dynamic list of resource folders based on the configuration, its dependencies,
     * as well as tested config if applicable (test of a library).
     * @return a list of input resource folders.
     */
    public List<File> getResourceInputs() {
        List<File> inputs = Lists.newArrayList();

        if (mBuildTypeSourceSet != null) {
            File typeResLocation = mBuildTypeSourceSet.getAndroidResources();
            if (typeResLocation != null) {
                inputs.add(typeResLocation);
            }
        }

        for (SourceSet sourceSet : mFlavorSourceSets) {
            File flavorResLocation = sourceSet.getAndroidResources();
            if (flavorResLocation != null) {
                inputs.add(flavorResLocation);
            }
        }

        File mainResLocation = mDefaultSourceSet.getAndroidResources();
        if (mainResLocation != null) {
            inputs.add(mainResLocation);
        }

        for (AndroidDependency dependency : mFlatLibraries) {
            File resFolder = dependency.getResFolder();
            if (resFolder != null) {
                inputs.add(resFolder);
            }
        }

        return inputs;
    }

    /**
     * Returns all the aidl import folder that are outside of the current project.
     *
     * @return
     */
    public List<File> getAidlImports() {
        List<File> list = Lists.newArrayList();

        for (AndroidDependency lib : mFlatLibraries) {
            File aidlLib = lib.getAidlFolder();
            if (aidlLib != null && aidlLib.isDirectory()) {
                list.add(aidlLib);
            }
        }

        return list;
    }

    /**
     * Returns the compile classpath for this config. If the config tests a library, this
     * will include the classpath of the tested config
     * @return
     */
    public Set<File> getCompileClasspath() {
        Set<File> classpath = Sets.newHashSet();

        for (File f : mDefaultSourceSet.getCompileClasspath()) {
            classpath.add(f);
        }

        if (mBuildTypeSourceSet != null) {
            for (File f : mBuildTypeSourceSet.getCompileClasspath()) {
                classpath.add(f);
            }
        }

        for (SourceSet sourceSet : mFlavorSourceSets) {
            for (File f : sourceSet.getCompileClasspath()) {
                classpath.add(f);
            }
        }

        for (AndroidDependency lib : mFlatLibraries) {
            classpath.add(lib.getJarFile());
        }

        if (mType == Type.TEST && mTestedConfig.mType == Type.LIBRARY) {
            // the tested library is added to the main app so we need its compile classpath as well.
            // which starts with its output
            classpath.add(mTestedConfig.mOutput.getJarFile());
            classpath.addAll(mTestedConfig.getCompileClasspath());
        }

        return classpath;
    }

    public List<String> getBuildConfigLines() {
        List<String> fullList = Lists.newArrayList();

        List<String> list = mDefaultConfig.getBuildConfigLines();
        if (!list.isEmpty()) {
            fullList.add("// lines from default config.");
            fullList.addAll(list);
        }

        list = mBuildType.getBuildConfigLines();
        if (!list.isEmpty()) {
            fullList.add("// lines from build type: " + mBuildType.getName());
            fullList.addAll(list);
        }

        for (ProductFlavor flavor : mFlavorConfigs) {
            list = flavor.getBuildConfigLines();
            if (!list.isEmpty()) {
                fullList.add("// lines from product flavor: " + flavor.getName());
                fullList.addAll(list);
            }
        }

        return fullList;
    }

    protected void validate() {
        if (mType != Type.TEST) {
            File manifest = mDefaultSourceSet.getAndroidManifest();
            if (!manifest.isFile()) {
                throw new IllegalArgumentException(
                        "Main Manifest missing from " + manifest.getAbsolutePath());
            }
        }
    }
}
