/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.packetanalysis.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.att.aro.core.packetanalysis.IThroughputCalculator;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Throughput;
import com.att.aro.core.packetreader.pojo.PacketDirection;

public class ThroughputCalculatorImpl implements IThroughputCalculator {

	public List<Throughput> calculateThroughput(double startTime, double endTime, double window,
			List<PacketInfo> packets) {
		List<Throughput> result = new ArrayList<Throughput>();
		List<PacketInfo> split = new ArrayList<>();
		if(window < 0.00001 || endTime-startTime < 0.00001) {
			return Collections.emptyList();
		}
		double splitStart = startTime;
		double splitEnd = startTime + window;
		for (PacketInfo packet : packets) {
			double stamp = packet.getTimeStamp();
			if (stamp < startTime) {
				continue;
			} else if (stamp >= endTime) {
				result.add(getThroughput(splitStart, splitEnd, split));
				break;
			} else if (stamp >= splitEnd) {
				while (stamp >= splitEnd) {
					result.add(getThroughput(splitStart, splitEnd, split));
					splitStart = splitEnd;
					splitEnd = splitStart + window;
					split = new ArrayList<>();
				}
				split.add(packet);
			} else if (stamp >= splitStart) {
				split.add(packet);
			}
		}
		do {
			result.add(getThroughput(splitStart, splitEnd, split));
			splitStart = splitEnd;
			splitEnd = splitStart + window;
			split = new ArrayList<>();
		} while (endTime >= splitStart);
		return result;
	}

	private Throughput getThroughput(double startTime, double endTime, List<PacketInfo> packets) {
		long up = 0;
		long down = 0;
		for (PacketInfo packet : packets) {
			if (packet.getDir() == null || packet.getDir() == PacketDirection.UNKNOWN) {
				continue;
			} else if (packet.getDir() == PacketDirection.UPLINK) {
				up += packet.getLen();
			} else {
				down += packet.getLen();
			}
		}
		return new Throughput(startTime, endTime, up, down);
	}

}
