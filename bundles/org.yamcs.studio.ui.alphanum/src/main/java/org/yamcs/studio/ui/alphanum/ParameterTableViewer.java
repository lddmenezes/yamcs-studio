package org.yamcs.studio.ui.alphanum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.yamcs.protobuf.Mdb.ParameterInfo;
import org.yamcs.protobuf.Pvalue.ParameterValue;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.studio.core.model.ParameterCatalogue;
import org.yamcs.studio.css.core.PVCatalogue;
import org.yamcs.studio.css.core.pvmanager.PVConnectionInfo;
import org.yamcs.studio.css.core.pvmanager.YamcsPVReader;

public class ParameterTableViewer extends TableViewer {

    public static final String COL_NAME = "Parameter";
    public static final String COL_ENG = "Eng Value";
    public static final String COL_RAW = "Raw Value";
    public static final String COL_TIME = "Generation";
    public static final String COL_AQU_TIME = "Aquisition";
    
    ParameterContentProvider contentProvider;
	private Map<ParameterInfo, ParameterReader> readers;
	
	public ParameterTableViewer(Composite parent) {
		super(new Table(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL));;
        TableColumnLayout tcl = new TableColumnLayout();
        parent.setLayout(tcl);
		
		getTable().setHeaderVisible(true);
        getTable().setLinesVisible(true);
        readers = new HashMap<>();
        addFixedColumns(tcl);
        contentProvider = new ParameterContentProvider(this);
        setContentProvider(contentProvider);
        setInput(contentProvider);
        
	}

