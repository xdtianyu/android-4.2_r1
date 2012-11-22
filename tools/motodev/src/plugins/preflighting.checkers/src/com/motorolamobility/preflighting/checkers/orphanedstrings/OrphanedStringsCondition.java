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
package com.motorolamobility.preflighting.checkers.orphanedstrings;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.ElementUtils;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Condition that looks for declared strings that are not used in the code (Java or resources XML)  
 */
public class OrphanedStringsCondition extends Condition implements ICondition
{

    private ResourcesFolderElement resFolder;

    /**
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        Set<String> usedStringsSet = data.getUsedStringsInApplication();

        PreflightingLogger.debug("Used strings: " + usedStringsSet);

        //notUsedStringsSet - initially we have the entire set of declared strings
        Set<String> notUsedStringsSet = data.getDeclaredStringsInResourceFiles();

        PreflightingLogger.debug("Declared strings: " + notUsedStringsSet);

        //removing all used strings (notUsedStringSet = declaredStringsSet - usedStringsSet)
        notUsedStringsSet.removeAll(usedStringsSet);

        PreflightingLogger.debug("Not used strings: " + notUsedStringsSet);

        if ((notUsedStringsSet != null) && !notUsedStringsSet.isEmpty())
        {
            //we found strings not used
            addResults(results, notUsedStringsSet, valManagerConfig);
        }
    }

    private void addResults(ValidationResult results, Set<String> notUsedStringsSet,
            ValidationManagerConfiguration valManagerConfig)
    {
        for (String notUsedString : notUsedStringsSet)
        {
            //adding string result  (reached max number of chars) 
            ValidationResultData result = new ValidationResultData();

            // Create a result and return it
            result.setSeverity(getSeverityLevel());
            result.setConditionID(getId());

            //Associate the result to the resFolder
            result.addFileToIssueLines(resFolder.getFile(), Collections.<Integer> emptyList());

            // Construct description
            String resultDescription =
                    CheckerNLS.bind(CheckerNLS.OrphanedStringsCondition_Result_Description,
                            notUsedString);
            result.setIssueDescription(resultDescription.toString());

            // Set quickfix  
            result.setQuickFixSuggestion(CheckerNLS.OrphanedStringsCondition_Result_QuickFix);
            result.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                    valManagerConfig));

            // Add result to the result list
            results.addValidationResult(result);
        }
    }

    /**
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, PreflightingCorePlugin.PLUGIN_ID, "");
        //Verify if model contains res folder
        List<Element> folderResElements =
                ElementUtils.getElementByType(data.getRootElement(), Type.FOLDER_RES);

        resFolder =
                folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements.get(0)
                        : null;

        if (resFolder == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.Missing_res_folder);
        }
        if (status.isOK())
        {
            // Check if at least one "values" folder exist with a appropriate "strings.xml" file
            int numberOfFoundValuesResources = 0;
            for (Element e : resFolder.getChildren())
            {
                if (e.getType() == Element.Type.FOLDER_VALUES)
                {
                    for (Element children : e.getChildren())
                    {
                        if (children.getType() == Element.Type.FILE_STRINGS)
                        {
                            numberOfFoundValuesResources++;
                        }
                    }
                }
            }

            if (numberOfFoundValuesResources == 0)
            {
                status =
                        new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.LocalizationStringsChecker_Missing_stringsXml_File);
            }
        }
        status.setConditionId(getId());
        return status;
    }
}
