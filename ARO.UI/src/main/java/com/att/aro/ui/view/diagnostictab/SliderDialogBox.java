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
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoFormat;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.diagnostictab.plot.VideoChunksPlot;
import com.att.aro.ui.view.video.IVideoPlayer;

public class SliderDialogBox extends JDialog {
	private static final Logger LOGGER = LogManager.getLogger(SliderDialogBox.class.getName());
	private static final long serialVersionUID = 1L;
	private static final int SLIDERDIALOG_COLUMN_COUNT = 2;
	private static final int MAXIMUM_EVENT_PER_SECOND = 20;
	private long eventTimeStamp = 0;
	private int eventCount =0;
	private JSlider slider;
	private JButton okButton;
	private int maxValue;
	private int minValue;
	private IVideoPlayer player;
	private double startTime;
	private VideoChunksPlot vcPlot;
	private MainFrame mainFrame;
	private GraphPanel parentPanel;
	private BufferedImage resizedThumbnail;
	private Map<Integer, VideoEvent> chunkSelectionList = new TreeMap<>();
	private JTable jTable;
	private List<JTableItems> listSegments = new ArrayList<>();
	private List<VideoEvent> allChunks = new ArrayList<>();
	private JTableItems segmentChosen;
	private int selectedIndex = 0;
	private JButton plusTunerBtn;
	private JButton minusTunerBtn;
	private JPanel panel;
	private JLabel imgLabel = new JLabel();
	private StreamingVideoData streamingVideoData;
	private static ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	private JComboBox<ComboManifest> jcb;
	private DefaultTableModel tableModel;
	private PlotHelperAbstract videoChunkPlotter = ContextAware.getAROConfigContext().getBean("videoChunkPlotterImpl",
			PlotHelperAbstract.class);
	private JTextField startTimeField;

	private static final Logger LOG = LogManager.getLogger(SliderDialogBox.class.getName());
	
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
						+ String.valueOf(videoStream.getVideoEventMap().size());
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

	private static class JTableItems {
		private String value;
		private VideoEvent videoEvent;
		private boolean isSelected;
		private int row;
		
