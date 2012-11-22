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

import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.Rect;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.NightModeQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.android.ide.common.resources.configuration.ScreenDimensionQualifier;
import com.android.ide.common.resources.configuration.ScreenOrientationQualifier;
import com.android.ide.common.resources.configuration.UiModeQualifier;
import com.android.ide.common.resources.configuration.VersionQualifier;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.resources.ResourceHelper;
import com.android.resources.Density;
import com.android.resources.NightMode;
import com.android.resources.ScreenOrientation;
import com.android.resources.UiMode;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.android.utils.Pair;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import java.util.List;

/**
 * A {@linkplain Configuration} is a selection of device, orientation, theme,
 * etc for use when rendering a layout.
 */
public class Configuration {
    /**
     * Setting name for project-wide setting controlling rendering target and locale which
     * is shared for all files
     */
    public final static QualifiedName NAME_RENDER_STATE =
        new QualifiedName(AdtPlugin.PLUGIN_ID, "render");          //$NON-NLS-1$

    private final static String MARKER_FRAMEWORK = "-";            //$NON-NLS-1$
    private final static String MARKER_PROJECT = "+";              //$NON-NLS-1$
    private final static String SEP = ":";                         //$NON-NLS-1$
    private final static String SEP_LOCALE = "-";                  //$NON-NLS-1$

    @NonNull
    protected final ConfigurationChooser mConfigChooser;

    /** The {@link FolderConfiguration} representing the state of the UI controls */
    @NonNull
    protected final FolderConfiguration mFullConfig = new FolderConfiguration();

    /** The {@link FolderConfiguration} being edited. */
    @Nullable
    protected FolderConfiguration mEditedConfig;

    /** The target of the project of the file being edited. */
    @Nullable
    private IAndroidTarget mTarget;

    /** The theme style to render with */
    @Nullable
    private String mTheme;

    /** The device to render with */
    @Nullable
    private Device mDevice;

    /** The device state */
    @Nullable
    private State mState;

    /**
     * The activity associated with the layout. This is just a cached value of
     * the true value stored on the layout.
     */
    @Nullable
    private String mActivity;

    /** The locale to use for this configuration */
    @NonNull
    private Locale mLocale = Locale.ANY;

    /** UI mode */
    @NonNull
    private UiMode mUiMode = UiMode.NORMAL;

    /** Night mode */
    @NonNull
    private NightMode mNightMode = NightMode.NOTNIGHT;

    /**
     * Creates a new {@linkplain Configuration}
     *
     * @param chooser the associated chooser
     */
    protected Configuration(@NonNull ConfigurationChooser chooser) {
        mConfigChooser = chooser;
    }

    /**
     * Creates a new {@linkplain Configuration}
     *
     * @param chooser the associated chooser
     * @return a new configuration
     */
    @NonNull
    public static Configuration create(@NonNull ConfigurationChooser chooser) {
        return new Configuration(chooser);
    }

    /**
     * Returns the associated activity
     *
     * @return the activity
     */
    @Nullable
    public String getActivity() {
        return mActivity;
    }

    /**
     * Returns the chosen device.
     *
     * @return the chosen device
     */
    @Nullable
    public Device getDevice() {
        return mDevice;
    }

    /**
     * Returns the chosen device state
     *
     * @return the device state
     */
    @Nullable
    public State getDeviceState() {
        return mState;
    }

    /**
     * Returns the chosen locale
     *
     * @return the locale
     */
    @NonNull
    public Locale getLocale() {
        return mLocale;
    }

    /**
     * Returns the UI mode
     *
     * @return the UI mode
     */
    @NonNull
    public UiMode getUiMode() {
        return mUiMode;
    }

    /**
     * Returns the day/night mode
     *
     * @return the night mode
     */
    @NonNull
    public NightMode getNightMode() {
        return mNightMode;
    }

    /**
     * Returns the current theme style
     *
     * @return the theme style
     */
    @Nullable
    public String getTheme() {
        return mTheme;
    }

    /**
     * Returns the rendering target
     *
     * @return the target
     */
    @Nullable
    public IAndroidTarget getTarget() {
        return mTarget;
    }

