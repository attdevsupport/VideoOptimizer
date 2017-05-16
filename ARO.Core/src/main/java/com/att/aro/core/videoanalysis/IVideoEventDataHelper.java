package com.att.aro.core.videoanalysis;

import com.att.aro.core.videoanalysis.pojo.VideoEventData;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;

public interface IVideoEventDataHelper {

	/**
	 * <pre>
	 * Creates a minimal VideoEventData object.
	 * Used when there is no validated regex parser (VideoAnalysisConfig).
	 * 
	 * @param name
	 * @param exten
	 * @return VideoEventData object
	 */
	VideoEventData create(String name, String exten);

	/**
	 * Creates and populates a VideoEventData object
	 * 
	 * @param vConfig VideoAnalysisConfig
	 * @param strData a String[] created by Pattern matching using VideoAnalysisConfig
	 * @return VideoEventData object
	 * @throws Exception 
	 */
	VideoEventData create(VideoAnalysisConfig vConfig, String[] strData);

}