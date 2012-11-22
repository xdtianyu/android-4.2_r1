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

import android.test.ActivityInstrumentationTestCase2;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.cts.WebViewOnUiThread.WaitForLoadedClient;


import org.apache.http.HttpStatus;

public class HttpAuthHandlerTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {

    private static final long TIMEOUT = 10000;

    private CtsTestServer mWebServer;
    private WebViewOnUiThread mOnUiThread;

    public HttpAuthHandlerTest() {
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

    public void testProceed() throws Exception {
        mWebServer = new CtsTestServer(getActivity());
        String url = mWebServer.getAuthAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);

        // wrong credentials
        MyWebViewClient client = new MyWebViewClient(true, "FakeUser", "FakePass");
        mOnUiThread.setWebViewClient(client);

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(CtsTestServer.AUTH_REALM, client.realm);
        assertEquals(CtsTestServer.getReasonString(HttpStatus.SC_UNAUTHORIZED), mOnUiThread.getTitle());
        assertTrue(client.useHttpAuthUsernamePassword);

        // missing credentials
        client = new MyWebViewClient(true, null, null);
        mOnUiThread.setWebViewClient(client);

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(CtsTestServer.AUTH_REALM, client.realm);
        assertEquals(
                CtsTestServer.getReasonString(HttpStatus.SC_UNAUTHORIZED), mOnUiThread.getTitle());
        assertTrue(client.useHttpAuthUsernamePassword);

        // correct credentials
        client = new MyWebViewClient(true, CtsTestServer.AUTH_USER, CtsTestServer.AUTH_PASS);
        mOnUiThread.setWebViewClient(client);

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(CtsTestServer.AUTH_REALM, client.realm);
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, mOnUiThread.getTitle());
        assertTrue(client.useHttpAuthUsernamePassword);
    }

    public void testCancel() throws Exception {
        mWebServer = new CtsTestServer(getActivity());

        String url = mWebServer.getAuthAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        MyWebViewClient client = new MyWebViewClient(false, null, null);
        mOnUiThread.setWebViewClient(client);

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(CtsTestServer.AUTH_REALM, client.realm);
        assertEquals(
                CtsTestServer.getReasonString(HttpStatus.SC_UNAUTHORIZED), mOnUiThread.getTitle());
    }

    private class MyWebViewClient extends WaitForLoadedClient {
        String realm;
        boolean useHttpAuthUsernamePassword;

        private boolean mProceed;
        private String mUser;
        private String mPassword;
        private int mAuthCount;

        MyWebViewClient(boolean proceed, String user, String password) {
            super(mOnUiThread);
            mProceed = proceed;
            mUser = user;
            mPassword = password;
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            ++mAuthCount;
            if (mAuthCount > 1) {
                handler.cancel();
                return;
            }
            this.realm = realm;
            this.useHttpAuthUsernamePassword = handler.useHttpAuthUsernamePassword();
            if (mProceed) {
                handler.proceed(mUser, mPassword);
            } else {
                handler.cancel();
            }
        }
    }
}
