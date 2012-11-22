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
package com.motorolamobility.preflighting.core.devicelayoutspecification;

import java.util.ArrayList;
import java.util.List;

import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.ConfigType;
import com.motorolamobility.preflighting.core.permissionfeature.Feature;

/**
 * <p>This class is a bean for device specification in XML format.
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="default" type="{http://schemas.android.com/sdk/android/layout-devices/1}parametersType" minOccurs="0"/>
 *         &lt;element name="config" type="{http://schemas.android.com/sdk/android/layout-devices/1}configType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}normalizedString" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
public class Device
{

    /**
     * Line separator
     */
    private final static String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Tab character
     */
    private final static String TAB = "\t"; //$NON-NLS-1$

    protected ParametersType _default;

    /**
     * The list of specifications.
     */
    protected List<ConfigType> config = new ArrayList<ConfigType>();

    /**
     * The list of features this device supports.
     */
    protected List<Feature> supportedFeatures = new ArrayList<Feature>();

    /**
     * The device name.
     */
    protected String name;

    /**
     * The device id.
     */
    protected String id;

    /**
     * The device provider/manufacturer.
     */
    protected String provider;

    /**
     * Gets the value of the default property.
     * 
     * @return
     *     possible object is
     *     {@link ParametersType }
     *     
     */
    public ParametersType getDefault()
    {
        return _default;
    }

    /**
     * Sets the value of the default property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParametersType }
     *     
     */
    public void setDefault(ParametersType value)
    {
        this._default = value;
    }

    /**
     * Gets the value of the config property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the config property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfig().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list:
     * {@link ConfigType}
     * 
     * 
     */
    public List<ConfigType> getConfig()
    {
        return this.config;
    }

    /**
     * Adds a configuration/specification to the list of specifications.
     * @param type the configuration to be added.
     */
    public void addConfig(ConfigType type)
    {
        config.add(type);
    }

    /**
     * Gets the value of the name property.
     * 
     * @return the name of this device.
     *     
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value the name of this device.
     *     
     */
    public void setName(String value)
    {
        this.name = value;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        String provider = getProvider();
        provider = (provider != null) && (!provider.equals("")) ? " - " + provider : "";
        builder.append(PreflightingCoreNLS.Device_Device + TAB + name + provider + NEWLINE);

        builder.append("Id: " + getId() + NEWLINE);

        builder.append(NEWLINE);
        if (_default != null)
        {
            builder.append(_default);
        }
        if ((config != null) && (config.size() > 0))
        {
            for (ConfigType conf : config)
            {
                builder.append(conf);
            }
        }
        builder.append(NEWLINE);
        if ((supportedFeatures != null) && (supportedFeatures.size() > 0))
        {
            builder.append(PreflightingCoreNLS.Device_SupportedFeatures + NEWLINE);
            for (Feature nonSupportedFeat : supportedFeatures)
            {
                builder.append(TAB + nonSupportedFeat.getId() + NEWLINE);
            }
        }
        return builder.toString();
    }

    public List<Feature> getSupportedFeatures()
    {
        return supportedFeatures;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

}
