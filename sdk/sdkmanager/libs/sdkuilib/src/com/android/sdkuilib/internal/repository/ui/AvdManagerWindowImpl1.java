/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository.ui;


import com.android.SdkConstants;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.internal.repository.ITaskFactory;
import com.android.sdkuilib.internal.repository.AboutDialog;
import com.android.sdkuilib.internal.repository.MenuBarWrapper;
import com.android.sdkuilib.internal.repository.SettingsController;
import com.android.sdkuilib.internal.repository.SettingsDialog;
import com.android.sdkuilib.internal.repository.UpdaterData;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.widgets.DeviceCreationDialog;
import com.android.sdkuilib.repository.AvdManagerWindow.AvdInvocationContext;
import com.android.sdkuilib.repository.ISdkChangeListener;
import com.android.sdkuilib.repository.SdkUpdaterWindow;
import com.android.utils.ILogger;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an intermediate version of the {@link AvdManagerPage}
 * wrapped in its own standalone window for use from the SDK Manager 2.
 */
public class AvdManagerWindowImpl1 {

    private static final String APP_NAME = "Android Virtual Device Manager";
    private static final String APP_NAME_MAC_MENU = "AVD Manager";
    private static final String SIZE_POS_PREFIX = "avdman1"; //$NON-NLS-1$

    private final Shell mParentShell;
    private final AvdInvocationContext mContext;
    /** Internal data shared between the window and its pages. */
    private final UpdaterData mUpdaterData;
    /** True if this window created the UpdaterData, in which case it needs to dispose it. */
    private final boolean mOwnUpdaterData;
    private final DeviceManager mDeviceManager;


    // --- UI members ---

    protected Shell mShell;
    private AvdManagerPage mAvdPage;
    private SettingsController mSettingsController;

    /**
     * Creates a new window. Caller must call open(), which will block.
     *
     * @param parentShell Parent shell.
     * @param sdkLog Logger. Cannot be null.
     * @param osSdkRoot The OS path to the SDK root.
     * @param context The {@link AvdInvocationContext} to change the behavior depending on who's
     *  opening the SDK Manager.
     */
    public AvdManagerWindowImpl1(
            Shell parentShell,
            ILogger sdkLog,
            String osSdkRoot,
            AvdInvocationContext context) {
        mParentShell = parentShell;
        mContext = context;
        mUpdaterData = new UpdaterData(osSdkRoot, sdkLog);
        mOwnUpdaterData = true;
        mDeviceManager = new DeviceManager(sdkLog);
    }

    /**
     * Creates a new window. Caller must call open(), which will block.
     * <p/>
     * This is to be used when the window is opened from {@link SdkUpdaterWindowImpl2}
     * to share the same {@link UpdaterData} structure.
     *
     * @param parentShell Parent shell.
     * @param updaterData The parent's updater data.
     * @param context The {@link AvdInvocationContext} to change the behavior depending on who's
     *  opening the SDK Manager.
     */
    public AvdManagerWindowImpl1(
            Shell parentShell,
            UpdaterData updaterData,
            AvdInvocationContext context) {
        mParentShell = parentShell;
        mContext = context;
        mUpdaterData = updaterData;
        mOwnUpdaterData = false;
        mDeviceManager = new DeviceManager(mUpdaterData.getSdkLog());
    }

