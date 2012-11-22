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
package com.motorolamobility.preflighting.ui.tabs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;

import com.motorolamobility.preflighting.ui.PreflightingUIPlugin;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

public class GeneralSettingsComposite extends AbstractAppValidatorTabComposite
{

    /**
     * 
     */
    public static enum outputTypes
    {
        TEXT, XML, CSV
    };

    private int outputTypeSelection;

    private Text limitText;

    private Button outputTypePlainText;

    private Button outputTypeXML;

    private Button outputTypeCSV;

    private Combo warningCombo;

    private Combo verbosityCombo;

    private Button eclipseProblemToWarningButton;

    private IPreferenceStore prefStore;

    private String errorMessage;

    /**
     * @return the canFinish
     */
    public boolean canFinish()
    {

        return canFinish;
    }

    /**
     * @param canFinish the canFinish to set
     */
    public void setCanFinish(boolean canFinish)
    {

        this.canFinish = canFinish;
    }

    private boolean canFinish = true;

    /**
     * @param parent
     * @param style
     */
    public GeneralSettingsComposite(Composite parent, int style)
    {
        super(parent, style);

        prefStore = PreflightingUIPlugin.getDefault().getPreferenceStore();

        outputTypeSelection =
                Integer.parseInt(getPrefStoreValue(PreflightingUIPlugin.OUTPUT_TYPE_VALUE, "0"));

        Layout layout = new GridLayout(1, true);
        this.setLayout(layout);
        this.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group limitGroup = new Group(this, SWT.NONE);
        layout = new GridLayout(1, false);
        limitGroup.setLayout(layout);
        limitGroup.setText(PreflightingUiNLS.GeneralSettingsComposite_OutputSettingLabel);
        limitGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        Composite limitsComposite = new Composite(limitGroup, SWT.NONE);
        limitsComposite.setLayout(new GridLayout(3, false));
        limitsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

        Label limitLabel = new Label(limitsComposite, SWT.NONE);
        limitLabel.setText(PreflightingUiNLS.GeneralSettingsComposite_OutputLimit);
        GridData labelData = new GridData(SWT.LEFT, SWT.NONE, false, false);
        limitLabel.setLayoutData(labelData);

        limitText = new Text(limitsComposite, SWT.BORDER);
        GridData textLayoutData = new GridData(SWT.LEFT, SWT.NONE, false, false);
        textLayoutData.widthHint = 70;
        limitText.setLayoutData(textLayoutData);

        limitText.setText(getPrefStoreValue(PreflightingUIPlugin.OUTPUT_LIMIT_VALUE,
                (PreflightingUIPlugin.OUTPUT_LIMIT_DEFAULT_VALUE)));

        limitText.addModifyListener(new ModifyListener()
        {

            public void modifyText(ModifyEvent e)
            {
                int value = 0;
                canFinish = true;
                try
                {
                    value = Integer.parseInt(limitText.getText());
                    if (value < 0)
                    {
                        canFinish = false;
                        errorMessage =
                                PreflightingUiNLS.GeneralSettingsComposite_OutputLimitNaNValidationMessage;

                    }
                }
                catch (NumberFormatException exc)
                {

                    canFinish = false;
                    errorMessage =
                            PreflightingUiNLS.GeneralSettingsComposite_OutputLimitNaNValidationMessage;

                }

                notifyListener();
            }
        });

        Label limitDefaultLabel = new Label(limitsComposite, SWT.NONE);
        limitDefaultLabel.setText(PreflightingUiNLS.GeneralSettingsComposite_LimitLabel);

        FontData[] oldFontData = limitDefaultLabel.getFont().getFontData();

        for (FontData f : oldFontData)
        {

            f.setStyle(f.getStyle() | SWT.ITALIC);

        }

        Font newItFont = new Font(getDisplay(), oldFontData);
        limitDefaultLabel.setFont(newItFont);
        limitDefaultLabel.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

        Composite outputTypeComposite = new Composite(limitGroup, SWT.NONE);
        layout = new GridLayout(1, false);
        outputTypeComposite.setLayout(layout);
        outputTypeComposite.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

        Label outputTypeLabel = new Label(outputTypeComposite, SWT.LEFT);
        outputTypeLabel.setText(PreflightingUiNLS.GeneralSettingsComposite_OutputTypeLabel);

        String outputType =
                getPrefStoreValue(PreflightingUIPlugin.OUTPUT_TYPE_VALUE,
                        PreflightingUIPlugin.OUTPUT_TYPE_DEFAULT_VALUE);

        outputTypePlainText = new Button(outputTypeComposite, SWT.RADIO);
        outputTypePlainText
                .setText(PreflightingUiNLS.GeneralSettingsComposite_PlainTextRadioButton);
        outputTypePlainText.setSelection(outputType.equals(String.valueOf(outputTypes.TEXT
                .ordinal())));
        outputTypePlainText.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent e)
            {

                if (outputTypePlainText.getSelection())
                {
                    outputTypeSelection = outputTypes.TEXT.ordinal();
                }

            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
              //do nothing
            }
        });

        outputTypeXML = new Button(outputTypeComposite, SWT.RADIO);
        outputTypeXML.setText(PreflightingUiNLS.GeneralSettingsComposite_XMLOutputCombo);
        outputTypeXML.setSelection(outputType.equals(String.valueOf(outputTypes.XML.ordinal()))); //$NON-NLS-1$
        outputTypeXML.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent e)
            {
                if (outputTypeXML.getSelection())
                {
                    outputTypeSelection = outputTypes.XML.ordinal();
                }
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
              //do nothing
            }
        });

        outputTypeCSV = new Button(outputTypeComposite, SWT.RADIO);
        outputTypeCSV.setText(PreflightingUiNLS.GeneralSettingsComposite_CSVOutputCombo);
        outputTypeCSV.setSelection(outputType.equals(String.valueOf(outputTypes.CSV.ordinal()))); //$NON-NLS-1$

        outputTypeCSV.addSelectionListener(new SelectionListener()
        {

            public void widgetSelected(SelectionEvent e)
            {
                if (outputTypeCSV.getSelection())
                {
                    outputTypeSelection = outputTypes.CSV.ordinal();
                }
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
              //do nothing
            }
        });

        Group verbosityGroup = new Group(this, SWT.NONE);
        layout = new GridLayout(1, false);
        verbosityGroup.setLayout(layout);
        verbosityGroup.setText(PreflightingUiNLS.GeneralSettingsComposite_VerbositySettingLabel);
        verbosityGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        Label warningLevelsLabel = new Label(verbosityGroup, SWT.NONE);
        warningLevelsLabel.setText(PreflightingUiNLS.GeneralSettingsComposite_WarningLevelSettings);

        warningCombo = new Combo(verbosityGroup, SWT.BORDER | SWT.READ_ONLY);
        GridData comboLayoutData = new GridData(SWT.NONE, SWT.NONE, false, true);
        comboLayoutData.widthHint = 400;
        warningCombo.setLayoutData(comboLayoutData);

        warningCombo.add(PreflightingUiNLS.GeneralSettingsComposite_WarningLevel0);
        warningCombo.add(PreflightingUiNLS.GeneralSettingsComposite_WarningLevel1);
        warningCombo.add(PreflightingUiNLS.GeneralSettingsComposite_WarningLevel2);
        warningCombo.add(PreflightingUiNLS.GeneralSettingsComposite_WarningLevel3);
        warningCombo.add(PreflightingUiNLS.GeneralSettingsComposite_WarningLevel4);
        warningCombo.select(Integer.parseInt(getPrefStoreValue(
                PreflightingUIPlugin.WARNING_LEVEL_VALUE,
                PreflightingUIPlugin.WARNING_LEVEL_DEFAULT_VALUE)));

        Label verbosityLevelsLabel = new Label(verbosityGroup, SWT.NONE);
        verbosityLevelsLabel
                .setText(PreflightingUiNLS.GeneralSettingsComposite_VerbosityLevelLabel);

        verbosityCombo = new Combo(verbosityGroup, SWT.BORDER | SWT.READ_ONLY);
        verbosityCombo.setLayoutData(comboLayoutData);
        verbosityCombo.add(PreflightingUiNLS.GeneralSettingsComposite_VerbosityLevel0);
        verbosityCombo.add(PreflightingUiNLS.GeneralSettingsComposite_VerbosityLevel1);
        verbosityCombo.add(PreflightingUiNLS.GeneralSettingsComposite_VerbosityLevel2);

        verbosityCombo.select(Integer.parseInt(getPrefStoreValue(
                PreflightingUIPlugin.VERBOSITY_LEVEL_VALUE,
                PreflightingUIPlugin.VERBOSITY_LEVEL_DEFAULT_VALUE)));

        eclipseProblemToWarningButton = new Button(this, SWT.CHECK);
        eclipseProblemToWarningButton
                .setText(PreflightingUiNLS.GeneralSettingsComposite_ShowErrosProblemsButton);
        eclipseProblemToWarningButton.setSelection(Boolean.parseBoolean(getPrefStoreValue(
                PreflightingUIPlugin.ECLIPSE_PROBLEM_TO_WARNING_VALUE,
                PreflightingUIPlugin.ECLIPSE_PROBLEM_TO_WARNING_DEFAULT_VALUE)));

    }

    @Override
    public void performDefaults()
    {

        limitText.setText(PreflightingUIPlugin.OUTPUT_LIMIT_DEFAULT_VALUE); //$NON-NLS-1$

        outputTypePlainText.setSelection(true);

        outputTypeSelection = 0;

        outputTypeXML.setSelection(false);

        outputTypeCSV.setSelection(false);

        warningCombo.select(Integer.valueOf(PreflightingUIPlugin.WARNING_LEVEL_DEFAULT_VALUE));

        verbosityCombo.select(0);

        eclipseProblemToWarningButton.setSelection(true);

    }

    private String getPrefStoreValue(String prefKey, String defaultValue)
    {

        String returnValue = null;
        if (!prefStore.isDefault(prefKey))
        {
            returnValue = prefStore.getString(prefKey);

        }
        else
        {
            returnValue = defaultValue;
        }

        return returnValue;

    }

    @Override
    public IStatus isValid()
    {
        IStatus status =
                new Status(IStatus.OK, PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID,
                        PreflightingUiNLS.GeneralSettingsComposite_24);

        if (!canFinish())
        {
            status =
                    new Status(IStatus.ERROR, PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID,
                            errorMessage);
        }

        return status;
    }

    @Override
    public void performOk(IPreferenceStore preferenceStore)
    {
        preferenceStore.setValue(PreflightingUIPlugin.OUTPUT_LIMIT_VALUE, limitText.getText());

        preferenceStore.setValue(PreflightingUIPlugin.OUTPUT_TYPE_VALUE,
                String.valueOf(outputTypeSelection));

        preferenceStore.setValue(PreflightingUIPlugin.WARNING_LEVEL_VALUE,
                String.valueOf(warningCombo.getSelectionIndex()));

        preferenceStore.setValue(PreflightingUIPlugin.VERBOSITY_LEVEL_VALUE,
                String.valueOf(verbosityCombo.getSelectionIndex()));

        preferenceStore.setValue(PreflightingUIPlugin.ECLIPSE_PROBLEM_TO_WARNING_VALUE,
                Boolean.toString(eclipseProblemToWarningButton.getSelection()));

    }

    @Override
    public String commandLineBuilder()
    {

        StringBuilder commandLine = new StringBuilder();

        //First part: output limit 
        String limit = limitText.getText();

        commandLine.append(!limit.equals("0") ? "-limit " + limit + " " : "");

        //Second, output type

        commandLine.append("-output "
                + GeneralSettingsComposite.outputTypes.values()[outputTypeSelection].toString()
                        .toLowerCase() + " ");

        //warning levels

        commandLine.append("-w" + String.valueOf(warningCombo.getSelectionIndex()) + " ");

        //verbosity levels

        commandLine.append("-v" + String.valueOf(verbosityCombo.getSelectionIndex()) + " ");

        return commandLine.toString().trim();

    }

}
