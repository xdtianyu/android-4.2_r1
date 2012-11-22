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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.FolderElement;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.StringsElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.sdk.SdkUtils;
import com.motorolamobility.preflighting.core.source.model.Constant;
import com.motorolamobility.preflighting.core.source.model.Field;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.Method;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;
import com.motorolamobility.preflighting.core.source.model.Variable;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;

public final class ProjectUtils
{
    //Constants
    private static final String ANDROID_VERSION_API_LEVEL = "AndroidVersion.ApiLevel";

    private static final String SOURCE_PROPERTIES = "source.properties";

    private static final String ANDROID_JAR = "android.jar";

    private static final String PLATFORMS = "platforms";

    private static final String ANDROID = "android-";

    private static final String TARGET = "target";

    private static final String DEFAULT_PROPERTIES = "default.properties";

    private static final String PROJECT_PROPERTIES = "project.properties";

    private static final String R_LAYOUT = "R.layout"; //$NON-NLS-1$

    private static final String R_STRING = "R.string"; //$NON-NLS-1$

    // Constants for validate the android project structure
    private static final String ANDROID_APK_NAMESPACE_URI =
            "http://schemas.android.com/apk/res/android"; //$NON-NLS-1$

    private static final String ANDROID_SCHEME_ATT = "xmlns:android"; //$NON-NLS-1$

    private static final String TAG_RESOURCES = "resources"; //$NON-NLS-1$

    private static final String TAG_MANIFEST = "manifest"; //$NON-NLS-1$

    private static final String ANDROID_MANIFEST_NAME = "AndroidManifest.xml"; //$NON-NLS-1$

    private static final String XML_FILE_EXTENSION = "xml"; //$NON-NLS-1$

    private static final String FOLDER_DRAWABLE = "drawable"; //$NON-NLS-1$

    private static final String FOLDER_VALUES = "values"; //$NON-NLS-1$

    private static final String FOLDER_LAYOUT = "layout"; //$NON-NLS-1$

    private static final String FOLDER_DIST = "dist"; //$NON-NLS-1$

    private static final String FOLDER_SRC = "src"; //$NON-NLS-1$

    private static final String FOLDER_SMALI = "smali"; //$NON-NLS-1$

    private static final String FOLDER_GEN = "gen"; //$NON-NLS-1$

    private static final String FOLDER_RES = "res"; //$NON-NLS-1$

    private static final String FOLDER_LIB = "lib"; //$NON-NLS-1$

    public static final String LINE_NUMBER_KEY = "line_number"; //$NON-NLS-1$

    public static final String JAVA_FILE_PROPERTY = "java_file";

    public static final FileNameMap fileNameMap = URLConnection.getFileNameMap();

    // mapping names of the android folders structure and theirs element types
    private static HashMap<String, Type> foldersName = new HashMap<String, Type>();

    /**
     * Returns in appData the tree of elements of a given project or package
     * 
     * @param project
     * @param appData
     * @throws IOException
     */
    public static void populateAplicationData(List<Parameter> globalParameters,
            ApplicationData appData) throws PreflightingToolException
    {

        Parameter applicationPathPrm = null;

        for (Parameter param : globalParameters)
        {
            if (ValidationManager.InputParameter.APPLICATION_PATH.getAlias().equals(
                    param.getParameterType()))
            {
                applicationPathPrm = param;
                break;
            }
        }

        String applicationPath = applicationPathPrm.getValue();
        appData.setApplicationPath(applicationPath);

        File file = null;
        if (applicationPath != null)
        {

            // mapping android project folders structure
            foldersName.put(FOLDER_SRC, Element.Type.FOLDER_SRC);
            foldersName.put(FOLDER_SMALI, Element.Type.FOLDER_SRC);
            foldersName.put(FOLDER_GEN, Element.Type.FOLDER_SRC);
            foldersName.put(FOLDER_RES, Element.Type.FOLDER_RES);
            foldersName.put(FOLDER_LIB, Element.Type.FOLDER_LIB);
            foldersName.put(FOLDER_DRAWABLE, Element.Type.FOLDER_DRAWABLE);
            foldersName.put(FOLDER_VALUES, Element.Type.FOLDER_VALUES);
            foldersName.put(FOLDER_LAYOUT, Element.Type.FOLDER_LAYOUT);

            file = new File(applicationPath);
            if ((file != null) && file.canRead())
            {
                if (file.isDirectory())
                {
                    Element element = new Element(file.getName(), null, Element.Type.ROOT);
                    boolean isFolderAProject = validateProjectFolder(file);

                    if (!isFolderAProject)
                    {
                        throw new PreflightingToolException(
                                PreflightingCoreNLS.ProjectUtils_InvalidPathErrorMessage);
                    }

                    boolean isProject = true;
                    appData.setRootElement(element);
                    appData.setRootElementPath(file.getAbsolutePath());
                    appData.setIsProject(isProject);
                    appData.setName(file.getName());

                    populateApplicationDataRecursively(file, appData.getRootElement(), isProject,
                            globalParameters);

                }
                else
                // it is a file, could be an android package
                {
                    Parameter sdkPathPrm = null;

                    for (Parameter param : globalParameters)
                    {
                        if (ValidationManager.InputParameter.SDK_PATH.getAlias().equals(
                                param.getParameterType()))
                        {
                            sdkPathPrm = param;
                            break;
                        }
                    }

                    String sdkPath = sdkPathPrm.getValue();
                    String tempSdkPath = SdkUtils.getLatestAAPTToolPath(sdkPath);

                    if (tempSdkPath != null)
                    {
                        sdkPath = tempSdkPath;
                    }
                    // the apk should be converted to an android project
                    // structure and
                    // this project will be converted to a tree
                    // apktool or aapt may be used

                    File projectFile = ApkUtils.extractProjectFromAPK(file, sdkPath);

                    if ((projectFile != null) && projectFile.canRead())
                    {
                        Element element =
                                new Element(projectFile.getName(), null, Element.Type.ROOT);
                        appData.setRootElement(element);
                        appData.setRootElementPath(projectFile.getAbsolutePath());
                        IPath path = new Path(file.getName());
                        appData.setName(path.removeFileExtension().toString());

                        // extract certificate info from APK
                        try
                        {
                            List<Certificate> certificateChain = ApkUtils.populateCertificate(file);
                            appData.setCertificateChain(certificateChain);
                        }
                        catch (Exception e)
                        {
                            PreflightingLogger.error(ProjectUtils.class,
                                    PreflightingCoreNLS.ProjectUtils_ErrorReadingCertificate, e); //$NON-NLS-1$
                            throw new PreflightingToolException(
                                    PreflightingCoreNLS.ProjectUtils_ErrorReadingCertificate);
                        }

                        populateApplicationDataRecursively(projectFile, appData.getRootElement(),
                                false, globalParameters);
                    }
                }
            }
            else
            {
                throw new PreflightingToolException(
                        PreflightingCoreNLS.ProjectUtils_InvalidPathErrorMessage);
            }
        }
    }

