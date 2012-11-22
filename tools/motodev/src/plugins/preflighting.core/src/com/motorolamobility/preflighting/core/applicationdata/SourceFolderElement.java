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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.motorolamobility.preflighting.core.source.model.Constant;
import com.motorolamobility.preflighting.core.source.model.Field;
import com.motorolamobility.preflighting.core.source.model.Instruction;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.Method;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;

/**
 * Folder that contains source code inside the Android application project. 
 */
public class SourceFolderElement extends FolderElement
{
    private static final String R_JAVA = "R.java";

    private static final String R$ID = "R$id";

    private static final String R$LAYOUT = "R$layout";

    private static final String R$STRING = "R$string";

    private List<SourceFileElement> sourceFileElements = new ArrayList<SourceFileElement>();

    private final boolean isApk;

    /**
     * Constructor which loads the minimum necessary data.
     * 
     * @param folder {@link File} which represents the folder. 
     * @param parent {@link Element} that is the parent in the tree of the Android project representation.
     * @param isApk <code>true</code> if dealing with APK, <code>false</code> if dealing with Project.
     */
    public SourceFolderElement(File folder, Element parent, boolean isApk)
    {
        super(folder, parent, Element.Type.FOLDER_SRC);
        this.isApk = isApk;
    }

    /**
     * Gets the list of {@link SourceFileElement} objects inside this folder.
     * 
     * @return Returns the list of {@link SourceFileElement} inside the folder.
     */
    public List<SourceFileElement> getSourceFileElements()
    {
        return sourceFileElements;
    }

    /**
     * Get the list of {@link Invoke} which is inside each {@link SourceFileElement}
     * of this folder.
     * 
     * @return returns the list of invoked methods inside each {@link SourceFileElement}.
     */
    public List<Invoke> getInvokedMethods()
    {
        List<Invoke> invokedMethods = new ArrayList<Invoke>();
        for (SourceFileElement smali : sourceFileElements)
        {
            List<Method> virtualMethods = smali.getVirtualMethods();
            List<Method> directMethods = smali.getDirectMethods();
            extractMethodsInvoked(invokedMethods, directMethods);
            extractMethodsInvoked(invokedMethods, virtualMethods);
        }
        return invokedMethods;
    }

    /**
     * Lists all methods invoked by a APK, given a list of methods.
     * 
     * @param invokedMethodsInsideProject List of {@link Invoke} methods inside
     * the project.
     * @param methods List of methods where the search will be done.
     */
    private void extractMethodsInvoked(List<Invoke> invokedMethodsInsideProject,
            List<Method> methods)
    {
        for (Method m : methods)
        {
            List<Instruction> instructions = m.getInstructions();
            for (Instruction instr : instructions)
            {
                if (instr instanceof Invoke)
                {
                    Invoke invoke = (Invoke) instr;
                    invokedMethodsInsideProject.add(invoke);
                }
            }
        }
    }

