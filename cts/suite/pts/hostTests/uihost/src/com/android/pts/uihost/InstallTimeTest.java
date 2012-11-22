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
import com.android.pts.util.ReportLog;
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
 * Test to measure installation time of a APK.
 */
public class InstallTimeTest extends DeviceTestCase implements IBuildReceiver {
    private static final String TAG = "InstallTimeTest";
    private final static String CTS_RUNNER = "android.test.InstrumentationCtsTestRunner";
    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;
    private ReportLog mReport = null;

    static final String PACKAGE = "com.replica.replicaisland";
    static final String APK = "com.replica.replicaisland.apk";

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReport = new ReportLog();
        mDevice = getDevice();
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mDevice.uninstallPackage(PACKAGE);
        mReport.throwReportToHost();
    }

    public void testInstallTime() throws Exception {
        final int NUMBER_REPEAT = 10;
        final CtsBuildHelper build = mBuild;
        final ITestDevice device = mDevice;
        double[] result = MeasureTime.measure(NUMBER_REPEAT, new MeasureRun() {
            @Override
            public void prepare(int i) throws Exception {
                device.uninstallPackage(PACKAGE);
            }
            @Override
            public void run(int i) throws Exception {
                File app = build.getTestApp(APK);
                device.installPackage(app, false);
            }
        });
        mReport.printArray("time in ms", result, false);
        StatResult stat = Stat.getStat(result);
        mReport.printSummary("time in ms", stat.mAverage, stat.mStddev);
    }

}
