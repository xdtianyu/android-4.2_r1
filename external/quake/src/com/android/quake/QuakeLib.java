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

package com.android.quake;

// Wrapper for native quake application

public class QuakeLib {

    public static final int KEY_PRESS = 1;
    public static final int KEY_RELEASE = 0;

    public static final int MOTION_DOWN = 0;
    public static final int MOTION_UP = 1;
    public static final int MOTION_MOVE = 2;
    public static final int MOTION_CANCEL = 3;

    // copied from Quake keys.h
    // these are the key numbers that should be passed to Key_Event
    //
    //
    // these are the key numbers that should be passed to Key_Event
    //
     public static final int K_TAB           = 9;
     public static final int K_ENTER         = 13;
     public static final int K_ESCAPE        = 27;
     public static final int K_SPACE         = 32;

     // normal keys should be passed as lowercased ascii

     public static final int K_BACKSPACE     = 127;
     public static final int K_UPARROW       = 128;
     public static final int K_DOWNARROW     = 129;
     public static final int K_LEFTARROW     = 130;
     public static final int K_RIGHTARROW    = 131;

     public static final int K_ALT           = 132;
     public static final int K_CTRL          = 133;
     public static final int K_SHIFT         = 134;
     public static final int K_F1            = 135;
     public static final int K_F2            = 136;
     public static final int K_F3            = 137;
     public static final int K_F4            = 138;
     public static final int K_F5            = 139;
     public static final int K_F6            = 140;
     public static final int K_F7            = 141;
     public static final int K_F8            = 142;
     public static final int K_F9            = 143;
     public static final int K_F10           = 144;
     public static final int K_F11           = 145;
     public static final int K_F12           = 146;
     public static final int K_INS           = 147;
     public static final int K_DEL           = 148;
     public static final int K_PGDN          = 149;
     public static final int K_PGUP          = 150;
     public static final int K_HOME          = 151;
     public static final int K_END           = 152;

     public static final int K_PAUSE         = 255;

     //
     // mouse buttons generate virtual keys
     //
     public static final int K_MOUSE1        = 200;
     public static final int K_MOUSE2        = 201;
     public static final int K_MOUSE3        = 202;

     //
     // joystick buttons
     //
     public static final int K_JOY1          = 203;
     public static final int K_JOY2          = 204;
     public static final int K_JOY3          = 205;
     public static final int K_JOY4          = 206;

     //
     // aux keys are for multi-buttoned joysticks to generate so they can use
     // the normal binding process
     //
     public static final int K_AUX1          = 207;
     public static final int K_AUX2          = 208;
     public static final int K_AUX3          = 209;
     public static final int K_AUX4          = 210;
     public static final int K_AUX5          = 211;
     public static final int K_AUX6          = 212;
     public static final int K_AUX7          = 213;
     public static final int K_AUX8          = 214;
     public static final int K_AUX9          = 215;
     public static final int K_AUX10         = 216;
     public static final int K_AUX11         = 217;
     public static final int K_AUX12         = 218;
     public static final int K_AUX13         = 219;
     public static final int K_AUX14         = 220;
     public static final int K_AUX15         = 221;
     public static final int K_AUX16         = 222;
     public static final int K_AUX17         = 223;
     public static final int K_AUX18         = 224;
     public static final int K_AUX19         = 225;
     public static final int K_AUX20         = 226;
     public static final int K_AUX21         = 227;
     public static final int K_AUX22         = 228;
     public static final int K_AUX23         = 229;
     public static final int K_AUX24         = 230;
     public static final int K_AUX25         = 231;
     public static final int K_AUX26         = 232;
     public static final int K_AUX27         = 233;
     public static final int K_AUX28         = 234;
     public static final int K_AUX29         = 235;
     public static final int K_AUX30         = 236;
     public static final int K_AUX31         = 237;
     public static final int K_AUX32         = 238;

     // JACK: Intellimouse(c) Mouse Wheel Support

     public static final int K_MWHEELUP      = 239;
     public static final int K_MWHEELDOWN    = 240;

     static {
         System.loadLibrary("quake");
     }

     public QuakeLib() {
     }

     public native boolean init();

    /**
     * Used to report key events
     * @param type KEY_PRESS or KEY_RELEASE
     * @param value the key code.
     * @return true if the event was handled.
     */
     public native boolean event(int type, int value);

    /**
     * Used to report touch-screen events
     * @param eventTime the time the event happened
     * @param action the kind of action being performed -- one of either
     * {@link #MOTION_DOWN}, {@link #MOTION_MOVE}, {@link #MOTION_UP},
     * or {@link #MOTION_CANCEL}
     * @param x the x coordinate in pixels
     * @param y the y coordinate in pixels
     * @param pressure the pressure 0..1, can be more than 1 sometimes
     * @param size the size of the area pressed (radius in X or Y)
     * @param deviceId the id of the device generating the events
     * @return true if the event was handled.
     */
     public native boolean motionEvent(long eventTime, int action,
            float x, float y, float pressure, float size, int deviceId);

     /**
      * Used to report trackball events
      * @param eventTime the time the event happened
      * @param action the kind of action being performed -- one of either
      * {@link #MOTION_DOWN}, {@link #MOTION_MOVE}, {@link #MOTION_UP},
      * or {@link #MOTION_CANCEL}
      * @param x the x motion in pixels
      * @param y the y motion in pixels
      * @return true if the event was handled.
      */
     public native boolean trackballEvent(long eventTime, int action,
             float x, float y);
    /**
     * @param width the current view width
     * @param height the current view height
     * @return true if quake is in "game" mode, false if it is in "menu" or
     * "typing" mode.
     */
     public native boolean step(int width, int height);

     /**
      * Tell Quake to quit. It will write out its config files and so forth.
      */
     public native void quit();
}
