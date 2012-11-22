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

package com.motorolamobility.preflighting.samplechecker.findviewbyid.implementation;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.samplechecker.findviewbyid.SampleCheckersActivator;

/**
 * This condition shows how to use utility class {@link CheckerUtils} to identify if java is available before checker execution.
 * <br>
 * The example illustrates how to check Android code through App Validator:
 * <ul>
 * <li>the access of CompilationUnits from project,</li> 
 * <li>the ASTVisitor to get information about the code</li>
 * <li>how to report results through a ValidationResultData</li>
 * </ul>
 *  
 * The checker searches for <code>findViewById</code> statements which are inside a loop block (<code>for, extended for, while, do-while</code>) 
 * that could be possibly placed outside the loop to reduce CPU processing. 
 */
public class FindViewByIdInLoop extends Condition implements ICondition
{

    /**
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#canExecute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List)
     */
    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        CanExecuteConditionStatus status = null;
        //Check if we are analysing an Android source code Project or an APK
        if (data.isProject())
        {
            //Verify if model is complete to allow checker analysis in the code
            status = CheckerUtils.isJavaModelComplete(data, getId());
        }
        else
        {
            //this checker is devoted only for Android projects (not for APK)
            status =
                    new CanExecuteConditionStatus(
                            IStatus.ERROR,
                            SampleCheckersActivator.PLUGIN_ID,
                            "This condition runs only for Android Project (not for APK). Please check the help for more details.");
        }
        status.setConditionId(getId());
        return status;
    }

    /**
     * @see com.motorolamobility.preflighting.core.checker.condition.Condition#execute(com.motorolamobility.preflighting.core.applicationdata.ApplicationData, java.util.List, com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration)
     */
    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        //get source compilation units to visit the code
        List<CompilationUnit> compilationUnits = data.getProjectCompilationUnits();
        if (compilationUnits != null)
        {
            for (CompilationUnit compilationUnit : compilationUnits)
            {
                //visit each source file to find issues
                if (compilationUnit != null)
                {
                    compilationUnit.accept(new FindViewByIdVisitor(getId(), getSeverityLevel(),
                            getMarkerType(), results, compilationUnit));
                }
            }
        }
        else
        {
            //print in the console (if info LEVEL set for verbosity of App Validator output)
            PreflightingLogger.info("No compilation unit found to visit the code");
        }
    }
}
