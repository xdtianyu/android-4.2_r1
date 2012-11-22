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

package com.motorolamobility.preflighting.core.checker.parameter;

import com.motorolamobility.preflighting.core.validation.ParameterType;

/**
 * Bean class representing a Checker Parameter extension element.
 */
public class CheckerParameter implements ICheckerParameter
{
    private String id;

    private String name;

    private String value = null;

    private Boolean booleanValue = null;

    private Integer intValue = null;

    private String description;

    private String valueDescription;

    private boolean isMandatory;

    private ParameterType type;

    /**
     * This constructors populates the beam with all properties.
     * 
     * @param id Checker Parameter Id.
     * @param name Checker Parameter Name.
     * @param description Checker Parameter Description.
     * @param valueDescription Value-specific description.
     * @param type The type of this parameter.
     * @param isMandatory Flag which determines whether this Checker
     * Parameter is mandatory.
     */
    public CheckerParameter(String id, String name, String description, String valueDescription,
            ParameterType type, boolean isMandatory)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.valueDescription = valueDescription;
        this.type = type;
        this.isMandatory = isMandatory;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getId()
     */
    public String getId()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setId(java.lang.String)
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getName()
     */
    public String getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getValue()
     */
    public String getValue()
    {
        return value;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setValue(java.lang.String)
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setValue(boolean)
     */
    public void setBooleanValue(Boolean value)
    {
        this.booleanValue = value;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#isValueTrue()
     */
    public Boolean getBooleanValue()
    {
        return booleanValue;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getIntValue()
     */
    public Integer getIntValue()
    {
        return intValue;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setValue(int)
     */
    public void setIntValue(Integer value)
    {
        this.intValue = value;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getDescription()
     */
    public String getDescription()
    {
        return description;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setDescription(java.lang.String)
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getValueDescription()
     */
    public String getValueDescription()
    {
        return valueDescription;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setValueDescription(java.lang.String)
     */
    public void setValueDescription(String valueDescription)
    {
        this.valueDescription = valueDescription;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#isMandatory()
     */
    public boolean isMandatory()
    {
        return isMandatory;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setMandatory(boolean)
     */
    public void setMandatory(boolean isMandatory)
    {
        this.isMandatory = isMandatory;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#getType()
     */
    public ParameterType getType()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter#setType(java.lang.String)
     */
    public void setType(ParameterType type)
    {
        this.type = type;
    }
}
