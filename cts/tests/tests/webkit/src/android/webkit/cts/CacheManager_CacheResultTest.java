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
import android.webkit.WebView;


import org.apache.http.HttpStatus;
import org.apache.http.impl.cookie.DateUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CacheManager_CacheResultTest
        extends ActivityInstrumentationTestCase2<WebViewStubActivity> {
    private static final long NETWORK_OPERATION_TIMEOUT = 10000L;

    private CtsTestServer mWebServer;
    private WebViewOnUiThread mOnUiThread;

    public CacheManager_CacheResultTest() {
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

    public void testCacheResult() throws Exception {
        final long validity = 5 * 60 * 1000; // 5 min
        final long age = 30 * 60 * 1000; // 30 min
        final long tolerance = 5 * 1000; // 5s

        mWebServer = new CtsTestServer(getActivity());
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mWebServer.setDocumentAge(age);
        mWebServer.setDocumentValidity(validity);

        mOnUiThread.clearCache(true);
        new PollingCheck(NETWORK_OPERATION_TIMEOUT) {
            @Override
            protected boolean check() {
                CacheResult result =
                    CacheManager.getCacheFile(url, null);
                return result == null;
            }
        }.run();
        final long time = System.currentTimeMillis();
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        Map<String, String> headers = new HashMap<String, String>();
        CacheResult result = CacheManager.getCacheFile(url, headers);
        assertTrue(headers.isEmpty());

        assertNotNull(result);
        assertNotNull(result.getInputStream());
        assertTrue(result.getContentLength() > 0);
        assertNull(result.getETag());
        assertEquals((double)(time - age),
                (double)DateUtils.parseDate(result.getLastModified()).getTime(),
                (double)tolerance);
        File file = new File(CacheManager.getCacheFileBaseDir().getPath(), result.getLocalPath());
        assertTrue(file.exists());
        assertNull(result.getLocation());
        assertEquals("text/html", result.getMimeType());
        assertNull(result.getOutputStream());
        assertEquals((double)(time + validity), (double)result.getExpires(),
                (double)tolerance);
        assertEquals(HttpStatus.SC_OK, result.getHttpStatusCode());
        assertNotNull(result.getEncoding());

        result.setEncoding("iso-8859-1");
        assertEquals("iso-8859-1", result.getEncoding());

        result.setInputStream(null);
        assertNull(result.getInputStream());
    }
}
