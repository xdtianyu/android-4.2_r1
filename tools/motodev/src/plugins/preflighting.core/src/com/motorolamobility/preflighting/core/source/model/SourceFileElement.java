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

package com.motorolamobility.preflighting.core.source.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.motorolamobility.preflighting.core.applicationdata.Element;

/**
 * Represent a source file.
 */
public class SourceFileElement extends Element
{
    private String classFullPath;

    private String superclassName;

    private String sourceName;

    private boolean isInnerClass;

    private CompilationUnit compilationUnit;

    private final List<Field> staticFields = new ArrayList<Field>();

    private final List<Field> instanceFields = new ArrayList<Field>();

    private final List<Method> directMethods = new ArrayList<Method>();

    private final List<Method> virtualMethods = new ArrayList<Method>();

    private final Set<String> usedStringConstants = new HashSet<String>();

    private final Set<String> usedLayoutConstants = new HashSet<String>();

    /**
     * Instantiates a source file based on a {@link File} and 
     * a {@link Element} as its parent.
     * 
     * @param file {@link File} object for creating the source file.
     * @param parent Parent element represented by an {@link Element}.
     */
    public SourceFileElement(File file, Element parent)
    {
        super(file.getName(), parent, Element.Type.FILE_JAVA);
        setFile(file);
    }

    /**
     * Returns <code>true</code> in case this element is an inner class,
     * <code>false</code> otherwise.
     * 
     * @return Returns <code>true</code> in case this element is an inner class,
     * <code>false</code> otherwise.
     */
    public boolean isInnerClass()
    {
        return isInnerClass;
    }

    /**
     * Set <code>true</code> for declaring this File as an inner
     * class, <code>false</code> for anything else.
     * 
     * @param isInnerClass Value which determines whether this
     * element is an inner class.
     */
    public void setInnerClass(boolean isInnerClass)
    {
        this.isInnerClass = isInnerClass;
    }

    /**
     * Gets the full path of the class inside source file.
     */
    public String getClassFullPath()
    {
        return classFullPath;
    }

    /**
     * Set the full path of the class inside source file.
     * 
     * @param classFullPath The class which originates the full path
     * to be set.
     */
    public void setClassFullPath(String classFullPath)
    {
        this.classFullPath = classFullPath;
    }

    /**
     * Gets the superclass name of this {@link SourceFileElement}.
     * 
     * @return Returns the name of the superclass of the class inside this source file.
     */
    public String getSuperclassName()
    {
        return superclassName;
    }

    /**
     * Sets the superclass name of this {@link SourceFileElement}.
     * 
     * @param superclassName Superclass name to be set. 
     */
    public void setSuperclassName(String superclassName)
    {
        this.superclassName = superclassName;
    }

    /**
     * Get the source name of this {@link SourceFileElement}.
     * 
     * @return Returns the source name.
     */
    public String getSourceName()
    {
        return sourceName;
    }

    /**
     * Sets the name of the source name.
     * 
     * @param sourceName source name to be set.
     */
    public void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }

    /**
     * Gets the list of static fields.
     * 
     * @return Return the list of {@link Field} declared as static in the source file.
     */
    public List<Field> getStaticFields()
    {
        return staticFields;
    }

    /**
     * Gets the list of instance fields.
     * 
     * @return Returns the list of {@link Field} declared as instance (non-static) in the source file. 
     */
    public List<Field> getInstanceFields()
    {
        return instanceFields;
    }

    /**
     * Gets the list of direct methods.
     * 
     * @return Returns the list of {@link Method} directly declared in this source file.
     */
    public List<Method> getDirectMethods()
    {
        return directMethods;
    }

    /** 
     * Gets the list of virtual methods.
     * 
     * @return Returns the list of {@link Method} inherited from other classes (or declared as abstract).
     */
    public List<Method> getVirtualMethods()
    {
        return virtualMethods;
    }

    /**
     * Gets the set of string constants used in the code (entries don't include "R.string").
     * 
     * @return the set of {@link String} constants used in the source file.
     */
    public Set<String> getUsedStringConstants()
    {
        return usedStringConstants;
    }

    /**
     * Gets the set of layout constants used in the code (entries don't include "R.layout").
     * 
     * @return the set of {@link String} constants representing layouts used in the source file.
     */
    public Set<String> getUsedLayoutConstants()
    {
        return usedLayoutConstants;
    }

    /**
     * Adds a method to this {@link SourceFileElement}.
     * 
     * @param type The types are: ("direct" or "virtual").
     * @param method The {@link Method} to be added.
     */
    public void addMethod(String type, Method method)
    {
        if (Method.DIRECT.equals(type))
        {
            getDirectMethods().add(method);
        }
        else
        {
            getVirtualMethods().add(method);
        }
    }

    /**
     * Gets the source file full path.
     * 
     * @return Returns the full path to source file.
     */
    public String getSourceFileFullPath()
    {
        String classFullPath = getClassFullPath();
        String pack = classFullPath;
        if (classFullPath.contains("/"))
        {
            pack = classFullPath.substring(0, classFullPath.lastIndexOf('/'));
        }
        String sourceFile = pack + "/" + getSourceName();
        return sourceFile;
    }

    /**
     * Get the {@link CompilationUnit} associated with this {@link SourceFileElement}.
     * 
     * @return Returns the compilationUnit (if associated with an Android project, but not for APK).
     */
    public CompilationUnit getCompilationUnit()
    {
        return compilationUnit;
    }

    /**
     * Set the compilationUnit (if associated with an Android project, but not for APK).
     * 
     * @param compilationUnit The {@link CompilationUnit} to be set. 
     */
    public void setCompilationUnit(CompilationUnit compilationUnit)
    {
        this.compilationUnit = compilationUnit;
    }

    /**
     * This implementation provides a human-readable text of this
     * {@link SourceFileElement}.
     * 
     * @return Returns a human-readable text of this {@link SourceFileElement}.
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "SourceFileElement [classFullPath=" + classFullPath + ", superclassName="
                + superclassName + ", sourceName=" + sourceName + ", staticFields=" + staticFields
                + ", instanceFields=" + instanceFields + ", directMethods=" + directMethods
                + ", virtualMethods=" + virtualMethods + "]";
    }
}
