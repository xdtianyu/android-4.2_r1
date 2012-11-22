#!/bin/sh
#
# This script is used for the build server only. It installs the latest generated build of the Studio and calls
# other script to configure the automated tests environment and run the auto tests.
# It also copies the auto test scripts from the git repository to the directory that contains all the auto test dependencies.
#
# Source the properties:
. ./config.properties

echo "Initiating init_auto_tests.sh"
echo "Cleaning previous installation."
rm -rf $AUTO_TEST_TEMP_FILES

echo "Retrieving last generated installer."
LAST_LOG_WC=`ls -latr $BUILD_INSTALLERS  |grep drw |tail -1|wc -m`
LAST_LOG_WC1=`echo "$LAST_LOG_WC-14" |bc`
LAST_LOG_WC2=`echo "$LAST_LOG_WC-1" |bc`
LAST_LOG=`ls -latr $BUILD_INSTALLERS  |grep drw |tail -1|cut -c $LAST_LOG_WC1-$LAST_LOG_WC2`
LAST_BUILD_INSTALLERS=$BUILD_INSTALLERS/$LAST_LOG
echo "LAST_LOG_DIR=$LAST_BUILD_INSTALLERS" > lastlog.txt
fileName=$INSTALLER_NAME
fullFileName=$LAST_BUILD_INSTALLERS/$fileName
if [ ! -f $fullFileName ]; then
  echo "Filename $fullFileName does not exists."
  exit 1
fi
echo "Last installer found at: $fullFileName"
mkdir $AUTO_TEST_TEMP_FILES
cp $fullFileName $AUTO_TEST_TEMP_FILES
chmod 755 $AUTO_TEST_TEMP_FILES/$fileName

echo "Updating the properties file with the installation path."
echo $MOTODEV_INSTALLATION_PATH|sed "s/\//\\\\\//g" > $AUTO_TEST_TEMP_FILES/temp.txt
W=`tail -1 $AUTO_TEST_TEMP_FILES/temp.txt`
sed "s/MOTODEV_INSTALLATION_PATH/$W/" $SOURCE_BASE_PATH/install_properties.txt > $AUTO_TEST_TEMP_FILES/install_properties_aux.txt

echo "Installing MOTODEV Studio for Android at $MOTODEV_INSTALLATION_PATH"
java -jar $AUTO_TEST_TEMP_FILES/$fileName $AUTO_TEST_TEMP_FILES/install_properties_aux.txt

echo "Copying the scripts to the directory with all autotest dependencies."
#cp $SOURCE_BASE_PATH/init_auto_tests* $AUTO_TEST_SCRIPT_FILES/.
#cp $SOURCE_BASE_PATH/configure_start_auto_tests*  $AUTO_TEST_SCRIPT_FILES/.
#cp $SOURCE_BASE_PATH/config.properties  $AUTO_TEST_SCRIPT_FILES/.
#cp -rf $SOURCE_BASE_PATH/ant  $AUTO_TEST_SCRIPT_FILES/.

cd $AUTO_TEST_SCRIPT_FILES
./configure_start_auto_tests.sh
