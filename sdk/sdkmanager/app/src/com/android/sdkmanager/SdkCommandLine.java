/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.sdkmanager;

import com.android.sdklib.SdkManager;
import com.android.sdklib.repository.SdkRepoConstants;
import com.android.sdklib.util.CommandLineParser;
import com.android.utils.ILogger;

import java.util.Arrays;


/**
 * Specific command-line flags for the {@link SdkManager}.
 */
class SdkCommandLine extends CommandLineParser {

    /*
     * Steps needed to add a new action:
     * - Each action is defined as a "verb object" followed by parameters.
     * - Either reuse a VERB_ constant or define a new one.
     * - Either reuse an OBJECT_ constant or define a new one.
     * - Add a new entry to mAction with a one-line help summary.
     * - In the constructor, add a define() call for each parameter (either mandatory
     *   or optional) for the given action.
     */

    public final static String VERB_LIST    = "list";                               //$NON-NLS-1$
    public final static String VERB_CREATE  = "create";                             //$NON-NLS-1$
    public final static String VERB_MOVE    = "move";                               //$NON-NLS-1$
    public final static String VERB_DELETE  = "delete";                             //$NON-NLS-1$
    public final static String VERB_UPDATE  = "update";                             //$NON-NLS-1$
    public final static String VERB_SDK     = "sdk";                                //$NON-NLS-1$
    public final static String VERB_AVD     = "avd";                                //$NON-NLS-1$

    public static final String OBJECT_SDK            = "sdk";                       //$NON-NLS-1$
    public static final String OBJECT_AVD            = "avd";                       //$NON-NLS-1$
    public static final String OBJECT_AVDS           = "avds";                      //$NON-NLS-1$
    public static final String OBJECT_TARGET         = "target";                    //$NON-NLS-1$
    public static final String OBJECT_TARGETS        = "targets";                   //$NON-NLS-1$
    public static final String OBJECT_PROJECT        = "project";                   //$NON-NLS-1$
    public static final String OBJECT_TEST_PROJECT   = "test-project";              //$NON-NLS-1$
    public static final String OBJECT_LIB_PROJECT    = "lib-project";               //$NON-NLS-1$
    public static final String OBJECT_ADB            = "adb";                       //$NON-NLS-1$

    public static final String ARG_ALIAS        = "alias";                          //$NON-NLS-1$
    public static final String ARG_ACTIVITY     = "activity";                       //$NON-NLS-1$

    public static final String KEY_ACTIVITY     = ARG_ACTIVITY;
    public static final String KEY_PACKAGE      = "package";                        //$NON-NLS-1$
    public static final String KEY_MODE         = "mode";                           //$NON-NLS-1$
    public static final String KEY_TARGET_ID    = OBJECT_TARGET;
    public static final String KEY_NAME         = "name";                           //$NON-NLS-1$
    public static final String KEY_LIBRARY      = "library";                        //$NON-NLS-1$
    public static final String KEY_PATH         = "path";                           //$NON-NLS-1$
    public static final String KEY_FILTER       = "filter";                         //$NON-NLS-1$
    public static final String KEY_SKIN         = "skin";                           //$NON-NLS-1$
    public static final String KEY_SDCARD       = "sdcard";                         //$NON-NLS-1$
    public static final String KEY_FORCE        = "force";                          //$NON-NLS-1$
    public static final String KEY_RENAME       = "rename";                         //$NON-NLS-1$
    public static final String KEY_SUBPROJECTS  = "subprojects";                    //$NON-NLS-1$
    public static final String KEY_MAIN_PROJECT = "main";                           //$NON-NLS-1$
    public static final String KEY_NO_UI        = "no-ui";                          //$NON-NLS-1$
    public static final String KEY_NO_HTTPS     = "no-https";                       //$NON-NLS-1$
    public static final String KEY_PROXY_PORT   = "proxy-port";                     //$NON-NLS-1$
    public static final String KEY_PROXY_HOST   = "proxy-host";                     //$NON-NLS-1$
    public static final String KEY_DRY_MODE     = "dry-mode";                       //$NON-NLS-1$
    public static final String KEY_ALL          = "all";                            //$NON-NLS-1$
    public static final String KEY_EXTENDED     = "extended";                       //$NON-NLS-1$
    public static final String KEY_SNAPSHOT     = "snapshot";                       //$NON-NLS-1$
    public static final String KEY_COMPACT      = "compact";                        //$NON-NLS-1$
    public static final String KEY_EOL_NULL     = "null";                           //$NON-NLS-1$
    public static final String KEY_ABI          = "abi";                            //$NON-NLS-1$
    public static final String KEY_ACCOUNT      = "account";                        //$NON-NLS-1$
    public static final String KEY_KEYSTORE     = "keystore";                       //$NON-NLS-1$
    public static final String KEY_ALIAS        = "alias";                          //$NON-NLS-1$
    public static final String KEY_STOREPASS    = "storepass";                      //$NON-NLS-1$
    public static final String KEY_KEYPASS      = "keypass";                        //$NON-NLS-1$
    public static final String KEY_CLEAR_CACHE   = "clear-cache";                   //$NON-NLS-1$