		JTableItems(VideoEvent videoEvent, int row) {
			StringBuffer sb = new StringBuffer();
			sb.append("Segment ID: ").append(videoEvent.getSegmentID());
			sb.append(", Quality: ").append(videoEvent.getQuality());
			sb.append(", DL Time Range: ").append(String.format("%.2f - %.2f", videoEvent.getStartTS(), videoEvent.getEndTS()));
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

	public SliderDialogBox(GraphPanel parentPanel, double maxVideoTime, Map<Integer, VideoEvent> chunks, int indexKey, List<VideoEvent> allChunks) {
		this.parentPanel = parentPanel;
		DiagnosticsTab parent = parentPanel.getGraphPanelParent();
		mainFrame = (MainFrame) parent.getAroView();
		player = parent.getVideoPlayer();
		vcPlot = parentPanel.getVcPlot();
		chunkSelectionList = chunks;
		this.allChunks.addAll(allChunks);
		createSliderDialog(maxVideoTime, player, vcPlot, indexKey);
		if (selectedIndex >= 0 && selectedIndex < jTable.getRowCount()) {
			jTable.setRowSelectionInterval(selectedIndex, selectedIndex);
			jTable.getModel().setValueAt(true, selectedIndex, 0);
			if (videoChunkPlotter.getStreamingVideoData().getStreamingVideoCompiled().getChunkPlayTimeList().keySet() != null) {
				for (VideoEvent veSegment : videoChunkPlotter.getStreamingVideoData().getStreamingVideoCompiled().getChunkPlayTimeList().keySet()) {
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

	public void populateList() {
		for (int index = 0; index < allChunks.size(); index++) {
			VideoEvent ve = allChunks.get(index);
			if (segmentChosen != null && ve.equals(segmentChosen.getVideoEvent())) {
				listSegments.add(segmentChosen);
			} else {
				listSegments.add(new JTableItems(ve, index));
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

	private void updateJTableData() {
		jTable.getColumnModel().getColumn(0).setMaxWidth(30);
		jTable.setTableHeader(null);
		for (int i = 0; i < listSegments.size(); i++) {
			JTableItems item = listSegments.get(i);
			jTable.getModel().setValueAt(item.isSelected, i, 0);
			jTable.getModel().setValueAt(item.value, i, 1);
		}
	}

	@SuppressWarnings("serial")
	public void createSliderDialog(double max, IVideoPlayer player, final VideoChunksPlot vcPlot, int indexKey) {
		setUndecorated(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(300, 200, 1000, 1000);
		setResizable(false);
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);
		streamingVideoData = mainFrame.getController().getTheModel().getAnalyzerResult().getStreamingVideoData();
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
		jcb.addActionListener(new ActionListener() {
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
		});
		panel.add(jcb);
		JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		labelPanel.add(new JLabel(resourceBundle.getString("sliderdialog.segmentlist.label")));
		panel.add(labelPanel);
		JPanel comboPanel = new JPanel(new BorderLayout(5, 0));
		comboPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
		comboPanel.setSize(500, 100);
		GridBagConstraints constraint = new GridBagConstraints();
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
		//Listener for mouse click
		jTable.addMouseListener(getCheckboxListener());

		// Listener for list selection on jTable
		jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
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
							? itemObject.getVideoEvent().getPlayTime() : itemObject.getVideoEvent().getEndTS();
					slider.setValue((int) Math.round(timestamp * 25));
					panel.setSize(panel.getPreferredSize());
					panel.revalidate();
					JDialog parentDialog = (JDialog) panel.getRootPane().getParent();
					parentDialog.setSize(parentDialog.getPreferredSize());
					parentDialog.revalidate();
				}
				selectSegment();
			}
		});
		
		JScrollPane listScrollPanel = new JScrollPane(jTable);
		listScrollPanel.setPreferredSize(new Dimension(500, 100));
		comboPanel.add(listScrollPanel);
		if (manifestIdxSelected > 1) {
			JLabel warningLabel = new JLabel(resourceBundle.getString("sliderdialog.manifest.warning"));
			comboPanel.add(warningLabel, BorderLayout.EAST);
		}
		panel.add(comboPanel);
		JPanel imgLabelPanel = new JPanel(new BorderLayout());
		imgLabelPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
		imgLabelPanel.add(imgLabel);
		imgLabelPanel.setSize(imgLabelPanel.getPreferredSize());
		panel.add(imgLabelPanel);
		JPanel sliderBoxPanel = new JPanel(new BorderLayout());
		sliderBoxPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 1));
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new GridBagLayout());
		constraint = new GridBagConstraints();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.weightx = 2;
		double initialValue = getPlayer().getVideoOffset();
		this.maxValue = setMaxMinValue(max + initialValue, true);
		this.minValue = setMaxMinValue(initialValue, false);
		this.player = player;
		
		// define slider
		slider = new JSlider(JSlider.HORIZONTAL, minValue, maxValue, minValue);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.addChangeListener(getSliderListener());
		slider.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				ComboManifest comboBoxItem = (ComboManifest) jcb.getSelectedItem();
				if (segmentChosen == null 
						|| (!segmentChosen.isSelected) 
						&& (comboBoxItem.getVideoStream() == null)) {
					showWarningMessage();
				}
			}
		});	
		sliderPanel.add(slider, constraint);
		
