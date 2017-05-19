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
package com.att.aro.ui.view.video;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;

import javax.media.CannotRealizeException;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.Time;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import com.att.aro.view.images.Images;

/**
 * Displays the ARO Video Player UI, and provides Video player handling for the Play and Pause functions. The AROVideoPlayer class provides methods for
 * refreshing the video frames when new traces are loaded in the ARO Data Analyzer.
 * 
 *
 *
 */
public class AROVideoPlayer extends JFrame implements ActionListener, IVideoPlayer {

	private static final long serialVersionUID = 1L;
	private static final float PLAYBACK_RATE = 1.0f; // 5.0f
	private static final String outVideoTimeFileName = "exVideo_time";

	private ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);

	private BufferedWriter mTraceVideoTimeStampWriter;
	private OutputStream mTraceVideoTimeStampFile;
	private Component visualComponent;
	private Component controlComponent;
	private double videoOffset;

	private JPanel aroVideoPanel;
	private JLabel jVideoLabel;
	private JButton jButton;
	private boolean syncVideoClicked = false;

	private Player videoPlayer;
	private boolean showInfoMsg = true;
	private File traceFile;

	private AROVideoUtil aroVideoUtil;
	private AbstractTraceResult tdResult;
	private DiagnosticsTab aroAdvancedTab;
	private SharedAttributesProcesses aroView;
	
	public AROVideoPlayer(SharedAttributesProcesses aroView){
    	this.aroView = aroView;
    }

	/* (non-Javadoc)
	 * @see com.att.aro.ui.view.video.IVideoPlayer#setAroAdvancedTab(com.att.aro.ui.view.diagnostictab.DiagnosticsTab)
	 */
	@Override
	public void setAroAdvancedTab(DiagnosticsTab aroAdvancedTab) {
		this.aroAdvancedTab = aroAdvancedTab;
	} 

	// Is this being used? 2016/11/09
	/**
	 * Check if exvideo_time file exist if yes, ask user if he wants to re-sync the video if yes, rollback/reset the videoOffset, delete the existing
	 * exvideo_time file and recreate it with new sync time, stop the video player, reset the blue line and the video slider . Action performed on Sync video
	 * button click event.
	 * 
	 * @param ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		double externalVideoStartTime = 0.0;
		File file;
		if (tdResult.getTraceResultType().equals(TraceResultType.TRACE_FILE)) {
			file = new File(traceFile.getParentFile(), outVideoTimeFileName);
		} else {
			file = new File(traceFile, outVideoTimeFileName);
		}
		if (file.exists()) {
			if (MessageDialogFactory.showConfirmDialog(AROVideoPlayer.this, ResourceBundleHelper.getMessageString("video.error.synchAgain"), "Information",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				// delete the exVideo_time file.
				try {
					file.delete();
				} catch (Exception ex) {
					logger.info("Exception in deleting exVideo_time file. ", ex);
				}
				syncVideoClicked = true;
				Time current = null;
				// synchronized (this) {
				current = videoPlayer.getMediaTime();
				// }
				externalVideoStartTime = (current.getSeconds() + ((double) tdResult.getTraceDateTime().getTime() / 1000));
				this.videoOffset = current.getSeconds();
				aroAdvancedTab.setGraphPanelClicked(false);
				setMediaTime(0.0);

			}
		} else {
			if (videoPlayer != null) {
				/* File does not exist,first time. */
				syncVideoClicked = true;
				Time current = null;
				/*
				 * checking timing with this thread and event handlers for MediaTimeSetEventget in line behind event handler threads
				 */
				// synchronized (this) {
				current = videoPlayer.getMediaTime();
				// }
				externalVideoStartTime = (current.getSeconds() + ((double) tdResult.getTraceDateTime().getTime() / 1000));
				setMediaTime(0.0);
				this.videoOffset = current.getSeconds();

			}
		}
		// Write video_time file.
		try {
			writeVideoTraceTime(Double.toString(externalVideoStartTime));
			closeVideoTraceTimeFile();
		} catch (IOException ioex) {
			logger.info("Exception in close exVideo_time file. ", ioex);

		}
	}

	/**
	 * Refreshes the Video player display using the specified analysis data.
	 * 
	 * @param analysisData
	 *            The analysis data that is used to refresh the video.
	 * @throws IOException
	 */
	private void refresh() throws IOException {
		
		aroVideoUtil = new AROVideoUtil();
		aroAdvancedTab.setVideoPlayer(this);
		
		String mediaUrl = aroVideoUtil.getMediaUrl(traceFile); // file:/Users/barrynelson/AROTraceAndroid/nt/video.mov
		if (traceFile == null || mediaUrl == null) {
			setVideoNotAvailableImage(true);
			return;
		}
		double videoStartTime = tdResult.getVideoStartTime();
		if(videoStartTime < 0.00001) { //checking if the video start time isn't available
			videoStartTime = new FileManagerImpl().getCreatedTime(mediaUrl)*1000;
		}
		MediaLocator mlr = new MediaLocator(mediaUrl);
		try {
			videoPlayer = Manager.createRealizedPlayer(mlr);

		} catch (NoPlayerException noe) {
			MessageDialogFactory.getInstance().showErrorDialog(new Window(new Frame()),
					ResourceBundleHelper.getMessageString("video.error.noplayerexception") + noe.getMessage());
			return;
		} catch (IOException ioe) {
			MessageDialogFactory.getInstance().showErrorDialog(new Window(new Frame()),
					ResourceBundleHelper.getMessageString("video.error.ioexception") + ioe.getMessage());
		} catch (CannotRealizeException cre) {
			MessageDialogFactory.getInstance().showErrorDialog(new Window(new Frame()),
					ResourceBundleHelper.getMessageString("video.error.cannotrealizeexception") + cre.getMessage());
			return;
		}
		this.videoOffset = videoStartTime > 0.0 ? videoStartTime - ((double) tdResult.getTraceDateTime().getTime() / 1000) : 0.0;
		// Resetting the offset if the offset is longer than the video
		if (Math.abs(videoOffset) >= videoPlayer.getDuration().getSeconds()) {
			videoOffset = 0;
		}
		videoPlayer.setRate(PLAYBACK_RATE);
		setVideoNotAvailableImage(false);

		// This is to turn off plug-in settings on video info window, plugin tab
		Control controls[] = videoPlayer.getControls();
		for (int i = 0; i < controls.length; i++) {
			String strControlName = controls[i].toString();
			if (strControlName.contains("BasicJMD")) {
				Component basicJMDComp = controls[i].getControlComponent();
				if (basicJMDComp.getParent() != null) {
					basicJMDComp.getParent().setVisible(false);
				}
				basicJMDComp.setVisible(false);
			}
		}

		controlComponent = videoPlayer.getControlPanelComponent();
		if (controlComponent != null) {
			controlComponent.setVisible(true);
			aroVideoPanel.add(controlComponent, BorderLayout.SOUTH);
		}

		visualComponent = videoPlayer.getVisualComponent();
		if (visualComponent != null) {
			aroVideoPanel.add(visualComponent, BorderLayout.CENTER);
			visualComponent.setVisible(true);
		}
		
		videoPlayer.addControllerListener(new ControllerListener() {
			@Override
			public void controllerUpdate(ControllerEvent evt) {
				if (evt instanceof StartEvent || evt instanceof MediaTimeSetEvent) {
					if ((evt instanceof StartEvent) && showInfoMsg && tdResult.isExVideoTimeFileNotFound()) {
						if (!syncVideoClicked) {
							MessageDialogFactory.showMessageDialog(AROVideoPlayer.this,
									"The Analyzer loaded an external video. The video may not be in Sync with the traces.", "Information", 1);
							showInfoMsg = false;
						} else {
							showInfoMsg = false;
						}
					}
					VideoSyncThread syncThread = new VideoSyncThread(AROVideoPlayer.this, aroAdvancedTab, videoOffset);
					new Thread(syncThread).start();
				}
			}
		});
		setMediaTime(0.0);

		setVisible(true);
		if (!tdResult.isNativeVideo()) {
			jButton.setVisible(true);
		}
		aroAdvancedTab.setGraphPanelClicked(false);
	}

	/**
	 * Clears the video player and its components.
	 * 
	 */
	@Override
	public void clear() {

		// Make sure to remove the components before closing the video player
		if (visualComponent != null) {
			aroVideoPanel.remove(visualComponent);
			visualComponent = null;
		}
		if (controlComponent != null) {
			aroVideoPanel.remove(controlComponent);
			controlComponent = null;
		}

		// ----- Dispose the video player - Start -----
		if (videoPlayer != null) {
			if (videoPlayer.getState() == Controller.Started) {
				videoPlayer.stop();
			}
			videoPlayer.deallocate();
			videoPlayer.close();
			videoPlayer = null;
			videoOffset = 0.0;
		}		
		this.dispose();
		// ----- Dispose the video player - End -----
		
		syncVideoClicked = false;

	}

	/**
	 * Create a file based on the trace directory path.
	 * @param filePath
	 */
	private void createTracePath(String filePath) {
		traceFile = new File(filePath);
	}

	/**
	 * Method to enable the default video player image as per the provided visible state.
	 * @param visible enables/disables the default image content in video player
	 */
	private void setVideoNotAvailableImage(boolean visible) {
		jVideoLabel.setVisible(visible);
	}

	/* (non-Javadoc)
	 * @see com.att.aro.ui.view.video.IVideoPlayer#setMediaTime(double)
	 */
	@Override
	public void setMediaTime(final double hairlineTime) {
		if ((videoPlayer != null) && (videoPlayer.getDuration() != null)) {
			double videoTime = hairlineTime - this.videoOffset;
			if ((videoTime < 0.0)) {
				videoPlayer.stop();
				videoPlayer.setMediaTime(new Time(0.0));
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						aroAdvancedTab.setTimeLineLinkedComponents(hairlineTime, false);
					}
				});
				return;
			} else if (videoTime > videoPlayer.getDuration().getSeconds()) {
				videoPlayer.setMediaTime(new Time(videoPlayer.getDuration().getSeconds()));
				return;
			} else {
				videoPlayer.setMediaTime(new Time(videoTime));
			}

		}
	}

	/* Writes the video_time file to the trace directory.*/
	private void writeVideoTraceTime(String timestamp) throws IOException {
		initVideoTraceTime();
		writeTimetoFile(mTraceVideoTimeStampWriter, timestamp);
	}

	/* Closes the FileOutputStream and BufferedWritter handles. */
	private void closeVideoTraceTimeFile() throws IOException {
		mTraceVideoTimeStampWriter.close();
		mTraceVideoTimeStampFile.close();
	}

	/* Initializes the FileOutputStream and BufferedWriter. */
	private void initVideoTraceTime() throws FileNotFoundException {
		if (aroVideoUtil.isPcaPFile(traceFile)) {
			mTraceVideoTimeStampFile = new FileOutputStream((traceFile.getParentFile().getAbsolutePath()) + File.separator + outVideoTimeFileName);
			mTraceVideoTimeStampWriter = new BufferedWriter(new OutputStreamWriter(mTraceVideoTimeStampFile));

		} else {
			mTraceVideoTimeStampFile = new FileOutputStream((traceFile.getAbsolutePath()) + File.separator + outVideoTimeFileName);
			mTraceVideoTimeStampWriter = new BufferedWriter(new OutputStreamWriter(mTraceVideoTimeStampFile));
		}
	}

	/* Writes the video_time file with externalVideoStartTIme. */
	private void writeTimetoFile(BufferedWriter outputfilewriter, String timestamp) throws IOException {
		final String eol = System.getProperty("line.separator");
		outputfilewriter.write(timestamp + eol);
	}

	/* (non-Javadoc)
	 * @see com.att.aro.ui.view.video.IVideoPlayer#getVideoOffset()
	 */
	@Override
	public double getVideoOffset(){
		return this.videoOffset;
	}
	
	@Override
	public double getMediaTime() {
		
		if (videoPlayer == null) {
			logger.debug("Null Player in AROVideoPlayer");
			return 0;
		}
		
		return videoPlayer.getMediaTime().getSeconds();
	}
	
	@Override
	public double getDuration() {
		if (videoPlayer != null) {
			return videoPlayer.getDuration().getSeconds();
		}
		return -1;
	}
	
	@Override
	public boolean isPlaying() {
		
		if(null != videoPlayer) {
			return videoPlayer.getState() == Controller.Started;
		}
		return false;
	}

	@Override
	public void setVisibility(boolean visible) {
		this.setVisible(visible);
	}

	@Override
	public void loadVideo(AbstractTraceResult traceResult) {	
		tdResult = traceResult;
		createTracePath(tdResult.getTraceDirectory());
		try {
			refresh();
		} catch (IOException e) {
			logger.error("Error launching player", e);
		}	
	}

	@Override
	public VideoPlayerType getPlayerType() {
		return VideoPlayerType.MOV;
	}
	
	@Override
	public void launchPlayer(int xPosition, int yPosition, int frameWidth, int frameHeight) {

		String titleName = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.videoTitle"), 
				ApplicationConfig.getInstance().getAppShortName());
		setTitle(titleName);
		
		this.setIconImage(Images.ICON.getImage());
		
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		
		addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            	aroView.updateVideoPlayerSelected(false);
            }
        });
		
		Dimension frameDim = new Dimension(350, 500);
		setMinimumSize(frameDim);
		setResizable(true);
		jVideoLabel = new JLabel(Images.NO_VIDEO_AVAILABLE.getIcon());
		jButton = new JButton("Sync Video");
		jButton.setBackground(Color.WHITE);
		jButton.addActionListener(this);
		jButton.setPreferredSize(new Dimension(20, 20));
		jButton.setVisible(false);
		aroVideoPanel = new JPanel();
		aroVideoPanel.setLayout(new BorderLayout());
		aroVideoPanel.add(jButton, BorderLayout.NORTH);
		aroVideoPanel.add(jVideoLabel, BorderLayout.CENTER);
		setContentPane(aroVideoPanel);
		
		setBounds(xPosition, yPosition, frameWidth, frameHeight);
		setVisibility(true);
	}
	
	public void stopPlayer(){
		
		if(null != videoPlayer) {
			videoPlayer.stop();
		}
	}

	@Override
	public void notifyLauncher(boolean enabled) {
		aroView.updateVideoPlayerSelected(enabled);
	}
}
