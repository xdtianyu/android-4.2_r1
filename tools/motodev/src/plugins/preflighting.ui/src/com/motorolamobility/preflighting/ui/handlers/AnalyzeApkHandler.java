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

package com.motorolamobility.preflighting.ui.handlers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.motorola.studio.android.common.log.StudioLogger;
import com.motorola.studio.android.common.utilities.AndroidUtils;
import com.motorolamobility.preflighting.core.exception.PreflightingParameterException;
import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.core.validation.ApplicationValidationResult;
import com.motorolamobility.preflighting.core.validation.Parameter;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;
import com.motorolamobility.preflighting.core.validation.ValidationResult;
import com.motorolamobility.preflighting.core.validation.ValidationResultData;
import com.motorolamobility.preflighting.core.validation.ValidationResultData.SEVERITY;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;
import com.motorolamobility.preflighting.core.verbose.WarningLevelFilter;
import com.motorolamobility.preflighting.internal.commandinput.ApplicationParameterInterpreter;
import com.motorolamobility.preflighting.internal.commandinput.CommandLineInputProcessor;
import com.motorolamobility.preflighting.internal.commandinput.exception.ParameterParseException;
import com.motorolamobility.preflighting.internal.commandoutput.OutputterFactory;
import com.motorolamobility.preflighting.output.AbstractOutputter;
import com.motorolamobility.preflighting.ui.PreflightingUIPlugin;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;
import com.motorolamobility.preflighting.ui.utilities.EclipseUtils;

public class AnalyzeApkHandler extends AbstractHandler
{

    /**
     * Newline character
     */
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Identifier for AppValidator default marker
     */
    public final static String DEFAULT_APP_VALIDATOR_MARKER_TYPE =
            "com.motorolamobility.preflighting.checkers.ui.appValidatorMarker"; //$NON-NLS-1$

    /**
     * Problems view ID
     */
    private final static String PROBLEMS_VIEW_ID = "org.eclipse.ui.views.ProblemView"; //$NON-NLS-1$

    /**
     * App validator exit code
     */
    private final int EXIT_CODE_OK = 0;

    private final int EXIT_CODE_TOOL_CONTEXT_ERROR = 1;

    private final int EXIT_APPLICATION_CONTEXT_ERROR = 2;

    /**
     * The resource being analyzed (project or APK).
     */
    private StructuredSelection initialSelection = null;

    /**
     * True if file is inside workspace, false otherwise (because markers can not be inserted)
     */
    private boolean enableMarkers = true;

    private boolean downgradeErrors = true;

    private final IProgressMonitor monitor = null;

    public AnalyzeApkHandler()
    {
    }

    public AnalyzeApkHandler(StructuredSelection selection)
    {
        //initialize items (applications) to be validated
        this.initialSelection = selection;
    }

    /**
     * Jobs that runs one app validation
     */
    private class PreFlightJob extends Job
    {
        private final String path;

        private final String sdkPath;

        private String strOutput = ""; //$NON-NLS-1$

        private final IResource analyzedResource;

        private int exitCode = EXIT_CODE_OK;

        /**
         * @param name
         * @param stream 
         * @param sdkPath 
         */
        private PreFlightJob(String path, String sdkPath, IResource analyzedResource)
        {
            super(PreflightingUiNLS.AnalyzeApkHandler_AppValidatorJobName);
            this.path = path;
            this.sdkPath = sdkPath;
            this.analyzedResource = analyzedResource;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            StringBuilder strbValidationOutput = new StringBuilder(""); //$NON-NLS-1$

            exitCode =
                    executePreFlightingTool(path, sdkPath, analyzedResource, strbValidationOutput);
            strOutput = strbValidationOutput.toString();

            return Status.OK_STATUS;
        }

        public String getOutput()
        {
            return strOutput;
        }

        public int getExitCode()
        {
            return exitCode;
        }
    }

    /**
     * Job that runs several app validations (one PreFlightJob each time)
     *
     */
    private class ParentJob extends Job
    {
        ArrayList<PreFlightJob> jobList;

