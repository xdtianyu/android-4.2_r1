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

import java.util.ArrayList;
import java.util.List;

/**
 * A complex parameter is a parameter that is composed of a list of {@link Parameter}. 
 */
public class ComplexParameter extends Parameter
{

    private List<Parameter> parameters;

    @Override
    public String toString()
    {
        String retValue =
                "Complex Parameter\n name: " + getParameterType() + "\n value: " + getValue(); //$NON-NLS-1$ //$NON-NLS-2$

        for (Parameter param : parameters)
        {
            retValue += "\n " + param.toString(); //$NON-NLS-1$
        }

        return retValue;
    }

    /**
     * Constructs a new ComplexParameter with the given parameters.
     * 
     * @param parameterType Parameter type.
     * @param value Parameter value.
     */
    public ComplexParameter(String parameterType, String value)
    {
        super(parameterType, value);
    }

    /**
     * Default constructor.
     * 
     */
    public ComplexParameter()
    {
    }

    /**
     * Adds a new parameter to the complex parameter.
     * 
     * @param key Name of the parameter attribute.
     * @param value Value for the parameter attribute. 
     */
    public void addParameter(String key, String value)
    {
        if (parameters == null)
        {
            parameters = new ArrayList<Parameter>();
        }
        parameters.add(new Parameter(key, value));
    }

    /**
     * Gets all {@link Parameter} that compose {@link ComplexParameter}.
     * 
     * @return The list of parameters.
     */
    public List<Parameter> getParameters()
    {
        return parameters;
    }
}
