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

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.motorola.studio.android.common.utilities.EclipseUtils;
import com.motorola.studio.android.manifest.AndroidProjectManifestFile;
import com.motorola.studio.android.model.manifest.AndroidManifestFile;
import com.motorola.studio.android.model.manifest.dom.ManifestNode;
import com.motorola.studio.android.model.manifest.dom.UsesPermissionNode;
import com.motorolamobility.preflighting.checkers.ui.i18n.CheckersUiNLS;

/**
 * This class implements the fix for missing permissions condition.
 * The fix consists in adding all missing permissions to AndroidManifest.xml. 
 */
public class MissingPermissionsQuickFix implements IMarkerResolution2
{

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    public String getLabel()
    {
        return CheckersUiNLS.MissingPermissionsQuickFix_Label;
    }

    /**
     * Adds all missing permissions to AndroidManifest.xml.
     * 
     * @param marker contains the list of missing permissions that must be added to AndroidManifest.xml.   
     *  
     * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
     */
    @SuppressWarnings("unchecked")
    public void run(IMarker marker)
    {
        try
        {
            //get the AndroidManifest file
            IProject project = marker.getResource().getProject();
            AndroidManifestFile manifestFile = AndroidProjectManifestFile.getFromProject(project);
            ManifestNode manifestNode = manifestFile.getManifestNode();

            //get the list of missing permissions, generated from app validator  
            List<Object> attributes =
                    (List<Object>) marker.getAttribute(QuickFixGenerator.QUICK_FIX_ID);

            //iterate over the list of missing permissions, adding them to AndroidManifest file
            for (Object missingPermission : attributes)
            {
                manifestNode.addUsesPermissionNode(new UsesPermissionNode(
                        (String) missingPermission));
            }

            //save the project with the new permissions
            AndroidProjectManifestFile.saveToProject(project, manifestFile, true);

            marker.delete();
        }
        catch (Exception e)
        {
            EclipseUtils.showErrorDialog("Marker resolution fail.",
                    "Missing permissions quick fix could not fix the problem: " + e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getDescription()
     */
    public String getDescription()
    {
        return CheckersUiNLS.MissingPermissionsQuickFix_Description;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getImage()
     */
    public Image getImage()
    {
        return null;
    }

}