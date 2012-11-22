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

package com.android.pts.bootup;

import android.cts.util.TimeoutReq;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.MeasureTime;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;
import com.android.pts.util.Stat.StatResult;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.TestDeviceOptions;
import com.android.tradefed.testtype.DeviceTestCase;

/**
 *  Measure reboot-time using adb shell reboot
 */
public class BootupTimeTest extends DeviceTestCase {

    private ReportLog mReport = null;
    // add some delay before each reboot
    final static long SLEEP_BEFORE_REBOOT_TIME = 2 * 60 * 1000L;
    final static int REBOOT_TIMEOUT_MS = 10 * 60 * 1000;

    @Override
    protected void setUp() throws Exception {
        mReport = new ReportLog();
        super.setUp();
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mReport.throwReportToHost();
    }

    @TimeoutReq(minutes = 30)
    public void testBootupTime() throws Exception {
        final int NUMBER_REPEAT = 5;
        double[] result = MeasureTime.measure(NUMBER_REPEAT, new MeasureRun() {
            @Override
            public void prepare(int i) throws Exception {
                if (i == 0) {
                    return;
                }
                Thread.sleep(SLEEP_BEFORE_REBOOT_TIME);
            }
            @Override
            public void run(int i) throws Exception {
                rebootDevice();
            }
        });
        mReport.printArray("time in ms", result, false);
        StatResult stat = Stat.getStat(result);
        mReport.printSummary("time in ms", stat.mAverage, stat.mStddev);
    }

    private void rebootDevice() throws DeviceNotAvailableException {
        TestDeviceOptions options = getDevice().getOptions();
        // store default value and increase time-out for reboot
        int rebootTimeout = options.getRebootTimeout();
        long onlineTimeout = options.getOnlineTimeout();
        options.setRebootTimeout(REBOOT_TIMEOUT_MS);
        options.setOnlineTimeout(REBOOT_TIMEOUT_MS);
        getDevice().setOptions(options);
        getDevice().reboot();
        // restore default values
        options.setRebootTimeout(rebootTimeout);
        options.setOnlineTimeout(onlineTimeout);
        getDevice().setOptions(options);
    }
}
