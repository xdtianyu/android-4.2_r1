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
package com.motorola.studio.android.adt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.NullSdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.internal.widgets.MessageBoxLog;
import com.motorola.studio.android.AndroidPlugin;
import com.motorola.studio.android.common.IAndroidConstants;
import com.motorola.studio.android.common.exception.AndroidException;
import com.motorola.studio.android.common.log.StudioLogger;
import com.motorola.studio.android.common.utilities.FileUtil;
import com.motorola.studio.android.i18n.AndroidNLS;
import com.motorola.studio.android.manifest.AndroidProjectManifestFile;
import com.motorola.studio.android.model.manifest.AndroidManifestFile;
import com.motorola.studio.android.model.manifest.dom.AndroidManifestNode;
import com.motorola.studio.android.model.manifest.dom.UsesSDKNode;

/**
 * DESCRIPTION: This class provides utility methods related to 
 * the Android SDK.
 * <br> 
 * USAGE: See public methods
 */

public class SdkUtils
{
    public static final int API_LEVEL_FOR_PLATFORM_VERSION_3_0_0 = 11;

    public static final String VM_CONFIG_FILENAME = "config.ini"; //$NON-NLS-1$

    public static final String USERIMAGE_FILENAME = "userdata-qemu.img"; //$NON-NLS-1$

    public static final String[] STATEDATA_FILENAMES =
    {
            "cache.img", "userdata.img", "emulator-user.ini" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    };

    public static final String EMU_CONFIG_SKIN_NAME_PROPERTY = "skin.name"; //$NON-NLS-1$

    /**
     * Gets the current SDK object
     */
    public static Sdk getCurrentSdk()
    {
        return Sdk.getCurrent();
    }

    /**
     * Gets the directory where the configured SDK is located
     */
    public static String getSdkPath()
    {
        String sdkDir = null;
        Sdk sdk = getCurrentSdk();
        if (sdk != null)
        {
            sdkDir = sdk.getSdkLocation();
        }
        return sdkDir;
    }

    /**
     * Gets the path to the "tools" folder of the SDK 
     * @return
     */
    public static String getSdkToolsPath()
    {
        return AdtPlugin.getOsSdkToolsFolder();
    }

    public static IAndroidTarget getTargetByAPILevel(Integer apiLevel)
    {
        IAndroidTarget returnTarget = null;

        for (IAndroidTarget target : getAllTargets())
        {
            if (target.getVersion().getApiLevel() == apiLevel)
            {
                returnTarget = target;
            }
        }
        return returnTarget;
    }

    /**
     * Get the AAPT application path from an android target
     * if null is passed, try to get some aapt
     * @param target
     * @return
     */
    public static String getTargetAAPTPath(IAndroidTarget target)
    {
        IAndroidTarget realTarget = null;
        if (target == null)
        {
            StudioLogger.warn(SdkUtils.class, "Trying to find a suitable aapt application to use"); //$NON-NLS-1$
            IAndroidTarget[] allTargets = Sdk.getCurrent().getTargets();
            if (allTargets.length > 0)
            {
                realTarget = allTargets[0];
            }
        }
        else
        {
            realTarget = target;
        }

        while ((realTarget != null) && !realTarget.isPlatform())
        {
            realTarget = realTarget.getParent();
        }

        if (realTarget == null)
        {
            StudioLogger.warn(SdkUtils.class,
                    "No aapt executable found: do you have an Android platform installed?"); //$NON-NLS-1$
        }

        return realTarget != null ? realTarget.getPath(IAndroidTarget.AAPT) : null;
    }

    /**
     * Gets the path to the "adb" executable of the SDK 
     * @return
     */
    public static String getAdbPath()
    {
        return AdtPlugin.getOsAbsoluteAdb();
    }

    /**
     * Reloads the recognized AVD list
     */
    public static void reloadAvds()
    {

        try
        {
            getVmManager().reloadAvds(NullSdkLog.getLogger());
        }
        catch (Exception e)
        {
            StudioLogger.error(SdkUtils.class, "Error while reloading AVDs"); //$NON-NLS-1$
        }

    }

