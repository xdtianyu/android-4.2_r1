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
package com.motorolamobility.preflighting.checkers.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.motorolamobility.preflighting.checkers.i18n.CheckerNLS;
import com.motorolamobility.preflighting.core.applicationdata.ApplicationData;
import com.motorolamobility.preflighting.core.applicationdata.SourceFolderElement;
import com.motorolamobility.preflighting.core.checker.condition.CanExecuteConditionStatus;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.checker.condition.ICondition;
import com.motorolamobility.preflighting.core.devicespecification.DeviceSpecification;
import com.motorolamobility.preflighting.core.exception.PreflightingCheckerException;
import com.motorolamobility.preflighting.core.internal.cond.utils.ConditionUtils;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.source.model.Instruction;
import com.motorolamobility.preflighting.core.source.model.Invoke;
import com.motorolamobility.preflighting.core.source.model.Method;
import com.motorolamobility.preflighting.core.source.model.SourceFileElement;
import com.motorolamobility.preflighting.core.source.model.Variable;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationManagerConfiguration;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;

/**
 *  This condition verifies if all opened db cursors are closed within a method
 */
public class OpenedCursorsCondition extends Condition implements ICondition
{

    private static final String DATABASE_CURSOR_QUALIFIED_NAME = "android.database.Cursor"; //$NON-NLS-1$

    private boolean isProject;

    /*
     * Exclude managed cursors
     */
    private static final String ACTIVITY_QUALIFIED_NAME = "android.app.Activity";

    private static final String START_MANAGING_CURSOR_METHOD_NAME = ".startManagingCursor";

    private static final String MANAGED_QUERY_METHOD_NAME = ".managedQuery";

    // close method
    private static final String CLOSE_METHOD_NAME = ".close"; //$NON-NLS-1$

    @Override
    public CanExecuteConditionStatus canExecute(ApplicationData data,
            List<DeviceSpecification> deviceSpecs) throws PreflightingCheckerException
    {
        return CheckerUtils.isJavaModelComplete(data, getId());
    }

    @Override
    public void execute(ApplicationData data, List<DeviceSpecification> deviceSpecs,
            ValidationManagerConfiguration valManagerConfig, ValidationResult results)
            throws PreflightingCheckerException
    {
        this.isProject = data.isProject();
        List<SourceFolderElement> sourceFolderElements = data.getJavaModel();
        for (SourceFolderElement sourceFolder : sourceFolderElements)
        {
            List<SourceFileElement> sourceFiles = sourceFolder.getSourceFileElements();
            for (SourceFileElement sourceFile : sourceFiles)
            {
                //Look on virtual methods
                List<Method> methods = sourceFile.getVirtualMethods();
                analizeMethods(valManagerConfig, results, sourceFile, methods);

                //Look on direct methods
                methods = sourceFile.getDirectMethods();
                analizeMethods(valManagerConfig, results, sourceFile, methods);
            }
        }
    }

    private void analizeMethods(ValidationManagerConfiguration valManagerConfig,
            ValidationResult results, SourceFileElement sourceFile, List<Method> methods)
    {
        for (Method method : methods)
        {
            List<Instruction> instructions = method.getInstructions();
            //Get opened cursors map, variable x num. times it was assigned a new cursor without being closed
            Map<Variable, Integer> openedCursors =
                    getOpenedCursors(method, sourceFile, instructions);

            //For each opened cursor fill a validation result
            for (Variable variable : openedCursors.keySet())
            {
                int openedCursor = openedCursors.get(variable);
                if (openedCursor > 0)
                {
                    addValidationResult(results, valManagerConfig, sourceFile, method, variable,
                            openedCursors);
                }
            }
        }
    }

