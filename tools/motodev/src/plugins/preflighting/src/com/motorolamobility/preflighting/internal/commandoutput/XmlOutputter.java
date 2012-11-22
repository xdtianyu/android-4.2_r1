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
package com.motorolamobility.preflighting.internal.commandoutput;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.internal.PreflightingPlugin;
import com.motorolamobility.preflighting.output.AbstractOutputter;

public class XmlOutputter extends AbstractOutputter
{

    protected Document document = null;

    private static final String XML_TAG_APP_VALIDATOR = "AppValidator";

    private static final String XML_TAG_APPLICATION = "Application";

    private static final String XML_TAG_ERROR = "Error";

    private static final String XML_TAG_FATAL_ERROR = "FatalError";

    private static final String XML_TAG_WARNING = "Warning";

    private static final String XML_TAG_RESOURCE = "Resource";

    private static final String XML_TAG_LINE = "Line";

    private static final String XML_TAG_SUGGESTION = "Suggestion";

    private static final String XML_ATTRIBUTE_CHECKER_ID = "checker_id";

    private static final String XML_ATTRIBUTE_CONDITION_ID = "condition_id";

    private static final String XML_ATTRIBUTE_INFO_URL = "info_url";

    private static final String XML_ATTRIBUTE_PATH = "path";

    private static final String XML_ATTRIBUTE_APP_NAME = "app_name";

    private static final String XML_TAG_DESCRIPTION = "Description";

    private static final String XML_TAG_PREVIEW = "Preview";

    private static final String XML_ATTRIBUTE_APPVALIDATOR_VERSION = "version";

    private static final String XML_ATTRIBUTE_APPLICATION_VERSION = "app_version";

    private static final String XML_ATTRIBUTE_MOTODEV_LINK = "description_url";

    private static final String XML_CHECKER_STATUS_VALUE_FAIL = "Failed";

    private static final String XML_CHECKER_STATUS_VALUE_DISABLED = "Disabled";

    private static final String XML_CHECKER_STATUS_VALUE_OK = "Executed";

    private static final String XML_ATTRIBUTE_MESSAGE = "message";

    private static final String XML_ATTRIBUTE_STATUS = "status";

    private static final String XML_TAG_CHECKER_STATUS = "CheckerStatus";

    private static final String XML_TAG_EXECUTION_REPORT = "ExecutionReport";

    //TODO change tag name
    private static final String XML_TAG_APP_VALIDATOR_EXECUTION_REPORT = "ExecutionReport";

    private static final String XML_TAG_MESSAGES = "Messages";

    /**
     * Writes to file xml or text in the following format: <br>
     * &lt;type (Error/Warning/Info)&gt; &lt;number files involved (Java, XML)&gt; &lt;filePath1 (without OS specific characters)&gt;:&lt;linePath1&gt; ... &lt;filePathN&gt;:&lt;linePathN&gt;  &lt;description&gt;
     * @throws PreflightingToolException 
     */
    @Override
    public void print(ApplicationValidationResult result, OutputStream stream,
            List<Parameter> parameters) throws PreflightingToolException
    {
        initializeParams(parameters);

        try
        {
            Element appValElem = createRootNode();

            Element applicationElem = document.createElement(XML_TAG_APPLICATION);
            applicationElem.setAttribute(XML_ATTRIBUTE_APP_NAME, getApplicationFile().getName());
            applicationElem.setAttribute(XML_ATTRIBUTE_APPLICATION_VERSION,
                    String.valueOf(result.getVersion()));

            generateCustomApplicationNodes(applicationElem, result, parameters);
            appValElem.appendChild(applicationElem);

            Element messagesElement = document.createElement(XML_TAG_MESSAGES);
            applicationElem.appendChild(messagesElement);

            //create result nodes and append them to document
            generateResultNodes(messagesElement, result.getResults());

            generateExecutionReport(applicationElem, result.getExecutionStatus());

            //create XML output
            XmlUtils.printXMLFormat(document);
        }
        catch (Exception e)
        {
            PreflightingLogger.error(getClass(), PreflightingNLS.TextOutputter_IOExceptionMessage
                    + e.getMessage());
            throw new PreflightingToolException(
                    PreflightingNLS.XMLOutputter_PrintResultsErrorMessage, e);
        }
    }

    /*
     * Generates the nodes for the result list
     */
    private void generateResultNodes(Element rootElement, List<ValidationResult> result)
    {
        for (ValidationResult checker : result)
        {
            for (ValidationResultData data : checker.getValidationResult())
            {
                if (SEVERITY.OK.compareTo(data.getSeverity()) != 0)
                {
                    Element issueElement = createIssueNode(data.getSeverity());
                    issueElement.setAttribute(XML_ATTRIBUTE_CHECKER_ID, checker.getCheckerId());
                    issueElement.setAttribute(XML_ATTRIBUTE_CONDITION_ID, data.getConditionID());
                    if (data.getInfoURL() != null)
                    {
                        issueElement.setAttribute(XML_ATTRIBUTE_INFO_URL, data.getInfoURL());
                    }
                    Element descriptionElement = createDescriptionNode(data.getIssueDescription());
                    issueElement.appendChild(descriptionElement);

                    for (File currentFile : data.getFileToIssueLines().keySet())
                    {
                        Element resourceElement =
                                createResourceNode(currentFile,
                                        data.getFileToIssueLines().get(currentFile));
                        issueElement.appendChild(resourceElement);
                    }

                    if (WarningLevelFilter.printQuickFixSuggestions())
                    {
                        Element suggestionElement =
                                createSuggestionNode(data.getQuickFixSuggestion());
                        issueElement.appendChild(suggestionElement);
                    }

                    if (data.getPreview() != null)
                    {
                        Element previewElement = createPreviewNode(data.getPreview());
                        issueElement.appendChild(previewElement);
                    }

                    rootElement.appendChild(issueElement);
                }
            }
        }
    }

