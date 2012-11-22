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
 * This Exception must be used every time the validation limit is exceeded. 
 */
public class ValidationLimitException extends ArrayIndexOutOfBoundsException
{
    private static final long serialVersionUID = -4575525459322334535L;

    /**
     * Constructs a <code>ValidationLimitException</code> with no 
     * detail message. 
     */
    public ValidationLimitException()
    {
    }

    /**
     * Constructs a <code>ValidationLimitException</code> class 
     * with the specified detail message. 
     *
     * @param   s   The detail message.
     */
    public ValidationLimitException(String s)
    {
        super(s);
    }

}
