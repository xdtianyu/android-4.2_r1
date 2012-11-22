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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.NLS;

import com.motorolamobility.preflighting.core.validation.ComplexParameter;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.internal.commandinput.exception.ParameterParseException;

/**
 * Process command line commands below:
 */
public class CommandLineInputProcessor
{
    /**
     * White space character
     */
    private static final String WHITE_SPACE = " "; //$NON-NLS-1$

    /**
     * Simple parameter marker character
     */
    private static final String PARAMETER_MARKER = "-"; //$NON-NLS-1$

    /**
     * Simple parameter separator
     */
    private static final String COMPLEX_PARAMETER_SEPARATOR = "="; //$NON-NLS-1$

    /**
     * 
     * @param params
     * @return
     * @throws ParameterParseException
     */
    public List<Parameter> processCommandLine(String params) throws ParameterParseException
    {
        // Parse command line arguments and create parameters
        List<Parameter> commandLineParameters = new ArrayList<Parameter>();

        // if no parameters were passed, assume help will be printed
        if (params.length() == 0)
        {
            Parameter helpParameter =
                    new Parameter(ApplicationParameterInterpreter.PARAM_HELP, null);
            commandLineParameters.add(helpParameter);
        }
        else
        {
            // find the pattern " -", which is expected to separate each parameter
            String regex = "((?!(\\s+" + PARAMETER_MARKER + ")).)*"; //$NON-NLS-1$ //$NON-NLS-2$
            Pattern pat = Pattern.compile(regex);
            Matcher matcher = pat.matcher(params);
            Parameter commandLineparameter = null;

            // for each parameter part found, process it
            while (matcher.find())
            {
                String parameterValues = params.substring(matcher.start(), matcher.end());

                if (parameterValues.length() > 0)
                {
                    commandLineparameter = getCommandLineParameter(parameterValues);
                    commandLineParameters.add(commandLineparameter);
                }
            }
        }
        return commandLineParameters;
    }

    /**
     * Gets a parameter from command line. Command line arguments are passed to the 
     * application like this: -arg foo bar -c check1 name1=val1 name2=val2
     * 
     * The parameter for this example is:
     * name = arg
     * values = foo, bar
     * 
     * @param parameterValues String containing the parameter and its values. Eg: -arg foo bar
     * @return And instance of object Parameter
     * @throws ParameterParseException 
     */
    private Parameter getCommandLineParameter(String parameterValues)
            throws ParameterParseException
    {

        Parameter parameter = null;

        if (parameterValues.indexOf(COMPLEX_PARAMETER_SEPARATOR) == -1)
        {

            // Its a simple parameter, example: -param1 value1 or example: -param2
            parameter = getParameterFromCommandLine(parameterValues);

        }
        else
        {
            // Its a complex parameter, example: -c checker1 file=C:\arg.txt version=2
            parameter = getComplexParameterFromCommandLine(parameterValues);
        }

        return parameter;
    }

    /**
     * Get a complex parameter from command line. Example: -c checker1 param1=val1 param2=var2
     * @param values String[] containing the complex parameter. For example, for command line -c checker1 param1=val1 param2=var2
     * the contents of values are: [[c] [check1] [param1=val1] [param2=val2]]
     * @return Instance of Parameter, containing a ComplexParameter
     * @throws ParameterParseException 
     */
    private Parameter getComplexParameterFromCommandLine(String parameterValues)
            throws ParameterParseException
    {
        // complex parameter example: -c checker param1=val1 param2=val2
        ComplexParameter parameter = new ComplexParameter();

        String[] values = parameterValues.split(WHITE_SPACE);

        if (values.length < 3)
        {
            throw new ParameterParseException(NLS.bind(
                    PreflightingNLS.CommandLineInputProcessor_IncorrectSyntax, parameterValues));
        }
        else
        {

            String parameterType = values[0];

            if (parameterType.startsWith(PARAMETER_MARKER))
            {
                parameterType = parameterType.substring(1);
            }
            parameter.setParameterType(parameterType);
            parameter.setValue(values[1]);

            int lenghtToCut = values[0].length() + values[1].length() + 2;
            String subParameters = parameterValues.substring(lenghtToCut);

            String[] checkSyntax = subParameters.split("\\s*\\w+\\s*" + COMPLEX_PARAMETER_SEPARATOR //$NON-NLS-1$
                    + "\\s*\\w+\\s*"); //$NON-NLS-1$
            if (checkSyntax.length > 0)
            {
                throw new ParameterParseException(NLS.bind(
                        PreflightingNLS.CommandLineInputProcessor_IncorrectSyntax, parameterValues));
            }

            Pattern pat = Pattern.compile("\\w+\\s*" + COMPLEX_PARAMETER_SEPARATOR + "\\s*\\w+"); //$NON-NLS-1$ //$NON-NLS-2$
            Matcher matcher = pat.matcher(subParameters);

            while (matcher.find())
            {
                String part = subParameters.substring(matcher.start(), matcher.end());
                String[] pair = part.split(COMPLEX_PARAMETER_SEPARATOR);
                parameter.addParameter(pair[0].trim(), pair[1].trim());
            }
        }
        return parameter;
    }

    /**
     * Get a parameter from command line (example -param value), passed as a String[].
     * @param parameterValues parameter values
     * @return Instance of Parameter
     * @throws ParameterParseException 
     */
    private Parameter getParameterFromCommandLine(String parameterValues)
            throws ParameterParseException
    {
        Parameter parameter = new Parameter();

        // find first white space to separate tag from value
        int indexOfFirstWhiteSpace = parameterValues.indexOf(WHITE_SPACE);
        String parameterType = null;
        String parameterValue = null;

        // if there is no whitespace, everything is the parameter value necessary
        if (indexOfFirstWhiteSpace >= 0)
        {
            // substring original string from character 1 (skip "-"), to first
            // whitespace character
            parameterType = parameterValues.substring(1, indexOfFirstWhiteSpace);
            // retrieve the rest of the original string (starting from first whitespace)
            // as the value for the parameter
            parameterValue = parameterValues.substring(indexOfFirstWhiteSpace).trim();
        }
        else
        {
            // skip "-"
            parameterType = parameterValues.substring(1);
        }

        // check for unexpected format (both parts of the parameter empty)
        if (((parameterValue == null) || (parameterValue.length() == 0))
                && ((parameterType == null) || (parameterType.length() == 0)))
        {
            throw new ParameterParseException(NLS.bind(
                    PreflightingNLS.CommandLineInputProcessor_IncorrectSyntax, parameterValues));
        }

        // store parameter type and value
        parameter.setParameterType(parameterType);
        if ((parameterValue != null) && (parameterValue.length() > 0))
        {
            parameter.setValue(parameterValue);
        }

        return parameter;
    }
}
