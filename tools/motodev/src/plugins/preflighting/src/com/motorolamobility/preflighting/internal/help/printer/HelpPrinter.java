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

package com.motorolamobility.preflighting.internal.help.printer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.motorolamobility.preflighting.core.checker.CheckerDescription;
import com.motorolamobility.preflighting.core.checker.CheckerExtension;
import com.motorolamobility.preflighting.core.checker.IChecker;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.DevicesSpecsContainer;
import com.motorolamobility.preflighting.core.devicespecification.DevicesSpecsContainer.SpecKey;
import com.motorolamobility.preflighting.core.exception.PreflightingExtensionPointException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.XmlUtils;
import com.motorolamobility.preflighting.core.validation.ParameterDescription;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.Value;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.internal.PreflightingPlugin;

/**
 * Class responsible for printing help information to the console / command line.
 *
 */
public class HelpPrinter
{

    private static final String SEPARATOR = ",";

    private static final String XML_TAG_CHECKERS = "Checkers";

    private static final String XML_TAG_SPECS = "Specs";

    private static final String XML_TAG_SPEC = "Spec";

    private static final String XML_TAG_SPEC_DEVICES = "SpecDevices";

    private static final String XML_TAG_DEVICES = "Devices";

    private static final String ATTRIBUTE_LEVEL = "level";

    private static final String XML_TAG_CONDITION = "Condition";

    private static final String XML_TAG_VALUE_DESCRIPTION = "ValueDescription";

    private static final String ATTRIBUTE_NAME = "name";

    private static final String XML_TAG_PARAMETER = "Parameter";

    private static final String XML_TAG_DESCRIPTION = "Description";

    private static final String ATTRIBUTE_ID = "id";

    public static final String XML_TAG_CHECKER = "Checker";

    public static final String XML_TAG_DEVICE = "Device";

    public static Document devicesCheckersSpecsMapDocument = null;

    /**
     * Line separator
     */
    private final static String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Tab character
     */
    private final static String TAB = "\t"; //$NON-NLS-1$

    /**
     * I/O Exception message logging.
     */
    private final static String IO_ERROR = PreflightingNLS.HelpPrinter_IOExceptionMessage;

    private static final String XML_TAG_APP_VALIDATOR = "AppValidator";

    private static final String XML_ATTRIBUTE_APPVALIDATOR_VERSION = "version";

    private static final String XML_ATTRIBUTE_MOTODEV_LINK = "description_url";

    private static final HashMap<String, Document> cache = new HashMap<String, Document>();

