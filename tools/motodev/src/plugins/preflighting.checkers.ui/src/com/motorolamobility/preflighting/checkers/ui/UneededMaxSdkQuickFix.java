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
package com.motorolamobility.preflighting.checkers.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.motorola.studio.android.common.utilities.EclipseUtils;
import com.motorola.studio.android.manifest.AndroidProjectManifestFile;
import com.motorola.studio.android.model.manifest.AndroidManifestFile;
import com.motorola.studio.android.model.manifest.dom.ManifestNode;
import com.motorola.studio.android.model.manifest.dom.UsesSDKNode;
import com.motorolamobility.preflighting.checkers.ui.i18n.CheckersUiNLS;

/**
 * Removes unneeded maxSdkVersion from AndroidManifest.xml.
 */
public class UneededMaxSdkQuickFix implements IMarkerResolution2
{

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    public String getLabel()
    {
        return CheckersUiNLS.UneededMaxSdkQuickFix_Label;
    }

    /**
     * Removes maxSdkVersion from AndroidManifest.xml.
     * 
     * @param marker   
     *  
     * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
     */
    public void run(IMarker marker)
    {
        try
        {
            //get the AndroidManifest file
            IProject project = marker.getResource().getProject();
            AndroidManifestFile manifestFile = AndroidProjectManifestFile.getFromProject(project);
            ManifestNode manifestNode = manifestFile.getManifestNode();

            UsesSDKNode usesSDKNode = manifestNode.getUsesSdkNode();
            if (usesSDKNode != null)
            {
                if (usesSDKNode.getPropMaxSdkVersion() != null)
                {
                    usesSDKNode.setPropMaxSdkVersion(null);

                    //save the project with the new permissions
                    AndroidProjectManifestFile.saveToProject(project, manifestFile, true);
                }
            }
            marker.delete();
        }
        catch (Exception e)
        {
            EclipseUtils.showErrorDialog(CheckersUiNLS.QuickFix_MarkerResolutionFailed,
                    CheckersUiNLS.QuickFix_CouldNotFixTheProblem + e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getDescription()
     */
    public String getDescription()
    {
        return CheckersUiNLS.UneededMaxSdkQuickFix_Description;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getImage()
     */
    public Image getImage()
    {
        return null;
    }

}
