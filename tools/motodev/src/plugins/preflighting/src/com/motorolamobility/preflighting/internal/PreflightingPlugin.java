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
package com.motorolamobility.preflighting.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.motorolamobility.preflighting.core.PreflightingCorePlugin;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

public class PreflightingPlugin extends Plugin implements BundleActivator
{
    private static PreflightingPlugin instance;

    private String appValidatorVersion = null;

    private static final String COM_MOTOROLAMOBILITY_PREFLIGHTING_FEATURE =
            "com.motorolamobility.preflighting.feature";

    public PreflightingPlugin()
    {
        instance = this;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        PreflightingLogger.debug(PreflightingPlugin.class, "Starting Preflighting Plugin...");

        super.start(context);

        PreflightingLogger.debug(PreflightingPlugin.class, "Preflighting Plugin started...");
    }

    public static PreflightingPlugin getInstance()
    {
        return instance;
    }

    private void readAppValidatorVersion()
    {
        IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();
        List<IBundleGroup> groups = new ArrayList<IBundleGroup>();
        if (providers != null)
        {
            for (int i = 0; i < providers.length; ++i)
            {
                IBundleGroup[] bundleGroups = providers[i].getBundleGroups();
                groups.addAll(Arrays.asList(bundleGroups));
            }
        }
        for (IBundleGroup group : groups)
        {
            if (group.getIdentifier().equals(COM_MOTOROLAMOBILITY_PREFLIGHTING_FEATURE))
            {
                appValidatorVersion = group.getVersion();
                break;
            }
        }

        /*
         * WORKAROUND for commandline product
         */
        if (appValidatorVersion == null)
        {
            List<Bundle> appValidatorBundles = new ArrayList<Bundle>();
            appValidatorBundles.add(this.getBundle());
            appValidatorBundles.add(Platform
                    .getBundle("com.motorolamobility.preflighting.checkers"));
            appValidatorBundles.add(PreflightingCorePlugin.getContext().getBundle());

            Version v = null;
            for (Bundle b : appValidatorBundles)
            {
                if ((v == null) && (b != null))
                {
                    v = b.getVersion();
                }
                if ((b != null) && (v != null) && (b.getVersion().compareTo(v) > 0))
                {
                    v = b.getVersion();
                }
            }

            appValidatorVersion = v.toString();
        }

        /*
         * End of WORKAROUND for commandline product
         */
    }

    public String getAppValidatorVersion()
    {
        if (appValidatorVersion == null)
        {
            readAppValidatorVersion();
        }
        return appValidatorVersion;
    }

}
