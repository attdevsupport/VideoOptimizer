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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.IVideoBestPractices;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.pojo.UserEvent;
import com.att.aro.core.peripheral.pojo.UserEvent.UserEventType;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.impl.VideoSegmentAnalyzer;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.diagnostictab.plot.VideoChunksPlot;
import com.att.aro.ui.view.video.IVideoPlayer;
import com.att.aro.ui.view.videotab.SegmentPanel;
import com.att.aro.ui.view.videotab.VideoManifestPanel;

public class StartUpDelayDialog extends JDialog {
	private static final Logger LOGGER = LogManager.getLogger(StartUpDelayDialog.class.getName());
	private static final long serialVersionUID = 1L;
	private static final int SLIDERDIALOG_COLUMN_COUNT = 2;
	private static final int MAXIMUM_EVENT_PER_SECOND = 20;
	private static final int CHECK_COL = 0;
	private long eventTimeStamp = 0;
	private int eventCount = 0;
	private JSlider slider;
	private JSlider userEventSlider;
	private JButton okButton;
	private int maxValue;
	private int userEventMaxValue;
	private int userEventMinValue;
	private int minValue;
	private IVideoPlayer player;
	private double startTime;
	private VideoChunksPlot vcPlot;
	private MainFrame mainFrame;
	private GraphPanel parentPanel;
	private BufferedImage resizedThumbnail;
	private Map<Integer, VideoEvent> chunkSelectionList = new TreeMap<>();
	private Map<Integer, UserEvent> userEventSelectionList = new TreeMap<>();
	private JTable jTable;
	private JTable userEventJTable;
	private List<JTableItems> listSegments = new ArrayList<>();
	private List<UserEventJTableItem> listUserEvents = new ArrayList<>();
	private List<VideoEvent> allChunks = new ArrayList<>();
	private List<UserEvent> allListUserEvent = new ArrayList<>();

	private JTableItems segmentChosen;
	private UserEventJTableItem eventChosen;
	private int selectedIndex = 0;
	private int selectedIdxUE = 0;
	private JButton plusTunerBtn;
	private JButton minusTunerBtn;
	private JPanel segmentPanel;
	private JLabel imgLabel = new JLabel();
	private StreamingVideoData streamingVideoData;
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	private JComboBox<ComboManifest> jcb;
	private DefaultTableModel tableModel;
	private UserEventDataModel userEventTableModel;

	private JTextField playRequestedTime;
	private JTextField startTimeField;

	private PlotHelperAbstract videoChunkPlotter = ContextAware.getAROConfigContext().getBean("videoChunkPlotterImpl",
			PlotHelperAbstract.class);
	private VideoSegmentAnalyzer videoSegmentAnalyzer = ContextAware.getAROConfigContext()
			.getBean("videoSegmentAnalyzer", VideoSegmentAnalyzer.class);
	private JPanel imgLabelPanel;
	private SegmentPanel segmentTablePanel;

	public StartUpDelayDialog(GraphPanel parentPanel, double maxVideoTime, VideoStream videoStream,
			List<UserEvent> userEventList, SegmentPanel segmentTablePanel) {
		this.parentPanel = parentPanel;
		this.segmentTablePanel=segmentTablePanel;
		DiagnosticsTab parent = parentPanel.getGraphPanelParent();
		mainFrame = (MainFrame) parent.getAroView();
		player = parent.getVideoPlayer();
		vcPlot = parentPanel.getVcPlot();
		chunkSelectionList = new TreeMap<>();
		userEventSelectionList = new TreeMap<>();
		this.allChunks.addAll(new ArrayList<VideoEvent>(videoStream.getVideoEventList().values()));
		if (!userEventList.isEmpty()) {
			for (UserEvent userEvent : userEventList) {
				if (UserEventType.SCREEN_TOUCH.equals(userEvent.getEventType())) {
					allListUserEvent.add(userEvent);
				}
			}
		}
		createSliderDialog(maxVideoTime, player, vcPlot, 0);
		if(!allListUserEvent.isEmpty()) {
			dialogLaunchUserEvent();
		}
		dialogLaunch();
		if (startTime == 0) {
			setStartTime(videoStream.getFirstSegment().getDLLastTimestamp());
		}
	}

	public void dialogLaunch() {
		if (selectedIndex >= 0 && selectedIndex < jTable.getRowCount()) {
			jTable.setRowSelectionInterval(selectedIndex, selectedIndex);
			jTable.getModel().setValueAt(true, selectedIndex, 0);
			if (videoChunkPlotter.getStreamingVideoData().getStreamingVideoCompiled().getChunkPlayTimeList()
					.keySet() != null) {
				for (VideoEvent veSegment : videoChunkPlotter.getStreamingVideoData().getStreamingVideoCompiled()
						.getChunkPlayTimeList().keySet()) {
					for (int index = 0; index < listSegments.size(); index++) {
						JTableItems item = listSegments.get(index);
						if (item.getVideoEvent().equals(veSegment)) {
							jTable.getModel().setValueAt(true, index, 0);
							break;
						}
					}
				}

			}
			jTable.scrollRectToVisible(new Rectangle(jTable.getCellRect(selectedIndex, 0, true)));
		}
		setStartTime(loadStartupDelay());
		getPlayer().setMediaTime(getStartTime());
		startTimeField.setText(String.format("%.3f", getStartTime()));
	}

