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
package com.motorolamobility.preflighting.checkers.permissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class MissingPermissionsCondition extends Condition implements ICondition
{

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status =
                CheckerUtils.isAndroidManifestFileExistent(data, getId());
        if (status.isOK())
        {
            status = CheckerUtils.isJavaModelComplete(data, getId());
        }

        return status;
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.devicespecification.PlatformRules, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        Map<String, Node> manifestPermissions =
                CheckerUtils.getPermissions(data.getManifestElement().getDocument());

        analyzeMissingPermissions(data, PlatformRules.getInstance(), valManagerConfig, results,
                manifestPermissions.keySet());

    }

    private void analyzeMissingPermissions(ApplicationData data, PlatformRules platformRules,
            ValidationManagerConfiguration valManagerConfig, ValidationResult result,
            Set<String> manifestPermissions)
    {
        NeededAppPermissions neededAppPermissions =
                ((PermissionsChecker) getChecker()).getNeededAppPermissions();

        Map<Invoke, List<String>> requiredPermissionsMap =
                neededAppPermissions.getRequiredPermissionsMap();
        Map<Invoke, List<String>> optionalPermissionsMap =
                neededAppPermissions.getOptionalPermissionsMap();

        //Check required permissions
        for (Invoke invoked : requiredPermissionsMap.keySet())
        {
            for (String requiredPermission : requiredPermissionsMap.get(invoked))
            {
                if (!manifestPermissions.contains(requiredPermission))
                {
                    ValidationResultData validationResult =
                            createMissingPermissionResult(valManagerConfig, invoked, new String[]
                            {
                                requiredPermission
                            });
                    result.addValidationResult(validationResult);
                }
            }
        }

        //Check optional permissions
        for (Invoke invoked : optionalPermissionsMap.keySet())
        {
            boolean optionalPermissionFound = false;
            List<String> optionalPermissionsList = optionalPermissionsMap.get(invoked);
            for (String optionalPermission : optionalPermissionsList)
            {
                if (manifestPermissions.contains(optionalPermission))
                {
                    optionalPermissionFound = true;
                    break;
                }
            }

            //If none of the optional permission was declared then setup an error on result.
            if (!optionalPermissionsList.isEmpty() && !optionalPermissionFound)
            {
                ValidationResultData validationResult =
                        createMissingPermissionResult(valManagerConfig, invoked,
                                optionalPermissionsList.toArray(new String[optionalPermissionsList
                                        .size()]));
                result.addValidationResult(validationResult);
            }
        }

    }

    private ValidationResultData createMissingPermissionResult(
            ValidationManagerConfiguration valManagerConfig, Invoke invokedMethod,
            String[] permissions)
    {
        //Fill ValidationData, missing required permission found!
        ValidationResultData resultData = new ValidationResultData();
        resultData.setConditionID(getId());
        resultData.setMarkerType(getMarkerType());

        ArrayList<Integer> lines = new ArrayList<Integer>();
        int issuedLine = invokedMethod.getLine();
        if (issuedLine != -1)
        {
            lines.add(issuedLine);
        }
        File javaFile = new File(invokedMethod.getSourceFileFullPath());
        resultData.addFileToIssueLines(javaFile, lines);

        StringBuffer missingPermissionsStrBuffer = new StringBuffer();
        for (String missingPermission : permissions)
        {
            missingPermissionsStrBuffer.append(missingPermission + ",");
            resultData.appendExtra(missingPermission);
        }
        missingPermissionsStrBuffer.deleteCharAt(missingPermissionsStrBuffer.length() - 1);
        String permissionsString = missingPermissionsStrBuffer.toString();

        resultData.setIssueDescription(CheckerNLS.bind(
                CheckerNLS.PermissionsChecker_MissingPermission_Message,
                invokedMethod.getQualifiedName(), permissionsString)); //Format the permissions array so that the displayed result will be nice.
        resultData.setQuickFixSuggestion(CheckerNLS.PermissionsChecker_MissingPermission_QuickFix);
        resultData.setSeverity(getSeverityLevel());
        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                valManagerConfig));
        //We don't have the preview for java at this time.
        return resultData;
    }
}
