/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout.configuration;

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_CONTEXT;
import static com.android.SdkConstants.FD_RES_LAYOUT;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.RES_QUALIFIER_SEP;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.ide.eclipse.adt.AdtUtils.isUiThread;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_ALL;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_DEVICE;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_DEVICE_CONFIG;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_FOLDER;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_LOCALE;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_RENDER_TARGET;
import static com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationClient.CHANGED_THEME;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.ide.common.sdk.LoadStatus;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.gle2.DomUtilities;
import com.android.ide.eclipse.adt.internal.editors.layout.gle2.IncludeFinder.Reference;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestInfo;
import com.android.ide.eclipse.adt.internal.resources.ResourceHelper;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.resources.ResourceType;
import com.android.resources.ScreenOrientation;
import com.android.resources.ScreenSize;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.DeviceManager.DevicesChangeListener;
import com.android.sdklib.devices.State;
import com.android.utils.Pair;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * The {@linkplain ConfigurationChooser} allows the user to pick a
 * {@link Configuration} by configuring various constraints.
 */
public class ConfigurationChooser extends Composite
        implements DevicesChangeListener, DisposeListener {
    /**
     * Settings name for file-specific configuration preferences, such as which theme or
     * device to render the current layout with
     */
    public final static QualifiedName NAME_CONFIG_STATE =
        new QualifiedName(AdtPlugin.PLUGIN_ID, "state");//$NON-NLS-1$

    private static final String ICON_SQUARE = "square";           //$NON-NLS-1$
    private static final String ICON_LANDSCAPE = "landscape";     //$NON-NLS-1$
    private static final String ICON_PORTRAIT = "portrait";       //$NON-NLS-1$
    private static final String ICON_LANDSCAPE_FLIP = "flip_landscape";//$NON-NLS-1$
    private static final String ICON_PORTRAIT_FLIP = "flip_portrait";//$NON-NLS-1$
    private static final String ICON_DISPLAY = "display";         //$NON-NLS-1$
    private static final String ICON_THEMES = "themes";           //$NON-NLS-1$
    private static final String ICON_ACTIVITY = "activity";       //$NON-NLS-1$

    /** The configuration state associated with this editor */
    private @NonNull Configuration mConfiguration = Configuration.create(this);

    /** Serialized state to use when initializing the configuration after the SDK is loaded */
    private String mInitialState;

    /** The client of the configuration editor */
    private final ConfigurationClient mClient;

    /** Counter for programmatic UI changes: if greater than 0, we're within a call */
    private int mDisableUpdates = 0;

    /** List of available devices */
    private List<Device> mDeviceList = Collections.emptyList();

    /** List of available targets */
    private final List<IAndroidTarget> mTargetList = new ArrayList<IAndroidTarget>();

    /** List of available themes */
    private final List<String> mThemeList = new ArrayList<String>();

    /** List of available locales */
    private final List<Locale > mLocaleList = new ArrayList<Locale>();

    /** The file being edited */
    private IFile mEditedFile;

    /** The {@link ProjectResources} for the edited file's project */
    private ProjectResources mResources;

    /** The target of the project of the file being edited. */
    private IAndroidTarget mProjectTarget;

    /** Dropdown for configurations */
    private ToolItem mConfigCombo;

    /** Dropdown for devices */
    private ToolItem mDeviceCombo;

    /** Dropdown for device states */
    private ToolItem mOrientationCombo;

    /** Dropdown for themes */
    private ToolItem mThemeCombo;

    /** Dropdown for locales */
    private ToolItem mLocaleCombo;

    /** Dropdown for activities */
    private ToolItem mActivityCombo;

    /** Dropdown for rendering targets */
    private ToolItem mTargetCombo;

    /** Whether the SDK has changed since the last model reload; if so we must reload targets */
    private boolean mSdkChanged = true;

    /**
     * Creates a new {@linkplain ConfigurationChooser} and adds it to the
     * parent. The method also receives custom buttons to set into the
     * configuration composite. The list is organized as an array of arrays.
     * Each array represents a group of buttons thematically grouped together.
     *
     * @param client the client embedding this configuration chooser
     * @param parent The parent composite.
     * @param initialState The initial state (serialized form) to use for the
     *            configuration
     */
    public ConfigurationChooser(
            @NonNull ConfigurationClient client,
            Composite parent,
            @Nullable String initialState) {
        super(parent, SWT.NONE);
        mClient = client;

        setVisible(false); // Delayed until the targets are loaded

        mInitialState = initialState;
        setLayout(new GridLayout(1, false));

        IconFactory icons = IconFactory.getInstance();

        // TODO: Consider switching to a CoolBar instead
        ToolBar toolBar = new ToolBar(this, SWT.WRAP | SWT.FLAT | SWT.RIGHT | SWT.HORIZONTAL);
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mConfigCombo = new ToolItem(toolBar, SWT.DROP_DOWN | SWT.BOLD);
        mConfigCombo.setImage(null);
        mConfigCombo.setToolTipText("Configuration to render this layout with in Eclipse");

        @SuppressWarnings("unused")
        ToolItem separator2 = new ToolItem(toolBar, SWT.SEPARATOR);

        mDeviceCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mDeviceCombo.setImage(icons.getIcon(ICON_DISPLAY));

        @SuppressWarnings("unused")
        ToolItem separator3 = new ToolItem(toolBar, SWT.SEPARATOR);

        mOrientationCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mOrientationCombo.setImage(icons.getIcon(ICON_PORTRAIT));
        mOrientationCombo.setToolTipText("Go to next state");

        @SuppressWarnings("unused")
        ToolItem separator4 = new ToolItem(toolBar, SWT.SEPARATOR);

        mThemeCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mThemeCombo.setImage(icons.getIcon(ICON_THEMES));

        @SuppressWarnings("unused")
        ToolItem separator5 = new ToolItem(toolBar, SWT.SEPARATOR);

        mActivityCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mActivityCombo.setToolTipText("Associated activity or fragment providing context");
        // The JDT class icon is lopsided, presumably because they've left room in the
        // bottom right corner for badges (for static, final etc). Unfortunately, this
        // means that the icon looks out of place when sitting close to the language globe
        // icon, the theme icon, etc so that it looks vertically misaligned:
        //mActivityCombo.setImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS));
        // ...so use one that is centered instead:
        mActivityCombo.setImage(icons.getIcon(ICON_ACTIVITY));

        @SuppressWarnings("unused")
        ToolItem separator6 = new ToolItem(toolBar, SWT.SEPARATOR);

        //ToolBar rightToolBar = new ToolBar(this, SWT.WRAP | SWT.FLAT | SWT.RIGHT | SWT.HORIZONTAL);
        //rightToolBar.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        ToolBar rightToolBar = toolBar;

        mLocaleCombo = new ToolItem(rightToolBar, SWT.DROP_DOWN);
        mLocaleCombo.setImage(LocaleManager.getGlobeIcon());
        mLocaleCombo.setToolTipText("Locale to use when rendering layouts in Eclipse");

        @SuppressWarnings("unused")
        ToolItem separator7 = new ToolItem(rightToolBar, SWT.SEPARATOR);

        mTargetCombo = new ToolItem(rightToolBar, SWT.DROP_DOWN);
        mTargetCombo.setImage(AdtPlugin.getAndroidLogo());
        mTargetCombo.setToolTipText("Android version to use when rendering layouts in Eclipse");

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Object source = e.getSource();

                if (source == mConfigCombo) {
                    ConfigurationMenuListener.show(ConfigurationChooser.this, mConfigCombo);
                } else if (source == mActivityCombo) {
                    ActivityMenuListener.show(ConfigurationChooser.this, mActivityCombo);
                } else if (source == mLocaleCombo) {
                    LocaleMenuListener.show(ConfigurationChooser.this, mLocaleCombo);
                } else if (source == mDeviceCombo) {
                    DeviceMenuListener.show(ConfigurationChooser.this, mDeviceCombo);
                } else if (source == mTargetCombo) {
                    TargetMenuListener.show(ConfigurationChooser.this, mTargetCombo);
                } else if (source == mThemeCombo) {
                    ThemeMenuAction.showThemeMenu(ConfigurationChooser.this, mThemeCombo,
                            mThemeList);
                } else if (source == mOrientationCombo) {
                    if (e.detail == SWT.ARROW) {
                        OrientationMenuAction.showMenu(ConfigurationChooser.this,
                                mOrientationCombo);
                    } else {
                        gotoNextState();
                    }
                }
            }
        };
        mConfigCombo.addSelectionListener(listener);
        mActivityCombo.addSelectionListener(listener);
        mLocaleCombo.addSelectionListener(listener);
        mDeviceCombo.addSelectionListener(listener);
        mTargetCombo.addSelectionListener(listener);
        mThemeCombo.addSelectionListener(listener);
        mOrientationCombo.addSelectionListener(listener);

        addDisposeListener(this);
    }

    IFile getEditedFile() {
        return mEditedFile;
    }

    IProject getProject() {
        return mEditedFile.getProject();
    }

    ConfigurationClient getClient() {
        return mClient;
    }

    ProjectResources getResources() {
        return mResources;
    }

    /**
     * Returns the full, complete {@link FolderConfiguration}
     *
     * @return the full configuration
     */
    public FolderConfiguration getFullConfiguration() {
        return mConfiguration.getFullConfig();
    }

    /**
     * Returns the project target
     *
     * @return the project target
     */
    IAndroidTarget getProjectTarget() {
        return mProjectTarget;
    }

    /**
     * Returns the configuration being edited by this {@linkplain ConfigurationChooser}
     *
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Returns the list of locales
     * @return a list of {@link ResourceQualifier} pairs
     */
    @NonNull
    public List<Locale> getLocaleList() {
        return mLocaleList;
    }

    /**
     * Returns the list of available devices
     *
     * @return a list of {@link Device} objects
     */
    @NonNull
    public List<Device> getDeviceList() {
        return mDeviceList;
    }

    /**
     * Returns the list of available render targets
     *
     * @return a list of {@link IAndroidTarget} objects
     */
    @NonNull
    public List<IAndroidTarget> getTargetList() {
        return mTargetList;
    }

    // ---- Configuration State Lookup ----

    /**
     * Returns the rendering target to be used
     *
     * @return the target
     */
    @NonNull
    public IAndroidTarget getTarget() {
        IAndroidTarget target = mConfiguration.getTarget();
        if (target == null) {
            target = mProjectTarget;
        }

        return target;
    }

    /**
     * Returns the current device string, or null if no device is selected
     *
     * @return the device name, or null
     */
    @Nullable
    public String getDeviceName() {
        Device device = mConfiguration.getDevice();
        if (device != null) {
            return device.getName();
        }

        return null;
    }

    /**
     * Returns the current theme, or null if none has been selected
     *
     * @return the theme name, or null
     */
    @Nullable
    public String getThemeName() {
        String theme = mConfiguration.getTheme();
        if (theme != null) {
            theme = ResourceHelper.styleToTheme(theme);
        }

        return theme;
    }

    /** Move to the next device state, changing the icon if it changes orientation */
    private void gotoNextState() {
        State state = mConfiguration.getDeviceState();
        State flipped = mConfiguration.getNextDeviceState(state);
        if (flipped != state) {
            selectDeviceState(flipped);
            onDeviceConfigChange();
        }
    }

    // ---- Implements DisposeListener ----

    @Override
    public void widgetDisposed(DisposeEvent e) {
        dispose();
    }

    @Override
    public void dispose() {
        if (!isDisposed()) {
            super.dispose();

            final Sdk sdk = Sdk.getCurrent();
            if (sdk != null) {
                DeviceManager manager = sdk.getDeviceManager();
                manager.unregisterListener(this);
            }
        }
    }

    // ---- Init and reset/reload methods ----

    /**
     * Sets the reference to the file being edited.
     * <p/>The UI is initialized in {@link #onXmlModelLoaded()} which is called as the XML model is
     * loaded (or reloaded as the SDK/target changes).
     *
     * @param file the file being opened
     *
     * @see #onXmlModelLoaded()
     * @see #replaceFile(IFile)
     * @see #changeFileOnNewConfig(IFile)
     */
    public void setFile(IFile file) {
        mEditedFile = file;
    }

    /**
     * Replaces the UI with a given file configuration. This is meant to answer the user
     * explicitly opening a different version of the same layout from the Package Explorer.
     * <p/>This attempts to keep the current config, but may change it if it's not compatible or
     * not the best match
     * @param file the file being opened.
     */
    public void replaceFile(IFile file) {
        // if there is no previous selection, revert to default mode.
        if (mConfiguration.getDevice() == null) {
            setFile(file); // onTargetChanged will be called later.
            return;
        }

        mEditedFile = file;
        IProject project = mEditedFile.getProject();
        mResources = ResourceManager.getInstance().getProjectResources(project);

        ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
        mConfiguration.setEditedConfig(resFolder.getConfiguration());

        mDisableUpdates++; // we do not want to trigger onXXXChange when setting
                           // new values in the widgets.

        try {
            // only attempt to do anything if the SDK and targets are loaded.
            LoadStatus sdkStatus = AdtPlugin.getDefault().getSdkLoadStatus();
            if (sdkStatus == LoadStatus.LOADED) {
                setVisible(true);

                LoadStatus targetStatus = Sdk.getCurrent().checkAndLoadTargetData(mProjectTarget,
                        null /*project*/);

                if (targetStatus == LoadStatus.LOADED) {

                    // update the current config selection to make sure it's
                    // compatible with the new file
                    ConfigurationMatcher matcher = new ConfigurationMatcher(this);
                    matcher.adaptConfigSelection(true /*needBestMatch*/);
                    mConfiguration.syncFolderConfig();

                    // update the string showing the config value
                    selectConfiguration(mConfiguration.getEditedConfig());
                    updateActivity();
                }
            }
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Updates the UI with a new file that was opened in response to a config change.
     * @param file the file being opened.
     *
     * @see #replaceFile(IFile)
     */
    public void changeFileOnNewConfig(IFile file) {
        mEditedFile = file;
        IProject project = mEditedFile.getProject();
        mResources = ResourceManager.getInstance().getProjectResources(project);

        ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
        FolderConfiguration config = resFolder.getConfiguration();
        mConfiguration.setEditedConfig(config);

        // All that's needed is to update the string showing the config value
        // (since the config combo settings chosen by the user).
        selectConfiguration(config);
    }

    /**
     * Resets the configuration chooser to reflect the given file configuration. This is
     * intended to be used by the "Show Included In" functionality where the user has
     * picked a non-default configuration (such as a particular landscape layout) and the
     * configuration chooser must be switched to a landscape layout. This method will
     * trigger a model change.
     * <p>
     * This will NOT trigger a redraw event!
     * <p>
     * FIXME: We are currently setting the configuration file to be the configuration for
     * the "outer" (the including) file, rather than the inner file, which is the file the
     * user is actually editing. We need to refine this, possibly with a way for the user
     * to choose which configuration they are editing. And in particular, we should be
     * filtering the configuration chooser to only show options in the outer configuration
     * that are compatible with the inner included file.
     *
     * @param file the file to be configured
     */
    public void resetConfigFor(IFile file) {
        setFile(file);

        IFolder parent = (IFolder) mEditedFile.getParent();
        ResourceFolder resFolder = mResources.getResourceFolder(parent);
        if (resFolder != null) {
            mConfiguration.setEditedConfig(resFolder.getConfiguration());
        } else {
            mConfiguration.setEditedConfig(FolderConfiguration.getConfig(
                    parent.getName().split(RES_QUALIFIER_SEP)));
        }

        onXmlModelLoaded();
    }


    /**
     * Sets the current configuration to match the given folder configuration,
     * the given theme name, the given device and device state.
     *
     * @param configuration new folder configuration to use
     */
    public void setConfiguration(@NonNull Configuration configuration) {
        if (mClient != null) {
            mClient.aboutToChange(CHANGED_ALL);
        }

        Configuration oldConfiguration = mConfiguration;
        mConfiguration = configuration;

        if (mClient != null) {
            mClient.changed(CHANGED_ALL);
        }

        selectTheme(configuration.getTheme());
        selectLocale(configuration.getLocale());
        selectDevice(configuration.getDevice());
        selectDeviceState(configuration.getDeviceState());
        selectTarget(configuration.getTarget());
        selectActivity(configuration.getActivity());

        // This may be a second refresh after triggered by theme above
        if (mClient != null) {
            boolean accepted = mClient.changed(CHANGED_ALL);
            if (!accepted) {
                configuration = oldConfiguration;
                selectTheme(configuration.getTheme());
                selectLocale(configuration.getLocale());
                selectDevice(configuration.getDevice());
                selectDeviceState(configuration.getDeviceState());
                selectTarget(configuration.getTarget());
                selectActivity(configuration.getActivity());
                return;
            }
        }

        saveConstraints();
    }

    /**
     * Responds to the event that the basic SDK information finished loading.
     * @param target the possibly new target object associated with the file being edited (in case
     * the SDK path was changed).
     */
    public void onSdkLoaded(IAndroidTarget target) {
        // a change to the SDK means that we need to check for new/removed devices.
        mSdkChanged = true;

        // store the new target.
        mProjectTarget = target;

        mDisableUpdates++; // we do not want to trigger onXXXChange when setting
                           // new values in the widgets.
        try {
            // this is going to be followed by a call to onTargetLoaded.
            // So we can only care about the layout devices in this case.
            initDevices();
            initTargets();
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Responds to the XML model being loaded, either the first time or when the
     * Target/SDK changes.
     * <p>
     * This initializes the UI, either with the first compatible configuration
     * found, or it will attempt to restore a configuration if one is found to
     * have been saved in the file persistent storage.
     * <p>
     * If the SDK or target are not loaded, nothing will happen (but the method
     * must be called back when they are.)
     * <p>
     * The method automatically handles being called the first time after editor
     * creation, or being called after during SDK/Target changes (as long as
     * {@link #onSdkLoaded(IAndroidTarget)} is properly called).
     *
     * @return the target data for the rendering target used to render the
     *         layout
     *
     * @see #saveConstraints()
     * @see #onSdkLoaded(IAndroidTarget)
     */
    public AndroidTargetData onXmlModelLoaded() {
        AndroidTargetData targetData = null;

        // only attempt to do anything if the SDK and targets are loaded.
        LoadStatus sdkStatus = AdtPlugin.getDefault().getSdkLoadStatus();
        if (sdkStatus == LoadStatus.LOADED) {
            mDisableUpdates++; // we do not want to trigger onXXXChange when setting

            try {
                // init the devices if needed (new SDK or first time going through here)
                if (mSdkChanged) {
                    initDevices();
                    initTargets();
                    mSdkChanged = false;
                }

                IProject project = mEditedFile.getProject();

                Sdk currentSdk = Sdk.getCurrent();
                if (currentSdk != null) {
                    mProjectTarget = currentSdk.getTarget(project);
                }

                LoadStatus targetStatus = LoadStatus.FAILED;
                if (mProjectTarget != null) {
                    targetStatus = Sdk.getCurrent().checkAndLoadTargetData(mProjectTarget, null);
                    initTargets();
                }

                if (targetStatus == LoadStatus.LOADED) {
                    setVisible(true);
                    if (mResources == null) {
                        mResources = ResourceManager.getInstance().getProjectResources(project);
                    }
                    if (mConfiguration.getEditedConfig() == null) {
                        IFolder parent = (IFolder) mEditedFile.getParent();
                        ResourceFolder resFolder = mResources.getResourceFolder(parent);
                        if (resFolder != null) {
                            mConfiguration.setEditedConfig(resFolder.getConfiguration());
                        } else {
                            mConfiguration.setEditedConfig(FolderConfiguration.getConfig(
                                    parent.getName().split(RES_QUALIFIER_SEP)));
                        }
                    }

                    targetData = Sdk.getCurrent().getTargetData(mProjectTarget);

                    // get the file stored state
                    boolean loadedConfigData = false;
                    String data = AdtPlugin.getFileProperty(mEditedFile, NAME_CONFIG_STATE);
                    if (mInitialState != null) {
                        data = mInitialState;
                        mInitialState = null;
                    }

                    if (data != null) {
                        loadedConfigData = mConfiguration.initialize(data);
                    }

                    // Load locale list. This must be run after we initialize the
                    // configuration above, since it attempts to sync the UI with
                    // the value loaded into the configuration.
                    updateLocales();

                    // If the current state was loaded from the persistent storage, we update the
                    // UI with it and then try to adapt it (which will handle incompatible
                    // configuration).
                    // Otherwise, just look for the first compatible configuration.
                    ConfigurationMatcher matcher = new ConfigurationMatcher(this);
                    if (loadedConfigData) {
                        // first make sure we have the config to adapt
                        selectDevice(mConfiguration.getDevice());
                        selectDeviceState(mConfiguration.getDeviceState());
                        mConfiguration.syncFolderConfig();

                        matcher.adaptConfigSelection(false);

                        IAndroidTarget target = mConfiguration.getTarget();
                        selectTarget(target);
                        targetData = Sdk.getCurrent().getTargetData(target);
                    } else {
                        matcher.findAndSetCompatibleConfig(false);

                        // Default to modern layout lib
                        IProject p = mEditedFile.getProject();
                        IAndroidTarget target = ConfigurationMatcher.findDefaultRenderTarget(p);
                        if (target != null) {
                            targetData = Sdk.getCurrent().getTargetData(target);
                            selectTarget(target);
                        }
                    }

                    // Update activity: This is done before updateThemes() since
                    // the themes selection can depend on the currently selected activity
                    // (e.g. when there are manifest registrations for the theme to use
                    // for a given activity)
                    updateActivity();

                    // Update themes. This is done after updating the devices above,
                    // since we want to look at the chosen device size to decide
                    // what the default theme (for example, with Honeycomb we choose
                    // Holo as the default theme but only if the screen size is XLARGE
                    // (and of course only if the manifest does not specify another
                    // default theme).
                    updateThemes();

                    // update the string showing the config value
                    selectConfiguration(mConfiguration.getEditedConfig());

                    // compute the final current config
                    mConfiguration.syncFolderConfig();
                }
            } finally {
                mDisableUpdates--;
            }
        }

        return targetData;
    }

    /**
     * An alternate layout for this layout has been created. This means that the
     * current layout may no longer be a best fit. However, since we support multiple
     * layouts being open at the same time, we need to adjust the current configuration
     * back to something where this layout <b>is</b> a best match.
     */
    public void onAlternateLayoutCreated() {
        IFile best = ConfigurationMatcher.getBestFileMatch(this);
        if (best != null && !best.equals(mEditedFile)) {
            ConfigurationMatcher matcher = new ConfigurationMatcher(this);
            matcher.adaptConfigSelection(true /*needBestMatch*/);
            mConfiguration.syncFolderConfig();
            if (mClient != null) {
                mClient.changed(CHANGED_ALL);
            }
        }
    }

    /**
     * Loads the list of {@link Device}s and inits the UI with it.
     */
    private void initDevices() {
        final Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            mDeviceList = sdk.getDevices();
            DeviceManager manager = sdk.getDeviceManager();
            // This method can be called more than once, so avoid duplicate entries
            manager.unregisterListener(this);
            manager.registerListener(this);
        } else {
            mDeviceList = new ArrayList<Device>();
        }

        // fill with the devices
        if (!mDeviceList.isEmpty()) {
            Device first = mDeviceList.get(0);
            selectDevice(first);
            List<State> states = first.getAllStates();
            selectDeviceState(states.get(0));
        } else {
            selectDevice(null);
        }
    }

    /**
     * Loads the list of {@link IAndroidTarget} and inits the UI with it.
     */
    private void initTargets() {
        mTargetList.clear();

        IAndroidTarget renderingTarget = mConfiguration.getTarget();

        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget[] targets = currentSdk.getTargets();
            IAndroidTarget match = null;
            for (int i = 0 ; i < targets.length; i++) {
                // FIXME: add check based on project minSdkVersion
                if (targets[i].hasRenderingLibrary()) {
                    mTargetList.add(targets[i]);

                    if (renderingTarget != null) {
                        // use equals because the rendering could be from a previous SDK, so
                        // it may not be the same instance.
                        if (renderingTarget.equals(targets[i])) {
                            match = targets[i];
                        }
                    } else if (mProjectTarget == targets[i]) {
                        match = targets[i];
                    }
                }
            }

            if (match == null) {
                selectTarget(null);

                // the rendering target is the same as the project.
                renderingTarget = mProjectTarget;
            } else {
                selectTarget(match);

                // set the rendering target to the new object.
                renderingTarget = match;
            }
        }
    }

    /** Update the toolbar whenever a label has changed, to not only
     * cause the layout in the current toolbar to update, but to possibly
     * wrap the toolbars and update the layout of the surrounding area.
     */
    private void resizeToolBar() {
        Point size = getSize();
        Point newSize = computeSize(size.x, SWT.DEFAULT, true);
        setSize(newSize);
        Composite parent = getParent();
        parent.layout();
        parent.redraw();
    }


    Image getOrientationIcon(ScreenOrientation orientation, boolean flip) {
        IconFactory icons = IconFactory.getInstance();
        switch (orientation) {
            case LANDSCAPE:
                return icons.getIcon(flip ? ICON_LANDSCAPE_FLIP : ICON_LANDSCAPE);
            case SQUARE:
                return icons.getIcon(ICON_SQUARE);
            case PORTRAIT:
            default:
                return icons.getIcon(flip ? ICON_PORTRAIT_FLIP : ICON_PORTRAIT);
        }
    }

    ImageDescriptor getOrientationImage(ScreenOrientation orientation, boolean flip) {
        IconFactory icons = IconFactory.getInstance();
        switch (orientation) {
            case LANDSCAPE:
                return icons.getImageDescriptor(flip ? ICON_LANDSCAPE_FLIP : ICON_LANDSCAPE);
            case SQUARE:
                return icons.getImageDescriptor(ICON_SQUARE);
            case PORTRAIT:
            default:
                return icons.getImageDescriptor(flip ? ICON_PORTRAIT_FLIP : ICON_PORTRAIT);
        }
    }

    @NonNull
    ScreenOrientation getOrientation(State state) {
        FolderConfiguration config = DeviceConfigHelper.getFolderConfig(state);
        ScreenOrientation orientation = null;
        if (config != null && config.getScreenOrientationQualifier() != null) {
            orientation = config.getScreenOrientationQualifier().getValue();
        }

        if (orientation == null) {
            orientation = ScreenOrientation.PORTRAIT;
        }

        return orientation;
    }

    /**
     * Stores the current config selection into the edited file such that we can
     * bring it back the next time this layout is opened.
     */
    public void saveConstraints() {
        String description = mConfiguration.toPersistentString();
        AdtPlugin.setFileProperty(mEditedFile, NAME_CONFIG_STATE, description);
    }

    // ---- Setting the current UI state ----

    void selectDeviceState(@Nullable State state) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mOrientationCombo.setData(state);

            State nextState = mConfiguration.getNextDeviceState(state);
            mOrientationCombo.setImage(getOrientationIcon(getOrientation(state),
                    nextState != state));
        } finally {
            mDisableUpdates--;
        }
    }

    void selectTarget(IAndroidTarget target) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mTargetCombo.setData(target);
            String label = getRenderingTargetLabel(target, true);
            mTargetCombo.setText(label);
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Selects a given {@link Device} in the device combo, if it is found.
     * @param device the device to select
     * @return true if the device was found.
     */
    boolean selectDevice(@Nullable Device device) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mDeviceCombo.setData(device);
            if (device != null) {
                mDeviceCombo.setText(getDeviceLabel(device, true));
            } else {
                mDeviceCombo.setText("Device");
            }
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }

        return false;
    }

    void selectActivity(@Nullable String fqcn) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            if (fqcn != null) {
                mActivityCombo.setData(fqcn);
                String label = getActivityLabel(fqcn, true);
                mActivityCombo.setText(label);
            } else {
                mActivityCombo.setText("(Select)");
            }
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    void selectTheme(@Nullable String theme) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            assert theme == null ||  theme.startsWith(STYLE_RESOURCE_PREFIX)
                    || theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX) : theme;
            mThemeCombo.setData(theme);
            if (theme != null) {
                mThemeCombo.setText(getThemeLabel(theme, true));
            } else {
                // FIXME eclipse claims this is dead code.
                mThemeCombo.setText("(Set Theme)");
            }
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    void selectLocale(@Nullable Locale locale) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mLocaleCombo.setData(locale);
            String label = Strings.nullToEmpty(getLocaleLabel(this, locale, true));
            mLocaleCombo.setText(label);

            Image image = getFlagImage(locale);
            mLocaleCombo.setImage(image);

            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    @NonNull
    Image getFlagImage(@Nullable Locale locale) {
        if (locale != null) {
            return locale.getFlagImage();
        }

        return LocaleManager.getGlobeIcon();
    }

    private void selectConfiguration(FolderConfiguration fileConfig) {
        assert isUiThread();
        try {
            String current = mEditedFile.getParent().getName();
            if (current.equals(FD_RES_LAYOUT)) {
                current = "default";
            }

            // Pretty things up a bit
            //if (current == null || current.equals("default")) {
            //    current = "Default Configuration";
            //}
            mConfigCombo.setText(current);
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Finds a locale matching the config from a file.
     *
     * @param language the language qualifier or null if none is set.
     * @param region the region qualifier or null if none is set.
     * @return true if there was a change in the combobox as a result of
     *         applying the locale
     */
    private boolean setLocale(@Nullable Locale locale) {
        boolean changed = !Objects.equal(mConfiguration.getLocale(), locale);
        selectLocale(locale);

        return changed;
    }

    // ---- Creating UI labels ----

    /**
     * Returns a suitable label to use to display the given activity
     *
     * @param fqcn the activity class to look up a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getActivityLabel(String fqcn, boolean brief) {
        if (brief) {
            String label = fqcn;
            int packageIndex = label.lastIndexOf('.');
            if (packageIndex != -1) {
                label = label.substring(packageIndex + 1);
            }
            int innerClass = label.lastIndexOf('$');
            if (innerClass != -1) {
                label = label.substring(innerClass + 1);
            }

            // Also strip out the "Activity" or "Fragment" common suffix
            // if this is a long name
            if (label.endsWith("Activity") && label.length() > 8 + 12) { // 12 chars + 8 in suffix
                label = label.substring(0, label.length() - 8);
            } else if (label.endsWith("Fragment") && label.length() > 8 + 12) {
                label = label.substring(0, label.length() - 8);
            }

            return label;
        }

        return fqcn;
    }

    /**
     * Returns a suitable label to use to display the given theme
     *
     * @param theme the theme to produce a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getThemeLabel(String theme, boolean brief) {
        theme = ResourceHelper.styleToTheme(theme);

        if (brief) {
            int index = theme.lastIndexOf('.');
            if (index < theme.length() - 1) {
                return theme.substring(index + 1);
            }
        }
        return theme;
    }

    /**
     * Returns a suitable label to use to display the given rendering target
     *
     * @param target the target to produce a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getRenderingTargetLabel(IAndroidTarget target, boolean brief) {
        if (target == null) {
            return "<null>";
        }

        AndroidVersion version = target.getVersion();

        if (brief) {
            if (target.isPlatform()) {
                return Integer.toString(version.getApiLevel());
            } else {
                return target.getName() + ':' + Integer.toString(version.getApiLevel());
            }
        }

        String label = String.format("API %1$d: %2$s",
                version.getApiLevel(),
                target.getShortClasspathName());

        return label;
    }

    /**
     * Returns a suitable label to use to display the given device
     *
     * @param device the device to produce a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getDeviceLabel(@Nullable Device device, boolean brief) {
        if (device == null) {
            return "";
        }
        String name = device.getName();

        if (brief) {
            // Produce a really brief summary of the device name, suitable for
            // use in the narrow space available in the toolbar for example
            int nexus = name.indexOf("Nexus"); //$NON-NLS-1$
            if (nexus != -1) {
                int begin = name.indexOf('(');
                if (begin != -1) {
                    begin++;
                    int end = name.indexOf(')', begin);
                    if (end != -1) {
                        return name.substring(begin, end).trim();
                    }
                }
            }
        }

        return name;
    }

    /**
     * Returns a suitable label to use to display the given locale
     *
     * @param chooser the chooser, if known
     * @param locale the locale to look up a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    @Nullable
    public static String getLocaleLabel(
            @Nullable ConfigurationChooser chooser,
            @Nullable Locale locale,
            boolean brief) {
        if (locale == null) {
            return null;
        }

        if (!locale.hasLanguage()) {
            if (brief) {
                // Just use the icon
                return "";
            }

            boolean hasLocale = false;
            ResourceRepository projectRes = chooser != null ? chooser.mClient.getProjectResources()
                    : null;
            if (projectRes != null) {
                hasLocale = projectRes.getLanguages().size() > 0;
            }

            if (hasLocale) {
                return "Other";
            } else {
                return "Any";
            }
        }

        String languageCode = locale.language.getValue();
        String languageName = LocaleManager.getLanguageName(languageCode);

        if (!locale.hasRegion()) {
            // TODO: Make the region string use "Other" instead of "Any" if
            // there is more than one region for a given language
            //if (regions.size() > 0) {
            //    return String.format("%1$s / Other", language);
            //} else {
            //    return String.format("%1$s / Any", language);
            //}
            if (!brief && languageName != null) {
                return String.format("%1$s (%2$s)", languageName, languageCode);
            } else {
                return languageCode;
            }
        } else {
            String regionCode = locale.region.getValue();
            if (!brief && languageName != null) {
                String regionName = LocaleManager.getRegionName(regionCode);
                if (regionName != null) {
                    return String.format("%1$s (%2$s) in %3$s (%4$s)", languageName, languageCode,
                            regionName, regionCode);
                }
                return String.format("%1$s (%2$s) in %3$s", languageName, languageCode,
                        regionCode);
            }
            return String.format("%1$s / %2$s", languageCode, regionCode);
        }
    }

    // ---- Implements DevicesChangeListener ----

    @Override
    public void onDevicesChange() {
        final Sdk sdk = Sdk.getCurrent();
        mDeviceList = sdk.getDevices();
    }

    // ---- Reacting to UI changes ----

    /**
     * Called when the selection of the device combo changes.
     */
    void onDeviceChange() {
        // because changing the content of a combo triggers a change event, respect the
        // mDisableUpdates flag
        if (mDisableUpdates > 0) {
            return;
        }

        // Attempt to preserve the device state
        String stateName = null;
        Device prevDevice = mConfiguration.getDevice();
        State prevState = mConfiguration.getDeviceState();
        Device device = (Device) mDeviceCombo.getData();
        if (prevDevice != null && prevState != null && device != null) {
            // get the previous config, so that we can look for a close match
            FolderConfiguration oldConfig = DeviceConfigHelper.getFolderConfig(prevState);
            if (oldConfig != null) {
                stateName = ConfigurationMatcher.getClosestMatch(oldConfig, device.getAllStates());
            }
        }
        mConfiguration.setDevice(device, true);
        State newState = Configuration.getState(device, stateName);
        mConfiguration.setDeviceState(newState, true);
        selectDeviceState(newState);
        mConfiguration.syncFolderConfig();

        // Notify
        boolean accepted = mClient.changed(CHANGED_DEVICE | CHANGED_DEVICE_CONFIG);
        if (!accepted) {
            mConfiguration.setDevice(prevDevice, true);
            mConfiguration.setDeviceState(prevState, true);
            mConfiguration.syncFolderConfig();
            selectDevice(prevDevice);
            selectDeviceState(prevState);
            return;
        }

        saveConstraints();
    }

    /**
     * Called when the device config selection changes.
     */
    void onDeviceConfigChange() {
        // because changing the content of a combo triggers a change event, respect the
        // mDisableUpdates flag
        if (mDisableUpdates > 0) {
            return;
        }

        State prev = mConfiguration.getDeviceState();
        State state = (State) mOrientationCombo.getData();
        mConfiguration.setDeviceState(state, false);

        if (mClient != null) {
            boolean accepted = mClient.changed(CHANGED_DEVICE | CHANGED_DEVICE_CONFIG);
            if (!accepted) {
                mConfiguration.setDeviceState(prev, false);
                selectDeviceState(prev);
                return;
            }
        }

        saveConstraints();
    }

    /**
     * Call back for language combo selection
     */
    void onLocaleChange() {
        // because mLocaleList triggers onLocaleChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates > 0) {
            return;
        }

        Locale prev = mConfiguration.getLocale();
        Locale locale = (Locale) mLocaleCombo.getData();
        if (locale == null) {
            locale = Locale.ANY;
        }
        mConfiguration.setLocale(locale, false);

        if (mClient != null) {
            boolean accepted = mClient.changed(CHANGED_LOCALE);
            if (!accepted) {
                mConfiguration.setLocale(prev, false);
                selectLocale(prev);
            }
        }

        // Store locale project-wide setting
        mConfiguration.saveRenderState();
    }


    void onThemeChange() {
        if (mDisableUpdates > 0) {
            return;
        }

        String prev = mConfiguration.getTheme();
        mConfiguration.setTheme((String) mThemeCombo.getData());

        if (mClient != null) {
            boolean accepted = mClient.changed(CHANGED_THEME);
            if (!accepted) {
                mConfiguration.setTheme(prev);
                selectTheme(prev);
                return;
            }
        }

        saveConstraints();
    }

    void notifyFolderConfigChanged() {
        if (mDisableUpdates > 0 || mClient == null) {
            return;
        }

        if (mClient.changed(CHANGED_FOLDER)) {
            saveConstraints();
        }
    }

    void onSelectActivity() {
        if (mDisableUpdates > 0) {
            return;
        }

        String activity = (String) mActivityCombo.getData();
        mConfiguration.setActivity(activity);

        if (activity == null) {
            return;
        }

        // See if there is a default theme assigned to this activity, and if so, use it
        ManifestInfo manifest = ManifestInfo.get(mEditedFile.getProject());
        Map<String, String> activityThemes = manifest.getActivityThemes();
        String preferred = activityThemes.get(activity);
        if (preferred != null && !Objects.equal(preferred, mConfiguration.getTheme())) {
            // Yes, switch to it
            selectTheme(preferred);
            onThemeChange();
        }

        // Persist in XML
        if (mClient != null) {
            mClient.setActivity(activity);
        }

        saveConstraints();
    }

    /**
     * Call back for api level combo selection
     */
    void onRenderingTargetChange() {
        // because mApiCombo triggers onApiLevelChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates > 0) {
            return;
        }

        IAndroidTarget prevTarget = mConfiguration.getTarget();
        String prevTheme = mConfiguration.getTheme();

        int changeFlags = 0;

        // tell the listener a new rendering target is being set. Need to do this before updating
        // mRenderingTarget.
        if (prevTarget != null) {
            changeFlags |= CHANGED_RENDER_TARGET;
            mClient.aboutToChange(changeFlags);
        }

        IAndroidTarget target = (IAndroidTarget) mTargetCombo.getData();
        mConfiguration.setTarget(target, true);

        // force a theme update to reflect the new rendering target.
        // This must be done after computeCurrentConfig since it'll depend on the currentConfig
        // to figure out the theme list.
        String oldTheme = mConfiguration.getTheme();
        updateThemes();
        // updateThemes may change the theme (based on theme availability in the new rendering
        // target) so mark theme change if necessary
        if (!Objects.equal(oldTheme, mConfiguration.getTheme())) {
            changeFlags |= CHANGED_THEME;
        }

        if (target != null) {
            changeFlags |= CHANGED_RENDER_TARGET;
            changeFlags |= CHANGED_FOLDER; // In case we added a -vNN qualifier
        }

        // Store project-wide render-target setting
        mConfiguration.saveRenderState();

        mConfiguration.syncFolderConfig();

        if (mClient != null) {
            boolean accepted = mClient.changed(changeFlags);
            if (!accepted) {
                mConfiguration.setTarget(prevTarget, true);
                mConfiguration.setTheme(prevTheme);
                mConfiguration.syncFolderConfig();
                selectTheme(prevTheme);
                selectTarget(prevTarget);
            }
        }
    }

    /**
     * Syncs this configuration to the project wide locale and render target settings. The
     * locale may ignore the project-wide setting if it is a locale-specific
     * configuration.
     *
     * @return true if one or both of the toggles were changed, false if there were no
     *         changes
     */
    public boolean syncRenderState() {
        if (mConfiguration.getEditedConfig() == null) {
            // Startup; ignore
            return false;
        }

        boolean renderTargetChanged = false;

        // When a page is re-activated, force the toggles to reflect the current project
        // state

        Pair<Locale, IAndroidTarget> pair = Configuration.loadRenderState(this);

        int changeFlags = 0;
        // Only sync the locale if this layout is not already a locale-specific layout!
        if (pair != null && !mConfiguration.isLocaleSpecificLayout()) {
            Locale locale = pair.getFirst();
            if (locale != null) {
                boolean localeChanged = setLocale(locale);
                if (localeChanged) {
                    changeFlags |= CHANGED_LOCALE;
                }
            } else {
                locale = Locale.ANY;
            }
            mConfiguration.setLocale(locale, true);
        }

        // Sync render target
        IAndroidTarget configurationTarget = mConfiguration.getTarget();
        IAndroidTarget target = pair != null ? pair.getSecond() : configurationTarget;
        if (target != null && configurationTarget != target) {
            if (mClient != null && configurationTarget != null) {
                changeFlags |= CHANGED_RENDER_TARGET;
                mClient.aboutToChange(changeFlags);
            }

            mConfiguration.setTarget(target, true);
            selectTarget(target);
            renderTargetChanged = true;
        }

        // Neither locale nor render target changed: nothing to do
        if (changeFlags == 0) {
            return false;
        }

        // Update the locale and/or the render target. This code contains a logical
        // merge of the onRenderingTargetChange() and onLocaleChange() methods, combined
        // such that we don't duplicate work.

        // Compute the new configuration; we want to do this both for locale changes
        // and for render targets.
        mConfiguration.syncFolderConfig();
        changeFlags |= CHANGED_FOLDER; // in case we added/remove a -v<NN> qualifier

        if (renderTargetChanged) {
            // force a theme update to reflect the new rendering target.
            // This must be done after computeCurrentConfig since it'll depend on the currentConfig
            // to figure out the theme list.
            updateThemes();
        }

        if (mClient != null) {
            mClient.changed(changeFlags);
        }

        return true;
    }

    // ---- Populate data structures with themes, locales, etc ----

    /**
     * Updates the internal list of themes.
     */
    private void updateThemes() {
        if (mClient == null) {
            return; // can't do anything without it.
        }

        ResourceRepository frameworkRes = mClient.getFrameworkResources(
                mConfiguration.getTarget());

        mDisableUpdates++;

        try {
            if (mEditedFile != null) {
                String theme = mConfiguration.getTheme();
                if (theme == null || theme.isEmpty() || mClient.getIncludedWithin() != null) {
                    mConfiguration.setTheme(null);
                    computePreferredTheme();
                }
                assert mConfiguration.getTheme() != null;
            }

            mThemeList.clear();

            ArrayList<String> themes = new ArrayList<String>();
            ResourceRepository projectRes = mClient.getProjectResources();
            // in cases where the opened file is not linked to a project, this could be null.
            if (projectRes != null) {
                // get the configured resources for the project
                Map<ResourceType, Map<String, ResourceValue>> configuredProjectRes =
                    mClient.getConfiguredProjectResources();

                if (configuredProjectRes != null) {
                    // get the styles.
                    Map<String, ResourceValue> styleMap = configuredProjectRes.get(
                            ResourceType.STYLE);

                    if (styleMap != null) {
                        // collect the themes out of all the styles, ie styles that extend,
                        // directly or indirectly a platform theme.
                        for (ResourceValue value : styleMap.values()) {
                            if (isTheme(value, styleMap, null)) {
                                String theme = value.getName();
                                themes.add(theme);
                            }
                        }

                        Collections.sort(themes);

                        for (String theme : themes) {
                            if (!theme.startsWith(PREFIX_RESOURCE_REF)) {
                                theme = STYLE_RESOURCE_PREFIX + theme;
                            }
                            mThemeList.add(theme);
                        }
                    }
                }
                themes.clear();
            }

            // get the themes, and languages from the Framework.
            if (frameworkRes != null) {
                // get the configured resources for the framework
                Map<ResourceType, Map<String, ResourceValue>> frameworResources =
                    frameworkRes.getConfiguredResources(mConfiguration.getFullConfig());

                if (frameworResources != null) {
                    // get the styles.
                    Map<String, ResourceValue> styles = frameworResources.get(ResourceType.STYLE);

                    // collect the themes out of all the styles.
                    for (ResourceValue value : styles.values()) {
                        String name = value.getName();
                        if (name.startsWith("Theme.") || name.equals("Theme")) { //$NON-NLS-1$ //$NON-NLS-2$
                            themes.add(value.getName());
                        }
                    }

                    // sort them and add them to the combo
                    Collections.sort(themes);

                    for (String theme : themes) {
                        if (!theme.startsWith(PREFIX_RESOURCE_REF)) {
                            theme = ANDROID_STYLE_RESOURCE_PREFIX + theme;
                        }
                        mThemeList.add(theme);
                    }

                    themes.clear();
                }
            }

            // Migration: In the past we didn't store the style prefix in the settings;
            // this meant we might lose track of whether the theme is a project style
            // or a framework style. For now we need to migrate. Search through the
            // theme list until we have a match
            String theme = mConfiguration.getTheme();
            if (theme != null && !theme.startsWith(PREFIX_RESOURCE_REF)) {
                String projectStyle = STYLE_RESOURCE_PREFIX + theme;
                String frameworkStyle = ANDROID_STYLE_RESOURCE_PREFIX + theme;
                for (String t : mThemeList) {
                    if (t.equals(projectStyle)) {
                        mConfiguration.setTheme(projectStyle);
                        break;
                    } else if (t.equals(frameworkStyle)) {
                        mConfiguration.setTheme(frameworkStyle);
                        break;
                    }
                }
            }

            // TODO: Handle the case where you have a theme persisted that isn't available??
            // We could look up mConfiguration.theme and make sure it appears in the list! And if
            // not, picking one.
            selectTheme(mConfiguration.getTheme());
        } finally {
            mDisableUpdates--;
        }
    }

    private void updateActivity() {
        if (mEditedFile != null) {
            String preferred = getPreferredActivity(mEditedFile);
            selectActivity(preferred);
        }
    }

    /**
     * Updates the locale combo.
     * This must be called from the UI thread.
     */
    public void updateLocales() {
        if (mClient == null) {
            return; // can't do anything w/o it.
        }

        mDisableUpdates++;

        try {
            mLocaleList.clear();

            SortedSet<String> languages = null;

            // get the languages from the project.
            ResourceRepository projectRes = mClient.getProjectResources();

            // in cases where the opened file is not linked to a project, this could be null.
            if (projectRes != null) {
                // now get the languages from the project.
                languages = projectRes.getLanguages();

                for (String language : languages) {
                    LanguageQualifier langQual = new LanguageQualifier(language);

                    // find the matching regions and add them
                    SortedSet<String> regions = projectRes.getRegions(language);
                    for (String region : regions) {
                        RegionQualifier regionQual = new RegionQualifier(region);
                        mLocaleList.add(Locale.create(langQual, regionQual));
                    }

                    // now the entry for the other regions the language alone
                    // create a region qualifier that will never be matched by qualified resources.
                    mLocaleList.add(Locale.create(langQual));
                }
            }

            // create language/region qualifier that will never be matched by qualified resources.
            mLocaleList.add(Locale.ANY);

            Locale locale = mConfiguration.getLocale();
            setLocale(locale);
        } finally {
            mDisableUpdates--;
        }
    }

    /** Returns the preferred theme, or null */
    @Nullable
    String computePreferredTheme() {
        if (mClient == null) {
            return null;
        }

        IProject project = mEditedFile.getProject();
        ManifestInfo manifest = ManifestInfo.get(project);

        // Look up the screen size for the current state
        ScreenSize screenSize = null;
        Device device = mConfiguration.getDevice();
        if (device != null) {
            List<State> states = device.getAllStates();
            for (State state : states) {
                FolderConfiguration folderConfig = DeviceConfigHelper.getFolderConfig(state);
                if (folderConfig != null) {
                    ScreenSizeQualifier qualifier = folderConfig.getScreenSizeQualifier();
                    screenSize = qualifier.getValue();
                    break;
                }
            }
        }

        // Look up the default/fallback theme to use for this project (which
        // depends on the screen size when no particular theme is specified
        // in the manifest)
        String defaultTheme = manifest.getDefaultTheme(mConfiguration.getTarget(), screenSize);

        String preferred = defaultTheme;
        if (mConfiguration.getTheme() == null) {
            // If we are rendering a layout in included context, pick the theme
            // from the outer layout instead

            String activity = mConfiguration.getActivity();
            if (activity != null) {
                Map<String, String> activityThemes = manifest.getActivityThemes();
                preferred = activityThemes.get(activity);
            }
            if (preferred == null) {
                preferred = defaultTheme;
            }
            mConfiguration.setTheme(preferred);
        }

        return preferred;
    }

    @Nullable
    private String getPreferredActivity(@NonNull IFile file) {
        // Store/restore the activity context in the config state to help with
        // performance if for some reason we can't write it into the XML file and to
        // avoid having to open the model below
        if (mConfiguration.getActivity() != null) {
            return mConfiguration.getActivity();
        }

        IProject project = file.getProject();

        // Look up from XML file
        Document document = DomUtilities.getDocument(file);
        if (document != null) {
            Element element = document.getDocumentElement();
            if (element != null) {
                String activity = element.getAttributeNS(TOOLS_URI, ATTR_CONTEXT);
                if (activity != null && !activity.isEmpty()) {
                    if (activity.startsWith(".") || activity.indexOf('.') == -1) { //$NON-NLS-1$
                        ManifestInfo manifest = ManifestInfo.get(project);
                        String pkg = manifest.getPackage();
                        if (!pkg.isEmpty()) {
                            if (activity.startsWith(".")) { //$NON-NLS-1$
                                activity = pkg + activity;
                            } else {
                                activity = activity + '.' + pkg;
                            }
                        }
                    }

                    mConfiguration.setActivity(activity);
                    saveConstraints();
                    return activity;
                }
            }
        }

        // No, not available there: try to infer it from the code index
        String includedIn = null;
        Reference includedWithin = mClient.getIncludedWithin();
        if (mClient != null && includedWithin != null) {
            includedIn = includedWithin.getName();
        }

        ManifestInfo manifest = ManifestInfo.get(project);
        String pkg = manifest.getPackage();
        String layoutName = ResourceHelper.getLayoutName(mEditedFile);

        // If we are rendering a layout in included context, pick the theme
        // from the outer layout instead
        if (includedIn != null) {
            layoutName = includedIn;
        }

        String activity = ManifestInfo.guessActivity(project, layoutName, pkg);

        if (activity == null) {
            List<String> activities = ManifestInfo.getProjectActivities(project);
            if (activities.size() == 1) {
                activity = activities.get(0);
            }
        }

        if (activity != null) {
            mConfiguration.setActivity(activity);
            saveConstraints();
            return activity;
        }

        // TODO: Do anything else, such as pick the first activity found?
        // Or just leave some default label instead?
        // Also, figure out what to store in the mState so I don't keep trying

        return null;
    }

    /**
     * Returns whether the given <var>style</var> is a theme.
     * This is done by making sure the parent is a theme.
     * @param value the style to check
     * @param styleMap the map of styles for the current project. Key is the style name.
     * @param seen the map of styles we have already processed (or null if not yet
     *          initialized). Only the keys are significant (since there is no IdentityHashSet).
     * @return True if the given <var>style</var> is a theme.
     */
    private static boolean isTheme(ResourceValue value, Map<String, ResourceValue> styleMap,
            IdentityHashMap<ResourceValue, Boolean> seen) {
        if (value instanceof StyleResourceValue) {
            StyleResourceValue style = (StyleResourceValue)value;

            boolean frameworkStyle = false;
            String parentStyle = style.getParentStyle();
            if (parentStyle == null) {
                // if there is no specified parent style we look an implied one.
                // For instance 'Theme.light' is implied child style of 'Theme',
                // and 'Theme.light.fullscreen' is implied child style of 'Theme.light'
                String name = style.getName();
                int index = name.lastIndexOf('.');
                if (index != -1) {
                    parentStyle = name.substring(0, index);
                }
            } else {
                // remove the useless @ if it's there
                if (parentStyle.startsWith("@")) {
                    parentStyle = parentStyle.substring(1);
                }

                // check for framework identifier.
                if (parentStyle.startsWith(ANDROID_NS_NAME_PREFIX)) {
                    frameworkStyle = true;
                    parentStyle = parentStyle.substring(ANDROID_NS_NAME_PREFIX.length());
                }

                // at this point we could have the format style/<name>. we want only the name
                if (parentStyle.startsWith("style/")) {
                    parentStyle = parentStyle.substring("style/".length());
                }
            }

            if (parentStyle != null) {
                if (frameworkStyle) {
                    // if the parent is a framework style, it has to be 'Theme' or 'Theme.*'
                    return parentStyle.equals("Theme") || parentStyle.startsWith("Theme.");
                } else {
                    // if it's a project style, we check this is a theme.
                    ResourceValue parentValue = styleMap.get(parentStyle);

                    // also prevent stack overflow in case the dev mistakenly declared
                    // the parent of the style as the style itself.
                    if (parentValue != null && !parentValue.equals(value)) {
                        if (seen == null) {
                            seen = new IdentityHashMap<ResourceValue, Boolean>();
                            seen.put(value, Boolean.TRUE);
                        } else if (seen.containsKey(parentValue)) {
                            return false;
                        }
                        seen.put(parentValue, Boolean.TRUE);
                        return isTheme(parentValue, styleMap, seen);
                    }
                }
            }
        }

        return false;
    }
}
