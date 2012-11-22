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

package com.motorolamobility.preflighting.samplechecker.findviewbyid.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * I18n for the checkers
 */
public class Messages extends NLS
{
    private static final String BUNDLE_NAME =
            "com.motorolamobility.preflighting.samplechecker.findviewbyid.i18n.messages"; //$NON-NLS-1$

    public static String FindViewByIdInsideLoopCondition_IssueDescription;

    public static String FindViewByIdInsideLoopCondition_QuickFixSuggestion;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