	public void dialogLaunchUserEvent() {
		if (selectedIdxUE >= 0 && selectedIdxUE < userEventJTable.getRowCount()) {
			userEventJTable.setRowSelectionInterval(selectedIdxUE, selectedIdxUE);
			userEventJTable.getModel().setValueAt(true, selectedIdxUE, 0);
			userEventJTable.scrollRectToVisible(new Rectangle(userEventJTable.getCellRect(selectedIdxUE, 0, true)));
		}
		setStartTime(loadStartupDelay());
		getPlayer().setMediaTime(getStartTime());
		playRequestedTime.setText(String.format("%.3f", getStartTime()));
	}

	public void setStartTime(double startTime) {
		LOGGER.info(String.format("SET startTime :%.03f", startTime));
		this.startTime = startTime;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public void createSliderDialog(double max, IVideoPlayer player, final VideoChunksPlot vcPlot, int indexKey) {

		this.player = player;
		double initialValue = getPlayer().getVideoOffset();
		streamingVideoData = mainFrame.getController().getTheModel().getAnalyzerResult().getStreamingVideoData();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setResizable(true);

		jcb = new JComboBox<ComboManifest>();
		jcb.addItem(new ComboManifest());
		int manifestsSelectedCount = 0;
		int manifestIdxSelected = 0;
		int selectedIndex = 1;
		for (VideoStream videoStream : streamingVideoData.getVideoStreamMap().values()) {
			if (videoStream.isSelected()) {
				manifestsSelectedCount++;
				selectedIndex = manifestIdxSelected;
				manifestIdxSelected++;
				jcb.addItem(new ComboManifest(videoStream));
			}
		}
		jcb.addActionListener(selectStreamActionListener());
		getContentPane().add(jcb);

		// add user event panel
		if (!allListUserEvent.isEmpty()) {

			JPanel userEventPanel = new JPanel();
			userEventPanel.setBorder(new RoundedBorder(new Insets(5, 5, 5, 5), null));

			JPanel userEventLabelPanel = new JPanel(new BorderLayout());
			userEventLabelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			userEventLabelPanel.add(new JLabel("Position slider to user touch event"));
			userEventPanel.add(userEventLabelPanel);

			JPanel userEventComboPanel = new JPanel(new BorderLayout(5, 0));
			userEventComboPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
			userEventComboPanel.setSize(500, 100);
			listUserEvents.clear();
			populateUserEventList();
			makeSelection(indexKey);

			userEventTableModel = new UserEventDataModel();
			userEventTableModel.setRowCount(listUserEvents.size());
			userEventTableModel.setColumnCount(SLIDERDIALOG_COLUMN_COUNT);

			userEventJTable = new JTable(userEventTableModel) {
				@Override
				public TableCellRenderer getCellRenderer(int row, int column) {
					if (getValueAt(row, column) instanceof Boolean) {
						UserEventJTableItem userEventItemObject = listUserEvents.get(row);
						boolean checkBoxStatus = (boolean) getValueAt(row, column);
						userEventItemObject.setSelected(checkBoxStatus);
						return super.getDefaultRenderer(Boolean.class);
					} else {
						return super.getCellRenderer(row, column);
					}
				}

				@Override
				public TableCellEditor getCellEditor(int row, int column) {
					if (getValueAt(row, column) instanceof Boolean) {
						return super.getDefaultEditor(Boolean.class);
					} else {
						return super.getCellEditor(row, column);
					}
				}
			};

			updateUserEventJTableData();
			// Listener for mouse click
			userEventJTable.addMouseListener(getCheckboxListener1());
			userEventJTable.getSelectionModel().addListSelectionListener(rowUserEventSelectionListener());

			JScrollPane userEventListScrollPanel = new JScrollPane(userEventJTable);
			userEventListScrollPanel.setPreferredSize(new Dimension(500, 100));

			userEventComboPanel.add(userEventListScrollPanel);
			userEventPanel.add(userEventComboPanel);

			JPanel userEventNoteLabelPanel = new JPanel(new BorderLayout());
			userEventNoteLabelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			userEventNoteLabelPanel.add(new JLabel(
					"Enabling the Settings-> Developer Option -> Show Tap helps to identify the precise video playback requested time."));
			userEventPanel.add(userEventNoteLabelPanel);

			JPanel userEventSliderBoxPanel = new JPanel(new BorderLayout());
			userEventSliderBoxPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 1));

			JPanel userEventSliderPanel = new JPanel(new GridBagLayout());
			userEventMaxValue = setMaxMinValue(max + initialValue, true);
			userEventMinValue = setMaxMinValue(initialValue, false);
			// define slider
			userEventSlider = new JSlider(JSlider.HORIZONTAL, minValue, userEventMaxValue, userEventMinValue);
			userEventSlider.setPaintLabels(true);
			userEventSlider.setPaintTicks(true);
			userEventSlider.addChangeListener(getUserEventSliderListener());

			GridBagConstraints userEventSliderGBC = new GridBagConstraints();
			userEventSliderGBC.fill = GridBagConstraints.HORIZONTAL;
			userEventSliderGBC.gridx = 0;
			userEventSliderGBC.gridy = 0;
			userEventSliderGBC.weightx = 2;

			userEventSliderPanel.add(userEventSlider, userEventSliderGBC);

			playRequestedTime = new JTextField("       0");
			playRequestedTime.setText(Double.toString(getStartTime()));
			playRequestedTime
					.addActionListener(startTimeTextFieldActionListener(playRequestedTime, userEventSlider));
			playRequestedTime.addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {
				}

				@Override
				public void keyReleased(KeyEvent e) {
				}

				@Override
				public void keyPressed(KeyEvent e) {
					// refresh hairline and videoplayer without triggering an analysis
					if (e.getKeyCode() == 157) { // mac Command key
						updateStartTime(playRequestedTime, userEventSlider);
					}
				}
			});

			GridBagConstraints userEventStartTimeGBC = new GridBagConstraints();
			userEventStartTimeGBC.fill = GridBagConstraints.HORIZONTAL;
			userEventStartTimeGBC.gridx = 1;
			userEventStartTimeGBC.gridy = 0;
			userEventStartTimeGBC.insets = new Insets(0, 0, 0, 1);
			userEventStartTimeGBC.weightx = 0.35;
			userEventSliderPanel.add(playRequestedTime, userEventStartTimeGBC);

			JButton minusTunerBtnUE = new JButton("-");
			minusTunerBtnUE.setPreferredSize(new Dimension(5, 15));
			minusTunerBtnUE.addActionListener(tunerUEButtonActionListener());
			GridBagConstraints minusTunerBtnUEGBC = new GridBagConstraints();
			minusTunerBtnUEGBC.fill = GridBagConstraints.HORIZONTAL;
			minusTunerBtnUEGBC.gridx = 2;
			minusTunerBtnUEGBC.gridy = 0;
			minusTunerBtnUEGBC.insets = new Insets(0, 0, 0, 1);
			minusTunerBtnUEGBC.weightx = 0.35;
			userEventSliderPanel.add(minusTunerBtnUE, minusTunerBtnUEGBC);

			JButton plusTunerBtnUE = new JButton("+");
			plusTunerBtnUE.setPreferredSize(new Dimension(5, 15));
			plusTunerBtnUE.addActionListener(tunerUEButtonActionListener());
			GridBagConstraints plusTunerBtnUEGBC = new GridBagConstraints();
			plusTunerBtnUEGBC.fill = GridBagConstraints.HORIZONTAL;
			plusTunerBtnUEGBC.gridx = 3;
			plusTunerBtnUEGBC.gridy = 0;
			plusTunerBtnUEGBC.insets = new Insets(0, 0, 0, 2);
			plusTunerBtnUEGBC.weightx = 0.35;
			userEventSliderPanel.add(plusTunerBtnUE, plusTunerBtnUEGBC);
			userEventSliderBoxPanel.add(userEventSliderPanel);

			userEventPanel.add(userEventSliderBoxPanel);
			userEventPanel.setLayout(new BoxLayout(userEventPanel, BoxLayout.Y_AXIS));

			getContentPane().add(userEventPanel);
		}
		// add segment panel
		segmentPanel = new JPanel();
		segmentPanel.setBorder(new RoundedBorder(new Insets(5, 5, 5, 5), null));
		segmentPanel.setLayout(new BoxLayout(segmentPanel, BoxLayout.Y_AXIS));
		getContentPane().add(segmentPanel);

		JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		labelPanel.add(new JLabel(resourceBundle.getString("sliderdialog.segmentlist.label")));
		GridBagConstraints gbc_labelPanel = new GridBagConstraints();
		gbc_labelPanel.anchor = GridBagConstraints.WEST;
		gbc_labelPanel.insets = new Insets(0, 0, 5, 5);
		gbc_labelPanel.gridx = 1;
		gbc_labelPanel.gridy = 0;
		segmentPanel.add(labelPanel, gbc_labelPanel);
		JPanel comboPanel = new JPanel(new BorderLayout(5, 0));
		comboPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
		comboPanel.setSize(500, 100);

		listSegments.clear();
		populateList();
		// making the correct selection of JTable row based on user input -->
		// Then scroll JTable to selected row
		makeSelection(indexKey);
		// JTable model having two columns
		tableModel = new DefaultTableModel();
		tableModel.setRowCount(listSegments.size());
		tableModel.setColumnCount(SLIDERDIALOG_COLUMN_COUNT);
		// JTable Renderer listeners
		jTable = new JTable(tableModel) {
			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				if (getValueAt(row, column) instanceof Boolean) {
					JTableItems itemObject = listSegments.get(row);
					boolean checkBoxStatus = (boolean) getValueAt(row, column);
					itemObject.setSelected(checkBoxStatus);
					return super.getDefaultRenderer(Boolean.class);
				} else {
					return super.getCellRenderer(row, column);
				}
			}

			@Override
			public TableCellEditor getCellEditor(int row, int column) {
				if (getValueAt(row, column) instanceof Boolean) {
					return super.getDefaultEditor(Boolean.class);
				} else {
					return super.getCellEditor(row, column);
				}
			}
		};
		updateJTableData();
		if (manifestsSelectedCount == 1) {
			jcb.setSelectedIndex(selectedIndex + 1);
		}
		// Listener for mouse click
		jTable.addMouseListener(getCheckboxListener());

		// Listener for list selection on jTable
		jTable.getSelectionModel().addListSelectionListener(rowSelectionListener());

		JScrollPane listScrollPanel = new JScrollPane(jTable);
		listScrollPanel.setPreferredSize(new Dimension(500, 100));
		comboPanel.add(listScrollPanel);
		if (manifestIdxSelected > 1) {
			JLabel warningLabel = new JLabel(resourceBundle.getString("sliderdialog.manifest.warning"));
			comboPanel.add(warningLabel, BorderLayout.EAST);
		}
		segmentPanel.add(comboPanel);
		imgLabelPanel = new JPanel(new BorderLayout());
		imgLabelPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
		imgLabelPanel.add(imgLabel);
		imgLabelPanel.setSize(imgLabelPanel.getPreferredSize());
		segmentPanel.add(imgLabelPanel);
		JPanel sliderBoxPanel = new JPanel(new BorderLayout());
		sliderBoxPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 1));
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new GridBagLayout());

		GridBagConstraints sliderGBC = new GridBagConstraints();
		sliderGBC.fill = GridBagConstraints.HORIZONTAL;
		sliderGBC.gridx = 0;
		sliderGBC.gridy = 0;
		sliderGBC.weightx = 2;
		this.maxValue = setMaxMinValue(max + initialValue, true);
		this.minValue = setMaxMinValue(initialValue, false);

		// define slider
		slider = new JSlider(JSlider.HORIZONTAL, minValue, maxValue, minValue);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.addChangeListener(getSliderListener());
		slider.addMouseMotionListener(chooseSegmentListener());
		GridBagConstraints startTimeFieldGBC = new GridBagConstraints();

		sliderPanel.add(slider, sliderGBC);

		startTimeField = getStartTimeDisplay();
		startTimeFieldGBC.fill = GridBagConstraints.HORIZONTAL;
		startTimeFieldGBC.gridx = 1;
		startTimeFieldGBC.gridy = 0;
		startTimeFieldGBC.insets = new Insets(0, 0, 0, 1);
		startTimeFieldGBC.weightx = 0.35;
		sliderPanel.add(startTimeField, startTimeFieldGBC);
		GridBagConstraints gbc_minusTunerBtn = new GridBagConstraints();

		minusTunerBtn = new JButton("-");
		minusTunerBtn.setPreferredSize(new Dimension(5, 15));
		minusTunerBtn.addActionListener(tunerButtonActionListener());
		gbc_minusTunerBtn.fill = GridBagConstraints.HORIZONTAL;
		gbc_minusTunerBtn.gridx = 2;
		gbc_minusTunerBtn.gridy = 0;
		gbc_minusTunerBtn.insets = new Insets(0, 0, 0, 1);
		gbc_minusTunerBtn.weightx = 0.35;
		sliderPanel.add(minusTunerBtn, gbc_minusTunerBtn);

		plusTunerBtn = new JButton("+");
		plusTunerBtn.setPreferredSize(new Dimension(5, 15));
		plusTunerBtn.addActionListener(tunerButtonActionListener());
		GridBagConstraints plusTinerBtnGBC = new GridBagConstraints();

		plusTinerBtnGBC.fill = GridBagConstraints.HORIZONTAL;
		plusTinerBtnGBC.gridx = 3;
		plusTinerBtnGBC.gridy = 0;
		plusTinerBtnGBC.insets = new Insets(0, 0, 0, 2);
		plusTinerBtnGBC.weightx = 0.35;
		sliderPanel.add(plusTunerBtn, plusTinerBtnGBC);

		sliderBoxPanel.add(sliderPanel);

		GridBagConstraints gbc_sliderBoxPanel = new GridBagConstraints();
		gbc_sliderBoxPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_sliderBoxPanel.insets = new Insets(0, 0, 0, 5);
		gbc_sliderBoxPanel.gridx = 1;
		gbc_sliderBoxPanel.gridy = 1;
		segmentPanel.add(sliderBoxPanel, gbc_sliderBoxPanel);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 220, 1, 220));

		// build the 'Set' JButton
		okButton = new JButton("Set");
		okButton.addActionListener(getSetButtonListener(vcPlot));
		btnPanel.add(okButton);
		getContentPane().add(btnPanel);

	}

	public JTextField getStartTimeDisplay() {
		JTextField startTimeField = new JTextField("       0");
		startTimeField.setText(Double.toString(getStartTime()));
		startTimeField.addActionListener(startTimeTextFieldActionListener(startTimeField, slider));
		startTimeField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// refresh hairline and videoplayer without triggering an analysis
				if (e.getKeyCode() == 157) { // mac Command key
					updateStartTime(startTimeField, slider);
				}
			}
		});
		return startTimeField;
	}

	public ActionListener tunerButtonActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (button.getText().trim().equals("+")) {
					int sliderValue = (slider.getValue());
					sliderValue = (sliderValue + 1);
					slider.setValue(sliderValue);
				} else {
					int sliderValue = (slider.getValue());
					sliderValue = (sliderValue - 1);
					slider.setValue(sliderValue);
				}
			}
		};
	}

	public ActionListener tunerUEButtonActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (button.getText().trim().equals("+")) {
					int sliderValue = (userEventSlider.getValue());
					sliderValue = (sliderValue + 1);
					userEventSlider.setValue(sliderValue);
				} else {
					int sliderValue = (userEventSlider.getValue());
					sliderValue = (sliderValue - 1);
					userEventSlider.setValue(sliderValue);
				}
			}
		};
	}

	public MouseMotionListener chooseSegmentListener() {
		return new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				ComboManifest comboBoxItem = (ComboManifest) jcb.getSelectedItem();
				if (segmentChosen == null || !segmentChosen.isSelected && comboBoxItem.getVideoStream() == null) {
					showWarningMessage();
				}
			}
		};
	}

	public void showWarningMessage() {
		JOptionPane.showMessageDialog(StartUpDelayDialog.this,
				"Please select a segment and a manifest first in order to set the start up delay.", "Warning",
				JOptionPane.WARNING_MESSAGE);
	}

	public void showUserEventWarningMessage() {
		JOptionPane.showMessageDialog(StartUpDelayDialog.this,
				"Startup time should be greater than Play requested time", "Warning", JOptionPane.WARNING_MESSAGE);
	}

	private boolean rateLimitEventsPerSecond() {
		long currentTime = System.currentTimeMillis();
		if (eventTimeStamp == 0) {
			eventTimeStamp = currentTime;
		}
		long diff = currentTime - eventTimeStamp;
		if (diff > 1000) { // diff value check against one second interval
			eventCount = 0;
			eventTimeStamp = currentTime;
			return true;
		}
		boolean executeEvent = (diff < 1000 && eventCount++ < MAXIMUM_EVENT_PER_SECOND) ? true : false;
		return executeEvent;
	}

	public ChangeListener getSliderListener() {
		return new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean executeEvent = true;
				if (Util.isWindowsOS()) {
					executeEvent = rateLimitEventsPerSecond();
				}
				if (executeEvent) {
					double value = ((JSlider) e.getSource()).getValue();
					double seconds = value * 0.04;
					double videoOffset = getPlayer().getVideoOffset();
					// forward to video player
					getPlayer().setMediaTime(seconds);
					double mediaTime = getPlayer().getMediaTime();
					setStartTime(mediaTime + videoOffset);
					startTimeField.setText(String.format("%.03f", mediaTime + videoOffset));
					startTimeField.revalidate();

				}
			}
		};
	}

	public ChangeListener getUserEventSliderListener() {
		return new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean executeEvent = true;
				if (Util.isWindowsOS()) {
					executeEvent = rateLimitEventsPerSecond();
				}
				if (executeEvent) {
					double value = ((JSlider) e.getSource()).getValue();
					double seconds = value * 0.04;
					double videoOffset = getPlayer().getVideoOffset();
					// forward to video player
					getPlayer().setMediaTime(seconds);
					double mediaTime = getPlayer().getMediaTime();
					setStartTime(mediaTime + videoOffset);
					playRequestedTime.setText(String.format("%.03f", mediaTime + videoOffset));
					playRequestedTime.revalidate();
				}
			}
		};
	}

	public void updateStartTime(JTextField startTimeField, JSlider slider) {
		double tempTime;
		try {
			tempTime = Double.valueOf(startTimeField.getText());
			setStartTime(tempTime);
			slider.setValue((int) Math.round(tempTime * 25));
		} catch (NumberFormatException e) {
			LOGGER.debug("Failed to parse bad data in startTimeField:" + startTimeField.getText());
			tempTime = getStartTime();
		}
		setStartTime(tempTime);
	}

	public ActionListener startTimeTextFieldActionListener(JTextField startTimeField, JSlider slider) {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// enter/return has been pressed in startTimeField
				try {
					double sec = Double.valueOf(e.getActionCommand());
					if (sec < 0) {
						// Handle user entered parsable bad data by replacing with the original data
						startTimeField.setText(Double.toString(getStartTime()));
					}
					slider.setValue((int) sec * 25);
					setStartTime(sec);
					launchStartupCalculations(sec);
				} catch (NumberFormatException e1) {
					// Handle user entered non-parsable bad data by replacing with the original data
					startTimeField.setText(Double.toString(getStartTime()));
				}
			}
		};
	}

	public ListSelectionListener rowSelectionListener() {
		return new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int row = jTable.getSelectedRow();
				if (row >= 0 && row < listSegments.size()) {
					JTableItems itemObject = listSegments.get(row);
					resizedThumbnail = itemObject.getVideoEvent().getImageOriginal();
					ImageIcon img = new ImageIcon(resizedThumbnail);
					img = new ImageIcon(img.getImage().getScaledInstance(-1, 300, Image.SCALE_DEFAULT));
					imgLabel.setIcon(img);

					double timestamp = itemObject.getVideoEvent().getPlayTime() != 0
							? itemObject.getVideoEvent().getPlayTime()
							: itemObject.getVideoEvent().getEndTS();
					slider.setValue((int) Math.round(timestamp * 25));
					segmentPanel.setSize(segmentPanel.getPreferredSize());
					Dimension dimen = imgLabelPanel.getSize();
					double ratio = dimen.getWidth() / dimen.getHeight();
					dimen.setSize(300 * ratio, 300);
					imgLabelPanel.setPreferredSize(dimen);
					segmentPanel.revalidate();
					JDialog parentDialog = (JDialog) segmentPanel.getRootPane().getParent();
					parentDialog.setSize(parentDialog.getPreferredSize());
					parentDialog.revalidate();
				}
				selectSegment();
			}
		};
	}

	public ListSelectionListener rowUserEventSelectionListener() {
		return new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int row = userEventJTable.getSelectedRow();
				if (row >= 0 && row < listUserEvents.size()) {
					UserEventJTableItem itemObject = listUserEvents.get(row);
					double timestamp = getUserEventTimeStamp(itemObject);
					userEventSlider.setValue((int) Math.round(timestamp * 25));
					playRequestedTime.setText(String.format("%.03f", timestamp));
					playRequestedTime.revalidate();

				}
				selectUserEvent();
			}
		};
	}

	private double getUserEventTimeStamp(UserEventJTableItem itemObject) {
		return itemObject.getUserEvent().getPressTime() != 0
				? itemObject.getUserEvent().getPressTime()
				: itemObject.getUserEvent().getReleaseTime();
		
	}
	
	public void selectSegment() {
		int column = jTable.getSelectedColumn();
		int row = jTable.getSelectedRow();
		if (row < 0 || listSegments.isEmpty()) {
			return;
		}

		JTableItems selectedChunk = listSegments.get(row);
		ComboManifest comboBoxItem = (ComboManifest) jcb.getSelectedItem();
		if (column != 1 && (!selectedChunk.isSelected)) {
			if (comboBoxItem.getVideoStream() != null) {
				// capture the selected segment
				if (segmentChosen != null) {
					jTable.getModel().setValueAt(false, segmentChosen.getRow(), 0);
					jTable.getModel().setValueAt(true, row, 0);
				}
				segmentChosen = selectedChunk;
			} else {
				jTable.getModel().setValueAt(false, selectedChunk.getRow(), 0);
				showWarningMessage();
			}
		} else if (column == 0 && segmentChosen != null) {// remove the selected segment
			jTable.getModel().setValueAt(false, segmentChosen.getRow(), 0);
			segmentChosen = null;
		}
	}

	public void selectUserEvent() {
		int column = userEventJTable.getSelectedColumn();
		int row = userEventJTable.getSelectedRow();
		if (row < 0 || listUserEvents.isEmpty()) {
			return;
		}

		UserEventJTableItem selectedEvent = listUserEvents.get(row);
		ComboManifest comboBoxItem = (ComboManifest) jcb.getSelectedItem();
		if (column != 1 && (!selectedEvent.isSelected)) {
			if (comboBoxItem.getVideoStream() != null) {
				// capture the selected user event
				if (eventChosen != null) {
					userEventJTable.getModel().setValueAt(false, eventChosen.getRow(), 0);
					userEventJTable.getModel().setValueAt(true, row, 0);
				}
				eventChosen = selectedEvent;
			} else {
				userEventJTable.getModel().setValueAt(false, selectedEvent.getRow(), 0);
				showUserEventWarningMessage();
			}
		} else if (column == 0 && eventChosen != null) {// remove the selected segment
			userEventJTable.getModel().setValueAt(false, eventChosen.getRow(), 0);
			eventChosen = null;
		}
	}

	public MouseAdapter getCheckboxListener() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				selectSegment();
			}
		};
	}

	public MouseAdapter getCheckboxListener1() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				selectUserEvent();
			}
		};
	}

	public void makeSelection(int indexKey) {
		if (indexKey < 0 || indexKey >= chunkSelectionList.size()) {
			return;
		}
		VideoEvent segment = chunkSelectionList.get(indexKey);
		for (int index = 0; index < listSegments.size(); index++) {
			if (listSegments.get(index).getVideoEvent().equals(segment)) {
				selectedIndex = index;
				break;
			}
		}
	}

	public void makeUserEventSelection(int indexKey) {
		if (indexKey < 0 || indexKey >= userEventSelectionList.size()) {
			return;
		}
		UserEvent userEvent = userEventSelectionList.get(indexKey);
		for (int index = 0; index < listUserEvents.size(); index++) {
			if (listUserEvents.get(index).getUserEvent().equals(userEvent)) {
				selectedIdxUE = index;
				break;
			}
		}
	}

	public double loadStartupDelay() {
		AROTraceData traceData = mainFrame.getController().getTheModel();

		Double tempStartupTime = traceData.getAnalyzerResult().getTraceresult().getVideoStartTime();
		VideoStreamStartup videoStreamStartup = ((TraceDirectoryResult) traceData.getAnalyzerResult().getTraceresult())
				.getVideoStartup();
		tempStartupTime = videoStreamStartup.getStartupDelay();
		if (tempStartupTime == 0) {
			return getStartTime();
		}
		return tempStartupTime;
	}

	public IVideoPlayer getPlayer() {
		return this.player;
	}

	private void updateJTableData() {
		jTable.getColumnModel().getColumn(0).setMaxWidth(30);
		jTable.setTableHeader(null);
		for (int i = 0; i < listSegments.size(); i++) {
			JTableItems item = listSegments.get(i);
			jTable.getModel().setValueAt(item.isSelected, i, 0);
			jTable.getModel().setValueAt(item.value, i, 1);
		}
	}

	private void updateUserEventJTableData() {
		userEventJTable.getColumnModel().getColumn(0).setMaxWidth(30);
		userEventJTable.setTableHeader(null);
		for (int i = 0; i < listUserEvents.size(); i++) {
			UserEventJTableItem item = listUserEvents.get(i);
			userEventJTable.getModel().setValueAt(item.isSelected, i, 0);
			userEventJTable.getModel().setValueAt(item.value, i, 1);
		}

	}

	public int setMaxMinValue(double value, boolean max) {
		int limit;
		if (max) {
			limit = (int) Math.ceil(value);
		} else {
			limit = (int) Math.floor(value);
		}
		return limit * 25;
	}

	public ActionListener selectStreamActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<ComboManifest> comboBox = (JComboBox<ComboManifest>) e.getSource();
				ComboManifest comboManifest = (ComboManifest) comboBox.getSelectedItem();
				VideoStream videoStream = comboManifest.getVideoStream();
				if (null != videoStream) {
					allChunks.clear();
					allChunks = new ArrayList<VideoEvent>();
					if (videoStream.getManifest().getVideoFormat() == VideoFormat.MPEG4) {
						for (VideoEvent videoEvent : videoStream.getVideoEventsBySegment()) {
							if (videoEvent.getSegmentID() != 0) {
								allChunks.add(videoEvent);
							}
						}
					} else {
						allChunks = new ArrayList<VideoEvent>(videoStream.getVideoEventsBySegment());
					}
					listSegments.clear();
					populateList();
					tableModel.getDataVector().removeAllElements();
					tableModel.fireTableDataChanged();
					tableModel.setRowCount(listSegments.size());
					updateJTableData();
				}
			}
		};
	}

	public void populateUserEventList() {
		for (int idx = 0; idx < allListUserEvent.size(); idx++) {
			UserEvent ue = allListUserEvent.get(idx);
			listUserEvents.add(new UserEventJTableItem(ue, idx));
		}

	}

	public void populateList() {
		for (int index = 0; index < allChunks.size(); index++) {
			VideoEvent ve = allChunks.get(index);
			if (ve.getSegmentInfo().isVideo()) {
				if (segmentChosen != null && ve.equals(segmentChosen.getVideoEvent())) {
					listSegments.add(segmentChosen);
				} else {
					listSegments.add(new JTableItems(ve, index));
				}
			}
		}
		Collections.sort(listSegments, new Comparator<JTableItems>() {
			@Override
			public int compare(JTableItems o1, JTableItems o2) {
				if (o1.getVideoEvent().getSegmentID() < o2.getVideoEvent().getSegmentID()) {
					return -1;
				} else if (o1.getVideoEvent().getSegmentID() > o2.getVideoEvent().getSegmentID()) {
					return 1;
				} else {
					return 0;
				}
			}
		});
	}

	public void launchStartupCalculations(double startupTime) {
		try {
			if (startupTime >= segmentChosen.getVideoEvent().getEndTS() && segmentChosen.isSelected) {
				ComboManifest selectedStream = (ComboManifest) jcb.getSelectedItem();
				if (selectedStream.getVideoStream() != null) {
					VideoStream videoStream = selectedStream.getVideoStream();
					videoSegmentAnalyzer.propagatePlaytime(startupTime, segmentChosen.getVideoEvent(), videoStream);

					segmentChosen.getVideoEvent().setPlayTime(startupTime);
					if (!allListUserEvent.isEmpty()) {
						segmentChosen.getVideoEvent().setPlayRequestedTime(Double.valueOf(playRequestedTime.getText()));
						videoStream.setPlayRequestedTime(Double.valueOf(playRequestedTime.getText()));
					}
					videoStream.getManifest().setDelay(startupTime - segmentChosen.getVideoEvent().getEndTS());
					videoStream.getManifest().setStartupVideoEvent(segmentChosen.getVideoEvent());
					videoStream.getManifest().setStartupDelay(segmentChosen.getVideoEvent().getSegmentStartTime()
							- videoStream.getManifest().getRequestTime());

					LOGGER.info(String.format("Segment playTime = %.03f", segmentChosen.getVideoEvent().getPlayTime()));
					startTimeField.setText(String.format("%.03f", segmentChosen.getVideoEvent().getPlayTime()));
					revalidate();
					for (VideoStream stream : streamingVideoData.getVideoStreamMap().values()) {
						if (stream.equals(videoStream)) {
							stream.setSelected(true);
						} else {
							stream.setSelected(false);
						}
					}
				} else {
					for (VideoStream stream : streamingVideoData.getVideoStreamMap().values()) {
						if (stream.isSelected()) {
							stream.getManifest().setDelay(startupTime - segmentChosen.getVideoEvent().getEndTS());
						}
					}
				}

				AROTraceData aroTraceData = mainFrame.getController().getTheModel();

				streamingVideoData.scanVideoStreams();

				IVideoBestPractices videoBestPractices = ContextAware.getAROConfigContext()
						.getBean(IVideoBestPractices.class);
				videoBestPractices.analyze(aroTraceData);

				// mainFrame.getDiagnosticTab().getGraphPanel().refresh(aroTraceData);
				getGraphPanel().setTraceData(aroTraceData);

				// StartupDelay calculations
				AROTraceData traceData = vcPlot.refreshPlot(
						getGraphPanel().getSubplotMap().get(ChartPlotOptions.VIDEO_CHUNKS).getPlot(),
						getGraphPanel().getTraceData(), startupTime, segmentChosen.getVideoEvent());

				getGraphPanel().setTraceData(traceData);
				mainFrame.getVideoTab().refreshLocal(traceData);
				VideoManifestPanel videoManifestPanel = segmentTablePanel.getVideoManifestPanel();
				videoManifestPanel.reload(segmentTablePanel.getAnalyzerResult(), segmentChosen.getVideoEvent());

				mainFrame.refreshBestPracticesTab();
				dispose();
			}
		} catch (Exception ex) {
			LOGGER.error("Error generating video chunk and buffer plots", ex);
			MessageDialogFactory.showMessageDialog(parentPanel, "Error in drawing buffer graphs",
					"Failed to generate buffer plots", JOptionPane.ERROR_MESSAGE);
		}
	}

	public GraphPanel getGraphPanel() {
		return this.parentPanel;
	}

	public ActionListener getSetButtonListener(final VideoChunksPlot vcPlot) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ComboManifest comboBoxItem = (ComboManifest) jcb.getSelectedItem();
				if ((!allListUserEvent.isEmpty())
						&& Double.valueOf(playRequestedTime.getText()) > Double.valueOf(startTimeField.getText())) {
					showUserEventWarningMessage();
					return;
				}
				if (segmentChosen != null && segmentChosen.isSelected && (comboBoxItem.getVideoStream() != null)) {
					dispose();
					try {
						LOGGER.info("startTimeField :" + startTimeField.getText());
						setStartTime(Double.valueOf(startTimeField.getText()));
					} catch (NumberFormatException e1) {
						// covers user entered bad data, just go with getStartTime()
						LOGGER.error("NumberFormatException: ", e1);
					}
					launchStartupCalculations(getStartTime());
				} else {
					showWarningMessage();
				}
			}
		};
	}

	class ComboManifest {
		VideoStream videoStream;
		String manifestName;

		public ComboManifest() {
			this.manifestName = resourceBundle.getString("sliderdialog.manifestselection.default");
		}

		public ComboManifest(VideoStream videoStream) {
			if (videoStream.isSelected()) {
				this.videoStream = videoStream;
				this.manifestName = videoStream.getManifest().getVideoName() + ", "
						+ resourceBundle.getString("sliderdialog.manifest.segmentcount")
						+ String.valueOf(videoStream.getVideoEventList().size());
			}
		}

		public VideoStream getVideoStream() {
			return this.videoStream;
		}

		@Override
		public String toString() {
			return manifestName;
		}
	}

	private class UserEventDataModel extends DefaultTableModel {

		@Override
		public void setValueAt(Object aValue, int row, int col) {
			if (col == CHECK_COL) {
				for (int r = 0; r < getRowCount(); r++) {
					super.setValueAt(false, r, CHECK_COL);
				}
			}
			super.setValueAt(aValue, row, col);
		}

		@Override
		public Class<?> getColumnClass(int col) {
			if (col == CHECK_COL) {
				return getValueAt(0, CHECK_COL).getClass();
			}
			return super.getColumnClass(col);
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col == CHECK_COL;
		}
	}

	private static class UserEventJTableItem {
		private String value;
		private UserEvent userEvent;
		private boolean isSelected;
		private int row;

		UserEventJTableItem(UserEvent userEvent, int row) {
			StringBuffer sb = new StringBuffer();
			sb.append("User Event type: ").append(userEvent.getEventType());
			sb.append(", Time: ").append(String.format("%.2f", userEvent.getPressTime()));

			this.value = sb.toString();
			this.row = row;
			this.userEvent = userEvent;
		}

		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}

		public UserEvent getUserEvent() {
			return userEvent;
		}

		public int getRow() {
			return row;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	private static class JTableItems {
		private String value;
		private VideoEvent videoEvent;
		private boolean isSelected;
		private int row;

		JTableItems(VideoEvent videoEvent, int row) {
			StringBuffer sb = new StringBuffer();
			sb.append("Segment ID: ").append(videoEvent.getSegmentID());
			sb.append(", Quality: ").append(videoEvent.getQuality());
			sb.append(", DL Time Range: ")
					.append(String.format("%.2f - %.2f", videoEvent.getStartTS(), videoEvent.getEndTS()));
			if (videoEvent.getPlayTime() != 0) {
				sb.append(", Play: ").append(String.format("%.2f", videoEvent.getPlayTime()));
			} else {
				sb.append(", mSec: ").append(String.format("%.2f", videoEvent.getSegmentStartTime()));
			}
			this.value = sb.toString();
			this.row = row;
			this.videoEvent = videoEvent;
		}

		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}

		public VideoEvent getVideoEvent() {
			return videoEvent;
		}

		public int getRow() {
			return row;
		}

		@Override
		public String toString() {
			return value;
		}
	}

}