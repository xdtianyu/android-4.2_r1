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
 *  Interface representing a checker parameter
 */
public interface ICheckerParameter
{

    /**
     * Gets the Checker Parameter Id.
     * 
     * @return Returns the Checker Parameter Id.
     */
    public String getId();

    /**
     * Sets the checker parameter's ID.
     * 
     * @param id The Checker Parameter Id to be set.
     */
    public void setId(String id);

    /**
     * Gets Checker Parameter Name.
     * 
     * @return Returns the Checker Parameter Name.
     */
    public String getName();

    /**
     * Sets the checker parameter's name.
     * 
     * @param name The Checker Parameter Name to be set.
     */
    public void setName(String name);

    /**
     * Gets the value entered for this parameter. This value is
     * entered by the user on the command line.
     * If not set, default value is null.
     * 
     * @return Returns the value set by the user.
     */
    public String getValue();

    /**
     * Gets the boolean value entered for this parameter if its type is boolean. This value is
     * entered by the user on the command line.
     * 
     * If not set, default value is null.
     * 
     * @return Returns the value, as boolean, set by the user.
     */
    public Boolean getBooleanValue();

    /**
     * Gets the int value entered for this parameter if its type is integer. This value is
     * entered by the user on the command line.
     * 
     * If not set, default value is null.
     * 
     * @return Returns the value, as int, set by the user.
     */
    public Integer getIntValue();

    /**
     * Sets the parameter value. This value is
     * entered by the user on the command line.
     * 
     * @param value The value set by set by the user.
     */
    public void setValue(String value);

    /**
     * Sets the parameter value. This value is
     * entered by the user on the command line.
     * 
     * @param value The value set by the user.
     */
    public void setIntValue(Integer intValue);

    /**
     * Sets the parameter value. This value is
     * entered by the user on the command line.
     * 
     * @param value The value set by the user.
     */
    public void setBooleanValue(Boolean booleanValue);

    /**
     * Gets the Checker Parameter Description.
     * 
     * @return Returns the Checker Parameter Description.
     */
    public String getDescription();

    /**
     * Sets the checker parameter's description.
     * 
     * @param description The Checker Parameter to be set.
     */
    public void setDescription(String description);

    /**
     * Gets the value-specific description.
     * 
     * @return Returns the value-specific description.
     */
    public String getValueDescription();

    /**
     * Sets the value-specific description.
     * 
     * @param valueDescription The value-specific description to set.
     */
    public void setValueDescription(String valueDescription);

    /**
     * Gets a flag which determines whether this Checker Parameter
     * is mandatory.
     * 
     * @return Returns a flag which determines whether this Checker Parameter
     * is mandatory.  
     */
    public boolean isMandatory();

    /**
     * Specifies whether this checker parameter is mandatory.
     * 
     * @param isMandatory The flag which determines whether this Checker Parameter
     * is mandatory to be set.
     */
    public void setMandatory(boolean isMandatory);

    /**
     * Gets the Parameter큦 type. This type represents the variable type
     * of the parameter: String, Integer, Boolean and so on. They are
     * defined in the parameter큦 type extension-point.
     * 
     * @return Returns the type.
     */
    public ParameterType getType();

    /**
     * Sets the parameter's type. This type represents the variable type
     * of the parameter: String, Integer, Boolean and so on. They are
     * defined in the parameter큦 type extension-point.
     * 
     * @param type The Parameter큦 type to set.
     */
    public void setType(ParameterType type);
}