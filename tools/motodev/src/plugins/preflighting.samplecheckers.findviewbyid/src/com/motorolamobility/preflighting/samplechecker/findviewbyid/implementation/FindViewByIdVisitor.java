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

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.utils.CheckerUtils;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.samplechecker.findviewbyid.i18n.Messages;

/**
 * Visitor specialized to identify <code>findViewById</code> statements inside loops.
 */
public class FindViewByIdVisitor extends ASTVisitor
{

    private static final String R_CONSTANT = "R."; //$NON-NLS-1$

    private static final String FIND_VIEW_BY_ID_METHOD_BINDING =
            "public android.view.View findViewById(int)"; //$NON-NLS-1$

    private final ValidationResult results;

    private final CompilationUnit compilationUnit;

    private final String id;

    private final SEVERITY severityLevel;

    private String markerType;

    /**
     * Construct a new FindViewByIdVisitor with the given parameters.
     * 
     * @param id the condition id
     * @param severityLevel the condition default severity level
     * @param markerType object to keep the checker results
     * @param results 
     * @param valManagerConfig manager responsible to format output of App Validator
     * @param compilationUnit object representing the source code to be analyzed
     */
    public FindViewByIdVisitor(String id, SEVERITY severityLevel, String markerType,
            ValidationResult results, CompilationUnit compilationUnit)
    {
        this.id = id;
        this.severityLevel = severityLevel;
        this.markerType = markerType;
        this.results = results;
        this.compilationUnit = compilationUnit;
    }

    /**
     * Visit method invocations to find invocation of <code>findViewById</code>.
     * 
     * @param invoked method that is being called and will be analyzed to check if it is a <code>findViewById</code> call. 
     */
    @Override
    public boolean visit(MethodInvocation invoked)
    {
        //find the signature of the method that is being called
        IMethodBinding methodBinding = invoked.resolveMethodBinding();
        if (methodBinding != null)
        {
            if (methodBinding.toString().trim().equalsIgnoreCase(FIND_VIEW_BY_ID_METHOD_BINDING))
            {
                //according to the signature, findViewById was called
                ASTNode parentNode = invoked.getParent();
                Object firstElement = invoked.arguments().get(0);
                if (firstElement != null && firstElement.toString() != null
                        && firstElement.toString().startsWith(R_CONSTANT))
                {
                    //argument has a constant R. (indicating access that could be possibly done outside the loop)
                    if (hasLoopStatementAsParent(parentNode))
                    {
                        //print in the console (if DEBUG level set for verbosity of App Validator output)
                        PreflightingLogger
                                .debug("Found findViewById invocation inside loop statement");

                        //call is inside a loop statement - raise issue                            
                        ValidationResultData validationResult = createResult(invoked);
                        results.addValidationResult(validationResult);
                    }
                }
            }
        }
        return super.visit(invoked);
    }

    /**
     * Recursively check if <code>node</code> has a loop statement as parent.
     * @param node to check if parent is a loop statement 
     * @return <code>true</code> if finds (<code>for, extended for, while, do-while</code>) as parent of <code>node</code>, <code>false</code> otherwise
     */
    private boolean hasLoopStatementAsParent(ASTNode node)
    {
        if (node == null)
        {
            return false;
        }
        else
        {
            ASTNode parentNode = node.getParent();
            if (parentNode == null || parentNode instanceof MethodDeclaration)
            {
                //base case of recursion: reached top level (method declaration or class declaration) without finding a loop statement
                return false;
            }
            else if (isLoopStatement(parentNode))
            {
                //base case of recursion: reached loop statement 
                return true;
            }
            else
            {
                //continue search : go to the parent node                    
                return hasLoopStatementAsParent(parentNode);
            }
        }
    }

    /**
     * Check if the {@link ASTNode} is a loop block (<code>for, extended for, while, do-while</code>).
     * 
     * @param statement node to verify
     * @return <code>true</code> if (<code>for, extended for, while, do-while</code>), <code>false</code> otherwise
     * 
     * @see org.eclipse.jdt.core.dom.ForStatement
     * @see org.eclipse.jdt.core.dom.DoStatement
     * @see org.eclipse.jdt.core.dom.WhileStatement
     * @see org.eclipse.jdt.core.dom.EnhancedForStatement
     */
    private boolean isLoopStatement(ASTNode statement)
    {
        return statement instanceof ForStatement || statement instanceof DoStatement
                || statement instanceof WhileStatement || statement instanceof EnhancedForStatement;
    }

    /**
     * Create the App Validator issues found for the checker.
     * 
     * @param invoked method where the problem occurs
     * @return data containing the issue
     */
    private ValidationResultData createResult(MethodInvocation invoked)
    {
        ValidationResultData resultData = new ValidationResultData();

        //set the condition related to the problem
        resultData.setConditionID(id);
        resultData.setMarkerType(markerType);

        //set the lines where the problem occurred
        ArrayList<Integer> lines = new ArrayList<Integer>();
        int issuedLine = compilationUnit.getLineNumber(invoked.getStartPosition());
        if (issuedLine != -1)
        {
            lines.add(issuedLine);
        }

        //set the source file associated with the issue
        File javaFile = (File) compilationUnit.getProperty(CheckerUtils.JAVA_FILE_PROPERTY);
        resultData.addFileToIssueLines(javaFile, lines);

        //set description, quick fix, and severity level
        resultData.setIssueDescription(Messages.FindViewByIdInsideLoopCondition_IssueDescription);
        resultData
                .setQuickFixSuggestion(Messages.FindViewByIdInsideLoopCondition_QuickFixSuggestion);
        resultData.setSeverity(severityLevel);

        //set the URL with the help associated with developer page regarding this checker
        resultData
                .setInfoURL("http://developer.motorola.com/docstools/library/motodev-app-validator/#unnecessaryFindViewById-unnecessaryFindViewByIdInsideLoops");
        return resultData;
    }
}