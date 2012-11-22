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

package com.android.sdkuilib.internal.repository.core;

import com.android.SdkConstants;
import com.android.sdklib.internal.repository.packages.BrokenPackage;
import com.android.sdklib.internal.repository.packages.FullRevision;
import com.android.sdklib.internal.repository.packages.MockAddonPackage;
import com.android.sdklib.internal.repository.packages.MockBrokenPackage;
import com.android.sdklib.internal.repository.packages.MockEmptyPackage;
import com.android.sdklib.internal.repository.packages.MockExtraPackage;
import com.android.sdklib.internal.repository.packages.MockPlatformPackage;
import com.android.sdklib.internal.repository.packages.MockPlatformToolPackage;
import com.android.sdklib.internal.repository.packages.MockSystemImagePackage;
import com.android.sdklib.internal.repository.packages.MockToolPackage;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.sources.SdkRepoSource;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdklib.repository.PkgProps;
import com.android.sdkuilib.internal.repository.ISettingsPage;
import com.android.sdkuilib.internal.repository.MockUpdaterData;
import com.android.sdkuilib.internal.repository.core.PackagesDiffLogic;
import com.android.sdkuilib.internal.repository.core.PkgCategory;
import com.android.sdkuilib.internal.repository.core.PkgItem;

import java.util.Properties;

import junit.framework.TestCase;

public class PackagesDiffLogicTest extends TestCase {

    private PackagesDiffLogic m;
    private MockUpdaterData u;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        u = new MockUpdaterData();
        m = new PackagesDiffLogic(u);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // ----
    //
    // Test Details Note: the way load is implemented in PackageLoader, the
    // loader processes each source and then for each source the packages are added
    // to a list and the sorting algorithm is called with that list. Thus for
    // one load, many calls to the sortByX/Y happen, with the list progressively
    // being populated.
    // However when the user switches sorting algorithm, the package list is not
    // reloaded and is processed at once.

