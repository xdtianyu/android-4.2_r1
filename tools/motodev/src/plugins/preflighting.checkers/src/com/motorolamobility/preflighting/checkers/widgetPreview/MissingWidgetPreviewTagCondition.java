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

package com.motorolamobility.preflighting.checkers.widgetPreview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.ElementUtils;
import com.motorolamobility.preflighting.core.applicationdata.FolderElement;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

public class MissingWidgetPreviewTagCondition extends Condition implements ICondition
{

    /**
     * 
     */
    private static final int MIN_TABLET_SDK_VERSION = 11;

    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        ArrayList<ValidationResultData> resultDataList = new ArrayList<ValidationResultData>();

        boolean hasTag = true;

        FolderElement xmlFolder = null;
        XMLElement xmlFileElement = data.getManifestElement();

        //Getting the sdk version, because only from android 3.0 on the previewImage is supported
        String minSdkStr = CheckerUtils.getMinSdk(xmlFileElement.getDocument());
        int minSdkVersion = -1;
        try
        {
            minSdkVersion = Integer.parseInt(minSdkStr);
        }
        catch (NumberFormatException e)
        {
            minSdkVersion = -1;
        }

        //it only makes sense to check for previewImage in widgets applications
        boolean isWidget = isWidgetAplication(data);

