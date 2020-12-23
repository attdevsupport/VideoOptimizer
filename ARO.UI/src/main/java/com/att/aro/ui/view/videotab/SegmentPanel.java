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
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.StartUpDelayDialog;
import com.att.aro.ui.view.video.IVideoPlayer;

import lombok.Data;
import lombok.Getter;

@Data
public class SegmentPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LogManager.getLogger(SegmentPanel.class);
	private static IFileManager fileManager = (IFileManager) ContextAware.getAROConfigContext().getBean("fileManager");

	private JPanel hiddenPanel;
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
	private JCheckBox enableCheckBox;

	private static final int HEIGHT_MAC = 18;
	private static final int HEIGHT_LINUX = 23;
	private static final int HEIGHT_WIN = 28;
	private int tableHeight = HEIGHT_MAC;
	private int textAdjust = 0;
	private static final int MAX_HEIGHT = 400;
	private static final int WIN_TEXT_FUDGE = 5;
	private static final int COL_MAX_WIDTH = 600;

	public static final String VIDEO_TABLE_NAME = "Video";
	public static final String AUDIO_TABLE_NAME = "Audio";
	public static final String CAPTION_TABLE_NAME = "Captioning";
	private JButton uploadButton;

	private TraceDirectoryResult traceDirectoryResult;

	private JPanel startupLatencyPanel;
	/**
	 * collection of tables for use when resizing & toggling
	 */
	HashMap<String, JTable> streamTables = new HashMap<>();

	private JPanel videoTablePanel;
	private JPanel audioTablePanel;
	private JPanel captionTablePanel;

	private JCheckBox checkBoxVideo;
	private JCheckBox checkBoxAudio;
	private JCheckBox checkBoxCC;

	private StreamingVideoData streamingVideoData;
	private AROTraceData analyzerResult;

	private JDialog dialog;

	/**
	 * @wbp.parser.constructor
	 */
	public SegmentPanel(VideoStream videoStream, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute,
			AROTraceData analyzerResult, SharedAttributesProcesses aroView, VideoManifestPanel videoManifestPanel) {
		this(true, aroView, videoStream, analyzerResult, videoManifestPanel);

		streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;
		updateHiddenPanelContent(true);
	}

	public SegmentPanel(boolean manifestFlag, SharedAttributesProcesses aroView, VideoStream videoStream,
			AROTraceData analyzerResult, VideoManifestPanel videoManifestPanel) {
		streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		this.aroView = aroView;
		this.videoStream = videoStream;
		this.analyzerResult = analyzerResult;
		this.videoManifestPanel = videoManifestPanel;
		setLayout(new BorderLayout());

		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		add(getTitleButton(), BorderLayout.NORTH);
		if (videoStream != null && (videoStream.getPlayRequestedTime() != null || videoStream.getVideoPlayBackTime() != null)) {
			startupLatencyPanel = new StartupLatencyPanel(videoStream);
			add(startupLatencyPanel, BorderLayout.CENTER);
			startupButton.setForeground(Color.GREEN);
			enableCheckBox.setSelected(videoStream.isCurrentStream());
			this.videoStream.setSelected(videoStream.isCurrentStream());

		} else {
			if (videoStreamMap.size() > 1) {
				enableCheckBox.setSelected(false);
			}
		}
		refreshGraphPanel(checkBoxVideo, checkBoxAudio);
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
		if (videoStream.getMissingSegmentCount() > 0) {
			titlePanel.add(getSessionProblems(videoStream.getMissingSegmentCount()));
		}

		// AMVOTS button
		if (validateUpload()) {
			titlePanel.add(getUploadButton());
			titlePanel.grabFocus();
		}

		titlePanel.add(getCheckBoxVideo());
		titlePanel.add(getCheckBoxAudio());
		titlePanel.add(getCheckBoxCC());

		if (videoStream != null && videoStream.getVideoPlayBackTime() != null) {
			titlePanel.add(new JLabel("Startup Delay : " + String.valueOf(videoStream.getVideoPlayBackTime())));
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
				launchStartUpDelayDialog();
			}
		});
		return startupButton;
	}

	public void launchStartUpDelayDialog() {

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews("StartupDelayDialog");
		IVideoPlayer player = aroView.getVideoPlayer();
		double maxDuration = player.getDuration();
		List<UserEvent> userEventList = analyzerResult.getAnalyzerResult().getTraceresult().getUserEvents();
		if (maxDuration >= 0) {
			selectVideoStreamWithRefresh(videoStream);
			dialog = new StartUpDelayDialog(aroView.getGraphPanel(), maxDuration, videoStream, userEventList, this);
			dialog.pack();
			dialog.setSize(dialog.getPreferredSize());
			dialog.validate();
			dialog.setModalityType(ModalityType.APPLICATION_MODAL);
			dialog.setVisible(true);
		}
	}

	public void selectVideoStreamWithRefresh(VideoStream selectedStream) {
		streamingVideoData.getVideoStreamMap().entrySet().stream().forEach(x -> {
			x.getValue().setSelected(x.getValue().equals(selectedStream));
		});
	}

	private Component getCheckBoxVideo() {
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
				refreshGraphPanel(checkBoxVideo, checkBoxAudio);
				refreshSegmentPanel();

			}
		});
		return checkBoxVideo;
	}

	private Component getCheckBoxAudio() {
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
				refreshGraphPanel(checkBoxVideo, checkBoxAudio);
				refreshSegmentPanel();
			}
		});
		return checkBoxAudio;
	}

	private Component getCheckBoxCC() {
		checkBoxCC = new JCheckBox("Captioning");
		checkBoxCC.setVisible(!CollectionUtils.isEmpty(videoStream.getCcEventList()));
		checkBoxCC.setSelected(true);
		checkBoxCC.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				audioTablePanel.setVisible(checkBoxCC.isSelected());
			}
		});
		return checkBoxCC;
	}

	/**
	 * Validate if Amvots button should be enabled
	 * 
	 * @return
	 */
	private boolean validateUpload() {
		AbstractTraceResult traceResult = analyzerResult.getAnalyzerResult().getTraceresult();
		if (TraceResultType.TRACE_DIRECTORY.equals(traceResult.getTraceResultType())) {
			traceDirectoryResult = (TraceDirectoryResult) traceResult;
			boolean mp4Exists = fileManager.createFile(traceResult.getTraceDirectory(), "video.mp4").exists();
			boolean uploadEnabled = SettingsImpl.getInstance().checkAttributeValue("AMVOTS", "TRUE")
					&& SettingsImpl.getInstance().checkAttributeValue("env", "dev");
			return uploadEnabled && mp4Exists;
		} else {
			return false;
		}
	}
	private Component getCheckBoxStreamEnable() {
		enableCheckBox = new JCheckBox();
		videoStreamMap = analyzerResult.getAnalyzerResult().getStreamingVideoData().getVideoStreamMap().values();
		boolean selected = videoStream.getVideoEventMap() != null ? true : false;
		if (!selected || videoStream.getVideoEventMap().isEmpty()
				|| ((VideoEvent) videoStream.getVideoEventMap().values().toArray()[0]).getSegmentID() < 0) {
			enableCheckBox.setEnabled(false);
		} else {
			boolean selectCheckBox = videoStreamMap.size() == 1;
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
		for (VideoStream stream : videoStreamMap) {
			if (!stream.equals(videoStream)) {
				stream.setSelected(false);
				enableCheckBox.setSelected(false);
			} else {
				stream.setSelected(true);
				enableCheckBox.setSelected(true);
			}
		}
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
	private JLabel label;

	private JButton startupButton;

	private Collection<VideoStream> videoStreamMap;

	protected void reAnalyze() {
		StreamingVideoData streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		if (streamingVideoData != null) {
			streamingVideoData.scanVideoStreams();
		}
		if (!videoStream.getVideoEventMap().isEmpty()) {
			if (checkBoxVideo != null && checkBoxAudio != null
					&& (checkBoxVideo.isVisible() || checkBoxAudio.isVisible())) {
				refreshGraphPanel(checkBoxVideo, checkBoxAudio);
			} else {
				refreshGraphPanel(null, null);
			}
		}
		((MainFrame) aroView).getDiagnosticTab().getGraphPanel().refresh(analyzerResult);
		analyzerResult = videoBestPractices.analyze(analyzerResult);
		((MainFrame) aroView).getDiagnosticTab().getGraphPanel().setTraceData(analyzerResult);
		((MainFrame) aroView).getVideoTab().refreshLocal(analyzerResult);
		((MainFrame)((MainFrame) aroView).getDiagnosticTab().getGraphPanel().getGraphPanelParent().getAroView()).refreshBestPracticesTab();
	}

	protected void refreshParent() {
		StreamingVideoData streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		if (streamingVideoData != null) {
			streamingVideoData.scanVideoStreams();
		}
		((MainFrame) aroView).getVideoTab().refreshLocal(analyzerResult);
	}

	private void updateHiddenPanelContent(boolean manifestFlag) {
		String text = "";
		tableHeight = HEIGHT_MAC;
		textAdjust = 0;
		if (Util.isWindowsOS()) {
			tableHeight = HEIGHT_WIN;
			textAdjust = WIN_TEXT_FUDGE;
		} else if (Util.isLinuxOS()) {
			tableHeight = HEIGHT_LINUX;
		}

		if (manifestFlag) {
			if (videoStream.getVideoEventsBySegment() != null) {
				text = (!videoStream.isValid())
						? MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"),
								videoStream.getManifest().getVideoName())
						: MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.manifest.name"),
								videoStream.getManifest().getVideoName());
			} else {
				text = MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"),
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
			if (!CollectionUtils.isEmpty(videoStream.getCcEventList())) {
				ccTableScrollPane = new JScrollPane();
				captionTablePanel = getStreamTable(CAPTION_TABLE_NAME, videoStream.getCcEventList(), ccTableScrollPane);
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
		TableModel tableModel = new SegmentTableModel(videoEventList);

		JTable jTable;
		jTable = new JTable(tableModel);
		jTable.setName(title);
		streamTables.put(title, jTable);
		jTable.setGridColor(Color.LIGHT_GRAY);

		for (int idx = 0; idx < jTable.getColumnCount() - 2; idx++) {
			jTable.getColumnModel().getColumn(idx).setCellRenderer(rightRenderer);
		}

		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.TRACK))
				.setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.TCP_STATE))
				.setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel) tableModel).findColumn(SegmentTableModel.CHANNELS))
				.setCellRenderer(centerRenderer);

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
		} else if (AUDIO_TABLE_NAME.equals(jTable.getName())) {
			setColumnMinMaxWidth(jTable, SegmentTableModel.CHANNELS, true, 36, 36);
			setColumnMinMaxWidth(jTable, SegmentTableModel.RESOLUTION, false, 0, 0);
		}

		// always visible
		setColumnMinMaxWidth(jTable, SegmentTableModel.SEGMENT_NO, false, 50, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.DL_END_TIME, true, 65, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TRACK, true, 36, 36);
		setColumnMinMaxWidth(jTable, SegmentTableModel.TCP_SESSION, true, 65, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.STALL_TIME, true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.PLAYBACK_TIME, true, 80, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, SegmentTableModel.SEGMENT_POSITION, true, 80, COL_MAX_WIDTH);
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

	public void refreshGraphPanel(JCheckBox checkBoxVideo, JCheckBox checkBoxAudio) {
		VideoTab videoTab = ((MainFrame) aroView).getVideoTab();
		VideoGraphPanel graphPanel = videoTab.getGraphPanel();
		if (videoStreamMap.size() > 1) {
			for (VideoStream stream : videoStreamMap) {
				if (stream.equals(videoStream)) {
					if (stream.isSelected()) {
						graphPanel.setVisible(true);
						refreshGraphPanel(checkBoxVideo, checkBoxAudio, graphPanel);
						break;
					}
				}
			}
		} else {
			graphPanel.setVisible(true);
			refreshGraphPanel(checkBoxVideo, checkBoxAudio, graphPanel);
		}
	}

	private void refreshGraphPanel(JCheckBox checkBoxVideo, JCheckBox checkBoxAudio, VideoGraphPanel graphPanel) {
		if (videoStream != null
				&& (videoStream.getVideoEventMap().size() > 0 || videoStream.getAudioEventMap().size() > 0)) {
			graphPanel.refresh(analyzerResult, videoStream, checkBoxVideo, checkBoxAudio);
		}
	}

	public void refreshSegmentPanel() {
		if (!CollectionUtils.isEmpty(streamTables)) {
			for (JTable jtable : streamTables.values()) {
				// 3 seems redundant, but this overcomes a resize issue problem in JTable ****
				// earn UI pro creds, find a better way
				autoToggleColumnWidth(jtable);
				autoToggleColumnWidth(jtable);
				autoToggleColumnWidth(jtable);
			}
		}
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
		SegmentPanel segmentTable = (SegmentPanel) SwingUtilities.getAncestorOfClass(SegmentPanel.class,
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

}