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

package com.motorolamobility.preflighting.checkers;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

public class CheckerPlugin implements BundleActivator
{

    private static BundleContext context;

    public static final String PLUGIN_ID = "com.motorolamobility.preflighting.checkers";

    static BundleContext getContext()
    {
        return context;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        PreflightingLogger.debug(CheckerPlugin.class, "Starting Preflighting Checker Plugin...");

        CheckerPlugin.context = bundleContext;

        PreflightingLogger.debug(CheckerPlugin.class, "Preflighting Checker Plugin started...");
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        CheckerPlugin.context = null;
    }

}
