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
package com.motorolamobility.preflighting.core.verbose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;
import com.motorolamobility.preflighting.core.utils.LimitedList;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;

/**
 * Abstract class responsible for defining warning levels, setting the current warning level,
 * and filtering results and returning appropriate messages based on current warning
 * level set.
 * The warning level will be passed as a parameter to the application, if not,
 * {@link WarningLevelFilter#DEFAULT_WARNING_LEVEL} will be assumed (default warning level).
 */
public abstract class WarningLevelFilter
{
    /**
     * The terminator string for the total message.
     */
    private static final String TOTAL_MESSAGE_TERMINATOR = "."; //$NON-NLS-1$

    /**
     * The level separator string for the total message.
     */
    private static final String TOTAL_MESSAGE_LEVEL_SEPARATOR = ", "; //$NON-NLS-1$

    /**
     * Enumeration representing the various warning levels available to the application.    
     */
    public enum WarningLevel
    {
        /**
         * Suppress all messages, just return success or failure.
         */
        w0,
        /**
         * Only indicate extremely severe conditions that could cause immediate failure of
         * the application on launch (fatal errors).
         */
        w1,
        /**
         * Indicate improper conditions that will lead to failure or possible problems on
         * different handsets (fatal errors + errors). This is the default warning level.
         */
        w2,
        /**
         * Indicate all improper conditions (fatal errors + errors + warnings).
         */
        w3,
        /**
         * Indicate all improper conditions (fatal errors + errors + warnings) and their
         * potential fixes, if known.
         */
        w4;
    }

    /**
     * The default warning level ({@link WarningLevel#w2}).
     */
    public static WarningLevel DEFAULT_WARNING_LEVEL = WarningLevel.w2;

    /**
     * The current warning level being used.
     */
    private static WarningLevel currentWarningLevel = DEFAULT_WARNING_LEVEL;

    /**
     * Retrieve the current warning level for the application.
     * 
     * @return Current warning level.
     */
    public static WarningLevel getCurrentWarningLevel()
    {
        return currentWarningLevel;
    }

    /**
     * Set the current warning level to the given one. If <code>null</code> is
     * passed, the default warning level is used ({@link WarningLevelFilter#DEFAULT_WARNING_LEVEL}).
     * 
     * @param warningLevel Warning level to be set.
     */
    public static void setCurrentWarningLevel(WarningLevel warningLevel)
    {
        if (warningLevel != null)
        {
            currentWarningLevel = warningLevel;
        }
        else
        {
            currentWarningLevel = DEFAULT_WARNING_LEVEL;
        }
    }

    /**
     * Return whether quick fix suggestions should be printed
     * (warning level equals {@link WarningLevel#w4}) or not.
     * 
     * @return <code>true</code> if quick fix suggestions should be printed,
     * <code>false</code> otherwise.
     */
    public static boolean printQuickFixSuggestions()
    {
        return currentWarningLevel.equals(WarningLevel.w4);
    }

    /**
     * Return whether the severities should be printed or not
     * (severities are not printed if warning level equals {@link WarningLevel#w0}).
     * 
     * @return <code>true</code> if severities should be printed,
     * <code>false</code> otherwise.
     */
    public static boolean printSeverity()
    {
        return !currentWarningLevel.equals(WarningLevel.w0);
    }

