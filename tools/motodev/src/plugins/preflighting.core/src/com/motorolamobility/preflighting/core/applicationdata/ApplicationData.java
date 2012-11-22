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

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.internal.utils.ProjectUtils;
import com.motorolamobility.preflighting.core.internal.utils.StringUsageIdentifier;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;
import com.motorolamobility.preflighting.core.validation.Parameter;

/**
 * This Class represents the Application either in project or APK format that
 * will be analyzed by the tool.
 * 
 * It has a root node attribute that points to the root Element of the
 * Application. From that node is possible to navigate through the Application
 * tree.
 * 
 */
public class ApplicationData
{
    private Element rootElement;

    private String rootElementPath;

    private int appVer;

    private String appName;

    private String applicationPath;

    private XMLElement manifestElement;

    private List<XMLElement> layoutElements = new java.util.ArrayList<XMLElement>();

    private List<XMLElement> stringElements = new java.util.ArrayList<XMLElement>();

    private List<XMLElement> xmlElements = new java.util.ArrayList<XMLElement>();

    private boolean isProject = false;

    private List<SourceFolderElement> sourceFolderElements =
            new java.util.ArrayList<SourceFolderElement>();

    private List<Certificate> certificateChain;

    /**
     * Construct a new ApplicationData with the given parameters.
     * 
     * @param globalParams the list of global parameters.
     * @throws PreflightingToolException
     *             In case the applcationPath does not point to a APK or valid
     *             Android Project.
     */
    public ApplicationData(List<Parameter> globalParams) throws PreflightingToolException
    {
        ProjectUtils.populateAplicationData(globalParams, this);
        initialize();
    }

    /***
     * Initialize the class properties by reading the Application tree
     * 
     */
    public void initialize()
    {
        // Manifest Element
        List<Element> manifestList =
                ElementUtils.getElementByType(rootElement, Element.Type.FILE_MANIFEST);
        if (manifestList.size() > 0)
        {
            if (manifestList.get(0) instanceof XMLElement)
            {
                manifestElement = (XMLElement) manifestList.get(0);
                NodeList manifestByTag =
                        manifestElement.getDocument().getElementsByTagName("manifest");

                Node manifest =
                        (manifestByTag != null) && (manifestByTag.getLength() > 0) ? manifestByTag
                                .item(0) : null;
                Node versionCode = null;
                if (manifest != null)
                {
                    versionCode = manifest.getAttributes().getNamedItem("android:versionCode");
                }

                try
                {
                    appVer = Integer.parseInt(versionCode.getTextContent());
                }
                catch (Exception e)
                {
                    appVer = 0;
                }
            }
        }

        // Layout Elements
        List<Element> layoutsList =
                ElementUtils.getElementByType(rootElement, Element.Type.FILE_LAYOUT);

        if (layoutsList != null)
        {
            for (Element element : layoutsList)
            {
                if (element instanceof XMLElement)
                {
                    layoutElements.add((XMLElement) element);
                }
            }
        }

        // String Elements
        List<Element> stringsList =
                ElementUtils.getElementByType(rootElement, Element.Type.FILE_STRINGS);
        if (stringsList != null)
        {
            for (Element element : stringsList)
            {
                if (element instanceof XMLElement)
                {
                    stringElements.add((XMLElement) element);
                }
            }
        }

        //Source Element
        List<Element> srcFolders =
                ElementUtils.getElementByType(rootElement, Element.Type.FOLDER_SRC);
        for (Element srcFolder : srcFolders)
        {
            if (srcFolder instanceof SourceFolderElement)
            {
                sourceFolderElements.add((SourceFolderElement) srcFolder);
            }
        }

        xmlElements = ElementUtils.getXMLElements(rootElement);
    }

    /**
     * Return the Application root element.
     * @return The Element which represents the Application root element.
     */
    public Element getRootElement()
    {
        return rootElement;
    }

    /**
     * Set the ApplicationData to a given Element.
     * @param rootElement The Element.
     */
    public void setRootElement(Element rootElement)
    {
        this.rootElement = rootElement;
    }

    /**
     * Return the Application root element path.
     * @return A string with the Application root element path.
     */
    public String getRootElementPath()
    {
        return rootElementPath;
    }

    /**
     * Set the Application root element path.
     * @param rootElementPath The element path.
     */
    public void setRootElementPath(String rootElementPath)
    {
        this.rootElementPath = rootElementPath;
    }

    /**
     * Return the Element which corresponds to the Manifest file.
     * @return A XML Element of the Manifest file.
     */
    public XMLElement getManifestElement()
    {
        return manifestElement;
    }

    /**
     * Return the Element list which corresponds to the Layout files.
     * @return A list of Elements of the layout files.
     */
    public List<XMLElement> getLayoutElements()
    {
        return layoutElements;
    }

    /**
     * Return the Element list which corresponds to the Localization String files.
     * @return A list of Elements of the Localization String files.
     */
    public List<XMLElement> getStringElements()
    {
        return stringElements;
    }

    public List<XMLElement> getXMLElements()
    {
        return xmlElements;
    }

    /**
     * Return wheter the ApplicationData belongs to a project.
     * @return A Boolean stating whether it is a project or not.
     */
    public boolean isProject()
    {
        return isProject;
    }

    /**
     * Set whether the ApplicationData belongs to a project or not.
     * @param isProject A Boolean stating whether it is a project or not.
     */
    public void setIsProject(boolean isProject)
    {
        this.isProject = isProject;
    }

    /**
     * Return the list of certificates.
     * @return A list of certificates, empty if the application (project or APK) does not have any certificates.
     */
    public List<Certificate> getCertificateChain()
    {
        return certificateChain;
    }

