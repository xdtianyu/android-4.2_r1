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
package com.motorolamobility.preflighting.checkers.missingdrawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Verifies existence of all density-specific drawable folders.
 * Raise issue if one or more drawable folders do not exist.
 */
public class MissingDrawableFoldersCondition extends Condition implements ICondition
{
    private MissingDrawableData missingDrawableData;

    /**
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        // First we need to check if this test is valid at all.
        // If the minSdkVersion is 3 (or lower) or the <supports-screen> tag does not exit, return an OK result.
        MissingDrawableChecker checker;
        if (getChecker() instanceof MissingDrawableChecker)
        {
            checker = (MissingDrawableChecker) getChecker();
            missingDrawableData = checker.getMissingDrawableData();
            if (missingDrawableData.isTestApplicable())
            {
                generateResults(results, valManagerConfig);
            }
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
                new CanExecuteConditionStatus(IStatus.OK, PreflightingCorePlugin.PLUGIN_ID,
                        "", getId()); //$NON-NLS-1$
        return status;
    }

    /**
     * Auxiliary method to execute validation and generate the results
     * @return A list of results
     */
    private void generateResults(ValidationResult results,
            ValidationManagerConfiguration valManagerConfig)
    {
        if (missingDrawableData.atLeastOneDrawableFolderExist())
        {

            if (missingDrawableData.isMissingAtLeastOneDrawableFolder())
            {
                // That's an error
                ArrayList<String> strsToBind = new ArrayList<String>();

                if (!missingDrawableData.isLdpiFolderExists())
                {
                    strsToBind.add(CheckerNLS.MissingDrawableChecker_ldpiFolder);
                }
                if (!missingDrawableData.isMdpiFolderExists())
                {
                    strsToBind.add(CheckerNLS.MissingDrawableChecker_mdpiFolder);
                }
                if (!missingDrawableData.isHdpiFolderExists())
                {
                    strsToBind.add(CheckerNLS.MissingDrawableChecker_hdpiFolder);
                }
                if (!missingDrawableData.isXhdpiFolderExists()
                        && missingDrawableData.isXhdpiApplicable())
                {
                    strsToBind.add(CheckerNLS.MissingDrawableChecker_xhdpiFolder);
                }

                for (String currentStr : strsToBind)
                {
                    ValidationResultData result = new ValidationResultData();
                    result.setSeverity(getSeverityLevel());
                    result.setIssueDescription(CheckerNLS.bind(
                            CheckerNLS.MissingDrawableChecker_FolderNotFound, currentStr));
                    result.setQuickFixSuggestion(CheckerNLS.bind(
                            CheckerNLS.MissingDrawableChecker_CreateFolderOnResFolder, currentStr));
                    result.addFileToIssueLines(missingDrawableData.getResFolder().getFile(),
                            Collections.<Integer> emptyList());
                    result.setConditionID(getId());
                    result.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                            getId(), valManagerConfig));
                    results.addValidationResult(result);

                }
            }
        }
        else
        {
            // No drawable resolution folders exist
            if (missingDrawableData.isStandardFolderExists())
            {
                ValidationResultData result = new ValidationResultData();
                result.setConditionID(getId());
                result.setSeverity(getSeverityLevel());
                result.setIssueDescription(CheckerNLS.MissingDrawableChecker_noDensitySpecificDrawableFolders);
                result.setQuickFixSuggestion(CheckerNLS.bind(
                        CheckerNLS.MissingDrawableChecker_CreateFoldersOnResFolder, new Object[]
                        {
                                CheckerNLS.MissingDrawableChecker_ldpiFolder,
                                CheckerNLS.MissingDrawableChecker_mdpiFolder,
                                CheckerNLS.MissingDrawableChecker_hdpiFolder
                        }));
                result.addFileToIssueLines(missingDrawableData.getResFolder().getFile(),
                        Collections.<Integer> emptyList());
                result.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                        valManagerConfig));
                results.addValidationResult(result);
            }
            else
            {
                ValidationResultData result = new ValidationResultData();
                result.setSeverity(getSeverityLevel());
                result.setIssueDescription(CheckerNLS.MissingDrawableChecker_noDrawableFolders);
                result.addFileToIssueLines(missingDrawableData.getResFolder().getFile(),
                        Collections.<Integer> emptyList());
                result.setConditionID(getId());
                result.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                        valManagerConfig));
                results.addValidationResult(result);
            }
        }
    }

}
