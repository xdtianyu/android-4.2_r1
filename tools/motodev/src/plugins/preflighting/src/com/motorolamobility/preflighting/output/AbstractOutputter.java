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
package com.motorolamobility.preflighting.output;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;

/**
 * Abstract class for printing results
 */
public abstract class AbstractOutputter
{
    private File applicationFile;

    private int limit = -1;

    /**
     * Returns the path for the application being validated.
     * <br><br>
     * Note: call initializeParams before calling this method.
     * @return the path for the application being validated.
     */
    public File getApplicationFile()
    {
        return applicationFile;
    }

    /**
     * Sets the path for the application being validated.
     * @param applicationFile the file path.
     */
    public void setApplicationFile(File applicationFile)
    {
        this.applicationFile = applicationFile;
    }

    /**
     * The maximum number of results that will be displayed.
     * <br><br>
     * Note: call initializeParams before calling this method.
     * @return an integer representing the limit.
     */
    public int getLimit()
    {
        return limit;
    }

    /**
     * Sets the maximum number of results that will be displayed.
     * @param limit the integer representing the limit.
     */
    public void setLimit(int limit)
    {
        this.limit = limit;
    }

    /**
     * Initialize limit and applicationFile variables
     * @param parameters the parameters to the outputter
     */
    public void initializeParams(List<Parameter> parameters)
    {
        for (Parameter inputParameter : parameters)
        {
            if (InputParameter.APPLICATION_PATH.getAlias()
                    .equals(inputParameter.getParameterType()))
            {
                String applicationPath = inputParameter.getValue();
                applicationFile = new File(applicationPath);
            }
            else if (InputParameter.LIMIT.getAlias().equals(inputParameter.getParameterType()))
            {
                try
                {
                    limit = Integer.parseInt(inputParameter.getValue());
                }
                catch (NumberFormatException nfe)
                {
                    //do nothing
                }
            }
        }
    }

    /**
     * Print errors to a stream.
     * 
     * @param exceptionThrown
     * @param out the stream used to print errors.
     */
    public abstract void printError(Exception exceptionThrown, PrintStream errorStream);

    /**
     * Prints the results to a stream.
     * <br>
     * Note: If you need parameter information inside this method, call initializeParams.
     * 
     * @param result the result set to be printed.
     * @param stream the stream used to print validation results.
     * @param parameters the parameters from the command line
     * @throws PreflightingToolException
     */
    public abstract void print(ApplicationValidationResult results, OutputStream stream,
            List<Parameter> parameters) throws PreflightingToolException;

    /**
     * Compute the relative path of project and APK resources
     * @param resouce the file representing the resource whose path is desired
     * @return the resource path
     */
    protected String computeResourcePath(File resource)
    {
        // Append file location (only relative path)
        String fileLocation = resource.getAbsolutePath();
        // project passed
        if (fileLocation.startsWith(applicationFile.getAbsolutePath()))
        {
            fileLocation = fileLocation.substring(applicationFile.getAbsolutePath().length() + 1);
        }
        // probably apk passed, check
        else
        {
            String apkName = applicationFile.getName();
            int apkNameIndex = fileLocation.indexOf(apkName);
            // if this test fails, the file with problem is possibly somewhere
            // unknown and the relative path cannot be guessed and it will be used as is 
            if (apkNameIndex != -1)
            {
                int relativePathStartIndex = fileLocation.indexOf(File.separator, apkNameIndex);
                fileLocation = fileLocation.substring(relativePathStartIndex + 1);
            }
            else
            {
                fileLocation = resource.getPath();
            }

        }
        return fileLocation;
    }
}
