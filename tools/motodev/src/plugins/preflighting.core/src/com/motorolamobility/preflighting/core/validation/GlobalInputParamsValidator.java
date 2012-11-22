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
package com.motorolamobility.preflighting.core.validation;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.motorolamobility.preflighting.core.checker.CheckerExtension;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingExtensionPointException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.sdk.SdkUtils;
import com.motorolamobility.preflighting.core.utils.LimitedList;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager.WarningLevelAdjustmentType;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;

/**
 * Utility class (static) which validates global parameters passed to App Validator.
 */
public final class GlobalInputParamsValidator
{
    /**
     * Constant for the variable that holds the system path.
     */
    public static final String PATH_ENVIRONMENT_VARIABLE = "path"; //$NON-NLS-1$

    private static final String WARNING_LEVEL_ADJUSTMENT_CHECKER_SEPARATOR = " "; //$NON-NLS-1$

    private static final String WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR = ", "; //$NON-NLS-1$

    /**
     * Validates all global parameters passed to the application.
     * Returns the ValidationResult object containing only ValidationResultData
     * corresponding to errors on given parameters.
     * 
     * @param params the global parameters passed to App Validator.
     * @param adjustedWarningLevelInfo The object to hold information about warning level configuration.
     * @return ValidationResult object containing the validation errors, if any.
     */
    public static ValidationResult validateGlobalParams(List<Parameter> params,
            Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo,
            List<DeviceSpecification> deviceSpecifications, ValidationManager validationManager)
    {
        ValidationResult globalResult = new ValidationResult(null, LimitedList.UNLIMITED);
        ValidationResultData resultData = null;
        boolean sdkParamFound = false;
        boolean appPathParamFound = false;

        for (Parameter param : params)
        {
            resultData = new ValidationResultData();
            String inputParam = param.getParameterType();
            String inputParamValue = param.getValue();

            if (InputParameter.SDK_PATH.getAlias().equals(inputParam))
            {
                validateSdkParam(resultData, inputParamValue);
                sdkParamFound = true;

                // say that sdk path passed as parameter is being used
                DebugVerboseOutputter
                        .printVerboseMessage(
                                PreflightingCoreNLS.GlobalInputParamsValidator_VerboseMessage_UsingParamSDKPath,
                                VerboseLevel.v1);
            }
            else if (InputParameter.APPLICATION_PATH.getAlias().equals(inputParam))
            {
                validateApplicationPathParam(resultData, inputParamValue);
                appPathParamFound = true;

            }
            else if (InputParameter.DEVICE_DESCRIPTION.getAlias().equals(inputParam))
            {
                validateDescribeDeviceParam(resultData, inputParamValue, deviceSpecifications);
            }
            else if (InputParameter.WARNING_TO_ERROR.getAlias().equals(inputParam)
                    || (InputParameter.ERROR_TO_WARNING.getAlias().equals(inputParam)))
            {
                validateWarningAdjustmentParameter(resultData, inputParamValue, inputParam,
                        adjustedWarningLevelInfo, validationManager);
            }
            else if (InputParameter.LIMIT.getAlias().equals(inputParam))
            {
                validateLimitParam(resultData, inputParamValue);
            }
            else
            {
                // unknown parameter passed
                resultData.setSeverity(SEVERITY.ERROR);
                resultData
                        .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_UnknownParameterMessage
                                + param.getParameterType());
            }

            if (!resultData.getSeverity().equals(SEVERITY.OK))
            {
                globalResult.addValidationResult(resultData);
            }
        }

