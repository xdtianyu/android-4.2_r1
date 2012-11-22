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

package com.motorolamobility.preflighting.core.internal.checkerparameter;

import com.motorolamobility.preflighting.core.checker.IChecker;

/**
 * Bean class representing a Checker Parameter extension element.
 */
public final class CheckerParameterElement
{
    public static final String CHECKER_PARAMETER_ELEMENT_NAME = "parameter"; //$NON-NLS-1$

    public static final String CHECKER_PARAMETER_ATTRIBUTE_ID = "id"; //$NON-NLS-1$

    public static final String CHECKER_PARAMETER_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

    public static final String CHECKER_PARAMETER_ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$

    public static final String CHECKER_PARAMETER_ATTRIBUTE_VALUE_DESCRIPTION = "valueDescription"; //$NON-NLS-1$

    public static final String CHECKER_PARAMETER_ATTRIBUTE_IS_MANDATORY = "isMandatory"; //$NON-NLS-1$

    public static final String CHECKER_PARAMETER_ATTRIBUTE_TYPE = "type"; //$NON-NLS-1$

    private String id;

    private String name;

    private String value;

    private String description;

    private String valueDescription;

    private boolean isMandatory;

    private String type;

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
    public CheckerParameterElement(String id, String name, String description,
            String valueDescription, String type, boolean isMandatory)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.valueDescription = valueDescription;
        this.type = type;
        this.isMandatory = isMandatory;
    }

    /**
     * Gets the Checker Parameter Id.
     * 
     * @return Returns the Checker Parameter Id.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the Checker Parameter Id.
     * 
     * @param id The Checker Parameter Id to be set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Gets Checker Parameter Name.
     * 
     * @return Returns the Checker Parameter Name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the Checker Parameter Name.
     * 
     * @param name The Checker Parameter Name to be set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the value entered for this parameter. This value is
     * entered by the one who uses the {@link IChecker} which utilizes
     * this parameter.
     * 
     * @return Returns the value set by the {@link IChecker}큦 user.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value entered for this parameter. This value is
     * entered by the one who uses the {@link IChecker} which utilizes
     * this parameter.
     * 
     * @param value The value set by the {@link IChecker}큦 user to be set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Gets the Checker Parameter Description.
     * 
     * @return Returns the Checker Parameter Description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the Checker Parameter Description.
     * 
     * @param description The Checker Parameter to be set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Gets the value-specific description.
     * 
     * @return Returns the value-specific description.
     */
    public String getValueDescription()
    {
        return valueDescription;
    }

    /**
     * Sets the value-specific description.
     * 
     * @param valueDescription The value-specific description to set.
     */
    public void setValueDescription(String valueDescription)
    {
        this.valueDescription = valueDescription;
    }

    /**
     * Gets a flag which determines whether this Checker Parameter
     * is mandatory.
     * 
     * @return Returns a flag which determines whether this Checker Parameter
     * is mandatory.  
     */
    public boolean isMandatory()
    {
        return isMandatory;
    }

    /**
     * Sets a flag which determines whether this Checker Parameter
     * is mandatory.
     * 
     * @param isMandatory The flag which determines whether this Checker Parameter
     * is mandatory to be set.
     */
    public void setMandatory(boolean isMandatory)
    {
        this.isMandatory = isMandatory;
    }

    /**
     * Gets the Parameter큦 type. This type represents the variable type
     * of the parameter: String, Integer, Boolean and so on. They are
     * defined in the parameter큦 type extension-point.
     * 
     * @return Returns the type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the Parameter큦 type. This type represents the variable type
     * of the parameter: String, Integer, Boolean and so on. They are
     * defined in the parameter큦 type extension-point.
     * 
     * @param type The Parameter큦 type to set.
     */
    public void setType(String type)
    {
        this.type = type;
    }

}
