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
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.DefaultEditorKit;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.configuration.pojo.ProfileType;
import com.att.aro.core.packetanalysis.impl.PacketAnalyzerImpl;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
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

	/**
	 * Initializes the dialog.
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(500, 350);
		this.setResizable(false);
		this.setModal(true);
		this.setTitle(resourceBundle.getString("timerangeanalysis.title"));
		this.setLocationRelativeTo(getOwner());
		this.setContentPane(getJDialogPanel());
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
			jDialogPanel.add(getTimeRangeDialogButtons(), BorderLayout.CENTER);
			jDialogPanel.add(getTimeRangeResultsPanel(), BorderLayout.SOUTH);
		}

		return jDialogPanel;
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
			timeRangeResultsPanel.setPreferredSize(new Dimension(500, 230));
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
					traceEndTime = endTimeResetValue;
					startTimeTextField.setText(DECIMAL_FORMAT.format(0.0));
					endTimeTextField.setText(DECIMAL_FORMAT.format(endTimeResetValue));
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

					double timeRangeEndTime = Double.valueOf(DECIMAL_FORMAT.format( traceEndTime));
					if (startTime < endTime) {
						if ((startTime >= 0.0) && (startTime <= endTime) &&
								endTime <= timeRangeEndTime) {
							
							AnalysisFilter filter = ((MainFrame) parent).getController().getTheModel().getAnalyzerResult().getFilter();
							filter.setTimeRange(new TimeRange(startTime, endTime));
							
							if (!hasDataAfterFiltering(filter)) {
								MessageDialogFactory.getInstance().showErrorDialog(TimeRangeAnalysisDialog.this,
										resourceBundle.getString("timerangeanalysis.noResultDataError"));
							} else {
								((MainFrame) parent).updateFilter(filter);
								dispose();
							}
							
						} else {
							String strErrorMessage = MessageFormat.format(resourceBundle.getString("timerangeanalysis.rangeError"), 0.00, DECIMAL_FORMAT.format(timeRangeEndTime));
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
		List<PacketInfo> packetsInfoBeforeFilter =  ((MainFrame) parent).getController().getTheModel()
															.getAnalyzerResult().getTraceresult().getAllpackets();
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
					PacketAnalyzerResult traceResult = ((MainFrame)parent).getController().getTheModel().getAnalyzerResult();
					if (traceResult == null) {
							MessageDialogFactory.showMessageDialog(
									TimeRangeAnalysisDialog.this,
									resourceBundle.getString("menu.error.noTraceLoadedMessage"),
									resourceBundle.getString("error.title"),
									JOptionPane.ERROR_MESSAGE);
					} else {
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
								TimeRangeAnalysis timeRangeAnalysis = new TimeRangeAnalysis(startTime, endTime, traceResult);
								String msg = null;
								ProfileType profileType = traceResult.getProfile().getProfileType();
								if (profileType == ProfileType.T3G) {
									msg = resourceBundle.getString("timerangeanalysis.3g");
								} else if (profileType == ProfileType.LTE) {
									msg = resourceBundle.getString("timerangeanalysis.lte");
								} else if (profileType == ProfileType.WIFI) {
									msg = resourceBundle.getString("timerangeanalysis.wifi");
								}

								timeRangeAnalysisResultsTextArea.setText(MessageFormat.format(
										(msg == null? "" : msg), 
										DECIMAL_FORMAT.format(startTime),
										DECIMAL_FORMAT.format(endTime),
										timeRangeAnalysis.getPayloadLen(),
										timeRangeAnalysis.getTotalBytes(),
										timeRangeAnalysis.getUplinkBytes(),
										timeRangeAnalysis.getDownlinkBytes(),
										DECIMAL_FORMAT.format(timeRangeAnalysis.getRrcEnergy()),
										DECIMAL_FORMAT.format(timeRangeAnalysis.getActiveTime()),
										DECIMAL_FORMAT.format(timeRangeAnalysis.getAverageThroughput()),
										DECIMAL_FORMAT.format(timeRangeAnalysis.getAverageUplinkThroughput()),
										DECIMAL_FORMAT.format(timeRangeAnalysis.getAverageDownlinkThroughput())
								));

								timeRangeStartTime = startTime;
								timeRangeEndTime = endTime;
							} else {
								String strErrorMessage = MessageFormat.format(resourceBundle.getString("timerangeanalysis.rangeError"), 0.00, DECIMAL_FORMAT.format(traceEndTimeRounded));
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
					closeWindow();
				}

			});
		}

		return cancelButton;
	}

	private void closeWindow() {
		AnalysisFilter filter = ((MainFrame) parent).getController().getTheModel().getAnalyzerResult().getFilter();
		filter.setTimeRange(new TimeRange(initTimeRangeStartTime, initTimeRangeEndTime));
		dispose();
	}

	/**
	 * Initializes and returns the startTimeTextField
	 */
	private JTextField getStartTimeTextField() {
		if (startTimeTextField == null) {
			startTimeTextField = new JTextField(8);
			String strStartTime = DECIMAL_FORMAT.format(timeRangeStartTime);
			startTimeTextField.setText(strStartTime);
			
			startTimeTextField.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					Pattern pattern = Pattern.compile("^(?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-9]*\\d)(\\.(\\d{1,9}))?$");
					Matcher matcher = pattern.matcher(startTimeTextField.getText());
					
					if (!matcher.find()) {
						MessageDialogFactory.showMessageDialog(TimeRangeAnalysisDialog.this, resourceBundle.getString("timerangeanalysis.numberError"));
						return;
					} else {
						if (!NumberUtils.isNumber(startTimeTextField.getText())) {
							
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
			String strEndTime = DECIMAL_FORMAT.format(timeRangeEndTime);
			endTimeTextField.setText(strEndTime);
			
			endTimeTextField.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					Pattern pattern = Pattern.compile("^(?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-9]*\\d)(\\.(\\d{1,9}))?$");
					Matcher matcher = pattern.matcher(endTimeTextField.getText());
					
					if (!matcher.find()) {
						MessageDialogFactory.showMessageDialog(TimeRangeAnalysisDialog.this, resourceBundle.getString("timerangeanalysis.numberError"));
						return;
					} else {
						if (!NumberUtils.isNumber(endTimeTextField.getText())) {
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
			DecimalFormat decimalFormat = new DecimalFormat("0.00");
			startTimeTextField.setText(decimalFormat.format(timeRangeStartTime));
			endTimeTextField.setText(decimalFormat.format(timeRangeEndTime));
		}
		super.setVisible(visible);
	}

}
