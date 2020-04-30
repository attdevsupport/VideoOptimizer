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
package com.att.aro.core.videoanalysis.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math3.util.MathUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.NearStall;
import com.att.aro.core.packetanalysis.pojo.VideoStall;
import com.att.aro.core.videoanalysis.AbstractBufferOccupancyCalculator;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.core.videoanalysis.pojo.VideoStream;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BufferInSecondsCalculatorImpl extends AbstractBufferOccupancyCalculator {

	protected static final Logger LOG = LogManager.getLogger(BufferInSecondsCalculatorImpl.class.getName());
	
	private static final double THRESHOLD = .00001;
	private List<VideoEvent> filteredChunks;
	private List<VideoEvent> chunkDownload;
	private List<VideoEvent> chunkDownloadCopy;

	private List<VideoEvent> veDone;

	double beginBuffer, endBuffer;

	Double dlTime = 0D;
	Double dlDuration = 0D;

	enum SortOrder {
		LAST, FIRST, HIGHEST
	}
	
	/**
	 * <pre>
	 * A mapped point set for graphics display key=(x-coord)timeStamp, (y-coord)duration Each key-value describes a point in the graph. A line will be drawn from
	 * {key} point to the {key+1} point ex: 0=61.287999868392944, 0.0 , 1=61.287999868392944, 2.75275 , 2=62.986000061035156, 2.75275 , 3=62.986000061035156,
	 * 6.089416666666667
	 */
	Map<Integer, String> seriesDataSets = new TreeMap<Integer, String>();

	int key;
	int skey = 0;
	int debugkey = 0;

	private BufferTimeBPResult bufferTimeResult;

	@Autowired
	private VideoChunkPlotterImpl videoChunkPlotterImpl;

	private List<VideoEvent> veWithIn;
	private List<VideoEvent> completedDownloads = new ArrayList<>();

	private List<VideoStall> videoStallResult;
	private List<NearStall> videoNearStallResult;
	private boolean stallStarted;
	double possibleStartPlayTime;
	private List<VideoEvent> completedDownloadsWithOutNearStalls = new ArrayList<>();
	private List<VideoEvent> veWithInPlayDownloadedSegments;

	@Autowired
	private IVideoUsagePrefsManager videoPrefManager;

	private double stallTriggerTime;
	private double stallPausePoint;
	private double stallRecovery;
	private double nearStall;
	private VideoStall videoStall = null;
	private TreeMap<Double, VideoEvent> stalls;

	public void detectionForNearStall(VideoEvent chunkPlaying) {
		if (completedDownloads.contains(chunkPlaying) && (!completedDownloadsWithOutNearStalls.contains(chunkPlaying))) {
			// mark chunkPlaying as nearly stalled segment
			NearStall nearStalledSegment = new NearStall(chunkPlaying.getEndTS() - (nearStall + stallPausePoint), chunkPlaying);
			videoNearStallResult.add(nearStalledSegment);
		} else if (completedDownloadsWithOutNearStalls.contains(chunkPlaying) && (!CollectionUtils.isEmpty(veWithInPlayDownloadedSegments))) {
			// add veWithInPlayDownloadedSegments to completedDownloadsWithOutNearStalls collection
			for (VideoEvent ve : veWithInPlayDownloadedSegments) {
				completedDownloadsWithOutNearStalls.add(ve);
				chunkDownloadCopy.remove(ve);
			}
		}
	}

	/*
	 * skip non-video
	 * 
	 */
	public Map<Integer, String> populate(StreamingVideoData streamingVideoData, Map<VideoEvent, Double> chunkPlayTimeList) {
		if (videoPrefManager.getVideoUsagePreference() != null) {
			setStallTriggerTime(videoPrefManager.getVideoUsagePreference().getStallTriggerTime());
			setStallPausePoint(videoPrefManager.getVideoUsagePreference().getStallPausePoint());
			setStallRecovery(videoPrefManager.getVideoUsagePreference().getStallRecovery());
			setNearStall(videoPrefManager.getVideoUsagePreference().getNearStall());
		}

		videoStallResult = new ArrayList<>();
		videoNearStallResult = new ArrayList<>();
		lastTimestamp = 0;
		totalDuration = 0;
		lastBufferTime = 0;
		bufferedTime = 0;
		playedTime = 0;
		lastBufferedTime = 0D;
		lastPlayedTime = 0D;

		// new way

		initialize(streamingVideoData);
		seriesDataSets.clear();
		skey = 0;
		bufferTime = 0D;

		ArrayList<VideoEvent> streams = selectSegments(streamingVideoData, SortOrder.LAST);
		
		/*
		 * key: playTime, value: duration
		 */
		TreeMap<Double, VideoEvent> playStream = new TreeMap<>();
		/*
		 * key: download end time, value: duration
		 */
		TreeMap<Double, Double> dlStream = new TreeMap<>();
		stalls = new TreeMap<>();

		VideoEvent playEvent = null;
		videoStall = null;

		double playTimeEnd = 0;

		// VID-TODO remove bufferTime based on duration of segment starting to play (playtime)
		for (VideoEvent event : streams) {
			LOG.debug(String.format("Seg:%.0f: P:%3.3f D:%3.6f Stall:%3.3f >> Next:%3.3f", event.getSegmentID(), event.getPlayTime(), event.getDuration(),
					event.getStallTime(), (event.getPlayTime() + event.getDuration())));

			if (event.getStallTime() != 0) {
				LOG.debug("stall");
				videoStallResult.add(new VideoStall(event));
				stalls.put(playTimeEnd, event);
			}
			dlTime = event.getEndTS();
			while (dlStream.get(dlTime) != null) {
				// protect against duplicate timestamps
				dlTime += .000000001;
			}
			dlStream.put(dlTime, event.getDuration());
			playStream.put(event.getPlayTime(), event);
			playTimeEnd = event.getPlayTimeEnd();
		}
		
		skey = 0;
		Double segPlayTime = 0D;

		double stallStart = Double.MAX_VALUE;
		double stallEnd = Double.MAX_VALUE;
		double priorDlTime = 0D;

		while (!playStream.isEmpty() && (segPlayTime = playStream.firstKey()) != null) {
			playEvent = playStream.remove(segPlayTime);
			LOG.debug(String.format("seg(%.0f) :%.3f - %.3f, duration: %.5f", playEvent.getSegmentID(), playEvent.getPlayTime(), playEvent.getPlayTimeEnd(), playEvent.getDuration()));
			double playTimestamp = playEvent.getPlayTime();
			double playEndTimestamp = playEvent.getPlayTimeEnd();
			double playDuration = playEvent.getDuration();

			if (isStallActive(segPlayTime) || isStallActive(playEvent.getPlayTimeEnd())) {
				stallStart = videoStall.getStallStartTimestamp();
				stallEnd = videoStall.getStallEndTimestamp();
				LOG.debug(String.format("Stalled event found: %.3f", videoStall.getStallStartTimestamp()));
			}

			if (!dlStream.isEmpty()) {
				while (!dlStream.isEmpty() && (dlTime = dlStream.firstKey()) != null) {
					if (playEndTimestamp< dlTime) {
						insertDataSet(playEndTimestamp, -playDuration);
						break;
					}
					dlDuration = dlStream.remove(dlTime);
					LOG.debug(String.format("BUFF \t%.3f\t%.5f\t%.5f", lastTimestamp, bufferedTime-lastBufferedTime, playedTime-lastPlayedTime));

					lastBufferedTime = bufferedTime;
					lastPlayedTime = playedTime;

					LOG.debug(String.format("<dl>dlTime(%.3f->%.3f)  dlDuration: %.5f", dlTime, dlTime + dlDuration, dlDuration));
					if (videoStall != null && dlTime >= videoStall.getStallEndTimestamp()) {
						// reset for next stall
						LOG.debug(String.format("RESETTING >> dlTime: %.3f,  videoStall :%s", dlTime, videoStall));
						videoStall = null;
						stallStart = Double.MAX_VALUE;
						stallEnd = Double.MAX_VALUE;
					}
					if (isStallActive(dlTime, dlTime + dlDuration)) {
						stallStart = videoStall.getStallStartTimestamp();
						stallEnd = videoStall.getStallEndTimestamp();
						LOG.debug(String.format("Stalled event found for dlTime:(%.3f->%.3f) %.3f - %.3f", dlTime, dlTime + dlDuration, stallStart, stallEnd));
					} else {
						isStallActive = false;
					}
					if (dlTime < playTimestamp){
						insertDataSet(dlTime, 0);
						insertDataSet(dlTime, dlDuration);

					} else if (isStallActive(priorDlTime, playEndTimestamp)) {
						stallStart = videoStall.getStallStartTimestamp();
						stallEnd = videoStall.getStallEndTimestamp();
						isStallActive = true;
						double fragTime = stallStart - playTimestamp;
						insertDataSet(lastTimestamp, 0);
						insertDataSet(stallStart, -fragTime);
						playTimestamp = dlTime;
						playDuration -= fragTime;
						insertDataSet(playTimestamp, 0);
						insertDataSet(playTimestamp, dlDuration);
						LOG.debug(String.format("left over in stall @211:  %.0f: %.3f, %.3f", playEvent.getSegmentID(), playTimestamp, playDuration));

					} else if (isStallActive) {
						insertDataSet(dlTime, 0);
						insertDataSet(dlTime, dlDuration);
						if (dlStream.isEmpty() || dlStream.firstKey() > playStream.firstKey()) {
							break;
						} else {
							continue;
						}
					} else if (dlTime < playEndTimestamp) {
						LOG.debug(String.format("%d -> dl:%.3f: seg:%.0f :%.3f - %.3f", skey, dlTime, playEvent.getSegmentID(), playEvent.getPlayTime(), playEvent.getPlayTimeEnd()));

						double fragTime = dlTime - playTimestamp;

						insertDataSet(playTimestamp, 0);
						insertDataSet(dlTime, -fragTime);

						playTimestamp = dlTime;
						playDuration -= fragTime;

						insertDataSet(playTimestamp, dlDuration);// should be done here 

						if (dlStream.isEmpty() || dlStream.firstKey() > playStream.firstKey()) {
							if (isStallActive(dlTime, playEvent.getPlayTimeEnd())) {
								stallStart = videoStall.getStallStartTimestamp();
								stallEnd = videoStall.getStallEndTimestamp();
								insertDataSet(stallStart, 0);
								if (stallEnd < dlTime) {
									LOG.debug(String.format("Stalled event found here for dlTime: %.3f (%.3f) %.3f", stallStart, dlTime, stallEnd));
									insertDataSet(stallEnd, 0);
									videoStall = null;
								} else {
									videoStall.setStallStartTimestamp(dlTime);
								}

							} 
							else {
								insertDataSet(playEndTimestamp, -playDuration);
							}
							priorDlTime = dlTime;
							break;
						}
					} else {
						insertDataSet(dlTime, 0);
						insertDataSet(dlTime, dlDuration);
						lastPlayEndTimestamp = playEndTimestamp;

						priorDlTime = dlTime;
						break;
					}
					priorDlTime = dlTime;
				}
			} else {
				insertDataSet(playEndTimestamp, -playDuration);
				LOG.debug(String.format("left over in stall @260:  %.0f: %.3f, %.3f", playEvent.getSegmentID(), playTimestamp, playDuration));
			}
		}
		while (playStream.size() > 1) {
			segPlayTime = playStream.firstKey();
			playEvent = playStream.remove(segPlayTime);
			insertDataSet(segPlayTime, 0);
			insertDataSet(playEvent.getPlayTimeEnd(), -playEvent.getDuration());
		}
		
		debugDisplay();
		return seriesDataSets;
	}

	public void debugDisplay() {
		StringBuilder strblr = new StringBuilder("\ntotalDuration=");
		strblr.append(totalDuration).append("\n{");
		for (int idx = 0; idx < seriesDataSets.size(); idx++) {
			strblr.append(String.format("%d\t%s", idx, seriesDataSets.get(idx).replaceAll(",", "\t")));
		}
		strblr.append("}");
		LOG.debug(strblr.toString());
	}

	/**
	 * Check for timestamp to fall within a VideoStall start-end timestamps.
	 * If timestamp is after the VideoStall end timestamp, the next, if any stall will be pulled from the stalls TreeMap
	 * 
	 * @param timeStamp
	 * @param stalls
	 * @return true is in range, false otherwise
	 */
	public boolean isStallActive(Double timeStamp) {
		if (videoStall == null && stalls.isEmpty()) {
			return false;
		}
		if (!stalls.isEmpty() && (videoStall == null || videoStall.getStallEndTimestamp() < timeStamp)) {
			videoStall = new VideoStall(stalls.pollFirstEntry().getValue());
		}
		return videoStall != null && videoStall.getStallStartTimestamp() <= timeStamp && timeStamp <= videoStall.getStallEndTimestamp();
	}

	private boolean isStallActive(Double startTimeStamp, double endTimeStamp) {
		if (videoStall == null && !stalls.isEmpty()) {
			videoStall = new VideoStall(stalls.pollFirstEntry().getValue());
		}
		if (videoStall != null 
				&& ((videoStall.getStallStartTimestamp() > startTimeStamp && videoStall.getStallStartTimestamp() < endTimeStamp)
				|| (videoStall.getStallEndTimestamp() > startTimeStamp && videoStall.getStallEndTimestamp() < endTimeStamp))) {
			return true;
		}
		return false;
	}

	String dataSetValues = "";
	private double lastTimestamp;
	private double lastPlayEndTimestamp;
	private double lastBufferTime;
	private int totalDuration;
	private double bufferTime;
	private double bufferedTime;
	private double playedTime;
	private double lastBufferedTime;
	private double lastPlayedTime;
	private boolean isStallActive;

	public void insertDataSet(double timestamp, double delta) {
		if (skey == 20) {
			System.out.print("");
		}
		if (delta > 0) {
			bufferedTime += delta;
		} else {
			playedTime += delta;
		}
		bufferTime += delta;
		if (bufferTime != 0 && sameDoubleValue(lastTimestamp, timestamp) && sameDoubleValue(bufferTime, lastBufferTime)) {
			LOG.debug(String.format("DROP\t%2d\t%2.3f\t%2.3f\t%2.3f", skey, timestamp, delta, bufferTime));
			return;
		}
		seriesDataSets.put(skey++, timestamp + "," + bufferTime);
		LOG.debug(String.format("IDS\t%2d\t%2.3f\t%2.5f\t%2.5f", skey - 1, timestamp, delta, bufferTime));
		lastTimestamp = timestamp;
		lastBufferTime = bufferTime;
	}

	public boolean sameDoubleValue(double dbl1, double dbl2) {
		return (Math.abs(dbl1 - dbl2) < THRESHOLD);
	}

	/**
	 * assemble ArrayList of VideoEvents based on selection criteria.
	 * 
	 * @param streamingVideoData
	 * @param tempSegment
	 * @param sortLast
	 * @param sortFirst
	 * @param sortHighest
	 * @return
	 */
	public ArrayList<VideoEvent> selectSegments(StreamingVideoData streamingVideoData, SortOrder sort) {
		
		VideoEvent priorEvent = null;
		ArrayList<VideoEvent> streams = new ArrayList<>();
		
		for (VideoStream stream : streamingVideoData.getVideoStreamMap().values()) {
			if (stream.isSelected()) {
				for (VideoEvent event : stream.getVideoSegmentEventList().values()) { // key definition segment-quality-timestamp
					if (event.isNormalSegment()) {
						if (priorEvent == null) {
							priorEvent = event;
							continue;
						} else {
							if (priorEvent.getSegmentID() == event.getSegmentID()) {
								if (sort==SortOrder.HIGHEST && Integer.valueOf(priorEvent.getQuality()).compareTo(Integer.valueOf(event.getQuality())) > 0) {
									priorEvent = event;
								} else if (sort==SortOrder.FIRST && event.getEndTS() < priorEvent.getEndTS()) {
									priorEvent = event;
								} else if (sort==SortOrder.LAST && event.getEndTS() > priorEvent.getEndTS()) {
									priorEvent = event;
								}
								continue;
							}
						}
						streams.add(priorEvent);
						priorEvent = event;
					} else {
						LOG.debug("skip abnormal VideoEvents such as moov:" + event);
					}
				}
			}
		}
		streams.add(priorEvent);
		return streams;
	}

	@Override
	public double drawVeDone(List<VideoEvent> veDone, double beginBuffer) {
		double buffer = beginBuffer;
		Collections.sort(veDone, new VideoEventComparator(SortSelection.END_TS));

		for (VideoEvent chunk : veDone) {

			seriesDataSets.put(key++, chunk.getEndTS() + "," + buffer);

			buffer = buffer + chunk.getDuration();

			seriesDataSets.put(key++, chunk.getEndTS() + "," + buffer);

			completedDownloads.add(chunk);
			chunkDownload.remove(chunk);

		}

		return buffer;
	}

	public double drawVeWithIn(List<VideoEvent> veWithIn, double beginBuffer) {
		double buffer = beginBuffer;

		if (veWithIn.size() == 0 && completedDownloads.contains(chunkPlaying)) {
			buffer = bufferDrain(buffer);
		} else if (completedDownloads.contains(chunkPlaying)) { // if not there then it is a stall
			Collections.sort(veWithIn, new VideoEventComparator(SortSelection.END_TS));

			boolean drained = false;
			VideoEvent chunk;
			double timeRange;
			double durationLeft = chunkPlayTimeDuration;

			seriesDataSets.put(key++, chunkPlayStartTime + "," + buffer);

			if (stallStarted) {
				updateStallInformation(chunkPlayStartTime);
				stallStarted = false;
			}

			for (int index = 0; index < veWithIn.size(); index++) {
				chunk = veWithIn.get(index);
				if (MathUtils.equals(chunk.getEndTS(), chunkPlayEndTime)) {
					// finish draining
					buffer = buffer - durationLeft;
					if (buffer <= 0) {
						buffer = 0;
						updateStallInformation(chunk.getEndTS());
					}
					seriesDataSets.put(key++, chunkPlayEndTime + "," + buffer);
					drained = true;

					buffer = buffer + chunk.getDuration();

					seriesDataSets.put(key++, chunk.getEndTS() + "," + buffer);

					completedDownloads.add(chunk);
					chunkDownload.remove(chunk);
				} else {
					if (index == 0) {
						timeRange = chunk.getEndTS() - chunkPlayStartTime;
					} else {
						timeRange = chunk.getEndTS() - veWithIn.get(index - 1).getEndTS();
					}

					buffer = buffer - timeRange;
					if (buffer <= 0) {
						buffer = 0;
						stallStarted = true;
						updateStallInformation(chunk.getEndTS());
					}
					durationLeft = durationLeft - timeRange;

					seriesDataSets.put(key++, chunk.getEndTS() + "," + buffer);

					buffer = buffer + chunk.getDuration();

					seriesDataSets.put(key++, chunk.getEndTS() + "," + buffer);

					completedDownloads.add(chunk);
					chunkDownload.remove(chunk);
				}
			}

			if (drained == false) {
				buffer = buffer - durationLeft;
				if (buffer <= 0) {
					buffer = 0;
					stallStarted = true;
					updateStallInformation(chunkPlayEndTime);
				}
				seriesDataSets.put(key++, chunkPlayEndTime + "," + buffer);

			}

		} else {
			/*
			 * chunkPlaying has not yet arrived, therefore a stall has occurred
			 */

			boolean skipStallStart = false;
			for (VideoStall stall : videoStallResult) {
				if (BigDecimal.valueOf(stall.getStallEndTimestamp()) == BigDecimal.valueOf(chunkPlayStartTime)) {
					skipStallStart = true;
					break;
				}
			}
			if (skipStallStart == false) {
				stallStarted = true;
				VideoStall stall = new VideoStall(chunkPlayStartTime - stallPausePoint);
				videoStallResult.add(stall);
			}
			return -1;
		}

		return buffer;

	}

	private void initialize(StreamingVideoData streamingVideoData) {
		this.streamingVideoData = streamingVideoData;
		filteredChunks = streamingVideoData.getStreamingVideoCompiled().getFilteredSegments(); // filterVideoSegment(videoUsage);
		chunkDownload = new ArrayList<>();
		chunkDownloadCopy = new ArrayList<>();
		for (VideoEvent vEvent : filteredChunks) {
			chunkDownload.add(vEvent);
			chunkDownloadCopy.add(vEvent);
		}

		runInit(streamingVideoData, streamingVideoData.getStreamingVideoCompiled().getChunksBySegmentID());
	}

	public void updateUnfinishedDoneVideoEvent() {
		for (VideoEvent ve : chunkDownload) {
			if (ve.getEndTS() <= chunkPlayStartTime) {
				veDone.add(ve);
			} else if (ve.getEndTS() > chunkPlayStartTime && ve.getEndTS() <= chunkPlayEndTime) { // late? stall?
				veWithIn.add(ve);
			}
		}
	}

	public void updateSegmentsDownloadedList() {
		List<VideoEvent> toBeRemovedSegments = new ArrayList<>();
		for (VideoEvent ve : chunkDownloadCopy) {
			if (ve.getDLTimeStamp() < chunkPlayStartTime && ve.getEndTS() - (nearStall + stallPausePoint) <= chunkPlayStartTime) {
				completedDownloadsWithOutNearStalls.add(ve);
				toBeRemovedSegments.add(ve);
			} else if (ve.getEndTS() - (nearStall + stallPausePoint) > chunkPlayStartTime && ve.getEndTS() - (nearStall + stallPausePoint) <= chunkPlayEndTime) {
				veWithInPlayDownloadedSegments.add(ve);
			}
		}
		for (VideoEvent ve : toBeRemovedSegments) {
			chunkDownloadCopy.remove(ve);
		}
	}

	public void updateStallInformation(double stallTime) {
		if (stallStarted && (videoStallResult != null && videoStallResult.size() != 0)) {
			VideoStall videoStall = videoStallResult.get(videoStallResult.size() - 1);
			if (videoStall.getStallEndTimestamp() != stallTime) {
				videoStall.setStallEndTimestamp(stallTime);
			}
		} else {
			stallStarted = true;
			VideoStall stall = new VideoStall(stallTime);
			videoStallResult.add(stall);
		}
	}

	@Override
	public double bufferDrain(double buffer) {
		if (buffer > 0 && completedDownloads.contains(chunkPlaying)) {
			seriesDataSets.put(key++, chunkPlayStartTime + "," + buffer);

			buffer = buffer - chunkPlayTimeDuration;
			if (buffer <= 0) {
				buffer = 0;
				stallStarted = true;
				VideoStall stall = new VideoStall(chunkPlayEndTime);
				videoStallResult.add(stall);
			} else {
				if (stallStarted) {
					updateStallInformation(chunkPlayStartTime);
					stallStarted = false;
				}
			}

			seriesDataSets.put(key++, chunkPlayEndTime + "," + buffer);

		} else {
			// stall
			if (buffer <= 0) { // want to see if buffer >0 & stall happening
				buffer = 0;
				stallStarted = true;
				updateStallInformation(chunkPlayStartTime);
			}
			seriesDataSets.put(key++, chunkPlayStartTime + "," + buffer);
			return -1;
		}

		return buffer;
	}

	public Map<Long, Double> getSegmentStartTimeMap() {
		return videoChunkPlotterImpl.getSegmentStartTimeMap();
	}

	public Map<Double, Long> getSegmentEndTimeMap() {
		Map<Long, Double> segmentStartTimeMap = getSegmentStartTimeMap();
		Map<Double, Long> segmentEndTimeMap = new HashMap<>();
		if (segmentStartTimeMap != null) {
			for (VideoEvent ve : streamingVideoData.getStreamingVideoCompiled().getFilteredSegments()) {
				if (ve != null) {
					Double startTime = segmentStartTimeMap.get(new Double(ve.getSegmentID()).longValue());
					double segmentPlayEndTime = (startTime != null ? startTime : 0.0) + ve.getDuration();
					segmentEndTimeMap.put(segmentPlayEndTime, new Double(ve.getSegmentID()).longValue());
				}
			}
		}
		return segmentEndTimeMap;
	}

	public BufferTimeBPResult updateBufferTimeResult(List<Double> bufferTimeBPResult) {
		bufferTimeResult = new BufferTimeBPResult(bufferTimeBPResult);
		return bufferTimeResult;
	}
}
