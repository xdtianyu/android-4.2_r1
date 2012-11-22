#!/bin/bash

exe=""

pwd=`pwd`
wdir=`dirname $0`

cd $wdir
fullwdir=`pwd`
if [ -e motodevstudio.app ]
then
	exe="motodevstudio" 
	
	if [ ! -e motodevstudio ]
	then
		ln -s "motodevstudio.app/Contents/MacOS/motodevstudio" .
	fi
else
	exe="eclipse"
fi

cd $pwd

$fullwdir/$exe -nosplash --launcher.suppressErrors -nl en -application com.motorolamobility.preflighting.MOTODEVApplicationValidator $@ -vmargs -Xms128m -Xmx512m -Declipse.exitdata="" -Djava.awt.headless=true | tee 
err=$PIPESTATUS

exit $err
