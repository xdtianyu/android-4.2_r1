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

import static com.android.SdkConstants.FD_RES_LAYOUT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PartInitException;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@linkplain ConfigurationMenuListener} class is responsible for
 * generating the configuration menu in the {@link ConfigurationChooser}.
 */
class ConfigurationMenuListener extends SelectionAdapter {
    private static final String ICON_NEW_CONFIG = "newConfig";    //$NON-NLS-1$
    private static final int ACTION_SELECT_CONFIG = 1;
    private static final int ACTION_CREATE_CONFIG_FILE = 2;

    private final ConfigurationChooser mConfigChooser;
    private final int mAction;
    private final IFile mResource;

    ConfigurationMenuListener(
            @NonNull ConfigurationChooser configChooser,
            int action,
            @Nullable IFile resource) {
        mConfigChooser = configChooser;
        mAction = action;
        mResource = resource;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        switch (mAction) {
            case ACTION_SELECT_CONFIG: {
                try {
                    AdtPlugin.openFile(mResource, null, false);
                } catch (PartInitException ex) {
                    AdtPlugin.log(ex, null);
                }
                break;
            }
            case ACTION_CREATE_CONFIG_FILE: {
                ConfigurationClient client = mConfigChooser.getClient();
                if (client != null) {
                    client.createConfigFile();
                }
                break;
            }
            default: assert false : mAction;
        }
    }

    static void show(ConfigurationChooser chooser, ToolItem combo) {
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);

        // Compute the set of layout files defining this layout resource
        IFile file = chooser.getEditedFile();
        String name = file.getName();
        IContainer resFolder = file.getParent().getParent();
        List<IFile> variations = new ArrayList<IFile>();
        try {
            for (IResource resource : resFolder.members()) {
                if (resource.getName().startsWith(FD_RES_LAYOUT)
                        && resource instanceof IContainer) {
                    IContainer layoutFolder = (IContainer) resource;
                    IResource variation = layoutFolder.findMember(name);
                    if (variation instanceof IFile) {
                        variations.add((IFile) variation);
                    }
                }
            }
        } catch (CoreException e1) {
            AdtPlugin.log(e1, null);
        }

        ResourceManager manager = ResourceManager.getInstance();
        for (final IFile resource : variations) {
            MenuItem item = new MenuItem(menu, SWT.CHECK);

            IFolder parent = (IFolder) resource.getParent();
            ResourceFolder parentResource = manager.getResourceFolder(parent);
            FolderConfiguration configuration = parentResource.getConfiguration();
            String title = configuration.toDisplayString();
            item.setText(title);

            boolean selected = file.equals(resource);
            if (selected) {
                item.setSelection(true);
                item.setEnabled(false);
            }

            item.addSelectionListener(new ConfigurationMenuListener(chooser,
                    ACTION_SELECT_CONFIG, resource));
        }

        Configuration configuration = chooser.getConfiguration();
        if (!configuration.getEditedConfig().equals(configuration.getFullConfig())) {
            if (variations.size() > 0) {
                @SuppressWarnings("unused")
                MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
            }

            // Add action for creating a new configuration
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText("Create New...");
            item.setImage(IconFactory.getInstance().getIcon(ICON_NEW_CONFIG));
            //item.setToolTipText("Duplicate: Create new configuration for this layout");

            item.addSelectionListener(
                    new ConfigurationMenuListener(chooser, ACTION_CREATE_CONFIG_FILE, null));
        }

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }
}
