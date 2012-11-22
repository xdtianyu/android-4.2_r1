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
package com.motorolamobility.preflighting.core.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;

/**
 * Corresponds to one validation issue found by a checker/condition.
 */
public class ValidationResultData
{
    /**
     * Severity level of the result.
     */
    public enum SEVERITY
    {
        /**
         * The FATAL level is used for severe error events. In case of FATAL, the
         * application could be aborted.
         */
        FATAL
        {
            @Override
            public String toString()
            {
                return PreflightingCoreNLS.ValidationResultData_FatalSeverityMessage;
            }
        },
        /**
         * The ERROR level is used by errors events. Less severe than FATAL, used
         * for situations of error that will not crash the application.
         */
        ERROR
        {
            @Override
            public String toString()
            {
                return PreflightingCoreNLS.ValidationResultData_ErrorSeverityMessage;
            }
        },
        /**
         * The WARNING level is used for potentially harmful situations. Used for
         * situations that can generate an error.
         */
        WARNING
        {
            @Override
            public String toString()
            {
                return PreflightingCoreNLS.ValidationResultData_WarningSeverityMessage;
            }
        },
        /**
         * The OK level is used if no problem was identified by this checker / condition.
         */
        OK
        {
            @Override
            public String toString()
            {
                return PreflightingCoreNLS.ValidationResultData_OkSeverityMessage;
            }
        };

    }

    private Map<File, List<Integer>> fileToIssueLines;

    // initialize to avoid invalid value (null)
    private SEVERITY severity = SEVERITY.OK;

    private String issueDescription;

    private String quickFixSuggestion;

    /**
     * The condition associated with this result data.
     */
    private String conditionID;

    /**
     * The preview associated with this data (like the line with problems)
     */
    private String preview = null;

    /**
     * Link with more information about validation/checker
     */
    private String infoURL = null;

    private String markerType;

    private final List<Object> extra;

    /**
     * Default constructor.
     */
    public ValidationResultData()
    {
        fileToIssueLines = new HashMap<File, List<Integer>>();
        extra = new ArrayList<Object>();
    }

    /**
     * Constructor that fills the most used items when an issue is raised.
     * @param fileToIssueLines List of files and the lines where the problem occurred
     * @param severity {@link SEVERITY} of the issue.
     * @param issueDescription Description of the issue.
     * @param quickFixSuggestion Quick fix suggested.
     * @param conditionID {@link Condition#getId()} that identified the issue.
     */
    public ValidationResultData(Map<File, List<Integer>> fileToIssueLines, SEVERITY severity,
            String issueDescription, String quickFixSuggestion, String conditionID)
    {
        this.fileToIssueLines =
                fileToIssueLines != null ? fileToIssueLines : new HashMap<File, List<Integer>>();
        this.severity = severity;
        this.issueDescription = issueDescription;
        this.quickFixSuggestion = quickFixSuggestion;
        this.conditionID = conditionID;
        this.extra = new ArrayList<Object>();
    }

    /**
     * Constructor that fills the most used items when an issue is raised.
     * @param fileToIssueLines List of files and the lines where the problem occurred
     * @param severity {@link SEVERITY} of the issue.
     * @param issueDescription Description of the issue.
     * @param quickFixSuggestion Quick fix suggested.
     * @param conditionID {@link Condition#getId()} that identified the issue.
     * @param markerType the type of the problem that will be reported by App Validator.
     */
    public ValidationResultData(Map<File, List<Integer>> fileToIssueLines, SEVERITY severity,
            String issueDescription, String quickFixSuggestion, String conditionID,
            String markerType)
    {
        this.fileToIssueLines =
                fileToIssueLines != null ? fileToIssueLines : new HashMap<File, List<Integer>>();
        this.severity = severity;
        this.issueDescription = issueDescription;
        this.quickFixSuggestion = quickFixSuggestion;
        this.conditionID = conditionID;
        this.markerType = markerType;
        this.extra = new ArrayList<Object>();
    }

