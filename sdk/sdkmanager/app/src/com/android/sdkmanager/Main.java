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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.io.FileWrapper;
import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.SdkManager;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.HardwareProperties;
import com.android.sdklib.internal.avd.HardwareProperties.HardwareProperty;
import com.android.sdklib.internal.project.ProjectCreator;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectCreator.OutputLevel;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;
import com.android.sdklib.internal.repository.DownloadCache;
import com.android.sdklib.internal.repository.DownloadCache.Strategy;
import com.android.sdklib.internal.repository.packages.PlatformToolPackage;
import com.android.sdklib.internal.repository.packages.ToolPackage;
import com.android.sdklib.repository.SdkAddonConstants;
import com.android.sdklib.repository.SdkRepoConstants;
import com.android.sdkuilib.internal.repository.SdkUpdaterNoWindow;
import com.android.sdkuilib.internal.widgets.MessageBoxLog;
import com.android.sdkuilib.repository.AvdManagerWindow;
import com.android.sdkuilib.repository.SdkUpdaterWindow;
import com.android.sdkuilib.repository.AvdManagerWindow.AvdInvocationContext;
import com.android.sdkuilib.repository.SdkUpdaterWindow.SdkInvocationContext;
import com.android.utils.ILogger;
import com.android.utils.Pair;
import com.android.xml.AndroidXPathFactory;

import org.eclipse.swt.widgets.Display;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

/**
 * Main class for the 'android' application.
 */
public class Main {

    /** Java property that defines the location of the sdk/tools directory. */
    public final static String TOOLSDIR = "com.android.sdkmanager.toolsdir";
    /** Java property that defines the working directory. On Windows the current working directory
     *  is actually the tools dir, in which case this is used to get the original CWD. */
    private final static String WORKDIR = "com.android.sdkmanager.workdir";

    /** Value returned by {@link #resolveTargetName(String)} when the target id does not match. */
    private final static int INVALID_TARGET_ID = 0;

    private final static String[] BOOLEAN_YES_REPLIES = new String[] { "yes", "y" };
    private final static String[] BOOLEAN_NO_REPLIES  = new String[] { "no",  "n" };

    /** Path to the SDK folder. This is the parent of {@link #TOOLSDIR}. */
    private String mOsSdkFolder;
    /** Logger object. Use this to print normal output, warnings or errors. */
    private ILogger mSdkLog;
    /** The SDK manager parses the SDK folder and gives access to the content. */
    private SdkManager mSdkManager;
    /** Command-line processor with options specific to SdkManager. */
    private SdkCommandLine mSdkCommandLine;
    /** The working directory, either null or set to an existing absolute canonical directory. */
    private File mWorkDir;

    public static void main(String[] args) {
        new Main().run(args);
    }

    /** Used by tests to set the sdk manager. */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    void setSdkManager(SdkManager sdkManager) {
        mSdkManager = sdkManager;
    }

    /**
     * Runs the sdk manager app
     */
    private void run(String[] args) {
        createLogger();
        init();
        mSdkCommandLine.parseArgs(args);
        parseSdk();
        doAction();
    }

    /**
     * Creates the {@link #mSdkLog} object.
     * This must be done before {@link #init()} as it will be used to report errors.
     * This logger prints to the attached console.
     */
    private void createLogger() {
        mSdkLog = new ILogger() {
            @Override
            public void error(Throwable t, String errorFormat, Object... args) {
                if (errorFormat != null) {
                    System.err.printf("Error: " + errorFormat, args);
                    if (!errorFormat.endsWith("\n")) {
                        System.err.printf("\n");
                    }
                }
                if (t != null) {
                    System.err.printf("Error: %s\n", t.getMessage());
                }
            }

            @Override
            public void warning(@NonNull String warningFormat, Object... args) {
                if (mSdkCommandLine.isVerbose()) {
                    System.out.printf("Warning: " + warningFormat, args);
                    if (!warningFormat.endsWith("\n")) {
                        System.out.printf("\n");
                    }
                }
            }

            @Override
            public void info(@NonNull String msgFormat, Object... args) {
                System.out.printf(msgFormat, args);
            }

            @Override
            public void verbose(@NonNull String msgFormat, Object... args) {
                System.out.printf(msgFormat, args);
            }
        };
    }

    /** For testing */
    public void setLogger(ILogger logger) {
        mSdkLog = logger;
    }

    /**
     * Init the application by making sure the SDK path is available and
     * doing basic parsing of the SDK.
     */
    private void init() {
        mSdkCommandLine = new SdkCommandLine(mSdkLog);

        // We get passed a property for the tools dir
        String toolsDirProp = System.getProperty(TOOLSDIR);
        if (toolsDirProp == null) {
            // for debugging, it's easier to override using the process environment
            toolsDirProp = System.getenv(TOOLSDIR);
        }

        if (toolsDirProp != null) {
            // got back a level for the SDK folder
            File tools;
            if (toolsDirProp.length() > 0) {
                tools = new File(toolsDirProp);
                mOsSdkFolder = tools.getParent();
            } else {
                try {
                    tools = new File(".").getCanonicalFile();
                    mOsSdkFolder = tools.getParent();
                } catch (IOException e) {
                    // Will print an error below since mSdkFolder is not defined
                }
            }
        }

        if (mOsSdkFolder == null) {
            errorAndExit("The tools directory property is not set, please make sure you are executing %1$s",
                SdkConstants.androidCmdName());
        }

        // We might get passed a property for the working directory
        // Either it is a valid directory and mWorkDir is set to it's absolute canonical value
        // or mWorkDir remains null.
        String workDirProp = System.getProperty(WORKDIR);
        if (workDirProp == null) {
            workDirProp = System.getenv(WORKDIR);
        }
        if (workDirProp != null) {
            // This should be a valid directory
            mWorkDir = new File(workDirProp);
            try {
                mWorkDir = mWorkDir.getCanonicalFile().getAbsoluteFile();
            } catch (IOException e) {
                mWorkDir = null;
            }
            if (mWorkDir == null || !mWorkDir.isDirectory()) {
                errorAndExit("The working directory does not seem to be valid: '%1$s", workDirProp);
            }
        }
    }

