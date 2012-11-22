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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
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
 * Layout checker condition that verifies if an ID is used for the same kind of view on all configurations of a given layout 
 */
public class ViewTypeIdsCondition extends Condition implements ICondition
{

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
     * 
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {

        List<XMLElement> layoutList = data.getLayoutElements();

        //Map that lists all LayoutFilesIds for a given layout. Key is a layoutfile name, values is a list containing all layoutfiles that represents all
        //configurations for that layout
        Map<String, List<LayoutFileId>> allIdsMap =
                new HashMap<String, List<LayoutFileId>>(layoutList.size());
        Map<File, XMLElement> xmlElementMap = new HashMap<File, XMLElement>(layoutList.size());

        //Mount allIdsMap
        collectNeededData(layoutList, allIdsMap, xmlElementMap);

        //Iterate over all layouts, analyzing all configurations
        Set<String> layouts = allIdsMap.keySet();
        for (String layout : layouts)
        {
            Map<String, String> idViewTypeMap = new HashMap<String, String>(); //Key=id, value=View type of the first occurrence
            List<LayoutFileId> layoutFileIds = allIdsMap.get(layout);
            for (LayoutFileId layoutFileId : layoutFileIds)
            {
                analyzeLayoutFileId(valManagerConfig, results, xmlElementMap, idViewTypeMap,
                        layoutFileId);
            }
        }

    }

    private void analyzeLayoutFileId(ValidationManagerConfiguration valManagerConfig,
            ValidationResult results, Map<File, XMLElement> xmlElementMap,
            Map<String, String> idViewTypeMap, LayoutFileId layoutFileId)
    {
        Map<Node, String> nodeMap = layoutFileId.getNodeIdMap();
        for (Node viewNode : nodeMap.keySet())
        {
            String nodeId = nodeMap.get(viewNode);
            String viewType = viewNode.getNodeName();
            if (idViewTypeMap.containsKey(nodeId)) //Verify if this id was previouslly found.
            {
                String mapViewType = idViewTypeMap.get(nodeId);
                if (!mapViewType.equals(viewType)) //Different types found for the same id, issue found!
                {
                    ValidationResultData resultData =
                            getIssueResultData(valManagerConfig, xmlElementMap, layoutFileId,
                                    viewNode, nodeId, viewType, mapViewType);
                    results.addValidationResult(resultData);
                }
            }
            else
            //First time for this ID, put this on the map!
            {
                idViewTypeMap.put(nodeId, viewType);
            }
        }
    }

    private ValidationResultData getIssueResultData(
            ValidationManagerConfiguration valManagerConfig, Map<File, XMLElement> xmlElementMap,
            LayoutFileId layoutFileId, Node viewNode, String nodeId, String viewType,
            String mapViewType)
    {
        ValidationResultData resultData =
                new ValidationResultData(null, getSeverityLevel(), NLS.bind(
                        CheckerNLS.ViewTypeIdsCondition_Results_Description, new String[]
                        {
                                nodeId, viewType, mapViewType
                        }), CheckerNLS.ViewTypeIdsCondition_Results_QuickFix, getId());
        File layoutFile = layoutFileId.getLayoutFile();
        int lineNumber = xmlElementMap.get(layoutFile).getNodeLineNumber(viewNode);
        resultData.addFileToIssueLines(layoutFile, lineNumber >= 0 ? Arrays.asList(lineNumber)
                : new ArrayList<Integer>());
        resultData.setPreview(XmlUtils.getXMLNodeAsString(viewNode, false));
        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                valManagerConfig));
        return resultData;
    }

    /*
     * Fill all needed information in order to do the verification.
     * Retrieves all ids from all layout files as well as its xml declaring nodes.
     */
    private void collectNeededData(List<XMLElement> layoutList,
            Map<String, List<LayoutFileId>> allIdsMap, Map<File, XMLElement> xmlElementMap)
    {
        for (XMLElement element : layoutList)
        {
            File layoutFile = element.getFile();
            xmlElementMap.put(layoutFile, element);
            LayoutFileId layoutFileId = new LayoutFileId(layoutFile);
            getLayoutIds(element, element.getDocument().getDocumentElement(), layoutFileId);
            List<LayoutFileId> layoutFileList;
            if (allIdsMap.containsKey(layoutFile.getName()))
            {
                layoutFileList = allIdsMap.get(layoutFile.getName());
            }
            else
            {
                layoutFileList = new ArrayList<LayoutFileId>();
            }
            layoutFileList.add(layoutFileId);
            allIdsMap.put(layoutFile.getName(), layoutFileList);
        }
    }

    private void getLayoutIds(XMLElement xmlElement, Node elementNode, LayoutFileId layoutFileId)
    {
        NamedNodeMap map = elementNode.getAttributes();
        Node attribute = map.getNamedItem(LayoutConstants.ANDROID_ID_ATTRIBUTE);
        if (attribute != null)
        {
            String id = attribute.getTextContent();
            id = CheckerUtils.getIdValue(id.trim());
            if (id.length() > 0)
            {
                layoutFileId.addId(elementNode, id);
            }
        }

        //visit children
        NodeList nodeList = elementNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                getLayoutIds(xmlElement, node, layoutFileId);
            }
        }
    }
}
