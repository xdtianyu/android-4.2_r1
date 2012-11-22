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

package com.motorolamobility.preflighting.samplechecker.androidlabel.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.ElementUtils;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.StringsElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.samplechecker.androidlabel.AndroidLabelActivator;
import com.motorolamobility.preflighting.samplechecker.androidlabel.i18n.AndroidLabelCheckerNLS;

/**
 * This Condition verifies whether a given text is a substring of the Android Application Label.
 * <br><br>
 * For this condition, a text is entered using the parameter labelText. In order to
 * have no warnings reported, all resources which the Android Application
 * Label points out, must have this parameter as a part of its name.
 */
public class CorrectTextInLabelCondition extends Condition implements ICondition
{
    /**
     * Represents the AndroidManifest.xml application node.
     */
    private static final String MANIFEST_TAG_APPLICATION = "application"; //$NON-NLS-1$

    /**
     * Represents the AndroidManifest.xml label property name.
     */
    private static final String MANIFEST_TAG_LABEL = "android:label"; //$NON-NLS-1$

    /**
     * Represents the prefix for String resources on the AndroidManifest.xml
     */
    private static final String ANDROID_STRING_IDENTIFIER = "@string/"; //$NON-NLS-1$

    private String parameterText;

    /**
     * Executes the {@link AndroidLabelChecker} validations, which are:
     * <ul>
     *  <li>The entered label must be contained in the default resource.</li>
     *  <li>The entered label must be contained in all alternative resources.</li>
     *  <li>In case the Application Label is declared inside AndroidManifest.xml, the entered label is contained in it.</li>
     * </ul>
     * 
     * @param data Data Structure of the Android Project. It serves for APKs and Android Projects.
     * @param deviceSpecs Device specifications for phones.
     * @param platformRules Rules and standards for the Android APi being used.
     * @param valManagerConfig App Validator Manager configuration.
     * @param results The results which will be returned from the validation performed in this method.
     * 
     * @throws PreflightingCheckerException Exception thrown when there are unexpected problems validating
     * the Android Application.
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        // get the label from AndroidManifest.xml
        XMLElement document = data.getManifestElement();
        Document manifestDoc = document.getDocument();
        Node labelNode = getLabelNode(manifestDoc);
        String androidLabelText = labelNode.getNodeValue();

        // get entered parameter
        AndroidLabelChecker checker = (AndroidLabelChecker) getChecker();
        parameterText =
                checker.getParameters().get(AndroidLabelChecker.PARAMETER_LABEL_TEXT).getValue();

        if (parameterText == null)
        {
            PreflightingLogger
                    .debug("Variable parameterText is null. Check if parameter \"labelText\" of checker androidLabel is being set.");
        }

        // handle case where the label is a resource identifier
        if (androidLabelText.startsWith(ANDROID_STRING_IDENTIFIER))
        {
            analyzeLocalizedLabel(data, valManagerConfig, results, document, androidLabelText);
        }
        else
        {
            // the label is a hard coded text, check the string itself
            analyzeHardcodedLabel(valManagerConfig, results, document, labelNode, androidLabelText);
        }
    }

    /**
     * Verify if the label value contains the parameterText. 
     */
    private void analyzeHardcodedLabel(ValidationManagerConfiguration valManagerConfig,
            ValidationResult results, XMLElement document, Node labelNode, String androidLabelText)
    {
        if ((parameterText != null)
                && !androidLabelText.toLowerCase().contains(parameterText.toLowerCase()))
        {
            List<Integer> lineList = new ArrayList<Integer>();
            int lineNumber = document.getNodeLineNumber(labelNode);
            if (lineNumber > 0) //Verify if line number is available.
            {
                lineList.add(lineNumber);
            }

            // create validation result - error structure
            ValidationResultData result =
                    createValidationResult(
                            AndroidLabelCheckerNLS.bind(
                                    AndroidLabelCheckerNLS.CorrectTextInLabelCondition_LabelNotContainedAndroidXML,
                                    parameterText),
                            AndroidLabelCheckerNLS
                                    .bind(AndroidLabelCheckerNLS.CorrectTextInLabelCondition_AddTextInLabel,
                                            parameterText), valManagerConfig, document,
                            parameterText, lineList);

            // add created result to the results list
            results.addValidationResult(result);
        }
    }

