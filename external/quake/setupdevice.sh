#!/bin/bash
#
# Copy Quake's data files from perforce to the Android device's data directory.
# Based on emulator.sh
#

# This funcion is copied from envsetup.sh

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


echo "Creating Quake directories on the device"

adb shell mkdir /data
adb shell mkdir /data/quake
adb shell mkdir /data/quake/id1

echo "Copying Quake data files to the device. (This could take several minutes)"
adb push $T/external/quake/quake/app/id1 /data/quake/id1
echo "Done."
