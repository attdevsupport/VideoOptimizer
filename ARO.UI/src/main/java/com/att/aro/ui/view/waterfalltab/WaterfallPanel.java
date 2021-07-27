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
package com.att.aro.ui.view.waterfalltab;

import static com.att.aro.ui.utils.ResourceBundleHelper.getMessageString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections.CollectionUtils;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAnchor;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPosition;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.CategoryLabelWidthType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.SlidingCategoryDataset;
import org.jfree.text.TextBlockAnchor;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.SimultnsConnectionResult;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.RequestResponseTimeline;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.model.waterfall.WaterfallCategory;
import com.att.aro.view.images.Images;

/**
 * Panel that displays a waterfall view of the analysis data
 *
 */
public class WaterfallPanel extends TabPanelJPanel {
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_TIMELINE = 100;
	private static final int CATEGORY_MAX_COUNT = 25;
	private static final double ZOOM_FACTOR = 2;
	private double startGraphRange;

	private Color noneColor = new Color(0, 0, 0, 0);
	private Color dnsLoolupColor = new Color(0, 128, 128);
	private Color initiaConnColor = new Color(255, 140, 0);
	private Color sslNegColor = new Color(199, 21, 133);
	private Color requestTimeColor = new Color(255, 255, 0);
	private Color firstByteTimeColor = new Color(0, 255, 0);
	private Color contentDownloadColor = new Color(70, 130, 180);
	private Color inactiveConnectionColor = Color.GRAY;
	private Color threexColor = Color.BLUE;
	private Color fourxColor = Color.RED;

	private SlidingCategoryDataset dataset;
	private JButton zoomInButton;
	private JButton zoomOutButton;
	private ChartPanel chartPanel;
	private JScrollBar verticalScroll;
	private JScrollBar horizontalScroll;
	private NumberAxis timeAxis;
	private CategoryAxis categoryAxis;
	private double traceDuration = DEFAULT_TIMELINE;

	private WaterfallPopup popup;
	private StackedBarRenderer renderer;

	private static final NumberFormat format = new DecimalFormat();
	private static final TickUnits units = new TickUnits();
	private List<WaterfallCategory> categoryList;
	private WaterfallTab waterfallTab;
	private double time;
	private AROTraceData data;

	static {
		units.add(new NumberTickUnit(500000, format, 5));
		units.add(new NumberTickUnit(250000, format, 5));
		units.add(new NumberTickUnit(100000, format, 10));
		units.add(new NumberTickUnit(50000, format, 5));
		units.add(new NumberTickUnit(25000, format, 5));
		units.add(new NumberTickUnit(10000, format, 10));
		units.add(new NumberTickUnit(5000, format, 5));
		units.add(new NumberTickUnit(2500, format, 5));
		units.add(new NumberTickUnit(1000, format, 5));
		units.add(new NumberTickUnit(500, format, 5));
		units.add(new NumberTickUnit(250, format, 5));
		units.add(new NumberTickUnit(100, format, 10));
		units.add(new NumberTickUnit(50, format, 10));
		units.add(new NumberTickUnit(25, format, 5));
		units.add(new NumberTickUnit(10, format, 10));
		units.add(new NumberTickUnit(5, format, 5));
		units.add(new NumberTickUnit(2, format, 4));
		units.add(new NumberTickUnit(1, format, 10));
		units.add(new NumberTickUnit(.5, format, 5));
		units.add(new NumberTickUnit(.25, format, 5));
		units.add(new NumberTickUnit(.1, format, 10));
		units.add(new NumberTickUnit(.05, format, 5));
		units.add(new NumberTickUnit(.01, format, 10));
	}

	public WaterfallPanel(WaterfallTab waterfallTab){
		super();
		this.waterfallTab = waterfallTab;
	}
	
