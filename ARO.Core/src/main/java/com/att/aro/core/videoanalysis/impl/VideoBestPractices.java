package com.att.aro.core.videoanalysis.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.IAROService;
import com.att.aro.core.IVideoBestPractices;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.AudioStreamResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.bestpractice.pojo.BufferOccupancyResult;
import com.att.aro.core.bestpractice.pojo.VideoChunkPacingResult;
import com.att.aro.core.bestpractice.pojo.VideoChunkSizeResult;
import com.att.aro.core.bestpractice.pojo.VideoConcurrentSessionResult;
import com.att.aro.core.bestpractice.pojo.VideoNetworkComparisonResult;
import com.att.aro.core.bestpractice.pojo.VideoRedundancyResult;
import com.att.aro.core.bestpractice.pojo.VideoResolutionQualityResult;
import com.att.aro.core.bestpractice.pojo.VideoAdaptiveBitrateLadderResult;
import com.att.aro.core.bestpractice.pojo.VideoStallResult;
import com.att.aro.core.bestpractice.pojo.VideoStartUpDelayResult;
import com.att.aro.core.bestpractice.pojo.VideoTcpConnectionResult;
import com.att.aro.core.bestpractice.pojo.VideoVariableBitrateResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.util.GoogleAnalyticsUtil;

public class VideoBestPractices implements IVideoBestPractices {

	@Autowired
	private IAROService aroService;

	public AROTraceData analyze(AROTraceData traceDataresult) {
		PacketAnalyzerResult result = null;
		if (null == traceDataresult) {
			return null;
		}
		result = traceDataresult.getAnalyzerResult();
		if (result == null) {
			return null;
		}

		VideoStallResult videoStallResult = null;
		VideoStartUpDelayResult videoStartUpDelayResult = null;
		BufferOccupancyResult bufferOccupancyResult = null;
		VideoNetworkComparisonResult videoNetworkComparisonResult = null;
		VideoTcpConnectionResult videoTcpConnectionResult = null;
		VideoChunkSizeResult videoChunkSizeResult = null;
		VideoChunkPacingResult videoChunkPacingResult = null;
		VideoRedundancyResult videoRedundancyResult = null;
		VideoConcurrentSessionResult videoConcurrentSessionResult = null;
		VideoVariableBitrateResult videoVariableBitrateResult = null;
		VideoAdaptiveBitrateLadderResult videoSegmentQualityResult = null;
		VideoResolutionQualityResult videoResolutionQualityResult = null;
		AudioStreamResult videoSeparateAudioResult = null;

		List<BestPracticeType> requests = BestPracticeType.getByCategory(Category.VIDEO);
		List<AbstractBestPracticeResult> bpResults = traceDataresult.getBestPracticeResults();
		List<AbstractBestPracticeResult> videoBestPracticeResults = aroService.analyze(result, requests);

		for (AbstractBestPracticeResult videoBPResult : videoBestPracticeResults) {

			BestPracticeType bpType = videoBPResult.getBestPracticeType();
			switch (bpType) {
				case VIDEO_STALL :
					videoStallResult = (VideoStallResult) videoBPResult;
					break;
				case STARTUP_DELAY :
					videoStartUpDelayResult = (VideoStartUpDelayResult) videoBPResult;
					break;
				case BUFFER_OCCUPANCY :
					bufferOccupancyResult = (BufferOccupancyResult) videoBPResult;
					break;
				case NETWORK_COMPARISON :
					videoNetworkComparisonResult = (VideoNetworkComparisonResult) videoBPResult;
					break;
				case TCP_CONNECTION :
					videoTcpConnectionResult = (VideoTcpConnectionResult) videoBPResult;
					break;
				case CHUNK_SIZE :
					videoChunkSizeResult = (VideoChunkSizeResult) videoBPResult;
					break;
				case CHUNK_PACING :
					videoChunkPacingResult = (VideoChunkPacingResult) videoBPResult;
					break;
				case VIDEO_REDUNDANCY :
					videoRedundancyResult = (VideoRedundancyResult) videoBPResult;
					break;
				case VIDEO_CONCURRENT_SESSION :
					videoConcurrentSessionResult = (VideoConcurrentSessionResult) videoBPResult;
					break;
				case VIDEO_VARIABLE_BITRATE :
					videoVariableBitrateResult = (VideoVariableBitrateResult) videoBPResult;
					break;
				case VIDEO_RESOLUTION_QUALITY :
					videoResolutionQualityResult = (VideoResolutionQualityResult) videoBPResult;
					break;
				case VIDEO_ABR_LADDER :
					videoSegmentQualityResult = (VideoAdaptiveBitrateLadderResult) videoBPResult;
					break;
				case AUDIO_STREAM :
					videoSeparateAudioResult = (AudioStreamResult) videoBPResult;
					break;
				default :
					break;
			}
		}
		sendGAVideoBPResult(videoBestPracticeResults);
		for (AbstractBestPracticeResult bestPractice : bpResults) {

			if (bestPractice instanceof VideoStallResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoStallResult);
			} else if (bestPractice instanceof VideoStartUpDelayResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoStartUpDelayResult);
			} else if (bestPractice instanceof BufferOccupancyResult) {
				bpResults.set(bpResults.indexOf(bestPractice), bufferOccupancyResult);
			} else if (bestPractice instanceof VideoNetworkComparisonResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoNetworkComparisonResult);
			} else if (bestPractice instanceof VideoTcpConnectionResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoTcpConnectionResult);
			} else if (bestPractice instanceof VideoChunkSizeResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoChunkSizeResult);
			} else if (bestPractice instanceof VideoChunkPacingResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoChunkPacingResult);
			} else if (bestPractice instanceof VideoChunkPacingResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoChunkPacingResult);
			} else if (bestPractice instanceof VideoRedundancyResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoRedundancyResult);
			} else if (bestPractice instanceof VideoConcurrentSessionResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoConcurrentSessionResult);
			} else if (bestPractice instanceof VideoVariableBitrateResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoVariableBitrateResult);
			} else if (bestPractice instanceof VideoResolutionQualityResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoResolutionQualityResult);
			} else if (bestPractice instanceof VideoAdaptiveBitrateLadderResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoSegmentQualityResult);
			} else if (bestPractice instanceof AudioStreamResult) {
				bpResults.set(bpResults.indexOf(bestPractice), videoSeparateAudioResult);
			}
		}

		traceDataresult.setBestPracticeResults(bpResults);
		return traceDataresult;
	}

	private void sendGAVideoBPResult(List<AbstractBestPracticeResult> videoBestPracticeResults) {
		for (AbstractBestPracticeResult videoBPResult : videoBestPracticeResults) {
			videoBPResult.getBestPracticeType().getDescription();
			StringBuffer eventLabel = new StringBuffer();
			if (videoBPResult.getResultType() == BPResultType.FAIL) {
				eventLabel.append(videoBPResult.getResultType());
				eventLabel.append(": ");
				eventLabel.append(videoBPResult.getResultText());
			} else {
				eventLabel.append(videoBPResult.getResultType());
			}
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(GoogleAnalyticsUtil.getAnalyticsEvents().getVideoBPResultEvent(),
					videoBPResult.getBestPracticeType().getDescription(), eventLabel.toString());
		}
	}
}
