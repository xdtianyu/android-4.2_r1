/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.motorolamobility.preflighting.i18n;

import org.eclipse.osgi.util.NLS;

public class PreflightingNLS extends NLS
{
    private static final String BUNDLE_NAME = "com.motorolamobility.preflighting.i18n.messages"; //$NON-NLS-1$

    public static String ApplicationParameterInterpreter_CheckerNotFound;

    public static String ApplicationParameterInterpreter_ListAvailableCheckersMessage;

    public static String ApplicationParameterInterpreter_HelpMessage;

    public static String ApplicationParameterInterpreter_InvalidVerbosityLevel;

    public static String ApplicationParameterInterpreter_InvalidWarningLevel;

    public static String ApplicationParameterInterpreter_ParameterVerbosityDescription;

    public static String ApplicationParameterInterpreter_ParameterWarningDescription;

    public static String ApplicationParameterInterpreter_V_W_PARAMETER_ERROR;

    public static String ApplicationParameterInterpreter_InvalidPort;

    public static String ApplicationParameterInterpreter_UsingDefaultPort;

    public static String CommandLineInputProcessor_IncorrectSyntax;

    public static String HelpPrinter_Device_Id;

    public static String HelpPrinter_Checker_Description;

    public static String HelpPrinter_Checker_Id;

    public static String HelpPrinter_Checker_Name;

    public static String HelpPrinter_Checker_NotAvailable;

    public static String HelpPrinter_DefaultMessage;

    public static String HelpPrinter_IOExceptionMessage;

    public static String HelpPrinter_OptionsMessage;

    public static String HelpPrinter_ProgramDescritpion;

    public static String HelpPrinter_ProgramName;

    public static String HelpPrinter_Usage;

    public static String HelperPrinter_CheckerId;

    public static String HelperPrinter_CheckerDescription;

    public static String HelperPrinter_CheckerUsesParameters;

    public static String HelperPrinter_CheckerDoesNotUseParameters;

    public static String HelperPrinter_CheckerHasConditions;

    public static String PreflightingApplication_VerboseMessage_ForwardingResultsMessage;

    public static String PreflightingApplication_VerboseMessage_StartingProcessMessage;

    public static String ApplicationParameterInterpreter_ListAvailableDevicesMessage;

    public static String ApplicationParameterInterpreter_DescribeDeviceMessage;

    public static String GlobalInputParamsValidator_NonExistentDeviceIdMessage;

    public static String Daemon_LinsteningMessage;

    public static String Daemon_StartingErrorMessage;

    public static String Daemon_Stopped;

    public static String Daemon_StartingStatusMessage;

    public static String Daemon_ValidationError;

    public static String Daemon_TestDaemonSucceedTry;

    public static String Daemon_TestDaemonFailedTry;

    public static String Daemon_TestDaemonStatusMessage;

    public static String OutputterFactory_OutputParametersValidMessage;

    public static String OutputterFactory_OutputParametersInvalidMessage;

    public static String OutputterFactory_TextOutputFormatMessage;

    public static String OutputterFactory_ValidationOutputModeMessage;

    public static String OutputterFactory_XmlOutputFormatNotImplementedMessage;

    public static String TextOutputter_File_Prefix;

    public static String TextOutputter_FixSuggestionMessage;

    public static String TextOutputter_Folder_Prefix;

    public static String TextOutputter_IOExceptionMessage;

    public static String TextOutputter_LineMessage;

    public static String TextOutputter_LinesMessage;

    public static String TextOutputter_PrintResultsErrorMessage;

    public static String TextOutputter_ResultsMessage;

    public static String TextOutputter_ApplicationMessage;

    public static String TextOutputter_ExecutionReportExecutedMsg;

    public static String TextOutputter_ExecutionReportSeparator;

    public static String TextOutputter_ExecutionReportTitle;

    public static String TextOutputter_OneOccurrenceMessage;

    public static String XMLOutputter_PrintResultsErrorMessage;

    public static String TextOutputter_MoreThanOneOccurrenceMessage;

    public static String ApplicationParameterInterpreter_OutputParam;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PreflightingNLS.class);
    }

    private PreflightingNLS()
    {
    }
}
