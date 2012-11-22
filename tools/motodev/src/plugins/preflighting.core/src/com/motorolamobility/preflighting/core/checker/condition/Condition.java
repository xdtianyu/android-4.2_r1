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
 * Default implementation for a Checker Condition.
 * 
 * It is recommended that conditions extend this class, overriding methods canExecute and execute. 
 * Note that the attributes id, name, description, defaultSeverityLevel and checker are all filled 
 * by the AppValidator core with information provided on the extension from plugin.xml file.
 * 
 */
public abstract class Condition implements ICondition
{

    /**
     * The condition ID.
     */
    protected String id;

    /**
     * The condition name.
     */
    protected String name;

    /**
     * Description of the condition.
     */
    protected String description;

    /**
     * Which severity this condition represents by default.
     */
    protected SEVERITY defaultSeverityLevel;

    /**
     * Checker that is the owner of this Condition.
     */
    protected IChecker checker;

    /**
     * True if condition is enabled to run.
     */
    protected boolean enabled = true;

    /**
     * Specific marker type for this Condition.
     * Default value is a default AppValidator marker type.
     * Subclasses should define their own marker type and properly set this attribute. 
     */
    protected String markerType;

    /**
     * Default constructor.
     * 
     */
    public Condition()
    {
    }

    /**
     * Construct a new Condition with the given parameters.
     * 
     * @param id Condition id.
     * @param name Condition name.
     * @param description Condition description.
     * @param defaultLevel Condition default level.
     */
    public Condition(String id, String name, String description, SEVERITY defaultLevel)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultSeverityLevel = defaultLevel;
    }

    /**
     * @return Condition id.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id Condition id to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return Condition name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name Condition name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return Condition description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description Condition description to set.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.ICondition#getSeverityLevel()
     */
    public SEVERITY getSeverityLevel()
    {
        return defaultSeverityLevel;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.ICondition#setSeverityLevel(com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY)
     */
    public void setSeverityLevel(SEVERITY defaultSeverityLevel)
    {
        this.defaultSeverityLevel = defaultSeverityLevel;
    }

    /**
     * @return Checker associated with the condition.
     */
    public IChecker getChecker()
    {
        return checker;
    }

    /**
     * @param checker Checker to set.
     */
    public void setChecker(IChecker checker)
    {
        this.checker = checker;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.ICondition#setMarkerType(String)
     */
    public void setMarkerType(String markerType)
    {
        this.markerType = markerType;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.ICondition#getMarkerType()
     */
    public String getMarkerType()
    {
        return this.markerType;
    }

    /**
     * <b>Must always be implemented.</b>
     * 
     * @param data the {@link ApplicationData} 
     * @param deviceSpecs the list of {@link DeviceSpecification}
     * @param valManagerConfig the {@link ValidationManagerConfiguration}
     * @param results
     * 
     * @throws PreflightingCheckerException
     */
    public abstract void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException;

    /**
     * <b>Must always be implemented.</b>
     * 
     * @param data the {@link ApplicationData} 
     * @param deviceSpecs the list of {@link DeviceSpecification}
     * @return the status, that is if this condition can be executed. See {@link CanExecuteConditionStatus} for more information.
     * @throws PreflightingCheckerException
     */
    public abstract CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.ICondition#isEnabled()
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.ICondition#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "Condition [id=" + id + ", name=" + name + ", description=" + description
                + ", defaultSeverityLevel=" + defaultSeverityLevel + ", checker=" + checker
                + ", enabled=" + enabled + "]";
    }
}