    private Map<Variable, Integer> getOpenedCursors(Method method, SourceFileElement sourceFile,
            List<Instruction> instructions)
    {
        Map<Variable, Integer> openedCursors = new HashMap<Variable, Integer>();
        for (Instruction instruction : instructions)
        {
            if (instruction instanceof Invoke)
            {
                Invoke calledMethod = (Invoke) instruction;

                if ((calledMethod.getReturnType() != null)
                        && (calledMethod.getQualifiedName() != null))
                {
                    /*
                     * NOTE: There is a difference between the Java Model for Projects and APKs
                     * 
                     * For Projects, when an inherit method is called, it's qualified name contains the
                     * class which actually contains the method. 
                     * 
                     * For APKs, this do not occur, and the qualified name contains the name of the
                     * class itself.
                     * 
                     * Ex: calling android.app.Activity.startManagingCursor
                     * - For Projects: qualified name = android.app.Activity.startManagingCursor
                     * - For APKs: qualified name = <the user activity>.startManagingCursor
                     */

                    /*
                     * Opening cursors
                     */
                    if ((calledMethod.getReturnType() != null)
                            && (calledMethod.getReturnType().equals(DATABASE_CURSOR_QUALIFIED_NAME))
                            && (!calledMethod.getQualifiedName().endsWith(
                                    ((isProject) ? ACTIVITY_QUALIFIED_NAME : sourceFile
                                            .getClassFullPath()) + MANAGED_QUERY_METHOD_NAME)))
                    {
                        // Opened cursor found, increment counter
                        Variable variable = getAssignedVariable(method, calledMethod);
                        Integer counter = null;
                        if (variable != null)
                        {
                            if (openedCursors.containsKey(variable))
                            {
                                counter = openedCursors.get(variable);
                                counter++;
                            }
                            else
                            {
                                counter = 1;
                            }
                            openedCursors.put(variable, counter);
                        }
                    }

                    /*
                     * Closing cursors
                     */
                    String objectName = null;
                    //close() found
                    if (calledMethod.getQualifiedName().equals(
                            DATABASE_CURSOR_QUALIFIED_NAME + CLOSE_METHOD_NAME))
                    {

                        objectName = calledMethod.getObjectName();
                    }
                    // startManagingCursos(cursor) found - disregard this cursor
                    else if (calledMethod.getQualifiedName().endsWith(
                            ((isProject) ? ACTIVITY_QUALIFIED_NAME : sourceFile.getClassFullPath())
                                    + START_MANAGING_CURSOR_METHOD_NAME))
                    {
                        List<String> paramNames = calledMethod.getParameterNames();
                        if ((paramNames != null) && (paramNames.size() > 0))
                        {
                            objectName = paramNames.get(0);
                        }
                    }

                    // decrement the counter
                    if (objectName != null)
                    {
                        Variable key = getKey(openedCursors, objectName);
                        Integer counter = null;
                        if (openedCursors.containsKey(key))
                        {
                            counter = openedCursors.get(key);
                            counter--;
                            openedCursors.put(key, counter);
                        }
                        else
                        {
                            //Don't do anything. for some reason a close was called on a non local variable.
                            //In the future add support for parameters and fields
                        }
                    }
                }
                else
                {
                    PreflightingLogger
                            .error("Could not retrieve qualified name or return type for method: "
                                    + calledMethod);
                }
            }

        }
        return openedCursors;
    }

    /*
     * Search within openedCursors keySet for a variable with name that is equal to objectName
     */
    private Variable getKey(Map<Variable, Integer> openedCursors, String objectName)
    {
        Variable key = null;
        Iterator<Variable> it = openedCursors.keySet().iterator();

        while ((key == null) && it.hasNext())
        {
            Variable variable = it.next();
            if (variable.getName().equals(objectName))
            {
                key = variable;
            }
        }

        return key;
    }

    /*
     * Retrieve the Variable assigned with the return of calledMethod, if any.
     */
    private Variable getAssignedVariable(Method method, Invoke calledMethod)
    {
        String assignedVariable = calledMethod.getAssignedVariable();
        List<Variable> variables = method.getVariables();
        Variable variable = null;
        Iterator<Variable> it = variables.iterator();
        while ((variable == null) && it.hasNext())
        {
            Variable listVar = it.next();
            if (listVar.getName().equals(assignedVariable))
            {
                variable = listVar;
            }
        }

        return variable;
    }

    private void addValidationResult(ValidationResult results,
            ValidationManagerConfiguration valManagerConfig, SourceFileElement sourceFile,
            Method method, Variable variable, Map<Variable, Integer> openedCursors)
    {
        ValidationResultData resultData = new ValidationResultData();
        resultData.setConditionID(getId());

        resultData.addFileToIssueLines(sourceFile.getFile(), Arrays.asList(new Integer[]
        {
            variable.getLineNumber()
        }));

        resultData.setIssueDescription(CheckerNLS.bind(
                CheckerNLS.OpenedCursorsCondition_Result_Message, new String[]
                {
                        variable.getName(), sourceFile.getClassFullPath() + "." //$NON-NLS-1$
                                + method.getMethodName(), openedCursors.get(variable).toString()
                }));
        resultData.setQuickFixSuggestion(CheckerNLS.OpenedCursorsCondition_Result_QuickFix);
        resultData.setSeverity(getSeverityLevel());
        resultData.setInfoURL(ConditionUtils.getDescriptionLink(getChecker().getId(), getId(),
                valManagerConfig));
        results.addValidationResult(resultData);
    }
}