    /**
     * Print help data.
     * @param parameters Help data
     * @param printSyntax Whether syntax should be printed or not
     * @param printStream 
     * @param checker checker to get the description and parameters returned
     */
    public static void printHelp(PrintStream printStream, List<ParameterDescription> parameters,
            boolean printSyntax)
    {
        if (parameters.size() > 0)
        {
            StringBuilder stringBuilder = new StringBuilder();

            if (printSyntax)
            {
                stringBuilder.append(PreflightingNLS.HelpPrinter_Usage + " ");
                stringBuilder.append("\t" + PreflightingNLS.HelpPrinter_ProgramName);
                stringBuilder
                        .append("[OPTION[PRM]]...\n\t" + PreflightingNLS.HelpPrinter_ProgramName + "[OPTION[PRM]]... -input [FILE] [OPTION[PRM]]...\n");//$NON-NLS-1$
                stringBuilder.append(PreflightingNLS.HelpPrinter_ProgramDescritpion + NEWLINE
                        + NEWLINE);
            }

            stringBuilder.append(PreflightingNLS.HelpPrinter_OptionsMessage + NEWLINE);
            // Iterate through the parameters and print the info
            for (ParameterDescription p : parameters)
            {
                /*
                 * The output format will be something like this:
                 * 
                 * Parameter info:
                 *  -{Parameter name} [TAB] Default value: {defaultValue} - {Description}
                 *      [TAB] {value1} Type: {type} - Description
                 */

                // Append parameter name
                if (p.getName() != null)
                {
                    stringBuilder.append("-" + p.getName()); //$NON-NLS-1$
                    int totalLenght = p.getName().length();
                    if (p.getValueDescription() != null)
                    {
                        stringBuilder.append(" " + p.getValueDescription()); //$NON-NLS-1$
                        totalLenght += p.getValueDescription().length() + 1;
                    }
                    if (totalLenght < 25)
                    {
                        int i = 25 - totalLenght;
                        while (i > 0)
                        {
                            stringBuilder.append(" ");
                            i--;
                        }
                    }
                }

                // Append default value
                if (p.getDefaultValue() != null)
                {
                    stringBuilder.append(PreflightingNLS.HelpPrinter_DefaultMessage
                            + p.getDefaultValue().getValue());
                    stringBuilder.append(" "); //$NON-NLS-1$
                }

                // Append description
                if (p.getDescription() != null)
                {
                    stringBuilder.append("- "); //$NON-NLS-1$
                    stringBuilder.append(p.getDescription());
                    stringBuilder.append(NEWLINE);
                }

                // Append values information
                for (Value v : p.getAllowedValues())
                {
                    stringBuilder.append(TAB);

                    // Append value
                    if (v.getValue() != null)
                    {
                        stringBuilder.append(v.getValue());
                        stringBuilder.append(" - "); //$NON-NLS-1$
                    }

                    // Append description
                    if (v.getDescription() != null)
                    {
                        stringBuilder.append(v.getDescription());
                        stringBuilder.append(NEWLINE);
                    }
                }

            }

            printStream.print(stringBuilder.toString());
            printStream.flush();
        }

    }

    /**
     * Prints a XML file which contains a list of devices, their id for App Validator and provider
     * @param stream The stream used to print the XML file
     * @throws ParserConfigurationException 
     * @throws TransformerException 
     * @throws UnsupportedEncodingException 
     */
    public static void printXMLDevicesList(PrintStream stream) throws ParserConfigurationException,
            UnsupportedEncodingException, TransformerException
    {

        ValidationManager validationManager = new ValidationManager();

        DevicesSpecsContainer devicesSpecsContainer = validationManager.getDevicesSpecsContainer();

        Document document =
                DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().newDocument();
        Element appvalidatorElement = createAppvalidatorNode(document);

        for (DeviceSpecification deviceInfo : devicesSpecsContainer.getDeviceSpecifications())
        {
            appvalidatorElement.appendChild(createDeviceNode(deviceInfo, document));
        }

        XmlUtils.printXMLFormat(document);

        validationManager = null;
    }

    private static Element createDeviceNode(DeviceSpecification deviceInfo, Document document)
    {
        Element deviceNode = document.createElement(XML_TAG_DEVICE);
        deviceNode.setTextContent(deviceInfo.getName());

        deviceNode.setAttribute("id", deviceInfo.getId());
        deviceNode.setAttribute("provider", deviceInfo.getProvider());

        return deviceNode;
    }

    /**
     * Prints a XML file which contains a list of checkers and all the information regarding each checker
     * @param stream The stream used to print the XML file
     */
    public static void printXMLCheckerList(PrintStream stream)
            throws PreflightingExtensionPointException, ParserConfigurationException,
            UnsupportedEncodingException, TransformerException
    {

        Map<String, CheckerExtension> checkers = ValidationManager.loadCheckers();
        Collection<CheckerExtension> checkerExtensions = checkers.values();

        Document document =
                DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().newDocument();
        Element appvalidatorElement = null;

        appvalidatorElement = createAppvalidatorNode(document);

        for (CheckerExtension extension : checkerExtensions)
        {
            Element node = getXMLCheckerNode(extension, document);
            appvalidatorElement.appendChild(node);
        }

        XmlUtils.printXMLFormat(document);

    }