    /*
     * Verify if all string resources (from all locales) referred by the label resource identifier
     * contains the parameterText
     */
    private void analyzeLocalizedLabel(ApplicationData data,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results,
            XMLElement document, String androidLabelText)
    {
        // get resource identifier
        String resId = androidLabelText.replace(ANDROID_STRING_IDENTIFIER, ""); //$NON-NLS-1$

        // get resource folder
        List<Element> folderResElements =
                ElementUtils.getElementByType(data.getRootElement(), Type.FOLDER_RES);
        ResourcesFolderElement resFolder =
                folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements.get(0)
                        : null;

        // check default locale
        StringsElement defaultElements = resFolder.getDefaultValuesElement();
        if (defaultElements != null)
        {
            Object value = defaultElements.getValue(resId);
            androidLabelText = (value != null) ? (String) value : ""; //$NON-NLS-1$

            // execute the checker condition - the entered text must be within the label
            if ((parameterText != null)
                    && !androidLabelText.toLowerCase().contains(parameterText.toLowerCase()))
            {
                // create validation result - error structure
                ValidationResultData result =
                        createValidationResult(
                                AndroidLabelCheckerNLS.bind(
                                        AndroidLabelCheckerNLS.CorrectTextInLabelCondition_LabelReferedAndroidXML,
                                        parameterText),
                                AndroidLabelCheckerNLS
                                        .bind(AndroidLabelCheckerNLS.CorrectTextInLabelCondition_LabelReferedAndroidXMLDefaultLocale,
                                                parameterText), valManagerConfig, document,
                                parameterText, new ArrayList<Integer>());

                // add created result to the results list
                results.addValidationResult(result);
            }
        }

        // check non-default locales
        if ((resFolder != null) && (resFolder.getAvailableLocales() != null)
                && (resFolder.getAvailableLocales().size() > 0))
        {
            for (Locale locale : resFolder.getAvailableLocales())
            {
                String localeText =
                        locale.getLanguage()
                                + ((locale.getCountry() != null)
                                        && (locale.getCountry().length() > 0)
                                        ? "_" + locale.getCountry() : ""); //$NON-NLS-1$ //$NON-NLS-2$

                // get the android label for each locale
                StringsElement stringsElement = resFolder.getValuesElement(locale);

                Object value = stringsElement.getValue(resId);
                androidLabelText = (value != null) ? (String) value : ""; //$NON-NLS-1$

                // execute the checker condition - the entered text must be within the label
                if (!androidLabelText.toLowerCase().contains(parameterText.toLowerCase()))
                {
                    // create validation result - error structure
                    ValidationResultData result =
                            createValidationResult(
                                    AndroidLabelCheckerNLS.bind(
                                            AndroidLabelCheckerNLS.CorrectTextInLabelCondition_LabelReferedAndroidXMLLocale,
                                            parameterText, localeText),
                                    AndroidLabelCheckerNLS
                                            .bind(AndroidLabelCheckerNLS.CorrectTextInLabelCondition_AddLabelAndroidXMLLocale,
                                                    parameterText, localeText), valManagerConfig,
                                    document, parameterText, new ArrayList<Integer>());

                    // add created result to the results list
                    results.addValidationResult(result);
                }
            }
        }
    }

