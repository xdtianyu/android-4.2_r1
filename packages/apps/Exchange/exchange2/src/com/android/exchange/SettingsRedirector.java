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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.emailcommon.utility.IntentUtilities;

/**
 * An empty {@link Activity} that simply redirects to the proper settings editor for the Email
 * application.
 * This is needed since the Exchange service runs as a separate UID and is therefore tracked as
 * a separate entity in the framework for things such as data usage. Links from those places to
 * Exchange should really go to Email where the real settings are.
 */
public class SettingsRedirector extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Intent redirect = new Intent(
                Intent.ACTION_EDIT,
                IntentUtilities.createActivityIntentUrlBuilder("settings").build());
        redirect.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(redirect);
        finish();
    }
}
