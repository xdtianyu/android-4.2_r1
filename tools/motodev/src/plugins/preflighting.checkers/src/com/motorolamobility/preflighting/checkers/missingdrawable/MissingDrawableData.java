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
package com.motorolamobility.preflighting.checkers.missingdrawable;

import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.ElementUtils;
import com.motorolamobility.preflighting.core.applicationdata.FolderElement;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;

/**
 * Keeps information about the drawable folders into application
 */
public class MissingDrawableData
{
    /*
     * Android manifest related constants
     */
    private final int MIN_SDK_THRESHOLD = 3;

    private final int MAX_XHDPI_SDK_THRESHOLD = 9;

    private final String TAG_NAME_USES_SDK = "uses-sdk"; //$NON-NLS-1$

    private final String TAG_NAME_SUPPORTS_SCREENS = "supports-screens"; //$NON-NLS-1$

    private final String ATTRIBUTE_ANY_DENSITY = "android:anyDensity"; //$NON-NLS-1$

    private final String ATTRIBUTE_MIN_SDK_VERSION = "android:minSdkVersion"; //$NON-NLS-1$

    private final String ATTRIBUTE_MAX_SDK_VERSION = "android:maxSdkVersion"; //$NON-NLS-1$

    private boolean standardFolderExists = false;

    private boolean ldpiFolderExists = false;

    private boolean mdpiFolderExists = false;

    private boolean hdpiFolderExists = false;

    private boolean xhdpiFolderExists = false;

    /**
     * The application manifest
     */
    private XMLElement manifestElement;

    /**
     * xhdpi resolution, available for api level 9 or above  
     */
    private boolean isXhdpiApplicable = true;

    private ResourcesFolderElement resFolder;

    //A list of all existing drawable elements names
    private HashSet<String> drawableElements = new HashSet<String>();

    private boolean testApplicable;

    public MissingDrawableData(ApplicationData appData)
    {
        this.manifestElement = appData.getManifestElement();

        // Retrieve all images declared in all drawable folders
        List<Element> folderResElements =
                ElementUtils.getElementByType(appData.getRootElement(), Type.FOLDER_RES);
        resFolder =
                folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements.get(0)
                        : null;
        // Check if all 4 resolution folders and the standard folder exist
        if (resFolder.getDrawableFolder() != null)
        {
            standardFolderExists = true;
        }

        // ldpi folder
        if (resFolder.getLdpiDrawableFolder() != null)
        {
            ldpiFolderExists = true;
        }

        // mdpi folder
        if (resFolder.getMdpiDrawableFolder() != null)
        {
            mdpiFolderExists = true;
        }

        // hdpi folder
        if (resFolder.getHdpiDrawableFolder() != null)
        {
            hdpiFolderExists = true;
        }

        // hdpi folder
        if (resFolder.getXhdpiDrawableFolder() != null)
        {
            xhdpiFolderExists = true;
        }

        // Construct a list of unique drawable elements
        List<FolderElement> drawableFolders = resFolder.getDrawableFolders();
        for (FolderElement f : drawableFolders)
        {
            for (Element e : ElementUtils.getElementByType(f, Type.FILE_DRAWABLE))
            {
                drawableElements.add(e.getName());
            }
        }

        checkTestApplicability();
    }

    /**
     * @return the resFolder
     */
    protected ResourcesFolderElement getResFolder()
    {
        return resFolder;
    }

    /**
     * @return the standardFolderExists
     */
    protected boolean isStandardFolderExists()
    {
        return standardFolderExists;
    }

    /**
     * @return the ldpiFolderExists
     */
    protected boolean isLdpiFolderExists()
    {
        return ldpiFolderExists;
    }

    /**
     * @return the mdpiFolderExists
     */
    protected boolean isMdpiFolderExists()
    {
        return mdpiFolderExists;
    }

    /**
     * @return the hdpiFolderExists
     */
    protected boolean isHdpiFolderExists()
    {
        return hdpiFolderExists;
    }

    /**
     * @return the xhdpiFolderExists
     */
    protected boolean isXhdpiFolderExists()
    {
        return xhdpiFolderExists;
    }

