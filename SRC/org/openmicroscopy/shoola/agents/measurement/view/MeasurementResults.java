/*
 * org.openmicroscopy.shoola.agents.measurement.view.MeasurementResults 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.measurement.view;


//Java imports
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.measurement.IconManager;
import org.openmicroscopy.shoola.agents.measurement.MeasurementAgent;
import org.openmicroscopy.shoola.agents.measurement.util.TabPaneInterface;
import org.openmicroscopy.shoola.agents.measurement.util.model.AnnotationDescription;
import org.openmicroscopy.shoola.agents.measurement.util.model.AnnotationField;
import org.openmicroscopy.shoola.agents.measurement.util.model.MeasurementObject;
import org.openmicroscopy.shoola.agents.measurement.util.ui.AttributeUnits;
import org.openmicroscopy.shoola.agents.measurement.util.ui.ResultsCellRenderer;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.ui.UserNotifier;
import org.openmicroscopy.shoola.util.file.ExcelWriter;
import org.openmicroscopy.shoola.util.filter.file.ExcelFilter;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.roi.figures.ROIFigure;
import org.openmicroscopy.shoola.util.roi.model.ROI;
import org.openmicroscopy.shoola.util.roi.model.ROIShape;
import org.openmicroscopy.shoola.util.roi.model.annotation.AnnotationKey;
import org.openmicroscopy.shoola.util.roi.model.annotation.AnnotationKeys;
import org.openmicroscopy.shoola.util.roi.model.annotation.MeasurementAttributes;
import org.openmicroscopy.shoola.util.roi.model.util.MeasurementUnits;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import org.openmicroscopy.shoola.util.ui.filechooser.FileChooser;

/** 
 * UI component displaying various value computed on a Region of Interest.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
class MeasurementResults
	extends JPanel
	implements ActionListener, TabPaneInterface
{
	
	/** Index to identify tab */
	public final static int		INDEX = MeasurementViewerUI.RESULTS_INDEX;

	/** The default size of the column in a table. */
	private static final int	COLUMNWIDTH = 64;

	/** ROI ID Column no for the wizard. */
	private static final int	ROIID_COLUMN = 0;

	/** Time point Column no for the wizard. */
	private static final int	TIME_COLUMN = 1;
	
	/** Z-Section Column no for the wizard. */
	private static final int	Z_COLUMN = 2;
	
	/** Identifies the save action. */
	private static final int	SAVE = 0;
	
	/** Identifies the refresh action. */
	private static final int	REFRESH = 1;
	
	/** Identifies the wizard action. */
	private static final int	WIZARD = 2;
	
	/** The name of the panel. */
	private static final String	NAME = "Results";
	
	/** Collection of column names. */
	private List<KeyDescription>			columnNames;
	
	/** Collection of column names. */
	private List<AnnotationField>			fields;
	
	/** Collection of column names for all possible fields. */
	private List<AnnotationField>			allFields;
	
	/** Button to save locally the results. */
	private JButton							saveButton;

	/** Button to save locally the results. */
	private JButton							refreshButton;
	
	/** Button to launch the results wizard. */
	private JButton 						resultsWizardButton;
	
	/** The table displaying the results. */
	private ResultsTable					results;
	
	/** The scroll pane for the results. */
	private JScrollPane						scrollPane;
	
	/** Reference to the control. */
	private MeasurementViewerControl		controller;
	
	/** Reference to the model. */
	private MeasurementViewerModel			model;
	
	/** Reference to the View. */
	private MeasurementViewerUI				view;
	
	/** 
	 * The table selection listener attached to the table displaying the 
	 * objects.
	 */
	private ListSelectionListener			listener;

	/**
	 * overridden version of {@line TabPaneInterface#getIndex()}
	 */
	public int getIndex() {return INDEX; }
	
	/** Initializes the components composing the display. */
	private void initComponents()
	{
		saveButton = new JButton("Save To Excel...");
		saveButton.setToolTipText("Save the results to Excel.");
		saveButton.setActionCommand(""+SAVE);
		saveButton.addActionListener(this);
		refreshButton = new JButton("Refresh");
		refreshButton.setToolTipText("Refresh the results table.");
		refreshButton.setActionCommand(""+REFRESH);
		refreshButton.addActionListener(this);
		resultsWizardButton = new JButton("Results Wizard...");
		resultsWizardButton.setToolTipText("Bring up the results wizard.");
		resultsWizardButton.setActionCommand(""+WIZARD);
		resultsWizardButton.addActionListener(this);
		
		refreshButton.setEnabled(false);
		saveButton.setEnabled(false);
		//Create table model.
		createAllFields();
		createDefaultFields();
		results = new ResultsTable();
		results.getTableHeader().setReorderingAllowed(false);
		MeasurementTableModel tm = new MeasurementTableModel(columnNames, 
				model.getMeasurementUnits());
		results.setModel(tm);
		results.setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		results.setRowSelectionAllowed(true);
		results.setColumnSelectionAllowed(false);
		listener = new ListSelectionListener() {
			
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;

		        ListSelectionModel lsm =
		            (ListSelectionModel) e.getSource();
		        if (lsm.isSelectionEmpty()) {
		        } else {
		        	int index = lsm.getMinSelectionIndex();
		        	if (index < 0) return;
		        	MeasurementTableModel m = 
	        			(MeasurementTableModel) results.getModel();
		        	long ROIID;
		        	int t, z;
		        	try
	        		{
		        		ROIID = (Long) m.getValueAt(index, ROIID_COLUMN);
		        		t = (Integer) m.getValueAt(index, TIME_COLUMN)-1;
		        		z = (Integer) m.getValueAt(index, Z_COLUMN)-1;
	        			ROI roi = model.getROI(ROIID);
	        			if(roi==null)
	        				return;
	        			view.selectFigure(ROIID, t, z);
	        		}
	        		catch(Exception exception)
	        		{
	        			Registry reg = MeasurementAgent.getRegistry();
	        	    	reg.getUserNotifier().notifyWarning("ROI does not exist",
	        	    	"ROI does not exist. Results may be out of date," +
	        	    	" try refreshing results.");
	        		}
		        }
			}
		
		};

		results.getSelectionModel().addListSelectionListener(listener);
	}
	
	/** Builds and lays out the GUI. */
	private void buildGUI()
	{
		setLayout(new BorderLayout());
		scrollPane = new JScrollPane(results);
		add(scrollPane, BorderLayout.CENTER);
		scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());
		scrollPane.setHorizontalScrollBar(
				scrollPane.createHorizontalScrollBar());
		JPanel panel = new JPanel();
		//panel.setLayout(new FlowLayout());
		panel.add(resultsWizardButton);
		panel.add(refreshButton);
		panel.add(saveButton);
		add(panel, BorderLayout.SOUTH);
	}

	/**
	 * Create the fields which can be selected from. 
	 *
	 */
	private void createAllFields()
	{
		allFields = new ArrayList<AnnotationField>();
		allFields.add(new AnnotationField(AnnotationKeys.TEXT,
						AnnotationDescription.annotationDescription.get(
							AnnotationKeys.TEXT), false)); 
		allFields.add(new AnnotationField(AnnotationKeys.CENTREX,
			AnnotationDescription.annotationDescription.get(
							AnnotationKeys.CENTREX), false)); 
		allFields.add(new AnnotationField(AnnotationKeys.CENTREY,
			AnnotationDescription.annotationDescription.get(
							AnnotationKeys.CENTREY), false)); 
		allFields.add(new AnnotationField(AnnotationKeys.AREA,
			AnnotationDescription.annotationDescription.get(AnnotationKeys.AREA)
				, false)); 
		allFields.add(new AnnotationField(AnnotationKeys.PERIMETER,
			AnnotationDescription.annotationDescription.get( AnnotationKeys.PERIMETER),
						false)); 
		allFields.add(new AnnotationField(AnnotationKeys.LENGTH, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.LENGTH),
						false)); 
		allFields.add(new AnnotationField(AnnotationKeys.WIDTH, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.WIDTH),
						false)); 
		allFields.add(new AnnotationField(AnnotationKeys.HEIGHT, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.HEIGHT),
						false)); 
		allFields.add(new AnnotationField(AnnotationKeys.ANGLE, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.ANGLE),
						false)); 
		allFields.add(new AnnotationField(AnnotationKeys.POINTARRAYX, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.POINTARRAYX),
				 false)); 
		allFields.add(new AnnotationField(AnnotationKeys.POINTARRAYY, 
						
			AnnotationDescription.annotationDescription.get(AnnotationKeys.POINTARRAYY),
				 false)); 
		allFields.add(new AnnotationField(AnnotationKeys.STARTPOINTX, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.STARTPOINTX),
				 false)); 
		allFields.add(new AnnotationField(AnnotationKeys.STARTPOINTY, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.STARTPOINTY),
				false)); 
		allFields.add(new AnnotationField(AnnotationKeys.ENDPOINTX,
			AnnotationDescription.annotationDescription.get(AnnotationKeys.ENDPOINTX),
				false)); 
		allFields.add(new AnnotationField(AnnotationKeys.ENDPOINTY,
			AnnotationDescription.annotationDescription.get(AnnotationKeys.ENDPOINTY),
				false)); 
	}
	
	/**
	 * Creates the default fields to show results of in the measurement tool.
	 */
	private void createDefaultFields()
	{
		fields = new ArrayList<AnnotationField>();	
		fields.add(new AnnotationField(AnnotationKeys.TEXT,
			AnnotationDescription.annotationDescription.get(
				AnnotationKeys.TEXT), false)); 
		fields.add(new AnnotationField(AnnotationKeys.CENTREX,
			AnnotationDescription.annotationDescription.get(
				AnnotationKeys.CENTREX), false)); 
		fields.add(new AnnotationField(AnnotationKeys.CENTREY,
			AnnotationDescription.annotationDescription.get(
				AnnotationKeys.CENTREY), false)); 
		fields.add(new AnnotationField(AnnotationKeys.AREA,
			AnnotationDescription.annotationDescription.get(AnnotationKeys.AREA)
	, false)); 
		fields.add(new AnnotationField(AnnotationKeys.LENGTH, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.LENGTH),
			false)); 
		fields.add(new AnnotationField(AnnotationKeys.WIDTH, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.WIDTH),
			false)); 
		fields.add(new AnnotationField(AnnotationKeys.HEIGHT, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.HEIGHT),
			false)); 
		fields.add(new AnnotationField(AnnotationKeys.ANGLE, 
			AnnotationDescription.annotationDescription.get(AnnotationKeys.ANGLE),
			false)); 
		columnNames = new ArrayList<KeyDescription>();
		columnNames.add(new KeyDescription(	AnnotationDescription.ROIID_STRING,
											AnnotationDescription.ROIID_STRING));
		columnNames.add(new KeyDescription( AnnotationDescription.TIME_STRING,
											AnnotationDescription.TIME_STRING));
		columnNames.add(new KeyDescription( AnnotationDescription.ZSECTION_STRING,
											AnnotationDescription.ZSECTION_STRING));
		columnNames.add(new KeyDescription( AnnotationDescription.SHAPE_STRING,
											AnnotationDescription.SHAPE_STRING));
		for (int i = 0 ; i < fields.size(); i++)
			columnNames.add(new KeyDescription(	fields.get(i).getKey().toString(),
												fields.get(i).getName()));
	}

	
	class KeyDescription
	{
		String key;
		String description;
		
		public KeyDescription(String key, String description)
		{
			this.key = key;
			this.description = description;
		}
		
		public String getKey() { return key;}
		public String getDescription() {return description;}
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param controller Reference to the Control. Mustn't be <code>null</code>.
	 * @param model		 Reference to the Model. Mustn't be <code>null</code>.
	 * @param view		 Reference to the View. Mustn't be <code>null</code>.
	 */
	MeasurementResults(MeasurementViewerControl	controller, 
					MeasurementViewerModel model, MeasurementViewerUI view)
	{
		if (controller == null)
			throw new IllegalArgumentException("No control.");
		if (model == null)
			throw new IllegalArgumentException("No model.");
		if (view == null)
			throw new IllegalArgumentException("No view.");
		this.controller = controller;
		this.model = model;
		this.view = view;
		initComponents();
		buildGUI();
	}
	
	/** Populate the table. */
	void populate()
	{
		TreeMap map = model.getROI();
		Iterator i = map.keySet().iterator();
		ROI roi;
		TreeMap shapes;
		Iterator j;
		ROIShape shape;
		ROIFigure figure;
		MeasurementObject row;
		AnnotationKey key;
		MeasurementTableModel tm = new MeasurementTableModel(columnNames, 
				model.getMeasurementUnits());
		
		while (i.hasNext()) {
			roi = (ROI) map.get(i.next());
			shapes = roi.getShapes();
			j = shapes.keySet().iterator();
			while (j.hasNext()) {
				shape = (ROIShape) shapes.get(j.next());
				figure = shape.getFigure();
				figure.calculateMeasurements();
				row = new MeasurementObject();
				row.addElement(shape.getROI().getID());
				row.addElement(shape.getCoord3D().getTimePoint()+1);
				row.addElement(shape.getCoord3D().getZSection()+1);
				row.addElement(shape.getFigure().getType());
				for (int k = 0; k < fields.size(); k++) {
					key = fields.get(k).getKey();
					Object value = key.get(shape);
					if (value instanceof List)
					{
						List valueArray = (List) value;
						List arrayList = new ArrayList(valueArray);
						row.addElement(arrayList);
					}
					else
						row.addElement(value);
				}
				tm.addRow(row);
			}
		}
		results.setModel(tm);
		resizeTableColumns();
	}

	/** 
	 * Resize the columns so that they fit the column names better, the 
	 * column will be a minimum size of COLUMNWIDTH or the length of the 
	 * text whichever is greater.
	 *
	 */
	private void resizeTableColumns()
	{	
		int columnWidth = 0;
		Font font = getFont();
		FontMetrics metrics = getFontMetrics( font );
		MeasurementTableModel tm = (MeasurementTableModel)results.getModel();
		for(int i = 0 ; i < results.getColumnCount(); i++)
		{
			TableColumn col = results.getColumnModel().getColumn(i);
			int w  =  metrics.stringWidth(tm.getColumnName(i));
			columnWidth = Math.max(w, COLUMNWIDTH);
			col.setMinWidth(columnWidth);
			col.setPreferredWidth(columnWidth);
		}
	}
	
	/**
	 * Returns the name of the component.
	 * 
	 * @return See above.
	 */
	String getComponentName() { return NAME; }
	
	/**
	 * Returns the icon of the component.
	 * 
	 * @return See above.
	 */
	Icon getComponentIcon()
	{
		IconManager icons = IconManager.getInstance();
		return icons.getIcon(IconManager.RESULTS);
	}
	
	/** 
	 * Save the results.
	 * 
	 * @throws IOException Thrown if the data cannot be written.
	 * @return true if results saved, false if users cancels save.
	 */
	boolean saveResults()
		throws IOException
	{
		List<FileFilter> filterList = new ArrayList<FileFilter>();
		FileFilter filter=new ExcelFilter();
		filterList.add(filter);
		FileChooser chooser=
				new FileChooser(
					view, FileChooser.SAVE, "Save the Results",
					"Save the Results data to a file which can be loaded " +
					"by a spreadsheet.",
					filterList);
		File f = UIUtilities.getDefaultFolder();
	    if (f != null) chooser.setCurrentDirectory(f);
		int choice = chooser.showDialog();
		if (choice != JFileChooser.APPROVE_OPTION) return false;
		File file = chooser.getSelectedFile();
		if (!file.getAbsolutePath().endsWith(ExcelFilter.EXCEL))
		{
			String fileName = file.getAbsolutePath()+"."+ExcelFilter.EXCEL;
			file = new File(fileName);
		}
		String filename = file.getAbsolutePath();
		ExcelWriter writer = new ExcelWriter(filename);
		writer.openFile();
		writer.createSheet("Measurement Results");
		writer.writeTableToSheet(0, 0, results.getModel());
		BufferedImage originalImage = model.getRenderedImage();
		BufferedImage image =  Factory.copyBufferedImage(originalImage);
		
		// Add the ROI for the current plane to the image.
		//TODO: Need to check that.
		model.setAttributes(MeasurementAttributes.SHOWID, true);
		model.getDrawingView().print(image.getGraphics());
		model.setAttributes(MeasurementAttributes.SHOWID, false);
		String imageName = "ROIImage";
		try {
			writer.addImageToWorkbook(imageName, image); 
		} catch (Exception e) {
			//TODO
		}
		int col = writer.getMaxColumn(0);
		writer.writeImage(0, col+1, 256, 256,	imageName);
		writer.close();
		return true;
	}

	/** Refreshes the result table. */
	public void refreshResults() { populate(); }
	
	/**
	 * Shows the results wizard and updates the fields based on the users 
	 * selection.
	 */
	void showResultsWizard()
	{
		ResultsWizard resultsWizard = new ResultsWizard(view, fields, allFields);
		resultsWizard.pack();
		UIUtilities.setLocationRelativeToAndShow(this, resultsWizard);
		columnNames.clear();
		columnNames = new ArrayList<KeyDescription>();
		columnNames.add(new KeyDescription(AnnotationDescription.ROIID_STRING,
											AnnotationDescription.ROIID_STRING));
		columnNames.add(new KeyDescription(AnnotationDescription.TIME_STRING,
											AnnotationDescription.TIME_STRING));
		columnNames.add(new KeyDescription( AnnotationDescription.ZSECTION_STRING,
											AnnotationDescription.ZSECTION_STRING));
		columnNames.add(new KeyDescription( AnnotationDescription.SHAPE_STRING,
											AnnotationDescription.SHAPE_STRING));
		for (int i = 0 ; i < fields.size(); i++)
			columnNames.add(new KeyDescription(	fields.get(i).getKey().toString(),
												fields.get(i).getName()));
		populate();
		results.repaint();
	}
	
	/**
	 * Reacts to controls selection.
	 * @see ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		int index = Integer.parseInt(e.getActionCommand());
		switch (index) {
			case REFRESH:
				if (!model.isServerROI())
		    		refreshResults();
				break;
			case SAVE:
				Registry reg = MeasurementAgent.getRegistry();
				UserNotifier un = reg.getUserNotifier();
				boolean saved = false;
				try {
					saved = saveResults();
				} catch (Exception ex) {
					reg.getLogger().error(this, 
							"Cannot save the results "+ex.getMessage());
					un.notifyInfo("Save ROI results", 
							"Cannot save the ROI results");
				}
				if (saved)
					un.notifyInfo("Save ROI results", 
							"The ROI results have been " +
													"successfully saved.");
				break;
			case WIZARD:
				showResultsWizard();
		}
	}
	
	/** Basic inner class use to set the cell renderer. */
	class ResultsTable
		extends JTable
	{
		
		/** Creates a new instance. */
		ResultsTable()
		{
			super();
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			int columnWidth = 0;
			FontMetrics metrics = getFontMetrics(getFont());
			int w;
			for(int i = 0 ; i < getColumnCount(); i++)
			{
				w =  metrics.stringWidth(getColumnName(i));
				columnWidth = Math.max(w, COLUMNWIDTH);
				TableColumn col;
				col = getColumnModel().getColumn(i);
				col.setMinWidth(columnWidth);
				col.setPreferredWidth(columnWidth);
			}
		
		}
		
		/**
		 * Overridden to return a customized cell renderer.
		 * @see JTable#getCellRenderer(int, int)
		 */
		public TableCellRenderer getCellRenderer(int row, int column) 
		{
	        return new ResultsCellRenderer();
	    }
	}
	
	/** Inner class used to display the results of measurement. */
	class MeasurementTableModel 
		extends AbstractTableModel
	{
	
		private MeasurementUnits				unitsType;
		
		/** The collection of column's names. */
		private List<KeyDescription>			columnNames;
		
		/** Collection of <code>Object</code>s hosted by this model. */
		private List<MeasurementObject>	values;
		
		/**
		 * Creates a new instance.
		 * 
		 * @param columnNames2	The collection of column's names.
		 * 						Mustn't be <code>null</code>.
		 */
		MeasurementTableModel(List<KeyDescription> colNames, MeasurementUnits units)
		{
			if (colNames == null)
				throw new IllegalArgumentException("No column's names " +
													"specified.");
			this.columnNames = colNames;
			this.values = new ArrayList<MeasurementObject>();
			this.unitsType = units;
		}
		
		/** 
		 * Adds a new row to the model.
		 * 
		 * @param row The value to add.
		 */
		void addRow(MeasurementObject row)
		{
			values.add(row);
			fireTableStructureChanged();
		}
		
		/** 
		 * Get a row from the model.
		 * 
		 * @param index The row to return
		 * 
		 * @return MeasurementObject the row.
		 */
		MeasurementObject getRow(int index)
		{
			if(index < values.size())
				return values.get(index);
			return null;
		}
		
		/**
		 * Returns the value of the specified cell.
		 * @see AbstractTableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int row, int col) 
	    {
			if (row < 0 || row > values.size()) return null;
			MeasurementObject rowData = values.get(row);
	    	return rowData.getElement(col);
		}
	    
		/**
		 * Sets the specified value.
		 * @see AbstractTableModel#setValueAt(Object, int, int)
		 */
	    public void setValueAt(Object value, int row, int col) 
	    {

	    }
	    
		/**
		 * Overridden to return the name of the specified column.
		 * @see AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName(int col) 
		{
			if(AttributeUnits.getUnits(columnNames.get(col).getKey(),unitsType).equals(""))
				return columnNames.get(col).getDescription();
			else
				return columnNames.get(col).getDescription()+
			" (" + AttributeUnits.getUnits(columnNames.get(col).getKey(),unitsType)+")"; }
	    
	    /**
		 * Overridden to return the number of columns.
		 * @see AbstractTableModel#getColumnCount()
		 */
		public int getColumnCount() { return columnNames.size();  }
	
		/**
		 * Overridden to return the number of rows.
		 * @see AbstractTableModel#getRowCount()
		 */
		public int getRowCount() { return values.size(); }
		
		/**
		 * Overridden so that the cell is not editable.
		 * @see AbstractTableModel#isCellEditable(int, int)
		 */
		public boolean isCellEditable(int row, int col) 
		{ 
			return false;
		}
		
	}

}
