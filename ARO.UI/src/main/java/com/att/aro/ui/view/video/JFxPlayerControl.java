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

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

@SuppressWarnings("restriction")
public class JFxPlayerControl {

	private MediaPlayer mediaPlayer;
	private MediaView mediaView;
	private HBox controllerBar;
	private Slider timeSlider;
	private Label playTime;
	private Button playButton;
	// 100 is a temp value, will be reset when video duration value is available
	private double sliderMax = 100; 
	private Duration duration;
	private StackPane stackPane;
	private DiagnosticsTab diagnosticTab;
	private double videoOffset;
	private VideoSyncThread diagnosticTabSynThread;	
	private JFxPlayer jfxPlayer;
	private static final Logger LOGGER = LogManager.getLogger(JFxPlayerControl.class);	
	private SharedAttributesProcesses aroView;
	 /*
	  *  We multiply video duration by it to be the max value of  
	  *  the slider of the player so that we can see every frame 
	  *  when we increment the slider in the slider dialog box.
	  */
	private static final int FRAME_RATE = VideoUtil.FRAME_RATE;

	public JFxPlayerControl(SharedAttributesProcesses aroView, JFxPlayer jfxPlayer, final MediaPlayer mediaPlayer, DiagnosticsTab diagnosticTab, 
			double videoOffset) {

		this.aroView = aroView;
		this.jfxPlayer = jfxPlayer;
		this.mediaPlayer = mediaPlayer;
		this.diagnosticTab = diagnosticTab;
		this.videoOffset = videoOffset;
		setUpUI();
	}

	private void setUpUI() {
		
		mediaView = new MediaView(mediaPlayer);
		setUpMediaView();
		setUpMediaPlayer();

		controllerBar = new HBox();
		setUpControllerBar();
	    		
		stackPane = new StackPane();
	    stackPane.getChildren().addAll(mediaView, controllerBar);
	    stackPane.setStyle("-fx-background-color: rgb(0, 0, 0);"); // black
	    StackPane.setMargin(controllerBar, new Insets(0,8,30,8));
	    StackPane.setAlignment(controllerBar, Pos.BOTTOM_CENTER);
	}

