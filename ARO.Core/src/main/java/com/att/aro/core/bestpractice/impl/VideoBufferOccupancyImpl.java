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

package com.att.aro.core.bestpractice.impl;

import java.text.MessageFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.BufferOccupancyResult;
import com.att.aro.core.packetanalysis.pojo.BufferOccupancyBPResult;
import com.att.aro.core.packetanalysis.pojo.BufferTimeBPResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;

/**
 * <pre>
 * VBP #3
 * Video Buffer Occupancy
 * 
 * Criteria:
 *  ARO will measure frequency and duration of video stream buffers
 *  
 * About:
 *  Buffer occupancy is the amount of video stored in RAM to help prevent interruption due to transmission delays, known as "buffering".
 * 
 * Result:
 *  Your video had X size buffer occupancy By  managing  buffer occupancy you can make sure it is not too long or too short.
 * 
 * Link:
 *  goes to a view of buffers.
 * 
 */

public class VideoBufferOccupancyImpl implements IBestPractice {
	@Value("${bufferOccupancy.title}")
	private String overviewTitle;

	@Value("${bufferOccupancy.detailedTitle}")
	private String detailTitle;

	@Value("${bufferOccupancy.desc}")
	private String aboutText;

	@Value("${bufferOccupancy.url}")
	private String learnMoreUrl;

	@Value("${bufferOccupancy.pass}")
	private String textResultPass;

	@Value("${bufferOccupancy.results}")
	private String textResults;

	@Value("${startUpDelay.init}")
	private String startUpDelayNotSet;

	@Value("${videoSegment.empty}")
	private String novalidManifestsFound;

	// private PreferenceHandlerImpl prefs;

	@Autowired
	private IVideoUsagePrefsManager videoUsagePrefs;

	private double maxBufferSet;
	double maxBufferReached;

	public void updateVideoPrefMaxBuffer() {
		if (videoUsagePrefs.getVideoUsagePreference() != null) {
			maxBufferSet = videoUsagePrefs.getVideoUsagePreference().getMaxBuffer();
		}
	}

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		BufferOccupancyBPResult bufferBPResult = tracedata.getBufferOccupancyResult();
		BufferTimeBPResult bufferTimeBPResult = tracedata.getBufferTimeResult();
		BufferOccupancyResult result = new BufferOccupancyResult();
		if (bufferBPResult != null && bufferBPResult.getBufferByteDataSet().size() > 0) {
			maxBufferReached = bufferBPResult.getMaxBuffer();// maxBufferReached is in KB (1024)
			maxBufferReached = maxBufferReached / 1024; // change to MB (2^20)
			List<Double> bufferDataSet = bufferBPResult.getBufferByteDataSet();
			result.setMinBufferByte(bufferDataSet.get(0) / 1024);
			double bufferSum = bufferDataSet.stream().reduce((a, b) -> a + b).get();
			result.setAvgBufferByte((bufferSum / bufferDataSet.size()) / 1024);
		} else {
			maxBufferReached = 0;
		}
		if (bufferTimeBPResult != null && bufferTimeBPResult.getBufferTimeDataSet().size() > 0) {
			List<Double> bufferTimeDataSet = bufferTimeBPResult.getBufferTimeDataSet();
			result.setMinBufferTime(bufferTimeDataSet.get(0));
			result.setMaxBufferTime(bufferTimeDataSet.get(bufferTimeDataSet.size() - 1));
			double sum = bufferTimeDataSet.stream().reduce((a, b) -> a + b).get();
			result.setAvgBufferTime(sum / bufferTimeDataSet.size());
		}

		result.setSelfTest(true);
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setMaxBuffer(maxBufferReached);

		updateVideoPrefMaxBuffer();
		double startupDelay = videoUsagePrefs.getVideoUsagePreference().getStartupDelay();
		double percentage = 0;
		if (maxBufferSet != 0) {
			percentage = (maxBufferReached / maxBufferSet) * 100;
		}

		if (tracedata.getVideoUsage() != null && tracedata.getVideoUsage().getChunkPlayTimeList().isEmpty()) {
			result.setResultText(MessageFormat.format(startUpDelayNotSet, String.format("%.2f", percentage),
					String.format("%.2f", maxBufferReached), String.format("%.2f", maxBufferSet)));
		} else {
			result.setResultText(MessageFormat.format(this.textResults, String.format("%.2f", percentage),
					String.format("%.2f", maxBufferReached), String.format("%.2f", maxBufferSet)));
		}
		if (Util.isTraceWithValidManifestsSelected(tracedata.getVideoUsage())
				&& !Util.isStartupDelaySet(tracedata.getVideoUsage())) {
			result.setResultType(BPResultType.CONFIG_REQUIRED);
			result.setResultText(MessageFormat.format(startUpDelayNotSet, startupDelay, startupDelay == 1 ? "" : "s"));
		} else if (!Util.isTraceWithValidManifestsSelected(tracedata.getVideoUsage())) {
			result.setResultType(BPResultType.SELF_TEST);
			result.setResultText(novalidManifestsFound);
		}

		return result;
	}

}// end class
