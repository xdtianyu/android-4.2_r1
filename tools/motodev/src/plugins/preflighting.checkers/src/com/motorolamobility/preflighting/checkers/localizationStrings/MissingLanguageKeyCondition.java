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
 * Check if keys present in the default language are missing in non-default languages.
 *
 */
public class MissingLanguageKeyCondition extends Condition implements ICondition
{
    private LocalizationStringsChecker checker;

    private ValidationManagerConfiguration valManagerConfig;

    private ResourcesFolderElement resFolder;

    private Locale defaultLocale;

    /**
     * The string element for the default locale
     */
    StringsElement stringsKeysDefault;

    /* (non-Javadoc)
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
            //unknow error
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            CheckerNLS.bind(CheckerNLS.LocalizationStringsChecker_UnknownError,
                                    getId()));
            status.setConditionId(getId());
        }

        return status;
    }

    /**
     * Check if there are keys defined in the default locale that are not defined in alternate locales.
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
         * Check for problems related to the non-default localization files
         */
        for (Locale l : resFolder.getAvailableLocales())
        {
            if (!l.equals(defaultLocale))
            {
                // Check for keys present in the default language that are missing from locales
                checkForMissingLocaleKeys(l, results);
            }
        }
    }

    /**
     * Auxiliary method to check for missing default keys for a given locale
     * @param locale - The locale to be validated.
     * @param results 
     * @return A validation result.
     * @throws PreflightingCheckerException 
     */
    private void checkForMissingLocaleKeys(Locale locale, ValidationResult results)
            throws PreflightingCheckerException
    {

        try
        {
            StringsElement localeStringsElement = resFolder.getValuesElement(locale);

            if (localeStringsElement != null)
            {
                // Construct a list of missing keys
                List<String> missingKeys = new ArrayList<String>();

                for (String s : stringsKeysDefault.getKeyList())
                {
                    if (!localeStringsElement.containsKey(s))
                    {
                        missingKeys.add(s);
                    }
                }

                // If there are missing keys, issue an warning listing them all
                if (missingKeys.size() > 0)
                {
                    ValidationResultData result;

                    for (String key : missingKeys)
                    {
                        result = new ValidationResultData();
                        result.setSeverity(getSeverityLevel());

                        result.setConditionID(getId());

                        //Associate the result to the res folder
                        result.addFileToIssueLines(resFolder.getFile(),
                                Collections.<Integer> emptyList());

                        String localeDisplayName = null;
                        if ((locale.getCountry() != null) && (locale.getCountry().length() > 0))
                        {
                            localeDisplayName = locale.getLanguage() + "_" + locale.getCountry(); //$NON-NLS-1$
                        }
                        else
                        {
                            localeDisplayName = locale.getLanguage();
                        }

                        // Set description
                        result.setIssueDescription(CheckerNLS.bind(
                                CheckerNLS.LocalizationStringsChecker_defaultStringNotFoundSimple,
                                key, localeDisplayName));

                        // Set quickfix
                        result.setQuickFixSuggestion(CheckerNLS.LocalizationStringsChecker_addStringToLocalizationResourceDetailed);
                        result.setInfoURL(ConditionUtils.getDescriptionLink(checker.getId(),
                                getId(), valManagerConfig));
                        //Add to result list
                        results.addValidationResult(result);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new PreflightingCheckerException(
                    CheckerNLS.LocalizationStringsChecker_Exception_MissingDefaultKeys, e);
        }
    }
}