    private static Element createAppvalidatorNode(Document document)
    {

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

    /**
     * Generate a XML node for a specific checker
     */
    private static Element getXMLCheckerNode(CheckerExtension extension, Document document)
    {

        Element checkerNode = document.createElement(XML_TAG_CHECKER);
        checkerNode.setAttribute(ATTRIBUTE_ID, extension.getId());

        Element descriptionNode = document.createElement(XML_TAG_DESCRIPTION);
        descriptionNode.setTextContent(extension.getDescription());

        checkerNode.appendChild(descriptionNode);

        IChecker checker = extension.getChecker();
        List<Condition> conditionList = getSortedConditions(checker);

        appendConditions(checkerNode, document, conditionList);
        appendParameters(checkerNode, document, checker);

        return checkerNode;
    }

    private static void appendParameters(Element checkerNode, Document document, IChecker checker)
    {
        List<ParameterDescription> paremeterDescriptionList = checker.getParameterDescriptions();
        if ((paremeterDescriptionList != null) && (paremeterDescriptionList.size() > 0))
        {
            for (ParameterDescription param : paremeterDescriptionList)
            {
                Element parameterNode = document.createElement(XML_TAG_PARAMETER);
                parameterNode.setAttribute(ATTRIBUTE_NAME, param.getName());

                Element valueDescriptionNode = document.createElement(XML_TAG_VALUE_DESCRIPTION);
                valueDescriptionNode.setTextContent(param.getValueDescription());

                Element paramDescrNode = document.createElement(XML_TAG_DESCRIPTION);
                paramDescrNode.setTextContent(param.getDescription());

                parameterNode.appendChild(valueDescriptionNode);
                parameterNode.appendChild(paramDescrNode);
                checkerNode.appendChild(parameterNode);
            }
        }
    }

    private static void appendConditions(Element checkerNode, Document document,
            List<Condition> conditionList)
    {
        for (ICondition condition : conditionList)
        {
            Element conditionNode = document.createElement(XML_TAG_CONDITION);
            conditionNode.setAttribute(ATTRIBUTE_ID, condition.getId());
            conditionNode.setAttribute(ATTRIBUTE_LEVEL, condition.getSeverityLevel().toString());

            Element conditionDescrNode = document.createElement(XML_TAG_DESCRIPTION);
            conditionDescrNode.setTextContent(condition.getDescription());

            conditionNode.appendChild(conditionDescrNode);
            checkerNode.appendChild(conditionNode);
        }
    }

    /**
     * Prints the description from the extension point declaration and 
     * print the parameters returned by the checker itself.
     * @param checkerExtension
     */
    public static void printHelpChecker(PrintStream out, CheckerExtension checkerExtension)
    {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(PreflightingNLS.HelperPrinter_CheckerId + checkerExtension.getId()
                + NEWLINE);
        stringBuilder.append(PreflightingNLS.HelperPrinter_CheckerDescription
                + checkerExtension.getDescription() + NEWLINE);

        IChecker checker = checkerExtension.getChecker();

        // Append condition information
        List<Condition> conditionList = getSortedConditions(checker);

        // Size < 0 should never happen, but you never can be too sure...
        if ((conditionList != null) && (conditionList.size() > 0))
        {
            // Iterate through the condition and print the descriptions
            stringBuilder.append(NEWLINE);
            stringBuilder.append(PreflightingNLS.HelperPrinter_CheckerHasConditions + NEWLINE);

            stringBuilder.append(NEWLINE);
            stringBuilder.append(TAB);
            stringBuilder.append("Default level");
            stringBuilder.append(TAB);

            stringBuilder.append("Condition ID");
            stringBuilder.append(TAB);
            stringBuilder.append(TAB);
            stringBuilder.append(TAB);
            stringBuilder.append("Description");
            stringBuilder.append(NEWLINE);

            for (ICondition c : conditionList)
            {
                stringBuilder.append(TAB);
                stringBuilder.append(c.getSeverityLevel().toString());
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(c.getId());
                stringBuilder.append(TAB);
                if (c.getId().length() < 24)
                {
                    stringBuilder.append(TAB);
                }
                if (c.getId().length() < 15)
                {
                    stringBuilder.append(TAB);
                }
                stringBuilder.append(c.getDescription());
                stringBuilder.append(NEWLINE);
            }

            stringBuilder.append(NEWLINE);
        }

        List<ParameterDescription> paremeterDescriptionList = checker.getParameterDescriptions();
        if (paremeterDescriptionList != null)
        {

            if (paremeterDescriptionList.size() > 0)
            {
                stringBuilder.append(PreflightingNLS.HelperPrinter_CheckerUsesParameters + NEWLINE);
            }
            else
            {
                stringBuilder.append(PreflightingNLS.HelperPrinter_CheckerDoesNotUseParameters
                        + NEWLINE);
            }
            for (ParameterDescription returnParam : paremeterDescriptionList)
            {
                String paramMessage =
                        TAB + returnParam.getName() + " = " + returnParam.getValueDescription();
                if ((returnParam.getType() != null) && (returnParam.getType().toString() != null))
                {
                    paramMessage += " [" + returnParam.getType().toString() + "]";
                }
                paramMessage += TAB + "- " + returnParam.getDescription() + NEWLINE;
                stringBuilder.append(paramMessage);
            }
        }

        out.print(stringBuilder.toString());
    }

    /**
     * Return a sorted list of checker conditions according to it default level
     * 
     * @param checker the checker what have the conditions
     * @return the sorted list of conditions
     */
    private static List<Condition> getSortedConditions(IChecker checker)
    {
        List<Condition> conditionList = new ArrayList(checker.getConditions().values());

        // Sort the conditions list
        Collections.sort(conditionList, new Comparator<Condition>()
        {
            public int compare(Condition o1, Condition o2)
            {
                if (o1.getSeverityLevel().ordinal() > o2.getSeverityLevel().ordinal())
                {
                    return +1;
                }
                else
                {
                    return -1;
                }
            }
        });
        return conditionList;
    }

    public static void printDevicesList(List<Value> list, PrintStream printStream)
    {
        if (list.size() > 0)
        {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(PreflightingNLS.HelpPrinter_Device_Id + " name - [id]");
            stringBuilder.append(NEWLINE);
            for (Value current : list)
            {
                //Append device ID
                stringBuilder.append(TAB);
                stringBuilder.append(current.getValue());
                stringBuilder.append(NEWLINE);
            }
            printStream.print(stringBuilder.toString());
            // Clean string builder
            stringBuilder.delete(0, stringBuilder.length());
        }
    }

    public static void printDevicesDescription(String deviceDescription, String deviceId,
            PrintStream printStream)
    {
        if ((deviceDescription != null) && !deviceDescription.trim().equals(""))
        {
            printStream.print(deviceDescription);
            printStream.println();
        }
        else
        {
            //device id not found
            printStream.print(PreflightingNLS
                    .bind(PreflightingNLS.GlobalInputParamsValidator_NonExistentDeviceIdMessage,
                            deviceId));
        }
    }

    public static void printCheckersList(List<CheckerDescription> list, PrintStream printStream)
    {
        if (list.size() > 0)
        {
            StringBuilder stringBuilder = new StringBuilder();

            /*
             * Header
             */

            // Iterate through the values and print the info                
            for (CheckerDescription checker : list)
            {
                /*
                 * The output format will be something like this:
                 * 
                 * {value} {TAB} {Description}
                 */

                //Append ID
                stringBuilder.append(PreflightingNLS.HelpPrinter_Checker_Id);
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                stringBuilder.append(checker.getId());
                stringBuilder.append(NEWLINE);

                //Append Name
                stringBuilder.append(PreflightingNLS.HelpPrinter_Checker_Name);
                stringBuilder.append(TAB);
                stringBuilder.append(TAB);
                if ((checker.getName() != null))
                {
                    stringBuilder.append(checker.getName());
                }
                else
                {
                    stringBuilder.append(PreflightingNLS.HelpPrinter_Checker_NotAvailable);
                }
                stringBuilder.append(NEWLINE);

                //Append Description
                stringBuilder.append(PreflightingNLS.HelpPrinter_Checker_Description);
                stringBuilder.append(TAB);
                if ((checker.getName() != null))
                {
                    stringBuilder.append(checker.getDescription());
                }
                else
                {
                    stringBuilder.append(PreflightingNLS.HelpPrinter_Checker_NotAvailable);
                }
                stringBuilder.append(NEWLINE);
                stringBuilder.append(NEWLINE);

                printStream.print(stringBuilder.toString());

                // Clean string builder
                stringBuilder.delete(0, stringBuilder.length());
            }
        }
    }

    /**
     * Print a list of values.
     * @param values The list of values
     * @param stream The output stream
     */
    public static void printList(List<Value> values, OutputStream stream)
    {
        if (values.size() > 0)
        {
            /*
             * Writer used for output
             */
            BufferedWriter writer = null;
            StringBuilder stringBuilder = new StringBuilder();

            writer = new BufferedWriter(new OutputStreamWriter(stream));

            try
            {
                // Iterate through the values and print the info
                for (Value v : values)
                {
                    /*
                     * The output format will be something like this:
                     * 
                     * {value} {TAB} {Description}
                     */

                    stringBuilder.append(v.getValue());
                    stringBuilder.append(TAB);
                    if (v.getDescription() != null)
                    {
                        stringBuilder.append(v.getDescription());
                    }

                    writer.write(stringBuilder.toString());
                    writer.flush();

                    // Clean string builder
                    stringBuilder.delete(0, stringBuilder.length());

                }
            }
            catch (IOException e)
            {

                PreflightingLogger.error(HelpPrinter.class, IO_ERROR + e.getMessage());

            }
            finally
            {
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    PreflightingLogger.error(HelpPrinter.class, IO_ERROR + e.getMessage());
                }
            }
        }

    }

