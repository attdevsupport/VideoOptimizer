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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import com.att.aro.view.images.Images;
import javafx.animation.FadeTransition;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.application.Platform;

public class AROMp4Player implements IVideoPlayer {

	private ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);
	private String traceDirectory;
	private AbstractTraceResult traceResult;
	private double videoOffset;
	private DiagnosticsTab diagnosticsTab;
	private Mp4PlayerControl playerControl;
	private MediaPlayer mediaPlayer;
	private JFrame frame;
	private int playbackWidth = 359;
	private int playbackHeight = 660;
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int rtEdge = screenSize.width - playbackWidth;
	
    private void initAndShowGUI() {

        frame = new JFrame();    
        final JFXPanel fxPanel = new JFXPanel();

        frame.setContentPane(fxPanel);
        frame.setBounds(rtEdge, 0, playbackWidth, playbackHeight);
        frame.setMinimumSize(new Dimension(300, 540));
        frame.setSize(359, 660);
        
        String titleName = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.videoTitle"),  
        										ApplicationConfig.getInstance().getAppShortName());
        frame.setTitle(titleName);
        
		frame.setIconImage(Images.ICON.getImage());
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Platform.setImplicitExit(false);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
       });
    }

    private void initFX(JFXPanel fxPanel) {
        Scene scene = createScene();
        fxPanel.setScene(scene);
    }

    private Scene createScene() {
        
    	String videoFile = traceDirectory + System.getProperty("file.separator") 
    	+ ResourceBundleHelper.getMessageString("video.videoFileOnDevice");
    	String mediaUrl = new File(videoFile).toURI().toString();
    	
     	Media media = new Media(mediaUrl);
        mediaPlayer = new MediaPlayer(media);

		VideoSyncThread syncThread = new VideoSyncThread(AROMp4Player.this, diagnosticsTab, videoOffset);
        playerControl = new Mp4PlayerControl(mediaPlayer, diagnosticsTab, syncThread);
        Scene scene = new Scene(playerControl.getControlPane());

 	    final FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), playerControl.getMediaBar());
 	    fadeTransition.setAutoReverse(true);
 	    fadeTransition.setFromValue(0);
 	    fadeTransition.setToValue(1);
	    scene.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<Event>() {
	    	
	    	@Override
	    	public void handle(Event event) {
	    		fadeTransition.setCycleCount(1);
	    		fadeTransition.playFromStart();  		
	    	}
		});
	    
	    scene.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<Event>() {
	    	
	    	@Override
	    	public void handle(Event event) {
	    		fadeTransition.setCycleCount(2);
	    		fadeTransition.playFrom(Duration.millis(500));
	    	}
		});
	    
        return scene;
    }

    private void launchFXPlayer() {
 		// Workaround for exception on Mac
	    try {
	        Class<?> macFontFinderClass = Class.forName("com.sun.t2k.MacFontFinder");
	        Field psNameToPathMap = macFontFinderClass.getDeclaredField("psNameToPathMap");
	        psNameToPathMap.setAccessible(true);
	        psNameToPathMap.set(null, new HashMap<String, String>());
	    } catch (Exception e) {
	        // Ignore
	    }
	    
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	initAndShowGUI();
            }
        });
    }

	@Override
	public void setAroAdvancedTab(DiagnosticsTab diagnosticsTab) {
		this.diagnosticsTab = diagnosticsTab;	
	}

	private void refresh() throws IOException {
		
		double videoStartTime = traceResult.getVideoStartTime();
		this.videoOffset = videoStartTime > 0.0 ? videoStartTime - ((double) traceResult.getTraceDateTime().getTime() / 1000) : 0.0;
		
		// Resetting the offset if the offset is longer than the video
		if (playerControl != null) {
			if (Math.abs(videoOffset) >= playerControl.getVideoDuration()) {
				videoOffset = 0;
			}
		}

		launchFXPlayer();
		
		setMediaTime(0.0);		
		diagnosticsTab.setGraphPanelClicked(false);
	}

	@Override
	public void clear() {
		
		if (playerControl != null) {
			playerControl.dispose();
		}
		
		if (frame != null) {
			frame.dispose();
		}
		
		videoOffset = 0.0;
	}

	@Override
	public double getVideoOffset() {
		return videoOffset;
	}
	
	@Override
	public double getMediaTime() {
		if (playerControl != null) {
			return playerControl.getMediaTime();
		}
		return 0.0;
	}	

	@Override
	public void setMediaTime(final double hairlineTime) {
		double videoTime = hairlineTime - this.videoOffset;
		if ((videoTime < 0.0)) {	
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (diagnosticsTab != null) {
						diagnosticsTab.setTimeLineLinkedComponents(hairlineTime, false);
					}
				}
			});
		}
		if (playerControl != null) {
			playerControl.setMediaTime(videoTime);
		}
	}
	
	@Override
	public double getDuration() {		
		if (playerControl != null) {
			return playerControl.getVideoDuration();
		}
		return 0;		
	}
	
	@Override
	public boolean isPlaying() {
		if (playerControl != null) {
			return playerControl.isPlaying();
		} 
		return false;
	}

	@Override
	public void setVisibility(boolean visible) {		
		if (frame != null) {
			frame.setVisible(visible);
		}
	}

	@Override
	public VideoPlayerType getPlayerType() {
		return VideoPlayerType.MP4;
	}

	@Override
	public void launchPlayer(AbstractTraceResult traceResult) {
		this.traceResult = traceResult;
		traceDirectory = traceResult.getTraceDirectory();
		try {
			refresh();
		} catch (IOException e) {
			logger.error("Error launching player", e);
		}
	}

}
