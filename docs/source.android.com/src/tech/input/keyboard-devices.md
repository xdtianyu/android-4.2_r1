<!--
   Copyright 2011 The Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Keyboard Devices #

Android supports a variety of keyboard devices including special function
keypads (volume and power controls), compact embedded QWERTY keyboards,
and fully featured PC-style external keyboards.

This document decribes physical keyboards only.  Refer to the Android SDK
for information about soft keyboards (Input Method Editors).

## Keyboard Classification ##

An input device is classified as a keyboard if either of the following
conditions hold:

*   The input device reports the presence of any Linux key codes used on keyboards
    including `0` through `0xff` or `KEY_OK` through `KEY_MAX`.

*   The input device reports the presence of any Linux key codes used on joysticks
    and gamepads including `BTN_0` through `BTN_9`, `BTN_TRIGGER` through `BTN_DEAD`,
    or `BTN_A` through `BTN_THUMBR`.

Joysticks are currently classified as keyboards because joystick and gamepad buttons
are reported by `EV_KEY` events in the same way keyboard keys are reported.  Thus
joysticks and gamepads also make use of key map files for configuration.
Refer to the section on [Joystick Devices](/tech/input/joystick-devices.html) for
more information.

Once an input device has been classified as a keyboard, the system loads the
input device configuration file and keyboard layout for the keyboard.

The system then tries to determine additional characteristics of the device.

*   If the input device has any keys that are mapped to `KEYCODE_Q`, then the
    device is considered to have an alphabetic keypad (as opposed to numeric).
    The alphabetic keypad capability is reported in the resource `Configuration`
    object as `KEYBOARD_QWERTY`.

*   If the input device has any keys that are mapped to `KEYCODE_DPAD_UP`,
    `KEYCODE_DPAD_DOWN`, `KEYCODE_DPAD_LEFT`, `KEYCODE_DPAD_RIGHT`, and
    `KEYCODE_DPAD_CENTER` (all must be present), then the device is considered
    to have a directional keypad.
    The directional keypad capability is reported in the resource `Configuration`
    object as `NAVIGATION_DPAD`.

*   If the input device has any keys that are mapped to `KEYCODE_BUTTON_A`
    or other gamepad related keys, then the device is considered to have a gamepad.

## Keyboard Driver Requirements ##

1.  Keyboard drivers should only register key codes for the keys that they
    actually support.  Registering excess key codes may confuse the device
    classification algorithm or cause the system to incorrectly detect
    the supported keyboard capabilities of the device.

2.  Keyboard drivers should use `EV_KEY` to report key presses, using a value
    of `0` to indicate that a key is released, a value of `1` to indicate that
    a key is pressed, and a value greater than or equal to `2` to indicate that
    the key is being repeated automatically.

3.  Android performs its own keyboard repeating.  Auto-repeat functionality
    should be disabled in the driver.

4.  Keyboard drivers may optionally indicate the HID usage or low-level scan
    code by sending `EV_MSC` with `MSC_SCANCODE` and a valud indicating the usage
    or scan code when the key is pressed.  This information is not currently
    used by Android.

5.  Keyboard drivers should support setting LED states when `EV_LED` is written
    to the device.  The `hid-input` driver handles this automatically.
    At the time of this writing, Android uses `LED_CAPSLOCK`, `LED_SCROLLLOCK`,
    and `LED_NUMLOCK`.  These LEDs only need to be supported when the
    keyboard actually has the associated indicator lights.

6.  Keyboard drivers for embedded keypads (for example, using a GPIO matrix)
    should make sure to send `EV_KEY` events with a value of `0` for any keys that
    are still pressed when the device is going to sleep.  Otherwise keys might
    get stuck down and will auto-repeat forever.

## Keyboard Operation ##

The following is a brief summary of the keyboard operation on Android.

1.  The `EventHub` reads raw events from the `evdev` driver and maps Linux key codes
    (sometimes referred to as scan codes) into Android key codes using the
    keyboard's key layout map.

2.  The `InputReader` consumes the raw events and updates the meta key state.
    For example, if the left shift key is pressed or released, the reader will
    set or reset the `META_SHIFT_LEFT_ON` and `META_SHIFT_ON` bits accordingly.

3.  The `InputReader` notifies the `InputDispatcher` about the key event.

4.  The `InputDispatcher` asks the `WindowManagerPolicy` what to do with the key
    event by calling `WindowManagerPolicy.interceptKeyBeforeQueueing`.  This method
    is part of a critical path that is responsible for waking the device when
    certain keys are pressed.  The `EventHub` effectively holds a wake lock
    along this critical path to ensure that it will run to completion.

5.  If an `InputFilter` is currently in use, the `InputDispatcher` gives it a
    chance to consume or transform the key.  The `InputFilter` may be used to implement
    low-level system-wide accessibility policies.

6.  The `InputDispatcher` enqueues the key for processing on the dispatch thread.

7.  When the `InputDispatcher` dequeues the key, it gives the `WindowManagerPolicy`
    a second chance to intercept the key event by calling
    `WindowManagerPolicy.interceptKeyBeforeDispatching`.  This method handles system
    shortcuts and other functions.

8.  The `InputDispatcher` then identifies the key event target (the focused window)
    and waits for them to become ready.  Then, the `InputDispatcher` delivers the
    key event to the application.

9.  Inside the application, the key event propagates down the view hierarchy to
    the focused view for pre-IME key dispatch.

10. If the key event is not handled in the pre-IME dispatch and an IME is in use, the
    key event is delivered to the IME.

11. If the key event was not consumed by the IME, then the key event propagates
    down the view hierarchy to the focused view for standard key dispatch.

12. The application reports back to the `InputDispatcher` as to whether the key
    event was consumed.  If the event was not consumed, the `InputDispatcher`
    calls `WindowManagerPolicy.dispatchUnhandledKey` to apply "fallback" behavior.
    Depending on the fallback action, the key event dispatch cycle may be restarted
    using a different key code.  For example, if an application does not handle
    `KEYCODE_ESCAPE`, the system may redispatch the key event as `KEYCODE_BACK` instead.

## Keyboard Configuration ##

Keyboard behavior is determined by the keyboard's key layout, key character
map and input device configuration.

Refer to the following sections for more details about the files that
participate in keyboard configuration:

*   [Key Layout Files](/tech/input/key-layout-files.html)
*   [Key Character Map Files](/tech/input/key-character-map-files.html)
*   [Input Device Configuration Files](/tech/input/input-device-configuration-files.html)

### Properties ###

The following input device configuration properties are used for keyboards.

#### `keyboard.layout` ####

*Definition:* `keyboard.layout` = &lt;name&gt;

Specifies the name of the key layout file associated with the input device,
excluding the `.kl` extension.  If this file is not found, the input system
will use the default key layout instead.

Spaces in the name are converted to underscores during lookup.

Refer to the key layout file documentation for more details.

#### `keyboard.characterMap` ####

*Definition:* `keyboard.characterMap` = &lt;name&gt;

Specifies the name of the key character map file associated with the input device,
excluding the `.kcm` extension.  If this file is not found, the input system
will use the default key character map instead.

Spaces in the name are converted to underscores during lookup.

Refer to the key character map file documentation for more details.

#### `keyboard.orientationAware` ####

*Definition:* `keyboard.orientationAware` = `0` | `1`

Specifies whether the keyboard should react to display orientation changes.

*   If the value is `1`, the directional keypad keys are rotated when the
    associated display orientation changes.

*   If the value is `0`, the keyboard is immune to display orientation changes.

The default value is `0`.

Orientation awareness is used to support rotation of directional keypad keys,
such as on the Motorola Droid.  For example, when the device is rotated
clockwise 90 degrees from its natural orientation, `KEYCODE_DPAD_UP` is
remapped to produce `KEYCODE_DPAD_RIGHT` since the 'up' key ends up pointing
'right' when the device is held in that orientation.

#### `keyboard.builtIn` ####

*Definition:* `keyboard.builtIn` = `0` | `1`

Specifies whether the keyboard is the built-in (physically attached)
keyboard.

The default value is `1` if the device name ends with `-keypad`, `0` otherwise.

The built-in keyboard is always assigned a device id of `0`.  Other keyboards
that are not built-in are assigned unique non-zero device ids.

Using an id of `0` for the built-in keyboard is important for maintaining
compatibility with the `KeyCharacterMap.BUILT_IN_KEYBOARD` field, which specifies
the id of the built-in keyboard and has a value of `0`.  This field has been
deprecated in the API but older applications might still be using it.

A special-function keyboard (one whose key character map specifies a
type of `SPECIAL_FUNCTION`) will never be registered as the built-in keyboard,
regardless of the setting of this property.  This is because a special-function
keyboard is by definition not intended to be used for general purpose typing.

### Example Configurations ###

    # This is an example input device configuration file for a built-in
    # keyboard that has a DPad.

    # The keyboard is internal because it is part of the device.
    device.internal = 1

    # The keyboard is the default built-in keyboard so it should be assigned
    # an id of 0.
    keyboard.builtIn = 1

    # The keyboard includes a DPad which is mounted on the device.  As the device
    # is rotated the orientation of the DPad rotates along with it, so the DPad must
    # be aware of the display orientation.  This ensures that pressing 'up' on the
    # DPad always means 'up' from the perspective of the user, even when the entire
    # device has been rotated.
    keyboard.orientationAware = 1

### Compatibility Notes ###

Prior to Honeycomb, the keyboard input mapper did not use any configuration properties.
All keyboards were assumed to be physically attached and orientation aware.  The default
key layout and key character map was named `qwerty` instead of `Generic`.  The key
character map format was also very different and the framework did not support
PC-style full keyboards or external keyboards.

When upgrading devices to Honeycomb, make sure to create or update the necessary
configuration and key map files.

## HID Usages, Linux Key Codes and Android Key Codes ##

The system refers to keys using several different identifiers, depending on the
layer of abstraction.

For HID devices, each key has an associated HID usage.  The Linux `hid-input`
driver and related vendor and device-specific HID drivers are responsible
for parsing HID reports and mapping HID usages to Linux key codes.

As Android reads `EV_KEY` events from the Linux kernel, it translates each
Linux key code into its corresponding Android key code according to the
key layout file of the device.

When the key event is dispatched to an application, the `android.view.KeyEvent`
instance reports the Linux key code as the value of `getScanCode()` and the
Android key code as the value of `getKeyCode()`.  For the purposes of the
framework, only the value of `getKeyCode()` is important.

Note that the HID usage information is not used by Android itself or
passed to applications.

## Code Tables ##

The following tables show how HID usages, Linux key codes and Android
key codes are related to one another.

The LKC column specifies the Linux key code in hexadecimal.

The AKC column specifies the Android key code in hexadecimal.

The Notes column refers to notes that are posted after the table.

The Version column specifies the first version of the Android platform
to have included this key in its default key map.  Multiple rows are
shown in cases where the default key map has changed between versions.
The oldest version indicated is 1.6.

