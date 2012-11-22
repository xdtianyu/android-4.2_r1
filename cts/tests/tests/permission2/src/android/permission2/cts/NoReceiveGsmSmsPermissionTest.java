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

package android.permission2.cts;

import android.app.PendingIntent;
import android.telephony.gsm.SmsManager;

/**
 * Verify Sms and Mms cannot be received without required permissions.
 * Uses {@link android.telephony.gsm.SmsManager}.
 */
@SuppressWarnings("deprecation")
public class NoReceiveGsmSmsPermissionTest extends NoReceiveSmsPermissionTest {

    protected void sendSms(PendingIntent sentIntent, PendingIntent deliveryIntent,
            String currentNumber) {
        SmsManager.getDefault().sendTextMessage(currentNumber, null, "test message",
                sentIntent, deliveryIntent);
    }
}
