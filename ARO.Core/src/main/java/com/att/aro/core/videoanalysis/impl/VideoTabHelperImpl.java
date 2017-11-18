
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

import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.bestpractice.pojo.VideoUsage;
import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.IVideoTabHelper;

public class VideoTabHelperImpl implements IVideoTabHelper{

	@Autowired
	private IVideoUsageAnalysis videoUsageImpl;
		
	@Override
	public TreeMap<Double, HttpRequestResponseInfo> getRequestListMap(){
		if(videoUsageImpl.getVideoUsage() == null) {
			return new TreeMap<Double, HttpRequestResponseInfo>();
		}
		return videoUsageImpl.getVideoUsage().getRequestMap();
	}

	@Override
	public void resetRequestMapList(){
		if(videoUsageImpl.getVideoUsage() != null){
			videoUsageImpl.getVideoUsage().setRequestMap(new TreeMap<Double, HttpRequestResponseInfo>());
		}
	}
	
	@Override
	public boolean isStartUpDelaySet() {
		boolean result = false;
		if (videoUsageImpl != null && videoUsageImpl.getVideoUsage() != null) {
			VideoUsage videousage = videoUsageImpl.getVideoUsage();
			result = ((!videousage.getChunkPlayTimeList().isEmpty()) ? true : false);
		}
		return result;
	}
}

