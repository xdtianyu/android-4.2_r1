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

package com.motorolamobility.preflighting.checkers.missingdrawable;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.ElementUtils;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.XMLElement;
import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;

/**
 * Missing Drawable Checker class implementation 
 */
public class MissingDrawableChecker extends Checker
{
    /**
     * Information about the drawable folders into application
     */
    private MissingDrawableData missingDrawableData;

    /*
     * Output related constants 
     */
    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.IChecker#validateApplication(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public void validateApplication(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        this.missingDrawableData = new MissingDrawableData(data);
        //run enabled conditions which belongs to the checker 
        super.validateApplication(data, deviceSpecs, valManagerConfig, results);
    }

    /**
     * @return the missingDrawableData
     */
    protected MissingDrawableData getMissingDrawableData()
    {
        return missingDrawableData;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.IChecker#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public IStatus canExecute(ApplicationData data, List<DeviceSpecification> deviceSpecs)
            throws PreflightingCheckerException
    {

        IStatus status = Status.OK_STATUS;

        //collect status from child condition
        status = super.canExecute(data, deviceSpecs);

        if (status.isOK())
        {
            // Check for a non-null document
            XMLElement manElem = data.getManifestElement();

            if (manElem == null)
            {
                status =
                        new Status(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.Invalid_ManifestFile);
            }
            else
            {
                Document manifestDoc = manElem.getDocument();
                if (manifestDoc == null)
                {
                    status =
                            new Status(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                    CheckerNLS.Invalid_ManifestFile);
                }

            }

            // Check for the existence of a \res folder
            List<Element> folderResElements =
                    ElementUtils.getElementByType(data.getRootElement(), Type.FOLDER_RES);

            ResourcesFolderElement resFolder =
                    folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements
                            .get(0) : null;

            if (resFolder == null)
            {
                status =
                        new Status(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.Missing_res_folder);
            }
        }

        return status;
    }
}
