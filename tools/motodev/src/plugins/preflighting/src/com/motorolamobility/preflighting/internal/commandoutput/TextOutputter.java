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
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.output.AbstractOutputter;

public class TextOutputter extends AbstractOutputter
{

    private static final int MAX_PREVIEW_LENGTH = 120;

    /**
     * Character for separating line numbers on the output
     */
    private static final String MESSAGE_LINES_SEPARATOR = ", "; //$NON-NLS-1$

    /**
     * Line separator
     */
    private final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * I/O Exception message logging.
     */
    private final String IO_ERROR = PreflightingNLS.TextOutputter_IOExceptionMessage;

    /**
     * Exception stating that it was not possible to print the results.
     */
    private final String PRINT_ERROR = PreflightingNLS.TextOutputter_PrintResultsErrorMessage;

    /**
     * Tab character
     */
    private final String TAB = "\t"; //$NON-NLS-1$

    /**
     *  Character for separating line numbers from preview message
     */
    private static final String PREVIEW_MESSAGE_SEPARATOR = " - "; //$NON-NLS-1$

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.commandoutput.IOutputter#print(java.util.Map, java.io.OutputStream, java.util.List)
     */
    @Override
    public void print(ApplicationValidationResult results, OutputStream stream,
            List<Parameter> parameters) throws PreflightingToolException
    {
        initializeParams(parameters);

        // Use a BufferedWriter to write the results
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
        BufferedWriter writer = new BufferedWriter(outputStreamWriter);
        try
        {
            if (results.getResults().size() > 0)
            {
                StringBuilder stringBuilder = new StringBuilder();

                // Append results message
                stringBuilder.append(NEWLINE + PreflightingNLS.TextOutputter_ApplicationMessage
                        + getApplicationFile().getAbsolutePath() + NEWLINE);
                stringBuilder.append(NEWLINE + PreflightingNLS.TextOutputter_ResultsMessage
                        + NEWLINE);

                writer.write(stringBuilder.toString());
                writer.flush();

                writer.newLine();

                // Iterate trough the results and print them.
                for (ValidationResult checker : results.getResults())
                {
                    printCheckerResult(checker, writer);
                }

                writer.newLine();

                /*
                //Print the execution Report
                printExecutionReport(results.getExecutionStatus(), stream);
                */

                String totalMessage =
                        WarningLevelFilter.getValidationResultTotalMessage(results.getResults());
                if (totalMessage != null)
                {
                    writer.newLine();
                    writer.write(totalMessage);
                    writer.flush();
                    writer.newLine();
                }
                writer.newLine();

            }
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
                outputStreamWriter.close();
            }
            catch (IOException e)
            {
                //Do Nothing.
            }
            try
            {
                writer.close();
            }
            catch (IOException e)
            {
                //Do Nothing.
            }
        }

    }

    public static void printExecutionReport(Map<String, IStatus> executionStatus,
            OutputStream stream) throws IOException
    {
        if (executionStatus != null)
        {
            OutputStreamWriter outputStreamWriter = null;
            BufferedWriter bufferedWriter = null;
            try
            {
                outputStreamWriter = new OutputStreamWriter(stream);
                bufferedWriter = new BufferedWriter(outputStreamWriter);
                bufferedWriter.newLine();
                bufferedWriter.newLine();
                bufferedWriter.write(PreflightingNLS.TextOutputter_ExecutionReportTitle);
                bufferedWriter.newLine();

                for (String checkerId : executionStatus.keySet())
                {
                    String checkerName = ValidationManager.getCheckerExtension(checkerId).getName();
                    IStatus checkerStatus = executionStatus.get(checkerId);
                    String statusMessage =
                            checkerStatus.isOK()
                                    ? PreflightingNLS.TextOutputter_ExecutionReportExecutedMsg
                                    : checkerStatus.getMessage();
                    bufferedWriter.newLine();
                    bufferedWriter.write(checkerName
                            + PreflightingNLS.TextOutputter_ExecutionReportSeparator
                            + statusMessage);
                }
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
            finally
            {
                if (bufferedWriter != null)
                {
                    bufferedWriter.close();
                }
                try
                {
                    outputStreamWriter.close();
                }
                catch (Exception e)
                {
                    //Do Nothing.
                }
            }
        }
    }

    /**
     * Auxiliary method to print the results of a single checker
     * 
     * @param checker - The checker that returned the results
     * @param validationResult - The validation results.
     * @param writer 
     * @throws IOException 
     */
    private void printCheckerResult(ValidationResult validationResult, BufferedWriter writer)
            throws IOException
    {
        List<ValidationResultData> data = validationResult.getValidationResult();

        if (data.size() > 0)
        {
            boolean printSeverity = WarningLevelFilter.printSeverity();

            StringBuilder stringBuilder = new StringBuilder();

            for (ValidationResultData d : data)
            {
                /*
                 *  Result format will be like this:
                 *  {Severity} : {Issue Description}
                 *      {File} ({Number of occurrences}) 
                 *      Line[s] {Line Number[s] (separated by comma)}  
                 */

                if (printSeverity && !d.getSeverity().equals(SEVERITY.OK))
                {
                    // Append severity
                    stringBuilder.append(d.getSeverity().toString());
                }

                // Append result description
                if (d.getIssueDescription() != null)
                {
                    if (printSeverity && !d.getSeverity().equals(SEVERITY.OK))
                    {
                        stringBuilder.append(": "); //$NON-NLS-1$
                    }
                    stringBuilder.append(d.getIssueDescription() + NEWLINE);
                }

                if (getApplicationFile() != null)
                {
                    // Iterate through the list of files 
                    for (File resource : d.getFileToIssueLines().keySet())
                    {
                        stringBuilder.append(TAB);
                        if (resource.isFile() || resource.getName().endsWith(".java"))
                        {
                            stringBuilder.append(PreflightingNLS.TextOutputter_File_Prefix);
                        }
                        else
                        {
                            stringBuilder.append(PreflightingNLS.TextOutputter_Folder_Prefix);
                        }

                        // Append file location (only relative path)
                        String fileLocation = computeResourcePath(resource);
                        // Append number of occurrences
                        List<Integer> lines = d.getFileToIssueLines().get(resource);
                        String numberOfOccurrences = ""; //$NON-NLS-1$
                        String lineMessage = null;
                        if (lines.size() == 1)
                        {
                            numberOfOccurrences =
                                    PreflightingNLS.TextOutputter_OneOccurrenceMessage;
                            lineMessage = PreflightingNLS.TextOutputter_LineMessage;
                        }
                        else if (lines.size() > 1)
                        {
                            numberOfOccurrences =
                                    PreflightingNLS
                                            .bind(PreflightingNLS.TextOutputter_MoreThanOneOccurrenceMessage,
                                                    lines.size());
                            lineMessage = PreflightingNLS.TextOutputter_LinesMessage;
                        }

                        stringBuilder.append(fileLocation + numberOfOccurrences);

                        stringBuilder.append(NEWLINE);

                        // Check if it is an apk or a project, and print file lines only for project
                        if ((getApplicationFile() != null) && getApplicationFile().isDirectory())
                        {
                            if (lineMessage != null)
                            {
                                stringBuilder.append(TAB);
                                stringBuilder.append(TAB);

                                stringBuilder.append(lineMessage);
                            }

                            // For each line found, report a result
                            for (int i = 0; i < lines.size(); i++)
                            {
                                // Append line number                            
                                stringBuilder.append(lines.get(i).intValue());
                                if (i < (lines.size() - 1))
                                {
                                    stringBuilder.append(MESSAGE_LINES_SEPARATOR);
                                }
                            }
                            if (d.getPreview() != null)
                            {
                                stringBuilder.append(PREVIEW_MESSAGE_SEPARATOR);
                                String preview = d.getPreview();
                                stringBuilder.append(preview.length() > MAX_PREVIEW_LENGTH
                                        ? preview.substring(0, MAX_PREVIEW_LENGTH) : preview);
                            }
                            stringBuilder.append(NEWLINE);
                        }
                    }
                }

                // print quick fix only for correct warning level and if a quick fix is available
                if (WarningLevelFilter.printQuickFixSuggestions()
                        && (d.getQuickFixSuggestion() != null))
                {
                    stringBuilder.append(TAB);
                    stringBuilder.append(PreflightingNLS.TextOutputter_FixSuggestionMessage);
                    stringBuilder.append(d.getQuickFixSuggestion());
                    stringBuilder.append(NEWLINE);
                }

                writer.write(stringBuilder.toString());
                writer.newLine();
                writer.flush();

                // Clean string builder
                stringBuilder.delete(0, stringBuilder.length());
            }
        }
    }

    @Override
    public void printError(Exception exceptionThrown, PrintStream out)
    {
        // Not needed.
    }
}
