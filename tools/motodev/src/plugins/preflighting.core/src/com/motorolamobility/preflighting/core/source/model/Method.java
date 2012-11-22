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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a method declaration for a given class.
 */
public class Method
{

    /**
     * Constant for direct methods.
     */
    public static final String DIRECT = "direct";

    /**
     * Constant for virtual methods.
     */
    public static final String VIRTUAL = "virtual";

    private boolean isStatic = false;

    private boolean isConstructor = false;

    private String visibility;

    private String returnType;

    private String methodName;

    private int lineNumber;

    private final List<String> parameterTypes = new ArrayList<String>();

    private final List<Instruction> instructions = new ArrayList<Instruction>();

    private final List<Variable> variables = new ArrayList<Variable>();

    /**
     * Returns the visibility of this method.
     * @return visibility of this method.
     */
    public String getVisibility()
    {
        return visibility;
    }

    /**
     * Sets the visibility of this method. 
     * @param visibility the visibility of this method.
     */
    public void setVisibility(String visibility)
    {
        this.visibility = visibility;
    }

    /**
     * Returns the return type of this method.
     * @return return type of this method.
     */
    public String getReturnType()
    {
        return returnType;
    }

    /**
     * Sets the return type of this method.
     * @param returnType the return type of this method.
     */
    public void setReturnType(String returnType)
    {
        this.returnType = returnType;
    }

    /** 
     * Returns true if this method is static, false otherwise.
     * @return <code>true</code> if static, <code>false</code> otherwise.
     */
    public boolean isStatic()
    {
        return isStatic;
    }

    /**
     * Returns true if this method is a constructor, false otherwise.
     * @return <code>true</code> if constructor, <code>false</code> otherwise. 
     */
    public boolean isConstructor()
    {
        return isConstructor;
    }

    /**
     * Returns the list of parameters types in the method declaration.
     * @return list of parameters types in the method declaration.
     */
    public List<String> getParameterTypes()
    {
        return parameterTypes;
    }

    /**
     * Returns the list of {@link Instruction} objects that are part of the method declaration.
     * @return the list of {@link Instruction} objects that are part of the method declaration.
     */
    public List<Instruction> getInstructions()
    {
        return instructions;
    }

    /**
     * Set true if the method is static.
     * @param isStatic the boolean that indicates whether this method is static.
     */
    public void setStatic(boolean isStatic)
    {
        this.isStatic = isStatic;
    }

    /**
     * Set true if the method is a constructor.
     * @param isConstructor the boolean that indicates whether this method is a constructor.
     */
    public void setConstructor(boolean isConstructor)
    {
        this.isConstructor = isConstructor;
    }

    /**
     * Returns the method name.
     * @return the method name.
     */
    public String getMethodName()
    {
        return methodName;
    }

    /**
     * Sets the method name.
     * @param methodName the method name.
     */
    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    /**
     * Returns the line number.
     * @return the line number (the first line of this method).
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * Sets the line number of the declared method (the first line).
     * @param lineNumber the line number of the declared method (the first line).
     */
    public void setLineNumber(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the list of {@link Variable} objects.
     * @return the list of {@link Variable} objects.
     */
    public List<Variable> getVariables()
    {
        return variables;
    }

    /**
     * Adds a new {@link Variable} declared inside of the method declaration.
     * @param variable the variable to be added.
     */
    public void addVariable(Variable variable)
    {
        this.variables.add(variable);
    }

    @Override
    public String toString()
    {
        return "Method [isStatic=" + isStatic + ", isConstructor=" + isConstructor
                + ", visibility=" + visibility + ", returnType=" + returnType + ", methodName="
                + methodName + ", parameterTypes=" + parameterTypes + ", instructions="
                + instructions + "]";
    }
}