    /**
     * Verifies if the given path contains an Android project. It just checks if
     * there's an AndroidManifest.xml on the root dir.
     * 
     * @param dir
     *            the path
     * @return true if the folder contains a project, false otherwise.
     * @throws PreflightingToolException
     */
    public static boolean validateProjectFolder(File dir)
    {
        File[] androidManifest = dir.listFiles(new FilenameFilter()
        {
            public boolean accept(File directory, String fileName)
            {

                return fileName.equalsIgnoreCase(ANDROID_MANIFEST_NAME);
            }
        });

        return androidManifest.length > 0;
    }

    /**
     * Build the tree of elements by categorizing each element, creating the
     * proper element object and adding the element on the proper node of the
     * tree.
     * 
     * @param project
     * @param appElement
     */
    private static void populateApplicationDataRecursively(File project, Element appElement,
            boolean isProject, List<Parameter> globalParameters)
    {
        Element element = null;
        File[] files = project.listFiles();
        // the XML Document fulfilled by checkElementType method when the
        // element is a XML file
        Document[] xmlDoc = new Document[1];

        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];
            Type elementType = checkElementType(file, appElement, xmlDoc);
            if (file.isFile())
            {
                if (xmlDoc[0] != null)
                {
                    if (elementType == Type.FILE_STRINGS)
                    {
                        element = new StringsElement(file.getName(), appElement);
                    }
                    else if (elementType == Type.FILE_LAYOUT)
                    {
                        element = new XMLElement(file.getName(), appElement, elementType);
                    }
                    else
                    {
                        element = new XMLElement(file.getName(), appElement, elementType);
                    }

                    // fill each node of xmlDoc with line number information
                    if (file.getName().equals(ANDROID_MANIFEST_NAME))
                    {
                        populateLineNumber(file, xmlDoc);
                    }

                    ((XMLElement) element).setDocument(xmlDoc[0]);
                }
                else
                {
                    element = new Element(file.getName(), appElement, elementType);
                }
                element.setFile(file);
                appElement.addChild(element);
            }
            else
            {
                // dist folder is not scanned
                if (file.getName().equals(FOLDER_DIST))
                {
                    continue;
                }
                else if (elementType.equals(Element.Type.FOLDER_RES))
                {
                    if (!file.getAbsolutePath().endsWith(
                            File.separator + "bin" + File.separator + "res"))
                    {
                        // WARNING: the if above is necessary since ADT R14
                        // do not consider /bin/res (otherwise it will break
                        // several conditions)
                        element = new ResourcesFolderElement(file, appElement);
                    }
                }
                else if (elementType.equals(Element.Type.FOLDER_SRC))
                {
                    try
                    {
                        if (!isProject)
                        {
                            element = ApktoolUtils.extractJavaModel(file, appElement, file);
                        }
                        else
                        {
                            element =
                                    ProjectUtils.createCompilationUnits(appElement, file, project,
                                            globalParameters);
                        }
                    }
                    catch (Exception e)
                    {
                        element = new FolderElement(file, appElement, elementType);
                        PreflightingLogger.error(ProjectUtils.class,
                                PreflightingCoreNLS.ProjectUtils_ErrorReadingJavaModel, e); //$NON-NLS-1$
                    }
                }
                else
                {
                    element = new FolderElement(file, appElement, elementType);
                }
                if (element != null)
                {
                    appElement.addChild(element);
                    populateApplicationDataRecursively(file, element, isProject, globalParameters);
                }
            }
        }
    }

    /*
     * Sets each DOM with line number information
     * 
     * @param xmlFile
     */
    private static void populateLineNumber(File xmlFile, Document[] xmlDoc)
    {
        ArrayList<Node> nodesSequencialList = new ArrayList<Node>();
        xmlDoc[0].getDocumentElement().normalize();
        populateSequencialNodesList(xmlDoc[0], nodesSequencialList, xmlDoc);

        SAXParserFactory saxfac = SAXParserFactory.newInstance();
        SAXParser saxParser;
        try
        {
            saxParser = saxfac.newSAXParser();
            saxParser.parse(xmlFile, new XmlLineHandler(nodesSequencialList));
        }
        catch (Exception e)
        {
            PreflightingLogger.warn(ProjectUtils.class,
                    "Line number information will not be added to " + xmlFile.getName(), e); //$NON-NLS-1$
            // do nothing - line number information will not be added
        }
        nodesSequencialList.clear();
    }

    /**
     * Find the proper Element.Type for the given fileElement.
     * 
     * @param fileElement
     * @param appElement
     * @return elementType
     */
    private static Type checkElementType(final File fileElement, Element appElement,
            Document[] xmlDoc)
    {
        Type elementType = null;
        if (fileElement.isDirectory())
        {
            // try to find the current folder on the android project default
            // structure
            elementType = foldersName.get(fileElement.getName().toLowerCase());

            if (elementType == null)
            {
                // this other folders are part of the android project default
                // structure but can have minor variations
                // such as: drawable-hdpi, drawable-ldpi, values-pt
                if (fileElement.getName().toLowerCase().startsWith(FOLDER_DRAWABLE))
                {
                    elementType = foldersName.get(FOLDER_DRAWABLE);
                }
                else if (fileElement.getName().toLowerCase().startsWith(FOLDER_VALUES))
                {
                    elementType = foldersName.get(FOLDER_VALUES);
                }
                else if (fileElement.getName().toLowerCase().startsWith(FOLDER_LAYOUT))
                {
                    elementType = foldersName.get(FOLDER_LAYOUT);
                }
                else
                {
                    elementType = Element.Type.FOLDER_UNKNOWN;
                }
            }
        }
        else
        // element is a file
        {
            // it probably is a XML file
            if (fileElement.getName().endsWith(XML_FILE_EXTENSION))
            {
                // try to convert the possible xml file into a Document
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = null;
                try
                {
                    db = dbf.newDocumentBuilder();
                    db.setErrorHandler(new ErrorHandler()
                    {
                        public void warning(SAXParseException saxException) throws SAXException
                        {
                            DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                                    PreflightingCoreNLS.ProjectUtils_Error_Parsing_Manifest_INFO,
                                    fileElement.getName()), VerboseLevel.v1);

                            DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                                    PreflightingCoreNLS.ProjectUtils_Error_Parsing_Manifest_DEBUG,
                                    new String[]
                                    {
                                            fileElement.getName(),
                                            Integer.toString(saxException.getLineNumber()),
                                            saxException.getLocalizedMessage()
                                    }), VerboseLevel.v2);

                            PreflightingLogger.warn(
                                    ProjectUtils.class,
                                    "Could not parse " //$NON-NLS-1$
                                            + fileElement.getName()
                                            + ":" + saxException.getLineNumber() + " - " + saxException.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
                        }

                        public void fatalError(SAXParseException saxException) throws SAXException
                        {
                            DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                                    PreflightingCoreNLS.ProjectUtils_Error_Parsing_Manifest_INFO,
                                    fileElement.getName()), VerboseLevel.v1);

                            DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                                    PreflightingCoreNLS.ProjectUtils_Error_Parsing_Manifest_DEBUG,
                                    new String[]
                                    {
                                            fileElement.getName(),
                                            Integer.toString(saxException.getLineNumber()),
                                            saxException.getLocalizedMessage()
                                    }), VerboseLevel.v2);

                            PreflightingLogger.fatal(
                                    ProjectUtils.class,
                                    "Could not parse " //$NON-NLS-1$
                                            + fileElement.getName()
                                            + ":" + saxException.getLineNumber() + " - " + saxException.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
                        }

                        public void error(SAXParseException saxException) throws SAXException
                        {
                            DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                                    PreflightingCoreNLS.ProjectUtils_Error_Parsing_Manifest_INFO,
                                    fileElement.getName()), VerboseLevel.v1);

                            DebugVerboseOutputter.printVerboseMessage(NLS.bind(
                                    PreflightingCoreNLS.ProjectUtils_Error_Parsing_Manifest_DEBUG,
                                    new String[]
                                    {
                                            fileElement.getName(),
                                            Integer.toString(saxException.getLineNumber()),
                                            saxException.getLocalizedMessage()
                                    }), VerboseLevel.v2);

                            PreflightingLogger.error(
                                    ProjectUtils.class,
                                    "Could not parse " //$NON-NLS-1$
                                            + fileElement.getName()
                                            + ":" + saxException.getLineNumber() + " - " + saxException.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
                        }
                    });

                    xmlDoc[0] = db.parse(fileElement);
                }
                catch (Exception e)
                {
                    PreflightingLogger.warn(ProjectUtils.class, "Could not read the " //$NON-NLS-1$
                            + fileElement.getName() + " file.", e); //$NON-NLS-1$
                }
                if (xmlDoc[0] != null) // it really is a valid XML file
                {
                    if (appElement.getType().equals(Element.Type.ROOT)
                            && fileElement.getName().equals(ANDROID_MANIFEST_NAME))
                    {
                        // the AndroidManifest file should be under the project
                        // root
                        // Validating the attribute
                        // xmlns:android="http://schemas.android.com/apk/res/android
                        // to check if the file really is an AndroidManifest
                        if (xmlDoc[0].getDocumentElement().getAttribute(ANDROID_SCHEME_ATT)
                                .equals(ANDROID_APK_NAMESPACE_URI)
                                && xmlDoc[0].getDocumentElement().getNodeName()
                                        .equals(TAG_MANIFEST))
                        {
                            elementType = Element.Type.FILE_MANIFEST;
                        }
                    }
                    else if (appElement.getType().equals(Element.Type.FOLDER_LAYOUT)) // the parent is the
                                                                                      // layout folder
                    {

                        // Checking if the xml file is an android layout file by
                        // validating the attribute
                        // xmlns:android="http://schemas.android.com/apk/res/android"

                        if (xmlDoc[0].getDocumentElement().getAttribute(ANDROID_SCHEME_ATT)
                                .equals(ANDROID_APK_NAMESPACE_URI))
                        {
                            elementType = Element.Type.FILE_LAYOUT;
                        }

                    }
                    else if (appElement.getType().equals(Element.Type.FOLDER_VALUES)) // the parent is the
                                                                                      // values folder
                    {
                        // Check the tag indicating if it really is a strings
                        // file
                        if (xmlDoc[0].getDocumentElement().getNodeName().equals(TAG_RESOURCES))
                        {
                            elementType = Element.Type.FILE_STRINGS;
                        }
                    }
                    else
                    {
                        elementType = Element.Type.FILE_XML;
                    }
                }
            }
            else if (appElement.getType().equals(Element.Type.FOLDER_DRAWABLE)) // the parent is the
                                                                                // drawable folder
            {
                // check if the file is a valid image
                String mimeType = fileNameMap.getContentTypeFor(fileElement.getAbsolutePath());

                if ((mimeType != null) && mimeType.startsWith("image/"))
                {
                    elementType = Element.Type.FILE_DRAWABLE;
                }
            }
            if (elementType == null)
            {
                elementType = Element.Type.FILE_UNKNOWN;
            }
        }
        return elementType;
    }

    public static SourceFolderElement createCompilationUnits(Element parent, File srcDir,
            File projectDir, List<Parameter> globalParameters) throws IOException,
            PreflightingToolException
    {
        SourceFolderElement model = new SourceFolderElement(srcDir, parent, false);
        IPath projectPath = Path.fromOSString(srcDir.getParent());
        List<CompilationUnit> list = new ArrayList<CompilationUnit>();
        List<File> classPathFiles = readLibPathsFromClasspathEntries(projectDir);
        File androidTarget = getAndroidTargetPathForProject(projectDir, globalParameters);
        classPathFiles.add(androidTarget);
        visitFolderToIdentifyClasses(srcDir, list, projectPath, classPathFiles);

        for (CompilationUnit compilationUnit : list)
        {
            SourceFileElement sourceFileElement = ProjectUtils.readFromJava(compilationUnit, model);
            sourceFileElement.setCompilationUnit(compilationUnit);
            model.addChild(sourceFileElement);
            model.getSourceFileElements().add(sourceFileElement);
        }

        return model;
    }

    /*
     * Read all .java files inside src folder and create its ASTs objects.
     * 
     * @param sourceFile
     * 
     * @param list List of ASTs which is updated at each recursive call
     * 
     * @param projectPath
     * 
     * @throws PreflightingToolException
     */
    private static void visitFolderToIdentifyClasses(File sourceFile, List<CompilationUnit> list,
            IPath projectPath, List<File> classPathFiles) throws PreflightingToolException
    {

        try
        {
            if (sourceFile.isFile())
            {
                // is a java file
                if (sourceFile.getName().endsWith(".java"))
                {
                    FileReader reader = null;
                    CharBuffer cb = null;

                    try
                    {
                        reader = new FileReader(sourceFile);
                        cb = CharBuffer.allocate((int) sourceFile.length());

                        int count = reader.read(cb);
                        cb.flip();

                        // verify if all bytes were read
                        if (count == sourceFile.length())
                        {
                            ASTParser parser = ASTParser.newParser(AST.JLS3);
                            parser.setSource(cb.array());

                            Map options = JavaCore.getOptions();
                            JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
                            parser.setCompilerOptions(options);

                            List<String> classPathList =
                                    new ArrayList<String>(classPathFiles.size());
                            for (File file : classPathFiles)
                            {
                                classPathList.add(file.getAbsolutePath());
                            }
                            String[] classpathEntries =
                                    classPathList.toArray(new String[classPathFiles.size()]);
                            File srcFolder = new File(projectPath.toFile(), "src");
                            File genFolder = new File(projectPath.toFile(), "gen");
                            int sourcepathEntriesSize = 0;
                            if (srcFolder.exists())
                            {
                                sourcepathEntriesSize++;
                            }
                            if (genFolder.exists())
                            {
                                sourcepathEntriesSize++;
                            }

                            String[] sourcepathEntries = new String[sourcepathEntriesSize];

                            if (sourcepathEntriesSize == 1)
                            {
                                sourcepathEntries[0] = srcFolder.getAbsolutePath();
                            }
                            if (sourcepathEntriesSize == 2)
                            {
                                sourcepathEntries[0] = srcFolder.getAbsolutePath();
                                sourcepathEntries[1] = genFolder.getAbsolutePath();
                            }

                            parser.setEnvironment(classpathEntries, sourcepathEntries, null, true);
                            parser.setUnitName(computeRelativePath(projectPath,
                                    sourceFile.getAbsolutePath()));
                            parser.setResolveBindings(true);

                            ASTNode nodes = parser.createAST(null);

                            if (nodes.getNodeType() == ASTNode.COMPILATION_UNIT)
                            {
                                CompilationUnit cu = (CompilationUnit) nodes;
                                cu.setProperty(JAVA_FILE_PROPERTY, sourceFile);
                                list.add(cu);
                            }
                        }
                        else
                        {
                            DebugVerboseOutputter.printVerboseMessage(
                                    PreflightingCoreNLS.ProjectUtils_ErrorReadingSourceFile
                                            + sourceFile.getName(), VerboseLevel.v1);
                        }
                    }
                    // syntax error
                    catch (Exception syntaxException)
                    {
                        DebugVerboseOutputter.printVerboseMessage(
                                PreflightingCoreNLS.ProjectUtils_ErrorReadingSourceFile
                                        + sourceFile.getName(), VerboseLevel.v1);
                    }
                    finally
                    {
                        if (cb != null)
                        {
                            cb.clear();
                        }
                        if (reader != null)
                        {
                            reader.close();
                        }
                    }
                }
            }
            else if (sourceFile.isDirectory())
            {
                File[] subDirs = sourceFile.listFiles();

                for (int i = 0; i < subDirs.length; i++)
                {
                    visitFolderToIdentifyClasses(subDirs[i], list, projectPath, classPathFiles);
                }
            }
        }
        catch (Exception e)
        {
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ProjectUtils_ErrorReadingSourceFile, e);
        }
    }

    /**
     * Returns a relative path e.g. /Project/src/package/name.java
     * 
     * @param projectPath
     * @param sourcePath
     * @return
     */
    private static String computeRelativePath(IPath projectPath, String sourcePath)
    {
        IPath relativePath = Path.fromOSString(sourcePath).makeRelativeTo(projectPath);
        return File.separator + projectPath.lastSegment() + File.separator
                + relativePath.toOSString();
    }

    /**
     * Reads lib paths from project .classpath file
     * 
     * @param projectDir
     * @return list of files for libraries used inside Android project
     * @throws PreflightingToolException
     *             problem to read .classpath file
     */
    private static List<File> readLibPathsFromClasspathEntries(File projectDir)
            throws PreflightingToolException
    {
        List<File> libPaths = new ArrayList<File>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;

        try
        {
            db = dbf.newDocumentBuilder();
            File classPathFile = new File(projectDir, ".classpath");
            if (classPathFile.exists())
            {
                Document doc = db.parse(classPathFile);
                doc.getDocumentElement().normalize();
                NodeList nodeLst = doc.getElementsByTagName("classpathentry");

                for (int s = 0; s < nodeLst.getLength(); s++)
                {
                    Node node = nodeLst.item(s);
                    if (node.getNodeType() == Node.ELEMENT_NODE)
                    {
                        NamedNodeMap attrs = node.getAttributes();
                        Node kindNode = attrs.getNamedItem("kind");
                        Node pathNode = attrs.getNamedItem("path");
                        if ((kindNode != null) && (pathNode != null)
                                && (kindNode.getNodeValue() != null)
                                && (pathNode.getNodeValue() != null))
                        {
                            if (kindNode.getNodeValue().equals("lib"))
                            {
                                String pathValue = pathNode.getNodeValue();
                                File f = new File(pathValue);
                                if (f.exists())
                                {
                                    libPaths.add(f);
                                }
                                else
                                {
                                    PreflightingLogger.debug(ProjectUtils.class,
                                            "Could not find lib path: " //$NON-NLS-1$
                                                    + f.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ProjectUtils_ErrorReadingClasspathFile, e);
        }
        return libPaths;
    }

    /**
     * Retrieves the path to android.jar file inside platform/$target$, where
     * $target$ is defined inside default.properties file
     * 
     * @param projectDir
     * @param globalParameters
     *            parameters where SDK_PATH is included
     * @return file with path to android.jar
     * @throws PreflightingToolException
     *             problem to read default.properties
     */
    private static File getAndroidTargetPathForProject(File projectDir,
            List<Parameter> globalParameters) throws PreflightingToolException
    {
        File androidJarTarget = null;
        Properties properties = new Properties();
        // changed from default.properties to project.properties after R14
        File defaultPropertiesFile = new File(projectDir, PROJECT_PROPERTIES);
        if (!defaultPropertiesFile.exists())
        {
            // WARNING: do not remove statement below assigning
            // default.properties file to keep compatibility with projects
            // created with ADTs before R14
            defaultPropertiesFile = new File(projectDir, DEFAULT_PROPERTIES);
        }
        if (defaultPropertiesFile.exists())
        {
            try
            {
                FileInputStream fileInputStream = null;
                try
                {
                    fileInputStream = new FileInputStream(defaultPropertiesFile);
                    properties.load(fileInputStream);
                }
                finally
                {
                    try
                    {
                        fileInputStream.close();
                    }
                    catch (Exception e)
                    {
                        //Do Nothing.
                    }
                }
                if (properties.containsKey(TARGET))
                {
                    String targetValue = properties.getProperty(TARGET);
                    if (targetValue != null)
                    {
                        if (!targetValue.startsWith(ANDROID))
                        {
                            try
                            {
                                // add-on => <name>:<model>:<version>
                                int colonIndex = targetValue.lastIndexOf(":");
                                if (colonIndex >= 0)
                                {
                                    targetValue = ANDROID + targetValue.substring(colonIndex + 1);
                                }
                            }
                            catch (Exception e)
                            {
                                PreflightingLogger.debug("Unable to set target value.");
                            }
                        }
                    }
                    Parameter sdkPathPrm = null;
                    for (Parameter param : globalParameters)
                    {
                        if (ValidationManager.InputParameter.SDK_PATH.getAlias().equals(
                                param.getParameterType()))
                        {
                            sdkPathPrm = param;
                            break;
                        }
                    }
                    String sdkPath = getSdkPath(sdkPathPrm);

                    if (sdkPath != null)
                    {
                        // found sdk path
                        androidJarTarget =
                                new File(sdkPath + File.separator + PLATFORMS + File.separator
                                        + targetValue + File.separator + ANDROID_JAR);
                        if (!androidJarTarget.exists())
                        {
                            //if not found the exact version, then look for one version of android that is greater than target value (and retrieve android.jar)
                            File baseFolder = new File(sdkPath + File.separator + PLATFORMS);
                            File[] androidPlatforms = baseFolder.listFiles();
                            boolean foundJar = false;
                            if (androidPlatforms.length > 0)
                            {
                                for (File androidPlatform : androidPlatforms)
                                {
                                    File sourcePropsFile =
                                            new File(androidPlatform, SOURCE_PROPERTIES);
                                    File jar = new File(androidPlatform, ANDROID_JAR);
                                    if (sourcePropsFile.exists() && jar.exists())
                                    {
                                        Properties sourceProperties = new Properties();
                                        try
                                        {
                                            fileInputStream = new FileInputStream(sourcePropsFile);
                                            sourceProperties.load(fileInputStream);
                                        }
                                        finally
                                        {
                                            if (fileInputStream != null)
                                            {
                                                fileInputStream.close();
                                            }
                                        }
                                        //platform api level
                                        String apiLevel =
                                                sourceProperties
                                                        .getProperty(ANDROID_VERSION_API_LEVEL);
                                        int index = targetValue.indexOf("-");
                                        if ((index >= 0) && (apiLevel != null))
                                        {
                                            //project target declared
                                            String versionName = targetValue.substring(index + 1);
                                            try
                                            {
                                                Integer version = Integer.valueOf(versionName);
                                                Integer apiLevelVersion = Integer.valueOf(apiLevel);
                                                if (apiLevelVersion >= version)
                                                {
                                                    //found a compatible platform
                                                    foundJar = true;
                                                    androidJarTarget = jar;
                                                    break;
                                                }
                                            }
                                            catch (NumberFormatException nfe)
                                            {
                                                //ignore this folder (add-on or preview)                                                
                                            }
                                        }
                                    }
                                }
                                if (!foundJar)
                                {
                                    throw new PreflightingToolException(
                                            androidJarTarget.getAbsolutePath()
                                                    + "not found, check your sdk or your application target platform.");
                                }
                            }
                        }
                    }
                    else
                    {
                        throw new PreflightingToolException("Sdk path not found.");
                    }
                }
            }
            catch (IOException e)
            {
                throw new PreflightingToolException(
                        PreflightingCoreNLS.ProjectUtils_ErrorReadingDefaultPropertiesFile, e);
            }
        }
        else
        {
            throw new PreflightingToolException("default.properties file not found.");
        }
        return androidJarTarget;
    }

    /**
     * Returns the path for Android SDK
     * 
     * @param sdkPathPrm
     *            parameter with the path or "aapt" (if it is taking the value
     *            from PATH environment variable)
     * @return resolved path to Android SDK
     */
    public static String getSdkPath(Parameter sdkPathPrm)
    {
        String sdkPath = null;
        if (sdkPathPrm.getValue().equals("aapt"))
        {
            // sdk was not defined - take from environment variable
            String pathVariable = System.getenv("PATH");
            String pathSeparator = System.getProperty("path.separator");
            String subPath = null;
            File checkedPath = null;
            String[] folderList = null;

            StringTokenizer token = new StringTokenizer(pathVariable, pathSeparator);
            while (token.hasMoreTokens())
            {
                subPath = token.nextToken();
                checkedPath = new File(subPath);
                if (checkedPath.isDirectory())
                {
                    folderList = checkedPath.list();
                    for (String s : folderList)
                    {
                        if (s.equals("emulator") || s.equals("emulator.exe") || s.equals("adb")
                                || s.equals("adb.exe"))
                        {
                            File root = checkedPath.getParentFile();
                            sdkPath = root.getAbsolutePath();
                            break;
                        }
                    }
                }
            }

        }
        else
        {
            // sdk was defined on execution
            sdkPath = sdkPathPrm.getValue();
        }
        return sdkPath;
    }

    /*
     * Returns a list of nodes at document order.
     */
    private static void populateSequencialNodesList(Node node, ArrayList<Node> nodesSequencialList,
            Document[] xmlDoc)
    {
        if (node instanceof Document)
        {
            xmlDoc[0] = (Document) node;
        }
        else if (node instanceof org.w3c.dom.Element)
        {
            nodesSequencialList.add(node);
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            populateSequencialNodesList(children.item(i), nodesSequencialList, xmlDoc);
        }
    }

    /**
     * This method deletes the directory, all files and all subdirectories under
     * it. If a deletion fails, the method stops attempting to delete and
     * returns false.
     * 
     * @param directory
     *            The directory to be deleted
     * @return Returns true if all deletions were successful. If the directory
     *         doesn't exist returns false.
     * @throws IOException
     *             When the parameter isn't a directory
     */
    public static boolean deleteDirRecursively(File directory) throws IOException
    {
        String dirName = ""; //$NON-NLS-1$

        boolean success = true;

        if (directory.exists())
        {
            if (directory.isDirectory())
            {
                dirName = directory.getName();
                File[] children = directory.listFiles();

                for (File element : children)
                {
                    if (element.isFile())
                    {
                        element.deleteOnExit();
                        success = success && element.delete();
                    }
                    else
                    {
                        success = success && deleteDirRecursively(element);
                    }
                }

                directory.deleteOnExit();
                success = success && directory.delete();
            }
            else
            {
                String errorMessage = directory.getName() + " is not a diretory."; //$NON-NLS-1$
                PreflightingLogger.error(errorMessage);
                throw new IOException(errorMessage);
            }
        }
        else
        {
            String errorMessage = "The directory does not exist."; //$NON-NLS-1$
            PreflightingLogger.error(errorMessage);
            success = false;
            throw new IOException(errorMessage);
        }

        if (success && !dirName.equals("")) //$NON-NLS-1$
        {
            PreflightingLogger.info("The directory " + dirName + "was successfully deleted."); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return success;
    }

    /**
     * Get the application version
     * 
     * @param appData
     * @return an integer representing the version
     */
    public static Integer getApplicationVersionCode(XMLElement manifest)
    {
        Integer versionCode = 0;
        if (manifest != null)
        {
            Document doc = manifest.getDocument();
            if (doc != null)
            {
                NodeList manifestList = doc.getElementsByTagName("manifest"); //$NON-NLS-1$
                org.w3c.dom.Element manifestElement = (org.w3c.dom.Element) manifestList.item(0);
                if (manifestElement != null)
                {
                    String strVersion = manifestElement.getAttribute("android:versionCode"); //$NON-NLS-1$
                    try
                    {
                        versionCode = Integer.parseInt(strVersion);
                    }
                    catch (NumberFormatException nfe)
                    {
                        // do nothing
                    }
                }
            }
        }

        return versionCode;
    }

    /**
     * Extract all needed information from a CompilationUnit and fill the
     * internal source file model (SourceFileElement). Fill all invoked Methods
     * and resource constants
     * 
     * @param javaCompilationUnit
     *            JDT model of a .java source file.
     * @param parent
     *            SourceFolderElement, the source folder model containing this
     *            compilation unit.
     * @return
     */
    public static SourceFileElement readFromJava(final CompilationUnit javaCompilationUnit,
            Element parent)
    {
        File file = (File) javaCompilationUnit.getProperty(JAVA_FILE_PROPERTY);
        final SourceFileElement sourceFileElement = new SourceFileElement(file, parent);

        /* Fill type information */
        Object type = javaCompilationUnit.types().get(0); /*
                                                          * Grab the class
                                                          * declaration
                                                          */
        if (type instanceof TypeDeclaration)
        {
            TypeDeclaration typeDeclaration = (TypeDeclaration) type;
            ITypeBinding superClassType = typeDeclaration.resolveBinding().getSuperclass();
            String superClass = superClassType != null ? superClassType.getQualifiedName() : null;
            sourceFileElement.setSuperclassName(superClass);
            String typeFullName = typeDeclaration.resolveBinding().getQualifiedName();
            sourceFileElement.setClassFullPath(typeFullName);
        }

        javaCompilationUnit.accept(new ASTVisitor()
        {

            /*
             * Visit method declaration, searching for instructions.
             */
            @Override
            public boolean visit(MethodDeclaration node)
            {
                // Fill Method information
                Method method = new Method();
                SimpleName name = node.getName();
                method.setMethodName(name.getFullyQualifiedName());
                int modifiers = node.getModifiers();
                boolean methodType = isMethodVirtual(modifiers);
                method.setStatic(ProjectUtils.isStatic(modifiers));

                /*
                 * Extract information regarding method parameters
                 */
                List<SingleVariableDeclaration> parameters = node.parameters();
                for (SingleVariableDeclaration param : parameters)
                {
                    ITypeBinding typeBinding = param.getType().resolveBinding();
                    if (typeBinding != null)
                    {
                        String paramTypeName = typeBinding.getName();
                        method.getParameterTypes().add(paramTypeName);
                    }

                }

                method.setConstructor(node.isConstructor());
                IMethodBinding binding = node.resolveBinding();
                if (binding != null)
                {
                    String returnTypeStr = binding.getReturnType().getName();
                    method.setReturnType(returnTypeStr);
                }

                Block body = node.getBody();
                int lineNumber =
                        body != null ? javaCompilationUnit.getLineNumber(body.getStartPosition())
                                : javaCompilationUnit.getLineNumber(node.getStartPosition());
                method.setLineNumber(lineNumber);

                // Navigate through statements...
                if (body != null)
                {
                    analizeBody(javaCompilationUnit, method, body);
                }

                sourceFileElement.addMethod(getMethodTypeString(methodType), method);
                return super.visit(node);
            }

            /*
             * Visit field declaration, only for R.java file. Extracting
             * declared constants.
             */
            @Override
            public boolean visit(FieldDeclaration node)
            {
                if (sourceFileElement.getName().equals("R.java"))
                {
                    String typeName = node.getType().resolveBinding().getName();
                    int modifiers = node.getModifiers();
                    boolean isStatic = isStatic(modifiers);
                    Field field = new Field();
                    field.setType(typeName);
                    field.setStatic(isStatic);
                    field.setVisibility(getVisibility(modifiers));
                    field.setFinal(isFinal(modifiers));
                    if (isStatic)
                    {
                        List<VariableDeclarationFragment> fragments = node.fragments(); // TODO Verify what to do when
                                                                                        // there's more than one
                                                                                        // fragment... enum?
                        for (VariableDeclarationFragment fragment : fragments)
                        {
                            IVariableBinding binding = fragment.resolveBinding();
                            String name = binding.getName();
                            String declaringClassName = binding.getDeclaringClass().getName();
                            field.setName(declaringClassName + "." + name);
                            Expression initializer = fragment.getInitializer();
                            if (initializer != null)
                            {
                                if (initializer instanceof NumberLiteral)
                                {
                                    NumberLiteral numberInitializer = (NumberLiteral) initializer;
                                    String value = numberInitializer.getToken();
                                    field.setValue(value);
                                }
                            }
                        }
                        sourceFileElement.getStaticFields().add(field);
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(QualifiedName node)
            {
                //visit to recognize R.string or R.layout usage
                if ((node.getQualifier() != null) && node.getQualifier().isQualifiedName())
                {
                    if (node.getQualifier().toString().equals(R_STRING))
                    {
                        sourceFileElement.getUsedStringConstants().add(node.getName().toString());
                    }
                    else if (node.getQualifier().toString().equals(R_LAYOUT))
                    {
                        sourceFileElement.getUsedLayoutConstants().add(node.getName().toString());
                    }
                }

                return super.visit(node);
            }
        });

        return sourceFileElement;
    }

    /*
     * All methods are virtual on Java, except those that are either private or
     * final.
     */
    private static boolean isMethodVirtual(int methodModifiers)
    {
        boolean isVirtual = (methodModifiers & (Modifier.PRIVATE | Modifier.FINAL)) == 0;
        return isVirtual;
    }

    private static String getMethodTypeString(boolean isVirtual)
    {
        return isVirtual ? Method.VIRTUAL : Method.DIRECT;
    }

    /*
     * Verify if modifiers flags contains the 'Static' bit on
     */
    private static boolean isStatic(int modifiers)
    {
        return (modifiers & Modifier.STATIC) != 0;
    }

    /*
     * Verify if modifiers flags contains the 'Final' bit on
     */
    private static boolean isFinal(int modifiers)
    {
        return (modifiers & Modifier.FINAL) != 0;
    }

    /*
     * Search on modifiers flags and retrieve the visibility keyword
     * corresponding to the flag bit
     */
    private static String getVisibility(int modifiers)
    {
        boolean isPublic = (modifiers & Modifier.PUBLIC) != 0;
        boolean isPrivate = (modifiers & Modifier.PRIVATE) != 0;
        boolean isProtected = (modifiers & Modifier.PROTECTED) != 0;

        if (isPublic)
        {
            return ModifierKeyword.PUBLIC_KEYWORD.toString();
        }
        if (isPrivate)
        {
            return ModifierKeyword.PRIVATE_KEYWORD.toString();
        }
        if (isProtected)
        {
            return ModifierKeyword.PROTECTED_KEYWORD.toString();
        }

        return "";

    }

    /*
     * Navigate in a Block and extract called methods and all R constants used
     * as Method parameters.
     */
    private static void analizeBody(final CompilationUnit javaCompilationUnit, final Method method,
            Block body)
    {
        body.accept(new ASTVisitor()
        {

            @Override
            public boolean visit(VariableDeclarationFragment node)
            {
                String varName = node.getName().getIdentifier();
                ITypeBinding typeBinding = node.resolveBinding().getType();
                String typeQualifiedName = typeBinding.getQualifiedName();
                int modifiers = typeBinding.getModifiers();
                boolean isFinal = isFinal(modifiers);
                boolean isStatic = isStatic(modifiers);
                String value = null;
                Expression initializer = node.getInitializer();
                if (initializer != null)
                {
                    value = initializer.toString();
                }
                int lineNumber = javaCompilationUnit.getLineNumber(node.getStartPosition());

                Variable variable = new Variable();
                variable.setName(varName);
                variable.setType(typeQualifiedName);
                variable.setFinal(isFinal);
                variable.setStatic(isStatic);
                variable.setValue(value);
                variable.setLineNumber(lineNumber);

                method.addVariable(variable);

                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node)
            {
                // Fill invoked method model.
                MethodInvocation invoked = node;
                IMethodBinding methodBinding = invoked.resolveMethodBinding();
                if (methodBinding != null)
                {
                    IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
                    ITypeBinding declaringClass = methodDeclaration.getDeclaringClass();
                    String declaringClassName = "";
                    if (declaringClass != null)
                    {
                        declaringClassName = declaringClass.getQualifiedName();
                    }
                    String methodSimpleName = methodBinding.getName();
                    int lineNumber = javaCompilationUnit.getLineNumber(invoked.getStartPosition());
                    String returnType = methodBinding.getReturnType().getQualifiedName();
                    int methodModifiers = methodBinding.getModifiers();
                    boolean isVirtual = isMethodVirtual(methodModifiers);
                    String sourceFileFullPath =
                            ((File) javaCompilationUnit.getProperty(JAVA_FILE_PROPERTY))
                                    .getAbsolutePath();

                    // Retrieve parameter types and look for R constants used
                    // within method arguments
                    List arguments = invoked.arguments();
                    List<String> parameterTypes = new ArrayList<String>(arguments.size());
                    List<String> parameterNames = new ArrayList<String>(arguments.size());
                    for (Object argument : arguments)
                    {
                        Expression argumentExpression = (Expression) argument;
                        ITypeBinding typeBinding = argumentExpression.resolveTypeBinding();
                        String parameterType = "";
                        String parameterName = "";
                        if (typeBinding != null)
                        {
                            parameterType = typeBinding.getName();
                            parameterName = argumentExpression.toString();
                        }
                        else
                        {
                            continue;
                        }

                        parameterTypes.add(parameterType);
                        parameterNames.add(parameterName);
                        if (argumentExpression instanceof QualifiedName) /*
                                                                         * Can
                                                                         * be a
                                                                         * constant
                                                                         * access
                                                                         */
                        {
                            QualifiedName qualifiedName = (QualifiedName) argumentExpression;
                            String fullQualifiedName =
                                    qualifiedName.getQualifier().getFullyQualifiedName();
                            if (fullQualifiedName.startsWith("R.")) /*
                                                                     * Accessing
                                                                     * a R
                                                                     * constant
                                                                     */
                            {
                                Constant constant = new Constant();
                                constant.setSourceFileFullPath(sourceFileFullPath);
                                constant.setLine(lineNumber);
                                constant.setType(parameterType);
                                Object constantExpressionValue =
                                        qualifiedName.resolveConstantExpressionValue();
                                if (constantExpressionValue != null)
                                {
                                    String constantValueHex = constantExpressionValue.toString();
                                    if (constantExpressionValue instanceof Integer)
                                    {
                                        Integer integerValue = (Integer) constantExpressionValue;
                                        constantValueHex = Integer.toHexString(integerValue);
                                    }
                                    constant.setValue(constantValueHex);
                                    method.getInstructions().add(constant);
                                }
                            }

                        }
                    }

                    // Get the name of the object who owns the method being
                    // called.
                    Expression expression = invoked.getExpression();
                    String objectName = null;
                    if ((expression != null) && (expression instanceof SimpleName))
                    {
                        SimpleName simpleName = (SimpleName) expression;
                        objectName = simpleName.getIdentifier();
                    }

                    // Get the variable, if any, that received the method
                    // returned value
                    ASTNode parent = invoked.getParent();
                    String assignedVariable = null;
                    if (parent instanceof VariableDeclarationFragment)
                    {
                        VariableDeclarationFragment variableDeclarationFragment =
                                (VariableDeclarationFragment) parent;
                        assignedVariable = variableDeclarationFragment.getName().getIdentifier();
                    }
                    else if (parent instanceof Assignment)
                    {
                        Assignment assignment = (Assignment) parent;
                        Expression leftHandSide = assignment.getLeftHandSide();
                        if (leftHandSide instanceof SimpleName)
                        {
                            SimpleName name = (SimpleName) leftHandSide;
                            assignedVariable = name.getIdentifier();
                        }
                    }

                    // Fill Invoke object and add to the method model.
                    Invoke invoke = new Invoke();
                    invoke.setLine(lineNumber);
                    invoke.setMethodName(methodSimpleName);
                    invoke.setObjectName(objectName);
                    invoke.setType(getMethodTypeString(isVirtual));
                    invoke.setReturnType(returnType);
                    invoke.setClassCalled(declaringClassName);
                    invoke.setParameterTypes(parameterTypes);
                    invoke.setParameterNames(parameterNames);
                    invoke.setSourceFileFullPath(sourceFileFullPath);
                    invoke.setAssignedVariable(assignedVariable);

                    method.getInstructions().add(invoke);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationExpression node)
            {

                return super.visit(node);
            }

            @Override
            public boolean visit(Assignment node)
            {
                Expression lhs = node.getLeftHandSide();
                String name = "";
                if (lhs instanceof SimpleName)
                {
                    SimpleName simpleName = (SimpleName) lhs;
                    name = simpleName.getIdentifier();
                }
                ITypeBinding typeBinding = lhs.resolveTypeBinding();
                String type = typeBinding.getName();
                // method.addAssigment(assignment);

                // TODO Auto-generated method stub
                return super.visit(node);
            }
        });
    }
}

class XmlLineHandler extends DefaultHandler
{
    private int index = 0;

    private ArrayList<Node> nodesSequencialList = null;

    private Locator locator;

    public XmlLineHandler(ArrayList<Node> nodesSequencialList)
    {
        this.nodesSequencialList = nodesSequencialList;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException
    {
        setCurrentNodeLineNumber(nodesSequencialList.get(index), locator.getLineNumber());
        index++;
    }

    @Override
    public void setDocumentLocator(Locator locator)
    {
        this.locator = locator;
    }

    /*
     * adds line number info to DOM node.
     */
    private void setCurrentNodeLineNumber(Node node, Integer lineNumber)
    {
        node.setUserData(ProjectUtils.LINE_NUMBER_KEY, lineNumber, new EmptyDataHandler());
    }
}

class EmptyDataHandler implements UserDataHandler
{
    public void handle(short operation, String key, Object data, Node src, Node dst)
    {
        // Do nothing
    }
}