    /**
     * Opens the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        if (mParentShell == null) {
            Display.setAppName(APP_NAME); //$hide$ (hide from SWT designer)
        }

        createShell();
        preCreateContent();
        createContents();
        createMenuBar();
        mShell.open();
        mShell.layout();

        boolean ok = postCreateContent();

        if (ok && mContext == AvdInvocationContext.STANDALONE) {
            Display display = Display.getDefault();
            while (!mShell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }

            dispose();  //$hide$
        }
    }

    private void createShell() {
        // The AVD Manager must use a shell trim when standalone
        // or a dialog trim when invoked from somewhere else.
        int style = SWT.SHELL_TRIM;
        if (mContext != AvdInvocationContext.STANDALONE) {
            style |= SWT.APPLICATION_MODAL;
        }

        mShell = new Shell(mParentShell, style);
        mShell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                ShellSizeAndPos.saveSizeAndPos(mShell, SIZE_POS_PREFIX);    //$hide$
                onAndroidSdkUpdaterDispose();                               //$hide$
                mAvdPage.dispose();                                         //$hide$
            }
        });

        GridLayout glShell = new GridLayout(2, false);
        glShell.verticalSpacing = 0;
        glShell.horizontalSpacing = 0;
        glShell.marginWidth = 0;
        glShell.marginHeight = 0;
        mShell.setLayout(glShell);

        mShell.setMinimumSize(new Point(500, 300));
        mShell.setSize(700, 500);
        mShell.setText(APP_NAME);

        ShellSizeAndPos.loadSizeAndPos(mShell, SIZE_POS_PREFIX);
    }

    private void createContents() {

        mAvdPage = new AvdManagerPage(mShell, SWT.NONE, mUpdaterData, mDeviceManager);
        mAvdPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    }

    @SuppressWarnings("unused")
    // MenuBarWrapper works using side effects
    private void createMenuBar() {
        Menu menuBar = new Menu(mShell, SWT.BAR);
        mShell.setMenuBar(menuBar);

        // Only create the tools menu when running as standalone.
        // We don't need the tools menu when invoked from the IDE, or the SDK Manager
        // or from the AVD Chooser dialog. The only point of the tools menu is to
        // get the about box, and invoke Tools > SDK Manager, which we don't
        // need to do in these cases.
        if (mContext == AvdInvocationContext.STANDALONE) {

            MenuItem menuBarTools = new MenuItem(menuBar, SWT.CASCADE);
            menuBarTools.setText("Tools");

            Menu menuTools = new Menu(menuBarTools);
            menuBarTools.setMenu(menuTools);

            MenuItem manageSdk = new MenuItem(menuTools, SWT.NONE);
            manageSdk.setText("Manage SDK...");
            manageSdk.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    onSdkManager();
                }
            });

            try {
                new MenuBarWrapper(APP_NAME_MAC_MENU, menuTools) {
                    @Override
                    public void onPreferencesMenuSelected() {
                        SettingsDialog sd = new SettingsDialog(mShell, mUpdaterData);
                        sd.open();
                    }

                    @Override
                    public void onAboutMenuSelected() {
                        AboutDialog ad = new AboutDialog(mShell, mUpdaterData);
                        ad.open();
                    }

                    @Override
                    public void printError(String format, Object... args) {
                        if (mUpdaterData != null) {
                            mUpdaterData.getSdkLog().error(null, format, args);
                        }
                    }
                };
            } catch (Throwable e) {
                mUpdaterData.getSdkLog().error(e, "Failed to setup menu bar");
                e.printStackTrace();
            }
        }

        MenuItem menuBarDevices = new MenuItem(menuBar, SWT.CASCADE);
        menuBarDevices.setText("Devices");
        setupDevices(menuBarDevices);
    }


    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // --- Public API -----------

    /**
     * Adds a new listener to be notified when a change is made to the content of the SDK.
     */
    public void addListener(ISdkChangeListener listener) {
        mUpdaterData.addListeners(listener);
    }

    /**
     * Removes a new listener to be notified anymore when a change is made to the content of
     * the SDK.
     */
    public void removeListener(ISdkChangeListener listener) {
        mUpdaterData.removeListener(listener);
    }

    // --- Internals & UI Callbacks -----------

    /**
     * Called before the UI is created.
     */
    private void preCreateContent() {
        mUpdaterData.setWindowShell(mShell);
        // We need the UI factory to create the UI
        mUpdaterData.setImageFactory(new ImageFactory(mShell.getDisplay()));
        // Note: we can't create the TaskFactory yet because we need the UI
        // to be created first, so this is done in postCreateContent().
    }

    /**
     * Once the UI has been created, initializes the content.
     * This creates the pages, selects the first one, setup sources and scan for local folders.
     *
     * Returns true if we should show the window.
     */
    private boolean postCreateContent() {
        setWindowImage(mShell);

        setupSources();
        initializeSettings();

        if (mUpdaterData.checkIfInitFailed()) {
            return false;
        }

        mUpdaterData.broadcastOnSdkLoaded();

        return true;
    }