    /**
     * Action definitions for SdkManager command line.
     * <p/>
     * This list serves two purposes: first it is used to know which verb/object
     * actions are acceptable on the command-line; second it provides a summary
     * for each action that is printed in the help.
     * <p/>
     * Each entry is a string array with:
     * <ul>
     * <li> the verb.
     * <li> an object (use #NO_VERB_OBJECT if there's no object).
     * <li> a description.
     * <li> an alternate form for the object (e.g. plural).
     * </ul>
     */
    private final static String[][] ACTIONS = {

            { VERB_SDK, NO_VERB_OBJECT,
                "Displays the SDK Manager window." },
            { VERB_AVD, NO_VERB_OBJECT,
                "Displays the AVD Manager window.",
                },

            { VERB_LIST, NO_VERB_OBJECT,
                "Lists existing targets or virtual devices." },
            { VERB_LIST, OBJECT_AVD,
                "Lists existing Android Virtual Devices.",
                OBJECT_AVDS },
            { VERB_LIST, OBJECT_TARGET,
                "Lists existing targets.",
                OBJECT_TARGETS },
            { VERB_LIST, OBJECT_SDK,
                "Lists remote SDK repository." },

            { VERB_CREATE, OBJECT_AVD,
                "Creates a new Android Virtual Device." },
            { VERB_MOVE, OBJECT_AVD,
                "Moves or renames an Android Virtual Device." },
            { VERB_DELETE, OBJECT_AVD,
                "Deletes an Android Virtual Device." },
            { VERB_UPDATE, OBJECT_AVD,
                "Updates an Android Virtual Device to match the folders of a new SDK." },

            { VERB_CREATE, OBJECT_PROJECT,
                "Creates a new Android project." },
            { VERB_UPDATE, OBJECT_PROJECT,
                "Updates an Android project (must already have an AndroidManifest.xml)." },

            { VERB_CREATE, OBJECT_TEST_PROJECT,
                "Creates a new Android project for a test package." },
            { VERB_UPDATE, OBJECT_TEST_PROJECT,
                "Updates the Android project for a test package (must already have an AndroidManifest.xml)." },

            { VERB_CREATE, OBJECT_LIB_PROJECT,
                "Creates a new Android library project." },
            { VERB_UPDATE, OBJECT_LIB_PROJECT,
                "Updates an Android library project (must already have an AndroidManifest.xml)." },

            { VERB_UPDATE, OBJECT_ADB,
                "Updates adb to support the USB devices declared in the SDK add-ons." },

            { VERB_UPDATE, OBJECT_SDK,
                "Updates the SDK by suggesting new platforms to install if available." },
    };

