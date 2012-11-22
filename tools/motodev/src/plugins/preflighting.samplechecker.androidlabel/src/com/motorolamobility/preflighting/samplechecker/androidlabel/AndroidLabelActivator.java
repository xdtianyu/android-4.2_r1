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

package com.motorolamobility.preflighting.samplechecker.androidlabel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

public class AndroidLabelActivator implements BundleActivator
{

    public static final String PLUGIN_ID =
            "com.motorolamobility.preflighting.samplechecker.androidLabel"; //$NON-NLS-1$

    private static BundleContext context;

    private static AndroidLabelActivator plugin;

    public AndroidLabelActivator()
    {
        plugin = this;
    }

    static BundleContext getContext()
    {
        return context;
    }

    public static AndroidLabelActivator getInstance()
    {
        return plugin;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        PreflightingLogger.debug(AndroidLabelActivator.class,
                "Starting MOTODEV Studio App Validator Android Label Sample Checker Plugin...");

        AndroidLabelActivator.context = bundleContext;

        PreflightingLogger.debug(AndroidLabelActivator.class,
                "MOTODEV Studio App Validator Android Label Sample Checker Plugin started.");
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        AndroidLabelActivator.context = null;
    }

}
