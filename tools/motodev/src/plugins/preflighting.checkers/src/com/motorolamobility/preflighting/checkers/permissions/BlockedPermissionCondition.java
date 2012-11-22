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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class BlockedPermissionCondition extends Condition implements ICondition
{

    private static final String USES_PERMISSION = "uses-permission"; //$NON-NLS-1$

    private static final String ANDROID_NAME = "android:name"; //$NON-NLS-1$

    /**
     * Elements to validate
     */
    private XMLElement manifestElement;

    private Document manifestDoc;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, "");

        manifestElement = data.getManifestElement();
        if (manifestElement == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.Invalid_ManifestFile);
        }
        else if ((manifestDoc = manifestElement.getDocument()) == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.Invalid_ManifestFile);
        }

        status.setConditionId(getId());
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
        NodeList permissionsNodeLst = manifestDoc.getElementsByTagName(USES_PERMISSION);

        //Extract permissions from manifest
        List<String> manifestPermissionsList =
                new ArrayList<String>(permissionsNodeLst.getLength());
        for (int i = 0; i < permissionsNodeLst.getLength(); i++)
        {
            Node permissionNode = permissionsNodeLst.item(i);
            NamedNodeMap permissionMap = permissionNode.getAttributes();
            Node permissionAtr = permissionMap.getNamedItem(ANDROID_NAME);

            if ((permissionAtr != null))
            {
                String permissionId = permissionAtr.getNodeValue().trim();
                if (!permissionId.equals("")) //$NON-NLS-1$
                {
                    manifestPermissionsList.add(permissionId);
                }
            }
        }

        //analyze permissions node
        verifyBlockedPermissions(PlatformRules.getInstance(), valManagerConfig, results,
                permissionsNodeLst);
    }

    private void verifyBlockedPermissions(PlatformRules platformRules,
            ValidationManagerConfiguration valManagerConfig, ValidationResult result,
            NodeList permissionsLst)
    {
        for (int i = 0; i < permissionsLst.getLength(); i++)
        {
            Node permissionNode = permissionsLst.item(i);
            NamedNodeMap permissionMap = permissionNode.getAttributes();
            Node permissionAtr = permissionMap.getNamedItem(ANDROID_NAME);

            if ((permissionAtr != null) && !permissionAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
            {
                String permissionId = permissionAtr.getNodeValue();
                //if blocked permission, create a result data
                if (platformRules.isPermissionBlocked(permissionId))
                {
                    result.addValidationResult(createBlockedPermissionsValidationResult(
                            permissionNode, permissionId, valManagerConfig));
                }
            }
        }
    }

    private ValidationResultData createBlockedPermissionsValidationResult(Node permissionNode,
            String permissionId, ValidationManagerConfiguration valManagerConfig)
    {
        ValidationResultData resultData = new ValidationResultData();
        resultData.setConditionID(getId());

        ArrayList<Integer> lines = new ArrayList<Integer>();
        int issuedLine = manifestElement.getNodeLineNumber(permissionNode);
        if (issuedLine != -1)
        {
            lines.add(issuedLine);
        }
        resultData.addFileToIssueLines(manifestElement.getFile(), lines);
        resultData.setIssueDescription(CheckerNLS.bind(
                CheckerNLS.UnsecurePermissionsChecker_conditionForbiddenPermission_description,
                permissionId));
        resultData
                .setQuickFixSuggestion(CheckerNLS.UnsecurePermissionsChecker_conditionForbiddenPermission_suggestion);
        resultData.setSeverity(getSeverityLevel());
        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                valManagerConfig));
        resultData.setPreview(XmlUtils.getXMLNodeAsString(permissionNode, false));

        return resultData;
    }
}
