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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.osgi.util.NLS;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ParameterDescription;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;

/**
 *  This is the base Checker implementation.
 *  <br>
 *  Usually new checkers do not need to have an implementation of this class.
 *  Default implementation of canExecute and execute methods will iterate over all checker conditions calling
 *  conditions canExecute and validateApplication methods.
 */
public class Checker implements IChecker
{

    private MultiStatus checkerStatus;

    /**
     * The unique identifier for this checker.
     */
    private String id;

    /**
     * A map for the conditions this checker has.
     */
    private Map<String, ICondition> conditions;

    /**
     * The parameters for this {@link Checker}.
     */
    private Map<String, ICheckerParameter> checkerParameters;

    /**
     * The global parameters that were passed on command line
     */
    private List<Parameter> globalParams = new ArrayList<Parameter>();

    /**
     * True if condition is enabled to run.
     */
    protected boolean enabled = true;

    /**
     * The default implementation of this method returns a list of {@link ParameterDescription} objects.
     * which is derived from the list of entered parameters of the Checker Extension-point.
     * 
     * @return List of {@link ParameterDescription} where each object holds a detailed description
     * of each parameter.
     * 
     * @see com.motorolamobility.preflighting.core.IParameterProcessor#getParameterDescriptions()
     */
    public List<ParameterDescription> getParameterDescriptions()
    {
        List<ParameterDescription> parameters = new ArrayList<ParameterDescription>();

        if ((checkerParameters != null) && (checkerParameters.size() > 0))
        {
            ParameterDescription parameterDescription = null;
            for (ICheckerParameter conditionParameter : checkerParameters.values())
            {
                parameterDescription = new ParameterDescription();
                parameterDescription.setName(conditionParameter.getName());
                parameterDescription.setDescription(conditionParameter.getDescription());
                parameterDescription.setValueDescription(conditionParameter.getValueDescription());
                parameterDescription.setValueRequired(conditionParameter.isMandatory());
                parameterDescription.setType(conditionParameter.getType());
                parameters.add(parameterDescription);
            }
        }

        return parameters;
    }

    /**
     * The default implementation of this method verifies whether all {@link ICheckerParameter} objects
     * which are mandatory have values. One may override this method in order to validate each {@link ICheckerParameter}
     * value. Also, when overriding this method, remember to call its <code>super</code> implementation
     * so the validation here is also performed.
     * <br>
     * The entered parameters are the ones inputed by the user and they are represented by a 
     * list of {@link Parameter}.
     * <br> 
     * This method returns an {@link IStatus} which represents the status of this method execution. However, for this to work properly, one has
     * to return its implementation represented by {@link MultiStatus}.
     * 
     * @param parameters The parameters which are entered by the user. When implementing this method, one
     * may use them to perform the necessary validations. One also can use the {@link ICheckerParameter} object values
     * to validate the entered parameters since they are the same.
     * 
     * @return Return the status of this method execution. Although its return value is an {@link IStatus},
     * one must return {@link MultiStatus}, which is its implementation for this framework.
     * 
     * @see com.motorolamobility.preflighting.core.IParameterProcessor#validateInputParams(java.util.List)
     */
    public IStatus validateInputParams(List<Parameter> parameters)
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(CanExecuteConditionStatus.OK,
                        PreflightingCorePlugin.PLUGIN_ID, ""); //$NON-NLS-1$

        // check whether all mandatory parameters were entered
        if ((checkerParameters != null) && (checkerParameters.size() > 0))
        {
            for (ICheckerParameter checkerParameter : checkerParameters.values())
            {
                if (checkerParameter.isMandatory() && (checkerParameter.getValue() == null))
                {
                    status =
                            new CanExecuteConditionStatus(
                                    CanExecuteConditionStatus.ERROR,
                                    PreflightingCorePlugin.PLUGIN_ID,
                                    NLS.bind(
                                            PreflightingCoreNLS.Checker_MandatoryParam_EmptyValueWarn,
                                            getId(), checkerParameter.getId()),
                                    checkerParameter.getId());
                    break;
                }
            }
        }

