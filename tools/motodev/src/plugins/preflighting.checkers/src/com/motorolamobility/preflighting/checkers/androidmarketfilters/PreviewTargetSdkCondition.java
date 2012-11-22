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
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class PreviewTargetSdkCondition extends Condition implements ICondition
{

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        XMLElement manifestElement = data.getManifestElement();
        Document manifestDoc = manifestElement.getDocument();
        NodeList usesSdkList = manifestDoc.getElementsByTagName(ManifestConstants.USES_SDK_TAG);
        if (usesSdkList.getLength() > 0)
        {
            // Check if attribute exists.
            for (int i = 0; i < usesSdkList.getLength(); i++)
            {
                Node usesSdkNode = usesSdkList.item(i);
                NamedNodeMap map = usesSdkNode.getAttributes();
                Node targetSdkNode =
                        map.getNamedItem(ManifestConstants.TARGET_SDK_VERSION_ATTRIBUTE); //$NON-NLS-1$
                try
                {
                    if (targetSdkNode != null)
                    {
                        String targetSdkStr = targetSdkNode.getNodeValue().trim();
                        Integer.valueOf(targetSdkStr); //Try to get the int value. It the exception is thrown issue the warning.
                    }
                }
                catch (NumberFormatException e)
                {
                    int currentIssuedLine = manifestElement.getNodeLineNumber(usesSdkNode);
                    ValidationResultData resultData =
                            new ValidationResultData(
                                    CheckerUtils.createFileToIssuesMap(manifestElement.getFile(),
                                            currentIssuedLine),
                                    getSeverityLevel(),
                                    CheckerNLS.AndroidMarketFiltersChecker_targetSdkIsPreview_Issue,
                                    CheckerNLS.AndroidMarketFiltersChecker_targetSdkIsPreview_Suggestion,
                                    getId());
                    resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                            getId(), valManagerConfig));
                    resultData.setPreview(XmlUtils.getXMLNodeAsString(usesSdkNode, false));
                    results.addValidationResult(resultData);
                }
            }
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
