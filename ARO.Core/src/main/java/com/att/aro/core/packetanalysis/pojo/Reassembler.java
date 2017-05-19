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
*/package com.att.aro.core.packetanalysis.pojo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * helper class for TCPSession. Tracks information about a reassembled session
 * Date: April 24, 2014
 */
public class Reassembler {
	private Long baseSeq;
	private long seq = -1;
	private List<PacketInfo> ooid = new ArrayList<PacketInfo>();
	private ByteArrayOutputStream storage = new ByteArrayOutputStream();
	private SortedMap<Integer, PacketInfo> packetOffsets = new TreeMap<Integer, PacketInfo>();
	private List<PacketRangeInStorage> pktRanges =  new ArrayList<PacketRangeInStorage>();
	
	public void clear() {
		baseSeq = null;
		seq = -1;
		ooid.clear();
		storage.reset();
		packetOffsets = new TreeMap<Integer, PacketInfo>();
		pktRanges = new ArrayList<PacketRangeInStorage>();
	}

	public void getPacketIDList(int begin, int end, List<Integer> pktIDList) {
		int pktrgnSize = pktRanges.size();
		for (int i = 0; i < pktrgnSize; i++) {
			if (end < pktRanges.get(i).getOffset()) {
				break;
			}
			if (begin > (pktRanges.get(i).getOffset() + pktRanges.get(i).getSize() - 1)) {
				continue;
			}
			pktIDList.add(pktRanges.get(i).getPktID());
		}
	}
	
	@Override
	public String toString() {
		StringBuilder tos = new StringBuilder("seq:");
		tos.append(seq);
		tos.append(": count:");
		tos.append(ooid.size());
		return tos.toString();
	}
	
	/**
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		storage.close();
		super.finalize();
	}

	public Long getBaseSeq() {
		return baseSeq;
	}

	public void setBaseSeq(Long baseSeq) {
		this.baseSeq = baseSeq;
	}

	public long getSeq() {
		return seq;
	}

	public void incrementSeq(){
		seq++;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	public List<PacketInfo> getOoid() {
		return ooid;
	}

	public void setOoid(List<PacketInfo> ooid) {
		this.ooid = ooid;
	}

	public ByteArrayOutputStream getStorage() {
		return storage;
	}

	public void setStorage(ByteArrayOutputStream storage) {
		this.storage = storage;
	}

	public SortedMap<Integer, PacketInfo> getPacketOffsets() {
		return packetOffsets;
	}

	public void setPacketOffsets(SortedMap<Integer, PacketInfo> packetOffsets) {
		this.packetOffsets = packetOffsets;
	}

	public List<PacketRangeInStorage> getPktRanges() {
		return pktRanges;
	}

	public void setPktRanges(List<PacketRangeInStorage> pktRanges) {
		this.pktRanges = pktRanges;
	}
	
}
