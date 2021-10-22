/*
 *  Copyright 2015 AT&T
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
package com.att.aro.ui.view.overviewtab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.CacheEntry;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.overview.AccessedDomainsTableModel;
import com.att.aro.ui.model.overview.DomainsTCPSessions;
import com.att.aro.ui.model.overview.ExpandedDomainTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.AROModelObserver;


/**
 *
 *
 */
public class OverviewTabTableSplitPane extends TabPanelJPanel implements  MouseListener {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LogManager.getLogger(OverviewTabTableSplitPane.class);	

	private JSplitPane tableSplitPane;
	private JPanel duplicatesPanel;
	private JSplitPane bottomSplitPane;
	
	private AccessedDomainsTableModel accessDomainModel = new AccessedDomainsTableModel();
	private ExpandedDomainTableModel expandedDomainModel = new ExpandedDomainTableModel();
	private DataTable<DomainsTCPSessions> accessedDataTable;
	private DataTable<Session> expandedDataTable;
	private DuplicateContentTablePanel dContentTablePanel;

	private AROModelObserver aroObservable;
	private OverviewTab overviewTab;
	public OverviewTabTableSplitPane(OverviewTab overviewTab){
		super();
		this.overviewTab = overviewTab;
	}
	
	public JSplitPane getOverviewSplitPanel(){
		aroObservable = new AROModelObserver();
		if(tableSplitPane == null){
			tableSplitPane = new JSplitPane();
			tableSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			tableSplitPane.setResizeWeight(0.5);

			tableSplitPane.setTopComponent(getJDuplicatesPanel(overviewTab));
			tableSplitPane.setBottomComponent(createTablesPanel());

		}
		
		return tableSplitPane;
		
	}
	
	/**
	 * Initializes and returns the Duplicate Contents Panel.
	 * 
	 * @return CacheAnalysisPanel The Duplicate Contents Panel.
	 */
	public  JPanel getJDuplicatesPanel(OverviewTab overviewTab) {
		if (duplicatesPanel == null) {
			dContentTablePanel = new DuplicateContentTablePanel(overviewTab);
			duplicatesPanel = dContentTablePanel.layoutDataPanel();
			aroObservable.registerObserver(dContentTablePanel);
		}
		return duplicatesPanel;
	}

	public void setHighlightedDuplicate(CacheEntry selectedDuplicate) {
		if (dContentTablePanel != null) {
			dContentTablePanel.setHighlightedDuplicate(selectedDuplicate);
		}
	}

	/**
	 * Initializes and returns the Split Pane that contains the tables at the
	 * bottom.
	 */
	private JSplitPane createTablesPanel() {
		if (bottomSplitPane == null) {
			bottomSplitPane = new JSplitPane();
			bottomSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			bottomSplitPane.setResizeWeight(0.5);
			bottomSplitPane.setLeftComponent(getSimpleDomainsPanel());
			bottomSplitPane.setRightComponent(getExpandedDomainsPanel());
			bottomSplitPane.setPreferredSize(new Dimension(100, 180));
		}
			
		return bottomSplitPane;
	}

	/**
	 * Initializes Domain TCP Sessions Panel.
	 */
	private JPanel getSimpleDomainsPanel() {
	
		JPanel simpleDomainsPanel = new JPanel(new BorderLayout());
			simpleDomainsPanel.add(getJLabelAccessedDomains(), BorderLayout.NORTH);
			simpleDomainsPanel.add(getjAccessedDomainsPanel(), BorderLayout.CENTER);
			
		return simpleDomainsPanel;
	}
	
	/**
	 * Initializes the Accessed Domains panel.
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getExpandedDomainsPanel() {
		JPanel expandedDomainsPanel = new JPanel();
		expandedDomainsPanel.setLayout(new BorderLayout());
		expandedDomainsPanel.add(getJLabelAccessedExpandedDomains(), BorderLayout.NORTH);
		expandedDomainsPanel.add(getjAccessedDomainsExpandedPanel(), BorderLayout.CENTER);
		
		
	
		return expandedDomainsPanel;
	}
	
	/**
	 * Initializes getJLabelAccessedDomains
	 * 
	 * @return javax.swing.JLabel
	 */
	private JLabel getJLabelAccessedDomains() {
		JLabel labelAccessedDomains = new JLabel(ResourceBundleHelper.getMessageString("simple.domain.title"), JLabel.CENTER);
		
		return labelAccessedDomains;
	}
	
