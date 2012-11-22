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

package com.android.providers.applications;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.internal.os.PkgUsageStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An extension of {@link ApplicationsProvider} that makes its testing easier.
 */
public class ApplicationsProviderForTesting extends ApplicationsProvider {

    private PackageManager mMockPackageManager;

    private MockActivityManager mMockActivityManager;

    private boolean mHasGlobalSearchPermission;

    private MockUpdateHandler mMockUpdateHandler;

    @Override
    protected PackageManager getPackageManager() {
        return mMockPackageManager;
    }

    protected void setMockPackageManager(PackageManager mockPackageManager) {
        mMockPackageManager = mockPackageManager;
    }

    @Override
    protected Map<String, PkgUsageStats> fetchUsageStats() {
        Map<String, PkgUsageStats> stats = new HashMap<String, PkgUsageStats>();
        for (PkgUsageStats pus : mMockActivityManager.getAllPackageUsageStats()) {
            stats.put(pus.packageName, pus);
        }
        return stats;
    }

    protected void setMockActivityManager(MockActivityManager mockActivityManager) {
        mMockActivityManager = mockActivityManager;
    }

    protected void setHasGlobalSearchPermission(boolean hasGlobalSearchPermission) {
        mHasGlobalSearchPermission = hasGlobalSearchPermission;
    }

    @Override
    protected boolean hasGlobalSearchPermission() {
        return mHasGlobalSearchPermission;
    }

    @Override
    Handler createHandler(Looper looper) {
        mMockUpdateHandler = new MockUpdateHandler(looper);
        return mMockUpdateHandler;
    }

    public boolean dispatchNextMessage() {
        return mMockUpdateHandler.dispatchNextMessage();
    }

    private class MockUpdateHandler extends UpdateHandler {

        List<Message> mMessages = new ArrayList<Message>();

        MockUpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            mMessages.add(msg);
            return true;
        }

        public boolean dispatchNextMessage() {
            if (!mMessages.isEmpty()) {
                dispatchMessage(mMessages.remove(0));
                return true;
            } else {
                return false;
            }
        }
    }
}