    /**
     * @return the isXhdpiApplicable
     */
    protected boolean isXhdpiApplicable()
    {
        return isXhdpiApplicable;
    }

    /**
     * @return the drawableElements
     */
    protected HashSet<String> getDrawableElements()
    {
        return drawableElements;
    }

    /**
     * Auxiliary method to determine is this test is applicable at all to the given application. If not, the checker will return an OK status.
     * If the minSdkVersion is 3 (or lower) or the <supports-screen> tag does not exit, return an OK result.
     * @param data The application data.
     * @return A boolean stating if the the test is applicable
     */
    private boolean checkTestApplicability()
    {
        testApplicable = true;

        Document manifestDoc = manifestElement.getDocument();
        isXhdpiApplicable = true;
        // Validate <uses-sdk> node.
        NodeList usesSdkList = manifestDoc.getElementsByTagName(TAG_NAME_USES_SDK);
        if (usesSdkList.getLength() > 0)
        {
            for (int i = 0; i < usesSdkList.getLength(); i++)
            {
                Node usesSdkNode = usesSdkList.item(i);
                Node minSdkAttribute =
                        usesSdkNode.getAttributes().getNamedItem(ATTRIBUTE_MIN_SDK_VERSION);
                if (minSdkAttribute != null)
                {
                    int minSdk = Integer.parseInt(minSdkAttribute.getNodeValue());
                    if (minSdk <= MIN_SDK_THRESHOLD)
                    {
                        testApplicable = false;
                    }
                }
                else
                {
                    testApplicable = false;
                }
                Node maxSdkAttribute =
                        usesSdkNode.getAttributes().getNamedItem(ATTRIBUTE_MAX_SDK_VERSION);

                if (maxSdkAttribute != null)
                {
                    int maxSdk = Integer.parseInt(maxSdkAttribute.getNodeValue());
                    if (maxSdk < MAX_XHDPI_SDK_THRESHOLD)
                    {
                        isXhdpiApplicable = false;
                    }
                }
            }
        }
        else
        {
            testApplicable = false;
        }

        if (testApplicable)
        {
            // Validate <supports-screen> node.
            NodeList supportsScreensList =
                    manifestDoc.getElementsByTagName(TAG_NAME_SUPPORTS_SCREENS);
            if (supportsScreensList.getLength() > 0)
            {
                for (int i = 0; i < supportsScreensList.getLength(); i++)
                {
                    Node supportsScreensNode = supportsScreensList.item(i);
                    Node anyDensityAttribute =
                            supportsScreensNode.getAttributes().getNamedItem(ATTRIBUTE_ANY_DENSITY);
                    if (anyDensityAttribute != null)
                    {
                        boolean anyDensity =
                                Boolean.parseBoolean(anyDensityAttribute.getNodeValue());
                        if (!anyDensity)
                        {
                            testApplicable = false;
                        }
                    }

                }
            }
        }

        return testApplicable;
    }

    /**
     * Keep the result of {@link MissingDrawableData#checkTestApplicability() } to avoid running it for all conditions
     * Warning: Guarantee that {@link MissingDrawableData#checkTestApplicability() } is run once before calling this method.
     * @return A boolean stating if the the test is applicable
     */
    protected boolean isTestApplicable()
    {
        return testApplicable;
    }

    /**
     * @return true if at least one folder (ldpi, mdpi, hdpi or xhdpi exists), false otherwise.
     */
    public boolean atLeastOneDrawableFolderExist()
    {
        return (isLdpiFolderExists() || isMdpiFolderExists() || isHdpiFolderExists() || (isXhdpiFolderExists() && isXhdpiApplicable()));
    }

    /**
     * @return true if at least one folder (ldpi, mdpi, hdpi or xhdpi is missing), false otherwise.
     */
    public boolean isMissingAtLeastOneDrawableFolder()
    {
        return (!isLdpiFolderExists() || !isMdpiFolderExists() || !isHdpiFolderExists() || (!isXhdpiFolderExists() && isXhdpiApplicable()));
    }
}
