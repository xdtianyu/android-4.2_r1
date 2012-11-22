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
package com.motorolamobility.preflighting.checkers.buildingblocksdeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * This condition verifies whether a building block inherits
 * from the class it is supposed to.
 * 
 */
public class BuildingBlocksInheritanceCondition extends Condition implements ICondition
{
    /**
     * Enumerator holds all building block types.
     */
    private enum BuildingBlockType
    {
        ACTIVITY, SERVICE, CONTENT_PROVIDER, BROADCAST_RECEIVER
    }

    private static final String ANDROID_NAME_NODE_ATTRIBUTE = "android:name"; //$NON-NLS-1$

    private static final String ACTIVITY_NODE_NAME = "activity"; //$NON-NLS-1$

    private static final String PROVIDER_NODE_NAME = "provider"; //$NON-NLS-1$

    private static final String RECEIVER_NODE_NAME = "receiver"; //$NON-NLS-1$

    private static final String SERVICE_NODE_NAME = "service"; //$NON-NLS-1$

    private static final String MANIFEST_NODE_NAME = "manifest"; //$NON-NLS-1$

    private static final String PACKAGE_NAME_NODE_ATTRIBUTE = "package"; //$NON-NLS-1$

    private static final String ACTIVITY_CLASS_NAME = "android.app.Activity"; //$NON-NLS-1$

    private static final String CONTENT_PROVIDER_CLASS_NAME = "android.content.ContentProvider"; //$NON-NLS-1$

    private static final String BROADCAST_RECEIVER_CLASS_NAME = "android.content.BroadcastReceiver"; //$NON-NLS-1$

    private static final String SERVICE_CLASS_NAME = "android.app.Service"; //$NON-NLS-1$

    private static String DEFAULT_PACKAGE_REPRESENTATION_REGULAR_EXPRESSION = "\\."; //$NON-NLS-1$

    private static String DEFAULT_PACKAGE_REPRESENTATION = "."; //$NON-NLS-1$

    private static char PACKAGE_SEPARATOR = '.'; //$NON-NLS-1$

    private static char PATH_SEPARATOR = '/'; //$NON-NLS-1$

    private static String COLUMN_STRING = ";"; //$NON-NLS-1$

    private static int ZERO_INDEX = 0;

    private static int FIRST_INDEX = 1;

    private static String[] KNOWN_ACITIVITES_IMPLEMENTATIONS = new String[]
    {
            "android.app.Activity", "android.accounts.AccountAuthenticatorActivity",
            "android.app.ActivityGroup", "android.app.AliasActivity",
            "android.app.ExpandableListActivity", "android.app.ListActivity",
            "android.app.NativeActivity", "android.app.LauncherActivity",
            "android.preference.PreferenceActivity", "android.app.TabActivity"
    };

    private static String[] KNOWN_CONTENT_PROVIDERS_IMPLEMENTATIONS = new String[]
    {
            "android.content.ContentProvider", "android.test.mock.MockContentProvider",
            "android.content.SearchRecentSuggestionsProvider"
    };

    private static String[] KNOWN_BROADCAST_RECEIVERS_IMPLEMENTATIONS = new String[]
    {
            "android.content.BroadcastReceiver", "android.appwidget.AppWidgetProvider",
            "android.app.admin.DeviceAdminReceiver"
    };

    private static String[] KNOWN_SERVICES_IMPLEMENTATIONS = new String[]
    {
            "android.app.Service", "android.inputmethodservice.AbstractInputMethodService",
            "android.accessibilityservice.AccessibilityService", "android.app.IntentService",
            "android.speech.RecognitionService", "android.widget.RemoteViewsService",
            "android.service.wallpaper.WallpaperService",
            "android.inputmethodservice.InputMethodService"
    };

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        return (CanExecuteConditionStatus) CheckerUtils
                .isAndroidManifestFileExistent(data, getId());
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.devicespecification.PlatformRules, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        XMLElement document = data.getManifestElement();
        Document manifestDoc = document.getDocument();

        List<SourceFolderElement> folders = data.getJavaModel();

        // get all missdeclared classes from all building blocks
        List<String> missDeclaredActivities =
                getMissDeclaredBuildingBlocks(manifestDoc, folders, BuildingBlockType.ACTIVITY);

        List<String> missDeclaredContentProviders =
                getMissDeclaredBuildingBlocks(manifestDoc, folders,
                        BuildingBlockType.CONTENT_PROVIDER);

        List<String> missDeclaredBroadcastReceivers =
                getMissDeclaredBuildingBlocks(manifestDoc, folders,
                        BuildingBlockType.BROADCAST_RECEIVER);

