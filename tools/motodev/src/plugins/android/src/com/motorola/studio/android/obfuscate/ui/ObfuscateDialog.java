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

package com.motorola.studio.android.obfuscate.ui;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.motorola.studio.android.AndroidPlugin;
import com.motorola.studio.android.i18n.AndroidNLS;
import com.motorola.studio.android.model.AndroidProject;
import com.motorola.studio.android.obfuscate.ObfuscatorManager;

public class ObfuscateDialog extends TitleAreaDialog
{

    private final String OBFUSCATION_DIALOG_HELP = AndroidPlugin.PLUGIN_ID + ".obfuscation-dialog";

    private CheckboxTreeViewer treeViewer;

    private Object[] selectedProjects;

    public ObfuscateDialog(Shell parentShell)
    {
        super(parentShell);

        setTitleImage(AndroidPlugin.imageDescriptorFromPlugin(AndroidPlugin.PLUGIN_ID,
                "icons/wizban/obfuscate.gif").createImage()); //$NON-NLS-1$            
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setSize(330, 300);
        newShell.setText(AndroidNLS.ObfuscateProjectsHandler_1);
    }

    /**
     * Center the dialog.
     */
    @Override
    protected void initializeBounds()
    {
        super.initializeBounds();
        Shell shell = this.getShell();
        Monitor primary = shell.getMonitor();
        Rectangle bounds = primary.getBounds();
        Rectangle rect = shell.getBounds();
        int x = bounds.x + ((bounds.width - rect.width) / 2);
        int y = bounds.y + ((bounds.height - rect.height) / 2);
        shell.setLocation(x, y);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        super.createDialogArea(parent).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout layout = new GridLayout(1, false);
        mainComposite.setLayout(layout);

        treeViewer = new CheckboxTreeViewer(mainComposite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
        treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Set content and label provider
        treeViewer.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                String label = null;
                if (element instanceof IProject)
                {
                    label = ((IProject) element).getName();
                }
                return label;
            }
        });
        treeViewer.setContentProvider(new TreeViewerContentProvider());

        ArrayList<IProject> projectsList = generateInputForTree();
        treeViewer.setInput(projectsList);

        for (IProject p : projectsList)
        {
            treeViewer.setChecked(p, ObfuscatorManager.isProguardSet(p));
        }

        treeViewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                selectedProjects = treeViewer.getCheckedElements();
            }
        });
        treeViewer.expandAll();
        mainComposite.layout(true);

        setTitle(AndroidNLS.ObfuscateProjectsHandler_2);
        setMessage(AndroidNLS.ObfuscateProjectsHandler_3, IMessageProvider.NONE);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, OBFUSCATION_DIALOG_HELP);

        return mainComposite;
    }

    private ArrayList<IProject> generateInputForTree()
    {
        ArrayList<IProject> androidProjects = new ArrayList<IProject>();
        IProject[] allProjectsList = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        for (int i = 0; i < allProjectsList.length; i++)
        {
            IProject currentProject = allProjectsList[i];
            try
            {
                if ((currentProject.getNature(AndroidProject.ANDROID_NATURE) != null)
                        && currentProject.isOpen())
                {
                    androidProjects.add(currentProject);
                }
            }
            catch (CoreException e)
            {
                // do nothing
            }
        }
        return androidProjects;
    }

    public ArrayList<IProject> getSelectedProjects()
    {
        ArrayList<IProject> list = new ArrayList<IProject>();
        if (selectedProjects != null)
        {
            for (int i = 0; i < selectedProjects.length; i++)
            {
                list.add((IProject) selectedProjects[i]);
            }
        }
        return list;
    }
}

class TreeViewerContentProvider implements ITreeContentProvider
{
    public void dispose()
    {
        // do nothing
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        // do nothing
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement)
    {
        Object[] elem = null;
        if (inputElement instanceof ArrayList)
        {
            elem = new Object[((ArrayList) inputElement).size()];
            elem = ((ArrayList) inputElement).toArray();
        }

        return elem;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement)
    {
        //no children
        return new Object[0];
    }

    public Object getParent(Object element)
    {
        return null;
    }

    public boolean hasChildren(Object element)
    {
        return false;
    }
}
