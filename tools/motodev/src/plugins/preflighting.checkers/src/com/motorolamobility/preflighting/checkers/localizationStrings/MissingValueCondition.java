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
 * Check if there are keys with empty values.
 *
 */
public class MissingValueCondition extends Condition implements ICondition
{
    private LocalizationStringsChecker checker;

    private ValidationManagerConfiguration valManagerConfig;

    private ResourcesFolderElement resFolder;

    private Locale defaultLocale;

    /**
     * The string element for the default locale
     */
    StringsElement stringsKeysDefault;

    /**
     * Check if there is at least one string key defined in default or alternate locales.
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

        //check if there is at least one key in default or non-default locales 
        if ((stringsKeysDefault != null) && (stringsKeysDefault.getKeyList() != null)
                && (stringsKeysDefault.getKeyList().size() > 0))
        {
            //at least one string key found at stringsKeyDefault 
            status = new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
            status.setConditionId(getId());
        }
        else if ((availableLocales != null) && (availableLocales.size() > 0))
        {
            //default locale has no string key defined
            //look for string keys in non-default languages
            boolean nonDefaultLanguageStringKeyFound = false;

            for (Locale locale : availableLocales)
            {
                if (resFolder.getValuesElement(locale).getKeyList().size() > 0)
                {
                    nonDefaultLanguageStringKeyFound = true;
                    break;
                }
            }

            if (nonDefaultLanguageStringKeyFound)
            {
                //at least one string key found in non default languages (availableLocales) 
                status = new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
                status.setConditionId(getId());
            }
            else
            {
                //both default and non-default localization files are empty
                status =
                        new CanExecuteConditionStatus(
                                IStatus.ERROR,
                                CheckerPlugin.PLUGIN_ID,
                                CheckerNLS
                                        .bind(CheckerNLS.LocalizationStringsChecker_conditionMissingValue_CouldNotBeRun_EmptyLocalizationFiles,
                                                getId()));
                status.setConditionId(getId());
            }
        }
        else
        {
            //default localization file is empty and there is no non-default locale set
            status =
                    new CanExecuteConditionStatus(
                            IStatus.ERROR,
                            CheckerPlugin.PLUGIN_ID,
                            CheckerNLS
                                    .bind(CheckerNLS.LocalizationStringsChecker_conditionMissingValue_CouldNotBeRun_EmptyLocalizationFiles,
                                            getId()));
            status.setConditionId(getId());
        }

        return status;
    }

    /**
     * Check if there are keys with empty values in default and alternate locales.  
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        this.valManagerConfig = valManagerConfig;
        this.defaultLocale = checker.getDefaultLocale();

        // Check for keys with empty values - First the default localization and then the locales
        checkForEmptyValues(null, results);

        /*
         * Check for problems related to the non-default localization files
         */
        for (Locale l : resFolder.getAvailableLocales())
        {
            if (!l.equals(defaultLocale))
            {
                // Check for keys with empty values
                checkForEmptyValues(l, results);
            }
        }
    }

    /**
     * Auxiliary method to check for keys with empty values.
     * @param locale - The locale to be validated. If null, the method will validate the default localization resource
     * @param results 
     * @return A validation result
     * @throws PreflightingCheckerException 
     */
    private void checkForEmptyValues(Locale locale, ValidationResult results)
            throws PreflightingCheckerException
    {
        // Strings element to be validated. Can either be from the default localization resource or a locale one.
        StringsElement localeStringsElement;

        // Construct a list of missing keys
        List<String> keysWithEmptyValues = new ArrayList<String>();

        if (locale != null)
        {
            localeStringsElement = resFolder.getValuesElement(locale);
        }
        else
        {
            // Validate default localization resource
            localeStringsElement = stringsKeysDefault;
        }

        try
        {
            if (localeStringsElement != null)
            {
                // Find keys with empty values
                keysWithEmptyValues = findKeysWithMissingValues(localeStringsElement);

                ValidationResultData result;
                for (String key : keysWithEmptyValues)
                {
                    result = new ValidationResultData();
                    // Builders to construct the description and quickfix
                    String resultDescription;

                    // Create a result and return it
                    result.setSeverity(getSeverityLevel());

                    result.setConditionID(getId());

                    //Associate the result to the resFolder
                    result.addFileToIssueLines(resFolder.getFile(),
                            Collections.<Integer> emptyList());

                    // The result is different depending if we are validating the default localization files or not
                    if (locale != null)
                    {
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
                                        .bind(CheckerNLS.LocalizationStringsChecker_localeStringEmptyValue,
                                                key, localeDisplayName);

                        result.setIssueDescription(resultDescription.toString());
                    }
                    else
                    {
                        //Set description 
                        result.setIssueDescription(CheckerNLS.bind(
                                CheckerNLS.LocalizationStringsChecker_defaultStringEmptyValue, key));
                    }

                    // Set quickfix  
                    result.setQuickFixSuggestion(CheckerNLS.LocalizationStringsChecker_stringEmptyValueQuickFix);
                    result.setInfoURL(ConditionUtils.getDescriptionLink(checker.getId(), getId(),
                            valManagerConfig));

                    // Add result to the result list
                    results.addValidationResult(result);
                }
            }

        }
        catch (Exception e)
        {
            String exceptionMessage;
            if (locale == null)
            {
                exceptionMessage =
                        CheckerNLS.LocalizationStringsChecker_Exception_EmptyValuesDefault;
            }
            else
            {
                exceptionMessage =
                        CheckerNLS.LocalizationStringsChecker_Exception_EmptyValuesLocale;
            }
            throw new PreflightingCheckerException(exceptionMessage, e);
        }
    }

    /**
     * Auxiliary method to look for empty values in a StringsElement
     * @param localeStringsElement - A strings element representing a localization resource.
     * @return A list containing keys with empty values.
     */
    private List<String> findKeysWithMissingValues(StringsElement localeStringsElement)
    {

        List<String> missingValues = new ArrayList<String>();

        if (localeStringsElement != null)
        {
            for (String key : localeStringsElement.getKeyList())
            {
                // Check value associated with the key
                if (localeStringsElement.getValue(key) != null)
                {
                    Object value = localeStringsElement.getValue(key);

                    // The value can either be a String or a List<String>.
                    if (value instanceof String)
                    {
                        if (((String) value).length() < 1)
                        {
                            // Empty value found!
                            missingValues.add(key);
                        }
                    }
                    else if (value instanceof List<?>)
                    {
                        if (((List<?>) value).size() < 1)
                        {
                            // Empty value found!
                            missingValues.add(key);
                        }
                    }
                }
                else
                {
                    missingValues.add(key);
                }

            }
        }

        return missingValues;
    }
}
