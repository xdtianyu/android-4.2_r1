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
package com.motorolamobility.preflighting.internal.commandoutput;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.output.AbstractOutputter;

public class CSVOutputter extends AbstractOutputter
{
    private static final int MAX_PREVIEW_LENGTH = 120;

    /**
     * Line separator
     */
    private final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * CSV separator
     */
    private final String DELIMITER = ",";

    /**
     * Quote string
     */
    private final String QUOTES = "\"";

    /**
     * Backslash string
     */
    private final String BACKSLASH = "\\";

    /**
     * I/O Exception message logging.
     */
    private final String IO_ERROR = PreflightingNLS.TextOutputter_IOExceptionMessage;

    /**
     * Exception stating that it was not possible to print the results.
     */
    private final String PRINT_ERROR = PreflightingNLS.TextOutputter_PrintResultsErrorMessage;

    /**
     * Writer used for output
     */
    private BufferedWriter writer = null;

    private enum CSV_FIELDS
    {
        type
        {
            @Override
            public String toString()
            {
                return "type";
            }
        },

        checker
        {
            @Override
            public String toString()
            {
                return "checker_id";
            }
        },

        condition
        {
            @Override
            public String toString()
            {
                return "condition";
            }
        },

        description
        {
            @Override
            public String toString()
            {
                return "description";
            }
        },

        file
        {
            @Override
            public String toString()
            {
                return "file";
            }
        },

        line
        {
            @Override
            public String toString()
            {
                return "line";
            }
        },

        preview
        {
            @Override
            public String toString()
            {
                return "preview";
            }
        },

        suggestion
        {
            @Override
            public String toString()
            {
                return "suggestion";
            }
        }
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.commandoutput.IOutputter#print(java.util.Map, java.io.OutputStream, java.util.List)
     */
    @Override
    public void print(ApplicationValidationResult results, OutputStream stream,
            List<Parameter> parameters) throws PreflightingToolException
    {
        initializeParams(parameters);

        // Use a BufferedWriter to write the results
        writer = new BufferedWriter(new OutputStreamWriter(stream));
        try
        {
            if (results.getResults().size() > 0)
            {
                printCSVHeader();
                for (ValidationResult checker : results.getResults())
                {
                    printCheckerResultCSV(checker);
                }
            }

            /*
                TextOutputter.printExecutionReport(results.getExecutionStatus(), errorStream);
            */
        }
        catch (IOException e)
        {
            PreflightingLogger.error(getClass(), IO_ERROR + e.getMessage());
            throw new PreflightingToolException(PRINT_ERROR, e);
        }
        finally
        {
            try
            {
                writer.close();
                //to help Garbage Collector to work
                writer = null;
            }
            catch (IOException e)
            {
                PreflightingLogger.error(getClass(), IO_ERROR + e.getMessage());
            }
        }
    }

    /**
     * Prints the CSV fields. 
     */
    private void printCSVHeader() throws IOException
    {
        StringBuilder stringBuilder = new StringBuilder();

        // Append results message
        CSV_FIELDS[] fields = CSV_FIELDS.values();

        DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v1);
        for (int i = 0; i < fields.length; i++)
        {
            if (fields[i].compareTo(CSV_FIELDS.suggestion) == 0)
            {
                if (WarningLevelFilter.printQuickFixSuggestions())
                {
                    stringBuilder.append(QUOTES + fields[i].toString() + QUOTES + DELIMITER);
                }
            }
            else
            {
                stringBuilder.append(QUOTES + fields[i].toString() + QUOTES + DELIMITER);
            }
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(NEWLINE);

        writer.write(stringBuilder.toString());
    }

