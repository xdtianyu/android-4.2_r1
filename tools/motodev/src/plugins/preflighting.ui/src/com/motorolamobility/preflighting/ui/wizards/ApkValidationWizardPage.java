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

package com.motorolamobility.preflighting.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

public class ApkValidationWizardPage extends WizardPage
{
    private Text sourceDirText = null;

    private Button browseDirButton = null;

    private Tree packagesTree = null;

    private Button selectAllButton = null;

    private Button deselectAllButton = null;

    //private WizardSelection selection = null;

    protected Composite mainComposite = null;

    /**
     * Create a new wizard page based on selection
     * 
     * @param pageName
     *            the page name
     * @param selection
     *            the selection
     */
    public ApkValidationWizardPage(String pageName, IStructuredSelection selection)
    {
        super(pageName);
        setDescription(PreflightingUiNLS.ApkValidationWizardPage_description);
        setTitle(PreflightingUiNLS.ApkValidationWizardPage_title);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
     * .Composite)
     */
    public void createControl(Composite parent)
    {
        this.mainComposite = new Composite(parent, SWT.NULL);
        // create new layout with 3 columns of different sizes
        GridLayout layout = new GridLayout(3, false);
        this.mainComposite.setLayout(layout);

        Label sourceDirLabel = new Label(this.mainComposite, SWT.NONE);
        sourceDirLabel.setText(PreflightingUiNLS.ApkValidationWizardPage_folderLabel);

        GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        sourceDirLabel.setLayoutData(layoutData);

        this.sourceDirText = new Text(this.mainComposite, SWT.BORDER);
        layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        this.sourceDirText.setLayoutData(layoutData);
        this.sourceDirText.addListener(SWT.Modify, new SourceDirectoryTextListener());

        this.browseDirButton = new Button(this.mainComposite, SWT.PUSH);
        this.browseDirButton.setText(PreflightingUiNLS.ApkValidationWizardPage_browseLabel);
        layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false);
        this.browseDirButton.setLayoutData(layoutData);
        this.browseDirButton.addListener(SWT.Selection, new BrowseButtonListener());

