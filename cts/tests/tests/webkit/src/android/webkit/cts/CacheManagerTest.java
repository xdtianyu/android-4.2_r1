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

package android.webkit.cts;

import android.cts.util.PollingCheck;
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.CacheManager;
import android.webkit.CacheManager.CacheResult;


import java.util.Map;

public class CacheManagerTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {
    private static final long CACHEMANAGER_INIT_TIMEOUT = 5000L;
    private static final long NETWORK_OPERATION_TIMEOUT = 10000L;

    private CtsTestServer mWebServer;
    private WebViewOnUiThread mOnUiThread;

    public CacheManagerTest() {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mOnUiThread = new WebViewOnUiThread(this, getActivity().getWebView());
    }

    @Override
    protected void tearDown() throws Exception {
        mOnUiThread.cleanUp();
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
        super.tearDown();
    }

    public void testGetCacheFileBaseDir() {
        assertTrue(CacheManager.getCacheFileBaseDir().exists());
    }

    public void testCacheTransaction() {
    }

    public void testCacheFile() throws Exception {
        mWebServer = new CtsTestServer(getActivity());
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.EMBEDDED_IMG_URL);

        // Wait for CacheManager#init() finish.
        new PollingCheck(CACHEMANAGER_INIT_TIMEOUT) {
            @Override
            protected boolean check() {
                return CacheManager.getCacheFileBaseDir() != null;
            }
        }.run();

        mOnUiThread.clearCache(true);
        new PollingCheck(NETWORK_OPERATION_TIMEOUT) {
            @Override
            protected boolean check() {
                CacheResult result = CacheManager.getCacheFile(url, null);
                return result == null;
            }
        }.run();

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck(NETWORK_OPERATION_TIMEOUT) {
            @Override
            protected boolean check() {
                CacheResult result = CacheManager.getCacheFile(url, null);
                return result != null;
            }
        }.run();

        // Can not test saveCacheFile(), because the output stream is null and
        // saveCacheFile() will throw a NullPointerException.  There is no
        // public API to set the output stream.
    }

    public void testCacheDisabled() {
        // The cache should always be enabled.
        assertFalse(CacheManager.cacheDisabled());
    }
}
