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
package com.motorolamobility.preflighting.core.internal.conditions;

import com.motorolamobility.preflighting.core.checker.condition.ICondition;

/**
 * Bean class representing a Condition extension element.
 */
public final class ConditionElement
{
    public static final String CHECKER_CONDITION_ELEMENT_NAME = "condition"; //$NON-NLS-1$

    public static final String CHECKER_CONDITION_ATTRIBUTE_ID = "id"; //$NON-NLS-1$

    public static final String CHECKER_CONDITION_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

    public static final String CHECKER_CONDITION_ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$

    public static final String CHECKER_CONDITION_ATTRIBUTE_DEFAULT_SEVERITY =
            "defaultSeverityLevel"; //$NON-NLS-1$

    public static final String CHECKER_CONDITION_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

    public static final String CHECKER_CONDITION_ATTRIBUTE_MARKER_TYPE = "markerType"; //$NON-NLS-1$

    private final String id;

    private final String name;

    private final String description;

    private final String defaultSeverityLevel;

    private final ICondition condition;

    /**
     * Creates a new Condition Element
     * @param id
     * @param name
     * @param description
     * @param defaultSeverityLevel 
     * @param checker
     * @param condition
     */
    public ConditionElement(String id, String name, String description,
            String defaultSeverityLevel, ICondition condition)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultSeverityLevel = defaultSeverityLevel;
        this.condition = condition;
    }

    /**
     * @return the id
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return the defaultSeverityLevel
     */
    public String getDefaultSeverityLevel()
    {
        return defaultSeverityLevel;
    }

    /**
     * @return the condition
     */
    public ICondition getChecker()
    {
        return condition;
    }
}