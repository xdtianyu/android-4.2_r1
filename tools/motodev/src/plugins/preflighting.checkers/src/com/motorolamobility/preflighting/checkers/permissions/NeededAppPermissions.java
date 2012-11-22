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
package com.motorolamobility.preflighting.checkers.permissions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.PermissionGroups;

/**
 * This class is intended to retrieve and store required permissions based on sourceFolderElements.
 * This will be used by PermissionsChecker conditions.
 */
class NeededAppPermissions
{
    private Map<Invoke, List<String>> requiredPermissionsMap;

    private Map<Invoke, List<String>> optionalPermissionsMap;

    public NeededAppPermissions(PlatformRules platformRules,
            List<SourceFolderElement> sourceFolderElements)
    {
        requiredPermissionsMap = new HashMap<Invoke, List<String>>(200);
        optionalPermissionsMap = new HashMap<Invoke, List<String>>(200);

        mountPermissionsMap(platformRules, sourceFolderElements);
    }

    private void mountPermissionsMap(PlatformRules platformRules,
            List<SourceFolderElement> sourceFolderElements)
    {
        for (SourceFolderElement sourceFolderElement : sourceFolderElements)
        {
            List<Invoke> invokedMethods = sourceFolderElement.getInvokedMethods();
            if (invokedMethods != null)
            {
                for (Invoke invoked : invokedMethods)
                {
                    String signature = invoked.getQualifiedName();
                    PermissionGroups permissionsForMethod =
                            platformRules.getPermissionsForMethod(signature);

                    if (permissionsForMethod != null)
                    {
                        List<String> requiredPermissions =
                                permissionsForMethod.getRequiredPermissions();
                        List<String> optionalPermissions =
                                permissionsForMethod.getOptionalPermissions();

                        requiredPermissionsMap.put(invoked, requiredPermissions);
                        optionalPermissionsMap.put(invoked, optionalPermissions);
                    }
                }
            }
        }
    }

    /**
     * @return the requiredPermissionsMap
     */
    public Map<Invoke, List<String>> getRequiredPermissionsMap()
    {
        return requiredPermissionsMap;
    }

    /**
     * @return the optionalPermissionsMap
     */
    public Map<Invoke, List<String>> getOptionalPermissionsMap()
    {
        return optionalPermissionsMap;
    }

}
