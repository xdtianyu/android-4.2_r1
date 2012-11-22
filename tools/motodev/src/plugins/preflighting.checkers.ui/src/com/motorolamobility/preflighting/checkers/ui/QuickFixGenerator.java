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
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;

/**
 * This class is responsible for checking which markers has quick fixes, and in the cases that a quick fix
 * is available, it returns the class that implements the fix.
 * */

public class QuickFixGenerator implements IMarkerResolutionGenerator2
{
    /**
     * AppValidator quick fix identifier.
     * */
    public static final String QUICK_FIX_ID = "QuickFix";

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
     */
    public IMarkerResolution2[] getResolutions(IMarker marker)
    {
        //list of possible resolutions for the given marker
        List<IMarkerResolution2> resolutions = new ArrayList<IMarkerResolution2>();

        try
        {
            //check the marker type to associate the proper marker resolution class(es).
            if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.missingPermissionsMarker"))
            {
                resolutions.add(new MissingPermissionsQuickFix());
            }
            else if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.unneededPermissionsMarker"))
            {
                resolutions.add(new UnneededPermissionsQuickFix());
            }
            else if (marker
                    .getType()
                    .equals("com.motorolamobility.preflighting.checkers.ui.deviceCompatibilityUnsupportedFeaturesMarker"))
            {
                resolutions.add(new DeviceCompatibilityUnsupportedFeaturesQuickFix());
            }
            else if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.googlePlayFiltersMissingMinSDK"))
            {
                resolutions.add(new MissingMinSdkQuickFix());
            }
            else if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.googlePlayFiltersUneededMaxSDK"))
            {
                resolutions.add(new UneededMaxSdkQuickFix());
            }
        }
        catch (CoreException e)
        {
            PreflightingLogger.info(QuickFixGenerator.class,
                    "Problem retrieving marker resolutions: " + e.getMessage());
        }

        return resolutions.toArray(new IMarkerResolution2[resolutions.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
     */
    public boolean hasResolutions(IMarker marker)
    {
        try
        {
            //check if the marker type has at least one resolution available.
            if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.missingPermissionsMarker"))
            {
                return true;
            }
            else if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.unneededPermissionsMarker"))
            {
                return true;
            }
            else if (marker
                    .getType()
                    .equals("com.motorolamobility.preflighting.checkers.ui.deviceCompatibilityUnsupportedFeaturesMarker"))
            {
                return true;
            }
            else if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.googlePlayFiltersMissingMinSDK"))
            {
                return true;
            }
            else if (marker.getType().equals(
                    "com.motorolamobility.preflighting.checkers.ui.googlePlayFiltersUneededMaxSDK"))
            {
                return true;
            }
        }
        catch (CoreException e)
        {
            PreflightingLogger.info(QuickFixGenerator.class,
                    "Problem checking if marker has resolutions: " + e.getMessage());
        }

        //no resolution found for this marker
        return false;
    }

}