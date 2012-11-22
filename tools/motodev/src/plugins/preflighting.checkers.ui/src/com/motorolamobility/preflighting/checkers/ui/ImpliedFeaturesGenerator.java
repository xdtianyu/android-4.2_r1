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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import com.motorola.studio.android.common.log.StudioLogger;

public class ImpliedFeaturesGenerator implements IMarkerResolutionGenerator2
{

    public IMarkerResolution[] getResolutions(IMarker marker)
    {
        List<IMarkerResolution2> resolutions = new ArrayList<IMarkerResolution2>();

        try
        {
            if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.impliedFeaturesMarker"))
            {
                resolutions.add(new ImpliedFeaturesMarkerResolution());
            }
        }
        catch (CoreException e) {
            StudioLogger.error("Requested marker does not exist: ", e.getMessage());
        }
        return resolutions.toArray(new IMarkerResolution2[resolutions.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
     */
    public boolean hasResolutions(IMarker marker)
    {
        boolean hasResolutions = false;
        try
        {
            if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.impliedFeaturesMarker"))
            {
                hasResolutions = true;
            }
        }
        catch (CoreException e)
        {
            StudioLogger.error("Requested marker does not exist: ", e.getMessage());
        }

        return hasResolutions;
    }

}
