/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.voicedialer;


import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import com.android.internal.telephony.TelephonyIntents;
import android.util.Log;
import android.widget.Toast;

public class VoiceDialerReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceDialerReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (false) Log.d(TAG, "onReceive " + intent);

        // fetch up useful stuff
        String action = intent.getAction();
        String host = intent.getData() != null ? intent.getData().getHost() : null;
        
        // force recompilation of g2g on boot
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            CommandRecognizerEngine.deleteCachedGrammarFiles(context);
        }
        
        // force recompilation if apps change, for 'OPEN' command
        else if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                Intent.ACTION_PACKAGE_CHANGED.equals(action) ||
                Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action) ||
                Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            CommandRecognizerEngine.deleteCachedGrammarFiles(context);
        }

        // Voice Dialer Logging Enabled, *#*#8351#*#*
        else if (TelephonyIntents.SECRET_CODE_ACTION.equals(action) && "8351".equals(host)) {
            RecognizerLogger.enable(context);
            Toast.makeText(context, R.string.logging_enabled, Toast.LENGTH_LONG).show();
        }

        // Voice Dialer Logging Disabled, *#*#8350#*#*
        else if (TelephonyIntents.SECRET_CODE_ACTION.equals(action) && "8350".equals(host)) {
            RecognizerLogger.disable(context);
            Toast.makeText(context, R.string.logging_disabled, Toast.LENGTH_LONG).show();
        }
    }
}