	public JPanel layoutDataPanel() {
		this.setLayout(new BorderLayout());
		this.dataset = new SlidingCategoryDataset(new DefaultCategoryDataset(), 0, CATEGORY_MAX_COUNT);
		this.popup = new WaterfallPopup();

		JPanel graphPanel = new JPanel(new BorderLayout());
		graphPanel.add(getChartPanel(), BorderLayout.CENTER);
		graphPanel.add(getVerticalScroll(), BorderLayout.EAST);
		graphPanel.add(getHorizontalScroll(), BorderLayout.SOUTH);
		this.add(graphPanel, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(getZoomInButton());
		buttonsPanel.add(getZoomOutButton());
		this.add(buttonsPanel, BorderLayout.SOUTH);

		return this;
	}

	private ChartPanel getChartPanel() {
		if (chartPanel == null) {
			renderer = new StackedBarRenderer();
			renderer.setMaximumBarWidth(0.05);
			renderer.setShadowVisible(false);
			renderer.setBaseItemLabelsVisible(true);
			renderer.setBasePositiveItemLabelPosition(
					new ItemLabelPosition(ItemLabelAnchor.INSIDE9, TextAnchor.CENTER_LEFT));
			renderer.setPositiveItemLabelPositionFallback(
					new ItemLabelPosition(ItemLabelAnchor.INSIDE9, TextAnchor.CENTER_LEFT));

			// Set up plot
			DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
			CategoryPlot plot = new CategoryPlot(categoryDataset, getCategoryAxis(), getTimeAxis(), renderer);
			plot.setOrientation(PlotOrientation.HORIZONTAL);
			plot.setDomainGridlinesVisible(true);
			plot.setDomainGridlinePosition(CategoryAnchor.END);

			JFreeChart chart = new JFreeChart(plot);
			chartPanel = new ChartPanel(chart, 400, 200, 200, 200, 2000, 5000, true, false, false, false, false, true);
			chartPanel.setMouseZoomable(false);
			chartPanel.setRangeZoomable(false);
			chartPanel.setDomainZoomable(false);
			chartPanel.addChartMouseListener(new WaterfallChartMouseListener());
		}

		return chartPanel;
	}
	
	/**
	 * @return the categoryAxis
	 */
	private CategoryAxis getCategoryAxis() {
		if (categoryAxis == null) {
			categoryAxis = new CategoryAxis();
			categoryAxis.setMaximumCategoryLabelWidthRatio(0.2f);
			categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.replaceLeftPosition(CategoryLabelPositions.STANDARD, new CategoryLabelPosition(
                RectangleAnchor.LEFT, TextBlockAnchor.CENTER_LEFT, 
                CategoryLabelWidthType.RANGE, 1.0f
            )));
		}
		return categoryAxis;
	}
	
	/**
	 * @return the timeAxis
	 */
	private NumberAxis getTimeAxis() {
		if (timeAxis == null) {
			timeAxis = new NumberAxis(getMessageString("waterfall.time")) {
				private static final long serialVersionUID = 1L;

				/**
				 * This override prevents the tick units from changing
				 * as the timeline is scrolled to numbers with more digits
				 */
				@Override
				protected double estimateMaximumTickLabelWidth(Graphics2D g2d,
						TickUnit unit) {

					if (isVerticalTickLabels()) {
						return super.estimateMaximumTickLabelWidth(g2d, unit);
					} else {
						RectangleInsets tickLabelInsets = getTickLabelInsets();
						double result = tickLabelInsets.getLeft()
								+ tickLabelInsets.getRight();

						// look at lower and upper bounds...
						FontMetrics fMetrics = g2d.getFontMetrics(getTickLabelFont());
						double upper = traceDuration;
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
			timeAxis.setRange(new Range(0, DEFAULT_TIMELINE));
			timeAxis.setStandardTickUnits(units);
		}
		return timeAxis;
	}
	
	/**
	 * @return the verticalScroll
	 */
	private JScrollBar getVerticalScroll() {
		if (verticalScroll == null) {
			verticalScroll = new JScrollBar(JScrollBar.VERTICAL, 0, 100, 0, 100);
			verticalScroll.getModel().addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					if (dataset.getColumnCount() > 0) {
						dataset.setFirstCategoryIndex(verticalScroll.getValue());
					}
				}
			});
		}
		return verticalScroll;
	}
	
