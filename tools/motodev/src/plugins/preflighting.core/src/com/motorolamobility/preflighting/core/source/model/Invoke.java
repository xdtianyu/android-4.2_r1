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
 * Represents a method (direct or virtual) being invoked.
 */
public class Invoke extends Instruction
{

    private String type; //direct or virtual

    private String classCalled;

    private String methodName;

    private String objectName;

    private String assignedVariable;

    private List<String> parameterTypes = new ArrayList<String>();

    private List<String> parameterNames = new ArrayList<String>();

    private String returnType;

    /**
     * Returns the type of the Invoke (direct or virtual).
     * @return the type of the Invoke.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the type of the Invoke (direct or virtual).
     * @param type the Invoke type.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Returns the name of the class that owns the method being invoked.
     * @return name of the class that owns the method being invoked.
     */
    public String getClassCalled()
    {
        return classCalled;
    }

    /**
     * Sets the class that owns the method being invoked.
     * @param classCalled the class that owns the method being invoked.
     */
    public void setClassCalled(String classCalled)
    {
        this.classCalled = classCalled;
    }

    /**
     * Returns the name of the method being invoked.
     * @return name of the method being invoked.
     */
    public String getMethodName()
    {
        return methodName;
    }

    /**
     * Sets the name of the method being invoked.
     * @param methodName the name of the method being invoked.
     */
    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    /**
     * Returns the name of the object reference name being invoked.
     * @return name of the object reference name being invoked.
     */
    public String getObjectName()
    {
        return objectName;
    }

    /**
     * Sets object reference name being invoked.
     * @param objectName the object name.
     */
    public void setObjectName(String objectName)
    {
        this.objectName = objectName;
    }

    /**
     * Returns the name of the variable being assigned in the return of the Invoke.
     * @return name of the variable being assigned in the return of the Invoke.
     */
    public String getAssignedVariable()
    {
        return assignedVariable;
    }

    /**
     * Sets the name of the variable being assigned in the return of the Invoke.
     * @param assignedVariable the name of the variable being assigned in the return of the Invoke.
     */
    public void setAssignedVariable(String assignedVariable)
    {
        this.assignedVariable = assignedVariable;
    }

    /**
     * Returns the return type of the method invocation.
     * @return the return type of the method invocation.
     */
    public String getReturnType()
    {
        return returnType;
    }

    /**
     * Sets the return type of the method invocation.
     * @param returnType the return type of the method invocation.
     */
    public void setReturnType(String returnType)
    {
        this.returnType = returnType;
    }

    /**
     * Returns the list of parameters types that the invocation needs to match signature.
     * @return the list of parameters types that the invocation needs to match signature.
     */
    public List<String> getParameterTypes()
    {
        return parameterTypes;
    }

    /**
     * Sets the list of parameters types that the invocation needs to match signature.
     * @param parameterTypes the list of parameters types that the invocation needs to match signature.
     */
    public void setParameterTypes(List<String> parameterTypes)
    {
        this.parameterTypes = parameterTypes;
    }

    /**
     * Returns the list of parameter names (variable references) that the invocation uses.
     * @return list of parameter names (variable references) that the invocation uses.
     */
    public List<String> getParameterNames()
    {
        return parameterNames;
    }

    /**
     * Set list of parameter names (variable references) that the invocation uses.
     * @param parameterNames
     */
    public void setParameterNames(List<String> parameterNames)
    {
        this.parameterNames = parameterNames;
    }

    @Override
    public String toString()
    {
        return "Invoke [type=" + type + ", classCalled=" + classCalled + ", methodName="
                + methodName + ", parameterTypes=" + parameterTypes + ", returnType=" + returnType
                + "]";
    }

    /**
     * Gets complete signature of the Invoke.
     * <br> 
     * @return string in the format &lt;classCalled&gt;.&lt;methodName&gt;
     */
    public String getQualifiedName()
    {
        String methodName = getMethodName();
        String classCalled = getClassCalled();
        String signature = "";
        if (classCalled != null)
        {
            signature = classCalled.replace('/', '.') + "." + methodName; //$NON-NLS-1$
        }
        else
        {
            signature = methodName;
        }
        return signature;
    }

}