    /**
     * Sets the Certificate Chain of the Application.
     * @param certificateChain a list of certificates.
     */
    public void setCertificateChain(List<Certificate> certificateChain)
    {
        this.certificateChain = certificateChain;
    }

    /**
     * Cleans all the data from the ApplicationData.
     */
    public void clean()
    {
        if (this.rootElement != null)
        {
            this.rootElement.clean();
        }
        this.rootElement = null;
        if (this.manifestElement != null)
        {
            this.manifestElement.clean();
        }
        this.manifestElement = null;
        if (this.layoutElements != null)
        {
            for (XMLElement elem : this.layoutElements)
            {
                elem.clean();
            }
            this.layoutElements.clear();
        }
        this.layoutElements = null;
        if (this.stringElements != null)
        {
            for (XMLElement elem : this.stringElements)
            {
                elem.clean();
            }
            this.stringElements.clear();
        }
        this.stringElements = null;
        if (this.certificateChain != null)
        {
            this.certificateChain.clear();
        }
        this.certificateChain = null;
        if (this.sourceFolderElements != null)
        {
            this.sourceFolderElements.clear();
        }
    }

    /**
     * Return the Application Version.
     * @return An Integer representing the application version.
     */
    public int getVersion()
    {
        return appVer;
    }

    /**
     * Set the Application name.
     * @param appName A string with the application name.
     */
    public void setName(String appName)
    {
        this.appName = appName;
    }

    /**
     * Return the Application name.
     * @return A string with the application name.
     */
    public String getName()
    {
        return appName;
    }

    /**
     * Set the Application path.
     * @param applicationPath A string with the application path.
     */
    public void setApplicationPath(String applicationPath)
    {
        this.applicationPath = applicationPath;
    }

    /**
     * Return the Application path.
     * @return A string with the application path.
     */
    public String getApplicationPath()
    {
        return applicationPath;
    }

    /**
     * Return a {@link SourceFolderElement} list which corresponds to the Java Model.
     * @return A {@link SourceFolderElement} list which corresponds to the Java Model.
     */
    public List<SourceFolderElement> getJavaModel()
    {
        return sourceFolderElements;
    }

    /**
     * 
     * Set a {@link SourceFolderElement} list which corresponds to the Java Model.
     * @param javaModel A {@link SourceFolderElement} list which corresponds to the Java Model.
     */
    public void setJavaModel(List<SourceFolderElement> javaModel)
    {
        this.sourceFolderElements = javaModel;
    }

    /**
     * Returns project list of {@link CompilationUnit} to enable checkers/conditions that are project specific (not available for APK verifications). 
     * @return The list of {@link CompilationUnit} objects.
     */
    public List<CompilationUnit> getProjectCompilationUnits()
    {
        List<CompilationUnit> projectCompilationUnits = new ArrayList<CompilationUnit>();
        if (this.sourceFolderElements != null)
        {
            for (SourceFolderElement folder : this.sourceFolderElements)
            {
                for (SourceFileElement file : folder.getSourceFileElements())
                {
                    projectCompilationUnits.add(file.getCompilationUnit());
                }
            }
        }
        return projectCompilationUnits;
    }

    /**
     * Gets the set of strings used by the app (identified by string ID, not including "R.string") 
     * @return set with the used string in the XML files
     */
    private Set<String> getUsedStringsInXMLFiles()
    {
        return StringUsageIdentifier.identifyStringsUsed(xmlElements);
    }

    /**
     * Gets the set of strings used by the app (identified by string ID, not including "R.string") 
     * @return set with the used strings in the Java files
     */
    private Set<String> getUsedStringsInJavaFiles()
    {
        Set<String> usedStringsInJava = new HashSet<String>();
        if (this.sourceFolderElements != null)
        {
            for (SourceFolderElement folder : this.sourceFolderElements)
            {
                usedStringsInJava.addAll(folder.getUsedStringConstants());
            }
        }
        return usedStringsInJava;
    }

    /**
     * Gets the set of used strings (they contain only the id of the string, that is, it does not include R.string) 
     * @return the complete set of strings used throughout the application (either in Java or XML files)
     */
    public Set<String> getUsedStringsInApplication()
    {
        Set<String> usedStringsInApplication = new HashSet<String>();
        usedStringsInApplication.addAll(getUsedStringsInXMLFiles());
        usedStringsInApplication.addAll(getUsedStringsInJavaFiles());
        return usedStringsInApplication;
    }

    /**
     * Gets the set of declared strings (based on default locale string keys merged with other locale string keys)
     * WARNING: Before calling this method, use {@link Condition#canExecute(ApplicationData, List)} to verify if there is a res folder in the application
     * @return the set of strings declared in the app's resource XML files 
     */
    public Set<String> getDeclaredStringsInResourceFiles()
    {
        Set<String> declaredStrings = new HashSet<String>();
        List<Element> folderResElements =
                ElementUtils.getElementByType(this.getRootElement(), Type.FOLDER_RES);

        ResourcesFolderElement resFolder =
                folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements.get(0)
                        : null;

        if (resFolder != null)
        {
            StringsElement stringsKeysDefault = resFolder.getDefaultValuesElement();

            if ((stringsKeysDefault != null) && (stringsKeysDefault.getKeyList() != null))
            {
                //adding keys from default locale
                declaredStrings.addAll(stringsKeysDefault.getKeyList());
            }
            for (Locale l : resFolder.getAvailableLocales())
            {
                StringsElement stringsKeysLocale = resFolder.getValuesElement(l);
                if ((stringsKeysLocale != null) && (stringsKeysLocale.getKeyList() != null))
                {
                    //adding keys from non-default locales
                    declaredStrings.addAll(stringsKeysLocale.getKeyList());
                }
            }
        }
        return declaredStrings;
    }
}
