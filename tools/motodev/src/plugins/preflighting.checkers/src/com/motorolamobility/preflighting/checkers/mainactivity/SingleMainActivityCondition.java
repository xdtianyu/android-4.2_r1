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

package com.motorolamobility.preflighting.checkers.mainactivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.DOMException;
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
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Condition to check if the AndroidManifest has exactly one main activity declared. 
 *
 */
public class SingleMainActivityCondition extends Condition implements ICondition
{
    /**
     * Identifier of category LAUNCHER. 
     */
    private static final String ANDROID_INTENT_CATEGORY_LAUNCHER =
            "android.intent.category.LAUNCHER";

    /**
     * Identifier of CATEGORY element.
     */
    private static final String CATEGORY_TAG = "category";

    /**
     * Identifier of action MAIN.
     */
    private static final String ANDROID_INTENT_ACTION_MAIN = "android.intent.action.MAIN";

    /**
     * Identifier of the android:name attribute.
     */
    private static final String ANDROID_NAME_ATTR = "android:name";

    /**
     * Identifier of the ACTION element.
     */
    private static final String ACTION_TAG = "action";

    /**
     * Identifier of the INTENT_FILTER element.
     */
    private static final String INTENT_FILTER_TAG = "intent-filter";

    /**
     * Identifier of the ACTIVITY element.
     */
    private static final String ACTIVITY_TAG = "activity";

    private ValidationManagerConfiguration valManagerConfig;

    private List<Integer> issuedLinesList = null;

    private XMLElement manElement;

    private static final String MAIN_ACTION_NODE =
            "<action android:name=\"android.intent.action.MAIN\"/>";

    /** 
     * Check that AndroidManifet exists and has a manifest node.
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, "");
        status.setConditionId(getId());

        XMLElement manElem = data.getManifestElement();
        if (manElem == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.Invalid_ManifestFile);
        }
        else
        {
            Document manifestDoc = manElem.getDocument();

            if (manifestDoc == null)
            {
                status =
                        new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.Invalid_ManifestFile);
            }
        }
        return status;
    }

    /** 
     * Check if there is exactly one main activity declared on AndroidManifest.
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        int mainActivityCount = 0;

        this.valManagerConfig = valManagerConfig;
        issuedLinesList = new ArrayList<Integer>();
        manElement = data.getManifestElement();
        Document manifestDoc = manElement.getDocument();

        // get activity nodes
        NodeList actLst = manifestDoc.getElementsByTagName(ACTIVITY_TAG); //$NON-NLS-1$

        for (int i = 0; i < actLst.getLength(); i++)
        {
            NodeList intentFilterLst = actLst.item(i).getChildNodes();
            for (int j = 0; j < intentFilterLst.getLength(); j++)
            {
                int currentIssuedLine = 0;

                boolean actionFound = false;
                boolean categoryFound = false;

                Node intentFilterNode = intentFilterLst.item(j);
                // get intent-filter nodes
                if (intentFilterNode.getNodeName().equals(INTENT_FILTER_TAG)) //$NON-NLS-1$
                {
                    NodeList actionLst = intentFilterNode.getChildNodes();
                    for (int k = 0; k < actionLst.getLength(); k++)
                    {
                        Node actionNode = actionLst.item(k);
                        // get action nodes
                        if (actionNode.getNodeName().equals(ACTION_TAG)) //$NON-NLS-1$
                        {
                            NamedNodeMap map = actionNode.getAttributes();
                            // name attribute must be set to
                            // android.intent.action.MAIN
                            Node nameAtr = map.getNamedItem(ANDROID_NAME_ATTR); //$NON-NLS-1$

                            try
                            {
                                if ((nameAtr != null)
                                        && nameAtr.getNodeValue()
                                                .equals(ANDROID_INTENT_ACTION_MAIN)) //$NON-NLS-1$
                                {
                                    actionFound = true;
                                    // NOTE: the action node position is issued
                                    currentIssuedLine = manElement.getNodeLineNumber(actionNode);
                                }

                            }
                            catch (DOMException e)
                            {
                                // Error retrieving value of the action intent
                                throw new PreflightingCheckerException(
                                        CheckerNLS.MainActivityChecker_Exception_Get_Action_Intent_Value,
                                        e);
                            }
                        }
                        // get category nodes
                        else if (actionNode.getNodeName().equals(CATEGORY_TAG)) //$NON-NLS-1$
                        {
                            NamedNodeMap map = actionNode.getAttributes();
                            // name attribute must be set to
                            // android.intent.category.LAUNCHER
                            Node nameAtr = map.getNamedItem(ANDROID_NAME_ATTR); //$NON-NLS-1$
                            try
                            {
                                if ((nameAtr != null)
                                        && nameAtr.getNodeValue().equals(
                                                ANDROID_INTENT_CATEGORY_LAUNCHER)) //$NON-NLS-1$
                                {
                                    categoryFound = true;
                                }

                            }
                            catch (DOMException e)
                            {
                                // Error retrieving value of the category intent
                                throw new PreflightingCheckerException(
                                        CheckerNLS.MainActivityChecker_Exception_Get_Category_Intent_Value,
                                        e);
                            }
                        }
                    }
                    // a main activity must have both category and action nodes
                    if (categoryFound && actionFound)
                    {
                        mainActivityCount++;
                        issuedLinesList.add(currentIssuedLine);
                    }
                }
            }
        }

        createValidationResult(mainActivityCount, results);
    }

    private void createValidationResult(int mainActivityCount, ValidationResult results)
    {
        ValidationResultData resultData = new ValidationResultData();

        // one main activity - OK
        if (mainActivityCount == 1)
        {
            resultData.setSeverity(ValidationResultData.SEVERITY.OK);
        }
        else
        {
            resultData.setSeverity(getSeverityLevel());
            resultData.setConditionID(getId());
            resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                    valManagerConfig));

            // none main activity - WARNING
            if (mainActivityCount == 0)
            {
                resultData.setIssueDescription(CheckerNLS.MainActivityChecker_noMainActivity);
                resultData.addFileToIssueLines(manElement.getFile(),
                        Collections.<Integer> emptyList());
                resultData
                        .setQuickFixSuggestion(CheckerNLS.MainActivityChecker_NoMainActivityFixSuggestion);
            }
            // more than one main activity - WARNING
            else
            {
                resultData.setIssueDescription(CheckerNLS.MainActivityChecker_manyMainActivity);
                resultData
                        .setQuickFixSuggestion(CheckerNLS.MainActivityChecker_MoreThanOneMainActivityFixSuggestion);
                resultData.addFileToIssueLines(manElement.getFile(), issuedLinesList);
                resultData.setPreview(MAIN_ACTION_NODE);
            }
        }
        results.addValidationResult(resultData);
    }

}