    /**
     * Gets all Ids declared in R.java.
     * 
     * @return Returns the {@link List} of Ids within R.Java, represented by {@link Field} objects.
     */
    public List<Field> getIds()
    {
        List<Field> ids = new ArrayList<Field>();
        for (SourceFileElement smali : sourceFileElements)
        {
            if (R_JAVA.equals(smali.getSourceName()))
            {
                List<Field> staticFields = smali.getStaticFields();
                if (smali.getClassFullPath().contains(R$ID))
                {
                    ids.addAll(smali.getStaticFields());
                    break;
                }
                else
                {
                    for (Field field : staticFields)
                    {
                        if (field.getName().startsWith("id"))
                        {
                            ids.add(field);
                        }
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Get layouts declared in R.java.
     * 
     * @return Returns the list of {@link Field} representing the
     * layouts declared in R.java. 
     */
    public List<Field> getLayouts()
    {
        List<Field> layouts = new ArrayList<Field>();
        if (isApk)
        {
            if (sourceFileElements != null)
            {
                for (SourceFileElement smali : sourceFileElements)
                {
                    if (R_JAVA.equals(smali.getSourceName()))
                    {
                        List<Field> staticFields = smali.getStaticFields();
                        if (smali.getClassFullPath().contains(R$LAYOUT))
                        {
                            layouts.addAll(staticFields);
                            break;
                        }
                        else
                        {
                            for (Field field : staticFields)
                            {
                                if (field.getName().startsWith("layout"))
                                {
                                    layouts.add(field);
                                }
                            }
                        }
                    }
                }
            }
        }
        return layouts;
    }

    /**
     * Get strings declared in R.java.
     * 
     * @return the list of {@link Field} objects representing the strings declared in R.java. 
     */
    public List<Field> getStrings()
    {
        List<Field> strings = new ArrayList<Field>();
        if (isApk)
        {
            if (sourceFileElements != null)
            {
                for (SourceFileElement smali : sourceFileElements)
                {
                    if (R_JAVA.equals(smali.getSourceName()))
                    {
                        List<Field> staticFields = smali.getStaticFields();
                        if (smali.getClassFullPath().contains(R$STRING))
                        {
                            strings.addAll(staticFields);
                            break;
                        }
                        else
                        {
                            for (Field field : staticFields)
                            {
                                if (field.getName().startsWith("string"))
                                {
                                    strings.add(field);
                                }
                            }
                        }
                    }
                }
            }
        }
        return strings;
    }

    /**
     * Gets the layouts that are used in the code.
     * (it makes the relationship among const command 
     * and the reference inside R.java)  
     * 
     * @return Returns the list of layouts used in the code.
     */
    private List<Field> getLayoutsUsedInCode()
    {
        List<Field> layoutsUsed = new ArrayList<Field>();
        List<Field> layoutsDeclared = getLayouts();
        if (isApk)
        {
            if (sourceFileElements != null)
            {
                for (SourceFileElement smali : sourceFileElements)
                {
                    List<Method> virtualMethods = smali.getVirtualMethods();
                    List<Method> directMethods = smali.getDirectMethods();
                    extractLayoutsUsed(layoutsUsed, layoutsDeclared, directMethods);
                    extractLayoutsUsed(layoutsUsed, layoutsDeclared, virtualMethods);
                }
            }
        }
        return layoutsUsed;

    }

    private void extractLayoutsUsed(List<Field> layoutsUsed, List<Field> layoutsDeclared,
            List<Method> methods)
    {
        for (Method m : methods)
        {
            List<Instruction> instructions = m.getInstructions();
            for (Instruction instr : instructions)
            {
                if (instr instanceof Constant)
                {
                    Constant constant = (Constant) instr;
                    String value = constant.getValue();
                    for (Field f : layoutsDeclared)
                    {
                        //find layout being used - unfortunately it may be false positive
                        if ((value != null) && value.equals(f.getValue()))
                        {
                            layoutsUsed.add(f);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the strings that are used in the code.
     * (it makes the relationship among const command 
     * and the reference inside R.java)  
     * 
     * @return Returns the list of layouts used in the code.
     */
    private List<Field> getStringsUsedInCode()
    {
        List<Field> stringsUsed = new ArrayList<Field>();
        List<Field> stringsDeclared = getStrings();
        if (isApk)
        {
            if (sourceFileElements != null)
            {
                for (SourceFileElement smali : sourceFileElements)
                {
                    List<Method> virtualMethods = smali.getVirtualMethods();
                    List<Method> directMethods = smali.getDirectMethods();
                    extractStringsUsed(stringsUsed, stringsDeclared, directMethods);
                    extractStringsUsed(stringsUsed, stringsDeclared, virtualMethods);
                }
            }
        }
        return stringsUsed;

    }

    private void extractStringsUsed(List<Field> stringsUsed, List<Field> stringsDeclared,
            List<Method> methods)
    {
        for (Method m : methods)
        {
            List<Instruction> instructions = m.getInstructions();
            for (Instruction instr : instructions)
            {
                if (instr instanceof Constant)
                {
                    Constant constant = (Constant) instr;
                    String value = constant.getValue();
                    for (Field f : stringsDeclared)
                    {
                        //find layout being used - unfortunately it may be false positive
                        if ((value != null) && value.equals(f.getValue()))
                        {
                            stringsUsed.add(f);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets all the strings used in Java files inside the source folder
     * @return the set of strings used in the Java files (identified by string ID, not including "R.string")
     */
    public Set<String> getUsedStringConstants()
    {
        Set<String> usedStrings = new HashSet<String>();
        if (sourceFileElements != null)
        {
            for (SourceFileElement source : sourceFileElements)
            {
                if (!isApk)
                {
                    //project
                    if (source.getUsedStringConstants() != null)
                    {
                        usedStrings.addAll(source.getUsedStringConstants());
                    }
                }
                else
                {
                    //APK
                    List<Field> fields = getStringsUsedInCode();
                    if (fields != null)
                    {
                        for (Field f : fields)
                        {
                            String aux = f.getName().replace("string.", "");
                            usedStrings.add(aux);
                        }
                    }
                }
            }
        }
        return usedStrings;
    }

    /**
     * Gets all layouts used in Java files inside the source folder
     * @return the set of layouts used in the Java files (identified by layout ID, not including "R.layout")
     */
    public Set<String> getUsedLayoutConstants()
    {
        Set<String> usedLayouts = new HashSet<String>();
        for (SourceFileElement source : sourceFileElements)
        {
            if (!isApk)
            {
                if (source.getUsedLayoutConstants() != null)
                {
                    usedLayouts.addAll(source.getUsedLayoutConstants());
                }
            }
            else
            {
                List<Field> fields = getLayoutsUsedInCode();
                if (fields != null)
                {
                    for (Field f : fields)
                    {
                        String aux = f.getName().replace("layout.", "");
                        usedLayouts.add(f.getName());
                    }
                }
            }
        }
        return usedLayouts;
    }

    /**
     * Clear Source File elements.
     */
    @Override
    public void clean()
    {
        super.clean();
        sourceFileElements.clear();
        sourceFileElements = null;
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
        return "ProjectJavaModel [sourceFileElements=" + sourceFileElements + "]";
    }
}
