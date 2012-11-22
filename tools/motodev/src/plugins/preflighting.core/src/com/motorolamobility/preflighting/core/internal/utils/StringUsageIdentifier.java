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
package com.motorolamobility.preflighting.core.internal.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.core.applicationdata.XMLElement;

/**
 * Utility class to help identifying strings that are used inside XML files 
 */
public class StringUsageIdentifier
{
    private static final String STRING_USAGE_PATTERN = "@string/";

    /**
     * Identify strings used inside XML files
     * @param xmlElements representation of the XML in the App validator
     * @return set with strings used
     */
    public static Set<String> identifyStringsUsed(List<XMLElement> xmlElements)
    {
        Set<String> stringsUsed = new HashSet<String>();
        if (xmlElements != null)
        {
            for (XMLElement xml : xmlElements)
            {
                Document document = xml.getDocument();
                for (Node node = document.getFirstChild(); node != null; node =
                        node.getNextSibling())
                {
                    visitNode(stringsUsed, node);
                }
            }
        }
        return stringsUsed;
    }

    /**
     * Visit the node
     * @param stringsUsed set to keep the strings used
     * @param node xml element to visit
     */
    private static void visitNode(Set<String> stringsUsed, Node node)
    {
        checkStringsUsedInNode(stringsUsed, node);
        if (node.hasChildNodes())
        {
            NodeList list = node.getChildNodes();
            for (int i = 0; i < list.getLength(); i++)
            {
                Node subnode = list.item(i);
                visitNode(stringsUsed, subnode);
            }
        }
    }

    /**
     * Visits attributes from the node
     * @param stringsUsed set to keep the strings used
     * @param node xml element to visit
     */
    private static void checkStringsUsedInNode(Set<String> stringsUsed, Node node)
    {
        if (node.getNodeType() != Node.COMMENT_NODE)
        {
            NamedNodeMap map = node.getAttributes();
            if (map != null)
            {
                for (int index = 0; index < map.getLength(); index++)
                {
                    Node atr = map.item(index);
                    if ((atr != null) && (atr.getNodeValue() != null)
                            && !atr.getNodeValue().trim().equals("")) //$NON-NLS-1$
                    {
                        String value = atr.getNodeValue();
                        readStringId(stringsUsed, value);
                    }
                }
            }
        }

    }

    /**
     * Check if attribute contains {@link StringUsageIdentifier#STRING_USAGE_PATTERN} and if so, get the string id  
     * @param stringsUsed set to keep the strings used
     * @param value attribute node value
     */
    private static void readStringId(Set<String> stringsUsed, String value)
    {
        int i = value.indexOf(STRING_USAGE_PATTERN);
        if (i >= 0)
        {
            //cut text after "@string/
            String stringId = value.substring(i + STRING_USAGE_PATTERN.length());
            stringsUsed.add(stringId);
        }
    }
}
