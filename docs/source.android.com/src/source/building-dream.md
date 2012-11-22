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

# Building for an Android Dev Phone #

*The information on this page is a bit out of date. We'll update this
page as soon as we can.*

The basic manifest for 1.6 defines which projects are
needed to do a generic build for the emulator or for unlocked Dream devices
(e.g. the Android Dev Phone 1). You need to have an appropriate device running
a matching official image.

To build donut for dream (your
device needs to be an ADP1 running an official 1.6 system):

1. Follow the [normal steps](downloading.html) to setup repo and check out the sources.

2. At the root of your source tree, run `. build/envsetup.sh` like you normally would for an emulator build.

3. Run `make adb` if you don't already have adb in your path.

4. run `adb root`.

5. in `vendor/htc/dream-open/` there is a script called "extract-files.sh" that must be run (from that directory) to extract some proprietary binaries from your device (*). You only need to do this once.

6. run `lunch aosp_dream_us-eng` to specifically configure the build system for dream (the default is the equivalent of "lunch generic-eng", which doesn't contain dream-specific files).

7. run make from the top of the source tree.

8. from this point, the fastboot tool (which is put automatically in your path) can be used to flash a device: boot the device into the bootloader by holding the back key while pressing the power key, and run `fastboot -w flashall`.

Note: these instructions work for the sapphire (ADP2) build target, as
well. Simply replace "dream" with "sapphire" above.

