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
package com.motorolamobility.preflighting.internal.commandoutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.motorolamobility.preflighting.core.IParameterProcessor;
import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ParameterDescription;
import com.motorolamobility.preflighting.core.validation.ParameterType;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;
import com.motorolamobility.preflighting.core.validation.Value;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.output.AbstractOutputter;

/**
 * Factory class for pre-flighting outputters
 */
public class OutputterFactory implements IParameterProcessor
{

    public static enum OutputType
    {
        CSV, TEXT, XML, DAEMON;

        @Override
        public String toString()
        {
            return super.toString().toLowerCase();
        };

    }

    /**
    * OUTPUT parameter keyword.
    */
    public final static String OUTPUT_PARAMETER = "output"; //$NON-NLS-1$

    /**
     * Maps parameters to possible values
     */
    private final Map<String, List<OutputType>> parameterValueMap =
            new HashMap<String, List<OutputType>>();

    /**
     * Maps parameters to default values
     */
    private final Map<String, Value> parameterDefaultMap = new HashMap<String, Value>();

    /**
     * Maps parameters to their descriptions
     */
    private final Map<String, String> parameterDescriptionMap = new HashMap<String, String>();

    /**
     * Maps possible values to their descriptions
     */
    private final Map<String, String> valueDescriptionMap = new HashMap<String, String>();

    /**
     * Maps possible values to their types
     */
    private final Map<String, ParameterType> parameterTypeMap =
            new HashMap<String, ParameterType>();

    private static OutputterFactory instance;

    private static OutputType defaultOutputter = OutputType.TEXT;

    /**
     * Default constructor.
     */
    private OutputterFactory()
    {
        // Initialize maps

        // List of values
        List<OutputType> valuesList = new ArrayList<OutputType>();
        valuesList.add(OutputType.XML); // XML support not present in this version
        valuesList.add(OutputType.CSV);
        valuesList.add(OutputType.TEXT);

        parameterValueMap.put(OUTPUT_PARAMETER, valuesList);

        Value defaultOutputValue = new Value();
        defaultOutputValue.setDescription(PreflightingNLS.OutputterFactory_TextOutputFormatMessage);
        defaultOutputValue.setValue(OutputType.TEXT.toString());
        parameterDefaultMap.put(OUTPUT_PARAMETER, defaultOutputValue);

        parameterDescriptionMap.put(OUTPUT_PARAMETER,
                PreflightingNLS.OutputterFactory_ValidationOutputModeMessage);

        valueDescriptionMap.put(OutputType.TEXT.toString(),
                PreflightingNLS.OutputterFactory_TextOutputFormatMessage);
        valueDescriptionMap.put(OutputType.CSV.toString(),
                PreflightingNLS.OutputterFactory_XmlOutputFormatNotImplementedMessage);
        valueDescriptionMap.put(OutputType.XML.toString(),
                PreflightingNLS.OutputterFactory_XmlOutputFormatNotImplementedMessage);

        parameterTypeMap.put(OUTPUT_PARAMETER, ParameterType.STRING);
    }

    public static OutputterFactory getInstance()
    {
        if (instance == null)
        {
            instance = new OutputterFactory();
        }
        return instance;
    }

    /**
     * Gets parameters accepted by the outputter created by the factory
     * @return
     */
    public List<ParameterDescription> getParameterDescriptions()
    {
        //TODO Since there is no implemented alternative to TEXT, disable disable parameter
        /*
        List<ParameterDescription> parameterList = new ArrayList<ParameterDescription>();

        for (String p : parameterValueMap.keySet())
        {
            ParameterDescription paramDesc = new ParameterDescription();

            // Set allowed values
            List<Value> valueList = new ArrayList<Value>();
            for (String v : parameterValueMap.get(p))
            {
                Value value = new Value();

                value.setDescription(valueDescriptionMap.get(v));
                value.setValue(v);

                valueList.add(value);
            }
            paramDesc.setType(parameterTypeMap.get(p));
            paramDesc.setAllowedValues(valueList);

            // Set default value
            paramDesc.setDefaultValue(parameterDefaultMap.get(p));

            // Set parameter description
            paramDesc.setDescription(parameterDescriptionMap.get(p));

            // Set parameter name
            paramDesc.setName(p);

            parameterList.add(paramDesc);

        }
        */

        return null;

    }

    /**
     * Create a new outputter passing the application parameter list as parameter
     * @param parameters the application parameters or null to create the default one
     * @return the desired outputter
     */
    public AbstractOutputter createOutputter(List<Parameter> parameters)
    {
        AbstractOutputter outputter = null;
        if (parameters != null)
        {
            for (Parameter param : parameters)
            {
                String parameterType = param.getParameterType();

                if (parameterType.equals(InputParameter.OUTPUT.getAlias()))
                {
                    if (param.getValue() != null)
                    {
                        try
                        {
                            outputter = internalCreateOutputter(param.getValue());
                        }
                        catch (IllegalArgumentException e)
                        {
                            //do nothing
                        }
                    }
                }
            }
        }
        //default outputter
        if (outputter == null)
        {
            outputter = internalCreateOutputter(defaultOutputter.toString());
        }

        return outputter;
    }

    private AbstractOutputter internalCreateOutputter(String type)
    {
        AbstractOutputter outputter = null;
        String typeUpper = type.toUpperCase();
        Map<String, AbstractOutputter> outputters = OutputterExtensionReader.getOutputtersMap();
        outputter = outputters.get(typeUpper);

        // the outputter can be defined through extension point
        if (outputter == null)
        {

            OutputType typeEnum = OutputType.valueOf(typeUpper);

            switch (typeEnum)
            {
                case CSV:
                    outputter = new CSVOutputter();
                    break;

                case XML:
                    outputter = new XmlOutputter();
                    break;

                case DAEMON:
                    outputter = new DaemonXMLOutputter();
                    break;

                case TEXT:
                    outputter = new TextOutputter();
                    break;
                default:

                    break;
            }
        }

        return outputter;
    }

    public IStatus isOutputterAvailable(String value)
    {
        IStatus validationResult = null;

        if (value != null)
        {
            value = value.toUpperCase();
        }

        // if this output is not defined through extension point, see if it is internal
        if (!OutputterExtensionReader.getOutputtersMap().containsKey(value))
        {
            try
            {
                OutputType.valueOf(value);
            }
            catch (IllegalArgumentException e)
            {
                validationResult =
                        new Status(IStatus.ERROR, PreflightingCorePlugin.PLUGIN_ID,
                                PreflightingNLS.OutputterFactory_OutputParametersInvalidMessage);
            }
        }

        // If no problems were found, return a OK status
        if (validationResult == null)
        {
            validationResult =
                    new Status(IStatus.OK, PreflightingCorePlugin.PLUGIN_ID,
                            PreflightingNLS.OutputterFactory_OutputParametersValidMessage);
        }

        return validationResult;
    }

    public IStatus validateInputParams(List<Parameter> parameters)
    {
        return Status.OK_STATUS;
    }

    public OutputType getDefaultOutputter()
    {
        return defaultOutputter;
    }

    public void setDefaultOutputter(OutputType outputter)
    {
        defaultOutputter = outputter;
    }
}
