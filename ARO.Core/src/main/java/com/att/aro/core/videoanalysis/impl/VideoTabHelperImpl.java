
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

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.IVideoTrafficCollector;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.IVideoTabHelper;
import com.att.aro.core.videoanalysis.pojo.StreamingVideoData;

public class VideoTabHelperImpl implements IVideoTabHelper {

	@Autowired
	private IVideoTrafficCollector videoTrafficCollectorImpl;

	@Override
	public SortedMap<Double, HttpRequestResponseInfo> getRequestListMap() {
		if (videoTrafficCollectorImpl.getStreamingVideoData() == null) {
			return new TreeMap<Double, HttpRequestResponseInfo>();
		}
		return videoTrafficCollectorImpl.getStreamingVideoData().getRequestMap();
	}

	@Override
	public void resetRequestMapList() {
		if (videoTrafficCollectorImpl.getStreamingVideoData() != null) {
			videoTrafficCollectorImpl.getStreamingVideoData().clear();
		}
	}

	@Override
	public boolean isStartUpDelaySet() {
		boolean result = false;
		if (videoTrafficCollectorImpl != null && videoTrafficCollectorImpl.getStreamingVideoData() != null) {
			StreamingVideoData streamingVideoData = videoTrafficCollectorImpl.getStreamingVideoData();
			result = MapUtils.isNotEmpty(streamingVideoData.getStreamingVideoCompiled().getChunkPlayTimeList());
		}
		return result;
	}
}