        //is a widget and is for android 3.0 and above version
        if ((minSdkVersion >= MIN_TABLET_SDK_VERSION) && (isWidget))
        {

            xmlFolder = getXMLFolder(data);
            //Get the xml filename responsible for the widget resources
            ArrayList<String> xmlFilename = getWidgetResourceFilename(data);

            for (String resourceFile : xmlFilename)
            {
                ValidationResultData resultData = new ValidationResultData();
                //get the actual xml file element
                xmlFileElement = getXMLFile(xmlFolder, resourceFile);

                //check for the android:previewImage tag
                hasTag = checkForPreviewTag(xmlFileElement);

                if (hasTag)
                {
                    //it is not a widget OR is not android 3.0+ version OR everything went fine
                    resultData.setSeverity(ValidationResultData.SEVERITY.OK);

                }
                else
                {
                    //start build the result
                    resultData.setSeverity(getSeverityLevel());
                    resultData.setConditionID(getId());
                    resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                            getId(), valManagerConfig));
                    resultData
                            .setQuickFixSuggestion(CheckerNLS.MissingWidgetPreviewTagCondition_quickFix);
                    resultData
                            .setIssueDescription(CheckerNLS.MissingWidgetPreviewTagCondition_WarningMessage);

                    resultData.addFileToIssueLines(xmlFileElement.getFile(),
                            Collections.<Integer> emptyList());

                }
                resultDataList.add(resultData);

            }
        }
        results.addAll(resultDataList);

    }

    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, null);
        status.setConditionId(getId());

        XMLElement manElem = data.getManifestElement();
        if (manElem == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.Invalid_ManifestFile);
            status.setConditionId(getId());
        }
        else
        {
            Document manifestDoc = manElem.getDocument();

            if (manifestDoc == null)
            {
                status =
                        new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.Invalid_ManifestFile);
                status.setConditionId(getId());
            }
        }

        return status;
    }

    /**
     * This Method check for the widget related intent in order to determine if the application
     * is a widget
     * 
     * @param data
     * @return whether the application is a widget project or not
     * @throws PreflightingCheckerException
     */

    private boolean isWidgetAplication(ApplicationData data) throws PreflightingCheckerException
    {
        Boolean actionFound = false;
        XMLElement manElem = data.getManifestElement();
        if (manElem != null)
        {
            Document manifestDoc = manElem.getDocument();
            if (manifestDoc != null)
            {
                NodeList rcvLst = manifestDoc.getElementsByTagName("receiver"); //$NON-NLS-1$

                for (int receiverIndex = 0; receiverIndex < rcvLst.getLength(); receiverIndex++)
                {

                    NodeList intentFilterLst = rcvLst.item(receiverIndex).getChildNodes();
                    for (int intentFilterIndex = 0; intentFilterIndex < intentFilterLst.getLength(); intentFilterIndex++)
                    {
                        Node intentFilterNode = intentFilterLst.item(intentFilterIndex);
                        // get intent-filter nodes
                        if (intentFilterNode.getNodeName().equals("intent-filter")) //$NON-NLS-1$
                        {
                            NodeList actionLst = intentFilterNode.getChildNodes();
                            for (int actionListIndex = 0; actionListIndex < actionLst.getLength(); actionListIndex++)
                            {
                                Node actionNode = actionLst.item(actionListIndex);
                                // get action nodes
                                if (actionNode.getNodeName().equals("action")) //$NON-NLS-1$
                                {
                                    NamedNodeMap map = actionNode.getAttributes();
                                    // name attribute must be set to
                                    // android.appwidget.action.APPWIDGET_UPDATE
                                    Node nameAtr = map.getNamedItem("android:name"); //$NON-NLS-1$

                                    try
                                    {
                                        if ((nameAtr != null)
                                                && nameAtr
                                                        .getNodeValue()
                                                        .equals("android.appwidget.action.APPWIDGET_UPDATE")) //$NON-NLS-1$
                                        {
                                            actionFound = true;

                                        }

                                    }
                                    catch (DOMException e)
                                    {
                                        // Error retrieving value of the action intent
                                        throw new PreflightingCheckerException(
                                                CheckerNLS.MainActivityChecker_Exception_Get_Action_Intent_Value,
                                                e);
                                    }
                                }

                            }

                        }

                    }
                }
            }
        }
        return actionFound;
    }

    /**
     * This method retrieves the filename of the widget resouces XML pointed in the AndroidManifest
     * @param data
     * @return the filename
     */

    private ArrayList<String> getWidgetResourceFilename(ApplicationData data)
    {

        ArrayList<String> xmlFilename = new ArrayList<String>();

        XMLElement manElem = data.getManifestElement();
        if (manElem != null)
        {
            Document manifestDoc = manElem.getDocument();
            if (manifestDoc != null)
            {
                NodeList actLst = manifestDoc.getElementsByTagName("receiver"); //$NON-NLS-1$

                for (int k = 0; k < actLst.getLength(); k++)
                {

                    NodeList intentFilterLst = actLst.item(k).getChildNodes();
                    for (int j = 0; j < intentFilterLst.getLength(); j++)
                    {
                        Node metaDataNode = intentFilterLst.item(j);
                        // get meta-data
                        if (metaDataNode.getNodeName().equals("meta-data")) //$NON-NLS-1$
                        {

                            NamedNodeMap map = metaDataNode.getAttributes();
                            // name attribute must be set to
                            // android.appwidget.provider
                            Node nameAtr = map.getNamedItem("android:name"); //$NON-NLS-1$

                            try
                            {
                                if ((nameAtr != null)
                                        && nameAtr.getNodeValue().equals(
                                                "android.appwidget.provider")) //$NON-NLS-1$
                                {
                                    Node resourceAtr = map.getNamedItem("android:resource"); //$NON-NLS-1$
                                    if (resourceAtr != null)
                                    {

                                        xmlFilename.add(resourceAtr.getNodeValue()
                                                .substring(
                                                        xmlFilename.indexOf("@xml/")
                                                                + "@xml/".length() + 1));

                                    }

                                }

                            }
                            catch (DOMException e)
                            {
                                // DO Nothing
                            }
                        }

                    }

                }

            }
        }
        return xmlFilename; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * This method gets the xml folder of the project
     * 
     * @param data
     * @return the xml folder
     */

    private FolderElement getXMLFolder(ApplicationData data)
    {
        List<Element> folderResElements =
                ElementUtils.getElementByType(data.getRootElement(), Type.FOLDER_RES);
        FolderElement xmlFolder = null;
        ResourcesFolderElement resFolder =
                folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements.get(0)
                        : null;

        if (resFolder != null)
        {
            for (Element element : resFolder.getChildren())
            {
                if ((element instanceof FolderElement) && (element.getName().equals("xml"))) //$NON-NLS-1$
                {
                    xmlFolder = (FolderElement) element;
                }
            }
        }
        return xmlFolder;
    }

    private XMLElement getXMLFile(FolderElement xmlFolder, String xmlFilename)
    {
        XMLElement xmlFileElement = null;
        if (xmlFolder instanceof FolderElement)
        {
            for (Element element : xmlFolder.getChildren())
            {
                if (element.getName().equals(xmlFilename + ".xml") //$NON-NLS-1$
                        && (element instanceof XMLElement))
                {
                    xmlFileElement = (XMLElement) element;
                }

            }
        }
        return xmlFileElement;
    }

    /**
     * @param xmlFileElement
     * @return
     */
    private boolean checkForPreviewTag(XMLElement xmlFileElement)
    {
        boolean hasPreviewImage = false;
        if (xmlFileElement != null)
        {
            Document resourceDoc = xmlFileElement.getDocument();
            if (resourceDoc != null)
            {
                NodeList provLst = resourceDoc.getElementsByTagName("appwidget-provider"); //$NON-NLS-1$

                for (int k = 0; k < provLst.getLength(); k++)
                {

                    NamedNodeMap atrMap = provLst.item(k).getAttributes();
                    Node previewImageNode = atrMap.getNamedItem("android:previewImage"); //$NON-NLS-1$

                    if (previewImageNode != null)
                    {
                        hasPreviewImage = true;
                    }
                }
            }
        }
        return hasPreviewImage;

    }

}
