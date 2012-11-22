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
import com.android.sdklib.IAndroidTarget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import java.util.List;

/**
 * The {@linkplain TargetMenuListener} class is responsible for
 * generating the rendering target menu in the {@link ConfigurationChooser}.
 */
class TargetMenuListener extends SelectionAdapter {
    private final ConfigurationChooser mConfigChooser;
    private final IAndroidTarget mTarget;

    TargetMenuListener(
            @NonNull ConfigurationChooser configChooser,
            @Nullable IAndroidTarget target) {
        mConfigChooser = configChooser;
        mTarget = target;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        mConfigChooser.selectTarget(mTarget);
        mConfigChooser.onRenderingTargetChange();
    }

    static void show(ConfigurationChooser chooser, ToolItem combo) {
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);
        Configuration configuration = chooser.getConfiguration();
        IAndroidTarget current = configuration.getTarget();
        List<IAndroidTarget> targets = chooser.getTargetList();

        for (final IAndroidTarget target : targets) {
            String title = ConfigurationChooser.getRenderingTargetLabel(target, false);
            MenuItem item = new MenuItem(menu, SWT.CHECK);
            item.setText(title);

            boolean selected = current == target;
            if (selected) {
                item.setSelection(true);
            }

            item.addSelectionListener(new TargetMenuListener(chooser, target));
        }

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }
}
