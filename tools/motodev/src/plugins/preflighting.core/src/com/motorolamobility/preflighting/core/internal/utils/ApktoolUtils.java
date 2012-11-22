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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.source.model.Constant;
import com.motorolamobility.preflighting.core.source.model.Field;
import com.motorolamobility.preflighting.core.source.model.Instruction;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.Method;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;
import com.motorolamobility.preflighting.core.source.model.Variable;

/**
 * Extracts smali representation for the files inside an apk
 */
public final class ApktoolUtils
{
    /**
     * 
     */
    private static final String PARAM_SEPARATOR = ";";

    private static final String INNER_CLASS = "InnerClass";

    private static final String SEMICOLON = PARAM_SEPARATOR;

    private static final String RPAREN = ")";

    private static final String LPAREN = "(";

    private static final String COLON = ":";

    private static final String TYPE_PREFIX_L = "L";

    private static final String ARRAYTYPE_PREFIX = "[";

    private static final String PARAMETERS_SEPARATOR = SEMICOLON;

    private static final String CONST = "const";

    private static final String METHODITEMS_SEPARATOR2 = "->";

    private static final String METHODITEMS_SEPARATOR1 = ";->";

    private static final String INVOKE = "invoke";

    private static final String END_METHOD = ".end method";

    private static final String CONSTRUCTOR = "constructor";

    private static final String ANNOTATION = ".annotation";

    private static final String METHOD = ".method";

    private static final String ASSIGNMENT_OPERATOR = "=";

    private static final String FINAL = "final";

    private static final String STATIC = "static";

    private static final String FIELD = ".field";

    private static final String SOURCE = ".source";

    private static final String SUPER = ".super";

    private static final String CLASS = ".class";

    private static final String LINE = ".line";

    private static final String VIRTUAL_METHODS = "# virtual methods";

    private static final String DIRECT_METHODS = "# direct methods";

    private static final String INSTANCE_FIELDS = "# instance fields";

    private static final String STATIC_FIELDS = "# static fields";

    private static final String SMALI = ".smali";

    private static final String APK = ".apk";

    private static final String APKTOOL_JAR_PATH = "/apktool/apktool.jar";

    private static Map<String, String> smaliTypeMap = new HashMap<String, String>(9);

    static
    {
        smaliTypeMap.put("V", PrimitiveType.VOID.toString());
        smaliTypeMap.put("Z", PrimitiveType.BOOLEAN.toString());
        smaliTypeMap.put("B", PrimitiveType.BYTE.toString());
        smaliTypeMap.put("S", PrimitiveType.SHORT.toString());
        smaliTypeMap.put("C", PrimitiveType.CHAR.toString());
        smaliTypeMap.put("I", PrimitiveType.INT.toString());
        smaliTypeMap.put("J", PrimitiveType.LONG.toString());
        smaliTypeMap.put("F", PrimitiveType.FLOAT.toString());
        smaliTypeMap.put("D", PrimitiveType.DOUBLE.toString());
    }

    /**
     * Extracts smali representation for the files inside an apk
     * @param apkFolder
     * @param appName apk filename
     * @return
     * @throws InterruptedException 
     * @throws IOException 
     * @throws AndrolibException 
     */
    public static SourceFolderElement extractJavaModel(File apkFolder, Element parent, File smaliDir)
            throws IOException, InterruptedException
    {
        SourceFolderElement model = new SourceFolderElement(apkFolder, parent, true);
        Set<File> smaliFiles = visitFolderToIdentifySmalis(smaliDir);
        for (File smaliFile : smaliFiles)
        {
            SourceFileElement m = readFromSmali(smaliFile, model);
            model.getSourceFileElements().add(m);
        }
        return model;
    }

