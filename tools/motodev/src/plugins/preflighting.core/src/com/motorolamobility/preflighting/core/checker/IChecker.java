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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import com.motorolamobility.preflighting.core.IParameterProcessor;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * This is the basic interface for Checkers. All Checkers must define an implementation of this Interface.
 * It can be used to keep common data among conditions and run common verifications before executing the conditions.
 * It is recommended to use the {@link Checker} class and override just the necessary methods instead of implementing this Interface.
 */
public interface IChecker extends IParameterProcessor
{

    /**
     * Performs the validation. This method is called once for each {@link IChecker} that is configured to execute if 
     * {@link IChecker#canExecute(ApplicationData, List)} method returns a {@link IStatus#OK}.
     * 
     * @param data The {@link ApplicationData} containing all available information for the application being tested.
     * @param deviceSpecs The {@link List} containing all {@link DeviceSpecification} available to AppValidator
     * @param valManagerConfig {@link ValidationManagerConfiguration} containing the configuration for this validation. 
     * @param checkerResults {@link ValidationResult} At the end of validation, {@link ValidationResultData} must be added to checkerResults.
     * @throws PreflightingCheckerException Exception thrown if there are any problems executing this validation.
     */
    public void validateApplication(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult checkerResults)
            throws PreflightingCheckerException;

    /**
     * Verifies is this Checker can be executed or not.
     * The idea here is to only return a non OK value if there's some invalid information on {@link ApplicationData}.
     * 
     * @param data The {@link ApplicationData} containing all available information for the application being tested.
     * @param deviceSpecs The {@link List} containing all {@link DeviceSpecification} available to AppValidator
     * 
     * @return Returns the status indicating whether the {@link IChecker} can be executed. Although
     * {@link IStatus} is returned, one must return its implementation: {@link MultiStatus}.
     * 
     * @throws PreflightingCheckerException Exception thrown in case there is any problem executing
     * this validation.
     */
    public IStatus canExecute(ApplicationData data, List<DeviceSpecification> deviceSpecs)
            throws PreflightingCheckerException;

    /**
     * Returns the {@link IChecker} unique identifier.
     * 
     * @return The checker unique identifier.
     */
    public String getId();

    /**
     * Sets the {@link IChecker} unique identifier.
     * 
     * @param id Returns the checker unique identifier.
     */
    public void setId(String id);

    /**
     * Returns the conditions {@link ICondition} defined by the {@link IChecker}.
     * <br>
     * If the {@link IChecker} has no conditions, an empty  {@link Map} must be returned.
     *             
     * @return A {@link Map} that contains the condition IDs as keys and the condition objects associated with them.
     */
    public Map<String, ICondition> getConditions();

    /**
     * Set the conditions defined by the checker. Conditions are not mandatory, so setting conditions is optional.
     * 
     * @param checkerConditions {@link Map} holding the conditions. Each condition is identified
     * by a {@link String}.
     */
    public void setConditions(HashMap<String, ICondition> checkerConditions);

    /**
     * Gets the list of Parameters for this Condition.
     * 
     * @return Returns the list of Parameters for this Condition.
     */
    public Map<String, ICheckerParameter> getParameters();

    /**
     * Sets the list of parameters for this condition.
     * 
     * @param conditionParameters This list of Parameters for this Condition.
     */
    public void setParameters(Map<String, ICheckerParameter> conditionParameters);

    /**
     * This method will be called right after execution.
     * All data used for this verification, which is stored in this class for some reason, must be cleared in order to reduce memory consumption.
     */
    public void clean();

    /**
     * @return true if this checker is set as enabled to run, false otherwise.
     */
    public boolean isEnabled();

    /**
     * Enables or disables the checker.
     * @param enabled true if enabled, false otherwise.
     */
    public void setEnabled(boolean enabled);

}
