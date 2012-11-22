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
package com.motorolamobility.preflighting.core.internal.devicelayoutspecification;

import com.motorolamobility.preflighting.core.devicelayoutspecification.Device;
import com.motorolamobility.preflighting.core.devicelayoutspecification.ParametersType;

/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the layout_devices package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
public final class ObjectFactory
{
    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: layout_devices
     * 
     */
    private ObjectFactory()
    {
    }

    /**
     * Singleton
     */
    private final static ObjectFactory instance = new ObjectFactory();

    public static synchronized ObjectFactory getInstance()
    {
        return instance;
    }

    /**
     * Create an instance of {@link ParametersType.ScreenDimension }
     * 
     */
    public ScreenDimension createParametersTypeScreenDimension()
    {
        return new ScreenDimension();
    }

    /**
     * Create an instance of {@link ConfigType }
     * 
     */
    public ConfigType createConfigType()
    {
        return new ConfigType();
    }

    /**
     * Create an instance of {@link ParametersType }
     * 
     */
    public ParametersType createParametersType()
    {
        return new ParametersType();
    }

    /**
     * Create an instance of {@link LayoutDevicesType.Device }
     * 
     */
    public Device createLayoutDevicesTypeDevice()
    {
        return new Device();
    }

    /**
     * Create an instance of {@link LayoutDevicesType }
     * 
     */
    public LayoutDevicesType createLayoutDevicesType()
    {
        return new LayoutDevicesType();
    }
}