    private void printCheckerResultCSV(ValidationResult validationResult) throws IOException
    {
        List<ValidationResultData> data = validationResult.getValidationResult();

        if (data.size() > 0)
        {
            StringBuilder stringBuilder = new StringBuilder();

            for (ValidationResultData d : data)
            {
                if (getApplicationFile() != null)
                {
                    //No files were issued
                    if (d.getFileToIssueLines().keySet().isEmpty())
                    {
                        printCSVLine(stringBuilder, d, validationResult, null, null);
                    }
                    else
                    {
                        // Iterate through the list of files 
                        for (File resource : d.getFileToIssueLines().keySet())
                        {
                            String fileLocation = computeResourcePath(resource);
                            // Number of occurrences
                            List<Integer> lines = d.getFileToIssueLines().get(resource);

                            if (lines.isEmpty())
                            {
                                printCSVLine(stringBuilder, d, validationResult, fileLocation, null);
                            }
                            else
                            {
                                for (Integer currentLine : lines)
                                {
                                    printCSVLine(stringBuilder, d, validationResult, fileLocation,
                                            currentLine);
                                }
                            }
                        }
                    }
                }

                writer.write(stringBuilder.toString());
                writer.flush();

                // Clean string builder
                stringBuilder.delete(0, stringBuilder.length());
            }
        }
    }

    /**
     * Prints each line of the CSV output. 
     */
    private void printCSVLine(StringBuilder stringBuilder, ValidationResultData resultData,
            ValidationResult validationResult, String fileLocation, Integer currentLine)
    {
        if (!resultData.getSeverity().equals(SEVERITY.OK))
        {
            // Append severity
            stringBuilder.append(QUOTES + resultData.getSeverity().toString() + QUOTES + DELIMITER);

            //checker id
            stringBuilder.append(QUOTES + validationResult.getCheckerId() + QUOTES + DELIMITER);
            //condition
            stringBuilder.append(QUOTES + resultData.getConditionID() + QUOTES + DELIMITER);

            // Append result description
            if (resultData.getIssueDescription() != null)
            {
                stringBuilder.append(QUOTES + escapeQuotes(resultData.getIssueDescription())
                        + QUOTES);
            }

            stringBuilder.append(DELIMITER);

            if (fileLocation != null)
            {
                stringBuilder.append(QUOTES + fileLocation + QUOTES);
            }
            stringBuilder.append(DELIMITER);

            if (currentLine != null)
            {
                stringBuilder.append(QUOTES + currentLine + QUOTES);
            }
            stringBuilder.append(DELIMITER);

            String preview = resultData.getPreview();
            if (preview != null)
            {
                stringBuilder.append(QUOTES
                        + escapeQuotes(preview.length() > MAX_PREVIEW_LENGTH ? preview.substring(0,
                                MAX_PREVIEW_LENGTH) : preview) + QUOTES);
            }
            //stringBuilder.append(DELIMITER);

            /* Removing infoUrl from non-xml outputters
            if (resultData.getInfoURL() != null)
            {
                stringBuilder.append(QUOTES + escapeQuotes(resultData.getInfoURL()) + QUOTES);
            }
            */

            if (WarningLevelFilter.printQuickFixSuggestions())
            {
                stringBuilder.append(DELIMITER);

                if (resultData.getQuickFixSuggestion() != null)
                {
                    stringBuilder.append(QUOTES + escapeQuotes(resultData.getQuickFixSuggestion())
                            + QUOTES);
                }
            }

            stringBuilder.append(NEWLINE);
        }
    }

    /**
     * Escape quotes from a given string
     * @param textToEscape string that may contain quotes to be escaped
     * @return
     */
    private String escapeQuotes(String textToEscape)
    {
        StringBuilder strBuilder = new StringBuilder();
        if (textToEscape.indexOf(QUOTES) != -1)
        {
            String[] splitedStr = textToEscape.split(QUOTES);
            for (int i = 0; i < (splitedStr.length - 1); i++)
            {
                strBuilder.append(splitedStr[i] + BACKSLASH + QUOTES);
            }
            strBuilder.append(splitedStr[splitedStr.length - 1]);

            //special case where the text ends with a quote
            if (textToEscape.endsWith(QUOTES))
            {
                strBuilder.append(BACKSLASH + QUOTES);
            }

            textToEscape = strBuilder.toString();
        }

        return textToEscape;
    }

    @Override
    public void printError(Exception exceptionThrown, PrintStream out)
    {
        // TODO Auto-generated method stub

    }
}
