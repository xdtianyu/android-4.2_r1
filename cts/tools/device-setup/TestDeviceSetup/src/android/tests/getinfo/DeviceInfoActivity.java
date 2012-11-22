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

package android.tests.getinfo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;


/**
 * Collect device information on target device.
 */
public class DeviceInfoActivity extends Activity {

    // work done should be reported in GLES..View
    private CountDownLatch mDone = new CountDownLatch(1);
    private GLESSurfaceView mGLView;

    /**
     * Other classes can call this function to wait for this activity
     * to finish. */
    public void waitForAcitityToFinish() {
        try {
            mDone.await();
        } catch (InterruptedException e) {
            // just move on
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityManager am =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        boolean useGL20 = (info.reqGlEsVersion >= 0x20000);

        mGLView = new GLESSurfaceView(this, useGL20, mDone);
        setContentView(mGLView);

        Configuration con = getResources().getConfiguration();
        String touchScreen = null;
        if (con.touchscreen == Configuration.TOUCHSCREEN_UNDEFINED) {
            touchScreen = "undefined";
        } else if (con.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) {
            touchScreen = "notouch";
        } else if (con.touchscreen == Configuration.TOUCHSCREEN_STYLUS) {
            touchScreen = "stylus";
        } else if (con.touchscreen == Configuration.TOUCHSCREEN_FINGER) {
            touchScreen = "finger";
        }
        if (touchScreen != null) {
            DeviceInfoInstrument.addResult(DeviceInfoConstants.TOUCH_SCREEN,
                    touchScreen);
        }

        String navigation = null;
        if (con.navigation == Configuration.NAVIGATION_UNDEFINED) {
            navigation = "undefined";
        } else if (con.navigation == Configuration.NAVIGATION_NONAV) {
            navigation = "nonav";
        } else if (con.navigation == Configuration.NAVIGATION_DPAD) {
            navigation = "drap";
        } else if (con.navigation == Configuration.NAVIGATION_TRACKBALL) {
            navigation = "trackball";
        } else if (con.navigation == Configuration.NAVIGATION_WHEEL) {
            navigation = "wheel";
        }

        if (navigation != null) {
            DeviceInfoInstrument.addResult(DeviceInfoConstants.NAVIGATION,
                    navigation);
        }

        String keypad = null;
        if (con.keyboard == Configuration.KEYBOARD_UNDEFINED) {
            keypad = "undefined";
        } else if (con.keyboard == Configuration.KEYBOARD_NOKEYS) {
            keypad = "nokeys";
        } else if (con.keyboard == Configuration.KEYBOARD_QWERTY) {
            keypad = "qwerty";
        } else if (con.keyboard == Configuration.KEYBOARD_12KEY) {
            keypad = "12key";
        }
        if (keypad != null) {
            DeviceInfoInstrument.addResult(DeviceInfoConstants.KEYPAD, keypad);
        }

        String[] locales = getAssets().getLocales();
        StringBuilder localeList = new StringBuilder();
        for (String s : locales) {
            if (s.length() == 0) { // default locale
                localeList.append(new Locale("en", "US").toString());
            } else {
                localeList.append(s);
            }
            localeList.append(";");
        }
        DeviceInfoInstrument.addResult(DeviceInfoConstants.LOCALES,
                localeList.toString());
    }
}
