
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

package com.att.aro.core.videoanalysis.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

public class VideoEventTest {

	public VideoEvent getVideoEventObject(){
		ArrayList<ByteRange> rangeList = new ArrayList<>();
		rangeList.add(new ByteRange(0, 100));
		HttpRequestResponseInfo resp = new HttpRequestResponseInfo("www.google.com", PacketDirection.DOWNLINK);
		resp.setAssocReqResp(new HttpRequestResponseInfo());
		HttpRequestResponseInfo response = new HttpRequestResponseInfo("www.google.com", PacketDirection.DOWNLINK);
		Packet packet = new Packet(10, 10, 3, 3, new byte[]{0x01});
		PacketInfo packetInfo = new PacketInfo(packet);
		response.setLastDataPacket(packetInfo);
		VideoEventData ved = new VideoEventData();
		AROManifest manifest = new AROManifest(VideoType.DASH, resp, "VOLibrary");
		VideoEvent ve = new VideoEvent(ved, "A".getBytes(), manifest, 1.0, "4", rangeList, 10.0, 10.0, 10.0, 10.0, response);
		return ve;
	}
	
	@Test
	public void equalsHashcodeTest(){
		VideoEvent obj = getVideoEventObject();
		VideoEvent other = getVideoEventObject();
		
		assertTrue(obj.equals(other));
		assertTrue(obj.equals(obj));
		assertEquals(obj.hashCode(), other.hashCode());
		assertFalse(obj.equals(null));
		
		other.setBitrate(20.0);
		assertFalse(obj.equals(other));
	}
}

