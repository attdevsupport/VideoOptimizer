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
package com.att.aro.ui.view.diagnostictab;

import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.commons.lang3.StringUtils;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.Util;
import com.att.aro.core.util.VideoUtils;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.model.diagnostic.TCPUDPFlowsTableModel;
import com.att.aro.ui.model.listener.TCPFlowsTableMenuItemListener;
import com.att.aro.ui.utils.ResourceBundleHelper;
/**
 * Added this class for adding the check boxes for existing TCP flow table.  
 *
 *
 * @param <T>
 */
public class TCPFlowsDataTable<T> extends DataTable<T> {

	private static final long serialVersionUID = 1L;
	
	private JTable table;  
	CheckBoxHeader rendererComponent;
	private MyItemListener it;
	private DiagnosticsTab diagnostics;
	private Map<String, String> sniMap = null;

	public String loadedTrace = "";

	/**
	 * Initializes a new instance of an empty DataTableModel class.
	 * 
	 * @param columns
	 *            An array of java.lang.String objects that are the columns in
	 *            the data table.
	 */
	public TCPFlowsDataTable(DataTableModel<T> dm, DiagnosticsTab diagnostics) {
		super(dm);
		table = this;
		this.diagnostics = diagnostics;
		TableColumn tc = table.getColumnModel().getColumn(1); 
		it = new MyItemListener();
		CheckBoxHeader checkBoxHeader = new CheckBoxHeader(it);
		tc.setHeaderRenderer(checkBoxHeader); 
		setDefaultRenderer(Boolean.class, checkBoxHeader);
		this.rendererComponent = checkBoxHeader;
		table.addMouseListener(new MyMouseListener());
	}

	public List<JMenuItem> getMenuItems() {
		List<JMenuItem> menuItems = new ArrayList<>();
		menuItems.add(getExportMenuItem());
		return menuItems;
	}

	private JMenuItem getExportMenuItem() {
		JMenuItem exportRTTMenuItem = new JMenuItem(ResourceBundleHelper.getMessageString("table.export"));
		exportRTTMenuItem.addActionListener(new TCPFlowsTableMenuItemListener(this));
		return exportRTTMenuItem;
	}

	public void showHighlightedSession(int row) {
		table.getSelectionModel().setSelectionInterval(row, row);
		table.scrollRectToVisible(new Rectangle(0, row * table.getRowHeight(), table.getWidth(), table.getHeight()));
	}

	/**
	 * USing Item listner for the Header check box for select ALL and deselect ALL
	 *
	 *
	 */
	 class MyItemListener implements ItemListener {     
		
		 public void itemStateChanged(ItemEvent e) {
			Object source = e.getSource();
	      if (source instanceof AbstractButton == false) return;     
			boolean checked = e.getStateChange() == ItemEvent.SELECTED;

			DataTableModel<T> dataModel = getDataTableModel();
			TCPUDPFlowsTableModel tcpmodel = (TCPUDPFlowsTableModel) dataModel; // get the specific model for this table
			for (int x = 0; x < table.getRowCount(); x++) {
				tcpmodel.setValueAt(checked, x, 1);
				tcpmodel.fireTableCellUpdated(x, 1);
			}
			diagnostics.openCollapsiblePane();
		}
	}

	/**
	 * Method is for return the select rows. Used this from table common table listner. 
	 * @param column
	 * @return
	 */
	public List<T> getSelectedCheckboxRows(int column) {

		List<T> selectedRows = new ArrayList<T>();
		DataTableModel<T> dataModel = getDataTableModel();
		// Condition avoid index out of bonds when we reopen the trace
		if (dataModel.getRowCount() == table.getRowCount()) {
			for (int i = 0; i < dataModel.getRowCount(); i++) {

				if ((Boolean) dataModel.getValueAt(i, table.convertColumnIndexToView(1))) {
					selectedRows.add(dataModel.getValueAt(convertRowIndexToModel(i)));
				}
			}
		}

		return selectedRows;
	}

	class MyMouseListener extends MouseAdapter {

		public void mouseClicked(MouseEvent mouseEvent) {

			if (mouseEvent.getClickCount() == 2 && mouseEvent.getButton() == MouseEvent.BUTTON1) {
				Session selectedSession = getModel().getSessionMap().get(getSessionKey(table.getSelectedRow()));
				AbstractTraceResult traceresult = diagnostics.getAroTraceData().getAnalyzerResult().getTraceresult();
				String trafficFile = getTrafficFile(traceresult);
				if (!trafficFile.equalsIgnoreCase(loadedTrace)) {
					sniMap = null;
				}
				ServerNameIndicationDialog serverNameIndicationDialog = new ServerNameIndicationDialog(selectedSession,
						sniMap, trafficFile);
				sniMap = serverNameIndicationDialog.getSniMap();
				loadedTrace = trafficFile;
			} else {
				int checkedCount = 0;
				rendererComponent.removeItemListener(it);
				if (rendererComponent instanceof JCheckBox) {
					boolean[] flags = new boolean[table.getRowCount()];
					for (int i = 0; i < table.getRowCount(); i++) {
						flags[i] = ((Boolean) table.getValueAt(i, table.convertColumnIndexToView(1))).booleanValue();
						if (flags[i]) {
							checkedCount++;
						}
					}
					if (checkedCount == table.getRowCount()) {
						((JCheckBox) rendererComponent).setSelected(true);
					}
					if (checkedCount != table.getRowCount()) {
						((JCheckBox) rendererComponent).setSelected(false);
					}
					diagnostics.openCollapsiblePane();
				}
				rendererComponent.addItemListener(it);
				table.getTableHeader().repaint();
			}
		}

		private String getTrafficFile(AbstractTraceResult traceresult) {
			Map<String, String[]> traceFileMap;

			String traceDirectory = traceresult.getTraceDirectory();
			if ((traceFileMap = VideoUtils.validateFolder(new File(traceDirectory))).size() > 0) {
				String[] trafficFile = traceFileMap.get(VideoUtils.TRAFFIC);
				return traceresult.getTraceDirectory() + Util.FILE_SEPARATOR + trafficFile[0];
			} else if (StringUtils.isNotEmpty(traceresult.getTraceFile())) {
				String trafficFile = traceresult.getTraceFile();
				return trafficFile;
			}
			return "";

		}
	}

	public String getSessionKey(int selectedRow) {
        return table.getValueAt(selectedRow, 4) + "$" + table.getValueAt(selectedRow, 5) + "$" + table.getValueAt(selectedRow, 3);
	}

	public void setHeaderDefaultValue() {
		((JCheckBox) rendererComponent).setSelected(true);
		table.getTableHeader().repaint();
	}

	public TCPUDPFlowsTableModel getModel() {
		return ((TCPUDPFlowsTableModel) dataModel);
	}

	public TCPFlowsDataTable<T> getData() {
		return this;
	}

}
