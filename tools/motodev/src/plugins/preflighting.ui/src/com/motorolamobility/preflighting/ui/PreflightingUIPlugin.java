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

package com.motorolamobility.preflighting.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class PreflightingUIPlugin extends AbstractUIPlugin
{

    /**
     * The Preflighting UI plugin id
     */
    public static final String PREFLIGHTING_UI_PLUGIN_ID = "com.motorolamobility.preflighting.ui"; //$NON-NLS-1$

    /**
     * The preference key for preflighting command line arguments
     */
    public static final String COMMAND_LINE_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".commandLinePreference"; //$NON-NLS-1$

    public static final String OUTPUT_LIMIT_VALUE = PREFLIGHTING_UI_PLUGIN_ID + ".outputLimit";

    public static final String OUTPUT_LIMIT_DEFAULT_VALUE = "1000";

    public static final String OUTPUT_TYPE_VALUE = PREFLIGHTING_UI_PLUGIN_ID + ".outputType";

    public static final String OUTPUT_TYPE_DEFAULT_VALUE = "0";

    public static final String WARNING_LEVEL_VALUE = PREFLIGHTING_UI_PLUGIN_ID + ".warningLevel";

    public static final String WARNING_LEVEL_DEFAULT_VALUE = "4";

    public static final String VERBOSITY_LEVEL_VALUE = PREFLIGHTING_UI_PLUGIN_ID
            + ".verbosityLevel";

    public static final String VERBOSITY_LEVEL_DEFAULT_VALUE = "0";

    /**
     * The preference key for preflighting problems view markers policy
     */
    public static final String ERRORS_TO_WARNINGS_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".errorsToWarningPreference"; //$NON-NLS-1$

    public static final String ECLIPSE_PROBLEM_TO_WARNING_VALUE = PREFLIGHTING_UI_PLUGIN_ID
            + ".eclipseErrorToWarning";

    public static final String ECLIPSE_PROBLEM_TO_WARNING_DEFAULT_VALUE = "true";

    /**
     * The preference key for devices to be checked against
     */
    public static final String DEVICES_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".devicesPreference"; //$NON-NLS-1$

    public static final String USE_ALL_DEVICES_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".useAllDevicesPreference"; //$NON-NLS-1$;

    /**
     * The preference keys for checkers
     */
    public static final String CHECKERS_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".checkersPreference"; //$NON-NLS-1$

    public static final String USE_ALL_CHECKERS_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".useAllCheckersPreference"; //$NON-NLS-1$

    public static final String CHECKERS_PARAMS_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".checkersParamsPreference"; //$NON-NLS-1$;

    public static final String CHECKERS_WARNING_LEVELS_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".checkersWarningLevelsPreference"; //$NON-NLS-1$;

    public static final String CHECKERS_CONDITIONS_WARNING_LEVELS_PREFERENCE_KEY =
            PREFLIGHTING_UI_PLUGIN_ID + ".checkersConditionsWarningLevelsPreference"; //$NON-NLS-1$;

    public static final String CHECKERS_CONDITIONS_PREFERENCE_KEY = PREFLIGHTING_UI_PLUGIN_ID
            + ".checkersConditionsPreference"; //$NON-NLS-1$;

    public static final String DEFAULT_BACKWARD_COMMANDLINE = "-w4 -v";

    public static final String DEFAULT_COMMANDLINE = "-output text -w4 -v0";

    public static final String SHOW_BACKWARD_DIALOG = PREFLIGHTING_UI_PLUGIN_ID
            + ".showBackwardDialog";

    public final static String TOGGLE_DIALOG = ".toggle.dialog";

    public static final String COMMAND_LINE_PREFERENCE_PAGE =
            "com.motorolamobility.preflighting.ui.commandLinePreferencePage";

    // The shared instance
    private static PreflightingUIPlugin plugin;

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception
    {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static PreflightingUIPlugin getDefault()
    {
        return plugin;
    }
}
