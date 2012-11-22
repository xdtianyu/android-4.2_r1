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

# Compatibility Program Overview #

The Android compatibility program makes it easy for mobile device
manufacturers to develop compatible Android devices.

# Program goals #

The Android compatibility program works for the benefit of the entire
Android community, including users, developers, and device manufacturers.

Each group depends on the others. Users want a wide selection of devices
and great apps; great apps come from developers motivated by a large market
for their apps with many devices in users' hands; device manufacturers rely
on a wide variety of great apps to increase their products' value for
consumers.

Our goals were designed to benefit each of these groups:

- *Provide a consistent application and hardware environment to application
developers.* 
    Without a strong compatibility standard, devices can vary so
greatly that developers must design different versions of their applications
for different devices. The compatibility program provides a precise definition
of what developers can expect from a compatible device in terms of APIs and
capabilities. Developers can use this information to make good design
decisions, and be confident that their apps will run well on any compatible
device.

- *Enable a consistent application experience for consumers.*
    If an application runs well on one compatible Android device, it should run well on
any other device that is compatible with the same Android platform version.
Android devices will differ in hardware and software capabilities, so the
compatibility program also provides the tools needed for distribution systems
such as Google Play to implement appropriate filtering. This means that
users can only see applications which they can actually run.

- *Enable device manufacturers to differentiate while being
compatible.*
    The Android compatibility program focuses on the aspects of
Android relevant to running third-party applications, which allows device
manufacturers the flexibility to create unique devices that are nonetheless
compatible.

- *Minimize costs and overhead associated with compatibility.*
    Ensuring compatibility should be easy and inexpensive to
device manufacturers. The testing tool (CTS) is free, open source, and
available for [download](downloads.html). 
CTS is designed to be used for continuous self-testing
during the device development process to eliminate the cost of changing your
workflow or sending your device to a third party for testing. Meanwhile, there
are no required certifications, and thus no corresponding costs and
fees.

The Android compatibility program consists of three key components:

- The source code to the Android software stack
- The Compatilbility Definition Document, representing the "policy" aspect of compatibility
- The Compatilbility Test Suite, representing the "mechanism" of compatibility

Just as each version of the Android platform exists in a separate branch in
the source code tree, there is a separate CTS and CDD for each version as
well. The CDD, CTS, and source code are -- along with your hardware and your
software customizations -- everything you need to create a compatible device.

# Compatibility Definition Document (CDD) #

For each release of the Android platform, a detailed Compatibility
Definition Document (CDD) will be provided. The CDD represents the "policy"
aspect of Android compatibility.

No test suite, including CTS, can truly be comprehensive. For instance, the
CTS includes a test that checks for the presence and correct behavior of
OpenGL graphics APIs, but no software test can verify that the graphics
actually appear correctly on the screen. More generally, it's impossible to
test the presence of hardware features such as keyboards, display density,
WiFi, and Bluetooth.

The CDD's role is to codify and clarify specific requirements, and
eliminate ambiguity.  The CDD does not attempt to be comprehensive. Since
Android is a single corpus of open-source code, the code itself is the
comprehensive "specification" of the platform and its APIs. The CDD acts as a
"hub", referencing other content (such as SDK API documentation) that provides
a framework in which the Android source code may be used so that the end
result is a compatible system.

If you want to build a device compatible with a given Android version,
start by checking out the source code for that version, and then read the
corresponding CDD and stay within its guidelines. For additional details,
simply examine [the latest CDD](4.1/android-4.1-cdd.pdf).

# Compatibility Test Suite (CTS) #

The CTS is a free, commercial-grade test suite, available for
[download](downloads.html).
The CTS represents the "mechanism" of compatibility.

The CTS runs on a desktop machine and executes test cases directly on
attached devices or an emulator. The CTS is a set of unit tests designed to be
integrated into the daily workflow (such as via a continuous build system) of
the engineers building a device. Its intent is to reveal incompatibilities
early on, and ensure that the software remains compatible throughout the
development process.


# Compatibility Test Suite Verifier (CTS Verifier) #
The Compatibility Test Suite Verifier (CTS Verifier) is a supplement to the
Compatibility Test Suite (CTS), available for [download](downloads.html).
CTS Verifier provides tests for APIs and functions that cannot be tested on a
stationary device without manual input (e.g. audio quality, accelerometer, etc).

For details on the CTS, consult the [CTS introduction](cts-intro.html).