    public SdkCommandLine(ILogger logger) {
        super(logger, ACTIONS);

        // The following defines the parameters of the actions defined in mAction.

        // --- generic actions that can work on any verb ---

        define(Mode.BOOLEAN, false,
                GLOBAL_FLAG_VERB, NO_VERB_OBJECT, ""/*shortName*/, KEY_CLEAR_CACHE, //$NON-NLS-1$
                "Clear the SDK Manager repository manifest cache.", false);         //$NON-NLS-1$

        // --- list avds ---

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_AVD, "c", KEY_COMPACT,                            //$NON-NLS-1$
                "Compact output (suitable for scripts)", false);

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_AVD, "0", KEY_EOL_NULL,                           //$NON-NLS-1$
                "Terminates lines with \\0 instead of \\n (e.g. for xargs -0). Only used by --" + KEY_COMPACT + ".",
                false);

        // --- list targets ---

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_TARGET, "c", KEY_COMPACT,                         //$NON-NLS-1$
                "Compact output (suitable for scripts)", false);

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_TARGET, "0", KEY_EOL_NULL,                        //$NON-NLS-1$
                "Terminates lines with \\0 instead of \\n (e.g. for xargs -0) Only used by --" + KEY_COMPACT + ".",
                false);

        // --- create avd ---

        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "p", KEY_PATH,                             //$NON-NLS-1$
                "Directory where the new AVD will be created.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_AVD, "n", KEY_NAME,                             //$NON-NLS-1$
                "Name of the new AVD.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_AVD, "t", KEY_TARGET_ID,                        //$NON-NLS-1$
                "Target ID of the new AVD.", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "s", KEY_SKIN,                             //$NON-NLS-1$
                "Skin for the new AVD.", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "c", KEY_SDCARD,                           //$NON-NLS-1$
                "Path to a shared SD card image, or size of a new sdcard for the new AVD.", null);
        define(Mode.BOOLEAN, false,
                VERB_CREATE, OBJECT_AVD, "f", KEY_FORCE,                            //$NON-NLS-1$
                "Forces creation (overwrites an existing AVD)", false);
        define(Mode.BOOLEAN, false,
                VERB_CREATE, OBJECT_AVD, "a", KEY_SNAPSHOT,                         //$NON-NLS-1$
                "Place a snapshots file in the AVD, to enable persistence.", false);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "b", KEY_ABI,                           //$NON-NLS-1$
                "The ABI to use for the AVD. The default is to auto-select the ABI if the platform has only one ABI for its system images.",
                null);

        // --- delete avd ---

        define(Mode.STRING, true,
                VERB_DELETE, OBJECT_AVD, "n", KEY_NAME,                             //$NON-NLS-1$
                "Name of the AVD to delete.", null);

        // --- move avd ---

        define(Mode.STRING, true,
                VERB_MOVE, OBJECT_AVD, "n", KEY_NAME,                               //$NON-NLS-1$
                "Name of the AVD to move or rename.", null);
        define(Mode.STRING, false,
                VERB_MOVE, OBJECT_AVD, "r", KEY_RENAME,                             //$NON-NLS-1$
                "New name of the AVD.", null);
        define(Mode.STRING, false,
                VERB_MOVE, OBJECT_AVD, "p", KEY_PATH,                               //$NON-NLS-1$
                "Path to the AVD's new directory.", null);

        // --- update avd ---

        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_AVD, "n", KEY_NAME,                             //$NON-NLS-1$
                "Name of the AVD to update", null);

        // --- list sdk ---

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_SDK, "u", KEY_NO_UI,                              //$NON-NLS-1$
                "Displays list result on console (no GUI)", true);

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_SDK, "s", KEY_NO_HTTPS,                           //$NON-NLS-1$
                "Uses HTTP instead of HTTPS (the default) for downloads.", false);

        define(Mode.STRING, false,
                VERB_LIST, OBJECT_SDK, "", KEY_PROXY_PORT,                          //$NON-NLS-1$
                "HTTP/HTTPS proxy port (overrides settings if defined)",
                null);

        define(Mode.STRING, false,
                VERB_LIST, OBJECT_SDK, "", KEY_PROXY_HOST,                          //$NON-NLS-1$
                "HTTP/HTTPS proxy host (overrides settings if defined)",
                null);

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_SDK, "a", KEY_ALL,                                //$NON-NLS-1$
                "Lists all available packages (including obsolete and installed ones)",
                false);

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_SDK, "o", "obsolete",                             //$NON-NLS-1$
                "Deprecated. Please use --all instead.",
                false);

        define(Mode.BOOLEAN, false,
                VERB_LIST, OBJECT_SDK, "e", KEY_EXTENDED,                           //$NON-NLS-1$
                "Displays extended details on each package",
                false);

        // --- update sdk ---

        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_SDK, "u", KEY_NO_UI,                            //$NON-NLS-1$
                "Updates from command-line (does not display the GUI)", false);

        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_SDK, "s", KEY_NO_HTTPS,                         //$NON-NLS-1$
                "Uses HTTP instead of HTTPS (the default) for downloads.", false);

        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_SDK, "", KEY_PROXY_PORT,                        //$NON-NLS-1$
                "HTTP/HTTPS proxy port (overrides settings if defined)",
                null);

        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_SDK, "", KEY_PROXY_HOST,                        //$NON-NLS-1$
                "HTTP/HTTPS proxy host (overrides settings if defined)",
                null);

        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_SDK, "f", KEY_FORCE,                            //$NON-NLS-1$
                "Forces replacement of a package or its parts, even if something has been modified.",
                false);

        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_SDK, "t", KEY_FILTER,                           //$NON-NLS-1$
                "A filter that limits the update to the specified types of packages in the form of a comma-separated list of " +
                Arrays.toString(SdkRepoConstants.NODES) +
                ". This also accepts the identifiers returned by 'list sdk --extended'.",
                null);

        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_SDK, "a", KEY_ALL,                              //$NON-NLS-1$
                "Includes all packages (such as obsolete and non-dependent ones.)",
                false);

        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_SDK, "p", "obsolete",                             //$NON-NLS-1$
                "Deprecated. Please use --all instead.",
                false);

        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_SDK, "n", KEY_DRY_MODE,                         //$NON-NLS-1$
                "Simulates the update but does not download or install anything.",
                false);

        // --- create project ---

        /* Disabled for ADT 0.9 / Cupcake SDK 1.5_r1 release. [bug #1795718].
           This currently does not work, the alias build rules need to be fixed.

        define(Mode.ENUM, true,
                VERB_CREATE, OBJECT_PROJECT, "m", KEY_MODE,                         //$NON-NLS-1$
                "Project mode", new String[] { ARG_ACTIVITY, ARG_ALIAS });
        */
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT,
                "p", KEY_PATH,
                "The new project's directory.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT, "t", KEY_TARGET_ID,                    //$NON-NLS-1$
                "Target ID of the new project.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT, "k", KEY_PACKAGE,                      //$NON-NLS-1$
                "Android package name for the application.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT, "a", KEY_ACTIVITY,                     //$NON-NLS-1$
                "Name of the default Activity that is created.", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_PROJECT, "n", KEY_NAME,                         //$NON-NLS-1$
                "Project name.", null);

        // --- create test-project ---

        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_TEST_PROJECT, "p", KEY_PATH,                    //$NON-NLS-1$
                "The new project's directory.", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_TEST_PROJECT, "n", KEY_NAME,                    //$NON-NLS-1$
                "Project name.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_TEST_PROJECT, "m", KEY_MAIN_PROJECT,            //$NON-NLS-1$
                "Path to directory of the app under test, relative to the test project directory.",
                null);

        // --- create lib-project ---

        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_LIB_PROJECT, "p", KEY_PATH,                     //$NON-NLS-1$
                "The new project's directory.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_LIB_PROJECT, "t", KEY_TARGET_ID,                //$NON-NLS-1$
                "Target ID of the new project.", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_LIB_PROJECT, "n", KEY_NAME,                     //$NON-NLS-1$
                "Project name.", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_LIB_PROJECT, "k", KEY_PACKAGE,                  //$NON-NLS-1$
                "Android package name for the library.", null);

        // --- update project ---

        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_PROJECT, "p", KEY_PATH,                         //$NON-NLS-1$
                "The project's directory.", null);
        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_PROJECT, "t", KEY_TARGET_ID,                    //$NON-NLS-1$
                "Target ID to set for the project.", null);
        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_PROJECT, "n", KEY_NAME,                         //$NON-NLS-1$
                "Project name.", null);
        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_PROJECT, "s", KEY_SUBPROJECTS,                  //$NON-NLS-1$
                "Also updates any projects in sub-folders, such as test projects.", false);
        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_PROJECT, "l", KEY_LIBRARY,                      //$NON-NLS-1$
                "Directory of an Android library to add, relative to this project's directory.",
                null);

        // --- update test project ---

        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_TEST_PROJECT, "p", KEY_PATH,                    //$NON-NLS-1$
                "The project's directory.", null);
        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_TEST_PROJECT, "m", KEY_MAIN_PROJECT,            //$NON-NLS-1$
                "Directory of the app under test, relative to the test project directory.", null);

        // --- update lib project ---

        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_LIB_PROJECT, "p", KEY_PATH,                     //$NON-NLS-1$
                "The project's directory.", null);
        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_LIB_PROJECT, "t", KEY_TARGET_ID,                //$NON-NLS-1$
                "Target ID to set for the project.", null);

    }

    @Override
    public boolean acceptLackOfVerb() {
        return true;
    }

    // -- some helpers for generic action flags

    /** Helper that returns true if --verbose was requested. */
    public boolean hasClearCache() {
        return
            ((Boolean) getValue(GLOBAL_FLAG_VERB, NO_VERB_OBJECT, KEY_CLEAR_CACHE)).booleanValue();
    }

    /** Helper to retrieve the --path value. */
    public String getParamLocationPath() {
        return (String) getValue(null, null, KEY_PATH);
    }

    /**
     * Helper to retrieve the --target id value.
     * The id is a string. It can be one of:
     * - an integer, in which case it's the index of the target (cf "android list targets")
     * - a symbolic name such as android-N for platforn API N
     * - a symbolic add-on name such as written in the avd/*.ini files,
     *   e.g. "Google Inc.:Google APIs:3"
     */
    public String getParamTargetId() {
        return (String) getValue(null, null, KEY_TARGET_ID);
    }

    /** Helper to retrieve the --name value. */
    public String getParamName() {
        return (String) getValue(null, null, KEY_NAME);
    }

    /** Helper to retrieve the --skin value. */
    public String getParamSkin() {
        return (String) getValue(null, null, KEY_SKIN);
    }

    /** Helper to retrieve the --sdcard value. */
    public String getParamSdCard() {
        return (String) getValue(null, null, KEY_SDCARD);
    }

    /** Helper to retrieve the --force flag. */
    public boolean getFlagForce() {
        return ((Boolean) getValue(null, null, KEY_FORCE)).booleanValue();
    }

    /** Helper to retrieve the --snapshot flag. */
    public boolean getFlagSnapshot() {
        return ((Boolean) getValue(null, null, KEY_SNAPSHOT)).booleanValue();
    }

    // -- some helpers for avd action flags

    /** Helper to retrieve the --rename value for a move verb. */
    public String getParamMoveNewName() {
        return (String) getValue(VERB_MOVE, null, KEY_RENAME);
    }


    // -- some helpers for project action flags

    /** Helper to retrieve the --package value.
     * @param directObject the directObject of the action, either {@link #OBJECT_PROJECT}
     * or {@link #OBJECT_LIB_PROJECT}.
     */
    public String getParamProjectPackage(String directObject) {
        return ((String) getValue(null, directObject, KEY_PACKAGE));
    }

    /** Helper to retrieve the --activity for any project action. */
    public String getParamProjectActivity() {
        return ((String) getValue(null, OBJECT_PROJECT, KEY_ACTIVITY));
    }

    /** Helper to retrieve the --library value.
     * @param directObject the directObject of the action, either {@link #OBJECT_PROJECT}
     * or {@link #OBJECT_LIB_PROJECT}.
     */
    public String getParamProjectLibrary(String directObject) {
        return ((String) getValue(null, directObject, KEY_LIBRARY));
    }


    /** Helper to retrieve the --subprojects for any project action. */
    public boolean getParamSubProject() {
        return ((Boolean) getValue(null, OBJECT_PROJECT, KEY_SUBPROJECTS)).booleanValue();
    }

    // -- some helpers for test-project action flags

    /** Helper to retrieve the --main value. */
    public String getParamTestProjectMain() {
        return ((String) getValue(null, null, KEY_MAIN_PROJECT));
    }


    // -- some helpers for update sdk flags

    /** Helper to retrieve the --no-ui flag. */
    public boolean getFlagNoUI(String verb) {
        return ((Boolean) getValue(verb, null, KEY_NO_UI)).booleanValue();
    }

    /** Helper to retrieve the --no-https flag. */
    public boolean getFlagNoHttps() {
        return ((Boolean) getValue(null, null, KEY_NO_HTTPS)).booleanValue();
    }

    /** Helper to retrieve the --dry-mode flag. */
    public boolean getFlagDryMode() {
        return ((Boolean) getValue(null, null, KEY_DRY_MODE)).booleanValue();
    }

    /** Helper to retrieve the --obsolete flag. */
    public boolean getFlagObsolete() {
        return ((Boolean) getValue(null, null, "obsolete")).booleanValue();
    }

    /** Helper to retrieve the --all flag. */
    public boolean getFlagAll() {
        return ((Boolean) getValue(null, null, KEY_ALL)).booleanValue();
    }

    /** Helper to retrieve the --extended flag. */
    public boolean getFlagExtended() {
        return ((Boolean) getValue(null, null, KEY_EXTENDED)).booleanValue();
    }

    /** Helper to retrieve the --filter value. */
    public String getParamFilter() {
        return ((String) getValue(null, null, KEY_FILTER));
    }

    /** Helper to retrieve the --abi value. */
    public String getParamAbi() {
        return ((String) getValue(null, null, KEY_ABI));
    }

    /** Helper to retrieve the --proxy-host value. */
    public String getParamProxyHost() {
        return ((String) getValue(null, null, KEY_PROXY_HOST));
    }

    /** Helper to retrieve the --proxy-port value. */
    public String getParamProxyPort() {
        return ((String) getValue(null, null, KEY_PROXY_PORT));
    }

    // -- some helpers for list avds and list targets flags

    /** Helper to retrieve the --compact value. */
    public boolean getFlagCompact() {
        return ((Boolean) getValue(null, null, KEY_COMPACT)).booleanValue();
    }

    /** Helper to retrieve the --null value. */
    public boolean getFlagEolNull() {
        return ((Boolean) getValue(null, null, KEY_EOL_NULL)).booleanValue();
    }
}
