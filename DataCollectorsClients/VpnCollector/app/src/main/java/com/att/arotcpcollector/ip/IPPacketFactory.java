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
package com.att.arotcpcollector.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.util.Log;

import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.util.PacketUtil;

/**
 * class for creating packet data, header etc related to IP
 * 
 * @author Borey Sao Date: June 30, 2014
 */
public class IPPacketFactory {

	public static final String TAG = "IPPacketFactory";

	/**
	 * Provides a copy of original IP header object
	 * @param ipheader
	 * @return
	 */
	public static IPHeader copyIPHeader(IPHeader ipheader) {
		if (ipheader.getIpVersion() == 6) {
			return new IPV6Header((IPV6Header) ipheader);
		} else {
			return new IPv4Header((IPv4Header) ipheader);
		}
	}

	/**
	 * Create appropriate IP header for the provided data
	 * @param buffer
	 * @param start
	 * @return
	 * @throws PacketHeaderException
	 */
	public static IPHeader createIPHeader(byte[] buffer, int start) throws PacketHeaderException {
		byte version = (byte) (buffer[start] >> 4);

		if (version == 4) {
			return createIPv4Header(version, buffer, start);
		} else if (version == 6) {
			return createIPv6Header(buffer, start);
		}
		throw new PacketHeaderException("Unsupported IP version " + version);
	}

	/**
	 * Create IP Header byte data from the corresponding object
	 * @param header
	 * @return
	 */
	public static byte[] createIPHeaderData(IPHeader header) {
		if (header.getIpVersion() == 4) {
			return createIPv4HeaderData((IPv4Header) header);
		} else {
			return createIPv6HeaderData((IPV6Header) header);
		}
	}


	/**
	 * Create IPv4 Header array of byte from a given IPv4Header object
	 * 
	 * @param header
	 *            instance of IPv4Header
	 * @return array of byte
	 */
	private static byte[] createIPv4HeaderData(IPv4Header header) {

		byte[] buffer = new byte[header.getIPHeaderLength()];
		byte first = (byte) (header.getInternetHeaderLength() & 0xF);
		first = (byte) (first | 0x40);
		buffer[0] = first;
		byte second = (byte) (header.getDscpOrTypeOfService() << 2);
		byte ecnMask = (byte) (header.getEcn() & 0xFF);
		second = (byte) (second & ecnMask);
		buffer[1] = second;

		byte totallength1 = (byte) (header.getTotalLength() >> 8);
		byte totallength2 = (byte) header.getTotalLength();
		buffer[2] = totallength1;
		buffer[3] = totallength2;

		byte id1 = (byte) (header.getIdentification() >> 8);
		byte id2 = (byte) header.getIdentification();
		buffer[4] = id1;
		buffer[5] = id2;

		//combine flags and partial fragment offset
		byte leftfrag = (byte) ((header.getFragmentOffset() >> 8) & 0x1F);
		byte flag = (byte) (header.getFlag() | leftfrag);
		buffer[6] = flag;
		byte rightfrag = (byte) header.getFragmentOffset();
		buffer[7] = rightfrag;

		byte timeToLive = header.getTimeToLive();
		buffer[8] = timeToLive;

		byte protocol = header.getProtocol();
		buffer[9] = protocol;

		byte checksum1 = (byte) (header.getHeaderChecksum() >> 8);
		byte checksum2 = (byte) header.getHeaderChecksum();
		buffer[10] = checksum1;
		buffer[11] = checksum2;

		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.put(header.getSourceIP().getAddress());
		buf.put(header.getDestinationIP().getAddress());

		//souce ip
		System.arraycopy(buf.array(), 0, buffer, 12, 4);
		//dest ip
		System.arraycopy(buf.array(), 4, buffer, 16, 4);

		if (header.getOptionBytes().length > 0) {
			System.arraycopy(header.getOptionBytes(), 0, buffer, 20, header.getOptionBytes().length);
		}

		return buffer;
	}
	/**
	 * Create IPv6 byte data from the object
	 * @param header
	 * @return
	 */
	private static byte[] createIPv6HeaderData(IPV6Header header) {
		byte[] data = new byte[header.getIPHeaderLength()];

		data[0] = (byte) ((header.getIpVersion() << 4) | (header.getTrafficClass() >> 4));
		data[1] = (byte) ((header.getTrafficClass() << 4) | (header.getFlowLabel() >> 16));
		data[2] = (byte) (header.getFlowLabel() >> 8);
		data[3] = (byte) header.getFlowLabel();
		data[4] = (byte) (header.getPayloadLength() >> 8);
		data[5] = (byte) header.getPayloadLength();
		data[6] = header.getNextHeader();
		data[7] = header.getHopLimit();

		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.put(header.getSourceIP().getAddress());
		buf.put(header.getDestinationIP().getAddress());
		if (header.getExtensionHeadersData().length > 0) {
			buf.put(header.getExtensionHeadersData());
		}
		// Copy Source IP to data
		System.arraycopy(buf.array(), 0, data, 8, 16);
		// Copy Destination IP to data
		System.arraycopy(buf.array(), 16, data, 24, 16);
		// Copy extension headers' data
		if (header.getExtensionHeadersData().length > 0) {
			System.arraycopy(buf.array(), 32, data, 40, header.getExtensionHeadersData().length);
		}

		return data;
	}


