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

import com.android.tradefed.device.ITestDevice;

/**
 * class to handle logcat log per each line.
 * processALine is supposed to be overridden.
 */
public class LogcatLineReceiver extends LogcatMonitor {
    public LogcatLineReceiver(ITestDevice device, String logcatFilter, long timeoutInSec) {
        super(device, logcatFilter, timeoutInSec);
    }

    /**
     * empty default implementation. Will be called whenever a line of log is received
     * @param line
     * @throws Exception
     */
    public void processALine(String line) throws Exception {

    }

    @Override
    public void addOutput(byte[] data, int offset, int length) {
        String lines = new String(data, offset, length);
        try {
            for (String line : lines.split("\n")) {
                processALine(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
