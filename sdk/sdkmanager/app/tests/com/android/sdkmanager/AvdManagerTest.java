/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.sdkmanager;

import com.android.SdkConstants;
import com.android.io.FileWrapper;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManagerTestCase;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.project.ProjectProperties;

import java.io.File;
import java.util.Map;

public class AvdManagerTest extends SdkManagerTestCase {

    private IAndroidTarget mTarget;
    private File mAvdFolder;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mTarget = getSdkManager().getTargets()[0];
        mAvdFolder = AvdInfo.getDefaultAvdFolder(getAvdManager(), getName());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateAvdWithoutSnapshot() {

        getAvdManager().createAvd(
                mAvdFolder,
                this.getName(),
                mTarget,
                SdkConstants.ABI_ARMEABI,
                null,   // skinName
                null,   // sdName
                null,   // properties
                false,  // createSnapshot
                false,  // removePrevious
                false,  // editExisting
                getLog());

        assertEquals("[P Created AVD '" + this.getName() + "' based on Android 0.0, ARM (armeabi) processor\n]",
                getLog().toString());
        assertTrue("Expected config.ini in " + mAvdFolder,
                new File(mAvdFolder, "config.ini").exists());
        Map<String, String> map = ProjectProperties.parsePropertyFile(
                new FileWrapper(mAvdFolder, "config.ini"), getLog());
        assertEquals("HVGA", map.get("skin.name"));
        assertEquals("platforms/v0_0/skins/HVGA", map.get("skin.path").replace(File.separatorChar, '/'));
        assertEquals("platforms/v0_0/images/", map.get("image.sysdir.1").replace(File.separatorChar, '/'));
        assertEquals(null, map.get("snapshot.present"));
        assertTrue("Expected userdata.img in " + mAvdFolder,
                new File(mAvdFolder, "userdata.img").exists());
        assertFalse("Expected NO snapshots.img in " + mAvdFolder,
                new File(mAvdFolder, "snapshots.img").exists());
    }

    public void testCreateAvdWithSnapshot() {

        getAvdManager().createAvd(
                mAvdFolder,
                this.getName(),
                mTarget,
                SdkConstants.ABI_ARMEABI,
                null,   // skinName
                null,   // sdName
                null,   // properties
                true,   // createSnapshot
                false,  // removePrevious
                false,  // editExisting
                getLog());

        assertEquals("[P Created AVD '" + this.getName() + "' based on Android 0.0, ARM (armeabi) processor\n]",
                getLog().toString());
        assertTrue("Expected snapshots.img in " + mAvdFolder,
                new File(mAvdFolder, "snapshots.img").exists());
        Map<String, String> map = ProjectProperties.parsePropertyFile(
                new FileWrapper(mAvdFolder, "config.ini"), getLog());
        assertEquals("true", map.get("snapshot.present"));
    }
}
