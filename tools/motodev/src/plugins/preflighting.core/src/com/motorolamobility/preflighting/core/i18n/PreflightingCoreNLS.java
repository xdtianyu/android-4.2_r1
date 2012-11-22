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
package com.motorolamobility.preflighting.core.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * I18n 
 */
public class PreflightingCoreNLS extends NLS
{
    private static final String BUNDLE_NAME =
            "com.motorolamobility.preflighting.core.i18n.messages"; //$NON-NLS-1$

    public static String Checker_MandatoryParam_EmptyValueWarn;

    public static String CheckerExtensionReader_CheckerExtensionPointNotFound;

    public static String CheckerExtensionReader_UnexpectedErrorCheckerExtensionPoint;

    public static String GlobalInputParamsValidator_NonExistentApplicationPathMessage;

    public static String GlobalInputParamsValidator_NonExistentSdkPathMessage;

    public static String GlobalInputParamsValidator_SdkPathNotFolderMessage;

    public static String GlobalInputParamsValidator_SdkPathNotValidSdkMessage;

    public static String GlobalInputParamsValidator_UnknownCheckersForWarningLevelAdjustmentMessage;

    public static String GlobalInputParamsValidator_UnknownParameterMessage;

    public static String GlobalInputParamsValidator_AppPathParameterMissing;

    public static String GlobalInputParamsValidator_RepeatedParameterErrorMessage;

    public static String GlobalInputParamsValidator_SdkPathParameterMissing;

    public static String GlobalInputParamsValidator_VerboseMessage_CheckingSystemPathForSDK;

    public static String GlobalInputParamsValidator_VerboseMessage_UsingParamSDKPath;

    public static String GlobalInputParamsValidator_VerboseMessage_UsingSystemSDKPath;

    public static String GlobalInputParamsValidator_VerboseMessage_SDKPathNotFoundOnSystemPath;

    public static String GlobalInputParamsValidator_WarningLevelAdjustmentAllCheckers;

    public static String GlobalInputParamsValidator_WarningLevelAdjustmentFollowingCheckers;

    public static String GlobalInputParamsValidator_WarningLevelAdjustmentParameterFoundMessage;

    public static String GlobalInputParamsValidator_LimitParam;

    public static String ProjectUtils_Error_Parsing_Manifest_DEBUG;

    public static String ProjectUtils_Error_Parsing_Manifest_INFO;

    public static String ProjectUtils_InvalidPathErrorMessage;

    public static String ValidationManager_ErrorRetrievingApplicationData;

    public static String ValidationManager_CheckerDescriptionMessage;

    public static String ValidationManager_DeviceDescriptionMessage;

    public static String ValidationManager_Errors_NoValidationApplyOrError;

    public static String ValidationManager_ErrorToWarningDescriptionMessage;

    public static String ValidationManager_OutputDescriptionMessage;

    public static String ValidationManager_OutputSintaxMessage;

    public static String ValidationManager_LimitDescription;

    public static String ValidationManager_InputParametersProblemMessage;

    public static String ValidationManager_SdkPathDescriptionMessage;

    public static String ValidationManager_UnableToExecuteCheckerMessage;

    public static String ValidationManager_UnableToExecuteCheckerMessage_Detailed;

    public static String ValidationManager_NoConditionsReason;

    public static String ValidationManager_UnableToExecuteCondition;

    public static String ValidationManager_UnableToExecuteCondition_Detailed;

    public static String ValidationManager_UnexpectedExceptiononChecker;

    public static String ValidationManager_UnexpectedExceptiononChecker_V2;

    public static String ValidationManager_UnknownCheckerMessage;

    public static String ValidationManager_UnknownCheckerOrConditionMessage;

    public static String ValidationManager_IncorrectSyntax;

    public static String ValidationManager_InvalidParamType_Bool;

    public static String ValidationManager_InvalidParamType_Int;

    public static String ValidationManager_UnknownParameterMessage;

    public static String ValidationManager_UnknownDeviceMessage;

    public static String ValidationManager_ValidationLimitReached;

    public static String ValidationManager_MandatoryParameterMissing;

    public static String ValidationManager_CheckerWasDisabled;

    public static String ValidationManager_ValidationManager_VerboseMessage_Skipping_Device_Verifications;

    public static String ValidationManager_VerboseMessage_AllCheckersRun;

    public static String ValidationManager_VerboseMessage_CheckerFinished;

    public static String ValidationManager_VerboseMessage_ProblemsCheckerParameters;

    public static String ValidationManager_VerboseMessage_ProblemsGlobalParameters;

    public static String ValidationManager_VerboseMessage_RunningChecker;

    public static String ValidationManager_VerboseMessage_UnknownParametersFound;

    public static String ValidationManager_VerboseMessage_ValidatingGlobalParameters;

    public static String ValidationManager_VerboseMessage_VerifyingCheckerRun;

    public static String ValidationManager_WarningToErrorDescriptionMessage;

    public static String ValidationResultData_ErrorSeverityMessage;

    public static String ValidationResultData_FatalSeverityMessage;

    public static String ValidationResultData_OkSeverityMessage;

    public static String ValidationResultData_WarningSeverityMessage;

    public static String VerboseOutputter_DebugVerboseLeveString;

    public static String VerboseOutputter_InfoVerboseLevelString;

    public static String ApkUtils_AaptExecutionProblemMessage;

    public static String ApkUtils_AaptResultReadProblemMessage;

    public static String ApkUtils_DomInstanceProblemMessage;

    public static String ApkUtils_ImpossibleExtractAndroidPackageMessage;

    public static String ApkUtils_ZipExtraction;

    public static String ApkUtils_ZipExtractionFile;

    public static String Device_Device;

    public static String Device_SupportedFeatures;

    public static String WarningLevelFilter_ErrorsCountMessage;

    public static String WarningLevelFilter_ErrorsMessage;

    public static String WarningLevelFilter_FatalErrorsCountMessage;

    public static String WarningLevelFilter_FatalErrorsMessage;

    public static String WarningLevelFilter_NoFatalErrorsMessage;

    public static String WarningLevelFilter_NoFatalErrorsNorWarningsMessage;

    public static String WarningLevelFilter_NoFatalNorErrorsMessage;

    public static String WarningLevelFilter_NoProblemsMessage;

    public static String WarningLevelFilter_TotalMessage;

    public static String WarningLevelFilter_VerboseMessage_FilterningResult;

    public static String WarningLevelFilter_VerboseMessage_ResultFiltered;

    public static String WarningLevelFilter_WarningsCountMessage;

    public static String ProjectUtils_ErrorReadingCertificate;

    public static String ProjectUtils_ErrorExecutingApkTool;

    public static String ProjectUtils_ErrorReadingJavaModel;

    public static String ProjectUtils_ErrorReadingSourceFile;

    public static String ProjectUtils_ErrorReadingDefaultPropertiesFile;

    public static String ProjectUtils_ErrorReadingClasspathFile;

    public static String Invalid_ManifestFile;

    public static String JavaModelNotFound_Err;

    public static String EmptyInvokedMethods_Err;

    public static String NoSourceFilesFound_Err;

    public static String ValidationManager_DisableCheckerDescriptionMessage;

    public static String ApkToolUtils_MalformedAPK;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PreflightingCoreNLS.class);
    }

    private PreflightingCoreNLS()
    {
    }
}
