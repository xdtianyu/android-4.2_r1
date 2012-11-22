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
import java.util.ArrayList;
import java.util.List;

/***
 * Base element of the Application Data tree. This class represents a node of the tree.
 * <br>  
 * Note: For specialized element types see subclasses of this class.
 */
public class Element
{
    /** 
     * Enumerator for available elements inside Android projects.
     * <br><br>
     * The project tree starts at {@link Element.Type#ROOT}
     * <br>
     * Inside root, there are folders e.g.: {@link Element.Type#FOLDER_RES} or {@link Element.Type#FOLDER_DRAWABLE}.
     * <br>
     * Inside folders there are files e.g.: {@link Element.Type#FILE_JAVA} or {@link Element.Type#FILE_LAYOUT} 
     *  
     */
    public enum Type
    {
        ROOT, FOLDER_SRC, FOLDER_RES, FOLDER_LAYOUT, FOLDER_VALUES, FOLDER_DRAWABLE, FOLDER_LIB,
        FOLDER_UNKNOWN, FILE_DRAWABLE, FILE_LAYOUT, FILE_XML, FILE_STRINGS, FILE_MANIFEST,
        FILE_UNKNOWN, FILE_JAVA
    };

    // Class Properties
    private String name;

    private Element parent;

    private Type type;

    private File file;

    private List<Element> children;

    /***
     * The Constructor.
     * 
     * @param name
     * @param parent
     * @param type
     */
    public Element(String name, Element parent, Type type)
    {
        this.name = name;
        this.parent = parent;
        this.type = type;
        children = new ArrayList<Element>();
    }

    /**
     * Adds a child to the Element.
     * @param childElement
     */
    public void addChild(Element childElement)
    {
        children.add(childElement);
    }

    /**
     * Removes a child from the Element.
     * @param childElement
     */
    public void removeChild(Element childElement)
    {
        children.remove(childElement);
    }

    /**
     * Returns the file which corresponds to the Element.
     * @return file which corresponds to the Element.
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Returns the type which corresponds to this Element.
     * @return Type which corresponds to the Element.
     */
    public Type getType()
    {
        return type;
    }

    /**
     * Sets the type which corresponds to this Element.
     * @return Type which corresponds to the Element.
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    public List<Element> getChildren()
    {
        return children;
    }

    public void setChildren(List<Element> children)
    {
        this.children = children;
    }

    /**
     * Returns the name of the Element.
     * @return String with the name of the Element.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the Element.
     * @param name String with the name.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the parent element of this Element.
     * @return an Element which corresponds to the parent of this Element.
     */
    public Element getParent()
    {
        return parent;
    }

    /**
     * Sets a parent Element to this Element.
     * @param parent The element which will be the parent of this Element.
     */
    public void setParent(Element parent)
    {
        this.parent = parent;
    }

    /**
     * Sets the file of the Element.
     * @param file the File of the Element.
     */
    public void setFile(File file)
    {
        this.file = file;
    }

    /**
     * Cleans this Element removing all of its children.
     * @param file the File of the Element.
     */
    public void clean()
    {
        if (this.children != null)
        {
            for (Element child : this.children)
            {
                child.clean();
            }
        }
        this.children = null;
    }

}