		startTimeField = getStartTimeDisplay();
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 1;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 0, 0, 1);
		constraint.weightx = 0.35;
		sliderPanel.add(startTimeField, constraint);
		
		minusTunerBtn = getTunerButton("-");
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 2;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 0, 0, 1);
		constraint.weightx = 0.35;
		sliderPanel.add(minusTunerBtn, constraint);
		
		plusTunerBtn = getTunerButton("+");
		constraint.fill = GridBagConstraints.HORIZONTAL;
		constraint.gridx = 3;
		constraint.gridy = 0;
		constraint.insets = new Insets(0, 0, 0, 2);
		constraint.weightx = 0.35;
		sliderPanel.add(plusTunerBtn, constraint);
		
		sliderBoxPanel.add(sliderPanel);
		
		panel.add(sliderBoxPanel);
		
		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 220, 1, 220));
		
		// build the 'Set' JButton
		okButton = new JButton("Set");
		okButton.addActionListener(getSetButtonListener(vcPlot));
		btnPanel.add(okButton);
		panel.add(btnPanel);
		
		panel.setSize(panel.getPreferredSize());
		panel.validate();
	}

	/**
	 * Tracks slider movement
	 * @return
	 */
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

	public ActionListener getSetButtonListener(final VideoChunksPlot vcPlot) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ComboManifest comboBoxItem = (ComboManifest) jcb.getSelectedItem();		
				if (segmentChosen != null && segmentChosen.isSelected && (comboBoxItem.getVideoStream() != null)) {
					dispose();
					
					try {
						LOGGER.info("startTimeField :" + startTimeField.getText());
						setStartTime(Double.valueOf(startTimeField.getText()));
					} catch (NumberFormatException e1) {
						// covers user entered bad data, just go with getStartTime()
					}
					
					try {
						if (getStartTime() >= segmentChosen.getVideoEvent().getEndTS() && segmentChosen.isSelected) {
							ComboManifest selectedStream = (ComboManifest) jcb.getSelectedItem();
							if (selectedStream.getVideoStream() != null) {
								VideoStream videoStream = selectedStream.getVideoStream();

								segmentChosen.getVideoEvent().setPlayTime(getStartTime());

								videoStream.getManifest().setDelay(getStartTime() - segmentChosen.getVideoEvent().getEndTS());
								videoStream.getManifest().setStartupVideoEvent(segmentChosen.getVideoEvent());
								videoStream.getManifest().setStartupDelay(segmentChosen.getVideoEvent().getSegmentStartTime() - videoStream.getManifest().getRequestTime());

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
										stream.getManifest().setDelay(getStartTime() - segmentChosen.getVideoEvent().getEndTS());
									}
								}
							}
								
							AROTraceData aroTraceData = mainFrame.getController().getTheModel();
							
							streamingVideoData.scanVideoStreams();
							
							IVideoBestPractices videoBestPractices = ContextAware.getAROConfigContext().getBean(IVideoBestPractices.class);
							videoBestPractices.analyze(aroTraceData);
							
							mainFrame.getDiagnosticTab().getGraphPanel().refresh(aroTraceData);
							getGraphPanel().setTraceData(aroTraceData);
							AROTraceData traceData = vcPlot.refreshPlot(getGraphPanel().getSubplotMap().get(ChartPlotOptions.VIDEO_CHUNKS).getPlot()
																			, getGraphPanel().getTraceData()
																			, getStartTime()
																			, segmentChosen.getVideoEvent());
							getGraphPanel().setTraceData(traceData);
							mainFrame.getVideoTab().refresh(traceData);
						}
					} catch (Exception ex) {
						LOGGER.error("Error generating video chunk and buffer plots", ex);
						MessageDialogFactory.showMessageDialog(parentPanel, "Error in drawing buffer graphs", "Failed to generate buffer plots", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					showWarningMessage();
				}
			}
		};
	}

	public void showWarningMessage(){
		JOptionPane.showMessageDialog(SliderDialogBox.this, "Please select a segment and a manifest first in order to set the start up delay.", "Warning", JOptionPane.WARNING_MESSAGE);
	}
	
	public MouseAdapter getCheckboxListener() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectSegment();
			}
		};
	}
	
	public void selectSegment(){
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
	
	public JTextField getStartTimeDisplay() {
		JTextField startTimeField = new JTextField("       0");
		startTimeField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// enter/return has been pressed in startTimeField
				try {
					double sec = Double.valueOf(e.getActionCommand());
					if (sec < 0) {
						// Handle user entered parsable bad data by replacing with the original data
						startTimeField.setText(Double.toString(getStartTime()));
					}
					setStartTime(sec);
					slider.setValue((int) sec * 25);
				} catch (NumberFormatException e1) {
					// Handle user entered non-parsable bad data by replacing with the original data
					startTimeField.setText(Double.toString(getStartTime()));
				}
			}
		});
		return startTimeField;
	}

	public GraphPanel getGraphPanel() {
		return this.parentPanel;
	}

	public int getMaxValue() {
		return maxValue;
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

	public IVideoPlayer getPlayer() {
		return this.player;
	}

	public void setStartTime(double startTime) {
		LOG.info(String.format("SET startTime :%.03f" , startTime));
		this.startTime = startTime;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public JButton getTunerButton(String btnName) {
		JButton tunerBtn = new JButton(btnName);
		tunerBtn.setPreferredSize(new Dimension(5, 15));
		tunerBtn.addActionListener(new ActionListener() {
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
		});
		return tunerBtn;
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

}
