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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.videoanalysis;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.att.aro.core.videoanalysis.impl.RegexMatchLbl;
import com.att.aro.core.videoanalysis.pojo.RegexMatchResult;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public interface IVideoAnalysisConfigHelper {

	VideoAnalysisConfig findConfig(String target);

	VideoAnalysisConfig loadConfigFile(String file);
	
	/**
	 * <pre>
	 * Apply regex filter from vConfig with 
	 * 
	 * @param vConfig
	 * @param requestStr
	 * @return a String array of matches
	 */
	String[] match(VideoAnalysisConfig vConfig, String requestStr);
	

	/**
	 * <pre>
	 * Apply regex filters from vConfig with the request line and header
	 * 
	 * @param vConfig
	 * @param requestStr
	 * @param headerStr
	 * @param responseStr
	 * @return String[] of matches
	 */
	Map<RegexMatchLbl, RegexMatchResult> match(VideoAnalysisConfig vConfig, String requestStr, String headerStr,
			String responseStr);

	/**
	 * Create and Serialize VideoAnalysisConfig object to {desc}.json file.
	 * 
	 * @param desc - Description
	 * @param requestType
	 * @param regex - regex pattern
	 * @param headerRegex
	 * @param responseRegex
	 * @param xref - VideoDataTags[] for standardizing array of matches
	 * @return VideoAnalysisConfig object
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * @throws Exception 
	 */
	VideoAnalysisConfig saveConfigFile(VideoType videoType, String desc, String type, String regex, String headerRegex, String responseRegex, VideoDataTags[] xref)
			throws JsonGenerationException, JsonMappingException, IOException, Exception;

	/**
	 * Returns path to VideoConfig_files
	 * 
	 * @return path
	 */
	String getFolderPath();

	/**
	 * Validate that extracted match count equal cross reference count
	 * 
	 * @param vConfig
	 * @return
	 */
	boolean validateConfig(VideoAnalysisConfig vConfig);

	/**
	 * Count capture groups in 
	 * 
	 * @param targetRegex
	 * @param sRegex
	 * @return
	 */
	int count(String targetRegex, String sRegex);


}
