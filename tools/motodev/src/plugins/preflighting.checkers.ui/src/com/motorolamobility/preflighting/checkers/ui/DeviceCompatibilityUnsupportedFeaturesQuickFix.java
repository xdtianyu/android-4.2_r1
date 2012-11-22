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
import com.motorola.studio.android.model.manifest.dom.UsesFeatureNode;
import com.motorolamobility.preflighting.checkers.ui.i18n.CheckersUiNLS;

/**
 * This class implements the fix for device compatibility - unsupported features condition.
 * The fix consists in adding &lt;uses-feature android:name="featureName" android:required="false"/&gt; to AndroidManifest.xml. 
 */
public class DeviceCompatibilityUnsupportedFeaturesQuickFix implements IMarkerResolution2
{

    private static final String FALSE = "false"; //$NON-NLS-1$    

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    public String getLabel()
    {
        return CheckersUiNLS.DeviceCompatibilityUnsupportedFeaturesQuickFix_Label;
    }

    /**
     * Adds android:required="false" for the feature AndroidManifest.xml.
     * 
     * @param marker contains the feature that must be made NOT REQUIRED in AndroidManifest.xml.   
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

            //get the feature that must be made NOT REQUIRED, generated from app validator              
            List<Object> attributes =
                    (List<Object>) marker.getAttribute(QuickFixGenerator.QUICK_FIX_ID);

            for (Object featureId : attributes)
            {
                UsesFeatureNode usesFeatureNode =
                        manifestNode.getUsesFeatureNode((String) featureId);
                if (usesFeatureNode == null)
                {
                    //uses-feature element does not exist - add the element uses feature node
                    usesFeatureNode = new UsesFeatureNode((String) featureId);
                    manifestNode.addUsesFeatureNode(usesFeatureNode);
                }
                //in both cases (uses-feature element exists or not) - add/update the required attribute
                usesFeatureNode.setRequired(Boolean.parseBoolean(FALSE));
            }

            //save the project with the new/updated uses-feature
            AndroidProjectManifestFile.saveToProject(project, manifestFile, true);

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
        return CheckersUiNLS.DeviceCompatibilityUnsupportedFeaturesQuickFix_Description;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getImage()
     */
    public Image getImage()
    {
        return null;
    }

}