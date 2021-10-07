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
package com.att.arocollector.packetRebuild;

public class StubbedEthernetHeader {

	public static byte[] getEthernetHeader(byte ipVersion){
		byte[] ethHeader = new byte[14];
		
		//set destination mac to be all 0s
		ByteUtils.setBigIndianInBytesArray(ethHeader, 0, 0, 6);
		
		//set src mac, random to be 1
		ByteUtils.setBigIndianInBytesArray(ethHeader, 6, 1, 6);
		
		//set eth type:
		// 0x0800 = 2048 for IPv4
		// 0x86DD for IPv6
		short ethType = ipVersion == 4 ? 0x0800 : (short) 0x86DD;
		ByteUtils.setBigIndianInBytesArray(ethHeader, 12, ethType, 2);
		
		return ethHeader;
	}
}
