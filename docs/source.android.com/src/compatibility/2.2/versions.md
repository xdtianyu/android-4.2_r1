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

# Permitted Version Strings for Android 2.2 #

As described in Section 3.2.2 of the [Android 2.2 Compatibility Definition](/cdds/android-2.2-cdd.pdf), 
only certain strings are allowable for the system property
`android.os.Build.VERSION.RELEASE`. The reason for this is that
applications and web sites may rely on predictable values for this string, and
so that end users can easily and reliably identify the version of Android
running on their devices.

Because subsequent releases of the Android software may revise this string,
but not change any API behavior, such releases may not be accompanied by a new
Compatibility Definition Document. This page lists the versions that are
allowable by an Android 2.2-based system. The only permitted values for
`android.os.Build.VERSION.RELEASE` for Android 2.2 are:

- 2.2

- 2.2.1