    private static Element createSpecDevicesMapNode(ValidationManager validationManager,
            Document document, String categories)
    {
        Element specDevicesNode = document.createElement(XML_TAG_SPEC_DEVICES);
        Map<SpecKey, List<DeviceSpecification>> specDevsMap =
                validationManager.getDevicesSpecsContainer().getSpecDevFilterMap();

        //Set<SpecKey> specs = specDevsMap.keySet();
        String[] specs = categories.split(SEPARATOR);

        for (String specStr : specs)
        {
            SpecKey spec = SpecKey.valueOf(specStr);
            Element specNode = document.createElement(XML_TAG_SPEC);
            specNode.setAttribute(ATTRIBUTE_ID, spec.getId());

            List<DeviceSpecification> devs = specDevsMap.get(spec);

            if (devs != null)
            {
                for (DeviceSpecification dev : devs)
                {
                    Element devNode = document.createElement(XML_TAG_DEVICE);
                    devNode.setAttribute(ATTRIBUTE_ID, dev.getId());
                    specNode.appendChild(devNode);
                }
            }

            specDevicesNode.appendChild(specNode);
        }

        return specDevicesNode;
    }

    private static Element creatSpecsNode(ValidationManager validationManager, Document document,
            String categories)
    {

        Element specsNode = document.createElement(XML_TAG_SPECS);
        String[] specList = categories.split(SEPARATOR);

        //SpecKey[] specs = SpecKey.values();
        //for (SpecKey spec : specs)
        for (String spec : specList)
        {
            Element specNode = document.createElement(XML_TAG_SPEC);
            specNode.setAttribute(ATTRIBUTE_ID, spec);//spec.getId());
            specNode.setAttribute(ATTRIBUTE_NAME, spec);//spec.getId());

            specsNode.appendChild(specNode);
        }

        return specsNode;
    }

