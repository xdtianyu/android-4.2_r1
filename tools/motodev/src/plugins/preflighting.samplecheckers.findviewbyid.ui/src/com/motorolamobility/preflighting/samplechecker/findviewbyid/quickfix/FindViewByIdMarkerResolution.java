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
package com.motorolamobility.preflighting.samplechecker.findviewbyid.quickfix;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.motorola.studio.android.common.utilities.EclipseUtils;
import com.motorolamobility.preflighting.samplechecker.findviewbyid.quickfix.i18n.MessagesNLS;

/**
 * MarkerResolution responsible for moving the findViewByID call outside the loop.
 */
public class FindViewByIdMarkerResolution implements IMarkerResolution2
{

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    public String getLabel()
    {
        return MessagesNLS.FindViewByIdMarkerResolution_Label;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
     */
    public void run(IMarker marker)
    {
        IResource resource = marker.getResource();
        final ICompilationUnit iCompilationUnit =
                JavaCore.createCompilationUnitFrom((IFile) resource);

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setProject(JavaCore.create(resource.getProject()));
        parser.setResolveBindings(true);
        parser.setSource(iCompilationUnit);
        parser.setStatementsRecovery(true);
        parser.setBindingsRecovery(true);
        final CompilationUnit compUnit = (CompilationUnit) parser.createAST(null);

        try
        {
            final int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
            MethodInvocation invokedMethod = null;

            //Look for the invokedMethod that shall be moved
            invokedMethod = getMethodInvocation(compUnit, lineNumber, invokedMethod);

            IEditorPart editor = openEditor(iCompilationUnit);

            ASTNode loopNode = getLoopNode(invokedMethod);

            //Retrieve block parent for the loop statement.
            Block targetBlock = (Block) loopNode.getParent();
            List<Statement> statements = targetBlock.statements();
            int i = getLoopStatementIndex(loopNode, statements);

            //Add the node before the loop.
            compUnit.recordModifications();
            ASTNode invokedMethodStatement = getInvokedStatement(invokedMethod);
            final VariableDeclarationStatement varDeclarationStatement[] =
                    new VariableDeclarationStatement[1];

            //Verify if the invoke statement contains a variable attribution
            if (invokedMethodStatement instanceof ExpressionStatement)
            {
                ExpressionStatement expressionStatement =
                        (ExpressionStatement) invokedMethodStatement;
                Expression expression = expressionStatement.getExpression();
                if (expression instanceof Assignment)
                {
                    Expression leftHandSide = ((Assignment) expression).getLeftHandSide();
                    if (leftHandSide instanceof SimpleName) //Search for the variable declaration
                    {
                        SimpleName simpleName = (SimpleName) leftHandSide;
                        final String varName = simpleName.getIdentifier();

                        loopNode.accept(new ASTVisitor()
                        {
                            @Override
                            public boolean visit(VariableDeclarationStatement node) //Visit all variable declarations inside the loop looking for the variable which receives the findViewById result
                            {
                                List<VariableDeclarationFragment> fragments = node.fragments();
                                for (VariableDeclarationFragment fragment : fragments)
                                {
                                    if (fragment.getName().getIdentifier().equals(varName))
                                    {
                                        varDeclarationStatement[0] = node;
                                        break;
                                    }
                                }
                                return super.visit(node);
                            }
                        });
                    }
                }
            }

            //Variable is declared inside the loop, now let's move the variable declaration if needed
            if (varDeclarationStatement[0] != null)
            {
                ASTNode varDeclarationSubTree =
                        ASTNode.copySubtree(targetBlock.getAST(), varDeclarationStatement[0]);
                statements.add(i, (Statement) varDeclarationSubTree.getRoot());

                //Delete the node inside loop.
                varDeclarationStatement[0].delete();
                i++;
            }
            ASTNode copySubtree = ASTNode.copySubtree(targetBlock.getAST(), invokedMethodStatement);
            statements.add(i, (Statement) copySubtree.getRoot());

            //Delete the node inside loop.
            invokedMethodStatement.delete();

            // apply changes to file
            final Map<?, ?> mapOptions = JavaCore.create(resource.getProject()).getOptions(true);
            final IDocument document =
                    ((AbstractTextEditor) editor).getDocumentProvider().getDocument(
                            editor.getEditorInput());
            PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable()
            {

                public void run()
                {
                    try
                    {
                        TextEdit edit = compUnit.rewrite(document, mapOptions);
                        iCompilationUnit.applyTextEdit(edit, new NullProgressMonitor());
                    }
                    catch (JavaModelException e)
                    {
                        EclipseUtils.showErrorDialog(
                                MessagesNLS.FindViewByIdMarkerResolution_Error_Msg_Title,
                                MessagesNLS.FindViewByIdMarkerResolution_Error_Aplying_Changes
                                        + e.getMessage());
                    }

                }
            });

            marker.delete();

        }
        catch (Exception e)
        {
            EclipseUtils.showErrorDialog(MessagesNLS.FindViewByIdMarkerResolution_Error_Msg_Title,
                    MessagesNLS.FindViewByIdMarkerResolution_Error_Could_Not_Fix_Code);
        }
    }

    private IEditorPart openEditor(final ICompilationUnit iCompilationUnit)
    {
        IEditorPart editor = null;
        try
        {
            editor = JavaUI.openInEditor(iCompilationUnit);
        }
        catch (Exception e)
        {
            EclipseUtils.showErrorDialog(MessagesNLS.FindViewByIdMarkerResolution_Error_Msg_Title,
                    MessagesNLS.FindViewByIdMarkerResolution_Error_Unable_To_Open_Editor);
        }
        return editor;
    }

    private MethodInvocation getMethodInvocation(final CompilationUnit compUnit,
            final int lineNumber, MethodInvocation invokedMethod)
    {
        final MethodInvocation[] tempMethodInvocation = new MethodInvocation[1];
        compUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(MethodInvocation node)
            {
                if (compUnit.getLineNumber(node.getStartPosition()) == lineNumber)
                {
                    tempMethodInvocation[0] = node;
                }
                return super.visit(node);
            };
        });
        if (tempMethodInvocation[0] != null)
        {
            invokedMethod = tempMethodInvocation[0];
        }
        return invokedMethod;
    }

    /*
     * Look for Statement containing the invokedMethod
     */
    private ASTNode getInvokedStatement(MethodInvocation invokedMethod)
    {
        boolean found = false;
        ASTNode parent = invokedMethod.getParent();
        do
        {
            if ((parent instanceof Statement))
            {
                found = true;
            }
            else
            {
                parent = parent.getParent();
            }
        }
        while (!found);
        return parent;
    }

    /*
     * Resturns the index of the loop statement, inside a list of statements
     */
    private int getLoopStatementIndex(ASTNode loopNode, List<Statement> statements)
    {
        int i = 0;
        for (i = 0; i < statements.size(); i++)
        {
            Statement statement = statements.get(i);
            if (statement.equals(loopNode))
            {
                break;
            }
        }
        return i;
    }

    /*
     * Find the loop node that contains the called method
     */
    private ASTNode getLoopNode(MethodInvocation invokedMethod)
    {
        boolean found = false;
        ASTNode parent = invokedMethod.getParent();
        do
        {
            parent = parent.getParent();
            if ((parent instanceof ForStatement) || (parent instanceof DoStatement)
                    || (parent instanceof WhileStatement)
                    || (parent instanceof EnhancedForStatement))
            {
                found = true;
            }
        }
        while (!found);
        return parent;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution2#getDescription()
     */
    public String getDescription()
    {
        return MessagesNLS.FindViewByIdMarkerResolution_Description;
    }

    public Image getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
