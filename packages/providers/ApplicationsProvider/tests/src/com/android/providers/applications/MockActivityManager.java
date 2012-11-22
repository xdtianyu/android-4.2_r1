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
package com.android.providers.applications;

import com.android.internal.os.PkgUsageStats;

import java.util.HashMap;
import java.util.Map;

/**
 * The real ActivityManager is difficult to mock out (has a package visibility
 * constructor), so this doesn't extend it.
 */
public class MockActivityManager {

    private Map<String, PkgUsageStats> mPackageUsageStats = new HashMap<String, PkgUsageStats>();

    public void addLastResumeTime(String packageName, String componentName, long lastResumeTime) {
        if (!mPackageUsageStats.containsKey(packageName)) {
            PkgUsageStats stats = new PkgUsageStats(packageName, 1, 0, new HashMap<String, Long>());
            mPackageUsageStats.put(packageName, stats);
        }
        mPackageUsageStats.get(packageName).componentResumeTimes.put(componentName, lastResumeTime);
    }

    public PkgUsageStats[] getAllPackageUsageStats() {
        return mPackageUsageStats.values().toArray(new PkgUsageStats[mPackageUsageStats.size()]);
    }
}
