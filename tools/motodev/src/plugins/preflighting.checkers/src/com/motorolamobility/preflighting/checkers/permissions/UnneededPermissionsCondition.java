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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class UnneededPermissionsCondition extends Condition implements ICondition
{

    private XMLElement manifestElement;

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
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.devicespecification.PlatformRules, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        manifestElement = data.getManifestElement();
        Map<String, Node> manifestPermissions =
                CheckerUtils.getPermissions(manifestElement.getDocument());

        List<String> allCodePermissions = getAllCodePermissions();

        for (String permission : manifestPermissions.keySet())
        {
            if (!allCodePermissions.contains(permission)) //Verify if the permission declared on Manifest File is on the code needed permissions list.
            {
                //If not needed fill a result
                addValidationResult(valManagerConfig, results, manifestPermissions, permission);
            }
        }
    }

    /*
     * Add a ValidationResultData to the results list, with the given data
     */
    private void addValidationResult(ValidationManagerConfiguration valManagerConfig,
            ValidationResult results, Map<String, Node> manifestPermissions, String permission)
    {
        ValidationResultData resultData = new ValidationResultData();
        resultData.setConditionID(getId());
        resultData.setMarkerType(getMarkerType());
        resultData.appendExtra(permission);

        ArrayList<Integer> lines = new ArrayList<Integer>(1);
        Node permissionNode = manifestPermissions.get(permission);
        int issuedLine = manifestElement.getNodeLineNumber(permissionNode);
        if (issuedLine >= 0)
        {
            lines.add(issuedLine);
        }

        File manifestFile = manifestElement.getFile();
        resultData.addFileToIssueLines(manifestFile, lines);

        resultData.setIssueDescription(CheckerNLS.bind(
                CheckerNLS.UnneededPermissionsCondition_UneededPermissionMessage, permission));
        resultData
                .setQuickFixSuggestion(CheckerNLS.UnneededPermissionsCondition_UneededPermissionQuickFix);
        resultData.setSeverity(getSeverityLevel());
        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                valManagerConfig));
        resultData.setPreview(XmlUtils.getXMLNodeAsString(permissionNode, false));
        results.addValidationResult(resultData);
    }

    /*
     * Compile all needed permissions found by code analysis into one single list.
     */
    private List<String> getAllCodePermissions()
    {
        NeededAppPermissions neededAppPermissions =
                ((PermissionsChecker) getChecker()).getNeededAppPermissions();

        Map<Invoke, List<String>> requiredPermissionsMap =
                neededAppPermissions.getRequiredPermissionsMap();
        Map<Invoke, List<String>> optionalPermissionsMap =
                neededAppPermissions.getOptionalPermissionsMap();

        List<String> allCodePermissions = new ArrayList<String>();
        Collection<List<String>> requiredPermissions = requiredPermissionsMap.values();
        for (List<String> methodPermissions : requiredPermissions)
        {
            allCodePermissions.addAll(methodPermissions);
        }

        Collection<List<String>> optionalPermissions = optionalPermissionsMap.values();
        for (List<String> methodPermissions : optionalPermissions)
        {
            allCodePermissions.addAll(methodPermissions);
        }
        return allCodePermissions;
    }
}
