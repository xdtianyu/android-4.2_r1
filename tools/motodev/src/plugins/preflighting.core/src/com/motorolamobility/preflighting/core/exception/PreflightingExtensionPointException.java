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
package com.motorolamobility.preflighting.core.exception;

/**
 * Exception class representing an error on extension point reading.
 */
@SuppressWarnings("serial")
public class PreflightingExtensionPointException extends PreflightingToolException
{

    /**
     * Instantiates the {@link PreflightingExtensionPointException} providing
     * the error message which originated this exception.
     *     
     * @param message Error message which originated this exception.
     */
    public PreflightingExtensionPointException(String message)
    {
        super(message);
    }

    /**
     * Instantiates the {@link PreflightingExtensionPointException} providing the
     * error message and {@link Throwable} which originated this exception.
     * 
     * @param message Error message for this exception.
     * @param cause {@link Throwable} which originated this exception.
     */
    public PreflightingExtensionPointException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