        public ParentJob(String name, ArrayList<PreFlightJob> jobList)
        {
            super(name);
            this.jobList = jobList;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            OutputStream console = getActiveConsole();
            PrintStream stream = null;

            try
            {
                stream = new PrintStream(console);

                monitor.beginTask(PreflightingUiNLS.ApplicationValidation_monitorTaskName,
                        jobList.size() + 1);
                monitor.worked(1);
                for (PreFlightJob job : jobList)
                {
                    try
                    {
                        job.schedule();
                        job.join();
                    }
                    catch (InterruptedException e)
                    {
                        //Do nothing
                    }
                    monitor.worked(1);
                    String currentJobOutput = job.getOutput();
                    stream.println(currentJobOutput);

                    //there is no need to print error messages several times
                    if (job.getExitCode() == EXIT_CODE_TOOL_CONTEXT_ERROR)
                    {
                        break;
                    }
                }
            }
            finally
            {
                stream.flush();
                stream.close();
                monitor.done();
            }

            return Status.OK_STATUS;
        }
    }

    public static final String CONSOLE_ID = "analyze_apk_console"; //$NON-NLS-1$

    private static final String QUICK_FIX_ID = "QuickFix";

    /*
     * (non-Javadoc)
     * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    public Object execute(ExecutionEvent event) throws ExecutionException
    {

        IWorkbench workbench = PlatformUI.getWorkbench();
        if ((workbench != null) && !workbench.isClosing())
        {
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window != null)
            {
                ISelection selection = null;
                if (initialSelection != null)
                {
                    selection = initialSelection;
                }
                else
                {
                    selection = window.getSelectionService().getSelection();
                }

                if (selection instanceof IStructuredSelection)
                {
                    IStructuredSelection sselection = (IStructuredSelection) selection;
                    Iterator<?> it = sselection.iterator();
                    String sdkPath = AndroidUtils.getSDKPathByPreference();
                    if (monitor != null)
                    {
                        monitor.setTaskName(PreflightingUiNLS.ApplicationValidation_monitorTaskName);
                        monitor.beginTask(PreflightingUiNLS.ApplicationValidation_monitorTaskName,
                                sselection.size() + 1);
                        monitor.worked(1);
                    }

                    ArrayList<PreFlightJob> jobList = new ArrayList<PreFlightJob>();
                    boolean isHelpExecution = false;

                    IPreferenceStore preferenceStore =
                            PreflightingUIPlugin.getDefault().getPreferenceStore();

                    boolean showMessageDialog = true;
                    if (preferenceStore.contains(PreflightingUIPlugin.SHOW_BACKWARD_DIALOG
                            + PreflightingUIPlugin.TOGGLE_DIALOG))
                    {
                        showMessageDialog =
                                MessageDialogWithToggle.ALWAYS.equals(preferenceStore
                                        .getString(PreflightingUIPlugin.SHOW_BACKWARD_DIALOG
                                                + PreflightingUIPlugin.TOGGLE_DIALOG));
                    }

                    if (showMessageDialog
                            && (!preferenceStore.contains(PreflightingUIPlugin.OUTPUT_LIMIT_VALUE))
                            && preferenceStore
                                    .contains(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY)
                            && (!(preferenceStore
                                    .getString(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY))
                                    .equals(PreflightingUIPlugin.DEFAULT_BACKWARD_COMMANDLINE)))
                    {
                        String commandLine =
                                PreflightingUIPlugin
                                        .getDefault()
                                        .getPreferenceStore()
                                        .getString(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY);
                        MessageDialogWithToggle dialog =
                                MessageDialogWithToggle
                                        .openYesNoQuestion(
                                                PlatformUI.getWorkbench()
                                                        .getActiveWorkbenchWindow().getShell(),
                                                PreflightingUiNLS.AnalyzeApkHandler_BackwardMsg_Title,
                                                NLS.bind(
                                                        PreflightingUiNLS.AnalyzeApkHandler_BackwardMsg_Message,
                                                        commandLine),
                                                PreflightingUiNLS.AnalyzeApkHandler_Do_Not_Show_Again,
                                                false, preferenceStore,
                                                PreflightingUIPlugin.SHOW_BACKWARD_DIALOG
                                                        + PreflightingUIPlugin.TOGGLE_DIALOG);

                        int returnCode = dialog.getReturnCode();
                        if (returnCode == IDialogConstants.YES_ID)
                        {
                            EclipseUtils.openPreference(PlatformUI.getWorkbench()
                                    .getActiveWorkbenchWindow().getShell(),
                                    PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_PAGE);
                        }
                    }

                    String userParamsStr =
                            preferenceStore
                                    .getString(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY);

                    downgradeErrors =
                            preferenceStore
                                    .contains(PreflightingUIPlugin.ECLIPSE_PROBLEM_TO_WARNING_VALUE)
                                    ? preferenceStore
                                            .getBoolean(PreflightingUIPlugin.ECLIPSE_PROBLEM_TO_WARNING_VALUE)
                                    : true;

                    //we look for a help parameter: -help or -list-checkers
                    //in such case we execute app validator only once, 
                    //since all executions will have the same output
                    if (userParamsStr.length() > 0)
                    {
                        String regex = "((?!(\\s+" + "-" + ")).)*"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        Pattern pat = Pattern.compile(regex);
                        Matcher matcher = pat.matcher(userParamsStr);
                        while (matcher.find())
                        {
                            String parameterValues =
                                    userParamsStr.substring(matcher.start(), matcher.end());
                            if (parameterValues.equals("-" //$NON-NLS-1$
                                    + ApplicationParameterInterpreter.PARAM_HELP)
                                    || parameterValues.equals("-" //$NON-NLS-1$
                                            + ApplicationParameterInterpreter.PARAM_LIST_CHECKERS))
                            {
                                isHelpExecution = true;
                            }
                        }
                    }
                    while (it.hasNext())
                    {
                        IResource analyzedResource = null;
                        Object resource = it.next();
                        String path = null;
                        if (resource instanceof IFile)
                        {
                            IFile apkfile = (IFile) resource;
                            analyzedResource = apkfile;
                            if (apkfile.getFileExtension().equals("apk") && apkfile.exists() //$NON-NLS-1$
                                    && apkfile.getLocation().toFile().canRead())
                            {
                                /*
                                 * For each apk, execute all verifications passaing the two needed parameters
                                 */
                                path = apkfile.getLocation().toOSString();
                            }
                            else
                            {
                                MessageDialog.openError(window.getShell(),
                                        PreflightingUiNLS.AnalyzeApkHandler_ErrorTitle,
                                        PreflightingUiNLS.AnalyzeApkHandler_ApkFileErrorMessage);
                            }
                        }
                        else if (resource instanceof File)
                        {
                            File apkfile = (File) resource;

                            if (apkfile.getName().endsWith(".apk") && apkfile.exists() //$NON-NLS-1$
                                    && apkfile.canRead())
                            {
                                /*
                                 * For each apk, execute all verifications passaing the two needed parameters
                                 */
                                path = apkfile.getAbsolutePath();
                            }
                            else
                            {
                                MessageDialog.openError(window.getShell(),
                                        PreflightingUiNLS.AnalyzeApkHandler_ErrorTitle,
                                        PreflightingUiNLS.AnalyzeApkHandler_ApkFileErrorMessage);
                            }
                            enableMarkers = false;
                        }
                        else if (resource instanceof IProject)
                        {
                            IProject project = (IProject) resource;
                            analyzedResource = project;
                            path = project.getLocation().toOSString();
                        }
                        else if (resource instanceof IAdaptable)
                        {
                            IAdaptable adaptable = (IAdaptable) resource;
                            IProject project = (IProject) adaptable.getAdapter(IProject.class);
                            analyzedResource = project;

                            if (project != null)
                            {
                                path = project.getLocation().toOSString();
                            }
                        }

                        if (path != null)
                        {
                            PreFlightJob job = new PreFlightJob(path, sdkPath, analyzedResource);
                            jobList.add(job);
                            if (isHelpExecution)
                            {
                                //app validator is executed only once for help commands
                                break;
                            }
                        }
                    }

                    if (enableMarkers)
                    {
                        // Show and activate problems view
                        Runnable showProblemsView = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                            .getActivePage().showView(PROBLEMS_VIEW_ID);
                                }
                                catch (PartInitException e)
                                {
                                    PreflightingLogger.error("Error showing problems view."); //$NON-NLS-1$
                                }

                            }
                        };

                        Display.getDefault().asyncExec(showProblemsView);
                    }
                    //show console for external apks
                    else
                    {
                        showApkConsole();
                    }

                    ParentJob parentJob =
                            new ParentJob(
                                    PreflightingUiNLS.AnalyzeApkHandler_PreflightingToolNameMessage,
                                    jobList);
                    parentJob.setUser(true);
                    parentJob.schedule();

                    try
                    {
                        if (monitor != null)
                        {
                            monitor.done();
                        }
                    }
                    catch (Exception e)
                    {
                        //Do nothing
                    }
                }
            }
        }

        return null;
    }

    private int executePreFlightingTool(String path, String sdkPath, IResource analyzedResource,
            StringBuilder strbValidationOutput)
    {
        int returnValue = EXIT_CODE_OK;
        List<Parameter> parameters = null;

        //Remove app validator markers every time.
        if (enableMarkers)
        {
            cleanMarkers(analyzedResource);
        }

        // set default warning and verbosity levels
        ByteArrayOutputStream verboseOutputStream = new ByteArrayOutputStream();
        PrintStream prtStream = new PrintStream(verboseOutputStream);
        DebugVerboseOutputter.setStream(prtStream);

        DebugVerboseOutputter.setCurrentVerboseLevel(DebugVerboseOutputter.DEFAULT_VERBOSE_LEVEL);
        WarningLevelFilter.setCurrentWarningLevel(WarningLevelFilter.DEFAULT_WARNING_LEVEL);

        DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v0); //$NON-NLS-1$
        DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v0); //$NON-NLS-1$
        DebugVerboseOutputter.printVerboseMessage(PreflightingUiNLS.AnalyzeApkHandler_Header,
                VerboseLevel.v0);
        DebugVerboseOutputter.printVerboseMessage("", VerboseLevel.v0); //$NON-NLS-1$

        IPreferenceStore preferenceStore = PreflightingUIPlugin.getDefault().getPreferenceStore();
        String userParamsStr = null;
        if (preferenceStore.contains(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY))
        {
            userParamsStr =
                    preferenceStore.getString(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY);
        }
        else
        {
            userParamsStr = PreflightingUIPlugin.DEFAULT_COMMANDLINE;
        }

        try
        {
            if (userParamsStr.length() > 0)
            {
                CommandLineInputProcessor commandLineInputProcessor =
                        new CommandLineInputProcessor();
                parameters = commandLineInputProcessor.processCommandLine(userParamsStr);
            }
            else
            {
                parameters = new ArrayList<Parameter>(5);
            }

            parameters.add(new Parameter(ValidationManager.InputParameter.APPLICATION_PATH
                    .getAlias(), path));

            //If user selected a different SDK, let's use their definition, otherwise we'll pick the SDK currently set
            Parameter sdkParam =
                    new Parameter(ValidationManager.InputParameter.SDK_PATH.getAlias(), null);
            if (!parameters.contains(sdkParam))
            {
                parameters.add(new Parameter(ValidationManager.InputParameter.SDK_PATH.getAlias(),
                        sdkPath));
            }

            //If user selected the help, list-checker, list-devices or describe-device parameter, let's clear and use only those parameters
            Parameter helpParam = new Parameter(ApplicationParameterInterpreter.PARAM_HELP, null);
            boolean hasHelpParam = parameters.contains(helpParam);
            Parameter listCheckersParam =
                    new Parameter(ApplicationParameterInterpreter.PARAM_LIST_CHECKERS, null);
            boolean hasListCheckersParam = parameters.contains(listCheckersParam);
            Parameter describeDeviceParam =
                    new Parameter(ApplicationParameterInterpreter.PARAM_DESC_DEVICE, null);
            boolean hasDescribeDeviceParam = parameters.contains(describeDeviceParam);
            Parameter listDevicesParam =
                    new Parameter(ApplicationParameterInterpreter.PARAM_LIST_DEVICES, null);
            boolean hasListDevicesParam = parameters.contains(listDevicesParam);

            if (hasHelpParam || hasListCheckersParam || hasDescribeDeviceParam
                    || hasListDevicesParam)
            {
                int neededParamIdx =
                        hasHelpParam ? parameters.indexOf(helpParam) : hasListCheckersParam
                                ? parameters.indexOf(listCheckersParam) : hasDescribeDeviceParam
                                        ? parameters.indexOf(describeDeviceParam) : parameters
                                                .indexOf(listDevicesParam);
                Parameter parameter = parameters.get(neededParamIdx);
                parameters.clear();
                parameters.add(parameter);
                // Show console
                showApkConsole();
            }

            List<Parameter> parametersCopy = new ArrayList<Parameter>(parameters);

            AbstractOutputter outputter = null;
            for (Parameter param : parametersCopy)
            {
                if (InputParameter.OUTPUT.getAlias().equals(param.getParameterType()))
                {
                    ApplicationParameterInterpreter.validateOutputParam(param.getValue());
                    outputter = OutputterFactory.getInstance().createOutputter(parameters);
                    parameters.remove(param);
                    break;
                }
            }

            if (outputter == null)
            {
                outputter = OutputterFactory.getInstance().createOutputter(null);
            }

            ValidationManager validationManager = new ValidationManager();

            if (userParamsStr.length() > 0)
            {
                ApplicationParameterInterpreter.checkApplicationParameters(parameters,
                        validationManager, prtStream);
            }

            if (!hasHelpParam && !hasListCheckersParam && !hasDescribeDeviceParam
                    && !hasListDevicesParam)
            {
                List<ApplicationValidationResult> results = validationManager.run(parameters);
                ApplicationValidationResult result = null;

                //inside studio, there won't be any support zip files, thus the map will have only one result 
                for (ApplicationValidationResult aResult : results)
                {
                    result = aResult;
                }

                strbValidationOutput.append(verboseOutputStream.toString());
                if (enableMarkers && (analyzedResource != null) && (result != null))
                {
                    // Create problem markers
                    createProblemMarkers(result.getResults(), analyzedResource);
                }

                if (result != null)
                {
                    ByteArrayOutputStream baos = null;
                    try
                    {
                        baos = new ByteArrayOutputStream();
                        outputter.print(result, baos, parameters);
                        strbValidationOutput.append(baos.toString());
                    }
                    finally
                    {
                        try
                        {
                            baos.close();
                        }
                        catch (IOException e)
                        {
                            StudioLogger.error("Could not close stream: ", e.getMessage());
                        }
                    }

                    if ((result.getResults().size() > 0))
                    {
                        //result already used
                        //clean it to help Garbage Collector to work freeing memory
                        result.getResults().clear();
                        result = null;
                        Runtime.getRuntime().gc();
                    }

                }

            }
            else
            {
                strbValidationOutput.append(verboseOutputStream.toString());
            }

            try
            {
                verboseOutputStream.flush();
            }
            catch (IOException e)
            {
                StudioLogger.error("Could not flush stream: ", e.getMessage());
            }
        }
        catch (PreflightingParameterException pe)
        {
            // only log, do not print message (ValidationManager.run() method already prints
            // each problem individually)
            PreflightingLogger.error(AnalyzeApkHandler.class,
                    "Parameter problems executing MOTODEV Studio Application Validator", pe); //$NON-NLS-1$
            strbValidationOutput.append(verboseOutputStream.toString());
            // Show console
            showApkConsole();
            returnValue = EXIT_CODE_TOOL_CONTEXT_ERROR;
        }
        catch (PreflightingToolException e)
        {
            PreflightingLogger.error(AnalyzeApkHandler.class,
                    "An error ocurred trying to execute MOTODEV Studio Application Validator", e); //$NON-NLS-1$

            strbValidationOutput.append(verboseOutputStream.toString());
            strbValidationOutput
                    .append(PreflightingUiNLS.AnalyzeApkHandler_PreflightingApplicationExecutionErrorMessage);
            strbValidationOutput.append(e.getMessage());
            strbValidationOutput.append(NEWLINE);
            returnValue = EXIT_APPLICATION_CONTEXT_ERROR;
            // Show console
            showApkConsole();

        }
        catch (ParameterParseException e)
        {
            PreflightingLogger.error(AnalyzeApkHandler.class,
                    "An error ocurred trying to execute MOTODEV Studio Application Validator", e); //$NON-NLS-1$

            strbValidationOutput.append(verboseOutputStream.toString());
            strbValidationOutput
                    .append(PreflightingUiNLS.AnalyzeApkHandler_PreflightingApplicationExecutionErrorMessage);
            strbValidationOutput.append(e.getMessage());
            strbValidationOutput.append(NEWLINE);

            // Show console
            showApkConsole();
            returnValue = EXIT_CODE_TOOL_CONTEXT_ERROR;
        }
        finally
        {
            try
            {
                verboseOutputStream.close();
                prtStream.close();
            }
            catch (IOException e)
            {
                StudioLogger.error("Could not close stream: ", e.getMessage());
            }
        }

        return returnValue;
    }

    /**
     * Get the output stream of existent apk analysis console. Create one if needed.
     * @return the output stream
     */
    private OutputStream getActiveConsole()
    {
        IConsole outputConsole = null;
        for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles())
        {
            if (CONSOLE_ID.equals(console.getType()))
            {
                outputConsole = console;
            }
        }

        if (outputConsole == null)
        {
            outputConsole =
                    new IOConsole(PreflightingUiNLS.AnalyzeApkHandler_PreflightingToolNameMessage,
                            CONSOLE_ID, null);
            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]
            {
                outputConsole
            });
        }

        IOConsoleOutputStream stream = ((IOConsole) outputConsole).newOutputStream();

        return stream;
    }

    /**
     * Bring the Apk console to the front. Create it if necessary.
     */
    private void showApkConsole()
    {
        IConsole outputConsole = null;
        for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles())
        {
            if (CONSOLE_ID.equals(console.getType()))
            {
                outputConsole = console;
            }
        }

        if (outputConsole == null)
        {
            outputConsole =
                    new IOConsole(PreflightingUiNLS.AnalyzeApkHandler_PreflightingToolNameMessage,
                            CONSOLE_ID, null);
            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]
            {
                outputConsole
            });
        }

        // Show console
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(outputConsole);

    }

    /**
     *  This method will create the markers that will be shown by the "Problems" 
     *  view based on the results of the AppValidator.
     */
    private void createProblemMarkers(List<ValidationResult> resultList, IResource analyzedResource)
    {

        cleanMarkers(analyzedResource);

        // Check if resource analyzed is a APK or not
        boolean isResourceAPK = false;

        if (analyzedResource instanceof IFile)
        {
            if (((IFile) analyzedResource).getFileExtension().equals("apk")) //$NON-NLS-1$
            {
                isResourceAPK = true;
            }
        }

        // Iterate through the results
        for (ValidationResult result : resultList)
        {
            for (ValidationResultData data : result.getValidationResult())
            {
                createMarkerBasedOnResultData(data, isResourceAPK, analyzedResource);
            }
        }
    }

    /**
     * Remove all MARKER_APP_VALIDATOR_TYPE markers
     */
    private void cleanMarkers(IResource analyzedResource)
    {
        try
        {
            if (analyzedResource != null)
            {
                analyzedResource.deleteMarkers(DEFAULT_APP_VALIDATOR_MARKER_TYPE, true,
                        IResource.DEPTH_INFINITE);
            }
        }
        catch (CoreException e)
        {
            // An error ocurred while cleaning the markers
            PreflightingLogger.error(AnalyzeApkHandler.class,
                    "Error cleaning problem markers for the analyzed resource: " //$NON-NLS-1$
                            + analyzedResource.getFullPath().toOSString());
        }
    }

    /**
     * Auxiliary method to create markers based on a validation result data.
     * @param data The validation result data
     * @param isResourceAPK boolean determining if the resource being analyzed is an APK or not
     */
    private void createMarkerBasedOnResultData(ValidationResultData data, boolean isResourceAPK,
            IResource analyzedResource)
    {
        // Create a marker for each result data (only errors and warnings)
        if ((data.getSeverity() == SEVERITY.FATAL) || (data.getSeverity() == SEVERITY.ERROR)
                || (data.getSeverity() == SEVERITY.WARNING))
        {

            /*
             * Now comes the tricky part. The result data can either have files and lines associated with it or not.
             * If the resource is an apk, ignore that.
             */
            if ((!isResourceAPK) && (data.getFileToIssueLines() != null)
                    && (data.getFileToIssueLines().size() > 0))
            {
                // Create a marker for each line in a file
                for (File f : data.getFileToIssueLines().keySet())
                {

                    IWorkspace workspace = ResourcesPlugin.getWorkspace();
                    IPath location = Path.fromOSString(f.getAbsolutePath());

                    IResource markerResource = workspace.getRoot().getFileForLocation(location);

                    // Convert to IFolder type if necessary
                    if (f.isDirectory())
                    {
                        markerResource =
                                workspace.getRoot().getFolder(markerResource.getFullPath());
                    }

                    if ((markerResource != null) && markerResource.exists())
                    {
                        // Iterate through the lines and create markers. If no lines are associated with the file, don't provide a line value
                        List<Integer> lineList = data.getFileToIssueLines().get(f);

                        if ((lineList != null) && (lineList.size() > 0))
                        {
                            // Create a marker for each line
                            for (Integer l : lineList)
                            {
                                createMarker(markerResource, data, l);
                            }
                        }
                        else
                        {
                            createMarker(markerResource, data, -1);
                        }

                    }

                }
            }
            else
            {
                /*
                 *  No files are associated with the result data. It's either an apk or a problem with no related file.
                 *  In any case, mark the resource being analyzed by the App validator tool
                 */
                createMarker(analyzedResource, data, -1);
            }

        }
    }

    /**
     * Auxiliary method to create markers. If lineNumber is less than 0, ignore it.
     * @param resource The resource to be marked.
     * @param data The set of information used to create the marker.
     * @param lineNumber The line number.
     */
    @SuppressWarnings("incomplete-switch")
    private void createMarker(IResource resource, ValidationResultData data, int lineNumber)
    {
        // Get problem message: error description + quickfix suggestion
        String markerMessage = data.getIssueDescription() + " " + data.getQuickFixSuggestion(); //$NON-NLS-1$

        // Determine the severity
        int markerSeverity = 0;

        if (downgradeErrors)
        {
            //every error is going to be addressed as an warning
            markerSeverity = IMarker.SEVERITY_WARNING;
        }
        else
        {
            switch (data.getSeverity())
            {
                case FATAL:
                    markerSeverity = IMarker.SEVERITY_ERROR;
                    break;
                case ERROR:
                    markerSeverity = IMarker.SEVERITY_ERROR;
                    break;
                case WARNING:
                    markerSeverity = IMarker.SEVERITY_WARNING;
                    break;
            }
        }

        try
        {
            String markerType =
                    data.getMarkerType() == null
                            ? AnalyzeApkHandler.DEFAULT_APP_VALIDATOR_MARKER_TYPE : data
                                    .getMarkerType();

            // Create marker
            IMarker marker = resource.createMarker(markerType);

            // Set description
            marker.setAttribute(IMarker.MESSAGE, markerMessage);

            // Set severity
            marker.setAttribute(IMarker.SEVERITY, markerSeverity);

            // Set extra data used to fix the problem
            marker.setAttribute(QUICK_FIX_ID, data.getExtra());

            if (lineNumber >= 0)
            {
                // Set line number
                marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
            }
        }
        catch (CoreException e)
        {
            // An error occurred while creating the markers
            PreflightingLogger.error(AnalyzeApkHandler.class,
                    "Error creating marker for the following resource: " + resource.getName()); //$NON-NLS-1$
        }

    }
}