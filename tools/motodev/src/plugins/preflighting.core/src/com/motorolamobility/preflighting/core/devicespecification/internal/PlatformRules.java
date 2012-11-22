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
package com.motorolamobility.preflighting.core.devicespecification.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.internal.permissionfeature.PermissionToFeatureMapReader;
import com.motorolamobility.preflighting.core.internal.permissionfeature.PermissionToFeatureMapping;
import com.motorolamobility.preflighting.core.internal.utils.MethodPermissionCSVReader;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.PermissionGroups;

/**
 * Singleton that contains rules that are specific from the Android Platform
 */
public class PlatformRules
{
    /**
     * XML from API level to permission
     */
    private static final String API_LEVEL_PERMISSION_MAP = "files/apilevel-permission-map.xml"; //$NON-NLS-1$

    /**
     * XML from feature to API level 
     */
    private static final String FEATURE_API_LEVEL_MAP = "files/feature-apilevel-map.xml"; //$NON-NLS-1$

    /**
     * XML from permission to implied features
     */
    private static final String PERMISSION_FEATURE_MAP = "files/permission-feature-map.xml"; //$NON-NLS-1$

    /**
     * Text file from blocked permissions
     */
    private static final String BLOCKED_PERMISSIONS_LIST = "files/blocked_permissions.txt"; //$NON-NLS-1$

    /**
     * Collection of method signature to permission required 
     */
    private static final String METHOD_PERMISSION_LIST = "files/method_permission_list_4.0.csv"; //$NON-NLS-1$

    private static PlatformRules uniqInstance;

    /**
     * Map from api level (key) to {@link Permission}
     */
    private HashMap<Integer, ArrayList<Permission>> permissionsList = null;

    // private HashMap<Integer, ArrayList<Feature>> featuresList = null;

    /**
     * Map from api level (key) to {@link Feature}
     */
    private HashMap<Integer, Feature> featuresList = null;

    /**
     * Map from method signature (key) to permissions required
     */
    private Map<String, PermissionGroups> methodToPermissionGroup =
            new HashMap<String, PermissionGroups>();

    //private final ArrayList<String> permissionsBySignatureList = new ArrayList<String>();

    private PermissionToFeatureMapping permissionToFeatureMapping = null;

    /**
     * List of blocked permissions
     */
    private final ArrayList<String> blockedPermissionsList = new ArrayList<String>();

    /**
     * <li>API level 1 <li>Android platform 1.0
     */
    public static final int API_LEVEL_1 = 1;

    /**
     * <li>API level 2 <li>Android platform 1.1
     */
    public static final int API_LEVEL_2 = 2;

    /**
     * <li>API level 3 <li>Android platform 1.5 (Cupcake) <li>Based on Linux
     * Kernel 2.6.27
     */
    public static final int API_LEVEL_3 = 3;

    /**
     * <li>API level 4 <li>Android platform 1.6 (Donut) <li>Based on Linux
     * Kernel 2.6.29
     */
    public static final int API_LEVEL_4 = 4;

    /**
     * <li>API level 5 <li>Android platform 2.0 (Eclair) <li>Based on Linux
     * Kernel 2.6.29
     */
    public static final int API_LEVEL_5 = 5;

    /**
     * <li>API level 6 <li>Android platform 2.0.1 (Eclair) <li>Based on Linux
     * Kernel 2.6.29
     */
    public static final int API_LEVEL_6 = 6;

    /**
     * <li>API level 7 <li>Android platform 2.1 (Eclair) <li>Based on Linux
     * Kernel 2.6.29
     */
    public static final int API_LEVEL_7 = 7;

    /**
     * <li>API level 8 <li>Android platform 2.2 (Froyo) <li>Based on Linux
     * Kernel 2.6.32
     */
    public static final int API_LEVEL_8 = 8;

    /**
     * Singleton instance.
     */
    private PlatformRules()
    {
        loadAllPermissions();
        loadAllFeatures();
        loadPermissionToRequiredImpliedFeatures();
        loadBlockedPermissions();
        loadMethodToPermissionList();
    }

    /**
     * Returns the unique instance of PlatformRules
     * 
     * @return The unique instance of PlatformRules
     */
    public static synchronized PlatformRules getInstance()
    {
        if (uniqInstance == null)
        {
            uniqInstance = new PlatformRules();
        }
        return uniqInstance;
    }