    /**
     * Filters the given list of validation results so that the returning list
     * of validation results contain only the appropriate issues, according to
     * warning level set.
     * 
     * @param validationResultList The list of validation results found by the application
     * 
     * @return The filtered validation results list, according to warning level set
     */
    public static List<ValidationResult> filterValidationResultsForCurrentWarningLevel(
            List<ValidationResult> validationResultList)
    {
        if (validationResultList == null)
        {
            throw new IllegalArgumentException("List<ValidationResult> cannot be null"); //$NON-NLS-1$
        }

        List<ValidationResult> filteredValidationResult = new ArrayList<ValidationResult>();

        if (validationResultList.size() > 0)
        {

            DebugVerboseOutputter.printVerboseMessage(
                    PreflightingCoreNLS.WarningLevelFilter_VerboseMessage_FilterningResult,
                    VerboseLevel.v2);

            if (currentWarningLevel.equals(WarningLevel.w0))
            {
                boolean fatalErrorsDetected = false, potentialErrorsDetected = false;

                for (ValidationResult result : validationResultList)
                {
                    for (ValidationResultData resultData : result.getValidationResult())
                    {
                        if (resultData.getSeverity().equals(SEVERITY.FATAL))
                        {
                            fatalErrorsDetected = true;
                            break;
                        }
                        else if (resultData.getSeverity().equals(SEVERITY.ERROR))
                        {
                            potentialErrorsDetected = true;
                        }
                    }
                }

                ValidationResult result = null;
                if (fatalErrorsDetected)
                {
                    result =
                            createValidationResultObject(
                                    PreflightingCoreNLS.WarningLevelFilter_FatalErrorsMessage,
                                    SEVERITY.FATAL);
                }
                else if (potentialErrorsDetected)
                {
                    result =
                            createValidationResultObject(
                                    PreflightingCoreNLS.WarningLevelFilter_ErrorsMessage,
                                    SEVERITY.ERROR);
                }
                else
                {
                    result =
                            createValidationResultObject(
                                    PreflightingCoreNLS.WarningLevelFilter_NoProblemsMessage,
                                    SEVERITY.OK);
                }
                filteredValidationResult.add(result);
            }
            else if (currentWarningLevel.equals(WarningLevel.w1)) // fatal errors only
            {
                boolean fatalErrorsDetected =
                        filterValidationResultForSeverity(validationResultList,
                                filteredValidationResult, SEVERITY.FATAL);
                if (!fatalErrorsDetected)
                {
                    ValidationResult result =
                            createValidationResultObject(
                                    PreflightingCoreNLS.WarningLevelFilter_NoFatalErrorsMessage,
                                    SEVERITY.OK);
                    filteredValidationResult.add(result);
                }
            }
            else if (currentWarningLevel.equals(WarningLevel.w2)) // fatal errors and errors only
            {
                boolean errorsOrFatalErrorsDetected =
                        filterValidationResultForSeverity(validationResultList,
                                filteredValidationResult, SEVERITY.ERROR);
                if (!errorsOrFatalErrorsDetected)
                {
                    ValidationResult result =
                            createValidationResultObject(
                                    PreflightingCoreNLS.WarningLevelFilter_NoFatalNorErrorsMessage,
                                    SEVERITY.OK);
                    filteredValidationResult.add(result);
                }
            }
            else if (currentWarningLevel.compareTo(WarningLevel.w3) >= 0) // all levels
            {
                boolean warningsErrorsOrFatalErrorsDetected =
                        filterValidationResultForSeverity(validationResultList,
                                filteredValidationResult, SEVERITY.WARNING);
                if (!warningsErrorsOrFatalErrorsDetected)
                {
                    ValidationResult result =
                            createValidationResultObject(
                                    PreflightingCoreNLS.WarningLevelFilter_NoFatalErrorsNorWarningsMessage,
                                    SEVERITY.OK);
                    filteredValidationResult.add(result);
                }
            }

            DebugVerboseOutputter.printVerboseMessage(
                    PreflightingCoreNLS.WarningLevelFilter_VerboseMessage_ResultFiltered,
                    VerboseLevel.v2);
        }

        return filteredValidationResult;
    }

    private static ValidationResult createValidationResultObject(String message, SEVERITY severity)
    {
        ValidationResult result = new ValidationResult(null, LimitedList.UNLIMITED);
        ValidationResultData resultData = new ValidationResultData();
        result.addValidationResult(resultData);
        resultData.setSeverity(severity);
        resultData.setIssueDescription(message);
        return result;
    }

