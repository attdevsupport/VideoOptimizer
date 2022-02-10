/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.tracemetadata.impl;

import java.awt.Dimension;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.peripheral.IMetaDataReadWrite;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.tracemetadata.pojo.MetaStream;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true) // allows for changes dropping items or using older versions, but not before this ignore
public class MetaDataHelper implements IMetaDataHelper {

	private static final Logger LOG = LogManager.getLogger(MetaDataHelper.class);
	
	@Autowired
	private IMetaDataReadWrite metaDataReadWrite;
			
	private static final String UNKNOWN = "unknown";

	private static final String METADATA_FILE = "metadata.json";
	
	@Autowired
	private IFileManager filemanager;

	@Getter
	private MetaDataModel metaData;

	private HashMap<Double, Integer> bitrateMap;

	private HashMap<Integer, Integer> resolutionMap;
	
	@Override
	public void saveJSON(String path, MetaDataModel metaData) throws Exception {
		this.metaData = metaData;
		saveJSON(path);
	}
	
	/**
	 * The method saves data into the metadata.json file.
	 * 
	 * @param path
	 *            - the trace folder
	 * @throws Exception
	 */
	@Override
	public void saveJSON(String path) throws Exception {
		metaDataReadWrite.save(filemanager.createFile(path), metaData);
	}

	@Override
	public MetaDataModel initMetaData(PacketAnalyzerResult result) {
		
		String tracePath = result.getTraceresult().getTraceDirectory();
		if ((metaData = metaDataReadWrite.readData(tracePath)) == null) {
			metaData = new MetaDataModel();
		} else {
			// metadata.json file did not exist
			int pos = tracePath.lastIndexOf(Util.FILE_SEPARATOR);
			String description = pos > 0 ? tracePath.substring(pos + 1) : tracePath;
			if (!filemanager.createFile(tracePath, METADATA_FILE).exists()) {
				metaData = new MetaDataModel();

				metaData.setDescription(description);
				metaData.setTraceType(UNKNOWN);
				metaData.setTargetedApp(UNKNOWN);
				metaData.setApplicationProducer(UNKNOWN);
			} else {
				try {
					metaData = loadMetaData(tracePath);
				} catch (Exception e) {
					LOG.error("error reading data from BufferedReader", e);
				}
			}
			try {
				if (updateMetaData(result)) {
					saveJSON(tracePath);
				}
			} catch (Exception e) {
				LOG.error("error saving metadata", e);
			}
		}
		return metaData;
	}

	@Override
	public MetaDataModel loadMetaData(String tracePath) throws Exception {
		return metaDataReadWrite.readData(tracePath);
	}
	
	private void setVideoStreamTotals(MetaStream metaStream, VideoStream videoStream) {
		bitrateMap = metaStream.getVideoBitrateMap();
		resolutionMap = metaStream.getVideoResolutionMap();
		Integer[] count = {0};
		videoStream.getVideoActiveMap().entrySet().stream().forEach(e -> {
			
			if (bitrateMap.containsKey(e.getValue().getBitrate())) {
				count[0] = bitrateMap.get(e.getValue().getBitrate());
			} else {
				count[0] = 0;
			}
			bitrateMap.put(e.getValue().getBitrate(), count[0] + 1);
			
			
			if (resolutionMap.containsKey(e.getValue().getResolutionHeight())) {
				count[0] = resolutionMap.get(e.getValue().getResolutionHeight());
			} else {
				count[0] = 0;
			}
			resolutionMap.put(e.getValue().getResolutionHeight(), count[0] + 1);
			
		});
		
		metaStream.setVideoResolutionMap(resolutionMap);
		metaStream.setVideoBitrateMap(bitrateMap);
		
		if (!videoStream.getAudioActiveMap().isEmpty()) {
			HashMap<String, Integer> channelMap = metaStream.getAudioChannelMap();
			bitrateMap = metaStream.getAudioBitrateMap();
			videoStream.getAudioActiveMap().entrySet().stream().forEach(e -> {

				if (bitrateMap.containsKey(e.getValue().getBitrate())) {
					count[0] = bitrateMap.get(e.getValue().getBitrate());
				} else {
					count[0] = 0;
				}
				bitrateMap.put(e.getValue().getBitrate(), count[0] + 1);

				if (channelMap.containsKey(e.getValue().getChannels())) {
					count[0] = channelMap.get(e.getValue().getChannels());
				} else {
					count[0] = 0;
				}
				channelMap.put(e.getValue().getChannels(), count[0] + 1);

			});

			metaStream.setAudioChannelMap(channelMap);
			metaStream.setAudioBitrateMap(bitrateMap);
		}
	}

