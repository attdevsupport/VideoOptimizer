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
			safePutDblKeyedMap(videoDownLoadKeyMap, e.getValue().getEndTS(), e.getValue());
			safePutDblKeyedMap(videoPlayKeyMap, e.getValue().getPlayTime(), e.getValue());
		});
		
		videoStream.clearPlayTimeData();
		process();
	}

	/**
	 * <pre>
	 * Put values into a map with double keys, with minor increment, to guarantee addition to the map
	 * Will increment key by 1e-12 until a gap is found
	 * 
	 * @param dblKeyMap
	 * @param endTS
	 * @param value		VideoEvent
	 */
	private void safePutDblKeyedMap(TreeMap<Double, VideoEvent> dblKeyMap, double endTS, VideoEvent value) {
		double key = endTS;

		while (dblKeyMap.containsKey(key)) {
			key += 1e-12;
		}
		dblKeyMap.put(key, value);
	}

	private void process() {
		buffer = 0.0;
		Double playKey = null;
		if (!videoPlayKeyMap.isEmpty()) {
			playKey = videoPlayKeyMap.firstKey();
		} else {
			return;
		}
		plKey = 0.0;
		dlItr = videoDownLoadKeyMap.entrySet().iterator();
		cntr = 0;
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
			logDownloadEvent();
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

	public void logDownloadEvent() {
		logPoint( dlEvent, dlKey, buffer, StreamStatus.Load);
		buffer += dlEvent.getDuration();
		logPoint( dlEvent, dlKey, buffer, StreamStatus.Load);
		dlKV = null;
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
		
		if (dlKV != null && dlKey < playTimeEnd && (dlItr.hasNext() || !priorKey.equals(dlKey))) {
			do {
				double partial = dlKey - priorKey;
				durationRemainder -= partial;
				buffer -= partial;
				logPoint(dlEvent, dlKey, buffer, StreamStatus.Load);
				buffer += dlEvent.getDuration();
				dlKV = null;
				logPoint(plEvent, dlKey, buffer, StreamStatus.Play);
				priorKey = dlKey;
			} while (loadNextDlEvent() && dlKey < playTimeEnd);
		}
		return durationRemainder;
	}

	/**
	 * Only to be called from handlePlay
	 * 
	 * Handles all loads that happen within the stall condition
	 * @return 
	 * 
	 */
	private void handleStallRecovery() {
		// a stall was located
		loadNextPlayEvent();

		while (dlItr.hasNext() && (Precision.round(dlKey, 5) <  Precision.round(plKey, 5))) { // dlKey < plKey
			logPoint( dlEvent, dlKey, buffer, StreamStatus.Load);
			buffer += dlEvent.getDuration();
			dlKV = null;
			logPoint( plEvent, dlKey, buffer, StreamStatus.Stall);
			loadNextDlEvent();
		}
		plKey = videoPlayKeyMap.lowerKey(plKey);
		return;
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
		try {
			if (!videoPlayKeyMap.lastKey().equals(plKey) && (plKey = videoPlayKeyMap.higherKey(plKey)) != null) {
				plEvent = videoPlayKeyMap.get(plKey);
			} else {
				return false;
			}
		} catch (Exception e) {
			LOG.debug("Reached end, no more keys! This should not happen.");
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
