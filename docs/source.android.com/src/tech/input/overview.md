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

# Overview #

The Android input subsystem nominally consists of an event pipeline
that traverses multiple layers of the system.

## Input Pipeline ##

At the lowest layer, the physical input device produces signals that
describe state changes such as key presses and touch contact points.
The device firmware encodes and transmits these signals in some way
such as by sending USB HID reports to the system or by producing
interrupts on an I2C bus.

The signals are then decoded by a device driver in the Linux kernel.
The Linux kernel provides drivers for many standard peripherals,
particularly those that adhere to the HID protocol.  However, an OEM
must often provide custom drivers for embedded devices that are
tightly integrated into the system at a low-level, such as touch screens.

The input device drivers are responsible for translating device-specific
signals into a standard input event format, by way of the Linux
input protocol.  The Linux input protocol defines a standard set of
event types and codes in the `linux/input.h` kernel header file.
In this way, components outside the kernel do not need to care about
the details such as physical scan codes, HID usages, I2C messages,
GPIO pins, and the like.

Next, the Android `EventHub` component reads input events from the kernel
by opening the `evdev` driver associated with each input device.
The Android InputReader component then decodes the input events
according to the device class and produces a stream of Android input
events.  As part of this process, the Linux input protocol event codes
are translated into Android event codes according to the
input device configuration, keyboard layout files, and various
mapping tables.

Finally, the `InputReader` sends input events to the InputDispatcher
which forwards them to the appropriate window.

## Control Points ##

There are several stages in the input pipeline which effect control
over the behavior of the input device.

### Driver and Firmware Configuration ###

Input device drivers frequently configure the behavior of the input
device by setting parameters in registers or even uploading the
firmware itself.  This is particularly the case for embedded
devices such as touch screens where a large part of the calibration
process involves tuning these parameters or fixing the firmware
to provide the desired accuracy and responsiveness and to suppress
noise.

Driver configuration options are often specified as module parameters
in the kernel board support package (BSP) so that the same driver
can support multiple different hardware implementations.

This documentation does attempt to describe driver or firmware
configuration, but it does offer guidance as to device calibration
in general.

### Board Configuration Properties ###

The kernel board support package (BSP) may export board configuration
properties via SysFS that are used by the Android InputReader component,
such as the placement of virtual keys on a touch screen.

Refer to the device class sections for details about how different
devices use board configuration properties.

### Resource Overlays ###

A few input behaviors are configured by way of resource overlays
in `config.xml` such as the operation of lid switch.

Here are a few examples:

*   `config_lidKeyboardAccessibility`: Specifies the effect of the
    lid switch on whether the hardware keyboard is accessible or hidden.

*   `config_lidNavigationAccessibility`: Specifies the effect of the
    lid switch on whether the trackpad is accessible or hidden.

*   `config_longPressOnPowerBehavior`: Specifies what should happen when
    the user holds down the power button.

*   `config_lidOpenRotation`: Specifies the effect of the lid switch
    on screen orientation.

Refer to the documentation within `frameworks/base/core/res/res/values/config.xml`
for details about each configuration option.

### Key Maps ###

Key maps are used by the Android `EventHub` and `InputReader` components
to configure the mapping from Linux event codes to Android event codes
for keys, joystick buttons and joystick axes.  The mapping may
be device or language dependent.

Refer to the device class sections for details about how different
devices use key maps.

### Input Device Configuration Files ###

Input device configuration files are used by the Android `EventHub` and
`InputReader` components to configure special device characteristics
such as how touch size information is reported.

Refer to the device class sections for details about how different
devices use input device configuration maps.

## Understanding HID Usages and Event Codes ##

There are often several different identifiers used to refer to any
given key on a keyboard, button on a game controller, joystick axis
or other control.  The relationships between these identifiers
are not always the same: they are dependent on a set of mapping tables,
some of which are fixed, and some which vary based on characteristics
of the device, the device driver, the current locale, the system
configuration, user preferences and other factors.

Physical Scan Code
:   A physical scan code is a device-specific identifier that is associated
    with each key, button or other control.  Because physical scan codes
    often vary from one device to another, the firmware or device driver
    is responsible for mapping them to standard identifiers such as
    HID Usages or Linux key codes.

    Scan codes are mainly of interest for keyboards.  Other devices
    typically communicate at a low-level using GPIO pins, I2C messages
    or other means.  Consequently, the upper layers of the software
    stack rely on the device drivers to make sense of what is going on.

