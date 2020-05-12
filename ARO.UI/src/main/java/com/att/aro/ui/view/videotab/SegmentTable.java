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
import java.util.SortedMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.IVideoBestPractices;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
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

public class SegmentTable extends JPanel implements ActionListener {

	private static final String START_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.StartTime");
	private static final String PLAY_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.PlayTime");
	private static final String STALL_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.StallTime");
	private static final String DURATION = ResourceBundleHelper.getMessageString("video.tab.segment.Duration");
	private static final String DL_END_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.DLEndTime");
	private static final String SEGMENT_NO = ResourceBundleHelper.getMessageString("video.tab.segment.Segment");
	private static final String TCP_SESSION = ResourceBundleHelper.getMessageString("video.tab.segment.TCPSession");
	private static final String TOTAL_BYTES = ResourceBundleHelper.getMessageString("video.tab.segment.TotalBytes");
	private static final String BIT_RATE = ResourceBundleHelper.getMessageString("video.tab.segment.Bitrate");
	private static final String CONTENT = ResourceBundleHelper.getMessageString("video.tab.segment.Content");
	private static final String RESOLUTION = ResourceBundleHelper.getMessageString("video.tab.segment.Resolution");
	private static final String DL_START_TIME = ResourceBundleHelper.getMessageString("video.tab.segment.DLStartTime");
	private static final String SESSION_LINK = ResourceBundleHelper.getMessageString("video.tab.segment.SessionLink");
	private static final String TCP_STATE = ResourceBundleHelper.getMessageString("video.tab.segment.TCPState");
	private static final String TRACK = ResourceBundleHelper.getMessageString("video.tab.segment.Track");
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LogManager.getLogger(SegmentTable.class);	
	
	private JPanel hiddenPanel;
	private VideoStream videoStream;
	private BasicArrowButton arrowButton;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private JLabel lbl;
	private AROTraceData analyzerResult;

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

	private TraceDirectoryResult traceDirectoryResult;
	
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

	public SegmentTable(VideoStream videoStream, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute, AROTraceData analyzerResult, SharedAttributesProcesses aroView) {
		this(true, aroView, videoStream, analyzerResult);
		
		streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;
		updateHiddenPanelContent(true);
	}
	
	public SegmentTable(boolean manifestFlag, SharedAttributesProcesses aroView, VideoStream videoStream, AROTraceData analyzerResult) {
		streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		this.aroView = aroView;
		this.videoStream = videoStream;
		this.analyzerResult = analyzerResult;
		setLayout(new BorderLayout());

		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		add(getTitleButton(),BorderLayout.NORTH);

		hiddenPanel = getHiddenPanel();
		add(hiddenPanel,BorderLayout.SOUTH);
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
			
			@Override public void componentShown(ComponentEvent e) {}
			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentHidden(ComponentEvent e) {}
			
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
		
		titlePanel.add(getCheckBoxVideo());
		titlePanel.add(getCheckBoxAudio());
		titlePanel.add(getCheckBoxCC());

		return titlePanel;
	}

	private Component getSessionProblems(int missingSegmentCount) {
		JLabel problemMessage = new JLabel(
				String.format("There %s %d segment gap%s", missingSegmentCount == 1 ? "is" : "are", missingSegmentCount, missingSegmentCount == 1 ? "" : "s"));
		problemMessage.setForeground(Color.RED);
		problemMessage.setFont(problemMessage.getFont().deriveFont(Font.BOLD, 14f));
		return problemMessage;
	}

