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
package com.motorolamobility.preflighting.checkers.deviceCompatibility;

import java.util.ArrayList;
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
import com.motorolamobility.preflighting.core.devicelayoutspecification.ParametersType;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Check if device has small screen but AndroidManifest set smallScreens=false.
 */
public class SmallScreensSupportCondition extends Condition implements ICondition
{

    /*
     * AndroidManifest.xml constants
     */

    /**
     * Elements to validate
     */
    private XMLElement manifestElement;

    private final String SMALL_SCREEN = "small"; //$NON-NLS-1$

    //small screen are supported since API level 4
    private final int SMALL_SCREEN_THRESHOLD = 4;

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
        else
        {
            Document manifestDoc = manifestElement.getDocument();

            if (manifestDoc == null)
            {
                status =
                        new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.Invalid_ManifestFile);
            }
        }

        status.setConditionId(getId());
        return status;
    }

    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        checkSmallScreenCondition(deviceSpecs, valManagerConfig, results);
    }

    private void checkSmallScreenCondition(List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        boolean userDeclaredUnsupported = false;
        boolean supportSmallScreen = true;
        Document manifestDoc = manifestElement.getDocument();
        Node usesSdkNode = null;

        // get supports-screen node
        NodeList supportsScreenLst =
                manifestDoc.getElementsByTagName(ManifestConstants.SUPPORTS_SCREEN_TAG);
        if (supportsScreenLst.getLength() > 0)
        {
            Node supportsScreenNode = supportsScreenLst.item(0); //Get the first occurrence.
            NamedNodeMap map = supportsScreenNode.getAttributes();
            //small-screen attribute
            Node smallScreenNode = map.getNamedItem(ManifestConstants.SMALL_SCREENS_ATTRIBUTE);
            if (smallScreenNode != null)
            {
                String smallScreenNodeValue = smallScreenNode.getNodeValue();
                try
                {
                    userDeclaredUnsupported = !Boolean.parseBoolean(smallScreenNodeValue);
                }
                catch (Exception e)
                {
                    //Do Nothing, the value will be assumed to be false.
                }
            }
        }

        if (!userDeclaredUnsupported)
        {
            NodeList usesSdkList = manifestDoc.getElementsByTagName(ManifestConstants.USES_SDK_TAG);
            if (usesSdkList.getLength() > 0)
            {
                usesSdkNode = usesSdkList.item(0);
                supportSmallScreen = checkIfMinSdkTooLowFilteringSmallDevices(usesSdkNode);
            }
        }

        //small screens are not supported by user application
        if (!supportSmallScreen)
        {
            ArrayList<String> smallDevicesList = new ArrayList<String>();
            //check if is there any device with small screen on the list
            for (DeviceSpecification currentSpec : deviceSpecs)
            {
                ParametersType params = currentSpec.getDeviceInfo().getDefault();
                String deviceScreenSize = params.getScreenSize();
                if (deviceScreenSize.equals(SMALL_SCREEN))
                {
                    smallDevicesList.add(currentSpec.getDeviceInfo().getName());
                }
            }

            int currentIssuedLine = manifestElement.getNodeLineNumber(usesSdkNode);
            String preview = XmlUtils.getXMLNodeAsString(usesSdkNode, false);
            //list of small screen devices
            for (String currentDevice : smallDevicesList)
            {
                ValidationResultData resultData = new ValidationResultData();
                resultData.setSeverity(getSeverityLevel());
                resultData.setConditionID(getId());
                resultData
                        .setIssueDescription(CheckerNLS
                                .bind(CheckerNLS.DeviceCompatibilityChecker_SMALL_SCREEN_SUPPORT_ISSUE_DESCRIPTION,
                                        currentDevice));

                String fixSuggestionMessage =
                        CheckerNLS.DeviceCompatibilityChecker_SMALL_SCREEN_SUPPORT_FIX_SUGGESTION;

                resultData.setQuickFixSuggestion(fixSuggestionMessage);

                ArrayList<Integer> lines = new ArrayList<Integer>();
                lines.add(currentIssuedLine);
                resultData.addFileToIssueLines(manifestElement.getFile(), lines);
                resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                        getId(), valManagerConfig));
                resultData.setPreview(preview);
                results.addValidationResult(resultData);
            }
        }
    }

    //Verify if minSdk and targetSdk values are less than 4 (the SMALL_SCREEN_THRESHOLD)
    private boolean checkIfMinSdkTooLowFilteringSmallDevices(Node usesSdkNode)
            throws PreflightingCheckerException
    {
        boolean supportsSmallScreens = true;
        Node minSdkAtr =
                usesSdkNode.getAttributes().getNamedItem(
                        ManifestConstants.MIN_SDK_VERSION_ATTRIBUTE);
        try
        {
            if (minSdkAtr != null)
            {
                String minSdkStr = minSdkAtr.getNodeValue().trim();
                try
                {
                    Integer minSdk = Integer.valueOf(minSdkStr);
                    if (minSdk < SMALL_SCREEN_THRESHOLD) //$NON-NLS-1$
                    {
                        // min sdk too low => check target version
                        Node targetSdkVersionAtr =
                                usesSdkNode.getAttributes().getNamedItem(
                                        ManifestConstants.ANDROID_TARGET_SDK_VERSION_ATTRIBUTE);
                        if ((targetSdkVersionAtr == null)
                                || (targetSdkVersionAtr.getNodeValue().trim().length() == 0))
                        {
                            supportsSmallScreens = false;
                        }
                        else
                        {
                            // target sdk declared => check if value is ok
                            String targetSdkVersionStr = targetSdkVersionAtr.getNodeValue().trim();
                            try
                            {
                                Integer targetSdkVersion = Integer.valueOf(targetSdkVersionStr);
                                if (targetSdkVersion < SMALL_SCREEN_THRESHOLD)
                                {
                                    supportsSmallScreens = false;
                                }
                            }
                            catch (NumberFormatException nfe)
                            {
                                // Do nothing, it will be handled on the target sdk is a preview condition.
                            }
                        }

                    }
                }
                catch (NumberFormatException nfe)
                {
                    // Do nothing, it will be handled on the min sdk is a preview condition.
                }
            }
        }
        catch (DOMException e)
        {
            // Error retrieving attribute
            throw new PreflightingCheckerException(CheckerNLS.bind(
                    CheckerNLS.AndroidMarketFiltersChecker_Exception_Getting_Manifest_Attribute,
                    ManifestConstants.ANDROID_TARGET_SDK_VERSION_ATTRIBUTE), e);
        }
        return supportsSmallScreens;
    }

}
