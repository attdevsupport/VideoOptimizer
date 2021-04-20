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
package com.att.arotcpcollector.udp;

import android.util.Log;

import com.att.arotcpcollector.ip.IPHeader;
import com.att.arotcpcollector.ip.IPPacketFactory;
import com.att.arotcpcollector.ip.IPV6Header;
import com.att.arotcpcollector.ip.IPv4Header;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.util.PacketUtil;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class UDPPacketFactory {
	public UDPPacketFactory(){}

	public UDPHeader createUDPHeader(byte[] buffer, int start) throws PacketHeaderException{
		UDPHeader header = null;
		if((buffer.length - start) < 8){
			throw new PacketHeaderException("Minimum UDP header is 8 bytes.");
		}
		int srcPort = PacketUtil.getNetworkInt(buffer, start, 2);
		int destPort = PacketUtil.getNetworkInt(buffer, start + 2, 2);
		int length = PacketUtil.getNetworkInt(buffer, start + 4,2);
		int checksum = PacketUtil.getNetworkInt(buffer, start + 6,2);
		StringBuilder str = new StringBuilder();
		str.append("\r\n..... new UDP header .....");
		str.append("\r\nstarting position in buffer: "+start);
		str.append("\r\nSrc port: "+srcPort);
		str.append("\r\nDest port: "+destPort);
		str.append("\r\nLength: "+length);
		str.append("\r\nChecksum: "+checksum);
		str.append("\r\n...... end UDP header .....");
		Log.d("AROCollector",str.toString());
		header = new UDPHeader(srcPort, destPort, length, checksum);
		return header;
	}
	public UDPHeader copyHeader(UDPHeader header){
		UDPHeader newh = new UDPHeader(header.getSourcePort(), header.getDestinationPort(), header.getLength(), header.getChecksum());
		return newh;
	}
	/**
	 * create packet data for responding to vpn client
	 * @param ip IPv4Header sent from VPN client, will be used as the template for response
	 * @param udp UDPHeader sent from VPN client
	 * @param packetdata packet data to be sent to client
	 * @return array of byte
	 */
	public byte[] createResponsePacket(IPHeader ip, UDPHeader udp, byte[] packetdata){
		byte[] buffer = null;
		int udplen = 8;
		int datalength = 0;
		if(packetdata != null){
			datalength = packetdata.length;
		}

		IPHeader ipheader = IPPacketFactory.copyIPHeader(ip);
		UDPHeader udpHeader = copyHeader(udp);

		int totallength = ipheader.getIPHeaderLength() + udplen + datalength;
		short checksum = 0;

		buffer = new byte[totallength];


		InetAddress srcIp = ip.getDestinationIP();
		InetAddress destIp = ip.getSourceIP();
		if (ipheader.getIpVersion() == 4) {
			((IPv4Header) ipheader).setMayFragment(false);
		}
		ipheader.setSourceIP(srcIp);
		ipheader.setDestinationIP(destIp);
		ipheader.setIdentification(PacketUtil.getPacketId());

		//ip's length is the length of the entire packet => IP header length + UDP header length (8) + UDP body length
		ipheader.setTotalLength(totallength);
		if (ipheader.getIpVersion() == 6) {
			((IPV6Header)ipheader).setPayloadLength((short) (totallength - IPV6Header.INITIAL_FIXED_HEADER_LENGTH));
		}

		byte[] ipdata = IPPacketFactory.createIPHeaderData(ipheader);
		// Calculate checksum for IPv4 header. There's no need to calculate checksum for IPv6 header.
		byte[] zero = {0,0};
		if (ipheader.getIpVersion() == 4) {
			//zero out checksum first before calculation
			System.arraycopy(zero, 0, ipdata, 10, 2);
			byte[] ipchecksum = PacketUtil.calculateChecksum(ipdata, 0, ipdata.length);
			//write result of checksum back to buffer
			System.arraycopy(ipchecksum, 0, ipdata, 10, 2);
		}

		// Copy IP Header data to final buffer
		System.arraycopy(ipdata, 0, buffer, 0, ipdata.length);


		int srcPort = udp.getDestinationPort();
		int destPort = udp.getSourcePort();
		udpHeader.setSourcePort(srcPort);
		udpHeader.setDestinationPort(destPort);
		// Reset UDP length field
		udpHeader.setLength(datalength + udplen);
		// Zero out checksum to recalculate
		udpHeader.setChecksum(0);
		byte[] udpData = getHeaderData(udpHeader);

		// Copy UDP Header data to final buffer
		System.arraycopy(udpData, 0, buffer, ipdata.length, udpData.length);

		// Copy UDP Payload data to final buffer if available
		if (datalength > 0) {
			System.arraycopy(packetdata, 0, buffer, ipdata.length + udpData.length, datalength);
		}

		// Calculate checksum
		int udpStartOffset = ipdata.length;
		byte[] udpchecksum = PacketUtil.calculateChecksum(buffer, udpStartOffset, udpData.length + datalength,
				ipheader.getDestinationIP(), ipheader.getSourceIP(), ipheader.getIpVersion(), ipheader.getProtocol());

		// Write new checksum back to array
		System.arraycopy(udpchecksum, 0, buffer, udpStartOffset + 6, 2);

		return buffer;
	}

	/**
	 * Converts UDPHeader object to corresponding byte stream
	 * @param header
	 * @return
	 */
	private byte[] getHeaderData(UDPHeader header) {
		byte[] buffer = new byte[8];
		ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer);

		wrappedBuffer.putShort((short) header.getSourcePort());
		wrappedBuffer.putShort((short) header.getDestinationPort());
		wrappedBuffer.putShort((short) header.getLength());
		wrappedBuffer.putShort((short) header.getChecksum());

		return wrappedBuffer.array();
	}

}//end