*   In Gingerbread (2.3) and earlier releases, the default key map was
    `qwerty.kl`. This key map was only intended for use with the Android
    Emulator and was not intended to be used to support arbitrary
    external keyboards.  Nevertheless, a few OEMs added Bluetooth
    keyboard support to the platform and relied on `qwerty.kl` to
    provide the necessary keyboard mappings.  Consequently these
    older mappings may be of interest to OEMs who are building
    peripherals for these particular devices.  Note that the mappings
    are substantially different from the current ones, particularly
    with respect to the treatment of the `HOME` key.  It is recommended
    that all new peripherals be developed according to the Honeycomb or more
    recent key maps (ie. standard HID).

*   As of Honeycomb (3.0), the default key map is `Generic.kl`.
    This key map was designed to support full PC style keyboards.
    Most functionality of standard HID keyboards should just work out
    of the box.

The key code mapping may vary across versions of the Linux kernel and Android.
When changes are known to have occurred in the Android default key maps,
they are indicated in the version column.

Device-specific HID drivers and key maps may apply different mappings
than are indicated here.

### HID Keyboard and Keypad Page (0x07) ###

| HID Usage   | HID Usage Name                   | LKC    | Linux Key Code Name              | Version | AKC    | Android Key Code Name            | Notes |
| ----------- | -------------------------------- | ------ | -------------------------------- | ------- | ------ | -------------------------------- | ----- |
| 0x07 0x0001 | Keyboard Error Roll Over         |        |                                  |         |        |                                  |       |
| 0x07 0x0002 | Keyboard POST Fail               |        |                                  |         |        |                                  |       |
| 0x07 0x0003 | Keyboard Error Undefined         |        |                                  |         |        |                                  |       |
| 0x07 0x0004 | Keyboard a and A                 | 0x001e | KEY_A                            | 1.6     | 0x001d | KEYCODE_A                        | 1     |
| 0x07 0x0005 | Keyboard b and B                 | 0x0030 | KEY_B                            | 1.6     | 0x001e | KEYCODE_B                        | 1     |
| 0x07 0x0006 | Keyboard c and C                 | 0x002e | KEY_C                            | 1.6     | 0x001f | KEYCODE_C                        | 1     |
| 0x07 0x0007 | Keyboard d and D                 | 0x0020 | KEY_D                            | 1.6     | 0x0020 | KEYCODE_D                        | 1     |
| 0x07 0x0008 | Keyboard e and E                 | 0x0012 | KEY_E                            | 1.6     | 0x0021 | KEYCODE_E                        | 1     |
| 0x07 0x0009 | Keyboard f and F                 | 0x0021 | KEY_F                            | 1.6     | 0x0022 | KEYCODE_F                        | 1     |
| 0x07 0x000a | Keyboard g and G                 | 0x0022 | KEY_G                            | 1.6     | 0x0023 | KEYCODE_G                        | 1     |
| 0x07 0x000b | Keyboard h and H                 | 0x0023 | KEY_H                            | 1.6     | 0x0024 | KEYCODE_H                        | 1     |
| 0x07 0x000c | Keyboard i and I                 | 0x0017 | KEY_I                            | 1.6     | 0x0025 | KEYCODE_I                        | 1     |
| 0x07 0x000d | Keyboard j and J                 | 0x0024 | KEY_J                            | 1.6     | 0x0026 | KEYCODE_J                        | 1     |
| 0x07 0x000e | Keyboard k and K                 | 0x0025 | KEY_K                            | 1.6     | 0x0027 | KEYCODE_K                        | 1     |
| 0x07 0x000f | Keyboard l and L                 | 0x0026 | KEY_L                            | 1.6     | 0x0028 | KEYCODE_L                        | 1     |
| 0x07 0x0010 | Keyboard m and M                 | 0x0032 | KEY_M                            | 1.6     | 0x0029 | KEYCODE_M                        | 1     |
| 0x07 0x0011 | Keyboard n and N                 | 0x0031 | KEY_N                            | 1.6     | 0x002a | KEYCODE_N                        | 1     |
| 0x07 0x0012 | Keyboard o and O                 | 0x0018 | KEY_O                            | 1.6     | 0x002b | KEYCODE_O                        | 1     |
| 0x07 0x0013 | Keyboard p and P                 | 0x0019 | KEY_P                            | 1.6     | 0x002c | KEYCODE_P                        | 1     |
| 0x07 0x0014 | Keyboard q and Q                 | 0x0010 | KEY_Q                            | 1.6     | 0x002d | KEYCODE_Q                        | 1     |
| 0x07 0x0015 | Keyboard r and R                 | 0x0013 | KEY_R                            | 1.6     | 0x002e | KEYCODE_R                        | 1     |
| 0x07 0x0016 | Keyboard s and S                 | 0x001f | KEY_S                            | 1.6     | 0x002f | KEYCODE_S                        | 1     |
| 0x07 0x0017 | Keyboard t and T                 | 0x0014 | KEY_T                            | 1.6     | 0x0030 | KEYCODE_T                        | 1     |
| 0x07 0x0018 | Keyboard u and U                 | 0x0016 | KEY_U                            | 1.6     | 0x0031 | KEYCODE_U                        | 1     |
| 0x07 0x0019 | Keyboard v and V                 | 0x002f | KEY_V                            | 1.6     | 0x0032 | KEYCODE_V                        | 1     |
| 0x07 0x001a | Keyboard w and W                 | 0x0011 | KEY_W                            | 1.6     | 0x0033 | KEYCODE_W                        | 1     |
| 0x07 0x001b | Keyboard x and X                 | 0x002d | KEY_X                            | 1.6     | 0x0034 | KEYCODE_X                        | 1     |
| 0x07 0x001c | Keyboard y and Y                 | 0x0015 | KEY_Y                            | 1.6     | 0x0035 | KEYCODE_Y                        | 1     |
| 0x07 0x001d | Keyboard z and Z                 | 0x002c | KEY_Z                            | 1.6     | 0x0036 | KEYCODE_Z                        | 1     |
| 0x07 0x001e | Keyboard 1 and !                 | 0x0002 | KEY_1                            | 1.6     | 0x0008 | KEYCODE_1                        | 1     |
| 0x07 0x001f | Keyboard 2 and @                 | 0x0003 | KEY_2                            | 1.6     | 0x0009 | KEYCODE_2                        | 1     |
| 0x07 0x0020 | Keyboard 3 and #                 | 0x0004 | KEY_3                            | 1.6     | 0x000a | KEYCODE_3                        | 1     |
| 0x07 0x0021 | Keyboard 4 and $                 | 0x0005 | KEY_4                            | 1.6     | 0x000b | KEYCODE_4                        | 1     |
| 0x07 0x0022 | Keyboard 5 and %                 | 0x0006 | KEY_5                            | 1.6     | 0x000c | KEYCODE_5                        | 1     |
| 0x07 0x0023 | Keyboard 6 and ^                 | 0x0007 | KEY_6                            | 1.6     | 0x000d | KEYCODE_6                        | 1     |
| 0x07 0x0024 | Keyboard 7 and &                 | 0x0008 | KEY_7                            | 1.6     | 0x000e | KEYCODE_7                        | 1     |
| 0x07 0x0025 | Keyboard 8 and *                 | 0x0009 | KEY_8                            | 1.6     | 0x000f | KEYCODE_8                        | 1     |
| 0x07 0x0026 | Keyboard 9 and (                 | 0x000a | KEY_9                            | 1.6     | 0x0010 | KEYCODE_9                        | 1     |
| 0x07 0x0027 | Keyboard 0 and )                 | 0x000b | KEY_0                            | 1.6     | 0x0007 | KEYCODE_0                        | 1     |
| 0x07 0x0028 | Keyboard Return (ENTER)          | 0x001c | KEY_ENTER                        | 1.6     | 0x0042 | KEYCODE_ENTER                    | 1     |
| 0x07 0x0029 | Keyboard ESCAPE                  | 0x0001 | KEY_ESC                          | 3.0     | 0x006f | KEYCODE_ESCAPE                   |       |
| ""          | ""                               | ""     | ""                               | 2.3     | 0x0004 | KEYCODE_BACK                     |       |
| 0x07 0x002a | Keyboard DELETE (Backspace)      | 0x000e | KEY_BACKSPACE                    | 1.6     | 0x0043 | KEYCODE_DEL                      |       |
| 0x07 0x002b | Keyboard Tab                     | 0x000f | KEY_TAB                          | 1.6     | 0x003d | KEYCODE_TAB                      |       |
| 0x07 0x002c | Keyboard Spacebar                | 0x0039 | KEY_SPACE                        | 1.6     | 0x003e | KEYCODE_SPACE                    |       |
| 0x07 0x002d | Keyboard - and _                 | 0x000c | KEY_MINUS                        | 1.6     | 0x0045 | KEYCODE_MINUS                    | 1     |
| 0x07 0x002e | Keyboard = and +                 | 0x000d | KEY_EQUAL                        | 1.6     | 0x0046 | KEYCODE_EQUALS                   | 1     |
| 0x07 0x002f | Keyboard \[ and \{               | 0x001a | KEY_LEFTBRACE                    | 1.6     | 0x0047 | KEYCODE_LEFT_BRACKET             | 1     |
| 0x07 0x0030 | Keyboard \] and \}               | 0x001b | KEY_RIGHTBRACE                   | 1.6     | 0x0048 | KEYCODE_RIGHT_BRACKET            | 1     |
| 0x07 0x0031 | Keyboard \\ and &#124;           | 0x002b | KEY_BACKSLASH                    | 1.6     | 0x0049 | KEYCODE_BACKSLASH                | 1     |
| 0x07 0x0032 | Keyboard Non-US # and ~          | 0x002b | KEY_BACKSLASH                    | 1.6     | 0x0049 | KEYCODE_BACKSLASH                | 1     |
| 0x07 0x0033 | Keyboard ; and :                 | 0x0027 | KEY_SEMICOLON                    | 1.6     | 0x004a | KEYCODE_SEMICOLON                | 1     |
| 0x07 0x0034 | Keyboard ' and "                 | 0x0028 | KEY_APOSTROPHE                   | 1.6     | 0x004b | KEYCODE_APOSTROPHE               | 1     |
| 0x07 0x0035 | Keyboard \` and ~                | 0x0029 | KEY_GRAVE                        | 3.0     | 0x0044 | KEYCODE_GRAVE                    | 1     |
| 0x07 0x0036 | Keyboard , and <                 | 0x0033 | KEY_COMMA                        | 1.6     | 0x0037 | KEYCODE_COMMA                    | 1     |
| 0x07 0x0037 | Keyboard . and >                 | 0x0034 | KEY_DOT                          | 1.6     | 0x0038 | KEYCODE_PERIOD                   | 1     |
| 0x07 0x0038 | Keyboard / and ?                 | 0x0035 | KEY_SLASH                        | 1.6     | 0x004c | KEYCODE_SLASH                    | 1     |
| 0x07 0x0039 | Keyboard Caps Lock               | 0x003a | KEY_CAPSLOCK                     | 3.0     | 0x0073 | KEYCODE_CAPS_LOCK                |       |
| 0x07 0x003a | Keyboard F1                      | 0x003b | KEY_F1                           | 3.0     | 0x0083 | KEYCODE_F1                       |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0052 | KEYCODE_MENU                     |       |
| 0x07 0x003b | Keyboard F2                      | 0x003c | KEY_F2                           | 3.0     | 0x0084 | KEYCODE_F2                       |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0002 | KEYCODE_SOFT_RIGHT               |       |
| 0x07 0x003c | Keyboard F3                      | 0x003d | KEY_F3                           | 3.0     | 0x0085 | KEYCODE_F3                       |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0005 | KEYCODE_CALL                     |       |
| 0x07 0x003d | Keyboard F4                      | 0x003e | KEY_F4                           | 3.0     | 0x0086 | KEYCODE_F4                       |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0006 | KEYCODE_ENDCALL                  |       |
| 0x07 0x003e | Keyboard F5                      | 0x003f | KEY_F5                           | 3.0     | 0x0087 | KEYCODE_F5                       |       |
| 0x07 0x003f | Keyboard F6                      | 0x0040 | KEY_F6                           | 3.0     | 0x0088 | KEYCODE_F6                       |       |
| 0x07 0x0040 | Keyboard F7                      | 0x0041 | KEY_F7                           | 3.0     | 0x0089 | KEYCODE_F7                       |       |
| 0x07 0x0041 | Keyboard F8                      | 0x0042 | KEY_F8                           | 3.0     | 0x008a | KEYCODE_F8                       |       |
| 0x07 0x0042 | Keyboard F9                      | 0x0043 | KEY_F9                           | 3.0     | 0x008b | KEYCODE_F9                       |       |
| 0x07 0x0043 | Keyboard F10                     | 0x0044 | KEY_F10                          | 3.0     | 0x008c | KEYCODE_F10                      |       |
| ""          | ""                               | ""     | ""                               | 2.3     | 0x0052 | KEYCODE_MENU                     |       |
| 0x07 0x0044 | Keyboard F11                     | 0x0057 | KEY_F11                          | 3.0     | 0x008d | KEYCODE_F11                      |       |
| 0x07 0x0045 | Keyboard F12                     | 0x0058 | KEY_F12                          | 3.0     | 0x008e | KEYCODE_F12                      |       |
| 0x07 0x0046 | Keyboard Print Screen            | 0x0063 | KEY_SYSRQ                        | 3.0     | 0x0078 | KEYCODE_SYSRQ                    |       |
| 0x07 0x0047 | Keyboard Scroll Lock             | 0x0046 | KEY_SCROLLLOCK                   | 3.0     | 0x0074 | KEYCODE_SCROLL_LOCK              |       |
| 0x07 0x0048 | Keyboard Pause                   | 0x0077 | KEY_PAUSE                        | 3.0     | 0x0079 | KEYCODE_BREAK                    |       |
| 0x07 0x0049 | Keyboard Insert                  | 0x006e | KEY_INSERT                       | 3.0     | 0x007c | KEYCODE_INSERT                   |       |
| 0x07 0x004a | Keyboard Home                    | 0x0066 | KEY_HOME                         | 3.0     | 0x007a | KEYCODE_MOVE_HOME                |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0003 | KEYCODE_HOME                     |       |
| 0x07 0x004b | Keyboard Page Up                 | 0x0068 | KEY_PAGEUP                       | 3.0     | 0x005c | KEYCODE_PAGE_UP                  |       |
| 0x07 0x004c | Keyboard Delete Forward          | 0x006f | KEY_DELETE                       | 3.0     | 0x0070 | KEYCODE_FORWARD_DEL              |       |
| 0x07 0x004d | Keyboard End                     | 0x006b | KEY_END                          | 3.0     | 0x007b | KEYCODE_MOVE_END                 |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0006 | KEYCODE_ENDCALL                  |       |
| 0x07 0x004e | Keyboard Page Down               | 0x006d | KEY_PAGEDOWN                     | 3.0     | 0x005d | KEYCODE_PAGE_DOWN                |       |
| 0x07 0x004f | Keyboard Right Arrow             | 0x006a | KEY_RIGHT                        | 1.6     | 0x0016 | KEYCODE_DPAD_RIGHT               |       |
| 0x07 0x0050 | Keyboard Left Arrow              | 0x0069 | KEY_LEFT                         | 1.6     | 0x0015 | KEYCODE_DPAD_LEFT                |       |
| 0x07 0x0051 | Keyboard Down Arrow              | 0x006c | KEY_DOWN                         | 1.6     | 0x0014 | KEYCODE_DPAD_DOWN                |       |
| 0x07 0x0052 | Keyboard Up Arrow                | 0x0067 | KEY_UP                           | 1.6     | 0x0013 | KEYCODE_DPAD_UP                  |       |
| 0x07 0x0053 | Keyboard Num Lock and Clear      | 0x0045 | KEY_NUMLOCK                      | 3.0     | 0x008f | KEYCODE_NUM_LOCK                 |       |
| 0x07 0x0054 | Keypad /                         | 0x0062 | KEY_KPSLASH                      | 3.0     | 0x009a | KEYCODE_NUMPAD_DIVIDE            |       |
| 0x07 0x0055 | Keypad *                         | 0x0037 | KEY_KPASTERISK                   | 3.0     | 0x009b | KEYCODE_NUMPAD_MULTIPLY          |       |
| 0x07 0x0056 | Keypad -                         | 0x004a | KEY_KPMINUS                      | 3.0     | 0x009c | KEYCODE_NUMPAD_SUBTRACT          |       |
| 0x07 0x0057 | Keypad +                         | 0x004e | KEY_KPPLUS                       | 3.0     | 0x009d | KEYCODE_NUMPAD_ADD               |       |
| 0x07 0x0058 | Keypad ENTER                     | 0x0060 | KEY_KPENTER                      | 3.0     | 0x00a0 | KEYCODE_NUMPAD_ENTER             |       |
| 0x07 0x0059 | Keypad 1 and End                 | 0x004f | KEY_KP1                          | 3.0     | 0x0091 | KEYCODE_NUMPAD_1                 |       |
| 0x07 0x005a | Keypad 2 and Down Arrow          | 0x0050 | KEY_KP2                          | 3.0     | 0x0092 | KEYCODE_NUMPAD_2                 |       |
| 0x07 0x005b | Keypad 3 and PageDn              | 0x0051 | KEY_KP3                          | 3.0     | 0x0093 | KEYCODE_NUMPAD_3                 |       |
| 0x07 0x005c | Keypad 4 and Left Arrow          | 0x004b | KEY_KP4                          | 3.0     | 0x0094 | KEYCODE_NUMPAD_4                 |       |
| 0x07 0x005d | Keypad 5                         | 0x004c | KEY_KP5                          | 3.0     | 0x0095 | KEYCODE_NUMPAD_5                 |       |
| 0x07 0x005e | Keypad 6 and Right Arrow         | 0x004d | KEY_KP6                          | 3.0     | 0x0096 | KEYCODE_NUMPAD_6                 |       |
| 0x07 0x005f | Keypad 7 and Home                | 0x0047 | KEY_KP7                          | 3.0     | 0x0097 | KEYCODE_NUMPAD_7                 |       |
| 0x07 0x0060 | Keypad 8 and Up Arrow            | 0x0048 | KEY_KP8                          | 3.0     | 0x0098 | KEYCODE_NUMPAD_8                 |       |
| 0x07 0x0061 | Keypad 9 and Page Up             | 0x0049 | KEY_KP9                          | 3.0     | 0x0099 | KEYCODE_NUMPAD_9                 |       |
| 0x07 0x0062 | Keypad 0 and Insert              | 0x0052 | KEY_KP0                          | 3.0     | 0x0090 | KEYCODE_NUMPAD_0                 |       |
| 0x07 0x0063 | Keypad . and Delete              | 0x0053 | KEY_KPDOT                        | 3.0     | 0x009e | KEYCODE_NUMPAD_DOT               |       |
| 0x07 0x0064 | Keyboard Non-US \\ and &#124;    | 0x0056 | KEY_102ND                        | 4.0     | 0x0049 | KEYCODE_BACKSLASH                | 1     |
| 0x07 0x0065 | Keyboard Application             | 0x007f | KEY_COMPOSE                      | 3.0     | 0x0052 | KEYCODE_MENU                     |       |
| ""          | ""                               | ""     | ""                               | 1.6     | 0x0054 | KEYCODE_SEARCH                   |       |
| 0x07 0x0066 | Keyboard Power                   | 0x0074 | KEY_POWER                        | 1.6     | 0x001a | KEYCODE_POWER                    |       |
| 0x07 0x0067 | Keypad =                         | 0x0075 | KEY_KPEQUAL                      | 3.0     | 0x00a1 | KEYCODE_NUMPAD_EQUALS            |       |
| 0x07 0x0068 | Keyboard F13                     | 0x00b7 | KEY_F13                          |         |        |                                  |       |
| 0x07 0x0069 | Keyboard F14                     | 0x00b8 | KEY_F14                          |         |        |                                  |       |
| 0x07 0x006a | Keyboard F15                     | 0x00b9 | KEY_F15                          |         |        |                                  |       |
| 0x07 0x006b | Keyboard F16                     | 0x00ba | KEY_F16                          |         |        |                                  |       |
| 0x07 0x006c | Keyboard F17                     | 0x00bb | KEY_F17                          |         |        |                                  |       |
| 0x07 0x006d | Keyboard F18                     | 0x00bc | KEY_F18                          |         |        |                                  |       |
| 0x07 0x006e | Keyboard F19                     | 0x00bd | KEY_F19                          |         |        |                                  |       |
| 0x07 0x006f | Keyboard F20                     | 0x00be | KEY_F20                          |         |        |                                  |       |
| 0x07 0x0070 | Keyboard F21                     | 0x00bf | KEY_F21                          |         |        |                                  |       |
| 0x07 0x0071 | Keyboard F22                     | 0x00c0 | KEY_F22                          |         |        |                                  |       |
| 0x07 0x0072 | Keyboard F23                     | 0x00c1 | KEY_F23                          |         |        |                                  |       |
| 0x07 0x0073 | Keyboard F24                     | 0x00c2 | KEY_F24                          |         |        |                                  |       |
| 0x07 0x0074 | Keyboard Execute                 | 0x0086 | KEY_OPEN                         |         |        |                                  |       |
| 0x07 0x0075 | Keyboard Help                    | 0x008a | KEY_HELP                         |         |        |                                  |       |
| 0x07 0x0076 | Keyboard Menu                    | 0x0082 | KEY_PROPS                        |         |        |                                  |       |
| 0x07 0x0077 | Keyboard Select                  | 0x0084 | KEY_FRONT                        |         |        |                                  |       |
| 0x07 0x0078 | Keyboard Stop                    | 0x0080 | KEY_STOP                         | 3.0     | 0x0056 | KEYCODE_MEDIA_STOP               |       |
| 0x07 0x0079 | Keyboard Again                   | 0x0081 | KEY_AGAIN                        |         |        |                                  |       |
| 0x07 0x007a | Keyboard Undo                    | 0x0083 | KEY_UNDO                         |         |        |                                  |       |
| 0x07 0x007b | Keyboard Cut                     | 0x0089 | KEY_CUT                          |         |        |                                  |       |
| 0x07 0x007c | Keyboard Copy                    | 0x0085 | KEY_COPY                         |         |        |                                  |       |
| 0x07 0x007d | Keyboard Paste                   | 0x0087 | KEY_PASTE                        |         |        |                                  |       |
| 0x07 0x007e | Keyboard Find                    | 0x0088 | KEY_FIND                         |         |        |                                  |       |
| 0x07 0x007f | Keyboard Mute                    | 0x0071 | KEY_MUTE                         | 3.0     | 0x00a4 | KEYCODE_VOLUME_MUTE              |       |
| 0x07 0x0080 | Keyboard Volume Up               | 0x0073 | KEY_VOLUMEUP                     | 1.6     | 0x0018 | KEYCODE_VOLUME_UP                |       |
| 0x07 0x0081 | Keyboard Volume Down             | 0x0072 | KEY_VOLUMEDOWN                   | 1.6     | 0x0019 | KEYCODE_VOLUME_DOWN              |       |
| 0x07 0x0082 | Keyboard Locking Caps Lock       |        |                                  |         |        |                                  |       |
| 0x07 0x0083 | Keyboard Locking Num Lock        |        |                                  |         |        |                                  |       |
| 0x07 0x0084 | Keyboard Locking Scroll Lock     |        |                                  |         |        |                                  |       |
| 0x07 0x0085 | Keypad Comma                     | 0x0079 | KEY_KPCOMMA                      | 3.0     | 0x009f | KEYCODE_NUMPAD_COMMA             |       |
| 0x07 0x0086 | Keypad Equal Sign                |        |                                  |         |        |                                  |       |
| 0x07 0x0087 | Keyboard International1          | 0x0059 | KEY_RO                           |         |        |                                  |       |
| 0x07 0x0088 | Keyboard International2          | 0x005d | KEY_KATAKANAHIRAGANA             |         |        |                                  |       |
| 0x07 0x0089 | Keyboard International3          | 0x007c | KEY_YEN                          |         |        |                                  |       |
| 0x07 0x008a | Keyboard International4          | 0x005c | KEY_HENKAN                       |         |        |                                  |       |
| 0x07 0x008b | Keyboard International5          | 0x005e | KEY_MUHENKAN                     |         |        |                                  |       |
| 0x07 0x008c | Keyboard International6          | 0x005f | KEY_KPJPCOMMA                    |         |        |                                  |       |
| 0x07 0x008d | Keyboard International7          |        |                                  |         |        |                                  |       |
| 0x07 0x008e | Keyboard International8          |        |                                  |         |        |                                  |       |
| 0x07 0x008f | Keyboard International9          |        |                                  |         |        |                                  |       |
| 0x07 0x0090 | Keyboard LANG1                   | 0x007a | KEY_HANGEUL                      |         |        |                                  |       |
| 0x07 0x0091 | Keyboard LANG2                   | 0x007b | KEY_HANJA                        |         |        |                                  |       |
| 0x07 0x0092 | Keyboard LANG3                   | 0x005a | KEY_KATAKANA                     |         |        |                                  |       |
| 0x07 0x0093 | Keyboard LANG4                   | 0x005b | KEY_HIRAGANA                     |         |        |                                  |       |
| 0x07 0x0094 | Keyboard LANG5                   | 0x0055 | KEY_ZENKAKUHANKAKU               |         |        |                                  |       |
| 0x07 0x0095 | Keyboard LANG6                   |        |                                  |         |        |                                  |       |
| 0x07 0x0096 | Keyboard LANG7                   |        |                                  |         |        |                                  |       |
| 0x07 0x0097 | Keyboard LANG8                   |        |                                  |         |        |                                  |       |
| 0x07 0x0098 | Keyboard LANG9                   |        |                                  |         |        |                                  |       |
| 0x07 0x0099 | Keyboard Alternate Erase         |        |                                  |         |        |                                  |       |
| 0x07 0x009a | Keyboard SysReq/Attention        |        |                                  |         |        |                                  |       |
| 0x07 0x009b | Keyboard Cancel                  |        |                                  |         |        |                                  |       |
| 0x07 0x009c | Keyboard Clear                   |        |                                  |         |        |                                  |       |
| 0x07 0x009d | Keyboard Prior                   |        |                                  |         |        |                                  |       |
| 0x07 0x009e | Keyboard Return                  |        |                                  |         |        |                                  |       |
| 0x07 0x009f | Keyboard Separator               |        |                                  |         |        |                                  |       |
| 0x07 0x00a0 | Keyboard Out                     |        |                                  |         |        |                                  |       |
| 0x07 0x00a1 | Keyboard Oper                    |        |                                  |         |        |                                  |       |
| 0x07 0x00a2 | Keyboard Clear/Again             |        |                                  |         |        |                                  |       |
| 0x07 0x00a3 | Keyboard CrSel/Props             |        |                                  |         |        |                                  |       |
| 0x07 0x00a4 | Keyboard ExSel                   |        |                                  |         |        |                                  |       |
| 0x07 0x00b0 | Keypad 00                        |        |                                  |         |        |                                  |       |
| 0x07 0x00b1 | Keypad 000                       |        |                                  |         |        |                                  |       |
| 0x07 0x00b2 | Thousands Separator              |        |                                  |         |        |                                  |       |
| 0x07 0x00b3 | Decimal Separator                |        |                                  |         |        |                                  |       |
| 0x07 0x00b4 | Currency Unit                    |        |                                  |         |        |                                  |       |
| 0x07 0x00b5 | Currency Sub-unit                |        |                                  |         |        |                                  |       |
| 0x07 0x00b6 | Keypad (                         | 0x00b3 | KEY_KPLEFTPAREN                  | 3.0     | 0x00a2 | KEYCODE_NUMPAD_LEFT_PAREN        |       |
| 0x07 0x00b7 | Keypad )                         | 0x00b4 | KEY_KPRIGHTPAREN                 | 3.0     | 0x00a3 | KEYCODE_NUMPAD_RIGHT_PAREN       |       |
| 0x07 0x00b8 | Keypad \{                        |        |                                  |         |        |                                  |       |
| 0x07 0x00b9 | Keypad \}                        |        |                                  |         |        |                                  |       |
| 0x07 0x00ba | Keypad Tab                       |        |                                  |         |        |                                  |       |
| 0x07 0x00bb | Keypad Backspace                 |        |                                  |         |        |                                  |       |
| 0x07 0x00bc | Keypad A                         |        |                                  |         |        |                                  |       |
| 0x07 0x00bd | Keypad B                         |        |                                  |         |        |                                  |       |
| 0x07 0x00be | Keypad C                         |        |                                  |         |        |                                  |       |
| 0x07 0x00bf | Keypad D                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c0 | Keypad E                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c1 | Keypad F                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c2 | Keypad XOR                       |        |                                  |         |        |                                  |       |
| 0x07 0x00c3 | Keypad ^                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c4 | Keypad %                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c5 | Keypad <                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c6 | Keypad >                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c7 | Keypad &                         |        |                                  |         |        |                                  |       |
| 0x07 0x00c8 | Keypad &&                        |        |                                  |         |        |                                  |       |
| 0x07 0x00c9 | Keypad &#124;                    |        |                                  |         |        |                                  |       |
| 0x07 0x00ca | Keypad &#124;&#124;              |        |                                  |         |        |                                  |       |
| 0x07 0x00cb | Keypad :                         |        |                                  |         |        |                                  |       |
| 0x07 0x00cc | Keypad #                         |        |                                  |         |        |                                  |       |
| 0x07 0x00cd | Keypad Space                     |        |                                  |         |        |                                  |       |
| 0x07 0x00ce | Keypad @                         |        |                                  |         |        |                                  |       |
| 0x07 0x00cf | Keypad !                         |        |                                  |         |        |                                  |       |
| 0x07 0x00d0 | Keypad Memory Store              |        |                                  |         |        |                                  |       |
| 0x07 0x00d1 | Keypad Memory Recall             |        |                                  |         |        |                                  |       |
| 0x07 0x00d2 | Keypad Memory Clear              |        |                                  |         |        |                                  |       |
| 0x07 0x00d3 | Keypad Memory Add                |        |                                  |         |        |                                  |       |
| 0x07 0x00d4 | Keypad Memory Subtract           |        |                                  |         |        |                                  |       |
| 0x07 0x00d5 | Keypad Memory Multiply           |        |                                  |         |        |                                  |       |
| 0x07 0x00d6 | Keypad Memory Divide             |        |                                  |         |        |                                  |       |
| 0x07 0x00d7 | Keypad +/-                       |        |                                  |         |        |                                  |       |
| 0x07 0x00d8 | Keypad Clear                     |        |                                  |         |        |                                  |       |
| 0x07 0x00d9 | Keypad Clear Entry               |        |                                  |         |        |                                  |       |
| 0x07 0x00da | Keypad Binary                    |        |                                  |         |        |                                  |       |
| 0x07 0x00db | Keypad Octal                     |        |                                  |         |        |                                  |       |
| 0x07 0x00dc | Keypad Decimal                   |        |                                  |         |        |                                  |       |
| 0x07 0x00dd | Keypad Hexadecimal               |        |                                  |         |        |                                  |       |
| 0x07 0x00e0 | Keyboard Left Control            | 0x001d | KEY_LEFTCTRL                     | 3.0     | 0x0071 | KEYCODE_CTRL_LEFT                |       |
| 0x07 0x00e1 | Keyboard Left Shift              | 0x002a | KEY_LEFTSHIFT                    | 1.6     | 0x003b | KEYCODE_SHIFT_LEFT               |       |
| 0x07 0x00e2 | Keyboard Left Alt                | 0x0038 | KEY_LEFTALT                      | 1.6     | 0x0039 | KEYCODE_ALT_LEFT                 |       |
| 0x07 0x00e3 | Keyboard Left GUI                | 0x007d | KEY_LEFTMETA                     | 3.0     | 0x0075 | KEYCODE_META_LEFT                |       |
| 0x07 0x00e4 | Keyboard Right Control           | 0x0061 | KEY_RIGHTCTRL                    | 3.0     | 0x0072 | KEYCODE_CTRL_RIGHT               |       |
| 0x07 0x00e5 | Keyboard Right Shift             | 0x0036 | KEY_RIGHTSHIFT                   | 1.6     | 0x003c | KEYCODE_SHIFT_RIGHT              |       |
| 0x07 0x00e6 | Keyboard Right Alt               | 0x0064 | KEY_RIGHTALT                     | 1.6     | 0x003a | KEYCODE_ALT_RIGHT                |       |
| 0x07 0x00e7 | Keyboard Right GUI               | 0x007e | KEY_RIGHTMETA                    | 3.0     | 0x0076 | KEYCODE_META_RIGHT               |       |
| 0x07 0x00e8 |                                  | 0x00a4 | KEY_PLAYPAUSE                    | 3.0     | 0x0055 | KEYCODE_MEDIA_PLAY_PAUSE         |       |
| 0x07 0x00e9 |                                  | 0x00a6 | KEY_STOPCD                       | 3.0     | 0x0056 | KEYCODE_MEDIA_STOP               |       |
| 0x07 0x00ea |                                  | 0x00a5 | KEY_PREVIOUSSONG                 | 3.0     | 0x0058 | KEYCODE_MEDIA_PREVIOUS           |       |
| 0x07 0x00eb |                                  | 0x00a3 | KEY_NEXTSONG                     | 3.0     | 0x0057 | KEYCODE_MEDIA_NEXT               |       |
| 0x07 0x00ec |                                  | 0x00a1 | KEY_EJECTCD                      | 3.0     | 0x0081 | KEYCODE_MEDIA_EJECT              |       |
| 0x07 0x00ed |                                  | 0x0073 | KEY_VOLUMEUP                     | 1.6     | 0x0018 | KEYCODE_VOLUME_UP                |       |
| 0x07 0x00ee |                                  | 0x0072 | KEY_VOLUMEDOWN                   | 1.6     | 0x0019 | KEYCODE_VOLUME_DOWN              |       |
| 0x07 0x00ef |                                  | 0x0071 | KEY_MUTE                         | 3.0     | 0x00a4 | KEYCODE_VOLUME_MUTE              |       |
| 0x07 0x00f0 |                                  | 0x0096 | KEY_WWW                          | 1.6     | 0x0040 | KEYCODE_EXPLORER                 |       |
| 0x07 0x00f1 |                                  | 0x009e | KEY_BACK                         | 1.6     | 0x0004 | KEYCODE_BACK                     |       |
| 0x07 0x00f2 |                                  | 0x009f | KEY_FORWARD                      | 3.0     | 0x007d | KEYCODE_FORWARD                  |       |
| 0x07 0x00f3 |                                  | 0x0080 | KEY_STOP                         | 3.0     | 0x0056 | KEYCODE_MEDIA_STOP               |       |
| 0x07 0x00f4 |                                  | 0x0088 | KEY_FIND                         |         |        |                                  |       |
| 0x07 0x00f5 |                                  | 0x00b1 | KEY_SCROLLUP                     | 3.0     | 0x005c | KEYCODE_PAGE_UP                  |       |
| 0x07 0x00f6 |                                  | 0x00b2 | KEY_SCROLLDOWN                   | 3.0     | 0x005d | KEYCODE_PAGE_DOWN                |       |
| 0x07 0x00f7 |                                  | 0x00b0 | KEY_EDIT                         |         |        |                                  |       |
| 0x07 0x00f8 |                                  | 0x008e | KEY_SLEEP                        |         |        |                                  |       |
| 0x07 0x00f9 |                                  | 0x0098 | KEY_COFFEE                       | 4.0     | 0x001a | KEYCODE_POWER                    |       |
| 0x07 0x00fa |                                  | 0x00ad | KEY_REFRESH                      |         |        |                                  |       |
| 0x07 0x00fb |                                  | 0x008c | KEY_CALC                         | 4.0.3   | 0x00d2 | KEYCODE_CALCULATOR               |       |

### HID Generic Desktop Page (0x01) ###

| HID Usage   | HID Usage Name                   | LKC    | Linux Key Code Name              | Version | AKC    | Android Key Code Name            | Notes |
| ----------- | -------------------------------- | ------ | -------------------------------- | ------- | ------ | -------------------------------- | ----- |
| 0x01 0x0081 | System Power Down                | 0x0074 | KEY_POWER                        | 1.6     | 0x001a | KEYCODE_POWER                    |       |
| 0x01 0x0082 | System Sleep                     | 0x008e | KEY_SLEEP                        | 4.0     | 0x001a | KEYCODE_POWER                    |       |
| 0x01 0x0083 | System Wake Up                   | 0x008f | KEY_WAKEUP                       | 4.0     | 0x001a | KEYCODE_POWER                    |       |
| 0x01 0x0084 | System Context Menu              |        |                                  |         |        |                                  |       |
| 0x01 0x0085 | System Main Menu                 |        |                                  |         |        |                                  |       |
| 0x01 0x0086 | System App Menu                  |        |                                  |         |        |                                  |       |
| 0x01 0x0087 | System Menu Help                 |        |                                  |         |        |                                  |       |
| 0x01 0x0088 | System Menu Exit                 |        |                                  |         |        |                                  |       |
| 0x01 0x0089 | System Menu Select               |        |                                  |         |        |                                  |       |
| 0x01 0x008a | System Menu Right                |        |                                  |         |        |                                  |       |
| 0x01 0x008b | System Menu Left                 |        |                                  |         |        |                                  |       |
| 0x01 0x008c | System Menu Up                   |        |                                  |         |        |                                  |       |
| 0x01 0x008d | System Menu Down                 |        |                                  |         |        |                                  |       |
| 0x01 0x008e | System Cold Restart              |        |                                  |         |        |                                  |       |
| 0x01 0x008f | System Warm Restart              |        |                                  |         |        |                                  |       |
| 0x01 0x00a0 | System Dock                      |        |                                  |         |        |                                  |       |
| 0x01 0x00a1 | System Undock                    |        |                                  |         |        |                                  |       |
| 0x01 0x00a2 | System Setup                     |        |                                  |         |        |                                  |       |
| 0x01 0x00a3 | System Break                     |        |                                  |         |        |                                  |       |
| 0x01 0x00a4 | System Debugger Break            |        |                                  |         |        |                                  |       |
| 0x01 0x00a5 | Application Break                |        |                                  |         |        |                                  |       |
| 0x01 0x00a6 | Application Debugger Break       |        |                                  |         |        |                                  |       |
| 0x01 0x00a7 | System Speaker Mute              |        |                                  |         |        |                                  |       |
| 0x01 0x00a8 | System Hibernate                 |        |                                  |         |        |                                  |       |
| 0x01 0x00b0 | System Display Invert            |        |                                  |         |        |                                  |       |
| 0x01 0x00b1 | System Display Internal          |        |                                  |         |        |                                  |       |
| 0x01 0x00b2 | System Display External          |        |                                  |         |        |                                  |       |
| 0x01 0x00b3 | System Display Both              |        |                                  |         |        |                                  |       |
| 0x01 0x00b4 | System Display Dual              |        |                                  |         |        |                                  |       |
| 0x01 0x00b5 | System Display Toggle Int/Ext    |        |                                  |         |        |                                  |       |
| 0x01 0x00b6 | System Display Swap Prim./Sec.   |        |                                  |         |        |                                  |       |
| 0x01 0x00b7 | System Display LCD Autoscale     |        |                                  |         |        |                                  |       |

### HID Consumer Page (0x0c) ###

| HID Usage   | HID Usage Name                   | LKC    | Linux Key Code Name              | Version | AKC    | Android Key Code Name            | Notes |
| ----------- | -------------------------------- | ------ | -------------------------------- | ------- | ------ | -------------------------------- | ----- |
| 0x0c 0x0030 | Power                            |        |                                  |         |        |                                  |       |
| 0x0c 0x0031 | Reset                            |        |                                  |         |        |                                  |       |
| 0x0c 0x0032 | Sleep                            |        |                                  |         |        |                                  |       |
| 0x0c 0x0033 | Sleep After                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0034 | Sleep Mode                       | 0x008e | KEY_SLEEP                        | 4.0     | 0x001a | KEYCODE_POWER                    |       |
| 0x0c 0x0040 | Menu                             | 0x008b | KEY_MENU                         | 1.6     | 0x0052 | KEYCODE_MENU                     |       |
| 0x0c 0x0041 | Menu Pick                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0042 | Menu Up                          |        |                                  |         |        |                                  |       |
| 0x0c 0x0043 | Menu Down                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0044 | Menu Left                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0045 | Menu Right                       | 0x0181 | KEY_RADIO                        |         |        |                                  |       |
| 0x0c 0x0046 | Menu Escape                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0047 | Menu Value Increase              |        |                                  |         |        |                                  |       |
| 0x0c 0x0048 | Menu Value Decrease              |        |                                  |         |        |                                  |       |
| 0x0c 0x0081 | Assign Selection                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0082 | Mode Step                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0083 | Recall Last                      | 0x0195 | KEY_LAST                         |         |        |                                  |       |
| 0x0c 0x0084 | Enter Channel                    |        |                                  |         |        |                                  |       |
| 0x0c 0x0085 | Order Movie                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0088 | Media Select Computer            | 0x0178 | KEY_PC                           |         |        |                                  |       |
| 0x0c 0x0089 | Media Select TV                  | 0x0179 | KEY_TV                           | 3.0     | 0x00aa | KEYCODE_TV                       |       |
| 0x0c 0x008a | Media Select WWW                 | 0x0096 | KEY_WWW                          | 1.6     | 0x0040 | KEYCODE_EXPLORER                 |       |
| 0x0c 0x008b | Media Select DVD                 | 0x0185 | KEY_DVD                          |         |        |                                  |       |
| 0x0c 0x008c | Media Select Telephone           | 0x00a9 | KEY_PHONE                        | 3.0     | 0x0005 | KEYCODE_CALL                     |       |
| 0x0c 0x008d | Media Select Program Guide       | 0x016a | KEY_PROGRAM                      | 3.0     | 0x00ac | KEYCODE_GUIDE                    |       |
| 0x0c 0x008e | Media Select Video Phone         | 0x01a0 | KEY_VIDEOPHONE                   |         |        |                                  |       |
| 0x0c 0x008f | Media Select Games               | 0x01a1 | KEY_GAMES                        |         |        |                                  |       |
| 0x0c 0x0090 | Media Select Messages            | 0x018c | KEY_MEMO                         |         |        |                                  |       |
| 0x0c 0x0091 | Media Select CD                  | 0x017f | KEY_CD                           |         |        |                                  |       |
| 0x0c 0x0092 | Media Select VCR                 | 0x017b | KEY_VCR                          |         |        |                                  |       |
| 0x0c 0x0093 | Media Select Tuner               | 0x0182 | KEY_TUNER                        |         |        |                                  |       |
| 0x0c 0x0094 | Quit                             | 0x00ae | KEY_EXIT                         |         |        |                                  |       |
| 0x0c 0x0095 | Help                             | 0x008a | KEY_HELP                         |         |        |                                  |       |
| 0x0c 0x0096 | Media Select Tape                | 0x0180 | KEY_TAPE                         |         |        |                                  |       |
| 0x0c 0x0097 | Media Select Cable               | 0x017a | KEY_TV2                          |         |        |                                  |       |
| 0x0c 0x0098 | Media Select Satellite           | 0x017d | KEY_SAT                          |         |        |                                  |       |
| 0x0c 0x0099 | Media Select Security            |        |                                  |         |        |                                  |       |
| 0x0c 0x009a | Media Select Home                | 0x016e | KEY_PVR                          | 3.0     | 0x00ad | KEYCODE_DVR                      |       |
| 0x0c 0x009c | Channel Increment                | 0x0192 | KEY_CHANNELUP                    | 3.0     | 0x00a6 | KEYCODE_CHANNEL_UP               |       |
| 0x0c 0x009d | Channel Decrement                | 0x0193 | KEY_CHANNELDOWN                  | 3.0     | 0x00a7 | KEYCODE_CHANNEL_DOWN             |       |
| 0x0c 0x009e | Media Select SAP                 |        |                                  |         |        |                                  |       |
| 0x0c 0x00a0 | VCR Plus                         | 0x017c | KEY_VCR2                         |         |        |                                  |       |
| 0x0c 0x00a1 | Once                             |        |                                  |         |        |                                  |       |
| 0x0c 0x00a2 | Daily                            |        |                                  |         |        |                                  |       |
| 0x0c 0x00a3 | Weekly                           |        |                                  |         |        |                                  |       |
| 0x0c 0x00a4 | Monthly                          |        |                                  |         |        |                                  |       |
| 0x0c 0x00b0 | Play                             | 0x00cf | KEY_PLAY                         | 3.0     | 0x007e | KEYCODE_MEDIA_PLAY               |       |
| 0x0c 0x00b1 | Pause                            | 0x0077 | KEY_PAUSE                        | 3.0     | 0x0079 | KEYCODE_BREAK                    |       |
| 0x0c 0x00b2 | Record                           | 0x00a7 | KEY_RECORD                       | 3.0     | 0x0082 | KEYCODE_MEDIA_RECORD             |       |
| 0x0c 0x00b3 | Fast Forward                     | 0x00d0 | KEY_FASTFORWARD                  | 3.0     | 0x005a | KEYCODE_MEDIA_FAST_FORWARD       |       |
| 0x0c 0x00b4 | Rewind                           | 0x00a8 | KEY_REWIND                       | 3.0     | 0x0059 | KEYCODE_MEDIA_REWIND             |       |
| 0x0c 0x00b5 | Scan Next Track                  | 0x00a3 | KEY_NEXTSONG                     | 3.0     | 0x0057 | KEYCODE_MEDIA_NEXT               |       |
| 0x0c 0x00b6 | Scan Previous Track              | 0x00a5 | KEY_PREVIOUSSONG                 | 3.0     | 0x0058 | KEYCODE_MEDIA_PREVIOUS           |       |
| 0x0c 0x00b7 | Stop                             | 0x00a6 | KEY_STOPCD                       | 3.0     | 0x0056 | KEYCODE_MEDIA_STOP               |       |
| 0x0c 0x00b8 | Eject                            | 0x00a1 | KEY_EJECTCD                      | 3.0     | 0x0081 | KEYCODE_MEDIA_EJECT              |       |
| 0x0c 0x00b9 | Random Play                      |        |                                  |         |        |                                  |       |
| 0x0c 0x00ba | Select Disc                      |        |                                  |         |        |                                  |       |
| 0x0c 0x00bb | Enter Disc                       |        |                                  |         |        |                                  |       |
| 0x0c 0x00bc | Repeat                           | 0x01b7 | KEY_MEDIA_REPEAT                 |         |        |                                  |       |
| 0x0c 0x00be | Track Normal                     |        |                                  |         |        |                                  |       |
| 0x0c 0x00c0 | Frame Forward                    |        |                                  |         |        |                                  |       |
| 0x0c 0x00c1 | Frame Back                       |        |                                  |         |        |                                  |       |
| 0x0c 0x00c2 | Mark                             |        |                                  |         |        |                                  |       |
| 0x0c 0x00c3 | Clear Mark                       |        |                                  |         |        |                                  |       |
| 0x0c 0x00c4 | Repeat From Mark                 |        |                                  |         |        |                                  |       |
| 0x0c 0x00c5 | Return To Mark                   |        |                                  |         |        |                                  |       |
| 0x0c 0x00c6 | Search Mark Forward              |        |                                  |         |        |                                  |       |
| 0x0c 0x00c7 | Search Mark Backwards            |        |                                  |         |        |                                  |       |
| 0x0c 0x00c8 | Counter Reset                    |        |                                  |         |        |                                  |       |
| 0x0c 0x00c9 | Show Counter                     |        |                                  |         |        |                                  |       |
| 0x0c 0x00ca | Tracking Increment               |        |                                  |         |        |                                  |       |
| 0x0c 0x00cb | Tracking Decrement               |        |                                  |         |        |                                  |       |
| 0x0c 0x00cc | Stop / Eject                     |        |                                  |         |        |                                  |       |
| 0x0c 0x00cd | Play / Pause                     | 0x00a4 | KEY_PLAYPAUSE                    | 3.0     | 0x0055 | KEYCODE_MEDIA_PLAY_PAUSE         |       |
| 0x0c 0x00ce | Play / Skip                      |        |                                  |         |        |                                  |       |
| 0x0c 0x00e2 | Mute                             | 0x0071 | KEY_MUTE                         | 3.0     | 0x00a4 | KEYCODE_VOLUME_MUTE              |       |
| 0x0c 0x00e5 | Bass Boost                       | 0x00d1 | KEY_BASSBOOST                    |         |        |                                  |       |
| 0x0c 0x00e6 | Surround Mode                    |        |                                  |         |        |                                  |       |
| 0x0c 0x00e7 | Loudness                         |        |                                  |         |        |                                  |       |
| 0x0c 0x00e8 | MPX                              |        |                                  |         |        |                                  |       |
| 0x0c 0x00e9 | Volume Increment                 | 0x0073 | KEY_VOLUMEUP                     | 1.6     | 0x0018 | KEYCODE_VOLUME_UP                |       |
| 0x0c 0x00ea | Volume Decrement                 | 0x0072 | KEY_VOLUMEDOWN                   | 1.6     | 0x0019 | KEYCODE_VOLUME_DOWN              |       |
| 0x0c 0x0181 | AL Launch Button Config. Tool    |        |                                  |         |        |                                  |       |
| 0x0c 0x0182 | AL Programmable Button Config.   | 0x009c | KEY_BOOKMARKS                    | 3.0     | 0x00ae | KEYCODE_BOOKMARK                 |       |
| 0x0c 0x0183 | AL Consumer Control Config.      | 0x00ab | KEY_CONFIG                       | 4.0.3   | 0x00d1 | KEYCODE_MUSIC                    |       |
| 0x0c 0x0184 | AL Word Processor                | 0x01a5 | KEY_WORDPROCESSOR                |         |        |                                  |       |
| 0x0c 0x0185 | AL Text Editor                   | 0x01a6 | KEY_EDITOR                       |         |        |                                  |       |
| 0x0c 0x0186 | AL Spreadsheet                   | 0x01a7 | KEY_SPREADSHEET                  |         |        |                                  |       |
| 0x0c 0x0187 | AL Graphics Editor               | 0x01a8 | KEY_GRAPHICSEDITOR               |         |        |                                  |       |
| 0x0c 0x0188 | AL Presentation App              | 0x01a9 | KEY_PRESENTATION                 |         |        |                                  |       |
| 0x0c 0x0189 | AL Database App                  | 0x01aa | KEY_DATABASE                     |         |        |                                  |       |
| 0x0c 0x018a | AL Email Reader                  | 0x009b | KEY_MAIL                         | 1.6     | 0x0041 | KEYCODE_ENVELOPE                 |       |
| 0x0c 0x018b | AL Newsreader                    | 0x01ab | KEY_NEWS                         |         |        |                                  |       |
| 0x0c 0x018c | AL Voicemail                     | 0x01ac | KEY_VOICEMAIL                    |         |        |                                  |       |
| 0x0c 0x018d | AL Contacts / Address Book       | 0x01ad | KEY_ADDRESSBOOK                  | 4.0.3   | 0x00cf | KEYCODE_CONTACTS                 |       |
| 0x0c 0x018e | AL Calendar / Schedule           | 0x018d | KEY_CALENDAR                     | 4.0.3   | 0x00d0 | KEYCODE_CALENDAR                 |       |
| 0x0c 0x018f | AL Task / Project Manager        |        |                                  |         |        |                                  |       |
| 0x0c 0x0190 | AL Log / Journal / Timecard      |        |                                  |         |        |                                  |       |
| 0x0c 0x0191 | AL Checkbook / Finance           | 0x00db | KEY_FINANCE                      |         |        |                                  |       |
| 0x0c 0x0192 | AL Calculator                    | 0x008c | KEY_CALC                         | 4.0.3   | 0x00d2 | KEYCODE_CALCULATOR               |       |
| 0x0c 0x0193 | AL A/V Capture / Playback        |        |                                  |         |        |                                  |       |
| 0x0c 0x0194 | AL Local Machine Browser         | 0x0090 | KEY_FILE                         |         |        |                                  |       |
| 0x0c 0x0195 | AL LAN/WAN Browser               |        |                                  |         |        |                                  |       |
| 0x0c 0x0196 | AL Internet Browser              | 0x0096 | KEY_WWW                          | 1.6     | 0x0040 | KEYCODE_EXPLORER                 |       |
| 0x0c 0x0197 | AL Remote Networking/ISP Connect |        |                                  |         |        |                                  |       |
| 0x0c 0x0198 | AL Network Conference            |        |                                  |         |        |                                  |       |
| 0x0c 0x0199 | AL Network Chat                  | 0x00d8 | KEY_CHAT                         |         |        |                                  |       |
| 0x0c 0x019a | AL Telephony / Dialer            |        |                                  |         |        |                                  |       |
| 0x0c 0x019b | AL Logon                         |        |                                  |         |        |                                  |       |
| 0x0c 0x019c | AL Logoff                        | 0x01b1 | KEY_LOGOFF                       |         |        |                                  |       |
| 0x0c 0x019d | AL Logon / Logoff                |        |                                  |         |        |                                  |       |
| 0x0c 0x019e | AL Terminal Lock / Screensaver   | 0x0098 | KEY_COFFEE                       | 4.0     | 0x001a | KEYCODE_POWER                    |       |
| 0x0c 0x019f | AL Control Panel                 |        |                                  |         |        |                                  |       |
| 0x0c 0x01a0 | AL Command Line Processor / Run  |        |                                  |         |        |                                  |       |
| 0x0c 0x01a1 | AL Process / Task Manager        |        |                                  |         |        |                                  |       |
| 0x0c 0x01a2 | AL Select Task / Application     |        |                                  |         |        |                                  |       |
| 0x0c 0x01a3 | AL Next Task / Application       |        |                                  |         |        |                                  |       |
| 0x0c 0x01a4 | AL Previous Task / Application   |        |                                  |         |        |                                  |       |
| 0x0c 0x01a5 | AL Preemptive Halt Task / App.   |        |                                  |         |        |                                  |       |
| 0x0c 0x01a6 | AL Integrated Help Center        | 0x008a | KEY_HELP                         |         |        |                                  |       |
| 0x0c 0x01a7 | AL Documents                     | 0x00eb | KEY_DOCUMENTS                    |         |        |                                  |       |
| 0x0c 0x01a8 | AL Thesaurus                     |        |                                  |         |        |                                  |       |
| 0x0c 0x01a9 | AL Dictionary                    |        |                                  |         |        |                                  |       |
| 0x0c 0x01aa | AL Desktop                       |        |                                  |         |        |                                  |       |
| 0x0c 0x01ab | AL Spell Check                   | 0x01b0 | KEY_SPELLCHECK                   |         |        |                                  |       |
| 0x0c 0x01ac | AL Grammar Check                 |        |                                  |         |        |                                  |       |
| 0x0c 0x01ad | AL Wireless Status               |        |                                  |         |        |                                  |       |
| 0x0c 0x01ae | AL Keyboard Layout               |        |                                  |         |        |                                  |       |
| 0x0c 0x01af | AL Virus Protection              |        |                                  |         |        |                                  |       |
| 0x0c 0x01b0 | AL Encryption                    |        |                                  |         |        |                                  |       |
| 0x0c 0x01b1 | AL Screen Saver                  |        |                                  |         |        |                                  |       |
| 0x0c 0x01b2 | AL Alarms                        |        |                                  |         |        |                                  |       |
| 0x0c 0x01b3 | AL Clock                         |        |                                  |         |        |                                  |       |
| 0x0c 0x01b4 | AL File Browser                  |        |                                  |         |        |                                  |       |
| 0x0c 0x01b5 | AL Power Status                  |        |                                  |         |        |                                  |       |
| 0x0c 0x01b6 | AL Image Browser                 | 0x00e2 | KEY_MEDIA                        | 3.0     | 0x004f | KEYCODE_HEADSETHOOK              |       |
| 0x0c 0x01b7 | AL Audio Browser                 | 0x00d5 | KEY_SOUND                        | 4.0.3   | 0x00d1 | KEYCODE_MUSIC                    |       |
| 0x0c 0x01b8 | AL Movie Browser                 |        |                                  |         |        |                                  |       |
| 0x0c 0x01b9 | AL Digital Rights Manager        |        |                                  |         |        |                                  |       |
| 0x0c 0x01ba | AL Digital Wallet                |        |                                  |         |        |                                  |       |
| 0x0c 0x01bc | AL Instant Messaging             | 0x01ae | KEY_MESSENGER                    |         |        |                                  |       |
| 0x0c 0x01bd | AL OEM Features / Tips Browser   | 0x0166 | KEY_INFO                         |         |        |                                  |       |
| 0x0c 0x01be | AL OEM Help                      |        |                                  |         |        |                                  |       |
| 0x0c 0x01bf | AL Online Community              |        |                                  |         |        |                                  |       |
| 0x0c 0x01c0 | AL Entertainment Content Browser |        |                                  |         |        |                                  |       |
| 0x0c 0x01c1 | AL Online Shopping Browser       |        |                                  |         |        |                                  |       |
| 0x0c 0x01c2 | AL SmartCard Information / Help  |        |                                  |         |        |                                  |       |
| 0x0c 0x01c3 | AL Market / Finance Browser      |        |                                  |         |        |                                  |       |
| 0x0c 0x01c4 | AL Customized Corp. News Browser |        |                                  |         |        |                                  |       |
| 0x0c 0x01c5 | AL Online Activity Browser       |        |                                  |         |        |                                  |       |
| 0x0c 0x01c6 | AL Research / Search Browser     |        |                                  |         |        |                                  |       |
| 0x0c 0x01c7 | AL Audio Player                  |        |                                  |         |        |                                  |       |
| 0x0c 0x0201 | AC New                           | 0x00b5 | KEY_NEW                          |         |        |                                  |       |
| 0x0c 0x0202 | AC Open                          | 0x0086 | KEY_OPEN                         |         |        |                                  |       |
| 0x0c 0x0203 | AC Close                         | 0x00ce | KEY_CLOSE                        |         |        |                                  |       |
| 0x0c 0x0204 | AC Exit                          | 0x00ae | KEY_EXIT                         |         |        |                                  |       |
| 0x0c 0x0205 | AC Maximize                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0206 | AC Minimize                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0207 | AC Save                          | 0x00ea | KEY_SAVE                         |         |        |                                  |       |
| 0x0c 0x0208 | AC Print                         | 0x00d2 | KEY_PRINT                        |         |        |                                  |       |
| 0x0c 0x0209 | AC Properties                    | 0x0082 | KEY_PROPS                        |         |        |                                  |       |
| 0x0c 0x021a | AC Undo                          | 0x0083 | KEY_UNDO                         |         |        |                                  |       |
| 0x0c 0x021b | AC Copy                          | 0x0085 | KEY_COPY                         |         |        |                                  |       |
| 0x0c 0x021c | AC Cut                           | 0x0089 | KEY_CUT                          |         |        |                                  |       |
| 0x0c 0x021d | AC Paste                         | 0x0087 | KEY_PASTE                        |         |        |                                  |       |
| 0x0c 0x021e | AC Select All                    |        |                                  |         |        |                                  |       |
| 0x0c 0x021f | AC Find                          | 0x0088 | KEY_FIND                         |         |        |                                  |       |
| 0x0c 0x0220 | AC Find and Replace              |        |                                  |         |        |                                  |       |
| 0x0c 0x0221 | AC Search                        | 0x00d9 | KEY_SEARCH                       | 1.6     | 0x0054 | KEYCODE_SEARCH                   |       |
| 0x0c 0x0222 | AC Go To                         | 0x0162 | KEY_GOTO                         |         |        |                                  |       |
| 0x0c 0x0223 | AC Home                          | 0x00ac | KEY_HOMEPAGE                     | 3.0     | 0x0003 | KEYCODE_HOME                     |       |
| 0x0c 0x0224 | AC Back                          | 0x009e | KEY_BACK                         | 1.6     | 0x0004 | KEYCODE_BACK                     |       |
| 0x0c 0x0225 | AC Forward                       | 0x009f | KEY_FORWARD                      | 3.0     | 0x007d | KEYCODE_FORWARD                  |       |
| 0x0c 0x0226 | AC Stop                          | 0x0080 | KEY_STOP                         | 3.0     | 0x0056 | KEYCODE_MEDIA_STOP               |       |
| 0x0c 0x0227 | AC Refresh                       | 0x00ad | KEY_REFRESH                      |         |        |                                  |       |
| 0x0c 0x0228 | AC Previous Link                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0229 | AC Next Link                     |        |                                  |         |        |                                  |       |
| 0x0c 0x022a | AC Bookmarks                     | 0x009c | KEY_BOOKMARKS                    | 3.0     | 0x00ae | KEYCODE_BOOKMARK                 |       |
| 0x0c 0x022b | AC History                       |        |                                  |         |        |                                  |       |
| 0x0c 0x022c | AC Subscriptions                 |        |                                  |         |        |                                  |       |
| 0x0c 0x022d | AC Zoom In                       | 0x01a2 | KEY_ZOOMIN                       |         |        |                                  |       |
| 0x0c 0x022e | AC Zoom Out                      | 0x01a3 | KEY_ZOOMOUT                      |         |        |                                  |       |
| 0x0c 0x022f | AC Zoom                          | 0x01a4 | KEY_ZOOMRESET                    |         |        |                                  | 2     |
| 0x0c 0x0230 | AC Full Screen View              |        |                                  |         |        |                                  |       |
| 0x0c 0x0231 | AC Normal View                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0232 | AC View Toggle                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0233 | AC Scroll Up                     | 0x00b1 | KEY_SCROLLUP                     | 3.0     | 0x005c | KEYCODE_PAGE_UP                  |       |
| 0x0c 0x0234 | AC Scroll Down                   | 0x00b2 | KEY_SCROLLDOWN                   | 3.0     | 0x005d | KEYCODE_PAGE_DOWN                |       |
| 0x0c 0x0236 | AC Pan Left                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0237 | AC Pan Right                     |        |                                  |         |        |                                  |       |
| 0x0c 0x0239 | AC New Window                    |        |                                  |         |        |                                  |       |
| 0x0c 0x023a | AC Tile Horizontally             |        |                                  |         |        |                                  |       |
| 0x0c 0x023b | AC Tile Vertically               |        |                                  |         |        |                                  |       |
| 0x0c 0x023c | AC Format                        |        |                                  |         |        |                                  |       |
| 0x0c 0x023d | AC Edit                          |        |                                  |         |        |                                  |       |
| 0x0c 0x023e | AC Bold                          |        |                                  |         |        |                                  |       |
| 0x0c 0x023f | AC Italics                       |        |                                  |         |        |                                  |       |
| 0x0c 0x0240 | AC Underline                     |        |                                  |         |        |                                  |       |
| 0x0c 0x0241 | AC Strikethrough                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0242 | AC Subscript                     |        |                                  |         |        |                                  |       |
| 0x0c 0x0243 | AC Superscript                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0244 | AC All Caps                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0245 | AC Rotate                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0246 | AC Resize                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0247 | AC Flip horizontal               |        |                                  |         |        |                                  |       |
| 0x0c 0x0248 | AC Flip Vertical                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0249 | AC Mirror Horizontal             |        |                                  |         |        |                                  |       |
| 0x0c 0x024a | AC Mirror Vertical               |        |                                  |         |        |                                  |       |
| 0x0c 0x024b | AC Font Select                   |        |                                  |         |        |                                  |       |
| 0x0c 0x024c | AC Font Color                    |        |                                  |         |        |                                  |       |
| 0x0c 0x024d | AC Font Size                     |        |                                  |         |        |                                  |       |
| 0x0c 0x024e | AC Justify Left                  |        |                                  |         |        |                                  |       |
| 0x0c 0x024f | AC Justify Center H              |        |                                  |         |        |                                  |       |
| 0x0c 0x0250 | AC Justify Right                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0251 | AC Justify Block H               |        |                                  |         |        |                                  |       |
| 0x0c 0x0252 | AC Justify Top                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0253 | AC Justify Center V              |        |                                  |         |        |                                  |       |
| 0x0c 0x0254 | AC Justify Bottom                |        |                                  |         |        |                                  |       |
| 0x0c 0x0255 | AC Justify Block V               |        |                                  |         |        |                                  |       |
| 0x0c 0x0256 | AC Indent Decrease               |        |                                  |         |        |                                  |       |
| 0x0c 0x0257 | AC Indent Increase               |        |                                  |         |        |                                  |       |
| 0x0c 0x0258 | AC Numbered List                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0259 | AC Restart Numbering             |        |                                  |         |        |                                  |       |
| 0x0c 0x025a | AC Bulleted List                 |        |                                  |         |        |                                  |       |
| 0x0c 0x025b | AC Promote                       |        |                                  |         |        |                                  |       |
| 0x0c 0x025c | AC Demote                        |        |                                  |         |        |                                  |       |
| 0x0c 0x025d | AC Yes                           |        |                                  |         |        |                                  |       |
| 0x0c 0x025e | AC No                            |        |                                  |         |        |                                  |       |
| 0x0c 0x025f | AC Cancel                        | 0x00df | KEY_CANCEL                       |         |        |                                  |       |
| 0x0c 0x0260 | AC Catalog                       |        |                                  |         |        |                                  |       |
| 0x0c 0x0261 | AC Buy / Checkout                |        |                                  |         |        |                                  |       |
| 0x0c 0x0262 | AC Add to Cart                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0263 | AC Expand                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0264 | AC Expand All                    |        |                                  |         |        |                                  |       |
| 0x0c 0x0265 | AC Collapse                      |        |                                  |         |        |                                  |       |
| 0x0c 0x0266 | AC Collapse All                  |        |                                  |         |        |                                  |       |
| 0x0c 0x0267 | AC Print Preview                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0268 | AC Paste Special                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0269 | AC Insert Mode                   |        |                                  |         |        |                                  |       |
| 0x0c 0x026a | AC Delete                        |        |                                  |         |        |                                  |       |
| 0x0c 0x026b | AC Lock                          |        |                                  |         |        |                                  |       |
| 0x0c 0x026c | AC Unlock                        |        |                                  |         |        |                                  |       |
| 0x0c 0x026d | AC Protect                       |        |                                  |         |        |                                  |       |
| 0x0c 0x026e | AC Unprotect                     |        |                                  |         |        |                                  |       |
| 0x0c 0x026f | AC Attach Comment                |        |                                  |         |        |                                  |       |
| 0x0c 0x0270 | AC Delete Comment                |        |                                  |         |        |                                  |       |
| 0x0c 0x0271 | AC View Comment                  |        |                                  |         |        |                                  |       |
| 0x0c 0x0272 | AC Select Word                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0273 | AC Select Sentence               |        |                                  |         |        |                                  |       |
| 0x0c 0x0274 | AC Select Paragraph              |        |                                  |         |        |                                  |       |
| 0x0c 0x0275 | AC Select Column                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0276 | AC Select Row                    |        |                                  |         |        |                                  |       |
| 0x0c 0x0277 | AC Select Table                  |        |                                  |         |        |                                  |       |
| 0x0c 0x0278 | AC Select Object                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0279 | AC Redo / Repeat                 | 0x00b6 | KEY_REDO                         |         |        |                                  |       |
| 0x0c 0x027a | AC Sort                          |        |                                  |         |        |                                  |       |
| 0x0c 0x027b | AC Sort Ascending                |        |                                  |         |        |                                  |       |
| 0x0c 0x027c | AC Sort Descending               |        |                                  |         |        |                                  |       |
| 0x0c 0x027d | AC Filter                        |        |                                  |         |        |                                  |       |
| 0x0c 0x027e | AC Set Clock                     |        |                                  |         |        |                                  |       |
| 0x0c 0x027f | AC View Clock                    |        |                                  |         |        |                                  |       |
| 0x0c 0x0280 | AC Select Time Zone              |        |                                  |         |        |                                  |       |
| 0x0c 0x0281 | AC Edit Time Zones               |        |                                  |         |        |                                  |       |
| 0x0c 0x0282 | AC Set Alarm                     |        |                                  |         |        |                                  |       |
| 0x0c 0x0283 | AC Clear Alarm                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0284 | AC Snooze Alarm                  |        |                                  |         |        |                                  |       |
| 0x0c 0x0285 | AC Reset Alarm                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0286 | AC Synchronize                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0287 | AC Send/Receive                  |        |                                  |         |        |                                  |       |
| 0x0c 0x0288 | AC Send To                       |        |                                  |         |        |                                  |       |
| 0x0c 0x0289 | AC Reply                         | 0x00e8 | KEY_REPLY                        |         |        |                                  |       |
| 0x0c 0x028a | AC Reply All                     |        |                                  |         |        |                                  |       |
| 0x0c 0x028b | AC Forward Msg                   | 0x00e9 | KEY_FORWARDMAIL                  |         |        |                                  |       |
| 0x0c 0x028c | AC Send                          | 0x00e7 | KEY_SEND                         |         |        |                                  |       |
| 0x0c 0x028d | AC Attach File                   |        |                                  |         |        |                                  |       |
| 0x0c 0x028e | AC Upload                        |        |                                  |         |        |                                  |       |
| 0x0c 0x028f | AC Download (Save Target As)     |        |                                  |         |        |                                  |       |
| 0x0c 0x0290 | AC Set Borders                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0291 | AC Insert Row                    |        |                                  |         |        |                                  |       |
| 0x0c 0x0292 | AC Insert Column                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0293 | AC Insert File                   |        |                                  |         |        |                                  |       |
| 0x0c 0x0294 | AC Insert Picture                |        |                                  |         |        |                                  |       |
| 0x0c 0x0295 | AC Insert Object                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0296 | AC Insert Symbol                 |        |                                  |         |        |                                  |       |
| 0x0c 0x0297 | AC Save and Close                |        |                                  |         |        |                                  |       |
| 0x0c 0x0298 | AC Rename                        |        |                                  |         |        |                                  |       |
| 0x0c 0x0299 | AC Merge                         |        |                                  |         |        |                                  |       |
| 0x0c 0x029a | AC Split                         |        |                                  |         |        |                                  |       |
| 0x0c 0x029b | AC Distribute Horizontally       |        |                                  |         |        |                                  |       |
| 0x0c 0x029c | AC Distribute Vertically         |        |                                  |         |        |                                  |       |

### Additional non-HID Mappings ###

These mappings describe functions that do not appear in HID but for which Linux
key codes exist.

| LKC    | Linux Key Code Name              | Version | AKC    | Android Key Code Name            | Notes |
| ------ | -------------------------------- | ------- | ------ | -------------------------------- | ----- |
| 0x01d0 | KEY_FN                           | 3.0     | 0x0077 | KEYCODE_FUNCTION                 |       |
| 0x01d1 | KEY_FN_ESC                       | 3.0     | 0x006f | KEYCODE_ESCAPE                   | 3     |
| 0x01d2 | KEY_FN_F1                        | 3.0     | 0x0083 | KEYCODE_F1                       | 3     |
| 0x01d3 | KEY_FN_F2                        | 3.0     | 0x0084 | KEYCODE_F2                       | 3     |
| 0x01d4 | KEY_FN_F3                        | 3.0     | 0x0085 | KEYCODE_F3                       | 3     |
| 0x01d5 | KEY_FN_F4                        | 3.0     | 0x0086 | KEYCODE_F4                       | 3     |
| 0x01d6 | KEY_FN_F5                        | 3.0     | 0x0087 | KEYCODE_F5                       | 3     |
| 0x01d7 | KEY_FN_F6                        | 3.0     | 0x0088 | KEYCODE_F6                       | 3     |
| 0x01d8 | KEY_FN_F7                        | 3.0     | 0x0089 | KEYCODE_F7                       | 3     |
| 0x01d9 | KEY_FN_F8                        | 3.0     | 0x008a | KEYCODE_F8                       | 3     |
| 0x01da | KEY_FN_F9                        | 3.0     | 0x008b | KEYCODE_F9                       | 3     |
| 0x01db | KEY_FN_F10                       | 3.0     | 0x008c | KEYCODE_F10                      | 3     |
| 0x01dc | KEY_FN_F11                       | 3.0     | 0x008d | KEYCODE_F11                      | 3     |
| 0x01dd | KEY_FN_F12                       | 3.0     | 0x008e | KEYCODE_F12                      | 3     |
| 0x01de | KEY_FN_1                         | 3.0     | 0x0008 | KEYCODE_1                        | 3     |
| 0x01df | KEY_FN_2                         | 3.0     | 0x0009 | KEYCODE_2                        | 3     |
| 0x01e0 | KEY_FN_D                         | 3.0     | 0x0020 | KEYCODE_D                        | 3     |
| 0x01e1 | KEY_FN_E                         | 3.0     | 0x0021 | KEYCODE_E                        | 3     |
| 0x01e2 | KEY_FN_F                         | 3.0     | 0x0022 | KEYCODE_F                        | 3     |
| 0x01e3 | KEY_FN_S                         | 3.0     | 0x002f | KEYCODE_S                        | 3     |
| 0x01e4 | KEY_FN_B                         | 3.0     | 0x001e | KEYCODE_B                        | 3     |

### Legacy Unsupported Keys ###

These mappings appeared in previous versions of Android but were inconsistent with
HID or used non-standard Linux key codes.  They are no longer supported.

| LKC    | Linux Key Code Name              | Version | AKC    | Android Key Code Name            | Notes |
| ------ | -------------------------------- | ------- | ------ | -------------------------------- | ----- |
| 0x00db | KEY_EMAIL                        | 1.6     | 0x004d | KEYCODE_AT                       | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e3 | KEY_STAR                         | 1.6     | 0x0011 | KEYCODE_STAR                     | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e4 | KEY_SHARP                        | 1.6     | 0x0012 | KEYCODE_POUND                    | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e5 | KEY_SOFT1                        | 1.6     | 0x0052 | KEYCODE_MENU                     | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e6 | KEY_SOFT2                        | 1.6     | 0x0002 | KEYCODE_SOFT_RIGHT               | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e7 | KEY_SEND                         | 1.6     | 0x0005 | KEYCODE_CALL                     | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e8 | KEY_CENTER                       | 1.6     | 0x0017 | KEYCODE_DPAD_CENTER              | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00e9 | KEY_HEADSETHOOK                  | 1.6     | 0x004f | KEYCODE_HEADSETHOOK              | 4     |
| ""     | ""                               | 4.0     |        |                                  | 4     |
| 0x00ea | KEY_0_5                          | 1.6     |        |                                  | 4     |
| 0x00eb | KEY_2_5                          | 1.6     |        |                                  | 4     |

### Notes ###

1.  The Android key code associated with common alphanumeric and symbolic
    keys may vary based on the keyboard layout and language.
    For historical reasons, the physical scan codes and HID usages
    associated with keys on a keyboard are often defined positionally
    even though the labels printed on those keys may vary from one
    language to another.

    On a US English (QWERTY) keyboard, the top-left alphabetic key is
    labeled Q.  On a French (AZERTY) keyboard, the key in the same
    position is labeled A.  Despite the label, on both keyboards the
    top-left alphabetic key is referred to using the HID usage
    0x07 0x0014 which is mapped to the Linux key code KEY_Q.

    When Android is configured with a US English keyboard layout, then
    the Linux key code KEY_Q will be mapped to the Android key code
    KEYCODE_Q and will produce the characters 'Q' and 'q'.
    However, when Android is configured with a French keyboard layout,
    then the Linux key code KEY_Q will be mapped to the Android key code
    KEYCODE_A and will produce the characters 'A' and 'a'.

    The Android key code typically reflects the language-specific
    interpretation of the key, so a different Android key code may
    be used for different languages.

2.  `0x0c 0x022f AC Zoom` is defined in the HID as a linear control but
    the kernel maps it as a key, which is probably incorrect.

3.  The Linux function keys `KEY_FN_*` are mapped to simpler
    key codes but are dispatched with the `META_FUNCTION` meta state
    bit set to true.

4.  Prior to Android Ice Cream Sandwich 4.0, the default key layout
    contained mappings for some extra key codes that were not defined
    in the mainline Linux kernel headers.  These mappings have since
    been removed because these previously undefined key codes have
    since been assigned different meanings in more recent versions
    of the Linux kernel.

### Sources ###

1.  [USB HID Usage Tables v1.12](http://www.usb.org/developers/devclass_docs/Hut1_12v2.pdf)
2.  Linux 2.6.39 kernel: include/linux/input.h, drivers/hid/hid-input.c
3.  Android ICS: qwerty.kl, Generic.kl, KeyEvent.java
