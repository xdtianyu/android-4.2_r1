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

package com.motorolamobility.preflighting.samplechecker.findviewbyid;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class SampleCheckersActivator implements BundleActivator
{

    // The plug-in ID
    public static final String PLUGIN_ID =
            "com.motorolamobility.preflighting.samplechecker.findviewbyid"; //$NON-NLS-1$

    private static BundleContext context;

    // The shared instance
    private static SampleCheckersActivator plugin;

    /**
     * The constructor
     */
    public SampleCheckersActivator()
    {
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception
    {
        PreflightingLogger
                .debug(SampleCheckersActivator.class,
                        "Starting MOTODEV Studio App Validator SDK Unnecessary findViewById Sample Checker...");

        SampleCheckersActivator.context = context;

        PreflightingLogger
                .debug(SampleCheckersActivator.class,
                        "MOTODEV Studio App Validator SDK Unnecessary findViewById Sample Checker started...");
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception
    {
        SampleCheckersActivator.context = null;
    }

    static BundleContext getContext()
    {
        return context;
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static SampleCheckersActivator getDefault()
    {
        return plugin;
    }

}
