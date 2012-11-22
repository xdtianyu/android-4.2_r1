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

# Input Device Configuration Files #

Input device configuration files (`.idc` files) contain device-specific
configuration properties that affect the behavior of input devices.

Input device configuration files are typically not necessary for standard
peripherals such as HID keyboards and mice since the default system behavior
usually ensures that they will work out of the box.  On the other hand,
built-in embedded devices, particularly touch screens, almost always
require input device configuration files to specify their behavior.

## Rationale ##

Android automatically detects and configures most input device capabilities
based on the event types and properties that are reported by the associated
Linux kernel input device driver.

For example, if an input device supports the `EV_REL` event type and codes
`REL_X` and `REL_Y` as well as the `EV_KEY` event type and `BTN_MOUSE`,
then Android will classify the input device as a mouse.  The default behavior
for a mouse is to present an on-screen cursor which tracks the mouse's movements
and simulates touches when the mouse is clicked.  Although the mouse can
be configured differently, the default behavior is usually sufficient for
standard mouse peripherals.

Certain classes of input devices are more ambiguous.  For example, multi-touch
touch screens and touch pads both support the `EV_ABS` event type and codes
`ABS_MT_POSITION_X` and `ABS_MT_POSITION_Y` at a minimum.  However, the intended
uses of these devices are quite different and cannot always be determined
automatically.  Also, additional information is required to make sense of the
pressure and size information reported by touch devices.  Hence touch devices,
especially built-in touch screens, usually need IDC files.

## Location ##

Input device configuration files are located by USB vendor, product (and
optionally version) id or by input device name.

The following paths are consulted in order.

*   `/system/usr/idc/Vendor_XXXX_Product_XXXX_Version_XXXX.idc`
*   `/system/usr/idc/Vendor_XXXX_Product_XXXX.idc`
*   `/system/usr/idc/DEVICE_NAME.idc`
*   `/data/system/devices/idc/Vendor_XXXX_Product_XXXX_Version_XXXX.idc`
*   `/data/system/devices/idc/Vendor_XXXX_Product_XXXX.idc`
*   `/data/system/devices/idc/DEVICE_NAME.idc`

When constructing a file path that contains the device name, all characters
in the device name other than '0'-'9', 'a'-'z', 'A'-'Z', '-' or '\_' are replaced by '\_'.

## Syntax ##

An input device configuration file is a plain text file consisting of property
assignments and comments.

### Properties ###

Property assignments each consist of a property name, an `=`, a property value,
and a new line.  Like this:

    property = value

Property names are non-empty literal text identifiers.  They must not contain
whitespace.  Each components of the input system defines a set of properties
that are used to configure its function.

Property values are non-empty string literals, integers or floating point numbers.
They must not contain whitespace or the reserved characters `\` or `"`.

Property names and values are case-sensitive.

### Comments ###

Comment lines begin with '#' and continue to the end of the line.  Like this:

    # A comment!

Blank lines are ignored.

### Example ###

    # This is an example of an input device configuration file.
    # It might be used to describe the characteristics of a built-in touch screen.

    # This is an internal device, not an external peripheral attached to the USB
    # or Bluetooth bus.
    device.internal = 1

    # The device should behave as a touch screen, which uses the same orientation
    # as the built-in display.
    touch.deviceType = touchScreen
    touch.orientationAware = 1

    # Additional calibration properties...
    # etc...

## Common Properties ##

The following properties are common to all input device classes.

Refer to the documentation of each input device class for information about the
special properties used by each class.

#### `device.internal` ####

*Definition:* `device.internal` = `0` | `1`

Specifies whether the input device is an internal built-in component as opposed to an
externally attached (most likely removable) peripheral.

*   If the value is `0`, the device is external.

*   If the value is `1`, the device is internal.

*   If the value is not specified, the default value is `0` for all devices on the
    USB (BUS_USB) or Bluetooth (BUS_BLUETOOTH) bus, `1` otherwise.

This property determines default policy decisions regarding wake events.

Internal input devices generally do not wake the display from sleep unless explicitly
configured to do so in the key layout file or in a hardcoded policy rule.  This
distinction prevents key presses and touches from spuriously waking up your phone
when it is in your pocket.  Usually there are only a small handful of wake keys defined.

Conversely, external input devices usually wake the device more aggressively because
they are assumed to be turned off or not plugged in during transport.  For example,
pressing any key on an external keyboard is a good indicator that the user wants the
device to wake up and respond.

It is important to ensure that the value of the `device.internal` property is set
correctly for all internal input devices.

## Validation ##

Make sure to validate your input device configuration files using the
[Validate Keymaps](/tech/input/validate-keymaps.html) tool.
