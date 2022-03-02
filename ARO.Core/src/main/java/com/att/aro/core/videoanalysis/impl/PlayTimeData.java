/*  Copyright 2021 AT&T
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
package com.att.aro.core.videoanalysis.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.util.Precision;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.videoanalysis.XYPair;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.core.videoanalysis.pojo.VideoStream.StreamStatus;

/**
 * Creates point data for Playtime tooltips, stored into VideoStream
 *
 */
public class PlayTimeData {

	private static final Logger LOG = LogManager.getLogger(PlayTimeData.class.getName());
	
	private TreeMap<Double, VideoEvent> videoDownLoadKeyMap = new TreeMap<>();
	private TreeMap<Double, VideoEvent> videoPlayKeyMap = new TreeMap<>();
	private Iterator<Entry<Double, VideoEvent>> dlItr;
	
	private Entry<Double, VideoEvent> dlKV;
	private Double dlKey = null;
	private VideoEvent dlEvent;
	
	private Double buffer = 0.0;
	private VideoEvent plEvent;
	private Double plKey;
	private int cntr = 0;
	private VideoStream videoStream;
	private ArrayList<XYPair> playTimeList;

	/**
	 * Builds Playtime chart points and tooltip data
	 * All methods are private by design
	 * 
	 * 
	 * @param videoStream
	 */
	public PlayTimeData(VideoStream videoStream) {
		this.videoStream = videoStream;
		playTimeList = videoStream.getPlayTimeList();

		// creates TreeMaps for DownLoadKeys and PlayKeys
		videoStream.getVideoActiveMap().entrySet().forEach(e -> {
			videoDownLoadKeyMap.put(e.getValue().getEndTS(), e.getValue());
			videoPlayKeyMap.put(e.getValue().getPlayTime(), e.getValue());
		});
		
		videoStream.clearPlayTimeData();
		process();
	}

	private void process() {
		buffer = 0.0;
		Double playKey = videoPlayKeyMap.firstKey();
		plKey = 0.0;
		dlItr = videoDownLoadKeyMap.entrySet().iterator();
		cntr=0;
		if (playKey != null) {
			handlePreload(playKey);
			handlePlay();
		}
	}

	/**
	 * Handles all preload events up to the first play time
	 * 
	 * @param playKey
	 */
	private void handlePreload(Double playKey) {
		// preload "buffering"
		while (loadNextDlEvent() && dlKey < playKey) {
			logPoint( dlEvent, dlKey, buffer, StreamStatus.Load);
			buffer += dlEvent.getDuration();
			logPoint( dlEvent, dlKey, buffer, StreamStatus.Load);
		}
	}

	/**
	 * This should only be accessed from VideoSegmentAnalyzer.propagatePlaytime(double, VideoEvent, VideoStream) >> PlayTimeData.process()
	 * 
	 * Handles Segments in Play, accommodates stalls
	 * 
	 */
	private void handlePlay() {
		double durationRemainder;
		double playTimeEnd;
		Double nextPlayKey;
		// play
		while (loadNextPlayEvent()) {
			durationRemainder = plEvent.getDuration();
			playTimeEnd = plEvent.getPlayTimeEnd();
		
			durationRemainder = handleDownloadsDuringEventPlay(durationRemainder, playTimeEnd);
			
			// Handles any Play time after last download event
			if (durationRemainder > 0) {
				buffer -= durationRemainder;
				durationRemainder = 0;

				if ((nextPlayKey = videoPlayKeyMap.higherKey(plKey)) != null) {
					if (!Precision.equals(nextPlayKey, playTimeEnd, 5)) {
						// log only if there is a stall or the end of stream playout
						logPoint(videoPlayKeyMap.get(videoPlayKeyMap.higherKey(plKey)), playTimeEnd, buffer, StreamStatus.Stall);
					}
				} else {
					logPoint(plEvent, playTimeEnd, buffer, StreamStatus.Play);
				}
			}
			
		    // finished playing a segment
			nextPlayKey = videoPlayKeyMap.higherKey(plKey);

			if (nextPlayKey != null && Precision.round(nextPlayKey, 5) >  Precision.round(playTimeEnd, 5)) {
				handleStallRecovery();
			}
		}
	}

	/**
	 * Only to be called from handlePlay()
	 * 
	 * @param durationRemainder
	 * @param playTimeEnd
	 * @return durationRemainder left over playtime after last download
	 */
	private double handleDownloadsDuringEventPlay(double durationRemainder, double playTimeEnd) {
		logPoint(plEvent, plKey, buffer, StreamStatus.Play);
		Double priorKey = plKey;

		if (dlItr.hasNext() && dlKey < playTimeEnd) {
			do {
				double partial = dlKey - priorKey;
				durationRemainder -= partial;
				buffer -= partial;
				logPoint(dlEvent, dlKey, buffer, StreamStatus.Load);
				buffer += dlEvent.getDuration();
				logPoint( plEvent, dlKey, buffer, StreamStatus.Play);
				priorKey = dlKey;
			} while (loadNextDlEvent() && dlKey < playTimeEnd);
		}
		return durationRemainder;
	}

	/**
	 * Only to be called from handlePlay
	 * 
	 * Handles all loads that happen within the stall condition
	 * 
	 */
	private void handleStallRecovery() {
		// a stall was located
		loadNextPlayEvent();
		
		while (dlItr.hasNext() && (Precision.round(dlKey, 5) <  Precision.round(plKey, 5))) { // dlKey < plKey
			logPoint( dlEvent, dlKey, buffer, StreamStatus.Load);
			buffer += dlEvent.getDuration();
			logPoint( plEvent, dlKey, buffer, StreamStatus.Stall);
			loadNextDlEvent();
		}
		plKey = videoPlayKeyMap.lowerKey(plKey);
	}
	
	/**
	 * Outputs playTime chart points and playTimeToolTipPoints into videoStream
	 * 
	 * @param event
	 * @param timestamp
	 * @param bufferVal
	 * @param status
	 */
	private void logPoint(VideoEvent event, double timestamp, Double bufferVal, StreamStatus status) {
		cntr++;
		LOG.debug(String.format("%s\t%d\t%.0f\t%.3f\t%.5f", status.toString(), cntr, event.getSegmentID(), timestamp, bufferVal));
		playTimeList.add(new XYPair(timestamp, bufferVal));
		videoStream.addPlayTimeToolTipPoint(event, bufferVal, status);
	}

	private boolean loadNextPlayEvent() {
		plKey = videoPlayKeyMap.higherKey(plKey);
		if (plKey != null) {
			plEvent = videoPlayKeyMap.get(plKey);
		}
		return plKey != null;
	}

	private boolean loadNextDlEvent() {
		if (dlItr.hasNext()) {
			dlKV = dlItr.next();
			dlKey = dlKV.getKey();
			dlEvent = dlKV.getValue();
			return true;
		}
		return false;
	}
}
