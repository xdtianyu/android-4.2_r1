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
package com.motorolamobility.preflighting.core.checker.condition;

import java.util.List;

import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.IChecker;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;

/**
 * A Checker Condition.
 * This class is responsible to verify one specific condition for a given Checker.
 * All Checkers must supply a Condition, even if there is only one condition in a Checker.
 * If you want you can extend the default {@link Condition} implementation and only override 
 * execute and canExecute methods as needed.
 */
public interface ICondition
{
    /**
     * 
     * This method is called in order to perform the validation for this condition.
     * 
     * @param data general information about the app being validated.
     * @param deviceSpecs device specifications to be used during validations.
     * @param valManagerConfig a bean that represents the configuration of a given validation.
     * @param results the object that will receive results for this validation. Use the add methods of it in order to include new results to the current validation.
     * @return A ValidationResult for problems or success. 
     * Can also return null if no problems are found.
     * @throws PreflightingCheckerException Exception thrown when there are problems executing
     * the checker.
     */
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException;

    /**
     * This method verifies if this condition can be executed or if there is something wrong with the {@link ApplicationData}
     * that prevents this checker from executing properly.
     * @param data The {@link ApplicationData} available for this validation.
     * @param deviceSpecs {@link List} of {@link DeviceSpecification} 
     * @return
     * @throws PreflightingCheckerException Exception thrown when there are problems validating
     * the checker.
     */
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException;

    /**
     * @return the condition ID.
     */
    public String getId();

    /**
     * @param id the condition id to set.
     */
    public void setId(String conditionId);

    /**
     * @return the condition name.
     */
    public String getName();

    /**
     * @param name the condition name to set.
     */
    public void setName(String name);

    /**
     * @return the condition description.
     */
    public String getDescription();

    /**
     * @param description the description to set.
     */
    public void setDescription(String description);

    /**
     * Get the default severity level for this condition.
     * @return the default severity level of this condition.
     */
    public SEVERITY getSeverityLevel();

    /**
     * Sets the default severity level for this condition.
     * @param defaultSeverityLevel the severity level to set.
     */
    public void setSeverityLevel(SEVERITY defaultSeverityLevel);

    /**
     * @param checker the checker that is the owner of this condition.
     */
    public void setChecker(IChecker checker);

    /**
     * @return the checker, owner of this condition.
     */
    public IChecker getChecker();

    /**
     * Returns true if this condition is set as enabled to run, false otherwise.
     * @return
     */
    public boolean isEnabled();

    /**
     * Enables or disables the condition.
     * @param enabled true if enabled, false otherwise.
     */
    public void setEnabled(boolean enabled);

    /**
     * Sets string the identifies the marker type of this condition.
     * The marker type is used to implement quick fix.
     * @param the type of the marker.
     */
    public void setMarkerType(String markerType);

    /**
     * Returns the string that identifies the marker type for this condition.
     */
    public String getMarkerType();
}
