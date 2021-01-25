/*
 *  Copyright 2019 AT&T
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.att.aro.core.util.Util;

import lombok.Data;

/**
 * Session contains all of the packets from the session. 
 * The purpose is for analysis and modeling of session data.
 * Date: April 24, 2014
 */
@Data 
public class Session implements Serializable, Comparable<Session> {

	private static final long serialVersionUID = 1L;

	/** plain text, no https */
	public static final int HTTPS_MODE_NONE = 0;
	/** https, no compression */
	public static final int HTTPS_MODE_NORMAL = 1;
	/** https, with deflate compression */
	public static final int HTTPS_MODE_DEFLATE = 2;
	public static final int ALERT_LEVEL_WARNING = 1;
	public static final int ALERT_LEVEL_FATAL = 2;
	public static final int ALERT_CLOSE_NOTIFY = 0;

	private int localPort;
	private int remotePort;
	private boolean decrypted;
	private long baseUplinkSequenceNumber = 0;
	private long baseDownlinkSequenceNumber = 0;
	
	private String sessionKey;
	private String remoteHostName;
	
	private InetAddress localIP;
	private InetAddress remoteIP;
	
	
	private PacketInfo dnsRequestPacket;
	private PacketInfo dnsResponsePacket;
	private PacketInfo lastSslHandshakePacket;
	
	private boolean iOSSecureSession;
	
	private boolean dataInaccessible = false;
	
	private boolean sessionComplete = false;

	/**
	 * Domain name is the initial host name requested that initiated a TCP
	 * session. This value is either the host name specified by the first HTTP
	 * request in the session or the referrer domain that caused this session to
	 * be opened
	 */
	private String domainName;

	/**
	 * A count of the number of files downloaded during the TCP session.
	 */
	private int fileDownloadCount;

	/**
	 * The number of bytes transferred during the session.
	 */
	private long bytesTransferred;
	
	/**
	 * Indicates whether SSL packets were detected in this session
	 */
	private boolean ssl = false;

	/**
	 * Indicates whether the session is UDP only or not.<br>
	 * true if session is UDP otherwise false
	 */
	private boolean udpOnly = false;

	/**
	 * A List of PacketInfo objects containing all packets in the session
	 */
	private List<PacketInfo> packets = new ArrayList<PacketInfo>();

	/**
	 * A List of PacketInfo objects containing the packet data.
	 */
	private List<PacketInfo> udpPackets = new ArrayList<PacketInfo>();
	
	
	/**
	 * List of Upload Packets ordered by Sequence Numbers for TCP Session.
	 */
	private TreeMap<Long, PacketInfo> uplinkPacketsSortedBySequenceNumbers = new TreeMap<>();
	
	/**
	 * List of Download Packets ordered by Sequence Numbers for TCP Session.
	 */
	private TreeMap<Long, PacketInfo> downlinkPacketsSortedBySequenceNumbers = new TreeMap<>();

	/**
	 * A Set of strings containing the application names.
	 */
	private Set<String> appNames = new HashSet<String>(1);

	/**
	 * A TCPSession.Termination object containing the information<br>
	 * Object is null for no session termination in the trace.
	 */
	private Termination sessionTermination;

	/**
	 * A List of HTTPRequestResponseInfo objects containing the information.
	 */
	private List<HttpRequestResponseInfo> requestResponseInfo = new ArrayList<HttpRequestResponseInfo>();

	/**
	 * An array of bytes containing the uplink storage.
	 */
	private byte[] storageUl;

	/**
	 * A Map of offsets and corresponding PacketInfo objects that contain the
	 * uplink packet data.
	 */
	private SortedMap<Integer, PacketInfo> packetOffsetsUl;

	/**
	 * An array of bytes containing the downlink storage.
	 */
	private byte[] storageDl;

	/**
	 * A Map of offsets and corresponding PacketInfo objects that contain the
	 * downlink packet data.
	 */
	private SortedMap<Integer, PacketInfo> packetOffsetsDl;

	/**
	 * app-layer-protocol<br>
	 * has been refactored out of this class
	 */
	private int protocol; //app-layer-protocol

	/**
	 * HttpsMode<br>
	 * has been refactored out of this class
	 */
	private int httpsMode = HTTPS_MODE_NONE;

