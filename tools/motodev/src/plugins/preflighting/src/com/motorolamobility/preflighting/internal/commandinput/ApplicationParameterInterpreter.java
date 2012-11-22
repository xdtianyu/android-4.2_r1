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
package com.motorolamobility.preflighting.internal.commandinput;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.osgi.util.NLS;

import com.motorolamobility.preflighting.core.checker.CheckerDescription;
import com.motorolamobility.preflighting.core.checker.CheckerExtension;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.validation.GlobalInputParamsValidator;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ParameterDescription;
import com.motorolamobility.preflighting.core.validation.ParameterType;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.validation.Value;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter.WarningLevel;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.internal.commandinput.exception.ParameterParseException;
import com.motorolamobility.preflighting.internal.commandoutput.OutputterFactory;
import com.motorolamobility.preflighting.internal.daemon.Daemon;
import com.motorolamobility.preflighting.internal.help.printer.HelpPrinter;

public class ApplicationParameterInterpreter
{
    public static final String PARAMETER_WARNING = "wN"; //$NON-NLS-1$

    public static final String WARNING_FLAG = PARAMETER_WARNING.substring(0, 1);

    public static final String PARAM_VERBOSITY = "vN"; //$NON-NLS-1$

    public static final String VERBOSITY_FLAG = PARAM_VERBOSITY.substring(0, 1);

    /**
     * Theses parameters are defined by Application. Application interprets
     * these parameters.
     */
    public static final String PARAM_HELP = "help"; //$NON-NLS-1$

    public static final String PARAM_LIST_CHECKERS = "list-checkers"; //$NON-NLS-1$

    public static final String PARAM_LIST_DEVICES = "list-devices"; //$NON-NLS-1$

    // Hidden parameter. For internal use only.
    public static final String KEEP_TEMP_FILES = "keepTempFiles"; //$NON-NLS-1$

    public static final String PARAM_DESC_DEVICE = "describe-device"; //$NON-NLS-1$

    public static final String PARAM_DAEMON = "daemon"; //$NON-NLS-1$

    private static final String PARAM_DEBUG = "showDebugMessages";

    /**
     * Check if there are application parameters within the parameter list.
     * Also, retrieve verbosity and warning levels from command line, if any,
     * and set the appropriate values.
     * 
     * @param parameters
     *            all parameters passed to application
     * @return true if there are application parameters, false otherwise
     * @throws ParameterParseException
     */
    public static boolean checkApplicationParameters(List<Parameter> parameters,
            ValidationManager validationManager, PrintStream printStream)
            throws ParameterParseException, PreflightingToolException
    {

        // retrieve warning level and verbosity level so that everything is
        // set before the application actually runs
        List<Parameter> parametersCopy = new ArrayList<Parameter>(parameters);

        // Reset the boolean just to make sure that files are not deleted only
        // when the user passes the appropriate argument
        validationManager.setDeleteApkTempFolder(true);

        for (Parameter param : parametersCopy)
        {
            String parameterType = param.getParameterType();

            // Check for the hidden keepTempFiles parameter
            if (parameterType.equals(KEEP_TEMP_FILES))
            {
                validationManager.setDeleteApkTempFolder(false);
                parameters.remove(param);
            }
            // ignore "-wx", check only for "-wN" parameters (and "-vN")
            else if (!InputParameter.WARNING_TO_ERROR.getAlias().equals(parameterType)
                    && (parameterType.startsWith(VERBOSITY_FLAG) || parameterType
                            .startsWith(WARNING_FLAG)))
            {
                if (param.getValue() != null)
                {
                    throw new ParameterParseException(NLS.bind(
                            PreflightingNLS.ApplicationParameterInterpreter_V_W_PARAMETER_ERROR,
                            parameterType));
                }

                if (parameterType.startsWith(VERBOSITY_FLAG))
                {
                    VerboseLevel verboseLevel = DebugVerboseOutputter.DEFAULT_VERBOSE_LEVEL;
                    try
                    {
                        // if the value passed is not a valid enum value, an
                        // IllegalArgumentException
                        // will be thrown
                        verboseLevel =
                                VerboseLevel.valueOf(VerboseLevel.class, parameterType.trim());

                        DebugVerboseOutputter.setCurrentVerboseLevel(verboseLevel);
                    }
                    catch (Exception e)
                    {
                        throw new ParameterParseException(
                                PreflightingNLS.ApplicationParameterInterpreter_InvalidVerbosityLevel);
                    }
                }
                else if (parameterType.startsWith(WARNING_FLAG))
                {
                    WarningLevel warningLevel = WarningLevelFilter.DEFAULT_WARNING_LEVEL;
                    try
                    {
                        // if the value passed is not a valid enum value, an
                        // IllegalArgumentException
                        // will be thrown
                        warningLevel =
                                WarningLevel.valueOf(WarningLevel.class, parameterType.trim());
                    }
                    catch (Exception e)
                    {
                        throw new ParameterParseException(
                                PreflightingNLS.ApplicationParameterInterpreter_InvalidWarningLevel);
                    }
                    WarningLevelFilter.setCurrentWarningLevel(warningLevel);
                }
                // remove from original list so it is not passed along
                parameters.remove(param);
            }
        }

        return checkHelpParameter(parameters, validationManager, printStream)
                || checkListParameters(parameters, validationManager, printStream)
                || checkDaemonParameter(parameters);
    }