    /**
     * Returns a list of permissions related to a given Android API level.
     * 
     * @param apiLevel The API Level. These values are constants of this {@link PlatformRules}
     * class. You have {@link PlatformRules#API_LEVEL_1}, {@link PlatformRules#API_LEVEL_2} and so on. 
     * 
     * @return An ArrayList of Strings containing all permission of a given
     *         Android API level -OR- returns a null ArrayList if there are no
     *         permissions or if the API level number is not valid/supported.
     */
    public ArrayList<String> getPermissions(int apiLevel)
    {
        ArrayList<String> permissions = null;
        if ((permissionsList != null) && (isApiLevelSupported(apiLevel))
                && (permissionsList.containsKey(apiLevel)))
        {
            ArrayList<Permission> permissionsForThisLevel = null;
            Set<Entry<Integer, ArrayList<Permission>>> entries = permissionsList.entrySet();
            for (Entry<Integer, ArrayList<Permission>> entry : entries)
            {
                if (apiLevel == entry.getKey().intValue())
                {
                    permissionsForThisLevel = entry.getValue();
                }
            }
            if ((permissionsForThisLevel != null) && (!permissionsForThisLevel.isEmpty()))
            {
                permissions = new ArrayList<String>();
                for (Permission permission : permissionsForThisLevel)
                {
                    permissions.add(permission.getPermissionName());
                }
            }
        }
        return permissions;
    }

    /**
     * Returns all features related to a given Android API level.
     * 
     * @param apiLevel The API Level. These values are constants of this {@link PlatformRules}
     * class. You have {@link PlatformRules#API_LEVEL_1}, {@link PlatformRules#API_LEVEL_2} and so on.
     * 
     * @return An ArrayList of Strings containing all features of a given
     *         Android API level -OR- returns a null ArrayList if there are no
     *         features or if the API level number is not valid/supported.
     */
    public ArrayList<String> getFeatures(int apiLevel)
    {
        ArrayList<String> features = null;
        if ((featuresList != null) && (isApiLevelSupported(apiLevel))
                && (featuresList.containsKey(apiLevel)))
        {
            Set<Entry<Integer, Feature>> entries = featuresList.entrySet();
            features = new ArrayList<String>();
            for (Entry<Integer, Feature> entry : entries)
            {
                if (apiLevel == entry.getKey().intValue())
                {
                    features.add(entry.getValue().getFeatureID());
                }
            }
        }
        return features;
    }

    /**
     * Returns all permissions related to a given method signature.
     * 
     * @param signature The method's signature. In order to better understand how these
     * signatures are created, see {@link Invoke#getQualifiedName()}, {@link SourceFolderElement#getInvokedMethods()} documentation.
     * The {@link Invoke#getQualifiedName()} method returns this signature.
     *
     * @return Given a Method signature, its {@link PermissionGroups}
     * are returned.
     * 
     * @see {@link Invoke#getQualifiedName()}, {@link SourceFolderElement#getInvokedMethods()} 
     */
    public PermissionGroups getPermissionsForMethod(String signature)
    {
        return methodToPermissionGroup.get(signature);
    }

    /**
     * Verifies if the API level number is valid/supported.
     * 
     * @param apiLevel The API Level. These values are constants of this {@link PlatformRules}
     * class. You have {@link PlatformRules#API_LEVEL_1}, {@link PlatformRules#API_LEVEL_2} and so on.
     * 
     * @return Returns <code>true</code> in case the API Level is supported,
     * <code>false</code> otherwise.
     */
    public boolean isApiLevelSupported(int apiLevel)
    {
        boolean supported = false;
        if ((apiLevel >= API_LEVEL_1) && (apiLevel <= API_LEVEL_8))
        {
            supported = true;
        }
        return supported;
    }

