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

import static com.motorolamobility.preflighting.core.logging.PreflightingLogger.warn;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.motorolamobility.preflighting.output.AbstractOutputter;

public class OutputterExtensionReader
{

    private static final String OUTPUTTER_EXT_POINT_ID =
            "com.motorolamobility.preflighting.outputter";

    private static final String OUTPUTTER_ATTRIBUTE_ID = "id";

    private static final String OUTPUTTER_ATTRIBUTE_CLASS = "class";

    private static Map<String, AbstractOutputter> outputtersMap = null;

    private static Map<String, AbstractOutputter> loadOutputters()
    {

        outputtersMap = new HashMap<String, AbstractOutputter>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements =
                registry.getConfigurationElementsFor(OUTPUTTER_EXT_POINT_ID);

        for (IConfigurationElement element : elements)
        {
            if (element.getName().equals("outputter"))
            {
                try
                {
                    String id = element.getAttribute(OUTPUTTER_ATTRIBUTE_ID).toUpperCase();
                    AbstractOutputter outputter =
                            (AbstractOutputter) element
                                    .createExecutableExtension(OUTPUTTER_ATTRIBUTE_CLASS);

                    outputtersMap.put(id, outputter);

                }
                catch (CoreException e)
                {
                    warn(OutputterExtensionReader.class,
                            "Error reading outputter extension for extension point " //$NON-NLS-1$
                                    + OUTPUTTER_EXT_POINT_ID, e);
                }

            }

        }

        return outputtersMap;

    }

    public static Map<String, AbstractOutputter> getOutputtersMap()
    {

        return outputtersMap != null ? outputtersMap : loadOutputters();
    }

}
