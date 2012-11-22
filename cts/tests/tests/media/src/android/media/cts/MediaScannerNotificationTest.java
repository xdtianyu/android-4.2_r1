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

package android.media.cts;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.test.AndroidTestCase;

public class MediaScannerNotificationTest extends AndroidTestCase {

    public void testMediaScannerNotification() throws Exception {
        ScannerNotificationReceiver startedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_STARTED);
        ScannerNotificationReceiver finishedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_FINISHED);

        IntentFilter startedIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        startedIntentFilter.addDataScheme("file");
        IntentFilter finshedIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        finshedIntentFilter.addDataScheme("file");

        mContext.registerReceiver(startedReceiver, startedIntentFilter);
        mContext.registerReceiver(finishedReceiver, finshedIntentFilter);

        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                + Environment.getExternalStorageDirectory())));

        startedReceiver.waitForBroadcast();
        finishedReceiver.waitForBroadcast();
    }
}
