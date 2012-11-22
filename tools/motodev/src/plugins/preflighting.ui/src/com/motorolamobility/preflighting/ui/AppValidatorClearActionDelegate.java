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

package com.motorolamobility.preflighting.ui;

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.ui.handlers.AnalyzeApkHandler;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

public class AppValidatorClearActionDelegate implements IViewActionDelegate
{
    public void run(IAction action)
    {
        //do nothing
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        //do nothing
    }

    public void init(IViewPart view)
    {
        IActionBars actionBar = view.getViewSite().getActionBars();
        IToolBarManager toolBar = actionBar.getToolBarManager();
        //defines action 
        Action action = new Action()
        {
            @Override
            public void run()
            {
                try
                {
                    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                    //clear app validator markers for every opened project and its subfiles
                    for (int i = 0; projects != null && i < projects.length; i++)
                    {
                        if (projects[i].isOpen())
                        {
                            projects[i].deleteMarkers(AnalyzeApkHandler.DEFAULT_APP_VALIDATOR_MARKER_TYPE,
                                    true, IResource.DEPTH_INFINITE);
                        }
                    }
                }
                catch (CoreException e)
                {
                    PreflightingLogger.error("Error removing markers from projects.");
                }

                super.run();
            }
        };
        //set icon
        URL imageUrl =
                PreflightingUIPlugin.getDefault().getBundle()
                        .getEntry("icons/MOTODEVAppValidator_16x16_clear.png");
        ImageDescriptor imageDesc = ImageDescriptor.createFromURL(imageUrl);
        action.setImageDescriptor(imageDesc);
        action.setEnabled(true);
        action.setToolTipText(PreflightingUiNLS.ProblemsView_ClearAppValidatorMarkers);
        //add to tool bar and update it
        toolBar.add(action);

        toolBar.add(new Separator());
        actionBar.updateActionBars();
    }
}