    /**
     * Returns whether the configuration's theme is a project theme.
     * <p/>
     * The returned value is meaningless if {@link #getTheme()} returns
     * <code>null</code>.
     *
     * @return true for project a theme, false for a framework theme
     */
    public boolean isProjectTheme() {
        String theme = getTheme();
        if (theme != null) {
            assert theme.startsWith(STYLE_RESOURCE_PREFIX)
                || theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX);

            return ResourceHelper.isProjectStyle(theme);
        }

        return false;
    }

    /**
     * Returns true if the current layout is locale-specific
     *
     * @return if this configuration represents a locale-specific layout
     */
    public boolean isLocaleSpecificLayout() {
        return mEditedConfig == null || mEditedConfig.getLanguageQualifier() != null;
    }

    /**
     * Returns the full, complete {@link FolderConfiguration}
     *
     * @return the full configuration
     */
    @NonNull
    public FolderConfiguration getFullConfig() {
        return mFullConfig;
    }

    /**
     * Copies the full, complete {@link FolderConfiguration} into the given
     * folder config instance.
     *
     * @param dest the {@link FolderConfiguration} instance to copy into
     */
    public void copyFullConfig(FolderConfiguration dest) {
        dest.set(mFullConfig);
    }

    /**
     * Returns the edited {@link FolderConfiguration} (this is not a full
     * configuration, so you can think of it as the "constraints" used by the
     * {@link ConfigurationMatcher} to produce a full configuration.
     *
     * @return the constraints configuration
     */
    @NonNull
    public FolderConfiguration getEditedConfig() {
        return mEditedConfig;
    }

    /**
     * Sets the edited {@link FolderConfiguration} (this is not a full
     * configuration, so you can think of it as the "constraints" used by the
     * {@link ConfigurationMatcher} to produce a full configuration.
     *
     * @param editedConfig the constraints configuration
     */
    public void setEditedConfig(@NonNull FolderConfiguration editedConfig) {
        mEditedConfig = editedConfig;
    }

    /**
     * Sets the associated activity
     *
     * @param activity the activity
     */
    public void setActivity(String activity) {
        mActivity = activity;
    }

    /**
     * Sets the device
     *
     * @param device the device
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setDevice(Device device, boolean skipSync) {
        mDevice = device;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the device state
     *
     * @param state the device state
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setDeviceState(State state, boolean skipSync) {
        mState = state;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the locale
     *
     * @param locale the locale
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setLocale(@NonNull Locale locale, boolean skipSync) {
        mLocale = locale;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the rendering target
     *
     * @param target rendering target
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setTarget(IAndroidTarget target, boolean skipSync) {
        mTarget = target;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the night mode
     *
     * @param night the night mode
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setNightMode(@NonNull NightMode night, boolean skipSync) {
        mNightMode = night;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the UI mode
     *
     * @param uiMode the UI mode
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setUiMode(@NonNull UiMode uiMode, boolean skipSync) {
        mUiMode = uiMode;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the theme style
     *
     * @param theme the theme
     */
    public void setTheme(String theme) {
        mTheme = theme;
    }

    /**
     * Updates the folder configuration such that it reflects changes in
     * configuration state such as the device orientation, the UI mode, the
     * rendering target, etc.
     */
    public void syncFolderConfig() {
        Device device = getDevice();
        if (device == null) {
            return;
        }

        // get the device config from the device/state combos.
        FolderConfiguration config = DeviceConfigHelper.getFolderConfig(getDeviceState());

        // replace the config with the one from the device
        mFullConfig.set(config);

        // sync the selected locale
        Locale locale = getLocale();
        mFullConfig.setLanguageQualifier(locale.language);
        mFullConfig.setRegionQualifier(locale.region);

        // Replace the UiMode with the selected one, if one is selected
        UiMode uiMode = getUiMode();
        if (uiMode != null) {
            mFullConfig.setUiModeQualifier(new UiModeQualifier(uiMode));
        }

        // Replace the NightMode with the selected one, if one is selected
        NightMode nightMode = getNightMode();
        if (nightMode != null) {
            mFullConfig.setNightModeQualifier(new NightModeQualifier(nightMode));
        }

        // replace the API level by the selection of the combo
        IAndroidTarget target = getTarget();
        if (target == null && mConfigChooser != null) {
            target = mConfigChooser.getProjectTarget();
        }
        if (target != null) {
            int apiLevel = target.getVersion().getApiLevel();
            mFullConfig.setVersionQualifier(new VersionQualifier(apiLevel));
        }
    }

    /**
     * Creates a string suitable for persistence, which can be initialized back
     * to a configuration via {@link #initialize(String)}
     *
     * @return a persistent string
     */
    @NonNull
    public String toPersistentString() {
        StringBuilder sb = new StringBuilder(32);
        Device device = getDevice();
        if (device != null) {
            sb.append(device.getName());
            sb.append(SEP);
            State state = getDeviceState();
            if (state != null) {
                sb.append(state.getName());
            }
            sb.append(SEP);
            Locale locale = getLocale();
            if (isLocaleSpecificLayout() && locale != null) {
                // locale[0]/[1] can be null sometimes when starting Eclipse
                sb.append(locale.language.getValue());
                sb.append(SEP_LOCALE);
                sb.append(locale.region.getValue());
            }
            sb.append(SEP);
            // Need to escape the theme: if we write the full theme style, then
            // we can end up with ":"'s in the string (as in @android:style/Theme) which
            // can be mistaken for {@link #SEP}. Instead use {@link #MARKER_FRAMEWORK}.
            String theme = getTheme();
            if (theme != null) {
                String themeName = ResourceHelper.styleToTheme(theme);
                if (theme.startsWith(STYLE_RESOURCE_PREFIX)) {
                    sb.append(MARKER_PROJECT);
                } else if (theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX)) {
                    sb.append(MARKER_FRAMEWORK);
                }
                sb.append(themeName);
            }
            sb.append(SEP);
            UiMode uiMode = getUiMode();
            if (uiMode != null) {
                sb.append(uiMode.getResourceValue());
            }
            sb.append(SEP);
            NightMode nightMode = getNightMode();
            if (nightMode != null) {
                sb.append(nightMode.getResourceValue());
            }
            sb.append(SEP);

            // We used to store the render target here in R9. Leave a marker
            // to ensure that we don't reuse this slot; add new extra fields after it.
            sb.append(SEP);
            String activity = getActivity();
            if (activity != null) {
                sb.append(activity);
            }
        }

        return sb.toString();
    }

    /**
     * Initializes a string previously created with
     * {@link #toPersistentString()}
     *
     * @param data the string to initialize back from
     * @return true if the configuration was initialized
     */
    boolean initialize(String data) {
        String[] values = data.split(SEP);
        if (values.length >= 6 && values.length <= 8) {
            for (Device d : mConfigChooser.getDeviceList()) {
                if (d.getName().equals(values[0])) {
                    mDevice = d;
                    String stateName = null;
                    FolderConfiguration config = null;
                    if (!values[1].isEmpty() && !values[1].equals("null")) { //$NON-NLS-1$
                        stateName = values[1];
                        config = DeviceConfigHelper.getFolderConfig(mDevice, stateName);
                    } else if (mDevice.getAllStates().size() > 0) {
                        State first = mDevice.getAllStates().get(0);
                        stateName = first.getName();
                        config = DeviceConfigHelper.getFolderConfig(first);
                    }
                    mState = getState(mDevice, stateName);
                    if (config != null) {
                        // Load locale. Note that this can get overwritten by the
                        // project-wide settings read below.
                        LanguageQualifier language = Locale.ANY_LANGUAGE;
                        RegionQualifier region = Locale.ANY_REGION;
                        String locales[] = values[2].split(SEP_LOCALE);
                        if (locales.length >= 2) {
                            if (locales[0].length() > 0) {
                                language = new LanguageQualifier(locales[0]);
                            }
                            if (locales[1].length() > 0) {
                                region = new RegionQualifier(locales[1]);
                            }
                            mLocale = Locale.create(language, region);
                        }

                        // Decode the theme name: See {@link #getData}
                        mTheme = values[3];
                        if (mTheme.startsWith(MARKER_FRAMEWORK)) {
                            mTheme = ANDROID_STYLE_RESOURCE_PREFIX
                                    + mTheme.substring(MARKER_FRAMEWORK.length());
                        } else if (mTheme.startsWith(MARKER_PROJECT)) {
                            mTheme = STYLE_RESOURCE_PREFIX
                                    + mTheme.substring(MARKER_PROJECT.length());
                        }

                        mUiMode = UiMode.getEnum(values[4]);
                        if (mUiMode == null) {
                            mUiMode = UiMode.NORMAL;
                        }
                        mNightMode = NightMode.getEnum(values[5]);
                        if (mNightMode == null) {
                            mNightMode = NightMode.NOTNIGHT;
                        }

                        // element 7/values[6]: used to store render target in R9.
                        // No longer stored here. If adding more data, make
                        // sure you leave 7 alone.

                        Pair<Locale, IAndroidTarget> pair = loadRenderState(mConfigChooser);
                        if (pair != null) {
                            // We only use the "global" setting
                            if (!isLocaleSpecificLayout()) {
                                mLocale = pair.getFirst();
                            }
                            mTarget = pair.getSecond();
                        }

                        if (values.length == 8) {
                            mActivity = values[7];
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Loads the render state (the locale and the render target, which are shared among
     * all the layouts meaning that changing it in one will change it in all) and returns
     * the current project-wide locale and render target to be used.
     *
     * @param chooser the {@link ConfigurationChooser} providing information about
     *     loaded targets
     * @return a pair of a locale and a render target
     */
    @Nullable
    static Pair<Locale, IAndroidTarget> loadRenderState(ConfigurationChooser chooser) {
        IProject project = chooser.getProject();
        if (!project.isAccessible()) {
            return null;
        }

        try {
            String data = project.getPersistentProperty(NAME_RENDER_STATE);
            if (data != null) {
                Locale locale = Locale.ANY;
                IAndroidTarget target = null;

                String[] values = data.split(SEP);
                if (values.length == 2) {
                    LanguageQualifier language = Locale.ANY_LANGUAGE;
                    RegionQualifier region = Locale.ANY_REGION;
                    String locales[] = values[0].split(SEP_LOCALE);
                    if (locales.length >= 2) {
                        if (locales[0].length() > 0) {
                            language = new LanguageQualifier(locales[0]);
                        }
                        if (locales[1].length() > 0) {
                            region = new RegionQualifier(locales[1]);
                        }
                    }
                    locale = Locale.create(language, region);

                    target = stringToTarget(chooser, values[1]);

                    // See if we should "correct" the rendering target to a better version.
                    // If you're using a pre-release version of the render target, and a
                    // final release is available and installed, we should switch to that
                    // one instead.
                    if (target != null) {
                        AndroidVersion version = target.getVersion();
                        List<IAndroidTarget> targetList = chooser.getTargetList();
                        if (version.getCodename() != null && targetList != null) {
                            int targetApiLevel = version.getApiLevel() + 1;
                            for (IAndroidTarget t : targetList) {
                                if (t.getVersion().getApiLevel() == targetApiLevel
                                        && t.isPlatform()) {
                                    target = t;
                                    break;
                                }
                            }
                        }
                    }
                }

                return Pair.of(locale, target);
            }

            return Pair.of(Locale.ANY, ConfigurationMatcher.findDefaultRenderTarget(project));
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    /**
     * Saves the render state (the current locale and render target settings) into the
     * project wide settings storage
     */
    void saveRenderState() {
        IProject project = mConfigChooser.getProject();
        try {
            // Generate a persistent string from locale+target
            StringBuilder sb = new StringBuilder();
            Locale locale = getLocale();
            if (locale != null) {
                // locale[0]/[1] can be null sometimes when starting Eclipse
                sb.append(locale.language.getValue());
                sb.append(SEP_LOCALE);
                sb.append(locale.region.getValue());
            }
            sb.append(SEP);
            IAndroidTarget target = getTarget();
            if (target != null) {
                sb.append(targetToString(target));
                sb.append(SEP);
            }

            project.setPersistentProperty(NAME_RENDER_STATE, sb.toString());
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
    }

    /**
     * Returns a String id to represent an {@link IAndroidTarget} which can be translated
     * back to an {@link IAndroidTarget} by the matching {@link #stringToTarget}. The id
     * will never contain the {@link #SEP} character.
     *
     * @param target the target to return an id for
     * @return an id for the given target; never null
     */
    @NonNull
    private static String targetToString(@NonNull IAndroidTarget target) {
        return target.getFullName().replace(SEP, "");  //$NON-NLS-1$
    }

    /**
     * Returns an {@link IAndroidTarget} that corresponds to the given id that was
     * originally returned by {@link #targetToString}. May be null, if the platform is no
     * longer available, or if the platform list has not yet been initialized.
     *
     * @param chooser the {@link ConfigurationChooser} providing information about
     *     loaded targets
     * @param id the id that corresponds to the desired platform
     * @return an {@link IAndroidTarget} that matches the given id, or null
     */
    @Nullable
    private static IAndroidTarget stringToTarget(
            @NonNull ConfigurationChooser chooser,
            @NonNull String id) {
        List<IAndroidTarget> targetList = chooser.getTargetList();
        if (targetList != null && targetList.size() > 0) {
            for (IAndroidTarget target : targetList) {
                if (id.equals(targetToString(target))) {
                    return target;
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link State} by the given name for the given {@link Device}
     *
     * @param device the device
     * @param name the name of the state
     */
    @Nullable
    static State getState(@Nullable Device device, @Nullable String name) {
        if (device == null) {
            return null;
        } else if (name != null) {
            State state = device.getState(name);
            if (state != null) {
                return state;
            }
        }

        return device.getDefaultState();
    }

    /**
     * Returns the currently selected {@link Density}. This is guaranteed to be non null.
     *
     * @return the density
     */
    @NonNull
    public Density getDensity() {
        if (mFullConfig != null) {
            DensityQualifier qual = mFullConfig.getDensityQualifier();
            if (qual != null) {
                // just a sanity check
                Density d = qual.getValue();
                if (d != Density.NODPI) {
                    return d;
                }
            }
        }

        // no config? return medium as the default density.
        return Density.MEDIUM;
    }

    /**
     * Returns the current device xdpi.
     *
     * @return the x dpi as a float
     */
    public float getXDpi() {
        Device device = getDevice();
        if (device != null) {
            State currState = getDeviceState();
            if (currState == null) {
                currState = device.getDefaultState();
            }
            float dpi = (float) currState.getHardware().getScreen().getXdpi();
            if (!Float.isNaN(dpi)) {
                return dpi;
            }
        }

        // get the pixel density as the density.
        return getDensity().getDpiValue();
    }

    /**
     * Returns the current device ydpi.
     *
     * @return the y dpi as a float
     */
    public float getYDpi() {
        Device device = getDevice();
        if (device != null) {
            State currState = getDeviceState();
            if (currState == null) {
                currState = device.getDefaultState();
            }
            float dpi = (float) currState.getHardware().getScreen().getYdpi();
            if (!Float.isNaN(dpi)) {
                return dpi;
            }
        }

        // get the pixel density as the density.
        return getDensity().getDpiValue();
    }

    /**
     * Returns the bounds of the screen
     *
     * @return the screen bounds
     */
    public Rect getScreenBounds() {
        return getScreenBounds(mFullConfig);
    }

    /**
     * Gets the orientation from the given configuration
     *
     * @param config the configuration to look up
     * @return the bounds
     */
    @NonNull
    public static Rect getScreenBounds(FolderConfiguration config) {
        // get the orientation from the given device config
        ScreenOrientationQualifier qual = config.getScreenOrientationQualifier();
        ScreenOrientation orientation = ScreenOrientation.PORTRAIT;
        if (qual != null) {
            orientation = qual.getValue();
        }

        // get the device screen dimension
        ScreenDimensionQualifier qual2 = config.getScreenDimensionQualifier();
        int s1, s2;
        if (qual2 != null) {
            s1 = qual2.getValue1();
            s2 = qual2.getValue2();
        } else {
            s1 = 480;
            s2 = 320;
        }

        switch (orientation) {
            default:
            case PORTRAIT:
                return new Rect(0, 0, s2, s1);
            case LANDSCAPE:
                return new Rect(0, 0, s1, s2);
            case SQUARE:
                return new Rect(0, 0, s1, s1);
        }
    }

    /**
     * Get the next cyclical state after the given state
     *
     * @param from the state to start with
     * @return the following state following
     */
    @Nullable
    public State getNextDeviceState(@Nullable State from) {
        Device device = getDevice();
        if (device == null) {
            return null;
        }
        List<State> states = device.getAllStates();
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i) == from) {
                return states.get((i + 1) % states.size());
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return toPersistentString();
    }
}