        return status;
    }

    /**
     * Set the Checker Identifier. This value identifies
     * the {@link Checker} uniquely.
     * 
     * @param id Represent the Checker Identifier.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Implementation of the generic {@link IChecker#canExecute(ApplicationData, List)} Method. 
     * This method verifies if all checker conditions can be executed.
     * <br>
     * Note: For backward support, old checkers must override this method with their own version.
     * <br>
     * If you override this method be aware that you either might have to manually call the canExecute method for your conditions or call super.canExecute(),
     * in order to verify make sure that canExecute method from conditions are being called.
     * 
     * @param data Represents the data structure of the APK or Android Project.
     * @param deviceSpecs When the checker is applied to specific devices, the list of
     * {@link DeviceSpecification} holds the ones to be validated.
     * 
     * @return Return the status of the execution. Although {@link IStatus} is the returned
     * interface, one must return {@link MultiStatus}, which is a collection of {@link CanExecuteConditionStatus}
     * 
     * 
     * @throws PreflightingCheckerException Exception thrown if there are any problems executing this validation.
     * 
     */
    public IStatus canExecute(ApplicationData data, List<DeviceSpecification> deviceSpecs)
            throws PreflightingCheckerException
    {

        checkerStatus = new MultiStatus(PreflightingCorePlugin.PLUGIN_ID, IStatus.OK, null, null);

        if ((conditions != null) && (!conditions.isEmpty()))
        {

            for (ICondition condition : conditions.values())
            {
                if (condition.isEnabled())
                {
                    IStatus conditionStatus = condition.canExecute(data, deviceSpecs);
                    checkerStatus.add(conditionStatus);
                }
            }
        }

        return checkerStatus;

    }

    /**
     * The default implementation executes all conditions that returned an {@link IStatus#OK} status on {@link IChecker#canExecute(ApplicationData, List)} method.
     * 
     * @see com.motorolamobility.preflighting.core.checker.IChecker#validateApplication(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    public void validateApplication(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        if ((conditions != null) && (!conditions.isEmpty()))
        {
            for (ICondition condition : conditions.values())
            {
                if (condition.isEnabled())
                {
                    for (IStatus conditionStatus : checkerStatus.getChildren())
                    {
                        if (conditionStatus instanceof CanExecuteConditionStatus)
                        {
                            if ((((CanExecuteConditionStatus) conditionStatus).getConditionId()
                                    .equals(condition.getId()))
                                    && (conditionStatus.getSeverity() == IStatus.OK))
                            {
                                condition.execute(data, deviceSpecs, valManagerConfig, results);

                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Free memory allocated
     */
    public void clean()
    {
        //Override if necessary
    }

    public String getId()
    {
        return id;
    }

    /**
    * 
    * @see com.motorolamobility.preflighting.core.checker.IChecker#getConditions()
    */
    public Map<String, ICondition> getConditions()
    {
        return conditions != null ? conditions : new HashMap<String, ICondition>(0);
    }

    /**
     *
     * @see com.motorolamobility.preflighting.core.checker.IChecker#setConditions(java.util.HashMap)
     */
    public void setConditions(HashMap<String, ICondition> conditions)
    {
        this.conditions = conditions;
    }

    /**
     * Gets the list of parameters for this checker.
     * 
     * @return the list of parameters for this checker.
     */
    public Map<String, ICheckerParameter> getParameters()
    {
        return this.checkerParameters;
    }

    /**
     * Sets the list of Parameters for this Condition.
     * 
     * @param conditionParameters This list of Parameters for this Condition.
     */
    public void setParameters(Map<String, ICheckerParameter> conditionParameters)
    {
        this.checkerParameters = conditionParameters;
    }

    /**
     * @return the globalParams
     */
    public final List<Parameter> getGlobalParams()
    {
        return globalParams;
    }

    /**
     * @param globalParams the globalParams to set
     */
    public final void setGlobalParams(List<Parameter> globalParams)
    {
        this.globalParams = globalParams;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.IChecker#isEnabled()
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.IChecker#setEnabled(boolean)
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
        return "Checker [checkerStatus=" + checkerStatus + ", id=" + id + ", enabled=" + enabled
                + "]";
    }
}
