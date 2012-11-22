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

package com.motorola.studio.android.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunch;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.internal.launch.LaunchConfigDelegate;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.io.IFolderWrapper;
import com.android.sdklib.xml.AndroidManifestParser;
import com.android.sdklib.xml.ManifestData;
import com.android.sdklib.xml.ManifestData.Activity;
import com.motorola.studio.android.adt.DDMSFacade;
import com.motorola.studio.android.adt.SdkUtils;
import com.motorola.studio.android.adt.StudioAndroidEventManager;
import com.motorola.studio.android.common.log.StudioLogger;
import com.motorola.studio.android.common.preferences.DialogWithToggleUtils;
import com.motorola.studio.android.emulator.EmulatorPlugin;
import com.motorola.studio.android.emulator.core.devfrm.DeviceFrameworkManager;
import com.motorola.studio.android.emulator.core.model.IAndroidEmulatorInstance;
import com.motorola.studio.android.launch.i18n.LaunchNLS;
import com.motorola.studio.android.launch.ui.StartedInstancesDialog;

/**
 * DESCRIPTION: This class is responsible to execute the launch process <br>
 * RESPONSIBILITY: Perform application launch on a device. <br>
 * COLABORATORS: none <br>
 */
@SuppressWarnings("restriction")
public class StudioAndroidConfigurationDelegate extends LaunchConfigDelegate
{

    private static final String ERRONEOUS_LAUNCH_CONFIGURATION = "erroneous.launch.config.dialog";

    private static final String NO_COMPATIBLE_DEVICE = "no.compatible.device.dialog";

    IAndroidEmulatorInstance compatibleInstance = null;

    IAndroidEmulatorInstance initialEmulatorInstance = null;

    public List<Client> waitingDebugger = new ArrayList<Client>();
   
    private class RunAsClientListener implements IClientChangeListener
    {
        /**
         * 
         */
        private final IAndroidEmulatorInstance instance;

        /**
         * 
         */
        private final String appToLaunch;

        /**
         * 
         * @param instance
         */
        RunAsClientListener(IAndroidEmulatorInstance instance, String appToLaunch)
        {
            this.instance = instance;
            this.appToLaunch = appToLaunch;
        }