	private void setUpMediaView() {
		/* 
		 * Keep video view's height and width ratio and 
		 * force the height or width to match container's 
		 * when container is dragged taller/wider.
		 */
		DoubleProperty mediaViewWidth = mediaView.fitWidthProperty();
		DoubleProperty mediaViewHeight = mediaView.fitHeightProperty();
		mediaViewWidth.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
		mediaViewHeight.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));

		mediaView.setPreserveRatio(true);	
	}

	private void setUpControllerBar() {

		controllerBar.setAlignment(Pos.CENTER);
		controllerBar.setPadding(new Insets(5,10,5,10));

		playButton = new Button(">");
		
		playButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Status status = mediaPlayer.getStatus();

				if (status == Status.UNKNOWN || status == Status.HALTED) {
					return;
				}
				
				if (status == Status.PAUSED || status == Status.READY
						|| status == Status.STOPPED) {
					mediaPlayer.play();
				} else {
					mediaPlayer.pause();
				}
			}
		});
		
		controllerBar.getChildren().add(playButton);
		
		Label spacer = new Label("  ");
		controllerBar.getChildren().add(spacer);

		timeSlider = new Slider(0, sliderMax, 0); 
		HBox.setHgrow(timeSlider, Priority.ALWAYS);
		timeSlider.setMaxWidth(Double.MAX_VALUE);
		
		timeSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				double newTime = ((Slider) event.getSource()).getValue();
				Duration newDuration = duration.multiply(newTime/sliderMax);
				mediaPlayer.seek(duration.multiply(newTime/sliderMax));
				diagnosticTab.setTimeLineLinkedComponents(newDuration.toSeconds() + videoOffset, true);
				
				if (newTime == 0 || newTime == sliderMax) {
					mediaPlayer.seek(Duration.ZERO);
					diagnosticTab.setTimeLineLinkedComponents(videoOffset, true);
				};
				
				updateTimeLabel();
			}	
		});
		
		timeSlider.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				double newTime = ((Slider) event.getSource()).getValue();			
				if (newTime == 0 || newTime == sliderMax) {
					mediaPlayer.seek(Duration.ZERO);
					diagnosticTab.setTimeLineLinkedComponents(videoOffset, true);
				}		
				updateTimeLabel();
			}			
		});
		
		controllerBar.getChildren().add(timeSlider);
		
	    playTime = new Label();
	    playTime.setStyle("-fx-text-fill: white;");
	    controllerBar.getChildren().add(playTime);

	    controllerBar.setStyle("-fx-background-color: rgba(120, 120, 120, 0.7);"); // grey
	    controllerBar.setMaxSize(350, 50);
	}

	private void setUpMediaPlayer() {
		
		AnalysisFilter filter = ((MainFrame)aroView).getController().getTheModel().getAnalyzerResult().getFilter();
		if(null != filter){
			Double startTime = filter.getTimeRange().getBeginTime();
			mediaPlayer.setStartTime(new Duration(startTime * 1000));
			updateControllerBar();
		}
		
		mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {	
			@Override
			public void invalidated(Observable arg0) {
				updateControllerBar();
			}
		});
		
		mediaPlayer.setOnPlaying(new Runnable() {
			@Override
			public void run() {
				playButton.setText("||");
				diagnosticTabSynThread = new VideoSyncThread(jfxPlayer, diagnosticTab, videoOffset);
				new Thread(diagnosticTabSynThread).start();
			}
		});

		mediaPlayer.setOnPaused(new Runnable() {		
			@Override
			public void run() {
				playButton.setText(">");
			}
		});
		
		mediaPlayer.setOnStopped(new Runnable() {		
			@Override
			public void run() {
				Double startTime = 0.0;
				AnalysisFilter filter = ((MainFrame)aroView).getController().getTheModel().getAnalyzerResult().getFilter();
				if(null != filter){
					startTime = filter.getTimeRange().getBeginTime();
				}
				mediaPlayer.seek(new Duration(startTime));
				playButton.setText(">");				
				timeSlider.setValue(startTime);
			}
		});
		
		mediaPlayer.setOnReady(new Runnable() {
			@Override
			public void run() {
				// Duration is available when player is ready (not available before that) 
				duration = mediaPlayer.getMedia().getDuration();			
				sliderMax = getVideoDuration()*FRAME_RATE;
				if(timeSlider != null) {
					timeSlider.setMax(sliderMax);
				}
				updateControllerBar();
			}
		});
		
		mediaPlayer.setOnEndOfMedia(new Runnable() {
			@Override
			public void run() {
				Double startTime = 0.0;
				AnalysisFilter filter = ((MainFrame)aroView).getController().getTheModel().getAnalyzerResult().getFilter();
				if(null != filter){
					startTime = filter.getTimeRange().getBeginTime();
				}
				mediaPlayer.seek(new Duration(startTime));
				mediaPlayer.pause();
				playButton.setText(">");
				timeSlider.setValue(startTime);
				updateTimeLabel();
			}
		});
	}

	// Moving slider and updating time label's value
	private void updateControllerBar() {		
	    if (playTime != null && timeSlider != null) { 
		    Platform.runLater(new Runnable() {
		        @SuppressWarnings("deprecation")
				public void run() {
		          Duration currentTime = mediaPlayer.getCurrentTime();
		          playTime.setText(formatTime(currentTime, duration));
		          if (duration == null) {
		        	  LOGGER.error("Video duration is unavailable");
		        	  return;
		          }
		          timeSlider.setDisable(duration.isUnknown());
		          if (!timeSlider.isDisabled() 
		            && duration.greaterThan(Duration.ZERO) 
		            && !timeSlider.isValueChanging()) {
		              timeSlider.setValue(currentTime.divide(duration).toMillis()*sliderMax);
		          }
		        }
		    });
	    }
	}

	private void updateTimeLabel() {		
	    if (playTime != null) { 
		    playTime.setText(formatTime(mediaPlayer.getCurrentTime(), duration));
	    }
	}
	
    private static String formatTime(Duration elapsed, Duration duration) {
	 
    	if (elapsed == null || duration == null) {
    		LOGGER.error("Missing time info. Elapsed: " + elapsed + ", Duration: " + duration);
    		return "";
    	}
    	
	    int intElapsed = (int) Math.floor(elapsed.toSeconds());
	    int elapsedHours = intElapsed / (60 * 60);
	    if (elapsedHours > 0) {
	        intElapsed -= elapsedHours * 60 * 60;
	    }
	    int elapsedMinutes = intElapsed / 60;
	    int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 
	                            - elapsedMinutes * 60;
	 
	    if (duration.greaterThan(Duration.ZERO)) {
	        int intDuration = (int)Math.floor(duration.toSeconds());
	        int durationHours = intDuration / (60 * 60);
	        if (durationHours > 0) {
	           intDuration -= durationHours * 60 * 60;
	        }
	        int durationMinutes = intDuration / 60;
	        int durationSeconds = intDuration - durationHours * 60 * 60 - 
	            durationMinutes * 60;
	        if (durationHours > 0) {
	            return String.format("%d:%02d:%02d/%d:%02d:%02d", 
	                elapsedHours, elapsedMinutes, elapsedSeconds,
	                durationHours, durationMinutes, durationSeconds);
	        } else {
	            return String.format("%02d:%02d/%02d:%02d",
	                elapsedMinutes, elapsedSeconds,durationMinutes, 
	                  durationSeconds);
	        }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours, 
                       elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d",elapsedMinutes, 
                       elapsedSeconds);
            }
        }
    }
    
 	protected StackPane getControlPane() {
 		return stackPane;
 	}
 	
 	protected Node getMediaBar() {
 		return controllerBar;
 	}
 	
 	/**
 	 * Returns current media time in seconds.
 	 * @return
 	 */
 	protected double getMediaTime() {
 		return mediaPlayer.getCurrentTime().toSeconds();
 	}
 	
 	protected void setMediaTime(double timeInSeconds) {
 		
 		Duration newMediaTime = new Duration(timeInSeconds*1000);

 		if (newMediaTime.lessThanOrEqualTo(Duration.ZERO)) {
 			mediaPlayer.seek(Duration.ZERO);
 		} else if (newMediaTime.greaterThanOrEqualTo(mediaPlayer.getMedia().getDuration())) {
 			mediaPlayer.seek(mediaPlayer.getMedia().getDuration());
 		} else {
	 		mediaPlayer.seek(newMediaTime);	 		
 		}
 		
 		diagnosticTab.setTimeLineLinkedComponents(newMediaTime.toSeconds() + videoOffset, true);
 		updateControllerBar();
 	}
 	
 	/**
 	 * Returns video duration in seconds.
 	 * 
 	 * @return
 	 */
 	protected double getVideoDuration() { 		
 		if (duration == null) {
 			LOGGER.error("Duration info not ready, need to wait for player to be ready");
 			return 0;
 		}else if(mediaPlayer == null){
 			return -1.0;
 		}
 		return mediaPlayer.getMedia().getDuration().toSeconds();
 	}
 	
 	protected boolean isPlaying() {
 		return mediaPlayer.getStatus() == Status.PLAYING;
 	}
 	
 	protected void dispose() {
		if (mediaPlayer != null) {
			mediaPlayer.dispose();
			mediaPlayer = null;
		}
 	}

}