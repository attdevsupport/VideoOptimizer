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
package com.att.aro.core.tracemetadata.pojo;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MetaStream {
	private String description = "";
	private String type = "";
	private String video = "";
	private String videoOrientation = "";
	private Double videoDuration = null;

	private HashMap<Integer, Integer> videoResolutionMap = new HashMap<>();
	private HashMap<Double, Integer> videoBitrateMap = new HashMap<>();
	private Double videoDownloadtime = null;
	private Integer videoSegmentTotal = null;

	private HashMap<String, Integer> audioChannelMap = new HashMap<>();
	private HashMap<Double, Integer> audioBitrateMap = new HashMap<>();
	private Double audioDownloadtime = null;
	private Integer audioSegmentTotal = null;
}
