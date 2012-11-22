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
package com.motorolamobility.preflighting.ui.tabs;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.ui.PreflightingUIPlugin;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

/**
 * This class represents the Devices Tab on the app validador preference page.
 */
public class DevicesTabComposite extends AbstractAppValidatorTabComposite
{
    private final class ColumnSelectionAdapter extends SelectionAdapter
    {
        private int columnIndex;

        public ColumnSelectionAdapter(int columnIndex)
        {
            this.columnIndex = columnIndex;
        }

        @Override
        public void widgetSelected(SelectionEvent e)
        {
            devicesTableComparator.setColumn(columnIndex);
            devicesTableViewer.getTable().setSortColumn((TableColumn) e.getSource());
            devicesTableViewer.getTable()
                    .setSortDirection(devicesTableComparator.getSwtDirection());
            devicesTableViewer.refresh();
            super.widgetSelected(e);
        }
    }

    /*
     * Table content provider
     */
    public class DevicesContentProvider implements IStructuredContentProvider
    {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement)
        {
            return ((List<DeviceSpecification>) inputElement).toArray();
        }

        public void dispose()
        {
          //do nothing
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
          //do nothing
        }

    }

    /*
     * Table label provider
     */
    public class DevicesLabelProvider implements ITableLabelProvider
    {

        public void addListener(ILabelProviderListener listener)
        {
          //do nothing
        }

        public void dispose()
        {
          //do nothing
        }

        public boolean isLabelProperty(Object element, String property)
        {
            return false;
        }

        public void removeListener(ILabelProviderListener listener)
        {
          //do nothing
        }

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            String text = ""; //$NON-NLS-1$
            DeviceSpecification deviceSpec = (DeviceSpecification) element;
            switch (columnIndex)
            {
                case NAME_COLUMN_INDEX:
                    text = deviceSpec.getName();
                    break;
                case SCREENSIZE_COLUMN_INDEX:
                    text = deviceSpec.getDeviceInfo().getDefault().getScreenSize();
                    break;
                case PIXELDENSITY_COLUMN_INDEX:
                    text = deviceSpec.getDeviceInfo().getDefault().getPixelDensity();
                    break;
                default:
                    break;
            }
            return text;
        }

    }

    /*
     * Table comparator
     * Add sort functionality
     */
    private class DevicesTableComparator extends ViewerComparator
    {
        private final int ORDER_ASC = 1;

        private final int ORDER_DESC = -1;

        /**
         * Column that must be used to sort elements
         */
        private int column = -1;

        private int direction = ORDER_ASC;

        public void setColumn(int column)
        {
            if (this.column == column)
            {
                direction = direction == ORDER_ASC ? ORDER_DESC : ORDER_ASC;
            }
            else
            {
                this.column = column;
                direction = ORDER_ASC;
            }
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Viewer viewer, Object e1, Object e2)
        {
            DeviceSpecification deviceLeft = (DeviceSpecification) e1;
            DeviceSpecification deviceRight = (DeviceSpecification) e2;

            String left = ""; //$NON-NLS-1$
            String right = ""; //$NON-NLS-1$
            switch (column)
            {
                case NAME_COLUMN_INDEX:
                    left = deviceLeft.getName();
                    right = deviceRight.getName();
                    break;
                case SCREENSIZE_COLUMN_INDEX:
                    left = deviceLeft.getDeviceInfo().getDefault().getScreenSize();
                    right = deviceRight.getDeviceInfo().getDefault().getScreenSize();
                    break;
                case PIXELDENSITY_COLUMN_INDEX:
                    left = deviceLeft.getDeviceInfo().getDefault().getPixelDensity();
                    right = deviceRight.getDeviceInfo().getDefault().getPixelDensity();
                    break;
                default:
                    break;
            }

            return left.compareTo(right) * direction;
        }

        /**
         * Returns the SWT constant which represents the direction
         * @return
         */
        public int getSwtDirection()
        {
            return direction == ORDER_ASC ? SWT.UP : SWT.DOWN;
        }
    }

    private static final String NO_DEVICE_SELECTED = "none"; //$NON-NLS-1$

    /**
     * Index of device name column
     */
    private static final int NAME_COLUMN_INDEX = 0;

    /**
     * Index of screen size column
     */
    private static final int SCREENSIZE_COLUMN_INDEX = 1;

    /**
     * Index of pixel density column
     */
    private static final int PIXELDENSITY_COLUMN_INDEX = 2;

    private Button selectAllCheck;

    private CheckboxTableViewer devicesTableViewer;

    private DevicesTableComparator devicesTableComparator;

    /**
     * Construct the GUI for the Devices Tab.
     * @param parent
     * @param style
     * @param preferenceStore
     */
    public DevicesTabComposite(Composite parent, int style, IPreferenceStore preferenceStore)
    {
        super(parent, style);

        //Create main layout
        this.setLayout(new GridLayout(1, false));

        Group deviceListGroup = new Group(this, SWT.NONE);
        Layout layout = new GridLayout(1, false);
        deviceListGroup.setLayout(layout);
        deviceListGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        deviceListGroup.setText(PreflightingUiNLS.DevicesTabComposite_Devices_Group);

        devicesTableViewer =
                CheckboxTableViewer.newCheckList(deviceListGroup, SWT.BORDER | SWT.MULTI
                        | SWT.FULL_SELECTION);

        Control devicesTableControl = devicesTableViewer.getTable();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = parent.getSize().y;
        devicesTableControl.setLayoutData(gd);

        devicesTableComparator = new DevicesTableComparator();
        devicesTableViewer.setComparator(devicesTableComparator);

        //Create Columns
        TableViewerColumn column = new TableViewerColumn(devicesTableViewer, SWT.NONE);
        column.getColumn().setText(PreflightingUiNLS.DevicesTabComposite_Name_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(250);
        column.getColumn().addSelectionListener(new ColumnSelectionAdapter(NAME_COLUMN_INDEX));

        column = new TableViewerColumn(devicesTableViewer, SWT.NONE);
        column.getColumn().setText(PreflightingUiNLS.DevicesTabComposite_ScreenSize_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(80);
        column.getColumn()
                .addSelectionListener(new ColumnSelectionAdapter(SCREENSIZE_COLUMN_INDEX));

        column = new TableViewerColumn(devicesTableViewer, SWT.NONE);
        column.getColumn().setText(PreflightingUiNLS.DevicesTabComposite_pixelDensity_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(80);
        column.getColumn().addSelectionListener(
                new ColumnSelectionAdapter(PIXELDENSITY_COLUMN_INDEX));

        //Configure Table
        devicesTableViewer.getTable().setHeaderVisible(true);

        devicesTableViewer.setContentProvider(new DevicesContentProvider());
        devicesTableViewer.setLabelProvider(new DevicesLabelProvider());

        ValidationManager validationManager = new ValidationManager();
        Collection<DeviceSpecification> deviceSpecifications =
                validationManager.getDevicesSpecsContainer().getDeviceSpecifications();
        devicesTableViewer.setInput(deviceSpecifications);

        devicesTableViewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                boolean isAllSelected = isAllItemsChecked();
                selectAllCheck.setSelection(isAllSelected);
            }
        });

        //Create Select all section
        Composite bottomComposite = new Composite(deviceListGroup, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        bottomComposite.setLayoutData(gd);
        layout = new GridLayout(2, true);
        bottomComposite.setLayout(layout);
        gd = new GridData(SWT.END, SWT.CENTER, false, true);

        selectAllCheck = new Button(bottomComposite, SWT.CHECK);
        selectAllCheck.setText(PreflightingUiNLS.DevicesTabComposite_SelectAll_Check);
        selectAllCheck.setLayoutData(gd);
        selectAllCheck.setSelection(true);
        selectAllCheck.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                modifyCheckBoxes(selectAllCheck.getSelection());
                super.widgetSelected(e);
            }
        });

        init(preferenceStore);
    }

    /*
     * Load data from preference store, reflecting in the GUI.
     */
    private void init(IPreferenceStore preferenceStore)
    {
        String prefKey = preferenceStore.getString(PreflightingUIPlugin.DEVICES_PREFERENCE_KEY);
        if (prefKey.length() > 0) //Found devices, check them!
        {
            if (!prefKey.equals(NO_DEVICE_SELECTED))
            {
                modifyCheckBoxes(false);
                StringTokenizer tokenizer = new StringTokenizer(prefKey, ","); //$NON-NLS-1$
                while (tokenizer.hasMoreTokens())
                {
                    String deviceIdPref = tokenizer.nextToken();
                    checkTableItem(deviceIdPref);
                }
            }
        }
        else
        {
            performDefaults();
        }

        selectAllCheck.setSelection(isAllItemsChecked());
    }

    private void checkTableItem(String deviceIdPref)
    {
        TableItem[] tableItems = devicesTableViewer.getTable().getItems();
        boolean found = false;
        int i = 0;
        while (!found && (i < tableItems.length))
        {
            TableItem tableItem = tableItems[i];
            DeviceSpecification deviceSpec = (DeviceSpecification) tableItem.getData();
            String deviceIdTable = deviceSpec.getId();
            if (deviceIdTable.equalsIgnoreCase(deviceIdPref))
            {
                tableItem.setChecked(true);
                found = true;
            }
            else
            {
                i++;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite#performDefaults()
     */
    @Override
    public void performDefaults()
    {
        modifyCheckBoxes(true);
        selectAllCheck.setSelection(true);
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite#performOk(org.eclipse.jface.preference.IPreferenceStore)
     */
    @Override
    public void performOk(IPreferenceStore preferenceStore)
    {
        Object[] elements = devicesTableViewer.getCheckedElements();
        //Build the comma separated list with all checked devices ids
        StringBuilder stringBuilder = new StringBuilder();
        for (Object element : elements)
        {
            DeviceSpecification deviceSpec = (DeviceSpecification) element;
            stringBuilder.append(deviceSpec.getId());
            stringBuilder.append(","); //$NON-NLS-1$
        }
        //Remove the last comma.
        if (stringBuilder.length() > 0)
        {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        else if (stringBuilder.length() == 0)
        {
            stringBuilder.append(NO_DEVICE_SELECTED);
        }

        preferenceStore.setValue(PreflightingUIPlugin.DEVICES_PREFERENCE_KEY,
                stringBuilder.toString());
        preferenceStore.setValue(PreflightingUIPlugin.USE_ALL_DEVICES_PREFERENCE_KEY,
                selectAllCheck.getSelection());

    }

    /*
     * Update all checkboxes with the given value
     */
    private void modifyCheckBoxes(boolean check)
    {
        TableItem[] items = devicesTableViewer.getTable().getItems();
        for (TableItem tableItem : items)
        {
            tableItem.setChecked(check);
        }
    }

    /*
     * Verifies if all items are checked
     */
    private boolean isAllItemsChecked()
    {
        TableItem[] items = devicesTableViewer.getTable().getItems();
        boolean allChecked = true;
        int i = 0;
        while ((i < items.length) && allChecked)
        {
            TableItem tableItem = items[i];
            if (!tableItem.getChecked())
            {
                allChecked = false;
            }
            i++;
        }
        return allChecked;
    }

    @Override
    public IStatus isValid()
    {
        return Status.OK_STATUS;
    }

    @Override
    public String commandLineBuilder()
    {

        String commandline = null;
        if (!isAllItemsChecked())
        {
            Object[] checkedElements = devicesTableViewer.getCheckedElements();
            StringBuilder stringBuilder = new StringBuilder(150);
            if (checkedElements.length > 0)
            {
                for (Object checkedObj : checkedElements)
                {
                    DeviceSpecification deviceSpec = (DeviceSpecification) checkedObj;
                    stringBuilder.append("-d ");
                    stringBuilder.append(deviceSpec.getId());
                    stringBuilder.append(" ");
                }
            }
            else
            {
                stringBuilder.append("-d none");
            }
            commandline = stringBuilder.toString().trim();
        }
        else
        {
            commandline = "";
        }
        return commandline;

    }
}