	private Component getStartupDialogButton() {
		JButton  button = new JButton("set StartupDelay");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				launchStartUpDelayDialog();
			}
		});
		return button;
	}

	public void launchStartUpDelayDialog() {

		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews("StartupDelayDialog");
		IVideoPlayer player = aroView.getVideoPlayer();
		double maxDuration = player.getDuration();
		if (maxDuration >= 0) {
			selectVideoStreamWithRefresh(videoStream);
			reAnalyze();
			JDialog dialog = new StartUpDelayDialog(aroView.getGraphPanel(), maxDuration, videoStream);
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
		checkBoxVideo = new JCheckBox("Video");
		checkBoxVideo.setVisible(!CollectionUtils.isEmpty(videoStream.getVideoEventList()) && !CollectionUtils.isEmpty(videoStream.getAudioEventList()));
		checkBoxVideo.setSelected(false);
		checkBoxVideo.setEnabled(false);
		checkBoxVideo.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!checkBoxVideo.isSelected() && !checkBoxAudio.isSelected()) {
					checkBoxAudio.setSelected(true);
				}
				autoToggleColumnWidth();
			}
		});
		return checkBoxVideo;
	}
	
	private Component getCheckBoxAudio() {
		checkBoxAudio = new JCheckBox("Audio");
		checkBoxAudio.setVisible(!CollectionUtils.isEmpty(videoStream.getAudioEventList()) && !CollectionUtils.isEmpty(videoStream.getVideoEventList()));
		checkBoxAudio.setSelected(false);
		checkBoxAudio.setEnabled(false);
		checkBoxAudio.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!checkBoxVideo.isSelected() && !checkBoxAudio.isSelected()) {
					checkBoxVideo.setSelected(true);
				}
				autoToggleColumnWidth();
			}
		});
		return checkBoxAudio;
	}
	

	private Component getCheckBoxCC() {
		checkBoxCC = new JCheckBox("Captioning");
		checkBoxCC.setVisible(!CollectionUtils.isEmpty(videoStream.getCcEventList()));
		checkBoxCC.setSelected(true);
		checkBoxCC.setEnabled(false);
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
		boolean selected = videoStream.getVideoEventList() != null? true:false;
		if (!selected 
				|| videoStream.getVideoEventList().isEmpty()
				|| ((VideoEvent) videoStream.getVideoEventList().values().toArray()[0]).getSegmentID() < 0
				) {
			enableCheckBox.setEnabled(false);
		} else {
			enableCheckBox.setSelected(videoStream.isSelected());
			enableCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					if (e.getSource().getClass().equals(JCheckBox.class)) {
						videoStream.setSelected(((JCheckBox) e.getSource()).isSelected());
						reAnalyze();
					}
				}
			});
		}

		return enableCheckBox;
	}
	
	public void updateTitleButton(AROTraceData analyzerResult) {
		if (titlePanel != null) {
			if (analyzerResult.getAnalyzerResult() != null) {
				streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();

				for (VideoStream manifest : streamingVideoData.getVideoStreamMap().values()) {

					if (manifest.equals(videoStream)
							&& ((!videoStream.getVideoEventsBySegment().isEmpty()) 
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

	private IVideoBestPractices videoBestPractices = ContextAware.getAROConfigContext().getBean(IVideoBestPractices.class);

	private int tblHeight;

	protected void reAnalyze() {
		StreamingVideoData streamingVideoData = analyzerResult.getAnalyzerResult().getStreamingVideoData();
		if (streamingVideoData != null) {
			streamingVideoData.scanVideoStreams();
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
		((MainFrame) aroView).getDiagnosticTab().getGraphPanel().refresh(analyzerResult);
		((MainFrame) aroView).getDiagnosticTab().getGraphPanel().setTraceData(analyzerResult);
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
						? MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"), videoStream.getManifest().getVideoName())
						: MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.manifest.name"), videoStream.getManifest().getVideoName());
			} else {
				text = MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"),
						videoStream.getManifest().getVideoName());
			}
			lbl.setText(text + ", segment count:" + videoStream.getVideoEventList().size());
			
			// add the chunk/segment tables
			if (!CollectionUtils.isEmpty(videoStream.getVideoEventList())) {
				videoTableScrollPane = new JScrollPane();
				videoTablePanel = getStreamTable("Video", videoStream.getVideoEventList(), videoTableScrollPane);
				checkBoxVideo.setSelected(true);
				hiddenPanel.add(videoTablePanel,
						new GridBagConstraints(1, 1, 1, 2, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
			}
			// AUDIO
			if (!CollectionUtils.isEmpty(videoStream.getAudioEventList())) {
				audioTableScrollPane = new JScrollPane();
				audioTablePanel = getStreamTable("Audio", videoStream.getAudioEventList(), audioTableScrollPane);
				checkBoxAudio.setSelected(true);
				hiddenPanel.add(audioTablePanel,
						new GridBagConstraints(2, 2, 1, 2, 1.0, 1.0, GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
			}
			// Closed Caption / subtitles
			if (!CollectionUtils.isEmpty(videoStream.getCcEventList())) {
				ccTableScrollPane = new JScrollPane();
				captionTablePanel = getStreamTable("Captioning", videoStream.getCcEventList(), ccTableScrollPane);
				checkBoxCC.setSelected(true);
				hiddenPanel.add(captionTablePanel,
						new GridBagConstraints(3, 2, 1, 2, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
			}
		}
	}

	private JPanel getStreamTable(String title, SortedMap<String, VideoEvent> eventList, JScrollPane tableScrollPane) {
		
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);	
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		
		Collection<VideoEvent> videoEventList = eventList.values();
		rowCount = videoEventList.size();
		TableModel tableModel = new SegmentTableModel(videoEventList);

		JTable jTable;
		jTable = new JTable(tableModel);
		streamTables.put(title, jTable);
		jTable.setGridColor(Color.LIGHT_GRAY);
		for (int idx = 0; idx < jTable.getColumnCount()-2; idx++) {
			jTable.getColumnModel().getColumn(idx).setCellRenderer(rightRenderer);
		}
		
		jTable.getColumnModel().getColumn(((SegmentTableModel)tableModel).findColumn(TRACK)).setCellRenderer(centerRenderer);
		jTable.getColumnModel().getColumn(((SegmentTableModel)tableModel).findColumn(TCP_STATE)).setCellRenderer(centerRenderer);

		JTableHeader header = jTable.getTableHeader();
		header.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
		
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		if (rowCount > tableHeight) {
		tableScrollPane.setPreferredSize(new Dimension(0, MAX_HEIGHT ));
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
		
		autoToggleColumnWidth();

		int colCount = rowSorter.getModel().getColumnCount();
		for (int column = 0; column < colCount; column++) {
			rowSorter.setComparator(column, new TableSortComparator(column, "-"));
		}

		jTable.addMouseListener(streamTableClickHandler(tableModel, jTable, jTable.getColumnModel().getColumnIndex("SessionLink")));
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

	public void setColumnWidthDefaults(JTable jTable) {
		// hidden
		setColumnMinMaxWidth(jTable, SESSION_LINK, false,  0, 0);
		
		// visible/hidden
		setColumnMinMaxWidth(jTable, DL_START_TIME,true, 65, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, RESOLUTION,true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, CONTENT,true, 50, 50);
		setColumnMinMaxWidth(jTable, BIT_RATE,true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, TOTAL_BYTES,true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, TCP_STATE,true, 40, COL_MAX_WIDTH);

		// always visible
		setColumnMinMaxWidth(jTable, SEGMENT_NO,false, 50, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, DL_END_TIME,true, 65, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, TRACK,true, 36, 36);
		setColumnMinMaxWidth(jTable, DURATION,true, 80, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, TCP_SESSION,true, 40, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, STALL_TIME,true, 60, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, PLAY_TIME,true, 80, COL_MAX_WIDTH);
		setColumnMinMaxWidth(jTable, START_TIME,true, 80, COL_MAX_WIDTH);
	}

	public void setColumnWidthMinimized(JTable jTable) {
		// hidden
		setColumnMinMaxWidth(jTable, SESSION_LINK, false, 0, 0);
		setColumnMinMaxWidth(jTable, DL_START_TIME, false, 0, 0);
		setColumnMinMaxWidth(jTable, RESOLUTION, false, 0, 0);
		setColumnMinMaxWidth(jTable, CONTENT, false, 0, 0);
		setColumnMinMaxWidth(jTable, BIT_RATE, false, 0, 0);
		setColumnMinMaxWidth(jTable, TOTAL_BYTES, false, 0, 0);
		setColumnMinMaxWidth(jTable, TCP_SESSION, false, 0, 0);
	}
	
	private void setColumnMinMaxWidth(JTable jTable, String colName, boolean isResizable, int minWidth, int maxWidth) {
		TableColumn column = jTable.getColumnModel().getColumn(jTable.getColumnModel().getColumnIndex(colName));
		column.setMinWidth(minWidth + textAdjust);
		column.setMaxWidth(maxWidth);
	}

	public void autoToggleColumnWidth() {
		if (!CollectionUtils.isEmpty(streamTables)) {
			for (JTable jtable : streamTables.values()) {
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
		if (videoTablePanel!=null) {videoTablePanel.setVisible(checkBoxVideo.isSelected());}
		if (audioTablePanel!=null) {audioTablePanel.setVisible(checkBoxAudio.isSelected());}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SegmentTable segmentTable = (SegmentTable) SwingUtilities.getAncestorOfClass(SegmentTable.class, (BasicArrowButton) e.getSource());
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
				checkBoxVideo.setEnabled(true);
				checkBoxAudio.setEnabled(true);
				checkBoxCC.setEnabled(true);
				autoToggleColumnWidth();
			} else {
				arrowButton.setDirection(SwingConstants.EAST);
				nestedPanel.setVisible(false);
				checkBoxVideo.setEnabled(false);
				checkBoxAudio.setEnabled(false);
				checkBoxCC.setEnabled(false);
			}
			aroView.getCurrentTabComponent().revalidate();
			nestedPanel.setVisible(!(arrowButton.getDirection() == SwingConstants.EAST));
		}
	}

	public void resizeTableScrollPanes(JScrollPane tableJScrollPane) {
		if (tableJScrollPane != null) {
			if (rowCount > tableHeight) {
				tableJScrollPane.setPreferredSize(new Dimension(tableJScrollPane.getWidth(), MAX_HEIGHT));
			} else {
				tableJScrollPane.setPreferredSize(new Dimension(tableJScrollPane.getWidth(), streamTables.get("Video").getHeight() + tableHeight));
				tableJScrollPane.getBounds().setSize(tableJScrollPane.getWidth(), streamTables.get("Video").getHeight() + tableHeight);
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
	
	private JPanel getHiddenPanel() {
		if (hiddenPanel == null) {
			hiddenPanel = new JPanel(new GridBagLayout());
			hiddenPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			hiddenPanel.setBorder(BorderFactory.createEtchedBorder(WIDTH));
		}
		return hiddenPanel;
	}

 }
