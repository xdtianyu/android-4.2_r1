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
package com.motorolamobility.preflighting.core.devicespecification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.motorolamobility.preflighting.core.devicelayoutspecification.Device;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

/**
 * Representation of the configuration of a device
 */
public final class DeviceSpecification
{
    //Some (but not all) of the keys to access the properties 
    public static String PROPERTY_TOUCH_SCREEN = "touchScreen"; //$NON-NLS-1$

    public static String PROPERTY_TRACKBALL = "hw.trackBall"; //$NON-NLS-1$

    public static String PROPERTY_KEYBOARD = "hw.keyboard"; //$NON-NLS-1$

    public static String PROPERTY_CAMERA = "hw.camera"; //$NON-NLS-1$

    public static String PROPERTY_GPS = "hw.gps"; //$NON-NLS-1$

    public static String PROPERTY_ACCELEROMETER = "hw.accelerometer"; //$NON-NLS-1$

    public static String PROPERTY_SDCARD = "hw.sdCard"; //$NON-NLS-1$

    public static String PROPERTY_NAME = "name"; //$NON-NLS-1$

    public static String PROPERTY_API_LEVEL = "api"; //$NON-NLS-1$

    private int deviceApiLevel = -1;

    private PlatformRules platformRules;

    //(merge hw.ini + manifest.ini)
    private Map<String, String> propertyNameToValue;

    /**
     * Data based on layout-devices.xsd
     */
    private Device deviceInfo;

    private static String COMMENT_SIGN = "#"; //$NON-NLS-1$

    private static String EQUALS_SIGN = "="; //$NON-NLS-1$

    /**
     * Reads an inputstream as a ini file and convert it to a Key-Value Map
     * @param input
     * @return a map that represents the file that was read.
     * @throws IOException
     */
    private static Map<String, String> iniFileToMap(InputStream input) throws IOException
    {
        Map<String, String> result = new HashMap<String, String>();
        String line;
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(input, "UTF-8")); //$NON-NLS-1$
            while ((line = reader.readLine()) != null)
            {
                // parse the line, ignoring comments (#) and empty lines, mount the map
                if ((!(line == null) && !line.trim().startsWith(COMMENT_SIGN))
                        && !line.trim().equals("")) //$NON-NLS-1$
                {
                    int equals = line.indexOf(EQUALS_SIGN);
                    if (equals > -1)
                    {
                        String key = line.substring(0, equals).trim();
                        String value = line.substring(equals + 1, line.length()).trim();

                        if (result.containsKey(key) && (!result.get(key).equals(value)))
                        {
                            PreflightingLogger.warn(DeviceSpecification.class,
                                    "Wrong device specification file. Duplicated key: " + key); //$NON-NLS-1$
                        }
                        result.put(key, value);
                    }
                    else
                    {
                        PreflightingLogger.warn(DeviceSpecification.class,
                                "Wrong device specification file. Line does not contain key-values:" //$NON-NLS-1$
                                        + line);
                    }
                }
            }
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (Exception e)
                {
                    //Do Nothing
                }
            }
            input.close();
        }

        return result;
    }

    /**
     * @param apiLevel the API level from the device being represented
     * @param deviceInfo {@link Device}
     */
    public DeviceSpecification(int apiLevel, Device deviceInfo)
    {
        this.deviceInfo = deviceInfo;
        this.deviceApiLevel = apiLevel;
        platformRules = PlatformRules.getInstance();
        if (!platformRules.isApiLevelSupported(deviceApiLevel))
        {
            PreflightingLogger.warn(DeviceSpecification.class, "API Level for device " + getName() //$NON-NLS-1$
                    + " not supported: " + deviceApiLevel); //$NON-NLS-1$
        }
    }

    /**
     * Creates a new Device Specification object
     *  
     * @param hardwareIni Stream containing the hardware.ini file for this device
     * @param manifestIni Stream containing the manifest.ini file for this device
     */
    public DeviceSpecification(InputStream hardwareIni, InputStream manifestIni)
    {

        platformRules = PlatformRules.getInstance();
        propertyNameToValue = new HashMap<String, String>();

        if ((hardwareIni == null) || (manifestIni == null))
        {
            PreflightingLogger.warn(DeviceSpecification.class,
                    "Wrong call to the constructor: null input stream"); //$NON-NLS-1$
            return;
        }

        try
        {
            propertyNameToValue.putAll(iniFileToMap(hardwareIni));
            propertyNameToValue.putAll(iniFileToMap(manifestIni));
        }
        catch (IOException e)
        {
            PreflightingLogger.error(DeviceSpecification.class,
                    "Error loading properties for device.", e); //$NON-NLS-1$
        }

        this.deviceApiLevel = Integer.parseInt(propertyNameToValue.get(PROPERTY_API_LEVEL));

        if (!platformRules.isApiLevelSupported(deviceApiLevel))
        {
            PreflightingLogger.warn(DeviceSpecification.class, "API Level for device " + getName() //$NON-NLS-1$
                    + " not supported: " + deviceApiLevel); //$NON-NLS-1$
        }
    }

    /**
     * Gets the API Level for this device.
     * @return the API level for this device.
     */
    public int getAPILevel()
    {
        return deviceApiLevel;
    }

    /**
     * Gets the property "name" for this device
     * @return the property "name" for this device.
     */
    public String getName()
    {
        return (deviceInfo != null) ? deviceInfo.getName() : "";
    }

    /**
     * Gets a Properties object containing
     * the specification for the device
     * @return
     */
    public Properties getSpecs()
    {
        Properties p = new Properties();
        p.putAll(propertyNameToValue);
        return p;
    }

    /**
     * Returns the device representation.
     * @return the device representation.
     */
    public Device getDeviceInfo()
    {
        return deviceInfo;
    }

    /**
     * The device id.
     * @return the device id.
     */
    public String getId()
    {

        return (deviceInfo != null) ? deviceInfo.getId() : "";
    }

    /**
     * The device provider.
     * @return the device provider.
     */
    public String getProvider()
    {
        return (deviceInfo != null) ? deviceInfo.getProvider() : "";
    }
}
