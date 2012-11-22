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

package com.android.traceview;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map.Entry;

public class PropertiesDialog extends Dialog {
    private HashMap<String, String> mProperties;

    public PropertiesDialog(Shell parent) {
        super(parent);

        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
    }

    public void setProperties(HashMap<String, String> properties) {
        mProperties = properties;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        container.setLayout(gridLayout);

        TableViewer tableViewer = new TableViewer(container, SWT.HIDE_SELECTION
                | SWT.V_SCROLL | SWT.BORDER);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.getTable().setHeaderVisible(true);

        TableViewerColumn propertyColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        propertyColumn.getColumn().setText("Property");
        propertyColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public String getText(Object element) {
                Entry<String, String> entry = (Entry<String, String>) element;
                return entry.getKey();
            }
        });
        propertyColumn.getColumn().setWidth(400);

        TableViewerColumn valueColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public String getText(Object element) {
                Entry<String, String> entry = (Entry<String, String>) element;
                return entry.getValue();
            }
        });
        valueColumn.getColumn().setWidth(200);

        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setInput(mProperties.entrySet().toArray());

        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        tableViewer.getControl().setLayoutData(gridData);

        return container;
    }
}