    private static Element createCheckersNode(ValidationManager validationManager, Document document)
    {

        Element checkersNode = document.createElement(XML_TAG_CHECKERS);

        List<CheckerDescription> checkers = validationManager.getCheckersDescription();

        for (CheckerDescription checker : checkers)
        {
            Element checkerNode = document.createElement(XML_TAG_CHECKER);
            checkerNode.setAttribute(ATTRIBUTE_ID, checker.getId());
            checkerNode.setAttribute(ATTRIBUTE_NAME, checker.getName());

            checkersNode.appendChild(checkerNode);
        }

        return checkersNode;
    }

    private static Element createDevicesNode(ValidationManager validationManager, Document document)
    {

        Element devicesNode = document.createElement(XML_TAG_DEVICES);
        DevicesSpecsContainer devicesSpecsContainer = validationManager.getDevicesSpecsContainer();
        List<DeviceSpecification> devices = devicesSpecsContainer.getDeviceSpecifications();

        // order by device name
        Collections.sort(devices, new Comparator<DeviceSpecification>()
        {
            public int compare(DeviceSpecification d1, DeviceSpecification d2)
            {
                return d1.getName().compareToIgnoreCase(d2.getName());
            }
        });

        for (DeviceSpecification devSpec : devices)
        {

            Element deviceNode = document.createElement(XML_TAG_DEVICE);

            deviceNode.setAttribute(ATTRIBUTE_ID, devSpec.getId());
            deviceNode.setAttribute(ATTRIBUTE_NAME, devSpec.getName().replace("™", "&#8482;"));

            devicesNode.appendChild(deviceNode);
        }

        return devicesNode;
    }

