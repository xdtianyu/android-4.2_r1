/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.os.cts;

import java.io.File;

import junit.framework.TestCase;
import android.os.Environment;
import android.os.StatFs;

public class StatFsTest extends TestCase {
    public void testStatFs(){
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        int blockSize = stat.getBlockSize();
        int totalBlocks = stat.getBlockCount();
        int freeBlocks = stat.getFreeBlocks();
        int availableBlocks = stat.getAvailableBlocks();

        assertTrue(blockSize > 0);
        assertTrue(totalBlocks > 0);
        assertTrue(freeBlocks >= availableBlocks);
        assertTrue(availableBlocks > 0);

        path = Environment.getRootDirectory();
        stat.restat(path.getPath());
        blockSize = stat.getBlockSize();
        totalBlocks = stat.getBlockCount();
        freeBlocks = stat.getFreeBlocks();
        availableBlocks = stat.getAvailableBlocks();

        assertTrue(blockSize > 0);
        assertTrue(totalBlocks > 0);
        assertTrue(freeBlocks >= availableBlocks);
        assertTrue(availableBlocks > 0);
    }
}
