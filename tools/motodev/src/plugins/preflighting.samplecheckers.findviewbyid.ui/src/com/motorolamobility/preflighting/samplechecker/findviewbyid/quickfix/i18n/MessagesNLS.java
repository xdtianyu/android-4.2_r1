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
package com.motorolamobility.preflighting.samplechecker.findviewbyid.quickfix.i18n;

import org.eclipse.osgi.util.NLS;

public class MessagesNLS extends NLS
{
    private static final String BUNDLE_NAME =
            "com.motorolamobility.preflighting.samplechecker.findviewbyid.quickfix.i18n.messages"; //$NON-NLS-1$

    public static String FindViewByIdMarkerResolution_Description;

    public static String FindViewByIdMarkerResolution_Error_Aplying_Changes;

    public static String FindViewByIdMarkerResolution_Error_Could_Not_Fix_Code;

    public static String FindViewByIdMarkerResolution_Error_Msg_Title;

    public static String FindViewByIdMarkerResolution_Error_Unable_To_Open_Editor;

    public static String FindViewByIdMarkerResolution_Label;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, MessagesNLS.class);
    }

    private MessagesNLS()
    {
    }
}
