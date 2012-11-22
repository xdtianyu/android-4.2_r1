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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.checker.CheckerDescription;
import com.motorolamobility.preflighting.core.checker.CheckerExtension;
import com.motorolamobility.preflighting.core.checker.IChecker;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter;
import com.motorolamobility.preflighting.core.devicelayoutspecification.Device;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.DevicesSpecsContainer;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.exception.PreflightingExtensionPointException;
import com.motorolamobility.preflighting.core.exception.PreflightingParameterException;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.exception.ValidationLimitException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.internal.checker.CheckerExtensionReader;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.LayoutDevicesType;
import com.motorolamobility.preflighting.core.internal.utils.AaptUtils;
import com.motorolamobility.preflighting.core.internal.utils.ApkUtils;
import com.motorolamobility.preflighting.core.internal.utils.ProjectUtils;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.LimitedList;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter;

/**
 * This class is responsible for accessing App Validator framework components (checkers,
 * device specifications, etc).
 */
public class ValidationManager
{
    /**
     * Keeps properties that control App Validator (such as the base URL for the checker/condition help).
     */
    private ValidationManagerConfiguration valManagerConfig = null;

    /**
     * Map of checkers, ordered by checker id (command line value used to call the checker).
     */
    private final static TreeMap<String, CheckerExtension> checkers =
            new TreeMap<String, CheckerExtension>();

    /**
     * Maps of global parameter descriptions (the key is {@link ParameterDescription#getName()}). 
     */
    private final Map<String, ParameterDescription> globalParametersDescriptions =
            new LinkedHashMap<String, ParameterDescription>();

    /**
     * List of {@link Parameter} (the global parameters for App Validator). 
     */
    private List<Parameter> globalParams;

    /**
     * Parameter to specify a checker.
     */
    public static final String CHECKER_PARAMETER = "c"; //$NON-NLS-1$

    /**
     * Parameter to disable a checker.
     */
    public static final String DISABLE_CHECKER_PARAMETER = "dc"; //$NON-NLS-1$

    /**
     * Parameter to specify a device.
     */
    public static final String DEVICE_PARAMETER = "d"; //$NON-NLS-1$

    private static final String DEVICE_PARAMETER_NONE_VALUE = "none"; //$NON-NLS-1$

    public static final String APP_VALIDATOR_RESULT_ID = "appValidatorResult"; //$NON-NLS-1$

    // Flag used to decided wether or not we will delete the temp folder used to
    // extract APK packages.
    // Only set to false when the user passes a "hidden" "-keepTempFiles"
    // parameter
    private boolean deleteApkTempFolder = true;

    private final ArrayList<String> tempResourcesToDelete;

    //Flag used to determine whether device verifications will be made or not
    // if 'true' then user specified the "-d none" parameter and no device verification will be executed.
    private boolean noneDeviceSpecified = false;

    /**
     * Container {@link DevicesSpecsContainer} responsible to keep the list of {@link DeviceSpecification}.
     */
    private final DevicesSpecsContainer devicesSpecsContainer;

    /**
     * Input parameters for the App Validator.
     */
    public static enum InputParameter
    {

        SDK_PATH("sdk"), //$NON-NLS-1$

        // app path: is omitted in command line
        APPLICATION_PATH("input"), //$NON-NLS-1$

        WARNING_TO_ERROR("wx"), //$NON-NLS-1$

        ERROR_TO_WARNING("xw"), //$NON-NLS-1$

        DEVICE_DESCRIPTION("describe-device"), //$NON-NLS-1$

        OUTPUT("output"), //$NON-NLS-1$

        LIMIT("limit"); //$NON-NLS-1$

        private String alias;

        private InputParameter(String id)
        {
            this.alias = id;
        }

        /**
         * Gets the alias.
         * 
         * @return Return the alias.
         */
        public String getAlias()
        {
            return alias;
        }

        /**
         * Verifies whether a certain value is recognized as a valid
         * alias for a {@link InputParameter}.
         * 
         * @param alias Alias to which the comparation will be made.
         * 
         * @return True if the value of alias is recognized as a valid
         *         {@link InputParameter}. Return false if alias is null.
         */
        public static boolean contains(String alias)
        {

            boolean contains = false;

            if (alias != null)
            {
                for (InputParameter input : InputParameter.values())
                {
                    if (input.getAlias().equals(alias))
                    {
                        contains = true;
                        break;
                    }
                }
            }

            return contains;
        }
    }

    /**
     * Constructor which instantiates the {@link ValidationManager} with
     * default parameters.
     */
    public ValidationManager()
    {
        tempResourcesToDelete = new ArrayList<String>();

        devicesSpecsContainer = DevicesSpecsContainer.getInstance();
        if (devicesSpecsContainer.getDeviceSpecifications().isEmpty())
        {
            loadDeviceSpecifications();
        }
    }

    /**
     * Load Device specifications.
     */
    private void loadDeviceSpecifications()
    {
        LayoutDevicesType layoutDevicesType = XmlUtils.parseDevicesXmlFiles();

        for (Device deviceInfo : layoutDevicesType.getDevices())
        {
            DeviceSpecification devSpec =
                    new DeviceSpecification(PlatformRules.API_LEVEL_3, deviceInfo);
            devicesSpecsContainer.addDeviceSpecification(devSpec);
        }
    }

    /**
     * Load existing checkers, mapped by checker id (value used to call the
     * checker on command line). The map is sorted alphabetically by checker id.
     * 
     * @return Checkers map.
     */
    public static Map<String, CheckerExtension> loadCheckers()
            throws PreflightingExtensionPointException
    {
        if (checkers.isEmpty())
        {
            CheckerExtensionReader.loadCheckers(checkers);
        }
        return checkers;
    }

    /**
     * Filter the device specifications based on the parameters.
     * @param deviceParams Parameters associated with devices.
     * @return List of {@link DeviceSpecification}. 
     */
    public List<DeviceSpecification> filterDeviceSpecifications(List<Parameter> deviceParams)
    {
        List<DeviceSpecification> filteredListDeviceSpecifications =
                new ArrayList<DeviceSpecification>();

        List<DeviceSpecification> deviceSpecifications =
                devicesSpecsContainer.getDeviceSpecifications();
        if (deviceParams.size() > 0)
        {
            //some devices specified => only fill specifications for the devices that match the id
            for (Parameter param : deviceParams)
            {
                String deviceId = param.getValue();
                //TODO Check how to get apiLevel from Device
                if (deviceSpecifications != null)
                {
                    for (DeviceSpecification deviceSpecification : deviceSpecifications)
                    {
                        if (deviceSpecification.getId().equalsIgnoreCase(deviceId))
                        {
                            //found id
                            DeviceSpecification devSpec =
                                    new DeviceSpecification(PlatformRules.API_LEVEL_1,
                                            deviceSpecification.getDeviceInfo());
                            filteredListDeviceSpecifications.add(devSpec);
                        }
                    }
                }
            }
        }
        else
        {
            //no device specified => use all devices
            filteredListDeviceSpecifications.addAll(deviceSpecifications);
        }

        return filteredListDeviceSpecifications;
    }

    /**
     * Returns the checker based on extension points declared.
     * @param checkerId
     * @return
     */
    public static CheckerExtension getCheckerExtension(String checkerId)
    {
        CheckerExtension result = null;
        try
        {
            loadCheckers();
            result = checkers.get(checkerId);
        }
        catch (PreflightingExtensionPointException e)
        {
            PreflightingLogger.debug("Unable to read checker extension " + checkerId);
        }
        return result;
    }

