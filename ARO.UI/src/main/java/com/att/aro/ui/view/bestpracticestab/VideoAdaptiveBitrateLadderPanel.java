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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.text.NumberFormat;
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
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.SortOrder;

import com.att.aro.core.bestpractice.impl.VideoAdaptiveBitrateLadderImpl;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.QualityTime;
import com.att.aro.ui.exception.AROUIPanelException;
import com.att.aro.ui.utils.ResourceBundleHelper;

/**
 * 
 * Panel that displays a waterfall view of the analysis data
 *
 *
 */
public class VideoAdaptiveBitrateLadderPanel extends AbstractChartPanel {

	private static final long serialVersionUID = 1L;

	private ChartPanel chartPanel;

	private StackedBarRenderer renderer;

	private CategoryAxis categoryAxis;
	private NumberAxis timeAxis;
	private double timeLine = VideoAdaptiveBitrateLadderImpl.PERCENTILE_LINE;

	VideoAdaptiveBitrateLadderTableModel graphicModel = new VideoAdaptiveBitrateLadderTableModel();
	private JFreeChart chart;
	
	public VideoAdaptiveBitrateLadderPanel(){
		this.setLayout(new BorderLayout());

		JPanel graphPanel = new JPanel(new GridBagLayout());
		graphPanel.add(getChartPanel());
		graphPanel.add(getLegendPanel());
		this.add(graphPanel, BorderLayout.CENTER);

	}
	private JPanel getLegendPanel() {
		JPanel legendPanel= new JPanel();
		legendPanel.setPreferredSize(new Dimension(200, 300));
		return legendPanel;
	}
	
	private ChartPanel getChartPanel() {

		if (chartPanel == null) {

			renderer = new StackedBarRenderer();
			renderer.setMaximumBarWidth(10);
			renderer.setShadowVisible(false);

			renderer.setSeriesPaint(6, new Color(127, 63, 63));  // ABC
			renderer.setSeriesPaint(7, new Color(63, 127, 255));  // BCA
			renderer.setSeriesPaint(8, new Color(255,  63, 254));  // CAB
			
			// Set up plot
			CategoryPlot plot = new CategoryPlot(new DefaultCategoryDataset(), getCategoryAxis(), getPercentAxis(), renderer);
			plot.setOrientation(PlotOrientation.VERTICAL);
			plot.setDomainGridlinesVisible(true);
			plot.setDomainGridlinePosition(CategoryAnchor.END);

			chart = new JFreeChart(null, null, plot, true);
			LegendTitle legend = chart.getLegend();
			legend.setPosition(RectangleEdge.RIGHT);
			legend.setSortOrder(SortOrder.DESCENDING);
			Rectangle2D bounds = legend.getBounds();
			bounds.setRect(0, 0, 100, 2800);
			bounds.setFrame(bounds);
			legend.setBounds(bounds);
			
			chartPanel = new ChartPanel(chart);  // 1.0.17
		}
		return chartPanel;
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
	private NumberAxis getPercentAxis() {
		if (timeAxis == null) {
			timeAxis = new NumberAxis(ResourceBundleHelper.getMessageString("startup.percentage")) {
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
						double upper = timeLine;
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
			timeAxis.setRange(new Range(0, timeLine));
		}
		return timeAxis;
	}

	@Override
	public void refresh(AROTraceData aModel) {
	}

	public void loadDataSet() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		timeAxis.setRange(new Range(0, timeLine));
		
		for (QualityTime qualityTime : graphicModel.getData()) {
			dataset.addValue(qualityTime.getPercentage()
							, MessageFormat.format(ResourceBundleHelper.getMessageString("abr_ladder.track")
												  , qualityTime.getTrack()
												  , String.format("%.0f", qualityTime.getPercentage()))
							, qualityTime.getName()
							);
		}
		
		// Add the dataset to the plot
		CategoryPlot plot = getChartPanel().getChart().getCategoryPlot();
		plot.setDataset(dataset);

	}

	public void setData(List<QualityTime> data) {
 		setVisible(data != null && !data.isEmpty());
		graphicModel.setData(data);
		loadDataSet();
	}
	
	@Override
	public void update(Observable observable, Object model){
		if (!(model instanceof AROTraceData)) {
			throw new AROUIPanelException("Bad data model type passed");
		}
	}

}
