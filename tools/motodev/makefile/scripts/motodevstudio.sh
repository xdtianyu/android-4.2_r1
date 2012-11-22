#!/bin/bash

######################
# Language Parameter:
######################
#
# Value: languageID_countryID (The _countryID may be ommited)
# language_ID = An identifier from ISO 639-1
# country_ID = An identifier from ISO 3166-1
#
# Uncomment the "LANGUAGE" parameter below to start the MOTODEV Studio for Android in the language you want
#

#LANGUAGE="-nl en"
#LANGUAGE="-nl pt_BR"

START_COMMAND=${0%.sh}
unset UBUNTU_MENUPROXY

# added duo to a eclipse bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=345979

export LIBOVERLAY_SCROLLBAR=0 

if [ ${START_COMMAND:0:1} == "/" ]
then
$START_COMMAND $@ $LANGUAGE
else
./$START_COMMAND $@ $LANGUAGE
fi
