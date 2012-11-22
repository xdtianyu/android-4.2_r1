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

package com.android.pts.filesystemperf;

import android.cts.util.TimeoutReq;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;


public class RandomRWTest extends PtsAndroidTestCase {
    private static final String DIR_RANDOM_WR = "RANDOM_WR";
    private static final String DIR_RANDOM_RD = "RANDOM_RD";

    @Override
    protected void tearDown() throws Exception {
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_WR);
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_RD);
        super.tearDown();
    }

    @TimeoutReq(minutes = 60)
    public void testRandomRead() throws Exception {
        final int READ_BUFFER_SIZE = 4 * 1024;
        final long fileSize = FileUtil.getFileSizeExceedingMemory(getContext(), READ_BUFFER_SIZE);
        FileUtil.doRandomReadTest(getContext(), DIR_RANDOM_RD, getReportLog(), fileSize,
                READ_BUFFER_SIZE);
    }

    // It is taking too long in tuna, and thus cannot run multiple times
    @TimeoutReq(minutes = 60)
    public void testRandomUpdate() throws Exception {
        final int WRITE_BUFFER_SIZE = 4 * 1024;
        final long fileSize = 512 * 1024 * 1024;
        FileUtil.doRandomWriteTest(getContext(), DIR_RANDOM_WR, getReportLog(), fileSize,
                WRITE_BUFFER_SIZE);
    }
}
