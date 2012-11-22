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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.motorolamobility.preflighting.core.checker.CheckerDescription;
import com.motorolamobility.preflighting.core.checker.condition.Condition;
import com.motorolamobility.preflighting.core.validation.ValidationManager;
import com.motorolamobility.preflighting.ui.PreflightingUIPlugin;
import com.motorolamobility.preflighting.ui.i18n.PreflightingUiNLS;

/**
 * This class represents the Checkers Tab on the App Validador preference page.
 */
public class CheckersTabComposite extends AbstractAppValidatorTabComposite
{

    private final class CheckersColumnSelectionAdapter extends SelectionAdapter
    {
        private int columnIndex;

        public CheckersColumnSelectionAdapter(int columnIndex)
        {
            this.columnIndex = columnIndex;
        }

        @Override
        public void widgetSelected(SelectionEvent e)
        {
            checkersTableComparator.setColumn(columnIndex);
            checkersTableViewer.getTable().setSortColumn((TableColumn) e.getSource());
            checkersTableViewer.getTable().setSortDirection(
                    checkersTableComparator.getSwtDirection());
            checkersTableViewer.refresh();
            super.widgetSelected(e);
        }
    }

    private final class ConditionsColumnSelectionAdapter extends SelectionAdapter
    {
        private int columnIndex;

        public ConditionsColumnSelectionAdapter(int columnIndex)
        {
            this.columnIndex = columnIndex;
        }

        @Override
        public void widgetSelected(SelectionEvent e)
        {
            conditionsTableComparator.setColumn(columnIndex);
            conditionsTableViewer.getTable().setSortColumn((TableColumn) e.getSource());
            conditionsTableViewer.getTable().setSortDirection(
                    conditionsTableComparator.getSwtDirection());
            conditionsTableViewer.refresh();
            super.widgetSelected(e);
        }
    }

    private static final String NO_CHECKERS_SELECTED = "none"; //$NON-NLS-1$

    /**
     * Checkers table label provider, provides Cell Text and Tooltip 
     */
    public class CheckersLabelProvider extends CellLabelProvider
    {

