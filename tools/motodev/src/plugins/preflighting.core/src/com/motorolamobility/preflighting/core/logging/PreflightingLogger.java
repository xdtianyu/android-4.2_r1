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
package com.motorolamobility.preflighting.core.logging;

import com.motorolamobility.preflighting.core.internal.logging.Logger;

/**
 * Logger class for App Validator tool.
 * All methods must be accessed statically. 
 */
public abstract class PreflightingLogger
{
    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#DEBUG} level.
     * 
     * @param message Message to be logged.
     */
    public static void debug(String message)
    {
        Logger.getLogger(PreflightingLogger.class.getName()).debug(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#INFO} level.
     * 
     * @param message Message to be logged.
     */
    public static void info(String message)
    {
        Logger.getLogger(PreflightingLogger.class.getName()).info(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#WARN} level.
     * 
     * @param message Message to be logged.
     */
    public static void warn(String message)
    {
        Logger.getLogger(PreflightingLogger.class.getName()).warn(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#ERROR} level.
     *  
     * @param message Message to be logged.
     */
    public static void error(String message)
    {
        Logger.getLogger(PreflightingLogger.class.getName()).error(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#FATAL} level.
     * 
     * @param message Message to be logged.
     */
    public static void fatal(String message)
    {
        Logger.getLogger(PreflightingLogger.class.getName()).fatal(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#DEBUG} level.
     * 
     * @param obj Object where the error occurred. Can be <code>null</code>.
     * @param message Message to be logged.
     */
    public static void debug(Object obj, String message)
    {
        Logger.getLogger(PreflightingLogger.class.getName()).debug(message);

    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#INFO} level.
     * 
     * @param message Message to be logged. 
     * @param aClass Class to identify in the log.
     */
    public static void info(Class<?> aClass, String message)
    {
        Logger.getLogger(aClass.toString()).info(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#WARN} level.
     * 
     * @param message Message to be logged.     
     * @param aClass Class to identify in the log.
     */
    public static void warn(Class<?> aClass, String message)
    {
        Logger.getLogger(aClass.toString()).warn(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#WARN} level.
     * 
     * @param aClass Class to identify in the log.
     * @param message Message to be logged.
     * @param Throwable that raised the warning.
     */
    public static void warn(Class<?> aClass, String message, Throwable throwable)
    {
        Logger.getLogger(aClass.toString()).warn(message, throwable);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#ERROR} level.
     * 
     * @param aClass Class to identify in the log.
     * @param message Message to be logged.
     */
    public static void error(Class<?> aClass, String message)
    {
        Logger.getLogger(aClass.toString()).error(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#ERROR} level.
     * 
     * @param aClass Class to identify in the log.
     * @param message Message to be logged.
     * @param Throwable that raised the warning.
     */
    public static void error(Class<?> aClass, String message, Throwable error)
    {
        Logger.getLogger(aClass.toString()).error(message, error);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#FATAL} level.
     * 
     * @param aClass Class to identify in the log.
     * @param message Message to be logged.
     */
    public static void fatal(Class<?> aClass, String message)
    {
        Logger.getLogger(aClass.toString()).fatal(message);
    }

    /**
     * Print log messages in {@link com.motorolamobility.preflighting.core.logging.Level#FATAL} level.
     * 
     * @param aClass Class to identify in the log.
     * @param message Message to be logged.
     * @param Throwable that raised the warning.
     */
    public static void fatal(Class<?> aClass, String message, Throwable error)
    {
        Logger.getLogger(aClass.toString()).fatal(message, error);
    }
}
