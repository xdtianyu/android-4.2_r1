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

package com.android.pts.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.webkit.cts.CtsTestServer;
import com.android.pts.util.PtsAndroidTestCase;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Device side code to run browser benchmarking.
 * It launches an activity with URL and wait for broadcast sent from host.
 * It is host's responsibility to send broadcast after parsing browser's result.
 */
public class LaunchBrowserTest extends PtsAndroidTestCase {

    private static final String OCTANE_START_FILE = "octane/index.html";
    private static final String HOST_COMPLETION_BROADCAST = "com.android.pts.browser.completion";
    private static long BROWSER_COMPLETION_TIMEOUT = 10 * 60;
    private CtsTestServer mWebServer;
    private HostBroadcastReceiver mReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWebServer = new CtsTestServer(getContext());
        mReceiver = new HostBroadcastReceiver();
        mReceiver.register(getContext(), HOST_COMPLETION_BROADCAST);
    }

    @Override
    protected void tearDown() throws Exception {
        mReceiver.unregister(getContext());
        mWebServer.shutdown();
        mWebServer = null;
        super.tearDown();
    }

    public void testOctane() throws InterruptedException {
        String url = mWebServer.getAssetUrl(OCTANE_START_FILE) + "?auto=1";
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        mReceiver.waitForBroadcast(BROWSER_COMPLETION_TIMEOUT);
    }

    class HostBroadcastReceiver extends BroadcastReceiver {
        private final Semaphore mSemaphore = new Semaphore(0);

        public void register(Context context, String actionName) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(actionName);
            context.registerReceiver(this, filter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        public boolean waitForBroadcast(long timeoutInSec) throws InterruptedException {
            return mSemaphore.tryAcquire(timeoutInSec, TimeUnit.SECONDS);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mSemaphore.release();
        }
    }
}