    /**
     * Extract information from methods, fields, constants, invocations, etc
     * from a smali file
     * @param smali file to read
     * @param parent 
     * @return smali model
     * @throws FileNotFoundException
     */
    private static SourceFileElement readFromSmali(File smali, Element parent)
            throws FileNotFoundException
    {
        SourceFileElement model = new SourceFileElement(smali, parent);
        FileInputStream fileInputStream = new FileInputStream(smali);
        Scanner scanner = new Scanner(fileInputStream);
        try
        {
            boolean readingStaticFields = false;
            boolean readingInstanceFields = false;
            boolean readingDirectMethods = false;
            boolean readingVirtualMethods = false;
            int lineInfo = -1;
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                if ((line != null) && !line.equals(""))
                {
                    if (line.trim().startsWith(STATIC_FIELDS))
                    {
                        readingStaticFields = true;
                        readingInstanceFields =
                                readingDirectMethods = readingVirtualMethods = false;
                    }
                    else if (line.trim().startsWith(INSTANCE_FIELDS))
                    {
                        readingInstanceFields = true;
                        readingStaticFields = readingDirectMethods = readingVirtualMethods = false;
                    }
                    else if (line.trim().startsWith(DIRECT_METHODS))
                    {
                        readingDirectMethods = true;
                        readingStaticFields = readingInstanceFields = readingVirtualMethods = false;
                    }
                    else if (line.trim().startsWith(VIRTUAL_METHODS))
                    {
                        readingVirtualMethods = true;
                        readingStaticFields = readingInstanceFields = readingDirectMethods = false;
                    }
                    else if (line.trim().startsWith(LINE))
                    {
                        StringTokenizer token = new StringTokenizer(line);
                        if (token.hasMoreTokens())
                        {
                            token.nextToken();
                        }
                        if (token.hasMoreTokens())
                        {
                            try
                            {
                                lineInfo = Integer.parseInt(token.nextToken());
                            }
                            catch (NumberFormatException e)
                            {
                                PreflightingLogger.error("Could not get line Number for line "
                                        + line);
                            }
                        }
                    }
                    else if (line.trim().startsWith(CLASS))
                    {
                        String[] tokens = line.trim().split(" ");
                        for (int i = 0; i < tokens.length; i++)
                        {
                            if ((tokens[i] != null) && tokens[i].startsWith(TYPE_PREFIX_L))
                            {
                                model.setClassFullPath(getType(tokens[i]));
                                break;
                            }
                        }
                    }
                    else if (line.trim().startsWith(SUPER))
                    {
                        String[] tokens = line.trim().split(" ");
                        if (tokens.length == 2)
                        {
                            model.setSuperclassName(tokens[1].substring(1)); //remove first L
                        }
                    }
                    else if (line.trim().startsWith(SOURCE))
                    {
                        String[] tokens = line.trim().split(" ");
                        if (tokens.length == 2)
                        {
                            String sourceName = tokens[1];
                            sourceName = sourceName.substring(1, sourceName.length() - 1); //Remove quotes
                            model.setSourceName(sourceName);
                            String pkg = getPkg(model.getClassFullPath());
                            model.setFile(new File(pkg.replace('.', '/') + "/" + sourceName)); //Smali file makes no sense for checkers because it's temporary.
                        }
                    }
                    else if (line.trim().startsWith(FIELD))
                    {
                        Field field = new Field();
                        if (line.contains(STATIC))
                        {
                            field.setStatic(true);
                        }
                        if (line.contains(FINAL))
                        {
                            field.setFinal(true);
                        }
                        String[] tokens = line.trim().split(" ");
                        String visibility = tokens[1];

                        field.setVisibility(visibility);
                        for (int i = 0; i < tokens.length; i++)
                        {
                            //find attribute name and type (separated by colon)
                            if (tokens[i].trim().contains(COLON))
                            {
                                String[] aux = tokens[i].trim().split(COLON);
                                String name = model.getName();
                                if (name.contains("$"))
                                {
                                    name =
                                            name.substring(name.indexOf("$") + 1,
                                                    name.lastIndexOf("."));
                                    field.setName(name + "." + aux[0]);
                                }
                                if (aux.length > 1)
                                {
                                    String typeKey = aux[1];
                                    String type = "";
                                    type = getType(typeKey);

                                    field.setType(type);
                                }
                                break;
                            }
                        }
                        if (ASSIGNMENT_OPERATOR.equals(tokens[tokens.length - 2]))
                        {
                            //there is assignment = <value>
                            field.setValue(tokens[tokens.length - 1]);
                        }
                        if (readingInstanceFields)
                        {
                            model.getInstanceFields().add(field);
                        }
                        else if (readingStaticFields)
                        {
                            model.getStaticFields().add(field);
                        }
                    }
                    else if (line.trim().startsWith(METHOD))
                    {
                        //Map representing the Smali variable index(vX) -> variable name
                        Map<String, String> varMap = new HashMap<String, String>();
                        Method method = new Method();
                        if (line.contains(STATIC))
                        {
                            method.setStatic(true);
                        }
                        if (line.contains(CONSTRUCTOR))
                        {
                            method.setConstructor(true);
                        }
                        String[] tokens = line.trim().split(" ");
                        for (int i = 0; i < tokens.length; i++)
                        {
                            if (tokens[i].trim().contains(LPAREN))
                            {
                                int lP = tokens[i].trim().indexOf(LPAREN);
                                if (lP >= 0)
                                {
                                    String temp = tokens[i].trim().substring(0, lP);
                                    method.setMethodName(temp);
                                    int rP = tokens[i].trim().indexOf(RPAREN);
                                    String params = null;
                                    if (rP >= 0)
                                    {
                                        params = tokens[i].trim().substring(lP + 1, rP);
                                        if (!params.equals(""))
                                        {
                                            String[] methodParams = params.split(SEMICOLON);
                                            for (String param : methodParams)
                                            {
                                                //Get all params (it gets only one in cases like IIL<type>; that results integer, integer, type, integer 
                                                List<String> paramTypes =
                                                        getParamTypes(param + PARAMETERS_SEPARATOR);
                                                method.getParameterTypes().addAll(paramTypes);
                                            }
                                        }
                                        String returnType = tokens[i].trim().substring(rP + 1);
                                        method.setReturnType(getType(returnType));
                                    }
                                    break;
                                }

                            }
                        }
                        line = scanner.nextLine();
                        Invoke invoke = null;
                        while (!line.trim().startsWith(END_METHOD))
                        {
                            //read instructions
                            if (line.trim().startsWith(INVOKE))
                            {
                                invoke = new Invoke();
                                if (line.contains("-" + Method.DIRECT))
                                {
                                    invoke.setType(Method.DIRECT);
                                }
                                else if (line.contains("-" + Method.VIRTUAL))
                                {
                                    invoke.setType(Method.VIRTUAL);
                                }
                                int objectIdxStart = line.indexOf('{') + 1;
                                int objectIdxEnd = line.indexOf('}');
                                String paramsIds = line.substring(objectIdxStart, objectIdxEnd);
                                String objIdx = null;
                                List<String> paramNames = new ArrayList<String>();
                                if (paramsIds.contains(","))
                                {
                                    String[] paramsSplit = paramsIds.split(", ");
                                    // the returned object is always the first item
                                    objIdx = paramsSplit[0];
                                    // the other items are parameters
                                    for (int paramsSplitIndex = 1; paramsSplitIndex < paramsSplit.length; paramsSplitIndex++)
                                    {
                                        if (varMap.containsKey(paramsSplit[paramsSplitIndex]))
                                        {
                                            paramNames.add(varMap
                                                    .get(paramsSplit[paramsSplitIndex]));
                                        }
                                        else
                                        {
                                            paramNames.add(paramsSplit[paramsSplitIndex]);
                                        }
                                    }
                                }
                                else
                                {
                                    // doesn't have parameters
                                    objIdx = !paramsIds.equals("p0") ? paramsIds : null;
                                }

                                String objectName = null;
                                if (varMap.containsKey(objIdx))
                                {
                                    objectName = varMap.get(objIdx);
                                }
                                else
                                {
                                    objectName = objIdx;
                                }
                                invoke.setObjectName(objectName);
                                invoke.setParameterNames(paramNames);

                                int commaInd = objectIdxEnd;
                                String callmethod;
                                if (commaInd >= 0)
                                {
                                    callmethod = line.substring(commaInd + 2, line.length()).trim();
                                    String[] methodItems = callmethod.split(METHODITEMS_SEPARATOR1);
                                    invoke.setClassCalled(getType(methodItems[0]));
                                    if (methodItems.length <= 1)
                                    {
                                        //try to use -> only
                                        methodItems = callmethod.split(METHODITEMS_SEPARATOR2);
                                    }
                                    extractInvoke(invoke, methodItems);
                                    invoke.setLine(lineInfo);
                                    invoke.setSourceFileFullPath(model.getSourceFileFullPath());
                                }

                                method.getInstructions().add(invoke);
                            }
                            else if (line.trim().startsWith(CONST))
                            {
                                String[] split = line.trim().split(" ");
                                Constant c = new Constant();
                                c.setType(split[0]);
                                if (split.length > 2)
                                {
                                    c.setValue(split[2]);
                                }
                                method.getInstructions().add(c);
                                c.setSourceFileFullPath(model.getSourceFileFullPath());
                            }
                            else if (line.trim().startsWith(LINE))
                            {
                                StringTokenizer token = new StringTokenizer(line);
                                if (token.hasMoreTokens())
                                {
                                    token.nextToken();
                                }
                                if (token.hasMoreTokens())
                                {
                                    try
                                    {
                                        lineInfo = Integer.parseInt(token.nextToken());
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        PreflightingLogger
                                                .error("Could not get line Number for line " + line);
                                    }
                                }
                            }
                            else if (line.trim().startsWith(".local ")) //Variable declaration
                            {
                                String[] split = line.trim().split(", ");
                                String varMapIdx = split[0];
                                varMapIdx = varMapIdx.replace(".local ", "");

                                String[] varNameType = split[1].split(COLON);
                                String varName = varNameType[0].trim();
                                String varType = varNameType[1];
                                varType = getType(varType);

                                Variable variable = new Variable();
                                variable.setName(varName);
                                variable.setType(varType);
                                variable.setLineNumber(lineInfo);
                                varMap.put(varMapIdx, varName);
                                method.addVariable(variable);
                                List<Instruction> instructions = method.getInstructions();
                                for (Instruction instruction : instructions)
                                {
                                    if (instruction instanceof Invoke)
                                    {
                                        Invoke inv = (Invoke) instruction;
                                        String invokeReturnVar = inv.getAssignedVariable();
                                        if ((invokeReturnVar != null)
                                                && invokeReturnVar.equals(varMapIdx))
                                        {
                                            inv.setAssignedVariable(varName);
                                        }
                                        String objectName = inv.getObjectName();
                                        if ((objectName != null) && objectName.equals(varMapIdx))
                                        {
                                            inv.setObjectName(varName);
                                        }
                                    }
                                }
                            }
                            else if (line.trim().startsWith("move-result-object"))
                            {
                                String resultVarIdx =
                                        line.trim().substring(line.trim().lastIndexOf(" ") + 1);
                                String varName =
                                        varMap.containsKey(resultVarIdx) ? varMap.get(resultVarIdx)
                                                : resultVarIdx;
                                if (invoke != null)
                                {
                                    invoke.setAssignedVariable(varName);
                                }
                            }

                            line = scanner.nextLine();
                        }

                        if (readingDirectMethods)
                        {
                            model.getDirectMethods().add(method);
                        }
                        else if (readingVirtualMethods)
                        {
                            model.getVirtualMethods().add(method);
                        }
                    }
                    // TODO : Here one must fetch the correct inner class name
                    else if (line.startsWith(ANNOTATION) && line.contains(INNER_CLASS))
                    {
                        model.setInnerClass(true);
                    }
                }
            }
        }
        finally
        {
            if (fileInputStream != null)
            {
                try
                {
                    fileInputStream.close();
                }
                catch (IOException e)
                {
                    //DO nothing.
                }
            }
            scanner.close();
        }
        return model;
    }

    private static String getPkg(String classFullPath)
    {
        int dotLastIndex = classFullPath.lastIndexOf('.');
        String pkg = "";
        if (dotLastIndex > 0)
        {
            pkg = classFullPath.substring(0, dotLastIndex);
        }
        return pkg;
    }

    /** 
     * @param params list of parameters
     * @return list of param types   
     */
    private static List<String> getParamTypes(String params)
    {
        StringBuilder replacementStr = new StringBuilder();
        List<String> paramTypes = new ArrayList<String>();
        for (int i = 0; i < params.length(); i++)
        {
            String c = "" + params.charAt(i);
            if (c.equals(ARRAYTYPE_PREFIX))
            {
                replacementStr.append(c);
            }
            else if (c.equals(TYPE_PREFIX_L))
            {
                replacementStr.append(c);
                do
                {
                    i++;
                    c = "" + params.charAt(i);
                    replacementStr.append(c);
                }
                while (!c.equals(PARAM_SEPARATOR));
                replacementStr.append(PARAM_SEPARATOR);
            }
            else if (smaliTypeMap.containsKey(c))
            {
                //it is a simple type                
                replacementStr.append(c + PARAM_SEPARATOR);
            }
            //PARAM_SEPARATOR chars are not expected to be logged
            else if (!c.equals(PARAM_SEPARATOR))
            {
                PreflightingLogger
                        .error("Chars not recognized. " + c + ". Check params: " + params);
            }
        }
        StringTokenizer tokenizer = new StringTokenizer(replacementStr.toString(), PARAM_SEPARATOR);
        while (tokenizer.hasMoreTokens())
        {
            String type = tokenizer.nextToken();
            paramTypes.add(getType(type));
        }
        return paramTypes;
    }

    /** 
     * @param typeKey
     * @return if it is an array appends to the type [], otherwise returns the type given for the source code  
     */
    private static String getType(String typeKey)
    {
        String type;
        boolean isArray = false;
        if (typeKey.startsWith(ARRAYTYPE_PREFIX))
        {
            isArray = true;
            typeKey = typeKey.substring(1);
        }
        if (typeKey.startsWith(TYPE_PREFIX_L))
        {
            type = typeKey.replaceAll("/", ".");
            type = type.replaceAll(PARAM_SEPARATOR, "");
            type =
                    type.contains("$") ? type.substring(1, type.lastIndexOf('$')) : type
                            .substring(1);
        }
        else
        {
            type = smaliTypeMap.get(typeKey);
        }
        if (isArray)
        {
            type += "[]";
        }
        if (type == null)
        {
            PreflightingLogger.error("Type not recognized. Check statement: " + typeKey);
        }
        return type;
    }

    /**
     * Extract information about invocation (the part after -> or ;->)  
     * @param invoke
     * @param methodItems
     */
    private static void extractInvoke(Invoke invoke, String[] methodItems)
    {
        int lparenInd = methodItems[1].indexOf(LPAREN);
        int rparenInd = methodItems[1].indexOf(RPAREN);
        String metName = methodItems[1].substring(0, lparenInd);
        invoke.setMethodName(metName);
        String param = methodItems[1].substring(lparenInd + 1, rparenInd);
        if (!param.equals(""))
        {
            String[] params = param.split(PARAMETERS_SEPARATOR);
            invoke.setParameterTypes(Arrays.asList(params));
        }
        String retType = methodItems[1].substring(rparenInd + 1);
        invoke.setReturnType(getType(retType));
    }

    /**
     * Runs apktool to extract java and resource files
     * @param apk
     * @param outputDir
     * @throws PreflightingToolException 
     */
    public static void extractFilesFromApk(File apk, File tempProjectFolder)
            throws PreflightingToolException
    {
        Process proc = null;
        try
        {
            Bundle bundle = PreflightingCorePlugin.getContext().getBundle();
            URL apktoolURL = bundle.getEntry(APKTOOL_JAR_PATH);
            apktoolURL = FileLocator.toFileURL(apktoolURL);
            String path = apktoolURL.getPath();

            if (Platform.getOS().equals(Platform.OS_WIN32) && path.startsWith("/"))
            {
                path = path.substring(1);
            }
            String[] args =
                    new String[]
                    {
                            "java", "-Xmx512m", "-jar", path, "d", "-f", apk.getAbsolutePath(),
                            tempProjectFolder.getAbsolutePath()
                    };

            proc = Runtime.getRuntime().exec(args);
        }
        catch (Exception e)
        {
            PreflightingLogger.error(ProjectUtils.class,
                    PreflightingCoreNLS.ProjectUtils_ErrorExecutingApkTool, e); //$NON-NLS-1$
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ProjectUtils_ErrorExecutingApkTool);
        }

        try
        {
            //verify if APK is not empty (otherwise command will not return)
            if (isApkValid(apk))
            {
                //trying to extract jar - if fail, jar is probably corrupted 
                File aux = File.createTempFile("apktemp", "folder");
                aux.getParentFile().mkdirs();
                File temp = new File(aux.getParentFile(), "apks");
                temp.mkdir();
                boolean fileOk = unpackZipFile(apk, temp.getAbsolutePath());
                if (fileOk && (proc != null) && (proc.waitFor() != 0))
                {
                    throw new PreflightingToolException(
                            PreflightingCoreNLS.ProjectUtils_ErrorExecutingApkTool);
                }
                else if (!fileOk)
                {
                    //apk corrupted
                    throw new PreflightingToolException(NLS.bind(
                            PreflightingCoreNLS.ApkUtils_ZipExtractionFile, apk.getAbsolutePath()));
                }
                deleteDirRecursively(temp);
            }
            else
            {
                //invalid apk
                throw new PreflightingToolException(NLS.bind(
                        PreflightingCoreNLS.ApkUtils_ZipExtractionFile, apk.getAbsolutePath()));
            }
        }
        catch (InterruptedException e)
        {
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ProjectUtils_ErrorExecutingApkTool, e);
        }
        catch (IOException e)
        {
            throw new PreflightingToolException(NLS.bind(
                    PreflightingCoreNLS.ApkUtils_ZipExtractionFile, apk.getAbsolutePath()));
        }
    }

    private static boolean isApkValid(File apk)
    {
        return (apk != null)
                && (apk.length() > 0)
                && (apk.getName().toLowerCase().endsWith("apk") || apk.getName().toLowerCase()
                        .endsWith("zip"));
    }

    private static void visitFolderToIdentifySmalis(File basefile, Set<File> classesFiles)
    {
        if ((basefile != null) && basefile.isDirectory())
        {
            File[] subfolders = basefile.listFiles();
            for (File file : subfolders)
            {
                visitFolderToIdentifySmalis(file, classesFiles);
            }
        }
        else if ((basefile != null) && basefile.isFile())
        {
            if (basefile.getName().endsWith(SMALI))
            {
                classesFiles.add(basefile);
            }
        }
    }

    private static Set<File> visitFolderToIdentifySmalis(File rootFolder)
    {
        Set<File> classesFiles = new HashSet<File>();
        visitFolderToIdentifySmalis(rootFolder, classesFiles);
        return classesFiles;
    }

    private static void visitFolderToIdentifyApks(File basefile, Set<File> apkFiles)
    {
        if ((basefile != null) && basefile.isDirectory())
        {
            File[] subfolders = basefile.listFiles();
            for (File file : subfolders)
            {
                visitFolderToIdentifyApks(file, apkFiles);
            }
        }
        else if ((basefile != null) && basefile.isFile())
        {
            if (basefile.getName().endsWith(APK))
            {
                apkFiles.add(basefile);
            }
        }
    }

    private static Set<File> visitFolderToIdentifyApks(File rootFolder)
    {
        Set<File> classesFiles = new HashSet<File>();
        visitFolderToIdentifyApks(rootFolder, classesFiles);
        return classesFiles;
    }

    /**
     * Unpack a zip file.
     * 
     * @param file the file
     * @param destination the destination path or null to unpack at the same directory of file
     * @return true if unpacked, false otherwise
     */
    private static boolean unpackZipFile(File file, String destination)
            throws PreflightingToolException
    {
        ZipFile zipFile = null;
        String extractDestination = destination != null ? destination : file.getParent();
        if (!extractDestination.endsWith(File.separator))
        {
            extractDestination += File.separator;
        }

        boolean unziped = true;
        try
        {
            zipFile = new ZipFile(file);
        }
        catch (Throwable e)
        {
            unziped = false;
        }
        if (zipFile != null)
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            InputStream input = null;
            OutputStream output = null;
            while (entries.hasMoreElements())
            {
                try
                {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith(".."))
                    {
                        throw new PreflightingToolException(
                                PreflightingCoreNLS.ApkToolUtils_MalformedAPK);
                    }

                    File newFile = new File(extractDestination + name);
                    if (entry.isDirectory())
                    {
                        newFile.mkdirs();
                    }
                    else
                    {
                        newFile.getParentFile().mkdirs();
                        if (newFile.createNewFile())
                        {
                            input = zipFile.getInputStream(entry);
                            output = new BufferedOutputStream(new FileOutputStream(newFile));
                            copyStreams(input, output);
                        }
                    }
                }
                catch (PreflightingToolException pe)
                {
                    unziped = false;
                    throw pe;
                }
                catch (Throwable t)
                {
                    unziped = false;
                }
                finally
                {
                    try
                    {
                        if (input != null)
                        {
                            input.close();
                        }
                        if (output != null)
                        {
                            output.close();
                        }
                    }
                    catch (Throwable t)
                    {
                        //do nothing
                    }
                }
            }
        }
        return unziped;
    }

    /**
     * Copy the input stream to the output stream
     * @param inputStream
     * @param outputStream
     * @throws IOException
     */
    private static void copyStreams(InputStream inputStream, OutputStream outputStream)
            throws IOException
    {
        byte[] buffer = new byte[1024];
        int length;

        while ((length = inputStream.read(buffer)) >= 0)
        {
            outputStream.write(buffer, 0, length);
        }
    }

    /**
     * This method deletes the directory, all files and all subdirectories under
     * it. If a deletion fails, the method stops attempting to delete and
     * returns false.
     *
     * @param directory
     *           The directory to be deleted
     * @return Returns true if all deletions were successful. If the directory
     *         doesn't exist returns false.
     * @throws IOException
     *            When the parameter isn't a directory
     */
    private static boolean deleteDirRecursively(File directory) throws IOException
    {
        String dirName = "";
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
                        success = success && element.delete();
                    }
                    else
                    {
                        success = success && deleteDirRecursively(element);
                    }
                }
                success = success && directory.delete();
            }
            else
            {
                String errorMessage = directory.getName() + " is not a diretory.";
                throw new IOException(errorMessage);
            }
        }
        else
        {
            String errorMessage = "The directory does not exist.";
            success = false;
            throw new IOException(errorMessage);
        }
        return success;
    }
}
