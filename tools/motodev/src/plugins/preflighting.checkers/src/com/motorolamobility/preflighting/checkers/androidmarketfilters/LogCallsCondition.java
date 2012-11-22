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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 * Condition that verifies if there's any call to Android logcat within the java source code.
 * If any is found a warning message is added to the results
 */
public class LogCallsCondition extends Condition
{

    /**
     * Full-qualified name of android Log class.
     */
    public static final String ANDROID_UTIL_LOG = "android.util.Log";

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        return CheckerUtils.isJavaModelComplete(data, getId());
    }

    /*
     * (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.devicespecification.PlatformRules, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration, com.motorolamobility.preflighting.core.validation.ValidationResult)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        List<SourceFolderElement> sourceFolderElements = data.getJavaModel();

        for (SourceFolderElement sourceFolder : sourceFolderElements)
        {
            List<Invoke> calledMethods = sourceFolder.getInvokedMethods();
            for (Invoke calledMethod : calledMethods)
            {
                analyzeCalledMethod(results, calledMethod);
            }
        }

    }

    /**
     * Verify if the called method is a log call.
     * If it is, add a warning result to results.
     * @param results
     * @param calledMethod
     */
    private void analyzeCalledMethod(ValidationResult results, Invoke calledMethod)
    {
        String classCalled = calledMethod.getClassCalled();

        if ((classCalled != null) && (classCalled.equals(ANDROID_UTIL_LOG)))
        {
            if (calledMethod.getMethodName().equals("w") //$NON-NLS-1$
                    || calledMethod.getMethodName().equals("i") //$NON-NLS-1$
                    || calledMethod.getMethodName().equals("v") //$NON-NLS-1$
                    || calledMethod.getMethodName().equals("d") //$NON-NLS-1$
                    || calledMethod.getMethodName().equals("e") //$NON-NLS-1$
                    || calledMethod.getMethodName().equals("wtf")) //$NON-NLS-1$
            {
                Map<File, List<Integer>> fileToIssueLines = new HashMap<File, List<Integer>>(1);
                int line = calledMethod.getLine();
                if (line > 0)
                {
                    fileToIssueLines.put(new File(calledMethod.getSourceFileFullPath()),
                            Arrays.asList(line));
                }

                ValidationResultData resultData =
                        new ValidationResultData(fileToIssueLines, getSeverityLevel(),
                                CheckerNLS.LogCallsCondition_CallFound_Message,
                                CheckerNLS.LogCallsCondition_CallFound_QuickFix, getId());
                resultData.setPreview(calledMethod.getQualifiedName());
                results.addValidationResult(resultData);
            }
        }
    }
}
