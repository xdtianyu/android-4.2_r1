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
package com.motorolamobility.preflighting.core.applicationdata;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to search for {@link Element} inside an Android project.
 */
public class ElementUtils
{
    /***
     * Returns a list of application elements for a specific type.
     * 
     * @param rootElement the root node of the Android project tree.
     * @param type used to filter the Elements that will be returned. (e.g.: {@link Element.Type#FILE_JAVA}, {@link Element.Type#FILE_LAYOUT} or {@link Element.Type#FILE_DRAWABLE}
     * @return the list of {@link Element} inside Android project that have the type specified.
     */
    public static List<Element> getElementByType(Element rootElement, Element.Type type)
    {
        List<Element> resultList = new ArrayList<Element>();
        for (Element element : rootElement.getChildren())
        {
            if (element.getType() == type)
            {
                resultList.add(element);
            }
            else if (element instanceof FolderElement)
            {
                resultList.addAll(getElementByType(element, type));
            }
        }
        return resultList;
    }

    /**
     * Gets all XML elements that are children (either directly or indirectly) of the root element  
     * @param rootElement 
     * @return list of elements that are XML files (ending with .xml)
     */
    public static List<XMLElement> getXMLElements(Element rootElement)
    {
        List<XMLElement> resultList = new ArrayList<XMLElement>();
        if (rootElement != null)
        {
            for (Element element : rootElement.getChildren())
            {
                if ((element instanceof XMLElement) && (element.getFile() != null)
                        && element.getFile().getName().endsWith(".xml"))
                {
                    resultList.add((XMLElement) element);
                }
                else if (element instanceof FolderElement)
                {
                    resultList.addAll(getXMLElements(element));
                }
            }
        }
        return resultList;
    }

    /**
     * Cleans the given root Element recursively cleaning and removing all of its children.
     * @param rootElement the root node to be clean.
     */
    public static void clean(Element rootElement)
    {
        rootElement.clean();
    }
}