	private boolean updateMetaData(PacketAnalyzerResult packetAnalyzerResult) {
		TraceDirectoryResult result = (TraceDirectoryResult) packetAnalyzerResult.getTraceresult();
		boolean isMetaDataUpdated = false;
		
		if (metaData.getVideoStreams().isEmpty() || metaData.getVideoStreams().get(0).getVideoResolutionMap().isEmpty()) {
			ArrayList<MetaStream> videoStreams = new ArrayList<>();

			for (VideoStream videoStream : packetAnalyzerResult.getStreamingVideoData().getVideoStreams()) {
				MetaStream metaStream = new MetaStream();
				metaStream.setVideoDuration(videoStream.getDuration());
				metaStream.setType(videoStream.getManifest().getVideoFormat().toString());
				metaStream.setVideo(videoStream.getManifest().getVideoName());
				metaStream.setVideoOrientation(result.getCollectOptions().getOrientation());
				
				metaStream.setVideoDownloadtime(videoStream.getVideoActiveMap().entrySet().stream().mapToDouble(e -> e.getValue().getDLTime()).sum());
				metaStream.setAudioDownloadtime(videoStream.getAudioActiveMap().entrySet().stream().mapToDouble(e -> e.getValue().getDLTime()).sum());
				metaStream.setVideoSegmentTotal(videoStream.getVideoActiveMap().size());
				metaStream.setAudioSegmentTotal(videoStream.getAudioActiveMap().size());
				
				setVideoStreamTotals(metaStream, videoStream);
				videoStream.setMetaStream(metaStream);
				videoStreams.add(metaStream);
			}
			
			metaData.setVideoStreams(videoStreams);
			isMetaDataUpdated = true;
		}
		
		if (metaData.getCollectorName().isEmpty()) {
			metaData.setCollectorName(result.getCollectorName());
			isMetaDataUpdated = true;
		}
		if (metaData.getCollectorVersion().isEmpty()) {
			metaData.setCollectorVersion(result.getCollectorVersion());
			isMetaDataUpdated = true;
		}

		if (metaData.getTraceName().isEmpty()) {
			String name = result.getTraceDirectory();
			int pos = name.lastIndexOf(Util.FILE_SEPARATOR);
			if (pos > 0) {
				name = name.substring(pos + 1);
			}
			metaData.setTraceName(name);
			isMetaDataUpdated = true;
		}
		
		if (metaData.getDeviceScreenSize() == null) {
			metaData.setDeviceScreenSize(new Dimension(result.getDeviceScreenSizeX(), result.getDeviceScreenSizeY()));
			isMetaDataUpdated = true;
		}
		
		if (metaData.getPhoneMake().isEmpty()) {
			metaData.setPhoneMake(result.getDeviceMake());
			isMetaDataUpdated = true;
		}
		if (metaData.getPhoneModel().isEmpty()) {
			metaData.setPhoneModel(result.getDeviceModel());
			isMetaDataUpdated = true;
		}
		if (metaData.getOs().isEmpty()) {
			metaData.setOs(result.getOsType());
			isMetaDataUpdated = true;
		}
		if (metaData.getOsVersion().isEmpty()) {
			metaData.setOsVersion(result.getOsVersion());
			isMetaDataUpdated = true;
		}	
		
		if (metaData.getStartUTC().isEmpty()) {
			ZonedDateTime startUTC = null;
			if (result != null && result.getTraceDateTime() != null) {
				startUTC = ZonedDateTime.ofInstant(result.getTraceDateTime().toInstant(), ZoneId.systemDefault());
				isMetaDataUpdated = true;
			}
			metaData.setStartUTC(startUTC != null ? startUTC.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "");
		}
		
		if (metaData.getUtc() == null || metaData.getUtc() == 0L) {
			metaData.setUtc(result.getTraceDateTime().toInstant().getEpochSecond());
			isMetaDataUpdated = true;
		}		
		
		if (metaData.getTraceDuration() == null) {
			metaData.setTraceDuration(result.getTraceDuration());
			isMetaDataUpdated = true;
		}
		
		if (metaData.getTraceSource().isEmpty()) {
			metaData.setTraceSource("Manual");
			isMetaDataUpdated = true;
		} // MACHINE when tested in automated test harness
		
		if ((metaData.getTargetAppVer().isEmpty() || UNKNOWN.equals(metaData.getTargetAppVer()))
				&& (!metaData.getTargetedApp().isEmpty() && !UNKNOWN.equals(metaData.getTargetedApp()))) {
			String appVersion = findAppVersion(result);
			if (!UNKNOWN.equals(appVersion)) {
				isMetaDataUpdated = true;
				metaData.setTargetAppVer(appVersion);
			}
		}

		return isMetaDataUpdated;
	}

	@Override
	public String findAppVersion(TraceDirectoryResult result) {
		Map<String, String> appVersion = result.getAppVersionMap();
		if (appVersion != null) {
			String app = metaData.getTargetedApp().toLowerCase();
			for (String key : appVersion.keySet()) {
				if (key.toLowerCase().startsWith(app)) {
					return appVersion.get(key);
				}
			}
		}
		return UNKNOWN;
	}

	@Override
	public MetaDataModel initMetaData(String tracePath, String traceDesc, String traceType, String targetedApp, String appProducer) {
		metaData = new MetaDataModel();
		metaData.setDescription(traceDesc);
		metaData.setTargetedApp(targetedApp);
		metaData.setTraceType(traceType);
		metaData.setApplicationProducer(appProducer);
		try {
			metaDataReadWrite.save(filemanager.createFile(tracePath), metaData);
		} catch (Exception e) {
			LOG.error("Faild to save metadata", e);
		}
		
		return metaData;
	}

}
