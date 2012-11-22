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


import android.test.AndroidTestCase;
import android.webkit.PluginData;
import android.webkit.UrlInterceptHandler;
import android.webkit.UrlInterceptRegistry;
import android.webkit.CacheManager.CacheResult;

import java.util.Map;

@SuppressWarnings("deprecation")
public class UrlInterceptRegistryTest extends AndroidTestCase {
    private int mService;

    public void testRegisterHandler() {
        UrlInterceptHandler handler1 = new MockUrlInterceptHandler();
        UrlInterceptHandler handler2 = new MockUrlInterceptHandler();

        assertFalse(UrlInterceptRegistry.unregisterHandler(handler1));

        assertTrue(UrlInterceptRegistry.registerHandler(handler1));
        assertFalse(UrlInterceptRegistry.registerHandler(handler1));
        assertTrue(UrlInterceptRegistry.registerHandler(handler2));

        assertTrue(UrlInterceptRegistry.unregisterHandler(handler1));
        assertTrue(UrlInterceptRegistry.unregisterHandler(handler2));
        assertFalse(UrlInterceptRegistry.unregisterHandler(handler1));
    }

    public void testGetSurrogate() {
        mService = 0;

        UrlInterceptHandler handler1 = new UrlInterceptHandler() {
            public CacheResult service(String url, Map<String, String> headers) {
                mService = 1;
                return new CacheResult();
            }

            public PluginData getPluginData(String url,
                    Map<String, String> headers) {
                return null;
            }
        };
        UrlInterceptHandler handler2 = new UrlInterceptHandler() {
            public CacheResult service(String url, Map<String, String> headers) {
                mService = 2;
                return new CacheResult();
            }

            public PluginData getPluginData(String url,
                    Map<String, String> headers) {
                return null;
            }
        };
        UrlInterceptHandler handler3 = new UrlInterceptHandler() {
            public CacheResult service(String url, Map<String, String> headers) {
                mService = 3;
                return null;
            }

            public PluginData getPluginData(String url,
                    Map<String, String> headers) {
                return null;
            }
        };
        UrlInterceptRegistry.registerHandler(handler1);
        UrlInterceptRegistry.registerHandler(handler2);
        UrlInterceptRegistry.registerHandler(handler3);

        UrlInterceptRegistry.setUrlInterceptDisabled(true);
        assertNull(UrlInterceptRegistry.getSurrogate(null, null));
        assertNull(UrlInterceptRegistry.getPluginData(null, null));
        // no handlers get called
        assertEquals(mService, 0);

        UrlInterceptRegistry.setUrlInterceptDisabled(false);
        assertNotNull(UrlInterceptRegistry.getSurrogate(null, null));
        // handler3 gets called first, but returns null, handler2 returns the surrogate
        assertEquals(mService, 2);
    }

    private class MockUrlInterceptHandler implements UrlInterceptHandler {
        public CacheResult service(String url, Map<String, String> headers) {
            return null;
        }

        public PluginData getPluginData(String url, Map<String, String> headers) {
            return null;
        }
    }
}
