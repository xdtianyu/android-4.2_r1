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
package com.motorolamobility.preflighting.core.source.model;

/**
 * Represents a constant being accessed in the application data model. 
 */
public class Constant extends Instruction
{

    private String type; //string, class

    private String value;

    /**
     * Retrieve the constant type.
     * 
     * @return Constant type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Set the type of the constant (e.g.: string, class).
     *  
     * @param type Constant type.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /** 
     * Retrieve the constant value.
     * 
     * @return The value of the constant. 
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set the value of the constant.
     * 
     * @param value Constant value.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return "Constant [type=" + type + ", value=" + value + "]";
    }
}
