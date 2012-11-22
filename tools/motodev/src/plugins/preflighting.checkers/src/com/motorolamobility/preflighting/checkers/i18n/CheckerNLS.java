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

package com.motorolamobility.preflighting.checkers.i18n;

import org.eclipse.osgi.util.NLS;

public class CheckerNLS extends NLS
{
    private static final String BUNDLE_NAME =
            "com.motorolamobility.preflighting.checkers.i18n.CheckerNLS"; //$NON-NLS-1$

    public static String Missing_res_folder;
    
    public static String MainActivityChecker_Exception_Get_Action_Intent_Value;

    public static String MainActivityChecker_Exception_Get_Category_Intent_Value;

    public static String Invalid_ManifestFile;

    public static String IsDebuggableCondition_AttrFound_Message;

    public static String IsDebuggableCondition_AttrFound_QuickFix;

    public static String MainActivityChecker_manyMainActivity;

    public static String MainActivityChecker_MoreThanOneMainActivityFixSuggestion;

    public static String MainActivityChecker_noMainActivity;

    public static String MainActivityChecker_NoMainActivityFixSuggestion;

    public static String MissingDrawableChecker_AddDrawableToFolder;

    public static String MissingDrawableChecker_CreateFolderOnResFolder;

    public static String MissingDrawableChecker_CreateFoldersOnResFolder;

    public static String MissingDrawableChecker_UselessXhdpiResources;

    public static String MissingDrawableChecker_UselessXhdpiResourcesSugestion;

    public static String MissingDrawableChecker_FolderNotFound;

    public static String MissingDrawableChecker_ldpiFolder;

    public static String MissingDrawableChecker_mdpiFolder;

    public static String MissingDrawableChecker_hdpiFolder;

    public static String MissingDrawableChecker_xhdpiFolder;

    public static String MissingDrawableChecker_missingDrawableDesc;

    public static String MissingDrawableChecker_noDensitySpecificDrawableFolders;

    public static String MissingDrawableChecker_noDrawableFolders;

    public static String MissingWidgetPreviewTagCondition_quickFix;

    public static String MissingWidgetPreviewTagCondition_WarningMessage;

    public static String LocalizationStringsChecker_Exception_MissingDefaultKeys;

    public static String LocalizationStringsChecker_Exception_MissingLocaleKeys;

    public static String LocalizationStringsChecker_Exception_EmptyValuesDefault;

    public static String LocalizationStringsChecker_Exception_EmptyValuesLocale;

    public static String LocalizationStringsChecker_defaultStringNotFoundSimple;

    public static String LocalizationStringsChecker_localeStringNotFoundSimple;

    public static String LocalizationStringsChecker_addStringToLocalizationResourceDetailed;

    public static String LocalizationStringsChecker_invalidParameters;

    public static String LocalizationStringsChecker_invalidDefaultLocaleParameter;

    public static String LocalizationStringsChecker_helpParameterLocaleValueFormatDescription;

    public static String LocalizationStringsChecker_helpParameterLocaleDescription;

    public static String LocalizationStringsChecker_helpParameterLocaleValueDescription;

    public static String LocalizationStringsChecker_detailedLocaleResourceNotFound;

    public static String LocalizationStringsChecker_localeStringEmptyValue;

    public static String LocalizationStringsChecker_defaultStringEmptyValue;

    public static String LocalizationStringsChecker_Missing_stringsXml_File;

    public static String LocalizationStringsChecker_stringEmptyValueQuickFix;

    public static String LocalizationStringsChecker_conditionMissingValue_CouldNotBeRun_EmptyLocalizationFiles;

    public static String LocalizationStringsChecker_conditionMissingKey_CouldNotBeRun_NoDefault;

    public static String LocalizationStringsChecker_conditionMissingKey_CouldNotBeRun_NoLocale;

    public static String LocalizationStringsChecker_UnknownError;

    public static String LogCallsCondition_CallFound_Message;

    public static String LogCallsCondition_CallFound_QuickFix;

    public static String AndroidMarketFiltersChecker_declaredMaxSdkVersion_Suggestion;

    public static String AndroidMarketFiltersChecker_Exception_Getting_Manifest_Attribute;

    public static String AndroidMarketFiltersChecker_missingManifestIconOrLabel_Suggestion;

