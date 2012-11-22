/*
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
package com.motorolamobility.preflighting.core.utils;

/**
 * This Interface intends to hold Android Manifest XML constants.
 * It's not mandatory to use these constants, they are here to avoid
 * mistypes and minimize impacts due to changes.
 */
public class ManifestConstants
{

    /**
     * The 'manifest' tag
     */
    public static final String MANIFEST_TAG = "manifest"; //$NON-NLS-1$

    /**
     * The 'supports-screens' tag
     */
    public static final String SUPPORTS_SCREEN_TAG = "supports-screens"; //$NON-NLS-1$

    /**
     * The 'android:smallScreens' tag
     */
    public static final String SMALL_SCREENS_ATTRIBUTE = "android:smallScreens"; //$NON-NLS-1$

    /**
     * The 'android:xlargeScreens' tag
     */
    public static final String XLARGE_SCREENS_ATTRIBUTE = "android:xlargeScreens"; //$NON-NLS-1$

    /**
     * The 'android:targetSdkVersion' tag
     */
    public static final String TARGET_SDK_VERSION_ATTRIBUTE = "android:targetSdkVersion"; //$NON-NLS-1$

    /**
     * The 'android:minSdkVersion' tag
     */
    public static final String MIN_SDK_VERSION_ATTRIBUTE = "android:minSdkVersion"; //$NON-NLS-1$

    /**
     * The 'uses-sdk' tag 
     */
    public static final String USES_SDK_TAG = "uses-sdk"; //$NON-NLS-1$

    /**
     * The 'uses-feature' tag
     */
    public static final String USES_FEATURE_TAG = "uses-feature"; //$NON-NLS-1$

    /**
     * The 'uses-permission' tag 
     */
    public final static String USES_PERMISSION_ATTRIBUTE = "uses-permission"; //$NON-NLS-1$

    /**
     * The 'android:name' tag 
     */
    public final static String ANDROID_NAME_ATTRIBUTE = "android:name"; //$NON-NLS-1$

    /**
     * The application node
     */
    public static final String APPLICATION_TAG = "application"; //$NON-NLS-1$

    /**
     * The 'android:debuggable' attribute
     */
    public static final String ANDROID_DEBUGGABLE_ATTRIBUTE = "android:debuggable"; //$NON-NLS-1$

    /**
     * The 'android:icon' attribute
     */
    public static final String ANDROID_ICON_ATTRIBUTE = "android:icon"; //$NON-NLS-1$

    /**
     * The 'android:label' attribute
     */
    public static final String ANDROID_LABEL_ATTRIBUTE = "android:label"; //$NON-NLS-1$

    /**
     * The 'android:versionName' attribute
     */
    public static final String ANDROID_VERSION_NAME_ATTRIBUTE = "android:versionName"; //$NON-NLS-1$

    /**
     * The 'android:versionCode' attribute
     */
    public static final String ANDROID_VERSION_CODE_ATTRUIBUTE = "android:versionCode"; //$NON-NLS-1$

    /**
     * The 'android:maxSdkVersion' attribute
     */
    public static final String ANDROID_MAX_SDK_VERSION_ATTRIBUTE = "android:maxSdkVersion"; //$NON-NLS-1$

    /**
     * The 'android:targetSdkVersion' attribute
     */
    public static final String ANDROID_TARGET_SDK_VERSION_ATTRIBUTE = "android:targetSdkVersion"; //$NON-NLS-1$

}
