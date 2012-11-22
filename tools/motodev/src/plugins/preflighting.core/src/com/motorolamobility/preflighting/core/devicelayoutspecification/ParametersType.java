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

import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.ScreenDimension;

/**
 * 
 * The parametersType define all the parameters that can happen either in a
 * "default" element or in a named "config" element.
 * Each parameter element can appear once at most.
 * 
 * Parameters here are the same as those used to specify alternate Android
 * resources, as documented by
 * http://d.android.com/guide/topics/resources/resources-i18n.html#AlternateResources
 *             
 * 
 * <p>Java class for parametersType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="parametersType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="country-code" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}float">
 *               &lt;minInclusive value="100"/>
 *               &lt;maxInclusive value="999"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="network-code" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}float">
 *               &lt;minExclusive value="0"/>
 *               &lt;maxExclusive value="1000"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="screen-size" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="small"/>
 *               &lt;enumeration value="normal"/>
 *               &lt;enumeration value="large"/>
 *               &lt;enumeration value="xlarge"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="screen-ratio" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="long"/>
 *               &lt;enumeration value="notlong"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="screen-orientation" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="port"/>
 *               &lt;enumeration value="land"/>
 *               &lt;enumeration value="square"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="pixel-density" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="ldpi"/>
 *               &lt;enumeration value="mdpi"/>
 *               &lt;enumeration value="hdpi"/>
 *               &lt;enumeration value="xhdpi"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="touch-type" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="notouch"/>
 *               &lt;enumeration value="stylus"/>
 *               &lt;enumeration value="finger"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="keyboard-state" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="keysexposed"/>
 *               &lt;enumeration value="keyshidden"/>
 *               &lt;enumeration value="keyssoft"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="text-input-method" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="nokeys"/>
 *               &lt;enumeration value="qwerty"/>
 *               &lt;enumeration value="12key"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="nav-state" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="navexposed"/>
 *               &lt;enumeration value="navhidden"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="nav-method" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *               &lt;enumeration value="dpad"/>
 *               &lt;enumeration value="trackball"/>
 *               &lt;enumeration value="wheel"/>
 *               &lt;enumeration value="nonav"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="screen-dimension" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence maxOccurs="2" minOccurs="2">
 *                   &lt;element name="size">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}positiveInteger">
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="xdpi" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}float">
 *               &lt;minExclusive value="0"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="ydpi" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}float">
 *               &lt;minExclusive value="0"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
public class ParametersType
{
    /**
     * Line separator
     */
    protected final static String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Tab character
     */
    protected final static String TAB = "\t"; //$NON-NLS-1$

    private Float countryCode;

    private Float networkCode;

    private String screenSize;

    private String screenRatio;

    private String screenOrientation;

    private String pixelDensity;

    private String touchType;

    private String keyboardState;

    private String textInputMethod;

    private String navState;

    private String navMethod;

    private ScreenDimension screenDimension;

    private Float xdpi;

    private Float ydpi;

    /**
     * Gets the value for the countryCode property.
     * 
     * @return the country code.
     *     
     */
    public Float getCountryCode()
    {
        return countryCode;
    }

    /**
     * Sets the value for the countryCode property.
     * 
     * @param value the country code.
     */
    protected void setCountryCode(Float value)
    {
        this.countryCode = value;
    }

    /**
     * Gets the value for the networkCode property.
     * 
     * @return the network code.
     *     
     */
    public Float getNetworkCode()
    {
        return networkCode;
    }

    /**
     * Sets the value for the networkCode property.
     * 
     * @param value the network code.
     */
    protected void setNetworkCode(Float value)
    {
        this.networkCode = value;
    }

    /**
     * Gets the value for the screenSize property.
     * 
     * @return the screen size property.
     */
    public String getScreenSize()
    {
        return screenSize;
    }

    /**
     * Sets the value for the screenSize property.
     * 
     * @param value the value for the screenSize property.
     */
    protected void setScreenSize(String value)
    {
        this.screenSize = value;
    }

    /**
     * Gets the value for the screenRatio property.
     * 
     * @return the value for the screenRatio property.
     *     
     */
    public String getScreenRatio()
    {
        return screenRatio;
    }

    /**
     * Sets the value for the screenRatio property.
     * 
     * @param value the value for the screenRatio property.
     */
    protected void setScreenRatio(String value)
    {
        this.screenRatio = value;
    }

    /**
     * Gets the value for the screenOrientation property.
     * 
     * @return the value for the screenOrientation property.
     */
    public String getScreenOrientation()
    {
        return screenOrientation;
    }

    /**
     * Sets the value for the screenOrientation property.
     * 
     * @param value the value for the screenOrientation property.
     */
    protected void setScreenOrientation(String value)
    {
        this.screenOrientation = value;
    }

    /**
     * Gets the value for the pixelDensity property.
     * 
     * @return the value for the pixelDensity property.
     */
    public String getPixelDensity()
    {
        return pixelDensity;
    }

    /**
     * Sets the value for the pixelDensity property.
     * 
     * @param value the value for the pixelDensity property.
     */
    protected void setPixelDensity(String value)
    {
        this.pixelDensity = value;
    }

    /**
     * Gets the value for the touchType property.
     * 
     * @return the value for the touchType property.
     */
    public String getTouchType()
    {
        return touchType;
    }

    /**
     * Sets the value for the touchType property.
     * 
     * @param value the value for the touchType property.
     *     allowed object is
     *     {@link String }
     *     
     */
    protected void setTouchType(String value)
    {
        this.touchType = value;
    }

    /**
     * Gets the value for the keyboardState property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKeyboardState()
    {
        return keyboardState;
    }

    /**
     * Sets the value for the keyboardState property.
     * 
     * @param value the value for the keyboardState property.
     */
    protected void setKeyboardState(String value)
    {
        this.keyboardState = value;
    }

    /**
     * Gets the value for the textInputMethod property.
     * 
     * @return the value for the textInputMethod property.
     */
    public String getTextInputMethod()
    {
        return textInputMethod;
    }

    /**
     * Sets the value for the textInputMethod property.
     * 
     * @param value the value for the textInputMethod property.
     */
    protected void setTextInputMethod(String value)
    {
        this.textInputMethod = value;
    }

    /**
     * Gets the value for the navState property.
     * 
     * @return the value for the navState property.
     */
    public String getNavState()
    {
        return navState;
    }

    /**
     * Sets the value for the navState property.
     * 
     * @param value the value for the navState property.
     */
    protected void setNavState(String value)
    {
        this.navState = value;
    }

    /**
     * Gets the value for the navMethod property.
     * 
     * @return the value for the navMethod property.
     */
    public String getNavMethod()
    {
        return navMethod;
    }

    /**
     * Sets the value for the navMethod property.
     * 
     * @param value the value for the navMethod property.
     */
    protected void setNavMethod(String value)
    {
        this.navMethod = value;
    }

    /**
     * Gets the value for the screenDimension property.
     * 
     * @return the value for the screenDimension property.
     */
    public ScreenDimension getScreenDimension()
    {
        return screenDimension;
    }

    /**
     * Sets the value for the screenDimension property.
     * 
     * @param value the value for the screenDimension property.
     */
    protected void setScreenDimension(ScreenDimension value)
    {
        this.screenDimension = value;
    }

    /**
     * Gets the value for the xdpi property.
     * 
     * @return the value for the xdpi property.
     */
    public Float getXdpi()
    {
        return xdpi;
    }

    /**
     * Sets the value for the xdpi property.
     * 
     * @param value the value for the xdpi property.
     */
    protected void setXdpi(Float value)
    {
        this.xdpi = value;
    }

    /**
     * Gets the value for the ydpi property.
     * 
     * @return the value for the ydpi property.
     */
    public Float getYdpi()
    {
        return ydpi;
    }

    /**
     * Sets the value for the ydpi property.
     * 
     * @param value the value for the ydpi property.
     */
    protected void setYdpi(Float value)
    {
        this.ydpi = value;
    }

    /**
     * Prints out the Header.
     * 
     * @return The text of the Header.
     */
    protected String toStringHeader()
    {
        return "Default parameters:" + "\n";
    }

    /**
     * This implementation provides a human-readable text of this
     * {@link ParametersType}.
     * 
     * @return Returns a human-readable text of this {@link ParametersType}.
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(toStringHeader());
        if (countryCode != null)
        {
            builder.append(TAB + "countryCode: " + countryCode + NEWLINE);
        }
        if (networkCode != null)
        {
            builder.append(TAB + "networkCode: " + networkCode + NEWLINE);
        }
        if (screenSize != null)
        {
            builder.append(TAB + "screenSize: " + screenSize + NEWLINE);
        }
        if (screenRatio != null)
        {
            builder.append(TAB + "screenRatio: " + screenRatio + NEWLINE);
        }
        if (screenOrientation != null)
        {
            builder.append(TAB + "screenOrientation: " + screenOrientation + NEWLINE);
        }
        if (pixelDensity != null)
        {
            builder.append(TAB + "pixelDensity: " + pixelDensity + NEWLINE);
        }
        if (touchType != null)
        {
            builder.append(TAB + "touchType: " + touchType + NEWLINE);
        }
        if (keyboardState != null)
        {
            builder.append(TAB + "keyboardState: " + keyboardState + NEWLINE);
        }
        if (textInputMethod != null)
        {
            builder.append(TAB + "textInputMethod: " + textInputMethod + NEWLINE);
        }
        if (navState != null)
        {
            builder.append(TAB + "navState: " + navState + NEWLINE);
        }
        if (navMethod != null)
        {
            builder.append(TAB + "navMethod: " + navMethod + NEWLINE);
        }
        if (screenDimension != null)
        {
            builder.append(TAB + "screenDimension: " + screenDimension + NEWLINE);
        }
        if (xdpi != null)
        {
            builder.append(TAB + "xdpi: " + xdpi + NEWLINE);
        }
        if (ydpi != null)
        {
            builder.append(TAB + "ydpi: " + ydpi + NEWLINE);
        }
        builder.append(NEWLINE);
        return builder.toString();
    }
}
