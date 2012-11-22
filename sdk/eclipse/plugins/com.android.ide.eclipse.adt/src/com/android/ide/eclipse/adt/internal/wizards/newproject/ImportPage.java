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
package com.android.ide.eclipse.adt.internal.wizards.newproject;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.tools.lint.detector.api.LintUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** WizardPage for importing Android projects */
class ImportPage extends WizardPage implements SelectionListener, IStructuredContentProvider,
        ICheckStateListener, ILabelProvider, IColorProvider, KeyListener, TraverseListener {
    private final NewProjectWizardState mValues;
    private List<ImportedProject> mProjectPaths;
    private final IProject[] mExistingProjects;

    private Text mDir;
    private Button mBrowseButton;
    private Button mCopyCheckBox;
    private Button mRefreshButton;
    private Button mDeselectAllButton;
    private Button mSelectAllButton;
    private Table mTable;
    private CheckboxTableViewer mCheckboxTableViewer;
    private WorkingSetGroup mWorkingSetGroup;

    ImportPage(NewProjectWizardState values) {
        super("importPage"); //$NON-NLS-1$
        mValues = values;
        setTitle("Import Projects");
        setDescription("Select a directory to search for existing Android projects");
        mWorkingSetGroup = new WorkingSetGroup();
        setWorkingSets(new IWorkingSet[0]);

        // Record all projects such that we can ensure that the project names are unique
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        mExistingProjects = workspaceRoot.getProjects();
    }

    public void init(IStructuredSelection selection, IWorkbenchPart activePart) {
        setWorkingSets(WorkingSetHelper.getSelectedWorkingSet(selection, activePart));
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and aren't unused
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(3, false));

        Label directoryLabel = new Label(container, SWT.NONE);
        directoryLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        directoryLabel.setText("Root Directory:");

        mDir = new Text(container, SWT.BORDER);
        mDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mDir.addKeyListener(this);
        mDir.addTraverseListener(this);

        mBrowseButton = new Button(container, SWT.NONE);
        mBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mBrowseButton.setText("Browse...");
        mBrowseButton.addSelectionListener(this);

        Label projectsLabel = new Label(container, SWT.NONE);
        projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        projectsLabel.setText("Projects:");

        mCheckboxTableViewer = CheckboxTableViewer.newCheckList(container,
                SWT.BORDER | SWT.FULL_SELECTION);
        mTable = mCheckboxTableViewer.getTable();
        mTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 4));
        mTable.addSelectionListener(this);
        mCheckboxTableViewer.setLabelProvider(this);
        mCheckboxTableViewer.setContentProvider(this);
        mCheckboxTableViewer.setInput(this);
        mCheckboxTableViewer.addCheckStateListener(this);

        mSelectAllButton = new Button(container, SWT.NONE);
        mSelectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mSelectAllButton.setText("Select All");
        mSelectAllButton.addSelectionListener(this);

        mDeselectAllButton = new Button(container, SWT.NONE);
        mDeselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mDeselectAllButton.setText("Deselect All");
        mDeselectAllButton.addSelectionListener(this);

        mRefreshButton = new Button(container, SWT.NONE);
        mRefreshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mRefreshButton.setText("Refresh");
        mRefreshButton.addSelectionListener(this);
        new Label(container, SWT.NONE);

        mCopyCheckBox = new Button(container, SWT.CHECK);
        mCopyCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mCopyCheckBox.setText("Copy projects into workspace");
        mCopyCheckBox.addSelectionListener(this);

        Composite group = mWorkingSetGroup.createControl(container);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        validatePage();
    }

    private void refresh() {
        File root = new File(mDir.getText().trim());
        mProjectPaths = searchForProjects(root);
        mCheckboxTableViewer.refresh();
        mCheckboxTableViewer.setAllChecked(true);

        List<ImportedProject> selected = new ArrayList<ImportedProject>();
        List<ImportedProject> disabled = new ArrayList<ImportedProject>();
        for (ImportedProject project : mProjectPaths) {
            String projectName = project.getProjectName();
            boolean invalid = false;
            for (IProject existingProject : mExistingProjects) {
                if (projectName.equals(existingProject.getName())) {
                    invalid = true;
                    break;
                }
            }
            if (invalid) {
                disabled.add(project);
            } else {
                selected.add(project);
            }
        }

        mValues.importProjects = selected;

        mCheckboxTableViewer.setGrayedElements(disabled.toArray());
        mCheckboxTableViewer.setCheckedElements(selected.toArray());
        mCheckboxTableViewer.refresh();
        mCheckboxTableViewer.getTable().setFocus();
        validatePage();
    }

    private List<ImportedProject> searchForProjects(File dir) {
        List<ImportedProject> projects = new ArrayList<ImportedProject>();
        addProjects(dir, projects);
        return projects;
    }

    /** Finds all project directories under the given directory */
    private void addProjects(File dir, List<ImportedProject> projects) {
        if (dir.isDirectory()) {
            if (LintUtils.isProjectDir(dir)) {
                projects.add(new ImportedProject(dir));
            }

            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    addProjects(child, projects);
                }
            }
        }
    }

    private void validatePage() {
        IStatus status = null;

        // Validate project name -- unless we're creating a sample, in which case
        // the user will get a chance to pick the name on the Sample page
        if (mProjectPaths == null || mProjectPaths.isEmpty()) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "Select a directory to search for existing Android projects");
        } else if (mValues.importProjects == null || mValues.importProjects.isEmpty()) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "Select at least one project");
        } else {
            for (ImportedProject project : mValues.importProjects) {
                if (mCheckboxTableViewer.getGrayed(project)) {
                    status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                            String.format("Cannot import %1$s because the project name is in use",
                                    project.getProjectName()));
                    break;
                }
            }
        }

        // -- update UI & enable finish if there's no error
        setPageComplete(status == null || status.getSeverity() != IStatus.ERROR);
        if (status != null) {
            setMessage(status.getMessage(),
                    status.getSeverity() == IStatus.ERROR
                        ? IMessageProvider.ERROR : IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    /**
     * Returns the working sets to which the new project should be added.
     *
     * @return the selected working sets to which the new project should be added
     */
    private IWorkingSet[] getWorkingSets() {
        return mWorkingSetGroup.getSelectedWorkingSets();
    }

    /**
     * Sets the working sets to which the new project should be added.
     *
     * @param workingSets the initial selected working sets
     */
    private void setWorkingSets(IWorkingSet[] workingSets) {
        assert workingSets != null;
        mWorkingSetGroup.setWorkingSets(workingSets);
    }

    @Override
    public IWizardPage getNextPage() {
        // Sync working set data to the value object, since the WorkingSetGroup
        // doesn't let us add listeners to do this lazily
        mValues.workingSets = getWorkingSets();

        return super.getNextPage();
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mBrowseButton) {
            // Choose directory
            DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
            String path = mDir.getText().trim();
            if (path.length() > 0) {
                dialog.setFilterPath(path);
            }
            String file = dialog.open();
            if (file != null) {
                mDir.setText(file);
                refresh();
            }
        } else if (source == mSelectAllButton) {
            mCheckboxTableViewer.setAllChecked(true);
            mValues.importProjects = mProjectPaths;
        } else if (source == mDeselectAllButton) {
            mCheckboxTableViewer.setAllChecked(false);
            mValues.importProjects = Collections.emptyList();
        } else if (source == mRefreshButton || source == mDir) {
            refresh();
        } else if (source == mCopyCheckBox) {
            mValues.copyIntoWorkspace = mCopyCheckBox.getSelection();
        }

        validatePage();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    // ---- KeyListener ----

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getSource() == mDir) {
            if (e.keyCode == SWT.CR) {
                refresh();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    // ---- TraverseListener ----

    @Override
    public void keyTraversed(TraverseEvent e) {
        // Prevent Return from running through the wizard; return is handled by
        // key listener to refresh project list instead
        if (SWT.TRAVERSE_RETURN == e.detail) {
            e.doit = false;
        }
    }

    // ---- Implements IStructuredContentProvider ----

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return mProjectPaths != null ? mProjectPaths.toArray() : new Object[0];
    }

    // ---- Implements ICheckStateListener ----

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
        // Try to disable other elements that conflict with this
        Object[] checked = mCheckboxTableViewer.getCheckedElements();
        List<ImportedProject> selected = new ArrayList<ImportedProject>(checked.length);
        for (Object o : checked) {
            if (!mCheckboxTableViewer.getGrayed(o)) {
                selected.add((ImportedProject) o);
            }
        }
        mValues.importProjects = selected;
        validatePage();
    }

    // ---- Implements ILabelProvider ----

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public Image getImage(Object element) {
        return null;
    }

    @Override
    public String getText(Object element) {
        ImportedProject file = (ImportedProject) element;
        return String.format("%1$s (%2$s)", file.getProjectName(), file.getLocation().getPath());
    }

    // ---- IColorProvider ----

    @Override
    public Color getForeground(Object element) {
        Display display = mTable.getDisplay();
        if (mCheckboxTableViewer.getGrayed(element)) {
            return display.getSystemColor(SWT.COLOR_DARK_GRAY);
        }

        return display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    }

    @Override
    public Color getBackground(Object element) {
        return mTable.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
    }
}
