#!/bin/bash
# Run automated tests
installer=`ls /android/4.1.0/output/output/MOTODEV_Application_Validator_* | grep linux.gtk`
if [ $? -lt 1 ]
then
echo [Autotest] Installer found
else
echo [Autotest] Installer not found, exiting
exit 1
fi

# Step 1: Copy and extract AppValidator installation
cp $installer /android/tests/AppValidator_install/.
cd /android/tests/AppValidator_install/
tar -xvf $installer
#rm $installer
if [ ! -e "MOTODEV_App_Validator/appvalidator.sh" ]
then
echo [Autotest] Error during untar, please check
exit 1
fi

#Step 2: Copy the correct results to compare
cp -a /android/tests/correct-output/* /android/4.1.0/env/code/android/autotester/appvalidator/AutomatedTests/correct-output/.

# Step 3: Run all tests and compare the results
perl /android/4.1.0/env/code/android/autotester/appvalidator/AutomatedTests/AppTester.pl -runMultiAndCompare

# Step 4: Copy results to tests folder
cd /android/4.1.0/env/code/android/autotester/appvalidator/AutomatedTests/zip-outputs
cp `ls -t *.zip | head -n1` /android/tests/results/.