        Label packagesLabel = new Label(this.mainComposite, SWT.NONE);
        packagesLabel.setText(PreflightingUiNLS.ApkValidationWizardPage_packagesLabel);
        packagesLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));

        this.packagesTree = new Tree(this.mainComposite, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2);
        layoutData.heightHint = 150;
        this.packagesTree.setLayoutData(layoutData);
        this.packagesTree.addListener(SWT.Selection, new TreeSelectionListener());

        Composite selectionButtons = new Composite(this.mainComposite, SWT.FILL);
        layoutData = new GridData(SWT.FILL, SWT.TOP, false, true, 1, 2);
        selectionButtons.setLayoutData(layoutData);
        FillLayout row = new FillLayout(SWT.VERTICAL);
        row.spacing = 3;
        selectionButtons.setLayout(row);

        this.selectAllButton = new Button(selectionButtons, SWT.PUSH);
        this.selectAllButton.setText(PreflightingUiNLS.ApkValidationWizardPage_selectAllLabel);
        SelectionButtonsListener selectionButtonsListener = new SelectionButtonsListener();
        this.selectAllButton.addListener(SWT.Selection, selectionButtonsListener);

        this.deselectAllButton = new Button(selectionButtons, SWT.PUSH);
        this.deselectAllButton.setText(PreflightingUiNLS.ApkValidationWizardPage_deseletAllLabel);
        this.deselectAllButton.addListener(SWT.Selection, selectionButtonsListener);

        updatePageComplete();
        setControl(this.mainComposite);
    }

    /**
     * Populates the tree with the packages of base dir Requires a valid folder
     * set as source dir
     */
    private void populateTree(List<String> selection)
    {
        File sourceDir = getSourcePath().toFile();
        this.packagesTree.removeAll();
        if (sourceDir.isDirectory() && sourceDir.canWrite())
        {
            File[] list = sourceDir.listFiles();
            for (File file : list)
            {
                if (file.canRead() && file.isFile() && file.getName().endsWith(".apk")) //$NON-NLS-1$
                {
                    TreeItem fileItem = new TreeItem(this.packagesTree, SWT.NONE);
                    String text = file.getName();

                    fileItem.setText(text);
                    fileItem.setData(file);
                    if ((selection != null) && selection.contains(file.getName())
                            && file.canWrite())
                    {
                        fileItem.setChecked(true);
                    }
                }
            }
        }
    }

    /**
     * Validates if the source directory is valid one
     * 
     * @return true if the source dir text is valid, false otherwise
     */
    private boolean isSourceDirValid()
    {

        String messageAux = null;
        int severity = IMessageProvider.NONE;

        /*
         * Check if the selected location is valid, even if non existent.
         */
        IPath path = new Path(this.sourceDirText.getText());

        // Test if path is blank, to warn user instead of show an error message
        if (this.sourceDirText.getText().equals("")) //$NON-NLS-1$
        {
            messageAux = PreflightingUiNLS.ApkValidationWizardPage_emptyListMsg;
            severity = IMessageProvider.INFORMATION;
        }

        /*
         * Do Win32 Validation
         */
        if ((messageAux == null) && Platform.getOS().equalsIgnoreCase(Platform.OS_WIN32))
        {
            // test path size
            if (path.toString().length() > 255)
            {
                messageAux = PreflightingUiNLS.ApkValidationWizardPage_tooLongMsg;
                severity = IMessageProvider.WARNING;
            }
            String device = path.getDevice();
            File deviceFile = null;
            if (device != null)
            {
                deviceFile = new File(path.getDevice());
            }

            if ((device != null) && !deviceFile.exists())
            {
                messageAux =
                        PreflightingUiNLS.ApkValidationWizardPage_invalidDeviceMsg
                                + " [" + device + "]"; //$NON-NLS-2$ //$NON-NLS-3$
                severity = IMessageProvider.ERROR;
            }

        }
        // test if path is absolute
        if (messageAux == null)
        {
            if (!path.isAbsolute() || !path.toFile().exists())
            {
                messageAux = PreflightingUiNLS.ApkValidationWizardPage_invalidFolderMsg;
                severity = IMessageProvider.ERROR;
            }
        }

        if (messageAux == null)
        {
            for (String folderName : path.segments())
            {
                if (!ResourcesPlugin.getWorkspace().validateName(folderName, IResource.FOLDER)
                        .isOK())
                {
                    messageAux = PreflightingUiNLS.ApkValidationWizardPage_invalidFolderMsg;
                    severity = IMessageProvider.ERROR;
                }
            }
        }

        if ((messageAux == null) && ((path.toFile().exists() && !path.toFile().isDirectory())))
        {
            messageAux = PreflightingUiNLS.ApkValidationWizardPage_invalidSourceDirectoryMsg;
            severity = IMessageProvider.ERROR;
        }

        /*
         * Setting message
         */
        if (messageAux == null)
        {
            messageAux = PreflightingUiNLS.ApkValidationWizardPage_validateMsg;
            severity = IMessageProvider.NONE;
        }
        setMessage(messageAux, severity);
        return severity == IMessageProvider.NONE;
    }

    /**
     * @return the path of base dir where packages are located
     */
    public IPath getSourcePath()
    {
        return new Path(this.sourceDirText.getText());
    }

    /**
     * 
     * @return the list with selected packages
     */
    public List<File> getSelectedPackages()
    {
        ArrayList<File> selected = new ArrayList<File>();
        for (TreeItem item : this.packagesTree.getItems())
        {
            if (item.getChecked())
            {
                selected.add((File) item.getData());
            }
        }

        return selected;
    }

    /**
     * Update the page status, validating each field of this page Subclasses
     */
    public void updatePageComplete()
    {
        String messageAux = null;
        int severity = IMessageProvider.NONE;

        if (isSourceDirValid())
        {
            if (this.packagesTree.getItemCount() == 0)
            {
                messageAux = PreflightingUiNLS.ApkValidationWizardPage_emptyFolderMsg;
                severity = IMessageProvider.ERROR;
            }
        }
        else
        {
            messageAux = getMessage();
            severity = getMessageType();
        }

        if ((messageAux == null) && (getSelectedPackages().size() == 0))
        {
            messageAux = PreflightingUiNLS.ApkValidationWizardPage_onePackageMsg;
            severity = IMessageProvider.INFORMATION;
        }

        if (messageAux == null)
        {
            messageAux = PreflightingUiNLS.ApkValidationWizardPage_validateMsg;
            severity = IMessageProvider.NONE;
        }

        setMessage(messageAux, severity);
        setPageComplete(severity == IMessageProvider.NONE);
    }

    /**
     * This class implements the listener of browse button, opening the browse
     * window and updating the dir text
     */
    class BrowseButtonListener implements Listener
    {
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
         * .Event)
         */
        public void handleEvent(Event event)
        {
            DirectoryDialog dialog =
                    new DirectoryDialog(ApkValidationWizardPage.this.mainComposite.getShell());
            dialog.setFilterPath(!ApkValidationWizardPage.this.sourceDirText.getText().trim()
                    .equals("") ? ApkValidationWizardPage.this.sourceDirText.getText() : null); //$NON-NLS-1$
            String path = dialog.open();
            if (path != null)
            {
                ApkValidationWizardPage.this.sourceDirText.setText(path);
                populateTree(null);
                updatePageComplete();
            }
        }
    }

    /*
     * Listener to validate any SourceDirectory text Change
     */
    class SourceDirectoryTextListener implements Listener
    {
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
         * .Event)
         */
        public void handleEvent(Event event)
        {
            ApkValidationWizardPage.this.packagesTree.removeAll();
            if (isSourceDirValid())
            {
                populateTree(null);
            }
            updatePageComplete();
        }
    }

    /*
     * This class handles clicks on select all and deselect all buttons
     */
    class SelectionButtonsListener implements Listener
    {

        /**
         * Check/Uncheck all items
         * 
         * @param check
         *            : true for check, false for unckeck
         */
        private void setCheckedAll(boolean check)
        {
            for (TreeItem item : ApkValidationWizardPage.this.packagesTree.getItems())
            {
                item.setChecked(check);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
         * .Event)
         */
        public void handleEvent(Event event)
        {
            if (event.widget == ApkValidationWizardPage.this.selectAllButton)
            {
                setCheckedAll(true);
            }
            else if (event.widget == ApkValidationWizardPage.this.deselectAllButton)
            {
                setCheckedAll(false);
            }
            updatePageComplete();
        }
    }

    /**
     * Listener to update wizard status according tree selection
     * 
     */
    class TreeSelectionListener implements Listener
    {
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
         * .Event)
         */
        public void handleEvent(Event event)
        {
            updatePageComplete();
        }
    }
}
