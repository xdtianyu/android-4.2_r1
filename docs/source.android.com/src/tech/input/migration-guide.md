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

# Migration Guide #

This document contains a few helpful tips when migrating to new Android releases.

## Migrating to Android Gingerbread 2.3 ##

In Gingerbread, we added the concept of input device configuration files
(also referred to as input device calibration files in this release).

Make sure to provide an input device configuration file for all touch screens.
In particular, it is worth spending time providing a calibration reference for
touch size information.

## Migrating to Android Honeycomb 3.0 ##

In Honeycomb, we revised the key character map file format and started making
greater use of input device configuration files.  We also added support for full
PC-style keyboards and introduced a new "Generic" key map, which
replaced the older emulator-specific "qwerty" key map (which was never
intended to be used as a general-purpose key map.)

Make sure to update all of your key character map files to use the new syntax.

If your peripherals relied on the old "qwerty" key map, then you
may need to provide new device-specific key maps to emulate the old behavior.
You should create a new key map for each device identified either by
USB product id / vendor id or by device name.

It is especially important to provide key character map files for all special
function input devices.  These files should simple contain a line to set
the keyboard type to `SPECIAL_FUNCTION`.

A good way to ensure that all built-in input devices are appropriately configured
is to run [Dumpsys](/tech/input/dumpsys.html) and look for devices that
are inappropriately using `Generic.kcm`.

## Migrating to Android Honeycomb 3.2 ##

In Honeycomb 3.2, we added support for joysticks and extended the key layout file
format to enable joystick axis mapping.

## Migrating to Android Ice Cream Sandwich 4.0 ##

In Ice Cream Sandwich 4.0, we changed the device driver requirements for touch screens
to follow the standard Linux multitouch input protocol and added support for
protocol "B".  We also support digitizer tablets and stylus-based touch devices.

You will probably need to update your input device driver to implement the Linux
multitouch input protocol correctly according to the standard.

You will also need to update your input device configuration files because some
properties have been changed to be simpler and more systematic.

Refer to [Touch Devices](/tech/input/touch-devices.html) for more details about
driver requirements.