    /**
     * Print an XML file with: list of checkers, list of devices, map of specifications vs devices
     * @param stream The output stream
     * @throws ParserConfigurationException 
     * @throws TransformerException 
     * @throws UnsupportedEncodingException 
     */
    public static void printXMLDevicesCheckersSpecsMap(PrintStream stream)
            throws ParserConfigurationException, UnsupportedEncodingException, TransformerException
    {

        // TODO: implement a configuration file or another way to keep this list
        String categories =
                SpecKey.screenSize_Small.getId() + SEPARATOR + SpecKey.screenSize_XLarge.getId()
                        + SEPARATOR + SpecKey.screenOrientation_Port.getId() + SEPARATOR
                        + SpecKey.screenOrientation_Land.getId() + SEPARATOR
                        + SpecKey.pixelDensity_High.getId() + SEPARATOR
                        + SpecKey.pixelDensity_Low.getId() + SEPARATOR
                        + SpecKey.pixelDensity_Medium.getId() + SEPARATOR
                        + SpecKey.pixelDensity_XHigh.getId();

        if (cache.get(categories) == null)
        {
            Document document =
                    DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().newDocument();
            Element appvalidatorElement = createAppvalidatorNode(document);

            ValidationManager validationManager = new ValidationManager();

            Element checkersNode = createCheckersNode(validationManager, document);
            appvalidatorElement.appendChild(checkersNode);

            Element devicesNode = createDevicesNode(validationManager, document);
            appvalidatorElement.appendChild(devicesNode);

            Element specsNode = creatSpecsNode(validationManager, document, categories);
            appvalidatorElement.appendChild(specsNode);

            Element specDevicesMapNode =
                    createSpecDevicesMapNode(validationManager, document, categories);
            appvalidatorElement.appendChild(specDevicesMapNode);

            cache.put(categories, document);
        }

        XmlUtils.printXMLFormat(cache.get(categories));

        XmlUtils.printXMLFormat(cache.get(categories));

    }

}
