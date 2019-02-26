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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAnchor;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoStartup;
import com.att.aro.ui.exception.AROUIPanelException;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * 
 * Panel that displays a waterfall view of the analysis data
 *
 *
 */
public class StartupDelayComparisonPanel extends AbstractChartPanel {

	private static final long serialVersionUID = 1L;
	private static final double DEFAULT_TIMELINE = 5;

	private ChartPanel chartPanel;

	private StackedBarRenderer renderer;

	private CategoryAxis categoryAxis;
	private NumberAxis timeAxis;
	private double timeLine;

	VideoStartUpTableModel graphicModel = new VideoStartUpTableModel();
	private JFreeChart chart;
	
	public StartupDelayComparisonPanel(){
		this.setLayout(new BorderLayout());

		JPanel graphPanel = new JPanel(new BorderLayout());
		graphPanel.add(getChartPanel(), BorderLayout.CENTER);
		this.add(graphPanel, BorderLayout.CENTER);

	}

	private ChartPanel getChartPanel() {

		if (chartPanel == null) {

			renderer = new StackedBarRenderer();
			renderer.setMaximumBarWidth(0.15);
			renderer.setShadowVisible(false);
			
			// Set up plot
			CategoryPlot plot = new CategoryPlot(new DefaultCategoryDataset(), getCategoryAxis(), getTimeAxis(), renderer);
			plot.setOrientation(PlotOrientation.HORIZONTAL);
			plot.setDomainGridlinesVisible(true);
			plot.setDomainGridlinePosition(CategoryAnchor.END);

			chart = new JFreeChart("Comparison", plot);
			chart = new JFreeChart(null, null, plot, true);
		
			int width = 100;
			int height = 200;
			int minimumDrawWidth = 200;
			int minimumDrawHeight = 200;
			int maximumDrawWidth = 700;
			int maximumDrawHeight = 400;
			boolean useBuffer = true;
			boolean properties = false;
			boolean save = false;
			boolean print = true;
			boolean zoom = false;
			boolean tooltips = false;
			
			chartPanel = new ChartPanel(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, useBuffer, properties, save, print, zoom, tooltips);

		}
		
		setValueMarker(new Color(0f,1f,0f,.3f ), 2.37, 5f); // green
		
		return chartPanel;
	}

	public ValueMarker setValueMarker(Color color, double position, float width) {
		ValueMarker marker = new ValueMarker(position); // position is the value on the axis
		marker.setLabel("goal");
		marker.setLabelTextAnchor(TextAnchor.CENTER);
		marker.setPaint(color);
		marker.setStroke(new BasicStroke(width));
	
		CategoryPlot plot = chart.getCategoryPlot();
		plot.addRangeMarker(marker);
		
		return marker ;
	}

	private CategoryAxis getCategoryAxis() {
		if (categoryAxis == null) {
			this.categoryAxis = new CategoryAxis();
		}
		return categoryAxis;
	}

	/**
	 * @return the timeAxis
	 */
	private NumberAxis getTimeAxis() {
		if (timeAxis == null) {
			timeAxis = new NumberAxis(ResourceBundleHelper.getMessageString("startup.time")) {
				private static final long serialVersionUID = 1L;

				/**
				 * This override prevents the tick units from changing as the timeline is scrolled to numbers with more digits
				 */
				@Override
				protected double estimateMaximumTickLabelWidth(Graphics2D g2d, TickUnit unit) {

					if (isVerticalTickLabels()) {
						return super.estimateMaximumTickLabelWidth(g2d, unit);
					} else {
						RectangleInsets tickLabelInsets = getTickLabelInsets();
						double result = tickLabelInsets.getLeft() + tickLabelInsets.getRight();

						// look at lower and upper bounds...
						FontMetrics fMetrics = g2d.getFontMetrics(getTickLabelFont());
						double upper = DEFAULT_TIMELINE;
						String upperStr = "";
						NumberFormat formatter = getNumberFormatOverride();
						if (formatter == null) {
							upperStr = unit.valueToString(upper);
						} else {
							upperStr = formatter.format(upper);
						}
						double width2 = fMetrics.stringWidth(upperStr);
						result += width2;
						return result;
					}
				}
			};
			if (timeLine == 0) {
				timeLine = DEFAULT_TIMELINE;
			}
			timeAxis.setRange(new Range(0, timeLine));
		}
		return timeAxis;
	}

	@Override
	public void refresh(AROTraceData aModel) {
		ArrayList<VideoStartup> compApps = new ArrayList<>();
		compApps.add(new VideoStartup("RefApp 1", 0.914, 3.423));
		compApps.add(new VideoStartup("RefApp 2", 3.270, 8.400));
		compApps.add(new VideoStartup("RefApp 3", 2.409, 3.969));
		compApps.add(new VideoStartup("Tested " ,  .6, 7.7));
		
		((VideoStartUpTableModel) graphicModel).setData(compApps);
		loadDataSet();
	}
	
	public void loadDataSet() {
		timeLine = DEFAULT_TIMELINE;
		List<StartupEvent> testData = new ArrayList<>();
		for (VideoStartup videoStartup : graphicModel.getData()) {
			double pt1 = videoStartup.getManifestArrived();
			double pt3 = videoStartup.getPrepareToPlay() - pt1;

			if (timeLine < videoStartup.getPrepareToPlay()) {
				timeLine = (double) Math.round(videoStartup.getPrepareToPlay());
				if (videoStartup.getPrepareToPlay() % 1 <= .5) {
					timeLine += .5;
				}
			}
			
			testData.add(new StartupEvent(videoStartup.getAppName(), pt1, LifeCycleType.LOAD_MANIFEST));
			testData.add(new StartupEvent(videoStartup.getAppName(), pt3, LifeCycleType.PRELOADING));

			timeAxis.setRange(new Range(0, timeLine));
		}

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (StartupEvent startupEvent : testData) {
			dataset.addValue(startupEvent.getTime()
							, ResourceBundleHelper.getMessageString("startup_delay_comparison." + startupEvent.getStatus().toString())
							, startupEvent.getName()
							);
		}

		// Add the dataset to the plot
		CategoryPlot plot = getChartPanel().getChart().getCategoryPlot();
		plot.setDataset(dataset);

	}

	static enum LifeCycleType {
		INIT, LOAD_MANIFEST, REQSEGMENT, PRELOADING, UNKNOWN
	}

	private class StartupEvent {

		private String name;
		private double time;
		private LifeCycleType status;

		public StartupEvent(String name, double time, LifeCycleType status) {
			this.name = name;
			this.time = time;
			this.status = status;
		}

		@Override
		public String toString() {
			StringBuilder strblr = new StringBuilder(83);
			strblr.append("\n\tname :").append(name);
			strblr.append(", time :").append(time);
			strblr.append(", status :").append(status.toString());

			return strblr.toString();
		}
		
		public String getName() {
			return name;
		}

		public double getTime() {
			return time;
		}

		public LifeCycleType getStatus() {
			return status;
		}
	}

	public void setData(List<VideoStartup> data) {
 		setVisible(data != null && !data.isEmpty());
		((VideoStartUpTableModel) graphicModel).setData(data);
		loadDataSet();
	}
	
	@Override
	public void update(Observable observable, Object model){
		if (!(model instanceof AROTraceData)) {
			throw new AROUIPanelException("Bad data model type passed");
		}
	}

}