	private void addFixedColumns(TableColumnLayout tcl) {

        TableViewerColumn nameColumn = new TableViewerColumn(this, SWT.LEFT);
        nameColumn.getColumn().setText(COL_NAME);
        tcl.setColumnData(nameColumn.getColumn(), new ColumnWeightData(40));
        nameColumn.setLabelProvider(new ColumnLabelProvider() {           
        	
        	@Override 
            public String getText(Object element) {
            	ParameterInfo cnt = (ParameterInfo) element;
                          
                return cnt.getQualifiedName();
            }
        });

        TableViewerColumn engValueColumn = new TableViewerColumn(this, SWT.RIGHT);
        engValueColumn.getColumn().setText(COL_ENG);
        tcl.setColumnData(engValueColumn.getColumn(), new ColumnWeightData(10));
        engValueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override 
            public String getText(Object element) {
            	ParameterReader reader = readers.get(element);
            	if(reader == null || reader.getValue() == null)
            		return "-";
            	ParameterValue value = reader.getValue();
                return String.valueOf(getValue(value.getEngValue()));
            }
        });

        TableViewerColumn rawValueColumn = new TableViewerColumn(this, SWT.RIGHT);
        rawValueColumn.getColumn().setText(COL_RAW);
        tcl.setColumnData(rawValueColumn.getColumn(), new ColumnWeightData(10));
        rawValueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override 
            public String getText(Object element) {
            	ParameterReader reader = readers.get(element);
            	if(reader == null || reader.getValue() == null)
            		return "-";
            	ParameterValue value = reader.getValue();
                return String.valueOf(getValue(value.getRawValue()));
            }
        });
        
        TableViewerColumn gentimeValueColumn = new TableViewerColumn(this, SWT.LEFT);
        gentimeValueColumn.getColumn().setText(COL_TIME);
        tcl.setColumnData(gentimeValueColumn.getColumn(), new ColumnWeightData(20));
        gentimeValueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override 
            public String getText(Object element) {
            	ParameterReader reader = readers.get(element);
            	if(reader == null || reader.getValue() == null)
            		return "-";
            	ParameterValue value = reader.getValue();
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date time = new Date(value.getGenerationTime());
                String strDate = sdfDate.format(time);
                return strDate;
            }
        });
        
        TableViewerColumn aqutimeValueColumn = new TableViewerColumn(this, SWT.LEFT);
        aqutimeValueColumn.getColumn().setText(COL_AQU_TIME);
        tcl.setColumnData(aqutimeValueColumn.getColumn(), new ColumnWeightData(20));
        aqutimeValueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override 
            public String getText(Object element) {
            	ParameterReader reader = readers.get(element);
            	if(reader == null || reader.getValue() == null)
            		return "-";
            	ParameterValue value = reader.getValue();
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date time = new Date(value.getAcquisitionTime());
                String strDate = sdfDate.format(time);
                return strDate;
            }
        });

        // Common properties to all columns
        List<TableViewerColumn> columns = new ArrayList<>();
        columns.add(nameColumn);
        columns.add(engValueColumn);
        columns.add(rawValueColumn);
        columns.add(gentimeValueColumn);
        columns.add(aqutimeValueColumn);
        for (TableViewerColumn column : columns) {
            // prevent resize to 0
            column.getColumn().addControlListener(new ControlListener() {
                @Override
                public void controlMoved(ControlEvent e) {
                }

                @Override
                public void controlResized(ControlEvent e) {
                    if (column.getColumn().getWidth() < 5)
                        column.getColumn().setWidth(5);
                }
            });
        }

    }
    

    public void addParameter(ParameterInfo element) {
    	if(contentProvider.addParameter(element)) {
    		ParameterReader reader = new ParameterReader(element);
    		readers.put(element, reader);
    		PVCatalogue.getInstance().register(reader);
    	}
    }
    
    public void removeParameter(ParameterInfo info) {
    	PVCatalogue.getInstance().unregister(readers.get(info));
    	readers.remove(info);
    	contentProvider.remove(info);
		refresh();
    }
    
	public void restoreParameters() {
		clear();
		for(ParameterInfo info : contentProvider.getInitial()) {
			addParameter(info);
		}
		refresh();
		
	}
    
	public void clear() {
		for(ParameterInfo info : readers.keySet()) {
			PVCatalogue.getInstance().unregister(readers.get(info));
		}
		readers.clear();
		contentProvider.clearAll();
		refresh();
				
	}
    
	public List<ParameterInfo> getParameters() {
		return contentProvider.getParameter();
	}
	
	public boolean hasChanged() {
		return contentProvider.hasChanged();
	}
    
	private Object getValue(Value value) {
		Object obj = null;
		if(value.hasStringValue())
			obj= value.getStringValue();
		else if(value.hasSint64Value())
			obj= value.getSint64Value();
		else if(value.hasSint32Value()) 
			obj= value.getSint32Value();
		else if(value.hasUint64Value())
			obj= value.getUint64Value();
		else if(value.hasUint32Value()) 
			obj= value.getUint32Value();
		else if(value.hasDoubleValue())
			obj= value.getDoubleValue();
		else if(value.hasFloatValue())
			obj= value.getFloatValue();
		else if(value.hasBooleanValue())
			obj= value.getBooleanValue();
		if(obj == null)
			return "-";
		return obj;
	}
    
    
    class ParameterReader implements YamcsPVReader{

    	NamedObjectId id;
    	ParameterInfo info;
    	ParameterValue value;
    	


		public ParameterReader(ParameterInfo info) {
    		for(ParameterInfo parameter: ParameterCatalogue.getInstance().getMetaParameters()) {
    			if(info.getQualifiedName().equals(parameter.getQualifiedName())) {
    				id = parameter.getAliasList().get(0);
    			}
    		}
    		this.info = info;
		}
    	
		@Override
		public void reportException(Exception e) {
			;
			
		}

		@Override
		public NamedObjectId getId() {
			return id;
		}
		
    	public ParameterValue getValue() {
			return value;
		}

		@Override
		public void processConnectionInfo(PVConnectionInfo info) {
			if(!info.connected) {
				//TODO do something
			}		
		}

		@Override
		public void processParameterValue(ParameterValue pval) {
			value = pval;
			if (getTable().isDisposed()) {
				return;
			}
				Display.getDefault().asyncExec( () -> {
					if (!getTable().isDisposed()) {
					ParameterTableViewer.this.refresh();
					}
				});	
		}
		    	
    }

}
