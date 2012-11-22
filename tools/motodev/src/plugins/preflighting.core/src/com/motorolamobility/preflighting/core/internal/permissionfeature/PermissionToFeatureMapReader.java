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
package com.motorolamobility.preflighting.core.internal.permissionfeature;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.motorolamobility.preflighting.core.permissionfeature.Feature;
import com.motorolamobility.preflighting.core.permissionfeature.Permission;

/**
 * Reads and populates permission_to_feature_map object
 */
public final class PermissionToFeatureMapReader
{
    /**
     * File to read
     */
    private final InputStream xmlStream;

    /**
     * @param xmlStream input stream from which to read the permission to implied required feature mapping
     */
    public PermissionToFeatureMapReader(InputStream xmlStream)
    {
        this.xmlStream = xmlStream;
    }

    /**
     * Reads XML set in the {@link PermissionToFeatureMapReader#xmlStream} variable
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public PermissionToFeatureMapping read() throws ParserConfigurationException, SAXException,
            IOException
    {
        PermissionToFeatureMapping permissionToFeatureMapping = new PermissionToFeatureMapping();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(xmlStream);

        NodeList categoriesList = doc.getElementsByTagName("category");
        for (int i = 0; i < categoriesList.getLength(); i++)
        {
            Node categoryNode = categoriesList.item(i);
            NamedNodeMap categoryNodeMap = categoryNode.getAttributes();
            Node categoryAtr = categoryNodeMap.getNamedItem("name");
            if ((categoryAtr != null) && !categoryAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
            {
                // add category
                String categoryName = categoryAtr.getNodeValue();
                List<Permission> permissions = new ArrayList<Permission>();
                NodeList permissionsList = categoryNode.getChildNodes();
                for (int j = 0; j < permissionsList.getLength(); j++)
                {
                    Node permissionNode = permissionsList.item(j);
                    if ((permissionNode != null)
                            && (permissionNode.getNodeType() == Node.ELEMENT_NODE))
                    {
                        NamedNodeMap permissionMap = permissionNode.getAttributes();
                        Node permissionAtr = permissionMap.getNamedItem("id");
                        if ((permissionAtr != null)
                                && !permissionAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
                        {
                            // add permission into category
                            String permId = permissionAtr.getNodeValue();
                            Permission permission = new Permission(permId);
                            permissions.add(permission);

                            // find implied required features to add
                            List<Feature> features = new ArrayList<Feature>();
                            NodeList featureList = permissionNode.getChildNodes();
                            for (int z = 0; z < featureList.getLength(); z++)
                            {
                                Node featureNode = featureList.item(z);
                                if ((featureNode != null)
                                        && (featureNode.getNodeType() == Node.ELEMENT_NODE))
                                {
                                    NamedNodeMap featureMap = featureNode.getAttributes();
                                    Node featureAtr = featureMap.getNamedItem("id");
                                    if ((featureAtr != null)
                                            && !featureAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
                                    {
                                        Feature feature = new Feature(featureAtr.getNodeValue());
                                        features.add(feature);
                                    }
                                }
                            }
                            permissionToFeatureMapping.putFeatures(permId, features);
                        }
                    }
                }
                permissionToFeatureMapping.putPermissions(categoryName, permissions);
            }
        }
        return permissionToFeatureMapping;
    }

}