    /**
     * Validate if all devices passed as parameters exists.
     * 
     * @param deviceParams devices passed as parameters
     * @return
     */
    private List<ValidationResult> validateDeviceParams(List<Parameter> deviceParams)
    {
        List<ValidationResult> resultsList = new ArrayList<ValidationResult>();

        for (Parameter param : deviceParams)
        {
            boolean deviceExists = false;
            String deviceId = param.getValue();
            if (deviceId == null)
            {
                ValidationResult globalResult = new ValidationResult(null, LimitedList.UNLIMITED);
                ValidationResultData resultData = new ValidationResultData();
                resultData.setSeverity(SEVERITY.ERROR);
                resultData.setIssueDescription(PreflightingCoreNLS.bind(
                        PreflightingCoreNLS.ValidationManager_IncorrectSyntax,
                        param.getParameterType()));
                globalResult.addValidationResult(resultData);
                resultsList.add(globalResult);
            }
            else
            {
                for (DeviceSpecification currentDevice : devicesSpecsContainer
                        .getDeviceSpecifications())
                {
                    if (currentDevice.getId().equals(deviceId))
                    {
                        deviceExists = true;
                        break;
                    }
                }
                if (!deviceExists)
                {
                    if (!param.getValue().equals(DEVICE_PARAMETER_NONE_VALUE))
                    {
                        ValidationResult globalResult =
                                new ValidationResult(null, LimitedList.UNLIMITED);
                        ValidationResultData resultData = new ValidationResultData();
                        resultData.setSeverity(SEVERITY.ERROR);
                        resultData.setIssueDescription(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.ValidationManager_UnknownDeviceMessage,
                                " '" + deviceId + "'")); //$NON-NLS-1$ //$NON-NLS-2$
                        globalResult.addValidationResult(resultData);
                        resultsList.add(globalResult);
                    }
                    else
                    {
                        noneDeviceSpecified = true;
                    }
                }
            }
        }

