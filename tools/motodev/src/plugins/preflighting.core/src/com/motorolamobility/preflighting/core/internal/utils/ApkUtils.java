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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.Path;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;

/**
 * This class holds methods which deal with APK package.
 */
public final class ApkUtils
{
    public static final String APP_VALIDATOR_TEMP_DIR = "MotodevAppValidator";

    private static final String JAVA_TEMP_DIR_PROPERTY = "java.io.tmpdir";

    private static final String TEMP_DIR_PATH = System.getProperty(JAVA_TEMP_DIR_PROPERTY);

    // Temp folder used for APK extracting
    public static final File tmpAppValidatorFolder =
            new File(TEMP_DIR_PATH, APP_VALIDATOR_TEMP_DIR);

    private static final String RSA = ".rsa";

    private static final String DSA = ".dsa";

    public static final String APK_EXTENSION = ".apk";

    public static final String ZIP_EXTENSION = ".zip";

    private static final int BUFFER_SIZE = 1024;

    /**
     * Give an APK file, a tree directories and files are extracted,
     * representing partially the project which generated the APK.
     * <p>
     * The files are created in a temporary directory.
     * 
     * @param apkFile
     *            APK file which the project tree will be extracted from.
     * @param sdkPath
     *            SDK path where the tools for extracting and interpreting the
     *            APK information will be used.
     * 
     * @return A file object holding a tree of directories and files which
     *         represent partially the project which generated the APK.
     * 
     * @throws PreflightingToolException
     *             Exception thrown when there are problems creating the files
     *             and directories structure.
     */
    public static File extractProjectFromAPK(File apkFile, String sdkPath)
            throws PreflightingToolException
    {
        String apkName = apkFile.getName();

        // Create a temp directory to contain all extracted packages, if needed
        if (!tmpAppValidatorFolder.exists())
        {

            try
            {
                tmpAppValidatorFolder.mkdir();
            }
            catch (SecurityException se)
            {
                PreflightingLogger.error(ApkUtils.class,
                        "It was not possible to extract the android package.", se); //$NON-NLS-1$
                throw new PreflightingToolException(
                        PreflightingCoreNLS.ApkUtils_ImpossibleExtractAndroidPackageMessage, se);
            }
        }

        File tmpProjectFile;
        try
        {
            tmpProjectFile = File.createTempFile(apkName, null, tmpAppValidatorFolder);
            tmpProjectFile.delete();
            tmpProjectFile.mkdir();
        }
        catch (IOException ioException)
        {
            PreflightingLogger.error(ApkUtils.class,
                    "It was not possible to extract the android package.", ioException); //$NON-NLS-1$
            throw new PreflightingToolException(
                    PreflightingCoreNLS.ApkUtils_ImpossibleExtractAndroidPackageMessage,
                    ioException);
        }

        String extractionMode =
                ValidationManagerConfiguration.getInstance().getProperty(
                        ValidationManagerConfiguration.ConfigProperties.APK_EXTRACTION_MODE
                                .getName());
        if (extractionMode.equals(ValidationManagerConfiguration.ExtractionModes.APKTOOL_MODE
                .getMode()))
        {
            ApktoolUtils.extractFilesFromApk(apkFile, tmpProjectFile);
        }
        else
        {
            AaptUtils.extractFilesFromAPK(apkFile, sdkPath, tmpProjectFile);
        }

        return tmpProjectFile;
    }

    /**
     * Returns a handler to the temp directory used for extracting APKs.
     * 
     * @return A handler to the temp directory.
     */
    public static File getAppValidatorTempApkFolder()
    {
        return tmpAppValidatorFolder;
    }

