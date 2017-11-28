/*
 *  Copyright 2017 AT&T
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

package com.att.aro.ui.model;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.ui.commonui.ContentViewer;

public class ImageBPTable<T> extends JTable {
	private static final long serialVersionUID = 1L;

	// Listener to handle the events on table.
	private MouseListener mouseListener = new MouseAdapter() {
		private boolean lastClicked = false;

		@Override
		public void mousePressed(final MouseEvent mEvent) {
			if (mEvent.getClickCount() < 2) {
				lastClicked = true;
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (lastClicked) {
							showContent(mEvent);
						}
						lastClicked = false;
					}

				});
				t.start();
			} else {
				lastClicked = false;
			}

			// showPopup(mEvent);
		}

		protected void showContent(MouseEvent mEvent) {

			Point point = mEvent.getPoint();
			int row = rowAtPoint(point);
			int col = columnAtPoint(point);
			if(col == -1) {
				return;
			}
			try {
				ContentViewer.getInstance().viewImageMetadataContent(getEXIF(getValueAt(row, 1).toString()));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	};

	/**
	 * Initializes a new instance of the DataTable class. This default
	 * constructor should be used when the table model is not immediately
	 * available.
	 */
	public ImageBPTable() {
		this(null, null);
	}

	/**
	 * Initializes a new instance of the DataTable class using the specified
	 * table model.
	 * 
	 * @param dtm
	 *            The table model.
	 */
	public ImageBPTable(DataTableModel<T> dtm) {
		this(dtm, dtm.createDefaultTableColumnModel());
		super.autoCreateColumnsFromModel = true;
	}

	/**
	 * Initializes a new instance of the DataTable class using the specified
	 * table model and table column model.
	 * 
	 * @param dtm
	 *            The table model.
	 * @param tcm
	 *            The table column model.
	 */
	public ImageBPTable(DataTableModel<T> dtm, TableColumnModel tcm) {
		super(dtm, tcm);
		setDefaultRenderer(Object.class, new DataTableCellRenderer());
		setDefaultRenderer(Double.class, new DataTableCellRenderer());
		setDefaultRenderer(Number.class, new DataTableCellRenderer());
		setDefaultRenderer(Float.class, new DataTableCellRenderer());
		this.addMouseListener(mouseListener);
	}

	/**
	 * Sets a data table model.
	 * 
	 * @param dataModel
	 *            The new DataTableModel.
	 */
	public void setModel(DataTableModel<T> dataModel) {
		super.setModel(dataModel);
		if (super.autoCreateColumnsFromModel) {
			setColumnModel(dataModel.createDefaultTableColumnModel());
		}
	}

	private StringBuffer getEXIF(String sValue) {

		int pos = sValue.lastIndexOf("/") + 1;
		String imageName = sValue.substring(pos);
		String tracePath = PreferenceHandlerImpl.getInstance().getPref("TRACE_PATH")
				+ System.getProperty("file.separator");

		return extractMetadata(tracePath + "Image" + System.getProperty("file.separator") + imageName);
	}

	private StringBuffer extractMetadata(String fullpath) {
		ImageMetadata metadata;
		String imgMetadata = "";
		StringBuffer completeMetadata = new StringBuffer();
		// List<? extends ImageMetadataItem> imgMdata;
		try {
			metadata = Imaging.getMetadata(new File(fullpath));

			if (metadata != null) {
				if (!metadata.getClass().equals(GenericImageMetadata.class)) {
					JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
					TiffImageMetadata exif = jpegMetadata.getExif();
					if (exif != null) {

						for (int i = 0; i < exif.getItems().size(); i++) {
							imgMetadata = exif.getItems().get(i).toString();
							completeMetadata.append(imgMetadata);
							completeMetadata.append("\n");
						}

					}
				} else {
					GenericImageMetadata genMetadata = (GenericImageMetadata) metadata;
					if (genMetadata.getItems() != null && genMetadata.getItems().size() > 5) {

						for (int i = 0; i < genMetadata.getItems().size(); i++) {
							imgMetadata = genMetadata.getItems().get(i).toString();

							completeMetadata.append(imgMetadata);
							completeMetadata.append("\n");
						}

					}
				}
			}

		} catch (IOException | ImageReadException e) {
			e.printStackTrace();
		}

		return completeMetadata;

	}

	/**
	 * Returns a default table header for the DataTable.
	 * 
	 * @return A JTableHeader object with default properties.
	 */
	@Override
	public JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent mEvent) {
				int column = columnAtPoint(mEvent.getPoint());

				// Locate the renderer under the event location
				if (column != -1) {
					TableColumn aColumn = columnModel.getColumn(column);
					Object tip = aColumn.getHeaderValue();
					if (tip != null) {
						return tip.toString();
					}
				}
				return null;
			}
		};
	}

	/**
	 * Returns the list of items currently selected in the table. This
	 * convenience method is for tables that use a multiple selection model.
	 * 
	 * @return A java.util.List object containing the list of selected items.
	 */
	public List<T> getSelectedItems() {
		int[] selectedRows = getSelectedRows();

		List<T> result = new ArrayList<T>(selectedRows.length);
		DataTableModel<T> dataModel = getDataTableModel();
		for (int row : selectedRows) {
			result.add(dataModel.getValueAt(convertRowIndexToModel(row)));
		}
		return result;
	}

	/**
	 * Returns the first selected item in the table. This convenience method is
	 * for tables that use a single selection model.
	 * 
	 * @return The item.
	 */
	public T getSelectedItem() {
		Integer row = getSelectedRow();
		if (row != -1) {
			return getItemAtRow(row);
		} else {
			return null;
		}
	}

	/**
	 * Returns the data item at the specified row index. The row index is the
	 * table view index which may not be the same index as in the data table
	 * model.
	 * 
	 * @param row
	 *            An int value that is the row index in the table view.
	 * @return The data item.
	 */
	public T getItemAtRow(int row) {
		try {
			return row >= 0 ? getDataTableModel().getValueAt(convertRowIndexToModel(row)) : null;
		} catch (IndexOutOfBoundsException ie) {
			return null;
		}
	}

	@Override
	/**
	 * Sets the column model for the DataTable to the specified
	 * TableColumnModel.
	 * 
	 * @param columnModel
	 *            - The new TableColumnModel.
	 */
	public void setColumnModel(TableColumnModel columnModel) {
		super.setColumnModel(columnModel);
	}

	/**
	 * Returns the DataTableModel encapsulated by this class.
	 * 
	 * @return The DataTableModel object.
	 * @see javax.swing.JTable#getModel()
	 */
	@SuppressWarnings("unchecked")
	public DataTableModel<T> getDataTableModel() {
		return (DataTableModel<T>) super.getModel();
	}

	@Override
	/**
	 * Sets the data model for the table.
	 * 
	 * @param dataModel
	 *            A TableModel object that is the new data model.
	 */
	public void setModel(TableModel dataModel) {
		super.setModel(dataModel);
	}

	/**
	 * Marks the specified item in the table as selected, if it exists.If the
	 * item exists in the table and is already marked as selected, the the
	 * selection is cleared.
	 * 
	 * @param item
	 *            The item in the table to mark as selected.
	 * @return A boolean value that is true if the specified item was found and
	 *         marked as selected.
	 */
	public boolean selectItem(T item) {
		int index;
		if (item != null && (index = getDataTableModel().indexOf(item)) >= 0) {
			index = convertRowIndexToView(index);
			ListSelectionModel selectionModel = getSelectionModel();
			if (selectionModel != null) {
				selectionModel.setSelectionInterval(index, index);
				scrollRectToVisible(getCellRect(index, 0, true));
				return true;
			}
		} else {
			clearSelection();
		}
		return false;
	}

}
