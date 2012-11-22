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

package com.android.pts.uihost;

import android.cts.util.TimeoutReq;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.MeasureTime;
import com.android.pts.util.PtsException;
import com.android.pts.util.Stat;
import com.android.pts.util.Stat.StatResult;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.TestDeviceOptions;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.util.Map;

/**
 * Measure time to taskswitching between two Apps: A & B
 * Actual test is done in device, but this host side code installs all necessary APKs
 * and starts device test which is in PtsDeviceTaskswitchingControl.
 */
public class TaskSwitchingTest extends DeviceTestCase implements IBuildReceiver {
    private static final String TAG = "TaskSwitchingTest";
    private final static String CTS_RUNNER = "android.test.InstrumentationCtsTestRunner";
    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;

    static final String[] PACKAGES = {
        "com.android.pts.taskswitching.control",
        "com.android.pts.taskswitching.appa",
        "com.android.pts.taskswitching.appb"
    };
    static final String[] APKS = {
        "PtsDeviceTaskswitchingControl.apk",
        "PtsDeviceTaskswitchingAppA.apk",
        "PtsDeviceTaskswitchingAppB.apk"
    };

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
        for (int i = 0; i < PACKAGES.length; i++) {
            mDevice.uninstallPackage(PACKAGES[i]);
            File app = mBuild.getTestApp(APKS[i]);
            mDevice.installPackage(app, false);
        }
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < PACKAGES.length; i++) {
            mDevice.uninstallPackage(PACKAGES[i]);
        }
    }

    @TimeoutReq(minutes = 30)
    public void testTaskswitching() throws Exception {
        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(PACKAGES[0], CTS_RUNNER,
                mDevice.getIDevice());
        CollectingTestListener listener = new CollectingTestListener();
        mDevice.runInstrumentationTests(testRunner, listener);
        TestRunResult result = listener.getCurrentRunResults();
        if (result.isRunFailure()) {
            fail(result.getRunFailureMessage());
        }
        Map<TestIdentifier, TestResult> details = result.getTestResults();
        final String expectedException = "com.android.pts.util.PtsException";
        for (Map.Entry<TestIdentifier, TestResult> entry : details.entrySet()) {
            TestResult res = entry.getValue();
            String stackTrace = res.getStackTrace();
            if (stackTrace != null) {
                if (stackTrace.startsWith(expectedException)) {
                    String[] lines = stackTrace.split("[\r\n]+");
                    String msg = lines[0].substring(expectedException.length() + 1).trim();
                    throw new PtsException(msg);
                }
            }
        }
        fail("no performance data");
    }

}
