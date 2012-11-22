#!/bin/sh
#
# This script configures a Studio installation in order to make it possible to run the automated tests over it.
#
# Source the properties:
. ./config.properties
echo "Initiating configure_start_auto_tests.sh"
echo "--- Starting required setups to prepare MOTODEVSTUDIO before to start the Automated Tests ---"
echo

echo "Copying SDK Folder (1/2)"
echo

### Copy and Paste to destination path. The folder 'SDK' to inside 'L' 
echo "Installation path: $MOTODEV_INSTALLATION_PATH"
cp -r $SDK_FOLDER $MOTODEV_INSTALLATION_PATH
sleep 1

echo "Copying SDK Folder (2/2)"
echo

mkdir $WORKSPACE
mkdir $WORKSPACE_TEMP
### The folder 'SDK' to inside 'WORKSPACE'
cp -r $SDK_FOLDER $WORKSPACE
sleep 1


echo "Deleting folder inside SDK Folder"
echo
rm -rf $WORKSPACE/$SDK_FOLDER/platforms/android-8


echo "Creating folders and Copying .keystore file"
echo

### The file '.keystore' to inside 'KEYSTORE_PATH'
cd $HOME_PATH
mkdir motodevstudio
cd motodevstudio
mkdir tools
cp $KEYSTORE_SOURCE $KEYSTORE_PATH

echo "Creating an AVD"
echo

### Create AVD
cd $SDK_FOLDER/tools
mkdir $AUTO_TEST_TEMP_FILES
AUX_UNAME=`uname -a|grep Linux`
if [ "$AUX_UNAME" != "" ]
then
ANDROID_EXE=android
else
ANDROID_EXE=android.bat
fi
echo "no" | ./$ANDROID_EXE create avd --force -n AVD -t 1 -p $AUTO_TEST_TEMP_FILES_BUILD/avds_autotest


echo "Installing SWTBot Plug-ins"
echo

### Install SWTBot Plug-ins

id1="org.eclipse.swtbot.feature.group"
id2="org.eclipse.swtbot.eclipse.test.junit4.feature.group"
id3="org.eclipse.swtbot.eclipse.feature.group"
id4="org.eclipse.swtbot.eclipse.gef.feature.group"

cd $MOTODEV_INSTALLATION_PATH
if [ "$AUX_UNAME" != "" ]
then
MOTODEV_EXE="motodevstudio"
else
MOTODEV_EXE="motodevstudio.exe"
fi


echo "Installing $id1"
./$MOTODEV_EXE -noSplash -application org.eclipse.equinox.p2.director -metadataRepository jar:file:$SWTBOT_FILES\!/ -artifactRepository jar:file:$SWTBOT_FILES\!/ -installIU $id1 -profileProperties org.eclipse.update.install.features=true -vmArgs -Xms512M -Xmx512M

echo "Installing $id2"
./$MOTODEV_EXE -noSplash -application org.eclipse.equinox.p2.director -metadataRepository jar:file:$SWTBOT_FILES\!/ -artifactRepository jar:file:$SWTBOT_FILES\!/ -installIU $id2 -profileProperties org.eclipse.update.install.features=true -vmArgs -Xms512M -Xmx512M

echo "Installing $id3"
./$MOTODEV_EXE -noSplash -application org.eclipse.equinox.p2.director -metadataRepository jar:file:$SWTBOT_FILES\!/ -artifactRepository jar:file:$SWTBOT_FILES\!/ -installIU $id3 -profileProperties org.eclipse.update.install.features=true -vmArgs -Xms512M -Xmx512M

echo "Installing $id4"
./$MOTODEV_EXE -noSplash -application org.eclipse.equinox.p2.director -metadataRepository jar:file:$SWTBOT_FILES\!/ -artifactRepository jar:file:$SWTBOT_FILES\!/ -installIU $id4 -profileProperties org.eclipse.update.install.features=true -vmArgs -Xms512M -Xmx512M



echo "Replacing 'library.xml' file"
echo

### Changing the 'library.xml' file (Replacing the original file)
echo $WORKSPACE_BUILD|sed "s/\//\\\\\//g" > $AUTO_TEST_TEMP_FILES/temp.txt
W=`tail -1 $AUTO_TEST_TEMP_FILES/temp.txt`
sed "s/WORKSPACE/$W/g" $LIBRARY_FILE > $AUTO_TEST_TEMP_FILES/library.xml

echo $WORKSPACE_TEMP_BUILD|sed "s/\//\\\\\//g" > $AUTO_TEST_TEMP_FILES/temp.txt
W1=`tail -1 $AUTO_TEST_TEMP_FILES/temp.txt`
sed "s/WORKTEMP/$W1/g" $AUTO_TEST_TEMP_FILES/library.xml > $LIBRARY_PATH/library.xml



if [ "$AUX_UNAME" != "" ]
then
AUTO_TEST_OS1=linux
AUTO_TEST_OS2=gtk
AUTO_TEST_ARCH=x86_64  
else
AUTO_TEST_OS1=win32
AUTO_TEST_OS2=win32
AUTO_TEST_ARCH=x86_64
fi


sed "s/AUTO_TEST_OS1/$AUTO_TEST_OS1/g" $LIBRARY_PATH/library.xml > $AUTO_TEST_TEMP_FILES/library.xml2
sed "s/AUTO_TEST_OS2/$AUTO_TEST_OS2/g" $AUTO_TEST_TEMP_FILES/library.xml2 > $AUTO_TEST_TEMP_FILES/library.xml3
sed "s/AUTO_TEST_ARCH/$AUTO_TEST_ARCH/g" $AUTO_TEST_TEMP_FILES/library.xml3 > $LIBRARY_PATH/library.xml


echo "Copying the .jar file"
echo

find $PLUGIN_FILES -iname '*.jar' -exec cp \{\} $MOTODEV_INSTALLATION_PATH/plugins/ \;
find $PLUGIN_FILES -iname '*.zip' -exec cp \{\} $WORKSPACE/ \;
cd $WORKSPACE
unzip -u *.zip


echo
echo "--- Setup DONE! ---"

echo
echo "--- Now, starting the ANT execution ---"
echo


### ANT
cd  $AUTO_TEST_SCRIPT_FILES/ant
export PATH=$ANT_LOCATION:$PATH
if [ "$SHOW_DISPLAY" != "" ]
then
	export DISPLAY=$SHOW_DISPLAY
fi
ant -logfile logant.txt

echo
echo "Finishing configure_start_auto_tests.sh"
echo
