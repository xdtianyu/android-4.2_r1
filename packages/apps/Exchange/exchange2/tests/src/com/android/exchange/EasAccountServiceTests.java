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

package com.android.exchange;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.EasAccountServiceTests exchange
 */
@SmallTest
public class EasAccountServiceTests extends AndroidTestCase {

    Context mMockContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mMockContext = getContext();
    }

    public void testResetHeartbeats() {
        EasAccountService svc = new EasAccountService();
        // Test case in which the minimum and force heartbeats need to come up
        svc.mPingMaxHeartbeat = 1000;
        svc.mPingMinHeartbeat = 200;
        svc.mPingHeartbeat = 300;
        svc.mPingForceHeartbeat = 100;
        svc.mPingHeartbeatDropped = true;
        svc.resetHeartbeats(400);
        assertEquals(400, svc.mPingMinHeartbeat);
        assertEquals(1000, svc.mPingMaxHeartbeat);
        assertEquals(400, svc.mPingHeartbeat);
        assertEquals(400, svc.mPingForceHeartbeat);
        assertFalse(svc.mPingHeartbeatDropped);

        // Test case in which the force heartbeat needs to come up
        svc.mPingMaxHeartbeat = 1000;
        svc.mPingMinHeartbeat = 200;
        svc.mPingHeartbeat = 100;
        svc.mPingForceHeartbeat = 100;
        svc.mPingHeartbeatDropped = true;
        svc.resetHeartbeats(150);
        assertEquals(200, svc.mPingMinHeartbeat);
        assertEquals(1000, svc.mPingMaxHeartbeat);
        assertEquals(150, svc.mPingHeartbeat);
        assertEquals(150, svc.mPingForceHeartbeat);
        assertFalse(svc.mPingHeartbeatDropped);

        // Test case in which the maximum needs to come down
        svc.mPingMaxHeartbeat = 1000;
        svc.mPingMinHeartbeat = 200;
        svc.mPingHeartbeat = 800;
        svc.mPingForceHeartbeat = 100;
        svc.mPingHeartbeatDropped = true;
        svc.resetHeartbeats(600);
        assertEquals(200, svc.mPingMinHeartbeat);
        assertEquals(600, svc.mPingMaxHeartbeat);
        assertEquals(600, svc.mPingHeartbeat);
        assertEquals(100, svc.mPingForceHeartbeat);
        assertFalse(svc.mPingHeartbeatDropped);
    }
}
