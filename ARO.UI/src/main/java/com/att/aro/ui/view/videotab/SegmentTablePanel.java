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
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.IVideoBestPractices;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.UserEvent;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup.ValidationStartup;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.StartupDelayDialog;
import com.att.aro.ui.view.video.IVideoPlayer;

import lombok.Getter;

public class SegmentTablePanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LogManager.getLogger(SegmentTablePanel.class);
	private static IFileManager fileManager = (IFileManager) ContextAware.getAROConfigContext().getBean("fileManager");

	private JPanel hiddenPanel;
	
	@Getter
	private VideoStream videoStream;
	private BasicArrowButton arrowButton;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private JLabel lbl;

	private SharedAttributesProcesses aroView;
	private int rowCount;
	private JScrollPane videoTableScrollPane = null;
	private JScrollPane audioTableScrollPane = null;
	private JScrollPane ccTableScrollPane = null;
	private JPanel titlePanel;
	
	@Getter
	private JCheckBox enableCheckBox;

	private static final int HEIGHT_MAC = 18;
	private static final int HEIGHT_LINUX = 23;
	private static final int HEIGHT_WIN = 28;
	private int tableHeight = HEIGHT_MAC;
	private int textAdjust = 0;
	private static final int MAX_HEIGHT = 400;
	private static final int TEXT_FUDGE = 5;
	private static final int COL_MAX_WIDTH = 600;

	public static final String VIDEO_TABLE_NAME = "Video";
	public static final String AUDIO_TABLE_NAME = "Audio";
	public static final String CAPTION_TABLE_NAME = "Captioning";
	private JButton uploadButton;

	private TraceDirectoryResult traceDirectoryResult;

	/**
	 * collection of tables for use when resizing & toggling
	 */
	@Getter
	HashMap<String, JTable> streamTables = new HashMap<>();

	private JPanel videoTablePanel;
	private JPanel audioTablePanel;
	private JPanel captionTablePanel;

	private JCheckBox checkBoxVideo;
	private JCheckBox checkBoxAudio;
	private JCheckBox checkBoxCC;

	private StreamingVideoData streamingVideoData;
	@Getter
	private AROTraceData analyzerResult;

	private JDialog dialog;
	private int viewIndex;


	/**
	 * @wbp.parser.constructor
	 */
	public SegmentTablePanel(VideoStream videoStream, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute,
			AROTraceData analyzerResult, SharedAttributesProcesses aroView, VideoManifestPanel videoManifestPanel, int viewIndex) {
		this(true, aroView, videoStream, analyzerResult, videoManifestPanel, viewIndex);

		streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;
		updateHiddenPanelContent(true);
	}

	public SegmentTablePanel(boolean manifestFlag, SharedAttributesProcesses aroView, VideoStream videoStream,
			AROTraceData analyzerResult, VideoManifestPanel videoManifestPanel, int viewIndex) {
		streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		this.aroView = aroView;
		this.videoStream = videoStream;
		this.analyzerResult = analyzerResult;
		this.videoManifestPanel = videoManifestPanel;
		this.viewIndex = viewIndex;
		setLayout(new BorderLayout());

		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		add(getTitleButton(), BorderLayout.NORTH);
		if (videoStream != null && (videoStream.getPlayRequestedTime() != null || videoStream.getVideoPlayBackTime() != null)) {
			
			defineStartupDelayButton(videoStream);
			enableCheckBox.setSelected(videoStream.isCurrentStream());
			this.videoStream.setSelected(videoStream.isCurrentStream());

		} else {
			if (videoStreamCollection.size() > 1) {
				enableCheckBox.setSelected(false);
			}
		}
		refresh(checkBoxVideo, checkBoxAudio);
		hiddenPanel = getHiddenPanel();
		add(hiddenPanel, BorderLayout.SOUTH);
		hiddenPanel.setVisible(false);
		arrowButton.addActionListener(this);
		updateHiddenPanelContent(false);
		hiddenPanel.addComponentListener(resizeListener());

	}

	public ComponentListener resizeListener() {
		return new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				int newWidth = (int) (e.getComponent().getWidth() * .45);

				Dimension vSize = videoTablePanel.getPreferredSize();
				vSize.setSize(newWidth, vSize.getHeight());
				videoTablePanel.setPreferredSize(vSize);

				if (audioTablePanel != null) {
					Dimension aSize = audioTablePanel.getPreferredSize();
					aSize.setSize(newWidth, aSize.getHeight());
					audioTablePanel.setPreferredSize(vSize);
				}
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}

		};
	}

	/**
	 * 
	 * @return
	 */
	private Component getTitleButton() {

		titlePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		titlePanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		titlePanel.setSize(500, 100);

		arrowButton = new BasicArrowButton(SwingConstants.EAST);

		lbl = new JLabel();
		lbl.setFont(new Font("accordionLabel", Font.ITALIC, 12));

		titlePanel.add(getCheckBoxStreamEnable());
		titlePanel.add(arrowButton);
		titlePanel.add(lbl);

		titlePanel.add(getStartupDialogButton());
		defineStartupDelayButton(videoStream);
		
		if (videoStream.getMissingSegmentCount() > 0) {
			titlePanel.add(getSessionProblems(videoStream.getMissingSegmentCount()));
		}

		titlePanel.add(createCheckBoxVideo());
		titlePanel.add(createCheckBoxAudio());
		titlePanel.add(createCheckBoxCC());

		if (videoStream.getVideoPlayBackTime() != null) {
			titlePanel.add(new JLabel(ResourceBundleHelper.getMessageString("video.tab.startUpTime") + String.format("%.3f", videoStream.getVideoPlayBackTime())));
		}

		return titlePanel;
	}

	private Component getSessionProblems(int missingSegmentCount) {
		JLabel problemMessage = new JLabel(String.format("There %s %d segment gap%s",
				missingSegmentCount == 1 ? "is" : "are", missingSegmentCount, missingSegmentCount == 1 ? "" : "s"));
		problemMessage.setForeground(Color.RED);
		problemMessage.setFont(problemMessage.getFont().deriveFont(Font.BOLD, 14f));
		return problemMessage;
	}

	private Component getStartupDialogButton() {
		startupButton = new JButton("Set StartupDelay");
		startupButton.setForeground(Color.RED);
		startupButton.setOpaque(true);
		startupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startupButton.setForeground(Color.GREEN);
				launchStartUpDelayDialog();
			}
		});
		return startupButton;
	}

	/**
	 * Change color & text if startup delay has been set.
	 * 
	 * @param videoStream
	 */
	private void defineStartupDelayButton(VideoStream videoStream) {
		if (videoStream.getVideoStreamStartup() != null
				&& videoStream.getVideoStreamStartup().getValidationStartup().equals(ValidationStartup.USER)) {

			startupButton.setForeground(Color.BLUE);
			startupButton.setText("Edit StartupDelay");
		} else {
			startupButton.setForeground(Color.RED);
		}
	}


	
	public void launchStartUpDelayDialog() {

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews("StartupDelayDialog");
		
		IVideoPlayer player = aroView.getVideoPlayer();
		double maxDuration = player.getDuration();
		List<UserEvent> userEventList = analyzerResult.getAnalyzerResult().getTraceresult().getUserEvents();
		if (maxDuration >= 0) {
			selectVideoStreamWithRefresh(videoStream);
			try {
				dialog = new StartupDelayDialog(aroView.getGraphPanel(), maxDuration, videoStream, userEventList, this, viewIndex);
				dialog.pack();
				dialog.setSize(dialog.getPreferredSize());
				dialog.validate();
				dialog.setModalityType(ModalityType.APPLICATION_MODAL);
				dialog.setVisible(true);
			} catch (Exception e) {
				LOG.error("Exception in StartupDelayDialog:", e);
				new MessageDialogFactory().showErrorDialog(null, ResourceBundleHelper.getMessageString("startupdelay.error.message"));
			}

		}
	}

	public void selectVideoStreamWithRefresh(VideoStream selectedStream) {
		streamingVideoData.getVideoStreamMap().entrySet().stream().forEach(x -> {
			x.getValue().setSelected(x.getValue().equals(selectedStream));
		});
	}

	private Component createCheckBoxVideo() {
		checkBoxVideo = new JCheckBox(VIDEO_TABLE_NAME);
		checkBoxVideo.setVisible(!CollectionUtils.isEmpty(videoStream.getVideoEventMap())
				&& !CollectionUtils.isEmpty(videoStream.getAudioEventMap()));
		checkBoxVideo.setSelected(false);
		checkBoxVideo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!checkBoxVideo.isSelected() && !checkBoxAudio.isSelected()) {
					checkBoxAudio.setSelected(true);
				}
				refresh(checkBoxVideo, checkBoxAudio);
				refreshSegmentPanel();

			}
		});
		return checkBoxVideo;
	}

	private Component createCheckBoxAudio() {
		checkBoxAudio = new JCheckBox(AUDIO_TABLE_NAME);
		checkBoxAudio.setVisible(!CollectionUtils.isEmpty(videoStream.getAudioEventMap())
				&& !CollectionUtils.isEmpty(videoStream.getVideoEventMap()));
		checkBoxAudio.setSelected(false);
		checkBoxAudio.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!checkBoxVideo.isSelected() && !checkBoxAudio.isSelected()) {
					checkBoxVideo.setSelected(true);
				}
				refresh(checkBoxVideo, checkBoxAudio);
				refreshSegmentPanel();
			}
		});
		return checkBoxAudio;
	}

	private Component createCheckBoxCC() {
		checkBoxCC = new JCheckBox("Captioning");
		checkBoxCC.setVisible(!CollectionUtils.isEmpty(videoStream.getCcEventMap()));
		checkBoxCC.setSelected(true);
		checkBoxCC.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				audioTablePanel.setVisible(checkBoxCC.isSelected());
			}
		});
		return checkBoxCC;
	}


	private Component getCheckBoxStreamEnable() {
		enableCheckBox = new JCheckBox();
		videoStreamCollection = analyzerResult.getAnalyzerResult().getStreamingVideoData().getVideoStreamMap().values();
		boolean selected = videoStream.getVideoEventMap() != null ? true : false;
		if (!selected 
				|| videoStream.getVideoEventMap().isEmpty()
				|| ((VideoEvent) videoStream.getVideoEventMap().values().toArray()[0]).getSegmentID() < 0) {
			enableCheckBox.setEnabled(false);
		} else {
			boolean selectCheckBox = videoStreamCollection.size() == 1;
			videoStream.setSelected(selectCheckBox);
			enableCheckBox.setSelected(selectCheckBox);
			if (selectCheckBox) {
				refreshStream();
			}
			enableCheckBox.addActionListener(addActionListener());
		}
		return enableCheckBox;
	}

	private ActionListener addActionListener() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (e.getSource().getClass().equals(JCheckBox.class)) {
					if (((JCheckBox) e.getSource()).isSelected()) {
						videoStream.setSelected(true);
						refreshStream();
						reAnalyze();
					} else {
						videoStream.setSelected(true);
						enableCheckBox.setSelected(true);
					}
				}
			}
		};
	}

	public void refreshStream() {
		videoStream.setSelected(true);
		for (VideoStream stream : videoStreamCollection) {
			if (!stream.equals(videoStream)) {
				toggleStream(stream, false);
			} else {
				toggleStream(stream, true);
			}
		}
	}

	private void toggleStream(VideoStream stream, boolean isCurrentStream) {
		stream.setSelected(isCurrentStream);
		enableCheckBox.setSelected(isCurrentStream);
		stream.setCurrentStream(isCurrentStream);
	}


	public void updateTitleButton(AROTraceData analyzerResult) {
		if (titlePanel != null) {
			if (analyzerResult.getAnalyzerResult() != null) {
				streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();

				for (VideoStream manifest : streamingVideoData.getVideoStreamMap().values()) {

					if (manifest.equals(videoStream) && ((!videoStream.getVideoEventsBySegment().isEmpty())
							&& ((VideoEvent) videoStream.getVideoEventsBySegment().toArray()[0]).getSegmentID() >= 0)) {

						videoStream.setSelected(manifest.isSelected());
						enableCheckBox.setSelected(videoStream.isSelected());
						streamingVideoData.setValidatedCount(false);
						break;
					}
				}

				if (streamingVideoData.getValidatedCount()) {
					streamingVideoData.scanVideoStreams();
				}
			}
		}
	}

	private IVideoBestPractices videoBestPractices = ContextAware.getAROConfigContext()
			.getBean(IVideoBestPractices.class);

	private int tblHeight;

	@Getter
	private VideoManifestPanel videoManifestPanel;

	private JButton startupButton;

	@Getter
	private Collection<VideoStream> videoStreamCollection;

	protected void reAnalyze() {
		StreamingVideoData streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		if (streamingVideoData != null) {
			streamingVideoData.scanVideoStreams();
		}
		((MainFrame) aroView).getDiagnosticTab().getGraphPanel().refresh(analyzerResult);
		analyzerResult = videoBestPractices.analyze(analyzerResult);
		((MainFrame) aroView).getDiagnosticTab().getGraphPanel().setTraceData(analyzerResult);
		((MainFrame) aroView).getVideoTab().refreshLocal(analyzerResult, false);
		((MainFrame)((MainFrame) aroView).getDiagnosticTab().getGraphPanel().getGraphPanelParent().getAroView()).refreshBestPracticesTab();
		
		if (!videoStream.getVideoEventMap().isEmpty()) {
			if (checkBoxVideo != null && checkBoxAudio != null
					&& (checkBoxVideo.isVisible() || checkBoxAudio.isVisible())) {
				refresh(checkBoxVideo, checkBoxAudio);
			} else {
				refresh(null, null);
			}
		}
	}

	private void updateHiddenPanelContent(boolean manifestFlag) {
		String text = "";
		tableHeight = HEIGHT_MAC;
		textAdjust = 0;
		if (Util.isWindowsOS()) {
			tableHeight = HEIGHT_WIN;
			textAdjust = TEXT_FUDGE;
		} else if (Util.isLinuxOS()) {
			tableHeight = HEIGHT_LINUX;
			textAdjust = TEXT_FUDGE + 3;
		}

		if (manifestFlag) {
			if (videoStream.getVideoEventsBySegment() != null) {
				text = (!videoStream.isValid())
						? MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"), viewIndex,
								videoStream.getManifest().getVideoName())
						: MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.manifest.name"), viewIndex,
								videoStream.getManifest().getVideoName());
			} else {
				text = MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"), viewIndex,
						videoStream.getManifest().getVideoName());
			}
			lbl.setText(text + ", segment count:" + videoStream.getVideoEventMap().size());

			// add the chunk/segment tables
			if (!CollectionUtils.isEmpty(videoStream.getVideoEventMap())) {
				videoTableScrollPane = new JScrollPane();
				videoTablePanel = getStreamTable(VIDEO_TABLE_NAME, videoStream.getVideoEventMap(),
						videoTableScrollPane);
				checkBoxVideo.setSelected(true);
				hiddenPanel.add(videoTablePanel, new GridBagConstraints(1, 1, 1, 2, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
			}
			// AUDIO
			if (!CollectionUtils.isEmpty(videoStream.getAudioEventMap())) {
				audioTableScrollPane = new JScrollPane();
				audioTablePanel = getStreamTable(AUDIO_TABLE_NAME, videoStream.getAudioEventMap(),
						audioTableScrollPane);
				checkBoxAudio.setSelected(true);
				hiddenPanel.add(audioTablePanel, new GridBagConstraints(2, 2, 1, 2, 1.0, 1.0,
						GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
			}
			// Closed Caption / subtitles
			if (!CollectionUtils.isEmpty(videoStream.getCcEventMap())) {
				ccTableScrollPane = new JScrollPane();
				captionTablePanel = getStreamTable(CAPTION_TABLE_NAME, videoStream.getCcEventMap(), ccTableScrollPane);
				checkBoxCC.setSelected(true);
				hiddenPanel.add(captionTablePanel, new GridBagConstraints(3, 2, 1, 2, 1.0, 1.0,
						GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
			}
		}
	}

	private class MultiLineTableHeaderRenderer extends JTextArea implements TableCellRenderer {
		private static final long serialVersionUID = 2526260641157677673L;

		public MultiLineTableHeaderRenderer() {
			super(2, 1);
			setFont(new Font(Font.SERIF, Font.PLAIN, 11));
			setEditable(false);
			setLineWrap(true);
			setOpaque(false);
			setFocusable(false);
			setWrapStyleWord(true);
			LookAndFeel.installBorder(this, "TableHeader.cellBorder");
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			int width = table.getColumnModel().getColumn(column).getWidth();
			setText((String) value);
			setSize(width, getPreferredSize().height);
			return this;
		}
	}

	private JPanel getStreamTable(String title, SortedMap<String, VideoEvent> eventList, JScrollPane tableScrollPane) {
		// Do not resize columns or setPreferredWidth in this method
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		Collection<VideoEvent> videoEventList = eventList.values();
		rowCount = videoEventList.size();
		TableModel tableModel = new SegmentTableModel(videoEventList, videoStream.getPlayRequestedTime() != null ? videoStream.getPlayRequestedTime() : 0.0);

		JTable jTable;
		jTable = new JTable(tableModel);
		jTable.setName(title);
		streamTables.put(title, jTable);
		jTable.setGridColor(Color.LIGHT_GRAY);

		for (int idx = 0; idx < jTable.getColumnCount() - 2; idx++) {
			jTable.getColumnModel().getColumn(idx).setCellRenderer(rightRenderer);
		}

		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.TRACK)).setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.TCP_STATE)).setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.CHANNELS)).setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.DOWNLOAD_DELAY)).setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.PLAYBACK_DELAY)).setCellRenderer(centerRenderer);

		JTableHeader header = jTable.getTableHeader();
		header.setDefaultRenderer(new MultiLineTableHeaderRenderer());

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		if (rowCount > tableHeight) {
			tableScrollPane.setPreferredSize(new Dimension(0, MAX_HEIGHT));
		} else {
			int rowHeight = jTable.getRowHeight();
			tblHeight = 100 + (eventList.size() * (rowHeight + 3));
			tableScrollPane.setPreferredSize(new Dimension(0, tblHeight));
			tableScrollPane.setSize(new Dimension(0, tblHeight));
			panel.setPreferredSize(new Dimension(0, tblHeight));
			panel.setMinimumSize(new Dimension(0, tblHeight));
			panel.setSize(new Dimension(0, tblHeight));
		}

		tableScrollPane.setViewportView(jTable);
		panel.setLayout(new BorderLayout());
		panel.add(header, BorderLayout.NORTH);
		panel.add(tableScrollPane, BorderLayout.CENTER);

		// Sorter for jTable
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(jTable.getModel());
		jTable.setRowSorter(rowSorter);

		refreshSegmentPanel();

		int colCount = rowSorter.getModel().getColumnCount();
		for (int column = 0; column < colCount; column++) {
			rowSorter.setComparator(column, new TableSortComparator(column, "-"));
		}

		jTable.addMouseListener(
				streamTableClickHandler(tableModel, jTable, jTable.getColumnModel().getColumnIndex("SessionLink")));
		return panel;
	}

	public MouseAdapter streamTableClickHandler(TableModel tableModel, JTable jTable, int sessionLinkColumn) {
		return new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (diagnosticsOverviewRoute != null && e.getClickCount() == 2) {
					if (e.getSource() instanceof JTable) {
						int selectionIndex = ((JTable) e.getSource()).getSelectedRow();
						Session session = (Session) jTable.getValueAt(selectionIndex, sessionLinkColumn);
						diagnosticsOverviewRoute.updateDiagnosticsTab(session);
					}
				}
			}
		};
	}

	private void setColumnWidthDefaults(JTable jTable) {
		// hidden
		setColumnMinMaxWidth(jTable, SegmentTableModel.SESSION_LINK, false, 0, 0);

		// visible/hidden
		setColumnMinMaxWidth(jTable, SegmentTableModel.DL_START_TIME, true, 65, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.BIT_RATE, true, 40, 55);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TOTAL_BYTES, true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TCP_STATE, true, 40, 60);
		setColumnMinMaxWidth(jTable, SegmentTableModel.DURATION, true, 80, COL_MAX_WIDTH);
		if (VIDEO_TABLE_NAME.equals(jTable.getName())) {
			setColumnMinMaxWidth(jTable, SegmentTableModel.RESOLUTION, true, 55, 60);
			if (streamTables.size() == 1) {
				setColumnMinMaxWidth(jTable, SegmentTableModel.CHANNELS, true, 36, 36);
				setColumnMinMaxWidth(jTable, SegmentTableModel.CONTENT, true, 50, 50);
			}
			setColumnMinMaxWidth(jTable, SegmentTableModel.DOWNLOAD_DELAY, true, 55, COL_MAX_WIDTH);
			setColumnMinMaxWidth(jTable, SegmentTableModel.PLAYBACK_DELAY, true, 55, COL_MAX_WIDTH);
		} else if (AUDIO_TABLE_NAME.equals(jTable.getName())) {
			setColumnMinMaxWidth(jTable, SegmentTableModel.CHANNELS, true, 36, 36);
			setColumnMinMaxWidth(jTable, SegmentTableModel.RESOLUTION, false, 0, 0);
		}

		// always visible
		setColumnMinMaxWidth(jTable, SegmentTableModel.SEGMENT_NO, false, 50, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.DL_END_TIME, true, 65, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TRACK, true, 36, 36);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TCP_SESSION, true, 50, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.STALL_TIME, true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.PLAYBACK_TIME, true, 50, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.SEGMENT_POSITION, true, 50, COL_MAX_WIDTH);
	}

	/**
	 * Currently, this method is only called for a combined view of audio/video
	 * tables
	 * 
	 * @param jTable
	 */
	public void setColumnWidthMinimized(JTable jTable) {
		// hidden
		// all must be set to false (not resizable)
		setColumnMinMaxWidth(jTable, SegmentTableModel.SESSION_LINK, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.DL_START_TIME, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.DURATION, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TOTAL_BYTES, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TCP_SESSION, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.CHANNELS, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.CONTENT, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.DOWNLOAD_DELAY, false, 0, 0);
		setColumnMinMaxWidth(jTable, SegmentTableModel.PLAYBACK_DELAY, false, 0, 0);

		if (VIDEO_TABLE_NAME.equals(jTable.getName())) {
			setColumnMinMaxWidth(jTable, SegmentTableModel.BIT_RATE, false, 0, 0);
		} else if (AUDIO_TABLE_NAME.equals(jTable.getName())) {
			setColumnMinMaxWidth(jTable, SegmentTableModel.RESOLUTION, false, 0, 0);
		}
	}

	private void setColumnMinMaxWidth(JTable jTable, String colName, boolean isResizable, int minWidth, int maxWidth) {
		TableColumn column = jTable.getColumnModel().getColumn(jTable.getColumnModel().getColumnIndex(colName));
		column.setMinWidth(minWidth + textAdjust);
		column.setPreferredWidth(minWidth + textAdjust);
		column.setMaxWidth(maxWidth);
	}


	public void autoToggleColumnWidth(JTable jtable) {
		if (checkBoxVideo.isSelected() && checkBoxAudio.isSelected()) {
			setColumnWidthMinimized(jtable);
		} else {
			setColumnWidthDefaults(jtable);
		}
		jtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		if (videoTablePanel != null) {
			videoTablePanel.setVisible(checkBoxVideo.isSelected());
		}
		if (audioTablePanel != null) {
			audioTablePanel.setVisible(checkBoxAudio.isSelected());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SegmentTablePanel segmentTable = (SegmentTablePanel) SwingUtilities.getAncestorOfClass(SegmentTablePanel.class,
				(BasicArrowButton) e.getSource());
		JPanel nestedPanel = segmentTable.getHiddenPanel();

		if (nestedPanel != null) {
			arrowButton = (BasicArrowButton) e.getSource();
			if (arrowButton.getDirection() == SwingConstants.EAST) {
				arrowButton.setDirection(SwingConstants.SOUTH);
				resizeTableScrollPanes(videoTableScrollPane);
				resizeTableScrollPanes(audioTableScrollPane);
				resizeTableScrollPanes(ccTableScrollPane);
				nestedPanel.updateUI();

				nestedPanel.setVisible(true);
				Dimension dim = nestedPanel.getSize();
				dim.height *= 2;
				nestedPanel.setSize(dim);
				checkBoxCC.setEnabled(true);
				refreshSegmentPanel();
			} else {
				arrowButton.setDirection(SwingConstants.EAST);
				nestedPanel.setVisible(false);
				checkBoxCC.setEnabled(false);
			}
			checkBoxVideo.setEnabled(true);
			checkBoxAudio.setEnabled(true);
			aroView.getCurrentTabComponent().revalidate();
			nestedPanel.setVisible(!(arrowButton.getDirection() == SwingConstants.EAST));
		}
	}

	public void resizeTableScrollPanes(JScrollPane tableJScrollPane) {
		if (tableJScrollPane != null) {
			if (rowCount > tableHeight) {
				tableJScrollPane.setPreferredSize(new Dimension(tableJScrollPane.getWidth(), MAX_HEIGHT));
			} else {
				tableJScrollPane.setPreferredSize(new Dimension(tableJScrollPane.getWidth(),
						streamTables.get(VIDEO_TABLE_NAME).getHeight() + tableHeight));
				tableJScrollPane.getBounds().setSize(tableJScrollPane.getWidth(),
						streamTables.get(VIDEO_TABLE_NAME).getHeight() + tableHeight);
			}
		}
	}

	public void setVisible(boolean state) {

		hiddenPanel.setVisible(state);
		if (state) {
			arrowButton.setDirection(SwingConstants.SOUTH);
		} else {
			arrowButton.setDirection(SwingConstants.EAST);
		}
	}

	public JPanel getHiddenPanel() {
		if (hiddenPanel == null) {
			hiddenPanel = new JPanel(new GridBagLayout());
			hiddenPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			hiddenPanel.setBorder(BorderFactory.createEtchedBorder(WIDTH));
		}
		return hiddenPanel;
	}

	// refreshments
	protected void refreshParent() {
		StreamingVideoData streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		if (streamingVideoData != null) {
			streamingVideoData.scanVideoStreams();
		}
		((MainFrame) aroView).getVideoTab().refreshLocal(analyzerResult, false);
	}

	public void refresh(JCheckBox checkBoxVideo, JCheckBox checkBoxAudio) {
		VideoTab videoTab = ((MainFrame) aroView).getVideoTab();
		if (videoStreamCollection.size() > 1) {
			for (VideoStream stream : videoStreamCollection) {
				if (stream.equals(videoStream)) {
					if (stream.isSelected() && stream.isCurrentStream()) {
						rebuildVideoTabGraphPanels(checkBoxVideo, checkBoxAudio, videoTab, true);
					}
				}
			}
		} else {
			rebuildVideoTabGraphPanels(checkBoxVideo, checkBoxAudio, videoTab, true);
		}
	}

	private void rebuildVideoTabGraphPanels(JCheckBox checkBoxVideo, JCheckBox checkBoxAudio, VideoTab videoTab, boolean isVisible) {
		SegmentThroughputGraphPanel throughputGraphPanel = videoTab.getThroughputGraphPanel();
		SegmentProgressGraphPanel progressGraphPanel = videoTab.getProgressGraphPanel();
		SegmentBufferGraphPanel bufferGraphPanel = videoTab.getBufferGraphPanel();
		
		boolean isStartupDelaySet = videoStream.isCurrentStream() && (videoStream.getPlayRequestedTime() != null || videoStream.getVideoPlayBackTime() != null);
		if (isVisible) {
			refreshVideoTabGraphPanels(checkBoxVideo, checkBoxAudio, throughputGraphPanel, progressGraphPanel, bufferGraphPanel, isStartupDelaySet);
		}
		
		videoTab.getThroughputPanel().setVisible(isVisible);
		throughputGraphPanel.setVisible(isVisible);
		videoTab.getProgressPanel().setVisible(isVisible);
		progressGraphPanel.setVisible(isVisible);
		videoTab.getBufferPanel().setVisible(isVisible && isStartupDelaySet);
		bufferGraphPanel.setVisible(isVisible && isStartupDelaySet);
	}

	private void refreshVideoTabGraphPanels(JCheckBox checkBoxVideo
											, JCheckBox checkBoxAudio
											, SegmentThroughputGraphPanel throughputGraphPanel
											, SegmentProgressGraphPanel progressGraphPanel
											, SegmentBufferGraphPanel bufferGraphPanel
											, boolean isStartupDelaySet
											) {
		if (videoStream != null && (videoStream.getVideoEventMap().size() > 0 || videoStream.getAudioEventMap().size() > 0)) {
			throughputGraphPanel.refresh(analyzerResult, videoStream, checkBoxVideo, checkBoxAudio);
			progressGraphPanel.refresh(analyzerResult, videoStream, checkBoxVideo, checkBoxAudio);
			if (isStartupDelaySet && videoStream.isCurrentStream()) {
				bufferGraphPanel.refresh( analyzerResult
										, videoStream
										, checkBoxVideo
										, checkBoxAudio
										);

			}
		}
	}

	public void refreshSegmentPanel() {
		if (!CollectionUtils.isEmpty(streamTables)) {
			for (JTable jtable : streamTables.values()) {
				// 3 seems redundant, but this overcomes a resize issue problem in JTable **** earn UI pro creds, find a better way
				autoToggleColumnWidth(jtable);
				autoToggleColumnWidth(jtable);
				autoToggleColumnWidth(jtable);
			}
		}
	}

}