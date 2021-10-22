/*
 *  Copyright 2017,2021 AT&T
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.ui.tabbedui.VerticalLayout;
import org.jfree.util.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.CollectionUtils;

import com.att.aro.core.AROConfig;
import com.att.aro.core.IVideoBestPractices;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.IVideoStartupReadWrite;
import com.att.aro.core.peripheral.impl.VideoStartupReadWriterImpl;
import com.att.aro.core.peripheral.pojo.UserEvent;
import com.att.aro.core.peripheral.pojo.UserEvent.UserEventType;
import com.att.aro.core.peripheral.pojo.VideoStreamStartup;
import com.att.aro.core.peripheral.pojo.VideoStreamStartupData;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.Orientation;
import com.att.aro.core.videoanalysis.impl.VideoSegmentAnalyzer;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.videoframe.FrameReceiver;
import com.att.aro.core.videoanalysis.videoframe.FrameRequest;
import com.att.aro.core.videoanalysis.videoframe.FrameRequest.JobType;
import com.att.aro.core.videoanalysis.videoframe.FrameStatus;
import com.att.aro.core.videoanalysis.videoframe.VideoFrameExtractor;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.ImagePanel;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.commonui.RoundedBorder;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.diagnostictab.plot.VideoChunksPlot;
import com.att.aro.ui.view.video.IVideoPlayer;
import com.att.aro.ui.view.videotab.SegmentTablePanel;
import com.att.aro.ui.view.videotab.VideoManifestPanel;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * StartupDelayDialog
 * Extracts and displays I-frames from video.mp4 or video.mov as fallback
 * Does not synchronize with the video player
 */
public class StartupDelayDialog extends JDialog implements FrameReceiver {

