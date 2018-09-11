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

package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.att.aro.core.bestpractice.pojo.ImageCompressionEntry;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.IAROExpandable;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.model.ImageCompressionDataTable;


public abstract class AbstractBpImageCompressionTablePanel extends TabPanelJPanel implements IAROExpandable, MouseListener {
	// private static final Logger logger =
	// Logger.getLogger(AbstractBpDetailTablePanel.class.getName());
	final static int ROW_HEIGHT = 20;
	final static int MINIMUM_ROWS = 5;

	@Override
	public void refresh(AROTraceData analyzerResult) {
		
	}

	private static final long serialVersionUID = 1L;

	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;

	@SuppressWarnings("rawtypes")
	DataTableModel tableModel;
	ImageCompressionDataTable<ImageCompressionEntry> contentTable;
	int noOfRecords;
	private JPanel contentPanel;

	private JScrollPane scrollPane;
	
	ImageTablePanelUtil imageTablePanelUtil;

	public AbstractBpImageCompressionTablePanel() {
		initTableModel();
		setLayout(new BorderLayout());
		add(layoutDataPanel(), BorderLayout.CENTER);
	}
	
	/**
	 * Instantiate table model
	 */
	abstract void initTableModel();

	public void addTablePanelRoute(IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;
		getContentTable().addMouseListener(this);
	}

	@Override
	public JPanel layoutDataPanel() {
		JPanel layout = new JPanel();
		layout.setLayout(new BorderLayout());
		layout.add(getContentPanel(), BorderLayout.CENTER);
		JPanel contentPanelWidth = new JPanel(new GridLayout(2, 1, 5, 5));
		JPanel contentPanelWidthAdjust = new JPanel(new GridBagLayout());
		contentPanelWidthAdjust.add(contentPanelWidth, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
		contentPanelWidthAdjust.setBackground(Color.BLUE);
		layout.add(contentPanelWidthAdjust, BorderLayout.EAST);
		return layout;
	}

	/**
	 * Initializes and returns the DuplicateContentPanel.
	 */
	private JPanel getContentPanel() {
		if (this.contentPanel == null) {
			this.contentPanel = new JPanel(new BorderLayout());
			this.contentPanel.add(getScrollPane(), BorderLayout.CENTER);
			imageTablePanelUtil = new ImageTablePanelUtil(scrollPane, tableModel, ROW_HEIGHT, MINIMUM_ROWS);
			this.contentPanel.add(imageTablePanelUtil.getButtonsPanel(), BorderLayout.EAST);
		}
		return this.contentPanel;
		}

	/**
	 * clicks the "+" button if table needs to expand
	 */
	@Override
	public void expand() {
		imageTablePanelUtil.expand();
	}




	/**
	 * Returns the Scroll Pane for the FileCompressionTable.
	 */
	private JScrollPane getScrollPane() {
		if(scrollPane==null){
			scrollPane = new JScrollPane();
			scrollPane.getViewport().add(getContentTable());
		}
		return scrollPane;
	}

	void setScrollSize(int scrollHeight) {
		imageTablePanelUtil.setScrollSize(scrollHeight);
	}

	void autoSetZoomBtn() {
		imageTablePanelUtil.autoSetZoomBtn();
	}

	@SuppressWarnings("rawtypes")
	public abstract ImageCompressionDataTable getContentTable();

	@Override
	public void mousePressed(MouseEvent event) {
		diagnosticsOverviewRoute = imageTablePanelUtil.mousePressed(event, diagnosticsOverviewRoute);
	}
	@Override
	public void mouseClicked(MouseEvent event) {
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

}
