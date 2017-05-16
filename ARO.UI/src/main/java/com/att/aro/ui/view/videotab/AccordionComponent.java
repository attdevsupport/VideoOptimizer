
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

package com.att.aro.ui.view.videotab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.IVideoTabHelper;
import com.att.aro.core.videoanalysis.impl.VideoTabHelperImpl;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.menu.tools.RegexWizard;

public class AccordionComponent extends JPanel implements ActionListener {

	private JPanel hiddenPanel;
	private AROManifest aroManifest;
	private JTable jTable;
	BasicArrowButton bb;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private JPanel requestListPanel;
	private VideoTabHelperImpl videoTabHelper = (VideoTabHelperImpl) ContextAware.getAROConfigContext().getBean("videoTabHelperImpl", IVideoTabHelper.class);
	private IAROView aroview;
	private JLabel lbl;
	
	public AccordionComponent(AROManifest aroManifest, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		this(true,null);
		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;
		this.aroManifest = aroManifest;
		updateHiddenPanelContent(true);
	}
	
	
	public AccordionComponent(boolean manifestFlag,IAROView aroview){
		this.aroview = aroview;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		bb = new BasicArrowButton(SwingConstants.EAST);
		bb.setMaximumSize(new Dimension(10, 10));
		
		JPanel titlePanel = new JPanel(new GridBagLayout());

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.weightx = 0.1;
		titlePanel.add(bb, constraint);

		lbl = new JLabel();
		lbl.setFont(new Font("accordionLabel", Font.ITALIC, 12));// AROUIManager.LABEL_FONT);
		constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.weightx = 3;
		titlePanel.add(lbl, constraint);

		hiddenPanel = new JPanel(new GridBagLayout());
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.weightx = 1;
		constraint.weighty = 1;
		constraint.insets = new Insets(10, 10, 5, 10);

		add(titlePanel);
		add(hiddenPanel);
		hiddenPanel.setVisible(false);

		bb.addActionListener(this);	
		updateHiddenPanelContent(false);
	}

	private void updateHiddenPanelContent(boolean manifestFlag){
		String text="";
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.weightx = 1;
		constraint.weighty = 1;
		constraint.insets = new Insets(10, 10, 5, 10);

		if (manifestFlag) {
			text = MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.manifest.name"),
					aroManifest.getVideoName());
			lbl.setText(text);
			hiddenPanel.add(addTable(), constraint);
		} else {
			if (this.aroview != null) {
				text=ResourceBundleHelper.getMessageString("video.tab.requests");
				lbl.setText(text);
				hiddenPanel.add(getRequestListPanel(aroview), constraint);
			}
		}	
	}

	private JPanel getRequestListPanel(IAROView aroview){
		if(requestListPanel == null){
			requestListPanel = new JPanel();
			requestListPanel.setLayout(new BorderLayout());

			//get requestMap
			List<HttpRequestResponseInfo> requestURL = new ArrayList<>();
			for (double keyTimeStamp : videoTabHelper.getRequestListMap().keySet()) {
				if (!videoTabHelper.getRequestListMap().get(keyTimeStamp).getObjName().contains(".m3u8")
				 && !videoTabHelper.getRequestListMap().get(keyTimeStamp).getObjName().contains(".mpd")) {
					requestURL.add(videoTabHelper.getRequestListMap().get(keyTimeStamp));
				}
			}
			
			TableModel model = new AbstractTableModel() {
				String[] columnNames = {"Request URL"};
				
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					Object value="";
					if(columnIndex == 0){
						value=requestURL.get(rowIndex).getObjUri().toString();
					}
					return value;
				}
				
				@Override
				public int getRowCount() {
					return requestURL.size();
				}
				
				@Override
				public int getColumnCount() {
					return columnNames.length;
				}
				
				@Override
				public String getColumnName(int columnIndex) {
					return columnNames[columnIndex];
				}
			};
			
			JTable requestListTable = new JTable(model);
			JTableHeader header = requestListTable.getTableHeader();
			requestListTable.setGridColor(Color.LIGHT_GRAY);
			requestListTable.getColumnModel().getColumn(0).setPreferredWidth(950);
			requestListTable.getColumnModel().getColumn(0).setCellRenderer(new WordWrapRenderer());
			
			requestListPanel.add(header, BorderLayout.NORTH);
			requestListPanel.add(requestListTable, BorderLayout.CENTER);
		
			requestListTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
		             int row = requestListTable.getSelectedRow();
		             HttpRequestResponseInfo request = requestURL.get(row);
		             if(e.getClickCount() == 2){   
		            	    requestListTable.getColumnModel().getColumn(0).setCellRenderer(new WordWrapRenderer(row));
		            		RegexWizard	regexWizard = new RegexWizard(aroview,request);
		            		regexWizard.setVisible(true);
		            		regexWizard.setAlwaysOnTop(true);  		
		             }
				}
			});
		}
		return requestListPanel;
	}

	private JPanel addTable() {
		Collection<VideoEvent> videoEventList = aroManifest.getVideoEventList().values();

		TableModel tableModel = new AccordionTableModel(videoEventList);
		jTable = new JTable(tableModel);
		jTable.setGridColor(Color.LIGHT_GRAY);

		JTableHeader header = jTable.getTableHeader();
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(header, BorderLayout.NORTH);
		panel.add(jTable, BorderLayout.CENTER);

		// Sorter for jTable
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(jTable.getModel());
		jTable.setRowSorter(rowSorter);

		for (int column = 0; column < 7; column++) {
			rowSorter.setComparator(column, new TableSortComparator(column));
		}

		jTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (diagnosticsOverviewRoute != null && e.getClickCount() == 2) {
					if (e.getSource() instanceof JTable) {
						int selectionIndex = ((JTable) e.getSource()).getSelectedRow();
						double segmentNo = jTable.getModel().getValueAt(selectionIndex, 0) != null
								? (Double.parseDouble(jTable.getModel().getValueAt(selectionIndex, 0).toString())) : -1;
						AccordionTableModel tableModel = (AccordionTableModel) jTable.getModel();
						BigDecimal segmentNumber = BigDecimal.valueOf(segmentNo);
						if (selectionIndex > -1) {
							for (VideoEvent videoEvent : tableModel.getVideoEventCollection()) {
								if (BigDecimal.valueOf(videoEvent.getSegment()).equals(segmentNumber)) {
									diagnosticsOverviewRoute.updateDiagnosticsTab(videoEvent.getSession());
									break;
								}
							}
						}
					}
				}

			}
		});
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		AccordionComponent accordion = (AccordionComponent) SwingUtilities.getAncestorOfClass(AccordionComponent.class,
				(BasicArrowButton) e.getSource());
		JPanel nestedPanel = accordion.getHiddenPanel();
		if (nestedPanel != null) {
			bb = (BasicArrowButton) e.getSource();
			if (bb.getDirection() == SwingConstants.EAST) {
				bb.setDirection(SwingConstants.SOUTH);
			} else {
				bb.setDirection(SwingConstants.EAST);
			}
			nestedPanel.setVisible(!(nestedPanel.isVisible()));
		}

	}

	private JPanel getHiddenPanel() {
		return this.hiddenPanel;
	}
	
	

}