    /*
     * Generate the execution report node based on contents from executionStatus map
     */
    private void generateExecutionReport(Element appValElem, Map<String, IStatus> executionStatus)
    {
        Element executionReportElement = document.createElement(XML_TAG_EXECUTION_REPORT);

        for (String checkerId : executionStatus.keySet())
        {
            IStatus checkerStatus = executionStatus.get(checkerId);
            Element checkerStatusElement = document.createElement(XML_TAG_CHECKER_STATUS);

            checkerStatusElement.setAttribute(XML_ATTRIBUTE_CHECKER_ID, checkerId);

            //checker status equal INFO will be displayed as not executed
            String status;
            switch (checkerStatus.getSeverity())
            {
                case IStatus.OK:
                    status = XML_CHECKER_STATUS_VALUE_OK;
                    break;
                case IStatus.INFO:
                    status = XML_CHECKER_STATUS_VALUE_DISABLED;
                    break;
                default: //failed
                    status = XML_CHECKER_STATUS_VALUE_FAIL;
                    break;
            }

            checkerStatusElement.setAttribute(XML_ATTRIBUTE_STATUS, status);
            checkerStatusElement.setAttribute(XML_ATTRIBUTE_MESSAGE, checkerStatus.getMessage());

            executionReportElement.appendChild(checkerStatusElement);
        }

        appValElem.appendChild(executionReportElement);
    }

    /*
     * ERROR, FATAL_ERROR or WARNING nodes
     */
    private Element createIssueNode(SEVERITY severity)
    {
        Element element = null;

        if (SEVERITY.ERROR.compareTo(severity) == 0)
        {
            element = document.createElement(XML_TAG_ERROR);
        }
        else if (SEVERITY.WARNING.compareTo(severity) == 0)
        {
            element = document.createElement(XML_TAG_WARNING);
        }
        else if (SEVERITY.FATAL.compareTo(severity) == 0)
        {
            element = document.createElement(XML_TAG_FATAL_ERROR);
        }

        return element;
    }

    /*
     * Issue description node
     */
    private Element createDescriptionNode(String description)
    {
        Element element = document.createElement(XML_TAG_DESCRIPTION);

        if (description != null)
        {
            element.setTextContent(description);
        }

        return element;
    }

    /**
     * Create resource node
     * @param currentFile (resource)
     * @param lines with errors in this resource
     * @return the resource node
     */
    private Element createResourceNode(File currentFile, List<Integer> lines)
    {
        Element resElement = document.createElement(XML_TAG_RESOURCE);
        resElement.setAttribute(XML_ATTRIBUTE_PATH, computeResourcePath(currentFile));

        for (Integer currentLine : lines)
        {
            Element lineElement = document.createElement(XML_TAG_LINE);
            lineElement.setTextContent(currentLine.toString());
            resElement.appendChild(lineElement);
        }

        return resElement;
    }

    /**
     * Create fix sugestion node
     * @param suggestion text
     * @return the sugestion node
     */
    private Element createSuggestionNode(String suggestion)
    {
        Element sugElement = document.createElement(XML_TAG_SUGGESTION);
        sugElement.setTextContent(suggestion);

        return sugElement;
    }

    /**
     * Create a Preview node with Validation preview
     * @param preview text
     * @return preview node
     */
    private Element createPreviewNode(String preview)
    {
        Element previewElement = document.createElement(XML_TAG_PREVIEW);
        previewElement.setTextContent(preview);

        return previewElement;
    }

    @Override
    public void printError(Exception exceptionThrown, PrintStream out)
    {
        try
        {
            Element appValElem = createRootNode();
            Element executionReportElement =
                    document.createElement(XML_TAG_APP_VALIDATOR_EXECUTION_REPORT);
            executionReportElement
                    .setAttribute(XML_ATTRIBUTE_STATUS, XML_CHECKER_STATUS_VALUE_FAIL);
            executionReportElement
                    .setAttribute(XML_ATTRIBUTE_MESSAGE, exceptionThrown.getMessage());
            appValElem.appendChild(executionReportElement);

            XmlUtils.printXMLFormat(document);
        }
        catch (Exception e)
        {
            PreflightingLogger.error(getClass(),
                    PreflightingNLS.XMLOutputter_PrintResultsErrorMessage + e.getMessage());
        }
    }

    //creates the document and its root node
    private Element createRootNode() throws ParserConfigurationException
    {
        document = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().newDocument();

        Element appValElem = document.createElement(XML_TAG_APP_VALIDATOR);
        document.appendChild(appValElem);
        String appValidatorVersion = PreflightingPlugin.getInstance().getAppValidatorVersion();
        ValidationManagerConfiguration valManagerConfiguration =
                ValidationManagerConfiguration.getInstance();
        String motodevLink =
                valManagerConfiguration
                        .getProperty(ValidationManagerConfiguration.ConfigProperties.BASE_URL_PROPERTY
                                .getName());
        appValElem.setAttribute(XML_ATTRIBUTE_APPVALIDATOR_VERSION, appValidatorVersion);
        appValElem.setAttribute(XML_ATTRIBUTE_MOTODEV_LINK, motodevLink);

        return appValElem;
    }

    protected void generateCustomApplicationNodes(Element applicationElem,
            ApplicationValidationResult result, List<Parameter> params)
    {
        //Do nothing.
    }
}
