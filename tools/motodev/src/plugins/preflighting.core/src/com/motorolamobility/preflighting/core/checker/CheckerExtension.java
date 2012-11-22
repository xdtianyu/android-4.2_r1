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
package com.motorolamobility.preflighting.core.checker;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.internal.checkerparameter.CheckerParameterElement;
import com.motorolamobility.preflighting.core.internal.conditions.ConditionElement;

/**
 * Bean class representing a checker extension.
 */
public final class CheckerExtension
{

    public static final String CHECKER_EXTENSION_POINT_ID = PreflightingCorePlugin.PLUGIN_ID
            + ".checker"; //$NON-NLS-1$

    public static final String CHECKER_EXTENSION_POINT_ELEMENT_CHECKER = "checker"; //$NON-NLS-1$

    public static final String CHECKER_EXTENSION_POINT_ATTRIBUTE_ID = "id"; //$NON-NLS-1$

    public static final String CHECKER_EXTENSION_POINT_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

    public static final String CHECKER_EXTENSION_POINT_ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$

    public static final String CHECKER_EXTENSION_POINT_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

    private final String id;

    private final String name;

    private final String description;

    private final IChecker checker;

    private ConditionElement[] conditions;

    private CheckerParameterElement[] conditionParameters;

    /**
     * Creates a new checker extension object.
     * 
     * @param id Checker id.
     * @param name Checker name.
     * @param description Checker description.
     * @param checker Checker object.
     */
    public CheckerExtension(String id, String name, String description, IChecker checker)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.checker = checker;
    }

    /**
     * @return the checker id.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the checker name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the checker description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return the checker object.
     */
    public IChecker getChecker()
    {
        return checker;
    }

    /**
     * @return the checker conditions. If the checker has no conditions declared an empty array is returned.
     */
    public ConditionElement[] getConditions()
    {
        return conditions != null ? conditions : new ConditionElement[0];
    }

    /**
     * @param conditions Checker conditions to set.
     */
    public void setConditions(ConditionElement[] conditions)
    {
        this.conditions = conditions;
    }

    /**
     * Gets the list of Parameters for this Condition.
     * 
     * @return Returns the list of Parameters for this Condition.
     */
    public CheckerParameterElement[] getConditionParameters()
    {
        return this.conditionParameters;
    }

    /**
     * Sets the list of Parameters for this Condition.
     * 
     * @param conditionParameters This list of Parameters for this Condition.
     */
    public void setConditionParameters(CheckerParameterElement[] conditionParameters)
    {
        this.conditionParameters = conditionParameters;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "CheckerExtension [id=" + id + ", name=" + name + ", description=" + description
                + ", checker=" + checker + "]";
    }
}
