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
package com.motorolamobility.preflighting.core.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

/**
 * This class has methods which aids the use of the Android SDK. 
 */
public class SdkUtils
{
    private static final boolean isWin32 = Platform.getOS().equals(Platform.OS_WIN32);

    /**
     * Folder "platforms" in the Android SDK
     */
    public static final String FD_PLATFORMS = "platforms"; //$NON-NLS-1$

    /**
     * Folder "tools" in the Android SDK
     */
    public static final String FD_TOOLS = "tools"; //$NON-NLS-1$

    private static String FD_PLATFORM_TOOLS = "platform-tools";;

    /**
     * AAPT executable command.
     */
    public static String AAPT_EXE = isWin32 ? "aapt.exe" : "aapt"; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String SOURCE_PROPERTY_FILE = "source.properties"; //$NON-NLS-1$

    private static final String API_LEVEL_KEY = "ro.build.version.sdk"; //$NON-NLS-1$

    private static final String BUILD_PROP_FILE = "build.prop"; //$NON-NLS-1$

    /**
     * Android version, API Level tag in AndroidManifext.xml file.
     */
    public final static String APILEVEL = "AndroidVersion.ApiLevel"; //$NON-NLS-1$

    /**
     * Get the AAPT path of the higher API Level available given the sdk path.
     * 
     * @param sdkFolder SDK folder.
     * 
     * @return Returns the AAPT. absolute path, null if path not found. 
     */
    public static String getLatestAAPTToolPath(String sdkFolder)
    {
        Integer maxApiLevel = 0;
        File newestAaptTool = null;

        File platformFolder = new File(sdkFolder, FD_PLATFORMS);
        File platformToolsFolder = new File(sdkFolder, FD_PLATFORM_TOOLS);

        //Verify if it's a tools_r8 sdk (appt tool will be located under the platform-tools folder). 
        if (platformToolsFolder.exists() && platformToolsFolder.isDirectory())
        {
            File aaptToolFile = new File(platformToolsFolder, AAPT_EXE);
            if (aaptToolFile.exists())
            {
                newestAaptTool = aaptToolFile;
            }
        }
        else
        // it's an older SDK, try to find the aapt tool inside the platform folders.
        {
            //platform folder
            if (platformFolder.exists() && platformFolder.isDirectory())
            {
                File[] targets = platformFolder.listFiles();

                if (targets != null)
                {
                    for (File target : targets)
                    {
                        if (target.isDirectory())
                        {
                            //look forward source.properties file
                            File propertiesFile = new File(target, SOURCE_PROPERTY_FILE);

                            if (propertiesFile.exists())
                            {
                                //load properties
                                Properties properties = getPropertiesFromTarget(propertiesFile);

                                String apilevel = properties.getProperty(APILEVEL, "0"); //$NON-NLS-1$

                                try
                                {
                                    //get api level and store aapt tool path if latest
                                    Integer intApiLevel = Integer.parseInt(apilevel);
                                    if (intApiLevel > maxApiLevel)
                                    {
                                        File aaptToolFile =
                                                new File(target, FD_TOOLS
                                                        + System.getProperty("file.separator")
                                                        + AAPT_EXE);
                                        if (aaptToolFile.exists())
                                        {
                                            newestAaptTool = aaptToolFile;
                                            maxApiLevel = intApiLevel;
                                        }
                                    }
                                }
                                catch (NumberFormatException e)
                                {
                                    // Do nothing
                                }
                            }
                            else
                            {
                                //if source.properties does not exist (jil and ophone cases) get build.prop file
                                propertiesFile = new File(target, BUILD_PROP_FILE);

                                if (propertiesFile.exists())
                                {
                                    //load properties
                                    Properties properties = getPropertiesFromTarget(propertiesFile);

                                    if ((properties != null)
                                            && properties.containsKey(API_LEVEL_KEY))
                                    {
                                        String apilevel = properties.getProperty(API_LEVEL_KEY);

                                        try
                                        {
                                            //store latest aapt tool path
                                            Integer intApiLevel = Integer.parseInt(apilevel);
                                            if (intApiLevel > maxApiLevel)
                                            {
                                                File aaptToolFile =
                                                        new File(
                                                                target,
                                                                FD_TOOLS
                                                                        + System.getProperty("file.separator")
                                                                        + AAPT_EXE);
                                                if (aaptToolFile.exists())
                                                {
                                                    newestAaptTool = aaptToolFile;
                                                    maxApiLevel = intApiLevel;
                                                }
                                            }
                                        }
                                        catch (NumberFormatException e)
                                        {
                                            // Do nothing
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return newestAaptTool != null ? newestAaptTool.getAbsolutePath() : null;
    }

    /**
     * Retrieves the properties from a SDK target.
     * 
     * @param propertiesFile The properties file.
     * 
     * @return Returns a {@link Properties} object containing the properties from a SDK or null
     * if the file could not be read.
     */
    private static Properties getPropertiesFromTarget(File propertiesFile)
    {
        Properties properties = new Properties();

        FileInputStream fis = null;

        try
        {
            fis = new FileInputStream(propertiesFile);
            properties.load(fis);
        }
        catch (FileNotFoundException e)
        {
            // Do nothing. If the file has problems, there is no way to detect
            // the target version
        }
        catch (IOException e)
        {
            // Do nothing. If the file has problems, there is no way to detect
            // the target version
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException e)
                {
                    // Do nothing
                }
            }
        }

        return properties;
    }
}
