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

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.motorolamobility.preflighting.core.internal.utils.ProjectUtils;

/***
 * Specialization of Element for XML files.
 * It has specific methods for XML files.
 */
public class XMLElement extends Element
{
    private Document document;

    private File xmlFile;

    /**
     * Construct an XMLElement object with the given parameters.
     * 
     * @param name the XML element name
     * @param parent the XML element parent
     * @param type the XML element type
     */
    public XMLElement(String name, Element parent, Type type)
    {
        super(name, parent, type);
    }

    /**
     * Returns the document of this {@link XMLElement}
     * @return the document of this XMLElement
     */
    public Document getDocument()
    {
        return document;
    }

    /**
     * Sets the document for this XML element
     * @param document the document for this XMLElement
     */
    public void setDocument(Document document)
    {
        this.document = document;
    }

    /**
     * Return the line number of a node at its document or -1 if not possible to retrieve it.
     * @param node Node to be located.
     * @return line Line number.
     */
    public int getNodeLineNumber(Node node)
    {
        Integer line = -1;

        if ((node.getUserData(ProjectUtils.LINE_NUMBER_KEY) != null)
                && (node.getUserData(ProjectUtils.LINE_NUMBER_KEY) instanceof Integer))
        {
            line = (Integer) node.getUserData(ProjectUtils.LINE_NUMBER_KEY);
        }

        return line;
    }

    @Override
    /**
     * Clean the object references.
     */
    public void clean()
    {
        super.clean();
        this.document = null;
        this.xmlFile = null;
    }
}
