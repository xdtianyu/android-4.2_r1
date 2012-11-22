/* Copyright (C) 2011 The Android Open Source Project.
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

package com.android.exchange.adapter;

import com.android.exchange.EasSyncService;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parse the result of a Settings command.
 *
 * We only send the Settings command in EAS 14.0 after sending a Provision command for the first
 * time.  parse() returns true in the normal case; false if access to the account is denied due
 * to the actual settings (e.g. if a particular device type isn't allowed by the server)
 */
public class SettingsParser extends Parser {
    private final EasSyncService mService;

    public SettingsParser(InputStream in, EasSyncService service) throws IOException {
        super(in);
        mService = service;
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.SETTINGS_SETTINGS) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.SETTINGS_STATUS) {
                int status = getValueInt();
                mService.userLog("Settings status = ", status);
                if (status == 1) {
                    res = true;
                } else {
                    // Access denied = 3; others should never be seen
                    res = false;
                }
            } else if (tag == Tags.SETTINGS_DEVICE_INFORMATION) {
                parseDeviceInformation();
            } else {
                skipTag();
            }
        }
        return res;
    }

    public void parseDeviceInformation() throws IOException {
        while (nextTag(Tags.SETTINGS_DEVICE_INFORMATION) != END) {
            if (tag == Tags.SETTINGS_SET) {
                parseSet();
            } else {
                skipTag();
            }
        }
    }

    public void parseSet() throws IOException {
        while (nextTag(Tags.SETTINGS_SET) != END) {
            if (tag == Tags.SETTINGS_STATUS) {
                mService.userLog("Set status = ", getValueInt());
            } else {
                skipTag();
            }
        }
    }
}
