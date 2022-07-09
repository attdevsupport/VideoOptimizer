/*
 *  Copyright 2022 AT&T
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