        /*
         * (non-Javadoc)
         * @see com.android.ddmlib.AndroidDebugBridge.IClientChangeListener#clientChanged(com.android.ddmlib.Client, int)
         */
        public void clientChanged(Client client, int changeMask)
        {
            if ((changeMask & Client.CHANGE_NAME) == Client.CHANGE_NAME)
            {
                String applicationName = client.getClientData().getClientDescription();
                if (applicationName != null)
                {
                    IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
                    String home = store.getString(AdtPrefs.PREFS_HOME_PACKAGE);
                    if (home.equals(applicationName))
                    {
                        String serialNumber = client.getDevice().getSerialNumber();
                        String avdName = DDMSFacade.getNameBySerialNumber(serialNumber);
                        if ((instance != null) && instance.getName().equals(avdName))
                        {
                            StudioLogger.info(StudioAndroidConfigurationDelegate.class,
                                    "Delegating launch session to ADT... ");

                            synchronized (StudioAndroidConfigurationDelegate.this)
                            {
                                StudioAndroidConfigurationDelegate.this.notify();
                            }
                        }
                    }

                    Client removeClient = null;
                    for (Client waiting : waitingDebugger)
                    {
                        int pid = waiting.getClientData().getPid();
                        if (pid == client.getClientData().getPid())
                        {
                            client.getDebuggerListenPort();
                            synchronized (StudioAndroidConfigurationDelegate.this)
                            {
                                StudioAndroidConfigurationDelegate.this.notify();
                            }
                            removeClient = waiting;
                            break;
                        }
                    }

                    if (removeClient != null)
                    {
                        waitingDebugger.remove(removeClient);
                    }
                }
            }

            if ((changeMask & Client.CHANGE_DEBUGGER_STATUS) == Client.CHANGE_DEBUGGER_STATUS)
            {
                ClientData clientData = client.getClientData();
                String applicationName = clientData.getClientDescription();
                if (clientData.getDebuggerConnectionStatus() == ClientData.DebuggerStatus.DEFAULT)
                {
                    if (((appToLaunch != null) && (applicationName != null))
                            && applicationName.equals(appToLaunch.substring(0,
                                    appToLaunch.lastIndexOf("."))))
                    {
                        client.getDebuggerListenPort();
                        synchronized (StudioAndroidConfigurationDelegate.this)
                        {
                            StudioAndroidConfigurationDelegate.this.notify();
                        }
                    }
                    else if (appToLaunch != null)
                    {
                        waitingDebugger.add(client);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode,
            IProgressMonitor monitor) throws CoreException
    {
        initialEmulatorInstance = null;
        boolean isOk = super.preLaunchCheck(configuration, mode, monitor);

        if (isOk)
        {
            final String instanceName =
                    configuration.getAttribute(
                            ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, (String) null);

            // we found an instance
            if ((instanceName != null) && (instanceName.length() > 0))
            {
                IAndroidEmulatorInstance instance =
                        DeviceFrameworkManager.getInstance().getInstanceByName(instanceName);
                if (instance == null)
                {
                    String serialNumber = LaunchUtils.getSerialNumberForInstance(instanceName);
                    if (!DDMSFacade.isDeviceOnline(serialNumber))
                    {
                        isOk = false;
                        handleErrorDuringLaunch(configuration, mode, instanceName);
                    }
                }
                else
                {
                    if (!instance.isAvailable())
                    {
                        isOk = false;
                        handleErrorDuringLaunch(configuration, mode, instanceName);
                    }

                    if (!instance.isStarted())
                    {
                        initialEmulatorInstance = instance;
                        //updates the compatible instance with user response
                        isOk = checkForCompatibleRunningInstances(configuration);
                    }
                }
            }
            else
            {
                isOk = false;
                handleErrorDuringLaunch(configuration, mode, null);
            }
        }
        // validate if the project isn't a library project
        if (isOk)
        {
            String projectName =
                    configuration.getAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME,
                            (String) null);
            if (projectName != null)
            {
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                if ((project != null) && SdkUtils.isLibraryProject(project))
                {
                    handleProjectError(configuration, project, mode);
                    isOk = false;
                }
            }
        }

        return isOk;
    }

    private void handleProjectError(final ILaunchConfiguration config, final IProject project,
            final String mode)
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {

            public void run()
            {
                Shell shell = LaunchUtils.getActiveWorkbenchShell();

                String message = LaunchNLS.UI_LaunchConfigurationTab_ERR_PROJECT_IS_LIBRARY;

                String prefKey = ERRONEOUS_LAUNCH_CONFIGURATION;

                DialogWithToggleUtils.showInformation(prefKey,
                        LaunchNLS.ERR_LaunchConfigurationShortcut_MsgTitle, message);

                StructuredSelection struturedSelection;

                String groupId = IDebugUIConstants.ID_RUN_LAUNCH_GROUP;

                ILaunchGroup group = DebugUITools.getLaunchGroup(config, mode);
                groupId = group.getIdentifier();
                struturedSelection = new StructuredSelection(config);

                DebugUITools.openLaunchConfigurationDialogOnGroup(shell, struturedSelection,
                        groupId);
            }
        });
    }

