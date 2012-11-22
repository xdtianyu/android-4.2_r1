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

import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class MissingIconLabelCondition extends Condition
{

    private XMLElement manifestElement;

    private ValidationManagerConfiguration valManagerConfig;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        this.valManagerConfig = valManagerConfig;
        manifestElement = data.getManifestElement();
        Document manifestDoc = manifestElement.getDocument();
        NodeList appLst = manifestDoc.getElementsByTagName(ManifestConstants.APPLICATION_TAG); //$NON-NLS-1$

        for (int i = 0; i < appLst.getLength(); i++)
        {
            Node applicationNode = appLst.item(i);
            NamedNodeMap map = applicationNode.getAttributes();
            
            checkIfAtributeIsMissing(
                    results,
                    applicationNode,
                    map,
                    ManifestConstants.ANDROID_ICON_ATTRIBUTE);


            checkIfAtributeIsMissing(
                    results,
                    applicationNode,
                    map,
                    ManifestConstants.ANDROID_LABEL_ATTRIBUTE);

        }

    }

    /**
     * Checks if the attribute name in the XML is not existent (null) or value
     * empty
     * 
     * @param map
     *            list of attribute from one XML element
     * @throws PreflightingCheckerException
     */
    private void checkIfAtributeIsMissing(ValidationResult results, Node manifestNode,
            NamedNodeMap map, String attributeName) throws PreflightingCheckerException
    {
        Node atr = map.getNamedItem(attributeName); //$NON-NLS-1$
        try
        {
            if ((atr == null) || atr.getNodeValue().trim().length() == 0)
            {
                int currentIssuedLine = manifestElement.getNodeLineNumber(manifestNode);
                ValidationResultData resultData =
                        new ValidationResultData(
                                CheckerUtils.createFileToIssuesMap(manifestElement.getFile(),
                                        currentIssuedLine),
                                getSeverityLevel(),
                                CheckerNLS
                                        .bind(CheckerNLS.AndroidMarketFiltersChecker_missingManifestIconOrLabel_Issue,
                                                attributeName),
                                CheckerNLS
                                        .bind(CheckerNLS.AndroidMarketFiltersChecker_missingManifestIconOrLabel_Suggestion,
                                                attributeName), getId());
                resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                        getId(), valManagerConfig));
                resultData.setPreview(XmlUtils.getXMLNodeAsString(manifestNode, false));
                results.addValidationResult(resultData);
            }

        }
        catch (DOMException e)
        {
            // Error retrieving attribute
            throw new PreflightingCheckerException(CheckerNLS.bind(
                    CheckerNLS.AndroidMarketFiltersChecker_Exception_Getting_Manifest_Attribute,
                    attributeName), e);
        }
    }

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

}