        /*
         * Display time in seconds
         */
        private static final int TOOLTIP_DISPLAYTIME = 10;

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
         */
        @Override
        public void update(ViewerCell cell)
        {
            String text = "";
            int columnIndex = cell.getColumnIndex();
            CheckerDescription checkerDescription = (CheckerDescription) cell.getElement();
            switch (columnIndex)
            {
                case NAME_COLUMN_INDEX:
                    text = checkerDescription.getName();
                    break;
                case ID_COLUMN_INDEX:
                    text = checkerDescription.getId();
                    break;
                case CHECKER_PARAMS_COLUMN_INDEX:
                    String checkerId = checkerDescription.getId();
                    text = checkerParams.containsKey(checkerId) ? checkerParams.get(checkerId) : "";
                    break;
                case CHECKER_CHANGE_WARNING_LEVEL_COLUMN_INDEX:
                    text =
                            getWarningLevelText(customCheckersWarningLevels.get(checkerDescription
                                    .getId()));
                    break;
                default:
                    break;
            }
            cell.setText(text);
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipDisplayDelayTime(java.lang.Object)
         */
        @Override
        public int getToolTipDisplayDelayTime(Object object)
        {
            return 200;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java.lang.Object)
         */
        @Override
        public int getToolTipTimeDisplayed(Object object)
        {
            return TOOLTIP_DISPLAYTIME * 1000;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
         */
        @Override
        public String getToolTipText(Object element)
        {
            CheckerDescription checkerDescription = (CheckerDescription) element;
            return checkerDescription.getDescription();
        }

    }

    /**
     * Conditions table label provider, provides Cell Text and Tooltip 
     */
    public class ConditionsLabelProvider extends CellLabelProvider
    {

        /*
         * Display time in seconds
         */
        private static final int TOOLTIP_DISPLAYTIME = 10;

        @Override
        public void update(ViewerCell cell)
        {
            String text = "";
            int columnIndex = cell.getColumnIndex();
            Condition condition = (Condition) cell.getElement();
            switch (columnIndex)
            {
                case NAME_COLUMN_INDEX:
                    text = condition.getName();
                    break;
                case ID_COLUMN_INDEX:
                    text = condition.getId();
                    break;
                case CONDITION_WARNING_LEVEL_COLUMN_INDEX:
                    text = condition.getSeverityLevel().toString();
                    break;
                case CONDITION_CHANGE_WARNING_LEVEL_COLUMN_INDEX:
                    text =
                            getWarningLevelText(customConditionsWarningLevels
                                    .get(condition.getId()));
                    break;
                default:
                    break;
            }
            cell.setText(text);
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipDisplayDelayTime(java.lang.Object)
         */
        @Override
        public int getToolTipDisplayDelayTime(Object object)
        {
            return 200;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java.lang.Object)
         */
        @Override
        public int getToolTipTimeDisplayed(Object object)
        {
            return TOOLTIP_DISPLAYTIME * 1000;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
         */
        @Override
        public String getToolTipText(Object element)
        {
            Condition condition = (Condition) element;
            return condition.getDescription();
        }

    }

    /**
     *  Checkers Table content provider
     */
    public class CheckersContentProvider implements IStructuredContentProvider
    {

        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement)
        {
            List<CheckerDescription> checkersDescription = (List<CheckerDescription>) inputElement;
            return checkersDescription.toArray();
        }

        public void dispose()
        {
            //do nothing
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
          //do nothing
        }
    }

    /**
     *  Conditions Table content provider
     */
    public class ConditionsContentProvider implements IStructuredContentProvider
    {

        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement)
        {
            List<Condition> conditions = (List<Condition>) inputElement;
            return conditions.toArray();
        }

        public void dispose()
        {
          //do nothing
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
          //do nothing
        }
    }

    /**
     * Table comparator
     * Add sort functionality for checkers
     */
    private class CheckersTableComparator extends ViewerComparator
    {
        private final int ORDER_ASC = 1;

        private final int ORDER_DESC = -1;

        /**
         * Column that must be used to sort elements
         */
        private int column = -1;

        private int direction = ORDER_ASC;

        public void setColumn(int column)
        {
            if (this.column == column)
            {
                direction = direction == ORDER_ASC ? ORDER_DESC : ORDER_ASC;
            }
            else
            {
                this.column = column;
                direction = ORDER_ASC;
            }
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Viewer viewer, Object e1, Object e2)
        {
            CheckerDescription checkerLeft = (CheckerDescription) e1;
            CheckerDescription checkerRight = (CheckerDescription) e2;

            String left = ""; //$NON-NLS-1$
            String right = ""; //$NON-NLS-1$
            switch (column)
            {
                case NAME_COLUMN_INDEX:
                    left = checkerLeft.getName();
                    right = checkerRight.getName();
                    break;
                case ID_COLUMN_INDEX:
                    left = checkerLeft.getId();
                    right = checkerRight.getId();
                    break;
                case CHECKER_PARAMS_COLUMN_INDEX:
                    String leftParams = checkerParams.get(checkerLeft.getId());
                    String rightParams = checkerParams.get(checkerRight.getId());
                    left = ((leftParams != null) ? leftParams : "");
                    right = ((rightParams != null) ? rightParams : "");
                    break;
                case CHECKER_CHANGE_WARNING_LEVEL_COLUMN_INDEX:
                    Boolean leftWarningLevel = customCheckersWarningLevels.get(checkerLeft.getId());
                    Boolean rightWarningLevel =
                            customCheckersWarningLevels.get(checkerRight.getId());
                    left = ((leftWarningLevel != null) ? leftWarningLevel.toString() : "");
                    right = ((rightWarningLevel != null) ? rightWarningLevel.toString() : "");
                    break;
                default:
                    break;
            }

            return left.compareTo(right) * direction;
        }

        /**
         * Returns the SWT constant which represents the direction
         * @return
         */
        public int getSwtDirection()
        {
            return direction == ORDER_ASC ? SWT.UP : SWT.DOWN;
        }
    }

    /**
     * Conditions comparator
     * Add sort functionality for checkers
     */
    private class ConditionsTableComparator extends ViewerComparator
    {
        private final int ORDER_ASC = 1;

        private final int ORDER_DESC = -1;

        /**
         * Column that must be used to sort elements
         */
        private int column = -1;

        private int direction = ORDER_ASC;

        public void setColumn(int column)
        {
            if (this.column == column)
            {
                direction = direction == ORDER_ASC ? ORDER_DESC : ORDER_ASC;
            }
            else
            {
                this.column = column;
                direction = ORDER_ASC;
            }
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Viewer viewer, Object e1, Object e2)
        {
            Condition conditionLeft = (Condition) e1;
            Condition conditionRight = (Condition) e2;

            String left = ""; //$NON-NLS-1$
            String right = ""; //$NON-NLS-1$
            switch (column)
            {
                case NAME_COLUMN_INDEX:
                    left = conditionLeft.getName();
                    right = conditionRight.getName();
                    break;
                case ID_COLUMN_INDEX:
                    left = conditionLeft.getId();
                    right = conditionRight.getId();
                    break;
                case CHECKER_PARAMS_COLUMN_INDEX:
                    left = conditionLeft.getSeverityLevel().toString();
                    right = conditionRight.getSeverityLevel().toString();
                    break;
                case CHECKER_CHANGE_WARNING_LEVEL_COLUMN_INDEX:
                    Boolean leftWarningLevel =
                            customConditionsWarningLevels.get(conditionLeft.getId());
                    Boolean rightWarningLevel =
                            customConditionsWarningLevels.get(conditionRight.getId());
                    left = ((leftWarningLevel != null) ? leftWarningLevel.toString() : "");
                    right = ((rightWarningLevel != null) ? rightWarningLevel.toString() : "");
                    break;
                default:
                    break;
            }

            return left.compareTo(right) * direction;
        }

        /**
         * Returns the SWT constant which represents the direction
         * @return
         */
        public int getSwtDirection()
        {
            return direction == ORDER_ASC ? SWT.UP : SWT.DOWN;
        }
    }

    /*
     * Table columns
     */
    private static final int NAME_COLUMN_INDEX = 0;

    private static final int ID_COLUMN_INDEX = 1;

    private static final int CONDITION_WARNING_LEVEL_COLUMN_INDEX = 2;

    private static final int CONDITION_CHANGE_WARNING_LEVEL_COLUMN_INDEX = 3;

    private static final int CHECKER_PARAMS_COLUMN_INDEX = 2;

    private static final int CHECKER_CHANGE_WARNING_LEVEL_COLUMN_INDEX = 3;

    /**
     * The Checkers TableViewer
     */
    private CheckboxTableViewer checkersTableViewer;

    /**
     * The Conditions TableViewer
     */
    private CheckboxTableViewer conditionsTableViewer;

    /**
     * The Select All checkbox for checkers
     */
    private Button selectAllCheckersCheck;

    /**
     * The Select All checkbox for conditions
     */
    private Button selectAllConditionsCheck;

    /**
     * A list with all CheckerDescription
     */
    private List<CheckerDescription> checkersDescriptions;

    /**
     * The model which keeps checker params, Key is checkerId and value is the Parameters String
     */
    private Map<String, String> checkerParams;

    /**
     * The model which keeps the custom warning levels, Key is checkerId and value is a Boolean that indicates:
     * true -> increases warning level
     * false -> decreases warning level
     * null and/or inexistent -> no change
     */
    private Map<String, Boolean> customCheckersWarningLevels = new HashMap<String, Boolean>();

    private Map<String, Boolean> customConditionsWarningLevels = new HashMap<String, Boolean>();

    /**
     * The model which keeps all conditions, Key is checkerId and value is a list of its conditions
     */
    private Map<String, List<Condition>> allConditionsMap = new HashMap<String, List<Condition>>();

    /**
     * The model which keeps only the selected conditions, Key is checkerId and value is a list of its selected conditions
     */
    private Map<String, List<Condition>> selectedConditionsMap =
            new HashMap<String, List<Condition>>();

    /**
     * Warning Level Operations (increase, keep, decrease)
     * Currently, it's not possible to set the warning level, you can only increase or decrease it
     */
    private String[] WarningLevelOperations =
    {
            PreflightingUiNLS.CheckersTabComposite_WarningLevel_Operation_Increase,
            PreflightingUiNLS.CheckersTabComposite_WarningLevel_Operation_Kepp,
            PreflightingUiNLS.CheckersTabComposite_WarningLevel_Operation_Decrease
    };

    /**
     * Checkers table comparator, used for sort functionality
     */
    private CheckersTableComparator checkersTableComparator;

    /**
     * Conditions table comparator, used for sort functionality
     */
    private ConditionsTableComparator conditionsTableComparator;

    /**
     * Return the text that represents the warning level change, which is
     * saved as a boolean
     * 
     * true -> increases warning level
     * false -> decreases warning level
     * null -> no change
     * 
     * @param action the value stored
     * @return the corresponding warning level operation
     */
    public String getWarningLevelText(Boolean action)
    {
        String result = PreflightingUiNLS.CheckersTabComposite_WarningLevel_Operation_Kepp;

        if (action != null)
        {
            if (action)
            {
                result = PreflightingUiNLS.CheckersTabComposite_WarningLevel_Operation_Increase;
            }
            else
            {
                result = PreflightingUiNLS.CheckersTabComposite_WarningLevel_Operation_Decrease;
            }
        }

        return result;
    }

    /**
     * Initialize all UI items
     * 
     * @param parent the parent composite
     * @param style the SWT styles
     * @param preferenceStore the preference store
     */
    public CheckersTabComposite(Composite parent, int style, IPreferenceStore preferenceStore)
    {
        super(parent, style);

        Layout layout = new GridLayout(1, false);
        this.setLayout(layout);

        createCheckersComposite();

        init(preferenceStore);
    }

    /**
     * Creates the composite which will have the checkers and also
     * the conditions selection
     */
    private void createCheckersComposite()
    {
        Layout layout;
        Composite composite = new Composite(this, SWT.NONE);
        layout = new GridLayout(1, true);
        composite.setLayout(layout);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gd);

        /*
         * Create checkers group
         */
        createCheckersGroup(composite);

        /*
         * Include the conditions area in the checker group.
         * This are is updated according to the checker selection
         */
        createConditionsGroup(composite);

    }

