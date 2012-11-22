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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.motorolamobility.preflighting.core.devicelayoutspecification.Device;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.permissionfeature.Feature;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Check if application requires any feature that is not available on a given device.
 */
public class UnsupportedFeaturesConditions extends Condition implements ICondition
{

    private static final String USES_FEATURE = "uses-feature"; //$NON-NLS-1$

    private static final String USES_PERMISSION = "uses-permission"; //$NON-NLS-1$

    private static final String ANDROID_NAME = "android:name"; //$NON-NLS-1$

    private static final String ANDROID_REQUIRED = "android:required"; //$NON-NLS-1$

    /**
     * Elements to validate
     */
    private XMLElement manifestElement;

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
        checkUnsupportedFeatures(PlatformRules.getInstance(), deviceSpecs, valManagerConfig,
                results);
    }

    /**
     * Check if there is features (declared or implied) that are unsupported by a device
     * @param platformRules
     * @param deviceSpecs
     * @param valManagerConfig
     * @param resultList
     * @throws PreflightingCheckerException
     */
    private void checkUnsupportedFeatures(PlatformRules platformRules,
            List<DeviceSpecification> deviceSpecs, ValidationManagerConfiguration valManagerConfig,
            ValidationResult results) throws PreflightingCheckerException
    {
        Document manifestDoc = manifestElement.getDocument();
        //collecting set of declared features
        Map<String, Feature> featuresDeclared = new HashMap<String, Feature>();
        for (DeviceSpecification deviceSpec : deviceSpecs)
        {
            Device device = deviceSpec.getDeviceInfo();
            checkIssuesForDeclaredFeatures(valManagerConfig, results, manifestDoc,
                    featuresDeclared, device);

            checkIssuesForFeaturesImplied(platformRules, valManagerConfig, results, manifestDoc,
                    featuresDeclared, device);
        }

    }

    /**
     * Checks unsupported features for a device that were declared
     * @param platformRules
     * @param valManagerConfig
     * @param resultList
     * @param manifestDoc
     * @param featuresDeclared
     * @param device
     * @throws PreflightingCheckerException
     */
    private void checkIssuesForFeaturesImplied(PlatformRules platformRules,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results,
            Document manifestDoc, Map<String, Feature> featuresDeclared, Device device)
            throws PreflightingCheckerException
    {
        int currentIssuedLine;
        String preview;
        //collecting set of declared features derived from permissions       
        NodeList permissionsLst = manifestDoc.getElementsByTagName(USES_PERMISSION);
        for (int i = 0; i < permissionsLst.getLength(); i++)
        {
            Node permissionNode = permissionsLst.item(i);
            NamedNodeMap permissionMap = permissionNode.getAttributes();
            Node permissionAtr = permissionMap.getNamedItem(ANDROID_NAME); //$NON-NLS-1$
            Node permissionRequiredAtr = permissionMap.getNamedItem(ANDROID_REQUIRED);//$NON-NLS-1$
            try
            {
                boolean required = true;
                if (permissionRequiredAtr != null)
                {
                    required = permissionRequiredAtr.getNodeValue().trim().equals("true"); //$NON-NLS-1$                   
                }
                if (required && (permissionAtr != null)
                        && !permissionAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
                {
                    String permissionId = permissionAtr.getNodeValue();
                    Set<Feature> featuresImplied =
                            platformRules.getImpliedFeaturesSet(permissionId);

                    for (Feature impliedFeature : featuresImplied)
                    {
                        if (!device.getSupportedFeatures().isEmpty()
                                && !device.getSupportedFeatures().contains(impliedFeature))
                        {
                            //if device has features supported declared (old devices.xml won't have this declaration)
                            //and feature was not found
                            Feature decl = featuresDeclared.get(impliedFeature.getId());
                            if ((decl == null) || decl.isRequired())
                            {
                                //permission => implied feature unsupported : raise issue
                                //because it is not marked as not required
                                currentIssuedLine =
                                        manifestElement.getNodeLineNumber(permissionNode);
                                preview = XmlUtils.getXMLNodeAsString(permissionNode, false);

                                //feature declared but unsupported => raise warning
                                ValidationResultData resultData = new ValidationResultData();
                                resultData.setSeverity(getSeverityLevel());
                                resultData.setConditionID(getId());
                                resultData
                                        .setIssueDescription(CheckerNLS
                                                .bind(CheckerNLS.DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_IMPLIED_ISSUE_DESCRIPTION,
                                                        new String[]
                                                        {
                                                                permissionId,
                                                                impliedFeature.getId(),
                                                                device.getName()
                                                        }));
                                String fixSuggestionMessage =
                                        (CheckerNLS
                                                .bind(CheckerNLS.DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_FIX_SUGGESTION,
                                                        new String[]
                                                        {
                                                            impliedFeature.getId()
                                                        }));
                                resultData.setQuickFixSuggestion(fixSuggestionMessage);
                                resultData.setMarkerType(getMarkerType());
                                resultData.appendExtra(impliedFeature.getId());

                                ArrayList<Integer> lines = new ArrayList<Integer>();
                                lines.add(currentIssuedLine);
                                resultData.addFileToIssueLines(manifestElement.getFile(), lines);
                                resultData.setInfoURL(ConditionUtils.getDescriptionLink(
                                        getChecker().getId(), getId(), valManagerConfig));
                                resultData.setPreview(preview);
                                results.addValidationResult(resultData);

                            }

                        }
                    }
                }
            }
            catch (DOMException e)
            {
                // Error retrieving attribute
                throw new PreflightingCheckerException(
                        CheckerNLS.bind(
                                CheckerNLS.AndroidMarketFiltersChecker_Exception_Getting_Manifest_Attribute,
                                ANDROID_NAME), e);
            }
        }
    }

    /**
     * Checks unsupported features for a device (permissions that imply in features)
     * @param valManagerConfig
     * @param resultList
     * @param manifestDoc
     * @param featuresDeclared
     * @param device
     */
    private void checkIssuesForDeclaredFeatures(ValidationManagerConfiguration valManagerConfig,
            ValidationResult results, Document manifestDoc, Map<String, Feature> featuresDeclared,
            Device device)
    {
        int currentIssuedLine;
        String preview;
        NodeList featureLst = manifestDoc.getElementsByTagName(USES_FEATURE);
        for (int j = 0; j < featureLst.getLength(); j++)
        {
            Node featureNode = featureLst.item(j);
            NamedNodeMap featureMap = featureNode.getAttributes();
            Node featureAtr = featureMap.getNamedItem(ANDROID_NAME);
            Node featureRequiredAtr = featureMap.getNamedItem(ANDROID_REQUIRED);//$NON-NLS-1$
            if ((featureAtr != null) && !featureAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
            {
                Feature featDecl = new Feature(featureAtr.getNodeValue());
                boolean required = true;
                if (featureRequiredAtr != null)
                {
                    required = featureRequiredAtr.getNodeValue().trim().equals("true"); //$NON-NLS-1$
                    featDecl.setRequired(required);
                }
                featuresDeclared.put(featDecl.getId(), featDecl);
                if (required)
                {
                    if (!device.getSupportedFeatures().isEmpty()
                            && !device.getSupportedFeatures().contains(featDecl))
                    {
                        //if device has features supported declared (old devices.xml won't have this declaration)
                        //and feature was not found

                        //feature declared but unsupported => raise issue
                        currentIssuedLine = manifestElement.getNodeLineNumber(featureNode);
                        preview = XmlUtils.getXMLNodeAsString(featureNode, false);

                        ValidationResultData resultData = new ValidationResultData();
                        resultData.setSeverity(getSeverityLevel());
                        resultData.setConditionID(getId());
                        resultData
                                .setIssueDescription(CheckerNLS
                                        .bind(CheckerNLS.DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_DECLARED_ISSUE_DESCRIPTION,
                                                new String[]
                                                {
                                                        featDecl.getId(), device.getName()
                                                }));
                        String fixSuggestionMessage =
                                (CheckerNLS
                                        .bind(CheckerNLS.DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_FIX_SUGGESTION,
                                                new String[]
                                                {
                                                    featDecl.getId()
                                                }));
                        resultData.setQuickFixSuggestion(fixSuggestionMessage);
                        resultData.setMarkerType(getMarkerType());
                        resultData.appendExtra(featDecl.getId());

                        ArrayList<Integer> lines = new ArrayList<Integer>();
                        lines.add(currentIssuedLine);
                        resultData.addFileToIssueLines(manifestElement.getFile(), lines);
                        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker()
                                .getId(), getId(), valManagerConfig));
                        resultData.setPreview(preview);
                        results.addValidationResult(resultData);
                    }
                }
            }
        }
    }

}