    /**
     * Creates the icon of the window shell.
     *
     * @param shell The shell on which to put the icon
     */
    private void setWindowImage(Shell shell) {
        String imageName = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            imageName = "android_icon_128.png";
        }

        if (mUpdaterData != null) {
            ImageFactory imgFactory = mUpdaterData.getImageFactory();
            if (imgFactory != null) {
                shell.setImage(imgFactory.getImageByName(imageName));
            }
        }
    }

    /**
     * Called by the main loop when the window has been disposed.
     */
    private void dispose() {
        mUpdaterData.getSources().saveUserAddons(mUpdaterData.getSdkLog());
    }

    /**
     * Callback called when the window shell is disposed.
     */
    private void onAndroidSdkUpdaterDispose() {
        if (mOwnUpdaterData && mUpdaterData != null) {
            ImageFactory imgFactory = mUpdaterData.getImageFactory();
            if (imgFactory != null) {
                imgFactory.dispose();
            }
        }
    }

    /**
     * Used to initialize the sources.
     */
    private void setupSources() {
        mUpdaterData.setupDefaultSources();
    }

    /**
     * Sets up the devices in the device menu.
     */
    @SuppressWarnings("unused")
    private void setupDevices(final MenuItem menuBarDevices) {
        Menu menuDevices = new Menu(menuBarDevices);
        menuBarDevices.setMenu(menuDevices);

        MenuItem createDevice = new MenuItem(menuDevices, SWT.NONE);
        createDevice.setText("Create New Device");
        createDevice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceCreationDialog dlg = new DeviceCreationDialog(
                        mShell, mDeviceManager, mUpdaterData.getImageFactory(), null);
                if (dlg.open() == Window.OK) {
                    setupDevices(menuBarDevices);
                }
            }
        });
        new MenuItem(menuDevices, SWT.SEPARATOR);

        Map<String, List<Device>> devices = new HashMap<String, List<Device>>();
        for (Device d : mDeviceManager.getDevices(mUpdaterData.getOsSdkRoot())) {
            List<Device> l;
            if (devices.containsKey(d.getManufacturer())) {
                l = devices.get(d.getManufacturer());
            } else {
                l = new ArrayList<Device>();
                devices.put(d.getManufacturer(), l);
            }
            l.add(d);
        }

        for (String manufacturer : devices.keySet()) {
            MenuItem manufacturerItem = new MenuItem(menuDevices, SWT.CASCADE);
            manufacturerItem.setText(manufacturer);
            Menu manufacturerMenu = new Menu(menuDevices);
            manufacturerItem.setMenu(manufacturerMenu);
            for (final Device d : devices.get(manufacturer)) {
                MenuItem deviceItem = new MenuItem(manufacturerMenu, SWT.NONE);
                deviceItem.setText(d.getName());
                deviceItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DeviceCreationDialog dlg = new DeviceCreationDialog(
                                mShell, mDeviceManager, mUpdaterData.getImageFactory(), d);
                        if(dlg.open() == Window.OK) {
                            setupDevices(menuBarDevices);
                        }
                    }
                });
            }
        }
    }

    /**
     * Initializes settings.
     * This must be called after addExtraPages(), which created a settings page.
     * Iterate through all the pages to find the first (and supposedly unique) setting page,
     * and use it to load and apply these settings.
     */
    private void initializeSettings() {
        mSettingsController = mUpdaterData.getSettingsController();
        mSettingsController.loadSettings();
        mSettingsController.applySettings();
    }

    private void onSdkManager() {
        ITaskFactory oldFactory = mUpdaterData.getTaskFactory();

        try {
            SdkUpdaterWindowImpl2 win = new SdkUpdaterWindowImpl2(
                    mShell,
                    mUpdaterData,
                    SdkUpdaterWindow.SdkInvocationContext.AVD_MANAGER);

            win.open();
        } catch (Exception e) {
            mUpdaterData.getSdkLog().error(e, "SDK Manager window error");
        } finally {
            mUpdaterData.setTaskFactory(oldFactory);
        }
    }
}
