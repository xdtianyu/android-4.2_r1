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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.core.devicelayoutspecification.Device;
import com.motorolamobility.preflighting.core.devicelayoutspecification.ParametersType;

/**
 * This class is responsible to keep Device Specifications.
 * When a new Device Specification is added it is also added to a internal map that tracks specs vs devices
 * The map uses SpecKey value as key, representing all specs, Each entry has a list of {@link DeviceSpecification}
 * containing all devices with that spec. 
 */
public class DevicesSpecsContainer
{

    /* @formatter:off */
    /**
     * Enumerator for the specifications and their variations.
     */
    public static enum SpecKey
    {
        screenSize_Small("screenSize_Small"), screenSize_Normal("screenSize_Normal"),
        screenSize_Large("screenSize_Large"), screenSize_XLarge("screenSize_XLarge"),

        screenRatio_NotLong("screenRatio_NotLong"), screenRatio_Long("screenRatio_Long"),

        screenOrientation_Square("screenOrientation_Square"), screenOrientation_Port(
                "screenOrientation_Port"), screenOrientation_Land("screenOrientation_Land"),

        pixelDensity_Low("pixelDensity_Low"), pixelDensity_Medium("pixelDensity_Medium"),
        pixelDensity_High("pixelDensity_High"), pixelDensity_XHigh("pixelDensity_XHigh"),

        touchType_NoTouch("touchType_NoTouch"), touchType_Stylus("touchType_Stylus"),
        touchType_Finger("touchType_Finger"),

        textIME_NoKeys("textIME_NoKeys"), textIME_Qwerty("textIME_Qwerty"), textIME_TwelveKey(
                "textIME_TwelveKey"),

        KbState_KeysSoft("KbState_KeysSoft"), KbState_KeysExposed("KbState_KeysExposed"),
        KbState_KeysHidden("KbState_KeysHidden"),

        navMethod_NoNav("navMethod_NoNav"), navMethod_DPad("navMethod_DPad"), navMethod_TrackBall(
                "navMethod_TrackBall"), navMethod_Wheel("navMethod_Wheel");

        private String id;

        private SpecKey(String id)
        {
            this.id = id;
        }

        /**
         * @return the alias
         */
        public String getId()
        {
            return id;
        }

        /**
         * @param alias
         * @return true if the value of alias is recognized as a valid
         *         InputParameter. Return false if alias is null.
         */

        public static boolean contains(String id)
        {
            boolean contains = false;

            if (id != null)
            {
                for (SpecKey key : SpecKey.values())
                {
                    if (key.getId().equals(id))
                    {
                        contains = true;
                        break;
                    }
                }
            }

            return contains;
        }
    }

    /* @formatter:on */

    private final List<DeviceSpecification> deviceSpecifications;

    private final Map<SpecKey, List<DeviceSpecification>> specDevFilterMap;

    private static DevicesSpecsContainer instance;

    private DevicesSpecsContainer()
    {
        specDevFilterMap = new HashMap<SpecKey, List<DeviceSpecification>>(SpecKey.values().length);
        deviceSpecifications = new ArrayList<DeviceSpecification>(50);
    }

    /**
     * An instance of this class. This class implements the singleton pattern.
     * @return the instance.
     */
    public static DevicesSpecsContainer getInstance()
    {
        if (instance == null)
        {
            instance = new DevicesSpecsContainer();
        }
        return instance;
    }

    /**
     * Clear internal structures that are cached.
     */
    public void clear()
    {
        specDevFilterMap.clear();
        deviceSpecifications.clear();
    }

    /**
     * @return the specDeviceMap
     */
    public Map<SpecKey, List<DeviceSpecification>> getSpecDevFilterMap()
    {
        return specDevFilterMap;
    }

    /**
     * Returns the value for a given specification.
     * @param specKey the key that represents a specification.
     * @return the value for that specification.
     */
    public List<DeviceSpecification> getDeviceSpecifications(SpecKey specKey)
    {
        return specDevFilterMap.get(specKey);
    }

    /**
     * Returns the list of device specifications.
     * @return the list of device specifications.
     */
    public List<DeviceSpecification> getDeviceSpecifications()
    {
        return deviceSpecifications;
    }

    /**
     * Adds device specifications.
     * @param deviceSpec the specifications to be added.
     */
    public void addDeviceSpecification(DeviceSpecification deviceSpec)
    {
        if (!deviceSpecifications.contains(deviceSpec))
        {
            deviceSpecifications.add(deviceSpec);
        }

        Device deviceInfo = deviceSpec.getDeviceInfo();
        ParametersType defaultSpecs = deviceInfo.getDefault();
        List<SpecKey> keysToAdd = new ArrayList<SpecKey>();

        keysToAdd.add(getScreenSizeKey(defaultSpecs.getScreenSize(), deviceSpec));
        keysToAdd.add(getScreenRatioKey(defaultSpecs.getScreenRatio(), deviceSpec));
        keysToAdd.add(getScreenOrientationKey(defaultSpecs.getScreenOrientation(), deviceSpec));
        keysToAdd.add(getPixelDensityKey(defaultSpecs.getPixelDensity(), deviceSpec));
        keysToAdd.add(getTouchTypeKey(defaultSpecs.getTouchType(), deviceSpec));
        keysToAdd.add(getTextInputMethodKey(defaultSpecs.getTextInputMethod(), deviceSpec));
        keysToAdd.add(getKeyboardStateKey(defaultSpecs.getKeyboardState(), deviceSpec));
        keysToAdd.add(getNavMethodKey(defaultSpecs.getNavMethod(), deviceSpec));

        for (SpecKey key : keysToAdd)
        {
            addToMap(key, deviceSpec);
        }

    }

