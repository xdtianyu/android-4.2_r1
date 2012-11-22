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
package com.motorolamobility.preflighting.core;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

/**
 * This is the PLugin's Activator. It is responsible for initializing
 * this plug-in.
 */
public class PreflightingCorePlugin implements BundleActivator
{
    /**
     * This is the unique identifier of this plugin.
     */
    public static final String PLUGIN_ID = "com.motorolamobility.preflighting.core"; //$NON-NLS-1$

    private static BundleContext context;

    private static PreflightingCorePlugin plugin;

    private static Set<String> availableMarkers;

    /**
     * Default constructor.
     */
    public PreflightingCorePlugin()
    {
        plugin = this;
        availableMarkers = new HashSet<String>();
    }

    /**
     * Retrieve the {@link BundleContext} of this plug-in.
     * 
     * @return Returns the {@link BundleContext} of this plug-in.
     */
    public static BundleContext getContext()
    {
        return context;
    }

    /**
     * Retrieves an instance of this plug-in.
     * 
     * @return Returns an instance of this plug-in.
     */
    public static PreflightingCorePlugin getInstance()
    {
        return plugin;
    }

    /**
     * Start this plug-in.
     * 
     * @param bundleContext {@link BundleContext} to be used with
     * this plug-in.
     * 
     * @throws Exception Exception thrown when there is an error
     * starting this plug-in.
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        PreflightingCorePlugin.context = bundleContext;
        PreflightingLogger.debug(PreflightingCorePlugin.class,
                "Starting Preflighting Core Plugin...");

        PreflightingLogger.debug(PreflightingCorePlugin.class,
                "Preflighting Core Plugin started...");
    }

    /**
     * Stop this plug-in.
     * 
     * @param bundleContext {@link BundleContext} used with this plug-in.
     * 
     * @throws Exception Exception thrown when there is an error stopping this plug-in.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        PreflightingCorePlugin.context = null;
    }

    public static boolean addAvailableMarker(String markerType)
    {
        return PreflightingCorePlugin.availableMarkers.add(markerType);
    }

    public static Set<String> getAvailableMarkers()
    {
        return PreflightingCorePlugin.availableMarkers;
    }
}