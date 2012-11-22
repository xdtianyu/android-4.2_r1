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

package com.android.pts.dram;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.WindowManager;

import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;

/**
 * check how many screens the memcpy function can copy in a sec.
 * Note that this does not represent the total memory bandwidth available in the system
 * as typically CPU cannot use the whole bandwidth.
 * Smaller buffers can fit into L1 or L2 cache, which can show big boost.
 */
public class BandwidthTest extends PtsAndroidTestCase {
    private static final String TAG = "BandwidthTest";
    private static final int REPETITION = 10;
    private static final int REPEAT_IN_EACH_CALL = 100;
    private static final int KB = 1024;
    private static final int MB = 1024 * 1024;

    public void testMemcpyK004() {
        doRunMemcpy(4 * KB);
    }

    public void testMemcpyK008() {
        doRunMemcpy(8 * KB);
    }

    public void testMemcpyK016() {
        doRunMemcpy(16 * KB);
    }

    public void testMemcpyK032() {
        doRunMemcpy(32 * KB);
    }

    public void testMemcpyK064() {
        doRunMemcpy(64 * KB);
    }

    public void testMemcpyK128() {
        doRunMemcpy(128 * KB);
    }

    public void testMemcpyK256() {
        doRunMemcpy(256 * KB);
    }

    public void testMemcpyK512() {
        doRunMemcpy(512 * KB);
    }

    public void testMemcpyM001() {
        doRunMemcpy(1 * MB);
    }

    public void testMemcpyM002() {
        doRunMemcpy(2 * MB);
    }

    public void testMemcpyM004() {
        doRunMemcpy(4 * MB);
    }

    public void testMemcpyM008() {
        doRunMemcpy(8 * MB);
    }

    public void testMemcpyM016() {
        doRunMemcpy(16 * MB);
    }

    private void doRunMemcpy(int bufferSize) {
        double[] result = new double[REPETITION];
        for (int i = 0; i < REPETITION; i++) {
            result[i] = MemoryNative.runMemcpy(bufferSize, REPEAT_IN_EACH_CALL);
        }
        getReportLog().printArray("ms", result, false);
        double[] mbps = ReportLog.calcRatePerSecArray(
                (double)bufferSize * REPEAT_IN_EACH_CALL / 1024.0 / 1024.0, result);
        getReportLog().printArray("MB/s", mbps, true);
        Stat.StatResult stat = Stat.getStat(mbps);
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        Log.i(TAG, " x " + size.x + " y " + size.y);
        double pixels = size.x * size.y;
        // now this represents how many times the whole screen can be copied in a sec.
        double screensPerSecAverage = stat.mAverage / pixels * 1024.0 * 1024.0 / 4.0;
        double screensPerSecStddev = stat.mStddev / pixels * 1024.0 * 1024.0 / 4.0;
        getReportLog().printSummary("screen copies per sec", screensPerSecAverage,
                screensPerSecStddev);
    }
}