    /**
     * Iterates over APK (jar entries) to populate
     * 
     * @param projectFile
     * @return
     * @throws IOException
     * @throws CertificateException
     */
    public static List<Certificate> populateCertificate(File projectFile) throws IOException,
            CertificateException
    {
        List<Certificate> certList = new ArrayList<Certificate>();
        JarFile jar = new JarFile(projectFile);
        Enumeration<JarEntry> jarEntries = jar.entries();
        while (jarEntries.hasMoreElements())
        {
            JarEntry entry = jarEntries.nextElement();
            if (entry.getName().toLowerCase().contains(DSA)
                    || entry.getName().toLowerCase().contains(RSA))
            {
                certList.addAll(extractCertificate(jar, entry));
            }
        }
        return certList;
    }

    /**
     * Extracts certificate from APK
     * 
     * @param jar
     * @param entry
     *            rsa or dsa jar item
     * @return
     * @throws IOException
     *             I/O problem to read jar
     * @throws CertificateException
     *             certificate has problems
     */
    private static List<Certificate> extractCertificate(JarFile jar, JarEntry entry)
            throws IOException, CertificateException
    {
        List<Certificate> certList = new ArrayList<Certificate>();
        InputStream inStream = null;
        try
        {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            inStream = jar.getInputStream(entry);
            Collection<? extends Certificate> c = cf.generateCertificates(inStream);
            Iterator<? extends Certificate> i = c.iterator();
            while (i.hasNext())
            {
                Certificate cert = i.next();
                certList.add(cert);
            }
        }
        finally
        {
            if (inStream != null)
            {
                inStream.close();
            }
        }
        return certList;
    }

    /**
     * Unzip zipFile and returns the directory created with its contents
     * @param zipFile
     * @return 
     * @throws PreflightingToolException
     */
    public static File unzip(File zipFile) throws PreflightingToolException
    {
        File tempExtractionDir = null;
        ZipInputStream apkInputStream = null;
        FileOutputStream apkOutputStream = null;

        try
        {
            //crate MOTODEV temp folder
            if (!tmpAppValidatorFolder.exists())
            {
                tmpAppValidatorFolder.mkdir();
            }
            //create extraction folder
            tempExtractionDir = File.createTempFile(zipFile.getName(), null, tmpAppValidatorFolder);
            tempExtractionDir.delete();
            tempExtractionDir.mkdir();

            //open zipFile stream
            apkInputStream = new ZipInputStream(new FileInputStream(zipFile.getAbsolutePath()));
            ZipEntry apkZipEntry = apkInputStream.getNextEntry();

            byte[] buf = new byte[BUFFER_SIZE];
            CRC32 crc = new CRC32();

            //while there are apks inside zip file
            while (apkZipEntry != null)
            {
                crc.reset();
                if (apkZipEntry.getName().endsWith(APK_EXTENSION))
                {
                    //file to be created (apk)
                    try
                    {
                        apkOutputStream =
                                new FileOutputStream(tempExtractionDir.getAbsolutePath()
                                        + Path.SEPARATOR + apkZipEntry.getName());

                        int length = 0;
                        //creates apk and updates CRC during the process
                        while ((length = apkInputStream.read(buf, 0, BUFFER_SIZE)) > -1)
                        {
                            apkOutputStream.write(buf, 0, length);
                            crc.update(buf, 0, length);
                        }

                        //test if extraction went fine
                        if (crc.getValue() != apkZipEntry.getCrc())
                        {
                            throw new PreflightingToolException(PreflightingCoreNLS.bind(
                                    PreflightingCoreNLS.ApkUtils_ZipExtractionFile,
                                    apkZipEntry.getName()));
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
                                // do nothing
                            }
                        }
                    }
                }
                apkZipEntry = apkInputStream.getNextEntry();
            }
        }
        catch (IOException ioe)
        {
            //error during extraction, abort validation
            throw new PreflightingToolException(PreflightingCoreNLS.ApkUtils_ZipExtraction, ioe);
        }
        finally
        {
            try
            {
                if (apkInputStream != null)
                {
                    apkInputStream.close();
                }
            }
            catch (IOException e)
            {
                // do nothing
            }
        }

        return tempExtractionDir;
    }
}