/*
 *  Copyright 2019 AT&T
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
package com.att.aro.core.packetanalysis.pojo;

import com.att.aro.core.util.Util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HttpDelayInfo {

	private double requestTimeStamp;
	private String firstPacketTimeStamp;
	private String firstPacketDelay;
	private String lastPacketTimeStamp;
	private int contentLength;
	private String lastPacketDelay;
	private HttpRequestResponseInfo httpRequestResponseInfo;

	public HttpDelayInfo(HttpRequestResponseInfo httpRequestResponseInfo) {
		this.httpRequestResponseInfo = httpRequestResponseInfo;
		if (httpRequestResponseInfo.getDirection() == HttpDirection.REQUEST) {
			requestTimeStamp = httpRequestResponseInfo.getTimeStamp();
			getResponedata();
		}
	}

	private void getResponedata() {
		HttpRequestResponseInfo response = httpRequestResponseInfo.getAssocReqResp();
		if (response != null && response.getFirstDataPacket() != null) {
			double firstPacketTime = response.getFirstDataPacket().getTimeStamp();
			double lastPacketTime = response.getLastPacketTimeStamp();
			firstPacketTimeStamp = Util.formatDouble(firstPacketTime);
			firstPacketDelay = Util.formatDouble(firstPacketTime - requestTimeStamp);
			lastPacketTimeStamp = Util.formatDouble(lastPacketTime);
			contentLength = response.getContentLength();
			lastPacketDelay = Util.formatDouble(lastPacketTime - requestTimeStamp);
		}

	}

}