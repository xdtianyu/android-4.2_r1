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
package com.motorolamobility.preflighting.checkers.layout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
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
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * This Checker verifies if a there's any layout defined without an xlarge screen support 
 */
public class XlargeConfigCondition extends Condition implements ICondition
{

    private static final int MIN_SDK_VERSION = 4;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {

        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, ""); //$NON-NLS-1$

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

        status.setConditionId(getId());
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

        if (supportsXLargeScreens(data))
        {
            List<XMLElement> layoutElements = data.getLayoutElements();
            List<File> defaultLayoutFiles = new ArrayList<File>(layoutElements.size());
            List<String> xLargeLayoutFileNames = new ArrayList<String>(layoutElements.size());
            List<File> missingXLargeList = new ArrayList<File>();

            for (XMLElement xmlElement : layoutElements)
            {
                File layoutFile = xmlElement.getFile();
                String parentFolder = layoutFile.getParentFile().getName();
                if (isDefaultLayout(parentFolder))
                {
                    defaultLayoutFiles.add(layoutFile);
                }
                else if (isXlarge(parentFolder))
                {
                    xLargeLayoutFileNames.add(layoutFile.getName());
                }
            }

            //Retrieve the Layouts that does not have xlarge configuration
            for (File defaultLayout : defaultLayoutFiles)
            {
                if (!xLargeLayoutFileNames.contains(defaultLayout.getName()))
                {
                    missingXLargeList.add(defaultLayout);
                }
            }

            fillResults(valManagerConfig, missingXLargeList, results);
        }
    }

    /*
     * Verifies if the app supports xlarge screens, based on the manifest file data
     */
    private boolean supportsXLargeScreens(ApplicationData data)
    {
        XMLElement manifestElement = data.getManifestElement();
        Document manifestDoc = manifestElement.getDocument();
        NodeList supportsScreenNodes =
                manifestDoc.getElementsByTagName(ManifestConstants.SUPPORTS_SCREEN_TAG);

        boolean supportsXlarge = true;
        if (supportsScreenNodes.getLength() > 0)
        {
            Node supportsScreenNode = supportsScreenNodes.item(0); //Get the first occurrence.
            NamedNodeMap map = supportsScreenNode.getAttributes();
            Node xLargeNode = map.getNamedItem(ManifestConstants.XLARGE_SCREENS_ATTRIBUTE);
            if (xLargeNode != null)
            {
                String xLargeScreenNodeValue = xLargeNode.getNodeValue();
                try
                {
                    supportsXlarge = Boolean.parseBoolean(xLargeScreenNodeValue);
                }
                catch (Exception e)
                {
                    //Do Nothing. Value will assumed to be true.
                }
            }
        }

        try
        {
            String minSdkStr = CheckerUtils.getMinSdk(manifestDoc);
            int minSdkVersion = -1;
            try
            {
                minSdkVersion = Integer.parseInt(minSdkStr);
            }
            catch (NumberFormatException e)
            {
                minSdkVersion = -1; //Min Sdk is a String, it's a preview SDK, we'll not be able to handle this.
            }

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

            if (((minSdkVersion > 0) && (minSdkVersion < MIN_SDK_VERSION))
                    || ((targetSdk > 0) && (targetSdk < MIN_SDK_VERSION)))
            {
                supportsXlarge = false;
            }
        }
        catch (NumberFormatException e)
        {
            supportsXlarge = true; //Let's assume that all preview SDKs supports xlargeScreens.
        }
        return supportsXlarge;
    }

    /*
     * Fill a result data for every layout file that is in the missingXLargeList
     */
    private void fillResults(ValidationManagerConfiguration valManagerConfig,
            List<File> missingXLargeList, ValidationResult results)
    {
        for (File xlargeMissingFile : missingXLargeList)
        {
            ValidationResultData resultData =
                    new ValidationResultData(null, getSeverityLevel(), NLS.bind(
                            CheckerNLS.XlargeConfigCondition_Result_Description,
                            xlargeMissingFile.getName()), NLS.bind(
                            CheckerNLS.XlargeConfigCondition_Result_QuickFix,
                            xlargeMissingFile.getName()), getId());
            resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                    valManagerConfig));
            resultData.addFileToIssueLines(xlargeMissingFile, new ArrayList<Integer>(0));
            results.addValidationResult(resultData);
        }
    }

    /*
     * Checks if a folder contains the xlarge suffix or not
     */
    private boolean isXlarge(String parentFolder)
    {
        return parentFolder.contains("xlarge"); //$NON-NLS-1$
    }

    /*
     * Checks if a folder is the default layout folder
     */
    private boolean isDefaultLayout(String parentFolder)
    {
        return parentFolder.equalsIgnoreCase("layout"); //$NON-NLS-1$
    }
}
