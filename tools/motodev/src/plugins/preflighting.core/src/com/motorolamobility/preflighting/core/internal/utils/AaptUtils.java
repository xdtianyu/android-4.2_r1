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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

public final class AaptUtils
{

    private static final String ANDROID_SMALL_SCREENS = "android:smallScreens";

    public static final String APP_VALIDATOR_TEMP_DIR = "MotodevAppValidator";

    private static final String JAVA_TEMP_DIR_PROPERTY = "java.io.tmpdir";

    private static final String TEMP_DIR_PATH = System.getProperty(JAVA_TEMP_DIR_PROPERTY);

    // Temp folder used for APK extracting
    public static final File tmpAppValidatorFolder =
            new File(TEMP_DIR_PATH, APP_VALIDATOR_TEMP_DIR);

    private static final String CLASSES_DEX = "classes.dex"; //$NON-NLS-1$

    private static final String RESOURCES_ARSC = "resources.arsc"; //$NON-NLS-1$

    private static final String XML_FILE = "xml"; //$NON-NLS-1$

    private static final String ELEMENT_NODE = "E:"; //$NON-NLS-1$

    private static final String ATTRIBUTE_NODE = "A:"; //$NON-NLS-1$

    private static final String NAMESPACE_XMLNS = "N:"; //$NON-NLS-1$

    private static HashMap<String, HashMap<String, String>> resourceValues =
            new HashMap<String, HashMap<String, String>>();

    private static Map<String, String> navigationMap;

    private static Map<String, String> nightMap;

    private static Map<String, String> keyboardMap;

    private static Map<String, String> touchMap;

    private static Map<String, String> densityMap;

    private static Map<String, String> sizeMap;

    private static Map<String, String> orientationMap;

    private static Map<String, String> longMap;

    private static Map<String, String> navHiddenMap;

    private static Map<String, String> keyHiddenMap;

    private static Map<String, String> typeMap;

    public static final String APK_EXTENSION = ".apk";

    public static final String ZIP_EXTENSION = ".zip";

    private static Map<Pattern, Map<String, String>> localizationAttributesMap2 =
            new HashMap<Pattern, Map<String, String>>();

    private static Map<Pattern, String> localizationAttributesMap1 = new HashMap<Pattern, String>();

    private static Pattern[] patternArray = new Pattern[18];

    static
    {
        // Initialize specific maps
        navigationMap = new HashMap<String, String>();
        navigationMap.put("1", "nonav");
        navigationMap.put("2", "dpad");
        navigationMap.put("3", "trackball");
        navigationMap.put("4", "wheel");

        nightMap = new HashMap<String, String>();
        nightMap.put("16", "notnight");
        nightMap.put("32", "night");

        keyboardMap = new HashMap<String, String>();
        keyboardMap.put("1", "nokeys");
        keyboardMap.put("2", "qwerty");
        keyboardMap.put("3", "12key");

        touchMap = new HashMap<String, String>();
        touchMap.put("1", "notouch");
        touchMap.put("2", "stylus");
        touchMap.put("3", "finger");

        densityMap = new HashMap<String, String>();
        densityMap.put("no", "nodpi");
        densityMap.put("120", "ldpi");
        densityMap.put("160", "mdpi");
        densityMap.put("240", "hdpi");

        sizeMap = new HashMap<String, String>();
        sizeMap.put("1", "small");
        sizeMap.put("2", "normal");
        sizeMap.put("3", "large");

        orientationMap = new HashMap<String, String>();
        orientationMap.put("1", "port");
        orientationMap.put("2", "land");
        orientationMap.put("3", "square");

        longMap = new HashMap<String, String>();
        longMap.put("16", "notlong");
        longMap.put("32", "long");

        navHiddenMap = new HashMap<String, String>();
        navHiddenMap.put("8", "navhidden");
        navHiddenMap.put("4", "navexposed");

        keyHiddenMap = new HashMap<String, String>();
        keyHiddenMap.put("1", "keyexposed");
        keyHiddenMap.put("2", "keyhidden");

        typeMap = new HashMap<String, String>();
        typeMap.put("3", "car");

        // initialize localization folder attributes
        // the order of patternArray elements are extremely important, do not
        // modify it
        localizationAttributesMap1.put(patternArray[0] = Pattern.compile("mcc=[0-9]+"), "mcc");
        localizationAttributesMap1.put(patternArray[1] = Pattern.compile("mnc=[0-9]+"), "mnc");
        localizationAttributesMap1.put(patternArray[2] = Pattern.compile("lang=[a-z]+"), "");
        localizationAttributesMap1.put(patternArray[3] = Pattern.compile("cnt=[A-Z]+"), "r");
        localizationAttributesMap2.put(patternArray[4] = Pattern.compile("sz=[0-9]"), sizeMap);
        localizationAttributesMap2.put(patternArray[5] = Pattern.compile("lng=[0-9]+"), longMap);
        localizationAttributesMap2.put(patternArray[6] = Pattern.compile("orient=[0-9]"),
                orientationMap);
        localizationAttributesMap2.put(patternArray[7] = Pattern.compile("type=[0-9]"), typeMap);
        localizationAttributesMap2.put(patternArray[8] = Pattern.compile("night=[0-9]+"), nightMap);
        localizationAttributesMap2.put(patternArray[9] = Pattern.compile("density=[0-9]+"),
                densityMap);
        localizationAttributesMap2.put(patternArray[10] = Pattern.compile("touch=[0-9]"), touchMap);
        localizationAttributesMap2.put(patternArray[11] = Pattern.compile("keyhid=[0-9]"),
                keyHiddenMap);
        localizationAttributesMap2
                .put(patternArray[12] = Pattern.compile("kbd=[0-9]"), keyboardMap);
        localizationAttributesMap2.put(patternArray[13] = Pattern.compile("navhid=[0-9]"),
                navHiddenMap);
        localizationAttributesMap2.put(patternArray[14] = Pattern.compile("nav=[0-9]"),
                navigationMap);
        localizationAttributesMap1.put(patternArray[15] = Pattern.compile("\\sw=[0-9]+"), "");
        localizationAttributesMap1.put(patternArray[16] = Pattern.compile("\\sh=[0-9]+"), "x");
        localizationAttributesMap1.put(patternArray[17] = Pattern.compile("sdk=[0-9]+"), "v");

    }

