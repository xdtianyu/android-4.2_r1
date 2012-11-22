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
package com.motorolamobility.preflighting.checkers.androidmarketfilters;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Condition that verifies if the android:debuggable flag is set to true.
 * It is recommended to set this flag to false before publishing the app.
 * A warning message is added to the results if the flag is true. 
 */
public class IsDebuggableCondition extends Condition implements ICondition
{

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        return CheckerUtils.isAndroidManifestFileExistent(data, getId());
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
        XMLElement manifestElement = data.getManifestElement();
        if (manifestElement != null)
        {
            Document manifestDoc = manifestElement.getDocument();
            NodeList applicationNodes =
                    manifestDoc.getElementsByTagName(ManifestConstants.APPLICATION_TAG);

            if (applicationNodes != null)
            {
                for (int i = 0; i < applicationNodes.getLength(); i++)
                {
                    Node applicationNode = applicationNodes.item(i);
                    analyzeApplicationNode(results, manifestElement, applicationNode);
                }
            }
        }

    }

    /**
     * Looks for the android:debuggable attribute. If found add a warning resultData if the value is true.
     * @param results
     * @param manifestElement
     * @param applicationNode
     */
    private void analyzeApplicationNode(ValidationResult results, XMLElement manifestElement,
            Node applicationNode)
    {
        NamedNodeMap attributes = applicationNode.getAttributes();
        Attr debuggableAttr =
                (Attr) attributes.getNamedItem(ManifestConstants.ANDROID_DEBUGGABLE_ATTRIBUTE);
        if (debuggableAttr != null)
        {
            String value = debuggableAttr.getValue();
            if (value.equalsIgnoreCase("true")) ////$NON-NLS-1$
            {
                Map<File, List<Integer>> fileToIssueLines = new HashMap<File, List<Integer>>(1);
                int lineNumber = manifestElement.getNodeLineNumber(applicationNode);
                if (lineNumber > 0)
                {
                    fileToIssueLines.put(manifestElement.getFile(), Arrays.asList(lineNumber));
                }
                ValidationResultData resultData =
                        new ValidationResultData(fileToIssueLines, getSeverityLevel(),
                                CheckerNLS.IsDebuggableCondition_AttrFound_Message,
                                CheckerNLS.IsDebuggableCondition_AttrFound_QuickFix, getId());
                resultData.setPreview(XmlUtils.getXMLNodeAsString(applicationNode, false));
                results.addValidationResult(resultData);
            }
        }
    }

}
