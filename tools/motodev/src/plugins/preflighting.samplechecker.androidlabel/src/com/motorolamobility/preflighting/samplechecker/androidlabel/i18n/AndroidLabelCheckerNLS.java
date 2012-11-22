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

package com.motorolamobility.preflighting.samplechecker.androidlabel.i18n;

import org.eclipse.osgi.util.NLS;

public class AndroidLabelCheckerNLS extends NLS
{
    private static final String BUNDLE_NAME =
            "com.motorolamobility.preflighting.samplechecker.androidlabel.i18n.AndroidLabelCheckerNLS"; //$NON-NLS-1$

    public static String CorrectTextInLabelCondition_AddLabelAndroidXMLLocale;

    public static String CorrectTextInLabelCondition_AddTextInLabel;

    public static String CorrectTextInLabelCondition_AndroidXMlMustHaveLabelRunChecker;

    public static String CorrectTextInLabelCondition_ExecuteCheckerEnterLabelText;

    public static String CorrectTextInLabelCondition_LabelNotContainedAndroidXML;

    public static String CorrectTextInLabelCondition_LabelReferedAndroidXML;

    public static String CorrectTextInLabelCondition_LabelReferedAndroidXMLDefaultLocale;

    public static String CorrectTextInLabelCondition_LabelReferedAndroidXMLLocale;

    public static String CorrectTextInLabelCondition_NoEnteredParamWarn;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, AndroidLabelCheckerNLS.class);
    }

    private AndroidLabelCheckerNLS()
    {
    }
}
