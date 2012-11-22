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

import java.util.ArrayList;
import java.util.List;

import com.motorolamobility.preflighting.core.devicelayoutspecification.Device;

/**
 * 
 *  The "layout-devices" element is the root element of this schema.
 * 
 *  It must contain zero or more "device" elements that each define the configurations
 *  available for a given device.
 * 
 *  These definitions are used in the Graphical Layout Editor in the
 *  Android Development Tools (ADT) plugin for Eclipse.
 *             
 * 
 * <p>Java class for layoutDevicesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="layoutDevicesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="device" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="default" type="{http://schemas.android.com/sdk/android/layout-devices/1}parametersType" minOccurs="0"/>
 *                   &lt;element name="config" type="{http://schemas.android.com/sdk/android/layout-devices/1}configType" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}normalizedString" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
public final class LayoutDevicesType
{

    protected List<Device> devices = new ArrayList<Device>();

    /**
     * Gets the value of the device property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the device property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDevice().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LayoutDevicesType.Device }
     * 
     * 
     */
    public List<Device> getDevices()
    {
        return this.devices;
    }

    @Override
    public String toString()
    {
        return "LayoutDevicesType [device=" + devices + "]";
    }
}