    /**
     * Does the basic SDK parsing required for all actions
     */
    private void parseSdk() {
        mSdkManager = SdkManager.createManager(mOsSdkFolder, mSdkLog);

        if (mSdkManager == null) {
            errorAndExit("Unable to parse SDK content.");
        }
    }

    /**
     * Actually do an action...
     */
    private void doAction() {

        if (mSdkCommandLine.hasClearCache()) {
            DownloadCache d = new DownloadCache(Strategy.SERVE_CACHE);
            d.clearCache();
            mSdkLog.info("SDK Manager repository: manifest cache cleared.\n");
        }

        String verb = mSdkCommandLine.getVerb();
        String directObject = mSdkCommandLine.getDirectObject();

        if (SdkCommandLine.VERB_LIST.equals(verb)) {
            // list action.
            if (SdkCommandLine.OBJECT_TARGET.equals(directObject)) {
                displayTargetList();

            } else if (SdkCommandLine.OBJECT_AVD.equals(directObject)) {
                displayAvdList();

            } else if (SdkCommandLine.OBJECT_SDK.equals(directObject)) {
                displayRemoteSdkListNoUI();

            } else {
                displayTargetList();
                displayAvdList();
            }

        } else if (SdkCommandLine.VERB_CREATE.equals(verb)) {
            if (SdkCommandLine.OBJECT_AVD.equals(directObject)) {
                createAvd();

            } else if (SdkCommandLine.OBJECT_PROJECT.equals(directObject)) {
                createProject(false /*library*/);

            } else if (SdkCommandLine.OBJECT_TEST_PROJECT.equals(directObject)) {
                createTestProject();

            } else if (SdkCommandLine.OBJECT_LIB_PROJECT.equals(directObject)) {
                createProject(true /*library*/);

            }
        } else if (SdkCommandLine.VERB_UPDATE.equals(verb)) {
            if (SdkCommandLine.OBJECT_AVD.equals(directObject)) {
                updateAvd();

            } else if (SdkCommandLine.OBJECT_PROJECT.equals(directObject)) {
                updateProject(false /*library*/);

            } else if (SdkCommandLine.OBJECT_TEST_PROJECT.equals(directObject)) {
                updateTestProject();

            } else if (SdkCommandLine.OBJECT_LIB_PROJECT.equals(directObject)) {
                updateProject(true /*library*/);

            } else if (SdkCommandLine.OBJECT_SDK.equals(directObject)) {
                if (mSdkCommandLine.getFlagNoUI(verb)) {
                    updateSdkNoUI();
                } else {
                    showSdkManagerWindow();
                }

            } else if (SdkCommandLine.OBJECT_ADB.equals(directObject)) {
                updateAdb();

            }
        } else if (SdkCommandLine.VERB_SDK.equals(verb)) {
            showSdkManagerWindow();

        } else if (SdkCommandLine.VERB_AVD.equals(verb)) {
            showAvdManagerWindow();

        } else if (SdkCommandLine.VERB_DELETE.equals(verb) &&
                SdkCommandLine.OBJECT_AVD.equals(directObject)) {
            deleteAvd();

        } else if (SdkCommandLine.VERB_MOVE.equals(verb) &&
                SdkCommandLine.OBJECT_AVD.equals(directObject)) {
            moveAvd();

        } else if (verb == null && directObject == null) {
            showSdkManagerWindow();

        } else {
            mSdkCommandLine.printHelpAndExit(null);
        }
    }