    public static void validateOutputParam(String paramValue) throws ParameterParseException
    {
        if (paramValue != null)
        {
            if (!OutputterFactory.getInstance().isOutputterAvailable(paramValue).isOK())
            {
                throw new ParameterParseException(PreflightingNLS.bind(
                        PreflightingNLS.ApplicationParameterInterpreter_OutputParam, paramValue));
            }
        }
        else
        {
            throw new ParameterParseException(PreflightingNLS.bind(
                    PreflightingNLS.ApplicationParameterInterpreter_OutputParam, ""));
        }
    }

    private static boolean checkDaemonParameter(List<Parameter> parameters)
            throws ParameterParseException, PreflightingToolException
    {
        boolean hasDaemonParameter = false;
        Parameter pDaemon = getParameter(parameters, PARAM_DAEMON);
        Parameter pSDK = getParameter(parameters, InputParameter.SDK_PATH.getAlias());
        Parameter pDebug = getParameter(parameters, PARAM_DEBUG);

        String sdkPath = (pSDK == null ? null : pSDK.getValue());

        if (pDaemon != null)
        {
            hasDaemonParameter = true;

            int serverPort = Daemon.DEFAULT_PORT;
            if (pDaemon.getValue() != null)
            {
                try
                {
                    serverPort = Integer.parseInt(pDaemon.getValue());
                }
                catch (NumberFormatException nfe)
                {
                    throw new ParameterParseException(NLS.bind(
                            PreflightingNLS.ApplicationParameterInterpreter_InvalidPort,
                            pDaemon.getValue()));
                }
            }
            else
            {
                DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                        PreflightingNLS.ApplicationParameterInterpreter_UsingDefaultPort,
                        Daemon.DEFAULT_PORT), VerboseLevel.v0);

            }

            if (sdkPath != null)
            {
                ValidationResultData resultData = new ValidationResultData();
                GlobalInputParamsValidator.validateSdkParam(resultData, sdkPath);

                if (resultData.getSeverity() != SEVERITY.OK)
                {
                    throw new ParameterParseException(resultData.getIssueDescription());
                }

            }