	/**
	 * @return the horizontalScroll
	 */
	private JScrollBar getHorizontalScroll() {
		if (horizontalScroll == null) {
			horizontalScroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, DEFAULT_TIMELINE, 0, DEFAULT_TIMELINE);
			horizontalScroll.getModel().addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					int scrollStart = horizontalScroll.getValue();
					int scrollEnd = scrollStart + horizontalScroll.getVisibleAmount();
					timeAxis.setRange(scrollStart, scrollEnd);
				}
			});
		}
		return horizontalScroll;
	}
	
	/**
	 * Implements the graph zoom out functionality.
	 */
	private JButton getZoomOutButton() {
		if (zoomOutButton == null) {
			ImageIcon zoomOutButtonIcon = Images.DEMAGNIFY.getIcon();
			zoomOutButton = new JButton(zoomOutButtonIcon);
			zoomOutButton.setEnabled(false);
			zoomOutButton.setPreferredSize(new Dimension(60, 30));
			zoomOutButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					zoomOut();

				}
			});
			zoomOutButton.setToolTipText(getMessageString("chart.tooltip.zoomout"));
		}
		return zoomOutButton;
	}

	/**
	 * Button for zoom in 
	 * @return
	 */
	private JButton getZoomInButton() {
		if (zoomInButton == null) {
			ImageIcon zoomInButtonIcon = Images.MAGNIFY.getIcon();
			zoomInButton = new JButton(zoomInButtonIcon);
			zoomInButton.setEnabled(true);
			zoomInButton.setPreferredSize(new Dimension(60, 30));
			zoomInButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					zoomIn();
				}
			});
			zoomInButton.setToolTipText(getMessageString("chart.tooltip.zoomin"));
		}
		return zoomInButton;
	}

	/**
	 * This method implements the graph zoom in functionality.
	 */
	private void zoomIn() {
		Range range = timeAxis.getRange();
		double lowl = range.getLowerBound();
		double high = lowl + (range.getUpperBound() - lowl) / ZOOM_FACTOR;
		setTimeRange(lowl, high);
	}

	/**
	 * This method implements the graph zoom out functionality.
	 */
	private void zoomOut() {
		Range r = timeAxis.getRange();
		double low = r.getLowerBound();
		double high = low + (r.getUpperBound() - low) * ZOOM_FACTOR;
		setTimeRange(low, high);
	}

	/**
	 * Setting the time range for the graph.
	 * @param low
	 * @param high
	 */
	public void setTimeRange(double low, double high) {
		double lTime = low;
		startGraphRange = low;
		double hTime = high;
		boolean zoomInEnabled = true;
		boolean zoomOutEnabled = true;
		JScrollBar scrollBarr = getHorizontalScroll();
		if (hTime > traceDuration) {
			double delta = hTime - traceDuration;
			lTime = lTime - delta;
			hTime = hTime - delta;
			if (lTime < 0) {
				lTime = 0.0;
			}
		}
		
		if (hTime - lTime <= 1.0) {
			hTime = lTime + 1.0;
			zoomInEnabled = false;
		}

		if((hTime - lTime) < traceDuration){
			zoomOutEnabled = true;
		} else {
			zoomOutEnabled = false;
		}
		
		scrollBarr.setValue((int) lTime);
		scrollBarr.setVisibleAmount((int) Math.ceil(hTime - lTime));
		scrollBarr.setBlockIncrement(scrollBarr.getVisibleAmount());

		// Enable zoom buttons appropriately
		zoomOutButton.setEnabled(zoomOutEnabled);
		zoomInButton.setEnabled(zoomInEnabled);
	}
	
	/**
	 * Refreshes the waterfall display with the specified analysis data
	 * @param Analyzed data from aro core.
	 */
	public void refresh(AROTraceData aModel){
		this.data = aModel;
		this.popup.refresh(null, 0);
		this.popup.setVisible(false);
				
		// Create sorted list of request/response pairs
		categoryList = new ArrayList<WaterfallCategory>();
		
		if(aModel != null && aModel.getAnalyzerResult() != null){
			this.traceDuration = aModel.getAnalyzerResult().getTraceresult().getTraceDuration();
			
			// add 20% to make sure labels close to the right edge of the screen are visible
			this.traceDuration *= 1.2; 
			
			for (Session tcpSession : aModel.getAnalyzerResult().getSessionlist()) {
				Session thisSession = tcpSession;
				if(!tcpSession.isUdpOnly()){
					for (HttpRequestResponseInfo reqResInfo : tcpSession.getRequestResponseInfo()) {
						if(reqResInfo.getDirection() == HttpDirection.REQUEST && reqResInfo.getWaterfallInfos() != null){
							categoryList.add(new WaterfallCategory(reqResInfo,thisSession));							
						} 						
					}
				}
			}
			
			// Sort and set index
			Collections.sort(categoryList);
			int index = 0;
			for (WaterfallCategory wCategory : categoryList) {
				wCategory.setIndex(++index);
			}
		} 
		
		// Horizontal scroll bar used to scroll through trace duration
		JScrollBar hScrollBar = getHorizontalScroll();
		hScrollBar.setMaximum((int) Math.ceil(this.traceDuration));
		
		
		CategoryAxis cAxis = getCategoryAxis();
		cAxis.clearCategoryLabelToolTips();
		DefaultCategoryDataset underlying = new DefaultCategoryDataset();
		for (WaterfallCategory wfc : categoryList) {
			RequestResponseTimeline rrTimeLine = wfc.getReqResp().getWaterfallInfos();

			underlying.addValue(rrTimeLine.getStartTime(), Waterfall.BEFORE, wfc);
			underlying.addValue(rrTimeLine.getDnsLookupDuration(), Waterfall.DNS_LOOKUP, wfc);
			underlying.addValue(rrTimeLine.getInitialConnDuration(), Waterfall.INITIAL_CONNECTION, wfc);
			underlying.addValue(rrTimeLine.getSslNegotiationDuration(), Waterfall.SSL_NEGOTIATION, wfc);
			underlying.addValue(rrTimeLine.getRequestDuration(), Waterfall.REQUEST_TIME, wfc);
			underlying.addValue(rrTimeLine.getTimeToFirstByte(), Waterfall.TIME_TO_FIRST_BYTE, wfc);
			underlying.addValue(rrTimeLine.getContentDownloadDuration(), Waterfall.CONTENT_DOWNLOAD, wfc);
			underlying.addValue(null, Waterfall.HTTP_3XX_REDIRECTION, wfc);
			underlying.addValue(null, Waterfall.HTTP_4XX_CLIENTERROR, wfc);
			int code = wfc.getReqResp().getAssocReqResp().getStatusCode();
			double endTime = this.traceDuration - rrTimeLine.getStartTime() - rrTimeLine.getTotalTime();
			if(code >= 300 && code < 400) {
				underlying.addValue(endTime, Waterfall.AFTER_3XX, wfc);
			} else if(code >= 400) {
				underlying.addValue(endTime, Waterfall.AFTER_4XX, wfc);
			} else {
				underlying.addValue(endTime, Waterfall.AFTER, wfc);
			}
			
			cAxis.addCategoryLabelToolTip(wfc, wfc.getTooltip());
		}

		// Vertical scroll bar is used to scroll through data
		JScrollBar vScrollBar = getVerticalScroll();
		int count = underlying.getColumnCount();
		vScrollBar.setValue(0);
		vScrollBar.setMaximum(count);
		vScrollBar.setVisibleAmount(count > 0 ? this.dataset.getMaximumCategoryCount() - 1 / count : 1);
		
		// Add the dataset to the plot
		CategoryPlot plot = getChartPanel().getChart().getCategoryPlot();
		this.dataset = new SlidingCategoryDataset(underlying, 0, CATEGORY_MAX_COUNT);
		plot.setDataset(this.dataset);

		// Set the visible time range
		setTimeRange(startGraphRange, startGraphRange+100);
		// Place proper colors on renderer for waterfall states
		final CategoryItemRenderer renderer = plot.getRenderer();
		for (Object obj : underlying.getRowKeys()) {
			Waterfall wFall = (Waterfall) obj;
			int index = underlying.getRowIndex(wFall);

			Color paint;
			switch (wFall) {
			case DNS_LOOKUP:
				paint = dnsLoolupColor;
				break;
			case INITIAL_CONNECTION:
				paint = initiaConnColor;
				break;
			case SSL_NEGOTIATION:
				paint = sslNegColor;
				break;
			case REQUEST_TIME:
				paint = requestTimeColor;
				break;
			case TIME_TO_FIRST_BYTE:
				paint = firstByteTimeColor;
				break;
			case CONTENT_DOWNLOAD:
				paint = contentDownloadColor;
				break;
			case INACTIVE:
				paint = inactiveConnectionColor;
				break;
			case AFTER_3XX:
				paint = noneColor;
				renderer.setSeriesItemLabelPaint(index, threexColor);
				renderer.setSeriesVisibleInLegend(index, false);
				break;
			case AFTER_4XX:
				paint = noneColor;
				renderer.setSeriesItemLabelPaint(index, fourxColor);
				renderer.setSeriesVisibleInLegend(index, false);
				break;
			case HTTP_3XX_REDIRECTION:
				paint = threexColor;
				break;
			case HTTP_4XX_CLIENTERROR:
				paint = fourxColor;
				break;
			default:
				renderer.setSeriesItemLabelPaint(index, Color.black);
				renderer.setSeriesVisibleInLegend(index, false);
				paint = noneColor;
			}
			renderer.setSeriesPaint(index, paint);
		}
		// Adding the label at the end of bars
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator() {
			private static final long serialVersionUID = 1L;

			@Override
			public String generateLabel(CategoryDataset dataset, int row, int column) {
				if (Waterfall.AFTER == dataset.getRowKey(row)
						|| Waterfall.AFTER_3XX == dataset.getRowKey(row)
						|| Waterfall.AFTER_4XX == dataset.getRowKey(row)) {
					WaterfallCategory waterfallItem = (WaterfallCategory) dataset.getColumnKey(column);
					RequestResponseTimeline waterfallInfos = waterfallItem.getReqResp().getWaterfallInfos();
					DecimalFormat formatter = new DecimalFormat("#.##");
					int code = waterfallItem.getReqResp().getAssocReqResp().getStatusCode();
					return MessageFormat.format(getMessageString("waterfall.totalTime"),
							formatter.format(waterfallInfos.getTotalTime()),
							code > 0 ? waterfallItem.getReqResp().getScheme() + " " + code
									: getMessageString("waterfall.unknownCode"));
				}
				return null;
			}
		});
		new Thread(() -> setCrosshairs(aModel)).start();
	}
	
	private void setCrosshairs(AROTraceData data) {
		double time = getCrosshairTime(data);
		CategoryPlot plot = getChartPanel().getChart().getCategoryPlot();
		plot.setRangeCrosshairLockedOnData(true);
		plot.setRangeCrosshairValue(time);
		plot.setRangeCrosshairVisible(true);
	}

	private double getCrosshairTime(AROTraceData data) {
		int iterations = 0;
		while (iterations++ < 10 && (data == null || isEmpty(data.getBestPracticeResults()))) {
			try {
				Thread.sleep(250);// Gives time for best practices to gather required data
			} catch (InterruptedException e) {
				// Do nothing
			}
		}
		double time = 0.0;
		if (data != null && CollectionUtils.isNotEmpty(data.getBestPracticeResults())) {
			Optional<AbstractBestPracticeResult> optionalResult = data.getBestPracticeResults().stream()
					.filter((result) -> result.getBestPracticeType() == BestPracticeType.MULTI_SIMULCONN).findFirst();
			if (optionalResult.isPresent()) {
				time = ((SimultnsConnectionResult) optionalResult.get()).getResults().stream()
						.max((e1, e2) -> e2.getConcurrentSessions() - e1.getConcurrentSessions()).get()
						.getStartTimeStamp();
			}
		}
		return time;
	}

	class WaterfallCategoryTooltipRenderer extends StandardCategoryToolTipGenerator {
		private static final double EPS = 0.00001;
		private static final long serialVersionUID = 1L;

		@Override
		public String generateToolTip(CategoryDataset dataset, int row, int column) {
			if(time < -EPS) {
				String toolTipText = super.generateToolTip(dataset, row, column);
				String trimmedTooltip = toolTipText.indexOf(",") < 0 || toolTipText.indexOf("- ") < 0 ? toolTipText
						: toolTipText.substring(toolTipText.indexOf(",") + 1, toolTipText.indexOf("- "));
				return trimmedTooltip;
			}
			int maxSimultConnectionCount = 0;
			boolean hasSessions = data != null && data.getAnalyzerResult() != null
					&& CollectionUtils.isNotEmpty(data.getAnalyzerResult().getSessionlist());
			if (categoryList != null && hasSessions) {
				for (Session session : data.getAnalyzerResult().getSessionlist()) {
					if (session != null && session.getTcpPackets().size() > 0) {
						if (time >= session.getSessionStartTime() && time <= session.getSessionEndTime()) {
							maxSimultConnectionCount++;
						}
					}
				}
			}
			String tooltipMessage = getMessageString("waterfall.crossHair.tooltip");
			return MessageFormat.format(tooltipMessage, String.format("%.2f", time), maxSimultConnectionCount);
		}
	}
	
	class WaterfallChartMouseListener implements ChartMouseListener {
		private volatile int count = 0;

		@Override
		public void chartMouseMoved(ChartMouseEvent evt) {
			CategoryPlot plot = getChartPanel().getChart().getCategoryPlot();
			Point2D p = chartPanel.translateScreenToJava2D(evt.getTrigger().getPoint());
			Rectangle2D plotArea = chartPanel.getScreenDataArea();
			boolean isMouseOnChart = p.getX() > plotArea.getX();
			if(isMouseOnChart) {
				time = plot.getRangeAxis().java2DToValue(p.getX(), plotArea, plot.getRangeAxisEdge());
			} else {
				time = -1;
			}
			CategoryItemRenderer renderer = plot.getRenderer();
			renderer.setBaseToolTipGenerator(new WaterfallCategoryTooltipRenderer());
		}

		@Override
		public void chartMouseClicked(ChartMouseEvent event) {
			if (event.getEntity() instanceof CategoryItemEntity) {
				CategoryItemEntity xyitem = (CategoryItemEntity) event.getEntity();
				WaterfallCategory category = (WaterfallCategory) xyitem.getColumnKey();
				if (category != null && category.getReqResp() != null) {
					count = event.getTrigger().getClickCount();
					if(count > 1) {
						return;
					}
					new Thread(() -> processClick(category)).start();
				}
			}
		}

		private void processClick(WaterfallCategory category) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// Do nothing
			}
			if (count > 1) {
				waterfallTab.updateMainFrame(category.getSession());
			} else {
				popup.refresh(category.getReqResp(), category.getIndex());
				if (!popup.getPopupDialog().isVisible()) {
					popup.getPopupDialog().setVisible(true);
				}
			}
		}
	}

	public String formatTooltip(String toolTip) {
		String formattedString = "";
		if (toolTip.indexOf(". ") > 0
				&& toolTip.indexOf(" - ") > 0) {
			formattedString = toolTip.substring(toolTip.indexOf(". ")+1, toolTip.indexOf(" - "));
		} else {
			formattedString = toolTip;
		}
		return formattedString;
	}
}