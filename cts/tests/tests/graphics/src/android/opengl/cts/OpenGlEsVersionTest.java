/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.opengl.cts;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.util.regex.Pattern;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Test for checking whether the ro.opengles.version property is set to the correct value.
 */
public class OpenGlEsVersionTest
        extends ActivityInstrumentationTestCase2<OpenGlEsVersionStubActivity> {

    private static final String TAG = OpenGlEsVersionTest.class.getSimpleName();

    private static final int EGL_OPENGL_ES2_BIT = 0x0004;

    private OpenGlEsVersionStubActivity mActivity;

    public OpenGlEsVersionTest() {
        super("com.android.cts.stub", OpenGlEsVersionStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testOpenGlEsVersion() throws InterruptedException {
        int detectedVersion = getDetectedVersion();
        int reportedVersion = getVersionFromActivityManager(mActivity);

        assertEquals("Detected OpenGL ES major version " + detectedVersion
                + " but Activity Manager is reporting " +  reportedVersion
                + " (Check ro.opengles.version)", detectedVersion, reportedVersion);
        assertEquals("Reported OpenGL ES version from ActivityManager differs from PackageManager",
                reportedVersion, getVersionFromPackageManager(mActivity));

        assertGlVersionString(1);
        if (detectedVersion >= 2) {
            restartActivityWithClientVersion(2);
            assertGlVersionString(2);
        }
    }

    /** @return OpenGL ES major version 1 or 2 or some negative number for error */
    private static int getDetectedVersion() {

        /*
         * Get all the device configurations and check if any of the attributes specify the
         * the EGL_OPENGL_ES2_BIT to determine whether the device supports 2.0.
         */

        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] numConfigs = new int[1];

        if (egl.eglInitialize(display, null)) {
            try {
                if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
                    EGLConfig[] configs = new EGLConfig[numConfigs[0]];
                    if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                        int[] value = new int[1];
                        for (int i = 0; i < numConfigs[0]; i++) {
                            if (egl.eglGetConfigAttrib(display, configs[i],
                                    EGL10.EGL_RENDERABLE_TYPE, value)) {
                                if ((value[0] & EGL_OPENGL_ES2_BIT) == EGL_OPENGL_ES2_BIT) {
                                    return 2;
                                }
                            } else {
                                Log.w(TAG, "Getting config attribute with "
                                        + "EGL10#eglGetConfigAttrib failed "
                                        + "(" + i + "/" + numConfigs[0] + "): "
                                        + egl.eglGetError());
                            }
                        }
                        return 1;
                    } else {
                        Log.e(TAG, "Getting configs with EGL10#eglGetConfigs failed: "
                                + egl.eglGetError());
                        return -1;
                    }
                } else {
                    Log.e(TAG, "Getting number of configs with EGL10#eglGetConfigs failed: "
                            + egl.eglGetError());
                    return -2;
                }
              } finally {
                  egl.eglTerminate(display);
              }
        } else {
            Log.e(TAG, "Couldn't initialize EGL.");
            return -3;
        }
    }

    private static int getVersionFromActivityManager(Context context) {
        ActivityManager activityManager =
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
        if (configInfo.reqGlEsVersion != ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
            return getMajorVersion(configInfo.reqGlEsVersion);
        } else {
            return 1; // Lack of property means OpenGL ES version 1
        }
    }

    private static int getVersionFromPackageManager(Context context) {
        PackageManager packageManager = context.getPackageManager();
        FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
        if (featureInfos != null && featureInfos.length > 0) {
            for (FeatureInfo featureInfo : featureInfos) {
                // Null feature name means this feature is the open gl es version feature.
                if (featureInfo.name == null) {
                    if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                        return getMajorVersion(featureInfo.reqGlEsVersion);
                    } else {
                        return 1; // Lack of property means OpenGL ES version 1
                    }
                }
            }
        }
        return 1;
    }

    /** @see FeatureInfo#getGlEsVersion() */
    private static int getMajorVersion(int glEsVersion) {
        return ((glEsVersion & 0xffff0000) >> 16);
    }

    /**
     * Check that the version string has some form of "Open GL ES X.Y" in it where X is the major
     * version and Y must be some digit.
     */
    private void assertGlVersionString(int majorVersion) throws InterruptedException {
        String message = "OpenGL version string '" + mActivity.getVersionString()
                + "' is not " + majorVersion + ".0+.";
        assertTrue(message, Pattern.matches(".*OpenGL.*ES.*" + majorVersion + "\\.\\d.*",
                mActivity.getVersionString()));
    }

    /** Restart {@link GLSurfaceViewStubActivity} with a specific client version. */
    private void restartActivityWithClientVersion(int version) {
        mActivity.finish();
        setActivity(null);

        try {
            Intent intent = OpenGlEsVersionStubActivity.createIntent(version);
            setActivityIntent(intent);
            mActivity = getActivity();
        } finally {
            setActivityIntent(null);
        }
    }
}