    /**
     * Create and return the Checkers group. 
     * It contains the table with all checkers listed. When a checker is
     * selected, the corresponding conditions are updated in the conditions
     * table.
     * 
     * @param topComposite the parent composite
     */
    private void createCheckersGroup(Composite topComposite)
    {

        Group checkersGroup = new Group(topComposite, SWT.NONE);
        checkersGroup.setLayout(new GridLayout(1, false));
        checkersGroup.setText(PreflightingUiNLS.CheckersTabComposite_Checkers_Group);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        checkersGroup.setLayoutData(gd);

        checkersTableViewer =
                CheckboxTableViewer.newCheckList(checkersGroup, SWT.BORDER | SWT.SINGLE
                        | SWT.FULL_SELECTION);
        Control checkersTableControl = checkersTableViewer.getTable();
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        checkersTableControl.setLayoutData(gd);

        checkersTableComparator = new CheckersTableComparator();
        checkersTableViewer.setComparator(checkersTableComparator);

        /*
         * Create Columns
         */
        TableViewerColumn column = new TableViewerColumn(checkersTableViewer, SWT.NONE); // Name
        column.getColumn().setText(PreflightingUiNLS.CheckersTabComposite_Name_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(230);
        column.setLabelProvider(new CheckersLabelProvider());
        column.getColumn().addSelectionListener(
                new CheckersColumnSelectionAdapter(NAME_COLUMN_INDEX));

        column = new TableViewerColumn(checkersTableViewer, SWT.NONE); // ID
        column.getColumn().setText(PreflightingUiNLS.CheckersTabComposite_Id_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new CheckersLabelProvider());
        column.getColumn()
                .addSelectionListener(new CheckersColumnSelectionAdapter(ID_COLUMN_INDEX));

        column = new TableViewerColumn(checkersTableViewer, SWT.NONE); // Parameters
        column.getColumn().setText(PreflightingUiNLS.CheckersTabComposite_Checker_Params_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new CheckersLabelProvider());
        column.getColumn().addSelectionListener(
                new CheckersColumnSelectionAdapter(CHECKER_PARAMS_COLUMN_INDEX));
        // set a custom editor for this column
        column.setEditingSupport(new ParameterEditingSupport(checkersTableViewer));

        column = new TableViewerColumn(checkersTableViewer, SWT.NONE); // Change Warning Level
        column.getColumn()
                .setText(PreflightingUiNLS.CheckersTabComposite_ChangeWarningLevel_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new CheckersLabelProvider());
        column.getColumn().addSelectionListener(
                new CheckersColumnSelectionAdapter(CHECKER_CHANGE_WARNING_LEVEL_COLUMN_INDEX));
        // set a custom editor for this column
        column.setEditingSupport(new WarningLevelEditingSupport(checkersTableViewer));

        checkersTableViewer.getTable().setHeaderVisible(true);

        checkersTableViewer.setContentProvider(new CheckersContentProvider());
        ColumnViewerToolTipSupport.enableFor(checkersTableViewer);
        // Selection Change Listener handles needed UI updates on the paramsText according to checkers selected
        // It also updates the conditions table
        checkersTableViewer.getTable().addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                CheckerDescription checkerDescription = (CheckerDescription) e.item.getData();
                TableItem tableItem = (TableItem) e.item;
                updateConditionTableBasedOnChecker(tableItem.getChecked());
                populateConditionsTable(checkerDescription.getId());
            }
        });

        // CheckStateListener handles the selectAll check box behavior and also selects the clicked item on the table.
        checkersTableViewer.addCheckStateListener(new ICheckStateListener()
        {

            public void checkStateChanged(CheckStateChangedEvent event)
            {
                boolean isAllSelected = areAllItemsChecked(checkersTableViewer);
                selectAllCheckersCheck.setSelection(isAllSelected);
                checkersTableViewer.setSelection(new StructuredSelection(event.getElement()), true);
                updateConditionTableBasedOnChecker(event.getChecked());
                notifyListener();
            }
        });

