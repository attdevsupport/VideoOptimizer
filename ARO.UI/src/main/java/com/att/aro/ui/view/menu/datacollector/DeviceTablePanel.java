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
package com.att.aro.ui.view.menu.datacollector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.DataCollectorSelectNStartDialog;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;

public class DeviceTablePanel extends JPanel implements MouseListener{
	private static final Logger logger = LogManager.getLogger(DeviceTablePanel.class.getName()); 

	private static final long serialVersionUID = 1L;

	private JPanel contentPanel;

	private JScrollPane scrollPane;

	private DeviceTableModel tableModel;

	/*
	 * used to pad out the height of a JPanel that holds a ScrollPane when in Windows
	 */
	final static int ROW_HEIGHT = 20;
	
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	private DataTable<IAroDevice> contentTable;

	private IAroDevice selectedIAroDevice = null;

	private DeviceDialogOptions optionPanel;

	private DataCollectorSelectNStartDialog dialog;

	protected int rowPadding;
	
	public DeviceTablePanel() {
		rowPadding = (Util.isWindowsOS()) ? 7 : 0;
		initTableModel();
		setLayout(new BorderLayout());

		add(layoutDataPanel(), BorderLayout.CENTER);
		setBackground(Color.pink);
	}
	
	void initTableModel() {
		tableModel = new DeviceTableModel();
	}
	
	public JPanel layoutDataPanel() {
		
		JPanel layout = new JPanel(new FlowLayout());
		layout.add(getContentPanel());
		
		return layout;
	}
	
	/**
	 * Initializes and returns the DuplicateContentPanel.
	 */
	private JPanel getContentPanel() {
		if (this.contentPanel == null) {
			this.contentPanel = new JPanel(new BorderLayout()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void setSize(Dimension d) {
					d.height += rowPadding;
					super.setSize(d);
				}

				@Override
				public void setSize(int width, int height) {
					height += rowPadding;
					super.setSize(width, height);
				}
			};
			this.contentPanel.add(getScrollPane(), BorderLayout.CENTER);
		}
		return this.contentPanel;
	}

	/**
	 * Returns the Scroll Pane for the FileCompressionTable.
	 */
	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane(getContentTable());
		}
		return scrollPane;
	}

	/**
	 * Initializes and returns the RequestResponseTable.
	 */
	public DataTable<IAroDevice> getContentTable() {
		if (contentTable == null) {
			contentTable = new DataTable<IAroDevice>(tableModel);
			contentTable.setAutoCreateRowSorter(true);
			contentTable.setGridColor(Color.LIGHT_GRAY);
			contentTable.setRowHeight(ROW_HEIGHT);
			contentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			contentTable.addMouseListener(this);

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) contentTable.getPopup();
            popupMenu.initialize();
		}

		return contentTable;
	}

	/**
	 * Sets the data for the Duplicate Content table.
	 * 
	 * @param data
	 *            - The data to be displayed in the Duplicate Content table.
	 */
	public void setData(Collection<IAroDevice> data) {
		if (data != null) {
			setVisible(!data.isEmpty());
		} else {
			setVisible(true);
		}

		tableModel.setData(data);
		setScrollSize(tableModel.getRowCount());
	}

	/**
	 * Set preferred height of scrollPane to match requested number of rows of JTable
	 * 
	 * @param scrollHeight 
	 * 
	 */
	void setScrollSize(int scrollHeight) {

		Dimension currentDimensions = scrollPane.getSize();
		TableColumnModel columnModel = contentTable.getTableHeader().getColumnModel();
		
		double panelWidth = 0;
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			panelWidth += columnModel.getColumn(i).getPreferredWidth();
		}
		
		double newHeight = ROW_HEIGHT * (scrollHeight + 1);
		currentDimensions.setSize(panelWidth, newHeight);
		scrollPane.setPreferredSize(currentDimensions);
	}

	public IAroDevice getSelection() {
		return selectedIAroDevice;
	}

	public void autoSelect() {
			setSelectedDevice(tableModel.getValueAt(0));
	}

	/**
	 * Records the IAroDevice as selected and pre configures options
	 * 
	 * @param aroDevice
	 */
	public void setSelectedDevice(IAroDevice aroDevice) {

			contentTable.selectItem(aroDevice);
			selectedIAroDevice = aroDevice;
			boolean validated = false;
			if (optionPanel != null) {
				validated = optionPanel.setDevice(selectedIAroDevice);

				dialog.enableStart(validated);
			} else if (dialog != null) {
				dialog.enableStart(true);
			}
			logger.info("device :" + selectedIAroDevice);
	}


	@Override
	public void mouseClicked(MouseEvent event) {
	}

	@Override
	public void mousePressed(MouseEvent event) {
		if (event.getSource() instanceof JTable){
			int selectionIndex = ((JTable)event.getSource()).getSelectedRow();
			int original = ((JTable)event.getSource()).convertRowIndexToModel(selectionIndex);

			setSelectedDevice(tableModel.getValueAt(original));
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {
	}

	@Override
	public void mouseEntered(MouseEvent event) {
	}

	@Override
	public void mouseExited(MouseEvent event) {
	}

	public void setSubscriber(DeviceDialogOptions optionPanel) {
		this.optionPanel = optionPanel;
	}

	public void subscribe(DataCollectorSelectNStartDialog dialog) {
		this.dialog = dialog;
	}





}