    /**
     * Filter the validation results of a particular checker, given the limit severity
     * (which is defined according to warning level set). The result is added to the filtered
     * list also passed.
     * Returns a flag indicating if any issues were found of at least the given severity.
     * 
     * @param validationResultList The original list of validation results
     * @param filteredValidationResult The filtered list; entries are added to it by this method
     * @param limitSeverity The limit severity for issues entering the filtered list or not
     * 
     * @return <code>true</code> if any issues were found with at least the severity passed
     * (or more severe), <code>false</code> otherwise
     */
    private static boolean filterValidationResultForSeverity(
            List<ValidationResult> validationResultList,
            List<ValidationResult> filteredValidationResult, SEVERITY limitSeverity)
    {
        boolean expectedLevelDetected = false;

        for (ValidationResult result : validationResultList)
        {
            ValidationResult filteredResult = null;
            for (ValidationResultData resultData : result.getValidationResult())
            {
                if (resultData.getSeverity().compareTo(limitSeverity) <= 0)
                {
                    if (filteredResult == null)
                    {
                        filteredResult =
                                new ValidationResult(result.getCheckerId(), LimitedList.UNLIMITED);
                    }
                    filteredResult.addValidationResult(resultData);
                    expectedLevelDetected = true;
                }
            }
            if (filteredResult != null)
            {
                filteredValidationResult.add(filteredResult);
            }
        }

        return expectedLevelDetected;
    }

