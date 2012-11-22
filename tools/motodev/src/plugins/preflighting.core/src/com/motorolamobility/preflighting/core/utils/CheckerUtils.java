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
package com.motorolamobility.preflighting.core.utils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;

/**
 * Utility class that can be used by any checker and its conditions.
 */
public class CheckerUtils
{
    /**
     * Defines the key for retrieving files of a map for a given {@link CompilationUnit}.
     */
    public static final String JAVA_FILE_PROPERTY = "java_file";

    /**
     * Parse the attribute value for the given id: @+id/object_id from the Android manifest file.
     * @param id the id whose value will be returnd.
     * @return The object_id portion of the string.
     */
    public static String getIdValue(String id)
    {
        int index = id.lastIndexOf("/"); //$NON-NLS-1$
        if ((index != -1) && ((index + 1) < id.length()))
        {
            id = id.substring(index + 1);
        }
        return id;
    }

    /**
     * Retrieve the app target from a given Manifest document.
     * @param manifestDoc XML representing AndroidManifest.xml.
     * @return The value of android:targetSdkVersion.
     * @throws NumberFormatException in case of preview sdk (target sdk being not a number string).
     */
    public static String getTargetSdk(Document manifestDoc) throws NumberFormatException
    {
        String targetSdk = "";
        NodeList usesSdkList = manifestDoc.getElementsByTagName(ManifestConstants.USES_SDK_TAG);
        if (usesSdkList.getLength() > 0)
        {
            Node usesSdkNode = usesSdkList.item(0);
            Node targetSdkAttribute =
                    usesSdkNode.getAttributes().getNamedItem(
                            ManifestConstants.TARGET_SDK_VERSION_ATTRIBUTE);

            if (targetSdkAttribute != null)
            {
                targetSdk = targetSdkAttribute.getNodeValue();
            }
        }
        return targetSdk;
    }

    /**
     * Retrieve the app minSdk version from a given Manifest document.
     * @param manifestDoc XML representing AndroidManifest.xml.
     * @return The value of android:minSdkVersion.
     * @throws NumberFormatException in case of preview sdk.
     */
    public static String getMinSdk(Document manifestDoc) throws NumberFormatException
    {
        String minSdk = "";
        NodeList usesSdkList = manifestDoc.getElementsByTagName(ManifestConstants.USES_SDK_TAG);
        if (usesSdkList.getLength() > 0)
        {
            Node usesSdkNode = usesSdkList.item(0);
            Node minSdkAttribute =
                    usesSdkNode.getAttributes().getNamedItem(
                            ManifestConstants.MIN_SDK_VERSION_ATTRIBUTE);

            if (minSdkAttribute != null)
            {
                minSdk = minSdkAttribute.getNodeValue();
            }
        }
        return minSdk;
    }

    /**
     * Retrieve all permissions declared on the manifest file.
     * @param manifestDoc The Manifest xml Document.
     * @return Map containing <permissionId, xmlNode> where xmlNode is the entire uses-permission for that permission.
     */
    public static Map<String, Node> getPermissions(Document manifestDoc)
    {
        NodeList permissionsNodeLst =
                manifestDoc.getElementsByTagName(ManifestConstants.USES_PERMISSION_ATTRIBUTE);

        //Extract permissions from manifest
        Map<String, Node> manifestPermissions =
                new HashMap<String, Node>(permissionsNodeLst.getLength());
        for (int i = 0; i < permissionsNodeLst.getLength(); i++)
        {
            Node permissionNode = permissionsNodeLst.item(i);
            NamedNodeMap permissionMap = permissionNode.getAttributes();
            Node permissionAtr =
                    permissionMap.getNamedItem(ManifestConstants.ANDROID_NAME_ATTRIBUTE);

            if ((permissionAtr != null))
            {
                String permissionId = permissionAtr.getNodeValue().trim();
                if (permissionId.length() > 0)
                {
                    manifestPermissions.put(permissionId, permissionNode);
                }
            }
        }
        return manifestPermissions;
    }

