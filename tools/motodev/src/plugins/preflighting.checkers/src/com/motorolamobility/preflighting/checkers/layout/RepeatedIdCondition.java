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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import com.motorolamobility.preflighting.core.utils.LayoutConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Layout Checker condition that verifies if an id is declared in more than one component on a given layout file. 
 */
public class RepeatedIdCondition extends Condition implements ICondition
{

    private ValidationManagerConfiguration valManagerConfig;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        //All situations already handled by Checker
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
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
        List<XMLElement> layoutList = data.getLayoutElements();
        if (layoutList != null)
        {
            this.valManagerConfig = valManagerConfig;

            //Iterate on all layout elements
            for (XMLElement layoutElement : layoutList)
            {
                HashSet<String> idsList = new HashSet<String>();
                Document layoutDocument = layoutElement.getDocument();
                Element docElement = layoutDocument.getDocumentElement();

                searchRepeatedIds(results, layoutElement, idsList, docElement);
            }
        }
    }

    /*
     * This method takes an XML element as input and recursively tries to locate repeated ids
     * In case an repeated id is detected the results are updated with a issue.
     */
    private void searchRepeatedIds(ValidationResult results, XMLElement xmlElement,
            HashSet<String> idsList, Node elementNode)
    {
        //Check if the node has an ID, if yes, verifies if it was not previously found (already present on  idList)
        NamedNodeMap map = elementNode.getAttributes();
        Node attribute = map.getNamedItem(LayoutConstants.ANDROID_ID_ATTRIBUTE);
        if (attribute != null)
        {
            String id = attribute.getTextContent();
            id = CheckerUtils.getIdValue(id.trim());
            if (id.length() > 0)
            {
                if (idsList.contains(id))
                {
                    File layoutFile = xmlElement.getFile();
                    String layoutName = layoutFile.getName();
                    String descriptionMsg =
                            NLS.bind(CheckerNLS.RepeatedIdCondition_Result_Description, layoutName,
                                    id);
                    String quickFixSuggestion =
                            NLS.bind(CheckerNLS.RepeatedIdCondition_Result_QuickFix, id, layoutName);
                    ValidationResultData resultData =
                            new ValidationResultData(null, getSeverityLevel(), descriptionMsg,
                                    quickFixSuggestion, getId());
                    int lineNumber = xmlElement.getNodeLineNumber(elementNode);
                    resultData.addFileToIssueLines(layoutFile,
                            lineNumber >= 0 ? Arrays.asList(lineNumber) : new ArrayList<Integer>());
                    resultData.setPreview(XmlUtils.getXMLNodeAsString(elementNode, false));
                    resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                            getId(), valManagerConfig));
                    results.addValidationResult(resultData);
                }
                else
                {
                    idsList.add(id);
                }
            }
        }

        //visit children
        NodeList nodeList = elementNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                searchRepeatedIds(results, xmlElement, idsList, node);
            }
        }
    }
}
