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
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.IBurstCollectionAnalysis;
import com.att.aro.core.packetanalysis.IPacketAnalyzer;
import com.att.aro.core.packetanalysis.IRrcStateMachineFactory;
import com.att.aro.core.packetanalysis.pojo.AbstractRrcStateMachine;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.BurstCollectionAnalysisData;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.Statistic;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.GUIPreferences;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.model.diagnostic.GraphPanelHelper;
import com.att.aro.ui.model.diagnostic.TCPUDPFlowsTableModel;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.plot.AlarmPlot;
import com.att.aro.ui.view.diagnostictab.plot.AttenuatorPlot;
import com.att.aro.ui.view.diagnostictab.plot.BatteryPlot;
import com.att.aro.ui.view.diagnostictab.plot.BluetoothPlot;
import com.att.aro.ui.view.diagnostictab.plot.BurstPlot;
import com.att.aro.ui.view.diagnostictab.plot.CameraPlot;
import com.att.aro.ui.view.diagnostictab.plot.CpuPlot;
import com.att.aro.ui.view.diagnostictab.plot.DLPacketPlot;
import com.att.aro.ui.view.diagnostictab.plot.GpsPlot;
import com.att.aro.ui.view.diagnostictab.plot.NetworkTypePlot;
import com.att.aro.ui.view.diagnostictab.plot.RadioPlot;
import com.att.aro.ui.view.diagnostictab.plot.RrcPlot;
import com.att.aro.ui.view.diagnostictab.plot.ScreenStatePlot;
import com.att.aro.ui.view.diagnostictab.plot.SpeedThrottlePlot;
import com.att.aro.ui.view.diagnostictab.plot.TemperaturePlot;
import com.att.aro.ui.view.diagnostictab.plot.ThroughputPlot;
import com.att.aro.ui.view.diagnostictab.plot.UserEventPlot;
import com.att.aro.ui.view.diagnostictab.plot.VideoChunksPlot;
import com.att.aro.ui.view.diagnostictab.plot.WakeLockPlot;
import com.att.aro.ui.view.diagnostictab.plot.WifiPlot;
import com.att.aro.ui.view.video.IVideoPlayer;
import com.att.aro.view.images.Images;

/**
 * Represents the Graph Panel that contains the graph in the Diagnostics tab.
 * 
 * Layer propagation description of Graph panel from outer to inner
 * 
 * getPane() ->> getScollableChartLabelPanel() ->>
 * [getInternalScrollableContainer(), getLabelsPanel() ]
 * getInternalScrollableContainer() ->> chartPanelScrollPane() ->>
 * getChartAndHandlePanel() --> [getHandlePanel(),getChartPanel()]
 * 
 * 
 */
public class GraphPanel extends JPanel implements ActionListener, ChartMouseListener {
	private static final long serialVersionUID = 1L;
	// private static final ILogger LOGGER =
	// ContextAware.getAROConfigContext().getBean(ILogger.class);
	private IRrcStateMachineFactory statemachinefactory;// =
														// ContextAware.getAROConfigContext().getBean(IRrcStateMachineFactory.class);
	private IBurstCollectionAnalysis burstcollectionanalyzer;// =
																// ContextAware.getAROConfigContext().getBean(IBurstCollectionAnalysis.class);
	private IPacketAnalyzer packetanalyzer;// =
											// ContextAware.getAROConfigContext().getBean(IPacketAnalyzer.class);

	private static final String ZOOM_IN_ACTION = "zoomIn";
	private static final String ZOOM_OUT_ACTION = "zoomOut";
	private static final String SAVE_AS_ACTION = "saveGraph";

	private static final Shape DEFAULT_POINT_SHAPE = new Ellipse2D.Double(-2, -2, 4, 4);
	private static final int MIN_SIGNAL = -121;
	private static final int MAX_SIGNAL = -25;

