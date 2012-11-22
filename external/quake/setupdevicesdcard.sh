#!/bin/bash
#
# Copy Quake's data files from perforce to the Android device's sd card.
# Based on emulator.sh
#

# We need some internal functions defined in envsetup.sh, so start
# by finding this file and sourcing it before anything else
#
function gettop
{
    echo $TOP
}

T=$(gettop)
if [ -z "$T" ] ; then
    echo "please run your envsetup.sh script"
    exit 1
fi

echo "top found at $T"

echo "Creating Quake directories on the device's sdcard"

adb shell mkdir /sdcard
adb shell mkdir /sdcard/data
adb shell mkdir /sdcard/data/quake
adb shell mkdir /sdcard/data/quake/id1

echo "Copying Quake data files to the device. (This could take several minutes)"
adb push $T/external/quake/quake/app/id1 /sdcard/data/quake/id1
echo "Done."
