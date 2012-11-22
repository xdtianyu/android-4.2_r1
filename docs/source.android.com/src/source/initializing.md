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

# Initializing a Build Environment #

The "Getting Started" section describes how to set up your local work environment, how to use Repo to get the Android files, and how to build the files on your machine.  To build the Android source files, you will need to use Linux or Mac OS. Building under Windows is not currently supported.

*Note: The source download is approximately 8.5GB in size.
You will need over 30GB free to complete a single build, and
up to 100GB (or more) for a full set of builds.*

For an overview of the entire code-review and code-update process, see [Life of a Patch](life-of-a-patch.html).

# Choosing a Branch #

Some of the requirements for your build environment are determined by which
version of the source code you plan to compile. See
[Build Numbers](build-numbers.html) for a full listing of branches you may
choose from. You may also choose to download and build the latest source code
(called "master"), in which case you will simply omit the branch specification
when you initialize the repository.

Once you have selected a branch, follow the appropriate instructions below to
set up your build environment.

# Setting up a Linux build environment #

These instructions apply to all branches, including master.

The Android build is routinely tested in house on recent versions of
Ubuntu LTS (10.04), but most distributions should have the required
build tools available. Reports of successes or failures on other
distributions are welcome.

For Gingerbread (2.3.x) and newer versions, including the master
branch, a 64-bit environment is required. Older versions can be
compiled on 32-bit systems.

*Note: It is also possible to build Android in a virtual machine.
If you are running Linux in a virtual machine, you will need at
least 16GB of RAM/swap and 30GB or more of disk space in order to
build the Android tree.*