    private void handleErrorDuringLaunch(final ILaunchConfiguration config, final String mode,
            final String instanceName)
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {

            public void run()
            {
                Shell shell = LaunchUtils.getActiveWorkbenchShell();

                String message =
                        instanceName != null ? NLS.bind(
                                LaunchNLS.ERR_LaunchDelegate_InvalidDeviceInstance, instanceName)
                                : NLS.bind(LaunchNLS.ERR_LaunchDelegate_No_Compatible_Device,
                                        config.getName());

                String prefKey =
                        instanceName != null ? ERRONEOUS_LAUNCH_CONFIGURATION
                                : NO_COMPATIBLE_DEVICE;

                DialogWithToggleUtils.showInformation(prefKey,
                        LaunchNLS.ERR_LaunchConfigurationShortcut_MsgTitle, message);

                StructuredSelection struturedSelection;

                String groupId = IDebugUIConstants.ID_RUN_LAUNCH_GROUP;

                ILaunchGroup group = DebugUITools.getLaunchGroup(config, mode);
                groupId = group.getIdentifier();
                struturedSelection = new StructuredSelection(config);

                DebugUITools.openLaunchConfigurationDialogOnGroup(shell, struturedSelection,
                        groupId);
            }
        });
    }

    /**
     * Launches an Android application based on the given launch configuration.
     */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException

    {
        //use a working copy because it can be changed and these changes should not be propagated to the original copy 
        ILaunchConfigurationWorkingCopy configurationWorkingCopy = configuration.getWorkingCopy();

        StudioLogger.info(StudioAndroidConfigurationDelegate.class,
                "Launch Android Application using Studio for Android wizard. Configuration: "
                        + configurationWorkingCopy + " mode:" + mode + " launch: " + launch);
        try
        {

            String projectName =
                    configurationWorkingCopy.getAttribute(
                            ILaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
            int launchAction =
                    configurationWorkingCopy.getAttribute(
                            ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
                            ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT);

            String instanceName =
                    configurationWorkingCopy.getAttribute(
                            ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, (String) null);

            if ((projectName != null) && (instanceName != null))
            {
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                if (project == null)
                {
                    IStatus status =
                            new Status(Status.ERROR, LaunchPlugin.PLUGIN_ID,
                                    "Could not retrieve project: " + projectName);
                    throw new CoreException(status);
                }

                String appToLaunch = null;
                if (launchAction == ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT)
                {
                    ManifestData manifestParser =
                            AndroidManifestParser.parse(new IFolderWrapper(project));
                    Activity launcherActivity = manifestParser.getLauncherActivity();
                    String activityName = null;
                    if (launcherActivity != null)
                    {
                        activityName = launcherActivity.getName();
                    }

                    // if there's no default activity. Then there's nothing to be launched.
                    if (activityName != null)
                    {
                        appToLaunch = activityName;
                    }
                }
                // case for a specific activity
                else if (launchAction == ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_ACTIVITY)
                {
                    appToLaunch =
                            configurationWorkingCopy.getAttribute(
                                    ILaunchConfigurationConstants.ATTR_ACTIVITY, (String) null);

                    if ((appToLaunch == null) || "".equals(appToLaunch))
                    {
                        IStatus status =
                                new Status(
                                        Status.ERROR,
                                        LaunchPlugin.PLUGIN_ID,
                                        "Activity field cannot be empty. Specify an activity or use the default activity on launch configuration.");
                        throw new CoreException(status);
                    }
                }
                // for the do nothing case there is nothing to do

                IAndroidEmulatorInstance emuInstance =
                        DeviceFrameworkManager.getInstance().getInstanceByName(instanceName);

                RunAsClientListener list = null;

                //if initialEmulatorInstance is not null it means that it was offline and user has interacted with StartedInstancesDialog.
                //The emuInstance variable should be overrided by the initialEmulatorInstance because the emuInstance can be the new
                //user choice (in case he has selected the check box in dialog asking to update the run configuration)
                if (initialEmulatorInstance != null)
                {
                    emuInstance = initialEmulatorInstance;
                }

                try
                {
                    if (appToLaunch != null)
                    {
                        list = new RunAsClientListener(emuInstance, appToLaunch);
                        StudioAndroidEventManager.asyncAddClientChangeListener(list);
                    }

                    // The instance from the launch configuration is an emulator (because the query returned
                    // something different from null) and is not started.
                    if ((emuInstance != null) && (!emuInstance.isStarted()))
                    {
                        if (compatibleInstance != null)
                        {
                            emuInstance = compatibleInstance;
                            instanceName = emuInstance.getName();
                            configurationWorkingCopy.setAttribute(
                                    ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
                                    emuInstance.getName());
                            configurationWorkingCopy.setAttribute(
                                    ILaunchConfigurationConstants.ATTR_ADT_DEVICE_INSTANCE_NAME,
                                    emuInstance.getName());
                        }
                        else
                        {
                            startEmuInstance(emuInstance);
                        }
                    }

                    StudioLogger.info(StudioAndroidConfigurationDelegate.class,
                            "AVD where the application will be executed: " + instanceName);


                    String serialNumber = LaunchUtils.getSerialNumberForInstance(instanceName);
                    if (serialNumber == null)
                    {
                        IStatus status =
                                new Status(Status.ERROR, LaunchPlugin.PLUGIN_ID,
                                        "Could not retrieve AVD instance: " + instanceName);
                        throw new CoreException(status);
                    }

                    bringConsoleView();

                    // Determining if it is an emulator or handset and creating the description
                    //to be used for usage data collection
                    String descriptionToLog = "";
                    if (emuInstance != null)
                    {
                        descriptionToLog = StudioLogger.VALUE_EMULATOR;
                    }
                    else
                    {
                        if ((serialNumber != null) && (!serialNumber.equals("")))
                        {
                            descriptionToLog = StudioLogger.VALUE_HANDSET;
                        }
                    }

                    if (!descriptionToLog.equals(""))
                    {
                        descriptionToLog =
                                StudioLogger.KEY_DEVICE_TYPE + descriptionToLog
                                        + StudioLogger.SEPARATOR;
                    }

                    descriptionToLog = descriptionToLog + StudioLogger.KEY_USE_VDL;

                    descriptionToLog = descriptionToLog + StudioLogger.VALUE_NO;
                    super.launch(configurationWorkingCopy, mode, launch, monitor);

                    // Collecting usage data for statistical purposes
                    try
                    {
                        String prjTarget = "";
                        if (project != null)
                        {
                            prjTarget = Sdk.getCurrent().getTarget(project).getName();
                        }

                        if (!descriptionToLog.equals(""))
                        {
                            descriptionToLog = descriptionToLog + StudioLogger.SEPARATOR;
                        }

                        descriptionToLog =
                                descriptionToLog + StudioLogger.KEY_PRJ_TARGET + prjTarget;

                        if (emuInstance != null)
                        {
                            String emuTarget = emuInstance.getTarget();
                            descriptionToLog = descriptionToLog + StudioLogger.SEPARATOR;
                            descriptionToLog =
                                    descriptionToLog + StudioLogger.KEY_TARGET + emuTarget;
                        }

                        StudioLogger.collectUsageData(mode, StudioLogger.KIND_APP_MANAGEMENT,
                                descriptionToLog, LaunchPlugin.PLUGIN_ID, LaunchPlugin.getDefault()
                                        .getBundle().getVersion().toString());
                    }
                    catch (Throwable e)
                    {
                        //Do nothing, but error on the log should never prevent app from working
                    }

                }
                finally
                {
                    if (list != null)
                    {
                        StudioAndroidEventManager.asyncRemoveClientChangeListener(list);
                    }
                    StudioAndroidEventManager.asyncAddClientChangeListener(AndroidLaunchController
                            .getInstance());
                }
            }
            else
            {                
                throw new CoreException(new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID,
                        "Missing parameters for launch"));
            }
        }
        catch (CoreException e)
        {
            AndroidLaunch androidLaunch = (AndroidLaunch) launch;
            androidLaunch.stopLaunch();
            StudioLogger.error(StudioAndroidConfigurationDelegate.class, "Error while lauching "
                    + configurationWorkingCopy.getName(), e);
            throw e;
        }
        catch (Exception e)
        {
            StudioLogger.error(LaunchUtils.class,
                    "An error occurred trying to parse AndroidManifest", e);
        }
        finally
        {
            if (mode.equals(ILaunchManager.RUN_MODE))
            {
                AndroidLaunch androidLaunch = (AndroidLaunch) launch;
                androidLaunch.stopLaunch();
            }
        }
    }

    /**
      * @param project
      * @param emuInstance
     * @throws CoreException 
      */
    private boolean checkForCompatibleRunningInstances(ILaunchConfiguration configuration)
            throws CoreException
    {
        IProject project = null;
        compatibleInstance = null;

        final String projectName =
                configuration.getAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME,
                        (String) null);

        project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project == null)
        {
            IStatus status =
                    new Status(Status.ERROR, LaunchPlugin.PLUGIN_ID, "Could not retrieve project: "
                            + projectName);
            throw new CoreException(status);
        }

        //Check if there is a compatible instance running to launch the app
        Collection<IAndroidEmulatorInstance> startedInstances =
                DeviceFrameworkManager.getInstance().getAllStartedInstances();

        final Collection<IAndroidEmulatorInstance> compatibleStartedInstances =
                new HashSet<IAndroidEmulatorInstance>();

        boolean continueLaunch = true;

        for (IAndroidEmulatorInstance i : startedInstances)
        {
            IStatus resultStatus = LaunchUtils.isCompatible(project, i.getName());
            if ((resultStatus.getSeverity() == Status.OK)
                    || (resultStatus.getSeverity() == Status.WARNING))
            {
                compatibleStartedInstances.add(i);
            }
        }
        if (compatibleStartedInstances.size() > 0)
        {
            //show a dialog with compatible running instances so the user can
            //choose one to run the app, or he can choose to run the preferred AVD

            StartedInstancesDialogProxy proxy =
                    new StartedInstancesDialogProxy(compatibleStartedInstances, configuration,
                            project);
            PlatformUI.getWorkbench().getDisplay().syncExec(proxy);

            compatibleInstance = proxy.getSelectedInstance();
            continueLaunch = proxy.continueLaunch();
        }
        return continueLaunch;
    }

    private class StartedInstancesDialogProxy implements Runnable
    {
        private IAndroidEmulatorInstance selectedInstance = null;

        private boolean continueLaunch = true;

        private final ILaunchConfiguration configuration;

        Collection<IAndroidEmulatorInstance> compatibleStartedInstances = null;

        IProject project = null;

        /**
         * 
         */
        public StartedInstancesDialogProxy(
                Collection<IAndroidEmulatorInstance> compatibleStartedInstances,
                ILaunchConfiguration configuration, IProject project)
        {
            this.compatibleStartedInstances = compatibleStartedInstances;
            this.configuration = configuration;
            this.project = project;
        }

        public void run()
        {
            Shell aShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            Shell shell = new Shell(aShell);
            StartedInstancesDialog dialog;
            try
            {
                dialog =
                        new StartedInstancesDialog(shell, compatibleStartedInstances,
                                configuration, project);
                dialog.setBlockOnOpen(true);
                dialog.open();

                selectedInstance = null;
                if (dialog.getReturnCode() == IDialogConstants.OK_ID)
                {
                    selectedInstance = dialog.getSelectedInstance();
                }
                else if (dialog.getReturnCode() == IDialogConstants.ABORT_ID)
                {
                    continueLaunch = false;
                }
            }
            catch (CoreException e)
            {
                StudioLogger.error(StudioAndroidConfigurationDelegate.class,
                        "It was not possible to open Started Instance Dialog", e);
            }
        }

        public IAndroidEmulatorInstance getSelectedInstance()
        {
            return selectedInstance;
        }

        public boolean continueLaunch()
        {
            return continueLaunch;
        }
    }

    /**
     * Bring Console View to the front and activate the appropriate stream
     * 
     */
    private void bringConsoleView()
    {
        IConsole activeConsole = null;

        IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
        for (IConsole console : consoles)
        {
            if (console.getName().equals(ILaunchConfigurationConstants.ANDROID_CONSOLE_ID))
            {
                activeConsole = console;
            }
        }

        // Bring Console View to the front  
        if (activeConsole != null)
        {
            ConsolePlugin.getDefault().getConsoleManager().showConsoleView(activeConsole);
        }

    }

    /**
     * 
     * @param instance
     * @throws CoreException
     */
    private void startEmuInstance(IAndroidEmulatorInstance instance) throws CoreException
    {
        StudioLogger.info(StudioAndroidConfigurationDelegate.class,
                "Needs to Start the AVD instance before launching... ");

        ServiceHandler startHandler = EmulatorPlugin.getStartServiceHandler();
        IStatus status = startHandler.run((IInstance) instance, null, new NullProgressMonitor());

        StudioLogger.info(StudioAndroidConfigurationDelegate.class,
                "Status of the launch service: " + status);

        if (status.getSeverity() == Status.ERROR)
        {
            throw new CoreException(status);
        }
        else if (status.getSeverity() == Status.CANCEL)
        {
            StudioLogger.info(StudioAndroidConfigurationDelegate.class,
                    "Abort launch session because the AVD start was canceled. ");
            return;
        }

        if (!instance.isStarted())
        {
            status =
                    new Status(Status.ERROR, LaunchPlugin.PLUGIN_ID,
                            "The Android Virtual Device is not started: " + instance.getName());
            throw new CoreException(status);
        }

        synchronized (this)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                StudioLogger.info("Could not wait: ", e.getMessage());
            }
        }
    }
}
