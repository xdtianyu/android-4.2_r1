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
 * Represents a variable declared in the code being verified.
 */
public class Variable
{

    protected boolean isStatic = false;

    protected boolean isFinal = false;

    protected String visibility;

    protected String type;

    protected String name;

    protected String value;

    protected int lineNumber;

    /**
     * Returns the type of the variable.
     * @return Type of the variable.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the type of the variable.
     * 
     * @param type the type of the variable.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /** 
     * Returns the name of the variable.
     * @return Name of the variable.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the variable.
     * 
     * @param name the name of the variable.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns a <code>boolean</code> stating whether this variable is static or not. 
     * @return <code>true</code> if variable is static, <code>false</code> if non-static (instance variable).
     */
    public boolean isStatic()
    {
        return isStatic;
    }

    /**
     * Returns a <code>boolean</code> stating whether this variable is final or not. 
     * @return <code>true</code> if variable is final, <code>false</code> if it is not.
     */
    public boolean isFinal()
    {
        return isFinal;
    }

    /**
     * Returns the visibility of the variable.
     * @return The visibility (public, protected, package, private) of the variable.
     */
    public String getVisibility()
    {
        return visibility;
    }

    /**
     * Set the visibility (public, protected, package, private) of the variable.
     * 
     * @param visibility
     */
    public void setVisibility(String visibility)
    {
        this.visibility = visibility;
    }

    /**
     * Set if the variable is static.
     *  
     * @param isStatic <code>true</code> if variable is static, <code>false</code> if non-static (instance variable).
     */
    public void setStatic(boolean isStatic)
    {
        this.isStatic = isStatic;
    }

    /**
     * Set if the variable is final.
     *  
     * @param isFinal <code>true</code> if variable is final, <code>false</code> if not.
     */
    public void setFinal(boolean isFinal)
    {
        this.isFinal = isFinal;
    }

    /**
     * Returns a <code> String </code> representing the value of the variable.
     * @return The value assigned to the variable.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set a value to the variable.
     * 
     * @param value
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Return the line number where this variable appears in the code.
     * @return The line of the variable if possible, 0 if not found.
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * Set the line of the variable.
     * 
     * @param lineNumber
     */
    public void setLineNumber(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString()
    {
        return "Variable [isStatic=" + isStatic + ", isFinal=" + isFinal + ", visibility="
                + visibility + ", type=" + type + ", name=" + name + ", value=" + value + "]";
    }

}