    /**
     * Retrieves the total message summing up fatal errors, errors and warnings, depending
     * on current warning level.
     * 
     * @param validationResultList The list of validation results already filtered (as it
     * was outputted to used)
     * 
     * @return The total message string, or <code>null</code> if no message applies
     */
    public static String getValidationResultTotalMessage(List<ValidationResult> validationResultList)
    {
        String totalMessage = null;
        if (validationResultList != null)
        {
            if (currentWarningLevel.compareTo(WarningLevel.w0) > 0)
            {
                int fatalErrorsCount = 0, errorsCount = 0, warningsCount = 0;

                for (ValidationResult result : validationResultList)
                {
                    for (ValidationResultData resultData : result.getValidationResult())
                    {
                        if (resultData.getSeverity().equals(SEVERITY.FATAL))
                        {
                            fatalErrorsCount++;
                        }
                        else if (resultData.getSeverity().equals(SEVERITY.ERROR))
                        {
                            errorsCount++;
                        }
                        else if (resultData.getSeverity().equals(SEVERITY.WARNING))
                        {
                            warningsCount++;
                        }
                    }
                }

                StringBuilder stringBuilder = new StringBuilder();

                // (some code repetition was used to avoid unnecessary repeated testing that would
                // slow performance)
                if (currentWarningLevel.compareTo(WarningLevel.w3) >= 0)
                {
                    if ((fatalErrorsCount > 0) || (errorsCount > 0) || (warningsCount > 0))
                    {
                        stringBuilder.append(PreflightingCoreNLS.WarningLevelFilter_TotalMessage);
                        stringBuilder.append(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.WarningLevelFilter_FatalErrorsCountMessage,
                                fatalErrorsCount)
                                + TOTAL_MESSAGE_LEVEL_SEPARATOR);
                        stringBuilder.append(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.WarningLevelFilter_ErrorsCountMessage,
                                errorsCount)
                                + TOTAL_MESSAGE_LEVEL_SEPARATOR);
                        stringBuilder.append(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.WarningLevelFilter_WarningsCountMessage,
                                warningsCount)
                                + TOTAL_MESSAGE_TERMINATOR);
                    }
                }
                else if (currentWarningLevel.equals(WarningLevel.w2))
                {
                    if ((fatalErrorsCount > 0) || (errorsCount > 0))
                    {
                        stringBuilder.append(PreflightingCoreNLS.WarningLevelFilter_TotalMessage);
                        stringBuilder.append(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.WarningLevelFilter_FatalErrorsCountMessage,
                                fatalErrorsCount)
                                + TOTAL_MESSAGE_LEVEL_SEPARATOR);
                        stringBuilder.append(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.WarningLevelFilter_ErrorsCountMessage,
                                errorsCount)
                                + TOTAL_MESSAGE_TERMINATOR);
                    }
                }
                else if (currentWarningLevel.equals(WarningLevel.w1))
                {
                    if (fatalErrorsCount > 0)
                    {
                        stringBuilder.append(PreflightingCoreNLS.WarningLevelFilter_TotalMessage);
                        stringBuilder.append(PreflightingCoreNLS.bind(
                                PreflightingCoreNLS.WarningLevelFilter_FatalErrorsCountMessage,
                                fatalErrorsCount)
                                + TOTAL_MESSAGE_TERMINATOR);
                    }
                }

                if (stringBuilder.length() > 0)
                {
                    totalMessage = stringBuilder.toString();
                }
            }
        }
        return totalMessage;
    }

    /**
     * Adjusts the warning level on the given validation result list.
     * The level can be increased or decreased.
     * Only the validation results from checkers or conditions passed on the are adjusted.
     * 
     * @param validationResultList The list of validation results
     * @param raiseWarningLevels Whether the warning level should be increased (<code>true</code>
     * is passed) or decreased (<code>false</code> is passed)
     * @param checkerIdsToAdjustWarningLevel The list of checkers whose validation results should
     * have the warning level adjusted
     * @param conditionIdsMap specific checkers conditions to adjust, instead of a whole checker. 
     * @param excludedConditionsIds 
     */
    public static void adjustWarningLevels(List<ValidationResult> validationResultList,
            boolean raiseWarningLevels, List<String> checkerIdsToAdjustWarningLevel,
            Map<String, List<String>> conditionIdsMap, Map<String, List<String>> exclusionMap)
    {
        if (validationResultList == null)
        {
            throw new IllegalArgumentException("List<ValidationResult> cannot be null"); //$NON-NLS-1$
        }
        if (checkerIdsToAdjustWarningLevel == null)
        {
            checkerIdsToAdjustWarningLevel = Collections.emptyList();
        }
        if (conditionIdsMap == null)
        {
            conditionIdsMap = new HashMap<String, List<String>>(0);
        }

        SEVERITY[] severities = SEVERITY.values();
        for (ValidationResult result : validationResultList)
        {
            String checkerId = result.getCheckerId();
            if (checkerId != null)
            {
                //Verify if there's anything to be changed
                if (checkerIdsToAdjustWarningLevel.contains(checkerId)
                        || (conditionIdsMap.containsKey(checkerId)))
                {
                    List<String> conditionsToAdjust = conditionIdsMap.get(checkerId);

                    //If something has to be changed, process the whole set, applying the changes. 
                    for (ValidationResultData resultData : result.getValidationResult())
                    {
                        boolean mustAdjust = true;
                        //User specified only some conditions
                        if (!checkerIdsToAdjustWarningLevel.contains(checkerId)
                                && (conditionsToAdjust != null))
                        {
                            mustAdjust = conditionsToAdjust.contains(resultData.getConditionID());
                        }
                        else if (exclusionMap != null)
                        {
                            List<String> conditionsToIgnore = exclusionMap.get(checkerId);
                            if (conditionsToIgnore != null)
                            {
                                mustAdjust =
                                        !conditionsToIgnore.contains(resultData.getConditionID());
                            }
                        }
                        if (mustAdjust)
                        {
                            SEVERITY resultSeverity = resultData.getSeverity();
                            if (raiseWarningLevels)
                            {
                                // last level is not raised, and above warning is not raised as well
                                if ((SEVERITY.FATAL.compareTo(resultSeverity) < 0)
                                        && !SEVERITY.OK.equals(resultSeverity))
                                {
                                    resultData
                                            .setSeverity(severities[resultSeverity.ordinal() - 1]);
                                }
                            }
                            else
                            {
                                // level is decreased for warning at least (nothing goes deeper than that)
                                if (SEVERITY.WARNING.compareTo(resultSeverity) > 0)
                                {
                                    resultData
                                            .setSeverity(severities[resultSeverity.ordinal() + 1]);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
