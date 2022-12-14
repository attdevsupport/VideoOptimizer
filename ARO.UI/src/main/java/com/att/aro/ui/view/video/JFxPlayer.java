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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import com.att.aro.view.images.Images;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;

@SuppressWarnings("restriction")
public class JFxPlayer implements IVideoPlayer {

	private static final Logger LOGGER = LogManager.getLogger(JFxPlayer.class);	
	private AbstractTraceResult traceResult;
	private double videoOffset;
	private DiagnosticsTab diagnosticsTab;
	private JFxPlayerControl playerControl;
	private MediaPlayer mediaPlayer;
	private JFrame frame;
	private int playerContentWidth;
	private int playerContentHeight;
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private SharedAttributesProcesses aroView;
    
    @Getter
    private String videoPath;
    @Getter
	private boolean started = false;

	public JFxPlayer(SharedAttributesProcesses aroView){
    	this.aroView = aroView;
    }
		
    private void initAndShowGUI() {

        frame = new JFrame();   
        String titleName = MessageFormat.format(ResourceBundleHelper.getMessageString("aro.videoTitle"),  
        										ApplicationConfig.getInstance().getAppShortName());
        frame.setTitle(titleName);       
		frame.setIconImage(Images.ICON.getImage());
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            	aroView.updateVideoPlayerSelected(false);
            }
        });
        
        final JFXPanel fxPanel = new JFXPanel();
        frame.setContentPane(fxPanel);

        setPlayerControl() ;
		
        if(playerControl != null  && VideoUtil.isVideoLandscape(traceResult)) {
        	playerContentWidth = VideoUtil.PLAYER_CONTENT_WIDTH_LANDSCAPE;
        	playerContentHeight = VideoUtil.PLAYER_CONTENT_HEIGHT_LANDSCAPE;
        } else {
        	playerContentWidth = VideoUtil.PLAYER_CONTENT_WIDTH_PORTRAIT;
        	playerContentHeight = VideoUtil.PLAYER_CONTENT_HEIGHT_PORTRAIT;
        }
        int rtEdge = screenSize.width - playerContentWidth;
        frame.getContentPane().setPreferredSize(new Dimension(playerContentWidth, playerContentHeight));
        frame.setLocation(rtEdge, 0);
        frame.pack();
        frame.setVisible(true);
        
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
        
    	setPlayerControl() ;
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

    private void setPlayerControl() {
    	videoPath = aroView.getVideoFile();
    	String mediaUrl = new File(videoPath).toURI().toString();
    	
     	Media media = new Media(mediaUrl);
        mediaPlayer = new MediaPlayer(media);
        playerControl = new JFxPlayerControl(aroView, this, mediaPlayer, diagnosticsTab, videoOffset);
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
		videoOffset = videoStartTime > 0.0 ? videoStartTime - ((double) traceResult.getTraceDateTime().getTime() / 1000) : 0.0;
		launchFXPlayer();
		
		if (playerControl == null) {
			LOGGER.error("Player control is not available!");
			return;
		}
		
		Double startTime = 0.0;
		AnalysisFilter filter = ((MainFrame)aroView).getController().getTheModel().getAnalyzerResult().getFilter();
		if(null != filter){
			startTime = filter.getTimeRange().getBeginTime();
		}
		setMediaTime(startTime);
		started = true;
		diagnosticsTab.setGraphPanelClicked(false);
	}

	@Override
	public void clear() {
		
		if (playerControl != null) {
			mediaPlayer = null;
			playerControl.dispose();
		}
		
		if (frame != null) {
			frame.setVisible(false);
			frame.dispose();
			frame = null;
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
		if (playerControl != null && mediaPlayer != null) {
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
		return VideoPlayerType.MP4_JFX;
	}

	@Override
	public void loadVideo(AbstractTraceResult traceResult) {
		this.traceResult = traceResult;
		try {
			refresh();
		} catch (IOException e) {
			LOGGER.error("Error launching player", e);
		}
	}

	@Override
	public void launchPlayer(int xPosition, int yPosition, int frameWidth, int frameHeight) {}

	@Override
	public void notifyLauncher(boolean enabled) {
		aroView.updateVideoPlayerSelected(enabled);
	}

	@Override
	public boolean isStarted() {
		return started;
	}
}
