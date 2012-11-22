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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.permissionfeature.Feature;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.utils.ManifestConstants;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class PermissionImpliedFeaturesCondition extends Condition implements ICondition
{

    /**
     * Checks each uses-permission declared into XML if there are all the implied uses-feature declared 
     * @param platformRules rules to get permission to feature mapping
     * @param result 
     * @param manifestDoc 
     * @throws PreflightingCheckerException
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        XMLElement manifestElement = data.getManifestElement();
        Document manifestDoc = manifestElement.getDocument();
        PlatformRules platformRules = PlatformRules.getInstance();

        //collecting set of declared features
        Set<Feature> featuresDeclared = new HashSet<Feature>();
        NodeList featureLst = manifestDoc.getElementsByTagName(ManifestConstants.USES_FEATURE_TAG);
        for (int j = 0; j < featureLst.getLength(); j++)
        {
            Node featureNode = featureLst.item(j);
            NamedNodeMap featureMap = featureNode.getAttributes();
            Node featureAtr = featureMap.getNamedItem(ManifestConstants.ANDROID_NAME_ATTRIBUTE); //$NON-NLS-1$
            if ((featureAtr != null) && featureAtr.getNodeValue().trim().length() != 0)
            {
                Feature featDecl = new Feature(featureAtr.getNodeValue());
                featuresDeclared.add(featDecl);
            }
        }

        //comparing with features required        
        NodeList permissionsLst =
                manifestDoc.getElementsByTagName(ManifestConstants.USES_PERMISSION_ATTRIBUTE);
        for (int i = 0; i < permissionsLst.getLength(); i++)
        {
            Node permissionNode = permissionsLst.item(i);
            NamedNodeMap permissionMap = permissionNode.getAttributes();
            Node permissionAtr =
                    permissionMap.getNamedItem(ManifestConstants.ANDROID_NAME_ATTRIBUTE); //$NON-NLS-1$
            try
            {
                if ((permissionAtr != null) && !permissionAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
                {
                    String permissionId = permissionAtr.getNodeValue();
                    Set<Feature> featuresRequired =
                            platformRules.getImpliedFeaturesSet(permissionId);
                    //check if these required features were declared using <uses-feature>
                    if ((featuresRequired != null)
                            && !featuresDeclared.containsAll(featuresRequired))
                    {
                        //there are missing features required
                        int currentIssuedLine = manifestElement.getNodeLineNumber(permissionNode);
                        //missing set = features required - features declared
                        Set<Feature> missingDeclaredFeatures =
                                new HashSet<Feature>(featuresRequired);
                        missingDeclaredFeatures.removeAll(featuresDeclared);
                        String issueDescription =
                                CheckerNLS
                                        .bind(CheckerNLS.AndroidMarketFiltersChecker_permissionToImpliedFeatures_Issue,
                                                new Object[]
                                                {
                                                        missingDeclaredFeatures, permissionId
                                                });
                        ValidationResultData resultData =
                                new ValidationResultData(
                                        CheckerUtils.createFileToIssuesMap(
                                                manifestElement.getFile(), currentIssuedLine),
                                        getSeverityLevel(),
                                        issueDescription,
                                        CheckerNLS.AndroidMarketFiltersChecker_permissionToImpliedFeatures_Suggestion,
                                        getId());
                        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker()
                                .getId(), getId(), valManagerConfig));
                        resultData.setPreview(XmlUtils.getXMLNodeAsString(permissionNode, false));
                        
                        resultData.setMarkerType(getMarkerType());
                        List<String> missingFeaturesIds = new ArrayList<String>(missingDeclaredFeatures.size());
                        for(Feature missingFeature : missingDeclaredFeatures)
                        {
                            missingFeaturesIds.add(missingFeature.toString());
                        }
                        resultData.appendExtra(missingFeaturesIds);
                        results.addValidationResult(resultData);
                    }
                }
            }
            catch (DOMException e)
            {
                // Error retrieving attribute
                throw new PreflightingCheckerException(
                        CheckerNLS.bind(
                                CheckerNLS.AndroidMarketFiltersChecker_Exception_Getting_Manifest_Attribute,
                                ManifestConstants.ANDROID_NAME_ATTRIBUTE), e);
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
