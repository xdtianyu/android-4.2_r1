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
package com.motorolamobility.preflighting.internal.commandoutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;

public class DaemonXMLOutputter extends XmlOutputter
{
    private static final String XML_TAG_CSV_OUTPUT = "CSVContent";

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.commandoutput.XmlOutputter#generateCustomApplicationNodes(org.w3c.dom.Element, com.motorolamobility.preflighting.core.validation.ApplicationValidationResult, java.util.List)
     */
    @Override
    protected void generateCustomApplicationNodes(Element applicationElem,
            ApplicationValidationResult result, List<Parameter> parameters)
    {
        /*
         * Put the validation result as a CDATA section inside xml output
         */
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outStream);
        CSVOutputter outputter = new CSVOutputter();
        try
        {
            outputter.print(result, stream, parameters);
            Element csvout = document.createElement(XML_TAG_CSV_OUTPUT);
            CDATASection cdata = document.createCDATASection(outStream.toString().trim());
            csvout.appendChild(cdata);
            applicationElem.appendChild(csvout);
            Node manifest =
                    document.importNode(
                            result.getXmlResultDocument().getElementsByTagName("manifest").item(0),
                            true);
            applicationElem.appendChild(manifest);

        }
        catch (PreflightingToolException e)
        {
            //do nothing
        }
        finally
        {
            stream.close();
            try
            {
                outStream.close();
            }
            catch (IOException e)
            {
                //Do nothing.
            }
        }
    }
}
