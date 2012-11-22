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
package com.motorolamobility.preflighting.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.motorolamobility.preflighting.core.devicelayoutspecification.LayoutDevicesReader;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.LayoutDevicesType;
import com.motorolamobility.preflighting.core.internal.devicelayoutspecification.ObjectFactory;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

/**
 * Utility class to manipulate XML documents, such as:
 * <ul> 
 * <li>Print formatted XML in output streams;</li>
 * <li>Get a specified node as a string;</li>
 * <li>Parse a &lt;device&gt;.xml available in the folder devices to get representation.</li>
 * </ul>
 */
public class XmlUtils
{
    public static final String DEVICE_CONFIGURATION_FOLDER = "devices";

    /**
     * Print the indented document.
     * 
     * @param stream
     * @param document
     * 
     * @throws UnsupportedEncodingException 
     * @throws TransformerExceptions 
     */
    public static void printXMLFormat(Document document) throws UnsupportedEncodingException,
            TransformerException
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4); //$NON-NLS-1$
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

        StreamResult streamResult = new StreamResult(); //$NON-NLS-1$

        DOMSource source = new DOMSource(document);
        transformer.transform(source, streamResult);
    }

    /**
     * Get a string representing a XML node. 
     * 
     * @param node XML node to be printed.
     * @param deep If true recursively prints the node and all of its children. If false prints only the node itself.
     * 
     * @return string Representation of the document.
     */
    public static String getXMLNodeAsString(Node node, boolean deep)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        String outputString = null;
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();

            DOMImplementationLS ls = (DOMImplementationLS) builder.getDOMImplementation();
            LSSerializer lss = ls.createLSSerializer();
            LSOutput lso = ls.createLSOutput();
            lso.setByteStream(stream);
            lss.write(node.cloneNode(deep), lso);
            stream.flush();
            outputString = baos.toString();
            outputString =
                    outputString.substring(outputString.indexOf(">") + 1, outputString.length());
        }
        catch (ParserConfigurationException e)
        {
            PreflightingLogger.debug("Unable to get Xml Node as String");
        }
        finally
        {
            if (baos != null)
            {
                try
                {
                    baos.close();
                }
                catch (IOException e)
                {
                    //Do nothing.
                }
            }
            try
            {
                if (stream != null)
                {
                    stream.flush();
                    stream.close();
                }
            }
            catch (Exception ex)
            {
                //do nothing
            }
        }

        return outputString;
    }

    /**
     * Parse the data about device folder that can be used in the validation.
     *   
     * @return {@link LayoutDevicesType} representing device (e.g.: screen layout).
     */
    public static LayoutDevicesType parseDevicesXmlFiles()
    {
        LayoutDevicesType layoutDevicesType = ObjectFactory.getInstance().createLayoutDevicesType();

        String path = Platform.getInstallLocation().getURL().getPath();
        if (!path.endsWith(File.separator))
        {
            path += File.separator;
        }
        path += DEVICE_CONFIGURATION_FOLDER;
        File xmlFilesFolder = new File(path);

        if (xmlFilesFolder.exists() && xmlFilesFolder.isDirectory())
        {
            String[] deviceXmlFiles = xmlFilesFolder.list(new FilenameFilter()
            {
                public boolean accept(File arg0, String arg1)
                {
                    if (arg1.endsWith(".xml"))
                    {
                        return true;
                    }
                    return false;
                }
            });

            for (String currentFile : deviceXmlFiles)
            {
                layoutDevicesType.getDevices().addAll(
                        readDeviceFile(new File(xmlFilesFolder, currentFile)).getDevices());
            }
        }

        return layoutDevicesType;
    }

    private static LayoutDevicesType readDeviceFile(File f)
    {
        LayoutDevicesType layoutDevicesType = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(f);
            LayoutDevicesReader reader = new LayoutDevicesReader(document);
            layoutDevicesType = reader.read();
        }
        catch (ParserConfigurationException e)
        {
            PreflightingLogger.debug("Unable to read file " + f.getAbsolutePath());
        }
        catch (Exception e)
        {
            PreflightingLogger.debug("Unable to parse file " + f.getAbsolutePath());
        }
        return layoutDevicesType;
    }
}
