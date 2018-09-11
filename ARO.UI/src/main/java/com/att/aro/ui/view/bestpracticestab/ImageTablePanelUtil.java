/*
 *  Copyright 2014 AT&T
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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;

import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public final class ImageTablePanelUtil {

	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	final String zoomOut = ResourceBundleHelper.getMessageString("button.ZoomOut"); // "+"
	final String zoomIn = ResourceBundleHelper.getMessageString("button.ZoomIn"); // "-"

	private JScrollPane scrollPane;
	private JButton zoomBtn;
	@SuppressWarnings("rawtypes")
	DataTableModel tableModel;
	private int ROW_HEIGHT = 0;
	private int MINIMUM_ROWS = 0;

	@SuppressWarnings("rawtypes")
	ImageTablePanelUtil(JScrollPane scrollPane, DataTableModel tableModel, int ROW_HEIGHT, int MINIMUM_ROWS) {
		this.scrollPane = scrollPane;
		this.tableModel = tableModel;
		this.ROW_HEIGHT = ROW_HEIGHT;
		this.MINIMUM_ROWS = MINIMUM_ROWS;
	}

	public JPanel getButtonsPanel() {
		JPanel bpButtonPanel = new JPanel(new GridBagLayout());
		JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
		panel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		bpButtonPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));

		getZoomBtn();
		panel.add(zoomBtn);
		bpButtonPanel.add(panel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
				GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		return bpButtonPanel;
	}
	
	public void setScrollSize(int scrollHeight) {
		
		Dimension currentDimensions = scrollPane.getSize();
		double panelWidth = screenSize.getWidth();
		if (panelWidth > 500) {
			panelWidth = panelWidth - 400;
		}
		currentDimensions.setSize(panelWidth, ROW_HEIGHT * (scrollHeight + 1));
		scrollPane.setPreferredSize(currentDimensions);
	}

	/**
	 * Returns the zoom button.
	 * 
	 * @param zoomBtn
	 * @param scrollPane
	 */
	private void getZoomBtn() {
		zoomBtn = new JButton(zoomOut);
		zoomBtn.setEnabled(false);
		zoomBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String val = zoomBtn.getText();
				if (val.equals(zoomOut)) {
					zoomBtn.setText(zoomIn);
					setScrollSize(tableModel.getRowCount());
				} else {
					zoomBtn.setText(zoomOut);
					setScrollSize(MINIMUM_ROWS);
				}
			}
		});
	}

	
	public void expand() {
		if (zoomBtn.getText().equals(zoomOut) && tableModel.getRowCount() >= MINIMUM_ROWS) {
			zoomBtn.doClick();
			this.scrollPane.revalidate();
		}
	
	}

	@SuppressWarnings("unchecked")
	public IARODiagnosticsOverviewRoute mousePressed(MouseEvent event,
			IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		if (diagnosticsOverviewRoute != null && event.getClickCount() == 2) {
			if (event.getSource() instanceof JTable) {
				int selectionIndex = ((JTable) event.getSource()).getSelectedRow();
				if (selectionIndex > -1) {
					int original = ((JTable) event.getSource()).convertRowIndexToModel(selectionIndex);
					diagnosticsOverviewRoute.route(tableModel, tableModel.getValueAt(original));
				}
			}
		}
		return diagnosticsOverviewRoute;
	}

	public void autoSetZoomBtn() {
		if (tableModel.getRowCount() > MINIMUM_ROWS) {
			zoomBtn.setEnabled(true);
			String val = zoomBtn.getText();
			if (val.equals(zoomIn)) {
				zoomBtn.setText(zoomOut);
				setScrollSize(MINIMUM_ROWS);
			}
		}
	}
}