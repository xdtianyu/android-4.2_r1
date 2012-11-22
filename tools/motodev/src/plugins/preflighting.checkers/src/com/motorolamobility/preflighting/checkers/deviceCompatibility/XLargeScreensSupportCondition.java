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
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Check if device has small screen but AndroidManifest set smallScreens=false.
 */
public class XLargeScreensSupportCondition extends Condition implements ICondition
{

    /*
     * AndroidManifest.xml constants
     */

    /**
     * Elements to validate
     */
    private XMLElement manifestElement;

    private final String XLARGE_SCREEN = "xlarge"; //$NON-NLS-1$

    //xlarge screens are supported since API level 4
    private final int XLARGE_SCREEN_THRESHOLD = 4;

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
        checkXLargeScreenCondition(deviceSpecs, valManagerConfig, results);
    }

    private void checkXLargeScreenCondition(List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
    {
        boolean supportXLargeScreen = true;
        int currentIssuedLine = -1;
        String preview = null;
        Document manifestDoc = manifestElement.getDocument();

        // get supports-screen node
        NodeList supportsScreenLst =
                manifestDoc.getElementsByTagName(ManifestConstants.SUPPORTS_SCREEN_TAG);
        if (supportsScreenLst.getLength() > 0)
        {
            Node supportsScreenNode = supportsScreenLst.item(0); //Get the first occurrence.
            NamedNodeMap map = supportsScreenNode.getAttributes();
            //xlarge-screen attribute
            Node xlargeScreenNode = map.getNamedItem(ManifestConstants.XLARGE_SCREENS_ATTRIBUTE);
            if (xlargeScreenNode != null)
            {
                String xlargeScreenNodeValue = xlargeScreenNode.getNodeValue();
                //tag line
                currentIssuedLine = manifestElement.getNodeLineNumber(supportsScreenNode);
                preview = XmlUtils.getXMLNodeAsString(supportsScreenNode, false);
                try
                {
                    supportXLargeScreen = Boolean.parseBoolean(xlargeScreenNodeValue);
                }
                catch (Exception e)
                {
                    //Do Nothing. Value will assumed to be true.
                }
            }
        }

        //Verify if Compatible screens list the xlarge size
        if (supportXLargeScreen)
        {
            boolean compatibleXLargeScreens = true;
            NodeList compatibleScreensNodes =
                    manifestDoc.getElementsByTagName("compatible-screens");
            if (compatibleScreensNodes.getLength() > 0)
            {
                compatibleXLargeScreens = false;
                supportXLargeScreen = false; //Change it to false until we find a xlarge screen size under compatible-screens
                Node compatibleScreensNode = compatibleScreensNodes.item(0);
                NodeList screenNodes = compatibleScreensNode.getChildNodes();
                for (int i = 0; i < screenNodes.getLength(); i++)
                {
                    Node screenNode = screenNodes.item(i);
                    NamedNodeMap screenAttributes = screenNode.getAttributes();
                    if (screenAttributes != null)
                    {
                        Node screenSizeNode = screenAttributes.getNamedItem("android:screenSize");
                        String screenSizeValue = screenSizeNode.getNodeValue();
                        if (screenSizeValue.equals("xlarge"))
                        {
                            compatibleXLargeScreens = true; //User declared that the xlarge is supported.
                            break;
                        }
                    }
                }
            }
            // If the user has not declared that supports xlarge screen = false and has not restricted slarge compatibility by using the compatible-screens tag. We consider that this app should support it and then we can validate
            if (supportXLargeScreen && compatibleXLargeScreens)
            {

                NodeList usesSdkList =
                        manifestDoc.getElementsByTagName(ManifestConstants.USES_SDK_TAG);
                if (usesSdkList.getLength() > 0)
                {
                    Node usesSdkNode = usesSdkList.item(0);
                    currentIssuedLine = manifestElement.getNodeLineNumber(usesSdkNode);
                    preview = XmlUtils.getXMLNodeAsString(usesSdkNode, false);

                    String targetSdkStr = CheckerUtils.getTargetSdk(manifestDoc);
                    int targetSdk = -1;
                    try
                    {
                        targetSdk = Integer.parseInt(targetSdkStr);
                    }
                    catch (NumberFormatException e)
                    {
                        targetSdk = -1; //Target Sdk is a String, it's a preview SDK, we'll not be able to handle this.
                    }

                    if ((targetSdk > -1) && (targetSdk < XLARGE_SCREEN_THRESHOLD))
                    {
                        supportXLargeScreen = false;
                    }

                    // If targetSdk >= XLARGE_SCREEN_THRESHOLD, we have to test with minSdkVersion
                    if (supportXLargeScreen)
                    {
                        int minSdk = -1;
                        try
                        {
                            minSdk = Integer.parseInt(targetSdkStr);
                        }
                        catch (NumberFormatException e)
                        {
                            minSdk = -1; //Target Sdk is a String, it's a preview SDK, we'll not be able to handle this.
                        }
                        if ((minSdk > -1) && (minSdk < XLARGE_SCREEN_THRESHOLD))
                        {
                            supportXLargeScreen = false;
                        }
                    }
                }
            }

            //xlarge screens are not supported by user application
            if (!supportXLargeScreen && compatibleXLargeScreens)
            {
                ArrayList<String> xlargeDevicesList = new ArrayList<String>();
                //check if is there any device with xlarge screen on the list
                for (DeviceSpecification currentSpec : deviceSpecs)
                {
                    ParametersType params = currentSpec.getDeviceInfo().getDefault();
                    String deviceScreenSize = params.getScreenSize();
                    if (deviceScreenSize.equals(XLARGE_SCREEN))
                    {
                        xlargeDevicesList.add(currentSpec.getDeviceInfo().getName());
                    }
                }

                if (xlargeDevicesList.size() > 0)
                {
                    String devices = "";
                    //list of xlarge screen devices
                    for (String currentDevice : xlargeDevicesList)
                    {
                        if (devices.equals(""))
                        {
                            devices += "[" + currentDevice + "]";
                        }
                        else
                        {
                            devices += ", [" + currentDevice + "]";
                        }
                    }

                    ValidationResultData resultData = new ValidationResultData();
                    resultData.setSeverity(getSeverityLevel());
                    resultData.setConditionID(getId());
                    resultData
                            .setIssueDescription(CheckerNLS
                                    .bind(CheckerNLS.DeviceCompatibilityChecker_XLARGE_SCREEN_SUPPORT_ISSUE_DESCRIPTION,
                                            devices));

                    String fixSuggestionMessage =
                            CheckerNLS.DeviceCompatibilityChecker_XLARGE_SCREEN_SUPPORT_FIX_SUGGESTION;

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
    }
}