	private static final Logger LOG = LogManager.getLogger(StartupDelayDialog.class.getName());

	ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);

	private static final VideoFrameExtractor videoFrameExtractor = ContextAware.getAROConfigContext().getBean("videoFrameExtractor", VideoFrameExtractor.class);

	IStringParse stringParse = context.getBean(IStringParse.class);

	private static final ResourceBundle resourceBundle = ResourceBundleHelper.getDefaultBundle();
	private static final VideoSegmentAnalyzer videoSegmentAnalyzer = ContextAware.getAROConfigContext().getBean("videoSegmentAnalyzer", VideoSegmentAnalyzer.class);

	private static final IFileManager fileManager = ContextAware.getAROConfigContext().getBean(FileManagerImpl.class);
	private static final IExternalProcessRunner externalProcessRunner = ContextAware.getAROConfigContext().getBean(ExternalProcessRunnerImpl.class);

	private static final IVideoStartupReadWrite videoStartupReadWrite = ContextAware.getAROConfigContext().getBean("videoStartupReadWrite", VideoStartupReadWriterImpl.class);
	
	private static final long serialVersionUID = 1L;

	private static final int MAC_COMMAND_KEY = 157;
	private static final int PC_ALT_KEY = 18;

	private int frameImagePanelWidth = 316;
	private int thumbnailHeight = 100;
	
	private JButton setButton;
	private JButton cancelButton;

	private JSlider segmentSlider;
	private JSlider userEventSlider;
	private int maxValue;
	private int userEventMaxValue;
	private int userEventMinValue;
	private int minValue;
	
	private VideoChunksPlot vcPlot;
	private MainFrame mainFrame;
	private GraphPanel parentPanel;
	private BufferedImage originalThumbnail;
	
	private List<VideoEvent> activeSegmentList = new ArrayList<>();
	private List<UserEvent> allUserEventList = new ArrayList<>();
	
	private List<SegmentItem> segmentItemList = new ArrayList<>();
	private List<UserEventItem> userEventItemList = new ArrayList<>();
	
	private JList<SegmentItem> segmentJList;
	private JList<UserEventItem> userEventJList;
	
	private DefaultListModel<SegmentItem> segmentListModel;
	private DefaultListModel<UserEventItem> userEventListModel;

	private SegmentItem segmentChosen;
	private UserEventItem userEventChosen;
	
	private int selectedSegmentIndex = 0;
	private int selectedIdxUE = 0;
	private JLabel thumbnailImgLabel = new JLabel();
	private StreamingVideoData streamingVideoData;

	private JTextField playRequestedTime;
	private JTextField segmentTimeField;
	private double startTime;

	private JPanel segmentThumbnailImagePanel;
	private SegmentTablePanel segmentTablePanel;
	private VideoStream videoStream;
	private VideoStreamStartup videoStreamStartup;
	private VideoStreamStartupData videoStreamStartupData;
	
	private double manifestRequestTime;

	private double initialDeviceVideoOffset;
	private double playerMediaTime;

	private String traceFolder;
	private String videoFrameFolder;
	private String deviceVideoPath;

	// device video
	private Double deviceVideoNbFrames;
	private Double deviceVideoDuration;
	private double deviceVideoRatio;
	private int deviceVideoWidth, deviceVideoHeight;
	@Getter
	@Setter
	private Dimension deviceScreenDimension;
	private Orientation orientation;

	private BufferedImage image;
	private boolean showThumbnail;

	private JPanel segmentPanel;
	private JPanel userEventPanel;
	private TreeMap<Double, VideoEvent> activeSegmentMap = new TreeMap<>();
	private TreeMap<Double, UserEvent> activeUserEventMap = new TreeMap<>();
	@NonNull private VideoEvent selectedVideoEvent;
	@NonNull private UserEvent selectedUserEvent;

	// extracted frame
	private int fWidth, fHeight;
	private double extractedFPS = 30;
	private TreeMap<Double, BufferedImage> frameMap = new TreeMap<>();
	private FrameImagePanel userEventFramePanel;
	private FrameImagePanel segmentFramePanel;

	private AROTraceData traceData;
	private int streamViewIndex;

	private Dimension computerScreenSize;


	public StartupDelayDialog(GraphPanel parentPanel
							, double deviceVideoDuration
							, VideoStream videoStream
							, List<UserEvent> userEventList
							, SegmentTablePanel segmentTablePanel
							, int streamViewIndex
						) throws Exception {

		this.deviceVideoDuration = deviceVideoDuration;
		this.videoStream = videoStream;
		this.manifestRequestTime = videoStream.getManifest().getRequestTime();
		this.parentPanel = parentPanel;
		this.segmentTablePanel = segmentTablePanel;
		this.streamViewIndex = streamViewIndex;
		DiagnosticsTab parent = parentPanel.getGraphPanelParent();
		this.mainFrame = (MainFrame) parent.getAroView();
		this.traceData = mainFrame.getController().getTheModel();
		vcPlot = parentPanel.getVcPlot();

		computerScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
		LOG.debug(String.format("Computer screen dimensions w:%d : h:%d", computerScreenSize.width, computerScreenSize.height)); // linux laptop 1366:768
		setBounds(0, 0, (int)computerScreenSize.width/3, (int)computerScreenSize.height/2);
		loadVideoData(parent.getVideoPlayer(), computerScreenSize);
		if (!initialize() || deviceVideoDuration == 0) {
			dispose();
			throw new Exception("No video file or duration is zero, startup delay cannot be set manually");
		}

		prepVideoSegments(videoStream);
		prepUserEvents(userEventList);

		Log.debug("StartupDelayDialog launched for " + videoStream.getManifest().getVideoName());
		Log.debug(String.format("Computer w: %d : h: %d", computerScreenSize.width, computerScreenSize.height));
		Log.debug(String.format("Device   w: %d : h: %d", deviceVideoWidth,deviceVideoHeight));
		Log.debug("videoDuration :" + deviceVideoDuration);
		Log.debug("videoOffset :" + initialDeviceVideoOffset);
		Log.debug("mediaTime :" + playerMediaTime);
		Log.debug("duration:" + deviceVideoDuration);
  		Log.debug("nb_frames:" + deviceVideoNbFrames);

		try {
			videoFrameExtractor.initialize(videoFrameFolder, deviceVideoPath, frameMap, fWidth * 2, fHeight * 2);
			extractedFPS = calculateFPS(30);
			preloadFrames(100);
		} catch (Exception e) {
			Log.error("VideoFrameExtractor Exception:" + e.getMessage());
		}
		
		createDialog(vcPlot);
		dialogLaunchUserEvent();
		dialogLaunchSegment();
		makeInitialSelections(videoStream);
		pack();
	}
	
	public void dialogLaunchSegment() {
		if (selectedSegmentIndex >= 0 && selectedSegmentIndex < segmentJList.getComponentCount()) {
			segmentJList.setSelectionInterval(selectedSegmentIndex, selectedSegmentIndex);
			segmentJList.getModel().getElementAt(selectedSegmentIndex).setSelected(true);
			segmentJList.scrollRectToVisible(new Rectangle(segmentJList.getBounds()));
		}
		double jsonStartTime = loadStartupDelay();
		setStartTime(jsonStartTime > manifestRequestTime ? jsonStartTime : activeSegmentList.get(0).getPlayTime());
		selectSegment(0);
	}

	public void dialogLaunchUserEvent() {
		if (!allUserEventList.isEmpty()) {
			if (selectedIdxUE >= 0 && selectedIdxUE < userEventJList.getComponentCount()) {
				userEventJList.setSelectionInterval(selectedIdxUE, selectedIdxUE);
				userEventJList.getModel().getElementAt(selectedIdxUE).setSelected(true);
				userEventJList.scrollRectToVisible(new Rectangle(userEventJList.getBounds()));
			}
			setStartTime(loadStartupDelay());
			setTimeJTextField(playRequestedTime, getStartTime());
		}
	}

	/**
	 * Sets initial start times and selected table items
	 * 
	 * @param videoStream
	 */
	public void makeInitialSelections(VideoStream videoStream) {
		if (startTime == 0) {
			setStartTime(videoStream.getFirstSegment().getDLLastTimestamp());
		}

		segmentFramePanel.setImage(frameMap.firstEntry().getValue());
		if (!allUserEventList.isEmpty()) {
			double timestamp;
			if ((selectedVideoEvent = makeSegmentSelection(0)) != null) {
				selectedUserEvent = videoStreamStartup.getUserEvent();
				if (getUserEventTimeStamp(selectedUserEvent) > manifestRequestTime) {
					UserEvent tempUserEvent = findPriorUserEvent(manifestRequestTime);
					selectedUserEvent = tempUserEvent != null ? tempUserEvent : selectedUserEvent;
				}
				if ((timestamp = videoStreamStartup.getPlayRequestedTime()) == 0) {
					timestamp = getUserEventTimeStamp(selectedUserEvent);
					updateUserEventSelection(timestamp);
				} else {
					int index = findUserEventIndex(selectedUserEvent);
					makeUserEventSelection(index);
					updateUserEventSelection(index);
					
					setTimeJTextField(playRequestedTime, timestamp);
				}
				updateStartTime(playRequestedTime, userEventSlider);
			} else {
				makeUserEventSelection(0);
			}
			updateUserEventJListData();
			userEventJList.repaint();
		}
		updateSegmentJListData();
		segmentJList.repaint();
	}

	/**
	 * Locate and return the closest UserEvent prior to a given timestamp
	 * 
	 * @param tsKey
	 * @return
	 */
	private UserEvent findPriorUserEvent(double tsKey) {
		if (CollectionUtils.isEmpty(allUserEventList)) {
			return null;
		}
		Double lowKey = null;
		try { // find key closest to but less than tsKey
			lowKey = activeUserEventMap.lowerKey(tsKey);
		} catch (Exception e) {
			lowKey = activeUserEventMap.firstKey();
		}
		
		return (lowKey != null ? activeUserEventMap.get(lowKey) : null);
	}

	/**
	 * Calculate output frames per second
	 * 
	 * @param frameMap
	 * @return fps
	 */
	private double calculateFPS(double defaultValue) {
		if (deviceVideoDuration > 0) {
			collectFrames(new FrameRequest(FrameRequest.JobType.COLLECT_FRAMES, 0, 1, 0D, null));
			collectFrames(new FrameRequest(FrameRequest.JobType.COLLECT_FRAMES, deviceVideoDuration / 2, 1, null, null));

			while (frameMap.size() < 2) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.info("Interrupted :" + e.getMessage());
				}
			}
			Double frame;
			if ((frame = frameMap.lastEntry().getKey()) > 0) {
				return frame / (deviceVideoDuration / 2);
			}
		}
		return defaultValue;
	}

	/**
	 * Build and return the SegmentPane, contains SegmentTable and SegmentFrame
	 * 
	 * @return JPanel SegmentPane
	 */
	private JPanel createSegmentPane() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new RoundedBorder(new Insets(5, 5, 5, 5), null));
		panel.add(createSegmentPanel(), BorderLayout.WEST);
		panel.add((segmentFramePanel = createFramePanel()), BorderLayout.EAST);
		panel.setPreferredSize(adjustHeight(panel, 0.38)); 
		return panel;
	}

	private Dimension adjustHeight(JPanel panel, double heightAdjustment) {
		Dimension dim = panel.getPreferredSize();
		dim.height = (int) (computerScreenSize.height * heightAdjustment);
		return dim;
	}

	private FrameImagePanel createFramePanel() {
		FrameImagePanel imagePanel = new FrameImagePanel(image, orientation);
		imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		return imagePanel;
	}

	/**
	 * Displays Video frame clippings from video.mp4/mov
	 */
	class FrameImagePanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		int imageWidth = frameImagePanelWidth;
		int imageHeight = (int)(imageWidth * deviceVideoRatio);
		
		private double seconds;
		Dimension dimn;
		ImagePanel imagePanel;
		
		public FrameImagePanel(Image image, Orientation orientation) {
			imagePanel = new ImagePanel(image);
			if (orientation.equals(Orientation.LANDSCAPE)) {
				Log.debug("Landscape ");
				imageWidth = fWidth;
				imageHeight = fHeight;
				dimn = new Dimension(fHeight, fWidth);
			} else {
				Log.debug("Portrait ");
				imageWidth = fWidth;
				imageHeight = fHeight;
				dimn = new Dimension(fWidth, fHeight);
			}
			
			imagePanel.setPreferredSize(dimn);
			imagePanel.setMinimumSize(dimn);
			imagePanel.setMaximumSize(dimn);
			imagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			Log.debug(String.format("Image Frame Size ( h:%d x w:%d )", dimn.height, dimn.width));
			add(imagePanel);
		}

		/**
		 * place image in 
		 * @param bImage
		 */
		public void setImage(BufferedImage bImage) {
			if (bImage != null) {
				if (isLandscape()) {
					imagePanel.setImage(bImage.getScaledInstance(imageHeight, imageWidth, Image.SCALE_DEFAULT));
				} else {
					imagePanel.setImage(bImage.getScaledInstance(imageWidth, imageHeight, Image.SCALE_DEFAULT));
				}
				repaint();
			}
		}

		private boolean isLandscape() {
			return orientation.equals(Orientation.LANDSCAPE);
		}

		public double getSeconds() {
			return seconds;
		}

		public void setSeconds(int value) {
			seconds = value * 0.04;
		}

		public void updateFrame() {
			Double key = seconds >= initialDeviceVideoOffset ? ((seconds - initialDeviceVideoOffset) * extractedFPS) : 0;
			Double priorKey = getFloorKey(key, 0D);
			Double nextKey = getCeilingKey(key, -1D);
			Double closestKey;

			closestKey = (key - priorKey < nextKey - key) ? priorKey : nextKey;
			BufferedImage tempImage = frameMap.get(closestKey);
			if (tempImage != null) {
				setImage(tempImage);
			}
		}

	}

	public void prepUserEvents(List<UserEvent> userEventList) {
		allUserEventList.clear();
		if (!userEventList.isEmpty()) {
			double pivotTime = -1.0;
			int totalUserEvents = 0;
			int skipEvent = 0;
			for (UserEvent userEvent : userEventList) {
				if (UserEventType.SCREEN_TOUCH.equals(userEvent.getEventType())) {
					totalUserEvents++;
					double deltaTime = userEvent.getPressTime() - pivotTime;
					if (deltaTime >= 1) {
						activeUserEventMap.put(userEvent.getPressTime(), userEvent);
						allUserEventList.add(userEvent);
						pivotTime = userEvent.getPressTime();
					} else {
						LOG.debug("skip time event : " + userEvent.getPressTime() + " delta time: " + deltaTime);
						skipEvent++;
					}
				}
			}
			LOG.debug("Skip event: " + skipEvent + " Total touch event: " + totalUserEvents);
		}
	}

	public void prepVideoSegments(VideoStream videoStream) {
		this.activeSegmentList.clear();
		for (VideoEvent videoEvent : videoStream.getVideoEventsBySegment()) {
			if (videoEvent.isNormalSegment() && videoEvent.isSelected()) {
				activeSegmentMap.put(videoEvent.getPlayTime(), videoEvent);
				activeSegmentList.add(videoEvent);
				if (!videoEvent.isDefaultThumbnail()) {
					showThumbnail = true;
				}
			}
		}
	}

	private void loadVideoData(IVideoPlayer videoPlayer, Dimension computerScreenSize) throws Exception {

		deviceVideoDuration = videoPlayer.getDuration();
		initialDeviceVideoOffset = videoPlayer.getVideoOffset();
		playerMediaTime = videoPlayer.getMediaTime();

		AbstractTraceResult traceResult = parentPanel.getTraceData().getAnalyzerResult().getTraceresult();
		traceFolder = traceResult.getTraceDirectory();
		String temp =  ((TraceDirectoryResult) parentPanel.getTraceData().getAnalyzerResult().getTraceresult()).getDeviceDetail().getScreenSize();
		String[] deviceScreenSize = stringParse.parse(temp, "(\\d+)\\*(\\d+)");
		setDeviceScreenDimension(new Dimension(StringParse.stringToDouble(deviceScreenSize[0], 800).intValue(), StringParse.stringToDouble(deviceScreenSize[1], 600).intValue()));

		String movVideoPath = traceResult.getTraceDirectory() + Util.FILE_SEPARATOR + resourceBundle.getString("video.videoDisplayFile");
		String mp4VideoPath = traceResult.getTraceDirectory() + Util.FILE_SEPARATOR + resourceBundle.getString("video.videoFileOnDevice");

		deviceVideoPath = new File(mp4VideoPath).exists() ? mp4VideoPath : movVideoPath;

		String cmd = String.format("%s -i \"%s\" %s", Util.getFFPROBE(), deviceVideoPath, " -v quiet -show_entries stream=height,width,nb_frames,duration,codec_name");

		String results = externalProcessRunner.executeCmd(cmd, true, true);
		results = results.replaceAll("[\n\r]", " ").replaceAll("  ", " ").replaceAll("] ", "]").replaceAll(" \\[", "\\[");

		if (!results.contains("STREAM")) {
			throw new Exception("Error executing ffprobe <" + cmd + ">" + results);
		}

		String streamSection;
		String fieldString = "[STREAM]";
		String delimiter = "\\[/STREAM\\]";
		results = results.replaceAll(fieldString + " ", fieldString).replaceAll(" " + delimiter, delimiter);

		while (StringUtils.isNotEmpty(streamSection = StringParse.findLabeledDataFromString(fieldString, delimiter, results))) {
			results = results.substring(fieldString.length() * 2 + 1 + streamSection.length());
			String[] dimension = stringParse.parse(streamSection, " width=(\\d+) height=(\\d+) ");

			Double height = 0D;
			Double width = height; 
			if (dimension != null && dimension.length > 1) {
				width  = StringParse.stringToDouble(dimension[0], 0);
				height = StringParse.stringToDouble(dimension[1], 0);
				if (height == 0 || width == 0) {
					continue;
				}
			} else {
				continue;
			}
			
			deviceVideoWidth = width.intValue();
			deviceVideoHeight = height.intValue();
			frameImagePanelWidth = (int) computerScreenSize.getWidth() / 4;

			if (height < width) {
				orientation = Orientation.LANDSCAPE;
				deviceVideoRatio = height / width;
			} else {
				orientation = Orientation.PORTRAIT;
				deviceVideoRatio = width / height;
			}

			fWidth = frameImagePanelWidth/2;
			fHeight = (int) ((double) frameImagePanelWidth / deviceVideoRatio)/2;

			thumbnailHeight = fHeight/2;
					
			deviceVideoNbFrames = StringParse.findLabeledDoubleFromString("nb_frames=", streamSection);
			Double duration = StringParse.findLabeledDoubleFromString("duration=", streamSection);
			if (duration != null) {
				deviceVideoDuration = duration;
			}
		}

		Log.info("videoDuration :" + deviceVideoDuration);
		Log.info("videoOffset :" + initialDeviceVideoOffset);
		Log.info("mediaTime :" + playerMediaTime);
		Log.info(String.format("h:w = %d:%d", deviceVideoHeight, deviceVideoWidth));
		Log.info("duration:" + deviceVideoDuration);
		Log.info("nb_frames:" + deviceVideoNbFrames);
 
	}

	/**
	 * Extracts 1 frame every frameSkip count. Results depend on the video. Do not depend on the frame skip to be an exact amount.
	 * 
	 * @param frameSkip
	 * @throws Exception
	 */
	public void preloadFrames(int frameSkip) throws Exception {
		videoFrameExtractor.addJob(new FrameRequest(JobType.PRELOAD, 0, frameSkip, null, this));
	}

	/**
	 * Collect frames from startTime for a given count of frames. Frames are added to a TreeMap<Double, BufferedImage> frameMap
	 * 
	 * Note: If a startTime is calculated to retrieve a specific frame this can fail if the ffmpeg fails to extract one or more frames depending on the state of the
	 * video file. If accuracy is important, the results should be examined, and adjustments should be made to handle missing frames. Usually this results in
	 * pulling frames beyond the target.
	 * 
	 * @param startTime
	 * @param frameCount
	 * @param resultSubscriber
	 *                             to receive results
	 */
	private void collectFrames(FrameRequest frameRequest) {
		try {
			videoFrameExtractor.addJob(frameRequest);
		} catch (Exception e) {
			Log.error("VideoFrameExtractor Exception:" + e.getMessage());
			return;
		}
	}

	@Override
	public void receiveResults(Class<?> sender, FrameStatus result) {
		Log.info(String.format("%s :%s, results:%s", sender.getSimpleName(), result.isSuccess() ? "received" : "nothing", result.getAddedCount()));

		if (result.isSuccess() && result.getAddedCount() > 0 && segmentFramePanel != null) {
			if (result.getFrameRequest().getTargetFrame() != null) {
				// checks if need to Recover from missing frames
				FrameRequest frameRequest = result.getFrameRequest();
				int frameOffset;
				if (Math.abs((frameOffset = frameRequest.getTargetFrame().intValue() - result.getFirstFrame())) > 1) {
					frameRequest.getTargetFrame().intValue();
					frameRequest.getStartTimeStamp();
					frameOffset = frameRequest.getTargetFrame().intValue() - result.getFirstFrame();

					frameRequest.setStartTimeStamp(result.getFrameRequest().getStartTimeStamp() - frameOffset / extractedFPS);
					frameRequest.setTryCount(frameRequest.getTryCount() + 1);
					collectFrames(frameRequest);
				}
			}
			segmentFramePanel.updateFrame();
			if (userEventFramePanel != null) {
				userEventFramePanel.updateFrame();
			}

		} else {
			Log.error("failed frame request:" + result);
		}
	}

	/**
	 * retrieve a frame based on seconds * deviceFrameRate
	 * 
	 * @param videoEvent
	 * @return
	 */
	private BufferedImage requestFrame(double timeStamp) {
		BufferedImage frame = null;
		if (!CollectionUtils.isEmpty(frameMap)) {

			Double key = timeStamp >= initialDeviceVideoOffset ? ((timeStamp - initialDeviceVideoOffset) * extractedFPS) : 0;
			Double priorKey = getFloorKey(key, 0D);
			Double nextKey = getCeilingKey(key, -1D);
			Double closeKey;

			double priorGap = 100;
			double nextGap = 100;
			double frameNumber = -1;
			if (priorKey == null || (priorGap = key - priorKey) > 10) {
				show(timeStamp, key, priorKey, priorGap, frameNumber);
				frameNumber = (key - (priorGap > 99 ? 100 : priorGap));
				collectFrames(new FrameRequest(JobType.COLLECT_FRAMES, calcFrameToSeconds(frameNumber), (int) (nextGap > 100 ? 100 : nextGap), key, this));
			}
			if (nextKey == null || (nextGap = nextKey - key) > 10) {
				collectFrames(new FrameRequest(JobType.COLLECT_FRAMES
						, timeStamp
						, (int) (nextGap > 100 ? 100 : nextGap)	// number of frames being requested
						, key
						, this
				));
			}

			priorKey = getFloorKey(key, 0D);
			nextKey = getCeilingKey(key, -1D);
			closeKey = (priorGap < nextGap) ? priorKey : nextKey;

			frame = frameMap.get(closeKey);
		}
		return frame;
	}

	public void show(double timeStamp, Double key, Double priorKey, double priorGap, double frameNumber) {
		LOG.debug(String.format("\ntimeStamp: %.3f, key: %.3f, frameNumber: %.3f, priorKey: %.3f, priorGap: %.3f", timeStamp, key, frameNumber, priorKey, priorGap));
	}

	private double calcFrameToSeconds(double frameNumber) {
		return (frameNumber / extractedFPS);
	}

	private boolean initialize() {
		traceFolder = parentPanel.getTraceData().getAnalyzerResult().getTraceresult().getTraceDirectory();
		if (fileManager.directoryExist(traceFolder)) {
			videoFrameFolder = fileManager.createFile(traceFolder, "tempVideoFrameFolder").toString();
			fileManager.mkDir(videoFrameFolder);
			fileManager.deleteFolderContents(videoFrameFolder);
			return true;
		}
		return false;
	}

	private void destroy() {
		cleanup();
		dispose();
		System.gc();
	}

	public void cleanup() {
		if (videoFrameExtractor != null) {
			videoFrameExtractor.shutdown();
		}
	}

	public void setTimeJTextField(JTextField timeField, double timestamp) {
		timeField.setText(String.format("%.03f", timestamp));
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getStartTime() {
		return this.startTime;
	}

	/**
	 * Builds out UI with User Events and Video Segments
	 * 
	 * @param vcPlot
	 *                   - optional, some traces do not contain user events
	 */
	private void createDialog(final VideoChunksPlot vcPlot) {
		streamingVideoData = traceData.getAnalyzerResult().getStreamingVideoData();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanup();
				super.windowClosing(e);
			}
		});

		setMaximumSize(computerScreenSize);
		
		add(createContentPanel(vcPlot));

		setResizable(false);
		
	}

	private JPanel createContentPanel(final VideoChunksPlot vcPlot) {
		JPanel contentPane = new JPanel(new GridBagLayout());
		
		contentPane.add(videoStreamNamePane(), new GridBagConstraints(0, 0, 1, 1, 2.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		// conditionally add user event panel
		if (!allUserEventList.isEmpty()) {
			contentPane.add(createUserEventPane(), new GridBagConstraints(0, 1, 1, 1, 5.0, .2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			userEventFramePanel.setImage(image);
		}

		contentPane.add(createSeparator(),   new GridBagConstraints(0, 2, 1, 1, 2.0, .1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(createSegmentPane(), new GridBagConstraints(0, 3, 1, 1, 2.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		contentPane.add(createButtonPane(vcPlot), new GridBagConstraints(0, 4, 1, 1, 2.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		segmentFramePanel.setImage(image);
		return contentPane;
	}

	public JPanel createButtonPane(final VideoChunksPlot vcPlot) {
		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 220, 1, 220));

		// build the 'Set' JButton
		setButton = new JButton("Set");
		setButton.setPreferredSize(new Dimension(100, 20));
		setButton.addActionListener(createUserEventsButtonListener(vcPlot));
		btnPanel.add(setButton, BorderLayout.EAST);

		// build the 'Cancel' JButton
		cancelButton = new JButton("Cancel");
		cancelButton.setPreferredSize(new Dimension(100, 20));
		cancelButton.addActionListener(createUserEventsButtonListener(vcPlot));
		btnPanel.add(cancelButton, BorderLayout.WEST);
		return btnPanel;
	}

	public JPanel buildLabelPanel(String lookupString) {
		JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		labelPanel.add(new JLabel(resourceBundle.getString(lookupString)));
		return labelPanel;
	}

	/**
	 * Build and return the UserEventPane, contains UserEventTable and UserEventFrame
	 * 
	 * @return JPanel UserEventPane
	 */
	private JPanel createUserEventPane() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new RoundedBorder(new Insets(5, 5, 5, 5), null));

		panel.add(createUserEventPanel(), BorderLayout.WEST);
		panel.add((userEventFramePanel = createFramePanel()), BorderLayout.EAST);

		panel.setPreferredSize(adjustHeight(panel, 0.38));
		return panel;
	}

	private Component createSeparator() {
		JPanel separator = new JPanel();
		return separator;
	}

	private JPanel createUserEventListPanel() {
		JPanel comboPanel = new JPanel(new BorderLayout(5, 0));
		comboPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
		comboPanel.setPreferredSize(new Dimension(500, 100));
		comboPanel.setMinimumSize(comboPanel.getPreferredSize());

		userEventListModel = new DefaultListModel<>();
		for (UserEventItem item : populateUserEventList()) {
			userEventListModel.addElement(item);
		}
		userEventJList = new JList<>(userEventListModel);
		userEventJList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int row = userEventJList.getSelectedValue().row;
				selectUserEvent(row);
			}
		});

		JScrollPane listScrollPanel = new JScrollPane(userEventJList);
		listScrollPanel.setPreferredSize(new Dimension(500, 100));
		comboPanel.add(listScrollPanel);
		return comboPanel;
	}

	private JPanel createSegmentPanel() {
		Dimension imageDimensions = new Dimension(fWidth, fHeight);
		segmentPanel = new JPanel(new BorderLayout());

		// Position slider to video startup time
		segmentPanel.add(buildLabelPanel("sliderdialog.segmentlist.label"), BorderLayout.NORTH);

		JPanel innerPanel = new JPanel(new VerticalLayout());
		innerPanel.add(createSegmentListPanel());

		if (showThumbnail) {
			innerPanel.add(buildSegmentThumbnailPanel());
		}

		segmentPanel.add(buildSegmentSliderPanel(), BorderLayout.SOUTH);	
		segmentPanel.add(innerPanel, BorderLayout.CENTER);

		segmentPanel.setMaximumSize(imageDimensions);
		segmentPanel.setMinimumSize(segmentPanel.getPreferredSize());
		return segmentPanel;
	}
	
	public JPanel createSegmentListPanel() {
		JPanel comboPanel = new JPanel(new BorderLayout(5, 0));
		comboPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
		comboPanel.setPreferredSize(new Dimension(500, 70));
		comboPanel.setMinimumSize(comboPanel.getPreferredSize());

		segmentListModel = new DefaultListModel<>();
		for (SegmentItem item : populateSegmentList()) {
			segmentListModel.addElement(item);
		}
		segmentJList = new JList<>(segmentListModel);
		segmentJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		segmentJList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int row = segmentJList.getSelectedValue().row;
				selectSegment(row);
			}
		});

		JScrollPane listScrollPanel = new JScrollPane(segmentJList);
		listScrollPanel.setPreferredSize(new Dimension(500, 50));
		listScrollPanel.setMaximumSize(listScrollPanel.getPreferredSize());
		comboPanel.add(listScrollPanel);
		return comboPanel;
	}

	public JPanel buildSegmentThumbnailPanel() {
		segmentThumbnailImagePanel = new JPanel(new BorderLayout());
		segmentThumbnailImagePanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
		segmentThumbnailImagePanel.add(thumbnailImgLabel);
		segmentThumbnailImagePanel.setSize(segmentThumbnailImagePanel.getPreferredSize());
		return segmentThumbnailImagePanel;
	}

	public JPanel buildSegmentSliderPanel() {

		JPanel segmentSliderPanel = new JPanel();
		segmentSliderPanel.setLayout(new GridBagLayout());

		this.maxValue = setMaxMinValue(deviceVideoDuration + initialDeviceVideoOffset, true);
		this.minValue = setMaxMinValue(initialDeviceVideoOffset >= 0 ? initialDeviceVideoOffset : 0, false);
		segmentSlider = createSlider(JSlider.HORIZONTAL, minValue, maxValue, minValue, createSegmentSliderListener());
		segmentTimeField = createSegmentStartTimeDisplay();
		
		// +- buttons
		JButton minusTunerBtn = new JButton("-");
		minusTunerBtn.setPreferredSize(getButtonDimension());
		minusTunerBtn.addActionListener(segmentTunerButtonActionListener());

		JButton plusTunerBtn = new JButton("+");
		plusTunerBtn.setPreferredSize(getButtonDimension());
		plusTunerBtn.addActionListener(segmentTunerButtonActionListener());
		
		segmentSliderPanel.add(segmentSlider,    new GridBagConstraints(0, 0, 1, 1, 1.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		segmentSliderPanel.add(segmentTimeField, new GridBagConstraints(1, 0, 1, 1, 0.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 1), 0, 0));
		segmentSliderPanel.add(minusTunerBtn,    new GridBagConstraints(2, 0, 1, 1, 0.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 1), 0, 0));
		segmentSliderPanel.add(plusTunerBtn,     new GridBagConstraints(3, 0, 1, 1, 0.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 2), 0, 0));

		return segmentSliderPanel;
	}
	
	private JPanel createUserEventPanel() {
		userEventPanel = new JPanel(new BorderLayout());
		
		userEventPanel.add(buildLabelPanel("sliderdialog.user.event.list.label"), BorderLayout.NORTH);
		userEventPanel.add(createUserEventListPanel(), BorderLayout.CENTER);
		userEventPanel.add(createUserEventSliderPanel(), BorderLayout.SOUTH);
		
		userEventPanel.setMinimumSize(userEventPanel.getPreferredSize());
		
		return userEventPanel;
	}

	public JPanel createUserEventSliderPanel() {
		JPanel userEventSliderPanel = new JPanel(new GridBagLayout());
		userEventMaxValue = setMaxMinValue(deviceVideoDuration + initialDeviceVideoOffset, true);
		userEventMinValue = setMaxMinValue(initialDeviceVideoOffset >= 0 ? initialDeviceVideoOffset : 0, false);
		userEventSlider   = createSlider(JSlider.HORIZONTAL, minValue, userEventMaxValue, userEventMinValue, createUserEventSliderListener());
		playRequestedTime = createUserEventStartTimeDisplay();

		// +- buttons
		JButton minusTunerBtnUE = new JButton("-");
		minusTunerBtnUE.setPreferredSize(getButtonDimension());
		minusTunerBtnUE.addActionListener(tunerUESliderButtonActionListener());

		JButton plusTunerBtnUE = new JButton("+");
		plusTunerBtnUE.setPreferredSize(getButtonDimension());
		plusTunerBtnUE.addActionListener(tunerUESliderButtonActionListener());
		
		userEventSliderPanel.add(userEventSlider, 	new GridBagConstraints(0, 0, 1, 1, 2.00, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		userEventSliderPanel.add(playRequestedTime, new GridBagConstraints(1, 0, 1, 1, 0.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 1), 0, 0));
		userEventSliderPanel.add(minusTunerBtnUE, 	new GridBagConstraints(2, 0, 1, 1, 0.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 1), 0, 0));
		userEventSliderPanel.add(plusTunerBtnUE, 	new GridBagConstraints(3, 0, 1, 1, 0.35, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 2), 0, 0));
		return userEventSliderPanel;
	}

	private Dimension getButtonDimension() {
		if (Util.isMacOS()) {
			return new Dimension(5, 15);
		}
		return new Dimension(20,20);
	}

	/**
	 * Generate a JSlider
	 * 
	 * @param orientation
	 * @param minValue
	 * @param maxValue
	 * @param value
	 * @param changeListener
	 * @return
	 */
	public JSlider createSlider(int orientation, int minValue, int maxValue, int value, ChangeListener changeListener) {
		JSlider slider = new JSlider(orientation, minValue, maxValue, value);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.addChangeListener(changeListener);
		return slider;
	}

	public JTextField createUserEventStartTimeDisplay() {
		JTextField playRequestedTime = new JTextField("       0");
		playRequestedTime.setText(Double.toString(getStartTime()));
		playRequestedTime.addActionListener(startTimeTextFieldActionListener(playRequestedTime, userEventSlider));
		playRequestedTime.addKeyListener(new KeyListener() {

			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				// User Events
				if (e.getKeyCode() == MAC_COMMAND_KEY || e.getKeyCode() == PC_ALT_KEY) {
					updateStartTime(playRequestedTime, userEventSlider);
					updateSegmentSelection(Double.valueOf(playRequestedTime.getText()));
				}
			}
		});
		return playRequestedTime;
	}
	
	public JPanel createUserEventNoteLabelPanel() {
		JPanel userEventNoteLabelPanel = new JPanel(new BorderLayout());
		userEventNoteLabelPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		userEventNoteLabelPanel.add(new JLabel(resourceBundle.getString("startupdelay.hint.userevent.note.label")));
		return userEventNoteLabelPanel;
	}

	protected void updateUserEventSelection(double sTime) {
		if (!allUserEventList.isEmpty()) {

			Double key = sTime;
			if (!activeUserEventMap.containsKey(sTime)) {
				key = activeUserEventMap.floorKey(sTime);
			}
			int row = key != null ? (activeUserEventMap.headMap(key)).size() : 0;
			updateUserEventSelection(row);
		}
	}

	public void updateUserEventSelection(int row) {
		selectUserEvent(row);
		makeUserEventSelection(row);
		userEventJList.setSelectedValue(userEventJList.getModel().getElementAt(row), true);
	}

	protected void updateSegmentSelection(double sTime) {

		VideoEvent videoEvent;
		if ((videoEvent = activeSegmentMap.get(sTime)) == null) {
			Double floorKey = activeSegmentMap.floorKey(sTime);
			if (floorKey != null) {
				videoEvent = activeSegmentMap.get(floorKey);
				setTimeJTextField(playRequestedTime, videoEvent.getPlayTime());
			}
			makeSegmentSelection(activeSegmentList.indexOf(videoEvent));
		}
	}

	private JPanel videoStreamNamePane() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		JLabel name = new JLabel(MessageFormat.format(resourceBundle.getString("videotab.manifest.name.time"), streamViewIndex, String.format("%.03f", manifestRequestTime)));
		name.setFont(new Font("Dialog", Font.BOLD, 14));
		name.setBackground(Color.WHITE);
		panel.add(name, BorderLayout.WEST);
		return panel;
	}

	public JTextField createSegmentStartTimeDisplay() {
		JTextField startTimeField = new JTextField("       0");
		startTimeField.addActionListener(startTimeTextFieldActionListener(startTimeField, segmentSlider));
		startTimeField.addKeyListener(new KeyListener() {

			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				// Segment
				if (e.getKeyCode() == MAC_COMMAND_KEY || e.getKeyCode() == PC_ALT_KEY) {
					updateStartTime(startTimeField, segmentSlider);
					updateUserEventSelection(getStartTime());
				}
			}
		});
		setTimeJTextField(startTimeField, getStartTime());
		return startTimeField;
	}

	public ActionListener segmentTunerButtonActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (button.getText().trim().equals("+")) {
					int sliderValue = (segmentSlider.getValue());
					segmentSlider.setValue(++sliderValue);
					segmentSlider.firePropertyChange("value", sliderValue - 1, sliderValue);
				} else {
					int sliderValue = (segmentSlider.getValue());
					segmentSlider.setValue(--sliderValue);
					segmentSlider.firePropertyChange("value", sliderValue + 1, sliderValue);
				}
			}
		};
	}

	public ActionListener tunerUESliderButtonActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (button.getText().trim().equals("+")) {
					int sliderValue = (userEventSlider.getValue());
					userEventSlider.setValue(++sliderValue);
					userEventSlider.firePropertyChange("value", sliderValue - 1, sliderValue);
				} else {
					int sliderValue = (userEventSlider.getValue());
					userEventSlider.setValue(--sliderValue);
					userEventSlider.firePropertyChange("value", sliderValue + 1, sliderValue);
				}
			}
		};
	}

	public void showWarningMessage() {
		JOptionPane.showMessageDialog(StartupDelayDialog.this
				, resourceBundle.getString("startupdelay.warning.dialog.must.choose")
				, resourceBundle.getString("startupdelay.warning.dialog.title")
				, JOptionPane.WARNING_MESSAGE);
	}

	public void showUserEventWarningMessage() {
		JOptionPane.showMessageDialog(StartupDelayDialog.this
				, resourceBundle.getString("startupdelay.warning.dialog.event.segment.sequence")
				, resourceBundle.getString("startupdelay.warning.dialog.title")
				, JOptionPane.WARNING_MESSAGE);
	}

	public ChangeListener createSegmentSliderListener() {
		return new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				segmentFramePanel.setSeconds(((JSlider) e.getSource()).getValue());
				segmentFramePanel.setImage(requestFrame(segmentFramePanel.getSeconds()));
				setTimeJTextField(segmentTimeField, segmentFramePanel.getSeconds());
				segmentTimeField.revalidate();
			}
		};
	}

	public ChangeListener createUserEventSliderListener() {
		return new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				userEventFramePanel.setSeconds(((JSlider) e.getSource()).getValue());
				userEventFramePanel.setImage(requestFrame(userEventFramePanel.getSeconds()));
				setTimeJTextField(playRequestedTime, userEventFramePanel.getSeconds());
				playRequestedTime.revalidate();
			}
		};
	}

	public void updateStartTime(JTextField timeTextField, JSlider slider) {
		double tempTime;

		if (timeTextField != null) {
			try {
				tempTime = Double.valueOf(timeTextField.getText());
				if (tempTime > deviceVideoDuration) {
					tempTime = deviceVideoDuration;
					setTimeJTextField(timeTextField, deviceVideoDuration + initialDeviceVideoOffset);
				}
				setStartTime(tempTime);
				int oldValue = slider.getValue();
				int newValue = (int) Math.round(tempTime * 25);
				slider.setValue(newValue);
				slider.firePropertyChange("value", oldValue, newValue);
			} catch (NumberFormatException e) {
				LOG.debug("Failed to parse bad data in timeTextField:" + timeTextField.getText());
				tempTime = getStartTime();
			}
		} else {
			tempTime = 0;
		}
		setStartTime(tempTime);
	}

	public ActionListener startTimeTextFieldActionListener(JTextField startTimeField, JSlider slider) {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// enter or return has been pressed in startTimeField
				try {
					double sec = Double.valueOf(e.getActionCommand());
					if (sec < 0) {
						// Handle user entered parsable bad data by replacing with the original data
						setTimeJTextField(startTimeField, getStartTime());
					}
					slider.setValue((int) sec * 25);
					setStartTime(sec);
					launchStartupCalculations(sec);
				} catch (NumberFormatException e1) {
					// Handle user entered non-parsable bad data by replacing with the original data
					setTimeJTextField(startTimeField, getStartTime());
				}
			}
		};
	}

	/**
	 * need to be a radio button box here toggle only if new * old are different
	 * 
	 * Need to uncheck previous
	 * Need to set check on newly selected
	 * @param i 
	 */
	public void selectSegment(int row) {
		if (row < 0 || segmentItemList.isEmpty()) {
			return;
		}
		if (segmentChosen == null) {
			segmentChosen = segmentItemList.get(row);
			segmentChosen.setSelected(true);
			segmentJList.getModel().getElementAt(row).setSelected(true);
			thumbnailView(segmentChosen.getVideoEvent());
		}
		SegmentItem selectedSegment = segmentItemList.get(row);
		if (!selectedSegment.equals(segmentChosen)) {
			segmentJList.getModel().getElementAt(segmentChosen.getRow()).setSelected(false);
			segmentJList.getModel().getElementAt(row).setSelected(true);
			segmentFramePanel.setImage(requestFrame(selectedSegment.getVideoEvent().getPlayTime()));
			segmentChosen = selectedSegment;
		}
		
		thumbnailView(segmentChosen.getVideoEvent());
		setTimeJTextField(segmentTimeField, selectedSegment.getVideoEvent().getPlayTime());
		segmentSlider.setValue((int) Math.round(segmentChosen.getVideoEvent().getPlayTime() * 25));
		segmentSlider.revalidate();
		segmentJList.ensureIndexIsVisible(row);
		LOG.debug(String.format("%d %d:%s", segmentJList.getSelectedIndex(), row, segmentJList.getModel().getElementAt(row)));
	}

	private void thumbnailView(VideoEvent videoEvent) {
		if (videoEvent.getSegmentInfo().isThumbnailExtracted()) {
			originalThumbnail = videoEvent.getImageOriginal();
			ImageIcon img = new ImageIcon(originalThumbnail);
			img = new ImageIcon(img.getImage().getScaledInstance(-1, thumbnailHeight, Image.SCALE_DEFAULT));
			thumbnailImgLabel.setIcon(img);
			segmentPanel.setSize(segmentPanel.getPreferredSize());
			segmentPanel.revalidate();
		}
	}

	private double getUserEventTimeStamp(UserEventItem itemObject) {
		return getUserEventTimeStamp(itemObject.getUserEvent());
	}

	private double getUserEventTimeStamp(UserEvent userEvent) {
		if (userEvent != null) {
			return userEvent.getPressTime() != 0 
					? userEvent.getPressTime() 
					: userEvent.getReleaseTime();
		} else {
			return 0;
		}
	}

	public void selectUserEvent(int row) {
		if (row < 0 || userEventItemList.isEmpty()) {
			return;
		}
		if (userEventChosen == null) {
			userEventChosen = userEventItemList.get(row);
			userEventChosen.setSelected(true);
			userEventJList.getModel().getElementAt(row).setSelected(true);
		}
		UserEventItem selectedUserEvent = userEventItemList.get(row);
		if (!selectedUserEvent.equals(userEventChosen)) {
			userEventJList.getModel().getElementAt(userEventChosen.getRow()).setSelected(false);
			userEventJList.getModel().getElementAt(row).setSelected(true);
			userEventFramePanel.setImage(requestFrame(selectedUserEvent.getUserEvent().getPressTime()));
			userEventChosen = selectedUserEvent;
		}
		setTimeJTextField(playRequestedTime, getUserEventTimeStamp(selectedUserEvent));
		userEventSlider.setValue((int) Math.round(getUserEventTimeStamp(userEventChosen) * 25));
		userEventSlider.revalidate();
		userEventJList.ensureIndexIsVisible(row);
	}
	
	public VideoEvent makeSegmentSelection(int indexKey) {
		if (indexKey < 0 || indexKey >= activeSegmentList.size()) {
			return activeSegmentList.get(0);
		}
		VideoEvent segment = activeSegmentList.get(indexKey);
		for (int index = 0; index < segmentItemList.size(); index++) {
			if (segmentItemList.get(index).getVideoEvent().equals(segment)) {
				selectedSegmentIndex = index;
				return segment;
			}
		}
		return activeSegmentList.get(0);
	}
	
	/**
	 * Locate UserEvent in allUserEventList/userEventTableItems
	 * Report index in field variable selectedIdxUE
	 * make selection in userEventJTable and firePropertyChange
	 * 
	 * @param indexKey
	 * @return UserEvent
	 */
	public UserEvent makeUserEventSelection(int indexKey) {

		UserEventItem userEventItem;
		selectedIdxUE = 0;

		if (indexKey < 0 || indexKey >= allUserEventList.size()) {
			userEventItem = userEventItemList.get(selectedIdxUE);
		} else {
			UserEvent userEvent = allUserEventList.get(indexKey);
			userEventItem = userEventItemList.get(indexKey);
			if (userEventItem.getUserEvent().equals(userEvent)) {
				selectedIdxUE = indexKey;
			} else {
				for (int index = 0; index < userEventItemList.size(); index++) {
					if (userEventItemList.get(index).getUserEvent().equals(userEvent)) {
						userEventItem = userEventItemList.get(index);
						selectedIdxUE = index;
						break;
					}
				}
			}
			userEventItem.setSelected(true);
		}
		return userEventItem.getUserEvent();
	}

	private int findUserEventIndex(UserEvent userEvent) {
		int index;
		try {
			if ((CollectionUtils.isEmpty(allUserEventList) || userEvent == null) || ((index = allUserEventList.indexOf(userEvent))) < 0) {
				index = 0;
			}
		} catch (Exception e) {
			index = 0;
		}
		return index;
	}

	public double loadStartupDelay() {

		Double tempStartupTime = ((TraceDirectoryResult) traceData.getAnalyzerResult().getTraceresult()).getVideoStartTime();
		if ((videoStreamStartupData = ((TraceDirectoryResult) traceData.getAnalyzerResult().getTraceresult()).getVideoStartupData()) != null) {
			if ((videoStreamStartup = videoSegmentAnalyzer.findStartupFromName(videoStreamStartupData, videoStream)) != null) {
				tempStartupTime = videoStreamStartup.getStartupTime();
			}
		} else {
			videoStreamStartupData = new VideoStreamStartupData();
		}
		if (videoStreamStartup == null) {
			videoStreamStartup = new VideoStreamStartup(videoStream.getManifest().getVideoName());
			videoStreamStartupData.getStreams().add(videoStreamStartup);
		}
		if (!allUserEventList.isEmpty() && videoStreamStartup.getUserEvent() == null) {

			UserEvent ue = findPriorUserEvent(manifestRequestTime);
			if (ue != null) {
				videoStreamStartup.setUserEvent(ue);
			}
			int lastIndex = 0;
			for (int idx = 0; idx < allUserEventList.size(); idx++) {
				if (allUserEventList.get(idx).getPressTime() > manifestRequestTime) {
					break;
				}
				lastIndex = idx;
			}
			videoStreamStartup.setUserEvent(allUserEventList.get(lastIndex));
		}
		((TraceDirectoryResult) traceData.getAnalyzerResult().getTraceresult()).setVideoStartupData(videoStreamStartupData);

		if (tempStartupTime == 0) { // as in based on Manifest request timeStamp
			return getStartTime();
		}
		return tempStartupTime;
	}

	private void saveStartupDelay(double startupTime, VideoEvent videoEvent, VideoStream videoStream2, UserEventItem userEventItemChosen) throws Exception {
		LOG.debug("Saving startup delay");
		videoStreamStartup.setStartupTime(startupTime);
		if (userEventItemChosen != null) {
			videoStreamStartup.setUserEvent(userEventItemChosen.getUserEvent());
		}
		videoStreamStartup.setManifestName(videoStream2.getManifest().getVideoName());
		videoStreamStartup.setFirstSegID(videoEvent.getSegmentID());
		videoStreamStartup.setPlayRequestedTime(videoEvent.getPlayRequestedTime());
		videoStreamStartup.setManifestReqTime(videoStream2.getManifest().getRequestTime());

		videoStartupReadWrite.save(traceFolder, videoStreamStartupData);
	}

	private void updateSegmentJListData() {
		ListModel<SegmentItem> listModel = segmentJList.getModel();
		for (int idx = 0; idx < segmentItemList.size(); idx++) {
			listModel.getElementAt(idx).setSelected(false);
		}
	}

	private void updateUserEventJListData() {
		ListModel<UserEventItem> listModel = userEventJList.getModel();
		for (int idx = 0; idx < userEventItemList.size(); idx++) {
			listModel.getElementAt(idx).setSelected(false);
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

	public List<UserEventItem> populateUserEventList() {
		userEventItemList.clear();
		for (int idx = 0; idx < allUserEventList.size(); idx++) {
			userEventItemList.add(new UserEventItem(allUserEventList.get(idx), idx));
		}
		return userEventItemList;
	}

	public List<SegmentItem>  populateSegmentList() {
		segmentItemList.clear();
		VideoEvent vEvent;
		for (int index = 0; index < activeSegmentList.size(); index++) {
			vEvent = activeSegmentList.get(index);
			if (vEvent.getSegmentInfo().isVideo()) {
				if (segmentChosen != null && vEvent.equals(segmentChosen.getVideoEvent())) {
					segmentItemList.add(segmentChosen);
				} else {
					segmentItemList.add(new SegmentItem(vEvent, index));
				}
			}
		}
		Collections.sort(segmentItemList, new Comparator<SegmentItem>() {
			@Override
			public int compare(SegmentItem o1, SegmentItem o2) {
				if (o1.getVideoEvent().getSegmentID() < o2.getVideoEvent().getSegmentID()) {
					return -1;
				} else if (o1.getVideoEvent().getSegmentID() > o2.getVideoEvent().getSegmentID()) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return segmentItemList;
	}

	/**
	 * 
	 * @param startupTime
	 */
	public void launchStartupCalculations(double startupTime) {
		try {
			if (startupTime >= segmentChosen.getVideoEvent().getEndTS() && segmentChosen.isSelected()) {
				if (videoStreamStartupData == null) {
					saveStartupDelay(startupTime, segmentChosen.getVideoEvent(), videoStream, userEventChosen);
				}
				videoSegmentAnalyzer.propagatePlaytime(startupTime, segmentChosen.getVideoEvent(), videoStream);

				segmentChosen.getVideoEvent().setPlayTime(startupTime);
				videoStream.setVideoPlayBackTime(startupTime);

				for (VideoStream stream : segmentTablePanel.getVideoStreamCollection()) {
					if (!stream.equals(videoStream)) {
						stream.setCurrentStream(false);
					} else {
						stream.setCurrentStream(true);
					}
				}

				if (!allUserEventList.isEmpty()) {
					segmentChosen.getVideoEvent().setPlayRequestedTime(Double.valueOf(playRequestedTime.getText()));
					videoStream.setPlayRequestedTime(Double.valueOf(playRequestedTime.getText()));
				}
				videoStream.getManifest().setDelay(startupTime - segmentChosen.getVideoEvent().getEndTS());
				videoStream.getManifest().setStartupVideoEvent(segmentChosen.getVideoEvent());
				videoStream.getManifest().setStartupDelay(segmentChosen.getVideoEvent().getSegmentStartTime() - videoStream.getManifest().getRequestTime());

				LOG.info(String.format("Segment playTime = %.03f", segmentChosen.getVideoEvent().getPlayTime()));
				setTimeJTextField(segmentTimeField, segmentChosen.getVideoEvent().getPlayTime());
				revalidate();

				for (VideoStream stream : streamingVideoData.getVideoStreamMap().values()) {
					if (stream.equals(videoStream)) {
						stream.setSelected(true);
					} else {
						stream.setSelected(false);
					}
				}

				AROTraceData aroTraceData = mainFrame.getController().getTheModel();

				streamingVideoData.scanVideoStreams();

				IVideoBestPractices videoBestPractices = ContextAware.getAROConfigContext().getBean(IVideoBestPractices.class);
				videoBestPractices.analyze(aroTraceData);

				getGraphPanel().setTraceData(aroTraceData);

				// StartupDelay calculations
				AROTraceData traceData = vcPlot.refreshPlot(getGraphPanel().getSubplotMap().get(ChartPlotOptions.VIDEO_CHUNKS).getPlot()
															, getGraphPanel().getTraceData()
															, startupTime, segmentChosen.getVideoEvent());

				getGraphPanel().setTraceData(traceData);

				VideoManifestPanel videoManifestPanel = segmentTablePanel.getVideoManifestPanel();
				videoManifestPanel.refresh(segmentTablePanel.getAnalyzerResult());

				saveStartupDelay(startupTime, segmentChosen.getVideoEvent(), videoStream, userEventChosen);

				mainFrame.refreshBestPracticesTab();
				destroy();
			}
		} catch (Exception ex) {
			LOG.error("Error generating video chunk and buffer plots", ex);
			MessageDialogFactory.showMessageDialog(parentPanel, "Error in drawing buffer graphs", "Failed to generate buffer plots", JOptionPane.ERROR_MESSAGE);
		}
	}

	public GraphPanel getGraphPanel() {
		return this.parentPanel;
	}

	public ActionListener createUserEventsButtonListener(final VideoChunksPlot vcPlot) {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ((!allUserEventList.isEmpty()) && Double.valueOf(playRequestedTime.getText()) > Double.valueOf(segmentTimeField.getText())) {
					showUserEventWarningMessage();
					return;
				}
				if (e.getActionCommand().equals("Cancel")) {
					destroy();
				} else {
					if (segmentChosen != null) {
						segmentChosen.setSelected(true);
						destroy();
						try {
							LOG.info("startTimeField :" + segmentTimeField.getText());
							setStartTime(Double.valueOf(segmentTimeField.getText()));
						} catch (NumberFormatException e1) {
							// covers user entered bad data, just go with getStartTime() value
							LOG.error("NumberFormatException: ", e1);
						}
						launchStartupCalculations(getStartTime());
					} else {
						showWarningMessage();
					}
				}
			}
		};

	}

	/**
	 * Get the ceiling key, with provisions for a defaultKey. the defaultKey has a magic value of -1D which causes lastKey to be sent if ceilingKey is null
	 * 
	 * @param key
	 * @param defaultKey
	 * @return ceilingKey, or defaultKey or lastKey()
	 */
	public Double getCeilingKey(Double key, Double defaultKey) {

		Double foundKey = frameMap.ceilingKey(key);
		if (foundKey == null) {
			foundKey = defaultKey == -1D ? frameMap.lastKey() : defaultKey;
		}
		return foundKey;
	}

	public Double getFloorKey(Double key, Double defaultKey) {
		Double foundKey = frameMap.floorKey(key);
		if (foundKey == null) {
			return defaultKey;
		}
		return foundKey;
	}

	@Getter
	@Setter
	private static class UserEventItem {
		private String value;
		private UserEvent userEvent;
		private boolean isSelected;
		private int row;

		UserEventItem(UserEvent userEvent, int row) {
			StringBuffer sb = new StringBuffer();
			sb.append("User Event type: ").append(userEvent.getEventType());
			sb.append(", Time: ").append(String.format("%.03f", userEvent.getPressTime())).append(" s");

			this.value = sb.toString();
			this.row = row;
			this.userEvent = userEvent;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	@Getter
	@Setter
	private static class SegmentItem {
		private String value;
		private VideoEvent videoEvent;
		private boolean selected;
		private int row;

		SegmentItem(VideoEvent videoEvent, int row) {
			StringBuffer sb = new StringBuffer();
			
			sb.append("Segment: ").append(String.format("%.0f", videoEvent.getSegmentID()));
			sb.append(", Track: ").append(videoEvent.getQuality());
			sb.append(", DL Time Range: ").append(String.format("%.03f - %.03f", videoEvent.getStartTS(), videoEvent.getEndTS()));
			if (videoEvent.getPlayTime() != 0) {
				sb.append(", Play: ").append(String.format("%.03f", videoEvent.getPlayTime()));
			} else {	
				sb.append(", mSec: ").append(String.format("%.03f", videoEvent.getSegmentStartTime()));
			}
			this.value = sb.toString();
			this.row = row;
			this.videoEvent = videoEvent;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}