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

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;	

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;

public class VideoPlayerController implements Observer {

	private IVideoPlayer currentPlayer;
	private List<IVideoPlayer> players;
	private AbstractTraceResult traceResult;
	private String traceDirectory;
	private DiagnosticsTab diagnosticsTab;
	private static final Logger LOGGER = LogManager.getLogger(VideoPlayerController.class);	
	
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
		return getPlayer(VideoPlayerType.MP4_VLCJ);
	}

	private IVideoPlayer getPlayer(VideoPlayerType playerType) {
		for (IVideoPlayer player: players) {
			if (player.getPlayerType() == playerType) {
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

	@Override
	public void update(Observable observable, Object model) {
		traceResult = ((AROTraceData) model).getAnalyzerResult().getTraceresult();
		TraceResultType traceResultType = traceResult.getTraceResultType();
		traceDirectory = traceResult.getTraceDirectory();	

		if (traceDirectory == null) {
			LOGGER.error("Trace dir is null, error launching video player.");
			return;
		}

		if (traceResultType == TraceResultType.TRACE_FILE
				|| !(VideoUtil.mp4VideoExists(traceDirectory) || VideoUtil.movVideoExists(traceDirectory))) {
			currentPlayer.clear();
			currentPlayer.notifyLauncher(false);
			return;
		}
		
		IVideoPlayer player = getPlayer(VideoPlayerType.MP4_VLCJ);;
		if (player == null) {
			LOGGER.error("Error launching video player - no appropriate Mp4 or Mov player found");
			return;
		}
		
		player.loadVideo(traceResult);	
			diagnosticsTab.setVideoPlayer(player);
			setCurrentVideoPlayer(player);
			player.notifyLauncher(true);
	}
	

	public void launchPlayer(int xPosition, int yPosition, int frameWidth, int frameHeight) {	

		if (players == null || players.size() == 0) {
			LOGGER.error("No player available to launch");
			return;
		}
				
		IVideoPlayer player = (IVideoPlayer) getDefaultPlayer();
		
		if (player == null) {
			LOGGER.error("Error launching player - player not available");
			return;		
		} 
		
		player.launchPlayer(xPosition, yPosition, frameWidth, frameHeight);
		player.setAroAdvancedTab(diagnosticsTab);
		diagnosticsTab.setVideoPlayer(player);
		setCurrentVideoPlayer(player);
	}

}