    /** 
     * Cleans resources maps among executions for applications
     */
    public static void cleanApplicationResourceValues()
    {
        resourceValues.clear();
    }

    public static void extractFilesFromAPK(File apkFile, String sdkPath, File tmpProjectFile)
            throws PreflightingToolException
    {
        if ((tmpProjectFile != null) && tmpProjectFile.exists() && tmpProjectFile.canWrite())
        {
            ZipInputStream apkInputStream = null;
            FileOutputStream apkOutputStream = null;
            try
            {
                // create the buffer and the the zip stream
                byte[] buf = new byte[1024];
                apkInputStream = new ZipInputStream(new FileInputStream(apkFile.getAbsolutePath()));

                ZipEntry apkZipEntry = null;
                try
                {
                    apkZipEntry = apkInputStream.getNextEntry();
                }
                catch (Exception e)
                {
                    PreflightingLogger.error(ApkUtils.class,
                            "It was not possible to read the android package.", e); //$NON-NLS-1$
                }

                if (apkZipEntry == null)
                {
                    throw new IOException("Invalid APK file.");
                }

                String folders = null;
                File fileToCreate = null;

                // create res folder
                fileToCreate = new File(tmpProjectFile, "res");
                if (!fileToCreate.exists())
                {
                    fileToCreate.mkdirs();
                }

                // create the resources file
                Map<File, Document> languageMap =
                        retrieveLocalizationStringsMapFromAPK(sdkPath, apkFile.getAbsolutePath(),
                                "ProjectResourcesValues.xml");
                createLocalizationFilesFromMap(languageMap, fileToCreate);

                // iterates through each entry to be extracted of the android
                // package
                while (apkZipEntry != null)
                {
                    try
                    {
                        String apkEntryName = apkZipEntry.getName();

                        if (apkEntryName.indexOf(Path.SEPARATOR) != -1)
                        {
                            // creates the directory structure
                            folders =
                                    apkEntryName.substring(0,
                                            apkEntryName.lastIndexOf(Path.SEPARATOR));
                            fileToCreate = new File(tmpProjectFile, folders);
                            if (!fileToCreate.exists())
                            {
                                fileToCreate.mkdirs();
                            }
                        }

                        if (apkEntryName.endsWith(XML_FILE))
                        {
                            // Gets XML from the parser
                            fileToCreate = new File(tmpProjectFile, apkEntryName);

                            createXMLFile(sdkPath, apkFile.getAbsolutePath(), apkEntryName,
                                    fileToCreate);
                        }
                        // filter files which is desired to create
                        else if (!apkEntryName.endsWith(RESOURCES_ARSC)
                                && !apkEntryName.endsWith(CLASSES_DEX))
                        {
                            // write the file
                            try
                            {
                                apkOutputStream =
                                        new FileOutputStream(tmpProjectFile.getAbsolutePath()
                                                + Path.SEPARATOR + apkEntryName);

                                int length = 0;
                                while ((length = apkInputStream.read(buf, 0, 1024)) > -1)
                                {
                                    apkOutputStream.write(buf, 0, length);
                                }
                            }
                            finally
                            {
                                if (apkOutputStream != null)
                                {
                                    try
                                    {
                                        apkOutputStream.close();
                                    }
                                    catch (IOException e)
                                    {
                                        //Do Nothing.
                                    }
                                }
                            }
                        }
                    }
                    catch (ZipException zipException)
                    {
                        // throw exception because the apk is probably corrupt
                        PreflightingLogger
                                .error(ApkUtils.class,
                                        "It was not possible to read the android package; it is probably corrupt.", zipException); //$NON-NLS-1$
                        throw new PreflightingToolException(
                                PreflightingCoreNLS.ApkUtils_ImpossibleExtractAndroidPackageMessage,
                                zipException);
                    }
                    catch (IOException ioException)
                    {
                        // log the error but do not thrown an exception because
                        // it will be attempted to create all files
                        PreflightingLogger.error(ApkUtils.class,
                                "It was not possible to extract the android package.", ioException); //$NON-NLS-1$
                    }
                    finally
                    {
                        apkInputStream.closeEntry();
                        apkZipEntry = apkInputStream.getNextEntry();
                    }
                }
            }
            catch (IOException ioException)
            {
                PreflightingLogger.error(ApkUtils.class,
                        "It was not possible to read the android package.", ioException); //$NON-NLS-1$
                throw new PreflightingToolException(
                        PreflightingCoreNLS.ApkUtils_ImpossibleExtractAndroidPackageMessage,
                        ioException);
            }
            finally
            {
                try
                {
                    if (apkInputStream != null)
                    {
                        apkInputStream.close();
                    }
                    if (apkOutputStream != null)
                    {
                        apkOutputStream.close();
                    }
                }
                catch (IOException ioException)
                {
                    // Do Nothing.
                }
            }

        }
        else
        {
            PreflightingLogger.error(ApkUtils.class,
                    "It was not possible to read the android package."); //$NON-NLS-1$
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ApkUtils_ImpossibleExtractAndroidPackageMessage);
        }
    }

    /**
     * Given an APK file, all folders and DOMs for creating the directory
     * structure with the localization files are returned in a {@link Map}. <br>
     * The {@link Map} returned holds the following info: [{@link File},
     * {@link Document}] in which the {@link File} represents the folder path in
     * which the {@link Document} is to be created.
     * 
     * @param aaptPath
     *            AAP tool path.
     * @param apkPath
     *            APK file which the strings of translation are retrieved.
     * @param xmlFileName
     *            XML file name generated by the AAP tool.
     * 
     * @return The {@link Map} structure holding the {@link File}s and
     *         {@link Document}s necessary to create the directory tree for
     *         translation.
     * 
     * @throws PreflightingToolException
     *             Exception thrown in case anything goes wrong extracting data.
     */
    public static Map<File, Document> retrieveLocalizationStringsMapFromAPK(String aaptPath,
            String apkPath, String xmlFileName) throws PreflightingToolException
    {
        BufferedReader bReader = null;
        InputStreamReader reader = null;
        Map<File, Document> map = new HashMap<File, Document>();
        try
        {
            Process aapt =
                    runAAPTCommandForExtractingResourcesAndValues(aaptPath, apkPath, xmlFileName);

            // read output and store it in a buffer
            reader = new InputStreamReader(aapt.getInputStream());
            bReader = new BufferedReader(reader);

            // patterns used to retrieve lines for language, key and values of
            // string translations
            Pattern languagePattern = Pattern.compile("config\\s[0-9]+");
            Pattern stringKeyPattern =
                    Pattern.compile("[\\s]{2,}resource.+:string/[a-zA-Z0-9\\._$]+:");
            Pattern stringArrayKeyPattern =
                    Pattern.compile("[\\s]{2,}resource.+:array/[a-zA-Z0-9\\._$]+:");
            Pattern stringArrayCountPattern = Pattern.compile("Count=[0-9]+");
            Pattern stringValuePattern = Pattern.compile("\".*\"");

            Matcher matcher = null;
            Document document = null;
            Element resourceElement = null;
            Element stringElement = null;
            File languageDirectory = null;

            int stringArraySize = 0;

            String folderName = null;
            String stringArraySizeText = null;
            String key = null;
            String value = null;
            String[] arrayValue = null;

            String infoLine = "";
            while ((infoLine = bReader.readLine()) != null)
            {
                // try to match with language
                matcher = languagePattern.matcher(infoLine);
                if (matcher.find())
                {
                    // in case there are a document and file, add it to the map
                    if ((document != null) && (languageDirectory != null)
                            && (resourceElement != null)
                            && (resourceElement.getChildNodes() != null)
                            && (resourceElement.getChildNodes().getLength() > 0))
                    {
                        map.put(languageDirectory, document);
                        // reset them
                        languageDirectory = null;
                        document = null;
                    }

                    // get the folder name based on the language
                    folderName = createResourcesSubfolders(infoLine, "values");
                    languageDirectory = new File(folderName);

                    // try to find an existent directory
                    document = findDocumentByLanguageDirectory(map, languageDirectory);

                    // the DOM was not found - initialize variables
                    if (document == null)
                    {
                        document = createNewDocument();
                        resourceElement = document.createElement("resources");
                        document.appendChild(resourceElement);
                    }
                    // the DOM was found - get the resources element (root
                    // element)
                    else
                    {
                        resourceElement = document.getDocumentElement();
                    }
                }

                // try to match with single string keys
                matcher = stringKeyPattern.matcher(infoLine);
                if (matcher.find())
                {
                    key = matcher.group();
                    key = key.split(":string/")[1].split(":")[0];

                    infoLine = "";
                    do
                    {
                        // go the the next line in order to read the value
                        infoLine += bReader.readLine();
                    }
                    // do not delete the bReader.ready() statement because this avoids infinitive loops in case the regular expression fails
                    while (!infoLine.matches(".*\".*\".*") && bReader.ready());
                    matcher = stringValuePattern.matcher(infoLine);
                    if (matcher.find())
                    {
                        value = matcher.group();
                        value = value.substring(1, value.length() - 1);

                        // create element to be appended to the resource element
                        appendNewElementToNode("string", "name", key, value, resourceElement,
                                document);
                    }
                }

                // try to match with array string keys
                matcher = stringArrayKeyPattern.matcher(infoLine);
                if (matcher.find())
                {
                    key = matcher.group();
                    key = key.split(":array/")[1].split(":")[0];
                    // go the the next line in order to get the number of
                    // elements in the array
                    infoLine = bReader.readLine();
                    matcher = stringArrayCountPattern.matcher(infoLine);
                    if (matcher.find())
                    {
                        stringArraySizeText = matcher.group();
                        stringArraySize = Integer.parseInt(stringArraySizeText.split("=")[1]);
                        // get each string of the array
                        arrayValue = new String[stringArraySize];
                        for (int arrayStringIndex = 0; arrayStringIndex < stringArraySize; arrayStringIndex++)
                        {
                            try
                            {
                                // go the the next line in order to read the
                                // value
                                infoLine = bReader.readLine();
                                matcher = stringValuePattern.matcher(infoLine);
                                matcher.find();
                                value = matcher.group();
                                value = value.substring(1, value.length() - 1);
                            }
                            catch (Exception e)
                            {
                                // TODO fix this (for now, just keep going, but
                                // this value may be necessary in the future)
                                value = "(reference)";
                            }
                            arrayValue[arrayStringIndex] = value;
                        }

                        // append array-string element
                        stringElement =
                                appendNewElementToNode("string-array", "name", key, null,
                                        resourceElement, document);

                        // create and append the array of strings
                        for (int arrayStringIndex = 0; arrayStringIndex < stringArraySize; arrayStringIndex++)
                        {
                            value = arrayValue[arrayStringIndex];
                            appendNewElementToNode("item", null, null, value, stringElement,
                                    document);
                        }
                    }
                }
            }
            // in case there are a document and file, add it to the map
            if ((document != null) && (languageDirectory != null) && (resourceElement != null)
                    && (resourceElement.getChildNodes() != null)
                    && (resourceElement.getChildNodes().getLength() > 0))
            {
                map.put(languageDirectory, document);
                // reset them
                languageDirectory = null;
                document = null;
            }
        }
        catch (IOException ioException)
        {
            PreflightingLogger.error(ApkUtils.class, ioException.getMessage());
            throw new PreflightingToolException(ioException.getMessage(), ioException);
        }
        finally
        {
            // close resources
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    //Do Nothing.
                }
            }
            if (bReader != null)
            {
                try
                {
                    bReader.close();
                }
                catch (IOException e)
                {
                    //Do Nothing.
                }
            }
        }

        return map;
    }

    /**
     * Execute the AAPT command: aapt d --values resources [ApkFile].apk >
     * [XMLFileName].xml.
     * 
     * @param aaptPath
     *            AAPT path.
     * @param apkPath
     *            Target APK File path.
     * @param xmlFileName
     *            XML file name which will be generated.
     * 
     * @return The {@link Process} created the the execution of the AAPT
     *         command.
     * 
     * @throws IOException
     *             Exception thrown in case the command execution fails.
     */
    private static Process runAAPTCommandForExtractingResourcesAndValues(String aaptPath,
            String apkPath, String xmlFileName) throws IOException
    {
        // execute command: aapt.exe d --values resources <name>.apk <name>.xml
        String[] aaptCommand = new String[]
        {
                aaptPath, "d", "--values", "resources", apkPath, xmlFileName //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                };

        return Runtime.getRuntime().exec(aaptCommand);
    }

    /**
     * Put data from {@link Map}, created in method
     * {@link #retrieveLocalizationStringsMapFromAPK(String, String, String)}
     * into the /res directory using the given parameter {@link File}.
     * 
     * @param map
     *            {@link Map} which data will be extracted.
     * @param resFile
     *            {@link File} structure which will hold the tree model holding
     *            directories and translation files.
     * @throws PreflightingToolException
     */
    private static void createLocalizationFilesFromMap(Map<File, Document> map, File resFile)
            throws PreflightingToolException
    {
        Set<File> fileSet = map.keySet();
        File stringFolder = null;

        // iterate through all directories
        for (File key : fileSet)
        {
            // create temporary directories
            stringFolder = new File(resFile, key.getPath());
            if (!stringFolder.exists())
            {
                stringFolder.mkdirs();
            }
            // create XML file           
            createXmlFromDom(map.get(key), new File(resFile.getAbsolutePath() + Path.SEPARATOR
                    + key.getPath() + File.separator + "strings.xml"));
        }
    }

    /**
     * Create a XML File based on an AAPT output from a APK embedded file.
     * 
     * @param aaptPath
     *            AAPT path.
     * @param apkPath
     *            APK path.
     * @param xmlFileName
     *            the XML file name which is embedded in the APT and is to be
     *            created as an XML file.
     * @param fileToCreate
     *            XML file to be created.
     * 
     * @throws PreflightingToolException
     *             Exception thrown when there are problems creating the XML
     *             file. The exception message describe in details the problem.
     */
    public static void createXMLFile(String aaptPath, String apkPath, String xmlFileName,
            File fileToCreate) throws PreflightingToolException
    {
        // command for AAPT tool which gets the XML-to-be file to be worked on
        String[] aaptCommand = new String[]
        {
                aaptPath, "dump", "xmltree", apkPath, xmlFileName //$NON-NLS-1$ //$NON-NLS-2$
                };

        // execute AAPT command
        Process aapt = null;
        try
        {
            aapt = Runtime.getRuntime().exec(aaptCommand);
        }
        catch (IOException ioException)
        {
            PreflightingLogger.error(ApkUtils.class, "Problems executing AAPT command.", //$NON-NLS-1$
                    ioException);
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ApkUtils_AaptExecutionProblemMessage, ioException);
        }

        Map<String, String> namespaceMap = new HashMap<String, String>();
        Map<Integer, LineElement> map =
                readToMap(aapt, aaptPath, apkPath, fileToCreate.getAbsolutePath(), namespaceMap);

        if (!map.isEmpty())
        {
            Integer outerRow = map.keySet().size();
            Integer innerRow;
            while (outerRow > 0)
            {
                LineElement childElement = map.get(outerRow);
                innerRow = outerRow - 1;
                while (innerRow > 0)
                {
                    LineElement parentElement = map.get(innerRow);
                    if (parentElement.getDepth() < childElement.getDepth())
                    {
                        parentElement.addChildLine(outerRow);
                        break;
                    }
                    innerRow--;
                }
                outerRow--;
            }

            // create new DOM
            Document document = createNewDocument();
            // populate it
            addNodes(document, map, map.get(1), null);
            // add schema
            for (String namespace : namespaceMap.keySet())
            {
                document.getDocumentElement().setAttribute("xmlns:" + namespace,
                        namespaceMap.get(namespace));
            }

            // create XML file
            createXmlFromDom(document, fileToCreate);
        }
    }

    /**
     * generate folder names according to configurations
     * 
     * @param lineRead
     *            line read from aapt output
     * @param folderPrefix
     *            The first name of all folders.
     * @return the directory name
     * @throws PreflightingToolException
     *             Exception thrown when the entered line has a bad format.
     */
    private static String createResourcesSubfolders(String lineRead, String folderPrefix)
            throws PreflightingToolException
    {
        Pattern configPattern = Pattern.compile("config\\s[0-9]");

        Matcher matcher = null;
        StringBuffer strBuf = new StringBuffer(lineRead);
        // try to match with type
        matcher = configPattern.matcher(strBuf);
        if (matcher.find())
        {
            for (int i = 0; i < 18; i++)
            {
                matcher = patternArray[i].matcher(strBuf);
                if (matcher.find())
                {
                    String result = matcher.group();
                    String value = result.split("=")[1];
                    // special treatment
                    if (localizationAttributesMap2.containsKey(patternArray[i]))
                    {
                        String nameSegment =
                                localizationAttributesMap2.get(patternArray[i]).get(value);
                        if (nameSegment != null)
                        {
                            folderPrefix += "-" + nameSegment;
                        }
                    }
                    else
                    {
                        String nameSegment =
                                localizationAttributesMap1.get(patternArray[i]) + value;
                        // treat the specific case of height, whose value is
                        // preceded by x egg. 1024x864
                        if (i != 16)
                        {
                            folderPrefix += "-" + nameSegment;
                        }
                        else
                        {
                            folderPrefix += nameSegment;
                        }
                    }
                }
            }
        }
        else
        {
            PreflightingLogger.error("The entered line has a bad format.");
            throw new PreflightingToolException("The entered line has a bad format.");
        }

        return folderPrefix;
    }

    /**
     * Create a XML file from a {@link Document}.
     * 
     * @param document
     *            Document to be turned into a XML File.
     * @param xmlFile
     *            XML file which will receive the {@link Document} stream.
     * 
     * @throws PreflightingToolException
     *             Exception thrown when there are problems creating the XML
     *             file.
     */
    private static void createXmlFromDom(Document document, File xmlFile)
            throws PreflightingToolException
    {

        StreamResult result = null;
        DOMSource source = null;
        Transformer transformer = null;
        FileOutputStream fo = null;
        StringWriter sw = null;

        try
        {
            // get factory
            TransformerFactory transformerFactory = TransformerFactory.newInstance();

            // get transformer and configure it
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
            // create the XML file
            source = new DOMSource(document);
            sw = new StringWriter();
            result = new StreamResult(sw);
            transformer.transform(source, result);
            fo = new FileOutputStream(xmlFile);
            fo.write(sw.toString().getBytes("utf-8"));

        }
        catch (Exception ex)
        {
            //log error, but try to continue validation without the XML file with problem 
            PreflightingLogger.error(ApkUtils.class, "Problems creating the XML file.", ex); //$NON-NLS-1$            
        }
        finally
        {
            try
            {
                // Close streams and stuff
                if (fo != null)
                {
                    fo.close();
                }

                if (result.getWriter() != null)
                {
                    result.getWriter().close();
                }

                if (sw != null)
                {
                    sw.close();
                }

            }
            catch (IOException e)
            {
                // do nothing
            }
        }
    }

    /**
     * Create a new {@link Document}.
     * 
     * @return A newly-created {@link Document} object.
     * 
     * @throws PreflightingToolException
     *             Exception thrown when there are problems creating a new
     *             {@link Document}.
     */
    private static Document createNewDocument() throws PreflightingToolException
    {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try
        {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException pcException)
        {
            PreflightingLogger
                    .error(ApkUtils.class, "Problems creating DOM isntance.", pcException); //$NON-NLS-1$
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ApkUtils_DomInstanceProblemMessage, pcException);
        }
        // create DOM
        Document document = documentBuilder.newDocument();
        return document;
    }

    /**
     * Create a Map holding the AAPT XML output info.
     * 
     * @param aaptProccess
     *            AAPT execution process - its data will be processed here
     * @param aaptPath
     *            APPT path
     * @param apkPath
     *            APK path
     * @param xmlFileName
     *            XML file name
     * 
     * @return The Map holding APPT XML output info.
     * 
     * @throws PreflightingToolException
     *             Exception thrown in case there are problems reading the APPT
     *             XML output info from the process.
     */
    public static Map<Integer, LineElement> readToMap(Process aaptProccess, String aaptPath,
            String apkPath, String xmlFileName, Map<String, String> namespaceMap)
            throws PreflightingToolException
    {
        InputStreamReader reader = new InputStreamReader(aaptProccess.getInputStream());
        BufferedReader bReader = new BufferedReader(reader);

        // list for the map
        List<LineElement> lineList = new ArrayList<LineElement>();
        LineElement lineElement;

        String infoLine;
        try
        {
            while ((infoLine = bReader.readLine()) != null)
            {
                if (infoLine.length() > 0)
                {
                    lineElement = new LineElement();
                    if (infoLine.contains(ELEMENT_NODE) || infoLine.contains(ELEMENT_NODE))
                    {
                        lineElement.setType(LineElement.LineType.ELEMENT);
                        lineElement.setDepth(infoLine.split(ELEMENT_NODE)[0].length());
                        lineElement.setName(getElementLineName(infoLine));
                        lineList.add(lineElement);
                    }
                    else if (infoLine.contains(ATTRIBUTE_NODE))
                    {
                        lineElement.setType(LineElement.LineType.ATTRIBUTE);
                        lineElement.setDepth(infoLine.split(ATTRIBUTE_NODE)[0].length());
                        lineElement.setName(getElementLineName(infoLine));
                        lineElement.setValue(getElementLineValue(aaptPath, apkPath, xmlFileName,
                                infoLine));
                        lineList.add(lineElement);
                    }
                    else if (infoLine.contains(NAMESPACE_XMLNS))
                    {
                        String namespace = infoLine.split(NAMESPACE_XMLNS)[1].trim();
                        if (namespace.indexOf("=") != -1)
                        {
                            String id = namespace.substring(0, namespace.indexOf("="));
                            String url = namespace.substring(namespace.indexOf("=") + 1);
                            namespaceMap.put(id, url);
                        }
                    }
                }
            }
        }
        catch (IOException ioException)
        {
            PreflightingLogger.error(ApkUtils.class,
                    "Problems reading AAPT command execution result.", ioException); //$NON-NLS-1$
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ApkUtils_AaptResultReadProblemMessage, ioException);
        }
        finally
        {
            // close resources
            try
            {
                if (bReader != null)
                {
                    bReader.close();
                }
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (IOException ioException)
            {
                PreflightingLogger.error(ApkUtils.class,
                        "Problems reading AAPT command execution result.", ioException); //$NON-NLS-1$
                throw new PreflightingToolException(
                        PreflightingCoreNLS.ApkUtils_AaptResultReadProblemMessage, ioException);
            }
        }

        Map<Integer, LineElement> map = new HashMap<Integer, LineElement>();
        Integer counter = 0;
        for (LineElement elem : lineList)
        {
            ++counter;
            map.put(counter, elem);
        }
        return map;

    }

    /**
     * Get the Element Line?s Name from a text line. It could either be an
     * Element or an Attribute.
     * 
     * @param lineText
     *            Text Line where the Name will be retrieved.
     * 
     * @return Returns the Name.
     */
    private static String getElementLineName(String lineText)
    {
        Matcher matcher = null;
        Pattern pattern = null;
        String matchText = null;
        String name = null;

        // try to match the element pattern
        pattern = Pattern.compile("(E: .+ ){1}"); //$NON-NLS-1$
        matcher = pattern.matcher(lineText);
        // in case there is a match, populate the Line Element object
        if (matcher.find())
        {
            matchText = matcher.group();
            name = matchText.split(ELEMENT_NODE)[1].trim();
        }
        else
        {
            // try to match the attribute pattern
            pattern = Pattern.compile("(A:){1}"); //$NON-NLS-1$
            matcher = pattern.matcher(lineText);
            if (matcher.find())
            {
                // since there is an element pattern, get its name
                pattern = Pattern.compile("^ *A:\\s*[\\w:\\w]*"); //$NON-NLS-1$
                matcher = pattern.matcher(lineText);
                if (matcher.find())
                {
                    matchText = matcher.group();
                    // get the name
                    name = matchText.split(ATTRIBUTE_NODE)[1];
                    // adjust it
                    name = name.trim();
                }
            }
        }

        return name;
    }

    /**
     * Try to find the {@link Document} associated with a certain language
     * directory path. In case nothing is found, null is returned.
     * 
     * @param map
     *            {@link Map} in which the search will be made.
     * @param languageDirectory
     *            Directory holding the path to be compared, in order to find
     *            the {@link Document} in the given {@link Map}. This Object is
     *            updated with a reference to the object in the {@link Map}.
     * 
     * @return {@link Document} element associated, in case a match is
     *         successful.
     */
    private static Document findDocumentByLanguageDirectory(Map<File, Document> map,
            File languageDirectory)
    {
        Document document = null;
        Set<File> languageFolders = map.keySet();
        if (languageFolders != null)
        {
            for (File languageFolder : languageFolders)
            {
                if (languageFolder.getPath().equals(languageDirectory.getPath()))
                {
                    document = map.get(languageFolder);
                    languageDirectory = languageFolder;
                    break;
                }
            }
        }
        return document;
    }

    /**
     * Given a certain parent {@link Element} and {@link Document}, append a
     * child {@link Element} with Tag Name (which cannot be null), Node
     * Attribute Name, Node Attribute Value (which both are null at the same
     * time or none is null at all), Node Value (which can be null).
     * 
     * @param nodeTagName
     *            Node Tag Name.
     * @param nodeAttributeName
     *            Node Attribute Name.
     * @param nodeAttributeValue
     *            Node Attribute Value.
     * @param nodeValue
     *            Node Value.
     * @param elementToBeApppendedTo
     *            Parent {@link Element} which the new created {@link Element}
     *            will be appended to.
     * @param document
     *            {@link Document} which everything belongs to.
     * 
     * @return Returns the created {@link Element}.
     */
    private static Element appendNewElementToNode(String nodeTagName, String nodeAttributeName,
            String nodeAttributeValue, String nodeValue, Element elementToBeApppendedTo,
            Document document)
    {
        // create element and append it
        Element element = document.createElement(nodeTagName);
        if ((nodeAttributeName != null) && (nodeAttributeValue != null))
        {
            element.setAttribute(nodeAttributeName, nodeAttributeValue);
        }
        if (nodeValue != null)
        {
            element.setTextContent(nodeValue);
        }
        elementToBeApppendedTo.appendChild(element);

        return element;
    }

    /**
     * Add all attributes and children in a {@link Document}, given a
     * {@link LineElement}.
     * 
     * @param document
     *            DOM where elements are added.
     * @param map
     *            Map holding all {@link LineElement}s.
     * @param elem
     *            Element to be added to the DOM.
     * @param rootElement
     *            Root element.
     */
    private static void addNodes(Document document, Map<Integer, LineElement> map,
            LineElement elem, Element rootElement)
    {
        if (elem.getType() == LineElement.LineType.ELEMENT)
        {
            // add element or root
            Element element = document.createElement(elem.getName());
            if (rootElement == null)
            {
                document.appendChild(element);
            }
            else
            {
                rootElement.appendChild(element);
            }

            // add children
            for (Integer childElementMapIndex : elem.getChildLines())
            {
                LineElement childElement = map.get(childElementMapIndex);
                // add all attributes from this node
                if (childElement.getType() == LineElement.LineType.ATTRIBUTE)
                {
                    element.setAttribute(childElement.getName(), childElement.getValue());
                }
                // add a child element
                else
                {
                    addNodes(document, map, childElement, element);
                }

            }
        }
    }

    /**
     * Retrieve the Value of an Text line.
     * 
     * @param lineText
     *            Text line where the value will be retrieved from.
     * 
     * @return Value retrieved.
     */
    private static String getElementLineValue(String aaptPath, String apkPath, String xmlFileName,
            String lineText)
    {
        Matcher matcher = null;
        Pattern pattern = null;
        String matchText = null;
        String name = null;

        // Get the values, depending on their pattern

        // start with Raw values
        pattern = Pattern.compile("(\\(Raw: \".*\"\\)){1}"); //$NON-NLS-1$
        matcher = pattern.matcher(lineText);
        if (matcher.find())
        {
            matchText = matcher.group();
            // get the element within ""
            pattern = Pattern.compile("(\".*\"){1}"); //$NON-NLS-1$
            matcher = pattern.matcher(matchText);
            if (matcher.find())
            {
                matchText = matcher.group();
                name = matchText.replaceAll("\"", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        else
        {
            // get values after @
            pattern = Pattern.compile("(\\)=@.*){1}"); //$NON-NLS-1$
            matcher = pattern.matcher(lineText);
            if (matcher.find())
            {
                matchText = matcher.group();
                name = matchText.replaceAll("\\)=@", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
                name = getResourceMatch(aaptPath, apkPath, xmlFileName, name);
            }
            else
            {
                // get values with type
                pattern = Pattern.compile("(\\(type .*\\).*){1}"); //$NON-NLS-1$
                matcher = pattern.matcher(lineText);
                if (matcher.find())
                {
                    matchText = matcher.group();
                    name = matchText.replaceAll("(\\(type .*\\))", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    name = name.replace("0x", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    try
                    {
                        long longValue = Long.parseLong(name, 16);
                        name = Long.toHexString(longValue).trim();

                        // TODO: correctly handle types instead of doing this kind of verification
                        if (lineText.contains(ANDROID_SMALL_SCREENS)
                                || lineText.contains("android:normalScreens")
                                || lineText.contains("android:largeScreens")
                                || lineText.contains("android:xlargeScreens")
                                || lineText.contains("android:anyDensity")
                                || lineText.contains("android:resizeable"))

                        {
                            name = longValue == 0 ? "false" : "true";
                        }

                    }
                    catch (NumberFormatException ex)
                    {
                        /*
                         * Do nothing because the number could not be converted
                         * to an integer. Leave it as it is to put in the XML
                         * file.
                         */
                    }
                }
            }
        }

        return name;
    }

    /**
     * Get the Resource reference from a @x value in the AAPT XML output.
     * 
     * @param aaptPath
     *            AAPT Path.
     * @param apkPath
     *            APK Path.
     * @param xmlFileName
     *            XML file Name
     * @param resourceId
     *            Resource Id which the value will be retrieved.
     * 
     * @return Value referenced by a resource Id.
     */
    private static String getResourceMatch(String aaptPath, String apkPath, String xmlFileName,
            String resourceId)
    {
        xmlFileName = xmlFileName.substring(xmlFileName.indexOf(".tmp") + 5);

        // we parse a xml file only once, so we check if its values are already
        // stored
        if (resourceValues.get(xmlFileName) == null)
        {
            HashMap<String, String> currentMap = new HashMap<String, String>();

            BufferedReader bReader = null;
            InputStreamReader reader = null;
            try
            {
                Process aapt =
                        runAAPTCommandForExtractingResourcesAndValues(aaptPath, apkPath,
                                xmlFileName);

                // read output and store it in a buffer
                reader = new InputStreamReader(aapt.getInputStream());
                bReader = new BufferedReader(reader);

                String infoLine = ""; //$NON-NLS-1$
                StringBuffer strBuf = new StringBuffer();
                while ((infoLine = bReader.readLine()) != null)
                {
                    strBuf.append(infoLine);
                    strBuf.append("\n"); //$NON-NLS-1$
                }

                // apply pattern to retrieve resource id and its value
                Pattern pattern =
                        Pattern.compile("resource\\s[0-9a-fxA-FX]+\\s[a-zA-Z_0-9.]+:[a-z0-9./_]+:"); //$NON-NLS-1$
                Matcher matcher = pattern.matcher(strBuf);

                Pattern keyPattern = Pattern.compile("\\s[0-9a-fxA-FX]+\\s"); //$NON-NLS-1$
                Pattern valuePattern = Pattern.compile(":[a-z0-9./_]+:"); //$NON-NLS-1$

                while (matcher.find())
                {
                    String match = matcher.group();
                    // key matcher
                    Matcher keyMatcher = keyPattern.matcher(match);
                    keyMatcher.find();
                    String key = keyMatcher.group();
                    key = key.trim();

                    // aapt output has a resource reference for each
                    // configuration
                    // e.g. a drawable resource can present three densities:
                    // hpdi, mpdi, lpdi
                    if (!currentMap.containsKey(key))
                    {
                        // value matcher
                        Matcher valueMatcher = valuePattern.matcher(match);
                        valueMatcher.find();
                        String value = valueMatcher.group();
                        value = "@" + value.substring(1, value.length() - 1); //$NON-NLS-1$

                        currentMap.put(key, value);
                    }
                }
                // store in global variable
                resourceValues.put(xmlFileName, currentMap);
            }
            catch (Exception e)
            {
                PreflightingLogger.error(ApkUtils.class, e.getMessage());
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        //Do nothing.
                    }
                }
                if (bReader != null)
                {
                    try
                    {
                        bReader.close();
                    }
                    catch (IOException e)
                    {
                        //Do nothing.
                    }
                }
            }
        }

        return resourceValues.get(xmlFileName).get(resourceId);
    }
}

/**
 * Class which holds each line information from the AAPT XML output. 
 */
class LineElement
{

    /**
     * Enumerator which determines which type of the emement it is to be put in
     * the XML file.    
     */
    public enum LineType
    {
        ELEMENT, ATTRIBUTE
    }

    private final List<Integer> childLines = new ArrayList<Integer>();

    /**
     * Get the list of children indexes.
     * 
     * @return List of children indexes.
     */
    public List<Integer> getChildLines()
    {
        Collections.sort(childLines);
        return childLines;
    }

    /**
     * Add a child index representation.
     * 
     * @param index
     *            Child inex representation.
     */
    public void addChildLine(Integer index)
    {
        childLines.add(index);
    }

    private LineType type;

    /**
     * Get the {@link LineType}.
     * 
     * @return The {@link LineType}.
     */
    public LineType getType()
    {
        return type;
    }

    /**
     * Set the {@link LineType}.
     * 
     * @param type
     *            The {@link LineType}.
     */
    public void setType(LineType type)
    {
        this.type = type;
    }

    /**
     * Get the Name.
     * 
     * @return The name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the Name.
     * 
     * @param name
     *            The name.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the node depth.
     * 
     * @return The node depth.
     */
    public int getDepth()
    {
        return depth;
    }

    /**
     * Set the node depth.
     * 
     * @param depth
     *            The node depth.
     */
    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    private String name;

    private String value;

    /**
     * Get the Node value.
     * 
     * @return The node value.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set the node value.
     * 
     * @param value
     *            The node value.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    private int depth;

}
