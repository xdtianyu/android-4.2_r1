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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
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
 * Layout checker condition that verifies if any id declared in a given layout configuration
 * is missing on another configurations for the declaring layout 
 */
public class MissingIdCondition extends Condition implements ICondition
{

    /*
     * This map keeps all the keys for a given layout e.g. main.xml. 
     * It is used to look for any missing ID.
     */
    private HashMap<String, GlobalLayoutId> globalMap;

    private ValidationManagerConfiguration valManagerConfig;

    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, "");
        status.setConditionId(getId());
        return status;
    }

    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        List<XMLElement> layoutList = data.getLayoutElements();
        if (layoutList != null)
        {
            this.valManagerConfig = valManagerConfig;

            globalMap = new HashMap<String, GlobalLayoutId>();

            //This map stores the IDs of each layout file.
            //An instance of LayoutFileIDs is created for each file
            //and the map associates it to the given layout.
            //Hence, two files of different configurations but with the same name e.g. main.xml
            //will be associated under the key "main.xml" on the map.
            HashMap<String, List<LayoutFileId>> mainMap = new HashMap<String, List<LayoutFileId>>();

            for (XMLElement element : layoutList)
            {
                String layoutName = element.getFile().getName();

                //initialize globalMap
                if (!globalMap.containsKey(layoutName))
                {
                    globalMap.put(layoutName, new GlobalLayoutId());
                }

                //search for IDs and saves it in the map
                HashSet<String> idsList = retriveLayoutIDs(layoutName, element.getDocument());

                if (mainMap.keySet().contains(layoutName))
                {
                    mainMap.get(layoutName).add(new LayoutFileId(element.getFile(), idsList));
                }
                else
                {
                    List<LayoutFileId> layoutArray = new ArrayList<LayoutFileId>();
                    layoutArray.add(new LayoutFileId(element.getFile(), idsList));
                    mainMap.put(layoutName, layoutArray);
                }
            }

            //create the results
            checkForMissingLayoutIDs(mainMap, results);
        }
    }

    /*
     * Analyze the generated lists and create the results.
     */
    private void checkForMissingLayoutIDs(HashMap<String, List<LayoutFileId>> mainMap,
            ValidationResult results)
    {
        //for each layout e.g. main.xml
        for (String key : mainMap.keySet())
        {
            Set<String> currentCompleteIDList = new HashSet<String>();
            //this is the list of all IDs found for the given layout
            final Set<String> completeIDList = globalMap.get(key).getIdsList();

            //for each file e.g. any main.xml found inside the layout's directories 
            for (LayoutFileId layout : mainMap.get(key))
            {
                currentCompleteIDList.addAll(completeIDList);

                //check if there are missing keys
                if (!layout.getIdsList().containsAll(currentCompleteIDList))
                {
                    currentCompleteIDList.removeAll(layout.getIdsList());

                    //create the result for each key
                    for (String missingKey : currentCompleteIDList)
                    {
                        ValidationResultData data = new ValidationResultData();
                        data.addFileToIssueLines(layout.getLayoutFile(), new ArrayList<Integer>());
                        data.setConditionID(getId());
                        data.setSeverity(getSeverityLevel());
                        data.setQuickFixSuggestion(CheckerNLS.LayoutChecker_MissingKeyFixSuggestion);
                        data.setPreview(XmlUtils.getXMLNodeAsString(
                                globalMap.get(key).getNode(missingKey), false));
                        data.setIssueDescription(CheckerNLS.bind(
                                CheckerNLS.LayoutChecker_MissingKeyWarningMessage, missingKey));
                        data.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                                getId(), valManagerConfig));
                        results.addValidationResult(data);
                    }
                }
                currentCompleteIDList.clear();
            }
        }
    }

    /*
     * start the visit by the root node
     */
    private HashSet<String> retriveLayoutIDs(String layoutName, Document document)
    {
        //this is the current layout ID list
        HashSet<String> idsList = new HashSet<String>();
        Element rootElem = document.getDocumentElement();

        String rootId = rootElem.getAttribute(LayoutConstants.ANDROID_ID_ATTRIBUTE);
        rootId = CheckerUtils.getIdValue(rootId.trim());
        if (rootId.length() > 0)
        {
            //add the ID to local and global list
            idsList.add(rootId);
            globalMap.get(layoutName).addID(rootId, rootElem);
        }

        //visit children
        NodeList nodeList = rootElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                visitNode(layoutName, node, idsList);
            }
        }

        return idsList;
    }

    /**
     * Visit nodes recursively, retrieving its IDs.
     */
    private void visitNode(String layoutName, Node node, HashSet<String> idsList)
    {
        NamedNodeMap map = node.getAttributes();
        Node attribute = map.getNamedItem(LayoutConstants.ANDROID_ID_ATTRIBUTE);

        if (attribute != null)
        {
            String id = attribute.getTextContent();
            id = CheckerUtils.getIdValue(id.trim());
            if (id.length() > 0)
            {
                //add the ID to local and global list
                idsList.add(id);
                globalMap.get(layoutName).addID(id, node);
            }
        }

        //visit children
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node childNode = nodeList.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE)
            {
                visitNode(layoutName, childNode, idsList);
            }
        }
    }

}
