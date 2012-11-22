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

package com.android.pts.browser;

import android.cts.util.TimeoutReq;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.pts.ptsutil.LogcatLineReceiver;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Run browser benchmarking.
 * Benchmarking result is printed via logcat (JavaScript console.log method).
 * Corresponding device side test should be running to make browser benchmarking working,
 * And the device test expects HOST_COMPLETION_BROADCAST broadcast from host as device
 * test cannot detect the completion from browser side.
 */
public class BrowserTest extends DeviceTestCase implements IBuildReceiver {
    private static final String BROADCAST_CMD = "am broadcast -a com.android.pts.browser.completion";
    private static final String TAG = "BrowserTest";
    private static final String CTS_RUNNER = "android.test.InstrumentationCtsTestRunner";
    private static final String PACKAGE = "com.android.pts.browser";
    private static final String APK = "PtsDeviceBrowserLauncher.apk";
    private static final long LOGCAT_TIMEOUT_IN_SEC = 10 * 60L;
    private static final String LOGCAT_FILTER = " browser:D chromium:D *:S";
    private static final long REBOOT_WAIT_TIME_IN_MS = 2 * 60 * 1000L;

    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;
    private LogcatLineReceiver mReceiver;
    private ReportLog mReport;

    private volatile boolean mIgnoreLine = true;
    private double mResult;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReport = new ReportLog();
        mDevice = getDevice();
        mDevice.uninstallPackage(PACKAGE);
        File app = mBuild.getTestApp(APK);
        mDevice.installPackage(app, false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mDevice.uninstallPackage(PACKAGE);
        mReport.throwReportToHost();
    }

    @TimeoutReq(minutes = 40)
    public void testOctane() throws Exception {
        String resultPattern = "([A-Z][\\d\\w]+): ([\\d]+)";
        String summaryPattern = "(Octane Score.*): ([\\d]+)";
        int numberRepeat = 5;
        double[] results = new double[numberRepeat];
        for (int i = 0; i < numberRepeat; i++) {
            Log.i(TAG, i + "-th round");
            // browser will not refresh if the page is already loaded.
            mDevice.reboot();
            Thread.sleep(REBOOT_WAIT_TIME_IN_MS);
            results[i] = runBenchmarking("testOctane", resultPattern, summaryPattern);
        }
        mReport.printArray("scores", results, true);
        Stat.StatResult stat = Stat.getStat(results);
        mReport.printSummary("Score", stat.mAverage, stat.mStddev);
    }

    private double runBenchmarking(String testMethodName, String resultPattern,
            String summaryPattern) throws DeviceNotAvailableException, InterruptedException {
        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(PACKAGE, CTS_RUNNER,
                mDevice.getIDevice());
        testRunner.setMethodName("com.android.pts.browser.LaunchBrowserTest", testMethodName);
        CollectingTestListener listener = new CollectingTestListener();
        mIgnoreLine = true;
        startLogMonitoring(resultPattern, summaryPattern);
        // hack to ignore already captured logcat as the monitor will get it again.
        // Checking time and skipping may be a better logic, but simply throwing away also works.
        Thread.sleep(5000);
        mIgnoreLine = false;
        Log.i(TAG, "start to run test in device");
        mDevice.runInstrumentationTests(testRunner, listener);
        TestRunResult result = listener.getCurrentRunResults();
        if (result.isRunFailure()) {
            fail(result.getRunFailureMessage());
        }
        if (result.getNumPassedTests() == 0) {
            fail("maybe timeout");
        }
        stopLogMonitoring();
        return mResult;
    }

    void startLogMonitoring(String resultPattern, String summaryPattern)
            throws InterruptedException, DeviceNotAvailableException {
        final Pattern result = Pattern.compile(resultPattern);
        final Pattern summary = Pattern.compile(summaryPattern);
        mReceiver = new LogcatLineReceiver(mDevice, LOGCAT_FILTER, LOGCAT_TIMEOUT_IN_SEC) {
            @Override
            public void processALine(String line) throws DeviceNotAvailableException {
                Log.i(TAG, "processALine " + line + " ignore " + mIgnoreLine);
                if (mIgnoreLine) {
                    return;
                }
                Matcher matchResult = result.matcher(line);
                if (matchResult.find()) {
                    mReport.printValue(matchResult.group(1),
                            Double.parseDouble(matchResult.group(2)));
                }
                Matcher matchSummary = summary.matcher(line);
                if (matchSummary.find()) {
                    mResult = Double.parseDouble(matchSummary.group(2));
                    mReport.printValue(matchSummary.group(1), mResult);
                    mDevice.executeShellCommand(BROADCAST_CMD);
                }
            }
        };
        mReceiver.start();
    }

    void stopLogMonitoring() {
        mReceiver.stop();
    }
}
