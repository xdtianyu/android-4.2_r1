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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.motorolamobility.preflighting.core.checker.Checker;

/**
 * Status that should be used throughout this framework. Every time an {@link IStatus} is
 * required, use {@link CanExecuteConditionStatus} instead.
 */
public class CanExecuteConditionStatus extends Status
{

    private String conditionId;

    /**
     * Returns a Condition identifier which this {@link CanExecuteConditionStatus}
     * instance is linked.
     * 
     * @return Returns the {@link Condition} identifier which this object
     * is attached with. 
     */
    public String getConditionId()
    {
        return conditionId;
    }

    /**
     * Sets the {@link Condition} identifier for this {@link CanExecuteConditionStatus}. This status
     * then is attached to a certain {@link Condition}.
     * 
     * @param conditionId The condition identifier which attaches a certain Condition to
     * a Status.
     */
    public void setConditionId(String conditionId)
    {
        this.conditionId = conditionId;
    }

    /**
     * Constructor which holds minimum info to instantiate a meaningful {@link CanExecuteConditionStatus}. Note
     * that if you wish to provide an {@link IStatus#ERROR} or {@link IStatus#WARNING}, the condition identifier
     * must be provided using the method {@link CanExecuteConditionStatus#setConditionId(String)}.
     * 
     * @param severity The severity of the status. One may find them as constants for the
     * {@link Status} class.
     * @param pluginId The plug-in identifier where the {@link Checker} is implemented.
     * @param message Message to be displayed by this status.
     */
    public CanExecuteConditionStatus(int severity, String pluginId, String message)
    {
        super(severity, pluginId, message);
    }

    /**
     *  Constructor which holds minimum info to instantiate a meaningful {@link CanExecuteConditionStatus}. Note
     * that if you wish to provide an {@link IStatus#ERROR} or {@link IStatus#WARNING}, the condition identifier
     * must be provided using the method {@link CanExecuteConditionStatus#setConditionId(String)}.
     * 
     * @param severity The severity of the status. One may find them as constants for the
     * {@link CanExecuteConditionStatus} class.
     * @param pluginId The plug-in identifier where the {@link Checker} is implemented.
     * @param message Message to be displayed by this status.
     * @param exception Exception which raised this {@link CanExecuteConditionStatus}.
     */
    public CanExecuteConditionStatus(int severity, String pluginId, String message,
            Throwable exception)
    {
        super(severity, pluginId, message, exception);

    }

    /**
     * Constructor which holds minimum info to instantiate a meaningful {@link CanExecuteConditionStatus}. Note
     * that if you wish to provide an {@link IStatus#ERROR} or {@link IStatus#WARNING}, the condition identifier
     * must be provided using the method {@link CanExecuteConditionStatus#setConditionId(String)}.
     * 
     * @param severity The severity of the status. One may find them as constants for the
     * {@link CanExecuteConditionStatus} class.
     * @param pluginId The plug-in identifier where the {@link Checker} is implemented.
     * @param code The plug-in specific {@link CanExecuteConditionStatus} status code.
     * @param message Message to be displayed by this status.
     * @param exception Exception which raised this {@link CanExecuteConditionStatus}.
     */
    public CanExecuteConditionStatus(int severity, String pluginId, int code, String message,
            Throwable exception)
    {
        super(severity, pluginId, code, message, exception);

    }

    /**
     * Constructor which holds minimum info to instantiate a meaningful {@link CanExecuteConditionStatus}.
     * 
     * @param severity The severity of the status. One may find them as constants for the
     * {@link CanExecuteConditionStatus} class.
     * @param pluginId The plug-in identifier where the {@link Checker} is implemented.
     * @param message Message to be displayed by this status.
     * @param conditionId The identifier of the condition
     */
    public CanExecuteConditionStatus(int severity, String pluginId, String message,
            String conditionId)
    {
        super(severity, pluginId, message);
        this.conditionId = conditionId;

    }

    /**
     *  Constructor which holds minimum info to instantiate a meaningful {@link CanExecuteConditionStatus}.
     * 
     * @param severity The severity of the status. One may find them as constants for the
     * {@link CanExecuteConditionStatus} class.
     * @param pluginId The plug-in identifier where the {@link Checker} is implemented.
     * @param message Message to be displayed by this status.
     * @param exception Exception which raised this {@link CanExecuteConditionStatus}.
     * @param conditionId The identifier of the condition
     */
    public CanExecuteConditionStatus(int severity, String pluginId, String message,
            Throwable exception, String conditionId)
    {
        super(severity, pluginId, message, exception);
        this.conditionId = conditionId;

    }

    /**
     * Constructor which holds minimum info to instantiate a meaningful {@link CanExecuteConditionStatus}.
     * 
     * @param severity The severity of the status. One may find them as constants for the
     * {@link CanExecuteConditionStatus} class.
     * @param pluginId The plug-in identifier where the {@link Checker} is implemented.
     * @param code The plug-in specific {@link CanExecuteConditionStatus} status code.
     * @param message Message to be displayed by this status.
     * @param exception Exception which raised this {@link CanExecuteConditionStatus}.
     * @param conditionId The identifier of the condition
     */
    public CanExecuteConditionStatus(int severity, String pluginId, int code, String message,
            Throwable exception, String conditionId)
    {
        super(severity, pluginId, code, message, exception);
        this.conditionId = conditionId;

    }

    public void setStatusSeverity(int severity)
    {
        setSeverity(severity);
    }

}
