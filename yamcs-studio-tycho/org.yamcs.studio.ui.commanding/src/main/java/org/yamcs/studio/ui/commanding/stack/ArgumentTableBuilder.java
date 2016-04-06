package org.yamcs.studio.ui.commanding.stack;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.yamcs.protobuf.Mdb.ArgumentInfo;
import org.yamcs.protobuf.Mdb.ArgumentTypeInfo;
import org.yamcs.protobuf.Mdb.EnumValue;

/*
 * Build an editable table to enter command's argument
 * TODO: provide support to specific type editor (e.g. combobox for enums)
 */
public class ArgumentTableBuilder {

    final String FLOAT = "float";
    final String DOUBLE = "double";
    final String INT = "integer";
    final String ENUM = "enumeration";

    StackedCommand command;

    public ArgumentTableBuilder(StackedCommand command) {
        this.command = command;
    }

    public TableViewer createArgumentTable(Composite shell) {

        TableColumnLayout tcl = new TableColumnLayout();
        Composite argumentsComposite = new Composite(shell, SWT.NONE);
        argumentsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        argumentsComposite.setLayout(tcl);

        TableViewer argumentTable = new TableViewer(argumentsComposite, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        Table table = argumentTable.getTable();
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(gridData);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        argumentTable.setContentProvider(new ArrayContentProvider());

        // create columns
        String[] titles = { "Argument", "Eng. Type", "Range", "Value" };

        // argument
        TableViewerColumn column = createTableViewerColumn(argumentTable, titles[0], 0);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ArgumentAssignement)
                    return ((ArgumentAssignement) element).arg.getName();
                return super.getText(element);
            }
        });
        tcl.setColumnData(column.getColumn(), new ColumnPixelData(10));

        // eng. type
        column = createTableViewerColumn(argumentTable, titles[1], 1);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ArgumentAssignement)
                    return ((ArgumentAssignement) element).arg.getType().getEngType();
                return super.getText(element);
            }
        });
        tcl.setColumnData(column.getColumn(), new ColumnPixelData(10));

        // range
        column = createTableViewerColumn(argumentTable, titles[2], 2);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ArgumentAssignement) {
                    ArgumentTypeInfo ati = ((ArgumentAssignement) element).arg.getType();
                    String format = INT.equals(ati.getEngType()) ? "%.0f" : "%f";
                    String range = "";
                    if (ati.hasRangeMin()) {
                        range = "[" + String.format(format, ati.getRangeMin()) + ", ";
                    }
                    if (ati.hasRangeMax()) {
                        range += String.format(format, ati.getRangeMax()) + "]";
                    }
                    return range;
                }
                return super.getText(element);
            }
        });
        tcl.setColumnData(column.getColumn(), new ColumnPixelData(0));

        // value
        column = createTableViewerColumn(argumentTable, titles[3], 3);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ArgumentAssignement)
                    return ((ArgumentAssignement) element).value;
                return super.getText(element);
            }
        });
        column.setEditingSupport(new ParameterEditingSupport(argumentTable));
        tcl.setColumnData(column.getColumn(), new ColumnWeightData(100));

        return argumentTable;
    }

    public void pack(TableViewer argumentTable) {
        argumentTable.getTable().getColumn(0).pack();
        argumentTable.getTable().getColumn(1).pack();
        argumentTable.getTable().getColumn(2).setText("");
        argumentTable.getTable().getColumn(2).pack();
        argumentTable.getTable().getColumn(2).setText("Range");
        if (argumentTable.getTable().getColumn(2).getWidth() < 20)
            argumentTable.getTable().getColumn(2).setWidth(0); // don't show range if there is no data
        else
            argumentTable.getTable().getColumn(2).pack();

    }

    private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, String header, int idx) {
        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.LEFT, idx);
        column.getColumn().setText(header);
        column.getColumn().setResizable(true);
        return column;
    }

    static class ArgumentAssignement {
        public ArgumentAssignement(ArgumentInfo arg, String value) {
            this.arg = arg;
            this.value = value;
        }

        ArgumentInfo arg;
        String value;
    }

    class ParameterEditingSupport extends EditingSupport {
        private final TableViewer viewer;
        private final CellEditor editor;
        private final CellEditor editorSpinner;
        private final CellEditor editorSpinnerDecimal;
        //  private final CellEditor editorCombobox;

        public ParameterEditingSupport(TableViewer viewer) {
            super(viewer);
            this.viewer = viewer;
            this.editor = new TextCellEditor(viewer.getTable());
            this.editorSpinner = new TextCellEditor(viewer.getTable());
            this.editorSpinnerDecimal = new TextCellEditor(viewer.getTable());
            //    this.editorCombobox = new TextCellEditor(viewer.getTable());
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            ArgumentAssignement aa = (ArgumentAssignement) element;
            String engType = aa.arg.getType().getEngType();
            if (FLOAT.equals(engType) ||
                    DOUBLE.equals(engType)) {
                return editorSpinnerDecimal;
            } else if (INT.equals(engType)) {
                return editorSpinner;
            } else if (ENUM.equals(engType)) {
                return newComboEditor(aa.arg);
            } else {
                return editor;
            }
        }

        private CellEditor newComboEditor(ArgumentInfo arg) {
            List<String> enumValues = new ArrayList<String>();
            for (EnumValue ev : arg.getType().getEnumValueList()) {
                enumValues.add(ev.getLabel());
            }

            ComboBoxCellEditor comboBox = new ComboBoxCellEditor(viewer.getTable(),
                    enumValues.toArray(new String[enumValues.size()]),
                    SWT.READ_ONLY);
            return comboBox;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        @Override
        protected Object getValue(Object element) {

            ArgumentAssignement aa = (ArgumentAssignement) element;
            String engType = aa.arg.getType().getEngType();

            if (ENUM.equals(engType)) {
                for (int i = 0; i < aa.arg.getType().getEnumValueList().size(); i++) {
                    if (aa.value.equals(aa.arg.getType().getEnumValue(i).getLabel())) {
                        // combobox cell editor works with integers...
                        return i;
                    }
                }
                return 0;
            }

            return ((ArgumentAssignement) element).value;
        }

        @Override
        protected void setValue(Object element, Object userInputValue) {

            ArgumentAssignement aa = (ArgumentAssignement) element;
            String engType = aa.arg.getType().getEngType();

            String value = "";

            // Store enum values
            if (ENUM.equals(engType)) {
                try {
                    Integer userValue = (Integer) userInputValue;
                    value = aa.arg.getType().getEnumValue(userValue).getLabel();
                } catch (Exception e) {
                }
            } else {
                // Store other type of arguments
                value = String.valueOf(userInputValue);
            }

            if (value.trim().isEmpty()) {
                command.addAssignment(aa.arg, null);
            } else {
                command.addAssignment(aa.arg, value);
            }

            // update table
            aa.value = String.valueOf(value);
            viewer.update(element, null);
        }

    }

    public class ArrayContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof ArrayList<?>)
                return ((ArrayList<?>) inputElement).toArray();
            return null;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

}
