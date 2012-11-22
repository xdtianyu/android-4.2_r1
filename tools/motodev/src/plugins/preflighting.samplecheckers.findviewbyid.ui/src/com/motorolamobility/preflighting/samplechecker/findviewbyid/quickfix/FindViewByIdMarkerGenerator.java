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
package com.motorolamobility.preflighting.samplechecker.findviewbyid.quickfix;

import static com.motorola.studio.android.common.log.StudioLogger.error;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class FindViewByIdMarkerGenerator implements IMarkerResolutionGenerator2
{

    private static final String MARKER_ID =
            "com.motorolamobility.preflighting.samplechecker.findviewbyid.ui.findViewByIdPermissionsMarker";

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
     */
    public IMarkerResolution2[] getResolutions(IMarker marker)
    {
        List<IMarkerResolution2> resolutions = new ArrayList<IMarkerResolution2>();

        try
        {
            if (marker.getType().equals(MARKER_ID))
            {
                resolutions.add(new FindViewByIdMarkerResolution());
            }
        }
        catch (CoreException e)
        {
            error(getClass(), "Could not get marker type", e); //$NON-NLS-1$
        }
        return resolutions.toArray(new IMarkerResolution2[resolutions.size()]);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
     */
    public boolean hasResolutions(IMarker marker)
    {
        boolean hasResolutions = false;
        try
        {
            if (marker.getType().equals(MARKER_ID))
            {
                hasResolutions = true;
            }
        }
        catch (CoreException e)
        {
            error(getClass(), "Could not get marker type", e); //$NON-NLS-1$
        }

        return hasResolutions;
    }
}
