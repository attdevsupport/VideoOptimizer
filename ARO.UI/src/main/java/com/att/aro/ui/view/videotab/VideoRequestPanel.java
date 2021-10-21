/*
 *  Copyright 2018 AT&T
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
package com.att.aro.ui.view.videotab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.TabPanelJScrollPane;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.video.VideoRequestTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.menu.tools.RegexWizard;

public class VideoRequestPanel extends TabPanelJScrollPane {

	private static final long serialVersionUID = 1L;

	private JPanel requestListPanel;

	private List<HttpRequestResponseInfo> requestURL = new ArrayList<>();	
	private JPanel requestPanel;
	private MainFrame aroView;
	private DataTable<HttpRequestResponseInfo> requestListTable; 
	private VideoRequestTableModel videoRequestTableModel = new VideoRequestTableModel();

	public VideoRequestPanel(MainFrame aroView) {

		requestPanel = new JPanel();
		this.aroView = aroView;
		requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.PAGE_AXIS));

		requestPanel.setBackground(new Color(238, 238, 238));
		requestPanel.add(getRequestListPanel());
		requestListTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		setViewportView(requestPanel);
		resize();
	}

	private JPanel getRequestListPanel() {

		if (requestListPanel == null) {
			requestListPanel = new JPanel();
			requestListPanel.setLayout(new BorderLayout());
			getDummyData();
			requestListTable = new DataTable<HttpRequestResponseInfo>();
			requestListPanel.setName(ResourceBundleHelper.getMessageString("video.tab.requests.tableName"));

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) requestListTable.getPopup();
			popupMenu.initialize();

			videoRequestTableModel.setData(requestURL);
			requestListTable.setModel(videoRequestTableModel);
			
			JTableHeader header = requestListTable.getTableHeader();
			requestListTable.setGridColor(Color.LIGHT_GRAY);
			int width = requestListTable.getParent() != null ? requestListTable.getParent().getWidth() : 1000;
			width = requestPanel.getWidth();
			requestListTable.getColumnModel().getColumn(0).setPreferredWidth(width);
			requestListTable.getColumnModel().getColumn(0).setCellRenderer(new WordWrapRenderer());

			requestListPanel.add(header, BorderLayout.NORTH);
			requestListPanel.add(requestListTable, BorderLayout.CENTER);

			requestListTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent event) {
					if (event.getClickCount() == 2) {
						int row = requestListTable.getSelectedRow();
						HttpRequestResponseInfo request = requestURL.get(row);
						requestListTable.getColumnModel().getColumn(0).setCellRenderer(new WordWrapRenderer(row));
						if (ResourceBundleHelper.getMessageString("videotab.videorequest.table.url")
								.equals(requestListTable.getColumnName(requestListTable.getSelectedColumn()))) {
							RegexWizard regexWizard = RegexWizard.getInstance(aroView.getJFrame());
							regexWizard.setRequest(request);
							regexWizard.setVisible(true);
						}
					}
				}
			});

		}
		return requestListPanel;
	}

	private void getDummyData() {
		requestURL = new ArrayList<>();
		// fill out a big empty
		for (int idx = 0; idx < 30; idx++) {
			requestURL.add(null);
		}
	}

	public void resize() {
		int width = requestPanel.getWidth() - 10;
		if (width > 55) {
			requestListTable.getColumnModel().getColumn(0).setPreferredWidth(55);
			requestListTable.getColumnModel().getColumn(1).setPreferredWidth(width - 55);
			requestListTable.getColumnModel().getColumn(0).setWidth(55);
		}
	}
	
	@Override
	public JPanel layoutDataPanel() {
		return null;
	}

	@Override
	public void refresh(AROTraceData analyzerResult) {
		requestURL = new ArrayList<>();
		if (analyzerResult != null && analyzerResult.getAnalyzerResult() != null && analyzerResult.getAnalyzerResult().getStreamingVideoData() != null) {
			for (HttpRequestResponseInfo req : analyzerResult.getAnalyzerResult().getStreamingVideoData().getRequestMap().values()) {
				requestURL.add(req);
			}
		}
		videoRequestTableModel.setData(requestURL);
		requestPanel.remove(requestListPanel);
		requestListPanel = getRequestListPanel();
		requestPanel.add(getRequestListPanel(),
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));

		requestPanel.updateUI();
		resize();
	}

	@Override
	public void setScrollLocationMap() {	
	}


}