	/**
	 * An array of bytes containing the extended uplink storage.
	 */
	private byte[] storageUlext = null;

	/**
	 * An array of bytes containing the extended downlink storage.
	 */
	private byte[] storageDlext = null;
		
	private TreeMap<Double, PacketInfo> synPackets = new TreeMap<>();
	private TreeMap<Double, PacketInfo> synAckPackets = new TreeMap<>();
	
	private static final Logger LOGGER = LogManager.getLogger(Session.class.getName());
	
	/**
	 * Initializes an instance of the TCPSession class, using the specified
	 * remote IP, remote port, and local port.
	 * 
	 * @param remoteIP
	 *            The remote IP address.
	 * 
	 * @param remotePort
	 *            The remote port.
	 * 
	 * @param localPort
	 *            The local port.
	 */
	public Session(InetAddress localIP, InetAddress remoteIP, int remotePort, int localPort, String sessionKey) {
		
		this.localIP = localIP;
		this.localPort = localPort;
		
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		
		this.sessionKey = sessionKey;
		
	}

	/**
	 * Compares Session start times
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Session session) {
		return Double.valueOf(getSessionStartTime()).compareTo(Double.valueOf(session.getSessionStartTime()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Session) {
			Session oTcp = (Session) obj;
			return Double.valueOf(getSessionStartTime()) == oTcp.getSessionStartTime()
					&& (getLocalPort() == oTcp.getLocalPort() && (getRemotePort() == oTcp.getRemotePort()));
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (int) Double.doubleToLongBits(getSessionStartTime());
	}

	/**
	 * Returns all of the  TCP & UDP packets in the session.
	 * 
	 * @return A List of PacketInfo objects containing the packet data.
	 */
	public List<PacketInfo> getAllPackets() {
		ArrayList<PacketInfo> allPackets = new ArrayList<>();
		if(packets != null && !packets.isEmpty()) {
			allPackets.addAll(packets);
		}
		if(udpPackets != null && !udpPackets.isEmpty()) {
			allPackets.addAll(udpPackets);
		}
		return allPackets;
	}

	/**
	 * Gets the start time of the session, in seconds, relative to the start of
	 * the trace.
	 * 
	 * @return The start time of the session.
	 */
	public double getSessionStartTime() {
		if (packets != null && !packets.isEmpty()) {
			return packets.get(0).getTimeStamp();
		}

		if (udpPackets != null && !udpPackets.isEmpty()) {
			return udpPackets.get(0).getTimeStamp();
		}

		return 0.0;
	}

	/**
	 * Gets the end time of the session, in seconds, relative to the start of
	 * the trace.
	 * 
	 * @return The end time of the session.
	 */
	public double getSessionEndTime() {
		
		if (packets != null && !packets.isEmpty()) {
			return packets.get(packets.size() - 1).getTimeStamp();
		}

		if (udpPackets != null && !udpPackets.isEmpty()) {
			return udpPackets.get(udpPackets.size() - 1).getTimeStamp();
		}

		return 0.0;
	}

	/**
	 * Gets the start time of the UDP session, in seconds, relative to the start
	 * of the trace.
	 * 
	 * @return The start time of the UDP session.
	 */
	public double getUDPSessionStartTime() {
		return udpPackets.get(0).getTimeStamp();
	}

	/**
	 * Gets the end time of the UDP session, in seconds, relative to the start
	 * of the trace.
	 * 
	 * @return The end time of the UDP session.
	 */
	public double getUDPSessionEndTime() {
		return udpPackets.get(udpPackets.size() - 1).getTimeStamp();
	}
	
