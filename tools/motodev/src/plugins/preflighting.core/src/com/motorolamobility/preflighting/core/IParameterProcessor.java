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
package com.motorolamobility.preflighting.core;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ParameterDescription;

/**
 * Interface for parameter processors. Implementers of this interface should validate parameters passed to App Validator.
 */
public interface IParameterProcessor
{

    /** 
     * Returns the definitions for checker input parameters. 
     * List of flagName, type (bool, string, integer,etc) and default value
     */
    public List<ParameterDescription> getParameterDescriptions();

    /**
     * Validates input parameters. In the case of a Checker also keep these parameters on an internal structure.
     */
    public IStatus validateInputParams(List<Parameter> parameters);

}