        List<String> missDelcaredServices =
                getMissDeclaredBuildingBlocks(manifestDoc, folders, BuildingBlockType.SERVICE);

        // having all missdeclared building blocks, add their appropriated error messages
        addValidationResults(results, missDeclaredActivities, BuildingBlockType.ACTIVITY,
                valManagerConfig);

        addValidationResults(results, missDeclaredBroadcastReceivers,
                BuildingBlockType.BROADCAST_RECEIVER, valManagerConfig);

        addValidationResults(results, missDeclaredContentProviders,
                BuildingBlockType.CONTENT_PROVIDER, valManagerConfig);

        addValidationResults(results, missDelcaredServices, BuildingBlockType.SERVICE,
                valManagerConfig);
    }

    /**
     * Get the default package name from the AndroidManifest.xml file.
     * 
     * @param manifestDoc {@link Document} representing the AndroidManifest.xml file.
     * 
     * @return Returns the default package name.
     */
    private String getDefaultPackageName(Document manifestDoc)
    {
        NodeList nodeList = manifestDoc.getElementsByTagName(MANIFEST_NODE_NAME);
        return nodeList.item(ZERO_INDEX).getAttributes().getNamedItem(PACKAGE_NAME_NODE_ATTRIBUTE)
                .getNodeValue();
    }

    /**
     * Add validation results to the results list.
     * 
     * @param results {@link ValidationResult} list where
     * the {@link ValidationResultData} is added.
     * @param missDelcaredBuildingBlocks The list of missdeclared building blocks
     * to be managed.
     * @param buildingBlockType {@link BuildingBlockType}.
     * @param valManagerConfig Validation manager result
     */
    private void addValidationResults(ValidationResult results,
            List<String> missDelcaredBuildingBlocks, BuildingBlockType buildingBlockType,
            ValidationManagerConfiguration valManagerConfig)
    {
        String buildingBlockClassName = null;
        String buildingBockText = null;

        switch (buildingBlockType)
        {
            case ACTIVITY:
                buildingBlockClassName = ACTIVITY_CLASS_NAME;
                buildingBockText = CheckerNLS.BuildingBlocksInheritanceCondition_Activity;
                break;
            case BROADCAST_RECEIVER:
                buildingBlockClassName = BROADCAST_RECEIVER_CLASS_NAME;
                buildingBockText = CheckerNLS.BuildingBlocksInheritanceCondition_BroadcastReceiver;
                break;
            case CONTENT_PROVIDER:
                buildingBlockClassName = CONTENT_PROVIDER_CLASS_NAME;
                buildingBockText = CheckerNLS.BuildingBlocksInheritanceCondition_ContentProvider;
                break;
            case SERVICE:
                buildingBlockClassName = SERVICE_CLASS_NAME;
                buildingBockText = CheckerNLS.BuildingBlocksInheritanceCondition_Service;
                break;
        }

        if ((missDelcaredBuildingBlocks != null) && (missDelcaredBuildingBlocks.size() > 0))
        {
            ValidationResultData validationResult = null;
            for (String missDelcaredBuildingBlock : missDelcaredBuildingBlocks)
            {
                validationResult = new ValidationResultData();
                validationResult.setConditionID(getId());
                validationResult
                        .setIssueDescription(CheckerNLS
                                .bind(CheckerNLS.BuildingBlockDeclarationCondition_TheClassXShouldExtendBuildingBlockY,
                                        new String[]
                                        {
                                                missDelcaredBuildingBlock, buildingBockText
                                        }));
                validationResult
                        .setQuickFixSuggestion(CheckerNLS
                                .bind(CheckerNLS.BuildingBlocksInheritanceCondition_TheBuildingBlockXShouldExtendClassY,
                                        new String[]
                                        {
                                                missDelcaredBuildingBlock, buildingBlockClassName
                                        }));
                validationResult.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(),
                        getId(), valManagerConfig));
                validationResult.setSeverity(getSeverityLevel());
                results.addValidationResult(validationResult);
            }
        }
    }

    /**
     * Retrieve the list of missdeclared building blocks of
     * a certain {@link BuildingBlockType}.
     * 
     * @param manifestDoc AndroidManifest.xml {@link Document} representation.
     * @param folders The {@link SourceFolderElement} list which represents
     * the Android Project.
     * @param buildingBlockType The building block type to be analyzed.
     * 
     * @return Returns the list of missdeclared building blocks for a certain 
     * {@link BuildingBlockType}.
     */
    private List<String> getMissDeclaredBuildingBlocks(Document manifestDoc,
            List<SourceFolderElement> folders, BuildingBlockType buildingBlockType)
    {
        String selectedNodeName = null;
        String[] selectedBuildingBlockClassNameList = null;

        // determined the nodes and classes to search accoding to the building block
        switch (buildingBlockType)
        {
            case ACTIVITY:
                selectedNodeName = ACTIVITY_NODE_NAME;
                selectedBuildingBlockClassNameList = KNOWN_ACITIVITES_IMPLEMENTATIONS;
                break;
            case CONTENT_PROVIDER:
                selectedNodeName = PROVIDER_NODE_NAME;
                selectedBuildingBlockClassNameList = KNOWN_CONTENT_PROVIDERS_IMPLEMENTATIONS;
                break;
            case BROADCAST_RECEIVER:
                selectedNodeName = RECEIVER_NODE_NAME;
                selectedBuildingBlockClassNameList = KNOWN_BROADCAST_RECEIVERS_IMPLEMENTATIONS;
                break;
            case SERVICE:
                selectedNodeName = SERVICE_NODE_NAME;
                selectedBuildingBlockClassNameList = KNOWN_SERVICES_IMPLEMENTATIONS;
                break;

        }

        List<String> missDeclaredBuldingBlocks = new ArrayList<String>();
        Node buildingBlockNode = null;
        Node nodeName = null;
        String buildingBlockName = null;
        String sourceFileName = null;
        String superClassName = null;
        List<SourceFileElement> files = null;

        // get all building block nodes
        NodeList applicationNodes = manifestDoc.getElementsByTagName(selectedNodeName);
        // For each building block node, check whether its superclass is
        // supposed parent
        if ((applicationNodes != null) && (applicationNodes.getLength() > 0))
        {
            for (int index = 0; index < applicationNodes.getLength(); index++)
            {
                // get the building block name
                buildingBlockNode = applicationNodes.item(index);
                nodeName =
                        buildingBlockNode.getAttributes().getNamedItem(ANDROID_NAME_NODE_ATTRIBUTE);
                if (nodeName != null)
                {
                    buildingBlockName = nodeName.getNodeValue();
                    // get the full package name of the building block
                    buildingBlockName = adjustFullPackageName(buildingBlockName, manifestDoc);
                    // Find the equivalent source of the building block and check
                    // its superclass.
                    for (SourceFolderElement folder : folders)
                    {
                        files = folder.getSourceFileElements();
                        if ((files != null) && (files.size() > 0))
                        {
                            for (SourceFileElement file : files)
                            {
                                // for executing the checker, for now it cannot be an inner class
                                // TODO : this verification can be removed after the correct class name is retrieved
                                if (!file.isInnerClass())
                                {
                                    sourceFileName = file.getClassFullPath();
                                    sourceFileName = getFullPackagePathFromFullPath(sourceFileName);
                                    if ((sourceFileName != null)
                                            && sourceFileName.equals(buildingBlockName))
                                    {
                                        superClassName = file.getSuperclassName();
                                        superClassName =
                                                getFullPackagePathFromFullPath(superClassName);
                                        if (!Arrays.asList(selectedBuildingBlockClassNameList)
                                                .contains(superClassName))
                                        {
                                            // The superclass is not the expected one!
                                            missDeclaredBuldingBlocks.add(sourceFileName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // retrieve the list of miss declared building blocks
        return missDeclaredBuldingBlocks;
    }

    /**
     * Given the full path to a Java file, its full packaged-name representation
     * is returned.
     * 
     * @param fullPath The full path of a Java file.
     * 
     * @return Returns the full package-name of a Java file.
     */
    public String getFullPackagePathFromFullPath(String fullPath)
    {
        String fullPackageAndClassName = null;
        fullPackageAndClassName = fullPath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
        if (fullPackageAndClassName.endsWith(COLUMN_STRING))
        {
            fullPackageAndClassName = fullPackageAndClassName.split(COLUMN_STRING)[ZERO_INDEX];
        }

        return fullPackageAndClassName;
    }

    /**
     * Set the full package name for the building block.
     * 
     * @param buildingBlockName Building block name.
     * @param manifestDoc AndroidManifest.xml {@link Document}.
     * 
     * @return Returns the building block with its full package name.
     */
    private String adjustFullPackageName(String buildingBlockName, Document manifestDoc)
    {
        String defaultPackageName = getDefaultPackageName(manifestDoc);
        if (!buildingBlockName.contains(defaultPackageName))
        {
            if (buildingBlockName.contains(DEFAULT_PACKAGE_REPRESENTATION))
            {
                buildingBlockName =
                        buildingBlockName.split(DEFAULT_PACKAGE_REPRESENTATION_REGULAR_EXPRESSION)[FIRST_INDEX];
            }
            buildingBlockName = defaultPackageName + PACKAGE_SEPARATOR + buildingBlockName;
        }

        return buildingBlockName;
    }
}
