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
package com.motorolamobility.preflighting.core.internal.checker;

import static com.motorolamobility.preflighting.core.logging.PreflightingLogger.error;
import static com.motorolamobility.preflighting.core.logging.PreflightingLogger.warn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.checker.CheckerExtension;
import com.motorolamobility.preflighting.core.checker.IChecker;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.checker.parameter.CheckerParameter;
import com.motorolamobility.preflighting.core.checker.parameter.ICheckerParameter;
import com.motorolamobility.preflighting.core.exception.PreflightingExtensionPointException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.internal.checkerparameter.CheckerParameterElement;
import com.motorolamobility.preflighting.core.internal.conditions.ConditionElement;
import com.motorolamobility.preflighting.core.validation.ParameterType;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;

public abstract class CheckerExtensionReader
{
    /**
     * Load existing checkers, mapped by checker id (value used to call the checker
     * on command line).
     * The map is sorted alphabetically by checker id.
     * 
     * @param checkersMap The map of checkers to be populated 
     * 
     * @throws PreflightingExtensionPointException If an error with this extension point is found,
     *      the exception is thrown to warn the framework that this functionality is broken
     */
    public static void loadCheckers(TreeMap<String, CheckerExtension> checkersMap)
            throws PreflightingExtensionPointException
    {
        checkersMap.clear();

        IExtensionRegistry extReg = Platform.getExtensionRegistry();
        IExtensionPoint extPoint =
                extReg.getExtensionPoint(CheckerExtension.CHECKER_EXTENSION_POINT_ID);
        // it should not be null, but check to prevent errors
        if (extPoint != null)
        {
            try
            {
                IExtension[] extensions = extPoint.getExtensions();
                for (IExtension aExtension : extensions)
                {
                    IConfigurationElement[] configElements = aExtension.getConfigurationElements();
                    for (IConfigurationElement aConfig : configElements)
                    {
                        if (aConfig.getName().equals(
                                CheckerExtension.CHECKER_EXTENSION_POINT_ELEMENT_CHECKER))
                        {
                            try
                            {
                                String id =
                                        aConfig.getAttribute(CheckerExtension.CHECKER_EXTENSION_POINT_ATTRIBUTE_ID);
                                String name =
                                        aConfig.getAttribute(CheckerExtension.CHECKER_EXTENSION_POINT_ATTRIBUTE_NAME);
                                String description =
                                        aConfig.getAttribute(CheckerExtension.CHECKER_EXTENSION_POINT_ATTRIBUTE_DESCRIPTION);

                                String checkerClassName =
                                        aConfig.getAttribute(CheckerExtension.CHECKER_EXTENSION_POINT_ATTRIBUTE_CLASS);
                                IChecker checker = null;
                                if (checkerClassName != null)
                                {
                                    checker =
                                            (IChecker) aConfig
                                                    .createExecutableExtension(CheckerExtension.CHECKER_EXTENSION_POINT_ATTRIBUTE_CLASS);
                                }
                                else
                                {
                                    checker = new Checker();
                                }
                                checker.setId(id);
                                checker.setEnabled(true);
                                CheckerExtension checkerExtension =
                                        new CheckerExtension(id, name, description, checker);

                                loadConditions(checkerExtension, aConfig);

                                loadParameters(checkerExtension, aConfig);

                                checkersMap.put(id, checkerExtension);
                            }
                            catch (CoreException ce)
                            {
                                warn(CheckerExtensionReader.class,
                                        "Error reading checker extension of id " //$NON-NLS-1$
                                                + aExtension.getUniqueIdentifier()
                                                + ": invalid checker or condition class", ce);
                            }
                            catch (Exception e)
                            {
                                warn(CheckerExtensionReader.class,
                                        "Error reading checker extension of id " //$NON-NLS-1$
                                                + aExtension.getUniqueIdentifier(), e);
                            }
                        }
                    }
                }
            }
            catch (InvalidRegistryObjectException e)
            {
                error(CheckerExtensionReader.class,
                        "Unexpected error with the checker extension point", e); //$NON-NLS-1$
                throw new PreflightingExtensionPointException(
                        PreflightingCoreNLS.CheckerExtensionReader_UnexpectedErrorCheckerExtensionPoint,
                        e);
            }
        }
        else
        {
            error(CheckerExtensionReader.class, "Checker extension point not found"); //$NON-NLS-1$
            throw new PreflightingExtensionPointException(
                    PreflightingCoreNLS.CheckerExtensionReader_CheckerExtensionPointNotFound);
        }
    }

