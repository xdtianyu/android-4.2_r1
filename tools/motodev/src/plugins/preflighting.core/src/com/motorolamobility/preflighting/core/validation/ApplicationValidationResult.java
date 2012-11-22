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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Document;

/**
 * This class encapsulates a single execution of validation for an application.
 */
public final class ApplicationValidationResult
{
    private final List<ValidationResult> results;

    private final Map<String, IStatus> executionStatus;

    private final String application;

    private final int version;

    private final String applicationPath;

    private Document resultXMLDocument;

    /**
     * Construct a new ApplicationValidationResult with the given parameters.
     * 
     * @param application the name of the Application.
     * @param version the version of the Application.
     * @param applicationPath the path of the Application.
     */
    public ApplicationValidationResult(String application, int version, String applicationPath)
    {
        this.results = new ArrayList<ValidationResult>();
        this.executionStatus = new LinkedHashMap<String, IStatus>();
        this.application = application;
        this.version = version;
        this.applicationPath = applicationPath;
    }

    /**
     * Add a {@link ValidationResult} representing the results collected from a specific condition from a checker.
     * @param result The result collected from a specific condition from a checker.
     */
    public void addResult(ValidationResult result)
    {
        results.add(result);
    }

    /**
     * Add a list of {@link ValidationResult} as the result of the Application.
     * @param result A list of ValidationResult.
     */
    public void addResult(List<ValidationResult> result)
    {
        results.addAll(result);
    }

    /**
     * Gets the results (list of {@link ValidationResult}) for the Application.
     * @return The list of ValidationResult for the Application.
     */
    public List<ValidationResult> getResults()
    {
        return results;
    }

    /**
     * Adds a status (successful or failure) to the given checker.
     * @param checkerID The checker id.
     * @param status Status of the checker.
     */
    public void addStatus(String checkerID, IStatus status)
    {
        executionStatus.put(checkerID, status);
    }

    /**
     * Gets the execution status (the key for the map is the checkerId).
     * @return map with the execution status for each checkerId.
     */
    public Map<String, IStatus> getExecutionStatus()
    {
        return executionStatus;
    }

    /**
     * Gets the Application name.
     * @return The Application name.
     */
    public String getApplication()
    {
        return application;
    }

    /**
     * Gets the Application version.
     * @return The Application version.
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Gets the Application path.
     * @return The Application path.
     */
    public String getApplicationPath()
    {
        return applicationPath;
    }

    /**
     * Gets the XML result document.
     * @return The XML result document.
     */
    public Document getXmlResultDocument()
    {
        return resultXMLDocument;
    }

    /**
     * Sets the XML result document.
     * @param document The XML result document.
     */
    public void setXmlResultDocument(Document document)
    {
        this.resultXMLDocument = document;
    }

}
