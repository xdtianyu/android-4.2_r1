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

# Key Layout Files #

Key layout files (`.kl` files) are responsible for mapping Linux key codes
and axis codes to Android key codes and axis codes and specifying associated
policy flags.

Device-specific key layout files are *required* for all internal (built-in)
input devices that have keys, including special keys such as volume, power
and headset media keys.

Device-specific key layout files are *optional* for other input devices but
they are *recommended* for special-purpose keyboards and joysticks.

If no device-specific key layout file is available, then the system will
choose a default instead.

## Location ##

Key layout files are located by USB vendor, product (and optionally version)
id or by input device name.

The following paths are consulted in order.

*   `/system/usr/keylayout/Vendor_XXXX_Product_XXXX_Version_XXXX.kl`
*   `/system/usr/keylayout/Vendor_XXXX_Product_XXXX.kl`
*   `/system/usr/keylayout/DEVICE_NAME.kl`
*   `/data/system/devices/keylayout/Vendor_XXXX_Product_XXXX_Version_XXXX.kl`
*   `/data/system/devices/keylayout/Vendor_XXXX_Product_XXXX.kl`
*   `/data/system/devices/keylayout/DEVICE_NAME.kl`
*   `/system/usr/keylayout/Generic.kl`
*   `/data/system/devices/keylayout/Generic.kl`

When constructing a file path that contains the device name, all characters
in the device name other than '0'-'9', 'a'-'z', 'A'-'Z', '-' or '_' are replaced by '_'.

## Generic Key Layout File ##

The system provides a special built-in generic key layout file called `Generic.kl`.
This key layout is intended to support a variety of standard external
keyboards and joysticks.

*Do not modify the generic key layout!*

## Syntax ##

A key layout file is a plain text file consisting of key or axis declarations
and flags.

### Key Declarations ###

Key declarations each consist of the keyword `key` followed by a Linux key code
number, an Android key code name, and optional set of whitespace delimited policy flags.

    key 1     ESCAPE
    key 114   VOLUME_DOWN       WAKE
    key 16    Q                 VIRTUAL     WAKE

The following policy flags are recognized:

*   `WAKE`: The key should wake the device when it is asleep.  For historical reasons,
    this flag behaves in the same manner as `WAKE_DROPPED` below.
*   `WAKE_DROPPED`: The key should wake the device when it is asleep but the key itself
    should be dropped when the wake-up occurs.  In a sense, the key's action was to
    wake the device, but the key itself is not processed.
*   `SHIFT`: The key should be interpreted as if the SHIFT key were also pressed.
*   `CAPS_LOCK`: The key should be interpreted as if the CAPS LOCK key were also pressed.
*   `ALT`: The key should be interpreted as if the ALT key were also pressed.
*   `ALT_GR`: The key should be interpreted as if the RIGHT ALT key were also pressed.
*   `FUNCTION`: The key should be interpreted as if the FUNCTION key were also pressed.
*   `VIRTUAL`: The key is a virtual soft key (capacitive button) that is adjacent to
    the main touch screen.  This causes special debouncing logic to be enabled, see below.
*   `MENU`: Deprecated.  Do not use.
*   `LAUNCHER`: Deprecated.  Do not use.

### Axis Declarations ###

Axis declarations each consist of the keyword `axis` followed by a Linux axis code
number, and qualifiers that control the behavior of the axis including at least
one Android axis code name.

#### Basic Axes ####

A basic axis simply maps a Linux axis code to an Android axis code name.

The following declaration maps `ABS_X` (indicated by `0x00`) to `AXIS_X` (indicated by `X`).

    axis 0x00 X

In the above example, if the value of `ABS_X` is `5` then `AXIS_X` will be set to `5`.

#### Split Axes ####

A split axis maps a Linux axis code to two Android axis code names, such that
values less than or greater than a threshold are split across two different axes when
mapped.  This mapping is useful when a single physical axis reported by the device
encodes two different mutually exclusive logical axes.

The following declaration maps values of the `ABS_Y` axis (indicated by `0x01`) to
`AXIS_GAS` when less than `0x7f` or to `AXIS_BRAKE` when greater than `0x7f`.

    axis 0x01 split 0x7f GAS BRAKE

In the above example, if the value of `ABS_Y` is `0x7d` then `AXIS_GAS` is set
to `2` (`0x7f - 0x7d`) and `AXIS_BRAKE` is set to `0`.  Conversely, if the value of
`ABS_Y` is `0x83` then `AXIS_GAS` is set to `0` and `AXIS_BRAKE` is set to `4`
(`0x83 - 0x7f`).  Finally, if the value of `ABS_Y` equals the split value of `0x7f`
then both `AXIS_GAS` and `AXIS_BRAKE` are set to `0`.

#### Inverted Axes ####

An inverted axis inverts the sign of the axis value.

The following declaration maps `ABS_RZ` (indicated by `0x05`) to `AXIS_BRAKE`
(indicated by `BRAKE`), and inverts the output by negating it.

    axis 0x05 invert AXIS_RZ

In the above example, if the value of `ABS_RZ` is `2` then `AXIS_RZ` is set to `-2`.

#### Center Flat Position Option ####

The Linux input protocol provides a way for input device drivers to specify the
center flat position of joystick axes but not all of them do and some of them
provide incorrect values.

The center flat position is the neutral position of the axis, such as when
a directional pad is in the very middle of its range and the user is not
touching it.

To resolve this issue, an axis declaration may be followed by a `flat`
option that specifies the value of the center flat position for the axis.

    axis 0x03 Z flat 4096

In the above example, the center flat position is set to `4096`.

### Comments ###

Comment lines begin with '#' and continue to the end of the line.  Like this:

    # A comment!

Blank lines are ignored.

