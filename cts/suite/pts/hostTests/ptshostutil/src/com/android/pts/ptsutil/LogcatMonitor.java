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

package com.android.pts.ptsutil;

import com.android.ddmlib.IShellOutputReceiver;
import com.android.tradefed.device.BackgroundDeviceAction;
import com.android.tradefed.device.ITestDevice;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * class to monitor adb logcat asynchronously.
 * Unlike tradefed's LogcatReceiver, log is accumulated to a buffer, and the log is removed after
 * reading once.
 */
public class LogcatMonitor implements IShellOutputReceiver {
    private static final String TAG = "LogcatMonitor";
    private LinkedBlockingQueue<String> mQ = new LinkedBlockingQueue<String>();
    private boolean mRunning = false;
    private long mTimeoutInSec;
    private BackgroundDeviceAction mDeviceAction;
    // sentinel to detect EOS
    private static final String EOS = "LOGCAT_MONITOR_EOS";
    private static final String LOGCAT_CMD = "logcat -v threadtime ";

    public LogcatMonitor(ITestDevice device, String logcatFilter, long timeoutInSec) {
        mTimeoutInSec = timeoutInSec;
        int logStartDelay = 0;
        mDeviceAction = new BackgroundDeviceAction(LOGCAT_CMD + logcatFilter, TAG, device,
                this, logStartDelay);
    }

    /** start monitoring log */
    public void start() {
        if (mRunning) {
            return;
        }
        clear();
        mDeviceAction.start();
        mRunning = true;
    }

    /** stop monitoring */
    public void stop() {
        mDeviceAction.cancel();
        mRunning = false;
    }

    /** clear all stored logs */
    public void clear() {
        mQ.clear();
    }

    /**
     * read a line of log. If there is no data stored, it can wait until
     *   the timeout specified in the constructor.
     * @return null for time-out. Otherwise, a line of log is returned.
     * @throws IOException for EOS (logcat terminated for whatever reason)
     * @throws InterruptedException
     */
    public String getALine() throws IOException, InterruptedException {
        String line = mQ.poll(mTimeoutInSec, TimeUnit.SECONDS);
        if (line == EOS) {
            throw new IOException();
        }
        return line;
    }

    public boolean dataAvailable() {
        return !mQ.isEmpty();
    }

    @Override
    public void addOutput(byte[] data, int offset, int length) {
        String lines = new String(data, offset, length);
        for (String line : lines.split("\n")) {
            mQ.add(line);
        }
    }

    @Override
    public void flush() {
        mQ.add(EOS);
        mRunning = false;
    }

    @Override
    public boolean isCancelled() {
        return !mRunning;
    }
}
