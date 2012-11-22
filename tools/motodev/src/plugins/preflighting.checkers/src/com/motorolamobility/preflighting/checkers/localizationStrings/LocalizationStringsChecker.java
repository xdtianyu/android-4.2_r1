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

package com.motorolamobility.preflighting.checkers.localizationStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.Element;
import com.motorolamobility.preflighting.core.applicationdata.Element.Type;
import com.motorolamobility.preflighting.core.applicationdata.ElementUtils;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.StringsElement;
import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.checker.IChecker;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ParameterDescription;
import com.motorolamobility.preflighting.core.validation.ParameterType;
import com.motorolamobility.preflighting.core.validation.Value;

/**
 * Localization Strings Checker implementation.
 * This checker will look for missing strings in a given language strings.xml file that exist in other language files.
 *
 */
public class LocalizationStringsChecker extends Checker implements IChecker
{
    // Parameter constants
    public final String PARAMETER_DEFAULT_LOCALE = "defaultLocale"; //$NON-NLS-1$

    private final String PARAMETER_DEFAULT_LOCALE_PATTERN = "[a-z]{2}(_[A-Z]{2})?"; //$NON-NLS-1$

    /**
     * Default locale parameter value
     */
    private String parameterDefaultLocaleValue = null;

    /**
     * Resources folder
     */
    private ResourcesFolderElement resFolder;

    /**
     * The string element for the default locale
     */
    private StringsElement stringsKeysDefault;

    private Locale defaultLocale;

    /**
     * Check if resources and values folder exists, set the default locale and call its conditions canExecute method. 
     * @see com.motorola.preflighting.core.checker.IChecker#canExecute(com.motorola.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public IStatus canExecute(ApplicationData data, List<DeviceSpecification> deviceSpecs)
            throws PreflightingCheckerException
    {
        //status of the checker
        IStatus status = Status.OK_STATUS;

        if (status.isOK())
        {
            status = setResourcesFolder(data);
        }
        if (status.isOK())
        {
            status = valuesFolderExists();
        }
        if (status.isOK())
        {
            status = setDefaultLocale();
        }
        if (status.isOK())
        {
            //the super implementation check its conditions status
            status = super.canExecute(data, deviceSpecs);
        }

        return status;
    }

    /**
     * Sets the default locale.
     * @return IStatus.Error if default locale is empty or does not exist.
     */
    private IStatus setDefaultLocale()
    {
        IStatus status = Status.OK_STATUS;

        defaultLocale = null;
        // Check if we should use the default language or the user specified one as the default
        if (parameterDefaultLocaleValue != null)
        {
            String[] splittedLocale = parameterDefaultLocaleValue.split("_"); //$NON-NLS-1$

            if (splittedLocale.length > 1)
            {
                defaultLocale = new Locale(splittedLocale[0], splittedLocale[1]);
                stringsKeysDefault = resFolder.getValuesElement(defaultLocale);
            }
            else
            {
                defaultLocale = new Locale(splittedLocale[0]);
                stringsKeysDefault = resFolder.getValuesElement(defaultLocale);
            }

            // Check if the locale provided was found. If not, the test cannot execute
            if ((stringsKeysDefault == null) || (stringsKeysDefault.getKeyList().size() < 1))
            {
                // Status
                status =
                        new Status(
                                IStatus.ERROR,
                                CheckerPlugin.PLUGIN_ID,
                                CheckerNLS.LocalizationStringsChecker_detailedLocaleResourceNotFound);

            }

        }
        else
        {
            // Retrieve the default language string and string array keys
            stringsKeysDefault = resFolder.getDefaultValuesElement();
        }
        return status;
    }

    /**
     * @param checkerStatus
     * @return
     */
    private IStatus valuesFolderExists()
    {
        IStatus checkerStatus = Status.OK_STATUS;

        // Check if at least one "values" folder exist with a appropriate "strings.xml" file
        int numberOfFoundValuesResources = 0;
        for (Element e : resFolder.getChildren())
        {
            if (e.getType() == Element.Type.FOLDER_VALUES)
            {
                for (Element children : e.getChildren())
                {
                    if (children.getType() == Element.Type.FILE_STRINGS)
                    {
                        numberOfFoundValuesResources++;
                    }
                }
            }
        }

        if (numberOfFoundValuesResources == 0)
        {
            checkerStatus =
                    new Status(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.LocalizationStringsChecker_Missing_stringsXml_File);
        }
        return checkerStatus;
    }

