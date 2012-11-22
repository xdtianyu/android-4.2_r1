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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.ConfigType;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.LayoutDevicesType;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.ObjectFactory;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.ScreenDimension;
import com.motorolamobility.preflighting.core.permissionfeature.Feature;

/**
 * Reads the XML with device specifications and creates a {@link LayoutDevicesType} model.
 */
public final class LayoutDevicesReader
{

    private static final String D_DEVICE = "d:device";

    private static final String D_SUPPORTED_FEATURES = "d:supported-features";

    private static final String D_DEFAULT = "d:default";

    private final Document document;

    /**
     * @param document XML that contains the properties of a device.
     */
    public LayoutDevicesReader(Document document)
    {
        this.document = document;
    }

    /**
     * Reads XML that contains the specifications of a given device.
     * @return {@link LayoutDevicesType} representing the device read.
     * @throws ParserConfigurationException 
     * @throws SAXException
     * @throws IOException 
     */
    public LayoutDevicesType read() throws ParserConfigurationException, SAXException, IOException
    {
        LayoutDevicesType layoutDevicesType = ObjectFactory.getInstance().createLayoutDevicesType();

        NodeList deviceList = document.getElementsByTagName(D_DEVICE);
        for (int i = 0; i < deviceList.getLength(); i++)
        {
            Node deviceNode = deviceList.item(i);
            NamedNodeMap deviceNodeAttrs = deviceNode.getAttributes();

            Node deviceIdAtr = deviceNodeAttrs.getNamedItem("id");
            if ((deviceIdAtr != null) && !deviceIdAtr.getNodeValue().trim().equals("")) //$NON-NLS-1$
            {
                // add device
                Device dev = new Device();
                dev.setId(deviceIdAtr.getNodeValue());

                dev.setName(extractValueFromAttributes(deviceNodeAttrs, "name"));
                dev.setProvider(extractValueFromAttributes(deviceNodeAttrs, "provider"));

                NodeList defaultOrConfigList = deviceNode.getChildNodes();
                for (int j = 0; j < defaultOrConfigList.getLength(); j++)
                {
                    Node defaultOrConfigNode = defaultOrConfigList.item(j);
                    if ((defaultOrConfigNode != null)
                            && (defaultOrConfigNode.getNodeType() == Node.ELEMENT_NODE))
                    {
                        if (defaultOrConfigNode.getNodeName()
                                .equalsIgnoreCase(D_SUPPORTED_FEATURES))
                        {
                            NodeList paramsList = defaultOrConfigNode.getChildNodes();
                            for (int z = 0; z < paramsList.getLength(); z++)
                            {
                                Node supportedFeatureNode = paramsList.item(z);
                                if ((supportedFeatureNode != null)
                                        && (supportedFeatureNode.getNodeType() == Node.ELEMENT_NODE))
                                {
                                    Node valueNode = supportedFeatureNode.getFirstChild();
                                    String supportedFeatureValue = valueNode.getNodeValue();
                                    if ((supportedFeatureValue != null)
                                            && !supportedFeatureValue.equals(""))
                                    {
                                        dev.getSupportedFeatures().add(
                                                new Feature(supportedFeatureValue));
                                    }
                                }
                            }
                        }
                        else
                        {
                            boolean isDefault =
                                    defaultOrConfigNode.getNodeName().equalsIgnoreCase(D_DEFAULT);
                            ParametersType paramTypes =
                                    extractParamTypes(defaultOrConfigNode, isDefault);
                            if (!(paramTypes instanceof ConfigType))
                            {
                                //default
                                dev.setDefault(paramTypes);
                            }
                            else
                            {
                                //config                                 
                                NamedNodeMap configAttrs = defaultOrConfigNode.getAttributes();
                                Node configAtr = configAttrs.getNamedItem("name");
                                if ((configAtr != null)
                                        && !configAtr.getNodeValue().trim().equals(""))
                                {
                                    ConfigType type = (ConfigType) paramTypes;
                                    type.setName(configAtr.getNodeValue());
                                    dev.addConfig(type);
                                }
                            }
                        }
                    }
                }
                layoutDevicesType.getDevices().add(dev);
            }
        }
        return layoutDevicesType;
    }

    private String extractValueFromAttributes(NamedNodeMap deviceNodeAttrs, String attribute)
    {

        String value = "";

        Node item = deviceNodeAttrs.getNamedItem(attribute);
        value = (item != null) ? item.getNodeValue() : "";

        value = (value != null) ? value : "";

        return value;
    }

    private ParametersType extractParamTypes(Node node, boolean isDefaultNode)
    {
        //add parameters
        ParametersType paramTypes =
                isDefaultNode ? ObjectFactory.getInstance().createParametersType() : ObjectFactory
                        .getInstance().createConfigType();
        NodeList paramsList = node.getChildNodes();
        for (int z = 0; z < paramsList.getLength(); z++)
        {
            Node paramNode = paramsList.item(z);
            if ((paramNode != null) && (paramNode.getNodeType() == Node.ELEMENT_NODE))
            {
                String paramName = paramNode.getNodeName();
                Node valueNode = paramNode.getFirstChild();
                String paramValue = valueNode.getNodeValue();
                if (paramName.equalsIgnoreCase("d:country-code"))
                {
                    paramTypes.setCountryCode(Float.parseFloat(paramValue));
                }
                else if (paramName.equalsIgnoreCase("d:network-code"))
                {
                    paramTypes.setNetworkCode(Float.parseFloat(paramValue));
                }
                else if (paramName.equalsIgnoreCase("d:screen-size"))
                {
                    paramTypes.setScreenSize(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:screen-ratio"))
                {
                    paramTypes.setScreenRatio(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:screen-orientation"))
                {
                    paramTypes.setScreenOrientation(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:pixel-density"))
                {
                    paramTypes.setPixelDensity(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:touch-type"))
                {
                    paramTypes.setTouchType(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:keyboard-state"))
                {
                    paramTypes.setKeyboardState(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:text-input-method"))
                {
                    paramTypes.setTextInputMethod(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:nav-state"))
                {
                    paramTypes.setNavState(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:nav-method"))
                {
                    paramTypes.setNavMethod(paramValue);
                }
                else if (paramName.equalsIgnoreCase("d:screen-dimension"))
                {
                    ScreenDimension dim =
                            ObjectFactory.getInstance().createParametersTypeScreenDimension();
                    NodeList dimensionList = paramNode.getChildNodes();
                    for (int w = 0; w < dimensionList.getLength(); w++)
                    {
                        Node dimensionNode = dimensionList.item(w);
                        if ((dimensionNode != null)
                                && (dimensionNode.getNodeType() == Node.ELEMENT_NODE))
                        {
                            Node sizeNode = dimensionNode.getFirstChild();
                            String dimValue = sizeNode.getNodeValue();
                            dim.addSize(Integer.parseInt(dimValue));
                        }
                    }
                    paramTypes.setScreenDimension(dim);
                }
                else if (paramName.equalsIgnoreCase("d:xdpi"))
                {
                    paramTypes.setXdpi(Float.parseFloat(paramValue));
                }
                else if (paramName.equalsIgnoreCase("d:ydpi"))
                {
                    paramTypes.setYdpi(Float.parseFloat(paramValue));
                }
            }
        }
        return paramTypes;
    }
}
