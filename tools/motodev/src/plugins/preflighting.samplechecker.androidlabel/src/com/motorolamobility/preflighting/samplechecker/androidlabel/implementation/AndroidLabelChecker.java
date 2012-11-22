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

package com.motorolamobility.preflighting.samplechecker.androidlabel.implementation;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.motorolamobility.preflighting.core.checker.Checker;
import com.motorolamobility.preflighting.core.checker.IChecker;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.validation.Parameter;

/**
 * This Checker is responsible for verify Android Label issues. In
 * order to see more details, see its Conditions.
 * <br><br>
 * This checker is intended to be used as an sample so App Validator
 * users can create their own checkers, using this as a reference.
 *
 */
public class AndroidLabelChecker extends Checker implements IChecker
{
    /**
     * Defines the command line parameter
     */
    public static final String PARAMETER_LABEL_TEXT = "labelText"; //$NON-NLS-1$

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.core.checker.Checker#validateInputParams(java.util.List)
     */
    @Override
    public IStatus validateInputParams(List<Parameter> parameters)
    {
        CanExecuteConditionStatus status =
                (CanExecuteConditionStatus) super.validateInputParams(parameters);

        if (status.getSeverity() == Status.ERROR)
        {
            status.setStatusSeverity(Status.INFO);
        }
        return status;
    }
}
