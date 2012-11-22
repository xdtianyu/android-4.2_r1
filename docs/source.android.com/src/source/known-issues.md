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

# Known Issues #

Even with our best care, small problems sometimes slip in. This page keeps
track of the known issues around using the Android source code.

## Missing CTS Native XML Generator ##

**Symptom**: On some builds of IceCreamSandwich and later, the following
warning is printed early during the build:
`/bin/bash: line 0: cd: cts/tools/cts-native-xml-generator/src/res: No
such file or directory`

**Cause**: Some makefile references that path, which doesn't exist.

**Fix**: None. This is a harmless warning.

## Black Gingerbread Emulator ##

**Symptom**: The emulator built directly from the gingerbread branch
doesn't start and stays stuck on a black screen.

**Cause**: The gingerbread branch uses version R7 of the emulator,
which doesn't have all the features necessary to run recent versions
of gingerbread.

**Fix**: Use version R12 of the emulator, and a newer kernel that matches
those tools. No need to do a clean build.

    $ repo forall platform/external/qemu -c git checkout aosp/tools_r12
    $ make
    $ emulator -kernel prebuilt/android-arm/kernel/kernel-qemu-armv7

## Emulator built on MacOS 10.7 Lion doesn't work. ##

**Symptom**: The emulator (any version) built on MacOS 10.7 Lion
and/or on XCode 4.x doesn't start.

**Cause**: Some change in the development environment causes
the emulator to be compiled in a way that prevents it from working.

**Fix**: Use an emulator binary from the SDK, which is built on
MacOS 10.6 with XCode 3 and works on MacOS 10.7.

## Difficulties syncing the source code (proxy issues). ##

**Symptom**: `repo init` or `repo sync` fail with http errors,
typically 403 or 500.

**Cause**: There are quite a few possible causes, most often
related to http proxies, which have difficulties handling the
large amounts of data getting transfered.

**Fix**: While there's no general solution, using python 2.7
and explicitly using `repo sync -j1` have been reported to
improve the situation for some users.

## Difficulties syncing the source tree (VirtualBox Ethernet issues). ##

**Symptom**: When running `repo sync` in some VirtualBox installations,
the process hangs or fails with a variety of possible symptoms.
One such symptom is
`DownloadError: HTTP 500 (Internal Server Error: Server got itself in trouble)`.

**Cause**: The default network behavior of VirtualBox is to use
NAT (Network Addrss Translation) to connect the guest system to
the network. The heavy network activity of repo sync triggers some
corner cases in the NAT code.

**Fix**: Configure VirtualBox to use bridged network instead of NAT.

## Difficulties syncing the source tree (DNS issues). ##

**Symptom**: When running `repo sync`, the process fails with
various errors related to not recognizing the hostname. One such
error is `<urlopen error [Errno -2] Name or service not known>`.

**Cause**: Some DNS systems have a hard time coping with the
high number of queries involved in syncing the source tree
(there can be several hundred requests in a worst-case scenario).

**Fix**: Manually resolve the relevant hostnames, and hard-code
those results locally.

You can resolve them with the `nslookup` command, which will give
you one numerical IP address for each of those (typically in the
"Address" part of the output).

    $ nslookup googlesource.com
    $ nslookup android.googlesource.com

You can then hard-code them locally by editing `/etc/hosts`, and
adding two lines in that file, of the form:

    aaa.bbb.ccc.ddd googlesource.com
    eee.fff.ggg.hhh android.googlesource.com

Note that this will only work as long as the servers' addresses
don't change, and if they do and you can't connect you'll have
to resolve those hostnames again and edit `etc/hosts` accordingly.

## Difficulties syncing the source tree (TCP issues). ##

**Symptom**: `repo sync` hangs while syncing, often when it's
completed 99% of the sync.

**Cause**: Some settings in the TCP/IP stack cause difficulties
in some network environments, such that `repo sync` neither completes
nor fails.

**Fix**: On linux, `sysctl -w net.ipv4.tcp_window_scaling=0`. On
MacOS, disable the rfc1323 extension in the network settings.

## `make snod` and emulator builds. ##

**Symptom**: When using `make snod` (make system no dependencies)
on emulator builds, the resulting build doesn't work.

**Cause**: All emulator builds now run Dex optimization at build
time by default, which requires to follow all dependencies to
re-optimize the applications each time the framework changes.

**Fix**: Locally disable Dex optimizations with
`export WITH_DEXPREOPT=false`, delete the existing optimized
versions with `make installclean` and run a full build to
re-generate non-optimized versions. After that, `make snod`
will work.

## "Permission Denied" during builds. ##

**Symptom**: All builds fail with "Permission Denied", possibly
along with anti-virus warnings.

**Cause**: Some anti-virus programs mistakenly recognize some
source files in the Android source tree as if they contained
viruses.

**Fix**: After verifying that there are no actual viruses
involved, disable anti-virus on the Android tree. This has
the added benefit of reducing build times.

## Camera, GPS and NFC don't work on Galaxy Nexus. ##

**Symptom**: Camera, GPS and NFC don't work on Galaxy Nexus.
As an example, the Camera application crashes as soon as it's
launched.

**Cause**: Those hardware peripherals require proprietary
libraries that aren't available in the Android Open Source
Project.

**Fix**: None.

## Build errors related to using the wrong compiler. ##

**Symptom**: The build fails with various symptoms. One
such symptom is `cc1: error: unrecognized command line option "-m32"`

**Cause**: The Android build system uses the default compiler
in the PATH, assuming it's a suitable compiler to generate
binaries that run on the host. Other situations (e.g. using
the Android NDK or building the kernel) cause the default
compiler to not be a host compiler.

**Fix**: Use a "clean" shell, in which no previous
actions could have swapped the default compiler.

## Build errors caused by non-default tool settings. ##

**Symptom**: The build fails with various symptoms, possibly
complinaing about missing files or files that have the
wrong format. One such symptom is `member [...] in archive is not an object`.

**Cause**: The Android build system tends to use many host tools
and to rely on their default behaviors. Some settings change
those tools' behaviors and make them behave in ways that
confuse the build system. Variables known to cause such
issues are `CDPATH` and `GREP_OPTIONS`.

**Fix**: Build Android in an environment that has as few
customizations as possible.

## Build error with 4.0.x and earlier on MacOS 10.7. ##

**Symptom**: Building IceCreamSandwich 4.0.x (and older
versions) fails on MacOS 10.7 with errors similar to this:
`Undefined symbols for architecture i386: "_SDL_Init"`

**Cause**: 4.0.x is not compatible with MacOS 10.7.

**Fix**: Either downgrade to MacOS 10.6, or use the master
branch, which can be built on MacOS 10.7.

    $ repo init -b master
    $ repo sync

## Build error on MacOS with XCode 4.3. ##

**Symptom**: All builds fail when using XCode 4.3.

**Cause**: XCode 4.3 switched the default compiler from
gcc to llvm, and llvm rejects code that used to be
accepted by gcc.

**Fix**: Use XCode 4.2.

## Build error with 4.0.x and earlier on Ubuntu 11.10. ##

**Symptom**: Building IceCreamSandwich 4.0.x (and older
versions) on Ubuntu 11.10 and newer fails with errors similar to this:
`<command-line>:0:0: warning: "_FORTIFY_SOURCE" redefined [enabled by default]`

**Cause**: Ubuntu 11.10 uses a version of gcc where that symbol
is defined by default, and Android also defines that symbol,
which causes a conflict.

**Fix**: Either downgrade to Ubuntu 10.04, or use the master
branch, which can be compiled on Ubuntu 11.10 and newer.

    $ repo init -b master
    $ repo sync
