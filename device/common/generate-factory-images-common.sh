# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if test "$RADIOSRC" = ""
then
  RADIOSRC=radio.img
fi
if test "$BOOTLOADERSRC" = ""
then
  BOOTLOADERSRC=bootloader.img
fi
rm -rf tmp
mkdir -p tmp/$PRODUCT-$VERSION
if test "$BOOTLOADERFILE" = ""
then
  unzip -d tmp ${SRCPREFIX}$PRODUCT-target_files-$BUILD.zip RADIO/$BOOTLOADERSRC
fi
if test "$RADIO" != ""
then
  unzip -d tmp ${SRCPREFIX}$PRODUCT-target_files-$BUILD.zip RADIO/$RADIOSRC
fi
if test "$CDMARADIO" != ""
then
  unzip -d tmp ${SRCPREFIX}$PRODUCT-target_files-$BUILD.zip RADIO/radio-cdma.img
fi
cp ${SRCPREFIX}$PRODUCT-img-$BUILD.zip tmp/$PRODUCT-$VERSION/image-$PRODUCT-$VERSION.zip
if test "$BOOTLOADERFILE" = ""
then
  cp tmp/RADIO/$BOOTLOADERSRC tmp/$PRODUCT-$VERSION/bootloader-$DEVICE-$BOOTLOADER.img
else
  cp $BOOTLOADERFILE tmp/$PRODUCT-$VERSION/bootloader-$DEVICE-$BOOTLOADER.img
fi
if test "$RADIO" != ""
then
  cp tmp/RADIO/$RADIOSRC tmp/$PRODUCT-$VERSION/radio-$DEVICE-$RADIO.img
fi
if test "$CDMARADIO" != ""
then
  cp tmp/RADIO/radio-cdma.img tmp/$PRODUCT-$VERSION/radio-cdma-$DEVICE-$CDMARADIO.img
fi
if test "$SLEEPDURATION" = ""
then
  SLEEPDURATION=5
fi
cat > tmp/$PRODUCT-$VERSION/flash-all.sh << EOF
#!/bin/sh

# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

EOF
if test "$ERASEALL" = "true"
then
cat >> tmp/$PRODUCT-$VERSION/flash-all.sh << EOF
fastboot erase boot
fastboot erase cache
fastboot erase recovery
fastboot erase system
fastboot erase userdata
EOF
fi
cat >> tmp/$PRODUCT-$VERSION/flash-all.sh << EOF
fastboot flash bootloader bootloader-$DEVICE-$BOOTLOADER.img
fastboot reboot-bootloader
sleep $SLEEPDURATION
EOF
if test "$RADIO" != ""
then
cat >> tmp/$PRODUCT-$VERSION/flash-all.sh << EOF
fastboot flash radio radio-$DEVICE-$RADIO.img
fastboot reboot-bootloader
sleep $SLEEPDURATION
EOF
fi
if test "$CDMARADIO" != ""
then
cat >> tmp/$PRODUCT-$VERSION/flash-all.sh << EOF
fastboot flash radio-cdma radio-cdma-$DEVICE-$CDMARADIO.img
fastboot reboot-bootloader
sleep $SLEEPDURATION
EOF
fi
cat >> tmp/$PRODUCT-$VERSION/flash-all.sh << EOF
fastboot -w update image-$PRODUCT-$VERSION.zip
EOF
chmod a+x tmp/$PRODUCT-$VERSION/flash-all.sh
cat > tmp/$PRODUCT-$VERSION/flash-all.bat << EOF
@ECHO OFF
:: Copyright 2012 The Android Open Source Project
::
:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::      http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.

PATH=%PATH%;"%SYSTEMROOT%\System32"
EOF
if test "$ERASEALL" = "true"
then
cat >> tmp/$PRODUCT-$VERSION/flash-all.bat << EOF
fastboot erase boot
fastboot erase cache
fastboot erase recovery
fastboot erase system
fastboot erase userdata
EOF
fi
cat >> tmp/$PRODUCT-$VERSION/flash-all.bat << EOF
fastboot flash bootloader bootloader-$DEVICE-$BOOTLOADER.img
fastboot reboot-bootloader
ping -n $SLEEPDURATION 127.0.0.1 >nul
EOF
if test "$RADIO" != ""
then
cat >> tmp/$PRODUCT-$VERSION/flash-all.bat << EOF
fastboot flash radio radio-$DEVICE-$RADIO.img
fastboot reboot-bootloader
ping -n $SLEEPDURATION 127.0.0.1 >nul
EOF
fi
if test "$CDMARADIO" != ""
then
cat >> tmp/$PRODUCT-$VERSION/flash-all.bat << EOF
fastboot flash radio-cdma radio-cdma-$DEVICE-$CDMARADIO.img
fastboot reboot-bootloader
ping -n $SLEEPDURATION 127.0.0.1 >nul
EOF
fi
cat >> tmp/$PRODUCT-$VERSION/flash-all.bat << EOF
fastboot -w update image-$PRODUCT-$VERSION.zip

echo Press any key to exit...
pause >nul
exit
EOF
cat > tmp/$PRODUCT-$VERSION/flash-base.sh << EOF
#!/bin/sh

# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

fastboot flash bootloader bootloader-$DEVICE-$BOOTLOADER.img
fastboot reboot-bootloader
sleep $SLEEPDURATION
EOF
if test "$RADIO" != ""
then
cat >> tmp/$PRODUCT-$VERSION/flash-base.sh << EOF
fastboot flash radio radio-$DEVICE-$RADIO.img
fastboot reboot-bootloader
sleep $SLEEPDURATION
EOF
fi
if test "$CDMARADIO" != ""
then
cat >> tmp/$PRODUCT-$VERSION/flash-base.sh << EOF
fastboot flash radio-cdma radio-cdma-$DEVICE-$CDMARADIO.img
fastboot reboot-bootloader
sleep $SLEEPDURATION
EOF
fi
chmod a+x tmp/$PRODUCT-$VERSION/flash-base.sh
(cd tmp ; tar zcvf ../$PRODUCT-$VERSION-factory.tgz $PRODUCT-$VERSION)
mv $PRODUCT-$VERSION-factory.tgz $PRODUCT-$VERSION-factory-$(sha1sum < $PRODUCT-$VERSION-factory.tgz | cut -b -8).tgz
rm -rf tmp
