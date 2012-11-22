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
package com.motorolamobility.preflighting.checkers.androidmarketfilters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import com.motorolamobility.preflighting.checkers.CheckerPlugin;
import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.internal.utils.ProjectUtils;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;

public class ZipalignedCondition extends Condition implements ICondition
{

    private final String ZIPALIGN_FAILED_STRING = "Verification FAILED";

    private final String ZIPALIGN_SUCCESS_STRING = "Verification succesful";

    private final String OPTIONS_FOR_ZIPALIGN = "-c -v 4";

    private final String ZIPALIGN_EXEC = Platform.getOS().equals(Platform.OS_WIN32)
            ? "zipalign.exe" : "zipalign"; //$NON-NLS-1$ //$NON-NLS-2$

    private static String TOOLS = "tools";

    private String zipalignPath;

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        if (!data.isProject() && (zipalignPath != null))
        {

            String applicationPath = data.getApplicationPath();
            String[] zipalignCmdLine = new String[OPTIONS_FOR_ZIPALIGN.split(" ").length + 2];
            String[] splitOptions = OPTIONS_FOR_ZIPALIGN.split(" ");
            zipalignCmdLine[0] = zipalignPath;
            zipalignCmdLine[OPTIONS_FOR_ZIPALIGN.split(" ").length + 1] = applicationPath;

            for (int i = 1; i <= splitOptions.length; i++)
            {
                zipalignCmdLine[i] = splitOptions[i - 1].trim();
            }

            BufferedReader reader = null;
            InputStreamReader inputStreamReader = null;
            try
            {

                Process p = Runtime.getRuntime().exec(zipalignCmdLine);
                inputStreamReader = new InputStreamReader(p.getInputStream());
                reader = new BufferedReader(inputStreamReader);

                String line = null;
                Boolean zipaligned = null;

                //look for the phrase "Verification FAILED" in the zipalign output.
                while ((line = reader.readLine()) != null)
                {
                    if (line.contains(ZIPALIGN_FAILED_STRING))
                    {
                        zipaligned = false;
                        break;
                    }
                    else if (line.contains(ZIPALIGN_SUCCESS_STRING))
                    {
                        zipaligned = true;
                        break;
                    }
                }

                if (zipaligned == null)
                {
                    PreflightingLogger.error(CertificateExpiredCondition.class,
                            "It was not possible to identify if file is zipaligned"); //$NON-NLS-1$
                }
                else
                {
                    if (!zipaligned)
                    {
                        ValidationResultData resultData =
                                new ValidationResultData(
                                        CheckerUtils.createFileToIssuesMap(null, 0),
                                        getSeverityLevel(),
                                        NLS.bind(
                                                CheckerNLS.AndroidMarketFiltersChecker_zipaligned_Issue,
                                                applicationPath),
                                        CheckerNLS.AndroidMarketFiltersChecker_zipaligned_Suggestion,
                                        getId());
                        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker()
                                .getId(), getId(), valManagerConfig));
                        results.addValidationResult(resultData);
                    }
                }
            }
            catch (IOException e)
            {
                PreflightingLogger.error(CertificateExpiredCondition.class,
                        "It was not possible to execute/read zipalign command"); //$NON-NLS-1$
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        //Do Nothing.
                    }
                }
                if (inputStreamReader != null)
                {
                    try
                    {
                        inputStreamReader.close();
                    }
                    catch (IOException e)
                    {
                        //Do Nothing.
                    }
                }
            }

        }
        else
        {
            if (data.isProject())
            {
                DebugVerboseOutputter
                        .printVerboseMessage(
                                CheckerNLS
                                        .bind(CheckerNLS.AndroidMarketFiltersChecker_zipaligned_ConditionNotExecutedForProject,
                                                getId()), VerboseLevel.v2);
            }
        }

    }

    private String getZipalignPath(String sdk)
    {

        String zipAlignPath = null;
        File sdkFolder = new File(sdk);

        if (sdkFolder.isDirectory())
        {
            File toolsFolder = new File(sdkFolder, TOOLS);

            if (toolsFolder.exists())
            {
                File zipalignToolFile = new File(toolsFolder, ZIPALIGN_EXEC);
                if (zipalignToolFile.exists())
                {
                    zipAlignPath = zipalignToolFile.getAbsolutePath();
                }
            }
        }

        return zipAlignPath;
    }

    /**
     * This checker can be executed if the zipalign is available in the SDK
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status = null;
        String sdkPath = null;

        for (Parameter parameter : ((Checker) getChecker()).getGlobalParams())
        {
            if (parameter.getParameterType().equals(
                    ValidationManager.InputParameter.SDK_PATH.getAlias()))
            {
                sdkPath = ProjectUtils.getSdkPath(parameter);
            }
        }

        if (sdkPath != null)
        {
            zipalignPath = getZipalignPath(sdkPath);
            status =
                    new CanExecuteConditionStatus(IStatus.OK, CheckerPlugin.PLUGIN_ID, "", getId());
        }
        else
        {
            status =
                    new CanExecuteConditionStatus(IStatus.ERROR, CheckerPlugin.PLUGIN_ID,
                            "Sdk path could not be retrieved from global paremeters", getId());
        }
        return status;
    }

}
