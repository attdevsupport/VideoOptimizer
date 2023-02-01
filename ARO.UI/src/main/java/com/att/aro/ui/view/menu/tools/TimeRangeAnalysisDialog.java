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


package com.att.aro.ui.view.menu.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.DefaultEditorKit;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.helper.StringUtil;

import com.att.aro.core.configuration.pojo.ProfileType;
import com.att.aro.core.packetanalysis.impl.PacketAnalyzerImpl;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.ApplicationSelection;
import com.att.aro.core.packetanalysis.impl.TimeRangeAnalysis;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.mvc.IAROView;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;

/**
 * Represents the Time Range Analysis Dialog that allows the user to set a time
 * range that delineates the section of the trace data to be analyzed.
 */
public class TimeRangeAnalysisDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LogManager.getLogger(TimeRangeAnalysisDialog.class);

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
	private static final double ROUNDING_VALUE = 0.01;
	private static final ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();

	private JPanel timeRangeSelectionPanel;
	private JPanel buttonPanel;
	private JPanel timeRangeResultsPanel;
	private JPanel jDialogPanel;
	private JButton resetButton;
	private JButton reanalyzeButton;
	private JButton calculateStatisticsButton;
	private JButton cancelButton;
	private JTextArea timeRangeAnalysisResultsTextArea;
	private JTextField startTimeTextField;
	private JTextField endTimeTextField;

	private Double traceEndTime;
	private double timeRangeStartTime;
	private double timeRangeEndTime;

	private double initTimeRangeStartTime;
	private double initTimeRangeEndTime;
	private double endTimeResetValue;

	private boolean ipv4Selection;
 	private boolean ipv6Selection;
 	private boolean tcpSelection;
 	private boolean udpSelection;
 	private boolean dnsSelection;

 	private JPanel checkBoxSelPanel;
 	private final JCheckBox chkTCP = new JCheckBox("TCP");
    private final JCheckBox chkUdp = new JCheckBox("UDP");
    private final JCheckBox chkDns = new JCheckBox("DNS");
    private final JCheckBox chkIpv4 = new JCheckBox("IPv4");
    private final JCheckBox chkIpv6 = new JCheckBox("IPv6");

 	private PacketAnalyzerResult currentTraceResult;
 	private AnalysisFilter initialFilter;

	private JPopupMenu timeRangeContextMenu;

	private IAROView parent;

	/**
	 * Initializes a new instance of the TimeRangeAnalysisDialog class using the
	 * specified instance of the ApplicationResourceOptimizer as the owner.
	 * 
	 * @param owner
	 *            The ApplicationResourceOptimizer instance.
	 * 
	 * @param parent
	 *            The menu's parent instance (MainFrame).
	 */
	public TimeRangeAnalysisDialog(Window owner, IAROView parent) {
		super(owner);

		PacketAnalyzerResult traceresult = ((MainFrame)parent).getController().getTheModel().getAnalyzerResult();
		if (traceresult==null){
			LOGGER.error("Trace result error!");
			MessageDialogFactory.getInstance().showErrorDialog(this, "wrong..");
		}else{
			currentTraceResult = traceresult;
			initialFilter = cloneFilter(traceresult.getFilter());
			endTimeResetValue = traceresult.getTraceresult().getTraceDuration();
			traceEndTime = endTimeResetValue;	
			TimeRange timeRange = traceresult.getFilter().getTimeRange();
			if(timeRange != null){
				timeRangeStartTime = timeRange.getBeginTime();
				timeRangeEndTime = timeRange.getEndTime();
			} else {
				timeRangeStartTime = 0.0;
				timeRangeEndTime = traceEndTime;
			}
		}

		// For cancel button
		initTimeRangeStartTime = timeRangeStartTime;
		initTimeRangeEndTime = timeRangeEndTime;

		this.parent = parent;
		initialize();
	}

	private AnalysisFilter cloneFilter(AnalysisFilter filter) {

		Collection<ApplicationSelection> appSel = filter.getApplicationSelections();
		HashMap<String, ApplicationSelection> applications = new HashMap<String, ApplicationSelection>(appSel.size());
	
		for (ApplicationSelection aSel: appSel) {
			ApplicationSelection clonedAP = new ApplicationSelection(aSel);
			applications.put(clonedAP.getAppName(), clonedAP);		
		}

		TimeRange clonedTimeRange = null;
		if (filter.getTimeRange() != null) {
			TimeRange original = filter.getTimeRange();
			clonedTimeRange = new TimeRange(original.getTitle(), original.getTimeRangeType(), original.getBeginTime(), original.getEndTime());
		}

		
		AnalysisFilter clonedFilter = new AnalysisFilter(applications, clonedTimeRange, filter.getDomainNames() == null ? null : new HashMap<>(filter.getDomainNames()));	
		clonedFilter.setIpv4Sel(filter.isIpv4Sel());
		clonedFilter.setIpv6Sel(filter.isIpv6Sel());
		clonedFilter.setTcpSel(filter.isTcpSel());
		clonedFilter.setUdpSel(filter.isUdpSel());
		clonedFilter.setDnsSelection(filter.isDnsSelection());
		return clonedFilter;
	}

	/**
	 * Initializes the dialog.
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(500, 500);
		this.setResizable(false);
		this.setModal(true);
		this.setTitle(resourceBundle.getString("timerangeanalysis.title"));
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getJDialogPanel());
		this.addWindowListener(getWindowListener());
	}

	/**
	 * Initializes jDialogPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJDialogPanel() {
		if (jDialogPanel == null) {
			jDialogPanel = new JPanel();
			jDialogPanel.setLayout(new BorderLayout());

			jDialogPanel.add(getTimeRangeSelectionPanel(), BorderLayout.NORTH);
			
			
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(getPacketSelectionPanel(), BorderLayout.NORTH);
			panel.add(getTimeRangeDialogButtons(), BorderLayout.CENTER);
			panel.add(getTimeRangeResultsPanel(), BorderLayout.SOUTH);
			jDialogPanel.add(panel, BorderLayout.CENTER);
		}

		return jDialogPanel;
	}

	private JPanel getPacketSelectionPanel(){
		if (checkBoxSelPanel == null) {
			checkBoxSelPanel = new JPanel();
		    chkIpv4.setSelected(ipv4Selection = currentTraceResult.getFilter().isIpv4Sel());
			chkIpv6.setSelected(ipv6Selection = currentTraceResult.getFilter().isIpv6Sel());
			chkTCP.setSelected(tcpSelection = currentTraceResult.getFilter().isTcpSel());
			chkUdp.setSelected(udpSelection = currentTraceResult.getFilter().isUdpSel());
			chkDns.setSelected(dnsSelection = currentTraceResult.getFilter().isDnsSelection());
					
			chkIpv4.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					JCheckBox cb = (JCheckBox) aEvent.getSource();
					ipv4Selection = cb.isSelected();				
				}
			});
	
			chkIpv6.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					JCheckBox cb = (JCheckBox) aEvent.getSource();
					ipv6Selection = cb.isSelected();
				}
			});
			
			chkTCP.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					JCheckBox cb = (JCheckBox) aEvent.getSource();
					tcpSelection = cb.isSelected();
				}
			});
			
			chkUdp.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					JCheckBox cb = (JCheckBox) aEvent.getSource();
					udpSelection = cb.isSelected();
				}
			});
			
			chkDns.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent aEvent) {
					JCheckBox cb = (JCheckBox)aEvent.getSource();
					dnsSelection = cb.isSelected();				
				}
			});
			
		    checkBoxSelPanel.add(chkIpv4);
		    checkBoxSelPanel.add(chkIpv6);
		    checkBoxSelPanel.add(getSeparator(checkBoxSelPanel.getPreferredSize()));
		    checkBoxSelPanel.add(chkTCP);
		    checkBoxSelPanel.add(chkUdp);
		    checkBoxSelPanel.add(chkDns);
		    checkBoxSelPanel.setSize(50, 20);
		}

		return checkBoxSelPanel;
	}

	private JSeparator getSeparator(Dimension dimension) {
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		separator.setMaximumSize(dimension);
		return separator;
	}

	private boolean updateCurrentfilter() {
		if (ipv4Selection || ipv6Selection) {
			if (tcpSelection || udpSelection || dnsSelection) {
				if (currentTraceResult.getFilter() != null) {
					AnalysisFilter filter = currentTraceResult.getFilter();
					Map<InetAddress, String> domainNames = filter.getDomainNames();
					Map<String, ApplicationSelection> appSelections = new HashMap<>(filter.getAppSelections().size());

					for (ApplicationSelection sel : filter.getAppSelections().values()) {
						if (domainNames != null) { 
							sel.setDomainNames(domainNames);
						}
						appSelections.put(sel.getAppName(), new ApplicationSelection(sel));
					}

					filter.setIpv4Sel(ipv4Selection);
					filter.setIpv6Sel(ipv6Selection);
					filter.setTcpSel(tcpSelection);
					filter.setUdpSel(udpSelection);
					filter.setDnsSelection(dnsSelection);
					return true;
				}
			} else {
				MessageDialogFactory.getInstance().showErrorDialog(this,
						resourceBundle.getString("filter.noProtocolSelection.error"));
			}
		} else {
			MessageDialogFactory.getInstance().showErrorDialog(this,
					resourceBundle.getString("filter.noIpSelection.error"));
		}

		return false;
	}

	/**
	 * Initializes and returns the timeRangeSelectionPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getTimeRangeSelectionPanel() {
		if (timeRangeSelectionPanel == null) {
			timeRangeSelectionPanel = new JPanel();
			JLabel startTimeLabel = new JLabel(resourceBundle.getString("timerangeanalysis.start"));
			JLabel endTimeLabel = new JLabel(resourceBundle.getString("timerangeanalysis.end"));

			timeRangeSelectionPanel.add(startTimeLabel);
			timeRangeSelectionPanel.add(getStartTimeTextField());
			timeRangeSelectionPanel.add(endTimeLabel);
			timeRangeSelectionPanel.add(getEndTimeTextField());
		}

		return timeRangeSelectionPanel;
	}

	private JPanel getTimeRangeDialogButtons() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.add(getResetButton());
			buttonPanel.add(getReanalyzeButton());
			buttonPanel.add(getCalculateStatisticsButton());
			buttonPanel.add(getCancelButton());
		}

		return buttonPanel;
	}

	/**
	 * Initializes and returns timeRangeResultsPanel
	 */
	private JPanel getTimeRangeResultsPanel() {
		if (timeRangeResultsPanel == null) {
			timeRangeResultsPanel = new JPanel();
			timeRangeResultsPanel.setLayout(new BorderLayout());
			timeRangeResultsPanel.setPreferredSize(new Dimension(500, 350));
			JLabel resultsLabel = new JLabel(resourceBundle.getString("timerangeanalysis.results"));
			if (timeRangeAnalysisResultsTextArea == null) {
				timeRangeAnalysisResultsTextArea = new JTextArea();
				
				timeRangeContextMenu = new JPopupMenu();
				timeRangeAnalysisResultsTextArea.setComponentPopupMenu(timeRangeContextMenu);
				
				JMenuItem menuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
				menuItem.setText("Copy");
				timeRangeContextMenu.add(menuItem);
				
				menuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
				menuItem.setText("Paste");
				timeRangeContextMenu.add(menuItem);		
			}
			timeRangeAnalysisResultsTextArea.setEditable(false);
			timeRangeAnalysisResultsTextArea.setFocusable(true);
			timeRangeAnalysisResultsTextArea.setLineWrap(true);
			timeRangeAnalysisResultsTextArea.setWrapStyleWord(true);
			Border padding = BorderFactory.createBevelBorder(BevelBorder.RAISED);
			timeRangeResultsPanel.setBorder(padding);
			timeRangeResultsPanel.add(resultsLabel, BorderLayout.NORTH);
			timeRangeResultsPanel.add(timeRangeAnalysisResultsTextArea, BorderLayout.CENTER);
		}

		return timeRangeResultsPanel;
	}

	
	/*
	 * Get Reset button
	 */
	private JButton getResetButton() {
		if (resetButton == null) {
			resetButton = new JButton();
			resetButton.setText(resourceBundle.getString("Button.reset"));

			resetButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					reset(true);
				}
			});
		}

		return resetButton;
	}

	/*
	 * Get Reanalyze button
	 */
	private JButton getReanalyzeButton() {
		if (reanalyzeButton == null) {
			reanalyzeButton = new JButton();
			reanalyzeButton.setText(resourceBundle.getString("menu.tools.timeRangeAnalysis.reanalyze.button"));
			reanalyzeButton.setToolTipText(resourceBundle.getString("menu.tools.timeRangeAnalysis.reanalyze.button.tooltip"));
			reanalyzeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					double startTime;
					double endTime;
					try {
						startTime = getTimeValue(startTimeTextField);
						endTime = getTimeValue(endTimeTextField);
					} catch (NumberFormatException e) {
						MessageDialogFactory.getInstance().showErrorDialog(TimeRangeAnalysisDialog.this,
								resourceBundle.getString("timerangeanalysis.numberError"));
						return;
					}

					double timeRangeEndTime = Double.valueOf(doubleToFixedDecimal(traceEndTime, 3));
					if (startTime < endTime) {
						if (startTime >= 0.0 && startTime <= endTime && endTime <= timeRangeEndTime) {
							if (!updateCurrentfilter()) {
								return;
							}

							AnalysisFilter filter = currentTraceResult.getFilter();
							filter.setTimeRange(new TimeRange(startTime, endTime));

							((MainFrame) parent).updateFilter(filter);
							dispose();
						} else {
							String strErrorMessage = MessageFormat.format(resourceBundle.getString("timerangeanalysis.rangeError"), 0.00, doubleToFixedDecimal(traceEndTime, 3));
							MessageDialogFactory.showMessageDialog(
									TimeRangeAnalysisDialog.this,
									strErrorMessage,
									resourceBundle.getString("menu.error.title"),
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						String strErrorMessage = resourceBundle.getString("timerangeanalysis.startTimeError");
						MessageDialogFactory.showMessageDialog(
								TimeRangeAnalysisDialog.this,
								strErrorMessage,
								resourceBundle.getString("menu.error.title"),
								JOptionPane.ERROR_MESSAGE);
					}
				}
			});
		}

		return reanalyzeButton;
	}

	private boolean hasDataAfterFiltering(AnalysisFilter filter) {	
		List<PacketInfo> packetsInfoBeforeFilter =  currentTraceResult.getTraceresult().getAllpackets();
		PacketAnalyzerImpl packetAnalyzerImpl = (PacketAnalyzerImpl) ((MainFrame)parent).getController().getAROService().getAnalyzer();
		List<PacketInfo> packetsInfoAfterFilter = packetAnalyzerImpl.filterPackets(filter, packetsInfoBeforeFilter);

		return packetsInfoAfterFilter.size() > 0;
	}

	/**
	 * Initializes and returns the Calculate Statistics button
	 */
	private JButton getCalculateStatisticsButton() {
		if (calculateStatisticsButton == null) {
			calculateStatisticsButton = new JButton();
			calculateStatisticsButton.setText(resourceBundle.getString("menu.tools.timeRangeAnalysis.calculateStatistics.button"));
			calculateStatisticsButton.setToolTipText(resourceBundle.getString("menu.tools.timeRangeAnalysis.calculateStatistics.button.tooltip"));
			calculateStatisticsButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (currentTraceResult == null) {
							MessageDialogFactory.showMessageDialog(
									TimeRangeAnalysisDialog.this,
									resourceBundle.getString("menu.error.noTraceLoadedMessage"),
									resourceBundle.getString("error.title"),
									JOptionPane.ERROR_MESSAGE);
					} else {
						if (!updateCurrentfilter()) {
							return;
						}

						if (!hasDataAfterFiltering(currentTraceResult.getFilter())) {
							MessageDialogFactory.getInstance().showErrorDialog(TimeRangeAnalysisDialog.this,
									resourceBundle.getString("timerangeanalysis.noResultDataError"));
							return;
						}

						double startTime;
						double endTime;
						try {
							startTime = getTimeValue(startTimeTextField);
							endTime = getTimeValue(endTimeTextField);
						} catch (NumberFormatException e) {
							MessageDialogFactory.showMessageDialog(
									TimeRangeAnalysisDialog.this,
									resourceBundle.getString("timerangeanalysis.numberError"));
							return;
						}

						// Rounding traceEndTime as getEndTimeTextField() to
						// handle time comparison
						Double traceEndTimeRounded = Double.valueOf(DECIMAL_FORMAT.format(traceEndTime + ROUNDING_VALUE));
						if (startTime < endTime) {
							if (startTime >= 0.0 && startTime <= traceEndTimeRounded && endTime >= 0.0 && endTime <= traceEndTimeRounded) {
								TimeRangeAnalysis timeRangeAnalysis = new TimeRangeAnalysis(startTime, endTime, currentTraceResult, ((MainFrame)parent).getController());
								String msg = null;
								ProfileType profileType = currentTraceResult.getProfile().getProfileType();
								if (profileType == ProfileType.T3G) {
									msg = resourceBundle.getString("timerangeanalysis.3g");
								} else if (profileType == ProfileType.LTE) {
									msg = resourceBundle.getString("timerangeanalysis.lte");
								} else if (profileType == ProfileType.WIFI) {
									msg = resourceBundle.getString("timerangeanalysis.wifi");
								}

								timeRangeAnalysisResultsTextArea.setText(MessageFormat.format(
										(msg == null? "" : msg), 
										doubleToFixedDecimal(startTime, 3),
										doubleToFixedDecimal(endTime, 3),
										timeRangeAnalysis.getPayloadLen(),
										timeRangeAnalysis.getTotalBytes(),
										timeRangeAnalysis.getUplinkBytes(),
										timeRangeAnalysis.getDownlinkBytes(),
										doubleToFixedDecimal(timeRangeAnalysis.getRrcEnergy(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getActiveTime(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getMaxThroughput(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getMaxULThroughput(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getMaxDLThroughput(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getAverageThroughput(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getAverageUplinkThroughput(), 2),
										doubleToFixedDecimal(timeRangeAnalysis.getAverageDownlinkThroughput(), 2) + getResultPanelNote(timeRangeAnalysis)
								));

								timeRangeStartTime = startTime;
								timeRangeEndTime = endTime;
							} else {
								String strErrorMessage = MessageFormat.format(resourceBundle.getString("timerangeanalysis.rangeError"), 0.00, doubleToFixedDecimal(traceEndTimeRounded, 3));
								MessageDialogFactory.showMessageDialog(
										TimeRangeAnalysisDialog.this,
										strErrorMessage,
										resourceBundle.getString("error.title"),
										JOptionPane.ERROR_MESSAGE);
							}
						} else {
							String strErrorMessage = resourceBundle.getString("timerangeanalysis.startTimeError");
							MessageDialogFactory.showMessageDialog(
									TimeRangeAnalysisDialog.this,
									strErrorMessage,
									resourceBundle.getString("error.title"),
									JOptionPane.ERROR_MESSAGE);
						}
					} 
				}
			});
		}

		return calculateStatisticsButton;
	}

	private String doubleToFixedDecimal(double number, int scale) {
		return String.format("%." + scale + "f", number);
	}
	
	private String getResultPanelNote(TimeRangeAnalysis timeRangeAnalysis) {
		String str = "";
		if (ipv4Selection && !timeRangeAnalysis.isIpv4Present()) {
			str = StringUtil.isBlank(str) ? "IPv4" : str + ", IPV4";
		}

		if (ipv6Selection && !timeRangeAnalysis.isIpv6Present()) {
			str = StringUtil.isBlank(str) ? "IPv6" : str + ", IPv6";
		}

		if (tcpSelection && !timeRangeAnalysis.isTcpPresent()) {
			str = StringUtil.isBlank(str) ? "TCP" : str + ", TCP";
		}

		if (udpSelection && !timeRangeAnalysis.isUdpPresent()) {
			str = StringUtil.isBlank(str) ? "UDP" : str + ", UDP";
		}

		if (dnsSelection && !timeRangeAnalysis.isDnsPresent()) {
			str = StringUtil.isBlank(str) ? "DNS" : str + ", DNS";
		}

		return StringUtil.isBlank(str) ? "" : "\n\nNote: Current data does not have any " + str + " packets! ";
	}

	/**
	 * Initializes and returns the cancel button.
	 */
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(resourceBundle.getString("Button.cancel"));
			cancelButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					reset(false);
					dispose();
				}

			});
		}

		return cancelButton;
	}

	private void reset(boolean cloneFilter) {
		traceEndTime = endTimeResetValue;
		startTimeTextField.setText(DECIMAL_FORMAT.format(0.0));
		endTimeTextField.setText(doubleToFixedDecimal(endTimeResetValue, 3));
		chkIpv4.setSelected(ipv4Selection = initialFilter.isIpv4Sel());
		chkIpv6.setSelected(ipv6Selection = initialFilter.isIpv6Sel());
		chkTCP.setSelected(tcpSelection = initialFilter.isTcpSel());
		chkUdp.setSelected(udpSelection = initialFilter.isUdpSel());
		chkDns.setSelected(dnsSelection = initialFilter.isDnsSelection());

		currentTraceResult.setFilter(cloneFilter ? cloneFilter(initialFilter) : initialFilter);
		currentTraceResult.getFilter().setTimeRange(new TimeRange(initTimeRangeStartTime, initTimeRangeEndTime));
		timeRangeAnalysisResultsTextArea.setText("");
	}

	/**
	 * Initializes and returns the startTimeTextField
	 */
	private JTextField getStartTimeTextField() {
		if (startTimeTextField == null) {
			startTimeTextField = new JTextField(8);
			String strStartTime = doubleToFixedDecimal(timeRangeStartTime, 3);
			startTimeTextField.setText(strStartTime);
			
			startTimeTextField.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					Pattern pattern = Pattern.compile("^(?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-9]*\\d)(\\.(\\d{1,9}))?$");
					Matcher matcher = pattern.matcher(startTimeTextField.getText());
					
					if (!matcher.find()) {
						if (!e.getOppositeComponent().equals(cancelButton) && !e.getOppositeComponent().equals(resetButton)) {
							MessageDialogFactory.showMessageDialog(TimeRangeAnalysisDialog.this, resourceBundle.getString("timerangeanalysis.numberError"));
						}
						return;
					} else {
						if (!NumberUtils.isCreatable(startTimeTextField.getText())) {
							
							String[] timeTokens = startTimeTextField.getText().split(":");
							if (timeTokens.length <= 3) {
								double totalSeconds = 0d;
								for (int i = 0; i < timeTokens.length; i++) {
									totalSeconds += (Integer.parseInt(timeTokens[timeTokens.length - i - 1]) * Math.pow(60, i));
								}
								startTimeTextField.setText("" + totalSeconds);
							} else {
								MessageDialogFactory.showMessageDialog(TimeRangeAnalysisDialog.this, resourceBundle.getString("timerangeanalysis.numberError"));
							}
						}
					}
				}
				
				@Override
				public void focusGained(FocusEvent e) {
					
				}
			});
		}
		return startTimeTextField;
	}

	/**
	 * Returns the start time from the start time field
	 */
	private Double getTimeValue(JTextField field) throws NumberFormatException {
		return Double.parseDouble(field.getText());
	}

	/**
	 * Initializes and returns the endTimeTextField
	 */
	private JTextField getEndTimeTextField() {
		if (endTimeTextField == null) {
			endTimeTextField = new JTextField(8);
			String strEndTime = doubleToFixedDecimal(timeRangeEndTime, 3);
			endTimeTextField.setText(strEndTime);
			
			endTimeTextField.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					Pattern pattern = Pattern.compile("^(?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-9]*\\d)(\\.(\\d{1,9}))?$");
					Matcher matcher = pattern.matcher(endTimeTextField.getText());
					
					if (!matcher.find()) {
						if (!e.getOppositeComponent().equals(cancelButton) && !e.getOppositeComponent().equals(resetButton)) {
							MessageDialogFactory.showMessageDialog(TimeRangeAnalysisDialog.this, resourceBundle.getString("timerangeanalysis.numberError"));
						}
						return;
					} else {
						if (!NumberUtils.isCreatable(endTimeTextField.getText())) {
							String[] timeTokens = endTimeTextField.getText().split(":");
							if (timeTokens.length <= 3) {
								double totalSeconds = 0d;
								for (int i = 0; i < timeTokens.length; i++) {	
									totalSeconds += (Integer.parseInt(timeTokens[timeTokens.length - i - 1]) * Math.pow(60, i));
								}
								endTimeTextField.setText("" + totalSeconds);
							} else {
								MessageDialogFactory.showMessageDialog(TimeRangeAnalysisDialog.this, resourceBundle.getString("timerangeanalysis.numberError"));
							}
						}
					}
				}
				
				@Override
				public void focusGained(FocusEvent e) {
					
				}
			});
		}
		return endTimeTextField;
	}

	/**
	 * Sets a value that indicates the visibility of the TimeRangeAnalysis
	 * dialog box.
	 * 
	 * @param visible
	 *            A boolean value that indicates whether the TimeRangeAnalysis
	 *            dialog box is visible.
	 * 
	 * @see java.awt.Dialog#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		if (!isVisible() || !visible) {
			startTimeTextField.setText(doubleToFixedDecimal(timeRangeStartTime, 3));
			endTimeTextField.setText(doubleToFixedDecimal(timeRangeEndTime, 3));
		}
		super.setVisible(visible);
	}

	private WindowListener getWindowListener() {
		return (new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				// Auto-generated method 				
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
				// Auto-generated method 		
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				// Auto-generated method 
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
				// Auto-generated method 
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				reset(false);
				dispose();
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				// Auto-generated method 
			}
		});	
	}
}
