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
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.model.diagnostic.GraphPanelHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.diagnostictab.GraphPanelCrossHairHandle;
import com.att.aro.ui.view.diagnostictab.GraphPanelListener;
import com.att.aro.ui.view.videotab.plot.SegmentOptions;
import com.att.aro.ui.view.videotab.plot.VideoBufferPlot;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class SegmentBufferGraphPanel extends JPanel implements ActionListener, ChartMouseListener {
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_TIMELINE = 100;

	private final int UPPER_PANEL_HEIGHT = 280;

	private JViewport portChart;
	private JScrollPane paneChart;
	private JPanel internalScrollableContainer;
	private JPanel scrollChart;
	private JScrollPane pane;
	private JViewport port;
	private JPanel chartPanel;
	private GraphPanelCrossHairHandle handlePanel;

	private VideoBufferPlot videoBufferPlot;
	private double endTime = 0.0;

	private double startTime = 0.0;

	private GraphPanelHelper graphHelper;

	private Set<GraphPanelListener> listeners = new HashSet<GraphPanelListener>();

	private XYPlot plot;
	private JFreeChart advancedGraph;
	private ChartPanel advancedGraphPanel;
	private int zoomCounter = 0;
	private int maxZoom = 5;
	private double zoomFactor = 2;

	private NumberAxis xAxis;
	private NumberAxis yAxis;

	private JLabel axisLabel;

	private AROTraceData traceData;

	private XYPlot xyplot;
	private MainFrame aroView;

	/**
	 * Initializes a new instance of the GraphPanel class.
	 * 
	 * @param videoStream
	 */
	public SegmentBufferGraphPanel(MainFrame aroView) {
		if (StringUtils.isNotBlank(aroView.getTracePath())) {
			this.aroView = aroView;
		}
		if (graphHelper == null) {
			graphHelper = new GraphPanelHelper();
		}

		Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension((int) (screenDimension.getWidth() - 50), 375));
		add(getPane(), BorderLayout.CENTER);
		setBorder(new RoundedBorder(new Insets(20, 20, 20, 20), Color.WHITE));
	}

	public void refresh(AROTraceData aroTraceData, VideoStream videoStream, JCheckBox checkBoxVideo,
			JCheckBox checkBoxAudio, Map<Integer, String> seriesDataSets,
			TreeMap<VideoEvent, Double> chunkPlayTimeList) {
		
		traceData = aroTraceData;
		
		if (aroTraceData != null && aroTraceData.getAnalyzerResult() != null
				&& aroTraceData.getAnalyzerResult().getSessionlist().size() > 0) {

			videoBufferPlot = new VideoBufferPlot(videoStream, getOptionSelected(checkBoxVideo, checkBoxAudio),
					seriesDataSets, chunkPlayTimeList);
			videoBufferPlot.calculateBufferProgress(aroTraceData);

			if (aroTraceData.getAnalyzerResult().getFilter().getTimeRange().getBeginTime() < aroTraceData
					.getAnalyzerResult().getFilter().getTimeRange().getEndTime()) {

				getXAxis().setRange(new Range(videoBufferPlot.getMinXValue() - 5, videoBufferPlot.getMaxXValue() + 10));

				getYAxis().setRange(Math.round(videoBufferPlot.getMinYValue() - 0.5),
						Math.round(videoBufferPlot.getMaxYValue() + 0.5));
			} else {
				getXAxis().setRange(new Range(-0.01, 0));
			}
		}
		videoBufferPlot.populate(getPlot(), aroTraceData);
	}

	private SegmentOptions getOptionSelected(JCheckBox checkBoxVideo, JCheckBox checkBoxAudio) {
		SegmentOptions option = SegmentOptions.DEFAULT;
		if (checkBoxAudio != null && checkBoxVideo != null) {
			if (checkBoxAudio.isSelected() && !checkBoxVideo.isSelected()) {
				return SegmentOptions.AUDIO;
			} else if (!checkBoxAudio.isSelected() && checkBoxVideo.isSelected()) {
				return SegmentOptions.VIDEO;
			}
		}
		return option;
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
			port.setView(getScollableChartLabelPanel());
		}
		return port;
	}

	private JViewport getViewportChartPane() {
		if (portChart == null) {
			portChart = new JViewport();
			portChart.setView(getChartAndHandlePanel());
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
		}
		return advancedGraph;
	}

	private XYPlot getPlot() {
		if (plot == null) {
			buildXAxis();
			buildYAxis();
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setSeriesPaint(0, Color.RED);
			renderer.setSeriesPaint(1, Color.BLUE);
			renderer.setSeriesPaint(2, Color.ORANGE);
			plot = new XYPlot(null, xAxis, yAxis, renderer);
			plot.setOrientation(PlotOrientation.VERTICAL);
		}
		return plot;
	}

	private void buildXAxis() {
		xAxis = new NumberAxis();
		xAxis.setStandardTickUnits(graphHelper.getTickUnits());
		xAxis.setRange(new Range(0, DEFAULT_TIMELINE));
		xAxis.setLowerBound(0);

		xAxis.setAutoTickUnitSelection(true);
		xAxis.setTickMarkInsideLength(1);
		xAxis.setTickMarkOutsideLength(1);

		xAxis.setMinorTickMarksVisible(true);
		xAxis.setMinorTickMarkInsideLength(2f);
		xAxis.setMinorTickMarkOutsideLength(2f);
		xAxis.setTickMarkInsideLength(4f);
		xAxis.setTickMarkOutsideLength(4f);
	}

	private void buildYAxis() {
		yAxis = new NumberAxis();
		yAxis.setStandardTickUnits(graphHelper.getTickUnits());
		yAxis.setRange(new Range(0, DEFAULT_TIMELINE));
		yAxis.setLowerBound(0);

		yAxis.setAutoTickUnitSelection(true);
		yAxis.setTickMarkInsideLength(1);
		yAxis.setTickMarkOutsideLength(1);

		yAxis.setMinorTickMarksVisible(true);
		yAxis.setMinorTickMarkInsideLength(2f);
		yAxis.setMinorTickMarkOutsideLength(2f);
		yAxis.setTickMarkInsideLength(4f);
		yAxis.setTickMarkOutsideLength(4f);
	}

	private float getScrollMax() {
		return new Float(chartPanelScrollPane().getHorizontalScrollBar().getMaximum());
	}

	public float getGraphLength() {
		return new Float(getXAxis().getRange().getLength());
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
		return new Float(chartPanelScrollPane().getHorizontalScrollBar().getValue());
	}

	private float getViewportOffsetRatio() {
		return new Float(new Float(chartPanelScrollPane().getWidth()) / new Float(getScrollMax()));
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent chartmouseevent) {
	}

	public void addGraphPanelListener(GraphPanelListener listner) {
		listeners.add(listner);
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent arg0) {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}
}