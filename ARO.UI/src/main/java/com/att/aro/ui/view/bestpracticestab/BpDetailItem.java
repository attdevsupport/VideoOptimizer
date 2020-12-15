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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.AsyncCheckEntry;
import com.att.aro.core.bestpractice.pojo.AsyncCheckInScriptResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.DisplayNoneInCSSResult;
import com.att.aro.core.bestpractice.pojo.DuplicateContentResult;
import com.att.aro.core.bestpractice.pojo.FileCompressionResult;
import com.att.aro.core.bestpractice.pojo.FileOrderResult;
import com.att.aro.core.bestpractice.pojo.ForwardSecrecyResult;
import com.att.aro.core.bestpractice.pojo.Http3xxCodeResult;
import com.att.aro.core.bestpractice.pojo.Http4xx5xxResult;
import com.att.aro.core.bestpractice.pojo.HttpsUsageResult;
import com.att.aro.core.bestpractice.pojo.ImageComparatorResult;
import com.att.aro.core.bestpractice.pojo.ImageCompressionResult;
import com.att.aro.core.bestpractice.pojo.ImageFormatResult;
import com.att.aro.core.bestpractice.pojo.ImageMdtaResult;
import com.att.aro.core.bestpractice.pojo.ImageSizeResult;
import com.att.aro.core.bestpractice.pojo.MinificationResult;
import com.att.aro.core.bestpractice.pojo.MultiSimultnsConnectionResult;
import com.att.aro.core.bestpractice.pojo.SimultnsConnectionResult;
import com.att.aro.core.bestpractice.pojo.SpriteImageResult;
import com.att.aro.core.bestpractice.pojo.TransmissionPrivateDataResult;
import com.att.aro.core.bestpractice.pojo.UnnecessaryConnectionResult;
import com.att.aro.core.bestpractice.pojo.UnsecureSSLVersionResult;
import com.att.aro.core.bestpractice.pojo.VideoAdaptiveBitrateLadderResult;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSessionResult;
import com.att.aro.core.bestpractice.pojo.VideoNetworkComparisonResult;
import com.att.aro.core.bestpractice.pojo.VideoStallResult;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.VideoStartup;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.BrowserGenerator;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.IAROExpandable;
import com.att.aro.ui.commonui.UIComponent;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.menu.file.BPSelectionPanel;
import com.att.aro.ui.view.menu.file.PreferencesDialog;
import com.att.aro.ui.view.menu.tools.PrivateDataDialog;

