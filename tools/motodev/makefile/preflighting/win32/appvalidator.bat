@echo off

set wdir=%0\..


if EXIST %wdir%\motodevstudioc.exe (
	SET exe=motodevstudioc
) ELSE (
	SET exe=eclipsec
)

%wdir%\%exe% -nosplash --launcher.suppressErrors -nl en -application com.motorolamobility.preflighting.MOTODEVApplicationValidator %* -vmargs -Xms128m -Xmx512m -Declipse.exitdata=""

set err=%ERRORLEVEL%


echo.


EXIT /B %err%