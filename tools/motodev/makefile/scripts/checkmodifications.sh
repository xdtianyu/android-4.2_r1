#!/bin/bash
#$1 - root path to code (e.g. /android/2.1.0/env/code)
#$2 - git tag (label) to use as initial baseline to find changed files (e.g.: STUDIO-ANDROID_N_02.01.00I_B001)
#$3 - studio version (e.g.: 2.1.0)
#$4 - platform version (e.g.: 1.0.1)
#$5 - app validator version (e.g.: 0.7.0)
#$6 - app validator ui version (e.g.: 0.7.0)
#$7 - folder to send report (output file)
cd $1
echo "The following plugins have incorrect version: " >> $7/plugins_to_update.txt
for f in `ls $1/android/src/plugins`; 
do 
	#echo $f;
	#retrieve the plugins modified and count the lines (if plugin not changed wc returns zero)	
	lines=`git diff --name-only $2 -- android/src/plugins/$f | wc -w | cut -d" " -f1`
	#echo $lines
	if [ "$lines" -gt "0" ]
	then
		#As plugin was modified, check if MANIFEST.MF has the version of the release, if not report
		#echo $1/android/src/plugins/$f
		#cat $1/android/src/plugins/$f/META-INF/MANIFEST.MF | grep "Bundle-Version:"
		if [[ $1/android/src/plugins/$f == *preflighting* ]] ;
		then
			#echo "preflighting plugin"
			if [[ $1/android/src/plugins/$f == *preflighting.ui* ]] ;
			then
				version=`cat $1/android/src/plugins/$f/META-INF/MANIFEST.MF | grep "Bundle-Version:" | grep $6 | cut -d":" -f2 | tr -d ' '`
				if [ "$version" != "$6.qualifier" ]
				then 
					echo android/src/plugins/$f >> $7/plugins_to_update.txt
				fi
			else
				version=`cat $1/android/src/plugins/$f/META-INF/MANIFEST.MF | grep "Bundle-Version:" | grep $5 | cut -d":" -f2 | tr -d ' '`
				if [ "$version" != "$5.qualifier" ]
				then 
					echo android/src/plugins/$f >> $7/plugins_to_update.txt
				fi
			fi
			
		else
			#echo "studio plugin"
			version=`cat $1/android/src/plugins/$f/META-INF/MANIFEST.MF | grep "Bundle-Version:" | grep $3 | cut -d":" -f2 | tr -d ' '`
			if [ "$version" != "$3.qualifier" ]
			then 
				echo android/src/plugins/$f >> $7/plugins_to_update.txt
			fi
		fi				
	fi
done
for f in `ls $1/platform/code/plugins`; 
do 
	#echo $f;
	#retrieve the plugins modified and count the lines (if plugin not changed wc returns zero)	
	lines=`git diff --name-only $2 -- platform/code/plugins/$f | wc -w | cut -d" " -f1`
	#echo $lines
	if [ "$lines" -gt "0" ]
	then
		#As plugin was modified, check if MANIFEST.MF has the version of the release, if not report
		#echo $1/platform/code/plugins/$f
		#cat $1/platform/code/plugins/$f/META-INF/MANIFEST.MF | grep "Bundle-Version:"
		if [[ $1/platform/code/plugins/$f == *platform* ]] ;
		then
			#echo "platform plugin"
			version=`cat $1/platform/code/plugins/$f/META-INF/MANIFEST.MF | grep "Bundle-Version:" | grep $4 | cut -d":" -f2 | tr -d ' '`
			if [ "$version" != "$4" ]
			then 
				echo platform/code/plugins/$f >> $7/plugins_to_update.txt
			fi
		fi
	fi
done
