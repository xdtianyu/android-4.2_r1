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
package com.motorolamobility.preflighting.core.internal.permissionfeature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.core.permissionfeature.Feature;
import com.motorolamobility.preflighting.core.permissionfeature.Permission;

/**
 * Container class to keep permissions to implied required feature mapping
 */
public final class PermissionToFeatureMapping
{
    /**
     * Category name to list of permission IDs
     */
    private final Map<String, List<Permission>> categoryNameToPermissions =
            new HashMap<String, List<Permission>>();

    /**
     * Permission id to list of implied required features
     */
    private final Map<String, List<Feature>> permissionIdToImpliedRequiredFeatures =
            new HashMap<String, List<Feature>>();

    /**
     * 
     * @param categoryName
     * @return list of permissions for the given category, null if not found
     */
    public List<Permission> getPermissionsForCategory(String categoryName)
    {
        return categoryNameToPermissions.get(categoryName);
    }

    /**
     * 
     * @param permissionId
     * @return list of required features for the given permission, null if not
     *         found
     */
    public List<Feature> getRequiredImpliedFeaturesForPermission(String permissionId)
    {
        return permissionIdToImpliedRequiredFeatures.get(permissionId);
    }

    public List<Permission> putPermissions(String key, List<Permission> value)
    {
        return categoryNameToPermissions.put(key, value);
    }

    public List<Feature> putFeatures(String key, List<Feature> value)
    {
        return permissionIdToImpliedRequiredFeatures.put(key, value);
    }

}
