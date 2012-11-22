<!--
   Copyright 2010 The Android Open Source Project 

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

# Android Compatibility #

Android's purpose is to establish an open platform for developers to build
innovative mobile apps. Three key components work together to realize this
platform.

The Android Compatibility Program defines the technical details of Android
platform and provides tools used by OEMs to ensure that developers' apps run
on a variety of devices. The Android SDK provides built-in tools that
Developers use to clearly state the device features their apps require. And
Google Play shows apps only to those devices that can properly run
them.

These pages describe the Android Compatibility Program and how to get
access to compatibility information and tools.

## Why build compatible Android devices? ##

### Users want a customizable device. ###

A mobile phone is a highly personal, always-on, always-present gateway to
the Internet. We haven't met a user yet who didn't want to customize it by
extending its functionality. That's why Android was designed as a robust
platform for running after-market applications.

### Developers outnumber us all. ###

No device manufacturer can hope to write all the software that a person could
conceivably need. We need third-party developers to write the apps users want,
so the Android Open Source Project aims to make it as easy and open as
possible for developers to build apps.

### Everyone needs a common ecosystem. ###

Every line of code developers write to work around a particular phone's bug
is a line of code that didn't add a new feature. The more compatible phones
there are, the more apps there will be. By building a fully compatible Android
device, you benefit from the huge pool of apps written for Android, while
increasing the incentive for developers to build more of those apps.

## Android compatibility is free, and it's easy. ##

If you are building a mobile device, you can follow these steps to make
sure your device is compatible with Android. For more details about the
Android compatibility program in general, see [the program overview](overview.html).

Building a compatible device is a three-step process:

1. *Obtain the Android software source code*.
    This is [the source code for the Android platform](/source/index.html), that you port to your hardware.

1. *Comply with Android Compatibility Definition Document (CDD)*.
    The CDD enumerates the software and hardware requirements of a compatible Android device.

1. *Pass the Compatibility Test Suite (CTS)*.
    You can use the CTS (included in the Android source code) as an ongoing aid to compatibility during the development process.

# Joining the Ecosystem #

Once you've built a compatible device, you may wish to include Google
Play to provide your users access to the third-party app ecosystem.
Unfortunately, for a variety of legal and business reasons, we aren't able to
automatically license Google Play to all compatible devices. To inquire
about access about Google Play, you can [contact us](contact-us.html).
