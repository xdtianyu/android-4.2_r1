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
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.Screen;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@linkplain DeviceMenuListener} class is responsible for generating the device
 * menu in the {@link ConfigurationChooser}.
 */
class DeviceMenuListener extends SelectionAdapter {
    private static final String NEXUS = "Nexus";       //$NON-NLS-1$
    private static final String GENERIC = "Generic";   //$NON-NLS-1$
    private static Pattern PATTERN = Pattern.compile(
            "(\\d+\\.?\\d*)in (.+?)( \\(.*Nexus.*\\))?"); //$NON-NLS-1$

    private final ConfigurationChooser mConfigChooser;
    private final Device mDevice;

    DeviceMenuListener(
            @NonNull ConfigurationChooser configChooser,
            @Nullable Device device) {
        mConfigChooser = configChooser;
        mDevice = device;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        mConfigChooser.selectDevice(mDevice);
        mConfigChooser.onDeviceChange();
    }

    static void show(final ConfigurationChooser chooser, ToolItem combo) {
        Configuration configuration = chooser.getConfiguration();
        Device current = configuration.getDevice();
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);

        List<Device> deviceList = chooser.getDeviceList();
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            AvdManager avdManager = sdk.getAvdManager();
            if (avdManager != null) {
                boolean separatorNeeded = false;
                AvdInfo[] avds = avdManager.getValidAvds();
                for (AvdInfo avd : avds) {
                    for (Device device : deviceList) {
                        if (device.getManufacturer().equals(avd.getDeviceManufacturer())
                                && device.getName().equals(avd.getDeviceName())) {
                            separatorNeeded = true;
                            MenuItem item = new MenuItem(menu, SWT.CHECK);
                            item.setText(avd.getName());
                            item.setSelection(current == device);

                            item.addSelectionListener(new DeviceMenuListener(chooser, device));
                        }
                    }
                }

                if (separatorNeeded) {
                    @SuppressWarnings("unused")
                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                }
            }
        }

        // Group the devices by manufacturer, then put them in the menu.
        // If we don't have anything but Nexus devices, group them together rather than
        // make many manufacturer submenus.
        boolean haveNexus = false;
        boolean haveNonNexus = false;
        if (!deviceList.isEmpty()) {
            Map<String, List<Device>> manufacturers = new TreeMap<String, List<Device>>();
            for (Device device : deviceList) {
                List<Device> devices;
                if (isNexus(device)) {
                    haveNexus = true;
                } else if (!isGeneric(device)) {
                    haveNonNexus = true;
                }
                if (manufacturers.containsKey(device.getManufacturer())) {
                    devices = manufacturers.get(device.getManufacturer());
                } else {
                    devices = new ArrayList<Device>();
                    manufacturers.put(device.getManufacturer(), devices);
                }
                devices.add(device);
            }
            if (haveNonNexus) {
                for (List<Device> devices : manufacturers.values()) {
                    Menu manufacturerMenu = menu;
                    if (manufacturers.size() > 1) {
                        MenuItem item = new MenuItem(menu, SWT.CASCADE);
                        item.setText(devices.get(0).getManufacturer());
                        manufacturerMenu = new Menu(menu);
                        item.setMenu(manufacturerMenu);
                    }
                    for (final Device device : devices) {
                        MenuItem deviceItem = new MenuItem(manufacturerMenu, SWT.CHECK);
                        deviceItem.setText(getGenericLabel(device));
                        deviceItem.setSelection(current == device);
                        deviceItem.addSelectionListener(new DeviceMenuListener(chooser, device));
                    }
                }
            } else {
                List<Device> nexus = new ArrayList<Device>();
                List<Device> generic = new ArrayList<Device>();
                if (haveNexus) {
                    // Nexus
                    for (List<Device> devices : manufacturers.values()) {
                        for (Device device : devices) {
                            if (isNexus(device)) {
                                if (device.getManufacturer().equals(GENERIC)) {
                                    generic.add(device);
                                } else {
                                    nexus.add(device);
                                }
                            } else {
                                generic.add(device);
                            }
                        }
                    }
                }

                if (!nexus.isEmpty()) {
                    sortNexusList(nexus);
                    for (final Device device : nexus) {
                        MenuItem item = new MenuItem(menu, SWT.CHECK);
                        item.setText(getNexusLabel(device));
                        item.setSelection(current == device);
                        item.addSelectionListener(new DeviceMenuListener(chooser, device));
                    }

                    @SuppressWarnings("unused")
                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                }

                // Generate the generic menu.
                Collections.reverse(generic);
                for (final Device device : generic) {
                    MenuItem item = new MenuItem(menu, SWT.CHECK);
                    item.setText(getGenericLabel(device));
                    item.setSelection(current == device);
                    item.addSelectionListener(new DeviceMenuListener(chooser, device));
                }
            }
        }

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }

    private static String getNexusLabel(Device d) {
        String name = d.getName();
        Screen screen = d.getDefaultHardware().getScreen();
        float length = (float) screen.getDiagonalLength();
        return String.format(java.util.Locale.US, "%1$s (%3$s\", %2$s)",
                name, getResolutionString(d), Float.toString(length));
    }

    private static String getGenericLabel(Device d) {
        // * Replace "'in'" with '"' (e.g. 2.7" QVGA instead of 2.7in QVGA)
        // * Use the same precision for all devices (all but one specify decimals)
        // * Add some leading space such that the dot ends up roughly in the
        //   same space
        // * Add in screen resolution and density
        String name = d.getName();
        if (name.equals("3.7 FWVGA slider")) {
            // Fix metadata: this one entry doesn't have "in" like the rest of them
            name = "3.7in FWVGA slider";
        }

        Matcher matcher = PATTERN.matcher(name);
        if (matcher.matches()) {
            String size = matcher.group(1);
            String n = matcher.group(2);
            int dot = size.indexOf('.');
            if (dot == -1) {
                size = size + ".0";
                dot = size.length() - 2;
            }
            for (int i = 0; i < 2 - dot; i++) {
                size = ' ' + size;
            }
            name = size + "\" " + n;
        }

        return String.format(java.util.Locale.US, "%1$s (%2$s)", name,
                getResolutionString(d));
    }

    @Nullable
    private static String getResolutionString(Device device) {
        Screen screen = device.getDefaultHardware().getScreen();
        return String.format(java.util.Locale.US,
                "%1$d \u00D7 %2$d: %3$s", // U+00D7: Unicode multiplication sign
                screen.getXDimension(),
                screen.getYDimension(),
                screen.getPixelDensity().getResourceValue());
    }

    private static boolean isGeneric(Device device) {
        return device.getManufacturer().equals(GENERIC);
    }

    private static boolean isNexus(Device device) {
        return device.getName().contains(NEXUS);
    }

    private static void sortNexusList(List<Device> list) {
        Collections.sort(list, new Comparator<Device>() {
            @Override
            public int compare(Device device1, Device device2) {
                // Descending order of age
                return nexusRank(device2) - nexusRank(device1);
            }
            private int nexusRank(Device device) {
                String name = device.getName();
                if (name.endsWith(" One")) {     //$NON-NLS-1$
                    return 1;
                }
                if (name.endsWith(" S")) {       //$NON-NLS-1$
                    return 2;
                }
                if (name.startsWith("Galaxy")) { //$NON-NLS-1$
                    return 3;
                }
                if (name.endsWith(" 7")) {       //$NON-NLS-1$
                    return 4;
                }

                return 5;
            }
        });
    }
}