        ValidationManager validationManager = new ValidationManager();
        checkersDescriptions = validationManager.getCheckersDescription();
        checkerParams = new HashMap<String, String>(checkersDescriptions.size());
        checkersTableViewer.setInput(checkersDescriptions);

        selectAllCheckersCheck = new Button(checkersGroup, SWT.CHECK);
        selectAllCheckersCheck
                .setText(PreflightingUiNLS.CheckersTabComposite_Checkers_SelectAll_Check);
        gd = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
        selectAllCheckersCheck.setLayoutData(gd);
        selectAllCheckersCheck.setSelection(true);
        selectAllCheckersCheck.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                checkersTableViewer.setAllChecked(selectAllCheckersCheck.getSelection());
                updateConditionTableBasedOnChecker(selectAllCheckersCheck.getSelection());
                super.widgetSelected(e);
                notifyListener();
            }
        });

        checkersTableComparator.setColumn(0);

    }

    /**
     * @param event
     */
    public void updateConditionTableBasedOnChecker(boolean checkerEnabled)
    {
        if (!checkerEnabled)
        {
            //disabling checker => disable all conditions as well (and disable the table)
            conditionsTableViewer.getTable().setEnabled(false);
            selectAllConditionsCheck.setEnabled(false);
        }
        else
        {
            //enabling checker => enables all conditions as well (and enable the table)
            conditionsTableViewer.getTable().setEnabled(true);
            selectAllConditionsCheck.setEnabled(true);
        }
    }

    /**
     * Create the conditions are, which contains a table with all
     * conditions of the selected checker. Its content is updated
     * dynamically based on the checker selection
     * 
     * @param topComposite the parent composite
     */
    /**
     * @param topComposite
     */
    private void createConditionsGroup(Composite topComposite)
    {

        Group conditionsGroup = new Group(topComposite, SWT.NONE);
        conditionsGroup.setLayout(new GridLayout(1, false));
        conditionsGroup.setText(PreflightingUiNLS.CheckersTabComposite_Conditions_Group);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 200;
        conditionsGroup.setLayoutData(gd);

        conditionsTableViewer =
                CheckboxTableViewer.newCheckList(conditionsGroup, SWT.BORDER | SWT.MULTI
                        | SWT.FULL_SELECTION);
        Control conditionsTableControl = conditionsTableViewer.getTable();
        gd.heightHint = 200;
        conditionsTableControl.setLayoutData(gd);

        conditionsTableViewer.setContentProvider(new ConditionsContentProvider());
        ColumnViewerToolTipSupport.enableFor(conditionsTableViewer);

        conditionsTableComparator = new ConditionsTableComparator();
        conditionsTableViewer.setComparator(conditionsTableComparator);

        // Create Columns
        TableViewerColumn column = new TableViewerColumn(conditionsTableViewer, SWT.NONE); // Name
        column.getColumn().setText(PreflightingUiNLS.CheckersTabComposite_Name_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(230);
        column.setLabelProvider(new ConditionsLabelProvider());
        column.getColumn().addSelectionListener(
                new ConditionsColumnSelectionAdapter(NAME_COLUMN_INDEX));

        column = new TableViewerColumn(conditionsTableViewer, SWT.NONE); // ID
        column.getColumn().setText(PreflightingUiNLS.CheckersTabComposite_Id_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new ConditionsLabelProvider());
        column.getColumn().addSelectionListener(
                new ConditionsColumnSelectionAdapter(ID_COLUMN_INDEX));

        column = new TableViewerColumn(conditionsTableViewer, SWT.NONE); // Warning Level
        column.getColumn().setText(PreflightingUiNLS.CheckersTabComposite_WarningLevel_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new ConditionsLabelProvider());
        column.getColumn().addSelectionListener(
                new ConditionsColumnSelectionAdapter(CONDITION_WARNING_LEVEL_COLUMN_INDEX));

        column = new TableViewerColumn(conditionsTableViewer, SWT.NONE); // Change Warning Level
        column.getColumn()
                .setText(PreflightingUiNLS.CheckersTabComposite_ChangeWarningLevel_Column);
        column.getColumn().setResizable(true);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new ConditionsLabelProvider());
        column.getColumn().addSelectionListener(
                new ConditionsColumnSelectionAdapter(CONDITION_CHANGE_WARNING_LEVEL_COLUMN_INDEX));
        // set a custom editor for this column
        column.setEditingSupport(new WarningLevelEditingSupport(conditionsTableViewer));

        conditionsTableViewer.getTable().setHeaderVisible(true);

        conditionsTableViewer.addCheckStateListener(new ICheckStateListener()
        {

            public void checkStateChanged(CheckStateChangedEvent event)
            {
                // update "select all" control
                boolean isAllSelected = areAllItemsChecked(conditionsTableViewer);
                selectAllConditionsCheck.setSelection(isAllSelected);
                // move the selection to the checked item
                conditionsTableViewer.setSelection(new StructuredSelection(event.getElement()),
                        true);

                // get the condition and the checker that are selected
                Condition condition = (Condition) event.getElement();
                CheckerDescription selectedChecker =
                        (CheckerDescription) ((StructuredSelection) checkersTableViewer
                                .getSelection()).getFirstElement();
                // update the selected conditions maps accordingly, adding the specified condition
                // if the checkbox is checked or removing it otherwise
                List<Condition> conditions = selectedConditionsMap.get(selectedChecker.getId());
                if (event.getChecked())
                {
                    conditions.add(condition);
                }
                else
                {
                    conditions.remove(condition);
                }
                notifyListener();
            }
        });

        selectAllConditionsCheck = new Button(conditionsGroup, SWT.CHECK);
        selectAllConditionsCheck
                .setText(PreflightingUiNLS.CheckersTabComposite_Checkers_SelectAll_Check);
        gd = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
        selectAllConditionsCheck.setLayoutData(gd);
        selectAllConditionsCheck.setSelection(true);
        selectAllConditionsCheck.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // update UI
                conditionsTableViewer.setAllChecked(selectAllConditionsCheck.getSelection());

                // update model
                CheckerDescription selectedChecker =
                        (CheckerDescription) ((StructuredSelection) checkersTableViewer
                                .getSelection()).getFirstElement();
                // get all conditions of the selected checker and add them to the selected conditions list
                if (selectAllConditionsCheck.getSelection())
                {
                    selectAllConditions(selectedChecker.getId());
                }
                // clear the selected conditions list
                else
                {
                    selectedConditionsMap.put(selectedChecker.getId(), new ArrayList<Condition>());
                }
                super.widgetSelected(e);
                notifyListener();
            }
        });

        selectAllConditionsCheck.setEnabled(conditionsTableViewer.getTable().isEnabled());

        conditionsTableComparator.setColumn(0);
    }

    /**
     * Select all conditions in the model, by copying the list with all conditions
     * 
     * @param checkerId the checker id to have all its conditions selected
     */
    private void selectAllConditions(String checkerId)
    {
        List<Condition> selectedConditions = new ArrayList<Condition>();
        for (Condition condition : allConditionsMap.get(checkerId))
        {
            selectedConditions.add(condition);
        }
        selectedConditionsMap.put(checkerId, selectedConditions);
    }

    /**
     * Create a custom cell editor which will contain a text editor so that
     * users can modify the parameters for a particular checker
     */
    public class ParameterEditingSupport extends EditingSupport
    {

        public ParameterEditingSupport(ColumnViewer viewer)
        {
            super(viewer);
        }

        @Override
        protected void setValue(Object element, Object value)
        {
            checkerParams.put(((CheckerDescription) element).getId(), (String) value);
            getViewer().update(element, null);
            notifyListener();
        }

        @Override
        protected Object getValue(Object element)
        {
            CheckerDescription checkerDescription = (CheckerDescription) element;
            String params = ""; //$NON-NLS-1$
            if (checkerParams.containsKey(checkerDescription.getId()))
            {
                params = checkerParams.get(checkerDescription.getId());
            }
            return params;
        }

        @Override
        protected CellEditor getCellEditor(Object element)
        {
            return new TextCellEditor(((TableViewer) getViewer()).getTable(), SWT.NONE);
        }

        @Override
        protected boolean canEdit(Object element)
        {
            return true;
        }

    }

    /**
     * Create a custom cell editor which will contain a combobox so that
     * users can modify (increase / decrease) the warning level for a particular 
     * condition or checker
     */
    public class WarningLevelEditingSupport extends EditingSupport
    {

        public WarningLevelEditingSupport(TableViewer viewer)
        {
            super(viewer);
        }

        @Override
        protected CellEditor getCellEditor(Object element)
        {
            CellEditor comboEditor =
                    new ComboBoxCellEditor(((TableViewer) getViewer()).getTable(),
                            WarningLevelOperations, SWT.READ_ONLY | SWT.FULL_SELECTION
                                    | SWT.DROP_DOWN);
            return comboEditor;

        }

        @Override
        protected boolean canEdit(Object element)
        {
            return true;
        }

        @Override
        protected Object getValue(Object element)
        {
            // Get value from model
            Boolean customWarningLevel = null;
            if (element instanceof Condition)
            {
                customWarningLevel =
                        customConditionsWarningLevels.get(((Condition) element).getId());
            }
            else
            {
                customWarningLevel =
                        customCheckersWarningLevels.get(((CheckerDescription) element).getId());
            }

            // select the appropriate item in the combobox 
            int value = 1; // keep 
            if (customWarningLevel != null)
            {
                value = ((customWarningLevel) ? 0 : 2); // increase, decrease
            }
            return new Integer(value);
        }

        @Override
        protected void setValue(Object element, Object value)
        {
            int intValue = ((Integer) value).intValue();
            Boolean booleanValue = null; // 1 = default (keep warning level)
            if (intValue != 1)
            {
                booleanValue = ((intValue == 0) ? new Boolean(true) : new Boolean(false));
            }

            // Set value in the model
            if (element instanceof Condition)
            {
                customConditionsWarningLevels.put(((Condition) element).getId(), booleanValue);
            }
            else
            {
                customCheckersWarningLevels.put(((CheckerDescription) element).getId(),
                        booleanValue);
            }

            // Update the UI. This generate a call to the label provider
            getViewer().update(element, null);
            getViewer().refresh();
        }
    }

    /**
     * Update the conditions table based on the checker that is selected
     * 
     * @param checkerId the checker selected in the UI
     */
    private void populateConditionsTable(String checkerId)
    {

        // display appropriate elements
        if (checkerId != null)
        {
            conditionsTableViewer.setInput(allConditionsMap.get(checkerId));
        }
        else
        {
            conditionsTableViewer.setInput(new ArrayList<Condition>());
        }

        // update selection in the table and also the "select all" button state
        conditionsTableViewer.setCheckedElements(selectedConditionsMap.get(checkerId).toArray());
        selectAllConditionsCheck.setEnabled(conditionsTableViewer.getTable().isEnabled());
        selectAllConditionsCheck.setSelection(areAllItemsChecked(conditionsTableViewer));

    }

    /*
     * Verifies if all items on a given CheckboxTableViewer are selected
     */
    private boolean areAllItemsChecked(CheckboxTableViewer tableViewer)
    {
        Table table = tableViewer.getTable();
        TableItem[] items = table.getItems();
        boolean allChecked = true;
        int i = 0;
        while ((i < items.length) && allChecked)
        {
            TableItem tableItem = items[i];
            if (!tableItem.getChecked())
            {
                allChecked = false;
            }
            i++;
        }
        return allChecked;
    }

    /*
     * Load data from the preference store and configure the UI accordingly.
     */
    private void init(IPreferenceStore preferenceStore)
    {
        /*
         * Populate condition maps
         */
        ValidationManager validationManager = new ValidationManager();
        for (CheckerDescription checkerDescription : checkersDescriptions)
        {
            allConditionsMap.put(checkerDescription.getId(),
                    validationManager.getCheckerConditions(checkerDescription.getId()));
        }

        String checkersPreference =
                preferenceStore.getString(PreflightingUIPlugin.CHECKERS_PREFERENCE_KEY);
        // Preference is empty means first time. Perform defaults
        if (checkersPreference.length() == 0)
        {
            performDefaults();
        }
        else
        {
            /*
             * Checkers information
             */
            if (!checkersPreference.equals(NO_CHECKERS_SELECTED)) //There's checkers to load and check on the table
            {
                StringTokenizer tokenizer = new StringTokenizer(checkersPreference, ","); //$NON-NLS-1$
                List<CheckerDescription> selectedCheckerDescriptions =
                        new ArrayList<CheckerDescription>(tokenizer.countTokens());
                while (tokenizer.hasMoreTokens())
                {
                    String checkerId = tokenizer.nextToken();
                    CheckerDescription checkerDescription = getCheckerDescription(checkerId);
                    if (checkerDescription != null)
                    {
                        selectedCheckerDescriptions.add(checkerDescription);
                    }
                }
                checkersTableViewer.setCheckedElements(selectedCheckerDescriptions.toArray());
            }
            selectAllCheckersCheck.setSelection(areAllItemsChecked(checkersTableViewer));

            String checkersConditionsPreference =
                    preferenceStore
                            .getString(PreflightingUIPlugin.CHECKERS_CONDITIONS_PREFERENCE_KEY);
            if (checkersConditionsPreference.length() > 0)
            {
                StringTokenizer tokenizer = new StringTokenizer(checkersConditionsPreference, ";");
                while (tokenizer.hasMoreElements())
                {
                    String checkerConditionStr = tokenizer.nextToken();
                    String[] split = checkerConditionStr.split(":");
                    if (split.length == 2)
                    {
                        String checkerId = split[0];
                        String[] selectedConditionsIds = split[1].split(",");

                        // define which are the selected conditions
                        Set<String> selectedConditionsIdsSet = new HashSet<String>();
                        for (String selectedConditionId : selectedConditionsIds)
                        {
                            selectedConditionsIdsSet.add(selectedConditionId);
                        }
                        // get the Condition objects and add them to the selected conditions map
                        List<Condition> allConditionsList = allConditionsMap.get(checkerId);
                        List<Condition> selectedConditionsList = new ArrayList<Condition>();
                        if (allConditionsList != null)
                        {
                            for (Condition condition : allConditionsList)
                            {
                                if (selectedConditionsIdsSet.contains(condition.getId()))
                                {
                                    selectedConditionsList.add(condition);
                                }
                            }
                        }
                        selectedConditionsMap.put(checkerId, selectedConditionsList);
                    }
                }
            }
            // create an empty list for the checkers that had no preferences set (new checkers, for example)
            for (CheckerDescription checkerDescription : checkersDescriptions)
            {
                if (selectedConditionsMap.get(checkerDescription.getId()) == null)
                {
                    selectedConditionsMap.put(checkerDescription.getId(),
                            new ArrayList<Condition>());
                }
            }

            /*
             * Get Extended Properties
             */
            // Checker parameters
            loadExtendedProperty(checkerParams, preferenceStore,
                    PreflightingUIPlugin.CHECKERS_PARAMS_PREFERENCE_KEY, String.class);

            // Custom checker warning levels
            loadExtendedProperty(customCheckersWarningLevels, preferenceStore,
                    PreflightingUIPlugin.CHECKERS_WARNING_LEVELS_PREFERENCE_KEY, Boolean.class);

            // Custom conditions warning levels
            loadExtendedProperty(customConditionsWarningLevels, preferenceStore,
                    PreflightingUIPlugin.CHECKERS_CONDITIONS_WARNING_LEVELS_PREFERENCE_KEY,
                    Boolean.class);

            checkersTableViewer.refresh();

        }
    }

    /**
     * Load saved extend property. The extended properties are related to
     * a checker or condition, and are saved in the form:
     * <id>,<extended_property>;<id>,<extended_property>...
     * 
     * These properties are loaded in a map <id> -> <extended_property>
     * 
     * @param map the map where the information will be loaded
     * @param preferenceStore the preference store
     * @param preferenceName the preference key used to store
     */
    @SuppressWarnings(
    {
            "rawtypes", "unchecked"
    })
    private void loadExtendedProperty(Map map, IPreferenceStore preferenceStore,
            String preferenceName, Class valueType)
    {

        String preference = preferenceStore.getString(preferenceName);
        if (preference.length() > 0)
        {
            StringTokenizer tokenizer = new StringTokenizer(preference, ";");
            while (tokenizer.hasMoreElements())
            {
                String extendedProperty = tokenizer.nextToken();
                String[] split = extendedProperty.split(",");
                if (split.length == 2)
                {
                    String id = split[0];
                    String params = split[1];
                    // string (checker param)
                    if (valueType.getName().equals("java.lang.String"))
                    {
                        map.put(id, params);
                    }
                    // boolean (warning levels)
                    else
                    {
                        map.put(id, new Boolean(params));
                    }
                }
            }
        }
    }

    /**
     * Saved extended properties. The extended properties are related to
     * a checker or condition, and are saved in the form:
     * <id>,<extended_property>;<id>,<extended_property>...
     * 
     * The properties come from a map <id> -> <extended_property>
     * 
     * @param map a map with the extended properties
     * @param preferenceStore the preference store
     * @param preferenceName the preference key used to store
     */
    private void saveExtendedProperty(Map<String, ?> map, IPreferenceStore preferenceStore,
            String preferenceName)
    {
        StringBuilder stringBuilder = new StringBuilder();
        if (!map.isEmpty())
        {
            for (String id : map.keySet())
            {
                String params = ((map.get(id) != null) ? map.get(id).toString() : null);
                if ((params != null) && (params.length() > 0))
                {
                    stringBuilder.append(id);
                    stringBuilder.append(","); //$NON-NLS-1$
                    stringBuilder.append(params);
                    stringBuilder.append(";"); //$NON-NLS-1$
                }
            }
            deleteLastChar(stringBuilder);
        }
        preferenceStore.setValue(preferenceName, stringBuilder.toString());
    }

    /**
     * Get the CheckerDescription object for a fiven checkerId passed as parameter
     * 
     * @param checkerId the checker id of the object to be retrieved
     * @return the CheckerDescription object that represents the checker with the id passed as parameter
     */
    private CheckerDescription getCheckerDescription(String checkerId)
    {
        CheckerDescription checkerDescriptionFound = null;
        Iterator<CheckerDescription> it = checkersDescriptions.iterator();
        while ((checkerDescriptionFound == null) && it.hasNext())
        {
            CheckerDescription checkerDescription = it.next();
            if (checkerDescription.getId().equals(checkerId))
            {
                checkerDescriptionFound = checkerDescription;
            }
        }
        return checkerDescriptionFound;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite#isValid()
     */
    @Override
    public IStatus isValid()
    {
        IStatus status = Status.OK_STATUS;
        boolean isValid = true;
        boolean hasWarning = false;
        String msg = "";

        // check if no checker is selected
        if (checkersTableViewer.getCheckedElements().length == 0)
        {
            isValid = false;
            msg = PreflightingUiNLS.CheckersTabComposite_Validation_Error_No_Checker;
        }

        // check if there are parameters for checkers that have custom conditions selection
        Object[] checkerDescriptions = checkersTableViewer.getCheckedElements();
        if (checkerDescriptions.length > 0)
        {
            for (Object checkerDescObj : checkerDescriptions)
            {
                String checkerId = ((CheckerDescription) checkerDescObj).getId();
                if (((checkerParams.get(checkerId) != null) && !checkerParams.get(checkerId)
                        .equals("")))
                {
                    if (hasCustomConditionsSelection(checkerId))
                    {
                        hasWarning = true;
                        msg =
                                PreflightingUiNLS.CheckersTabComposite_Validation_Warning_Param_Problem;
                    }
                }
            }
        }

        if (!isValid)
        {
            status = new Status(IStatus.ERROR, PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID, msg);
        }
        else if (hasWarning)
        {
            status =
                    new Status(IStatus.WARNING, PreflightingUIPlugin.PREFLIGHTING_UI_PLUGIN_ID, msg);
        }

        return status;
    }

    /**
     * Check if there are unselected conditions for the checker passed as parameter
     * 
     * @param checkerId the checker to have the conditions selection analysed
     * @return true if there is custom selection for the conditions (i.e. unselected conditions), false otherwise
     */
    private boolean hasCustomConditionsSelection(String checkerId)
    {
        boolean result = false;
        List<Condition> selectedConditions = selectedConditionsMap.get(checkerId);

        if ((selectedConditions != null)
                && (selectedConditionsMap.get(checkerId).size() != allConditionsMap.get(checkerId)
                        .size()))
        {
            result = true;
        }

        return result;
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite#performDefaults()
     */
    @Override
    public void performDefaults()
    {

        /*
         * Checkers - select all
         */
        // model / UI
        checkersTableViewer.setAllChecked(true);
        selectAllCheckersCheck.setSelection(true);

        // model
        for (CheckerDescription checkerDescription : checkersDescriptions)
        {
            selectAllConditions(checkerDescription.getId());
        }
        // UI
        conditionsTableViewer.setAllChecked(true);
        selectAllConditionsCheck.setSelection(true);

        /*
         * Extended Properties
         */
        // model
        checkerParams.clear();
        customCheckersWarningLevels.clear();
        customConditionsWarningLevels.clear();

        /*
         * Update UIs
         */
        checkersTableViewer.refresh();
        conditionsTableViewer.refresh();

    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite#performOk(org.eclipse.jface.preference.IPreferenceStore)
     */
    @Override
    public void performOk(IPreferenceStore preferenceStore)
    {
        StringBuilder stringBuilder;

        /*
         * Checkers information
         */
        Object[] checkerDescriptions = checkersTableViewer.getCheckedElements();

        stringBuilder = new StringBuilder();
        if (checkerDescriptions.length > 0)
        {
            for (Object checkerDescObj : checkerDescriptions)
            {
                CheckerDescription checkerDescription = (CheckerDescription) checkerDescObj;
                stringBuilder.append(checkerDescription.getId());
                stringBuilder.append(","); //$NON-NLS-1$
            }
            deleteLastChar(stringBuilder);
            preferenceStore.putValue(PreflightingUIPlugin.CHECKERS_PREFERENCE_KEY,
                    stringBuilder.toString());
        }
        else
        {
            preferenceStore.putValue(PreflightingUIPlugin.CHECKERS_PREFERENCE_KEY,
                    NO_CHECKERS_SELECTED);
        }

        stringBuilder = new StringBuilder();
        if (!selectedConditionsMap.isEmpty())
        {
            for (String checkerId : selectedConditionsMap.keySet())
            {
                stringBuilder.append(checkerId);
                stringBuilder.append(":");
                for (Condition condition : selectedConditionsMap.get(checkerId))
                {
                    stringBuilder.append(condition.getId());
                    stringBuilder.append(","); //$NON-NLS-1$
                }
                deleteLastChar(stringBuilder);
                stringBuilder.append(";");
            }
            deleteLastChar(stringBuilder);
        }
        preferenceStore.setValue(PreflightingUIPlugin.CHECKERS_CONDITIONS_PREFERENCE_KEY,
                stringBuilder.toString());

        /*
         * Extended Properties
         */
        // Checker parameters
        saveExtendedProperty(checkerParams, preferenceStore,
                PreflightingUIPlugin.CHECKERS_PARAMS_PREFERENCE_KEY);
        // Custom checker warning levels
        saveExtendedProperty(customCheckersWarningLevels, preferenceStore,
                PreflightingUIPlugin.CHECKERS_WARNING_LEVELS_PREFERENCE_KEY);
        // Custom conditions warning levels
        saveExtendedProperty(customConditionsWarningLevels, preferenceStore,
                PreflightingUIPlugin.CHECKERS_CONDITIONS_WARNING_LEVELS_PREFERENCE_KEY);

    }

    /**
     * Delete the last char of a string builder
     * 
     * @param stringBuilder the string builder to have the last char deleted
     */
    private void deleteLastChar(StringBuilder stringBuilder)
    {
        if (stringBuilder.length() > 0)
        {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
    }

    /* (non-Javadoc)
     * @see com.motorolamobility.preflighting.ui.tabs.AbstractAppValidatorTabComposite#commandLineBuilder()
     */
    @Override
    public String commandLineBuilder()
    {
        String commandline = "";

        boolean hasCustomCheckers = !areAllItemsChecked(checkersTableViewer);
        boolean hasCustomParameters = hasParameters();
        boolean hasCustomConditions = hasCustomConditions();
        boolean hasCustomWarningLevels = hasCustomWarningLevels();

        // check if there are custom configurations
        if (hasCustomCheckers || hasCustomParameters || hasCustomConditions
                || hasCustomWarningLevels)
        {
            Object[] checkedElements = checkersTableViewer.getCheckedElements();
            StringBuilder stringBuilder = new StringBuilder();

            Boolean customWarningLevel = null;
            List<String> elementsToIncreaseWarningLevel = new ArrayList<String>();
            List<String> elementsToDecreaseWarningLevel = new ArrayList<String>();

            /*
             * Iterate in the selected checkers
             */
            for (Object checkedObj : checkedElements)
            {
                CheckerDescription checkerDescription = (CheckerDescription) checkedObj;
                String checkerId = checkerDescription.getId();
                List<Condition> selectedConditions =
                        ((selectedConditionsMap.get(checkerId) != null) ? selectedConditionsMap
                                .get(checkerId) : new ArrayList<Condition>());

                // Have custom parameters or there are checkers disabled
                // just create the command line block to 
                // select the checker and to pass the appropriate parameters, if any
                if (hasCustomParameters
                        || (checkedElements.length < checkersTableViewer.getTable().getItems().length))
                {
                    stringBuilder.append("-c ");
                    stringBuilder.append(checkerId);
                    stringBuilder.append(" ");

                    // params only apply if there are no unselected/custom conditions
                    String parameters = checkerParams.get(checkerId);
                    if ((parameters != null) && (parameters.length() > 0))
                    {
                        stringBuilder.append(parameters);
                        stringBuilder.append(" ");
                    }
                }
                // Given that there are conditions disabled, create the command line block
                // to enable only the selected ones. Additionally, define the custom warning level
                // for that condition, if any
                List<Condition> allConditions = allConditionsMap.get(checkerId);
                if (allConditions != null)
                {
                    for (Condition condition : allConditions)
                    {
                        if ((selectedConditions != null) && !selectedConditions.contains(condition))
                        {
                            //Condition is NOT selected => add it to -dc (disable condition) command
                            stringBuilder.append("-dc ");
                            stringBuilder.append(checkerId);
                            stringBuilder.append(".");
                            stringBuilder.append(condition.getId());
                            stringBuilder.append(" ");
                        }
                    }
                }

                /*
                 * Build the list of items that must have their warning levels changed
                 */
                // checker
                customWarningLevel = customCheckersWarningLevels.get(checkerId);
                if (customWarningLevel != null)
                {
                    if (customWarningLevel)
                    {
                        elementsToIncreaseWarningLevel.add(checkerId);
                    }
                    else
                    {
                        elementsToDecreaseWarningLevel.add(checkerId);
                    }
                }
                // conditions
                for (Condition condition : ((selectedConditions != null) ? selectedConditions
                        : allConditionsMap.get(checkerId)))
                {
                    customWarningLevel = customConditionsWarningLevels.get(condition.getId());
                    if (customWarningLevel != null)
                    {
                        if (customWarningLevel)
                        {
                            elementsToIncreaseWarningLevel.add(checkerId + "." + condition.getId());
                        }
                        else
                        {
                            elementsToDecreaseWarningLevel.add(checkerId + "." + condition.getId());
                        }
                    }

                }

            }

            /*
             * Custom warning levels
             */
            stringBuilder.append(createCustomWarningLevelCommandLine(true,
                    elementsToIncreaseWarningLevel));
            stringBuilder.append(createCustomWarningLevelCommandLine(false,
                    elementsToDecreaseWarningLevel));

            commandline = stringBuilder.toString().trim();

        }

        return commandline;

    }

    /**
     * Create the command line piece which increases or decreases the warning level
     * for certain element (checker or condition)
     * 
     * @param increase true if the warning level must be increased, false if it must be decreased, null if the warning level must not change
     * @param element the element to have its warning level changed
     * @return the command line piece
     */
    public String createCustomWarningLevelCommandLine(Boolean increase, List<String> elements)
    {
        StringBuilder result = new StringBuilder();

        if ((increase != null) && (elements.size() > 0))
        {
            if (increase)
            {
                result.append("-wx"); // increase
            }
            else
            {
                result.append("-xw"); // decrease
            }
            result.append(" ");
            for (String element : elements)
            {
                result.append(element);
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Check if there are parameters set for any checker
     * 
     * @return true if there are parameters set, false otherwise
     */
    private boolean hasParameters()
    {
        boolean paramsFound = false;

        Iterator<String> it = checkerParams.values().iterator();
        while (it.hasNext() && !paramsFound)
        {
            String checkerParam = it.next();
            if (checkerParam.length() > 0)
            {
                paramsFound = true;
            }
        }

        return paramsFound;
    }

    /**
     * Check if there are unselected/custom conditions.
     * This is used to decide if the App Validator command line must be created or not
     * 
     * @return true if there are custom unselected/custom conditions, false otherwise
     */
    private boolean hasCustomConditions()
    {
        boolean result = false;

        for (String checkerId : allConditionsMap.keySet())
        {
            List<Condition> selectedConditions =
                    ((selectedConditionsMap.get(checkerId) != null) ? selectedConditionsMap
                            .get(checkerId) : new ArrayList<Condition>());
            if (allConditionsMap.get(checkerId).size() != selectedConditions.size())
            {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Check if there are custom warning levels
     * This is used to decide if the App Validator command line must be created or not
     * 
     * @return true if there are custom warning levels, false otherwise
     */
    private boolean hasCustomWarningLevels()
    {

        boolean result = false;

        List<Map<String, Boolean>> maps = new ArrayList<Map<String, Boolean>>();
        maps.add(customCheckersWarningLevels);
        maps.add(customConditionsWarningLevels);

        for (Map<String, Boolean> map : maps)
        {
            for (Boolean customWarningLevel : map.values())
            {
                if (customWarningLevel != null)
                {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