    /**
     * @param data 
     * @param checkerStatus
     * @return
     */
    private IStatus setResourcesFolder(ApplicationData data)
    {
        IStatus status = Status.OK_STATUS;

        // Check for the existence of a \res folder
        List<Element> folderResElements =
                ElementUtils.getElementByType(data.getRootElement(), Type.FOLDER_RES);

        this.resFolder =
                folderResElements.size() > 0 ? (ResourcesFolderElement) folderResElements.get(0)
                        : null;

        if (resFolder == null)
        {
            status =
                    new Status(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.Missing_res_folder);
        }
        return status;
    }

    /**
     * Check if default locale was changed via parameter and properly sets it.
     * @see com.motorola.preflighting.core.checker.AbstractChecker#validateInputParams(java.util.List)
     */
    @Override
    public IStatus validateInputParams(List<Parameter> parameters)
    {
        IStatus status = Status.OK_STATUS;
        parameterDefaultLocaleValue = null;

        // Iterate through the list of parameters and check for problems
        if (parameters.size() > 0)
        {

            for (Parameter p : parameters)
            {

                if (p.getParameterType().equals(PARAMETER_DEFAULT_LOCALE))
                {
                    // Check if parameter is valid
                    if (p.getValue().matches(PARAMETER_DEFAULT_LOCALE_PATTERN))
                    {
                        parameterDefaultLocaleValue = p.getValue();
                    }
                    else
                    {
                        status =
                                new Status(
                                        Status.ERROR,
                                        CheckerPlugin.PLUGIN_ID,
                                        CheckerNLS.LocalizationStringsChecker_invalidDefaultLocaleParameter);

                        break;
                    }
                }
                else
                {
                    // Invalid parameters
                    status =
                            new Status(Status.ERROR, CheckerPlugin.PLUGIN_ID, CheckerNLS.bind(
                                    CheckerNLS.LocalizationStringsChecker_invalidParameters,
                                    p.getParameterType()));

                    break;
                }
            }
        }

        return status;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.AbstractChecker#getParams()
     */
    @Override
    public List<ParameterDescription> getParameterDescriptions()
    {
        // Return this cheker parameters

        List<ParameterDescription> returnList = new ArrayList<ParameterDescription>();

        /*
         * "defaultLocale" parameter
         */
        Value valueLocale = new Value();
        valueLocale.setValue("ll_CC"); //$NON-NLS-1$
        valueLocale
                .setDescription(CheckerNLS.LocalizationStringsChecker_helpParameterLocaleValueFormatDescription);

        List<Value> allowedValuesLocale = new ArrayList<Value>();
        allowedValuesLocale.add(valueLocale);

        ParameterDescription descriptionLocale = new ParameterDescription();
        descriptionLocale.setName(PARAMETER_DEFAULT_LOCALE);
        descriptionLocale.setType(ParameterType.STRING);
        descriptionLocale.setDefaultValue(valueLocale);
        descriptionLocale.setAllowedValues(allowedValuesLocale);
        descriptionLocale.setValueRequired(true);
        descriptionLocale
                .setDescription(CheckerNLS.LocalizationStringsChecker_helpParameterLocaleDescription);
        descriptionLocale
                .setValueDescription(CheckerNLS.LocalizationStringsChecker_helpParameterLocaleValueDescription);

        returnList.add(descriptionLocale);

        return returnList;
    }

    @Override
    public void clean()
    {

        resFolder = null;
        if (stringsKeysDefault != null)
        {
            stringsKeysDefault.clean();
        }
        stringsKeysDefault = null;
        defaultLocale = null;
    }

    /**
     * @return the resFolder
     */
    public ResourcesFolderElement getResourcesFolder()
    {
        return resFolder;
    }

    /**
     * @return the stringsKeysDefault
     */
    public StringsElement getStringsKeysDefault()
    {
        return stringsKeysDefault;
    }

    /**
     * @return the defaultLocale
     */
    public Locale getDefaultLocale()
    {
        return defaultLocale;
    }
}