### Examples ###

#### Keyboard ####

    # This is an example of a key layout file for a keyboard.

    key 1     ESCAPE
    key 2     1
    key 3     2
    key 4     3
    key 5     4
    key 6     5
    key 7     6
    key 8     7
    key 9     8
    key 10    9
    key 11    0
    key 12    MINUS
    key 13    EQUALS
    key 14    DEL

    # etc...

#### System Controls ####

    # This is an example of a key layout file for basic system controls, such as
    # volume and power keys which are typically implemented as GPIO pins that
    # the device decodes into key presses.

    key 114   VOLUME_DOWN       WAKE
    key 115   VOLUME_UP         WAKE
    key 116   POWER             WAKE

#### Capacitive Buttons ####

    # This is an example of a key layout file for a touch device with capacitive buttons.

    key 139    MENU           VIRTUAL
    key 102    HOME           VIRTUAL
    key 158    BACK           VIRTUAL
    key 217    SEARCH         VIRTUAL

#### Headset Jack Media Controls ####

    # This is an example of a key layout file for headset mounted media controls.
    # A typical headset jack interface might have special control wires or detect known
    # resistive loads as corresponding to media functions or volume controls.
    # This file assumes that the driver decodes these signals and reports media
    # controls as key presses.

    key 163   MEDIA_NEXT        WAKE
    key 165   MEDIA_PREVIOUS    WAKE
    key 226   HEADSETHOOK       WAKE

#### Joystick ####

    # This is an example of a key layout file for a joystick.

    # These are the buttons that the joystick supports, represented as keys.
    key 304   BUTTON_A
    key 305   BUTTON_B
    key 307   BUTTON_X
    key 308   BUTTON_Y
    key 310   BUTTON_L1
    key 311   BUTTON_R1
    key 314   BUTTON_SELECT
    key 315   BUTTON_START
    key 316   BUTTON_MODE
    key 317   BUTTON_THUMBL
    key 318   BUTTON_THUMBR

    # Left and right stick.
    # The reported value for flat is 128 out of a range from -32767 to 32768, which is absurd.
    # This confuses applications that rely on the flat value because the joystick actually
    # settles in a flat range of +/- 4096 or so.  We override it here.
    axis 0x00 X flat 4096
    axis 0x01 Y flat 4096
    axis 0x03 Z flat 4096
    axis 0x04 RZ flat 4096

    # Triggers.
    axis 0x02 LTRIGGER
    axis 0x05 RTRIGGER

    # Hat.
    axis 0x10 HAT_X
    axis 0x11 HAT_Y

## Wake Keys ##

Wake keys are special keys that wake the device from sleep, such as the power key.

By default, for internal keyboard devices, no key is a wake key.  For external
keyboard device, all keys are wake keys.

To make a key be a wake key, set the `WAKE_DROPPED` flag in the key layout file
for the keyboard device.

Note that the `WindowManagerPolicy` component is responsible for implementing wake
key behavior.  Moreover, the key guard may prevent certain keys from functioning
as wake keys.  A good place to start understanding wake key behavior is
`PhoneWindowManager.interceptKeyBeforeQueueing`.

## Virtual Soft Keys ##

The input system provides special features for implementing virtual soft keys.

There are three cases:

1.  If the virtual soft keys are displayed graphically on the screen, as on the
    Galaxy Nexus, then they are implemented by the Navigation Bar component in
    the System UI package.

    Because graphical virtual soft keys are implemented at a high layer in the
    system, key layout files are not involved and the following information does
    not apply.

2.  If the virtual soft keys are implemented as an extended touchable region
    that is part of the main touch screen, as on the Nexus One, then the
    input system uses a virtual key map file to translate X / Y touch coordinates
    into Linux key codes, then uses the key layout file to translate
    Linux key codes into Android key codes.

    Refer to the section on [Touch Devices](/tech/input/touch-devices.html)
    for more details about virtual key map files.

    The key layout file for the touch screen input device must specify the
    appropriate key mapping and include the `VIRTUAL` flag for each key.

3.  If the virtual soft keys are implemented as capacitive buttons that are
    separate from the main touch screen, as on the Nexus S, then the kernel
    device driver or firmware is responsible for translating touches into
    Linux key codes which the input system then translates into Android
    key codes using the key layout file.

    The key layout file for the capacitive button input device must specify the
    appropriate key mapping and include the `VIRTUAL` flag for each key.

When virtual soft key are located within or in close physical proximity of the
touch screen, it is easy for the user to accidentally press one of the buttons
when touching near the bottom of the screen or when sliding a finger from top
to bottom or from bottom to top on the screen.

To prevent this from happening, the input system applies a little debouncing
such that virtual soft key presses are ignored for a brief period of time
after the most recent touch on the touch screen.  The delay is called the
virtual key quiet time.

To enable virtual soft key debouncing, we must do two things.

First, we provide a key layout file for the touch screen or capacitive button
input device with the `VIRTUAL` flag set for each key.

    key 139    MENU           VIRTUAL
    key 102    HOME           VIRTUAL
    key 158    BACK           VIRTUAL
    key 217    SEARCH         VIRTUAL

Then, we set the value of the virtual key quiet time in a resource overlay
for the framework `config.xml` resource.

    <!-- Specifies the amount of time to disable virtual keys after the screen is touched
         in order to filter out accidental virtual key presses due to swiping gestures
         or taps near the edge of the display.  May be 0 to disable the feature.
         It is recommended that this value be no more than 250 ms.
         This feature should be disabled for most devices. -->
    <integer name="config_virtualKeyQuietTimeMillis">250</integer>

## Validation ##

Make sure to validate your key layout files using the
[Validate Keymaps](/tech/input/validate-keymaps.html) tool.
