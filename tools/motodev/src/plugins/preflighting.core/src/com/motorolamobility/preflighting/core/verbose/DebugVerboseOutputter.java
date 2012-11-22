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

import java.io.PrintStream;

import com.motorolamobility.preflighting.core.i18n.PreflightingCoreNLS;

/**
 * Abstract class responsible for defining verbosity levels, setting the current verbosity level,
 * and printing verbose messages according to verbosity level set.
 * The verbosity level will be passed as parameter to the application, and if not passed,
 * {@link DebugVerboseOutputter#DEFAULT_VERBOSE_LEVEL} will be assumed (default verbosity level).
 * The default stream used by this outputter is system.err.
 */
public abstract class DebugVerboseOutputter
{
    public enum VerboseLevel
    {
        /**
         * Default verbosity level (no added verbosity).
         */
        v0
        {
            /* (non-Javadoc)
             * @see java.lang.Enum#toString()
             */
            @Override
            public String toString()
            {
                return ""; //$NON-NLS-1$
            }
        },

        /**
         * Indicate the application flow as individual resources or conditions are checked.
         */
        v1
        {
            /* (non-Javadoc)
             * @see java.lang.Enum#toString()
             */
            @Override
            public String toString()
            {
                return PreflightingCoreNLS.VerboseOutputter_InfoVerboseLevelString;
            }
        },

        /**
         * Debugging mode indicates a great deal of information about every aspect of the execution
         * flow. This could be as functions are entered or exited.
         */
        v2
        {
            /* (non-Javadoc)
             * @see java.lang.Enum#toString()
             */
            @Override
            public String toString()
            {
                return PreflightingCoreNLS.VerboseOutputter_DebugVerboseLeveString;
            }
        };
    }

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private static PrintStream myStream = System.out;

    /**
     * The default verbosity level ({@link VerboseLevel#v0}).
     */
    public static final VerboseLevel DEFAULT_VERBOSE_LEVEL = VerboseLevel.v0;

    /**
     * The current verbosity level used by the application.
     */
    private static VerboseLevel currentVerboseLevel = DEFAULT_VERBOSE_LEVEL;

    /**
     * Retrieve the current verbosity level used by the application.
     * 
     * @return Current verbosity level used by the application.
     */
    public static VerboseLevel getCurrentVerboseLevel()
    {
        return currentVerboseLevel;
    }

    /**
     * Set the current verbosity level to the given value.
     * If <code>null</code> is passed, the default verbosity level
     * ({@link DebugVerboseOutputter#DEFAULT_VERBOSE_LEVEL}) will be set.
     * 
     * @param level The verbosity level to be set.
     */
    public static void setCurrentVerboseLevel(VerboseLevel level)
    {
        if (level != null)
        {
            currentVerboseLevel = level;
        }
        else
        {
            currentVerboseLevel = DEFAULT_VERBOSE_LEVEL;
        }
    }

    /**
     * Print the message with the given verbosity level, if and only
     * if the current verbosity level allows it to be printed (current
     * level is greater or equal to the passed level).
     * 
     * @param message Message to be printed.
     * @param level The verbosity level in which the message should be printed.
     */
    public static void printVerboseMessage(String message, VerboseLevel level)
    {
        if (currentVerboseLevel.compareTo(level) >= 0)
        {
            if ((message != null) && (message.length() > 0))
            {
                getPrintStream().println(level + message);
            }
            else
            {
                getPrintStream().println();
            }
            getPrintStream().flush();
        }
    }

    /**
     * Return a string with the message with the given verbosity level, if and only
     * if the current verbosity level allows it to be printed (current
     * level is greater or equal to the passed level).
     * 
     * @param message Message to be printed.
     * @param level The verbosity level in which the message should be printed.
     * @return a string with the message with the given verbosity level.
     */
    public static String printVerboseMessageToString(String message, VerboseLevel level)
    {
        String strMsg = "";

        if (currentVerboseLevel.compareTo(level) >= 0)
        {
            if ((message != null) && (message.length() > 0))
            {
                strMsg = level + message;
            }
            else
            {
                strMsg = NEWLINE;
            }
        }
        return strMsg;
    }

    /**
     * Set the stream for printing the verbose output.
     * 
     * @param printStream The stream output.
     */
    public static void setStream(PrintStream printStream)
    {
        myStream = printStream;
    }

    /**
     * Retrieve the print stream to be used for printing messages.
     * 
     * @return The print stream.
     */
    private static PrintStream getPrintStream()
    {
        return myStream;
    }
}