    /**
     * Load all permission.
     */
    private void loadAllPermissions()
    {
        ArrayList<Permission> permissions = null;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        NodeList apiLevelNodes = loadXmlFiles(builderFactory, documentBuilder, "api_level", //$NON-NLS-1$
                //                "C:\\tmp\\apilevel-permission-map.xml"); // for JUnit purposes. //$NON-NLS-1$
                API_LEVEL_PERMISSION_MAP);
        int apiLevel = 0;

        if ((apiLevelNodes != null) && (apiLevelNodes.getLength() > 0))
        {
            permissions = new ArrayList<Permission>();
            for (int i = 0; i < apiLevelNodes.getLength(); i++)
            {
                Element apiLevelTag = (Element) apiLevelNodes.item(i);
                apiLevel = Integer.parseInt(apiLevelTag.getAttribute("number")); //$NON-NLS-1$
                NodeList permissionNodes = apiLevelTag.getElementsByTagName("permission"); //$NON-NLS-1$
                for (int j = 0; j < permissionNodes.getLength(); j++)
                {
                    Element permissionTag = (Element) permissionNodes.item(j);
                    if (permissionTag != null)
                    {
                        String name = null;
                        String description = null;
                        boolean deprecated = false;
                        try
                        {
                            name = getChildNodes(permissionTag.getElementsByTagName("name")); //$NON-NLS-1$
                            description =
                                    getChildNodes(permissionTag.getElementsByTagName("description")); //$NON-NLS-1$
                            deprecated =
                                    Boolean.parseBoolean(getChildNodes(permissionTag
                                            .getElementsByTagName("deprecated"))); //$NON-NLS-1$
                        }
                        catch (Exception e)
                        {

                            e.printStackTrace();
                        }
                        if (name != null)
                        {
                            Permission permission =
                                    new Permission(apiLevel, name, description, deprecated);
                            permissions.add(permission);
                        }
                    }
                }
            }
            if ((!permissions.isEmpty()) && (apiLevel != 0))
            {
                permissionsList = new HashMap<Integer, ArrayList<Permission>>();
                permissionsList.put(apiLevel, permissions);
            }
        }
    }

