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

package com.motorolamobility.preflighting.core.source.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains a list of required and optional permissions that the application needs (or should declare)
 * based on the methods invoked inside the application. These permissions are
 * the ones declared in the AndroidManifest.xml in the tag &lt;uses-permission>.
 */
public class PermissionGroups
{
    /**
     * The permission must be declared
     */
    private final List<String> requiredPermissions = new ArrayList<String>();

    /**
     * At least one of this permissions must be declared
     */
    private final List<String> optionalPermissions = new ArrayList<String>();

    /**
     * Get the {@link List} of required permissions. These are the ones in the
     * AndroidManifest.xml represented within the tag &lt;uses-permission>.
     * 
     * @return Returns the list of required permissions.
     */
    public List<String> getRequiredPermissions()
    {
        return requiredPermissions;
    }

    /**
     * Get the list of optional permissions. These are the ones in the
     * AndroidManifest.xml represented within the tag &lt;uses-permission>.
     * 
     * @return Returns the list of optional permissions.
     */
    public List<String> getOptionalPermissions()
    {
        return optionalPermissions;
    }

    /**
     * This implementation provides a human-readable text of this
     * {@link PermissionGroups}.
     * 
     * @return Returns a human-readable text of this {@link PermissionGroups}.
     * 
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "PermissionGroups [requiredPermissions=" + requiredPermissions
                + ", optionalPermissions=" + optionalPermissions + "]";
    }
}
