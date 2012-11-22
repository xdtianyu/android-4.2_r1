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
package com.motorolamobility.preflighting.internal;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.motorolamobility.preflighting.core.exception.PreflightingParameterException;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.internal.commandinput.ApplicationParameterInterpreter;
import com.motorolamobility.preflighting.internal.commandinput.CommandLineInputProcessor;
import com.motorolamobility.preflighting.internal.commandinput.exception.ParameterParseException;
import com.motorolamobility.preflighting.internal.commandoutput.OutputterFactory;
import com.motorolamobility.preflighting.output.AbstractOutputter;

/**
 * This class controls all aspects of the application's execution
 */
public class PreflightingApplication implements IApplication
{

    /* (non-Javadoc)
     * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
     */
    public Object start(IApplicationContext context)
    {
        String commandLine = getCommandLineParameters(context.getArguments());
        Integer exitCode = validate(commandLine, System.out, System.err);
        return exitCode;
    }

    public static Integer validate(String commandLine, PrintStream out, PrintStream err)
    {
        Integer exitCode = IApplication.EXIT_OK;

        CommandLineInputProcessor commandLineInputProcessor = new CommandLineInputProcessor();

        AbstractOutputter outputter = null;
        DebugVerboseOutputter.setStream(err);

        // Get the parameter list
        List<Parameter> parameterList;

        try
        {
            parameterList = commandLineInputProcessor.processCommandLine(commandLine);
            ValidationManager validationManager = new ValidationManager();
            outputter = OutputterFactory.getInstance().createOutputter(parameterList);

            List<Parameter> parametersCopy = new ArrayList<Parameter>(parameterList);

            for (Parameter param : parametersCopy)
            {
                if (InputParameter.OUTPUT.getAlias().equals(param.getParameterType()))
                {
                    ApplicationParameterInterpreter.validateOutputParam(param.getValue());
                    outputter = OutputterFactory.getInstance().createOutputter(parameterList);
                    parameterList.remove(param);
                    break;
                }
            }

            if (!ApplicationParameterInterpreter.checkApplicationParameters(parameterList,
                    validationManager, out))
            {
                try
                {
                    //path of application or project or apk
                    Parameter pathParam = null;
                    for (Parameter param : parameterList)
                    {
                        if (param.getParameterType().equals(
                                InputParameter.APPLICATION_PATH.getAlias()))
                        {
                            pathParam = param;
                        }
                    }

                    DebugVerboseOutputter
                            .printVerboseMessage(
                                    PreflightingNLS.PreflightingApplication_VerboseMessage_StartingProcessMessage,
                                    VerboseLevel.v2);
                    List<ApplicationValidationResult> validationResults =
                            validationManager.run(parameterList);

                    if (validationResults.size() > 0)
                    {
                        DebugVerboseOutputter
                                .printVerboseMessage(
                                        PreflightingNLS.PreflightingApplication_VerboseMessage_ForwardingResultsMessage,
                                        VerboseLevel.v2);
                        // print empty line to separate
                        DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v2);

                        exitCode = getExitCode(exitCode, validationResults);
                    }

                    // Print validation results
                    for (ApplicationValidationResult result : validationResults)
                    {
                        parameterList.remove(pathParam);
                        Parameter appParam =
                                new Parameter(
                                        ValidationManager.InputParameter.APPLICATION_PATH
                                                .getAlias(),
                                        result.getApplicationPath());
                        parameterList.add(appParam);
                        outputter.print(result, out, parameterList);
                    }
                }
                catch (PreflightingParameterException pe)
                {
                    // parameter problem message should already be printed; only set exit
                    // code (do not show message because it is not necessary)
                    exitCode = new Integer(1);

                    if (outputter != null)
                    {
                        outputter.printError(pe, out);
                    }
                }
            }
        }
        catch (ParameterParseException e)
        {
            // Command line parameter problem. Show message.
            DebugVerboseOutputter.printVerboseMessage(e.getMessage(), VerboseLevel.v0);
            exitCode = new Integer(1);
            outputter = OutputterFactory.getInstance().createOutputter(null);
            outputter.printError(e, out);

        }
        catch (PreflightingToolException e)
        {
            exitCode = new Integer(1);
            DebugVerboseOutputter.printVerboseMessage(e.getMessage(), VerboseLevel.v0);
            outputter = OutputterFactory.getInstance().createOutputter(null);
            outputter.printError(e, out);
        }
        return exitCode;
    }

    /**
     * Verifies if any checker returned an error or a fatal error, and then return the
     * required exit code for a given situation.
     * @param exitCode
     * @param result
     * @return
     */
    private static Integer getExitCode(Integer exitCode, List<ApplicationValidationResult> results)
    {
        for (ApplicationValidationResult appValidationResult : results)
        {
            List<ValidationResult> result = appValidationResult.getResults();
            Iterator<ValidationResult> it = result.iterator();
            while (it.hasNext() && (exitCode < 1))
            {
                ValidationResult validationResult = it.next();
                List<ValidationResultData> resultData = validationResult.getValidationResult();
                Iterator<ValidationResultData> itData = resultData.iterator();
                while (itData.hasNext() && (exitCode < 1))
                {
                    ValidationResultData data = itData.next();
                    if (data.getSeverity() == ValidationResultData.SEVERITY.ERROR)
                    {
                        exitCode = new Integer(2);
                        return exitCode;
                    }
                }
            }
        }
        return exitCode;
    }

    /**
     * Gets parameters from  parameter map provided from IApplicationContext.
     * <br>
     * Application context parameters are placed in the map using String keys and String[] values
     * 
     * @param appContextParameters Map<String, String[]> containing parameters
     * @return String containing all parameters passed to the application
     */
    @SuppressWarnings("rawtypes")
    private String getCommandLineParameters(Map appContextParameters)
    {

        StringBuffer commandLine = new StringBuffer();

        Iterator iterator = appContextParameters.values().iterator();

        while (iterator.hasNext())
        {
            String[] args = (String[]) iterator.next();

            for (int i = 0; i < args.length; i++)
            {
                commandLine.append(args[i] + " "); //$NON-NLS-1$     
            }
        }

        return commandLine.toString();
    }

    /* (non-Javadoc)
     * @see org.eclipse.equinox.app.IApplication#stop()
     */
    public void stop()
    {
        // nothing to do
    }
}