    private void addToMap(SpecKey key, DeviceSpecification deviceSpec)
    {
        if (key != null)
        {
            List<DeviceSpecification> devicesSpecs = specDevFilterMap.get(key);
            if (devicesSpecs == null)
            {
                devicesSpecs = new ArrayList<DeviceSpecification>();
            }
            devicesSpecs.add(deviceSpec);
            specDevFilterMap.put(key, devicesSpecs);
        }
    }

    private SpecKey getScreenSizeKey(String screenSize, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;
        if (screenSize != null)
        {
            if (screenSize.equalsIgnoreCase("small"))
            {
                key = SpecKey.screenSize_Small;
            }
            else if (screenSize.equalsIgnoreCase("normal"))
            {
                key = SpecKey.screenSize_Normal;
            }
            else if (screenSize.equalsIgnoreCase("large"))
            {
                key = SpecKey.screenSize_Large;
            }
            else if (screenSize.equalsIgnoreCase("xlarge"))
            {
                key = SpecKey.screenSize_XLarge;
            }
        }

        return key;
    }

    private SpecKey getScreenRatioKey(String screenRatio, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (screenRatio != null)
        {
            if (screenRatio.equalsIgnoreCase("notlong"))
            {
                key = SpecKey.screenRatio_NotLong;
            }
            else if (screenRatio.equalsIgnoreCase("long"))
            {
                key = SpecKey.screenRatio_Long;
            }
        }

        return key;
    }

    private SpecKey getScreenOrientationKey(String screenOrientation, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (screenOrientation != null)
        {
            if (screenOrientation.equalsIgnoreCase("square"))
            {
                key = SpecKey.screenOrientation_Square;
            }
            else if (screenOrientation.equalsIgnoreCase("port"))
            {
                key = SpecKey.screenOrientation_Port;
            }
            else if (screenOrientation.equalsIgnoreCase("land"))
            {
                key = SpecKey.screenOrientation_Land;
            }
        }

        return key;
    }

    private SpecKey getPixelDensityKey(String pixelDensity, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (pixelDensity != null)
        {
            if (pixelDensity.equalsIgnoreCase("ldpi"))
            {
                key = SpecKey.pixelDensity_Low;
            }
            else if (pixelDensity.equalsIgnoreCase("mdpi"))
            {
                key = SpecKey.pixelDensity_Medium;
            }
            else if (pixelDensity.equalsIgnoreCase("hdpi"))
            {
                key = SpecKey.pixelDensity_High;
            }
            else if (pixelDensity.equalsIgnoreCase("xhdpi"))
            {
                key = SpecKey.pixelDensity_XHigh;
            }
        }

        return key;
    }

    private SpecKey getTouchTypeKey(String touchType, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (touchType != null)
        {
            if (touchType.equalsIgnoreCase("notouch"))
            {
                key = SpecKey.touchType_NoTouch;
            }
            else if (touchType.equalsIgnoreCase("stylus"))
            {
                key = SpecKey.touchType_Stylus;
            }
            else if (touchType.equalsIgnoreCase("finger"))
            {
                key = SpecKey.touchType_Finger;
            }
        }

        return key;
    }

    private SpecKey getTextInputMethodKey(String textInputMethod, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (textInputMethod != null)
        {
            if (textInputMethod.equalsIgnoreCase("nokeys"))
            {
                key = SpecKey.textIME_NoKeys;
            }
            else if (textInputMethod.equalsIgnoreCase("qwerty"))
            {
                key = SpecKey.textIME_Qwerty;
            }
            else if (textInputMethod.equalsIgnoreCase("twelvekey"))
            {
                key = SpecKey.textIME_TwelveKey;
            }
        }

        return key;
    }

    private SpecKey getKeyboardStateKey(String keyboardState, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (keyboardState != null)
        {
            if (keyboardState.equalsIgnoreCase("keyssoft"))
            {
                key = SpecKey.KbState_KeysSoft;
            }
            else if (keyboardState.equalsIgnoreCase("keysexposed"))
            {
                key = SpecKey.KbState_KeysExposed;
            }
            else if (keyboardState.equalsIgnoreCase("keyshidden"))
            {
                key = SpecKey.KbState_KeysHidden;
            }
        }

        return key;
    }

    private SpecKey getNavMethodKey(String navMethod, DeviceSpecification deviceSpec)
    {
        SpecKey key = null;

        if (navMethod != null)
        {
            if (navMethod.equalsIgnoreCase("nonav"))
            {
                key = SpecKey.navMethod_NoNav;
            }
            else if (navMethod.equalsIgnoreCase("dpad"))
            {
                key = SpecKey.navMethod_DPad;
            }
            else if (navMethod.equalsIgnoreCase("trackball"))
            {
                key = SpecKey.navMethod_TrackBall;
            }
            else if (navMethod.equalsIgnoreCase("wheel"))
            {
                key = SpecKey.navMethod_TrackBall;
            }
        }

        return key;
    }
}
