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

import com.att.aro.core.ILogger;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

public class Mp4PlayerControl {

	private MediaPlayer mediaPlayer;
	private MediaView mediaView;
	private HBox controllerBar;
	private Slider timeSlider;
	private Label playTime;
	private Button playButton;
	private boolean atEndOfMedia = false;
	private boolean stopRequested = false;
	private boolean repeat = false;
	private Duration duration;
	private StackPane stackPane;
	private DiagnosticsTab diagnosticTab;
	private VideoSyncThread diagnosticTabSynThread;	
	private static ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);
	
	public Mp4PlayerControl(final MediaPlayer mediaPlayer, DiagnosticsTab diagnosticTab, VideoSyncThread diagnosticTabSyncThread) {

		this.mediaPlayer = mediaPlayer;
		this.diagnosticTab = diagnosticTab;
		diagnosticTabSynThread = diagnosticTabSyncThread;
		
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
						|| status == Status.STOPPED)
				{
					if (atEndOfMedia) {
						mediaPlayer.seek(mediaPlayer.getStartTime());
						atEndOfMedia = false;
					}
					mediaPlayer.play();
				} else {
					mediaPlayer.pause();
				}
			}
		});
		
		controllerBar.getChildren().add(playButton);
		
		Label spacer = new Label("  ");
		controllerBar.getChildren().add(spacer);

		timeSlider = new Slider();
		HBox.setHgrow(timeSlider, Priority.ALWAYS);
		timeSlider.setMaxWidth(Double.MAX_VALUE);
		
		timeSlider.valueProperty().addListener(new InvalidationListener() {			
			@Override
			public void invalidated(Observable arg0) {
				if (timeSlider.isValueChanging())  {
					mediaPlayer.seek(duration.multiply(timeSlider.getValue() / 100.0));
				}			
			}
		});
		
		timeSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				mediaPlayer.seek(duration.multiply(timeSlider.getValue() / 100.0));		
				diagnosticTab.setTimeLineLinkedComponents(mediaPlayer.getCurrentTime().toSeconds(), true);
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
		
		mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {	
			@Override
			public void invalidated(Observable arg0) {
				updateValues();
			}
		});
		
		mediaPlayer.setOnPlaying(new Runnable() {
			@Override
			public void run() {
				if (stopRequested) {
					mediaPlayer.pause();
					stopRequested = false;
				} else{
					playButton.setText("||");
					new Thread(diagnosticTabSynThread).start();
				}
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
				playButton.setText(">");
				mediaPlayer.seek(mediaPlayer.getStartTime());
			}
		});
		
		mediaPlayer.setOnReady(new Runnable() {
			@Override
			public void run() {
				duration = mediaPlayer.getMedia().getDuration();
				updateValues();
			}
		});
		
		mediaPlayer.setCycleCount(repeat? MediaPlayer.INDEFINITE:1);
		mediaPlayer.setOnEndOfMedia(new Runnable() {
			@Override
			public void run() {
				if (!repeat) {
					playButton.setText(">");
				}
				stopRequested = true;
				atEndOfMedia = true;
			}
		});
	}

	private void updateValues() {
		
	    if (playTime != null && timeSlider != null) { 
		    Platform.runLater(new Runnable() {
		        public void run() {
		          Duration currentTime = mediaPlayer.getCurrentTime();
		          playTime.setText(formatTime(currentTime, duration));
		          if (duration == null) {
		        	  logger.error("Duration: " + duration);
		        	  return;
		          }
		          timeSlider.setDisable(duration.isUnknown());
		          if (!timeSlider.isDisabled() 
		            && duration.greaterThan(Duration.ZERO) 
		            && !timeSlider.isValueChanging()) {
		              timeSlider.setValue(currentTime.divide(duration).toMillis()
		                  * 100.0);
		          }
		        }
		    });
	    }
	}
	
    private static String formatTime(Duration elapsed, Duration duration) {
	 
    	if (elapsed == null || duration == null) {
    		logger.error("Missing time info. Elapsed: " + elapsed + ", Duration: " + duration);
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
 			mediaPlayer.stop();
 		} else if (newMediaTime.greaterThanOrEqualTo(mediaPlayer.getMedia().getDuration())) {
 			mediaPlayer.seek(mediaPlayer.getMedia().getDuration());
 			mediaPlayer.stop();
 		} else {
	 		mediaPlayer.seek(newMediaTime);	 		
	 		if (mediaPlayer.getStatus() == Status.PLAYING) {
	 			mediaPlayer.play();
	 		}
 		}
 		
 		updateValues();
 	}
 	
 	/**
 	 * Returns video duration in seconds.
 	 * 
 	 * @return
 	 */
 	protected double getVideoDuration() { 		
 		return mediaPlayer.getMedia().getDuration().toSeconds();
 	}
 	
 	protected boolean isPlaying() {
 		return mediaPlayer.getStatus() == Status.PLAYING;
 	}
 	
 	protected void dispose() {
 		mediaPlayer.dispose();
 	}

}
