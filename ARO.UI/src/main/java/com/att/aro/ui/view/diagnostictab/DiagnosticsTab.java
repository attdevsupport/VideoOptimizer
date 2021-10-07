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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.bestpractice.pojo.ForwardSecrecyEntry;
import com.att.aro.core.bestpractice.pojo.TransmissionPrivateDataEntry;
import com.att.aro.core.bestpractice.pojo.UnsecureSSLVersionEntry;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfoWithSession;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.GUIPreferences;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTablePopupMenu;
import com.att.aro.ui.model.diagnostic.PacketViewTableModel;
import com.att.aro.ui.model.diagnostic.TCPUDPFlowsTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.overviewtab.DeviceNetworkProfilePanel;
import com.att.aro.ui.view.video.IVideoPlayer;
import com.att.aro.view.images.Images;

import lombok.Getter;


public class DiagnosticsTab extends TabPanelJPanel implements ListSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LogManager.getLogger(DiagnosticsTab.class);	
	private static final int MAX_ZOOM = 4;
	private static final Double MATCH_SECONDS_RANGE = 0.5;
	// network profile panel
	private DeviceNetworkProfilePanel deviceNetworkProfilePanel;
	// Chart panel
	private GraphPanel graphPanel;
	// Split pane for TCP flow data
	private JSplitPane internalPanel;
	private JSplitPane diagnosticsPanel;
	// TCP Flows header
	private JPanel tcpFlowsHeadingPanel;
	private JLabel tcpFlowsLabel;
	private DataTable<PacketInfo> jPacketViewTable;
	private RequestResponseDetailsPanel jHttpReqResPanel;
	private HttpDelayPanel jHttpDelayPanel;

	@Getter
	private TCPFlowsDataTable<Session> tcpflowsTable;

	// Model
	private TCPUDPFlowsTableModel jTcpUdpFlowsModel = TCPUDPFlowsTableModel.getInstance();
	private PacketViewTableModel jPacketViewTableModel = new PacketViewTableModel();
	private List<HttpRequestResponseInfoWithSession> requestResponseWithSession = new ArrayList<HttpRequestResponseInfoWithSession>();
	private AROTraceData analyzerResult;

	private BasicArrowButton arrowBtnWest;
	private BasicArrowButton arrowBtnEast;
	private JPanel refreshPanel;
	private JButton refreshGraphButton;
	private static final String REFRESH_AS_ACTION = "refreshGraph";
	private JScrollPane collapsibleRefreshPane;
	private boolean arrowBtnWestClickStatus = false;

	public List<HttpRequestResponseInfoWithSession> getRequestResponseWithSession() {
		return requestResponseWithSession;
	}

	public void setRequestResponseWithSession(List<HttpRequestResponseInfoWithSession> requestResponseWithSession) {
		this.requestResponseWithSession = requestResponseWithSession;
	}

	// Components for TCP Flows scroll table
	private JPanel jTCPFlowsPanel;
	private JScrollPane jTCPFlowsScrollPane;
	// TCP flow detail tabbed pane
	private JTabbedPane jTCPFlowsContentTabbedPane;
	// Packet view
	private JScrollPane jPacketViewTapScrollPane;
	// Content view
	private ContentViewJPanel jContentViewPanel; // Content View
	private DiagnosticTabHelper diagHelper = new DiagnosticTabHelper();
	private IVideoPlayer videoPlayer;
	private List<Session> sessionsSortedByTimestamp = new ArrayList<Session>();

	public IVideoPlayer getVideoPlayer() {
		return videoPlayer;
	}

	public void setVideoPlayer(IVideoPlayer videoPlayer) {
		this.videoPlayer = videoPlayer;
	}

	private AROTraceData aroTraceData;

	public AROTraceData getAroTraceData() {
		return aroTraceData;
	}

	public void setAroTraceData(AROTraceData aroTraceData) {
		this.aroTraceData = aroTraceData;
	}

	private boolean graphPanelClicked = false;
	private boolean bTCPPacketFound = false;
	private IAROView aroview;
	private IARODiagnosticsOverviewRoute diagnosticRoute;

	public IAROView getAroView() {
		return this.aroview;
	}

	public AROTraceData getAnalyzerResult() {
		return analyzerResult;
	}

	public void setAnalyzerResult(AROTraceData analyzerResult) {
		this.analyzerResult = analyzerResult;
	}

	JPanel chartAndTablePanel;
	GUIPreferences guiPreferences;
	

	public DiagnosticsTab(IAROView aroview, IARODiagnosticsOverviewRoute diagnosticRoute) {
		super(true);
		this.aroview = aroview;
		this.diagnosticRoute = diagnosticRoute;
		setLayout(new BorderLayout());
		add(getDeviceNetworkProfilePanel().layoutDataPanel(), BorderLayout.NORTH);
		chartAndTablePanel = new JPanel();
		chartAndTablePanel.setLayout(new BorderLayout());
		// Add chart
		chartAndTablePanel.add(getGraphPanel(), BorderLayout.NORTH);
		// Add TCP flows split pane
		chartAndTablePanel.add(getOrientationPanel(), BorderLayout.CENTER);
		chartAndTablePanel.add(getDiagnosticsPanel(), BorderLayout.CENTER);
		add(chartAndTablePanel, BorderLayout.CENTER);	
	}
	
	public IARODiagnosticsOverviewRoute getDiagnosticRoute() {
		return diagnosticRoute;
	}

	public void addGraphPanel() {
		if (chartAndTablePanel != null & diagnosticsPanel != null) {
			GraphPanel graphPanel = getGraphPanel();
			graphPanel.setGraphPanelBorder(false);
			chartAndTablePanel.add(graphPanel, BorderLayout.NORTH);
			diagnosticsPanel.setTopComponent(graphPanel);
			add(chartAndTablePanel, BorderLayout.CENTER);
		}
		if (guiPreferences == null) {
			guiPreferences = GUIPreferences.getInstance();
		}
		setChartOptions(guiPreferences.getChartPlotOptions());
	}

	/**
	 * Returns the Panel that contains the graph.
	 */
	public GraphPanel getGraphPanel() {
		if (graphPanel == null) {
			graphPanel = new GraphPanel(aroview, this);
			graphPanel.setZoomFactor(2);
			graphPanel.setMaxZoom(MAX_ZOOM);
			graphPanel.addGraphPanelListener(new GraphPanelListener() {
				@Override
				public void graphPanelClicked(double timeStamp) {
					setTimeLineToTable(timeStamp);
					if (getVideoPlayer() != null) {
						graphPanelClicked = true;
						getVideoPlayer().setMediaTime(timeStamp);
					}
				}
			});
		}
		return graphPanel;
	}

	private JSplitPane getOrientationPanel() {
		if (internalPanel == null) {
			internalPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getJTCPFlowsPanel(),
					getJTCPFlowsContentTabbedPane());
			internalPanel.setOneTouchExpandable(true);
			internalPanel.setContinuousLayout(true);
			internalPanel.setResizeWeight(0.5);
			internalPanel.setDividerLocation(0.5);
		}
		return internalPanel;
	}

	private JSplitPane getDiagnosticsPanel() {
		if (diagnosticsPanel == null) {
			diagnosticsPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getGraphPanel(), getOrientationPanel());
			diagnosticsPanel.setOneTouchExpandable(true);
			diagnosticsPanel.setContinuousLayout(true);
			diagnosticsPanel.setResizeWeight(0.5);
			diagnosticsPanel.setDividerLocation(0.5);
			diagnosticsPanel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
					new PropertyChangeListener() {
						@Override
						public void propertyChange(PropertyChangeEvent evt) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									getGraphPanel().layoutGraphLabels();
								}
							});
						}
					});
		}
		return diagnosticsPanel;
	}

	/**
	 * Initializes and returns the Tabbed pane at the bottom.
	 */
	private JTabbedPane getJTCPFlowsContentTabbedPane() {
		if (jTCPFlowsContentTabbedPane == null) {
			jTCPFlowsContentTabbedPane = new JTabbedPane();
			jTCPFlowsContentTabbedPane.addTab(ResourceBundleHelper.getMessageString("tcp.tab.reqResp"), null,
					getJHttpReqResPanel(), null);
			jTCPFlowsContentTabbedPane.addTab(ResourceBundleHelper.getMessageString("tcp.tab.packet"), null,
					getJPacketViewTapScrollPane(), null);
			jTCPFlowsContentTabbedPane.addTab(ResourceBundleHelper.getMessageString("tcp.tab.content"), null,
					getJContentViewPanel(), null);
			jTCPFlowsContentTabbedPane.addTab(ResourceBundleHelper.getMessageString("tcp.tab.delay"), null,
					getHttpDelayPanel(), null);
			// jTCPFlowsPanel.setPreferredSize(new Dimension(400, 110));
			jTCPFlowsPanel.setMinimumSize(new Dimension(400, 110));
		}
		return jTCPFlowsContentTabbedPane;
	}

	/**
	 * Initializes and returns the PacketViewTapScrollPane
	 */
	private JScrollPane getJPacketViewTapScrollPane() {
		if (jPacketViewTapScrollPane == null) {
			jPacketViewTapScrollPane = new JScrollPane(getJPacketViewTable());
		}
		return jPacketViewTapScrollPane;
	}

	/**
	 * Initializes and returns the Packet View Table.
	 */
	public DataTable<PacketInfo> getJPacketViewTable() {
		if (jPacketViewTable == null) {
			jPacketViewTable = new DataTable<PacketInfo>(jPacketViewTableModel);
			jPacketViewTable.setAutoCreateRowSorter(true);
			jPacketViewTable.setGridColor(Color.LIGHT_GRAY);
			jPacketViewTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				PacketInfo packetInfo;

				@Override
				public void valueChanged(ListSelectionEvent arg0) {
					PacketInfo packetInfo = jPacketViewTable.getSelectedItem();
					if (packetInfo != null && packetInfo != this.packetInfo) {
						double crossHairValue = packetInfo.getTimeStamp();
						boolean centerGraph = !(crossHairValue <= graphPanel.getViewportUpperBound()
								&& crossHairValue >= graphPanel.getViewportLowerBound());
						graphPanel.setGraphView(crossHairValue, centerGraph); // crossHairValue+103
						// getJHttpReqResPanel().select(
						// packetInfo.getRequestResponseInfo());
						if (videoPlayer != null) {
							// logger.info("enter getJPacketViewTable()");
							videoPlayer.setMediaTime(graphPanel.getCrosshair());
							// logger.info("leave getJPacketViewTable()");
						}
					}
					this.packetInfo = packetInfo;
				}
			});

			DataTablePopupMenu popupMenu = (DataTablePopupMenu) jPacketViewTable.getPopup();
            popupMenu.initialize();
		}
		return jPacketViewTable;
	}

	/**
	 * Initializes and returns the Panel for the Content View tab at the bottom.
	 */
	private ContentViewJPanel getJContentViewPanel() {
		if (jContentViewPanel == null) {
			jContentViewPanel = new ContentViewJPanel();
		}
		return jContentViewPanel;
	}
	
	/**
	 * Initializes and returns the Panel for the Delay tab at the bottom.
	 * @param httpDelayPanel 
	 * @return 
	 */
	private HttpDelayPanel getHttpDelayPanel() {
		if (jHttpDelayPanel == null) {
			jHttpDelayPanel = new HttpDelayPanel();
		}
		return jHttpDelayPanel;

	}

	/**
	 * Initializes and returns the Device network profile panel.
	 */
	public DeviceNetworkProfilePanel getDeviceNetworkProfilePanel() {
		if (deviceNetworkProfilePanel == null) {
			deviceNetworkProfilePanel = new DeviceNetworkProfilePanel();
		}
		return deviceNetworkProfilePanel;
	}

	/**
	 * Initializes jTCPFlowsPanel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJTCPFlowsPanel() {
		if (jTCPFlowsPanel == null) {
			jTCPFlowsPanel = new JPanel();
			jTCPFlowsPanel.setLayout(new BorderLayout());
			jTCPFlowsPanel.add(getTcpFlowsHeadingPanel(), BorderLayout.NORTH);
			jTCPFlowsPanel.add(getJTCPFlowsScrollPane(), BorderLayout.CENTER);
			jTCPFlowsPanel.add(getCollapsibleRefreshPanel(), BorderLayout.EAST);
			jTCPFlowsPanel.setMinimumSize(new Dimension(400, 200));
		}
		return jTCPFlowsPanel;
	}
	
	private JScrollPane getCollapsibleRefreshPanel() {
		if (collapsibleRefreshPane == null) {
			collapsibleRefreshPane = new JScrollPane();
			collapsibleRefreshPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			collapsibleRefreshPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			arrowBtnWest = new BasicArrowButton(SwingConstants.WEST);
			arrowBtnWest.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					collapsibleRefreshPane.setPreferredSize(getRefreshPanel().getPreferredSize());
					collapsibleRefreshPane.setViewportView(getRefreshPanel());
					arrowBtnWestClickStatus = true;
					jTCPFlowsPanel.updateUI();
					collapsibleRefreshPane.revalidate();
				}
			});
			collapsibleRefreshPane.setViewportView(arrowBtnWest);
		}
		return collapsibleRefreshPane;
	}
	
	public void openCollapsiblePane() {
		if (!arrowBtnWestClickStatus) {
			arrowBtnWest.doClick();
		}
	}

	private JPanel getRefreshPanel() {
		if (refreshPanel == null) {
			arrowBtnEast = new BasicArrowButton(SwingConstants.EAST);
			arrowBtnEast.setPreferredSize(new Dimension(20, 100));
			arrowBtnEast.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					collapsibleRefreshPane.setViewportView(arrowBtnWest);
					arrowBtnWestClickStatus = false;
					collapsibleRefreshPane.setPreferredSize(new Dimension(20, 100));
					jTCPFlowsPanel.updateUI();
					collapsibleRefreshPane.revalidate();
				}
			});
			refreshPanel = new JPanel();
			refreshPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc1 = new GridBagConstraints();
			gbc1.gridx = 0;
			gbc1.gridy = 0;
			gbc1.insets = new Insets(0, 0, 0, 0);
			gbc1.weighty = 0;
			gbc1.anchor = GridBagConstraints.FIRST_LINE_START;
			gbc1.fill = GridBagConstraints.BOTH;
			gbc1.ipady = 90;
			refreshPanel.add(arrowBtnEast, gbc1);

			GridBagConstraints gbc2 = new GridBagConstraints();
			gbc2.gridx = 1;
			gbc2.gridy = 0;
			gbc2.ipady = 0;
			gbc2.weighty = 1;
			gbc2.anchor = GridBagConstraints.CENTER;
			gbc2.fill = GridBagConstraints.NONE;
			refreshPanel.add(getRefreshButton(), gbc2);
			refreshPanel.setPreferredSize(new Dimension(60, 100));
		}
		return refreshPanel;
	}

	public JButton getRefreshButton() {
		if (refreshGraphButton == null) {
			ImageIcon refreshButtonIcon = Images.REFRESH.getIcon();
			refreshGraphButton = new JButton("", refreshButtonIcon);
			refreshGraphButton.setActionCommand(REFRESH_AS_ACTION);
			refreshGraphButton.setEnabled(false);
			refreshGraphButton.setPreferredSize(new Dimension(40, 30));
			refreshGraphButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getGraphPanel().filterFlowTable();
					refreshGraphButton.setEnabled(false);
				}
			});
			refreshGraphButton.setToolTipText(ResourceBundleHelper.getMessageString("chart.tooltip.refresh"));
		}
		return refreshGraphButton;
	}
	/**
	 * Creates the TCP Flows heading panel.
	 */
	private JPanel getTcpFlowsHeadingPanel() {
		if (tcpFlowsHeadingPanel == null) {
			tcpFlowsHeadingPanel = new JPanel();
			tcpFlowsHeadingPanel.setLayout(new GridBagLayout());
			tcpFlowsHeadingPanel.add(getTcpFlowsLabel());
			tcpFlowsHeadingPanel.setPreferredSize(new Dimension(110, 15));
		}
		return tcpFlowsHeadingPanel;
	}

	/**
	 * Returns the TCP flows label.
	 */
	private JLabel getTcpFlowsLabel() {
		if (tcpFlowsLabel == null) {
			tcpFlowsLabel = new JLabel(ResourceBundleHelper.getMessageString("tcp.title"));
		}
		return tcpFlowsLabel;
	}

	/**
	 * Initializes and returns the TCPFlowsScrollPane.
	 */
	private JScrollPane getJTCPFlowsScrollPane() {
		jTCPFlowsScrollPane = new JScrollPane(getJTCPFlowsTable());
		jTCPFlowsScrollPane.setPreferredSize(new Dimension(100, 200));
		return jTCPFlowsScrollPane;
	}

	/**
	 * Initializes and returns the Scroll Pane for the TCP flows table.
	 */
	public TCPFlowsDataTable<Session> getJTCPFlowsTable() {
		if (tcpflowsTable == null) {
			tcpflowsTable = new TCPFlowsDataTable<Session>(jTcpUdpFlowsModel, this);
			tcpflowsTable.setName("sessionTable");
			tcpflowsTable.setAutoCreateRowSorter(true);
			tcpflowsTable.setGridColor(Color.LIGHT_GRAY);
			tcpflowsTable.getSelectionModel().addListSelectionListener(this);
			DataTablePopupMenu popupMenu = (DataTablePopupMenu) tcpflowsTable.getPopup();
            popupMenu.setMenuItems(tcpflowsTable.getMenuItems());
            popupMenu.initialize();

			// Adding the table listener for getting the check box changes
			tcpflowsTable.getModel().addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent arg0) {
					setTraceAnalysis();
				}
			});
			TableRowSorter<TableModel> sorter = new TableRowSorter<>(tcpflowsTable.getModel());
			tcpflowsTable.setRowSorter(sorter);
			sorter.setComparator(TCPUDPFlowsTableModel.REMOTEIP_COL, Util.getDomainSorter());
			sorter.setComparator(TCPUDPFlowsTableModel.DOMAIN_COL, Util.getDomainSorter());
			sorter.toggleSortOrder(TCPUDPFlowsTableModel.TIME_COL);
		}
		return tcpflowsTable;
	}

	public void setTraceAnalysis() {
		getRefreshButton().setEnabled(analyzerResult != null); // Greg Story
	}

	/**
	 * Returns RequestResponseDetailsPanel.
	 *
	 * @return the jHttpReqResPanel
	 */
	public RequestResponseDetailsPanel getJHttpReqResPanel() {
		if (jHttpReqResPanel == null) {
			jHttpReqResPanel = new RequestResponseDetailsPanel();
		}
		return jHttpReqResPanel;
	}

	// get update info from core model
	@Override
	public void refresh(AROTraceData AnalyzerResult) {
		analyzerResult = AnalyzerResult;
		setAroTraceData(analyzerResult);
		getDeviceNetworkProfilePanel().refresh(analyzerResult);
		jTcpUdpFlowsModel.refresh(analyzerResult, getFont());
		
		sessionsSortedByTimestamp = analyzerResult.getAnalyzerResult().getSessionlist();
		setRequestResponseWithSession(buildHttpRequestResponseWithSession(analyzerResult.getAnalyzerResult().getSessionlist()));
		getGraphPanel().refresh(analyzerResult);
		// clear table
		jPacketViewTableModel.removeAllRows();
		getJHttpReqResPanel().getjRequestResponseTableModel().removeAllRows();
		getHttpDelayPanel().getHttpDelayTableModel().removeAllRows();
		getJContentViewPanel().getJContentTextArea().setText("");
	}

	public void setChartOptions(List<ChartPlotOptions> optionsSelected) {
		getGraphPanel().setChartOptions(optionsSelected);
	}

	/**
	 * Hide Charts from the Charts Panel on the Diagnostic tab
	 * If an argument list is passed, it will hide the sub-series of charts.
	 * If no arguments are passed, it will hide all charts.
	 */
	public void hideCharts(String... chartPlotOptionEnumNames) {
		getGraphPanel().hideChartOptions(chartPlotOptionEnumNames);
	}

	/**
	 * Show Charts from the Charts Panel on the Diagnostic tab
	 * If an argument list is passed, it will un-hide the sub-series of charts.
	 * If no arguments are passed, it will un-hide all hidden charts unless the sub-series is hidden.
	 */
	public void showCharts(String... chartPlotOptionEnumNames) {
		getGraphPanel().showChartOptions(chartPlotOptionEnumNames);
	}

	// get update info from tcp/udp flow table
	@Override
	public void valueChanged(ListSelectionEvent evt) {
		if (evt.getSource() instanceof ListSelectionModel) {
			ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
			if (lsm.getMinSelectionIndex() != -1) {
				Session session = getJTCPFlowsTable().getSelectedItem();
				if (session == null) {
					jPacketViewTableModel.removeAllRows();
					getJHttpReqResPanel().getjRequestResponseTableModel().removeAllRows();
					getHttpDelayPanel().getHttpDelayTableModel().removeAllRows();
					getJContentViewPanel().getJContentTextArea().setText("");
				} else {
					getHttpDelayPanel().updateTable(session);
					getJHttpReqResPanel().updateTable(session);
					if (session.isUdpOnly()) {
						jPacketViewTableModel.setData(session.getUdpPackets());
						getJPacketViewTable().setGridColor(Color.LIGHT_GRAY);
						if (!session.getUdpPackets().isEmpty()) {
							getJPacketViewTable().getSelectionModel().setSelectionInterval(0, 0);
						}
						if (jTCPFlowsContentTabbedPane.getSelectedComponent() == getJContentViewPanel()) {
							getJContentViewPanel().updateContext(session);
						}
						getJContentViewPanel().getJContentTextArea().setCaretPosition(0);						
					} else {
						jPacketViewTableModel.setData(session.getTcpPackets());
						getJPacketViewTable().setGridColor(Color.LIGHT_GRAY);
						if (!session.getTcpPackets().isEmpty()) {
							getJPacketViewTable().getSelectionModel().setSelectionInterval(0, 0);
						}
						getJContentViewPanel().updateContext(session);
						getJContentViewPanel().getJContentTextArea().setCaretPosition(0);
						
					}
				}
			}
		}
	}

	public HttpRequestResponseInfo getRrAssoSession(Session session) {
		HttpRequestResponseInfo reqInfo = null;
		for (HttpRequestResponseInfoWithSession reqResSession : requestResponseWithSession) {
			if (reqResSession.getSession().equals(session)) {
				reqInfo = reqResSession.getInfo();
				break;
			}
		}
		return reqInfo;
	}

	/**
	 * TODO: This belongs in core! As a matter of fact, it's mostly copied and
	 * pasted from internal code in CacheAnalysisImpl
	 *
	 * @param sessions
	 * @return
	 */
	private List<HttpRequestResponseInfoWithSession> buildHttpRequestResponseWithSession(List<Session> sessions) {
		List<HttpRequestResponseInfoWithSession> returnList = new ArrayList<HttpRequestResponseInfoWithSession>();
		for (Session session : sessions) {
			if (!session.isUdpOnly()) {
				for (HttpRequestResponseInfo item : session.getRequestResponseInfo()) {
					HttpRequestResponseInfoWithSession itemsession = new HttpRequestResponseInfoWithSession();
					itemsession.setInfo(item);
					itemsession.setSession(session);
					returnList.add(itemsession);
				}
			}
		}
		Collections.sort(returnList);
		return Collections.unmodifiableList(returnList);
	}

	public void setTimeLineLinkedComponents(double timeStamp, boolean isReset) {
		if (getAroTraceData() != null) {
			if (timeStamp < 0.0) {
				timeStamp = 0.0;
			}
			double traceDuration = getAroTraceData().getAnalyzerResult().getTraceresult().getTraceDuration();
			if (timeStamp > traceDuration) {
				timeStamp = traceDuration;
			}
			getGraphPanel().setGraphView(timeStamp, isReset);
		}
	}

	// old analyzer method name is setTimeLineLinkedComponents(double
	// timeStamp,double dTimeRangeInterval)
	public void setTimeLineToTable(double timeStamp) {
		// logger.info("enter setTimeLineTable()");
		if (getAroTraceData() == null) {
			LOGGER.info("no analyze traces data");
		} else {
			boolean bTCPTimeStampFound = false;
			boolean bExactMatch = false;
			// Attempt to find corresponding packet for time.
			double packetTimeStamp = 0.0;
			double packetTimeStampDiff = 0.0;
			double previousPacketTimeStampDiff = 9999.0;
			Session bestMatchingTcpSession = null;
			PacketInfo bestMatchingPacketInfo = null;
			// logger.info("enter sesionlist for loop");
			for (Session tcpSess : getAroTraceData().getAnalyzerResult().getSessionlist()) {
				PacketInfo packetInfo = diagHelper.getBestMatchingPacketInTcpSession(tcpSess, bExactMatch, timeStamp,
						MATCH_SECONDS_RANGE);
				if (packetInfo != null) {
					packetTimeStamp = packetInfo.getTimeStamp();
					packetTimeStampDiff = timeStamp - packetTimeStamp;
					if (packetTimeStampDiff < 0.0) {
						packetTimeStampDiff *= -1.0;
					}
					if (packetTimeStampDiff < previousPacketTimeStampDiff) {
						bestMatchingTcpSession = tcpSess;
						bestMatchingPacketInfo = packetInfo;
						bTCPTimeStampFound = true;
					}
				}
			}
			// logger.info("leave sesionlist for loop");
			if (bTCPTimeStampFound) {
				getJTCPFlowsTable().selectItem(bestMatchingTcpSession);
				getJPacketViewTable().selectItem(bestMatchingPacketInfo);
				getJPacketViewTable().setGridColor(Color.LIGHT_GRAY);
			} else {
				getJTCPFlowsTable().selectItem(null);
				getJPacketViewTable().selectItem(null);
				// if (videoPlayer != null) {
				// bTCPPacketFound = false;
				// videoPlayer.setMediaDisplayTime(graphPanel
				// .getCrosshair());
				// }
				// }
			}
		}
		// logger.info("leave setTimeLineTable()");
	}

	public boolean getTCPPacketFoundStatus() {
		return bTCPPacketFound;
	}

	public void reSetTCPPacketFoundStatus(boolean val) {
		bTCPPacketFound = val;
	}

	/**
	 * Highlights the specified TCP session in the TCP flows table.
	 *
	 * @param tcpSession
	 *            - The TCPSession object to be highlighted.
	 */
	public void setHighlightedTCP(Session tcpSession) {
		getJTCPFlowsTable().selectItem(tcpSession);
		getJTCPFlowsTable().showHighlightedSession(getJTCPFlowsTable().getSelectedRow());
	}

	public void setHighlightedTCP(HttpRequestResponseInfo reqResInfo) {
		for (HttpRequestResponseInfoWithSession reqResSession : requestResponseWithSession) {
			if (reqResSession.getInfo().equals(reqResInfo)) {
				Session sessionTemp = reqResSession.getSession();
				LOGGER.info("local port = " + sessionTemp.getLocalPort());
				setHighlightedTCP(reqResSession.getSession());
				jHttpReqResPanel.setHighlightedRequestResponse(reqResInfo);
				break;
			}
		}
	}

	public void setHighlightedSessionTCP(HttpRequestResponseInfo reqResInfo) {
		setHighlightedTCP(reqResInfo.getSession());
		for (HttpRequestResponseInfoWithSession reqResSession : requestResponseWithSession) {
			if (reqResInfo.getHostName() != null) {
				if (reqResSession.getInfo().equals(reqResInfo)) {
					LOGGER.info("local port = " + reqResSession.getSession().getLocalPort());
				jHttpReqResPanel.setHighlightedRequestResponse(reqResInfo);
					break;
				}
			}
		}
	}

	// only UnnecessaryConnectionEntry table use this method
	public void setHighlightedTCP(Double timestampParm) {
		if (timestampParm != null) {
			double timestamp = timestampParm.doubleValue();
			double timestampDiff = Double.MAX_VALUE;
			double lastTimestampDiff = timestampDiff;
			Session foundSession = null;
			for (Session tcpSess : sessionsSortedByTimestamp) {
				if (tcpSess != null) {
					double currentTimestampDiff = Math.abs(tcpSess.getSessionStartTime() - timestamp);
					if (currentTimestampDiff < timestampDiff) {
						timestampDiff = currentTimestampDiff;
						foundSession = tcpSess;
					}
					if (currentTimestampDiff > lastTimestampDiff) {
						break;
					}
					lastTimestampDiff = currentTimestampDiff;
				}
			}
			if (foundSession != null) {
				// setHighlightedTCP(foundSession, timestamp);
				setHighlightedTCP(foundSession);
			} else {
				LOGGER.warn("No session found to route to Diagnostic Tab for timestamp " + timestamp);
			}
		} else {
			LOGGER.warn("No timestamp for Diagnostic Tab routing");
		}
	}

	// only for security best practice table route
	public void setHighlightedTCP(Object routeInfo) {
		double timestamp = -1;
		String destIP = null;
		if (routeInfo instanceof TransmissionPrivateDataEntry) {
			TransmissionPrivateDataEntry entry = (TransmissionPrivateDataEntry) routeInfo;
			timestamp = entry.getSessionStartTime();
			destIP = entry.getDestIP();
		} else if (routeInfo instanceof UnsecureSSLVersionEntry) {
			UnsecureSSLVersionEntry entry = (UnsecureSSLVersionEntry) routeInfo;
			timestamp = entry.getSessionStartTime();
			destIP = entry.getDestIP();
		} else if (routeInfo instanceof ForwardSecrecyEntry) {
			ForwardSecrecyEntry entry = (ForwardSecrecyEntry) routeInfo;
			timestamp = entry.getSessionStartTime();
			destIP = entry.getDestIP();
		}
		if (timestamp == -1 || destIP == null) {
			LOGGER.warn("invalid route information");
			return;
		}
		for (Session session : sessionsSortedByTimestamp) {
			if (session != null && session.getRemoteIP() != null) {
				if (session.getSessionStartTime() == timestamp
						&& destIP.equals(session.getRemoteIP().getHostAddress())) {
					setHighlightedTCP(session);
					return;
				}
			}
		}
		LOGGER.warn("No session found to route to Diagnostic Tab for timestamp " + timestamp);
	}

	@Override
	public JPanel layoutDataPanel() {
		return null;
	}

	/**
	 * Returns the graph panel clicked status
	 *
	 * @return boolean value.
	 */
	public boolean IsGraphPanelClicked() {
		return graphPanelClicked;
	}

	/**
	 * Set the graph panel clicked status
	 */
	public void setGraphPanelClicked(boolean val) {
		graphPanelClicked = val;
	}

	public void updateTcpTable() {
	}
}