public class BpDetailItem extends AbstractBpPanel implements IAROExpandable {
	private final class HyperlinkAdapter extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			routeHyperlink();
		}
	}

	private static final long serialVersionUID = 1L;
	JLabel imageLabel = null;
	JLabel nameLabel = null;
	// JTextPane nameTextLabel = null;
	JLabel nameTextLabel = null;
	JButton buttonPrivateData = null;
	JLabel aboutLabel = null;
	JTextPane aboutTextLabel = null;
	JLabel resultsLabel = null;
	JTextPane resultsTextLabel = null;
	
	private AbstractChartPanel resultsChartPanel;
	private AbstractBpDetailTablePanel resultsTablePanel;
	private AbstractImageBpDetailTablePanel imgMdataResultsTablePanel;
	private AbstractBpImageCompressionTablePanel imageCompressionResultsTablePanel;
	private AbstractBpImageFormatTablePanel imageFormatResultsTablePanel;
	private AbstractBpImageComparatorTablePanel imageComparisonResultsTablePanel;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private MainFrame aroView;
	Insets imageInsets = new Insets(25, 10, 10, 10);
	Insets startInsets = new Insets(25, 5, 2, 5);
	Insets insets = new Insets(2, 5, 2, 5);
	JPanel dataPanel;
	private BestPracticeType bpType;
	static final Font TEXT_FONT = new Font("TextFont", Font.PLAIN, 12);
	private static final int TEXT_WIDTH = 600;
	private static final int FILE_PREFERENCE_VIDEO_INDEX = 2;
	private static final int VIDEO_TAB_INDEX = 3;
	private HyperlinkAdapter hyperlinkAdapter = new HyperlinkAdapter();
	private static final Logger LOG = LogManager.getLogger(BpDetailItem.class.getName());

	/**
	 * This will show a table
	 * 
	 * @param name
	 * @param bpType
	 * @param resultsTablePanel
	 */
	public BpDetailItem(String name, BestPracticeType bpType, AbstractBpDetailTablePanel resultsTablePanel) {
		super();
		this.bpType = bpType;
		this.resultsTablePanel = resultsTablePanel;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType bpType, AbstractBpDetailTablePanel resultsTablePanel, MainFrame aroView) {
		super();
		this.aroView = aroView;
		this.bpType = bpType;
		this.resultsTablePanel = resultsTablePanel;
		buttonPrivateData = new JButton();
		setDefaultLayout(name);
	}
	
	public BpDetailItem(String name, BestPracticeType bpType, AbstractBpDetailTablePanel resultsTablePanel, AbstractChartPanel resultsChart, MainFrame aroView) {
		super();
		this.aroView = aroView;
		this.bpType = bpType;
		this.resultsTablePanel = resultsTablePanel;
		this.resultsChartPanel = resultsChart;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType bpType, AbstractChartPanel resultsChart, MainFrame aroView) {
		super();
		this.aroView = aroView;
		this.bpType = bpType;
		this.resultsTablePanel = null;
		this.resultsChartPanel = resultsChart;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType bpType, MainFrame aroView) {
		super();
		this.aroView = aroView;
		this.bpType = bpType;
		this.resultsTablePanel = null;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType bpType) {
		super();
		this.bpType = bpType;
		this.resultsTablePanel = null;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType imageMdata, BpFileImageMDataTablePanel bpFileImageMDataTablePanel) {
		super();
		this.bpType = imageMdata;
		this.imgMdataResultsTablePanel = bpFileImageMDataTablePanel;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType imageMdata, BpFileImageCompressionTablePanel imageCompressionResultsTablePanel) {
		super();
		this.bpType = imageMdata;
		this.imageCompressionResultsTablePanel = imageCompressionResultsTablePanel;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType imageFormat, BpFileImageFormatTablePanel imageFormatResultsTablePanel) {
		super();
		this.bpType = imageFormat;
		this.imageFormatResultsTablePanel = imageFormatResultsTablePanel;
		setDefaultLayout(name);
	}

	public BpDetailItem(String name, BestPracticeType imageFormat, BpFileImageComparisionTablePanel imageComparisionTablePanel) {
		super();
		this.bpType = imageFormat;
		this.imageComparisonResultsTablePanel = imageComparisionTablePanel;
		setDefaultLayout(name);
	}

	public void setDefaultLayout(String name) {
		imageLabel = new JLabel(loadImageIcon(null));
		nameLabel = new JLabel();
		aboutLabel = new JLabel();
		resultsLabel = new JLabel();
		nameTextLabel = new JLabel();
		add(layoutPanel(name), BorderLayout.CENTER);
	}

	public void addTablePanelRoute(IARODiagnosticsOverviewRoute DiagnosticsOverviewRoute) {
		this.diagnosticsOverviewRoute = DiagnosticsOverviewRoute;
		if (resultsTablePanel != null) {
			resultsTablePanel.addTablePanelRoute(DiagnosticsOverviewRoute);
			;
		} else if (imgMdataResultsTablePanel != null) {
			imgMdataResultsTablePanel.addTablePanelRoute(DiagnosticsOverviewRoute);
			;
		} else if (imageCompressionResultsTablePanel != null) {
			imageCompressionResultsTablePanel.addTablePanelRoute(DiagnosticsOverviewRoute);
			;
		} else if (imageFormatResultsTablePanel != null) {
			imageFormatResultsTablePanel.addTablePanelRoute(DiagnosticsOverviewRoute);
			;
		} else if (imageComparisonResultsTablePanel != null) {
			imageComparisonResultsTablePanel.addTablePanelRoute(DiagnosticsOverviewRoute);
			;
		}
	}

	public void setwidth(JTextPane textPanel, int width) {
		textPanel.setPreferredSize(null);
		textPanel.setSize(width, 9999);
		Dimension dimTextPanel = textPanel.getPreferredSize();
		dimTextPanel.width = width;
		textPanel.setPreferredSize(dimTextPanel);
		textPanel.setMinimumSize(dimTextPanel);
	}
	
	private void openVideoPreferencesDialog() {
		boolean isRelatedToStartupDelay = this.bpType == BestPracticeType.VIDEO_STALL
				|| this.bpType == BestPracticeType.STARTUP_DELAY || this.bpType == BestPracticeType.BUFFER_OCCUPANCY;
		if (isRelatedToStartupDelay && this.aroView != null) {
			PreferencesDialog prefDialog = new PreferencesDialog(this.aroView, resultsTextLabel);
			prefDialog.getTabbedPane().setSelectedIndex(FILE_PREFERENCE_VIDEO_INDEX);
			prefDialog.setVisible(true);
		}
	}

	private void openVideoTab() {
		if (diagnosticsOverviewRoute != null && diagnosticsOverviewRoute.getJtabbedPane() != null) {
			JTabbedPane voTabbedPane = diagnosticsOverviewRoute.getJtabbedPane();
			voTabbedPane.setSelectedIndex(VIDEO_TAB_INDEX);
		}
	}
	public JPanel layoutPanel(String name) {
		JScrollPane scroll;
		if (dataPanel == null) {
			dataPanel = new JPanel(new GridBagLayout());
			dataPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			Insets insets = new Insets(2, 2, 2, 2);
			int idx = 0;
			//
			nameLabel.setText(ResourceBundleHelper.getMessageString("bestPractices.test"));
			nameTextLabel.setText(ResourceBundleHelper.getMessageString(name + ".detailedTitle"));
			aboutLabel.setText(ResourceBundleHelper.getMessageString("bestPractices.About"));
			aboutTextLabel = createJTextArea(ResourceBundleHelper.getMessageString(name + ".desc"), getLearnMoreURI());
			resultsLabel.setText(ResourceBundleHelper.getMessageString("bestPractices.results"));
			resultsTextLabel = createJTextPane("");
			setwidth(aboutTextLabel, TEXT_WIDTH);
			resultsTextLabel.setPreferredSize(null);
			resultsTextLabel.setSize(TEXT_WIDTH, 9999);
			Dimension labelDim = resultsTextLabel.getPreferredSize();
			labelDim.width = TEXT_WIDTH;
			labelDim.height = 60;
			resultsTextLabel.setPreferredSize(labelDim);
			resultsTextLabel.setMinimumSize(labelDim);
			resultsTextLabel.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						if (e.getDescription().equalsIgnoreCase("preferences")) {
							openVideoPreferencesDialog();
						} else if (e.getDescription().equalsIgnoreCase("selectStream")) {
							openVideoTab();
						} else {
							routeHyperlink();
						}

					}
				}
			});
			
			// Icon
			dataPanel.add(imageLabel, new GridBagConstraints(0, idx, 1, 4, 0.0, 0.0, GridBagConstraints.NORTH,
					GridBagConstraints.NONE, imageInsets, 0, 0));
			
			// Text:
			dataPanel.add(nameLabel, new GridBagConstraints(1, idx, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST,
					GridBagConstraints.NONE, startInsets, 0, 0));
			
			// about: detailedTitle - desc
			nameLabel.setFont(new Font("TimesRoman", Font.PLAIN, 16));
			nameTextLabel.setFont(new Font("TimesRoman", Font.PLAIN, 16));
			scroll = new JScrollPane(nameTextLabel);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			removeMouseWheelListeners(scroll);
			dataPanel.add(scroll, new GridBagConstraints(2, idx, 1, 1, 1.0, 1.0, GridBagConstraints.SOUTHWEST,
					GridBagConstraints.NONE, insets, 0, 0));
			dataPanel.add(aboutLabel, new GridBagConstraints(1, ++idx, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE, insets, 0, 0));
			scroll = new JScrollPane(aboutTextLabel);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			removeMouseWheelListeners(scroll);
			dataPanel.add(scroll, new GridBagConstraints(2, idx, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL, insets, 0, 0));
			
			// Results
			dataPanel.add(resultsLabel, new GridBagConstraints(1, ++idx, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE, insets, 0, 0));
			scroll = new JScrollPane(resultsTextLabel);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			removeMouseWheelListeners(scroll);
			dataPanel.add(scroll, new GridBagConstraints(2, idx, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.BOTH, insets, 0, 0));
			
			// PrivateData Button
			if (name.equalsIgnoreCase("security.transmissionPrivateData")) {
				buttonPrivateData.setText("Add Private Data Tracking");
				buttonPrivateData.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						openPrivateDataDialog();
					}
				});
				buttonPrivateData.setEnabled(false);
				for (BestPracticeType bptype : BPSelectionPanel.getInstance().getCheckedBP()) {
					if (bptype.name().equalsIgnoreCase("TRANSMISSION_PRIVATE_DATA")) {
						buttonPrivateData.setEnabled(true);
					}
				}
				scroll = new JScrollPane(buttonPrivateData);
				scroll.setBorder(BorderFactory.createEmptyBorder());
				removeMouseWheelListeners(scroll);
				dataPanel.add(scroll, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
						GridBagConstraints.NORTHWEST, insets, 0, 0));
			}
			
			// Table
			if (resultsTablePanel != null) {
				dataPanel.add(resultsTablePanel, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
			} else if (imgMdataResultsTablePanel != null) {
				dataPanel.add(imgMdataResultsTablePanel, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
			} else if (imageCompressionResultsTablePanel != null) {
				dataPanel.add(imageCompressionResultsTablePanel, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
			} else if (imageFormatResultsTablePanel != null) {
				dataPanel.add(imageFormatResultsTablePanel, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
			} else if (imageComparisonResultsTablePanel != null) {
				dataPanel.add(imageComparisonResultsTablePanel, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
			}
			
			// Chart
			if (resultsChartPanel != null) {
				dataPanel.add(resultsChartPanel, new GridBagConstraints(2, ++idx, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0));
			}

		}
		return dataPanel;
	}

	private void openPrivateDataDialog() {
		PrivateDataDialog privateDataDialog = aroView.getPrivateDataDialog();
		if (privateDataDialog == null) {
			privateDataDialog = new PrivateDataDialog(aroView);
		}
		privateDataDialog.setVisible(true);
		privateDataDialog.setAlwaysOnTop(true);
	}

	void routeHyperlink() {
		diagnosticsOverviewRoute.routeHyperlink(this.bpType);
	}

	/**
	 * This method creates the about text area.
	 */
	private JTextPane createJTextArea(String textToDisplay, final URI url) {
		HTMLDocument doc = new HTMLDocument();
		StyleSheet style = doc.getStyleSheet();
		style.addRule(
				"body { font-family: " + TEXT_FONT.getFamily() + "; " + "font-size: " + TEXT_FONT.getSize() + "pt; }");
		style.addRule("a { text-decoration: underline; font-weight:bold; }");
		JTextPane jTextArea = new JTextPane(doc);
		jTextArea.setEditable(false);
		jTextArea.setEditorKit(new HTMLEditorKit());
		jTextArea.setStyledDocument(doc);
		jTextArea.setMargin(new Insets(0, 0, 0, 0));
		if (url != null) {
			jTextArea.setText(textToDisplay + "&nbsp;" + " <a href=\"#\">"
					+ ResourceBundleHelper.getMessageString("bestPractices.learnMore") + "</a>");
			jTextArea.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						try {
							BrowserGenerator.openBrowser(url);
						} catch (IOException e1) {
							LOG.error(e1.getMessage());
						}
					}
				}
			});
		} else {
			jTextArea.setText(textToDisplay);
		}
		// Calculate preferred size
		jTextArea.setSize(TEXT_WIDTH, 9999);
		Dimension d = jTextArea.getPreferredSize();
		d.width = TEXT_WIDTH;
		jTextArea.setPreferredSize(d);
		jTextArea.setMinimumSize(d);
		return jTextArea;
	}

	private URI getLearnMoreURI() {
		String learnMoreURI = "";
		switch (bpType) {
		case FILE_COMPRESSION:
			learnMoreURI = ResourceBundleHelper.getURLResource("textFileCompression.url");
			break;
		case DUPLICATE_CONTENT:
			learnMoreURI = ResourceBundleHelper.getURLResource("caching.duplicateContent.url");
			break;
		case USING_CACHE:
			learnMoreURI = ResourceBundleHelper.getURLResource("caching.usingCache.url");
			break;
		case CACHE_CONTROL:
			learnMoreURI = ResourceBundleHelper.getURLResource("caching.cacheControl.url");
			break;
		case COMBINE_CS_JSS:
			learnMoreURI = ResourceBundleHelper.getURLResource("combinejscss.url");
			break;
		case IMAGE_SIZE:
			learnMoreURI = ResourceBundleHelper.getURLResource("imageSize.url");
			break;
		case IMAGE_MDATA:
			learnMoreURI = ResourceBundleHelper.getURLResource("imageMetadata.url");
			break;
		case IMAGE_CMPRS:
			learnMoreURI = ResourceBundleHelper.getURLResource("imageCompression.url");
			break;
		case IMAGE_FORMAT:
			learnMoreURI = ResourceBundleHelper.getURLResource("imageFormat.url");
			break;
		case IMAGE_COMPARE:
			learnMoreURI = ResourceBundleHelper.getURLResource("uiComparator.url");
			break;
		case MINIFICATION:
			learnMoreURI = ResourceBundleHelper.getURLResource("minification.url");
			break;
		case SPRITEIMAGE:
			learnMoreURI = ResourceBundleHelper.getURLResource("spriteimages.url");
			break;
		case UNNECESSARY_CONNECTIONS:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.unnecssaryConn.url");
			break;
		case SCRIPTS_URL:
			learnMoreURI = ResourceBundleHelper.getURLResource("3rd.party.scripts.url");
			break;
		case SCREEN_ROTATION:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.screenRotation.url");
			break;
		case PERIODIC_TRANSFER:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.periodic.url");
			break;
		case HTTP_4XX_5XX:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.http4xx5xx.url");
			break;
		case HTTP_3XX_CODE:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.http3xx.url");
			break;
		case HTTP_1_0_USAGE:
			learnMoreURI = ResourceBundleHelper.getURLResource("html.httpUsage.url");
			break;
		case FILE_ORDER:
			learnMoreURI = ResourceBundleHelper.getURLResource("html.fileorder.url");
			break;
		case EMPTY_URL:
			learnMoreURI = ResourceBundleHelper.getURLResource("emptyUrl.url");
			break;
		case DISPLAY_NONE_IN_CSS:
			learnMoreURI = ResourceBundleHelper.getURLResource("html.displaynoneincss.url");
			break;
		case CONNECTION_OPENING:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.connectionOpening.url");
			break;
		case CONNECTION_CLOSING:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.connClosing.url");
			break;
		case ASYNC_CHECK:
			learnMoreURI = ResourceBundleHelper.getURLResource("html.asyncload.url");
			break;
		case ACCESSING_PERIPHERALS:
			learnMoreURI = ResourceBundleHelper.getURLResource("other.accessingPeripherals.url");
			break;
		case HTTPS_USAGE:
			learnMoreURI = ResourceBundleHelper.getURLResource("security.httpsUsage.url");
			break;
		case TRANSMISSION_PRIVATE_DATA:
			learnMoreURI = ResourceBundleHelper.getURLResource("security.transmissionPrivateData.url");
			break;
		case UNSECURE_SSL_VERSION:
			learnMoreURI = ResourceBundleHelper.getURLResource("security.unsecureSSLVersion.url");
			break;
		case FORWARD_SECRECY:
			learnMoreURI = ResourceBundleHelper.getURLResource("security.forwardSecrecy.url");
			break;
		case VIDEO_STALL:
			learnMoreURI = ResourceBundleHelper.getURLResource("videoStall.url");
			break;
		case STARTUP_DELAY:
			learnMoreURI = ResourceBundleHelper.getURLResource("startUpDelay.url");
			break;
		case VIDEO_ABR_LADDER:
			learnMoreURI = ResourceBundleHelper.getURLResource("adaptiveBitrateLadder.url");
			break;
		case BUFFER_OCCUPANCY:
			learnMoreURI = ResourceBundleHelper.getURLResource("bufferOccupancy.url");
			break;
		case NETWORK_COMPARISON:
			learnMoreURI = ResourceBundleHelper.getURLResource("networkComparison.url");
			break;
		case TCP_CONNECTION:
			learnMoreURI = ResourceBundleHelper.getURLResource("tcpConnection.url");
			break;
		case CHUNK_SIZE:
			learnMoreURI = ResourceBundleHelper.getURLResource("segmentSize.url");
			break;
		case CHUNK_PACING:
			learnMoreURI = ResourceBundleHelper.getURLResource("segmentPacing.url");
			break;
		case VIDEO_REDUNDANCY:
			learnMoreURI = ResourceBundleHelper.getURLResource("videoRedundancy.url");
			break;
		case SIMUL_CONN:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.simultaneous.url");
			break;
		case MULTI_SIMULCONN:
			learnMoreURI = ResourceBundleHelper.getURLResource("connections.multiSimultaneous.url");
			break;
		case VIDEO_CONCURRENT_SESSION:
			learnMoreURI = ResourceBundleHelper.getURLResource("videoConcurrentSession.url");
			break;
		case VIDEO_VARIABLE_BITRATE:
			learnMoreURI = ResourceBundleHelper.getURLResource("videoVariableBitrate.url");
			break;
		case VIDEO_RESOLUTION_QUALITY:
			learnMoreURI = ResourceBundleHelper.getURLResource("videoResolutionQuality.url");
			break;
		case AUDIO_STREAM:
			learnMoreURI = ResourceBundleHelper.getURLResource("audioStream.url");
			break;
		default:
			break;
		}
		return URI.create(learnMoreURI);
	}

	/**
	 * Wrap supplied HTML decorated text in a JTextPane
	 *
	 * @param text
	 * @return
	 */
	private JTextPane createJTextPane(String text) {
		HTMLDocument doc = new HTMLDocument();
		StyleSheet style = doc.getStyleSheet();
		style.addRule(
				"body { font-family: " + TEXT_FONT.getFamily() + "; " + "font-size: " + TEXT_FONT.getSize() + "pt; }");
		style.addRule("a { text-decoration: underline }");
		JTextPane jTextArea = new JTextPane(doc);
		jTextArea.setEditable(false);
		jTextArea.setEditorKit(new HTMLEditorKit());
		jTextArea.setStyledDocument(doc);
		jTextArea.setMargin(new Insets(0, 0, 0, 0));
		if (text != null) {
			jTextArea.setText(text);
			jTextArea.setSize(TEXT_WIDTH, 9999);
			Dimension dim = jTextArea.getPreferredSize();
			dim.width = TEXT_WIDTH;
			jTextArea.setPreferredSize(dim);
			jTextArea.setMinimumSize(dim);
		} else {
			jTextArea.setPreferredSize(new Dimension(500, 50));
		}
		return jTextArea;
	}

	/**
	 * locate and load an ImageIcon based on AbstractBestPracticeResult
	 *
	 * @param bestPracticeResult
	 * @return ImageIcon corresponding to PASS, FAIL, WARNING, SELF_TEST
	 */
	private ImageIcon loadImageIcon(AbstractBestPracticeResult bestPracticeResult) {
		String imageName = "Image.naGray";
		if (bestPracticeResult != null) {
			BPResultType resType = bestPracticeResult.getResultType(); // PASS,
																		// FAIL
																		// or
																		// WARNING
			if (resType.equals(BPResultType.PASS)) {
				imageName = "Image.bpPassDark";
			} else if (resType.equals(BPResultType.FAIL)) {
				imageName = "Image.bpFailDark";
			} else if (resType.equals(BPResultType.WARNING)) {
				imageName = "Image.bpWarningDark";
			} else if (resType.equals(BPResultType.SELF_TEST)) {
				imageName = "Image.bpManual";
			} else if (resType.equals(BPResultType.CONFIG_REQUIRED)) {
				imageName = "Image.bpConfig";
			} else if (resType.equals(BPResultType.NO_DATA)) {
				imageName = "Image.bpNoData";
			}
		}
		return UIComponent.getInstance().getIconByKey(imageName);
	}

	private static void removeMouseWheelListeners(JScrollPane scrollPane) {
		for (MouseWheelListener mwl : scrollPane.getMouseWheelListeners()) {
			scrollPane.removeMouseWheelListener(mwl);
		}
	}

	@Override
	public void refresh(AROTraceData model) {
		List<AbstractBestPracticeResult> bpResults = model.getBestPracticeResults();
		for (AbstractBestPracticeResult bpr : bpResults) {
			if (bpr.getBestPracticeType().equals(this.bpType)) {
				resultsTextLabel.setText(bpr.getResultText());
				imageLabel.setIcon(loadImageIcon(bpr));
				
				if (bpType == BestPracticeType.VIDEO_STALL 
						|| bpType == BestPracticeType.STARTUP_DELAY 
						|| bpType == BestPracticeType.BUFFER_OCCUPANCY) {
					if (bpr.getResultType().equals(BPResultType.CONFIG_REQUIRED)) {
						addConfigIconActions();
					} else {
						imageLabel.setToolTipText("");
						if (imageLabel.getMouseListeners() != null && imageLabel.getMouseListeners().length > 1) {
							imageLabel.setCursor(Cursor.getDefaultCursor());
							imageLabel.removeMouseListener(hyperlinkAdapter);
						}
					}
				}
				
				BestPracticeType resultType = bpr.getBestPracticeType();
				switch (resultType) {
				case FILE_COMPRESSION:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpFileCompressionTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileCompressionTablePanel) resultsTablePanel)
								.setData(((FileCompressionResult) bpr).getResults());
					return;
				case DUPLICATE_CONTENT:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpFileDuplicateContentTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileDuplicateContentTablePanel) resultsTablePanel)
								.setData(((DuplicateContentResult) bpr).getDuplicateContentList());
					return;
				case IMAGE_SIZE:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA)
						((BpFileImageSizeTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileImageSizeTablePanel) resultsTablePanel).setData(((ImageSizeResult) bpr).getResults());
					return;
				case IMAGE_MDATA:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA)
						((BpFileImageMDataTablePanel) imgMdataResultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileImageMDataTablePanel) imgMdataResultsTablePanel)
								.setData(((ImageMdtaResult) bpr).getResults());
					return;
				case IMAGE_CMPRS:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA)
						((BpFileImageCompressionTablePanel) imageCompressionResultsTablePanel)
								.setData(Collections.emptyList());
					else
						((BpFileImageCompressionTablePanel) imageCompressionResultsTablePanel)
								.setData(((ImageCompressionResult) bpr).getResults());
					return;
				case IMAGE_FORMAT:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA)
						((BpFileImageFormatTablePanel) imageFormatResultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileImageFormatTablePanel) imageFormatResultsTablePanel)
								.setData(((ImageFormatResult) bpr).getResults());
					return;
				case IMAGE_COMPARE:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA) {
						((BpFileImageComparisionTablePanel) imageComparisonResultsTablePanel)
								.setData(Collections.emptyList());
					} else {
						((BpFileImageComparisionTablePanel) imageComparisonResultsTablePanel)
								.setData(((ImageComparatorResult) bpr).getResults());
					}
					return;
				case MINIFICATION:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpFileMinificationTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileMinificationTablePanel) resultsTablePanel)
								.setData(((MinificationResult) bpr).getMinificationEntryList());
					return;
				case SPRITEIMAGE:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpFileSpriteImagesTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpFileSpriteImagesTablePanel) resultsTablePanel)
								.setData(((SpriteImageResult) bpr).getAnalysisResults());
					return;
				case HTTP_4XX_5XX:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpConnectionsHttp4xx5xxTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpConnectionsHttp4xx5xxTablePanel) resultsTablePanel)
								.setData(((Http4xx5xxResult) bpr).getHttpResCodelist());
					return;
				case HTTP_3XX_CODE:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpConnectionsHttp3xxTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpConnectionsHttp3xxTablePanel) resultsTablePanel)
								.setData(((Http3xxCodeResult) bpr).getHttp3xxResCode());
					return;
				case ASYNC_CHECK:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpHtmlAsyncLoadTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else {
						List<AsyncCheckEntry> res = ((AsyncCheckInScriptResult) bpr).getResults();
						((BpHtmlAsyncLoadTablePanel) resultsTablePanel).setData(res);
					}
					return;
				case FILE_ORDER:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpHtmlFileOrderTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpHtmlFileOrderTablePanel) resultsTablePanel).setData(((FileOrderResult) bpr).getResults());
					return;
				case DISPLAY_NONE_IN_CSS:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpHtmlDisplayNoneInCSSTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpHtmlDisplayNoneInCSSTablePanel) resultsTablePanel)
								.setData(((DisplayNoneInCSSResult) bpr).getResults());
					return;
				case UNNECESSARY_CONNECTIONS:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpConnectionsUnnecessaryTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpConnectionsUnnecessaryTablePanel) resultsTablePanel)
								.setData(((UnnecessaryConnectionResult) bpr).getTightlyCoupledBurstsDetails());
					return;
				case HTTPS_USAGE:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpSecurityHttpsUsageTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpSecurityHttpsUsageTablePanel) resultsTablePanel)
								.setData(((HttpsUsageResult) bpr).getResults());
					return;
				case TRANSMISSION_PRIVATE_DATA:
					if (bpr.getResultType() == BPResultType.NONE) {
						((BpSecurityTransmissionPrivateDataTablePanel) resultsTablePanel)
								.setData(Collections.emptyList());
						buttonPrivateData.setEnabled(false);
					} else {
						((BpSecurityTransmissionPrivateDataTablePanel) resultsTablePanel)
								.setData(((TransmissionPrivateDataResult) bpr).getResults());
						buttonPrivateData.setEnabled(true);
					}
					return;
				case UNSECURE_SSL_VERSION:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpSecurityUnsecureSSLVersionTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpSecurityUnsecureSSLVersionTablePanel) resultsTablePanel)
								.setData(((UnsecureSSLVersionResult) bpr).getResults());
					return;
				case FORWARD_SECRECY:
					if (bpr.getResultType() == BPResultType.NONE)
						((BpSecurityForwardSecrecyTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BpSecurityForwardSecrecyTablePanel) resultsTablePanel)
								.setData(((ForwardSecrecyResult) bpr).getResults());
					return;
				case SIMUL_CONN:
					if (bpr.getResultType() == BPResultType.NONE)
						((BPConnectionsSimultnsTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BPConnectionsSimultnsTablePanel) resultsTablePanel)
								.setData(((SimultnsConnectionResult) bpr).getResults());
					return;
				case MULTI_SIMULCONN:
					if (bpr.getResultType() == BPResultType.NONE)
						((BPConnectionsSimultnsTablePanel) resultsTablePanel).setData(Collections.emptyList());
					else
						((BPConnectionsSimultnsTablePanel) resultsTablePanel)
								.setData(((MultiSimultnsConnectionResult) bpr).getResults());
					return;
				case VIDEO_STALL:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA) {
						((BPVideoStallTablePanel) resultsTablePanel).setData(Collections.emptyList());
					} else {
						((BPVideoStallTablePanel) resultsTablePanel).setData(((VideoStallResult) bpr).getResults());
					}
					return;

				case STARTUP_DELAY:
					StartupDelayComparisonPanel startupDelayPanel = (StartupDelayComparisonPanel) resultsChartPanel;
					List<VideoStartup> results = Collections.emptyList();
					if (bpr.getResultType() != BPResultType.NONE) {
						results = ((VideoStartUpDelayResult) bpr).getResults();
					}
					startupDelayPanel.setData(results);
					return;

				case VIDEO_ABR_LADDER:				 
					VideoAdaptiveBitrateLadderPanel adaptiveBitrateLadderPanel = (VideoAdaptiveBitrateLadderPanel) resultsChartPanel;					
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA) {
						adaptiveBitrateLadderPanel.setData(Collections.emptyList());
						((BPAdaptiveBitrateTablePanel) resultsTablePanel).setData(Collections.emptyList());
					} else {
						adaptiveBitrateLadderPanel.setData(((VideoAdaptiveBitrateLadderResult) bpr).getResults());
						((BPAdaptiveBitrateTablePanel) resultsTablePanel).setData(((VideoAdaptiveBitrateLadderResult) bpr).getResults());

					}					
					return;

				case VIDEO_CONCURRENT_SESSION:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA) {
						((BPVideoConcurrentSessionTablePanel) resultsTablePanel).setData(Collections.emptyList());
					} else {
						((BPVideoConcurrentSessionTablePanel) resultsTablePanel)
								.setData(((VideoConcurrentSessionResult) bpr).getResults());
					}
					return;
					
				case NETWORK_COMPARISON:
					if (bpr.getResultType() == BPResultType.NONE || bpr.getResultType() == BPResultType.NO_DATA) {
						((BPNetworkComparisonTablePanel) resultsTablePanel).setData(Collections.emptyList());
					} else {
						((BPNetworkComparisonTablePanel) resultsTablePanel)
								.setData(((VideoNetworkComparisonResult) bpr).getResults());
					}
					return;

				default:
					return;
				}
			}
		}
	}

	private void addConfigIconActions() {
		imageLabel.setToolTipText(ResourceBundleHelper.getMessageString("startUpDelay.config"));
		if (imageLabel.getMouseListeners() != null && imageLabel.getMouseListeners().length < 2) {
			imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			imageLabel.addMouseListener(hyperlinkAdapter);
		}
	}

	@Override
	public JPanel layoutDataPanel() {
		return null;
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		return 0;
	}

	/**
	 * forward the expand() to the resultsTablePanel
	 */
	@Override
	public void expand() {
		if (resultsTablePanel != null) {
			resultsTablePanel.expand();
		} else if (imgMdataResultsTablePanel != null) {
			imgMdataResultsTablePanel.expand();
		} else if (imageCompressionResultsTablePanel != null) {
			imageCompressionResultsTablePanel.expand();
		} else if (imageFormatResultsTablePanel != null) {
			imageFormatResultsTablePanel.expand();
		} else if (imageComparisonResultsTablePanel != null) {
			imageComparisonResultsTablePanel.expand();
		}
	}

	public JLabel getNameTextLabel() {
		return nameTextLabel;
	}
}