            try
            {
                Daemon daemon = new Daemon(serverPort, sdkPath);

                //The first connection test goes to console instead of nullStream
                daemon.setDebugOn(true);

                daemon.startDaemon();

                //wait for daemon to bound port 
                Thread.sleep(Daemon.BOUND_TIMEOUT);

                daemon.testDaemon();

                // set debug true if the -debugDaemon is passed
                daemon.setDebugOn(pDebug != null);

                //never exits
                daemon.join();
            }
            catch (Exception e)
            {
                throw new PreflightingToolException(e.getMessage(), e);
            }
        }

        return hasDaemonParameter;
    }

    private static boolean checkListParameters(List<Parameter> parameters,
            ValidationManager validationManager, PrintStream printStream)
            throws ParameterParseException
    {
        boolean hasListParameter = false;
        Parameter p = getParameter(parameters, PARAM_LIST_CHECKERS);
        if (p != null)
        {
            hasListParameter = true;
            List<CheckerDescription> checkers = validationManager.getCheckersDescription();
            HelpPrinter.printCheckersList(checkers, printStream);
        }

        p = getParameter(parameters, PARAM_LIST_DEVICES);
        if (p != null)
        {
            hasListParameter = true;
            List<Value> devices = validationManager.getDevicesInfoList();
            HelpPrinter.printDevicesList(devices, printStream);
        }

        p = getParameter(parameters, PARAM_DESC_DEVICE);
        if (p != null)
        {
            hasListParameter = true;
            String deviceId = p.getValue();
            if (deviceId == null)
            {
                throw new ParameterParseException(PreflightingNLS.bind(
                        PreflightingNLS.CommandLineInputProcessor_IncorrectSyntax,
                        p.getParameterType()));
            }
            String deviceDescription = validationManager.getDeviceDescription(deviceId);
            HelpPrinter.printDevicesDescription(deviceDescription, deviceId, printStream);
        }

        return hasListParameter;
    }

    private static boolean checkHelpParameter(List<Parameter> parameters,
            ValidationManager validationManager, PrintStream printStream)
    {
        boolean hasHelpParam = false;
        Parameter p = getParameter(parameters, PARAM_HELP);
        if (p != null)
        {
            hasHelpParam = true;
            List<ParameterDescription> paramsDescr = null;
            if (p.getValue() != null)
            {
                CheckerExtension checkerExt = ValidationManager.getCheckerExtension(p.getValue());
                if (checkerExt == null)
                {
                    printStream
                            .print(PreflightingNLS.ApplicationParameterInterpreter_CheckerNotFound
                                    + p.getValue() + "\n\n"); //$NON-NLS-1$ 
                    paramsDescr = getAllParams(validationManager);
                    HelpPrinter.printHelp(printStream, paramsDescr, true);
                }
                else
                {
                    // informed CHECKERID, print the description from the
                    // extension point declaration and print the parameters
                    // returned by the checker itself.
                    paramsDescr = validationManager.getParametersDescription(p.getValue());
                    // HelpPrinter.printHelp(paramsDescr, false);
                    HelpPrinter.printHelpChecker(printStream, checkerExt);
                }
            }
            else
            {
                paramsDescr = getAllParams(validationManager);
                HelpPrinter.printHelp(printStream, paramsDescr, true);
            }
        }
        return hasHelpParam;
    }

    private static List<ParameterDescription> getAllParams(ValidationManager validationManager)
    {
        List<ParameterDescription> paramsDescr = new ArrayList<ParameterDescription>();
        if (validationManager.getParametersDescription(null) != null)
        {
            paramsDescr.addAll(validationManager.getParametersDescription(null));
        }
        if (OutputterFactory.getInstance().getParameterDescriptions() != null)
        {
            paramsDescr.addAll(OutputterFactory.getInstance().getParameterDescriptions());
        }

        // TODO Confirm if it can be removed and also remove ununsed methods
        /*
         * paramsDescr.add(createHelpDescription());
         * paramsDescr.add(createListDescription());
         */

        ParameterDescription listCheckersDesc = new ParameterDescription();
        listCheckersDesc.setDefaultValue(null);
        listCheckersDesc.setName(PARAM_LIST_CHECKERS);
        listCheckersDesc
                .setDescription(PreflightingNLS.ApplicationParameterInterpreter_ListAvailableCheckersMessage);
        paramsDescr.add(listCheckersDesc);

        ParameterDescription listDevDesc = new ParameterDescription();
        listDevDesc.setDefaultValue(null);
        listDevDesc.setName(PARAM_LIST_DEVICES);
        listDevDesc
                .setDescription(PreflightingNLS.ApplicationParameterInterpreter_ListAvailableDevicesMessage);
        paramsDescr.add(listDevDesc);

        ParameterDescription describeDevice = new ParameterDescription();
        describeDevice.setDefaultValue(null);
        describeDevice.setName(PARAM_DESC_DEVICE);
        describeDevice.setValueDescription("[DEV]");//$NON-NLS-1$
        describeDevice
                .setDescription(PreflightingNLS.ApplicationParameterInterpreter_DescribeDeviceMessage);
        paramsDescr.add(describeDevice);

        ParameterDescription helpDesc = new ParameterDescription();
        helpDesc.setName(PARAM_HELP);
        helpDesc.setDescription(PreflightingNLS.ApplicationParameterInterpreter_HelpMessage);
        helpDesc.setValueDescription("[CHK]");//$NON-NLS-1$
        helpDesc.setType(ParameterType.STRING);
        paramsDescr.add(helpDesc);

        ParameterDescription verbosityDesc = new ParameterDescription();
        verbosityDesc.setName(PARAM_VERBOSITY);
        verbosityDesc
                .setDescription(PreflightingNLS.ApplicationParameterInterpreter_ParameterVerbosityDescription);
        paramsDescr.add(verbosityDesc);

        ParameterDescription warningDesc = new ParameterDescription();
        warningDesc.setName(PARAMETER_WARNING);
        warningDesc
                .setDescription(PreflightingNLS.ApplicationParameterInterpreter_ParameterWarningDescription);
        paramsDescr.add(warningDesc);

        return paramsDescr;
    }

    public static Parameter getParameter(List<Parameter> parameters, String parameter)
    {
        Parameter param = null;
        Iterator<Parameter> it = parameters.iterator();
        while (it.hasNext() && (param == null))
        {
            Parameter p = it.next();
            if (p.getParameterType().compareTo(parameter) == 0)
            {
                param = p;
            }
        }
        return param;
    }
}