    public static String AndroidMarketFiltersChecker_missingManifestIconOrLabel_Issue;

    public static String AndroidMarketFiltersChecker_certificatePeriodExpired_Suggestion;

    public static String AndroidMarketFiltersChecker_certificatePeriodExpired_Issue;

    public static String AndroidMarketFiltersChecker_declaredMaxSdkVersion_Issue;

    public static String AndroidMarketFiltersChecker_minSdkIsPreview_Issue;

    public static String AndroidMarketFiltersChecker_minSdkIsPreview_Suggestion;

    public static String AndroidMarketFiltersChecker_targetSdkIsPreview_Issue;

    public static String AndroidMarketFiltersChecker_targetSdkIsPreview_Suggestion;

    public static String AndroidMarketFiltersChecker_permissionToImpliedFeatures_Suggestion;

    public static String AndroidMarketFiltersChecker_permissionToImpliedFeatures_Issue;

    //version 
    public static String AndroidMarketFiltersChecker_missingVersionCodeOrName_Suggestion;

    public static String AndroidMarketFiltersChecker_missingVersionCodeOrName_Issue;

    public static String AndroidMarketFiltersChecker_certificatePeriodNotYeatValid_Issue;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, CheckerNLS.class);
    }

    public static String AndroidMarketFiltersChecker_missingMinSdkVersion_Issue;

    public static String AndroidMarketFiltersChecker_missingMinSdkVersion_Suggestion;

    public static String BuildingBlockDeclarationCondition_TheClassXShouldExtendBuildingBlockY;

    public static String AndroidMarketFiltersChecker_zipaligned_Suggestion;

    public static String AndroidMarketFiltersChecker_zipaligned_Issue;

    public static String AndroidMarketFiltersChecker_zipaligned_ConditionNotExecutedForProject;

    public static String BuildingBlocksInheritanceCondition_Activity;

    public static String BuildingBlocksInheritanceCondition_BroadcastReceiver;

    public static String BuildingBlocksInheritanceCondition_ContentProvider;

    public static String BuildingBlocksInheritanceCondition_Service;

    public static String BuildingBlocksInheritanceCondition_TheBuildingBlockXShouldExtendClassY;

    public static String DeviceCompatibilityChecker_SMALL_SCREEN_SUPPORT_FIX_SUGGESTION;

    public static String DeviceCompatibilityChecker_SMALL_SCREEN_SUPPORT_ISSUE_DESCRIPTION;

    public static String DeviceCompatibilityChecker_XLARGE_SCREEN_SUPPORT_FIX_SUGGESTION;

    public static String DeviceCompatibilityChecker_XLARGE_SCREEN_SUPPORT_ISSUE_DESCRIPTION;

    public static String PermissionsChecker_MissingPermission_Message;

    public static String PermissionsChecker_MissingPermission_QuickFix;

    public static String UnneededPermissionsCondition_UneededPermissionMessage;

    public static String UnneededPermissionsCondition_UneededPermissionQuickFix;

    public static String UnsecurePermissionsChecker_conditionForbiddenPermission_description;

    public static String UnsecurePermissionsChecker_conditionForbiddenPermission_suggestion;

    public static String DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_FIX_SUGGESTION;

    public static String DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_DECLARED_ISSUE_DESCRIPTION;

    public static String DeviceCompatibilityChecker_CONDITION_UNSUPPORTED_FEATURE_IMPLIED_ISSUE_DESCRIPTION;

    public static String LayoutChecker_MissingKeyWarningMessage;

    public static String LayoutChecker_MissingKeyFixSuggestion;

    public static String OpenedCursorsCondition_Result_Message;

    public static String OpenedCursorsCondition_Result_QuickFix;

    public static String RepeatedIdCondition_Result_Description;

    public static String RepeatedIdCondition_Result_QuickFix;

    public static String ViewTypeIdsCondition_Results_Description;

    public static String ViewTypeIdsCondition_Results_QuickFix;

    public static String XlargeConfigCondition_Result_Description;

    public static String XlargeConfigCondition_Result_QuickFix;

    public static String OrphanedStringsCondition_Result_Description;

    public static String OrphanedStringsCondition_Result_QuickFix;

    private CheckerNLS()
    {
    }
}
