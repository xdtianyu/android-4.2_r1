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

package com.android.sdkuilib.internal.widgets;

import com.android.SdkConstants;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.resources.Density;
import com.android.resources.ScreenSize;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.SdkManager;
import com.android.sdklib.devices.Camera;
import com.android.sdklib.devices.CameraLocation;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.Hardware;
import com.android.sdklib.devices.Screen;
import com.android.sdklib.devices.Storage;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdConflict;
import com.android.sdklib.internal.avd.HardwareProperties;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.ui.GridDialog;
import com.android.utils.ILogger;
import com.android.utils.Pair;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AvdCreationDialog extends GridDialog {

    private AvdManager mAvdManager;
    private ImageFactory mImageFactory;
    private ILogger mSdkLog;
    private AvdInfo mAvdInfo;

    // A map from manufacturers to their list of devices.
    private Map<String, List<Device>> mDeviceMap;
    private final TreeMap<String, IAndroidTarget> mCurrentTargets =
            new TreeMap<String, IAndroidTarget>();

    private Button mOkButton;

    private Text mAvdName;

    private Combo mDeviceManufacturer;
    private Combo mDeviceName;

    private Combo mTarget;
    private Combo mAbi;

    private Combo mFrontCamera;
    private Combo mBackCamera;

    private Button mSnapshot;
    private Button mGpuEmulation;

    private Text mRam;
    private Text mVmHeap;

    private Text mDataPartition;
    private Combo mDataPartitionSize;

    private Button mSdCardSizeRadio;
    private Text mSdCardSize;
    private Combo mSdCardSizeCombo;
    private Button mSdCardFileRadio;
    private Text mSdCardFile;
    private Button mBrowseSdCard;

    private Button mForceCreation;
    private Composite mStatusComposite;

    private Label mStatusIcon;
    private Label mStatusLabel;

    /**
     * {@link VerifyListener} for {@link Text} widgets that should only contains
     * numbers.
     */
    private final VerifyListener mDigitVerifier = new VerifyListener() {
        @Override
        public void verifyText(VerifyEvent event) {
            int count = event.text.length();
            for (int i = 0; i < count; i++) {
                char c = event.text.charAt(i);
                if (c < '0' || c > '9') {
                    event.doit = false;
                    return;
                }
            }
        }
    };

    public AvdCreationDialog(Shell shell,
            AvdManager avdManager,
            ImageFactory imageFactory,
            ILogger log,
            AvdInfo editAvdInfo) {

        super(shell, 2, false);
        mAvdManager = avdManager;
        mImageFactory = imageFactory;
        mSdkLog = log;
        mAvdInfo = editAvdInfo;

        mDeviceMap = new TreeMap<String, List<Device>>();

        SdkManager sdkMan = avdManager.getSdkManager();
        if (sdkMan != null && sdkMan.getLocation() != null) {
            List<Device> devices = (new DeviceManager(log)).getDevices(sdkMan.getLocation());
            for (Device d : devices) {
                List<Device> list;
                if (mDeviceMap.containsKey(d.getManufacturer())) {
                    list = mDeviceMap.get(d.getManufacturer());
                } else {
                    list = new ArrayList<Device>();
                    mDeviceMap.put(d.getManufacturer(), list);
                }
                list.add(d);
            }
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getShell().setText(mAvdInfo == null ? "Create new Android Virtual Device (AVD)"
                                            : "Edit Android Virtual Device (AVD)");

        mOkButton = getButton(IDialogConstants.OK_ID);

        if (mAvdInfo != null) {
            fillExistingAvdInfo(mAvdInfo);
        }

        validatePage();
        return control;
    }

    @Override
    public void createDialogContent(Composite parent) {

        Label label;
        String tooltip;
        ValidateListener validateListener = new ValidateListener();

        // --- avd name
        label = new Label(parent, SWT.NONE);
        label.setText("AVD Name:");
        tooltip = "The name of the Android Virtual Device";
        label.setToolTipText(tooltip);
        mAvdName = new Text(parent, SWT.BORDER);
        mAvdName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mAvdName.addModifyListener(new CreateNameModifyListener());

        // --- device selection
        label = new Label(parent, SWT.NONE);
        label.setText("Device\nManufacturer:");
        tooltip = "The manufacturer of the device this AVD will be based on";
        mDeviceManufacturer = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        for (String manufacturer : mDeviceMap.keySet()) {
            mDeviceManufacturer.add(manufacturer);
        }
        mDeviceManufacturer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDeviceManufacturer.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                reloadDeviceNameCombo();
                validatePage();
            }
        });

        label = new Label(parent, SWT.NONE);
        label.setText("Device Name:");
        tooltip = "The name of the device this AVD will be based on";
        mDeviceName = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mDeviceName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDeviceName.addSelectionListener(new DeviceSelectionListener());

        // --- api target
        label = new Label(parent, SWT.NONE);
        label.setText("Target:");
        tooltip = "The target API of the AVD";
        label.setToolTipText(tooltip);
        mTarget = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mTarget.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTarget.setToolTipText(tooltip);
        mTarget.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                reloadAbiTypeCombo();
                validatePage();
            }
        });

        reloadTargetCombo();

        // --- avd ABIs
        label = new Label(parent, SWT.NONE);
        label.setText("CPU/ABI:");
        tooltip = "The CPU/ABI of the virtual device";
        label.setToolTipText(tooltip);
        mAbi = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mAbi.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mAbi.setToolTipText(tooltip);
        mAbi.addSelectionListener(validateListener);

        label = new Label(parent, SWT.NONE);
        label.setText("Front Camera:");
        tooltip = "";
        label.setToolTipText(tooltip);
        mFrontCamera = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mFrontCamera.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFrontCamera.add("None");
        mFrontCamera.add("Emulated");
        mFrontCamera.add("Webcam0");
        mFrontCamera.select(0);

        label = new Label(parent, SWT.NONE);
        label.setText("Back Camera:");
        tooltip = "";
        label.setToolTipText(tooltip);
        mBackCamera = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mBackCamera.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mBackCamera.add("None");
        mBackCamera.add("Emulated");
        mBackCamera.add("Webcam0");
        mBackCamera.select(0);

        toggleCameras();

        // --- memory options group
        label = new Label(parent, SWT.NONE);
        label.setText("Memory Options:");


        Group memoryGroup = new Group(parent, SWT.BORDER);
        memoryGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        memoryGroup.setLayout(new GridLayout(4, false));

        label = new Label(memoryGroup, SWT.NONE);
        label.setText("RAM:");
        tooltip = "The amount of RAM the emulated device should have in MiB";
        label.setToolTipText(tooltip);
        mRam = new Text(memoryGroup, SWT.BORDER);
        mRam.addVerifyListener(mDigitVerifier);
        mRam.addModifyListener(validateListener);
        mRam.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(memoryGroup, SWT.NONE);
        label.setText("VM Heap:");
        tooltip = "The amount of memory, in MiB, available to typical Android applications";
        label.setToolTipText(tooltip);
        mVmHeap = new Text(memoryGroup, SWT.BORDER);
        mVmHeap.addVerifyListener(mDigitVerifier);
        mVmHeap.addModifyListener(validateListener);
        mVmHeap.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mVmHeap.setToolTipText(tooltip);

        // --- Data partition group
        label = new Label(parent, SWT.NONE);
        label.setText("Internal Storage:");
        tooltip = "The size of the data partition on the device.";
        Group storageGroup = new Group(parent, SWT.NONE);
        storageGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        storageGroup.setLayout(new GridLayout(2, false));
        mDataPartition = new Text(storageGroup, SWT.BORDER);
        mDataPartition.setText("200");
        mDataPartition.addVerifyListener(mDigitVerifier);
        mDataPartition.addModifyListener(validateListener);
        mDataPartition.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDataPartitionSize = new Combo(storageGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
        mDataPartitionSize.add("MiB");
        mDataPartitionSize.add("GiB");
        mDataPartitionSize.select(0);
        mDataPartitionSize.addModifyListener(validateListener);

        // --- sd card group
        label = new Label(parent, SWT.NONE);
        label.setText("SD Card:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group sdCardGroup = new Group(parent, SWT.NONE);
        sdCardGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdCardGroup.setLayout(new GridLayout(3, false));

        mSdCardSizeRadio = new Button(sdCardGroup, SWT.RADIO);
        mSdCardSizeRadio.setText("Size:");
        mSdCardSizeRadio.setToolTipText("Create a new SD Card file");
        mSdCardSizeRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean sizeMode = mSdCardSizeRadio.getSelection();
                enableSdCardWidgets(sizeMode);
                validatePage();
            }
        });

        mSdCardSize = new Text(sdCardGroup, SWT.BORDER);
        mSdCardSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCardSize.addVerifyListener(mDigitVerifier);
        mSdCardSize.addModifyListener(validateListener);
        mSdCardSize.setToolTipText("Size of the new SD Card file (must be at least 9 MiB)");

        mSdCardSizeCombo = new Combo(sdCardGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        mSdCardSizeCombo.add("KiB");
        mSdCardSizeCombo.add("MiB");
        mSdCardSizeCombo.add("GiB");
        mSdCardSizeCombo.select(1);
        mSdCardSizeCombo.addSelectionListener(validateListener);

        mSdCardFileRadio = new Button(sdCardGroup, SWT.RADIO);
        mSdCardFileRadio.setText("File:");
        mSdCardFileRadio.setToolTipText("Use an existing file for the SD Card");

        mSdCardFile = new Text(sdCardGroup, SWT.BORDER);
        mSdCardFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCardFile.addModifyListener(validateListener);
        mSdCardFile.setToolTipText("File to use for the SD Card");

        mBrowseSdCard = new Button(sdCardGroup, SWT.PUSH);
        mBrowseSdCard.setText("Browse...");
        mBrowseSdCard.setToolTipText("Select the file to use for the SD Card");
        mBrowseSdCard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onBrowseSdCard();
                validatePage();
            }
        });

        mSdCardSizeRadio.setSelection(true);
        enableSdCardWidgets(true);

        // --- avd options group
        label = new Label(parent, SWT.NONE);
        label.setText("Emulation Options:");
        Group optionsGroup = new Group(parent, SWT.NONE);
        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        optionsGroup.setLayout(new GridLayout(2, true));
        mSnapshot = new Button(optionsGroup, SWT.CHECK);
        mSnapshot.setText("Snapshot");
        mSnapshot.setToolTipText("Emulator's state will be persisted between emulator executions");
        mSnapshot.addSelectionListener(validateListener);
        mGpuEmulation = new Button(optionsGroup, SWT.CHECK);
        mGpuEmulation.setText("GPU Emulation");
        mGpuEmulation.setToolTipText("Enable hardware OpenGLES emulation");
        mGpuEmulation.addSelectionListener(validateListener);

        // --- force creation group
        mForceCreation = new Button(parent, SWT.CHECK);
        mForceCreation.setText("Override the existing AVD with the same name");
        mForceCreation
                .setToolTipText("There's already an AVD with the same name. Check this to delete it and replace it by the new AVD.");
        mForceCreation.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
                true, false, 2, 1));
        mForceCreation.setEnabled(false);
        mForceCreation.addSelectionListener(validateListener);

        // add a separator to separate from the ok/cancel button
        label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));

        // add stuff for the error display
        mStatusComposite = new Composite(parent, SWT.NONE);
        mStatusComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 3, 1));
        GridLayout gl;
        mStatusComposite.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;

        mStatusIcon = new Label(mStatusComposite, SWT.NONE);
        mStatusIcon.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));
        mStatusLabel = new Label(mStatusComposite, SWT.NONE);
        mStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStatusLabel.setText(""); //$NON-NLS-1$

    }

    /**
     * {@link ModifyListener} used for live-validation of the fields content.
     */
    private class ValidateListener extends SelectionAdapter implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            validatePage();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);
            validatePage();
        }
    }

    /**
     * Callback when the AVD name is changed. When creating a new AVD, enables
     * the force checkbox if the name is a duplicate. When editing an existing
     * AVD, it's OK for the name to match the existing AVD.
     */
    private class CreateNameModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            String name = mAvdName.getText().trim();
            if (mAvdInfo == null || !name.equals(mAvdInfo.getName())) {
                // Case where we're creating a new AVD or editing an existing
                // one
                // and the AVD name has been changed... check for name
                // uniqueness.

                Pair<AvdConflict, String> conflict = mAvdManager.isAvdNameConflicting(name);
                if (conflict.getFirst() != AvdManager.AvdConflict.NO_CONFLICT) {
                    // If we're changing the state from disabled to enabled,
                    // make sure
                    // to uncheck the button, to force the user to voluntarily
                    // re-enforce it.
                    // This happens when editing an existing AVD and changing
                    // the name from
                    // the existing AVD to another different existing AVD.
                    if (!mForceCreation.isEnabled()) {
                        mForceCreation.setEnabled(true);
                        mForceCreation.setSelection(false);
                    }
                } else {
                    mForceCreation.setEnabled(false);
                    mForceCreation.setSelection(false);
                }
            } else {
                // Case where we're editing an existing AVD with the name
                // unchanged.

                mForceCreation.setEnabled(false);
                mForceCreation.setSelection(false);
            }
            validatePage();
        }
    }

    private class DeviceSelectionListener extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            Device currentDevice = null;
            for (Device d : mDeviceMap.get(mDeviceManufacturer.getText())) {
                if (d.getName().equals(mDeviceName.getText())) {
                    currentDevice = d;
                    break;
                }
            }

            if (currentDevice != null) {
                Hardware hw = currentDevice.getDefaultHardware();
                Long ram = hw.getRam().getSizeAsUnit(Storage.Unit.MiB);
                mRam.setText(Long.toString(ram));

                // Set the default VM heap size. This is based on the Android CDD minimums for each
                // screen size and density.
                Screen s = hw.getScreen();
                ScreenSize size = s.getSize();
                Density density = s.getPixelDensity();
                int vmHeapSize = 32;
                if (size.equals(ScreenSize.XLARGE)) {
                    switch (density) {
                        case LOW:
                        case MEDIUM:
                            vmHeapSize = 32;
                            break;
                        case TV:
                        case HIGH:
                            vmHeapSize = 64;
                            break;
                        case XHIGH:
                        case XXHIGH:
                            vmHeapSize = 128;
                    }
                } else {
                    switch (density) {
                        case LOW:
                        case MEDIUM:
                            vmHeapSize = 16;
                            break;
                        case TV:
                        case HIGH:
                            vmHeapSize = 32;
                            break;
                        case XHIGH:
                        case XXHIGH:
                            vmHeapSize = 64;

                    }
                }
                mVmHeap.setText(Integer.toString(vmHeapSize));
            }

            toggleCameras();
            validatePage();
        }

    }

    private void toggleCameras() {
        mFrontCamera.setEnabled(false);
        mBackCamera.setEnabled(false);
        if (mDeviceName.getSelectionIndex() >= 0) {
            List<Device> devices = mDeviceMap.get(mDeviceManufacturer.getText());
            for (Device d : devices) {
                if (mDeviceName.getText().equals(d.getName())) {
                    for (Camera c : d.getDefaultHardware().getCameras()) {
                        if (CameraLocation.FRONT.equals(c.getLocation())) {
                            mFrontCamera.setEnabled(true);
                        }
                        if (CameraLocation.BACK.equals(c.getLocation())) {
                            mBackCamera.setEnabled(true);
                        }
                    }
                }
            }

        }
    }

    private void reloadDeviceNameCombo() {
        mDeviceName.removeAll();
        if (mDeviceMap.containsKey(mDeviceManufacturer.getText())) {
            for (final Device d : mDeviceMap.get(mDeviceManufacturer.getText())) {
                mDeviceName.add(d.getName());
            }
        }

    }

    private void reloadTargetCombo() {
        String selected = null;
        int index = mTarget.getSelectionIndex();
        if (index >= 0) {
            selected = mTarget.getItem(index);
        }

        mCurrentTargets.clear();
        mTarget.removeAll();

        boolean found = false;
        index = -1;

        SdkManager sdkManager = mAvdManager.getSdkManager();
        if (sdkManager != null) {
            for (IAndroidTarget target : sdkManager.getTargets()) {
                String name;
                if (target.isPlatform()) {
                    name = String.format("%s - API Level %s",
                            target.getName(),
                            target.getVersion().getApiString());
                } else {
                    name = String.format("%s (%s) - API Level %s",
                            target.getName(),
                            target.getVendor(),
                            target.getVersion().getApiString());
                }
                mCurrentTargets.put(name, target);
                mTarget.add(name);
                if (!found) {
                    index++;
                    found = name.equals(selected);
                }
            }
        }

        mTarget.setEnabled(mCurrentTargets.size() > 0);

        if (found) {
            mTarget.select(index);
        }
    }

    /**
     * Reload all the abi types in the selection list
     */
    private void reloadAbiTypeCombo() {
        String selected = null;
        boolean found = false;

        int index = mTarget.getSelectionIndex();
        if (index >= 0) {
            String targetName = mTarget.getItem(index);
            IAndroidTarget target = mCurrentTargets.get(targetName);

            ISystemImage[] systemImages = getSystemImages(target);

            mAbi.setEnabled(systemImages.length > 1);

            // If user explicitly selected an ABI before, preserve that option
            // If user did not explicitly select before (only one option before)
            // force them to select
            index = mAbi.getSelectionIndex();
            if (index >= 0 && mAbi.getItemCount() > 1) {
                selected = mAbi.getItem(index);
            }

            mAbi.removeAll();

            int i;
            for (i = 0; i < systemImages.length; i++) {
                String prettyAbiType = AvdInfo.getPrettyAbiType(systemImages[i].getAbiType());
                mAbi.add(prettyAbiType);
                if (!found) {
                    found = prettyAbiType.equals(selected);
                    if (found) {
                        mAbi.select(i);
                    }
                }
            }

            if (systemImages.length == 1) {
                mAbi.select(0);
            }
        }
    }

    /**
     * Enable or disable the sd card widgets.
     *
     * @param sizeMode if true the size-based widgets are to be enabled, and the
     *            file-based ones disabled.
     */
    private void enableSdCardWidgets(boolean sizeMode) {
        mSdCardSize.setEnabled(sizeMode);
        mSdCardSizeCombo.setEnabled(sizeMode);

        mSdCardFile.setEnabled(!sizeMode);
        mBrowseSdCard.setEnabled(!sizeMode);
    }

    private void onBrowseSdCard() {
        FileDialog dlg = new FileDialog(getContents().getShell(), SWT.OPEN);
        dlg.setText("Choose SD Card image file.");

        String fileName = dlg.open();
        if (fileName != null) {
            mSdCardFile.setText(fileName);
        }
    }

    @Override
    public void okPressed() {
        if (createAvd()) {
            super.okPressed();
        }
    }

    private void validatePage() {
        String error = null;
        String warning = null;
        boolean valid = true;
        if (mAvdName.getText().isEmpty()) {
            valid = false;
        }

        if (mDeviceManufacturer.getSelectionIndex() < 0 || mDeviceName.getSelectionIndex() < 0) {
            valid = false;
        }

        if (mTarget.getSelectionIndex() < 0 || mAbi.getSelectionIndex() < 0) {
            valid = false;
        }

        if (mRam.getText().isEmpty()) {
            valid = false;
        }

        if (mVmHeap.getText().isEmpty()) {
            valid = false;
        }

        if (mDataPartition.getText().isEmpty() || mDataPartitionSize.getSelectionIndex() < 0) {
            valid = false;
            error = "Data partition must be a valid file size.";
        }

        // validate sdcard size or file
        if (mSdCardSizeRadio.getSelection()) {
            if (!mSdCardSize.getText().isEmpty() && mSdCardSizeCombo.getSelectionIndex() >= 0) {
                try {
                    long sdSize = Long.parseLong(mSdCardSize.getText());

                    int sizeIndex = mSdCardSizeCombo.getSelectionIndex();
                    if (sizeIndex >= 0) {
                        // index 0 shifts by 10 (1024=K), index 1 by 20, etc.
                        sdSize <<= 10 * (1 + sizeIndex);
                    }

                    if (sdSize < AvdManager.SDCARD_MIN_BYTE_SIZE ||
                            sdSize > AvdManager.SDCARD_MAX_BYTE_SIZE) {
                        valid = false;
                        error = "SD Card size is invalid. Range is 9 MiB..1023 GiB.";
                    }
                } catch (NumberFormatException e) {
                    valid = false;
                    error = " SD Card size must be a valid integer between 9 MiB and 1023 GiB";
                }
            }
        } else {
            if (mSdCardFile.getText().isEmpty() || !new File(mSdCardFile.getText()).isFile()) {
                valid = false;
                error = "SD Card path isn't valid.";
            }
        }

        if (mForceCreation.isEnabled() && !mForceCreation.getSelection()) {
            valid = false;
            error = String.format(
                    "The AVD name '%s' is already used.\n" +
                            "Check \"Override the existing AVD\" to delete the existing one.",
                    mAvdName.getText());
        }

        if (mAvdInfo != null && !mAvdInfo.getName().equals(mAvdName.getText())) {
            warning = String.format("The AVD '%1$s' will be duplicated into '%2$s'.",
                    mAvdInfo.getName(),
                    mAvdName.getText());
        }

        if (mGpuEmulation.getSelection() && mSnapshot.getSelection()) {
            valid = false;
            error = "GPU Emulation and Snapshot cannot be used simultaneously";
        }

        mOkButton.setEnabled(valid);
        if (error != null) {
            mStatusIcon.setImage(mImageFactory.getImageByName("reject_icon16.png")); //$NON-NLS-1$
            mStatusLabel.setText(error);
        } else if (warning != null) {
            mStatusIcon.setImage(mImageFactory.getImageByName("warning_icon16.png")); //$NON-NLS-1$
            mStatusLabel.setText(warning);
        } else {
            mStatusIcon.setImage(null);
            mStatusLabel.setText(" \n "); //$NON-NLS-1$
        }

        mStatusComposite.pack(true);
    }

    private boolean createAvd() {

        String avdName = mAvdName.getText();
        if (avdName == null || avdName.isEmpty()) {
            return false;
        }

        String targetName = mTarget.getItem(mTarget.getSelectionIndex());
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return false;
        }

        // get the abi type
        String abiType = SdkConstants.ABI_ARMEABI;
        ISystemImage[] systemImages = getSystemImages(target);
        if (systemImages.length > 0) {
            int abiIndex = mAbi.getSelectionIndex();
            if (abiIndex >= 0) {
                String prettyname = mAbi.getItem(abiIndex);
                // Extract the abi type
                int firstIndex = prettyname.indexOf("(");
                int lastIndex = prettyname.indexOf(")");
                abiType = prettyname.substring(firstIndex + 1, lastIndex);
            }
        }

        // get the SD card data from the UI.
        String sdName = null;
        if (mSdCardSizeRadio.getSelection()) {
            // size mode
            String value = mSdCardSize.getText().trim();
            if (value.length() > 0) {
                sdName = value;
                // add the unit
                switch (mSdCardSizeCombo.getSelectionIndex()) {
                    case 0:
                        sdName += "K"; //$NON-NLS-1$
                        break;
                    case 1:
                        sdName += "M"; //$NON-NLS-1$
                        break;
                    case 2:
                        sdName += "G"; //$NON-NLS-1$
                        break;
                    default:
                        // shouldn't be here
                        assert false;
                }
            }
        } else {
            // file mode.
            sdName = mSdCardFile.getText().trim();
        }

        // Get the device
        List<Device> devices = mDeviceMap.get(mDeviceManufacturer.getText());
        if (devices == null) {
            return false;
        }

        Device device = null;
        for (Device d : devices) {
            if (mDeviceName.getText().equals(d.getName())) {
                device = d;
                break;
            }
        }

        if (device == null) {
            return false;
        }

        Screen s = device.getDefaultHardware().getScreen();
        String skinName = s.getXDimension() + "x" + s.getYDimension();

        ILogger log = mSdkLog;
        if (log == null || log instanceof MessageBoxLog) {
            // If the current logger is a message box, we use our own (to make sure
            // to display errors right away and customize the title).
            log = new MessageBoxLog(
                    String.format("Result of creating AVD '%s':", avdName),
                    getContents().getDisplay(),
                    false /* logErrorsOnly */);
        }

        Map<String, String> hwProps = DeviceManager.getHardwareProperties(device);
        if (mGpuEmulation.getSelection()) {
            hwProps.put(AvdManager.AVD_INI_GPU_EMULATION, HardwareProperties.BOOLEAN_YES);
        }

        File avdFolder = null;
        try {
            avdFolder = AvdInfo.getDefaultAvdFolder(mAvdManager, avdName);
        } catch (AndroidLocationException e) {
            return false;
        }

        // Although the device has this information, some devices have more RAM than we'd want to
        // allocate to an emulator.
        hwProps.put(AvdManager.AVD_INI_RAM_SIZE, mRam.getText());
        hwProps.put(AvdManager.AVD_INI_VM_HEAP_SIZE, mVmHeap.getText());

        String suffix;
        switch (mDataPartitionSize.getSelectionIndex()) {
            case 0:
                suffix = "M";
                break;
            case 1:
                suffix = "G";
                break;
            default:
                suffix = "K";
        }
        hwProps.put(AvdManager.AVD_INI_DATA_PARTITION_SIZE, mDataPartition.getText()+suffix);

        if (mFrontCamera.isEnabled()) {
            hwProps.put(AvdManager.AVD_INI_CAMERA_FRONT,
                    mFrontCamera.getText().toLowerCase());
        }

        if (mBackCamera.isEnabled()) {
            hwProps.put(AvdManager.AVD_INI_CAMERA_BACK,
                    mBackCamera.getText().toLowerCase());
        }

        if (sdName != null) {
            hwProps.put(HardwareProperties.HW_SDCARD, HardwareProperties.BOOLEAN_YES);
        }

        AvdInfo avdInfo = mAvdManager.createAvd(avdFolder,
                avdName,
                target,
                abiType,
                skinName,
                sdName,
                hwProps,
                mSnapshot.getSelection(),
                mForceCreation.getSelection(),
                mAvdInfo != null, // edit existing
                log);

        boolean success = avdInfo != null;

        if (log instanceof MessageBoxLog) {
            ((MessageBoxLog) log).displayResult(success);
        }
        return success;
    }

    private void fillExistingAvdInfo(AvdInfo avd) {
        mAvdName.setText(avd.getName());
        String manufacturer = avd.getDeviceManufacturer();
        for (int i = 0; i < mDeviceManufacturer.getItemCount(); i++) {
            if (mDeviceManufacturer.getItem(i).equals(manufacturer)) {
                mDeviceManufacturer.select(i);
                break;
            }
        }
        reloadDeviceNameCombo();

        String deviceName = avd.getDeviceName();
        for (int i = 0; i < mDeviceName.getItemCount(); i++) {
            if (mDeviceName.getItem(i).equals(deviceName)) {
                mDeviceName.select(i);
                break;
            }
        }
        toggleCameras();

        IAndroidTarget target = avd.getTarget();

        if (target != null && !mCurrentTargets.isEmpty()) {
            // Try to select the target in the target combo.
            // This will fail if the AVD needs to be repaired.
            //
            // This is a linear search but the list is always
            // small enough and we only do this once.
            int n = mTarget.getItemCount();
            for (int i = 0; i < n; i++) {
                if (target.equals(mCurrentTargets.get(mTarget.getItem(i)))) {
                    mTarget.select(i);
                    reloadAbiTypeCombo();
                    break;
                }
            }
        }

        ISystemImage[] systemImages = getSystemImages(target);
        if (target != null && systemImages.length > 0) {
            mAbi.setEnabled(systemImages.length > 1);
            String abiType = AvdInfo.getPrettyAbiType(avd.getAbiType());
            int n = mAbi.getItemCount();
            for (int i = 0; i < n; i++) {
                if (abiType.equals(mAbi.getItem(i))) {
                    mAbi.select(i);
                    break;
                }
            }
        }

        Map<String, String> props = avd.getProperties();

        if (props != null) {
            String snapshots = props.get(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
            if (snapshots != null && snapshots.length() > 0) {
                mSnapshot.setSelection(snapshots.equals("true"));
            }

            String gpuEmulation = props.get(AvdManager.AVD_INI_GPU_EMULATION);
            mGpuEmulation.setSelection(gpuEmulation != null &&
                    gpuEmulation.equals(HardwareProperties.BOOLEAN_VALUES[0]));

            String sdcard = props.get(AvdManager.AVD_INI_SDCARD_PATH);
            if (sdcard != null && sdcard.length() > 0) {
                enableSdCardWidgets(false);
                mSdCardSizeRadio.setSelection(false);
                mSdCardFileRadio.setSelection(true);
                mSdCardFile.setText(sdcard);
            }

            String ramSize = props.get(AvdManager.AVD_INI_RAM_SIZE);
            if (ramSize != null) {
                mRam.setText(ramSize);
            }

            String vmHeapSize = props.get(AvdManager.AVD_INI_VM_HEAP_SIZE);
            if (vmHeapSize != null) {
                mVmHeap.setText(vmHeapSize);
            }

            String dataPartitionSize = props.get(AvdManager.AVD_INI_DATA_PARTITION_SIZE);
            if (dataPartitionSize != null) {
                mDataPartition.setText(
                        dataPartitionSize.substring(0, dataPartitionSize.length() - 1));
                switch (dataPartitionSize.charAt(dataPartitionSize.length() - 1)) {
                    case 'M':
                        mDataPartitionSize.select(0);
                        break;
                    case 'G':
                        mDataPartitionSize.select(1);
                        break;
                    default:
                        mDataPartitionSize.select(-1);
                }
            }

            String cameraFront = props.get(AvdManager.AVD_INI_CAMERA_FRONT);
            if (cameraFront != null) {
                String[] items = mFrontCamera.getItems();
                for (int i = 0; i < items.length; i++) {
                    if (items[i].toLowerCase().equals(cameraFront)) {
                        mFrontCamera.select(i);
                        break;
                    }
                }
            }

            String cameraBack = props.get(AvdManager.AVD_INI_CAMERA_BACK);
            if (cameraBack != null) {
                String[] items = mBackCamera.getItems();
                for (int i = 0; i < items.length; i++) {
                    if (items[i].toLowerCase().equals(cameraBack)) {
                        mBackCamera.select(i);
                        break;
                    }
                }
            }

            sdcard = props.get(AvdManager.AVD_INI_SDCARD_SIZE);
            if (sdcard != null && sdcard.length() > 0) {
                String[] values = new String[2];
                long sdcardSize = AvdManager.parseSdcardSize(sdcard, values);

                if (sdcardSize != AvdManager.SDCARD_NOT_SIZE_PATTERN) {
                    enableSdCardWidgets(true);
                    mSdCardFileRadio.setSelection(false);
                    mSdCardSizeRadio.setSelection(true);

                    mSdCardSize.setText(values[0]);

                    String suffix = values[1];
                    int n = mSdCardSizeCombo.getItemCount();
                    for (int i = 0; i < n; i++) {
                        if (mSdCardSizeCombo.getItem(i).startsWith(suffix)) {
                            mSdCardSizeCombo.select(i);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the list of system images of a target.
     * <p/>
     * If target is null, returns an empty list. If target is an add-on with no
     * system images, return the list from its parent platform.
     *
     * @param target An IAndroidTarget. Can be null.
     * @return A non-null ISystemImage array. Can be empty.
     */
    private ISystemImage[] getSystemImages(IAndroidTarget target) {
        if (target != null) {
            ISystemImage[] images = target.getSystemImages();

            if ((images == null || images.length == 0) && !target.isPlatform()) {
                // If an add-on does not provide any system images, use the ones
                // from the parent.
                images = target.getParent().getSystemImages();
            }

            if (images != null) {
                return images;
            }
        }

        return new ISystemImage[0];
    }

}
