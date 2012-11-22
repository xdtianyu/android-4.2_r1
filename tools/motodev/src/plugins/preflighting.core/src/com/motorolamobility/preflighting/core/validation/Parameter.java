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

/**
 * Represents a parameter that can be received by App Validator. It can be either a global parameter or a specific parameter from a checker.
 */
public class Parameter
{
    private String parameterType;

    private String value;

    /**
     * Print a human-readable Text of this {@link Parameter}.
     * 
     * @return Returns a human-readable Text.
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "Parameter\n name: " + parameterType + "\n value: " + value; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Constructs a {@link Parameter} that can be passed to App Validator
     *  
     * @param parameterType Indicates of of the types of the instantiated {@link Parameter}.  
     * @param value  The value to be set into the {@link Parameter}.
     */
    public Parameter(String parameterType, String value)
    {
        this.parameterType = parameterType;
        this.value = value;
    }

    /**
     * Constructs a {@link Parameter} with no values.
     */
    public Parameter()
    {
    }

    /**
     * Gets the parameter type. It holds the following values:
     * <ui>
     *  <li>{@link ValidationManager#DEVICE_PARAMETER}</li>
     *  <li>{@link ValidationManager#CHECKER_PARAMETER}</li><li></li>
     *  <li>{@link ValidationManager#DISABLE_CHECKER_PARAMETER}</li><li></li>
     * </ui>
     * 
     * @return Returns the parameter type.
     */
    public String getParameterType()
    {
        return parameterType;
    }

    /**
     * Gets the value of the parameter
     * 
     * @return
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets parameter type. It holds the following values:
     * <ui>
     *  <li>{@link ValidationManager#DEVICE_PARAMETER}</li>
     *  <li>{@link ValidationManager#CHECKER_PARAMETER}</li>
     *  <li>{@link ValidationManager#DISABLE_CHECKER_PARAMETER}</li><li></li>
     * </ui>
     * 
     * @param parameterType The type of the parameter.
     */
    public void setParameterType(String parameterType)
    {
        this.parameterType = parameterType;
    }

    /**
     * Sets parameter value. This represents the content of the {@link Parameter}.
     * 
     * @param value Parameter value to be set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Verifies if a given object is a {@link Parameter} and whether
     * it is equal to this instance. In case two {@link Parameter} objects
     * have the same {@link Parameter#getParameterType()}, they are considered
     * equal.
     * 
     * @param obj Object to be compared. Note that it must be an instance
     * of {@link Parameter}.
     */
    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;
        if (obj instanceof Parameter)
        {
            equals = true;
            Parameter objParam = (Parameter) obj;
            String objParamType = objParam.getParameterType();

            if (objParamType == null)
            {
                equals = (this.parameterType == null);
            }

            if (equals && (objParamType != null))
            {
                equals = objParamType.equals(this.parameterType);
            }
        }
        return equals;
    }
}