    /**
     * Given an {@link ApplicationData} file structure, it is verified whether
     * the AndroidManifest.xml file exists. The result is returned as an
     * {@link IStatus}.
     * 
     * @param data {@link ApplicationData} file structure.
     * @param conditionId The condition Id, can be null.
     * 
     * @return Return the {@link IStatus} of the AndroidManifest.xml file
     * existence. The {@link IStatus} returned actually is an extension of it,
     * called {@link CanExecuteConditionStatus}.
     */
    public static CanExecuteConditionStatus isAndroidManifestFileExistent(ApplicationData data,
            String conditionId)
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, PreflightingCorePlugin.PLUGIN_ID, ""); //$NON-NLS-1$

        //Look for Manifest file
        XMLElement manifestElement = data.getManifestElement();
        if (manifestElement == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, PreflightingCorePlugin.PLUGIN_ID,
                            PreflightingCoreNLS.Invalid_ManifestFile);
        }
        else if ((manifestElement.getDocument()) == null)
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, PreflightingCorePlugin.PLUGIN_ID,
                            PreflightingCoreNLS.Invalid_ManifestFile);
        }

        if (conditionId != null)
        {
            status.setConditionId(conditionId);
        }

        return status;
    }

    /**
     * Given an {@link ApplicationData} structure, it is verified whether
     * the javaModel is available. The result is returned as an
     * {@link IStatus}.
     * 
     * @param data {@link ApplicationData} file structure.
     * @param conditionId The condition Id, can be null.
     * 
     * @return Return the {@link IStatus} of the AndroidManifest.xml file
     * existence. The {@link IStatus} returned actually is an extension of it,
     * called {@link CanExecuteConditionStatus}.
     */
    public static CanExecuteConditionStatus isJavaModelAvailable(ApplicationData data,
            String conditionId)
    {
        CanExecuteConditionStatus status =
                new CanExecuteConditionStatus(IStatus.OK, PreflightingCorePlugin.PLUGIN_ID, ""); //$NON-NLS-1$;
        List<SourceFolderElement> sourceFolderElements = data.getJavaModel();
        if ((sourceFolderElements == null) || sourceFolderElements.isEmpty())
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, PreflightingCorePlugin.PLUGIN_ID,
                            PreflightingCoreNLS.JavaModelNotFound_Err);
        }

        if (conditionId != null)
        {
            status.setConditionId(conditionId);
        }

        return status;
    }

    /**
     * Given an {@link ApplicationData} structure, it is verified whether
     * the javaModel is complete, i.e. exists and invokeMethods are available. The result is returned as an
     * {@link IStatus}.
     * 
     * @param data {@link ApplicationData} file structure.
     * @param conditionId The condition Id, can be null.
     * 
     * @return Return the {@link IStatus} of the AndroidManifest.xml file
     * existence. The {@link IStatus} returned actually is an extension of it,
     * called {@link CanExecuteConditionStatus}.
     */
    public static CanExecuteConditionStatus isJavaModelComplete(ApplicationData data,
            String conditionId)
    {
        CanExecuteConditionStatus status = isJavaModelAvailable(data, conditionId); //$NON-NLS-1$;

        List<SourceFolderElement> sourceFolderElements = data.getJavaModel();
        if (status.isOK())
        {
            boolean found = false;
            for (SourceFolderElement sourceFolderElement : sourceFolderElements)
            {
                List<Invoke> invokedMethods = sourceFolderElement.getInvokedMethods();
                if ((invokedMethods != null) && !invokedMethods.isEmpty())
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                status =
                        new CanExecuteConditionStatus(IStatus.ERROR,
                                PreflightingCorePlugin.PLUGIN_ID,
                                PreflightingCoreNLS.EmptyInvokedMethods_Err);
            }
        }
        if (status.isOK())
        {
            boolean found = false;
            for (SourceFolderElement sourceFolderElement : sourceFolderElements)
            {
                List<SourceFileElement> sourceFiles = sourceFolderElement.getSourceFileElements();
                if ((sourceFiles != null) && !sourceFiles.isEmpty())
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                status =
                        new CanExecuteConditionStatus(IStatus.ERROR,
                                PreflightingCorePlugin.PLUGIN_ID,
                                PreflightingCoreNLS.NoSourceFilesFound_Err);
            }
        }

        if (conditionId != null)
        {
            status.setConditionId(conditionId);
        }

        return status;
    }

    /**
     * Create a fileToIssues map, containing only 1 line.
     * @param file
     * @param currentIssuedLine
     * @return 
     */
    public static Map<File, List<Integer>> createFileToIssuesMap(File file, int currentIssuedLine)
    {
        Map<File, List<Integer>> fileToIssueLines = new HashMap<File, List<Integer>>(1);
        if ((file != null) && (currentIssuedLine > 0))
        {
            fileToIssueLines.put(file, Arrays.asList(currentIssuedLine));
        }
        return fileToIssueLines;
    }

}