    /**
     * Gets the VmManager object
     */
    public static AvdManager getVmManager()
    {
        AvdManager vmManager = null;
        Sdk sdk = getCurrentSdk();
        if (sdk != null)
        {
            vmManager = sdk.getAvdManager();
        }

        return vmManager;
    }

    /**
     * Gets all available Targets
     */
    public static IAndroidTarget[] getAllTargets()
    {
        IAndroidTarget[] allTargets = null;
        Sdk sdk = getCurrentSdk();
        if (sdk != null)
        {
            allTargets = sdk.getTargets();
        }
        return allTargets;
    }

    /**
     * Gets a Target by name
     * 
     * @param name the target name
     */
    public static IAndroidTarget getTargetByName(String name)
    {
        IAndroidTarget ret = null;

        if ((name != null) && (name.length() > 0))
        {
            IAndroidTarget[] allTargets = getAllTargets();

            for (int i = 0; i < allTargets.length; i++)
            {
                if (name.equals(allTargets[i].getName()))
                {
                    ret = allTargets[i];
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * Gets all VMs
     */
    public static AvdInfo[] getAllVms()
    {
        AvdInfo[] allVmInfo = null;
        AvdManager vmManager = getVmManager();
        if (vmManager != null)
        {
            allVmInfo = vmManager.getAllAvds();
        }
        return allVmInfo;
    }

    /**
     * Gets all valid VMs
     */
    public static AvdInfo[] getAllValidVms()
    {
        AvdInfo[] validVmInfo = null;
        AvdManager vmManager = getVmManager();
        if (vmManager != null)
        {
            validVmInfo = vmManager.getValidAvds();
        }
        return validVmInfo;
    }

    /**
     * Gets the name of all VMs that are recognized by the configured SDK.
     */
    public static Collection<String> getAllVmNames()
    {
        Collection<String> vmNames = new LinkedList<String>();
        for (AvdInfo vm : getAllVms())
        {
            vmNames.add(vm.getName());
        }
        return vmNames;
    }

    /**
     * Gets the name of all VMs that are recognized by the configured SDK.
     */
    public static Collection<String> getAllValidVmNames()
    {
        Collection<String> vmNames = new LinkedList<String>();
        AvdInfo[] allAvds = getAllValidVms();
        if (allAvds != null)
        {
            for (AvdInfo vm : allAvds)
            {
                vmNames.add(vm.getName());
            }
        }

        return vmNames;
    }

    /**
     * Gets a skin name
     * 
     * @param vmInfo the VM to get the skin
     */
    public static String getSkin(AvdInfo vmInfo)
    {
        String skin = ""; //$NON-NLS-1$
        File configFile = vmInfo.getConfigFile();
        Properties p = new Properties();
        InputStream configFileStream = null;
        try
        {
            configFileStream = new FileInputStream(configFile);
            p.load(configFileStream);
            skin = p.getProperty(EMU_CONFIG_SKIN_NAME_PROPERTY);
        }
        catch (FileNotFoundException e)
        {
            StudioLogger
                    .error(SdkUtils.class,
                            "Error getting VM skin definition. Could not find file " + configFile.getAbsolutePath(), e); //$NON-NLS-1$
        }
        catch (IOException e)
        {
            StudioLogger
                    .error(SdkUtils.class,
                            "Error getting VM skin definition. Could not access file " + configFile.getAbsolutePath(), e); //$NON-NLS-1$
        }
        finally
        {
            if (configFileStream != null)
            {
                try
                {
                    configFileStream.close();
                }
                catch (IOException e)
                {
                    //nothing to do
                }
            }
        }

        return skin;
    }

    /**
     * Gets a VM by name.
     * 
     * @param vmName The VM name
     */
    public static AvdInfo getValidVm(String vmName)
    {
        AvdInfo vmInfo = null;
        AvdManager vmManager = getVmManager();
        if (vmManager != null)
        {
            vmInfo = vmManager.getAvd(vmName, true);
        }
        return vmInfo;
    }

    /**
     * Gets a VM by name.
     * 
     * @param vmName The VM name
     */
    public static AvdInfo getVm(String vmName)
    {
        AvdInfo vmInfo = null;
        AvdManager vmManager = getVmManager();
        if (vmManager != null)
        {
            vmInfo = vmManager.getAvd(vmName, false);
        }
        return vmInfo;
    }

    /**
     * Creates a new VM instance.
     * 
     * @param folder Folder where the VM files will be stored
     * @param name VM Name
     * @param target VM Target represented by the IAndroidTarget object
     * @param skinName VM Skin name from the VM Target
     * 
     * @throws CoreException
     */
    public static AvdInfo createVm(String folder, String name, IAndroidTarget target,
            String abiType, String skinName, String useSnapshot, String sdCard)
            throws CoreException

    {
        AvdInfo vmInfo;
        AvdManager vmManager = SdkUtils.getVmManager();

        // get the abi type
        if (abiType == null)
        {
            abiType = SdkConstants.ABI_ARMEABI;
        }

        /*
         * public VmInfo createVm(String parentFolder, String name, IAndroidTarget target, 
         * String skinName, String sdcard, Map hardwareConfig)
         */
        vmInfo =
                vmManager.createAvd(new File(folder), name, target, abiType, skinName, sdCard,
                        null, Boolean.parseBoolean(useSnapshot), true, false,
                        NullSdkLog.getLogger());

        if (vmInfo == null)
        {
            String errMsg = NLS.bind(AndroidNLS.EXC_SdkUtils_CannotCreateTheVMInstance, name);

            IStatus status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, errMsg);
            throw new CoreException(status);
        }

        return vmInfo;
    }

    /**
     * Deletes a VM instance.
     * 
     * @param name VM Name
     */
    public static void deleteVm(String name)
    {

        AvdManager vmManager = SdkUtils.getVmManager();
        AvdInfo avdToDelete = vmManager != null ? vmManager.getAvd(name, false) : null;
        if (avdToDelete != null)
        {
            try
            {
                if ((avdToDelete.getIniFile() != null) && avdToDelete.getIniFile().exists())
                {
                    avdToDelete.getIniFile().delete();
                }
                String path = avdToDelete.getDataFolderPath();
                if (path != null)
                {
                    File avdDir = new File(path);
                    if (avdDir.exists())
                    {
                        FileUtil.deleteDirRecursively(avdDir);
                    }
                }
                vmManager.removeAvd(avdToDelete);

            }
            catch (Exception e)
            {
                StudioLogger.error("Could not delete AVD: " + e.getMessage()); //$NON-NLS-1$
            }
        }

    }

    /**
     * Get the reference to the File that points to the filesystem location of the directory 
     * where the user data files of the VM with the given name are stored.
     * @param vmName name of the VM whose userdata directory is to be retrieved.
     * @return the File object that references the filesystem location of the directory where 
     * the userdata files of the given VM will be stored. Returns a null reference if SDK is not configured 
     * or if there is no VM with the given name.
     */
    public static File getUserdataDir(String vmName)
    {
        AvdInfo vminfo = SdkUtils.getValidVm(vmName);
        File userdataDir = null;

        if (vminfo != null)
        {
            String vmpath = vminfo.getDataFolderPath();
            userdataDir = new File(vmpath);
        }

        return userdataDir;
    }

    /**
     * Get the reference to the File that points to the filesystem location where the 
     * user data file of the VM with the given name is.
     * 
     * @param vmName name of the VM whose userdata file is to be retrieved.
     * @return the File object that references the filesystem location where the userdata 
     * of the given VM should be. Returns a null reference if SDK is not configured 
     * or if there is no VM with the given name.
     */
    public static File getUserdataFile(String vmName)
    {
        File userdataDir = getUserdataDir(vmName);
        File userdataFile = null;

        if (userdataDir != null)
        {
            userdataFile = new File(userdataDir, USERIMAGE_FILENAME);
        }

        return userdataFile;
    }

    /**
     * Get the reference to the Files that point to the filesystem location where the 
     * state data files of the VM with the given name are.
     * 
     * @param vmName name of the VM whose state data files is to be retrieved.
     * @return the File objects that reference the filesystem location where the state data
     * files of the given VM should be. Returns a null reference if SDK is not configured 
     * or if there is no VM with the given name.
     */
    public static List<File> getStateDataFiles(String vmName)
    {
        File userdataDir = getUserdataDir(vmName);
        List<File> stateDataFiles = null;

        if (userdataDir != null)
        {
            stateDataFiles = new ArrayList<File>();

            for (int i = 0; i < STATEDATA_FILENAMES.length; i++)
            {
                stateDataFiles.add(new File(userdataDir, STATEDATA_FILENAMES[i]));
            }
        }

        return stateDataFiles;
    }

    /**
     * Retrieves all sample applications from a target
     * 
     * @param target The target
     * @return all sample applications from a target
     */
    public static Object[] getSamples(IAndroidTarget target)
    {
        List<Sample> samples = new ArrayList<Sample>();
        File samplesFolder = new File(target.getPath(IAndroidTarget.SAMPLES));
        samples = findSamples(samplesFolder, target);
        return samples.toArray();
    }

    /**
     * Find the samples inside an specific directory (recursively)
     * 
     * @param folder the folder that can contain samples
     * @param target the target of the samples in the folder
     * @return a list of samples
     */
    private static List<Sample> findSamples(File folder, IAndroidTarget target)
    {

        List<Sample> samples = new ArrayList<Sample>();

        if (folder.exists() && folder.isDirectory())
        {
            for (File sampleFolder : folder.listFiles())
            {
                if (sampleFolder.isDirectory())
                {
                    if (Sample.isSample(sampleFolder))
                    {
                        samples.add(new Sample(sampleFolder, target));
                    }
                    else
                    {
                        samples.addAll(findSamples(sampleFolder, target));
                    }
                }
            }
        }

        return samples;
    }

    /**
     * Retrieves all targets for a given SDK
     * 
     * @param sdk The sdk
     * 
     * @return all targets for the given SDK
     */
    public static Object[] getTargets(Sdk sdk)
    {
        Object[] targets = null;
        if (sdk != null)
        {
            targets = sdk.getTargets();
        }
        return targets;
    }

    /**
     * Associates a project to a target
     * 
     * @param project The project
     * @param target The target
     */
    public static void associate(IProject project, IAndroidTarget target)
    {
        try
        {
            Sdk.getCurrent().initProject(project, target);
        }
        catch (Exception e)
        {
            StudioLogger.error(SdkUtils.class, "Error associating project " + project.getName() //$NON-NLS-1$
                    + " with target " + target.getName()); //$NON-NLS-1$
        }
    }

    /**
     * Retrieves the target for a project
     * 
     * @param project the project
     * 
     * @return the target for the project
     */
    public static IAndroidTarget getTarget(IProject project)
    {
        IAndroidTarget target = null;
        if (project != null)
        {
            target = Sdk.getCurrent().getTarget(project);
        }
        return target;
    }

    /**
     * Retrieves the target for a project
     * 
     * @param project the project
     * 
     * @return the target for the project
     */
    public static String getMinSdkVersion(IProject project)
    {
        String minSdkVersion = "";
        try
        {
            AndroidManifestFile androidManifestFile =
                    AndroidProjectManifestFile.getFromProject(project);
            UsesSDKNode usesSdkNode =
                    (UsesSDKNode) androidManifestFile.getNode(AndroidManifestNode.NodeType.UsesSdk);
            if (usesSdkNode != null)
            {
                minSdkVersion = usesSdkNode.getMinSdkVersion();
            }

        }
        catch (AndroidException e)
        {
            StudioLogger.error("Error getting min sdk version. " + e.getMessage());
        }
        catch (CoreException e)
        {
            StudioLogger.error("Error getting min sdk version. " + e.getMessage());
        }
        return minSdkVersion;
    }

    /**
     * Retrieves all Activity Actions for a project.
     * 
     * @param project The project
     * 
     * @return all Activity Actions for the project.
     */
    public static String[] getActivityActions(IProject project)
    {
        String[] attributeValues = new String[0];

        if ((project != null) && project.isOpen())
        {
            IAndroidTarget target = SdkUtils.getTarget(project);
            AndroidTargetData targetData = Sdk.getCurrent().getTargetData(target);

            if (targetData != null)
            {
                attributeValues =
                        targetData.getAttributeValues("action", "android:name", "activity"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

        return attributeValues;
    }

    /**
     * Retrieves all Service Actions for a project.
     * 
     * @param project The project
     * 
     * @return all Service Actions for the project.
     */
    public static String[] getServiceActions(IProject project)
    {
        String[] attributeValues = new String[0];

        if ((project != null) && project.isOpen())
        {
            IAndroidTarget target = SdkUtils.getTarget(project);
            AndroidTargetData targetData = Sdk.getCurrent().getTargetData(target);

            if (targetData != null)
            {
                attributeValues =
                        targetData.getAttributeValues("action", "android:name", "service"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

        return attributeValues;
    }

    /**
     * Retrieves all Broadcast Receiver Actions for a project.
     * 
     * @param project The project
     * 
     * @return all Broadcast Receiver Actions for the project.
     */
    public static String[] getReceiverActions(IProject project)
    {
        String[] attributeValues = new String[0];

        if ((project != null) && project.isOpen())
        {
            IAndroidTarget target = SdkUtils.getTarget(project);
            AndroidTargetData targetData = Sdk.getCurrent().getTargetData(target);

            if (targetData != null)
            {
                attributeValues =
                        targetData.getAttributeValues("action", "android:name", "receiver"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

        return attributeValues;
    }

    /**
     * Retrieves all Intent Filter Actions for a project.
     * 
     * @param project The project
     * 
     * @return all Intent Filter Actions for the project.
     */
    public static String[] getIntentFilterCategories(IProject project)
    {
        String[] attributeValues = new String[0];

        if ((project != null) && project.isOpen())
        {
            IAndroidTarget target = SdkUtils.getTarget(project);
            AndroidTargetData targetData = Sdk.getCurrent().getTargetData(target);

            if (targetData != null)
            {
                attributeValues = targetData.getAttributeValues("category", "android:name"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return attributeValues;
    }

    /**
     * Get the api version number for a given project
     * @param project: the project
     * @return the api version number or 0 if some error occurs
     */
    public static int getApiVersionNumberForProject(IProject project)
    {
        int api = 0;
        IAndroidTarget target = SdkUtils.getTarget(project);
        if (target != null)
        {
            AndroidVersion version = target.getVersion();
            if (version != null)
            {
                api = version.getApiLevel();
            }
        }
        return api;
    }

    public static String getTargetNameForProject(IProject project)
    {
        IAndroidTarget target = getTarget(project);
        return target != null ? target.getName() : ""; //$NON-NLS-1$
    }

    public static boolean isPlatformTarget(String avdName)
    {
        IAndroidTarget target = getValidVm(avdName).getTarget();
        return target != null ? target.isPlatform() : false;
    }

    public static boolean isProjectTargetAPlatform(IProject project)
    {
        IAndroidTarget target = getTarget(project);
        return target != null ? target.isPlatform() : false;
    }

    public static boolean isPlatformTarget(IAndroidTarget target)
    {
        return target != null ? target.isPlatform() : false;
    }

    /**
     * Retrieves the APK configurations of a project
     * 
     * @param project the project
     * @return the APK configurations
     */
    public static Map<String, String> getAPKConfigurationsForProject(IProject project)
    {

        Map<String, String> apkConfigurations = null;

        if ((project != null) && project.isOpen())
        {
            Sdk.getCurrent();
            //This is not supported on ADT 14 preview so let's comment it for now.
            //            apkConfigurations = Sdk.getProjectState(project).getApkSettings().getLocaleFilters();
            apkConfigurations = new HashMap<String, String>(0);
        }

        return apkConfigurations;

    }

    public static String getBaseTarget(String name)
    {
        IAndroidTarget target = getValidVm(name).getTarget();
        while (!target.isPlatform())
        {
            target = target.getParent();
        }
        return target.getName();
    }

    /**
     * Check if an SDK is an OPhone Sdk
     * @return
     */
    public static boolean isOphoneSDK()
    {
        boolean result = false;

        // check if the folder contains the oms jar
        FilenameFilter omsFilenameFilter = new FilenameFilter()
        {
            public boolean accept(File arg0, String arg1)
            {
                return arg1.equals(IAndroidConstants.OPHONE_JAR);
            }
        };

        Sdk sdk = getCurrentSdk();
        IAndroidTarget[] targets = sdk.getTargets();
        for (IAndroidTarget target : targets)
        {
            File folder = new File(target.getLocation());
            if (folder.list(omsFilenameFilter).length > 0)
            {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Check if an SDK is an JIL sdk
     * @return
     */
    public static boolean isJILSdk()
    {
        boolean result = false;

        // check if the folder contains the oms jar
        FilenameFilter jilFilenameFilter = new FilenameFilter()
        {

            public boolean accept(File arg0, String arg1)
            {
                return arg1.equals(IAndroidConstants.JIL_JAR);
            }
        };

        Sdk sdk = getCurrentSdk();
        if (sdk != null)
        {
            IAndroidTarget[] targets = sdk.getTargets();
            for (IAndroidTarget target : targets)
            {
                File folder = new File(target.getLocation());
                if (folder.list(jilFilenameFilter).length > 0)
                {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    public static String getEmulatorWindowName(String avdName, int port)
    {
        String windowName = ""; //$NON-NLS-1$
        if (isJILSdk())
        {
            windowName = "JIL Emulator (" + avdName + ":" + port + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        else if (isOphoneSDK())
        {
            windowName = "OPhone Emulator (" + avdName + ":" + port + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        else
        {
            windowName = port + ":" + avdName; //$NON-NLS-1$
        }
        return windowName;
    }

    public static boolean isLibraryProject(IProject project)
    {
        return Sdk.getProjectState(project) != null ? Sdk.getProjectState(project).isLibrary()
                : false;
    }

    /**
     * Returns all available permissions
     * 
     * @return String array containing the available permissions 
     */
    public static String[] getIntentFilterPermissions(IProject project)
    {
        String[] attributeValues = new String[0];

        if ((project != null) && project.isOpen())
        {
            IAndroidTarget target = SdkUtils.getTarget(project);
            AndroidTargetData targetData = Sdk.getCurrent().getTargetData(target);

            if (targetData != null)
            {
                attributeValues = targetData.getAttributeValues("uses-permission", "android:name"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return attributeValues;
    }

    /**
     * Try to repair an AVD. Currently only avds with wrong image path are repariable.
     * Display a message with the changes to the config.ini
     * @param avdInfo
     * @return Status ERROR if an IO exception occured.
     */
    public static IStatus repairAvd(AvdInfo avdInfo)
    {
        IStatus status = Status.OK_STATUS;

        AvdManager avdManager = Sdk.getCurrent().getAvdManager();
        Display display = PlatformUI.getWorkbench().getDisplay();
        ISdkLog log =
                new MessageBoxLog(String.format("Result of updating AVD '%s':", avdInfo.getName()), //$NON-NLS-1$
                        display, false);
        try
        {
            avdManager.updateAvd(avdInfo, log);
            // display the result
            if (log instanceof MessageBoxLog)
            {
                ((MessageBoxLog) log).displayResult(true);
            }
            SdkUtils.reloadAvds();

        }
        catch (IOException e)
        {
            status =
                    new Status(Status.ERROR, AndroidPlugin.PLUGIN_ID,
                            AndroidNLS.SdkUtils_COULD_NOT_REPAIR_AVD, e);
        }

        return status;
    }

    public static String getDefaultSkin(String targetName)
    {
        IAndroidTarget target = getTargetByName(targetName);
        return target != null ? target.getDefaultSkin() : "HVGA";
    }
}