    /**
     * Display the main SDK Manager app window
     */
    private void showSdkManagerWindow() {
        try {
            MessageBoxLog errorLogger = new MessageBoxLog(
                    "SDK Manager",
                    Display.getCurrent(),
                    true /*logErrorsOnly*/);

            SdkUpdaterWindow window = new SdkUpdaterWindow(
                    null /* parentShell */,
                    errorLogger,
                    mOsSdkFolder,
                    SdkInvocationContext.STANDALONE);
            window.open();

            errorLogger.displayResult(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Display the main AVD Manager app window
     */
    private void showAvdManagerWindow() {
        try {
            MessageBoxLog errorLogger = new MessageBoxLog(
                    "AVD Manager",
                    Display.getCurrent(),
                    true /*logErrorsOnly*/);

            AvdManagerWindow window = new AvdManagerWindow(
                    null /* parentShell */,
                    errorLogger,
                    mOsSdkFolder,
                    AvdInvocationContext.STANDALONE);

            window.open();

            errorLogger.displayResult(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayRemoteSdkListNoUI() {
        boolean force    = mSdkCommandLine.getFlagForce();
        boolean useHttp  = mSdkCommandLine.getFlagNoHttps();
        boolean all      = mSdkCommandLine.getFlagAll();
        boolean extended = mSdkCommandLine.getFlagExtended();
        String proxyHost = mSdkCommandLine.getParamProxyHost();
        String proxyPort = mSdkCommandLine.getParamProxyPort();

        boolean obsolete = mSdkCommandLine.getFlagObsolete();
        all |= obsolete;

        SdkUpdaterNoWindow upd = new SdkUpdaterNoWindow(
                mOsSdkFolder,
                mSdkManager,
                mSdkLog,
                force,
                useHttp,
                proxyHost,
                proxyPort);
        upd.listRemotePackages(all, extended);

        if (obsolete) {
            mSdkLog.info("Note: Flag --obsolete is deprecated and will be removed in the next version.\n      Please use --all instead.\n");
        }
    }

    /**
     * Updates the whole SDK without any UI, just using console output.
     */
    private void updateSdkNoUI() {
        boolean force    = mSdkCommandLine.getFlagForce();
        boolean useHttp  = mSdkCommandLine.getFlagNoHttps();
        boolean dryMode  = mSdkCommandLine.getFlagDryMode();
        boolean all      = mSdkCommandLine.getFlagAll();
        String proxyHost = mSdkCommandLine.getParamProxyHost();
        String proxyPort = mSdkCommandLine.getParamProxyPort();

        boolean obsolete = mSdkCommandLine.getFlagObsolete();
        all |= obsolete;

        // Check filter types.
        Pair<String, ArrayList<String>> filterResult =
            checkFilterValues(mSdkCommandLine.getParamFilter());
        if (filterResult.getFirst() != null) {
            // We got an error.
            errorAndExit(filterResult.getFirst());
        }

        SdkUpdaterNoWindow upd = new SdkUpdaterNoWindow(
                mOsSdkFolder,
                mSdkManager,
                mSdkLog,
                force,
                useHttp,
                proxyHost,
                proxyPort);
        upd.updateAll(filterResult.getSecond(), all, dryMode);

        if (obsolete) {
            mSdkLog.info("Note: Flag --obsolete is deprecated and will be removed in the next version.\n      Please use --all instead.\n");
        }
    }

    /**
     * Checks the values from the filter parameter and returns a tuple
     * (error , accepted values). Either error is null and accepted values is not,
     * or the reverse.
     * <p/>
     * Note that this is a quick sanity check of the --filter parameter *before* we
     * start loading the remote repository sources. Loading the remotes takes a while
     * so it's worth doing a quick sanity check before hand.
     *
     * @param filter A comma-separated list of keywords
     * @return A pair <error string, usable values>, only one must be null and the other non-null.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    Pair<String, ArrayList<String>> checkFilterValues(String filter) {
        ArrayList<String> pkgFilter = new ArrayList<String>();

        if (filter != null && filter.length() > 0) {
            // Available types
            Set<String> filterTypes = new TreeSet<String>();
            filterTypes.addAll(Arrays.asList(SdkRepoConstants.NODES));
            filterTypes.addAll(Arrays.asList(SdkAddonConstants.NODES));

            for (String t : filter.split(",")) {    //$NON-NLS-1$
                if (t == null) {
                    continue;
                }
                t = t.trim();
                if (t.length() <= 0) {
                    continue;
                }

                if (t.indexOf('-') > 0 ||
                        t.equals(ToolPackage.INSTALL_ID) ||
                        t.equals(PlatformToolPackage.INSTALL_ID)) {
                    // Heuristic: if the filter name contains a dash, it is probably
                    // a variable package install id. Since we haven't loaded the remote
                    // repositories we can't validate it yet, so just accept it.
                    pkgFilter.add(t);
                    continue;
                }

                if (t.replaceAll("[0-9]+", "").length() == 0) { //$NON-NLS-1$ //$NON-NLS-2$
                    // If the filter argument *only* contains digits, accept it.
                    // It's probably an index for the remote repository list,
                    // which we can't validate yet.
                    pkgFilter.add(t);
                    continue;
                }

                if (filterTypes.contains(t)) {
                    pkgFilter.add(t);
                    continue;
                }

                return Pair.of(
                    String.format(
                       "Unknown package filter type '%1$s'.\nAccepted values are: %2$s",
                       t,
                       Arrays.toString(filterTypes.toArray())),
                    null);
            }
        }

        return Pair.of(null, pkgFilter);
    }

    /**
     * Returns a configured {@link ProjectCreator} instance.
     */
    private ProjectCreator getProjectCreator() {
        ProjectCreator creator = new ProjectCreator(mSdkManager, mOsSdkFolder,
                mSdkCommandLine.isVerbose() ? OutputLevel.VERBOSE :
                    mSdkCommandLine.isSilent() ? OutputLevel.SILENT :
                        OutputLevel.NORMAL,
                mSdkLog);
        return creator;
    }


    /**
     * Creates a new Android project based on command-line parameters
     */
    private void createProject(boolean library) {
        String directObject = library? SdkCommandLine.OBJECT_LIB_PROJECT :
                SdkCommandLine.OBJECT_PROJECT;

        // get the target and try to resolve it.
        int targetId = resolveTargetName(mSdkCommandLine.getParamTargetId());
        IAndroidTarget[] targets = mSdkManager.getTargets();
        if (targetId == INVALID_TARGET_ID || targetId > targets.length) {
            errorAndExit("Target id is not valid. Use '%s list targets' to get the target ids.",
                    SdkConstants.androidCmdName());
        }
        IAndroidTarget target = targets[targetId - 1];  // target id is 1-based

        ProjectCreator creator = getProjectCreator();

        String projectDir = getProjectLocation(mSdkCommandLine.getParamLocationPath());

        String projectName = mSdkCommandLine.getParamName();
        String packageName = mSdkCommandLine.getParamProjectPackage(directObject);
        String activityName = null;
        if (library == false) {
            activityName = mSdkCommandLine.getParamProjectActivity();
        }

        if (projectName != null &&
                !ProjectCreator.RE_PROJECT_NAME.matcher(projectName).matches()) {
            errorAndExit(
                "Project name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                projectName, ProjectCreator.CHARS_PROJECT_NAME);
            return;
        }

        if (activityName != null &&
                !ProjectCreator.RE_ACTIVITY_NAME.matcher(activityName).matches()) {
            errorAndExit(
                "Activity name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                activityName, ProjectCreator.CHARS_ACTIVITY_NAME);
            return;
        }

        if (packageName != null &&
                !ProjectCreator.RE_PACKAGE_NAME.matcher(packageName).matches()) {
            errorAndExit(
                "Package name '%1$s' contains invalid characters.\n" +
                "A package name must be constitued of two Java identifiers.\n" +
                "Each identifier allowed characters are: %2$s",
                packageName, ProjectCreator.CHARS_PACKAGE_NAME);
            return;
        }

        creator.createProject(projectDir,
                projectName,
                packageName,
                activityName,
                target,
                library,
                null /*pathToMain*/);
    }

    /**
     * Creates a new Android test project based on command-line parameters
     */
    private void createTestProject() {

        String projectDir = getProjectLocation(mSdkCommandLine.getParamLocationPath());

        // first check the path of the parent project, and make sure it's valid.
        String pathToMainProject = mSdkCommandLine.getParamTestProjectMain();

        File parentProject = new File(pathToMainProject);
        if (parentProject.isAbsolute() == false) {
            // if the path is not absolute, we need to resolve it based on the
            // destination path of the project
            try {
                parentProject = new File(projectDir, pathToMainProject).getCanonicalFile();
            } catch (IOException e) {
                errorAndExit("Unable to resolve Main project's directory: %1$s",
                        pathToMainProject);
                return; // help Eclipse static analyzer understand we'll never execute the rest.
            }
        }

        if (parentProject.isDirectory() == false) {
            errorAndExit("Main project's directory does not exist: %1$s",
                    pathToMainProject);
            return;
        }

        // now look for a manifest in there
        File manifest = new File(parentProject, SdkConstants.FN_ANDROID_MANIFEST_XML);
        if (manifest.isFile() == false) {
            errorAndExit("No AndroidManifest.xml file found in the main project directory: %1$s",
                    parentProject.getAbsolutePath());
            return;
        }

        // now query the manifest for the package file.
        XPath xpath = AndroidXPathFactory.newXPath();
        String packageName, activityName;

        try {
            packageName = xpath.evaluate("/manifest/@package",
                    new InputSource(new FileInputStream(manifest)));

            mSdkLog.info("Found main project package: %1$s\n", packageName);

            // now get the name of the first activity we find
            activityName = xpath.evaluate("/manifest/application/activity[1]/@android:name",
                    new InputSource(new FileInputStream(manifest)));
            // xpath will return empty string when there's no match
            if (activityName == null || activityName.length() == 0) {
                activityName = null;
            } else {
                mSdkLog.info("Found main project activity: %1$s\n", activityName);
            }
        } catch (FileNotFoundException e) {
            // this shouldn't happen as we test it above.
            errorAndExit("No AndroidManifest.xml file found in main project.");
            return; // this is not strictly needed because errorAndExit will stop the execution,
            // but this makes the java compiler happy, wrt to uninitialized variables.
        } catch (XPathExpressionException e) {
            // looks like the main manifest is not valid.
            errorAndExit("Unable to parse main project manifest to get information.");
            return; // this is not strictly needed because errorAndExit will stop the execution,
                    // but this makes the java compiler happy, wrt to uninitialized variables.
        }

        // now get the target hash
        ProjectProperties p = ProjectProperties.load(parentProject.getAbsolutePath(),
                PropertyType.PROJECT);
        if (p == null) {
            errorAndExit("Unable to load the main project's %1$s",
                    PropertyType.PROJECT.getFilename());
            return;
        }

        String targetHash = p.getProperty(ProjectProperties.PROPERTY_TARGET);
        if (targetHash == null) {
            errorAndExit("Couldn't find the main project target");
            return;
        }

        // and resolve it.
        IAndroidTarget target = mSdkManager.getTargetFromHashString(targetHash);
        if (target == null) {
            errorAndExit(
                    "Unable to resolve main project target '%1$s'. You may want to install the platform in your SDK.",
                    targetHash);
            return;
        }

        mSdkLog.info("Found main project target: %1$s\n", target.getFullName());

        ProjectCreator creator = getProjectCreator();

        String projectName = mSdkCommandLine.getParamName();

        if (projectName != null &&
                !ProjectCreator.RE_PROJECT_NAME.matcher(projectName).matches()) {
            errorAndExit(
                "Project name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                projectName, ProjectCreator.CHARS_PROJECT_NAME);
            return;
        }

        creator.createProject(projectDir,
                projectName,
                packageName,
                activityName,
                target,
                false /* library*/,
                pathToMainProject);
    }

    /**
     * Updates an existing Android project based on command-line parameters
     * @param library whether the project is a library project.
     */
    private void updateProject(boolean library) {
        // get the target and try to resolve it.
        IAndroidTarget target = null;
        String targetStr = mSdkCommandLine.getParamTargetId();
        // For "update project" the target parameter is optional so having null is acceptable.
        // However if there's a value, it must be valid.
        if (targetStr != null) {
            IAndroidTarget[] targets = mSdkManager.getTargets();
            int targetId = resolveTargetName(targetStr);
            if (targetId == INVALID_TARGET_ID || targetId > targets.length) {
                errorAndExit("Target id '%1$s' is not valid. Use '%2$s list targets' to get the target ids.",
                        targetStr,
                        SdkConstants.androidCmdName());
            }
            target = targets[targetId - 1];  // target id is 1-based
        }

        ProjectCreator creator = getProjectCreator();

        String projectDir = getProjectLocation(mSdkCommandLine.getParamLocationPath());

        String libraryPath = library ? null :
            mSdkCommandLine.getParamProjectLibrary(SdkCommandLine.OBJECT_PROJECT);

        creator.updateProject(projectDir,
                target,
                mSdkCommandLine.getParamName(),
                libraryPath);

        if (library == false) {
            boolean doSubProjects = mSdkCommandLine.getParamSubProject();
            boolean couldHaveDone = false;

            // If there are any sub-folders with a manifest, try to update them as projects
            // too. This will take care of updating any underlying test project even if the
            // user changed the folder name.
            File[] files = new File(projectDir).listFiles();
            if (files != null) {
                for (File dir : files) {
                    if (dir.isDirectory() &&
                            new File(dir, SdkConstants.FN_ANDROID_MANIFEST_XML).isFile()) {
                        if (doSubProjects) {
                            creator.updateProject(dir.getPath(),
                                    target,
                                    mSdkCommandLine.getParamName(),
                                    null /*libraryPath*/);
                        } else {
                            couldHaveDone = true;
                        }
                    }
                }
            }

            if (couldHaveDone) {
                mSdkLog.info(
                        "It seems that there are sub-projects. If you want to update them\nplease use the --%1$s parameter.\n",
                        SdkCommandLine.KEY_SUBPROJECTS);
            }
        }
    }

    /**
     * Updates an existing test project with a new path to the main project.
     */
    private void updateTestProject() {
        ProjectCreator creator = getProjectCreator();

        String projectDir = getProjectLocation(mSdkCommandLine.getParamLocationPath());

        creator.updateTestProject(projectDir, mSdkCommandLine.getParamTestProjectMain(),
                mSdkManager);
    }

    /**
     * Adjusts the project location to make it absolute & canonical relative to the
     * working directory, if any.
     *
     * @return The project absolute path relative to {@link #mWorkDir} or the original
     *         newProjectLocation otherwise.
     */
    private String getProjectLocation(String newProjectLocation) {

        // If the new project location is absolute, use it as-is
        File projectDir = new File(newProjectLocation);
        if (projectDir.isAbsolute()) {
            return newProjectLocation;
        }

        // if there's no working directory, just use the project location as-is.
        if (mWorkDir == null) {
            return newProjectLocation;
        }

        // Combine then and get an absolute canonical directory
        try {
            projectDir = new File(mWorkDir, newProjectLocation).getCanonicalFile();

            return projectDir.getPath();
        } catch (IOException e) {
            errorAndExit("Failed to combine working directory '%1$s' with project location '%2$s': %3$s",
                    mWorkDir.getPath(),
                    newProjectLocation,
                    e.getMessage());
            return null;
        }
    }

    /**
     * Displays the list of available Targets (Platforms and Add-ons)
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    void displayTargetList() {

        // Compact output, suitable for scripts.
        if (mSdkCommandLine != null && mSdkCommandLine.getFlagCompact()) {
            char eol = mSdkCommandLine.getFlagEolNull() ? '\0' : '\n';

            for (IAndroidTarget target : mSdkManager.getTargets()) {
                mSdkLog.info("%1$s%2$c", target.hashString(), eol);
            }

            return;
        }

        mSdkLog.info("Available Android targets:\n");

        int index = 1;
        for (IAndroidTarget target : mSdkManager.getTargets()) {
            mSdkLog.info("----------\n");
            mSdkLog.info("id: %1$d or \"%2$s\"\n", index, target.hashString());
            mSdkLog.info("     Name: %s\n", target.getName());
            if (target.isPlatform()) {
                mSdkLog.info("     Type: Platform\n");
                mSdkLog.info("     API level: %s\n", target.getVersion().getApiString());
                mSdkLog.info("     Revision: %d\n", target.getRevision());
            } else {
                mSdkLog.info("     Type: Add-On\n");
                mSdkLog.info("     Vendor: %s\n", target.getVendor());
                mSdkLog.info("     Revision: %d\n", target.getRevision());
                if (target.getDescription() != null) {
                    mSdkLog.info("     Description: %s\n", target.getDescription());
                }
                mSdkLog.info("     Based on Android %s (API level %s)\n",
                        target.getVersionName(), target.getVersion().getApiString());

                // display the optional libraries.
                IOptionalLibrary[] libraries = target.getOptionalLibraries();
                if (libraries != null) {
                    mSdkLog.info("     Libraries:\n");
                    for (IOptionalLibrary library : libraries) {
                        mSdkLog.info("      * %1$s (%2$s)\n",
                                library.getName(), library.getJarName());
                        mSdkLog.info("          %1$s\n", library.getDescription());
                    }
                }
            }

            // get the target skins & ABIs
            displaySkinList(target, "     Skins: ");
            displayAbiList (target, "     ABIs : ");

            if (target.getUsbVendorId() != IAndroidTarget.NO_USB_ID) {
                mSdkLog.info("     Adds USB support for devices (Vendor: 0x%04X)\n",
                        target.getUsbVendorId());
            }

            index++;
        }
    }

    /**
     * Displays the skins valid for the given target.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    void displaySkinList(IAndroidTarget target, String message) {
        String[] skins = target.getSkins();
        String defaultSkin = target.getDefaultSkin();
        mSdkLog.info(message);
        if (skins != null) {
            boolean first = true;
            for (String skin : skins) {
                if (first == false) {
                    mSdkLog.info(", ");
                } else {
                    first = false;
                }
                mSdkLog.info(skin);

                if (skin.equals(defaultSkin)) {
                    mSdkLog.info(" (default)");
                }
            }
            mSdkLog.info("\n");
        } else {
            mSdkLog.info("no skins.\n");
        }
    }

    /**
     * Displays the ABIs valid for the given target.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    void displayAbiList(IAndroidTarget target, String message) {
        ISystemImage[] systemImages = target.getSystemImages();
        mSdkLog.info(message);
        if (systemImages.length > 0) {
            boolean first = true;
            for (ISystemImage si : systemImages) {
                if (first == false) {
                    mSdkLog.info(", ");
                } else {
                    first = false;
                }
                mSdkLog.info(si.getAbiType());
            }
            mSdkLog.info("\n");
        } else {
            mSdkLog.info("no ABIs.\n");
        }
    }

    /**
     * Displays the list of available AVDs for the given AvdManager.
     *
     * @param avdManager
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    void displayAvdList(AvdManager avdManager) {

        AvdInfo[] avds = avdManager.getValidAvds();

        // Compact output, suitable for scripts.
        if (mSdkCommandLine != null && mSdkCommandLine.getFlagCompact()) {
            char eol = mSdkCommandLine.getFlagEolNull() ? '\0' : '\n';

            for (int index = 0 ; index < avds.length ; index++) {
                AvdInfo info = avds[index];
                mSdkLog.info("%1$s%2$c", info.getName(), eol);
            }

            return;
        }

        mSdkLog.info("Available Android Virtual Devices:\n");

        for (int index = 0 ; index < avds.length ; index++) {
            AvdInfo info = avds[index];
            if (index > 0) {
                mSdkLog.info("---------\n");
            }
            mSdkLog.info("    Name: %s\n", info.getName());
            mSdkLog.info("    Path: %s\n", info.getDataFolderPath());

            // get the target of the AVD
            IAndroidTarget target = info.getTarget();
            if (target.isPlatform()) {
                mSdkLog.info("  Target: %s (API level %s)\n", target.getName(),
                        target.getVersion().getApiString());
            } else {
                mSdkLog.info("  Target: %s (%s)\n", target.getName(), target
                        .getVendor());
                mSdkLog.info("          Based on Android %s (API level %s)\n",
                        target.getVersionName(), target.getVersion().getApiString());
            }
            mSdkLog.info("     ABI: %s\n", info.getAbiType());

            // display some extra values.
            Map<String, String> properties = info.getProperties();
            if (properties != null) {
                String skin = properties.get(AvdManager.AVD_INI_SKIN_NAME);
                if (skin != null) {
                    mSdkLog.info("    Skin: %s\n", skin);
                }
                String sdcard = properties.get(AvdManager.AVD_INI_SDCARD_SIZE);
                if (sdcard == null) {
                    sdcard = properties.get(AvdManager.AVD_INI_SDCARD_PATH);
                }
                if (sdcard != null) {
                    mSdkLog.info("  Sdcard: %s\n", sdcard);
                }
                String snapshot = properties.get(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
                if (snapshot != null) {
                    mSdkLog.info("Snapshot: %s\n", snapshot);
                }
            }
        }

        // Are there some unused AVDs?
        AvdInfo[] badAvds = avdManager.getBrokenAvds();

        if (badAvds.length == 0) {
            return;
        }

        mSdkLog.info("\nThe following Android Virtual Devices could not be loaded:\n");
        boolean needSeparator = false;
        for (AvdInfo info : badAvds) {
            if (needSeparator) {
                mSdkLog.info("---------\n");
            }
            mSdkLog.info("    Name: %s\n", info.getName() == null ? "--" : info.getName());
            mSdkLog.info("    Path: %s\n",
                    info.getDataFolderPath() == null ? "--" : info.getDataFolderPath());

            String error = info.getErrorMessage();
            mSdkLog.info("   Error: %s\n", error == null ? "Uknown error" : error);
            needSeparator = true;
        }
    }

    /**
     * Displays the list of available AVDs.
     */
    private void displayAvdList() {
        try {
            AvdManager avdManager = AvdManager.getInstance(mSdkManager, mSdkLog);
            displayAvdList(avdManager);
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
        }
    }

    /**
     * Creates a new AVD. This is a text based creation with command line prompt.
     */
    private void createAvd() {
        // find a matching target
        int targetId = resolveTargetName(mSdkCommandLine.getParamTargetId());
        IAndroidTarget[] targets = mSdkManager.getTargets();

        if (targetId == INVALID_TARGET_ID || targetId > targets.length) {
            errorAndExit("Target id is not valid. Use '%s list targets' to get the target ids.",
                    SdkConstants.androidCmdName());
        }

        IAndroidTarget target = targets[targetId-1]; // target id is 1-based

        try {
            boolean removePrevious = mSdkCommandLine.getFlagForce();
            AvdManager avdManager = AvdManager.getInstance(mSdkManager, mSdkLog);

            String avdName = mSdkCommandLine.getParamName();

            if (!AvdManager.RE_AVD_NAME.matcher(avdName).matches()) {
                errorAndExit(
                    "AVD name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                    avdName, AvdManager.CHARS_AVD_NAME);
                return;
            }

            AvdInfo info = avdManager.getAvd(avdName, false /*validAvdOnly*/);
            if (info != null) {
                if (removePrevious) {
                    mSdkLog.warning(
                            "Android Virtual Device '%s' already exists and will be replaced.",
                            avdName);
                } else {
                    errorAndExit("Android Virtual Device '%s' already exists.\n" +
                                 "Use --force if you want to replace it.",
                                 avdName);
                    return;
                }
            }

            String paramFolderPath = mSdkCommandLine.getParamLocationPath();
            File avdFolder = null;
            if (paramFolderPath != null) {
                avdFolder = new File(paramFolderPath);
            } else {
                avdFolder = AvdInfo.getDefaultAvdFolder(avdManager, avdName);
            }

            // Validate skin is either default (empty) or NNNxMMM or a valid skin name.
            Map<String, String> skinHardwareConfig = null;
            String skin = mSdkCommandLine.getParamSkin();
            if (skin != null && skin.length() == 0) {
                skin = null;
            }

            if (skin != null && target != null) {
                boolean valid = false;
                // Is it a know skin name for this target?
                for (String s : target.getSkins()) {
                    if (skin.equalsIgnoreCase(s)) {
                        skin = s;  // Make skin names case-insensitive.
                        valid = true;

                        // get the hardware properties for this skin
                        File skinFolder = avdManager.getSkinPath(skin, target);
                        FileWrapper skinHardwareFile = new FileWrapper(skinFolder,
                                AvdManager.HARDWARE_INI);
                        if (skinHardwareFile.isFile()) {
                            skinHardwareConfig = ProjectProperties.parsePropertyFile(
                                    skinHardwareFile, mSdkLog);
                        }
                        break;
                    }
                }

                // Is it NNNxMMM?
                if (!valid) {
                    valid = AvdManager.NUMERIC_SKIN_SIZE.matcher(skin).matches();
                }

                if (!valid) {
                    displaySkinList(target, "Valid skins: ");
                    errorAndExit("'%s' is not a valid skin name or size (NNNxMMM)", skin);
                    return;
                }
            }

            String abiType = mSdkCommandLine.getParamAbi();
            if (target != null && (abiType == null || abiType.length() == 0)) {
                ISystemImage[] systemImages = target.getSystemImages();
                if (systemImages != null && systemImages.length == 1) {
                    // Auto-select the single ABI available
                    abiType = systemImages[0].getAbiType();
                    mSdkLog.info("Auto-selecting single ABI %1$s\n", abiType);
                } else {
                    displayAbiList(target, "Valid ABIs: ");
                    errorAndExit("This platform has more than one ABI. Please specify one using --%1$s.",
                            SdkCommandLine.KEY_ABI);
                }
            }

            Map<String, String> hardwareConfig = null;
            if (target != null && target.isPlatform()) {
                try {
                    hardwareConfig = promptForHardware(target, skinHardwareConfig);
                } catch (IOException e) {
                    errorAndExit(e.getMessage());
                }
            }

            @SuppressWarnings("unused") // oldAvdInfo is never read, yet useful for debugging
            AvdInfo oldAvdInfo = null;
            if (removePrevious) {
                oldAvdInfo = avdManager.getAvd(avdName, false /*validAvdOnly*/);
            }

            @SuppressWarnings("unused") // newAvdInfo is never read, yet useful for debugging
            AvdInfo newAvdInfo = avdManager.createAvd(avdFolder,
                    avdName,
                    target,
                    abiType,
                    skin,
                    mSdkCommandLine.getParamSdCard(),
                    hardwareConfig,
                    mSdkCommandLine.getFlagSnapshot(),
                    removePrevious,
                    false, //edit existing
                    mSdkLog);

        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
        }
    }

    /**
     * Delete an AVD. If the AVD name is not part of the available ones look for an
     * invalid AVD (one not loaded due to some error) to remove it too.
     */
    private void deleteAvd() {
        try {
            String avdName = mSdkCommandLine.getParamName();
            AvdManager avdManager = AvdManager.getInstance(mSdkManager, mSdkLog);
            AvdInfo info = avdManager.getAvd(avdName, false /*validAvdOnly*/);

            if (info == null) {
                errorAndExit("There is no Android Virtual Device named '%s'.", avdName);
                return;
            }

            avdManager.deleteAvd(info, mSdkLog);
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
        }
    }

    /**
     * Moves an AVD.
     */
    private void moveAvd() {
        try {
            String avdName = mSdkCommandLine.getParamName();
            AvdManager avdManager = AvdManager.getInstance(mSdkManager, mSdkLog);
            AvdInfo info = avdManager.getAvd(avdName, true /*validAvdOnly*/);

            if (info == null) {
                errorAndExit("There is no valid Android Virtual Device named '%s'.", avdName);
                return;
            }

            // This is a rename if there's a new name for the AVD
            String newName = mSdkCommandLine.getParamMoveNewName();
            if (newName != null && newName.equals(info.getName())) {
                // same name, not actually a rename operation
                newName = null;
            }

            // This is a move (of the data files) if there's a new location path
            String paramFolderPath = mSdkCommandLine.getParamLocationPath();
            if (paramFolderPath != null) {
                // check if paths are the same. Use File methods to account for OS idiosyncrasies.
                try {
                    File f1 = new File(paramFolderPath).getCanonicalFile();
                    File f2 = new File(info.getDataFolderPath()).getCanonicalFile();
                    if (f1.equals(f2)) {
                        // same canonical path, so not actually a move
                        paramFolderPath = null;
                    }
                } catch (IOException e) {
                    // Fail to resolve canonical path. Fail now since a move operation might fail
                    // later and be harder to recover from.
                    errorAndExit(e.getMessage());
                    return;
                }
            }

            if (newName == null && paramFolderPath == null) {
                mSdkLog.warning("Move operation aborted: same AVD name, same canonical data path");
                return;
            }

            // If a rename was requested and no data move was requested, check if the original
            // data path is our default constructed from the AVD name. In this case we still want
            // to rename that folder too.
            if (newName != null && paramFolderPath == null) {
                // Compute the original data path
                File originalFolder = new File(
                        AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD,
                        info.getName() + AvdManager.AVD_FOLDER_EXTENSION);
                if (info.getDataFolderPath() != null &&
                        originalFolder.equals(new File(info.getDataFolderPath()))) {
                    try {
                        // The AVD is using the default data folder path based on the AVD name.
                        // That folder needs to be adjusted to use the new name.
                        File f = new File(AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD,
                                     newName + AvdManager.AVD_FOLDER_EXTENSION);
                        paramFolderPath = f.getCanonicalPath();
                    } catch (IOException e) {
                        // Fail to resolve canonical path. Fail now rather than later.
                        errorAndExit(e.getMessage());
                    }
                }
            }

            // Check for conflicts
            if (newName != null) {
                if (avdManager.getAvd(newName, false /*validAvdOnly*/) != null) {
                    errorAndExit("There is already an AVD named '%s'.", newName);
                    return;
                }

                File ini = info.getIniFile();
                if (ini.equals(AvdInfo.getDefaultIniFile(avdManager, newName))) {
                    errorAndExit("The AVD file '%s' is in the way.", ini.getCanonicalPath());
                    return;
                }
            }

            if (paramFolderPath != null && new File(paramFolderPath).exists()) {
                errorAndExit(
                        "There is already a file or directory at '%s'.\nUse --path to specify a different data folder.",
                        paramFolderPath);
            }

            avdManager.moveAvd(info, newName, paramFolderPath, mSdkLog);
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
        } catch (IOException e) {
            errorAndExit(e.getMessage());
        }
    }

    /**
     * Updates a broken AVD.
     */
    private void updateAvd() {
        try {
            String avdName = mSdkCommandLine.getParamName();
            AvdManager avdManager = AvdManager.getInstance(mSdkManager, mSdkLog);
            avdManager.updateAvd(avdName, mSdkLog);
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
        } catch (IOException e) {
            errorAndExit(e.getMessage());
        }
    }

    /**
     * Updates adb with the USB devices declared in the SDK add-ons.
     */
    private void updateAdb() {
        try {
            mSdkManager.updateAdb();

            mSdkLog.info(
                    "adb has been updated. You must restart adb with the following commands\n" +
                    "\tadb kill-server\n" +
                    "\tadb start-server\n");
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
        } catch (IOException e) {
            errorAndExit(e.getMessage());
        }
    }



    /**
     * Prompts the user to setup a hardware config for a Platform-based AVD.
     * @throws IOException
     */
    private Map<String, String> promptForHardware(IAndroidTarget createTarget,
            Map<String, String> skinHardwareConfig) throws IOException {
        byte[] readLineBuffer = new byte[256];
        String result;
        String defaultAnswer = "no";

        mSdkLog.info("%s is a basic Android platform.\n", createTarget.getName());
        mSdkLog.info("Do you wish to create a custom hardware profile [%s]",
                defaultAnswer);

        result = readLine(readLineBuffer).trim();
        // handle default:
        if (result.length() == 0) {
            result = defaultAnswer;
        }

        if (getBooleanReply(result) == false) {
            // no custom config, return the skin hardware config in case there is one.
            return skinHardwareConfig;
        }

        mSdkLog.info("\n"); // empty line

        // get the list of possible hardware properties
        File hardwareDefs = new File (mOsSdkFolder + File.separator +
                SdkConstants.OS_SDK_TOOLS_LIB_FOLDER, SdkConstants.FN_HARDWARE_INI);
        Map<String, HardwareProperty> hwMap = HardwareProperties.parseHardwareDefinitions(
                hardwareDefs, null /*sdkLog*/);

        HashMap<String, String> map = new HashMap<String, String>();

        // we just want to loop on the HardwareProperties
        HardwareProperty[] hwProperties = hwMap.values().toArray(
                new HardwareProperty[hwMap.size()]);
        for (int i = 0 ; i < hwProperties.length ;) {
            HardwareProperty property = hwProperties[i];

            String description = property.getDescription();
            if (description != null) {
                mSdkLog.info("%s: %s\n", property.getAbstract(), description);
            } else {
                mSdkLog.info("%s\n", property.getAbstract());
            }

            String defaultValue = property.getDefault();
            String defaultFromSkin = skinHardwareConfig != null ? skinHardwareConfig.get(
                    property.getName()) : null;

            if (defaultFromSkin != null) {
                mSdkLog.info("%s [%s (from skin)]:", property.getName(), defaultFromSkin);
            } else if (defaultValue != null) {
                mSdkLog.info("%s [%s]:", property.getName(), defaultValue);
            } else {
                mSdkLog.info("%s (%s):", property.getName(), property.getType());
            }

            result = readLine(readLineBuffer);
            if (result.length() == 0) {
                if (defaultFromSkin != null || defaultValue != null) {
                    if (defaultFromSkin != null) {
                        // we need to write this one in the AVD file
                        map.put(property.getName(), defaultFromSkin);
                    }

                    mSdkLog.info("\n"); // empty line
                    i++; // go to the next property if we have a valid default value.
                         // if there's no default, we'll redo this property
                }
                continue;
            }

            switch (property.getType()) {
                case BOOLEAN:
                    try {
                        if (getBooleanReply(result)) {
                            map.put(property.getName(), "yes");
                            i++; // valid reply, move to next property
                        } else {
                            map.put(property.getName(), "no");
                            i++; // valid reply, move to next property
                        }
                    } catch (IOException e) {
                        // display error, and do not increment i to redo this property
                        mSdkLog.info("\n%s\n", e.getMessage());
                    }
                    break;
                case INTEGER:
                    try {
                        Integer.parseInt(result);
                        map.put(property.getName(), result);
                        i++; // valid reply, move to next property
                    } catch (NumberFormatException e) {
                        // display error, and do not increment i to redo this property
                        mSdkLog.info("\n%s\n", e.getMessage());
                    }
                    break;
                case DISKSIZE:
                    // TODO check validity
                    map.put(property.getName(), result);
                    i++; // valid reply, move to next property
                    break;
            }

            mSdkLog.info("\n"); // empty line
        }

        return map;
    }

    /**
     * Reads a line from the input stream.
     * @param buffer
     * @throws IOException
     */
    private String readLine(byte[] buffer) throws IOException {
        int count = System.in.read(buffer);

        // is the input longer than the buffer?
        if (count == buffer.length && buffer[count-1] != 10) {
            // create a new temp buffer
            byte[] tempBuffer = new byte[256];

            // and read the rest
            String secondHalf = readLine(tempBuffer);

            // return a concat of both
            return new String(buffer, 0, count) + secondHalf;
        }

        // ignore end whitespace
        while (count > 0 && (buffer[count-1] == '\r' || buffer[count-1] == '\n')) {
            count--;
        }

        return new String(buffer, 0, count);
    }

    /**
     * Reads a line from the input stream, masking it as much as possible.
     */
    private String promptPassword(String prompt) throws IOException {

        // Setup a thread that tries to overwrite any input by
        // masking the last character with a space. This is quite
        // crude but is a documented workaround to the lack of a
        // proper password getter.
        final AtomicBoolean keepErasing = new AtomicBoolean(true);

        Thread eraser = new Thread(new Runnable() {
            @Override
            public void run() {
                while (keepErasing.get()) {
                    System.err.print("\b ");    //$NON-NLS-1$. \b=Backspace
                    try {
                        Thread.sleep(10 /*millis*/);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }, "eraser");                           //$NON-NLS-1$

        try {
            System.err.print(prompt);
            eraser.start();
            byte[] buffer = new byte[256];
            return readLine(buffer);
        } finally {
            keepErasing.set(false);
            try {
                eraser.join();
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    /**
     * Returns the boolean value represented by the string.
     * @throws IOException If the value is not a boolean string.
     */
    private boolean getBooleanReply(String reply) throws IOException {

        for (String valid : BOOLEAN_YES_REPLIES) {
            if (valid.equalsIgnoreCase(reply)) {
                return true;
            }
        }

        for (String valid : BOOLEAN_NO_REPLIES) {
            if (valid.equalsIgnoreCase(reply)) {
                return false;
            }
        }

        throw new IOException(String.format("%s is not a valid reply", reply));
    }

    private void errorAndExit(String format, Object...args) {
        mSdkLog.error(null, format, args);
        System.exit(1);
    }

    /**
     * Converts a symbolic target name (such as those accepted by --target on the command-line)
     * to an internal target index id. A valid target name is either a numeric target id (> 0)
     * or a target hash string.
     * <p/>
     * If the given target can't be mapped, {@link #INVALID_TARGET_ID} (0) is returned.
     * It's up to the caller to output an error.
     * <p/>
     * On success, returns a value > 0.
     */
    private int resolveTargetName(String targetName) {

        if (targetName == null) {
            return INVALID_TARGET_ID;
        }

        targetName = targetName.trim();

        // Case of an integer number
        if (targetName.matches("[0-9]*")) {
            try {
                int n = Integer.parseInt(targetName);
                return n < 1 ? INVALID_TARGET_ID : n;
            } catch (NumberFormatException e) {
                // Ignore. Should not happen.
            }
        }

        // Let's try to find a platform or addon name.
        IAndroidTarget[] targets = mSdkManager.getTargets();
        for (int i = 0; i < targets.length; i++) {
            if (targetName.equals(targets[i].hashString())) {
                return i + 1;
            }
        }

        return INVALID_TARGET_ID;
    }
}
