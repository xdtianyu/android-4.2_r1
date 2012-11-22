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
import java.net.URL;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import com.motorolamobility.preflighting.ui.PreflightingUIPlugin;
import com.motorolamobility.preflighting.ui.handlers.AnalyzeApkHandler;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

/**
 * This Wizard selects the packages to validate
 */
public class ApkValidationWizard extends Wizard
{
    private ApkValidationWizardPage page = null;

    private final ExecutionEvent event;

    public ApkValidationWizard(IStructuredSelection selection, ExecutionEvent event)
    {
        setWindowTitle(PreflightingUiNLS.ApkValidationWizard_wizardTitle);
        setNeedsProgressMonitor(true);
        setHelpAvailable(false);
        this.page = new ApkValidationWizardPage("apkWizardPage", selection); //$NON-NLS-1$
        this.event = event;
        
        Bundle bundle = PreflightingUIPlugin.getDefault().getBundle();
        URL url =
                bundle.getEntry((new StringBuilder("/")).append( //$NON-NLS-1$
                        "icons" + IPath.SEPARATOR + "MOTODEVAppValidator_64x64.png") //$NON-NLS-1$ //$NON-NLS-2$
                        .toString());
        
        setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
        		PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID, url.getPath()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages()
    {
        addPage(this.page);
    }

    /**
     * Finishes this wizard, validating the selected packages
     */
    @Override
    public boolean performFinish()
    {
        List<File> selectedFiles = ApkValidationWizard.this.page.getSelectedPackages();

        StructuredSelection selection = new StructuredSelection(selectedFiles);
        AnalyzeApkHandler apkHandler = new AnalyzeApkHandler(selection);
        try
        {
            apkHandler.execute(event);
        }
        catch (ExecutionException e)
        {
            //do nothing
        }
        
        return true;
    }

}