    public void testSortByApi_Empty() {
        m.updateStart();
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[0]));
        assertFalse(m.updateEnd(true /*sortByApi*/));

        // We also keep these 2 categories even if they contain nothing
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
               getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_AddSamePackage() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        m.updateStart();
        // First insert local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "some pkg", 1)
        }));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));

        // Insert the next source
        // Same package as the one installed, so we don't display it
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "some pkg", 1)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_AddOtherPackage() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        m.updateStart();
        // First insert local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "some pkg", 1)
        }));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));

        // Insert the next source
        // Not the same package as the one installed, so we'll display it
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "other pkg", 1)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'other pkg' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_Update1() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        // Typical case: user has a locally installed package in revision 1
        // The display list after sort should show that installed package.
        m.updateStart();
        // First insert local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));

        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 4),
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=4>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_Reload() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        // First load reveals a package local package and its update
        m.updateStart();
        // First insert local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, true /*displaySortByApi*/));

        // Now simulate a reload that clears the package list and creates similar
        // objects but not the same references. The only difference is that updateXyz
        // returns false since nothing changes.

        m.updateStart();
        // First insert local packages
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_InstallPackage() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        // First load reveals a new package
        m.updateStart();
        // No local packages at first
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));

        // Install it.
        m.updateStart();
        // local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertTrue(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));

        // Load reveals an update
        m.updateStart();
        // local packages
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_DeletePackage() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        // We have an installed package
        m.updateStart();
        // local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, true /*displaySortByApi*/));

        // User now deletes the installed package.
        m.updateStart();
        // No local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    public void testSortByApi_NoRemoteSources() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://example.com/url2", "repo2");

        // We have a couple installed packages
        m.updateStart();
        // local packages
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src2, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src2, "android", "usb_driver", 5, 3),
        }));
        // and no remote sources have been loaded (e.g. because there's no network)
        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 5>\n" +
                "-- <INSTALLED, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, true /*displaySortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategorySource <source=repo2 (example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 5>\n" +
                "-- <INSTALLED, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortByApi_CompleteUpdate() {
        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://2.example.com/url2", "repo2");

        // Resulting categories are sorted by Tools, descending platform API and finally Extras.
        // Addons are sorted by name within their API.
        // Extras are sorted by vendor name.
        // The order packages are added to the mAllPkgItems list is purposedly different from
        // the final order we get.

        // First update has the typical tools and a couple extras
        m.updateStart();

        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
        }));
        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 4, updated by:Android USB Driver, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, true /*displaySortByApi*/));

        // Next update adds platforms and addon, sorted in a category based on their API level
        m.updateStart();
        MockPlatformPackage p1;
        MockPlatformPackage p2;
        @SuppressWarnings("unused") // keep p3 for clarity
        MockPlatformPackage p3;

        assertTrue(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),  // API 1
                p3 = new MockPlatformPackage(src1, 3, 6, 3),
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon D", p1, 10),
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),    // API 2
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                // the rev 7+8 will be ignored since there's a rev 9 coming after
                new MockAddonPackage(src2, "addon B", p2, 7),
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
                // 11+12 should be ignored updates, 13 will update 10
                new MockAddonPackage(src2, "addon D", p1, 10),
                new MockAddonPackage(src2, "addon D", p1, 12),  // note: 12 listed before 11
                new MockAddonPackage(src2, "addon D", p1, 11),
                new MockAddonPackage(src2, "addon D", p1, 13),
        }));
        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=API 3, label=Android android-3 (API 3), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "PkgCategoryApi <API=API 2, label=Android android-2 (API 2), #items=3>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <NEW, pkg:The addon B from vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:The addon C from vendor 2, Android API 2, revision 9>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 5, updated by:The addon A from vendor 1, Android API 1, revision 6>\n" +
                "-- <INSTALLED, pkg:The addon D from vendor 1, Android API 1, revision 10, updated by:The addon D from vendor 1, Android API 1, revision 13>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 4, updated by:Android USB Driver, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, true /*displaySortByApi*/));

        // Reloading the same thing should have no impact except for the update methods
        // returning false when they don't change the current list.
        m.updateStart();

        assertFalse(m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),
                p3 = new MockPlatformPackage(src1, 3, 6, 3),
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon D", p1, 10),
        }));
        assertFalse(m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),
        }));
        assertTrue(m.updateSourcePackages(true /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                // the rev 7+8 will be ignored since there's a rev 9 coming after
                new MockAddonPackage(src2, "addon B", p2, 7),
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
                // 11+12 should be ignored updates, 13 will update 10
                new MockAddonPackage(src2, "addon D", p1, 10),
                new MockAddonPackage(src2, "addon D", p1, 12),  // note: 12 listed before 11
                new MockAddonPackage(src2, "addon D", p1, 11),
                new MockAddonPackage(src2, "addon D", p1, 13),
        }));
        assertFalse(m.updateEnd(true /*sortByApi*/));

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=API 3, label=Android android-3 (API 3), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "PkgCategoryApi <API=API 2, label=Android android-2 (API 2), #items=3>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <NEW, pkg:The addon B from vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:The addon C from vendor 2, Android API 2, revision 9>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 5, updated by:The addon A from vendor 1, Android API 1, revision 6>\n" +
                "-- <INSTALLED, pkg:The addon D from vendor 1, Android API 1, revision 10, updated by:The addon D from vendor 1, Android API 1, revision 13>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 4, updated by:Android USB Driver, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, true /*displaySortByApi*/));
    }

    // ----

    public void testSortBySource_Empty() {
        m.updateStart();
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[0]));
        // UpdateEnd returns true since it removed the synthetic "unknown source" category
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertTrue(m.getCategories(false /*sortByApi*/).isEmpty());

        assertEquals(
                "",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortBySource_AddPackages() {
        // Since we're sorting by source, items are grouped under their source
        // even if installed. The 'local' source is only for installed items for
        // which we don't know the source.
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        m.updateStart();
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "known source", 2),
                new MockEmptyPackage(null, "unknown source", 3),
        }));

        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'unknown source' rev=3>\n" +
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'known source' rev=2>\n",
                getTree(m, false /*displaySortByApi*/));

        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "new", 1),
        }));

        assertFalse(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'unknown source' rev=3>\n" +
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new' rev=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'known source' rev=2>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortBySource_Update1() {

        // Typical case: user has a locally installed package in revision 1
        // The display list after sort should show that instaled package.
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));

        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=0>\n" +
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, false /*displaySortByApi*/));

        // Edge case: the source reveals an update in revision 2. It is ignored since
        // we already have a package in rev 4.

        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 4),
                new MockEmptyPackage(src1, "type1", 2),
        }));

        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=4>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortBySource_Reload() {

        // First load reveals a package local package and its update
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now simulate a reload that clears the package list and creates similar
        // objects but not the same references. Update methods return false since
        // they don't change anything.
        m.updateStart();
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortBySource_InstallPackage() {

        // First load reveals a new package
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        // no local package
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, false /*displaySortByApi*/));


        // Install it. The display only shows the installed one, 'hiding' the remote package
        m.updateStart();
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now we have an update
        m.updateStart();
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortBySource_DeletePackage() {
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");

        // Start with an installed package and its matching remote package
        m.updateStart();
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, false /*displaySortByApi*/));

        // User now deletes the installed package.
        m.updateStart();
        // no local package
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSortBySource_CompleteUpdate() {
        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://2.example.com/url2", "repo2");

        // First update has the typical tools and a couple extras
        m.updateStart();

        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
        }));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=4>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 4, updated by:Android USB Driver, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, false /*displaySortByApi*/));

        // Next update adds platforms and addon, sorted in a category based on their API level
        m.updateStart();
        MockPlatformPackage p1;
        MockPlatformPackage p2;
        @SuppressWarnings("unused") // keep p3 for clarity
        MockPlatformPackage p3;

        assertTrue(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),  // API 1
                p3 = new MockPlatformPackage(src1, 3, 6, 3),
                new MockPlatformPackage(src1, 3, 6, 3),       // API 3
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon D", p1, 10),
        }));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),    // API 2
        }));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                // the rev 7+8 will be ignored since there's a rev 9 coming after
                new MockAddonPackage(src2, "addon B", p2, 7),
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
                // 11+12 should be ignored updates, 13 will update 10
                new MockAddonPackage(src2, "addon D", p1, 10),
                new MockAddonPackage(src2, "addon D", p1, 12),  // note: 12 listed before 11
                new MockAddonPackage(src2, "addon D", p1, 11),
                new MockAddonPackage(src2, "addon D", p1, 13),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=7>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 4, updated by:Android USB Driver, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=4>\n" +
                "-- <NEW, pkg:The addon B from vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:The addon C from vendor 2, Android API 2, revision 9>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 5, updated by:The addon A from vendor 1, Android API 1, revision 6>\n" +
                "-- <INSTALLED, pkg:The addon D from vendor 1, Android API 1, revision 10, updated by:The addon D from vendor 1, Android API 1, revision 13>\n",
                getTree(m, false /*displaySortByApi*/));

        // Reloading the same thing should have no impact except for the update methods
        // returning false when they don't change the current list.
        m.updateStart();

        assertFalse(m.updateSourcePackages(false /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),  // API 1
                p3 = new MockPlatformPackage(src1, 3, 6, 3),
                new MockPlatformPackage(src1, 3, 6, 3),       // API 3
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon D", p1, 10),
        }));
        assertFalse(m.updateSourcePackages(false /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),
        }));
        assertTrue(m.updateSourcePackages(false /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                // the rev 7+8 will be ignored since there's a rev 9 coming after
                new MockAddonPackage(src2, "addon B", p2, 7),
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
                // 11+12 should be ignored updates, 13 will update 10
                new MockAddonPackage(src2, "addon D", p1, 10),
                new MockAddonPackage(src2, "addon D", p1, 12),  // note: 12 listed before 11
                new MockAddonPackage(src2, "addon D", p1, 11),
                new MockAddonPackage(src2, "addon D", p1, 13),
        }));
        assertTrue(m.updateEnd(false /*sortByApi*/));

        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=7>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver, revision 4, updated by:Android USB Driver, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=4>\n" +
                "-- <NEW, pkg:The addon B from vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:The addon C from vendor 2, Android API 2, revision 9>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 5, updated by:The addon A from vendor 1, Android API 1, revision 6>\n" +
                "-- <INSTALLED, pkg:The addon D from vendor 1, Android API 1, revision 10, updated by:The addon D from vendor 1, Android API 1, revision 13>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    // ----

    public void testIsFirstLoadComplete() {
        // isFirstLoadComplete is a simple toggle that goes from true to false when read once
        assertTrue(m.isFirstLoadComplete());
        assertFalse(m.isFirstLoadComplete());
        assertFalse(m.isFirstLoadComplete());
    }

    public void testCheckNewUpdateItems_NewOnly() {
        // Populate the list with a few items and an update
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "has update", 1),
                new MockEmptyPackage(src1, "no update", 4)
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "has update", 2),
                new MockEmptyPackage(src1, "new stuff", 3),
        });
        m.updateEnd(true /*sortByApi*/);
        // Nothing is checked at first
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now request to check new items only
        m.checkNewUpdateItems(true, false, false, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- < * NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- < * NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testCheckNewUpdateItems_UpdateOnly() {
        // Populate the list with a few items and an update
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "has update", 1),
                new MockEmptyPackage(src1, "no update", 4)
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "has update", 2),
                new MockEmptyPackage(src1, "new stuff", 3),
        });
        m.updateEnd(true /*sortByApi*/);
        // Nothing is checked at first
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now request to check update items only
        m.checkNewUpdateItems(false, true, false, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=3>\n" +
                "-- < * INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=3>\n" +
                "-- < * INSTALLED, pkg:MockEmptyPackage 'has update' rev=1, updated by:MockEmptyPackage 'has update' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new stuff' rev=3>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'no update' rev=4>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testCheckNewUpdateItems_SelectInitial() {
        // Populate the list with typical items: tools, platforms tools, extras, 2 platforms.
        // With nothing installed, this should pick the top platform and its system images
        // (the mock platform claims to not have any included abi)
        // It's ok not to select the tools, since they are a dependency of all platforms.

        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://2.example.com/url2", "repo2");

        m.updateStart();
        MockPlatformPackage p1;
        MockPlatformPackage p2;

        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "google", "usb_driver", 5, 3),
                p1 = new MockPlatformPackage(src1, 1, 2, 3),    // API 1
                p2 = new MockPlatformPackage(src1, 2, 4, 3),    // API 2
                new MockSystemImagePackage(src1, p2, 1, "armeabi"),
                new MockSystemImagePackage(src1, p2, 1, "x86"),
        });
        m.updateSourcePackages(true /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon B", p2, 7),
                new MockExtraPackage(src2, "carrier", "custom_rom", 1, 0),
        });
        m.updateEnd(true /*sortByApi*/);

        m.checkNewUpdateItems(false, true, true, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 10>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=API 2, label=Android android-2 (API 2), #items=4>\n" +
                "-- < * NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- < * NEW, pkg:ARM EABI System Image, Android API 2, revision 1>\n" +
                "-- < * NEW, pkg:Intel x86 Atom System Image, Android API 2, revision 1>\n" +
                "-- < * NEW, pkg:The addon B from vendor 2, Android API 2, revision 7>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=2>\n" +
                "-- <NEW, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 5>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n" +
                "-- <NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=7>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 10>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- < * NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <NEW, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- < * NEW, pkg:ARM EABI System Image, Android API 2, revision 1>\n" +
                "-- < * NEW, pkg:Intel x86 Atom System Image, Android API 2, revision 1>\n" +
                "-- <NEW, pkg:Google USB Driver, revision 5>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=3>\n" +
                "-- < * NEW, pkg:The addon B from vendor 2, Android API 2, revision 7>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom, revision 1>\n",
                getTree(m, false /*displaySortByApi*/));

        // We don't install the USB driver by default on Mac or Linux, only on Windows
        m.clear();
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockExtraPackage(src1, "google", "usb_driver", 5, 3),
        });
        m.updateEnd(true /*sortByApi*/);
        m.checkNewUpdateItems(false, true, true, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- <NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, false /*displaySortByApi*/));

        m.clear();
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockExtraPackage(src1, "google", "usb_driver", 5, 3),
        });
        m.updateEnd(true /*sortByApi*/);
        m.checkNewUpdateItems(false, true, true, SdkConstants.PLATFORM_DARWIN);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- <NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, false /*displaySortByApi*/));

        m.clear();
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockExtraPackage(src1, "google", "usb_driver", 5, 3),
        });
        m.updateEnd(true /*sortByApi*/);
        m.checkNewUpdateItems(false, true, true, SdkConstants.PLATFORM_WINDOWS);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- < * NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- < * NEW, pkg:Google USB Driver, revision 5>\n",
                getTree(m, false /*displaySortByApi*/));

    }

    public void testCheckUncheckAllItems() {
        // Populate the list with a couple items and an update
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
                new MockEmptyPackage(src1, "type3", 3),
        });
        m.updateEnd(true /*sortByApi*/);
        // Nothing is checked at first
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, true /*displaySortByApi*/));

        // Manually check the items in the sort-by-API case, but not the source
        for (PkgItem item : m.getAllPkgItems(true /*byApi*/, false /*bySource*/)) {
            item.setChecked(true);
        }

        // by-api sort should be checked but not by source
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- < * INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- < * NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, false /*displaySortByApi*/));

        // now uncheck them all
        m.uncheckAllItems();

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, false /*displaySortByApi*/));

        // Manually check the items in both by-api and by-source
        for (PkgItem item : m.getAllPkgItems(true /*byApi*/, true /*bySource*/)) {
            item.setChecked(true);
        }

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- < * INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- < * NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- < * INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- < * NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, false /*displaySortByApi*/));

        // now uncheck them all
        m.uncheckAllItems();

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type3' rev=3>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    // ----

    public void testLocalIsNewer() {
        // This tests an edge case that typically happens only during development where
        // one would have a local package which revision number is larger than what the
        // remove repositories can offer. In this case we don't want to offer the remote
        // package as an "upgrade" nor as a downgrade.

        // Populate the list with local revisions 5 and lower remote revisions 3
        SdkSource src1 = new SdkRepoSource("http://example.com/url", "repo1");
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(        src1, 5, 5),
                new MockPlatformToolPackage(src1, 5),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(        src1, 3, 3),
                new MockPlatformToolPackage(src1, 3),
        });
        m.updateEnd(true /*sortByApi*/);

        // The remote packages in rev 3 are hidden by the local packages in rev 5
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 5>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 5>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 5>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 5>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testSourceDups() {
        // This tests an edge case were 2 remote repositories are giving the
        // same kind of packages. In rev 14, we didn't want to merge them together
        // unless they had the same hostname. In rev 15, we now treat them the same.

        // repo1, 2 and 3 have the same hostname so redundancy is ok
        SdkSource src1 = new SdkRepoSource("http://example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://example.com/url2", "repo2");
        SdkSource src3 = new SdkRepoSource("http://example.com/url3", "repo3");
        // repo4 has a different hostname but as of rev 15, the packages will be merged together.
        SdkSource src4 = new SdkRepoSource("http://4.example.com/url4", "repo4");
        MockPlatformPackage p1 = null;

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(        src1, 3, 3),
                new MockPlatformToolPackage(src1, 3),
                p1 = new MockPlatformPackage(src1, 1, 2, 3),    // API 1
        });
        m.updateSourcePackages(true /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon B", p1, 6),
        });
        m.updateSourcePackages(true /*sortByApi*/, src3, new Package[] {
                new MockAddonPackage(src3, "addon A", p1, 5), // same as  addon A rev 5 from src2
                new MockAddonPackage(src3, "addon B", p1, 7), // upgrades addon B rev 6 from src2
        });
        m.updateSourcePackages(true /*sortByApi*/, src4, new Package[] {
                new MockAddonPackage(src4, "addon A", p1, 5), // same as  addon A rev 5 from src2
                new MockAddonPackage(src4, "addon B", p1, 7), // upgrades addon B rev 6 from src2
        });
        m.updateEnd(true /*sortByApi*/);

        // The remote packages in rev 3 are hidden by the local packages in rev 5.
        // When sorting by API, the user can tell where the packages come from by looking
        // at the UI tooltip on the packages.
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 5>\n" + // from src2+3+4
                "-- <NEW, pkg:The addon B from vendor 1, Android API 1, revision 7>\n" + // from src3+4
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        // When sorting by source, the src4 source is listed, however since its
        // packages are the same as the ones from src2 or src3 the packages themselves
        // are not shown.
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=3>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategorySource <source=repo2 (example.com), #items=1>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 5>\n" + // from src2+3+4
                "PkgCategorySource <source=repo3 (example.com), #items=1>\n" +
                "-- <NEW, pkg:The addon B from vendor 1, Android API 1, revision 7>\n" + // from src3+4
                "PkgCategorySource <source=repo4 (4.example.com), #items=0>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testRenamedExtraPackage() {
        // Starting with schemas repo v5 and addon v3, an extra package can be renamed
        // using the "old-paths" attribute. This test checks that the diff logic will
        // match an old extra and its new name together.

        // First scenario: local pkg "old_path1" and remote pkg "new_path2".
        // Since the new package does not provide an old_paths attribute, the
        // new package is not treated as an update.

        SdkSource src1 = new SdkRepoSource("http://example.com/url1", "repo1");
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockExtraPackage(src1, "vendor1", "old_path1", 1, 1),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockExtraPackage(src1, "vendor1", "new_path2", 2, 1),
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <NEW, pkg:Vendor1 New Path2, revision 2>\n" +
                "-- <INSTALLED, pkg:Vendor1 Old Path1, revision 1>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=2>\n" +
                "-- <NEW, pkg:Vendor1 New Path2, revision 2>\n" +
                "-- <INSTALLED, pkg:Vendor1 Old Path1, revision 1>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now, start again, but this time the new package uses the old-path attribute
        Properties props = new Properties();
        props.setProperty(PkgProps.EXTRA_OLD_PATHS, "old_path1;oldpath2");
        m.clear();

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockExtraPackage(src1, "vendor1", "old_path1", 1, 1),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockExtraPackage(src1, props, "vendor1", "new_path2", 2),
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:Vendor1 Old Path1, revision 1, updated by:Vendor1 New Path2, revision 2>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:Vendor1 Old Path1, revision 1, updated by:Vendor1 New Path2, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testBrokenAddon() {
        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://2.example.com/url2", "repo2");

        MockPlatformPackage p1 = null;
        MockAddonPackage a1 = null;

        // User has a platform + addon locally installed
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                p1 = new MockPlatformPackage(src1, 1, 2, 3),    // API 1
                a1 = new MockAddonPackage(src2, "addon A", p1, 4),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1 /*locals*/, new Package[] {
                p1
        });
        m.updateSourcePackages(true /*sortByApi*/, src2 /*locals*/, new Package[] {
                a1
        });
        m.updateEnd(true /*sortByApi*/);
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=2>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 4>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 4>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now user deletes the platform on disk and reload.
        // The local package parser will only find a broken addon.
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockBrokenPackage(BrokenPackage.MIN_API_LEVEL_NOT_SPECIFIED, 1),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1 /*locals*/, new Package[] {
                new MockPlatformPackage(src1, 1, 2, 3)
        });
        m.updateSourcePackages(true /*sortByApi*/, src2 /*locals*/, new Package[] {
                new MockAddonPackage(src2, "addon A", p1, 4)
        });
        m.updateEnd(true /*sortByApi*/);
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=2>\n" +
                "-- <NEW, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 4>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:Broken package for API 1>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- <NEW, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=1>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 4>\n" +
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:Broken package for API 1>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now user restores the missing platform on disk.
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                p1 = new MockPlatformPackage(src1, 1, 2, 3),    // API 1
                a1 = new MockAddonPackage(src2, "addon A", p1, 4),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1 /*locals*/, new Package[] {
                p1
        });
        m.updateSourcePackages(true /*sortByApi*/, src2 /*locals*/, new Package[] {
                a1
        });
        m.updateEnd(true /*sortByApi*/);
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=2>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 4>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:The addon A from vendor 1, Android API 1, revision 4>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testToolsUpdate() {
        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://2.example.com/url2", "repo2");
        MockPlatformPackage p1;

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(3, 3),    // tool package has no source defined
                new MockPlatformToolPackage(src1, 3),
                p1 = new MockPlatformPackage(src1, 1, 2, 3),    // API 1
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 4, 4),
                new MockPlatformToolPackage(src1, 4),
        });
        m.updateSourcePackages(true /*sortByApi*/, src2, new Package[] {
                new MockAddonPackage(src2, "addon A", p1, 5),
                new MockAddonPackage(src2, "addon B", p1, 6),
        });
        m.updateEnd(true /*sortByApi*/);

        // The remote packages in rev 3 are hidden by the local packages in rev 5
        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 4>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3, updated by:Android SDK Platform-tools, revision 4>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 5>\n" +
                "-- <NEW, pkg:The addon B from vendor 1, Android API 1, revision 6>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 4>\n" +
                "PkgCategorySource <source=repo1 (1.example.com), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3, updated by:Android SDK Platform-tools, revision 4>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategorySource <source=repo2 (2.example.com), #items=2>\n" +
                "-- <NEW, pkg:The addon A from vendor 1, Android API 1, revision 5>\n" +
                "-- <NEW, pkg:The addon B from vendor 1, Android API 1, revision 6>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testToolsMinorUpdate() {
        // Test: Check a minor revision updates an installed major revision.

        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(3, 3),                                          // Tools 3.0.0
                new MockPlatformToolPackage(src1, 3),
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 1), 3),          // Tools 3.0.1
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 3.0.1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 3.0.1>\n" +
                "PkgCategorySource <source=repo1 (1.example.com), #items=1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testToolsPreviewsDisabled() {
        // Test: No local tools installed. The remote server has both tools and platforms
        // in release and RC versions. However the settings "enable previews" is disabled
        // (which is the default) so the previews are not actually loaded from the server.

        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, new FullRevision(2, 0, 0), 3),          // Tools 2
                new MockToolPackage(src1, new FullRevision(4, 0, 0, 1), 3),       // Tools 4 rc1
                new MockPlatformToolPackage(src1, new FullRevision(3, 0, 0)),     // Plat-T 3
                new MockPlatformToolPackage(src1, new FullRevision(5, 0, 0, 1)),  // Plat-T 5 rc1
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 2>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 2>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 3>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testToolsPreviews() {
        // Test: No local tools installed. The remote server has both tools and platforms
        // in release and RC versions.

        // Enable previews in the settings
        u.overrideSetting(ISettingsPage.KEY_ENABLE_PREVIEWS, true);

        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, new FullRevision(2, 0, 0), 3),          // Tools 2
                new MockToolPackage(src1, new FullRevision(4, 0, 0, 1), 3),       // Tools 4 rc1
                new MockPlatformToolPackage(src1, new FullRevision(3, 0, 0)),     // Plat-T 3
                new MockPlatformToolPackage(src1, new FullRevision(5, 0, 0, 1)),  // Plat-T 5 rc1
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 2>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 4 rc1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 5 rc1>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=4>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 4 rc1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 5 rc1>\n",
                getTree(m, false /*displaySortByApi*/));
    }

    public void testPreviewUpdateInstalledRelease() {
        // Test: Local release Tools 3.0.0 installed, server has both a release 3.0.1 available
        // and a Tools Preview 4.0.0 rc1 available.
        // => v3 is updated by 3.0.1
        // => v4.0.0rc1 does not update 3.0.0, instead it's a separate download.

        // Enable previews in the settings
        u.overrideSetting(ISettingsPage.KEY_ENABLE_PREVIEWS, true);

        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(3, 3),    // tool package has no source defined
                new MockPlatformToolPackage(src1, 3),
                new MockPlatformPackage(src1, 1, 2, 3),    // API 1
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, 3, 3),                                  // Tools 3
                new MockToolPackage(src1, new FullRevision(3, 0, 1), 3),          // Tools 3.0.1
                new MockToolPackage(src1, new FullRevision(4, 0, 0, 1), 3),       // Tools 4 rc1
                new MockPlatformToolPackage(src1, new FullRevision(3, 0, 1)),     // PT    3.0.1
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 0, 1)),  // PT    4 rc1
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 3.0.1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3, updated by:Android SDK Platform-tools, revision 3.0.1>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 4 rc1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4 rc1>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 3.0.1>\n" +
                "PkgCategorySource <source=repo1 (1.example.com), #items=4>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 4 rc1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3, updated by:Android SDK Platform-tools, revision 3.0.1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4 rc1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));

        // Now request to check new items and updates:
        // Tools 4 rc1 is greater than the installed Tools 3, but it's a preview so we will NOT
        //   auto-select it by default even though we requested to select "NEW" packages. We
        //   want the user to manually opt-in into the rc/preview package.
        // However Tools 3 has a 3.0.1 update that we'll auto-select.
        m.checkNewUpdateItems(true, true, false, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- < * INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 3.0.1>\n" +
                "-- < * INSTALLED, pkg:Android SDK Platform-tools, revision 3, updated by:Android SDK Platform-tools, revision 3.0.1>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 4 rc1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4 rc1>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=Local Packages (no.source), #items=1>\n" +
                "-- < * INSTALLED, pkg:Android SDK Tools, revision 3, updated by:Android SDK Tools, revision 3.0.1>\n" +
                "PkgCategorySource <source=repo1 (1.example.com), #items=4>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 4 rc1>\n" +
                "-- < * INSTALLED, pkg:Android SDK Platform-tools, revision 3, updated by:Android SDK Platform-tools, revision 3.0.1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4 rc1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));

    }

    public void testPreviewUpdateInstalledPreview() {
        // Test: Local preview Tools 3.0.1rc1 installed, server has both a release 3.0.0 available
        // and a Tools Preview 3.0.1 rc2 available.
        // => Installed 3.0.1rc1 can be updated by 3.0.1rc2
        // => There's a separate "new" download for 3.0.0, not installed and NOT updating 3.0.1rc1.

        // Enable previews in the settings
        u.overrideSetting(ISettingsPage.KEY_ENABLE_PREVIEWS, true);

        SdkSource src1 = new SdkRepoSource("http://1.example.com/url1", "repo1");

        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 1, 1), 4),       //  T 3.0.1rc1
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 1, 1)),  // PT 4.0.1rc1
                new MockPlatformPackage(src1, 1, 2, 3),    // API 1
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 0), 4),          //  T 3.0.0
                new MockToolPackage(src1, new FullRevision(3, 0, 1, 2), 4),       //  T 3.0.1rc2
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 0)),     // PT 4.0.0
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 1, 2)),  // PT 4.0.1 rc2
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 3>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1, updated by:Android SDK Tools, revision 3.0.1 rc2>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1, updated by:Android SDK Platform-tools, revision 4.0.1 rc2>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=5>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 3>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1, updated by:Android SDK Tools, revision 3.0.1 rc2>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1, updated by:Android SDK Platform-tools, revision 4.0.1 rc2>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));

        // Auto select new and update items. In this case:
        // - the previews have updates available.
        // - we're not selecting the non-installed "3.0" version that is older than the
        //   currently installed "3.0.1rc1" version since that would be a downgrade.
        m.checkNewUpdateItems(true, true, false, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 3>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- < * INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1, updated by:Android SDK Tools, revision 3.0.1 rc2>\n" +
                "-- < * INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1, updated by:Android SDK Platform-tools, revision 4.0.1 rc2>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=5>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 3>\n" +
                "-- < * INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1, updated by:Android SDK Tools, revision 3.0.1 rc2>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4>\n" +
                "-- < * INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1, updated by:Android SDK Platform-tools, revision 4.0.1 rc2>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));

        // -----

        // Now simulate that the server has a final package (3.0.1) to replace the
        // installed 3.0.1rc1 package. It's not installed yet, just available.
        // - A new 3.0.1 will be available.
        // - The server no longer lists the RC since there's a final package, yet it is
        //   still locally installed.
        // - The 3.0.1 rc1 is not listed as having an update, since we treat the previews
        //   separately. TODO: consider having the 3.0.1 show up as both a new item /and/
        //   as an update to the 3.0.1rc1. That may have some other side effects.

        m.uncheckAllItems();
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 1, 1), 4),       //  T 3.0.1rc1
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 1, 1)),  // PT 4.0.1rc1
                new MockPlatformPackage(src1, 1, 2, 3),    // API 1
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 1), 4),          //  T 3.0.1
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 1)),     // PT 4.0.1
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 3.0.1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4.0.1>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=5>\n" +
                "-- <NEW, pkg:Android SDK Tools, revision 3.0.1>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1>\n" +
                "-- <NEW, pkg:Android SDK Platform-tools, revision 4.0.1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));

        // Auto select new and update items. In this case the new items are considered
        // updates and yet new at the same time.
        // Test by selecting new items only:
        m.checkNewUpdateItems(true, false, false, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- < * NEW, pkg:Android SDK Tools, revision 3.0.1>\n" +
                "-- < * NEW, pkg:Android SDK Platform-tools, revision 4.0.1>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));

        // Test by selecting update items only:
        m.uncheckAllItems();
        m.checkNewUpdateItems(false, true, false, SdkConstants.PLATFORM_LINUX);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- < * NEW, pkg:Android SDK Tools, revision 3.0.1>\n" +
                "-- < * NEW, pkg:Android SDK Platform-tools, revision 4.0.1>\n" +
                "PkgCategoryApi <API=TOOLS-PREVIEW, label=Tools (Preview Channel), #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1 rc1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1 rc1>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));


        // -----

        // Now simulate that the user has installed the final package (3.0.1) to replace the
        // installed 3.0.1rc1 package.
        // - The 3.0.1 is installed.
        // - The 3.0.1 rc1 isn't listed anymore by the server.

        m.uncheckAllItems();
        m.updateStart();
        m.updateSourcePackages(true /*sortByApi*/, null /*locals*/, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 1), 4),          //  T 3.0.1
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 1)),     // PT 4.0.1
                new MockPlatformPackage(src1, 1, 2, 3),    // API 1
        });
        m.updateSourcePackages(true /*sortByApi*/, src1, new Package[] {
                new MockToolPackage(src1, new FullRevision(3, 0, 1), 4),          //  T 3.0.1
                new MockPlatformToolPackage(src1, new FullRevision(4, 0, 1)),     // PT 4.0.1
        });
        m.updateEnd(true /*sortByApi*/);

        assertEquals(
                "PkgCategoryApi <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1>\n" +
                "PkgCategoryApi <API=API 1, label=Android android-1 (API 1), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "PkgCategoryApi <API=EXTRAS, label=Extras, #items=0>\n",
                getTree(m, true /*displaySortByApi*/));
        assertEquals(
                "PkgCategorySource <source=repo1 (1.example.com), #items=3>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 3.0.1>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 4.0.1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n",
                getTree(m, false /*displaySortByApi*/));
    }



    // ----

    /**
     * Simulates the display we would have in the Packages Tree.
     * This always depends on mCurrentCategories like the tree does.
     * The display format is something like:
     * <pre>
     *   PkgCategory &lt;description&gt;
     *   -- &lt;PkgItem description&gt;
     * </pre>
     */
    public String getTree(PackagesDiffLogic l, boolean displaySortByApi) {
        StringBuilder sb = new StringBuilder();

        for (PkgCategory cat : m.getCategories(displaySortByApi)) {
            sb.append(cat.toString()).append('\n');
            for (PkgItem item : cat.getItems()) {
                sb.append("-- ").append(item.toString()).append('\n');
            }
        }

        return sb.toString();
    }
}
