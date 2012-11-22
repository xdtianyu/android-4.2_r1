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

import com.motorolamobility.preflighting.core.devicelayoutspecification.ParametersType;

/**
 * 
 *                 The configType defines the content of a "config" element in a "device" element.
 * 
 *                 A "config" element can have all the parameters elements defined by
 *                 "parameterType". It also has a required "name" attribute that indicates the
 *                 user-interface name for this configuration.
 *             
 * 
 * <p>Java class for configType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="configType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.android.com/sdk/android/layout-devices/1}parametersType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}normalizedString" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
public final class ConfigType extends ParametersType
{
    protected String name;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value)
    {
        this.name = value;
    }

    @Override
    protected String toStringHeader()
    {
        return "Configuration: " + name + NEWLINE;
    }
}