    /*
     * Load all children elements from a given checker element.
     * Currently only conditions are supported. 
     */
    private static void loadConditions(CheckerExtension checkerExtension,
            IConfigurationElement checkerElement) throws CoreException
    {
        IConfigurationElement[] childrenElements = checkerElement.getChildren();
        if (childrenElements.length > 0)
        {
            List<ConditionElement> conditionsList =
                    new ArrayList<ConditionElement>(childrenElements.length);
            HashMap<String, ICondition> checkerConditions =
                    new HashMap<String, ICondition>(childrenElements.length);
            for (IConfigurationElement childElement : childrenElements)
            {
                if (childElement.getName().equals(ConditionElement.CHECKER_CONDITION_ELEMENT_NAME)) //Condition found, load it!
                {
                    String conditionId =
                            childElement
                                    .getAttribute(ConditionElement.CHECKER_CONDITION_ATTRIBUTE_ID);
                    String conditionName =
                            childElement
                                    .getAttribute(ConditionElement.CHECKER_CONDITION_ATTRIBUTE_NAME);
                    String conditionDesc =
                            childElement
                                    .getAttribute(ConditionElement.CHECKER_CONDITION_ATTRIBUTE_DESCRIPTION);
                    String defaultSeverityLevel =
                            childElement
                                    .getAttribute(ConditionElement.CHECKER_CONDITION_ATTRIBUTE_DEFAULT_SEVERITY);
                    String markerType =
                            childElement
                                    .getAttribute(ConditionElement.CHECKER_CONDITION_ATTRIBUTE_MARKER_TYPE);
                    ICondition condition =
                            (ICondition) childElement
                                    .createExecutableExtension(ConditionElement.CHECKER_CONDITION_ATTRIBUTE_CLASS);
                    condition.setId(conditionId);
                    condition.setName(conditionName);
                    condition.setDescription(conditionDesc);
                    condition.setSeverityLevel(SEVERITY.valueOf(defaultSeverityLevel));
                    condition.setChecker(checkerExtension.getChecker());
                    condition.setMarkerType(markerType);
                    PreflightingCorePlugin.addAvailableMarker(markerType);

                    ConditionElement conditionElement =
                            new ConditionElement(conditionId, conditionName, conditionDesc,
                                    defaultSeverityLevel, condition);
                    conditionsList.add(conditionElement);
                    checkerConditions.put(conditionId, condition);
                }
            }
            checkerExtension.getChecker().setConditions(checkerConditions);
            checkerExtension.setConditions(conditionsList
                    .toArray(new ConditionElement[conditionsList.size()]));
        }
    }

    /**
     * Load the list of {@link CheckerParameterElement} objects into the {@link ConditionElement} parameter,
     * This list comes from the defined extension-points.
     * 
     * @param checkerExtension {@link CheckerExtension} where the {@link CheckerParameterElement} objects
     * will be added.
     * @param conditionConfigurationElement Configuration element. This element is responsible to fetch the
     * data from the defined extension-points.
     */
    private static void loadParameters(CheckerExtension checkerExtension,
            IConfigurationElement conditionConfigurationElement)
    {
        IConfigurationElement[] childrenElements = conditionConfigurationElement.getChildren();
        if (childrenElements.length > 0)
        {
            List<CheckerParameterElement> parameterElementList =
                    new ArrayList<CheckerParameterElement>(childrenElements.length);
            Map<String, ICheckerParameter> parameters = new HashMap<String, ICheckerParameter>();
            for (IConfigurationElement childElement : childrenElements)
            {
                if (childElement.getName().equals(
                        CheckerParameterElement.CHECKER_PARAMETER_ELEMENT_NAME)) //Condition found, load it!
                {
                    String parameterId =
                            childElement
                                    .getAttribute(CheckerParameterElement.CHECKER_PARAMETER_ATTRIBUTE_ID);
                    String parameterName =
                            childElement
                                    .getAttribute(CheckerParameterElement.CHECKER_PARAMETER_ATTRIBUTE_NAME);
                    String parameterDescription =
                            childElement
                                    .getAttribute(CheckerParameterElement.CHECKER_PARAMETER_ATTRIBUTE_DESCRIPTION);
                    String parameterValueDescription =
                            childElement
                                    .getAttribute(CheckerParameterElement.CHECKER_PARAMETER_ATTRIBUTE_VALUE_DESCRIPTION);
                    String parameterTypeString =
                            childElement
                                    .getAttribute(CheckerParameterElement.CHECKER_PARAMETER_ATTRIBUTE_TYPE);
                    boolean parameterIsMandatory =
                            Boolean.parseBoolean(childElement
                                    .getAttribute(CheckerParameterElement.CHECKER_PARAMETER_ATTRIBUTE_IS_MANDATORY));

                    ParameterType parameterType = null;
                    if (parameterTypeString.equals("BOOLEAN"))
                    {
                        parameterType = ParameterType.BOOLEAN;
                    }
                    else if (parameterTypeString.equals("INTEGER"))
                    {
                        parameterType = ParameterType.INTEGER;
                    }
                    else
                    {
                        parameterType = ParameterType.STRING;
                    }

                    ICheckerParameter parameter =
                            new CheckerParameter(parameterId, parameterName, parameterDescription,
                                    parameterValueDescription, parameterType, parameterIsMandatory);

                    CheckerParameterElement parameterElement =
                            new CheckerParameterElement(parameterId, parameterName,
                                    parameterDescription, parameterValueDescription,
                                    parameterTypeString, parameterIsMandatory);

                    parameters.put(parameterId, parameter);
                    parameterElementList.add(parameterElement);
                }
            }
            checkerExtension.getChecker().setParameters(parameters);
            checkerExtension.setConditionParameters(parameterElementList
                    .toArray(new CheckerParameterElement[parameterElementList.size()]));
        }
    }
}