HID Usage
:   A HID usage is a standard identifier that is used to report the
    state of a control such as a keyboard key, joystick axis,
    mouse button, or touch contact point.  Most USB and Bluetooth
    input devices conform to the HID specification, which enables
    the system to interface with them in a uniform manner.

    The Android Framework relies on the Linux kernel HID drivers to
    translate HID usage codes into Linux key codes and other identifiers.
    Therefore HID usages are mainly of interest to peripheral manufacturers.

Linux Key Code
:   A Linux key code is a standard identifier for a key or button.
    Linux key codes are defined in the `linux/input.h` header file using
    constants that begin with the prefix `KEY_` or `BTN_`.  The Linux
    kernel input drivers are responsible for translating physical
    scan codes, HID usages and other device-specific signals into Linux
    key codes and delivering information about them as part of
    `EV_KEY` events.

    The Android API sometimes refers to the Linux key code associated
    with a key as its "scan code".  This is technically incorrect in
    but it helps to distinguish Linux key codes from Android key codes
    in the API.

Linux Relative or Absolute Axis Code
:   A Linux relative or absolute axis code is a standard identifier
    for reporting relative movements or absolute positions along an
    axis, such as the relative movements of a mouse along its X axis
    or the absolute position of a joystick along its X axis.
    Linux axis code are defined in the `linux/input.h` header file using
    constants that begin with the prefix `REL_` or `ABS_`.  The Linux
    kernel input drivers are responsible for translating HID usages
    and other device-specific signals into Linux axis codes and
    delivering information about them as part of `EV_REL` and
    `EV_ABS` events.

Linux Switch Code
:   A Linux switch code is a standard identifier for reporting the
    state of a switch on a device, such as a lid switch.  Linux
    switch codes are defined in the `linux/input.h` header file
    using constants that begin with the prefix `SW_`.  The Linux
    kernel input drivers report switch state changes as `EV_SW` events.

    Android applications generally do not receive events from switches,
    but the system may use them interally to control various
    device-specific functions.

Android Key Code
:   An Android key code is a standard identifier defined in the Android
    API for indicating a particular key such as 'HOME'.  Android key codes
    are defined by the `android.view.KeyEvent` class as constants that
    begin with the prefix `KEYCODE_`.

    The key layout specifies how Linux key codes are mapped to Android
    key codes.  Different key layouts may be used depending on the keyboard
    model, language, country, layout, or special functions.

    Combinations of Android key codes are transformed into character codes
    using a device and locale specific key character map.  For example,
    when the keys identified as `KEYCODE_SHIFT` and `KEYCODE_A` are both
    pressed together, the system looks up the combination in the key
    character map and finds the capital letter 'A', which is then inserted
    into the currently focused text widget.

Android Axis Code
:   An Android axis code is a standard identifier defined in the Android
    API for indicating a particular device axis.  Android axis codes are
    defined by the `android.view.MotionEvent` class as constants that
    begin with the prefix `AXIS_`.

    The key layout specifies how Linux Axis Codes are mapped to Android
    axis codes.  Different key layouts may be used depending on the device
    model, language, country, layout, or special functions.

Android Meta State
:   An Android meta state is a standard identifier defined in the Android
    API for indicating which modifier keys are pressed.  Android meta states
    are defined by the `android.view.KeyEvent` class as constants that
    begin with the prefix `META_`.

    The current meta state is determined by the Android InputReader
    component which monitors when modifier keys such as `KEYCODE_SHIFT_LEFT`
    are pressed / released and sets / resets the appropriate meta state flag.

    The relationship between modifier keys and meta states is hardcoded
    but the key layout can alter how the modifier keys themselves are
    mapped which in turns affects the meta states.

Android Button State
:   An Android button state is a standard identifier defined in the Android
    API for indicating which buttons (on a mouse or stylus) are pressed.
    Android button states are defined by the `android.view.MotionEvent`
    class as constants that begin with the prefix `BUTTON_`.

    The current button state is determined by the Android InputReader
    component which monitors when buttons (on a mouse or stylus) are
    pressed / released and sets / resets appropriate button state flag.

    The relationship between buttons and button states is hardcoded.

## Further Reading ##

1. [Linux input event codes](http://www.kernel.org/doc/Documentation/input/event-codes.txt)
2. [Linux multi-touch protocol](http://www.kernel.org/doc/Documentation/input/multi-touch-protocol.txt)
3. [Linux input drivers](http://www.kernel.org/doc/Documentation/input/input.txt)
4. [Linux force feedback](http://www.kernel.org/doc/Documentation/input/ff.txt)
5. [HID information, including HID usage tables](http://www.usb.org/developers/hidpage)