    /**
     * Load all features.
     */
    private void loadAllFeatures()
    {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        NodeList featureApiNodes = loadXmlFiles(builderFactory, documentBuilder, "feature", //$NON-NLS-1$
                //                "C:\\tmp\\feature-apilevel-map.xml"); // for JUnit purposes. //$NON-NLS-1$
                FEATURE_API_LEVEL_MAP);
        int apiLevel = 0;

        if ((featureApiNodes != null) && (featureApiNodes.getLength() > 0))
        {
            featuresList = new HashMap<Integer, Feature>();
            for (int i = 0; i < featureApiNodes.getLength(); i++)
            {
                Element featureTag = (Element) featureApiNodes.item(i);
                if (featureTag != null)
                {
                    String featureID = null;
                    int sinceApiLevel = 0;
                    try
                    {
                        featureID = getChildNodes(featureTag.getElementsByTagName("id")); //$NON-NLS-1$
                        sinceApiLevel =
                                Integer.parseInt(getChildNodes(featureTag
                                        .getElementsByTagName("since_apilevel"))); //$NON-NLS-1$
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    if ((featureID != null) && (isApiLevelSupported(sinceApiLevel)))
                    {
                        Feature feature = new Feature(featureID, sinceApiLevel);
                        featuresList.put(apiLevel, feature);
                    }
                }
            }
        }

    }

    /**
     * Load XML file into a {@link NodeList} structure. The basis for
     * the {@link NodeList} is the tag paramter.
     * 
     * @param builderFactory Builder factory object.
     * @param documentBuilder Builder object.
     * @param tag Tag which will serve as the basis for bringing
     * the {@link NodeList}.
     * @param fileName The XML file Name.
     * 
     * @return Returns a {@link NodeList} from a XML file based on
     * the tag parameter.
     */
    private NodeList loadXmlFiles(DocumentBuilderFactory builderFactory,
            DocumentBuilder documentBuilder, String tag, String fileName)
    {
        try
        {
            documentBuilder = builderFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e1)
        {

            e1.printStackTrace();
        }
        Document doc = null;
        try
        {
            // For JUnit tests purpose
            //            doc = documentBuilder.parse(new File(fileName));
            // -------------------------
            doc =
                    documentBuilder.parse(PreflightingCorePlugin.getContext().getBundle()
                            .getEntry(fileName).openStream());
        }
        catch (SAXException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return doc.getDocumentElement().getElementsByTagName(tag);
    }

    /**
     * Given a {@link NodeList} consider only the first element. Return
     * its first child큦 value.
     * 
     * @param node {@link NodeList} where only the first child will
     * be considered.
     * 
     * @return Returns the value of the {@link NodeList}큦 first element큦
     * first child.
     * 
     * @throws DOMException Exception thrown in case there are problems
     * handling the {@link NodeList}. 
     */
    private String getChildNodes(NodeList node) throws DOMException
    {
        if ((node != null) && (node.getLength() > 0))
        {
            Element nameElement = (Element) node.item(0);
            if ((nameElement != null) && (nameElement.hasChildNodes()))
            {
                Node childTag = nameElement.getFirstChild();
                if (childTag != null)
                {
                    return childTag.getNodeValue();
                }
            }
        }
        return null;
    }

    private void loadMethodToPermissionList()
    {
        InputStream csvStream = null;
        InputStreamReader streamReader = null;
        try
        {
            csvStream =
                    PreflightingCorePlugin.getContext().getBundle()
                            .getEntry(METHOD_PERMISSION_LIST).openStream();
            streamReader = new InputStreamReader(csvStream);
            methodToPermissionGroup =
                    MethodPermissionCSVReader.readMapMethodToPermission(streamReader);
        }
        catch (IOException e)
        {
            PreflightingLogger
                    .error("Problem reading " + METHOD_PERMISSION_LIST + " exception: " + e.getLocalizedMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        finally
        {
            if (streamReader != null)
            {
                try
                {
                    streamReader.close();
                }
                catch (IOException e)
                {
                    //Do Nothing.
                }
            }
            try
            {
                if (csvStream != null)
                {
                    csvStream.close();
                }
            }
            catch (IOException e)
            {
                //do nothing
            }
        }
    }

    private void loadBlockedPermissions()
    {
        InputStream txtStream = null;
        BufferedReader br = null;
        try
        {
            txtStream =
                    PreflightingCorePlugin.getContext().getBundle()
                            .getEntry(BLOCKED_PERMISSIONS_LIST).openStream();
            br = new BufferedReader(new InputStreamReader(txtStream));

            String line = br.readLine();
            while (line != null)
            {
                blockedPermissionsList.add(line.trim());
                line = br.readLine();
            }
        }
        catch (IOException e)
        {
            PreflightingLogger.error("Problem reading " + BLOCKED_PERMISSIONS_LIST + " exception: "
                    + e.getLocalizedMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    //Do Nothing.
                }
            }
            try
            {
                if (txtStream != null)
                {
                    txtStream.close();
                }
            }
            catch (IOException e)
            {
                //do nothing
            }
        }
    }

    /**
     * Checks whether permission is blocked.
     * 
     * @param permission Identifier of the permission (as specified in <code>android:name</code> tag inside AndroidManifest.xml)
     * 
     * @return Returns <code>true</code> if blocked (need special system certificate), false if not
     */
    public boolean isPermissionBlocked(String permission)
    {
        return blockedPermissionsList.contains(permission);
    }

    private void loadPermissionToRequiredImpliedFeatures()
    {
        InputStream xmlStream;
        try
        {
            xmlStream =
                    PreflightingCorePlugin.getContext().getBundle()
                            .getEntry(PERMISSION_FEATURE_MAP).openStream();
            PermissionToFeatureMapReader permissionToFeatureReader =
                    new PermissionToFeatureMapReader(xmlStream);
            permissionToFeatureMapping = permissionToFeatureReader.read();
        }
        catch (Exception e)
        {
            PreflightingLogger.error("Problem reading " + PERMISSION_FEATURE_MAP + " exception: "
                    + e.getLocalizedMessage());
        }
    }

    /**
     * Gets the list of required features for a certain permission. In case none
     * is required, <code>null</code> is returned.
     * 
     * @param permissionId Permision identifier which the required featured will be
     * fetched. 
     * 
     * @return Returns the list of required features for a certain permission. In case none
     * is required, <code>null</code> is returned.
     */
    public Set<com.motorolamobility.preflighting.core.permissionfeature.Feature> getImpliedFeaturesSet(
            String permissionId)
    {
        Set<com.motorolamobility.preflighting.core.permissionfeature.Feature> features =
                new HashSet<com.motorolamobility.preflighting.core.permissionfeature.Feature>();
        if (permissionToFeatureMapping.getRequiredImpliedFeaturesForPermission(permissionId) != null)
        {
            features.addAll(permissionToFeatureMapping
                    .getRequiredImpliedFeaturesForPermission(permissionId));
        }
        return features;
    }

    /**
     * Given a certain category, all permissions related to it are retrieved.
     * 
     * @param categoryName Category name which the related permissions will be fetched.
     * 
     * @return Returns the list of permissions for a certain category.
     */
    public Set<com.motorolamobility.preflighting.core.permissionfeature.Permission> getPermissionsForCategory(
            String categoryName)
    {
        Set<com.motorolamobility.preflighting.core.permissionfeature.Permission> permissions =
                new HashSet<com.motorolamobility.preflighting.core.permissionfeature.Permission>();
        if (permissionToFeatureMapping.getPermissionsForCategory(categoryName) != null)
        {
            permissions.addAll(permissionToFeatureMapping.getPermissionsForCategory(categoryName));
        }
        return permissions;
    }

    /**
     * An inner class to represent a set of permissions for a given API level.
     */
    class Permission
    {
        private int apLevelNumber = 0;

        private String permissionName = null;

        private String permissionDescription = null;

        private boolean deprecated = false;

        /**
         * Constructor which load a set of parameters.
         * 
         * @param apiLevel API level.
         * @param name Permission name.
         * @param description Permission description.
         * @param deprecated A flag indicating whether the permission is deprecated.
         */
        Permission(int apiLevel, String name, String description, boolean deprecated)
        {
            apLevelNumber = apiLevel;
            permissionName = name;
            permissionDescription = (description != null) ? description : ""; //$NON-NLS-1$
            this.deprecated = deprecated;
        }

        /**
         * Gets the permission큦 name.
         * 
         * @return Returns the permission name.
         */
        private String getPermissionName()
        {
            return permissionName;
        }

        /**
         * Sets the permission큦 name.
         * 
         * @param permissionName
         *            The permission name to set
         */
        @SuppressWarnings("unused")
        private void setPermissionName(String permissionName)
        {
            this.permissionName = permissionName;
        }

        /**
         * Gets the permission큦 description.
         * 
         * @return Returns the permission description
         */
        @SuppressWarnings("unused")
        private String getPermissionDescription()
        {
            return permissionDescription;
        }

        /**
         * Sets the permission큦 description.
         * 
         * @param permissionDescription
         *            the permission description to set.
         */
        @SuppressWarnings("unused")
        private void setPermissionDescription(String permissionDescription)
        {
            this.permissionDescription = permissionDescription;
        }

        /**
         * The a flag indicating whether the permission is deprecated.
         * 
         * @return the Returns the flag indicating whether a permission
         * is deprecated.
         */
        @SuppressWarnings("unused")
        private boolean isDeprecated()
        {
            return deprecated;
        }

        /**
         * Set a flag indicating whether the permission is deprecated.
         * 
         * @param permissionDescription
         *            The flag for deprecation to be set.
         */
        @SuppressWarnings("unused")
        private void setDeprecated(boolean deprecated)
        {
            this.deprecated = deprecated;
        }

        /**
         * Get the API Level related to this permission.
         * 
         * @return Returns the API Level related to this permission.
         */
        @SuppressWarnings("unused")
        private int getApLevelNumber()
        {
            return apLevelNumber;
        }
    }

    /**
     * An inner class to represent a set of features and their first API level.
     */
    class Feature
    {
        private String featureID = null;

        private int sinceApiLevel = 0;

        /**
         * Constructor which sets necessary parameters.
         * 
         * @param featureID Feature identifier.
         * @param sinceApiLevel Since-when API Level.
         */
        Feature(String featureID, int sinceApiLevel)
        {
            this.featureID = featureID;
            this.sinceApiLevel = sinceApiLevel;
        }

        /**
         * Gets the feature Id.
         * 
         * @return Returns the feature Id.
         */
        private String getFeatureID()
        {
            return featureID;
        }

        /**
         * Sets the feature Id.
         * 
         * @param featureID
         *            The feature Id to be set.
         */
        @SuppressWarnings("unused")
        private void setFeatureID(String featureID)
        {
            this.featureID = featureID;
        }

        /**
         * Get the Since-when API level.
         * 
         * @return Returns the Since-when API Level.
         */
        @SuppressWarnings("unused")
        private int getSinceApiLevel()
        {
            return sinceApiLevel;
        }

        /**
         * Sets the Since-When API level.
         * 
         * @param sinceApiLevel
         *            Sets the Since-When API Level.
         */
        @SuppressWarnings("unused")
        private void setSinceApiLevel(int sinceApiLevel)
        {
            this.sinceApiLevel = sinceApiLevel;
        }
    }
}
