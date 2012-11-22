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

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.devicespecification.internal.PlatformRules;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;

public class PermissionsChecker extends Checker
{
    NeededAppPermissions neededAppPermissions;

    /*
     * This checker won't run the java model is not preset or incomplete.
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.Checker#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public IStatus canExecute(ApplicationData data, List<DeviceSpecification> deviceSpecs)
            throws PreflightingCheckerException
    {
        IStatus status = CheckerUtils.isJavaModelComplete(data, null);

        if (status.isOK())
        {
            status = super.canExecute(data, deviceSpecs);
        }

        return status;
    }

    @Override
    public void validateApplication(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        List<SourceFolderElement> sourceFolderElements = data.getJavaModel();
        neededAppPermissions =
                new NeededAppPermissions(PlatformRules.getInstance(), sourceFolderElements);
        super.validateApplication(data, deviceSpecs, valManagerConfig, results);
    }

    /**
     * @return the neededAppPermissions
     */
    public NeededAppPermissions getNeededAppPermissions()
    {
        return neededAppPermissions;
    }
}
