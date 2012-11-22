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
package com.motorola.studio.android.generateviewbylayout;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.motorola.studio.android.generatecode.AbstractCodeGenerator;
import com.motorola.studio.android.generatecode.AbstractCodeGeneratorData;
import com.motorola.studio.android.generatecode.JavaCodeModifier;
import com.motorola.studio.android.generateviewbylayout.codegenerators.ClassAttributesCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.EditTextCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.FindViewByIdCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.GalleryCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.OnClickGUIsCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.RadioButtonCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.RatingBarCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.SaveStateCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.SeekBarCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.codegenerators.SpinnerCodeGenerator;
import com.motorola.studio.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;

/**
 * Manager responsible to modify activity / fragment based on layout xml
 */
public class JavaModifierBasedOnLayout extends JavaCodeModifier
{
    private boolean generateDefaultListeners = false;

    private MethodDeclaration onCreateDeclaration;

    static
    {
        IMPORT_LIST.add(JavaViewBasedOnLayoutModifierConstants.IMPORT_ANDROID_APP);
        IMPORT_LIST.add(JavaViewBasedOnLayoutModifierConstants.IMPORT_ANDROID_WIDGET);
        IMPORT_LIST.add(JavaViewBasedOnLayoutModifierConstants.IMPORT_ANDROID_VIEW_VIEW);
    }

    @Override
    protected void initVariables()
    {
        CodeGeneratorDataBasedOnLayout codeGeneratorDataBasedOnLayout =
                (CodeGeneratorDataBasedOnLayout) getCodeGeneratorData();
        onCreateDeclaration =
                codeGeneratorDataBasedOnLayout.getJavaLayoutData().getVisitor()
                        .getOnCreateDeclaration();
        //get typeDeclaration
        super.initVariables();
    }

    @Override
    protected void callCodeGenerators(final SubMonitor theMonitor, IFile java)
            throws JavaModelException
    {
        CodeGeneratorDataBasedOnLayout codeGeneratorDataBasedOnLayout =
                (CodeGeneratorDataBasedOnLayout) getCodeGeneratorData();
        new ClassAttributesCodeGenerator(codeGeneratorDataBasedOnLayout, onCreateDeclaration,
                typeDeclaration).generateCode(theMonitor);
        FindViewByIdCodeGenerator f =
                new FindViewByIdCodeGenerator(codeGeneratorDataBasedOnLayout, onCreateDeclaration,
                        typeDeclaration, java);
        f.generateCode(theMonitor);
        if (shouldGenerateDefaultListeners())
        {
            //call the generators declared into the codeGenerators list
            super.callCodeGenerators(theMonitor, java);
        }

        //generate save state code
        new SaveStateCodeGenerator(codeGeneratorDataBasedOnLayout, onCreateDeclaration,
                typeDeclaration).generateCode(theMonitor);
    }

    /**
     * Factory method to include all the code generators for the given java modifier 
     * @param codeGeneratorDataBasedOnLayout
     * @return list of code generators
     */
    @Override
    public List<AbstractCodeGenerator> populateListOfCodeGenerators(
            AbstractCodeGeneratorData abstractCodeGeneratorData)
    {
        CodeGeneratorDataBasedOnLayout codeGeneratorDataBasedOnLayout =
                (CodeGeneratorDataBasedOnLayout) abstractCodeGeneratorData;
        codeGenerators.add(new OnClickGUIsCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        codeGenerators.add(new RadioButtonCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        codeGenerators.add(new EditTextCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        codeGenerators.add(new SpinnerCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        codeGenerators.add(new GalleryCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        codeGenerators.add(new RatingBarCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        codeGenerators.add(new SeekBarCodeGenerator(codeGeneratorDataBasedOnLayout,
                onCreateDeclaration, typeDeclaration));
        return codeGenerators;
    }

    /**
     * @return true if need to generate code for listeners (buttons, edit fields), false if do not need  
     */
    public boolean shouldGenerateDefaultListeners()
    {
        return generateDefaultListeners;
    }

    /**
     * Defines if the class have to generate listeners (e.g.: onclick for buttons, onKey for edittext, and so on) 
     * @param generateDefaultListeners the generateDefaultListeners to set
     */
    public void setGenerateDefaultListeners(boolean generateDefaultListeners)
    {
        this.generateDefaultListeners = generateDefaultListeners;
    }

    @Override
    protected File getDataResource()
    {
        return ((CodeGeneratorDataBasedOnLayout) codeGeneratorData).getLayoutFile().getFile();
    }
}