	/**
	 * Initializes and returns the Domain TCP Sessions Panel.
	 */
	private JScrollPane getjAccessedDomainsPanel() {
		JScrollPane accessedDomainsPanel = new JScrollPane(getAccessedDomainContentTable());		
		return accessedDomainsPanel;
	}
	
	/**
	 * Initializes and returns the RequestResponseTable.
	 */
	public JTable getAccessedDomainContentTable() {
		if (accessedDataTable == null) {
			accessedDataTable = new DataTable<DomainsTCPSessions>(accessDomainModel);
			accessedDataTable.setName(ResourceBundleHelper.getMessageString("overview.accessed.domains.tableName"));
			DataTablePopupMenu popupMenu = (DataTablePopupMenu) accessedDataTable.getPopup();
            popupMenu.initialize();

			accessedDataTable.setAutoCreateRowSorter(true);
			accessedDataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			accessedDataTable.setGridColor(Color.LIGHT_GRAY);
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(accessDomainModel);
			accessedDataTable.setRowSorter(sorter);
			sorter.setComparator(0, Util.getDomainSorter());
			accessedDataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				
				@Override
				public void valueChanged(ListSelectionEvent arg0) {
					DomainsTCPSessions aTCpSession = accessedDataTable.getSelectedItem();
					if(null != aTCpSession){
						expandedDomainModel.setData(aTCpSession.getTcpSessions());
					} else {
						expandedDomainModel.removeAllRows();
					}
					
				}
			});
		}
		
		return accessedDataTable;
	}

	
	/**
	 * Initializes returns the Accessed Domains Label.
	 */
	private JLabel getJLabelAccessedExpandedDomains() {
	
		JLabel labelAccessedExpandedDomains = new JLabel(ResourceBundleHelper.getMessageString("expanded.title"), JLabel.CENTER);
		return labelAccessedExpandedDomains;
	}
	
	/**
	 * Initializes and returns the Accessed Domains panel.
	 */
	private JScrollPane getjAccessedDomainsExpandedPanel() {
		JScrollPane jAccessedDomainsExpandedPanel = new JScrollPane(getExpandedDomainContentTable());
		return jAccessedDomainsExpandedPanel;
	}
	
	/**
	 * Initializes and returns the RequestResponseTable.
	 */
	public JTable getExpandedDomainContentTable() {
		if (expandedDataTable == null) {
			expandedDataTable = new DataTable<Session>(expandedDomainModel);
			expandedDataTable.setName(ResourceBundleHelper.getMessageString("overview.domain.tcp.sessions.tableName"));
			expandedDataTable.setAutoCreateRowSorter(true);
			expandedDataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			expandedDataTable.setGridColor(Color.LIGHT_GRAY);
			expandedDataTable.addMouseListener(this);
			
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(expandedDomainModel);
			expandedDataTable.setRowSorter(sorter);
			sorter.setComparator(1, Util.getDomainSorter());

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) expandedDataTable.getPopup();
            popupMenu.initialize();
		}
		
		return expandedDataTable;
	}

	/**
	 * Do nothing Since current class is a split pane. Since extended baseJpane need to implement this method.
	 * Extended base class for Observer. 
	 */
	public JPanel layoutDataPanel(){
		return new JPanel(); // doing nothing 
	}
	
	private void loadAccessedDomainsTable(){
		List<Session> tcpSessions = new ArrayList<Session>();
		for(Session tcpSession : this.getAroModel().getAnalyzerResult().getSessionlist()){
			
			if(!tcpSession.isUdpOnly()){
				tcpSessions.add(tcpSession);
			}
		}
		accessDomainModel.setData(DomainsTCPSessions.extractDomainTcpSessions(tcpSessions));
	}
	
	public void refresh(AROTraceData aModel){
		aroObservable.refreshModel(aModel);
		loadAccessedDomainsTable();
	}

 
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent event) {
		if(event.getClickCount()==2){
			if (event.getSource() instanceof JTable){
				int selectionIndex = ((JTable)event.getSource()).getSelectedRow();
				LOGGER.info("selectionIndex: "+ selectionIndex);
				if(selectionIndex!=-1){
					Session session = expandedDomainModel.getValueAt(selectionIndex);
					overviewTab.updateDiagnosticsTab(session);
				}
			}
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	public AccessedDomainsTableModel getAccessDomainModel() {
		return accessDomainModel;
	}
	
	


}
