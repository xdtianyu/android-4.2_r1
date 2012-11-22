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

package com.android.builder.packaging;


import java.io.File;
import java.io.IOException;

public class JavaResourceProcessor {

    private final IArchiveBuilder mBuilder;

    public interface IArchiveBuilder {

        /**
         * Adds a file to the archive at a given path
         * @param file the file to add
         * @param archivePath the path of the file inside the APK archive.
         * @throws PackagerException if an error occurred
         * @throws SealedPackageException if the archive is already sealed.
         * @throws DuplicateFileException if a file conflicts with another already added to the APK
         *                                   at the same location inside the APK archive.
         */
        void addFile(File file, String archivePath) throws PackagerException,
                SealedPackageException, DuplicateFileException;
    }


    public JavaResourceProcessor(IArchiveBuilder builder) {
        mBuilder = builder;
    }

    /**
     * Adds the resources from a source folder to a given {@link IArchiveBuilder}
     * @param sourceLocation the source folder.
     * @throws PackagerException if an error occurred
     * @throws SealedPackageException if the APK is already sealed.
     * @throws DuplicateFileException if a file conflicts with another already added to the APK
     *                                   at the same location inside the APK archive.
     */
    public void addSourceFolder(String sourceLocation)
            throws PackagerException, DuplicateFileException, SealedPackageException {
        File sourceFolder = new File(sourceLocation);
        if (sourceFolder.isDirectory()) {
            try {
                // file is a directory, process its content.
                File[] files = sourceFolder.listFiles();
                for (File file : files) {
                    processFileForResource(file, null);
                }
            } catch (DuplicateFileException e) {
                throw e;
            } catch (SealedPackageException e) {
                throw e;
            } catch (Exception e) {
                throw new PackagerException(e, "Failed to add %s", sourceFolder);
            }
        } else {
            // not a directory? check if it's a file or doesn't exist
            if (sourceFolder.exists()) {
                throw new PackagerException("%s is not a folder", sourceFolder);
            } else {
                throw new PackagerException("%s does not exist", sourceFolder);
            }
        }
    }


    /**
     * Processes a {@link File} that could be an APK {@link File}, or a folder containing
     * java resources.
     *
     * @param file the {@link File} to process.
     * @param path the relative path of this file to the source folder.
     *          Can be <code>null</code> to identify a root file.
     * @throws IOException
     * @throws DuplicateFileException if a file conflicts with another already added
     *          to the APK at the same location inside the APK archive.
     * @throws PackagerException if an error occurred
     * @throws SealedPackageException if the APK is already sealed.
     */
    private void processFileForResource(File file, String path)
            throws IOException, DuplicateFileException, PackagerException, SealedPackageException {
        if (file.isDirectory()) {
            // a directory? we check it
            if (checkFolderForPackaging(file.getName())) {
                // if it's valid, we append its name to the current path.
                if (path == null) {
                    path = file.getName();
                } else {
                    path = path + "/" + file.getName();
                }

                // and process its content.
                File[] files = file.listFiles();
                for (File contentFile : files) {
                    processFileForResource(contentFile, path);
                }
            }
        } else {
            // a file? we check it to make sure it should be added
            if (checkFileForPackaging(file.getName())) {
                // we append its name to the current path
                if (path == null) {
                    path = file.getName();
                } else {
                    path = path + "/" + file.getName();
                }

                // and add it to the apk
                mBuilder.addFile(file, path);
            }
        }
    }

    /**
     * Checks whether a folder and its content is valid for packaging into the .apk as
     * standard Java resource.
     * @param folderName the name of the folder.
     */
    public static boolean checkFolderForPackaging(String folderName) {
        return folderName.equalsIgnoreCase("CVS") == false &&
            folderName.equalsIgnoreCase(".svn") == false &&
            folderName.equalsIgnoreCase("SCCS") == false &&
            folderName.equalsIgnoreCase("META-INF") == false &&
            folderName.startsWith("_") == false;
    }

    /**
     * Checks a file to make sure it should be packaged as standard resources.
     * @param fileName the name of the file (including extension)
     * @return true if the file should be packaged as standard java resources.
     */
    public static boolean checkFileForPackaging(String fileName) {
        String[] fileSegments = fileName.split("\\.");
        String fileExt = "";
        if (fileSegments.length > 1) {
            fileExt = fileSegments[fileSegments.length-1];
        }

        return checkFileForPackaging(fileName, fileExt);
    }

    /**
     * Checks a file to make sure it should be packaged as standard resources.
     * @param fileName the name of the file (including extension)
     * @param extension the extension of the file (excluding '.')
     * @return true if the file should be packaged as standard java resources.
     */
    public static boolean checkFileForPackaging(String fileName, String extension) {
        // ignore hidden files and backup files
        if (fileName.charAt(0) == '.' || fileName.charAt(fileName.length()-1) == '~') {
            return false;
        }

        return "aidl".equalsIgnoreCase(extension) == false &&       // Aidl files
            "rs".equalsIgnoreCase(extension) == false &&            // RenderScript files
            "rsh".equalsIgnoreCase(extension) == false &&           // RenderScript header files
            "d".equalsIgnoreCase(extension) == false &&             // Dependency files
            "java".equalsIgnoreCase(extension) == false &&          // Java files
            "scala".equalsIgnoreCase(extension) == false &&         // Scala files
            "class".equalsIgnoreCase(extension) == false &&         // Java class files
            "scc".equalsIgnoreCase(extension) == false &&           // VisualSourceSafe
            "swp".equalsIgnoreCase(extension) == false &&           // vi swap file
            "thumbs.db".equalsIgnoreCase(fileName) == false &&      // image index file
            "picasa.ini".equalsIgnoreCase(fileName) == false &&     // image index file
            "package.html".equalsIgnoreCase(fileName) == false &&   // Javadoc
            "overview.html".equalsIgnoreCase(fileName) == false;    // Javadoc
    }


}
