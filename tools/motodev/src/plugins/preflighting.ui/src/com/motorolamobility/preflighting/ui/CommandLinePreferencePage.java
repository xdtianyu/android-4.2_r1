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

package com.motorolamobility.preflighting.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.motorolamobility.preflighting.core.logging.PreflightingLogger;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;
import com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite;
import com.motorolamobility.preflighting.ui.tabs.CheckersTabComposite;
import com.motorolamobility.preflighting.ui.tabs.DevicesTabComposite;
import com.motorolamobility.preflighting.ui.tabs.GeneralSettingsComposite;
import com.motorolamobility.preflighting.ui.tabs.UIChangedListener;

public class CommandLinePreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
    private final String PREFERENCE_PAGE_HELP = PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID
            + ".preference-appvalidator-commandline"; //$NON-NLS-1$

    private List<AbstractAppValidatorTabComposite> pagesComposite;

    public CommandLinePreferencePage()
    {
        setPreferenceStore(PreflightingUIPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench)
    {
        //do nothing
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk()
    {
        /*
         *  Workaround to make it work on MacOS
         *  
         *  On MacOS, the TableViewer cell doesn't lose the focus when the
         *  user clicks "Apply" or "OK". This way, the editor's setValue method
         *  is not called, and the cell value is not updated in the model. 
         *  Given that, the last modification would not be persisted.
         *  
         *  This forces the focus change and consequently updates the model 
         *  before continuing.
         */
        getShell().setFocus();
        getControl().setFocus();

        StringBuilder commandLine = new StringBuilder();
        for (AbstractAppValidatorTabComposite composite : pagesComposite)
        {
            composite.performOk(getPreferenceStore());
            commandLine.append(composite.commandLineBuilder() + " "); //$NON-NLS-1$

        }
        getPreferenceStore().setValue(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY,
                commandLine.toString().trim());
        getPreferenceStore().setValue(PreflightingUIPlugin.ERRORS_TO_WARNINGS_PREFERENCE_KEY,
                Boolean.getBoolean(PreflightingUIPlugin.ECLIPSE_PROBLEM_TO_WARNING_VALUE));

        PreflightingLogger.debug("App Validator command line: " + commandLine);

        return super.performOk();
    }

    /**
     * 
     */

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults()
    {
        for (AbstractAppValidatorTabComposite composite : pagesComposite)
        {
            composite.performDefaults();
        }
        super.performDefaults();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#isValid()
     */

    @Override
    protected Control createContents(Composite parent)
    {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, PREFERENCE_PAGE_HELP);

        pagesComposite = new ArrayList<AbstractAppValidatorTabComposite>();

        IPreferenceStore prefStore = PreflightingUIPlugin.getDefault().getPreferenceStore();
        if ((!prefStore.contains(PreflightingUIPlugin.OUTPUT_LIMIT_VALUE))
                && prefStore.contains(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY)
                && (!(prefStore.getString(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY))
                        .equals(PreflightingUIPlugin.DEFAULT_BACKWARD_COMMANDLINE)))
        {

            Label backLabel = new Label(parent, SWT.WRAP);
            GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
            layoutData.widthHint = 450;
            backLabel.setLayoutData(layoutData);
            backLabel.setText("You have previously set the following App Validator parameters:\n"
                    + prefStore.getString(PreflightingUIPlugin.COMMAND_LINE_PREFERENCE_KEY));

        }

        Composite mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, false));
        GridData mainData = new GridData(SWT.FILL, SWT.TOP, true, false);
        mainComposite.setLayoutData(mainData);

        TabFolder tabFolder = new TabFolder(mainComposite, SWT.TOP);

        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem generalSettingsTab = new TabItem(tabFolder, SWT.NONE);
        generalSettingsTab
                .setText(PreflightingUiNLS.CommandLinePreferencePage_GeneralSettingTabName);
        AbstractAppValidatorTabComposite genSettingsComposite =
                new GeneralSettingsComposite(tabFolder, SWT.NONE);
        generalSettingsTab.setControl(genSettingsComposite);
        genSettingsComposite.addUIChangedListener(new UIChangedListener()
        {

            public void uiChanged(AbstractAppValidatorTabComposite composite)
            {
                validateUI(composite);
            }
        });
        pagesComposite.add(genSettingsComposite);

        TabItem checkersSettingsTab = new TabItem(tabFolder, SWT.NONE);
        checkersSettingsTab.setText(PreflightingUiNLS.CommandLinePreferencePage_Checkers_Tab);
        AbstractAppValidatorTabComposite checkersTabComposite =
                new CheckersTabComposite(tabFolder, SWT.NONE, getPreferenceStore());
        checkersSettingsTab.setControl(checkersTabComposite);
        checkersTabComposite.addUIChangedListener(new UIChangedListener()
        {

            public void uiChanged(AbstractAppValidatorTabComposite composite)
            {
                validateUI(composite);
            }
        });
        pagesComposite.add(checkersTabComposite);

        TabItem devicesSettingTab = new TabItem(tabFolder, SWT.NONE);
        devicesSettingTab.setText(PreflightingUiNLS.CommandLinePreferencePage_Devices_Tab);
        AbstractAppValidatorTabComposite devicesTabComposite =
                new DevicesTabComposite(tabFolder, SWT.NONE, getPreferenceStore());
        devicesSettingTab.setControl(devicesTabComposite);
        pagesComposite.add(devicesTabComposite);

        setValid(((GeneralSettingsComposite) genSettingsComposite).canFinish());

        return mainComposite;
    }

    public void validateUI(AbstractAppValidatorTabComposite composite)
    {
        IStatus status = composite.isValid();

        if (status.getSeverity() == IStatus.ERROR)
        {
            setValid(false);
            setMessage(status.getMessage(), IMessageProvider.ERROR);
        }
        else if (status.getSeverity() == IStatus.WARNING)
        {
            setMessage(status.getMessage(), IMessageProvider.WARNING);
        }
        else
        {
            setValid(true);
            setMessage(null);
        }

    }
}
