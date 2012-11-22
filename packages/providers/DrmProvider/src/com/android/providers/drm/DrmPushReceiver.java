/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.providers.drm;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import android.drm.mobile1.DrmException;
import android.drm.mobile1.DrmRights;
import android.drm.mobile1.DrmRightsManager;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.provider.Telephony;
import android.util.Log;

public class DrmPushReceiver extends BroadcastReceiver {
    private static final String TAG = "DrmPushReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION)) {
            // Get right mimetype.
            String rightMimeType = intent.getType();
            if (DrmRightsManager.DRM_MIMETYPE_RIGHTS_XML_STRING.equals(rightMimeType) ||
                DrmRightsManager.DRM_MIMETYPE_RIGHTS_WBXML_STRING.equals(rightMimeType)) {
                // Get right data.
                byte[] rightData = (byte[]) intent.getExtra("data");
                if (rightData == null) {
                    Log.e(TAG, "The rights data is invalid.");
                    return;
                }
                ByteArrayInputStream rightDataStream = new ByteArrayInputStream(rightData);
                try {
                    DrmRightsManager.getInstance().installRights(rightDataStream,
                            rightData.length,
                            rightMimeType);
                } catch (DrmException e) {
                    Log.e(TAG, "Install drm rights failed.");
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "IOException occurs when install drm rights.");
                    return;
                }

                Log.d(TAG, "Install drm rights successfully.");
                return;
            }
            Log.d(TAG, "This is not drm rights push mimetype.");
        }
        Log.d(TAG, "This is not wap push received action.");
    }
}
