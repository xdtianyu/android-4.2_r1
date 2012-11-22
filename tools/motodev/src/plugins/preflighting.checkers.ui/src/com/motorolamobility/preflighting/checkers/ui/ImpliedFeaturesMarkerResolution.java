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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.motorola.studio.android.common.exception.AndroidException;
import com.motorola.studio.android.common.utilities.EclipseUtils;
import com.motorola.studio.android.manifest.AndroidProjectManifestFile;
import com.motorola.studio.android.model.manifest.AndroidManifestFile;
import com.motorola.studio.android.model.manifest.dom.ManifestNode;
import com.motorola.studio.android.model.manifest.dom.UsesFeatureNode;
import com.motorolamobility.preflighting.checkers.ui.i18n.CheckersUiNLS;

/**
 * MarkerResolution responsible for adding the implied feature by permission as uses-feature manifest node.
 */
public class ImpliedFeaturesMarkerResolution implements IMarkerResolution2
{

    public String getLabel()
    {
        return CheckersUiNLS.ImpliedFeaturesMarkerResolution_Label;
    }

    @SuppressWarnings("unchecked")
    public void run(IMarker marker)
    {
        IResource resource = marker.getResource();
        AndroidManifestFile manifestFile;
        try
        {
            IProject project = resource.getProject();
            manifestFile = AndroidProjectManifestFile.getFromProject(project);
            ManifestNode manifestNode = manifestFile.getManifestNode();
            
            List<Object> attributes =
                    (List<Object>) marker.getAttribute(QuickFixGenerator.QUICK_FIX_ID);
            for(Object attribute : attributes)
            {
                List<String> impliedFeatures = (List<String>) attribute;
                for(String impliedFeature : impliedFeatures)
                {
                    UsesFeatureNode usesFeatureNode = manifestNode.getUsesFeatureNode(impliedFeature); 
                    if(usesFeatureNode == null) //User has not yet added it!
                    {
                        usesFeatureNode = new UsesFeatureNode(impliedFeature);
                        manifestNode.addUsesFeatureNode(usesFeatureNode);
                    }
                }
            }
            AndroidProjectManifestFile.saveToProject(project, manifestFile, true);
            
            marker.delete();
        }
        catch (AndroidException e)
        {
            EclipseUtils.showErrorDialog(CheckersUiNLS.ImpliedFeaturesMarkerResolution_Fail_Msg_Dlg_Title, CheckersUiNLS.ImpliedFeaturesMarkerResolution_Fail_Msg_Save_Manifest);
        }
        catch (CoreException e)
        {
            EclipseUtils.showErrorDialog(CheckersUiNLS.ImpliedFeaturesMarkerResolution_Fail_Msg_Dlg_Title, CheckersUiNLS.ImpliedFeaturesMarkerResolution_Fail_Msg_Manipulate_Manifest);
        }

    }

    public Image getImage()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getDescription()
     */
    public String getDescription()
    {
        return null; //There's no additional description.
    }

}
