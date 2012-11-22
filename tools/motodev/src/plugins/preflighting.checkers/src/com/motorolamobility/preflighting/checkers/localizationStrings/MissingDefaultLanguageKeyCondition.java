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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.ResourcesFolderElement;
import com.motorolamobility.preflighting.core.applicationdata.StringsElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Check if keys present in other languages and missing in the default language.
 */
public class MissingDefaultLanguageKeyCondition extends Condition implements ICondition
{
    private ValidationManagerConfiguration valManagerConfig;

    private LocalizationStringsChecker checker;

    private ResourcesFolderElement resFolder;

    /**
     * The string element for the default locale
     */
    private StringsElement stringsKeysDefault;

    private Locale defaultLocale;

    /**
     * Check if there are default and alternate locales.
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status;

        this.checker = (LocalizationStringsChecker) getChecker();
        this.resFolder = checker.getResourcesFolder();
        this.stringsKeysDefault = checker.getStringsKeysDefault();

        List<Locale> availableLocales = resFolder.getAvailableLocales();

        //check if default or non-default locales exist
        if ((stringsKeysDefault != null) && (stringsKeysDefault.getKeyList() != null)
                && (availableLocales != null) && (availableLocales.size() > 0))
        {
            //there is both default and non-default locales
            status = new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
            status.setConditionId(getId());
        }
        else if (((stringsKeysDefault == null) || (stringsKeysDefault.getKeyList() == null))
                && (availableLocales != null) && (availableLocales.size() > 0))
        {
            //there is no default locale set 
            status =
                    new CanExecuteConditionStatus(
                            IStatus.ERROR,
                            CheckerPlugin.PLUGIN_ID,
                            CheckerNLS
                                    .bind(CheckerNLS.LocalizationStringsChecker_conditionMissingKey_CouldNotBeRun_NoDefault,
                                            getId()));
            status.setConditionId(getId());
        }
        else if (((stringsKeysDefault != null) && (stringsKeysDefault.getKeyList() != null) && (stringsKeysDefault
                .getKeyList().size() > 0))
                && ((availableLocales == null) || (availableLocales.size() < 1)))
        {
            //there is no non-default locales
            status =
                    new CanExecuteConditionStatus(
                            IStatus.ERROR,
                            CheckerPlugin.PLUGIN_ID,
                            CheckerNLS
                                    .bind(CheckerNLS.LocalizationStringsChecker_conditionMissingKey_CouldNotBeRun_NoLocale,
                                            getId()));
            status.setConditionId(getId());
        }
        else
        {
            //unknown error
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.bind(CheckerNLS.LocalizationStringsChecker_UnknownError,
                                    getId()));
            status.setConditionId(getId());
        }

        return status;
    }

    /**
     * Check if alternate locales define keys that are not defined in the default locale.
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        this.valManagerConfig = valManagerConfig;
        this.defaultLocale = checker.getDefaultLocale();

        /*
         * Check for problems related to the default localization files 
         */

        // Check for missing keys
        for (Locale locale : resFolder.getAvailableLocales())
        {
            if (!locale.equals(defaultLocale))
            {
                // Check for keys present in the locales that are missing in the default language
                checkForMissingDefaultKeys(locale, results);
            }
        }

    }

    /**
     * Auxiliary method to check for missing locale keys in the default localization resource
     * @param locale - The locale to be validated.
     * @return A validation result
     * @throws PreflightingCheckerException 
     */
    private void checkForMissingDefaultKeys(Locale locale, ValidationResult results)
            throws PreflightingCheckerException
    {

        try
        {
            StringsElement localeStringsElement = resFolder.getValuesElement(locale);

            if (localeStringsElement != null)
            {
                // Builders to construct the description and quickfix
                String resultDescription;

                for (String s : localeStringsElement.getKeyList())
                {
                    if (!stringsKeysDefault.containsKey(s))
                    {
                        ValidationResultData result = new ValidationResultData();

                        result.setSeverity(getSeverityLevel());
                        result.setConditionID(getId());

                        String localeDisplayName = null;
                        if ((locale.getCountry() != null) && (locale.getCountry().length() > 0))
                        {
                            localeDisplayName = locale.getLanguage() + "_" + locale.getCountry(); //$NON-NLS-1$
                        }
                        else
                        {
                            localeDisplayName = locale.getLanguage();
                        }

                        // Construct description                   
                        resultDescription =
                                CheckerNLS
                                        .bind(CheckerNLS.LocalizationStringsChecker_localeStringNotFoundSimple,
                                                s, localeDisplayName);

                        result.setIssueDescription(resultDescription.toString());

                        //Associate the res folder to the issue
                        result.addFileToIssueLines(resFolder.getFile(),
                                Collections.<Integer> emptyList());

                        // Set fix suggestion  
                        result.setQuickFixSuggestion(CheckerNLS.LocalizationStringsChecker_addStringToLocalizationResourceDetailed);
                        result.setInfoURL(ConditionUtils.getDescriptionLink(checker.getId(),
                                getId(), valManagerConfig));

                        //Add the result to the list
                        results.addValidationResult(result);
                    }
                }

            }

        }
        catch (Exception e)
        {
            throw new PreflightingCheckerException(
                    CheckerNLS.LocalizationStringsChecker_Exception_MissingLocaleKeys, e);
        }
    }
}
