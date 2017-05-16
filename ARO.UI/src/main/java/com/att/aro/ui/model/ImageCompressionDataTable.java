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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.ui.commonui.ContentViewer;

public class ImageCompressionDataTable<T> extends JTable {
	private static final long serialVersionUID = 1L;

	// Listener to handle the events on table.
	private MouseListener mouseListener = new MouseAdapter() {
		private boolean lastClicked = false;

		@Override
		public void mousePressed(final MouseEvent mEvent) {
			// showPopup(mEvent);
			
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
			
		}

		/*
		 * @Override public void mouseReleased(MouseEvent mEvent) { //
		 * hidePopup(mEvent); }
		 * 
		 * private void hidePopup(MouseEvent mEvent) { if (popup != null) {
		 * popup.removeAll(); popup.setVisible(false); } }
		 */

		/*
		 * private void showPopup(MouseEvent mEvent) { if (popup != null) {
		 * Point point = mEvent.getPoint(); int row; int column; row =
		 * rowAtPoint(point); column = columnAtPoint(point); getValueAt(row,
		 * column).toString();
		 * 
		 * Rectangle aRect = new Rectangle(); aRect.height = 250; aRect.width =
		 * 300; aRect.x = mEvent.getXOnScreen(); aRect.y =
		 * mEvent.getYOnScreen(); popup.scrollRectToVisible(aRect);
		 * 
		 * popup.show(null, mEvent.getXOnScreen(), mEvent.getYOnScreen());
		 * 
		 * } }
		 */
	};

	/**
	 * Initializes a new instance of the DataTable class. This default
	 * constructor should be used when the table model is not immediately
	 * available.
	 */
	public ImageCompressionDataTable() {
		this(null, null);
	}

	protected void showContent(MouseEvent mEvent) {

		Point point = mEvent.getPoint();
		int row;
		int column;
		row = rowAtPoint(point);
		column = columnAtPoint(point);

		String iVal = getValueAt(row, column).toString();
		String imgName = "";
		if (!isImage(iVal)) {
			iVal = getValueAt(row, 1).toString();
		} else {
			iVal = getValueAt(row, column).toString();
		}

		try {
			String tracePath = PreferenceHandlerImpl.getInstance().getPref("TRACE_PATH")
					+ System.getProperty("file.separator");
			// String imagePath = tracePath + "image" +
			// System.getProperty("file.separator");
			String quality = "";
			String title = "Original Image";
			int pos = iVal.lastIndexOf("/") + 1;
			imgName = iVal.substring(pos);
			StringBuffer imageFile = new StringBuffer();
			imageFile.append(tracePath + "Image" + System.getProperty("file.separator"));
			if (column != 1 && column != 2) {

				if (column == 4) {
					quality = "70.0";
					title = "70% Compressed Image";
				} else if (column == 3) {
					quality = "85.0";
					title = "85% Compressed Image";
				}

				// imageName = iVal.substring(iVal.lastIndexOf("/")+1);
				String imageName = imgName.substring(0, imgName.lastIndexOf(".")) + "_compressed_" + quality + "."
						+ imgName.substring(imgName.lastIndexOf(".") + 1, imgName.length());
				imgName = imageName;
				imageFile.append("Compressed" +System.getProperty("file.separator"));

			}
			imageFile.append(imgName);
			ContentViewer.getInstance().viewImage(imageFile.toString(), imgName, title);

		} catch (Exception imageException) {
			// log.error(imageException.toString());
		}

	}

	private boolean isImage(String iVal) {
		boolean isImage = false;
		if (iVal.contains(".jpg") || iVal.contains(".jpeg")) {
			isImage = true;
		}
		return isImage;
	}

	/**
	 * Initializes a new instance of the DataTable class using the specified
	 * table model.
	 * 
	 * @param dtm
	 *            The table model.
	 */
	public ImageCompressionDataTable(DataTableModel<T> dtm) {
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
	public ImageCompressionDataTable(DataTableModel<T> dtm, TableColumnModel tcm) {
		super(dtm, tcm);
		setDefaultRenderer(Object.class, new DataTableCellRenderer());
		setDefaultRenderer(Double.class, new DataTableCellRenderer());
		setDefaultRenderer(Number.class, new DataTableCellRenderer());
		setDefaultRenderer(Float.class, new DataTableCellRenderer());
		this.addMouseListener(mouseListener);
		// this.popup = new DataTablePopupMenu(this);
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
