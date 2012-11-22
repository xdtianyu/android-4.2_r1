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

import junit.framework.TestCase;

import java.io.File;

public class VariantConfigurationTest extends TestCase {

    private ProductFlavor mDefaultConfig;
    private ProductFlavor mFlavorConfig;
    private BuildType mBuildType;

    private static class ManifestParserMock implements ManifestParser {

        private final String mPackageName;

        ManifestParserMock(String packageName) {
            mPackageName = packageName;
        }

        public String getPackage(File manifestFile) {
            return mPackageName;
        }
    }

    @Override
    protected void setUp() throws Exception {
        mDefaultConfig = new ProductFlavor("main");
        mFlavorConfig = new ProductFlavor("flavor");
        mBuildType = new BuildType("debug");
    }

    public void testPackageOverrideNone() {
        VariantConfiguration variant = getVariant();

        assertNull(variant.getPackageOverride());
    }

    public void testPackageOverridePackageFromFlavor() {
        mFlavorConfig.setPackageName("foo.bar");

        VariantConfiguration variant = getVariant();

        assertEquals("foo.bar", variant.getPackageOverride());
    }

    public void testPackageOverridePackageFromFlavorWithSuffix() {
        mFlavorConfig.setPackageName("foo.bar");
        mBuildType.setPackageNameSuffix(".fortytwo");

        VariantConfiguration variant = getVariant();

        assertEquals("foo.bar.fortytwo", variant.getPackageOverride());
    }

    public void testPackageOverridePackageFromFlavorWithSuffix2() {
        mFlavorConfig.setPackageName("foo.bar");
        mBuildType.setPackageNameSuffix("fortytwo");

        VariantConfiguration variant = getVariant();

        assertEquals("foo.bar.fortytwo", variant.getPackageOverride());
    }

    public void testPackageOverridePackageWithSuffixOnly() {

        mBuildType.setPackageNameSuffix("fortytwo");

        VariantConfiguration variant = getVariantWithManifestPackage("fake.package.name");

        assertEquals("fake.package.name.fortytwo", variant.getPackageOverride());
    }

    private VariantConfiguration getVariant() {
        VariantConfiguration variant = new VariantConfiguration(
                mDefaultConfig, new MockSourceSet("main"),
                mBuildType, new MockSourceSet("debug"),
                VariantConfiguration.Type.DEFAULT) {
            // don't do validation.
            @Override
            protected void validate() {

            }
        };

        variant.addProductFlavor(mFlavorConfig, new MockSourceSet("custom"));

        return variant;
    }

    private VariantConfiguration getVariantWithManifestPackage(final String packageName) {
        VariantConfiguration variant = new VariantConfiguration(
                mDefaultConfig, new MockSourceSet("main"),
                mBuildType, new MockSourceSet("debug"),
                VariantConfiguration.Type.DEFAULT) {
            @Override
            public String getPackageFromManifest() {
                return packageName;
            }
            // don't do validation.
            @Override
            protected void validate() {

            }
        };

        variant.addProductFlavor(mFlavorConfig, new MockSourceSet("custom"));
        return variant;
    }
}