Detailed instructions for Ubuntu and MacOS follow. In general you will need:

 - Python 2.5 -- 2.7, which you can download from [python.org](http://www.python.org/download/).

 - GNU Make 3.81 -- 3.82, which you can download from [gnu.org](http://ftp.gnu.org/gnu/make/),

 - JDK 6 if you wish to build Gingerbread or newer; JDK 5 for Froyo or older.  You can download both from [java.sun.com](http://java.sun.com/javase/downloads/).

 - Git 1.7 or newer. You can find it at [git-scm.com](http://git-scm.com/download).

## Installing the JDK ##

The Sun JDK is no longer in Ubuntu's main package repository.  In order to download it, you need to add the appropriate repository and indicate to the system which JDK should be used.

Java 6: for Gingerbread and newer

    $ sudo add-apt-repository "deb http://archive.canonical.com/ lucid partner"
    $ sudo apt-get update
    $ sudo apt-get install sun-java6-jdk

Java 5: for Froyo and older

    $ sudo add-apt-repository "deb http://archive.ubuntu.com/ubuntu hardy main multiverse"
    $ sudo add-apt-repository "deb http://archive.ubuntu.com/ubuntu hardy-updates main multiverse"
    $ sudo apt-get update
    $ sudo apt-get install sun-java5-jdk

*Note: The `lunch` command in the build step will ensure that the Sun JDK is
used instead of any previously installed JDK.*

## Installing required packages (Ubuntu 10.04 -- 11.10) ##

You will need a 64-bit version of Ubuntu.  Ubuntu 10.04 is recommended.
Building using a newer version of Ubuntu is currently only experimentally
supported and is not guaranteed to work on branches other than master.

    $ sudo apt-get install git-core gnupg flex bison gperf build-essential \
      zip curl zlib1g-dev libc6-dev lib32ncurses5-dev ia32-libs \
      x11proto-core-dev libx11-dev lib32readline5-dev lib32z-dev \
      libgl1-mesa-dev g++-multilib mingw32 tofrodos python-markdown \
      libxml2-utils xsltproc

On Ubuntu 10.10:

    $ sudo ln -s /usr/lib32/mesa/libGL.so.1 /usr/lib32/mesa/libGL.so

On Ubuntu 11.10:

    $ sudo apt-get install libx11-dev:i386

## Installing required packages (Ubuntu 12.04) ##

Building on Ubuntu 12.04 is currently only experimentally supported and is not
guaranteed to work on branches other than master.

    $ sudo apt-get install git-core gnupg flex bison gperf build-essential \
      zip curl libc6-dev libncurses5-dev:i386 x11proto-core-dev \
      libx11-dev:i386 libreadline6-dev:i386 libgl1-mesa-glx:i386 \
      libgl1-mesa-dev g++-multilib mingw32 openjdk-6-jdk tofrodos \
      python-markdown libxml2-utils xsltproc zlib1g-dev:i386
    $ sudo ln -s /usr/lib/i386-linux-gnu/mesa/libGL.so.1 /usr/lib/i386-linux-gnu/libGL.so

## Configuring USB Access ##

Under GNU/linux systems (and specifically under Ubuntu systems),
regular users can't directly access USB devices by default. The
system needs to be configured to allow such access.

The recommended approach is to create a file
`/etc/udev/rules.d/51-android.rules` (as the root user) and to copy
the following lines in it. `<username>` must be replaced by the
actual username of the user who is authorized to access the phones
over USB.

    # adb protocol on passion (Nexus One)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="4e12", MODE="0600", OWNER="<username>"
    # fastboot protocol on passion (Nexus One)
    SUBSYSTEM=="usb", ATTR{idVendor}=="0bb4", ATTR{idProduct}=="0fff", MODE="0600", OWNER="<username>"
    # adb protocol on crespo/crespo4g (Nexus S)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="4e22", MODE="0600", OWNER="<username>"
    # fastboot protocol on crespo/crespo4g (Nexus S)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="4e20", MODE="0600", OWNER="<username>"
    # adb protocol on stingray/wingray (Xoom)
    SUBSYSTEM=="usb", ATTR{idVendor}=="22b8", ATTR{idProduct}=="70a9", MODE="0600", OWNER="<username>"
    # fastboot protocol on stingray/wingray (Xoom)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="708c", MODE="0600", OWNER="<username>"
    # adb protocol on maguro/toro (Galaxy Nexus)
    SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", ATTR{idProduct}=="6860", MODE="0600", OWNER="<username>"
    # fastboot protocol on maguro/toro (Galaxy Nexus)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="4e30", MODE="0600", OWNER="<username>"
    # adb protocol on panda (PandaBoard)
    SUBSYSTEM=="usb", ATTR{idVendor}=="0451", ATTR{idProduct}=="d101", MODE="0600", OWNER="<username>"
    # fastboot protocol on panda (PandaBoard)
    SUBSYSTEM=="usb", ATTR{idVendor}=="0451", ATTR{idProduct}=="d022", MODE="0600", OWNER="<username>"
    # usbboot protocol on panda (PandaBoard)
    SUBSYSTEM=="usb", ATTR{idVendor}=="0451", ATTR{idProduct}=="d00f", MODE="0600", OWNER="<username>"
    # usbboot protocol on panda (PandaBoard ES)
    SUBSYSTEM=="usb", ATTR{idVendor}=="0451", ATTR{idProduct}=="d010", MODE="0600", OWNER="<username>"
    # adb protocol on grouper (Nexus 7)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="4e42", MODE="0600", OWNER="<username>"
    # fastboot protocol on grouper (Nexus 7)
    SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", ATTR{idProduct}=="4e40", MODE="0600", OWNER="<username>"

Those new rules take effect the next time a device is plugged in.
It might therefore be necessary to unplug the device and plug it
back into the computer.

This is known to work on both Ubuntu Hardy Heron (8.04.x LTS) and
Lucid Lynx (10.04.x LTS). Other versions of Ubuntu or other
variants of GNU/linux might require different configurations.

<a name="ccache"></a>
## Setting up ccache ##

You can optionally tell the build to use the ccache compilation tool.
Ccache acts as a compiler cache that can be used to speed-up rebuilds.
This works very well if you do "make clean" often, or if you frequently
switch between different build products.

Put the following in your .bashrc or equivalent.

    export USE_CCACHE=1

By default the cache will be stored in ~/.ccache.
If your home directory is on NFS or some other non-local filesystem,
you will want to specify the directory in your .bashrc as well.

    export CCACHE_DIR=<path-to-your-cache-directory>

The suggested cache size is 50-100GB.
You will need to run the following command once you have downloaded
the source code:

    prebuilts/misc/linux-x86/ccache/ccache -M 50G

When building Ice Cream Sandwich (4.0.x) or older, ccache is in
a different location:

    prebuilt/linux-x86/ccache/ccache -M 50G

This setting is stored in the CCACHE_DIR and is persistent.

## Using a separate output directory ##

By default, the output of each build is stored in the out/
subdirectory of the matching source tree.

On some machines with multiple storage devices, builds are
faster when storing the source files and the output on
separate volumes. For additional performance, the output
can be stored on a filesystem optimized for speed instead
of crash robustness, since all files can be re-generated
in case of filesystem corruption.

To set this up, export the `OUT_DIR_COMMON_BASE` variable
to point to the location where your output directories
will be stored.

    export OUT_DIR_COMMON_BASE=<path-to-your-out-directory>

The output directory for each separate source tree will be
named after the directory holding the source tree.

For instance, if you have source trees as `/source/master1`
and `/source/master2` and `OUT_DIR_COMMON_BASE` is set to
`/output`, the output directories will be `/output/master1`
and `/output/master2`.

It's important in that case to not have multiple source
trees stored in directories that have the same name,
as those would end up sharing an output directory, with
unpredictable results.

This is only supported on Jelly Bean (4.1) and newer,
including the master branch.

# Setting up a Mac OS X build environment #

In a default installation, OS X runs on a case-preserving but case-insensitive
filesystem. This type of filesystem is not supported by git and will cause some
git commands (such as "git status") to behave abnormally. Because of this, we
recommend that you always work with the AOSP source files on a case-sensitive
filesystem. This can be done fairly easily using a disk image, discussed below.

Once the proper filesystem is available, building the master branch in a modern
OS X environment is very straightforward. Earlier branches, including ICS,
require some additional tools and SDKs.

### Creating a case-sensitive disk image ###

You can create a case-sensitive filesystem within your existing OS X environment
using a disk image. To create the image, launch Disk
Utility and select "New Image".  A size of 25GB is the minimum to
complete the build, larger numbers are more future-proof. Using sparse images
saves space while allowing to grow later as the need arises. Be sure to select
"case sensitive, journaled" as the volume format.

You can also create it from a shell with the following command:

    # hdiutil create -type SPARSE -fs 'Case-sensitive Journaled HFS+' -size 40g ~/android.dmg

This will create a .dmg (or possibly a .dmg.sparsefile) file which, once mounted, acts as a drive with the required formatting for Android development. For a disk image named "android.dmg" stored in your home directory, you can add the following to your `~/.bash_profile` to mount the image when you execute "mountAndroid":

    # mount the android file image
    function mountAndroid { hdiutil attach ~/android.dmg -mountpoint /Volumes/android; }

Once mounted, you'll do all your work in the "android" volume. You can eject it (unmount it) just like you would with an external drive.

## Master branch ##

To build the latest source in a Mac OS environment, you will need an Intel/x86
machine running MacOS 10.6 (Snow Leopard) or MacOS 10.7 (Lion), along with Xcode
4.2 (Apple's Developer Tools). Although Lion does not come with a JDK, it should
install automatically when you attempt to build the source.

The remaining sections for Mac OS X only apply to those who wish to build
earlier branches.

## Branch 4.0.x and all earlier branches ##

To build android-4.0.x and earlier branches in a Mac OS environment, you need an
Intel/x86 machine running MacOS 10.5 (Leopard) or MacOS 10.6 (Snow Leopard). You
will need the MacOS 10.5 SDK.

### Installing required packages ###

- Install Xcode from [the Apple developer site](http://developer.apple.com/).
We recommend version 3.1.4 or newer, i.e. gcc 4.2.
Version 4.x could cause difficulties.
If you are not already registered as an Apple developer, you will have to
create an Apple ID in order to download.

- Install MacPorts from [macports.org](http://www.macports.org/install.php).

    *Note: Make sure that `/opt/local/bin` appears in your path BEFORE `/usr/bin`.  If not, add* 

        export PATH=/opt/local/bin:$PATH

    *to your `~/.bash_profile`.*

- Get make, git, and GPG packages from MacPorts: 

        $ POSIXLY_CORRECT=1 sudo port install gmake libsdl git-core gnupg

    If using Mac OS 10.4, also install bison:

        $ POSIXLY_CORRECT=1 sudo port install bison

### Reverting from make 3.82 ###

For versions of Android before ICS, there is a bug in gmake 3.82 that prevents android from building.  You can install version 3.81 using MacPorts by taking the following steps:

- Edit `/opt/local/etc/macports/sources.conf` and add a line that says
    
        file:///Users/Shared/dports

    above the rsync line.  Then create this directory: 

        $ mkdir /Users/Shared/dports

- In the new `dports` directory, run 

        $ svn co --revision 50980 http://svn.macports.org/repository/macports/trunk/dports/devel/gmake/ devel/gmake/

- Create a port index for your new local repository: 

        $ portindex /Users/Shared/dports

- Finally, install the old version of gmake with 

        $ sudo port install gmake @3.81

### Setting a file descriptor limit ###

On MacOS the default limit on the number of simultaneous file descriptors open is too low and a highly parallel build process may exceed this limit.  

To increase the cap, add the following lines to your `~/.bash_profile`: 

    # set the number of open files to be 1024
    ulimit -S -n 1024

# Next: Download the source #

Your build environment is good to go!  Proceed to [downloading the source](downloading.html)....
