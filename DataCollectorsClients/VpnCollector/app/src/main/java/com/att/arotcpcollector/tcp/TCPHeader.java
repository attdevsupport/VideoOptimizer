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
package com.att.arotcpcollector.tcp;

/**
 * data structure for TCP Header
 * 
 * @author Borey Sao Date: May 8, 2014
 *
 */
public class TCPHeader{
	
	private int sourcePort;
	private int destinationPort;
	private long sequenceNumber;
	private int dataOffset;

	private int tcpFlags;

	private boolean isNS  = false; //  NS (1 bit) – ECN-nonce concealment protection (experimental: see RFC 3540).
	private boolean isCWR = false; // CWR (1 bit) – Congestion Window Reduced (CWR) flag is set by the sending host to indicate that it received a TCP segment with the ECE flag set and had responded in congestion control mechanism
	private boolean isECE = false; // ECE (1 bit) – ECN-Echo has a dual role, depending on the value of the SYN flag. It indicates:
	private boolean isSYN = false; // SYN (1 bit) – Synchronize sequence numbers. Only the first packet sent from each end should have this flag set.
	private boolean isACK = false; // ACK (1 bit) – indicates that the Acknowledgment field is significant. All packets after the initial SYN packet sent by the client should have this flag set.
	private boolean isFIN = false; // FIN (1 bit) – No more data from sender
	private boolean isRST = false; // RST (1 bit) – Reset the connection
	private boolean isPSH = false; // PSH (1 bit) – Push function. Asks to push the buffered data to the receiving application.
	private boolean isURG = false; // URG (1 bit) – indicates that the Urgent pointer field is significant

	private int windowSize;
	private int checksum;
	private int urgentPointer;
	private byte[] options;
	private long ackNumber;

	// vars below need to be set via setters when copying
	private int maxSegmentSize = 0;
	private int windowScale = 0;
	private boolean isSelectiveAckPermitted = false;
	private int timeStampSender = 0;
	private int timeStampReplyTo = 0;
 
	public TCPHeader() {}

	public TCPHeader(int sourcePort, int destinationPort, long sequenceNumber,
			int dataOffset, boolean isNS, int tcpFlags, int windowSize, int checksum, int urgentPointer,
			byte[] options, long ackNumber) {

		this.sourcePort = sourcePort;
		this.destinationPort = destinationPort;
		this.sequenceNumber = sequenceNumber;
		this.dataOffset = dataOffset;
		this.isNS = isNS;
		this.tcpFlags = tcpFlags;
		this.windowSize = windowSize;
		this.checksum = checksum;
		this.urgentPointer = urgentPointer;
		this.options = options;
		this.ackNumber = ackNumber;
		setFlagBits(this.tcpFlags);
	}
	
	void setFlagBits(int tcpFlags) {
		isACK = (tcpFlags & 0x10) > 0;
		isFIN = (tcpFlags & 0x01) > 0;
		isPSH = (tcpFlags & 0x08) > 0;
		isRST = (tcpFlags & 0x04) > 0;
		isSYN = (tcpFlags & 0x02) > 0;
		isURG = (tcpFlags & 0x20) > 0;
		isCWR = (tcpFlags & 0x80) > 0;
		isECE = (tcpFlags & 0x40) > 0;
	}

	public boolean isNS() {
		return isNS;
	}

	public void setIsNS(boolean isNS) {
		this.isNS = isNS;
	}

	public boolean isCWR() {
		return isCWR;
	}

	public void setIsCWR(boolean iscwr) {
		this.isCWR = iscwr;
		if (iscwr) {
			this.tcpFlags |= 0x80;
		} else {
			this.tcpFlags &= 0x7F;
		}
	}

	/**
	 * <pre>
	 * ECE (1 bit) – ECN-Echo has a dual role, depending on the value of the SYN flag. It indicates:
	 * If the SYN flag is set (1), that the TCP peer is ECN capable.
	 * If the SYN flag is clear (0), that a packet with Congestion Experienced flag in IP header set is received during normal transmission (added to header by RFC 3168
	 * 
	 * @return
	 */
	public boolean isECE() {
		return isECE;
	}

	/**
	 * 
	 * <pre>
	 * ECE (1 bit) – ECN-Echo has a dual role, depending on the value of the SYN flag. It indicates:
	 * If the SYN flag is set (1), that the TCP peer is ECN capable.
	 * If the SYN flag is clear (0), that a packet with Congestion Experienced flag in IP header set is received during normal transmission (added to header by RFC 3168
	 *
	 * @param isECE
	 */
	public void setIsECE(boolean isECE) {
		this.isECE = isECE;
		if (isECE) {
			this.tcpFlags |= 0x40;
		} else {
			this.tcpFlags &= 0xBF;
		}
	}

