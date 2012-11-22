#!/bin/sh

wdir=`dirname $0`

[ -e $wdir/motodevstudio ] && exe="motodevstudio" || exe="eclipse"

$wdir/$exe -nosplash --launcher.suppressErrors -nl en -application com.motorolamobility.preflighting.MOTODEVApplicationValidator $@ -vmargs -Xms128m -Xmx512m -Declipse.exitdata=""
err=$?

exit $err