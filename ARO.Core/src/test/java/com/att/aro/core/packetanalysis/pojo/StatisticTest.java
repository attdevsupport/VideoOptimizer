
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

package com.att.aro.core.packetanalysis.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class StatisticTest {

	public Statistic getOther(){
		Set<String> set = new HashSet<>();
		set.add("VO");
		List<ApplicationPacketSummary> list = new ArrayList<>();
		list.add(new ApplicationPacketSummary("VO", 5, 100, 150));
		Statistic other = new Statistic();
		other.setAppName(set);
		other.setAverageKbps(10.0);
		other.setApplicationPacketSummary(list);
		other.setAverageTCPKbps(10.0);
		other.setPacketDuration(10.0);
		other.setTcpPacketDuration(10.0);
		other.setTotalTCPBytes(10);
		other.setTotalHTTPSByte(10);
		other.setTotalByte(10);
		other.setTotalPackets(10);
		other.setTotalTCPPackets(10);
		
		return other;
	}
	
	@Test
	public void equalsHashcodeTest(){
		Statistic obj = new Statistic();
		Set<String> set = new HashSet<>();
		set.add("VO");
		obj.setAppName(set);
		obj.setAverageKbps(10.0);
		List<ApplicationPacketSummary> list = new ArrayList<>();
		list.add(new ApplicationPacketSummary("VO", 5, 100, 150));
		obj.setApplicationPacketSummary(list);
		obj.setAverageTCPKbps(10.0);
		obj.setPacketDuration(10.0);
		obj.setTcpPacketDuration(10.0);
		obj.setTotalTCPBytes(10);
		obj.setTotalHTTPSByte(10);
		obj.setTotalByte(10);
		obj.setTotalPackets(10);
		obj.setTotalTCPPackets(10);
		
		Statistic other = getOther();
		
		assertTrue(obj.equals(other));
		assertTrue(obj.equals(obj));
		assertEquals(obj.hashCode(), other.hashCode());
		assertFalse(obj.equals(null));
		
		List<ApplicationPacketSummary> list2 = new ArrayList<>();
		list2.add(new ApplicationPacketSummary("VO", 10, 100, 150));
		other = getOther();
		other.setApplicationPacketSummary(list2);
		assertFalse(obj.equals(other));

		other = getOther();
		other.setAverageKbps(5.0);
		assertFalse(obj.equals(other));
		
		other = getOther();
		other.setAverageTCPKbps(5.0);
		assertFalse(obj.equals(other));
		
		other = getOther();
		other.setPacketDuration(5.0);
		assertFalse(obj.equals(other));
		
		other = getOther();
		other.setTcpPacketDuration(5.0);
		assertFalse(obj.equals(other));
		
		other = getOther();
		other.setTotalByte(5);
		assertFalse(obj.equals(other));
		
		other = getOther();
		other.setTotalPackets(5);
		assertFalse(obj.equals(other));
		
		other = getOther();
		other.setTotalTCPPackets(5);
		assertFalse(obj.equals(other));
	}
}