        return resultsList;
    }

    /**
     * Validate two things: 1- all checkers passed are known on the framework,
     * and 2- all parameters passed for the checkers are valid
     * 
     * @param params
     *            The checkers parameters
     * @param knownCheckers
     *            The map of known checkers
     * 
     * @return A list of ValidationResult. Each ValidationResult of its checker.
     *         Return an empty list if no problems are found.
     */
    private List<ValidationResult> validateCheckerParams(List<Parameter> params,
            Map<String, CheckerExtension> knownCheckers)
    {
        List<ValidationResult> resultsList = new ArrayList<ValidationResult>();

        //clear the parameter list inside each Checker
        initializeParams(knownCheckers);

        if ((knownCheckers != null) && !knownCheckers.isEmpty())
        {
            if (!params.isEmpty())
            {
                for (Parameter param : params)
                {
                    String checkerId = param.getValue();
                    ValidationResultData resultData = new ValidationResultData();
                    if (checkerId == null)
                    {
                        resultData.setSeverity(SEVERITY.ERROR);
                        resultData.setIssueDescription(NLS.bind(
                                PreflightingCoreNLS.ValidationManager_IncorrectSyntax,
                                param.getParameterType()));
                    }
                    else if (!knownCheckers.keySet().contains(checkerId))
                    {
                        resultData.setSeverity(SEVERITY.ERROR);
                        resultData.setIssueDescription(NLS.bind(
                                PreflightingCoreNLS.ValidationManager_UnknownCheckerMessage,
                                checkerId));
                    }
                    else
                    {
                        List<Parameter> subParameters = null;
                        IChecker checker = knownCheckers.get(checkerId).getChecker();

                        if (param instanceof ComplexParameter)
                        {
                            ComplexParameter complexParam = (ComplexParameter) param;
                            subParameters = complexParam.getParameters();
                        }
                        else
                        {
                            subParameters = new ArrayList<Parameter>();
                        }
                        IStatus validationStatus = setParameters(checker, subParameters);
                        if (validationStatus.isOK())
                        {
                            resultData.setSeverity(SEVERITY.OK);
                            validationStatus = checker.validateInputParams(subParameters);
                            if (validationStatus.isOK())
                            {
                                resultData.setSeverity(SEVERITY.OK);
                            }
                            else
                            {
                                if (validationStatus.getSeverity() == Status.INFO)
                                {
                                    resultData.setSeverity(SEVERITY.WARNING);
                                }
                                else
                                {
                                    resultData.setSeverity(SEVERITY.ERROR);
                                }

                                resultData.setIssueDescription(validationStatus.getMessage());
                            }
                        }
                        else
                        {
                            resultData.setSeverity(SEVERITY.ERROR);
                            resultData.setIssueDescription(validationStatus.getMessage());
                        }

                    }

                    ValidationResult result =
                            new ValidationResult(checkerId, LimitedList.UNLIMITED);
                    if (!resultData.getSeverity().equals(SEVERITY.OK))
                    {
                        result.addValidationResult(resultData);
                        resultsList.add(result);
                    }
                }
            }
            else
            //Validate the empty list, because some checkers can have mandatory parameters
            {
                validateMandatoryParams(params, knownCheckers, resultsList);
            }
        }

        return resultsList;
    }

    private void validateMandatoryParams(List<Parameter> params,
            Map<String, CheckerExtension> knownCheckers, List<ValidationResult> resultsList)
    {
        for (CheckerExtension checkerExtension : knownCheckers.values())
        {
            IChecker checker = checkerExtension.getChecker();

            // validate only for those that are enabled
            if (checker.isEnabled())
            {
                IStatus validationStatus = checker.validateInputParams(params);
                ValidationResultData resultData = new ValidationResultData();
                if (validationStatus.isOK())
                {
                    resultData.setSeverity(SEVERITY.OK);
                }
                else
                {
                    if (validationStatus.getSeverity() == Status.INFO)
                    {
                        resultData.setSeverity(SEVERITY.WARNING);
                    }
                    else
                    {
                        resultData.setSeverity(SEVERITY.ERROR);
                    }
                    resultData.setIssueDescription(validationStatus.getMessage());
                }
                ValidationResult result =
                        new ValidationResult(checker.getId(), LimitedList.UNLIMITED);
                if (!resultData.getSeverity().equals(SEVERITY.OK))
                {
                    result.addValidationResult(resultData);
                    resultsList.add(result);
                }
            }
        }
    }

    /**
     * Initialize the list of parameters of each checker.
     * @param knownCheckers
     */
    private void initializeParams(Map<String, CheckerExtension> knownCheckers)
    {
        for (CheckerExtension checkerExtension : knownCheckers.values())
        {
            IChecker checker = checkerExtension.getChecker();

            Map<String, ICheckerParameter> parameters = checker.getParameters();
            if (parameters != null)
            {
                for (ICheckerParameter checkerParam : parameters.values())
                {
                    ParameterType type = checkerParam.getType();
                    switch (type)
                    {
                        case BOOLEAN:
                            checkerParam.setBooleanValue(null);
                            break;
                        case STRING:
                            checkerParam.setValue(null);
                            break;
                        case INTEGER:
                            checkerParam.setIntValue(null);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * All parameters inputed by a user (stored in the {@link List} of {@link Parameter} objects)
     * are set into the {@link IChecker} map of {@link ICheckerParameter} objects. 
     * 
     * @param checker {@link IChecker} where the parameter values will be set.
     * @param params The {@link List} of {@link Parameter} which values will be set
     * into a {@link IChecker}.
     */
    private IStatus setParameters(IChecker checker, List<Parameter> params)
    {
        IStatus status = Status.OK_STATUS;

        // set values entered by the user into the Checker Parameters
        if ((params != null) && (params.size() > 0))
        {
            Map<String, ICheckerParameter> checkerParameters = checker.getParameters();

            ICheckerParameter checkerParameter = null;
            for (Parameter enteredParameter : params)
            {
                if ((checkerParameters != null) && (checkerParameters.size() > 0))
                {
                    checkerParameter = checkerParameters.get(enteredParameter.getParameterType());
                    if (checkerParameter != null)
                    {
                        String value = enteredParameter.getValue();
                        ParameterType type = checkerParameter.getType();
                        if (type == ParameterType.INTEGER)
                        {
                            try
                            {
                                int intValue = Integer.parseInt(value);
                                checkerParameter.setIntValue(intValue);
                            }
                            catch (NumberFormatException e)
                            {
                                return new Status(
                                        IStatus.ERROR,
                                        PreflightingCorePlugin.PLUGIN_ID,
                                        NLS.bind(
                                                PreflightingCoreNLS.ValidationManager_InvalidParamType_Int,
                                                value, checkerParameter.getId()));
                            }

                        }
                        else if (type == ParameterType.BOOLEAN)
                        {
                            if (isBoolean(value))
                            {
                                boolean booleanValue = Boolean.parseBoolean(value);
                                checkerParameter.setBooleanValue(booleanValue);
                            }
                            else
                            {
                                return new Status(
                                        IStatus.ERROR,
                                        PreflightingCorePlugin.PLUGIN_ID,
                                        NLS.bind(
                                                PreflightingCoreNLS.ValidationManager_InvalidParamType_Bool,
                                                value, checkerParameter.getId()));
                            }
                        }

                        checkerParameter.setValue(value);
                    }
                }
            }
        }
        return status;
    }

    private boolean isBoolean(String value)
    {
        return Boolean.TRUE.toString().equalsIgnoreCase(value)
                || Boolean.FALSE.toString().equalsIgnoreCase(value);
    }

    private ValidationResult validateGlobalParams(List<Parameter> params,
            Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo,
            List<DeviceSpecification> deviceSpecifications)
    {
        return GlobalInputParamsValidator.validateGlobalParams(params, adjustedWarningLevelInfo,
                deviceSpecifications, this);

    }

    /**
     * Delegates all parameters validation and sets global parameters during the process
     *  
     * @param knownCheckers checkers available
     * @param commandLineParams parsed command line parameters
     * @param checkerParams -c parameter
     * @param disabledCheckers -dc parameter (represents checkers that will not run)
     * @param deviceParams -d parameter
     * @param adjustedWarningLevelInfo warning level adjustments -wx -xw
     * @return List of found issues. Should be empty it everything goes fine. 
     */
    private List<ValidationResult> startParamsValidation(
            Map<String, CheckerExtension> knownCheckers, List<Parameter> commandLineParams,
            List<Parameter> checkerParams, List<Parameter> disabledCheckers,
            List<Parameter> deviceParams,
            Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo)
    {
        List<Parameter> globalParams = new ArrayList<Parameter>();
        List<Parameter> errorParams = new ArrayList<Parameter>();

        for (Parameter param : commandLineParams)
        {
            if (InputParameter.contains(param.getParameterType()))
            {
                globalParams.add(param);
            }
            else if (CHECKER_PARAMETER.equals(param.getParameterType()))
            {
                checkerParams.add(param);
            }
            else if (DISABLE_CHECKER_PARAMETER.equals(param.getParameterType()))
            {
                disabledCheckers.add(param);
            }
            else if (DEVICE_PARAMETER.equals(param.getParameterType()))
            {
                deviceParams.add(param);
            }
            else
            {
                errorParams.add(param);
            }
        }

        setGlobalParameters(globalParams);

        for (CheckerExtension checkerExtension : knownCheckers.values())
        {
            if ((checkerExtension != null) && (checkerExtension.getChecker() instanceof Checker))
            {
                Checker checker = (Checker) checkerExtension.getChecker();
                checker.setGlobalParams(globalParams);
            }

        }

        List<ValidationResult> mergedResultList = new ArrayList<ValidationResult>();

        DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v0); //$NON-NLS-1$

        DebugVerboseOutputter.printVerboseMessage(
                PreflightingCoreNLS.ValidationManager_VerboseMessage_ValidatingGlobalParameters,
                VerboseLevel.v2);
        //        Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo =
        //                new LinkedHashMap<ValidationManager.WarningLevelAdjustmentType, Set<String>>(2);
        ValidationResult globalValidationResult =
                validateGlobalParams(globalParams, adjustedWarningLevelInfo,
                        devicesSpecsContainer.getDeviceSpecifications());
        List<ValidationResult> deviceValidationResultList = validateDeviceParams(deviceParams);

        if (errorParams.size() > 0)
        {
            DebugVerboseOutputter.printVerboseMessage(
                    PreflightingCoreNLS.ValidationManager_VerboseMessage_UnknownParametersFound,
                    VerboseLevel.v2);
            mergedResultList.add(getValidationResultFromErrorParams(errorParams));
        }
        if (!globalValidationResult.getValidationResult().isEmpty())
        {
            DebugVerboseOutputter.printVerboseMessage(
                    PreflightingCoreNLS.ValidationManager_VerboseMessage_ProblemsGlobalParameters,
                    VerboseLevel.v2);
            mergedResultList.add(globalValidationResult);
        }
        if (!deviceValidationResultList.isEmpty())
        {
            mergedResultList.addAll(deviceValidationResultList);
        }

        return mergedResultList;
    }

    /***
     * Run the validation tool itself. The input parameters passed are first
     * validated and if they are valid, the checkers are run. If they are not
     * valid, the validation problems are printed.
     * 
     * @param commandLineParams
     *            The input parameters from command line.
     * 
     * @return The validation result from the checkers.
     * 
     * @throws PreflightingParameterException
     *             In case of problems with input parameters.
     * @throws PreflightingToolException
     *             In case of any other problems (not related to input
     *             parameters).
     */
    public synchronized List<ApplicationValidationResult> run(List<Parameter> commandLineParams)
            throws PreflightingParameterException, PreflightingToolException
    {
        List<Parameter> checkerParams = new ArrayList<Parameter>();
        List<Parameter> disabledCheckers = new ArrayList<Parameter>();
        List<Parameter> deviceParams = new ArrayList<Parameter>();
        List<ApplicationValidationResult> results = new ArrayList<ApplicationValidationResult>();

        Map<String, CheckerExtension> knownCheckers = loadCheckers();

        //validate parameters
        Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo =
                new LinkedHashMap<ValidationManager.WarningLevelAdjustmentType, Set<String>>(2);
        List<ValidationResult> mergedResultList =
                startParamsValidation(knownCheckers, commandLineParams, checkerParams,
                        disabledCheckers, deviceParams, adjustedWarningLevelInfo);

        //no problems regarding the parameters
        if (mergedResultList.isEmpty())
        {
            //by default all checkers and conditions are enabled 
            setAllCheckersConditionsAsEnabled(knownCheckers);

            // Retrieve the list of disabled checkers and disabled conditions
            // Implementing the -dc switch behavior
            // If no checker was selected, populate the list with all known checkers
            disableCheckersConditions(disabledCheckers, knownCheckers);

            List<ValidationResult> checkerValidationResultList =
                    validateCheckerParams(checkerParams, knownCheckers);

            List<String> invalidParamsCheckers =
                    new ArrayList<String>(checkerValidationResultList.size());
            if (!checkerValidationResultList.isEmpty())
            {
                for (ValidationResult checkerValidationResult : checkerValidationResultList)
                {
                    invalidParamsCheckers.add(checkerValidationResult.getCheckerId());
                }
                DebugVerboseOutputter
                        .printVerboseMessage(
                                PreflightingCoreNLS.ValidationManager_VerboseMessage_ProblemsCheckerParameters,
                                VerboseLevel.v2);
                DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v2); //$NON-NLS-1$
                printParameterErrors(checkerValidationResultList);
                //new line before results 
                DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v0);
            }

            enableCheckers(checkerParams, knownCheckers, invalidParamsCheckers);

            if (checkerParams.isEmpty()
                    || (!checkerParams.isEmpty() && (enabledCheckers(knownCheckers) > 0)))
            {
                //list of apks or single project
                ArrayList<String> applications = getResourcesToValidate();

                valManagerConfig = ValidationManagerConfiguration.getInstance();

                //for each application

                if ((applications != null) && (applications.size() > 0))
                {
                    for (String currentApplication : applications)
                    {
                        boolean isApk = currentApplication.endsWith(".apk"); //$NON-NLS-1$
                        Parameter appParam =
                                new Parameter(
                                        ValidationManager.InputParameter.APPLICATION_PATH
                                                .getAlias(),
                                        currentApplication);
                        //add path to the current app
                        globalParams.add(appParam);

                        ApplicationValidationResult applicationValidationResult =
                                validateApplication(knownCheckers, deviceParams, isApk,
                                        adjustedWarningLevelInfo, invalidParamsCheckers);

                        results.add(applicationValidationResult);

                        //remove path for next iteration
                        globalParams.remove(appParam);
                    }
                }
                else
                {
                    PreflightingToolException e =
                            new PreflightingToolException("No application(s) found");

                    throw e;
                }

                if (deleteApkTempFolder)
                {
                    deleteTempResources();
                }
            }
        }
        else
        {
            // print empty line to separate the messages for parameter errors
            DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v2); //$NON-NLS-1$
            if (!mergedResultList.isEmpty())
            {
                printParameterErrors(mergedResultList);
            }
            throw new PreflightingParameterException(
                    PreflightingCoreNLS.ValidationManager_InputParametersProblemMessage);
        }
        return results;
    }

    /**
     * Enable checkers according to -c parameters (-dc has higher precedence)
     * @param checkerParams
     * @param knownCheckers
     * @param invalidParamsCheckers
     */
    private void enableCheckers(List<Parameter> checkerParams,
            Map<String, CheckerExtension> knownCheckers, List<String> invalidParamsCheckers)
    {

        Set<String> enabled = new HashSet<String>();

        for (Parameter checkerParam : checkerParams)
        {
            enabled.add(checkerParam.getValue());
        }

        //implementing -c behavior 
        //case 2: (identify items with -c) if there is parameter, all checkers will be disabled (except the ones that are marked with -c)
        for (CheckerExtension checkerExt : knownCheckers.values())
        {
            IChecker checker = checkerExt.getChecker();

            // -c and contains or invalid
            if (((enabled.size() > 0) && (!enabled.contains(checker.getId())))
                    || invalidParamsCheckers.contains(checker.getId()))
            {
                checker.setEnabled(false);
            }

        }

    }

    /**
     * @param checkersToRun
     * @return
     */
    public List<CheckerExtension> findCheckersToRun(Collection<CheckerExtension> knownCheckers)
    {
        List<CheckerExtension> checkersToRun = new ArrayList<CheckerExtension>();
        for (CheckerExtension checkerExt : knownCheckers)
        {
            if (checkerExt.getChecker().isEnabled())
            {
                checkersToRun.add(checkerExt);
            }
        }
        return checkersToRun;
    }

    private ICondition extractCondition(Parameter disabledChecker,
            Map<String, CheckerExtension> knownCheckers)
    {

        ICondition condition = null;
        String str = disabledChecker.getValue();
        int pos = str.indexOf('.', 0);

        if (pos > 0)
        {
            String conditionStr = str.substring(pos + 1);
            String checkerStr = str.substring(0, pos);

            CheckerExtension checker = knownCheckers.get(checkerStr);

            if (checker != null)
            {
                Map<String, ICondition> conditionsMap = checker.getChecker().getConditions();

                Boolean runCheckerExecution = false;
                Collection<ICondition> conditions = conditionsMap.values();
                if (conditions != null)
                {
                    for (ICondition c : conditions)
                    {
                        if (c.getId().equals(conditionStr))
                        {
                            //sets if condition with the given id is disabled
                            condition = c;
                            condition.setEnabled(false);
                        }
                        //sets if checker should run (because at least one condition is enabled)
                        runCheckerExecution |= c.isEnabled();
                    }
                }

                checker.getChecker().setEnabled(runCheckerExecution);
            }
        }

        return condition;
    }

    /**
     * WARNING: This method is required because the algorithm relies that all checkers/conditions are enabled 
     * before applying -dc (disable checker or condition) and -c (enable checker).
     * 
     * Otherwise CheckerExtension objects, which are instantiate only once (on plug-in loading) will be with enabled in an inconsistent state.
     */
    private void setAllCheckersConditionsAsEnabled(Map<String, CheckerExtension> knownCheckers)
    {
        if ((knownCheckers != null) && !knownCheckers.isEmpty())
        {
            for (CheckerExtension chkExt : knownCheckers.values())
            {
                chkExt.getChecker().setEnabled(true);

                Map<String, ICondition> conditionsMap = chkExt.getChecker().getConditions();
                Collection<ICondition> conditions = conditionsMap.values();
                if (conditions != null)
                {
                    for (ICondition c : conditions)
                    {
                        c.setEnabled(true);
                    }
                }
            }
        }
    }

    /**
     * Disable checkers that apply. Retrieve disabledCheckers and disabledConditions 
     */
    private void disableCheckersConditions(List<Parameter> disabledCheckers,
            Map<String, CheckerExtension> knownCheckers)
    {

        //try to remove the checker described in disabledChecker
        for (Parameter disabledChecker : disabledCheckers)
        {
            ICondition condition = extractCondition(disabledChecker, knownCheckers);

            // if it is not a condition it might be a checker
            if (condition == null)
            {
                CheckerExtension checkerExtension = knownCheckers.get(disabledChecker.getValue());
                if (checkerExtension != null)
                {
                    //implementing -dc for checker
                    checkerExtension.getChecker().setEnabled(false);
                }
                // at this point, if neither a condition nor a checker were retrieved the parameter is incorrect
                else
                {

                    ValidationResultData resultData = new ValidationResultData();
                    resultData.setSeverity(SEVERITY.ERROR);
                    resultData.setIssueDescription(NLS.bind(
                            PreflightingCoreNLS.ValidationManager_UnknownCheckerOrConditionMessage,
                            disabledChecker.getValue()));

                    ValidationResult result = new ValidationResult(null, LimitedList.UNLIMITED);
                    result.addValidationResult(resultData);
                    ArrayList<ValidationResult> results = new ArrayList<ValidationResult>();
                    results.add(result);
                    printParameterErrors(results);
                }
            }
        }
    }

    /*
     * Extract zip file if any or simply returns the apk or project
     */
    private ArrayList<String> getResourcesToValidate() throws PreflightingToolException
    {
        String path = null;
        Parameter pathParam = null;
        for (Parameter param : globalParams)
        {
            //keep application path for later evaluation
            if (param.getParameterType().equals(InputParameter.APPLICATION_PATH.getAlias()))
            {
                path = param.getValue();
                pathParam = param;
            }
        }
        globalParams.remove(pathParam);

        ArrayList<String> applications = new ArrayList<String>();
        //path to a .zip file with .apks inside it
        if (path.endsWith(ApkUtils.ZIP_EXTENSION))
        {
            //extract zip to temp folder and store its apks paths
            File zipFolder = ApkUtils.unzip(new File(path));
            tempResourcesToDelete.add(zipFolder.getAbsolutePath());

            File[] apks = zipFolder.listFiles();
            for (int i = 0; i < apks.length; i++)
            {
                applications.add(apks[i].getAbsolutePath());
            }
            DebugVerboseOutputter.setCurrentVerboseLevel(VerboseLevel.v0);
        }
        else
        //project or .apk
        {
            applications.add(path);
        }

        return applications;
    }

    /**
     * Applies a change in the level for the list of {@link ValidationResult} provided.
     * @param knownCheckers Map of checkers.
     * @param adjustedWarningLevelInfo The map of levels to adjust.
     * @param validationResult The results to change the levels.
     */
    private void applyWarningLevelAdjustment(Map<String, CheckerExtension> knownCheckers,
            Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo,
            List<ValidationResult> validationResult)
    {
        Map<String, List<String>> exceptionsMap = null;
        Map<WarningLevelAdjustmentType, List<String>> checkersIdsMap =
                new HashMap<WarningLevelAdjustmentType, List<String>>(2);
        Map<WarningLevelAdjustmentType, Map<String, List<String>>> conditionsIdsMap =
                new HashMap<WarningLevelAdjustmentType, Map<String, List<String>>>(2);

        // Extract what will be adjusted, whole checkers and separated
        // conditions
        for (WarningLevelAdjustmentType type : adjustedWarningLevelInfo.keySet())
        {
            Set<String> checkersSet = adjustedWarningLevelInfo.get(type);
            List<String> checkerIdsToAdjust = null;
            Map<String, List<String>> conditionIdsToAdjust = null;

            if ((checkersSet != null) && !checkersSet.isEmpty())
            {
                checkerIdsToAdjust = new ArrayList<String>(checkersSet.size());
                conditionIdsToAdjust = new HashMap<String, List<String>>(checkersSet.size());
                if (exceptionsMap == null)
                {
                    exceptionsMap = new HashMap<String, List<String>>(2 * checkersSet.size());
                }

                for (String completeId : checkersSet)
                {
                    String[] split = completeId.split("\\."); //$NON-NLS-1$
                    if (split.length > 1)
                    {
                        String checkerId = split[0];
                        String conditionId = split[1];

                        // update conditions to adjust map
                        List<String> checkerConditions = null;
                        if (conditionIdsToAdjust.containsKey(checkerId))
                        {
                            checkerConditions = conditionIdsToAdjust.get(checkerId);
                        }
                        else
                        {
                            checkerConditions = new ArrayList<String>(5);
                        }
                        checkerConditions.add(conditionId);
                        conditionIdsToAdjust.put(checkerId, checkerConditions);

                        // update exceptions map, it contains every condition
                        // processed, doesn't matter the condition
                        if (exceptionsMap.containsKey(checkerId))
                        {
                            checkerConditions = exceptionsMap.get(checkerId);
                        }
                        else
                        {
                            checkerConditions = new ArrayList<String>(5);
                        }
                        checkerConditions.add(conditionId);
                        exceptionsMap.put(checkerId, checkerConditions);
                    }
                    else
                    {
                        checkerIdsToAdjust.add(completeId);
                    }
                }
            }
            else
            {
                checkerIdsToAdjust = new ArrayList<String>(knownCheckers.keySet());
            }
            checkersIdsMap.put(type, checkerIdsToAdjust);
            conditionsIdsMap.put(type, conditionIdsToAdjust);
        }

        // Adjust conditions first, it is higher priority.
        for (WarningLevelAdjustmentType type : conditionsIdsMap.keySet())
        {
            if (conditionsIdsMap != null)
            {
                Map<String, List<String>> conditionsToAjust = conditionsIdsMap.get(type);
                if ((conditionsToAjust != null) && !conditionsToAjust.isEmpty())
                {
                    WarningLevelFilter.adjustWarningLevels(validationResult,
                            type == WarningLevelAdjustmentType.INCREASE, null, conditionsToAjust,
                            null);
                }
            }

        }

        // Adjust checkers, set the conditionsMap as the ignore map, so it won't
        // be processed again.
        for (WarningLevelAdjustmentType type : checkersIdsMap.keySet())
        {
            if (checkersIdsMap != null)
            {
                List<String> checkersIdsToAdjust = checkersIdsMap.get(type);
                if ((checkersIdsToAdjust != null) && !checkersIdsToAdjust.isEmpty())
                {
                    WarningLevelFilter.adjustWarningLevels(validationResult,
                            type == WarningLevelAdjustmentType.INCREASE, checkersIdsToAdjust, null,
                            exceptionsMap);
                }
            }
        }
    }

    /**
     * Prints the validation result for input parameters.
     * 
     * @param parametersValidationResults
     *            The results of the input parameters validation.
     */
    private void printParameterErrors(List<ValidationResult> parametersValidationResults)
    {
        for (ValidationResult parameterResult : parametersValidationResults)
        {
            for (ValidationResultData parameterResultData : parameterResult.getValidationResult())
            {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(parameterResultData.getSeverity().toString());
                if (parameterResultData.getIssueDescription() != null)
                {
                    stringBuilder.append(": "); //$NON-NLS-1$
                    stringBuilder.append(parameterResultData.getIssueDescription());
                }
                DebugVerboseOutputter
                        .printVerboseMessage(stringBuilder.toString(), VerboseLevel.v0);
            }
        }
    }

    private ValidationResult getValidationResultFromErrorParams(List<Parameter> errorParams)
    {

        ValidationResult validationResult = new ValidationResult(null, LimitedList.UNLIMITED);

        for (Parameter param : errorParams)
        {
            ValidationResultData resultData = new ValidationResultData();
            resultData.setSeverity(SEVERITY.ERROR);
            resultData
                    .setIssueDescription(PreflightingCoreNLS.ValidationManager_UnknownParameterMessage
                            + param.getParameterType());
            validationResult.addValidationResult(resultData);
        }

        return validationResult;
    }

    private int enabledCheckers(Map<String, CheckerExtension> knownCheckers)
    {

        int counter = 0;

        if ((knownCheckers != null) && (knownCheckers.values() != null))
        {
            for (CheckerExtension c : knownCheckers.values())
            {
                counter += c.getChecker().isEnabled() ? 1 : 0;
            }
        }

        return counter;
    }

    /***
     * Run the checkers passed. If the list is empty or <code>null</code>, run
     * all checkers.
     * 
     * @param checkersList
     *            The list of checkers to be run, or an empty list (or
     *            <code>null</code> list) for running all checkers
     * @param knownCheckers
     *            The list of all known checkers, which will be run in case
     *            there is no specific checkers passed on checkersToRun
     * @param currentApplication 
     * @param adjustedWarningLevelInfo 
     * 
     * @return The list of validation results
     * 
     * @throws PreflightingToolException
     * 
     */
    private ApplicationValidationResult validateApplication(
            Map<String, CheckerExtension> knownCheckers, List<Parameter> deviceParams,
            boolean isApk, Map<WarningLevelAdjustmentType, Set<String>> adjustedWarningLevelInfo,
            List<String> invalidParamsCheckers) throws PreflightingToolException
    {
        ApplicationValidationResult applicationResult = null;

        List<ValidationResult> results = new ArrayList<ValidationResult>();

        int checkersCounter = enabledCheckers(knownCheckers);

        if ((knownCheckers != null) && (checkersCounter == knownCheckers.size()))
        {
            DebugVerboseOutputter.printVerboseMessage(
                    PreflightingCoreNLS.ValidationManager_VerboseMessage_AllCheckersRun,
                    VerboseLevel.v1);

        }

        // Will be used to print a message in case no checker was executed
        boolean wasAnyCheckerApplicable = false;

        // Will be used to print a message in case a checker failed to execute
        // due to an exception
        List<String> failedCheckers = new ArrayList<String>();

        if ((knownCheckers != null) && (checkersCounter > 0))
        {
            // run the necessary checkers

            // Try to create application data
            ApplicationData applicationData = null;

            try
            {
                applicationData = new ApplicationData(getGlobalParameters());
                if (isApk)
                {
                    tempResourcesToDelete.add(applicationData.getRootElementPath());
                }
            }
            catch (PreflightingToolException e)
            {
                DebugVerboseOutputter.printVerboseMessage(
                        PreflightingCoreNLS.ValidationManager_ErrorRetrievingApplicationData,
                        VerboseLevel.v1);
                DebugVerboseOutputter.printVerboseMessage(e.getMessage(), VerboseLevel.v2);
                throw new PreflightingToolException(
                        PreflightingCoreNLS.ValidationManager_ErrorRetrievingApplicationData, e);
            }

            if (applicationData != null)
            {
                applicationResult =
                        new ApplicationValidationResult(applicationData.getName(),
                                applicationData.getVersion(), applicationData.getApplicationPath());
                XMLElement manifestElement = applicationData.getManifestElement();
                if (manifestElement != null)
                {
                    applicationResult.setXmlResultDocument(manifestElement.getDocument());
                }

                List<DeviceSpecification> deviceSpecs = null;

                if (!noneDeviceSpecified)
                {
                    deviceSpecs = filterDeviceSpecifications(deviceParams);
                }
                else
                {
                    deviceSpecs = Collections.emptyList();
                    DebugVerboseOutputter
                            .printVerboseMessage(
                                    PreflightingCoreNLS.ValidationManager_ValidationManager_VerboseMessage_Skipping_Device_Verifications,
                                    VerboseLevel.v1);
                }

                boolean limitReached = false;
                int limit = getLimit();
                for (CheckerExtension checkerExt : knownCheckers.values())
                {
                    if (checkerExt.getChecker().isEnabled())
                    {
                        IChecker checker = checkerExt.getChecker();

                        DebugVerboseOutputter
                                .printVerboseMessage(
                                        PreflightingCoreNLS
                                                .bind(PreflightingCoreNLS.ValidationManager_VerboseMessage_VerifyingCheckerRun,
                                                        checkerExt.getId()), VerboseLevel.v2);
                        /*
                         * Checkers will return a IStatus in the method canExecute
                         * to explain any problems preventing the execution. They
                         * also should throw exceptions
                         * (PreflightingCheckerException) whenever a problem occurs
                         * during execution.
                         */
                        try
                        {

                            //The returned status can be either a single status or 
                            //a MultiStatus with the statuses from every condition
                            //ran for this specific checker

                            IStatus canExecuteChecker =
                                    checker.canExecute(applicationData, deviceSpecs);
                            if (canExecuteChecker.isMultiStatus())
                            {
                                if (!canExecuteChecker.isOK()) //There are at least one condition that can't be executed
                                {
                                    IStatus canProceed = null;
                                    for (IStatus conditionStatus : canExecuteChecker.getChildren())
                                    {
                                        if (!conditionStatus.isOK())
                                        {
                                            CanExecuteConditionStatus canExecuteConditionStatus =
                                                    (CanExecuteConditionStatus) conditionStatus;
                                            DebugVerboseOutputter
                                                    .printVerboseMessage(
                                                            PreflightingCoreNLS
                                                                    .bind(PreflightingCoreNLS.ValidationManager_UnableToExecuteCondition,
                                                                            canExecuteConditionStatus
                                                                                    .getConditionId(),
                                                                            checkerExt.getId()),
                                                            VerboseLevel.v0);

                                            DebugVerboseOutputter
                                                    .printVerboseMessage(
                                                            PreflightingCoreNLS
                                                                    .bind(PreflightingCoreNLS.ValidationManager_UnableToExecuteCondition_Detailed,
                                                                            new String[]
                                                                            {
                                                                                    canExecuteConditionStatus
                                                                                            .getConditionId(),
                                                                                    checkerExt
                                                                                            .getId(),
                                                                                    canExecuteConditionStatus
                                                                                            .getMessage()
                                                                            }), VerboseLevel.v1);

                                        }
                                        else
                                        {
                                            canProceed = conditionStatus;
                                        }
                                    }
                                    canExecuteChecker = canProceed;
                                }
                            }

                            // that is none of the conditions were able to run
                            if (canExecuteChecker == null)
                            {
                                canExecuteChecker =
                                        new Status(
                                                IStatus.CANCEL,
                                                PreflightingCorePlugin.PLUGIN_ID,
                                                PreflightingCoreNLS.ValidationManager_NoConditionsReason);
                            }

                            if (!canExecuteChecker.isOK())
                            {
                                DebugVerboseOutputter
                                        .printVerboseMessage(
                                                PreflightingCoreNLS
                                                        .bind(PreflightingCoreNLS.ValidationManager_UnableToExecuteCheckerMessage,
                                                                checkerExt.getId()),
                                                VerboseLevel.v0);

                                DebugVerboseOutputter
                                        .printVerboseMessage(
                                                PreflightingCoreNLS
                                                        .bind(PreflightingCoreNLS.ValidationManager_UnableToExecuteCheckerMessage_Detailed,
                                                                checkerExt.getId(),
                                                                canExecuteChecker.getMessage()),
                                                VerboseLevel.v1);

                                applicationResult
                                        .addStatus(
                                                checkerExt.getId(),
                                                new Status(
                                                        IStatus.CANCEL,
                                                        PreflightingCorePlugin.PLUGIN_ID,
                                                        PreflightingCoreNLS
                                                                .bind(PreflightingCoreNLS.ValidationManager_UnableToExecuteCheckerMessage_Detailed,
                                                                        checkerExt.getId(),
                                                                        canExecuteChecker
                                                                                .getMessage())));
                            }
                            else
                            {
                                wasAnyCheckerApplicable = true;
                                DebugVerboseOutputter
                                        .printVerboseMessage(
                                                PreflightingCoreNLS
                                                        .bind(PreflightingCoreNLS.ValidationManager_VerboseMessage_RunningChecker,
                                                                checkerExt.getId()),
                                                VerboseLevel.v2);
                                ValidationResult checkerResults = null;
                                try
                                {
                                    checkerResults = new ValidationResult(checker.getId(), limit);
                                    checker.validateApplication(applicationData, deviceSpecs,
                                            valManagerConfig, checkerResults);
                                    applicationResult.addStatus(checkerExt.getId(),
                                            Status.OK_STATUS);
                                    if (checkerResults != null)
                                    {
                                        results.add(checkerResults);

                                        int resultsSize =
                                                checkerResults.getValidationResult().size();
                                        if (limit != LimitedList.UNLIMITED)
                                        {
                                            limit -= resultsSize;
                                            if (limit <= 0)
                                            {
                                                limitReached = true;
                                            }
                                        }
                                    }
                                }
                                catch (PreflightingCheckerException e)
                                {
                                    failedCheckers.add(checkerExt.getId());

                                    applicationResult.addStatus(
                                            checkerExt.getId(),
                                            new Status(IStatus.ERROR,
                                                    PreflightingCorePlugin.PLUGIN_ID, e
                                                            .getMessage()));

                                    PreflightingLogger.error(this.getClass(),
                                            "Unexpected exception while running checker " //$NON-NLS-1$
                                                    + checkerExt.getId(), e);

                                    DebugVerboseOutputter
                                            .printVerboseMessage(
                                                    PreflightingCoreNLS
                                                            .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker,
                                                                    checkerExt.getId()),
                                                    VerboseLevel.v1);

                                    DebugVerboseOutputter.printVerboseMessage(e.getMessage(),
                                            VerboseLevel.v2);

                                }
                                catch (ValidationLimitException e)
                                {
                                    if (checkerResults != null)
                                    {
                                        results.add(checkerResults);
                                    }
                                    limitReached = true;
                                }
                                catch (Exception e)
                                {
                                    failedCheckers.add(checkerExt.getId());

                                    PreflightingLogger.error(this.getClass(),
                                            "Unexpected exception while running checker " //$NON-NLS-1$
                                                    + checkerExt.getName(), e);

                                    applicationResult
                                            .addStatus(
                                                    checkerExt.getId(),
                                                    new Status(
                                                            IStatus.ERROR,
                                                            PreflightingCorePlugin.PLUGIN_ID,
                                                            PreflightingCoreNLS
                                                                    .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker_V2,
                                                                            e.getLocalizedMessage(),
                                                                            checkerExt.getId())));

                                    DebugVerboseOutputter
                                            .printVerboseMessage(
                                                    PreflightingCoreNLS
                                                            .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker,
                                                                    checkerExt.getId()),
                                                    VerboseLevel.v1);

                                    DebugVerboseOutputter
                                            .printVerboseMessage(
                                                    PreflightingCoreNLS
                                                            .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker_V2,
                                                                    e.getLocalizedMessage(),
                                                                    checkerExt.getId()),
                                                    VerboseLevel.v2);
                                }

                                DebugVerboseOutputter
                                        .printVerboseMessage(
                                                PreflightingCoreNLS
                                                        .bind(PreflightingCoreNLS.ValidationManager_VerboseMessage_CheckerFinished,
                                                                checkerExt.getId()),
                                                VerboseLevel.v2);

                            }
                        }
                        catch (Exception e)
                        {
                            failedCheckers.add(checkerExt.getId());

                            PreflightingLogger.error(this.getClass(),
                                    "Unexpected exception while running checker " //$NON-NLS-1$
                                            + checkerExt.getName(), e);

                            applicationResult
                                    .addStatus(
                                            checkerExt.getId(),
                                            new Status(
                                                    IStatus.ERROR,
                                                    PreflightingCorePlugin.PLUGIN_ID,
                                                    PreflightingCoreNLS
                                                            .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker_V2,
                                                                    e.getLocalizedMessage(),
                                                                    checkerExt.getId())));

                            DebugVerboseOutputter
                                    .printVerboseMessage(
                                            PreflightingCoreNLS
                                                    .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker,
                                                            checkerExt.getId()), VerboseLevel.v1);

                            DebugVerboseOutputter
                                    .printVerboseMessage(
                                            PreflightingCoreNLS
                                                    .bind(PreflightingCoreNLS.ValidationManager_UnexpectedExceptiononChecker_V2,
                                                            e.getLocalizedMessage(),
                                                            checkerExt.getId()), VerboseLevel.v2);
                        }

                        if (limitReached)
                        {
                            DebugVerboseOutputter.printVerboseMessage(PreflightingCoreNLS.bind(
                                    PreflightingCoreNLS.ValidationManager_ValidationLimitReached,
                                    getLimit()), VerboseLevel.v2);
                            break;
                        }

                        //clean checker data
                        checker.clean();
                        //call Garbage Collector to free memory
                        Runtime.getRuntime().gc();
                    }
                    else
                    {
                        Status status = null;
                        IChecker checker = checkerExt.getChecker();

                        //if checker was not disabled but a mandatory parameter is missing, the checker will
                        //be disabled. 
                        if ((invalidParamsCheckers != null)
                                && (invalidParamsCheckers.contains(checker.getId())))
                        {
                            status =
                                    new Status(
                                            IStatus.ERROR,
                                            PreflightingCorePlugin.PLUGIN_ID,
                                            PreflightingCoreNLS.ValidationManager_MandatoryParameterMissing);
                        }
                        //if checker is not in the list of invalid parameters, it was manually disabled by the user.
                        else
                        {
                            status =
                                    new Status(
                                            IStatus.INFO,
                                            PreflightingCorePlugin.PLUGIN_ID,
                                            PreflightingCoreNLS.ValidationManager_CheckerWasDisabled);
                        }

                        applicationResult.addStatus(checkerExt.getChecker().getId(), status);
                    }

                }

                //clean up application data private instance objects
                applicationData.clean();
                //clean data from APK
                AaptUtils.cleanApplicationResourceValues();
                //after all checkers run - call Garbage Collector to free memory
                Runtime.getRuntime().gc();
            }

        }

        if (!wasAnyCheckerApplicable)
        {
            // No checker was executed, warns user about that
            DebugVerboseOutputter.printVerboseMessage(
                    PreflightingCoreNLS.ValidationManager_Errors_NoValidationApplyOrError,
                    VerboseLevel.v0);
        }

        /*
         * 
         * REMOVED BLOCK
         * Execution Status will be printed after execution
         * 
         * 
         * 
        if (!failedCheckers.isEmpty())
        {
            // At least one checker failed to execute due to an exception.
            StringBuffer formattedFailedCheckers = new StringBuffer();
            for (String checkerId : failedCheckers)
            {
                formattedFailedCheckers.append("\n\t"); //$NON-NLS-1$
                formattedFailedCheckers.append(checkerId);
            }
            VerboseOutputter.printVerboseMessage(NLS.bind(
                    PreflightingCoreNLS.ValidationManager_FailedCheckers,
                    formattedFailedCheckers.toString()), VerboseLevel.v0);

            if ((VerboseOutputter.getCurrentVerboseLevel() == VerboseLevel.v0)
                    || (VerboseOutputter.getCurrentVerboseLevel() == VerboseLevel.v1))
            {
                VerboseOutputter.printVerboseMessage(
                        PreflightingCoreNLS.ValidationManager_FailedCheckers_IncreaseVerbosity,
                        VerboseLevel.v0);
            }

        }*/

        // if the warning level for the validation result needs to be
        // adjusted, do it
        if (!adjustedWarningLevelInfo.isEmpty())
        {
            applyWarningLevelAdjustment(knownCheckers, adjustedWarningLevelInfo, results);
        }

        // filter results according to warning level before printing
        results = WarningLevelFilter.filterValidationResultsForCurrentWarningLevel(results);
        applicationResult.addResult(results);
        // return full list of results
        return applicationResult;
    }

    /**
     * @param checkersList
     * @param knownCheckers 
     * @return
     */

    private int getLimit()
    {
        int limit = LimitedList.UNLIMITED;
        for (Parameter param : globalParams)
        {
            if (InputParameter.LIMIT.getAlias().equals(param.getParameterType()))
            {
                try
                {
                    limit = Integer.parseInt(param.getValue());
                }
                catch (NumberFormatException nfe)
                {
                    //do nothing
                }
            }
        }
        return limit;
    }

    /**
     * Sets the global parameters for the validation
     * @param globalParams
     */
    private void setGlobalParameters(List<Parameter> globalParams)
    {
        this.globalParams = globalParams;
    }

    /**
     * Get the list of global arguments and their values.
     * 
     * @return The list of global Parameters.
     */
    public List<Parameter> getGlobalParameters()
    {
        return globalParams;
    }

    /**
     * Get the description for the parameters of the given checker.
     * @param checkerId
     * @return List of parameter descriptions.
     */
    public List<ParameterDescription> getParametersDescription(String checkerId)
    {
        if (checkerId == null)
        {
            return new ArrayList<ParameterDescription>(getParametersDescriptionAsMap().values());
        }
        else
        {
            CheckerExtension checkerExt = getCheckerExtension(checkerId);
            if (checkerExt != null)
            {
                return checkerExt.getChecker().getParameterDescriptions();
            }
            else
            {
                return null;
            }
        }
    }

    /** 
     * @return Map from {@link ParameterDescription#getName()} to {@link ParameterDescription}
     */
    public Map<String, ParameterDescription> getParametersDescriptionAsMap()
    {
        if (globalParametersDescriptions.isEmpty())
        {
            populateParametersDescriptionList();
        }

        return new LinkedHashMap<String, ParameterDescription>(globalParametersDescriptions);
    }

    private void populateParametersDescriptionList()
    {
        ParameterDescription desc = new ParameterDescription();
        desc.setName(InputParameter.SDK_PATH.getAlias());
        desc.setDescription(PreflightingCoreNLS.ValidationManager_SdkPathDescriptionMessage);
        desc.setValueDescription("SDKPATH"); //$NON-NLS-1$
        desc.setType(ParameterType.STRING);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(CHECKER_PARAMETER);
        desc.setDescription(PreflightingCoreNLS.ValidationManager_CheckerDescriptionMessage);
        desc.setValueDescription("CHK [PRM]..."); //$NON-NLS-1$
        desc.setType(ParameterType.STRING);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(DISABLE_CHECKER_PARAMETER);
        desc.setDescription(PreflightingCoreNLS.ValidationManager_DisableCheckerDescriptionMessage);
        desc.setValueDescription("CHK[.CND]"); //$NON-NLS-1$
        desc.setType(ParameterType.STRING);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(DEVICE_PARAMETER);
        desc.setDescription(PreflightingCoreNLS.ValidationManager_DeviceDescriptionMessage);
        desc.setValueDescription("[DEV]"); //$NON-NLS-1$
        desc.setType(ParameterType.STRING);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(InputParameter.WARNING_TO_ERROR.getAlias());
        desc.setValueDescription("[CHK[.CND]]..."); //$NON-NLS-1$
        desc.setDescription(PreflightingCoreNLS.ValidationManager_WarningToErrorDescriptionMessage);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(InputParameter.ERROR_TO_WARNING.getAlias());
        desc.setValueDescription("[CHK[.CND]]..."); //$NON-NLS-1$
        desc.setDescription(PreflightingCoreNLS.ValidationManager_ErrorToWarningDescriptionMessage);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(InputParameter.OUTPUT.getAlias());
        desc.setValueDescription(PreflightingCoreNLS.ValidationManager_OutputSintaxMessage);
        desc.setDescription(PreflightingCoreNLS.ValidationManager_OutputDescriptionMessage);
        globalParametersDescriptions.put(desc.getName(), desc);

        desc = new ParameterDescription();
        desc.setName(InputParameter.LIMIT.getAlias());
        desc.setValueDescription("[COUNT]"); //$NON-NLS-1$
        desc.setDescription(PreflightingCoreNLS.ValidationManager_LimitDescription);
        globalParametersDescriptions.put(desc.getName(), desc);
    }

    /**
     * Get a list of checkers (with id and description).
     * 
     * @return
     */
    public List<CheckerDescription> getCheckersDescription()
    {
        List<CheckerDescription> list = new ArrayList<CheckerDescription>();
        try
        {
            loadCheckers();
            for (String checkerId : checkers.keySet())
            {
                CheckerDescription chkDesc = new CheckerDescription();
                CheckerExtension checkerExt = checkers.get(checkerId);

                chkDesc.setId(checkerId);
                chkDesc.setName(checkerExt.getName());
                chkDesc.setDescription(checkerExt.getDescription());

                list.add(chkDesc);
            }
        }
        catch (PreflightingExtensionPointException e)
        {
            // Do nothing
        }
        return list;
    }

    /**
     * Get all conditions for the checker passed as parameter.
     * The information is retrieved from the extension point.
     * 
     * @param checkerId The checker id to have its conditions retrieved.
     * @return All conditions of the checker passed as parameter.
     */
    public List<Condition> getCheckerConditions(String checkerId)
    {
        List<Condition> conditionList = null;

        try
        {
            loadCheckers();

            CheckerExtension checkerExt = checkers.get(checkerId);

            conditionList = new ArrayList(checkerExt.getChecker().getConditions().values());

        }
        catch (PreflightingExtensionPointException e)
        {
            // Do nothing
        }

        return conditionList;

    }

    /**
     * Get a list of devices available (with name and description).
     * 
     * @return
     */
    public List<Value> getDevicesInfoList()
    {
        ArrayList<Value> list = new ArrayList<Value>();
        for (DeviceSpecification currentDevice : devicesSpecsContainer.getDeviceSpecifications())
        {
            Value v = new Value();
            v.setValue(currentDevice.getName() + " - [" + currentDevice.getId() + "]"); //$NON-NLS-1$ $NON-NLS-2$
            v.setDescription(""); //$NON-NLS-1$
            list.add(v);
        }

        return list;
    }

    /**
     * Get a device description.
     * 
     * @return
     */
    public String getDeviceDescription(String deviceId)
    {
        String deviceDescr = null;
        for (DeviceSpecification currentDevice : devicesSpecsContainer.getDeviceSpecifications())
        {
            if ((currentDevice.getId() != null) && currentDevice.getId().equals(deviceId))
            {
                deviceDescr = currentDevice.getDeviceInfo().toString();
                break;
            }
        }
        return deviceDescr;
    }

    /**
     * Types of warning level available adjustment.
     */
    public enum WarningLevelAdjustmentType
    {
        INCREASE, DECREASE;
    }

    /**
     * @param deleteApkTempFolder
     *            The deleteApkTempFolder to set
     */
    public void setDeleteApkTempFolder(boolean deleteApkTempFolder)
    {
        this.deleteApkTempFolder = deleteApkTempFolder;
    }

    private void deleteTempResources()
    {
        for (String currentPath : tempResourcesToDelete)
        {
            File currentFile = new File(currentPath);
            if ((currentFile != null) && currentFile.exists())
            {
                try
                {
                    // Try to delete the APK temp folder
                    if (!ProjectUtils.deleteDirRecursively(currentFile))
                    {
                        currentFile.deleteOnExit();
                    }
                }
                catch (IOException e)
                {
                    // If the attempt above fails, try to schedule a
                    // deletion when JVM execution is finished
                    currentFile.deleteOnExit();
                }
            }
        }
    }

    /**
     * @return The {@link DevicesSpecsContainer} for the validator.
     */
    public DevicesSpecsContainer getDevicesSpecsContainer()
    {
        return devicesSpecsContainer;
    }
}
