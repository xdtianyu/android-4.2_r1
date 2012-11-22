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

# Flashing #

To flash a device, you will need to use `fastboot`. Place the device in fastboot mode either manually by holding the appropriate key combination at boot, or from the shell with

    $ adb reboot bootloader

Once the device is in fastboot mode, run 

    $ fastboot flashall -w

The `-w` option wipes the `/data` partition on the device; this is useful for your first time flashing a particular device, but is otherwise unnecessary.

# Emulating #

To run the emulator, type

    $ emulator


