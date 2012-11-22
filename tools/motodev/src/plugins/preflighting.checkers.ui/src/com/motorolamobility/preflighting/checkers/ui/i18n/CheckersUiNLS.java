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
package com.motorolamobility.preflighting.checkers.ui.i18n;

import org.eclipse.osgi.util.NLS;

public class CheckersUiNLS extends NLS
{
    private static final String BUNDLE_NAME = "com.motorolamobility.preflighting.checkers.ui.i18n.messages"; //$NON-NLS-1$

    public static String MissingPermissionsQuickFix_Description;

    public static String MissingPermissionsQuickFix_Label;

    public static String UnneededPermissionsQuickFix_Description;

    public static String UnneededPermissionsQuickFix_Label;

    public static String DeviceCompatibilityUnsupportedFeaturesQuickFix_Description;

    public static String QuickFix_CouldNotFixTheProblem;

    public static String DeviceCompatibilityUnsupportedFeaturesQuickFix_Label;

    public static String QuickFix_MarkerResolutionFailed;

    public static String MissingMinSdkQuickFix_Description;

    public static String MissingMinSdkQuickFix_Label;

    public static String UneededMaxSdkQuickFix_Description;

    public static String UneededMaxSdkQuickFix_Label;
    
    public static String ImpliedFeaturesMarkerResolution_Fail_Msg_Dlg_Title;
    
    public static String ImpliedFeaturesMarkerResolution_Label;

    public static String ImpliedFeaturesMarkerResolution_Fail_Msg_Manipulate_Manifest;

    public static String ImpliedFeaturesMarkerResolution_Fail_Msg_Save_Manifest;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, CheckersUiNLS.class);
    }

    private CheckersUiNLS()
    {
    }
}
