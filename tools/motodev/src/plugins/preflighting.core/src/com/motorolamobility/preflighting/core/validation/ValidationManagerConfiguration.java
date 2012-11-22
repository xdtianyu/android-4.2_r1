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
package com.motorolamobility.preflighting.core.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

/**
 * Singleton that encapsulates the data in a file that configures App Validator execution.
 */
public class ValidationManagerConfiguration
{

    private final static String APP_VALIDATOR_CONFIG_FILE_NAME = "appvalidator.cfg"; //$NON-NLS-1$

    private static ValidationManagerConfiguration instance;

    /**
     * @return singleton instance of {@link ValidationManagerConfiguration}
     */
    public static ValidationManagerConfiguration getInstance()
    {
        if (instance == null)
        {
            instance = new ValidationManagerConfiguration();
        }
        return instance;
    }

    /**
     * Properties that are available into {@link ValidationManagerConfiguration#APP_VALIDATOR_CONFIG_FILE_NAME}. 
     */
    public static enum ConfigProperties
    {
        BASE_URL_PROPERTY("base_url"), //$NON-NLS-1$

        URL_QUERY_PROPERTY("info_url_query"), //$NON-NLS-1$

        APK_EXTRACTION_MODE("apk_extraction_mode"); //$NON-NLS-1$

        private String name;

        private ConfigProperties(String name)
        {
            this.name = name;
        }

        /**
         * @return string with the name of the configuration property
         */
        public String getName()
        {
            return name;
        }
    }

    /**
     * Property that exposes the modes to analyze APK.
     */
    public static enum ExtractionModes
    {
        APKTOOL_MODE("apktool"), //$NON-NLS-1$

        AAPT_MODE("aapt"); //$NON-NLS-1$

        private String mode;

        private ExtractionModes(String mode)
        {
            this.mode = mode;
        }

        /**
         * @return mode the chosen tool (mode to analyze APK)  
         */
        public String getMode()
        {
            return mode;
        }

        /**
         * Checks if a mode is not contained in the list of {@link ExtractionModes} available
         * @param mode the string containing the mode (tool to analyze APK)
         * @return <code>true</code> if found, <code>false</code> otherwise
         */
        public static boolean contains(String mode)
        {
            boolean contains = false;
            if (mode != null)
            {
                for (ExtractionModes input : ExtractionModes.values())
                {
                    if (input.getMode().equals(mode))
                    {
                        contains = true;
                        break;
                    }
                }
            }

            return contains;
        }
    }

    private final static String DEFAULT_URL = "http://developer.motorola.com/"; //$NON-NLS-1$

    private final Properties p = new Properties();

    /**
     * Initializes map with app validator startup configuration
     */
    private ValidationManagerConfiguration()
    {
        String path = Platform.getInstallLocation().getURL().getPath();
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }
        path += APP_VALIDATOR_CONFIG_FILE_NAME;

        File f = new File(path);
        FileInputStream fis = null;
        try
        {
            if (f.exists() && f.isFile())
            {
                fis = new FileInputStream(f);
                p.load(fis);
            }
            if (getProperty(ConfigProperties.BASE_URL_PROPERTY.getName()) == null)
            {
                p.put(ConfigProperties.BASE_URL_PROPERTY.getName(), DEFAULT_URL);
            }
            if (getProperty(ConfigProperties.URL_QUERY_PROPERTY.getName()) == null)
            {
                p.put(ConfigProperties.URL_QUERY_PROPERTY.getName(), "");
            }
            if (!ExtractionModes.contains(getProperty(ConfigProperties.APK_EXTRACTION_MODE
                    .getName())))
            {
                p.put(ConfigProperties.APK_EXTRACTION_MODE.getName(),
                        ExtractionModes.APKTOOL_MODE.getMode());
            }
        }
        catch (IOException e)
        {
            //populate with default values
            p.put(ConfigProperties.BASE_URL_PROPERTY.getName(), DEFAULT_URL);
            p.put(ConfigProperties.URL_QUERY_PROPERTY.getName(), "");
            p.put(ConfigProperties.APK_EXTRACTION_MODE.getName(),
                    ExtractionModes.APKTOOL_MODE.getMode());
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
                    // do nothing
                }
            }
        }
    }

    /**
     * Get the selected property.
     * @param property string with one of the items available in {@link ConfigProperties}
     * @return the selected property or null if its not found.
     */
    public String getProperty(String property)
    {
        return p.getProperty(property);
    }
}
