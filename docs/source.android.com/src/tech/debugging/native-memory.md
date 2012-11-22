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

# Debugging Native Memory Use #

This tip assume that you are working with an eng
or userdebug build of the platform, not on a production device.

Android's native memory allocator has some useful debugging features.  You
can turn on memory tracking with:

      $ adb shell setprop libc.debug.malloc 1
      $ adb shell stop
      $ adb shell start

You need to restart the runtime so that zygote and all processes launched from
it are restarted with the property set.  Now all Dalvik processes have memory
tracking turned on.  You can look at these with DDMS, but first you need to
turn on its native memory UI:

  - Open ~/.android/ddms.cfg
  - Add a line "native=true"

Upon relaunching DDMS and selecting a process, you can switch to the new
native allocation tab and populate it with a list of allocations.  This is
especially useful for debugging memory leaks.
