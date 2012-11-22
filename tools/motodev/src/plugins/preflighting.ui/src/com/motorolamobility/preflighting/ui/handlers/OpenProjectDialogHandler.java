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

package com.motorolamobility.preflighting.ui.handlers;

import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.osgi.framework.Bundle;

import com.motorola.studio.android.common.log.StudioLogger;
import com.motorolamobility.preflighting.ui.PreflightingUIPlugin;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

public class OpenProjectDialogHandler extends AbstractHandler
{

    /*
     * Opens dialog to choose a project
     */
    private IProject[] openProjectSelectionDialog()
    {
        IProject[] selectedProjects = null;

        // get shell
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        // crate package dialog
        final ElementTreeSelectionDialog packageDialog =
                new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(),
                        new WorkbenchContentProvider());

        // set title and message
        packageDialog.setTitle(PreflightingUiNLS.OpenProjectDialogHandler_dialogTitle);
        packageDialog.setMessage(PreflightingUiNLS.OpenProjectDialogHandler_dialogDescription);

        Bundle bundle = PreflightingUIPlugin.getDefault().getBundle();
        URL url = bundle.getEntry((new StringBuilder("/")).append( //$NON-NLS-1$
                "icons" + IPath.SEPARATOR + "MOTODEVAppValidator_16x16.png") //$NON-NLS-1$ //$NON-NLS-2$
                .toString());

        packageDialog.setImage(AbstractUIPlugin.imageDescriptorFromPlugin(
                PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID, url.getPath()).createImage());

        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        for (int j = 0; j < projects.length; j++)
        {
            try
            {
                if (projects[j].getNature("com.android.ide.eclipse.adt.AndroidNature") != null
                        && projects[j].isOpen())
                {
                    packageDialog.setInitialSelection(projects[j]);
                    break;
                }
            }
            catch (CoreException e)
            {
                StudioLogger.error("There was a problem with the project: ", e.getMessage());
            }
        }

        // set workspace as root
        packageDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        // set comparator
        packageDialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

        //filter extensions
        packageDialog.addFilter(new ViewerFilter()
        {

            /*
             * (non-Javadoc)
             * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
             */
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                boolean returnValue = false;
                if (element instanceof IProject)
                {
                    IProject proj = (IProject) element;
                    try
                    {
                        if (proj.getNature("com.android.ide.eclipse.adt.AndroidNature") != null) //$NON-NLS-1$
                        {
                            returnValue = true;
                        }
                    }
                    catch (Exception e)
                    {
                        //object is skipped
                    }
                }

                // the element must be a project
                return returnValue;
            }
        });

        packageDialog.setValidator(new ISelectionStatusValidator()
        {
            /*
             * (non-Javadoc)
             * @see org.eclipse.ui.dialogs.ISelectionStatusValidator#validate(java.lang.Object[])
             */
            public IStatus validate(Object[] selection)
            {
                // by default the status is OK
                IStatus valid = null;
                if (selection.length == 0)
                {
                    valid =
                            new Status(IStatus.ERROR,
                                    PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID,
                                    PreflightingUiNLS.OpenProjectDialogHandler_OneProjectSelected);
                }
                else
                {
                    valid =
                            new Status(IStatus.OK, PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID,
                                    ""); //$NON-NLS-1$
                }
                // return validation
                return valid;
            }
        });
        // open dialog
        if (packageDialog.open() == IDialogConstants.OK_ID)
        {
            // get the result
            Object[] resources = packageDialog.getResult();
            selectedProjects = new IProject[resources.length];

            for (int i = 0; i < resources.length; i++)
            {
                selectedProjects[i] = (IProject) resources[i];
            }
        }
        // return the selected projects
        return selectedProjects;
    }

    public Object execute(final ExecutionEvent event) throws ExecutionException
    {
        
        final IProject[] selectedProjects = openProjectSelectionDialog();
        if (selectedProjects != null)
        {
            StructuredSelection selection = new StructuredSelection(selectedProjects);
            AnalyzeApkHandler apkHandler = new AnalyzeApkHandler(selection);
            try
            {
                apkHandler.execute(event);
            }
            catch (ExecutionException e)
            {
                //do nothing
            }
        }
        
        return null;
    }
}