	/**
	 * create IPv4 Header from a given array of byte
	 * 
	 * @param buffer
	 *            array of byte
	 * @param start
	 *            position to start extracting data
	 * @return a new instance of IPv4Header
	 * @throws PacketHeaderException
	 */
	private static IPHeader createIPv4Header(byte version, byte[] buffer, int start) throws PacketHeaderException {

		IPv4Header head = null;

		//avoid Index out of range
		if ((buffer.length - start) < 20) {
			throw new PacketHeaderException("Minimum IPv4 header is 20 bytes. There are less than 20 bytes"
					+ " from start position to the end of array.");
		}
		if (version != 0x04) {
			throw new PacketHeaderException("Invalid IPv4 header. IP version should be 4.");
		}
		byte internetHeaderLength = (byte) (buffer[start] & 0x0F);
		if (buffer.length < (start + internetHeaderLength * 4)) {
			throw new PacketHeaderException("Not enough space in array for IP header");
		}
		byte dscp = (byte) (buffer[start + 1] >> 2);
		byte ecn = (byte) (buffer[start + 1] & 0x03);
		int totalLength = PacketUtil.getNetworkInt(buffer, start + 2, 2);
		int identification = PacketUtil.getNetworkInt(buffer, start + 4, 2);
		byte flag = buffer[start + 6];
		boolean mayFragment = (flag & 0x40) > 0x00;
		boolean lastFragment = (flag & 0x20) > 0x00;
		int fragmentBits = PacketUtil.getNetworkInt(buffer, start + 6, 2);
		int fragset = fragmentBits & 0x1FFF;
		short fragmentOffset = (short) fragset;
		byte timeToLive = buffer[start + 8];
		byte protocol = buffer[start + 9];
		int checksum = PacketUtil.getNetworkInt(buffer, start + 10, 2);
		InetAddress sourceIP = null;
		InetAddress destinationIP = null;
		try {
			sourceIP = InetAddress.getByAddress(Arrays.copyOfRange(buffer, start + 12, start + 16));
			destinationIP = InetAddress.getByAddress(Arrays.copyOfRange(buffer, start + 16, start + 20));
		} catch (UnknownHostException e) {
			Log.e(TAG, "Unable to extract source or destination IP", e);
		}

		byte[] options = null;
		if (internetHeaderLength == 5) {
			options = new byte[0];
		} else {
			int optionLength = (internetHeaderLength - 5) * 4;
			options = new byte[optionLength];
			System.arraycopy(buffer, start + 20, options, 0, optionLength);
		}
		head = new IPv4Header(version, internetHeaderLength, dscp, ecn, totalLength, identification,
				mayFragment, lastFragment, fragmentOffset, timeToLive, protocol, checksum,
				sourceIP, destinationIP, options);
		return head;
	}

	/**
	 *
	 * @param buffer
	 * @param start
	 * @return
	 * @throws PacketHeaderException
	 */
	private static IPHeader createIPv6Header(byte[] buffer, int start) throws PacketHeaderException {
		return new IPV6Header(buffer, start);
	}
}
