/*
 Copyright 2014 AT&T
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.att.aro.core.packetreader.pojo;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.IpV6Packet.IpV6Header;
import org.apache.log4j.LogManager;

/**
 * A bean class that provides access to IP Packet data.
 */
public class IPPacket extends Packet implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger(IPPacket.class.getName());

	private byte ipVersion;
	private byte priority;
	private int packetLength;
	private boolean rsvFrag;
	private boolean dontFrag;
	private boolean moreFrag;
	private short fragmentOffset;
	private short timeToLive;
	private short protocol;
	private InetAddress sourceIPAddress;
	private InetAddress destinationIPAddress;

	// For IPv6, dataOffset and payloadLen are initially set to the values just after IPv6 raw header (40 bytes) in the packet which is true by the definition of an IPv6 packet. 
	// For TCP/UDP packets in this tool, the values are reset to represent the data corresponding to actual upper level packet protocol i.e. TCP/UDP, ignoring the extension headers if present.
	private int dataOffset;
	private int payloadLen;



	public IPPacket(long seconds, long microSeconds, org.pcap4j.packet.Packet pcap4jPacket) {
		super(seconds, microSeconds, pcap4jPacket);

		IpPacket pcap4jIPPacket = pcap4jPacket.get(IpPacket.class);
		if (pcap4jIPPacket instanceof IpV4Packet) {
			IpV4Packet ipv4Packet = (IpV4Packet) pcap4jIPPacket;
			IpV4Header ipv4Header = ipv4Packet.getHeader();

			ipVersion = 0x04;
			rsvFrag = ipv4Header.getReservedFlag();
			dontFrag = ipv4Header.getDontFragmentFlag();
			moreFrag = ipv4Header.getMoreFragmentFlag();
			fragmentOffset = ipv4Header.getFragmentOffset();
			timeToLive = ipv4Header.getTtl();
			protocol = ipv4Header.getProtocol().value();
			sourceIPAddress = ipv4Header.getSrcAddr();
			destinationIPAddress = ipv4Header.getDstAddr();

			packetLength = ipv4Packet.length();
			payloadLen = ipv4Packet.getPayload() != null ? ipv4Packet.getPayload().length() : 0;
			dataOffset = super.getDataOffset() + ipv4Header.length();
		} else if (pcap4jIPPacket instanceof IpV6Packet) {
			IpV6Packet ipv6Packet = (IpV6Packet) pcap4jIPPacket;
			IpV6Header ipv6Header = ipv6Packet.getHeader();

			ipVersion = 0x06;
			sourceIPAddress = ipv6Header.getSrcAddr();
			destinationIPAddress = ipv6Header.getDstAddr();
			packetLength = ipv6Packet.length(); 
			payloadLen = ipv6Header.getPayloadLengthAsInt();
			dataOffset = super.getDataOffset() + ipv6Header.length();
		}
	}

	/**
	 * Creates a new instance of the IPPacket class.
	 */
	public IPPacket(long seconds, long microSeconds, int len, int datalinkHdrLen, Byte protocol, Integer extensionHeadersLength, byte[] data) {
		super(seconds, microSeconds, len, datalinkHdrLen, data);

		// Parse data
		ByteBuffer bytes = ByteBuffer.wrap(data);
		int headerOffset = super.getDataOffset();

		// check for IPv4 or IPv6
		ipVersion = (byte) ((bytes.get(headerOffset) & 0xf0) >> 4);
		int hlen = -1;

		if (ipVersion == 6) {
			hlen = 40;
			if (extensionHeadersLength == null || protocol == null) {
				this.protocol = protocol == null ? bytes.get(headerOffset + 6) : protocol;
				extensionHeadersLength = calculateLengthOfExtensionHeaders(data, headerOffset + hlen);
			}

			packetLength = len - headerOffset;
			dataOffset = headerOffset + hlen + extensionHeadersLength;
			payloadLen = packetLength - extensionHeadersLength - hlen;
		} else {
			hlen = ((bytes.get(headerOffset) & 0x0f) << 2);
			dataOffset = headerOffset + hlen;

			/*
			 * the packet length read could be negative, if the packet length is a large number (more
			 * than 15 bits in storage size (but less than 16 bits).
			 * So chop off the first two bytes (and hence the signed bits) before setting the field.
			 */
			packetLength = bytes.getShort(headerOffset + 2)& 0x0000ffff;
			if (packetLength == 0) {
				// Assume TCP segmentation offload (TSO) so calculate our own packet length 
				packetLength = len - headerOffset;
			}
			
			payloadLen = packetLength - hlen;

			short ivalue = bytes.getShort(headerOffset + 6);
			rsvFrag = (ivalue & 0x8000) != 0;
			dontFrag = (ivalue & 0x4000) != 0;
			moreFrag = (ivalue & 0x2000) != 0;
			fragmentOffset = (short) (ivalue & 0x1fff);

			timeToLive = bytes.get(headerOffset + 7);
			this.protocol = protocol == null ? bytes.get(headerOffset + 8) : protocol;
		}


		byte[] buffer = null;
		int addrLgth = -1;
		int addrOffset = -1;
		if (ipVersion == 6) {
			addrLgth = 16;
			addrOffset = 8;
		} else {
			addrLgth = 4;
			addrOffset = 12;
		}

		buffer = new byte[addrLgth];
		bytes.position(headerOffset + addrOffset);
		bytes.get(buffer, 0, addrLgth);
		try {
			sourceIPAddress = InetAddress.getByAddress(buffer);
		} catch (UnknownHostException e) {
			LOGGER.warn("Unable to determine source IP - " + e.getMessage());
		}
		try {
			bytes.get(buffer, 0, addrLgth);
			destinationIPAddress = InetAddress.getByAddress(buffer);
		} catch (UnknownHostException e) {
			LOGGER.warn("Unable to determine destination IP - " + e.getMessage());
		}
	}


	/**
     * Calculate total length of extension headers until an Upper Layer Protocol i.e. TCP(6) or UDP(17) is encountered
     * OR No Next Header (59) is found
     *
     * Currently Hop By Hop (0), Routing (43) and Destination Options (60) headers are supported.
     * TODO: Implement support for Fragment (44) and Encapsulating Security Payload (50) headers
     * @param data
     * @param start
     * @return Total length of extension headers present
     */
	private int calculateLengthOfExtensionHeaders(byte[] data, int start) {
		int headerLength = 0;

		if (start < data.length - 1) {
	        switch (protocol) {
	            case 0: // Hop by Hop Options Header
	            case 43: // Routing Header
	            case 51: // Authentication Header
	            case 60: // Destination Options Header
	                headerLength += (protocol == 51 ? (data[start + 1] + 2) * 4 : (data[start + 1] * 8) + 8);
	                protocol = data[start];
	                headerLength += calculateLengthOfExtensionHeaders(data, start + headerLength);
	                break;
	            case 59: // No Next header. This signifies that there exists nothing after the corresponding header
	            case 6: // TCP
	            case 17: // UDP
	                break;
	        }
		}

        return headerLength;
	}

	/**
	 * @see com.att.aro.pcap.Packet#getDataOffset()
	 * @return The offset within the data array of the packet data, excluding the header information.
	 */
	@Override
	public int getDataOffset() {
		return dataOffset;
	}

	public void setDataOffset(int dataOffset) {
		this.dataOffset = dataOffset;
	}

	/**
	 * @see com.att.aro.pcap.Packet#getPayloadLen()
	 */
	@Override
	public int getPayloadLen() {
		return payloadLen;
	}

	public void setPayloadLen(int payloadLen) {
		this.payloadLen = payloadLen;
	}

	/**
	 * Gets the IP version for IPv4 and IPv6.
	 * 
	 * @return A byte value that is the IP version. A value of 4 indicates IPv4.
	 */
	public byte getIPVersion() {
		return ipVersion;
	}

	/**
	 * Gets the priority value for the IPPacket. In Pv4 this is TOS, and in Pv6
	 * it is Traffic Class.
	 * 
	 * @return A byte value that is the priority value.
	 */
	public byte getPriority() {
		return priority;
	}

	/**
	 * Gets the length of the packet including the header.
	 * 
	 * @return An int value that is the length of the packet in bytes.
	 */
	public int getPacketLength() {
		return packetLength;
	}

	/**
	 * Gets the value of the Fragmentation Reservation flag. (IPv4 only)
	 * 
	 * @return A boolean value that is the Fragmentation Reservation flag.
	 */
	public boolean isRsvFrag() {
		return rsvFrag;
	}

	/**
	 * Gets the value of the Don't Fragment flag. (IPv4 only)
	 * 
	 * @return A boolean value that is the Don't Fragment flag.
	 */
	public boolean isDontFrag() {
		return dontFrag;
	}

	/**
	 * Gets the value of the More Fragment flag. (IPv4 only)
	 * 
	 * @return A boolean value that is the More Fragment flag.
	 */
	public boolean isMoreFrag() {
		return moreFrag;
	}

	/**
	 * gets the fragment offset.(IPv4 only)
	 * 
	 * @return A short value that is the fragment offset.
	 */
	public short getFragmentOffset() {
		return fragmentOffset;
	}

	/**
	 * Gets theTime to Live (TTL) value. (IPv4 only)
	 * 
	 * @return A short that is the TTL value.
	 */
	public short getTimeToLive() {
		return timeToLive;
	}

	/**
	 * Gets the protocol for the IPPacket.
	 * 
	 * @return A short value that is the protocol.
	 */
	public short getProtocol() {
		return protocol;
	}

	/**
	 * Gets the source IP Address.
	 * 
	 * @return The source IP Address in the InetAddress format.
	 */
	public InetAddress getSourceIPAddress() {
		return sourceIPAddress;
	}

	/**
	 * Returns Gets the destination IP address.
	 * 
	 * @return The destination IP address in the InetAddress format.
	 */
	public InetAddress getDestinationIPAddress() {
		return destinationIPAddress;
	}

}
