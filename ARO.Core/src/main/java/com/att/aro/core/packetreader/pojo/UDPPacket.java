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
package com.att.aro.core.packetreader.pojo;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UdpPacket.UdpHeader;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A bean class that provides access to UDP Packet data.
 */
public class UDPPacket extends IPPacket implements Serializable {
	private static final long serialVersionUID = 1L;


	public static enum DNSPort {
		DNS(53),
		MDNS(5353),
		DNS_OVER_TLS(853);


		private final int portNumber;

		private DNSPort(int portNumber) {
			this.portNumber = portNumber;
		}

		public static boolean contains(int portNumber) {
			for (DNSPort port : values()) {
				if (port.portNumber == portNumber) {
					return true;
				}
			}

			return false;
		}
	}

	private int sourcePort;
	private int destinationPort;
	private int packetLength;
	private int dataOffset;
	private int payloadLen;
	@JsonIgnore
	private DomainNameSystem dns;


	public UDPPacket(long seconds, long microSeconds, org.pcap4j.packet.Packet pcap4jPacket, UdpPacket pcap4jUDPPacket) {
		super(seconds, microSeconds, pcap4jPacket);

		UdpHeader pcap4jUDPHeader = pcap4jUDPPacket.getHeader();
		// Reset data offset and payload length corresponding to upper layer protocol
		super.setDataOffset(pcap4jPacket.length() - pcap4jUDPPacket.length());
		super.setPayloadLen(pcap4jUDPPacket.length());

		sourcePort = pcap4jUDPHeader.getSrcPort().valueAsInt();
		destinationPort = pcap4jUDPHeader.getDstPort().valueAsInt();
		packetLength = pcap4jUDPPacket.length();
		payloadLen = pcap4jUDPPacket.getPayload() != null ? pcap4jUDPPacket.getPayload().length() : 0;
		dataOffset = super.getDataOffset() + pcap4jUDPHeader.length();
	}

	/**
	 * Constructor
	 */
	public UDPPacket(long seconds, long microSeconds, int len, int datalinkHdrLen, Byte protocol, Integer extensionHeadersLength, byte[] data) {
		super(seconds, microSeconds, len, datalinkHdrLen, protocol, extensionHeadersLength, data);

		int headerOffset = super.getDataOffset();
		dataOffset = headerOffset + 8;

		ByteBuffer bytes = ByteBuffer.wrap(data);
		sourcePort = bytes.getShort(headerOffset) & 0xFFFF;
		destinationPort = bytes.getShort(headerOffset + 2) & 0xFFFF;
		packetLength = bytes.getShort(headerOffset + 4) & 0xFFFF;
		payloadLen = packetLength - 8;
		
	}

	/**
	 * @return The offset within the data array of the packet data excluding the header information.
	 * @see com.att.aro.pcap.IPPacket#getDataOffset()
	 */
	@Override
	public int getDataOffset() {
		return dataOffset;
	}

	/**
	 * @see com.att.aro.pcap.IPPacket#getPayloadLen()
	 */
	@Override
	public int getPayloadLen() {
		return payloadLen;
	}

	/**
	 * Gets the source port number.
	 * 
	 * @return An int value that is the source port number.
	 */
	public int getSourcePort() {
		return sourcePort;
	}

	/**
	 * Gets the destination port number.
	 * 
	 * @return An int value that is the destination port number.
	 */
	public int getDestinationPort() {
		return destinationPort;
	}

	/**
	 * Gets the length of the packet including the header.
	 * 
	 * @return An int value that is the length of the packet (in bytes).
	 */
	@Override
	public int getPacketLength() {
		return packetLength;
	}

	/**
	 * Indicates whether the data portion of this packet contains a DNS packet
	 * @return
	 */
	public boolean isDNSPacket() {
		return DNSPort.contains(destinationPort) || DNSPort.contains(sourcePort);
	}

	/**
	 * If this packet contains DNS info it may be accessed here
	 * @return the dns or null if this is not a DNS packet
	 */
	public DomainNameSystem getDns() {
		return dns;
	}
	public void setDns(DomainNameSystem dns){
		this.dns = dns;
	}

}