    /**
     * Returns a {@link Map} containing the {@link File} and the corresponding lines with issues of this
     * {@link ValidationResultData}.
     * @return a {@link Map} with the {@link File} and its issue lines.
     */
    public Map<File, List<Integer>> getFileToIssueLines()
    {
        return fileToIssueLines;
    }

    /**
     * Adds to a File, usually the problematic one, to the issue lines of the {@link ValidationResultData}.
     * @param file the file descriptor.
     * @param lines the lines in which the issue appears.    */

    public void addFileToIssueLines(File file, List<Integer> lines)
    {
        if (fileToIssueLines == null)
        {
            fileToIssueLines = new HashMap<File, List<Integer>>();
        }
        fileToIssueLines.put(file, lines);
    }

    /**
     * Returns the issue {@link SEVERITY}. 
     * @return the issue {@link SEVERITY}. 
     */
    public SEVERITY getSeverity()
    {
        return severity;
    }

    /**
     * Sets the {@link ValidationResultData} issue severity.
     * @param severity a {@link SEVERITY}. 
     */
    public void setSeverity(SEVERITY severity)
    {
        // protect from invalid value (null)
        if (severity != null)
        {
            this.severity = severity;
        }
        else
        {
            this.severity = SEVERITY.OK;
        }
    }

    /**
     * Returns the issue description for this {@link ValidationResultData}.
     * @return the issue description.
     */
    public String getIssueDescription()
    {
        return issueDescription;
    }

    /**
     * Sets the issue description for this {@link ValidationResultData}.
     * @param issueDescription the issue description.
     */
    public void setIssueDescription(String issueDescription)
    {
        this.issueDescription = issueDescription;
    }

    /**
     * Return the quick fix suggestion for this issue, or <code>null</code>
     * if there's no suggestion.
     * 
     * @return The quick fix suggestion.
     */
    public String getQuickFixSuggestion()
    {
        return quickFixSuggestion;
    }

    /**
     * Set the quick fix suggestion for this issue. If an empty string, or <code>null</code>
     * is passed, the quick fix suggestion is set to <code>null</code>, indicating
     * there is no suggestion for the issue.
     * 
     * @param quickFixSuggestion The quick fix suggestion for the issue.
     */
    public void setQuickFixSuggestion(String quickFixSuggestion)
    {
        if ((quickFixSuggestion == null) || (quickFixSuggestion.length() == 0))
        {
            this.quickFixSuggestion = null;
        }
        else
        {
            this.quickFixSuggestion = quickFixSuggestion;
        }
    }

    /**
     * Returns the condition ID from this {@link ValidationResultData}.
     * @return The condition ID.
     */
    public String getConditionID()
    {
        return conditionID;
    }

    /**
     * Sets the Condition Id of this {@link ValidationResultData}.
     * @param conditionID The condition ID to set.
     */
    public void setConditionID(String conditionID)
    {
        this.conditionID = conditionID;
    }

    /**
     * Get line preview of validation result.
     * @return The preview text or null if no preview is available.
     */
    public String getPreview()
    {
        return preview;
    }

    /**
     * Set the validation preview that will be seen by user.
     * @param preview A preview text.
     */
    public void setPreview(String preview)
    {
        this.preview = preview;
    }

    /**
     * Get infoURL with more information about error.
     * @return The infoURL.
     */
    public String getInfoURL()
    {
        return infoURL;
    }

    /**
     * Set infoURL with more information.
     * @param infoURL the url with the info
     */
    public void setInfoURL(String url)
    {
        this.infoURL = url;
    }

    /**
     * @return the markerType
     */
    public String getMarkerType()
    {
        return markerType;
    }

    /**
     * @param markerType the markerType to set
     */
    public void setMarkerType(String markerType)
    {
        this.markerType = markerType;
    }

    /**
     * @return the list of extra values.
     */
    public List<Object> getExtra()
    {
        return this.extra;
    }

    /**
     * Appends {@code value} to the list of extras. 
     */
    public void appendExtra(Object value)
    {
        extra.add(value);
    }
}