    /**
     * In order to execute the checker, first several conditions must be verified.
     * <ul>
     *  <li>There must be an AndroidManifest.xml file in the Android Project.</li>
     *  <li>There must be a label in the AndroidManifest.xml file.</li>
     *  <li>There must be a value for the parameter labelText.</li>
     * </ul>
     * 
     * @param data Data structure holding all files, classes and resources of the
     * APK or Project.
     * @param deviceSpecs List of device specifications.
     * 
     * @return Returns the {@link IStatus} which states whether the Checker
     * can be run.
     * 
     * @throws PreflightingCheckerException Exception thrown in case there is any problem
     * verifying the conditions.
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        // first check the manifest file status
        CanExecuteConditionStatus status =
                CheckerUtils.isAndroidManifestFileExistent(data, getId());

        // there must be parameters AndroidLabelChecker.PARAMETER_LABEL_TEXT set
        String labelParameterValue =
                getChecker().getParameters().get(AndroidLabelChecker.PARAMETER_LABEL_TEXT)
                        .getValue();
        if (labelParameterValue == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.INFO, AndroidLabelActivator.PLUGIN_ID,
                            AndroidLabelCheckerNLS.CorrectTextInLabelCondition_NoEnteredParamWarn);
        }

        if (status.getSeverity() != IStatus.ERROR)
        {
            // there must be a parameter set
            AndroidLabelChecker checker = (AndroidLabelChecker) getChecker();
            if ((checker.getParameters() != null)
                    && checker.getParameters()
                            .containsKey(AndroidLabelChecker.PARAMETER_LABEL_TEXT))
            {

                XMLElement document = data.getManifestElement();
                Document manifestDoc = document.getDocument();

                // there must be a label in order to allow the checker execution
                Node labelNode = getLabelNode(manifestDoc);
                if ((labelNode != null) && (labelNode.getNodeValue() != null))
                {
                    status = new CanExecuteConditionStatus(IStatus.OK, getChecker().getId(), ""); //$NON-NLS-1$
                }
                else
                {
                    status =
                            new CanExecuteConditionStatus(
                                    IStatus.ERROR,
                                    AndroidLabelActivator.PLUGIN_ID,
                                    AndroidLabelCheckerNLS.CorrectTextInLabelCondition_AndroidXMlMustHaveLabelRunChecker);
                }
            }
            else
            {
                status =
                        new CanExecuteConditionStatus(
                                IStatus.ERROR,
                                AndroidLabelActivator.PLUGIN_ID,
                                AndroidLabelCheckerNLS.CorrectTextInLabelCondition_ExecuteCheckerEnterLabelText);
            }
        }

        status.setConditionId(getId());

        return status;
    }

    /**
     * Create the {@link ValidationResultData} which represents the error that
     * has occurred during the validation. It will be reported to the user.
     * 
     * @param issueDescription The description of the error.
     * @param quickFixSuggestion The quick-fix-suggestion for the error.
     * @param valManagerConfig App Validator Manager configuration.
     * @param document AndroidManifest.xml in a {@link Document} object.
     * @param parameterText The text entered by the user as a parameter.
     * @param lineList The list of lines where the error has occurred.
     * 
     * @return Returns the {@link ValidationResultData} which holds the error
     * to be displayed by the App Validator.
     */
    private ValidationResultData createValidationResult(String issueDescription,
            String quickFixSuggestion, ValidationManagerConfiguration valManagerConfig,
            XMLElement document, String parameterText, List<Integer> lineList)
    {
        ValidationResultData result = new ValidationResultData();
        result.setConditionID(getId());
        result.addFileToIssueLines(document.getFile(), lineList);
        result.setIssueDescription(issueDescription);
        result.setQuickFixSuggestion(quickFixSuggestion);
        result.setInfoURL("http://developer.motorola.com/docstools/library/motodev-app-validator/#androidLabel-findTextInLabel"); //$NON-NLS-1$
        result.setSeverity(getSeverityLevel());
        return result;
    }

    /**
     * Get the label {@link Node} from the AndroidManifext.xml file
     * represented as a {@link Document}.
     * <br>
     * In case nothing is found, <code>null</code> is returned.
     * 
     * @param manifestDoc AdnroidManifest.xml file as a {@link Document} where
     * the label will be sought.
     * 
     * @return Returns the AndroidManifest.xml label {@link Node}.
     */
    private Node getLabelNode(Document manifestDoc)
    {
        final int APPLICATION_NODE_INDEX = 0;

        Node labelNode = null;

        NodeList applicationNodes = manifestDoc.getElementsByTagName(MANIFEST_TAG_APPLICATION);

        // there must be one application note, get it
        Node applicationNode = applicationNodes.item(APPLICATION_NODE_INDEX);
        if (applicationNode != null)
        {
            // get label node
            labelNode = applicationNode.getAttributes().getNamedItem(MANIFEST_TAG_LABEL);
        }

        return labelNode;
    }
}