	private static final Shape CPU_PLOT_POINT_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);
	private static final int MIN_CPU_USAGE = -10;
	private static final int MAX_CPU_USAGE = 110;
	private static final int DEFAULT_TIMELINE = 100;

	private static final int MIN_BATTERY = 0;
	private static final int MAX_BATTERY = 110;
	
	private static final int MIN_TEMPERATURE = 0;
	private static final int MAX_TEMPERATURE = 100;

	private final int UPPER_PANEL_HEIGHT = 280;// 222

	private Map<Integer, VideoEvent> chunkInfo = new TreeMap<>();

	private JViewport portChart;
	private JScrollPane paneChart;
	private JPanel internalScrollableContainer;
	private JPanel scrollChart;

	private JScrollPane pane;
	private JButton zoomInButton;
	private JButton zoomOutButton;
	private JButton saveGraphButton;
	private JButton refreshGraphButton;
	private JPanel zoomSavePanel;
	private JViewport port;
	private JPanel graphLabelsPanel;
	private JPanel chartPanel;
	private GraphPanelCrossHairHandle handlePanel;

	private ThroughputPlot throughput;
	private BurstPlot burstPlot;
	private RrcPlot rrcPlot;
	private UserEventPlot eventPlot;
	private TemperaturePlot tPlot;
	private DLPacketPlot dlPlot;
	private DLPacketPlot upPlot;
	private AlarmPlot alarmPlot;
	private GpsPlot gpsPlot;
	private RadioPlot radioPlot;
	private CpuPlot cpuPlot;
	private ScreenStatePlot ssPlot;
	private BatteryPlot bPlot;
	private BluetoothPlot bluetoothPlot;
	private WifiPlot wPlot;
	private CameraPlot cPlot;
	private NetworkTypePlot ntPlot;
	private WakeLockPlot wlPlot;
	private VideoChunksPlot vcPlot;
	private AttenuatorPlot attnrPlot;
	private SpeedThrottlePlot stPlot;
	private CombinedDomainXYPlot combinedPlot;
	
	private double endTime = 0.0;

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	private double startTime = 0.0;

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	private GraphPanelHelper graphHelper;
	private GUIPreferences guiPreferences;
	private CreateBarPlot barPlot;

	public CreateBarPlot getBarPlot() {
		return barPlot;
	}

	private Map<ChartPlotOptions, GraphPanelPlotLabels> subplotMap = new EnumMap<ChartPlotOptions, GraphPanelPlotLabels>(
			ChartPlotOptions.class);

	public Map<ChartPlotOptions, GraphPanelPlotLabels> getSubplotMap() {
		return subplotMap;
	}

	private Set<GraphPanelListener> listeners = new HashSet<GraphPanelListener>();

	// from jfree chart
	private CombinedDomainXYPlot plot;
	private JFreeChart advancedGraph;
	private ChartPanel advancedGraphPanel;
	private int zoomCounter = 0;
	private int maxZoom = 5;
	private double zoomFactor = 2;

	private NumberAxis axis;

	public NumberAxis getAxis() {
		return axis;
	}

	public void setAxis(NumberAxis axis) {
		this.axis = axis;
	}

	private JLabel axisLabel;

	public JLabel getAxisLabel() {
		return axisLabel;
	}

	public void setAxisLabel(JLabel axisLabel) {
		this.axisLabel = axisLabel;
	}

	public void setMaxZoom(int zoom) {
		this.maxZoom = zoom;
	}

	public void setZoomFactor(double zoomFactor) {
		this.zoomFactor = zoomFactor;
	}

	private AROTraceData traceData;// greg story

	public AROTraceData getTraceData() {
		return traceData;
	}

	public void setTraceData(AROTraceData traceData) {
		this.traceData = traceData;
	}

	private List<PacketInfo> allPackets;// GregStrory
	private double allTcpSessions; // GregStorys
	private double traceDuration; // GregStorys added to go back to original
									// trace duration
	private DiagnosticsTab parent;
	private Border border;

	/**
	 * Initializes a new instance of the GraphPanel class.
	 */
	public GraphPanel(IAROView aroview, DiagnosticsTab parent) {

		if (statemachinefactory == null) {
			statemachinefactory = ContextAware.getAROConfigContext().getBean(IRrcStateMachineFactory.class);
		}
		if (burstcollectionanalyzer == null) {
			burstcollectionanalyzer = ContextAware.getAROConfigContext().getBean(IBurstCollectionAnalysis.class);
		}
		if (packetanalyzer == null) {
			packetanalyzer = ContextAware.getAROConfigContext().getBean(IPacketAnalyzer.class);
		}
		if (graphHelper == null) {
			graphHelper = new GraphPanelHelper();
		}
		if (guiPreferences == null) {
			guiPreferences = GUIPreferences.getInstance();
		}
		if (barPlot == null) {
			barPlot = new CreateBarPlot();
		}

		this.parent = parent;
		subplotMap.put(ChartPlotOptions.THROUGHPUT, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.throughput"), getBarPlot().drawXYItemPlot(), 2));
		subplotMap.put(ChartPlotOptions.BURSTS,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.bursts"),
						getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.USER_INPUT,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.userInput"),
						getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.RRC, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.rrc"), getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.ALARM, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.alarm"), getBarPlot().drawXYBarPlot(Color.gray, true), 2));
		subplotMap.put(ChartPlotOptions.GPS, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.gps"), getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.BUFFER_OCCUPANCY, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.buffer.occupancy"), getBarPlot().drawXYItemPlot(), 1));
		subplotMap.put(ChartPlotOptions.VIDEO_CHUNKS, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.video.chunks"), getBarPlot().drawYIntervalPlot(), 1));
		subplotMap.put(ChartPlotOptions.RADIO,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.radio"),
						getBarPlot().drawStandardXYPlot(DEFAULT_POINT_SHAPE, Color.red, MIN_SIGNAL, MAX_SIGNAL), 2));
		subplotMap.put(ChartPlotOptions.BLUETOOTH,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.bluetooth"),
						getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.CAMERA,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.camera"),
						getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.SCREEN,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.screen"),
						getBarPlot().drawXYBarPlot(new Color(34, 177, 76), false), 1));
		subplotMap.put(ChartPlotOptions.BATTERY,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.battery"),
						getBarPlot().drawStandardXYPlot(DEFAULT_POINT_SHAPE, Color.red, MIN_BATTERY, MAX_BATTERY), 2));
		subplotMap.put(ChartPlotOptions.TEMPERATURE,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.temperature"),
						getBarPlot().drawStandardXYPlot(DEFAULT_POINT_SHAPE, Color.green, MIN_TEMPERATURE, MAX_TEMPERATURE), 2));
		subplotMap.put(ChartPlotOptions.WAKELOCK,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.wakelock"),
						getBarPlot().drawXYBarPlot(Color.yellow, false), 1));
		subplotMap.put(ChartPlotOptions.WIFI, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.wifi"), getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap.put(ChartPlotOptions.NETWORK_TYPE,
				new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.networkType"),
						getBarPlot().drawXYBarPlot(Color.gray, false), 1));
		subplotMap
				.put(ChartPlotOptions.CPU,
						new GraphPanelPlotLabels(ResourceBundleHelper.getMessageString("chart.cpu"), getBarPlot()
								.drawStandardXYPlot(CPU_PLOT_POINT_SHAPE, Color.black, MIN_CPU_USAGE, MAX_CPU_USAGE),
								1));
		subplotMap.put(ChartPlotOptions.UL_PACKETS, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.ul"), getBarPlot().drawYIntervalPlot(), 1));
		subplotMap.put(ChartPlotOptions.DL_PACKETS, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.dl"), getBarPlot().drawYIntervalPlot(), 1));
		subplotMap.put(ChartPlotOptions.BUFFER_TIME_OCCUPANCY, new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.bufferTime.occupancy"), getBarPlot().drawXYItemPlot(), 1));
				
		subplotMap.put(ChartPlotOptions.ATTENUATION,new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.attenuation"),getBarPlot().drawStepChartPlot(),2));

		subplotMap.put(ChartPlotOptions.SPEED_THROTTLE,new GraphPanelPlotLabels(
				ResourceBundleHelper.getMessageString("chart.attenuation"),getBarPlot().drawStepChartPlot(),2));
		
		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(300, 280));
		add(getZoomSavePanel(), BorderLayout.EAST);
		add(getPane(), BorderLayout.CENTER);
		setGraphPanelBorder(true);
		setChartOptions(guiPreferences.getChartPlotOptions());
	}
	
	public void setGraphPanelBorder(boolean value){
		if(true == value){
			border = new RoundedBorder(new Insets(20, 20, 20, 20), Color.WHITE);
		}else{
			border = null;
		}
		setBorder(border);
	}

	// In 4.1.1, the method called refreshGraph()
	public void filterFlowTable() {
		// Greg Story
		AROTraceData filteredSessionTraceData = getTraceData();
		double filteredStartTime = 0.0;
		double filteredEndTime = 0.0;
		double filteredDuration = filteredSessionTraceData.getAnalyzerResult().getTraceresult().getTraceDuration();

		List<Session> tcpsessionsList = new ArrayList<Session>();
		if (getTraceData() == null) {
			return;
		} else {
			TCPUDPFlowsTableModel model = (TCPUDPFlowsTableModel) parent.getJTCPFlowsTable().getModel();
			Map<String, Session> subSessionMap = model.getSessionMap();
			Map<String, Boolean> subcheckboxMap = model.getCheckboxMap();
			for (Map.Entry<String, Boolean> entry : subcheckboxMap.entrySet()) {
				if (entry.getValue()) {
					tcpsessionsList.add(subSessionMap.get(entry.getKey()));
				}
			}

			// tcpsessionsList =
			// parent.getJTCPFlowsTable().getSelectedCheckboxRows(1);
			filteredSessionTraceData.getAnalyzerResult().setSessionlist(tcpsessionsList);
		}

		List<PacketInfo> packetsForSelectedSession = new ArrayList<PacketInfo>();
		for (Session tcpSession : tcpsessionsList) {
			if (tcpSession.getPackets() != null) {
				packetsForSelectedSession.addAll(tcpSession.getPackets());
			}
		}

		// when generating graph, make sure session is ordered by time stamp
		Collections.sort(packetsForSelectedSession, new Comparator<PacketInfo>() {
			@Override
			public int compare(PacketInfo p1, PacketInfo p2) {
				return (int) (p1.getTimeStamp() * 1000 - p2.getTimeStamp() * 1000);
			}
		});

		boolean selectedAllPackets = false;

		// Adding the TCP packets to the trace for getting redoing the analysis
		if (packetsForSelectedSession.size() > 0) {
			if (tcpsessionsList.size() == getAllTcpSessions()) { // For select
																	// all use
																	// all
																	// exiting
																	// packets
				filteredSessionTraceData.getAnalyzerResult().getTraceresult().setAllpackets(getAllPackets());
				selectedAllPackets = true;
			} else {
				// Collections.sort(packetsForSelectedSession);//?

				filteredSessionTraceData.getAnalyzerResult().getTraceresult().setAllpackets(packetsForSelectedSession);
			}
		}

		if (selectedAllPackets) {
			filteredStartTime = -0.01;
			filteredEndTime = filteredDuration;
		} else {
			int index = 0;
			for (Session tcpSession : tcpsessionsList) {
				if (tcpSession.getPackets().size() != 0) {
					if (index == 0) {
						filteredStartTime = tcpSession.getPackets().get(0).getTimeStamp();
						filteredEndTime = tcpSession.getPackets().get(0).getTimeStamp();
					}

					if (filteredStartTime > tcpSession.getPackets().get(0).getTimeStamp()) {
						filteredStartTime = tcpSession.getPackets().get(0).getTimeStamp();
					}

					if (filteredEndTime < tcpSession.getPackets().get(0).getTimeStamp()) {
						filteredEndTime = tcpSession.getPackets().get(0).getTimeStamp();
					}
					index++;
				}
			}
			if (index == 0) {
				filteredStartTime = 0.0;
				filteredEndTime = 0.0;
			}
		}
		// for Analysis data perticalar time of the graph, some number is not
		// clear..
		if (filteredStartTime > 0) {
			filteredStartTime = filteredStartTime - 2;// adjust the time line
														// axis number
			if (filteredStartTime < 0) {
				filteredStartTime = -0.01;
			}
		}
		if (filteredStartTime < 0) {
			filteredStartTime = -0.01;
		}
		if (!selectedAllPackets) {
			if (filteredEndTime > 0) {
				filteredEndTime = filteredEndTime + 15;// adjust the time line
														// axis number
			}
			if (filteredEndTime > filteredDuration) {
				filteredEndTime = filteredDuration;
			}
		}

		this.startTime = filteredStartTime;
		this.endTime = filteredEndTime;

		if (getTraceData() != null) {
			TimeRange timeRange = new TimeRange(filteredStartTime, filteredEndTime);
			AnalysisFilter filter = filteredSessionTraceData.getAnalyzerResult().getFilter();
			filter.setTimeRange(timeRange);
			filteredSessionTraceData.getAnalyzerResult().setFilter(filter);
			Statistic stat = packetanalyzer.getStatistic(packetsForSelectedSession);
			int totaltemp = 0;
			for (Session byteCountSession : tcpsessionsList) {
				totaltemp += byteCountSession.getBytesTransferred();
			}
			stat.setTotalByte(totaltemp);
			AbstractRrcStateMachine statemachine = statemachinefactory.create(packetsForSelectedSession,
					filteredSessionTraceData.getAnalyzerResult().getProfile(), stat.getPacketDuration(),
					filteredDuration, stat.getTotalByte(), timeRange);

			BurstCollectionAnalysisData burstcollectiondata = new BurstCollectionAnalysisData();
			if (stat.getTotalByte() > 0) {
				burstcollectiondata = burstcollectionanalyzer.analyze(packetsForSelectedSession,
						filteredSessionTraceData.getAnalyzerResult().getProfile(), stat.getPacketSizeToCountMap(),
						statemachine.getStaterangelist(),
						filteredSessionTraceData.getAnalyzerResult().getTraceresult().getUserEvents(),
						filteredSessionTraceData.getAnalyzerResult().getTraceresult().getCpuActivityList()
								.getCpuActivities(),
						tcpsessionsList);
			}
			filteredSessionTraceData.getAnalyzerResult().getStatistic().setTotalByte(stat.getTotalByte());
			filteredSessionTraceData.getAnalyzerResult().setStatemachine(statemachine);
			filteredSessionTraceData.getAnalyzerResult().setBurstCollectionAnalysisData(burstcollectiondata);
			refresh(filteredSessionTraceData);

		}

	}

	// In 4.1.1, the method name is resetChart(TraceData.Analysis analysis)
	public void refresh(AROTraceData aroTraceData) {
		getSaveGraphButton().setEnabled(aroTraceData != null);
		if (combinedPlot != null) {
			setGraphView(combinedPlot.getDomainCrosshairValue(), true);
		} else {
			setGraphView(0, true);
		}
		setTraceData(aroTraceData);
		if(aroTraceData != null) {
			setAllPackets(aroTraceData.getAnalyzerResult().getTraceresult().getAllpackets());
			setTraceDuration(aroTraceData.getAnalyzerResult().getTraceresult().getTraceDuration());
			setAllTcpSessions(aroTraceData.getAnalyzerResult().getSessionlist().size());// list
																						// length
		} else {
			setAllPackets(new LinkedList<PacketInfo>());
			setTraceDuration(0);
			setAllTcpSessions(0);
		}
		

		if (aroTraceData != null && aroTraceData.getAnalyzerResult().getFilter() != null
				&& aroTraceData.getAnalyzerResult().getFilter().getTimeRange() != null) {
			if (aroTraceData.getAnalyzerResult().getSessionlist().size() > 0
					&& aroTraceData.getAnalyzerResult().getFilter().getTimeRange().getBeginTime() < aroTraceData
							.getAnalyzerResult().getFilter().getTimeRange().getEndTime()) {
				getAxis().setRange(new Range(aroTraceData.getAnalyzerResult().getFilter().getTimeRange().getBeginTime(),
						aroTraceData != null ? aroTraceData.getAnalyzerResult().getFilter().getTimeRange().getEndTime()
								: DEFAULT_TIMELINE));
			} else {
				getAxis().setRange(new Range(-0.01, 0));
			}
		} else {
			if (getEndTime() > 0) { // greg story selected rows from UI
				if (aroTraceData != null) {
					getAxis().setRange(new Range(getStartTime(), getEndTime()));
				}
				setStartTime(0.0); // Reset times
				setEndTime(0.0);
			} else {
				getAxis().setRange(new Range(-0.01, aroTraceData != null
						? aroTraceData.getAnalyzerResult().getTraceresult().getTraceDuration() : DEFAULT_TIMELINE));
			}
		}
		if (aroTraceData != null && aroTraceData.getAnalyzerResult().getSessionlist().size() > 0) {
			for (Map.Entry<ChartPlotOptions, GraphPanelPlotLabels> entry : getSubplotMap().entrySet()) {
				switch (entry.getKey()) {
				case THROUGHPUT:
					if (throughput == null) {
						throughput = new ThroughputPlot();
					}
					throughput.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case BURSTS:
					if (burstPlot == null) {
						burstPlot = new BurstPlot();
					}
					// burstPlot = new BurstPlot();
					burstPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case RRC:
					if (rrcPlot == null) {
						rrcPlot = new RrcPlot();
					}
					rrcPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case USER_INPUT:
					if (eventPlot == null) {
						eventPlot = new UserEventPlot();
					}
					eventPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case DL_PACKETS:
					if (dlPlot == null) {
						dlPlot = new DLPacketPlot();
					}
					dlPlot.populate(entry.getValue().getPlot(), aroTraceData, true);
					break;
				case UL_PACKETS:
					if (upPlot == null) {
						upPlot = new DLPacketPlot();
					}
					upPlot.populate(entry.getValue().getPlot(), aroTraceData, false);
					break;
				case ALARM:
					if (alarmPlot == null) {
						alarmPlot = new AlarmPlot();
					}
					alarmPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case GPS:
					if (gpsPlot == null) {
						gpsPlot = new GpsPlot();
					}
					gpsPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case RADIO:
					if (radioPlot == null) {
						radioPlot = new RadioPlot();
					}
					radioPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case CPU:
					if (cpuPlot == null) {
						cpuPlot = new CpuPlot();
					}
					cpuPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case SCREEN:
					if (ssPlot == null) {
						ssPlot = new ScreenStatePlot();
					}
					ssPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case BATTERY:
					if (bPlot == null) {
						bPlot = new BatteryPlot();
					}
					bPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case TEMPERATURE:
					if (tPlot == null) {
						tPlot = new TemperaturePlot();
					}
					tPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case BLUETOOTH:
					if (bluetoothPlot == null) {
						bluetoothPlot = new BluetoothPlot();
					}
					bluetoothPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case WIFI:
					if (wPlot == null) {
						wPlot = new WifiPlot();
					}
					wPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case CAMERA:
					if (cPlot == null) {
						cPlot = new CameraPlot();
					}
					cPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case NETWORK_TYPE:
					if (ntPlot == null) {
						ntPlot = new NetworkTypePlot();
					}
					ntPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case WAKELOCK:
					if (wlPlot == null) {
						wlPlot = new WakeLockPlot();
					}
					wlPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
				case ATTENUATION:
					if (attnrPlot == null) {
						attnrPlot = new AttenuatorPlot();
					}
					attnrPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;

				case SPEED_THROTTLE:
					if(stPlot == null){
						stPlot = new SpeedThrottlePlot();
					}
					stPlot.populate(entry.getValue().getPlot(), aroTraceData);
					break;
					
				case VIDEO_CHUNKS:
					if (vcPlot == null) {
						vcPlot = new VideoChunksPlot();
					}
					XYPlot bufferOccupancyPlot = getSubplotMap().get(ChartPlotOptions.BUFFER_OCCUPANCY).getPlot();
					XYPlot bufferTimePlot = getSubplotMap().get(ChartPlotOptions.BUFFER_TIME_OCCUPANCY).getPlot();
					// videoChunkPlotDataItem = false;
					this.chunkInfo.clear();
					vcPlot.setBufferOccupancyPlot(bufferOccupancyPlot);
					vcPlot.setBufferTimePlot(bufferTimePlot);
					// vcPlot.setFirstChunkPlayTime(0);
					AROManifest selectedManifest=null;
					int count=0;
					VideoUsage videoUsage = aroTraceData.getAnalyzerResult().getVideoUsage();
					if (videoUsage != null) {
						for (AROManifest manifest : videoUsage.getManifests()) {
							if (manifest != null && manifest.isSelected()) {
								selectedManifest = manifest;
								count++;
							}
						}
						if (count == 1 && selectedManifest != null && selectedManifest.getDelay() != 0) {
							VideoEvent firstSegment = (VideoEvent) selectedManifest.getVideoEventsBySegment().toArray()[0];

							Map<AROManifest, VideoEvent> firstSelectedSegments = null;
							if (vcPlot.getVideoChunkPlotterReference() != null && aroTraceData.getAnalyzerResult().getVideoUsage().getChunkPlayTimeList() != null) {
								firstSelectedSegments = aroTraceData.getAnalyzerResult().getVideoUsage().getFirstSelectedSegment();
							}
							if (firstSelectedSegments != null) {
								for (AROManifest manifest : firstSelectedSegments.keySet()) {
									if (manifest.equals(selectedManifest)) {
										firstSegment = firstSelectedSegments.get(manifest);
										break;
									}
								}
							} else 
								if (selectedManifest.getVideoFormat() == VideoFormat.MPEG4) {
								for (VideoEvent video : selectedManifest.getVideoEventsBySegment()) {
									if (video.getSegment() != 0) {
										firstSegment = video;
										break;
									}
								}
							}

							vcPlot.refreshPlot(getSubplotMap().get(ChartPlotOptions.VIDEO_CHUNKS).getPlot(), aroTraceData, selectedManifest.getDelay() + firstSegment.getEndTS(),
									firstSegment);
						} else {
							vcPlot.populate(entry.getValue().getPlot(), aroTraceData);
						}
						SliderDialogBox.segmentListChosen = new ArrayList<>();
					}
					break;
				/*
				 * case BUFFER_OCCUPANCY: if(boPlot==null){ boPlot = new
				 * BufferOccupancyPlot(); }
				 * boPlot.populate(entry.getValue().getPlot(),aroTraceData);
				 * break;
				 */
				default:
					break;
				}
			}
		}
		getZoomInButton().setEnabled(aroTraceData != null);
		getZoomOutButton().setEnabled(aroTraceData != null);
		getSaveGraphButton().setEnabled(aroTraceData != null);
		if(aroTraceData != null) {
			parent.getDeviceNetworkProfilePanel().refresh(aroTraceData); // Greg
																		 // Story
		}
	}

	// need add more
	public void hideChartOptions() {
		resetGraphZoom();
		advancedGraphPanel.setVisible(false);
	}
	
	public void showChartOptions() {
		// these log.error lines are for tracking down a problem in jfreechart, leave in until problem is resolved
		// if "showChartOptions()" is not followed by " done" then VO is stuck in a deadlock
		advancedGraphPanel.setVisible(true);
	}
	
	public void setChartOptions(List<ChartPlotOptions> optionsSelected) {

		if (optionsSelected == null || optionsSelected.contains(ChartPlotOptions.DEFAULT_VIEW)) {
			optionsSelected = ChartPlotOptions.getDefaultList();
		}

		// Remove all plots from combined plot
		CombinedDomainXYPlot plot = getPlot();
		for (GraphPanelPlotLabels subplot : getSubplotMap().values()) {
			if (subplot != null && subplot.getPlot() != null) {
				plot.remove(subplot.getPlot());
				subplot.getLabel().setVisible(false);
			}
		}

		// Add selected plots
		for (ChartPlotOptions option : graphHelper.getPlotOrder()) {
			// Keep charts in order of enum
			if (optionsSelected.contains(option)) {
				GraphPanelPlotLabels subplot = getSubplotMap().get(option);
				if (subplot != null && subplot.getPlot() != null) {
					plot.add(subplot.getPlot(), subplot.getWeight());
					subplot.getLabel().setVisible(true);
				}
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				layoutGraphLabels();

			}
		});
	}

	public void layoutGraphLabels() {
		CombinedDomainXYPlot combinedPlot = getPlot();
		int height = getChartPanel().getBounds().height - 15;// - 20;

		// logger.info("height: "+ height);
		// find weights and use them to determine how may divisions are needed.
		int plotWeightedDivs = 0;
		List<?> plots = combinedPlot.getSubplots();
		for (Object obj : plots) {
			if (obj instanceof XYPlot) {
				plotWeightedDivs += ((XYPlot) obj).getWeight();
			}
		}

		// check for zero
		plotWeightedDivs = plotWeightedDivs == 0 ? 1 : plotWeightedDivs;

		// determine the size of the divisions for each XYPlot
		int division = Math.round(((float) height) / ((float) plotWeightedDivs));

		// working from top to bottom, set the y-coord. for the first XYPlot
		int currentY = getLabelsPanel().getBounds().y + getChartPanel().getBounds().y; // getLabelsPanel().getBounds().y
																						// +
																						// 4
																						// +
																						// getChartPanel().getBounds().y;
		// loop on the list of Plots
		// logger.info("size: "+ graphHelper.getPlotOrder().size());
		for (ChartPlotOptions option : graphHelper.getPlotOrder()) {
			GraphPanelPlotLabels subplot = getSubplotMap().get(option);
			if (subplot != null && subplot.getLabel().isVisible()) {
				int weightDivisionFactor = division * subplot.getWeight();
				// set the current position using weight
				subplot.getLabel().setBounds(3, currentY + 1, 100, weightDivisionFactor + 3);
				// adjust the currentY value for the next label in the loop
				currentY += weightDivisionFactor;
			}
		}

		getAxisLabel().setBounds(3, height + 4, 100, 15); // + 3
															// +getChartPanel().getBounds().y
		// logger.info("axis label height: " + (height + 3 +
		// getChartPanel().getBounds().y) );

	}

	private JPanel getZoomSavePanel() {
		if (zoomSavePanel == null) {
			zoomSavePanel = new JPanel();
			zoomSavePanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0; // Greg Story
			gbc.gridy = 0;
			zoomSavePanel.add(getZoomInButton(), gbc);
			GridBagConstraints gbc1 = new GridBagConstraints();
			gbc1.gridx = 0;
			gbc1.gridy = 1;
			zoomSavePanel.add(getZoomOutButton(), gbc1);
			GridBagConstraints gbc2 = new GridBagConstraints();
			gbc2.gridx = 0;
			gbc2.gridy = 2;
			zoomSavePanel.add(getSaveGraphButton(), gbc2);
		}
		return zoomSavePanel;
	}

	private JButton getZoomOutButton() {
		if (zoomOutButton == null) {
			ImageIcon zoomOutButtonIcon = Images.DEMAGNIFY.getIcon();
			zoomOutButton = new JButton("", zoomOutButtonIcon);
			zoomOutButton.setActionCommand(ZOOM_OUT_ACTION);
			zoomOutButton.setEnabled(false);
			zoomOutButton.setPreferredSize(new Dimension(60, 30));
			zoomOutButton.addActionListener(this);
			zoomOutButton.setToolTipText(ResourceBundleHelper.getMessageString("chart.tooltip.zoomout"));
		}
		return zoomOutButton;
	}

	private JButton getZoomInButton() {
		if (zoomInButton == null) {
			ImageIcon zoomInButtonIcon = Images.MAGNIFY.getIcon();
			zoomInButton = new JButton("", zoomInButtonIcon);
			zoomInButton.setActionCommand(ZOOM_IN_ACTION);
			zoomInButton.setEnabled(false);
			zoomInButton.setPreferredSize(new Dimension(60, 30));
			zoomInButton.addActionListener(this);
			zoomInButton.setToolTipText(ResourceBundleHelper.getMessageString("chart.tooltip.zoomin"));
		}
		return zoomInButton;
	}

	private JButton getSaveGraphButton() {
		if (saveGraphButton == null) {
			ImageIcon saveGraphButtonIcon = Images.SAVE.getIcon();
			saveGraphButton = new JButton("", saveGraphButtonIcon);
			saveGraphButton.setActionCommand(SAVE_AS_ACTION);
			saveGraphButton.setEnabled(false);
			saveGraphButton.setPreferredSize(new Dimension(60, 30));
			saveGraphButton.addActionListener(this);
			saveGraphButton.setToolTipText(ResourceBundleHelper.getMessageString("chart.tooltip.saveas"));
		}
		return saveGraphButton;
	}

	private JScrollPane getPane() {
		if (pane == null) {
			pane = new JScrollPane();
			pane.setMinimumSize(new Dimension(300, 280));
			pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			pane.setViewport(getViewport());
		}
		return pane;
	}

	private JViewport getViewport() {
		if (port == null) {
			port = new JViewport();
			port.setView(getScollableChartLabelPanel()); // getChartAndHandlePanel());
		}
		return port;
	}

	private JViewport getViewportChartPane() {
		if (portChart == null) {
			portChart = new JViewport();
			portChart.setView(getChartAndHandlePanel());
			// portChart.setAutoscrolls(true);
		}
		return portChart;
	}

	private JScrollPane chartPanelScrollPane() {
		if (paneChart == null) {
			paneChart = new JScrollPane();
			paneChart.setMinimumSize(new Dimension(100, 110));
			paneChart.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			paneChart.getHorizontalScrollBar().setUnitIncrement(10);
			paneChart.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			paneChart.setViewport(getViewportChartPane());
			// paneChart.setAutoscrolls(true);
		}

		return paneChart;
	}

	private JPanel getInternalScrollableContainer() {
		if (internalScrollableContainer == null) {
			internalScrollableContainer = new JPanel();
			internalScrollableContainer.setLayout(new BorderLayout());

			internalScrollableContainer.setPreferredSize(new Dimension(110, UPPER_PANEL_HEIGHT));

			internalScrollableContainer.add(chartPanelScrollPane(), BorderLayout.CENTER);
		}

		return internalScrollableContainer;
	}

	private JPanel getScollableChartLabelPanel() {
		if (scrollChart == null) {
			scrollChart = new JPanel();
			scrollChart.setLayout(new BorderLayout());

			scrollChart.add(getInternalScrollableContainer(), BorderLayout.CENTER);
			scrollChart.add(getLabelsPanel(), BorderLayout.WEST);
		}
		return scrollChart;
	}

	private JPanel getChartAndHandlePanel() {
		if (chartPanel == null) {
			chartPanel = new JPanel();
			chartPanel.setLayout(new BorderLayout());

			chartPanel.add(getHandlePanel(), BorderLayout.NORTH);
			chartPanel.add(getChartPanel(), BorderLayout.CENTER);
		}
		return chartPanel;
	}

	private GraphPanelCrossHairHandle getHandlePanel() {
		if (handlePanel == null) {
			handlePanel = new GraphPanelCrossHairHandle(Color.blue);
		}
		return handlePanel;
	}

	private ChartPanel getChartPanel() {
		if (advancedGraphPanel == null) {
			advancedGraphPanel = new ChartPanel(getAdvancedGraph());
			advancedGraphPanel.setMouseZoomable(false);
			advancedGraphPanel.setDomainZoomable(false);
			advancedGraphPanel.setRangeZoomable(false);
			advancedGraphPanel.setDisplayToolTips(true);
			advancedGraphPanel.addChartMouseListener(this);
			advancedGraphPanel.setAutoscrolls(false);
			advancedGraphPanel.setPopupMenu(null);
			advancedGraphPanel.setPreferredSize(new Dimension(100, 100));
			advancedGraphPanel.setRefreshBuffer(true);
			advancedGraphPanel.setMaximumDrawWidth(100000);

		}
		return advancedGraphPanel;
	}

	private JFreeChart getAdvancedGraph() {
		if (advancedGraph == null) {
			advancedGraph = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, getPlot(), true);
			advancedGraph.removeLegend();
		}
		return advancedGraph;
	}

	private JPanel getLabelsPanel() {
		if (graphLabelsPanel == null) {
			graphLabelsPanel = new JPanel();
			graphLabelsPanel.setPreferredSize(new Dimension(100, 110));
			graphLabelsPanel.setLayout(null);// in order to set label position,
												// it has to set null
			axisLabel = new JLabel(ResourceBundleHelper.getMessageString("chart.timeline"));
			axisLabel.setHorizontalAlignment(SwingConstants.CENTER);

			for (GraphPanelPlotLabels label : getSubplotMap().values()) {
				// logger.info(label.getLabel().toString() );
				graphLabelsPanel.add(label.getLabel());
			}
			graphLabelsPanel.add(axisLabel);
		}

		return graphLabelsPanel;
	}

	private CombinedDomainXYPlot getPlot() {
		if (plot == null) {
			axis = new NumberAxis();
			axis.setStandardTickUnits(graphHelper.getTickUnits());
			axis.setRange(new Range(0, DEFAULT_TIMELINE));
			axis.setLowerBound(0);

			axis.setAutoTickUnitSelection(true);
			axis.setTickMarkInsideLength(1);
			axis.setTickMarkOutsideLength(1);

			axis.setMinorTickMarksVisible(true);
			axis.setMinorTickMarkInsideLength(2f);
			axis.setMinorTickMarkOutsideLength(2f);
			axis.setTickMarkInsideLength(4f);
			axis.setTickMarkOutsideLength(4f);

			plot = new CombinedDomainXYPlot(axis);
			plot.setOrientation(PlotOrientation.VERTICAL);
			plot.setGap(0.1);
		}
		return plot;
	}

	public void setGraphView(double graphCrosshairSetting, boolean centerChartOnCrosshair) {
		setCrossHair(graphCrosshairSetting);
		if (centerChartOnCrosshair) {
			resetScrollPosition();
		}

	}

	private void resetScrollPosition() {
		// SwingUtilities.invokeLater(new Runnable() {
		// @Override
		// public void run() {
		// getPane().getHorizontalScrollBar().setValue(getCrosshairViewPos());
		chartPanelScrollPane().getHorizontalScrollBar().setValue(getCrosshairViewPos());
		// }
		// });
		// update the handle position
		getHandlePanel().setHandlePosition(getHandleCoordinate());
	}

	private int getCrosshairViewPos() {
		float pos = getCrosshairPosRatio() - getCrossSectionOffsetRatio();
		float chartPosValue = new Float(getScrollMax() * pos);
		return Math.max(0, Math.round(chartPosValue));
	}

	private float getCrosshairPosRatio() {
		return new Float(new Float(getCrosshair()) / new Float(getGraphLength()));
	}

	private float getCrossSectionOffsetRatio() {
		return new Float(new Float(getCrossSection()) / new Float(getScrollMax()));
	}

	private float getScrollMax() {
		// return new Float(getPane().getHorizontalScrollBar().getMaximum());
		return new Float(chartPanelScrollPane().getHorizontalScrollBar().getMaximum());
	}

	public float getGraphLength() {
		return new Float(getAxis().getRange().getLength());
	}

	public double getCrosshair() {
		return getPlot().getDomainCrosshairValue();
	}

	public boolean isCrossHairInViewport() {
		return (getCrosshair() >= getViewportLowerBound() && getCrosshair() <= getViewportUpperBound());
	}

	private float getCrossSection() {
		// return new Float(getPane().getWidth() / 2);
		return new Float(chartPanelScrollPane().getWidth() / 2);
	}

	public double getViewportLowerBound() {
		return new Float(getScrollPosRatio() * getGraphLength());
	}

	public double getViewportUpperBound() {
		return new Float(getViewportLowerBound() + (getGraphLength() * getViewportOffsetRatio()));
	}

	private float getScrollPosRatio() {
		return new Float(getScrollPos() / getScrollMax());
	}

	private float getScrollPos() {
		// return new Float(getPane().getHorizontalScrollBar().getValue());
		return new Float(chartPanelScrollPane().getHorizontalScrollBar().getValue());
	}

	private float getViewportOffsetRatio() {
		// return new Float(new Float(getPane().getWidth()) / new
		// Float(getScrollMax()));
		return new Float(new Float(chartPanelScrollPane().getWidth()) / new Float(getScrollMax()));
	}

	private void setCrossHair(double crossHairValue) {
		// set the cross hair values of plot and sub-plots
		Plot mainplot = getAdvancedGraph().getPlot();
		if (mainplot instanceof CombinedDomainXYPlot) {
			combinedPlot = (CombinedDomainXYPlot) mainplot;
			List<?> plots = combinedPlot.getSubplots();
			for (Object p : plots) {
				if (p instanceof XYPlot) {
					XYPlot subPlot = (XYPlot) p;
					subPlot.setDomainCrosshairLockedOnData(false);
					subPlot.setDomainCrosshairValue(crossHairValue);
					subPlot.setDomainCrosshairVisible(true);
				}
			}
			combinedPlot.setDomainCrosshairLockedOnData(false);
			combinedPlot.setDomainCrosshairValue(crossHairValue, true);
			combinedPlot.setDomainCrosshairVisible(true);
		}
		getHandlePanel().setHandlePosition(getHandleCoordinate());

	}

	private int getHandleCoordinate() {
		Rectangle2D plotArea = getChartPanel().getScreenDataArea();
		XYPlot plot = (XYPlot) getAdvancedGraph().getPlot();
		int handleCoordinate = new Float(
				plot.getDomainAxis().valueToJava2D(getCrosshair(), plotArea, plot.getDomainAxisEdge())).intValue();
		return handleCoordinate; // handleCoordinate+100;
	}

	/**
	 * Implements the graph zoom in functionality.
	 */
	private void zoomIn() {
		if (zoomCounter < maxZoom) {
			this.getZoomInButton().setEnabled(false);
			getChartPanel()
					.setPreferredSize(new Dimension((int) (getChartPanel().getBounds().width * this.zoomFactor), 100));// 200
			zoomCounter++;
			zoomEventUIUpdate();

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					getZoomInButton().setEnabled(true);
					chartPanelScrollPane().getViewport().setViewPosition(new Point(getPointX(), 0));
					positionHairLineHandle(getPointX());
				}
			});
		}
	}

	/**
	 * This method implements the graph zoom out functionality.
	 */
	private void zoomOut() {
		if (zoomCounter > 0) {
			this.getZoomOutButton().setEnabled(false);
			getChartPanel()
					.setPreferredSize(new Dimension((int) (getChartPanel().getBounds().width / this.zoomFactor), 100));// 200
			zoomCounter--;
			zoomEventUIUpdate();

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					getZoomOutButton().setEnabled(true);
					chartPanelScrollPane().getViewport().setViewPosition(new Point(getPointX(), 0));
					positionHairLineHandle(getPointX());
				}
			});
		}
	}

	/**
	 * This method implements the reset of Zoom.
	 */
	private void resetGraphZoom() {
		getChartPanel()
		.setPreferredSize(new Dimension((int) (getChartPanel().getBounds().width / Math.pow(this.zoomFactor, zoomCounter)), 100));
		zoomCounter = 0;
		zoomEventUIUpdate();
		chartPanelScrollPane().getViewport().setViewPosition(new Point(getPointX(), 0));
		positionHairLineHandle(getPointX());
	}
	
	/**
	 * Updates the graph UI after zoom in or zoom out.
	 */
	private final int initialPlotAreaWidth = 881;
	private int pointX;

	public int getPointX() {
		return this.pointX;
	}

	private void zoomEventUIUpdate() {
		// allow for better scrolling efficiency for new size
		chartPanelScrollPane().getHorizontalScrollBar().setUnitIncrement(zoomCounter * 10);
		// update the screen panels for repaint
		getChartPanel().updateUI();
		// updates the scroll bar after resize updates.
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		// resetScrollPosition();
		Rectangle2D plotArea = getChartPanel().getScreenDataArea();
		XYPlot plot = (XYPlot) getAdvancedGraph().getPlot();

		int plotWidth = initialPlotAreaWidth;

		for (int i = 1; i <= zoomCounter; i++) {
			plotWidth = (plotWidth * 2) + 16;
		}
		plotArea.setRect(plotArea.getX(), plotArea.getY(), plotWidth, plotArea.getHeight());
		double scrollPoint = new Float(
				plot.getDomainAxis().valueToJava2D(getCrosshair(), plotArea, plot.getDomainAxisEdge())).intValue();

		int width = chartPanelScrollPane().getWidth();
		scrollPoint = Math.max(0, scrollPoint - (width / 2));

		this.pointX = (int) scrollPoint;
	}

	private void positionHairLineHandle(double hairlinePt) {
		if (zoomCounter == 0) {
			getHandlePanel().setHandlePosition(getHandleCoordinate());
		} else {
			int pos;
			int width = chartPanelScrollPane().getWidth();
			if (hairlinePt == 0) {
				if (getZoomInButton().isEnabled()) { // zoomin
					pos = (getHandleCoordinate());
				} else {// zoomout
					pos = ((getHandleCoordinate() * 2) - 5);
				}
			} else {
				pos = (int) hairlinePt + (width / 2);
			}

			getHandlePanel().setHandlePosition(pos);
		}
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent chartmouseevent) {
		Point2D point = chartmouseevent.getTrigger().getPoint();
		Rectangle2D plotArea = getChartPanel().getScreenDataArea();

		XYPlot plot = (XYPlot) getAdvancedGraph().getPlot();
		final double lastChartX = new Double(
				plot.getDomainAxis().java2DToValue(point.getX(), plotArea, plot.getDomainAxisEdge()));

		// setCrossHair(lastChartX);

		// SwingUtilities.invokeLater(new Runnable() {
		// @Override
		// public void run() {
		// setCrossHair(lastChartX);
		// }
		// });

		for (GraphPanelListener gpl : listeners) {
			gpl.graphPanelClicked(lastChartX);

			/* New added @Tinbit */
			ChartEntity entity = chartmouseevent.getEntity();

			if (entity instanceof XYItemEntity) {
				XYItemEntity xyItem = (XYItemEntity) entity;

				XYDataset xyDataset = xyItem.getDataset();
				int seriesIndex = xyItem.getSeriesIndex();
				int itemIndex = xyItem.getItem();

				double xDataValue = xyDataset.getXValue(seriesIndex, itemIndex);
				double yDataValue = xyDataset.getYValue(seriesIndex, itemIndex);

				Map<Integer, VideoEvent> veSegment = vcPlot.getChunk(xDataValue);
				int indexKey = 0;
				if (vcPlot.isDataItemPoint(xDataValue, yDataValue)) {

					if (veSegment != null) {

						for (int key : veSegment.keySet()) {
							// String value="Chunk "+(key+1)+" at
							// "+String.format("%.2f",
							// veSegment.get(key).getDLTimeStamp())+"S";
							chunkInfo.put(key, veSegment.get(key));
							// chunkInfo.add(value);
						}
						indexKey = (int) veSegment.keySet().toArray()[0];
					}

					launchSliderDialog(indexKey);

				} else if (vcPlot.getBufferTimePlot().isDataItemStallPoint(xDataValue, yDataValue) != null) {
					VideoEvent segmentToPlay = vcPlot.getBufferTimePlot().isDataItemStallPoint(xDataValue, yDataValue);
					veSegment = vcPlot.getSegmentToPlayLocation(segmentToPlay);
					if (veSegment != null) {
						for (int key : veSegment.keySet()) {
							chunkInfo.put(key, veSegment.get(key));
						}
						indexKey = (int) veSegment.keySet().toArray()[0];
					}
					launchSliderDialog(indexKey);
				}
			}
		}
	}

	public void launchSliderDialog(int indexKey) {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews("StartupDelayDialog");
		IVideoPlayer player = parent.getVideoPlayer();
		double maxDuration = player.getDuration();
		if (maxDuration != -1) {
			JDialog dialog = new SliderDialogBox(this, maxDuration, chunkInfo, indexKey, vcPlot.getAllChunks());
			dialog.pack();
			dialog.setSize(dialog.getPreferredSize());
			dialog.validate();
			dialog.setModalityType(ModalityType.APPLICATION_MODAL);
			dialog.setVisible(true);
		}
	}

	public DiagnosticsTab getGraphPanelParent() {
		return this.parent;
	}

	public VideoChunksPlot getVcPlot() {
		return this.vcPlot;
	}

	public void addGraphPanelListener(GraphPanelListener listner) {
		listeners.add(listner);
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent arg0) {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (ZOOM_IN_ACTION.equals(e.getActionCommand())) {
			zoomIn();
		} else if (ZOOM_OUT_ACTION.equals(e.getActionCommand())) {
			zoomOut();
		} else if (SAVE_AS_ACTION.equals(e.getActionCommand())) {
			graphHelper.SaveImageAs(getViewport(),
					getTraceData().getAnalyzerResult().getTraceresult().getTraceDirectory());
		}
	}

	public List<PacketInfo> getAllPackets() {
		return allPackets;
	}

	public void setAllPackets(List<PacketInfo> allPackets) {
		this.allPackets = allPackets;
	}

	public double getAllTcpSessions() {
		return allTcpSessions;
	}

	public void setAllTcpSessions(double allTcpSessions) {
		this.allTcpSessions = allTcpSessions;
	}

	public double getTraceDuration() {
		return traceDuration;
	}

	public void setTraceDuration(double traceDuration) {
		this.traceDuration = traceDuration;
	}

}