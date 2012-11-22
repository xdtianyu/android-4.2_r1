/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.packages.MockEmptyPackage;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

public class UpdaterDataTest extends TestCase {

    private MockUpdaterData m;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m = new MockUpdaterData();
        assertEquals("[]", Arrays.toString(m.getInstalled()));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests the case where we have nothing to install.
     */
    public void testInstallArchives_None() {
        m._installArchives(new ArrayList<ArchiveInfo>());
        assertEquals("[]", Arrays.toString(m.getInstalled()));
    }


    /**
     * Tests the case where there's a simple dependency, in the right order
     * (e.g. install A1 then A2 that depends on A1).
     */
    public void testInstallArchives_SimpleDependency() {

        ArrayList<ArchiveInfo> archives = new ArrayList<ArchiveInfo>();

        Archive a1 = new MockEmptyPackage("a1").getLocalArchive();
        ArchiveInfo ai1 = new ArchiveInfo(a1, null, null);

        Archive a2 = new MockEmptyPackage("a2").getLocalArchive();
        ArchiveInfo ai2 = new ArchiveInfo(a2, null, new ArchiveInfo[] { ai1 } );

        archives.add(ai1);
        archives.add(ai2);

        m._installArchives(archives);
        assertEquals(
                "[MockEmptyPackage 'a1', MockEmptyPackage 'a2']",
                Arrays.toString(m.getInstalled()));
    }

    /**
     * Tests the case where there's a simple dependency, in the wrong order
     * (e.g. install A2 then A1 which A2 depends on)
     */
    public void testInstallArchives_ReverseDependency() {

        ArrayList<ArchiveInfo> archives = new ArrayList<ArchiveInfo>();

        Archive a1 = new MockEmptyPackage("a1").getLocalArchive();
        ArchiveInfo ai1 = new ArchiveInfo(a1, null, null);

        Archive a2 = new MockEmptyPackage("a2").getLocalArchive();
        ArchiveInfo ai2 = new ArchiveInfo(a2, null, new ArchiveInfo[] { ai1 } );

        archives.add(ai2);
        archives.add(ai1);

        m._installArchives(archives);
        assertEquals(
                "[MockEmptyPackage 'a1', MockEmptyPackage 'a2']",
                Arrays.toString(m.getInstalled()));
    }

}
