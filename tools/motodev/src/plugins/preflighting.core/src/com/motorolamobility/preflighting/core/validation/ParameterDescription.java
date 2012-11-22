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
 * This class contains the description for a given parameter.
 */
public class ParameterDescription
{
    private String name = null;

    private String description = null;

    private String valueDescription = null;

    private boolean valueRequired = false;

    /**
     * The type of value held by the parameter
     */
    private ParameterType Type;

    /**
     * Default value
     */
    private Value defaultValue = null;

    /**
     * List of values allowed from this parameter
     */
    private List<Value> allowedValues = new ArrayList<Value>();

    /**
     * Gets the list of {@link Value} objects which are allowed.
     * 
     * @return Returns a {@link List} of values which are allowed as parameters.
     */
    public List<Value> getAllowedValues()
    {
        return allowedValues;
    }

    /**
     * Sets the {@link List} of {@link Value} objects which are allowed.
     * 
     * @param allowedValues {@link List} of allowed {@link Value} objects to be set.
     */
    public void setAllowedValues(List<Value> allowedValues)
    {
        this.allowedValues = allowedValues;
    }

    /**
     * Gets the description of a Parameter (e.g. used in help)
     * 
     * @return Return the description of a Parameter.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description of a Parameter.
     * 
     * @param description The description of a Parameter to be set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Gets the default value of a Parameter
     * 
     * @return Returns the default value of a Parameter.
     */
    public Value getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Sets the default value of a Parameter.
     * 
     * @param defaultValue The default value of a Parameter to be set.
     */
    public void setDefaultValue(Value defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the Parameter object name.
     * 
     * @return Returns the Parameter object name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the Parameter object´s name.
     * 
     * @param name Parameter´s name to be set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the {@link ParameterType}. This represents the several
     * object which a {@link ParameterDescription} can have.
     * 
     * @return Returns the {@link ParameterType} of this instance.
     */
    public ParameterType getType()
    {
        return Type;
    }

    /**
     * Set the {@link ParameterType} of this instance. This represents the several
     * object which a {@link ParameterDescription} can have.
     * 
     * @param type The {@link ParameterType} to be set.
     */
    public void setType(ParameterType type)
    {
        Type = type;
    }

    /**
     * Gets a description of the value being assigned to a parameter (used in help)
     * 
     * @return Returns the value description.
     */
    public String getValueDescription()
    {
        return valueDescription;
    }

    /**
     * Sets the description of the value being assigned to a parameter (used in help).
     * 
     * @param description The value description to be set.
     */
    public void setValueDescription(String description)
    {
        this.valueDescription = description;
    }

    /**
     * Returns <code>true</code> if this parameter is required, 
     * <code>false</code> otherwise.
     * 
     * @return <code>true</code> if required, <code>false</code> otherwise
     */
    public boolean isValueRequired()
    {
        return valueRequired;
    }

    /**
     * Sets if the value is required to run the checker or condition.
     * 
     * @param valueRequired Set <code>true</code> in case this {@link ParameterDescription}
     * is required, <code>false</code> otherwise.
     */
    public void setValueRequired(boolean valueRequired)
    {
        this.valueRequired = valueRequired;
    }

    /**
     * This implementation provides a human-readable text of this
     * {@link ParameterDescription}.
     * 
     * @return Returns a human-readable text of this {@link ParameterDescription}.
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "ParameterDescription [name=" + name + ", description=" + description //$NON-NLS-1$ //$NON-NLS-2$
                + ", defaulfValue=" + defaultValue + ", allowedValues=" + allowedValues + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
