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

import java.io.File;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;

public interface IVideoPlayer {

	/**
	 * "true" for visible, "false" for invisible.
	 * @param visible
	 */
	void setVisibility(boolean visible);
	
	void setAroAdvancedTab(DiagnosticsTab aroAdvancedTab);

	double getVideoOffset();
	
	/**
	 * Gets video play time in seconds.
	 * @return
	 */
	double getMediaTime();

	/**
	 * Sets the current time position in the video based on input and video offset.
	 * @param hairlineTime currentTime displayed in hairline tracker/packet view
	 */
	void setMediaTime(double hairlineTime);
	
	/**
	 * Gets duration in seconds.
	 * @return
	 */
	double getDuration();
	
	/**
	 * If the player is playing the video.
	 * @return
	 */
	boolean isPlaying();
	
	/**
	 * Called upon loading of a trace.
	 * 
	 * This method can be confusing because of the difference 
	 * in the way a new trace/video is launched among players- 
	 * JMF and JavaFx: a new player instance is created every
	 * time when we load a new trace; VLCJ: player is created 
	 * only once with a new video loaded when we load a new 
	 * trace.
	 */
	void loadVideo(AbstractTraceResult traceResult);
	
	/**
	 * Called upon launching the VO app - launch the player 
	 * GUI without video loaded.
	 */
	void launchPlayer(int xPosition, int yPosition, int frameWidth, int frameHeight);

	void clear();
	
	VideoPlayerType getPlayerType();
	
	void notifyLauncher(boolean enabled);

	String getVideoPath();

	boolean isStarted();
}