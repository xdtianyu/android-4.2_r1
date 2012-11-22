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

# Building the System #

The basic sequence of build commands is as follows:

## Initialize ##

Initialize the environment with the `envsetup.sh` script. Note
that replacing "source" with a single dot saves a few characters,
and the short form is more commonly used in documentation.

    $ source build/envsetup.sh

or
    
    $ . build/envsetup.sh

## Choose a Target ##

Choose which target to build with `lunch`.  The exact configuration can be passed as
an argument, e.g. 
    
    $ lunch full-eng

The example above refers to a complete build for the emulator, with all debugging enabled.

If run with no arguments `lunch` will prompt you to choose a target from the menu. 

All build targets take the form BUILD-BUILDTYPE, where the BUILD is a codename
referring to the particular feature combination. Here's a partial list:

Build name  | Device   | Notes
------------|----------|---------------------------
full        | emulator | fully configured with all languages, apps, input methods
full_maguro | maguro   | `full` build running on Galaxy Nexus GSM/HSPA+ ("maguro")
full_panda  | panda    | `full` build running on PandaBoard ("panda")

and the BUILDTYPE is one of the following:

Buildtype   | Use
------------|--------------------------------------
user        | limited access; suited for production
userdebug   | like "user" but with root access and debuggability; preferred for debugging
eng         | development configuration with additional debugging tools

For more information about building for and running on actual hardware, see
[Building for devices](building-devices.html)

## Build the Code ##

Build everything with `make`. GNU make can handle parallel
tasks with a `-jN` argument, and it's common to use a number of
tasks N that's between 1 and 2 times the number of hardware
threads on the computer being used for the build. E.g. on a
dual-E5520 machine (2 CPUs, 4 cores per CPU, 2 threads per core),
the fastest builds are made with commands between `make -j16` and
`make -j32`.

    $ make -j4

## Run It! ##

You can either run your build on an emulator or flash it on a device. Please note that you have already selected your build target with `lunch`, and it is unlikely at best to run on a different target than it was built for.

### Flash a Device ###

To flash a device, you will need to use `fastboot`, which should be included in your path after a successful build. Place the device in fastboot mode either manually by holding the appropriate key combination at boot, or from the shell with

    $ adb reboot bootloader

Once the device is in fastboot mode, run 

    $ fastboot flashall -w

The `-w` option wipes the `/data` partition on the device; this is useful for your first time flashing a particular device, but is otherwise unnecessary.

For more information about building for and running on actual hardware, see
[Building for devices](building-devices.html)

### Emulate an Android Device ###

The emulator is added to your path automatically by the build process. To run the emulator, type

    $ emulator

# Using ccache #

ccache is a compiler cache for C and C++ that can help make builds faster.
In the root of the source tree, do the following:

    $ export USE_CCACHE=1
    $ export CCACHE_DIR=/<path_of_your_choice>/.ccache
    $ prebuilts/misc/linux-x86/ccache/ccache -M 50G

The suggested cache size is 50-100G.

You can watch ccache being used by doing the following:

    $ watch -n1 -d prebuilts/misc/linux-x86/ccache/ccache -s

On OSX, you should replace `linux-x86` with `darwin-x86`.

When using Ice Cream Sandwich (4.0.x) or older, you should replace
`prebuilts/misc` with `prebuilt`.

# Troubleshooting Common Build Errors #

## Wrong Java Version ##

If you are attempting to build froyo or earlier with Java 1.6, or gingerbread or later
with Java 1.5, `make` will abort with a message such as

    ************************************************************
    You are attempting to build with the incorrect version
    of java.
 
    Your version is: WRONG_VERSION.
    The correct version is: RIGHT_VERSION.
 
    Please follow the machine setup instructions at
        https://source.android.com/source/download.html
    ************************************************************

This may be caused by

- failing to install the correct JDK as specified on the [Initializing](initializing.html) page.  Building Android requires Sun JDK 5 or 6 depending on which release you are building.  

- another JDK that you previously installed appearing in your path.  You can remove the offending JDK from your path with:

        $ export PATH=${PATH/\/path\/to\/jdk\/dir:/}

## Python Version 3 ##

Repo is built on particular functionality from Python 2.x and is unfortunately incompatible with Python 3.  In order to use repo, please install Python 2.x:

    $ apt-get install python

## Case Insensitive Filesystem ##

If you are building on an HFS filesystem on Mac OS X, you may encounter an error such as

    ************************************************************
    You are building on a case-insensitive filesystem.
    Please move your source tree to a case-sensitive filesystem.
    ************************************************************

Please follow the instructions on the [Initializing](initializing.html) page for creating a case-sensitive disk image.

## No USB Permission ##

On most Linux systems, unprivileged users cannot access USB ports by default. If you see a permission denied error, follow the instructions on the [Initializing](initializing.html) page for configuring USB access.  

If adb was already running and cannot connect to the device after
getting those rules set up, it can be killed with `adb kill-server`.
That will cause adb to restart with the new configuration.

