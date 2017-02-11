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
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import com.att.aro.core.ILogger;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;

public class VideoPlayerController implements Observer {

	private IVideoPlayer currentPlayer;
	private List<IVideoPlayer> players;
	private AbstractTraceResult traceResult;
	private String traceDirectory;
	private DiagnosticsTab diagnosticsTab;
	private int playbackWidth = 350;
	private int playbackHeight = 600;
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private int rtEdge = screenSize.width - playbackWidth;
	
	private ILogger logger = ContextAware.getAROConfigContext().getBean(ILogger.class);
	
	public VideoPlayerController(DiagnosticsTab diagnosticTab, List<IVideoPlayer> videoPlayers) {
		this.diagnosticsTab = diagnosticTab;
		players = new ArrayList<IVideoPlayer>();
		for (IVideoPlayer videoPlayer: videoPlayers) {
			videoPlayer.setAroAdvancedTab(diagnosticTab);
			players.add(videoPlayer);
		}
	}
	
	private void setCurrentVideoPlayer(IVideoPlayer player) {
		currentPlayer = player;
	}
	
	public IVideoPlayer getCurrentVideoPlayer() {
		return currentPlayer;
	}
	
	/**
	 * Default player is the one that plays .mov video.
	 * @return
	 */
	public IVideoPlayer getDefaultPlayer() {
		return getMovPlayer();
	}
	
	private void launchPlayer(String traceDirectory) {

		IVideoPlayer mp4Player = getMp4Player();
		IVideoPlayer movPlayer = getMovPlayer();
		
		if (mp4VideoExists(traceDirectory) && mp4Player != null) {
			launchMp4Player();
		} else if (movPlayer != null){
			launchMovPlayer();
		} else {
			logger.error("Error launching video player - no Mp4 or Mov player found");
		}
	}

	private IVideoPlayer getMp4Player() {		
		for (IVideoPlayer player: players) {
			if (player.getPlayerType() == VideoPlayerType.MP4) {
				return player;
			}
		}		
		return null;
	}
	
	private IVideoPlayer getMovPlayer() {		
		for (IVideoPlayer player: players) {
			if (player.getPlayerType() == VideoPlayerType.MOV) {
				return player;
			}
		}		
		return null;
	}
	
	// Making assumption here that there is only 1 Mp4 Player.
	private void launchMp4Player() {
		IVideoPlayer player = getMp4Player();
		player.launchPlayer(traceResult);
		diagnosticsTab.setVideoPlayer(player);
		setCurrentVideoPlayer(player);
	}
	
	// Making assumption here that there is only 1 Mov Player.
	private void launchMovPlayer() {
		IVideoPlayer player = getMovPlayer();
		player.launchPlayer(traceResult);
		diagnosticsTab.setVideoPlayer(player);
		setCurrentVideoPlayer(player);
	}
	
	/*
	 * Returns whether the mp4 video file exists in the trace directory.
	 */
	private boolean mp4VideoExists(String traceDirectory) {
		
		if (traceDirectory == null) {
			logger.error("Trace Dir = " + traceDirectory);
			return false;
		}
		
		String videoMp4FilePath = traceDirectory 
								+ System.getProperty("file.separator") 
								+ ResourceBundleHelper.getMessageString("video.videoFileOnDevice");  
		
		if (new File(videoMp4FilePath).exists()) {
			return true;
		}
		
		return false;
	}

	@Override
	public void update(Observable observable, Object model) {
		
		traceResult = ((AROTraceData) model).getAnalyzerResult().getTraceresult();
		traceDirectory = traceResult.getTraceDirectory();	

		if (traceDirectory == null) {
			logger.error("Trace dir is null, error launching video player.");
			return;
		}

		if (currentPlayer != null) {
			currentPlayer.clear();
		}
		
		launchPlayer(traceDirectory);		
	}

	public void initAppLaunchTimeInitialPlayer() {

		if (players == null || players.size() == 0) {
			logger.error("No player available to launch");
			return;
		}
		
		AROVideoPlayer movPlayer = (AROVideoPlayer) getDefaultPlayer();
		
		if (movPlayer == null) {
			logger.error("error launching player - mov player not available");
			return;		
		} 
		
		movPlayer.setBounds(rtEdge, 0, playbackWidth, playbackHeight);
		movPlayer.setVisibility(true);
		movPlayer.setAroAdvancedTab(diagnosticsTab); 
		setCurrentVideoPlayer(movPlayer);
	}
}
