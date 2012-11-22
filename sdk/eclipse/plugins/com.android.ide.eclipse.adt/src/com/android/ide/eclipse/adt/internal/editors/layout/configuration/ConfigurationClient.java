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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.editors.layout.gle2.IncludeFinder.Reference;
import com.android.resources.NightMode;
import com.android.resources.ResourceType;
import com.android.resources.UiMode;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;

import java.util.Map;

/**
 * Interface implemented by clients who embed a {@link ConfigurationChooser}.
 */
public interface ConfigurationClient {
    /** The {@link FolderConfiguration} in the configuration has changed */
    public static final int CHANGED_FOLDER        = 1 << 0;
    /** The {@link Device} in the configuration has changed */
    public static final int CHANGED_DEVICE        = 1 << 1;
    /** The {@link State} in the configuration has changed */
    public static final int CHANGED_DEVICE_CONFIG = 1 << 2;
    /** The theme in the configuration has changed */
    public static final int CHANGED_THEME         = 1 << 3;
    /** The locale in the configuration has changed */
    public static final int CHANGED_LOCALE        = 1 << 4;
    /** The rendering {@link IAndroidTarget} in the configuration has changed */
    public static final int CHANGED_RENDER_TARGET = 1 << 5;
    /** The {@link NightMode} in the configuration has changed */
    public static final int CHANGED_NIGHT_MODE = 1 << 6;
    /** The {@link UiMode} in the configuration has changed */
    public static final int CHANGED_UI_MODE = 1 << 7;

    /** Everything has changed */
    public static final int CHANGED_ALL = 0xFFFF;

    /**
     * The configuration is about to be changed.
     *
     * @param flags details about what changed; consult the {@code CHANGED_} flags
     *   such as {@link #CHANGED_DEVICE}, {@link #CHANGED_LOCALE}, etc.
     */
    void aboutToChange(int flags);

    /**
     * The configuration has changed. If the client returns false, it means that
     * the change was rejected. This typically means that changing the
     * configuration in this particular way makes a configuration which has a
     * better file match than the current client's file, so it will open that
     * file to edit the new configuration -- and the current configuration
     * should go back to editing the state prior to this change.
     *
     * @param flags details about what changed; consult the {@code CHANGED_} flags
     *   such as {@link #CHANGED_DEVICE}, {@link #CHANGED_LOCALE}, etc.
     * @return true if the change was accepted, false if it was rejected.
     */
    boolean changed(int flags);

    /**
     * Compute the project resources
     *
     * @return the project resources as a {@link ResourceRepository}
     */
    @Nullable
    ResourceRepository getProjectResources();

    /**
     * Compute the framework resources
     *
     * @return the project resources as a {@link ResourceRepository}
     */
    @Nullable
    ResourceRepository getFrameworkResources();

    /**
     * Compute the framework resources for the given Android API target
     *
     * @param target the target to look up framework resources for
     * @return the project resources as a {@link ResourceRepository}
     */
    @Nullable
    ResourceRepository getFrameworkResources(@Nullable IAndroidTarget target);

    /**
     * Returns the configured project resources for the current file and
     * configuration
     *
     * @return resource type maps to names to resource values
     */
    @NonNull
    Map<ResourceType, Map<String, ResourceValue>> getConfiguredProjectResources();

    /**
     * Returns the configured framework resources for the current file and
     * configuration
     *
     * @return resource type maps to names to resource values
     */
    @NonNull
    Map<ResourceType, Map<String, ResourceValue>> getConfiguredFrameworkResources();

    /**
     * If the current layout is an included layout rendered within an outer layout,
     * returns the outer layout.
     *
     * @return the outer including layout, or null
     */
    @Nullable
    Reference getIncludedWithin();

    /**
     * Called when the "Create" button is clicked.
     */
    void createConfigFile();

    /**
     * Called when an associated activity is picked
     *
     * @param fqcn the fully qualified class name for the associated activity context
     */
    void setActivity(@NonNull String fqcn);
}