	/**
	 * Returns the consolidated string for uplink and downlink storage.
	 * 
	 * @return The result string.
	 */
	public String getDataText() {
		
		// Limit Content Viewer to 20000 Bytes to prevent the system from hanging
		
		ByteArrayOutputStream contentData = new ByteArrayOutputStream();
		BufferedOutputStream outputStream = new BufferedOutputStream(contentData);
		
		try {
			int totalBytesWritten = 0;
			for (HttpRequestResponseInfo rrInfo : getRequestResponseInfo()) {
				if (totalBytesWritten < 20000) {
					totalBytesWritten+=rrInfo.getContentLength();
					if (rrInfo.getDirection().equals(HttpDirection.REQUEST)) {
						outputStream.write("\n--UPLINK--\n".getBytes());
					} else {
						outputStream.write("\n--DOWNLINK--\n".getBytes());
					}
					outputStream.write(rrInfo.getHeaderData().toByteArray());
					outputStream.write(rrInfo.getPayloadData().toByteArray());
				} else {
					break;
				}
			}
			outputStream.flush();
		} catch (IOException exception) {
			LOGGER.error("Error while retrieving content : ", exception);
		}
		return Util.byteArrayToString(contentData.toByteArray());
	}

	/**
	 * for debugging purposes
	 */
	@Override
	public String toString() {
		StringBuilder tos = new StringBuilder(24);
		tos.append("seq:").append(getSessionStartTime());
		tos.append(", abs:").append(getSessionStartTime());
		tos.append(", lPort:").append(getLocalPort());
		tos.append(", count:");
		if (getPackets() != null) {
			tos.append(getPackets().size());
		} else {
			tos.append("null");
		}
		return tos.toString();
	}
	
	public String getLatency(Session session) {
		double latency = 0.0;
		if (!session.isUdpOnly()) {
			latency = getAcknowledgeTimestamp(session.getAllPackets(), true) - session.getSessionStartTime();
		}
		return latency != 0 ? Util.formatDouble(latency) : "0.00";
	}

	public double getAcknowledgeTimestamp(List<PacketInfo> packetInfoList, boolean isTCP) {
		double timeStamp = 0.0;
		for (PacketInfo packetInfo : packetInfoList) {
			if (isTCP) {
				if (packetInfo.getTcpInfo() == TcpInfo.TCP_ESTABLISH
						&& packetInfo.getDir() == PacketDirection.DOWNLINK) {
					timeStamp = packetInfo.getTimeStamp();
					break;
				}
			}
		}
		return timeStamp;
	}
	
	public boolean addTcpPacket(PacketInfo packetInfo, long sequnceNumber) {
		packets.add(packetInfo);
		if (packetInfo.getDir().equals(PacketDirection.UPLINK)) {
			// Done to handle TCP Sequence Number Wrap Around
			if (sequnceNumber < getBaseUplinkSequenceNumber()) {
				sequnceNumber += 0xFFFFFFFF;
				sequnceNumber++;
			}
			PacketInfo tempPacket;
			if ((tempPacket = uplinkPacketsSortedBySequenceNumbers.get(sequnceNumber)) != null) {
				if (packetInfo.getPayloadLen() == 0 || packetInfo.getPayloadLen() == tempPacket.getPayloadLen()) {
					return false;
				}
			}
			uplinkPacketsSortedBySequenceNumbers.put(sequnceNumber, packetInfo);
		} else if (packetInfo.getDir().equals(PacketDirection.DOWNLINK)) {
			// Done to handle TCP Sequence Number Wrap Around
			if (sequnceNumber < getBaseDownlinkSequenceNumber()) {
				sequnceNumber += 0xFFFFFFFF;
				sequnceNumber++;
			}
			PacketInfo tempPacket;
			if ((tempPacket = downlinkPacketsSortedBySequenceNumbers.get(sequnceNumber)) != null) {
				if (packetInfo.getPayloadLen() == 0 || packetInfo.getPayloadLen() == tempPacket.getPayloadLen()) {
					return false;
				}
			}
			downlinkPacketsSortedBySequenceNumbers.put(sequnceNumber, packetInfo);
		}
		return true;
	}
	
	public void addUdpPacket(PacketInfo packetInfo) {
		udpPackets.add(packetInfo);
	}
	/*
	 * Function to add Packets with SYN flag to the Map
	 * 
	 */
	public void addSynPackets(PacketInfo packetInfo) {
		synPackets.put(packetInfo.getTimeStamp(), packetInfo);
	}
	
	/*
	 * Function to add Packets with SYNACK flag to the Map
	 * 
	 */
	public void addSynAckPackets(PacketInfo packetInfo) {
		synAckPackets.put(packetInfo.getTimeStamp(), packetInfo);
	}
	
}