	/**
	 * <pre>
	 * SYN (1 bit) – Synchronize sequence numbers. 
	 * 
	 * Only the first packet sent from each end should have this flag set. Some
	 * other flags and fields change meaning based on this flag, and some are
	 * only valid for when it is set, and others when it is clear.
	 * 
	 * @return
	 */
	public boolean isSYN() {
		return isSYN;
	}

	/**
	 * <pre>
	 * SYN (1 bit) – Synchronize sequence numbers. 
	 * 
	 * Only the first packet sent from each end should have this flag set. Some
	 * other flags and fields change meaning based on this flag, and some are
	 * only valid for when it is set, and others when it is clear.
	 * 
	 * @param issyn
	 */
	public void setIsSYN(boolean issyn) {
		this.isSYN = issyn;
		if (issyn) {
			this.tcpFlags |= 0x02;
		} else {
			this.tcpFlags &= 0xFD;
		}
	}

	/**
	 * <pre>
	 * ACK (1 bit) – indicates that the Acknowledgment field is significant. 
	 * 
	 * All packets after the initial SYN packet sent by the client should have
	 * this flag set.
	 * 
	 * @return
	 */
	public boolean isACK() {
		return isACK;
	}

	/**
	 * <pre>
	 * ACK (1 bit) – indicates that the Acknowledgment field is significant. 
	 * 
	 * All packets after the initial SYN packet sent by the client should have
	 * this flag set.
	 * 
	 * @param isack
	 */
	public void setIsACK(boolean isack) {
		this.isACK = isack;
		if (isack) {
			this.tcpFlags |= 0x10;
		} else {
			this.tcpFlags &= 0xEF;
		}
	}

	public boolean isFIN() {
		return isFIN;
	}

	public void setIsFIN(boolean isfin) {
		this.isFIN = isfin;
		if (isfin) {
			this.tcpFlags |= 0x1;
		} else {
			this.tcpFlags &= 0xFE;
		}
	}

	public boolean isRST() {
		return isRST;
	}

	public void setIsRST(boolean isrst) {
		this.isRST = isrst;
		if (isrst) {
			this.tcpFlags |= 0x04;
		} else {
			this.tcpFlags &= 0xFB;
		}
	}

	public boolean isPSH() {
		return isPSH;
	}

	public void setIsPSH(boolean ispsh) {
		this.isPSH = ispsh;
		if (ispsh) {
			this.tcpFlags |= 0x08;
		} else {
			this.tcpFlags &= 0xF7;
		}
	}

	public boolean isURG() {
		return isURG;
	}

	public void setIsURG(boolean isurg) {
		this.isURG = isurg;
		if (isurg) {
			this.tcpFlags |= 0x20;
		} else {
			this.tcpFlags &= 0xDF;
		}
	}

	public int getSourcePort() {
		return sourcePort;
	}

	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public int getDataOffset() {
		return dataOffset;
	}

	public void setDataOffset(int dataOffset) {
		this.dataOffset = dataOffset;
	}

	public int getTcpFlags() {
		return tcpFlags;
	}

	public void setTcpFlags(int tcpFlags) {
		this.tcpFlags = tcpFlags;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public int getChecksum() {
		return checksum;
	}

	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}

	public int getUrgentPointer() {
		return urgentPointer;
	}

	public void setUrgentPointer(int urgentPointer) {
		this.urgentPointer = urgentPointer;
	}

	public byte[] getOptions() {
		return options;
	}

	public void setOptions(byte[] options) {
		this.options = options;
	}

	public long getAckNumber() {
		return ackNumber;
	}

	public void setAckNumber(long ackNumber) {
		this.ackNumber = ackNumber;
	}

	/**
	 * length of TCP Header including options length if available.
	 * 
	 * @return
	 */
	public int getTCPHeaderLength() {
		return (dataOffset * 4);
	}

	public int getMaxSegmentSize() {
		return maxSegmentSize;
	}

	public void setMaxSegmentSize(int maxSegmentSize) {
		this.maxSegmentSize = maxSegmentSize;
	}

	public int getWindowScale() {
		return windowScale;
	}

	public void setWindowScale(int windowScale) {
		this.windowScale = windowScale;
	}

	public boolean isSelectiveAckPermitted() {
		return isSelectiveAckPermitted;
	}

	public void setSelectiveAckPermitted(boolean isSelectiveAckPermitted) {
		this.isSelectiveAckPermitted = isSelectiveAckPermitted;
	}

	public int getTimeStampSender() {
		return timeStampSender;
	}

	public void setTimeStampSender(int timeStampSender) {
		this.timeStampSender = timeStampSender;
	}

	public int getTimeStampReplyTo() {
		return timeStampReplyTo;
	}

	public void setTimeStampReplyTo(int timeStampReplyTo) {
		this.timeStampReplyTo = timeStampReplyTo;
	}

}