        resultData = new ValidationResultData();
        if (!appPathParamFound)
        {
            resultData.setSeverity(SEVERITY.ERROR);
            resultData
                    .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_AppPathParameterMissing);
            globalResult.addValidationResult(resultData);
        }
        else if (!sdkParamFound)
        {
            DebugVerboseOutputter
                    .printVerboseMessage(
                            PreflightingCoreNLS.GlobalInputParamsValidator_VerboseMessage_CheckingSystemPathForSDK,
                            VerboseLevel.v2);

            boolean aaptFound = true;

            String[] aaptCommand = new String[]
            {
                    "aapt", "help" //$NON-NLS-1$ //$NON-NLS-2$
            };

            // test AAPT command
            try
            {
                Runtime.getRuntime().exec(aaptCommand);
            }
            catch (IOException ioException)
            {
                //aapt not found
                aaptFound = false;
                DebugVerboseOutputter
                        .printVerboseMessage(
                                PreflightingCoreNLS.GlobalInputParamsValidator_VerboseMessage_SDKPathNotFoundOnSystemPath,
                                VerboseLevel.v2);
            }

            if (!aaptFound)
            {
                resultData.setSeverity(SEVERITY.ERROR);
                resultData
                        .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_SdkPathParameterMissing);
            }
            else
            {
                params.add(new Parameter(ValidationManager.InputParameter.SDK_PATH.getAlias(),
                        "aapt")); //$NON-NLS-1$

                // say that system sdk path is being used
                DebugVerboseOutputter
                        .printVerboseMessage(
                                PreflightingCoreNLS.GlobalInputParamsValidator_VerboseMessage_UsingSystemSDKPath,
                                VerboseLevel.v1);

            }
            if ((resultData.getSeverity() != null) && !resultData.getSeverity().equals(SEVERITY.OK))
            {
                globalResult.addValidationResult(resultData);
            }
        }

        return globalResult;
    }

    private static void validateApplicationPathParam(ValidationResultData resultData,
            String inputParamValue)
    {
        File appPath = new File(inputParamValue);

        if (!appPath.exists())
        {
            resultData.setSeverity(SEVERITY.ERROR);
            resultData
                    .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_NonExistentApplicationPathMessage
                            + inputParamValue);
        }
        else
        {
            resultData.setSeverity(SEVERITY.OK);
        }
    }

    private static void validateLimitParam(ValidationResultData resultData, String paramValue)
    {
        resultData.setSeverity(SEVERITY.OK);

        if (paramValue != null)
        {
            try
            {
                int limit = Integer.parseInt(paramValue);
                if (limit >= 0)
                {
                    return;
                }
            }
            catch (NumberFormatException nfe)
            {
                resultData.setSeverity(SEVERITY.ERROR);
                resultData.setIssueDescription(PreflightingCoreNLS.bind(
                        PreflightingCoreNLS.GlobalInputParamsValidator_LimitParam, paramValue));
            }
        }

        resultData.setSeverity(SEVERITY.ERROR);
        resultData.setIssueDescription(PreflightingCoreNLS.bind(
                PreflightingCoreNLS.GlobalInputParamsValidator_LimitParam, paramValue));
    }

    /**
     * Validates if SDK parameter is valid (if SDK folder exists, is a directory and have the binaries required to run App Validator).
     * @param resultData result that is filled if there is any problem with SDK parameter. 
     * @param inputParamValue the global input value passed to run App Validator.
     */
    public static void validateSdkParam(ValidationResultData resultData, String inputParamValue)
    {
        File sdkFolder = new File(inputParamValue);

        if (!sdkFolder.exists())
        {
            resultData.setSeverity(SEVERITY.ERROR);
            resultData
                    .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_NonExistentSdkPathMessage
                            + inputParamValue);
        }
        else if (!sdkFolder.isDirectory())
        {
            resultData.setSeverity(SEVERITY.ERROR);
            resultData
                    .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_SdkPathNotFolderMessage
                            + inputParamValue);
        }
        // check if a particular tool is inside the folder (in this case, aapt), and
        // if it isn't, assume it is not a valid sdk folder
        else if (SdkUtils.getLatestAAPTToolPath(inputParamValue) == null)
        {
            resultData.setSeverity(SEVERITY.ERROR);
            resultData
                    .setIssueDescription(PreflightingCoreNLS.GlobalInputParamsValidator_SdkPathNotValidSdkMessage
                            + inputParamValue);
        }
        else
        {
            resultData.setSeverity(SEVERITY.OK);
        }
    }

    private static boolean validateDescribeDeviceParam(ValidationResultData resultData,
            String describeDeviceParamValue, List<DeviceSpecification> deviceSpecifications)
    {
        boolean deviceIdfound = false;
        if (deviceSpecifications != null)
        {
            for (DeviceSpecification spec : deviceSpecifications)
            {
                if ((spec.getId() != null) && spec.getId().equals(describeDeviceParamValue))
                {
                    deviceIdfound = true;
                    break;
                }
            }
        }
        return deviceIdfound;
    }

    private static void validateWarningAdjustmentParameter(ValidationResultData resultData,
            String inputParamValue, String inputParam,
            Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo,
            ValidationManager validationManager)
    {
        DebugVerboseOutputter
                .printVerboseMessage(
                        PreflightingCoreNLS.GlobalInputParamsValidator_WarningLevelAdjustmentParameterFoundMessage,
                        VerboseLevel.v1);

        WarningLevelAdjustmentType adjustmentType;
        if (InputParameter.WARNING_TO_ERROR.getAlias().equals(inputParam))
        {
            adjustmentType = WarningLevelAdjustmentType.INCREASE;
        }
        else
        {
            adjustmentType = WarningLevelAdjustmentType.DECREASE;
        }

        // trim parameter value for guaranteeing there is something on it other than white spaces
        if (inputParamValue != null)
        {
            inputParamValue = inputParamValue.trim();
        }

        Set<String> checkerIdsToAdjust = adjustedWarningLevelInfo.get(adjustmentType);

        // if the parameter was not previously passed, create the list of checkers for it
        if (checkerIdsToAdjust == null)
        {
            checkerIdsToAdjust = new HashSet<String>();
            adjustedWarningLevelInfo.put(adjustmentType, checkerIdsToAdjust);
        }
        // if the parameter was previously passed, this is not allowed (return immediately to avoid complicated code)
        else
        {
            resultData.setSeverity(SEVERITY.ERROR);
            resultData.setIssueDescription(PreflightingCoreNLS.bind(
                    PreflightingCoreNLS.GlobalInputParamsValidator_RepeatedParameterErrorMessage,
                    inputParam));
            return;
        }

        // no checker id passed; all checkers will have their warning levels adjusted;
        // validation result is ok
        if ((inputParamValue == null) || (inputParamValue.length() == 0))
        {
            DebugVerboseOutputter
                    .printVerboseMessage(
                            PreflightingCoreNLS.GlobalInputParamsValidator_WarningLevelAdjustmentAllCheckers,
                            VerboseLevel.v1);
            resultData.setSeverity(SEVERITY.OK);
        }
        else
        {
            // retrieve passed checker ids, validate they exist, and add
            // them to a list of checker ids to be adjusted for warning level
            String[] completeIds =
                    inputParamValue.split(WARNING_LEVEL_ADJUSTMENT_CHECKER_SEPARATOR);
            Map<String, CheckerExtension> knownCheckers = null;
            StringBuilder unknownCheckerIdsMessage = new StringBuilder();
            StringBuilder knownCheckerIdsMessage = new StringBuilder();
            boolean unknownCheckerFound = false;
            try
            {
                knownCheckers = ValidationManager.loadCheckers();
            }
            catch (PreflightingExtensionPointException e)
            {
                // do nothing; the list of checkers will fail to be validated
                // and a correct message with unknown checkers will be used
            }

            for (String completeId : completeIds)
            {
                completeId = completeId.trim();
                if (completeId.length() > 0)
                {
                    //Grab the conditionID if exists, so we can verify it later.
                    String[] checkerCondition = completeId.split("\\.");
                    String checkerId = null;
                    String conditionId = null;
                    switch (checkerCondition.length)
                    {
                        case 0:
                            //Do nothing, checkerId is already correct
                            break;
                        case 1:
                            checkerId = checkerCondition[0];
                            break;
                        case 2:
                            checkerId = checkerCondition[0];
                            conditionId = checkerCondition[1];
                            break;
                        default:
                            unknownCheckerIdsMessage.append(completeId
                                    + WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR);
                            unknownCheckerFound = true;
                    }

                    //Verify if checker is valid
                    if ((knownCheckers == null) || !knownCheckers.containsKey(checkerId))
                    {
                        unknownCheckerIdsMessage.append(completeId
                                + WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR);
                        unknownCheckerFound = true;
                    }
                    else
                    {
                        //Checker is valid now verify if the condition is valid
                        boolean conditionValid = true;
                        if (conditionId != null)
                        {
                            Map<String, ICondition> checkerConditions =
                                    knownCheckers.get(checkerId).getChecker().getConditions();
                            if (checkerConditions != null)
                            {
                                conditionValid = checkerConditions.containsKey(conditionId);
                            }
                            else
                            {
                                conditionValid = false;
                            }
                        }

                        if (conditionValid)
                        {
                            checkerIdsToAdjust.add(completeId);
                            knownCheckerIdsMessage.append(completeId
                                    + WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR);
                        }
                        else
                        {
                            unknownCheckerIdsMessage.append(completeId
                                    + WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR);
                            unknownCheckerFound = true;
                        }
                    }
                }
            }

            if (unknownCheckerFound)
            {
                // remove last comma added
                unknownCheckerIdsMessage.delete(unknownCheckerIdsMessage
                        .lastIndexOf(WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR),
                        unknownCheckerIdsMessage.length());
                String unknownCheckerIdsMessageStr = unknownCheckerIdsMessage.toString();

                resultData.setSeverity(SEVERITY.ERROR);
                resultData
                        .setIssueDescription(PreflightingCoreNLS
                                .bind(PreflightingCoreNLS.GlobalInputParamsValidator_UnknownCheckersForWarningLevelAdjustmentMessage,
                                        unknownCheckerIdsMessageStr));
            }
            else
            {
                // remove last comma added
                knownCheckerIdsMessage.delete(knownCheckerIdsMessage
                        .lastIndexOf(WARNING_LEVEL_ADJUSTMENT_CHECKER_MESSAGE_SEPARATOR),
                        knownCheckerIdsMessage.length());
                String knownCheckerIdsMessageStr = knownCheckerIdsMessage.toString();

                DebugVerboseOutputter
                        .printVerboseMessage(
                                PreflightingCoreNLS
                                        .bind(PreflightingCoreNLS.GlobalInputParamsValidator_WarningLevelAdjustmentFollowingCheckers,
                                                knownCheckerIdsMessageStr), VerboseLevel.v1);

                resultData.setSeverity(SEVERITY.OK);
            }
        }
    }
}
