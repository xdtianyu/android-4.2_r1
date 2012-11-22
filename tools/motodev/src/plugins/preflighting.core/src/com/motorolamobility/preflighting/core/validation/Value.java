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
 * Encapsulates 2 strings: one for the value, and another for a description.
 */
public class Value
{
    private String value;

    private String description = null;

    /**
     * Returns the value
     * @return The value.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value 
     * @param value The value to set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Retrieve the description of the value.
     * 
     * @return The description of the value.
     */
    public String getDescription()
    {
        return description;
    }

    /** 
     * Set the description of the value.
     * 
     * @param description The description to set on the value.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
}
