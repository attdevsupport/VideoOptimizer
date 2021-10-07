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

package com.att.arotcpcollector;

public class PacketData {

	private long timeStamp;
	private byte[] packetData;

	public PacketData(byte[] packetData) {
		this.timeStamp = System.currentTimeMillis();
		this.packetData = packetData;
	}
	
	public PacketData(byte[] packetData, long timeStamp) {
		this.timeStamp = timeStamp;
		this.packetData = packetData;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public byte[] getPacketData() {
		return packetData;
	}

}
