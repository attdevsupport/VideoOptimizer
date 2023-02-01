/*
 * Copyright 2019 AT&T
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.att.aro.core.packetanalysis.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.IByteArrayLineReader;
import com.att.aro.core.packetanalysis.IParseHeaderLine;
import com.att.aro.core.packetanalysis.ISessionManager;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpPattern;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.RequestResponseTimeline;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TcpInfo;
import com.att.aro.core.packetanalysis.pojo.Termination;
import com.att.aro.core.packetreader.pojo.DomainNameSystem;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.att.aro.core.packetreader.pojo.TCPPacket;
import com.att.aro.core.packetreader.pojo.UDPPacket;
import com.att.aro.core.util.Util;

public class SessionManagerImpl implements ISessionManager {

	private static final Logger LOGGER = LogManager.getLogger(SessionManagerImpl.class.getName());

	@Autowired
	private IParseHeaderLine parseHeaderLine;

	private String tracePath = "";

	private IByteArrayLineReader storageReader;

	private static final byte TLS_APPLICATION_DATA = 23;

	private static final int AVG_QUIC_UDP_PACKET_SIZE = 100;

	private double pcapTimeOffset;

	public double getPcapTimeOffset() {
		return pcapTimeOffset;
	}

	public void setPcapTimeOffset(double pcapTimeOffset) {
		this.pcapTimeOffset = pcapTimeOffset;
	}

	@Autowired
	public void setByteArrayLineReader(IByteArrayLineReader reader) {
		this.storageReader = reader;
	}

	public void setiOSSecureTracePath(String tracePath) {
		this.tracePath = tracePath + Util.FILE_SEPARATOR + "iosSecure" + Util.FILE_SEPARATOR;
	}

	public String getTracePath() {
		return tracePath;
	}

	Map<String, Integer> wellKnownPorts = new HashMap<String, Integer>(5);

	private double synTime = 0.0;
	private double synAckTime = 0.0;

	public SessionManagerImpl() {
		wellKnownPorts.put("HTTP", 80);
		wellKnownPorts.put("HTTPS", 443);
		wellKnownPorts.put("RTSP", 554);
	}

	/**
	 * Entry point into SessionManager from PacketAnalyzerImpl
	 * 
	 * returns List<Session> sessionList
	 */
	public List<Session> processPacketsAndAssembleSessions(List<PacketInfo> packets) {

		LOGGER.warn("processPacketsAndAssembleSessions -> Trace path: " + tracePath);
		List<Session> sessions = new ArrayList<>();
		List<PacketInfo> udpPackets = new ArrayList<>();
		Map<InetAddress, String> hostMap = new HashMap<>();
		Map<String, Session> udpSessions = new LinkedHashMap<>();
		Map<String, PacketInfo> dnsRequestDomains = new HashMap<>();
		Map<String, List<Session>> tcpSessions = new LinkedHashMap<>();
		Map<InetAddress, PacketInfo> dnsResponsePackets = new HashMap<>();

		if (packets != null) {
			for (PacketInfo packetInfo : packets) {
				Packet packet = packetInfo.getPacket();

				if (packet instanceof UDPPacket) { // UDP
					udpPackets.add(packetInfo);
					if (((UDPPacket) packet).isDNSPacket()) {
						DomainNameSystem dns = ((UDPPacket) packet).getDns();
						if (dns != null && dns.isResponse()) {
							for (InetAddress inet : dns.getIpAddresses()) {
								hostMap.put(inet, dns.getDomainName());
								dnsResponsePackets.put(inet, packetInfo);
							}
						} else if (dns != null && !dns.isResponse()) {
							dnsRequestDomains.put(dns.getDomainName(), packetInfo);
						}
					}
					associatePacketToUDPSessionAndPopulateCollections(sessions, udpSessions, packetInfo, (UDPPacket) packet);
				} else if (packet instanceof TCPPacket) { // TCP
					TCPPacket tcpPacket = (TCPPacket) packet;
					packetInfo.setTcpInfo(null);
					Session session = associatePacketToTCPSessionAndPopulateCollections(sessions, tcpSessions, packetInfo, tcpPacket);

					populateTCPPacketInfo(packetInfo, tcpPacket);

					session.setSsl(session.isSsl() ? session.isSsl() : tcpPacket.isSsl());
					if (tcpPacket.isDecrypted()) {
						tcpPacket.setDataOffset(0);
						session.setDecrypted(true);
					}
					if (session.getDnsResponsePacket() == null && dnsResponsePackets.containsKey(session.getRemoteIP())) {
						session.setDnsResponsePacket(dnsResponsePackets.get(session.getRemoteIP()));
						session.setDomainName((((UDPPacket) (session.getDnsResponsePacket()).getPacket()).getDns()).getIpAddresses().stream().findFirst().get().getHostName());
					}
					if (session.getDnsRequestPacket() == null && StringUtils.isNotBlank(session.getDomainName()) && dnsRequestDomains.containsKey(session.getDomainName())) {
						session.setRemoteHostName(session.getDomainName());
						session.setDnsRequestPacket(dnsRequestDomains.get(session.getDomainName()));
					} else {
						session.setRemoteHostName(hostMap.get(session.getRemoteIP()));
					}
					if (tcpPacket.isSslHandshake()) {
						session.setLastSslHandshakePacket(packetInfo);
						if (tcpPacket.isClientHello() && StringUtils.isNotBlank(tcpPacket.getServerNameIndication())) {
							session.setServerNameIndication(tcpPacket.getServerNameIndication());
						}
					}
					if (packetInfo.getAppName() != null) {
						session.getAppNames().add(packetInfo.getAppName());
					}

					if (!session.isSessionComplete() && (packetInfo.getTcpFlagString().contains("R")
							|| packetInfo.getTcpFlagString().contains("F"))) {
						session.setSessionComplete(true);
					}

					session.setLatency((!session.getSynAckPackets().isEmpty() && !session.getSynPackets().isEmpty()) ? calculateLatency(session) :  -1 );
				}
			}
		}

		Collections.sort(sessions);
		analyzeRequestResponses(sessions);

		return sessions;
	}

	/*
	 * Returns calculated Latency value
	 * 
	 * Packet Sequence : SYN - SYNACK Latency = First SYNACK time - First SYN time
	 * 
	 * Packet Sequence: SYN1 -SYN2 - SYNACK Latency = SYNACK time - SYN2 time
	 * 
	 * Packet Sequence: SYN1 - SYNACK - SYN2 Latency = SYNACK time - SYN1 time
	 * 
	 */
	private double calculateLatency(Session session) {
		double latencyValue = 0.0;
		double latency = -1.0;
		calculateSynAckTimestamp(session.getSynAckPackets());
		if (synAckTime > 0.0) {
			calculateSynTimestamp(session.getSynPackets(), synAckTime);
			if (synAckTime >= synTime) {
				latencyValue = synAckTime - synTime;
				latency = latencyValue > 0 ? latencyValue : 0.0;
			} else {
				LOGGER.debug("Negative latency value : " + session.getSessionKey());
			}
			session.setSynAckTime(synAckTime);
			session.setSynTime(synTime);
		}
		return latency;
	}

	/*
	 * Returns the timestamp of the packet with last SYN before the SYNACK
	 */
	private void calculateSynTimestamp(TreeMap<Double, PacketInfo> synPackets, double syncAckTime) {
		if (synPackets.containsKey(syncAckTime)) {
			synTime = syncAckTime;
		} else {
			Entry<Double, PacketInfo> synEntry = synPackets.lowerEntry(syncAckTime);
			if (synEntry != null) {
				synTime = synEntry.getKey();
			} else {
				LOGGER.debug("Packet info error : No SYN's found before the SYNACK - " + syncAckTime);
			}
		}
	}

	/*
	 * Returns the timestamp of the first packet with the SYNACK flag
	 */
	private void calculateSynAckTimestamp(TreeMap<Double, PacketInfo> synAckPackets) {
		for (PacketInfo packetInfo : synAckPackets.values()) {
			synAckTime = packetInfo.getTimeStamp();
			break;
		}
	}

	private void populateTCPPacketInfo(PacketInfo packetInfo, TCPPacket tcpPacket) {
		if (tcpPacket.isSYN()) {
			packetInfo.setTcpInfo(TcpInfo.TCP_ESTABLISH);
		} else if (tcpPacket.isFIN()) {
			packetInfo.setTcpInfo(TcpInfo.TCP_CLOSE);
		} else if (tcpPacket.isRST()) {
			packetInfo.setTcpInfo(TcpInfo.TCP_RESET);
		}
		if (packetInfo.getPayloadLen() > 0) {
			packetInfo.setTcpInfo(TcpInfo.TCP_DATA);
		}
	}

	/**
	 * Assemble UDP packets into Session
	 * 
	 * @param sessions
	 * @param udpSessions
	 * @param packetInfo
	 * @param packet
	 * @return Session of UDP packets
	 */
	private Session associatePacketToUDPSessionAndPopulateCollections(List<Session> sessions, Map<String, Session> udpSessions, PacketInfo packetInfo, UDPPacket packet) {

		int localPort;
		int remotePort;
		Session session;
		InetAddress localIP;
		InetAddress remoteIP;

		switch (packetInfo.getDir()) {
		case UPLINK:
			localIP = packet.getSourceIPAddress();
			localPort = packet.getSourcePort();
			remoteIP = packet.getDestinationIPAddress();
			remotePort = packet.getDestinationPort();
			break;
		case DOWNLINK:
			localIP = packet.getDestinationIPAddress();
			localPort = packet.getDestinationPort();
			remoteIP = packet.getSourceIPAddress();
			remotePort = packet.getSourcePort();
			break;
		default:
			localIP = packet.getSourceIPAddress();
			localPort = packet.getSourcePort();
			remoteIP = packet.getDestinationIPAddress();
			remotePort = packet.getDestinationPort();
			LOGGER.warn("29 - Unable to determine packet direction");
			break;
		}

		String sessionKey = localIP.getHostAddress() + " " + localPort + " " + remotePort + " " + remoteIP.getHostAddress();
		if (!udpSessions.containsKey(sessionKey)) {
			session = new Session(localIP, remoteIP, remotePort, localPort, sessionKey);
			if (packet.isDNSPacket()) {
				DomainNameSystem dns = packet.getDns();
				if (dns != null) {
					session.setRemoteHostName(dns.getDomainName());
					session.setDomainName(dns.getIpAddresses().stream().findFirst().get().getHostName());
				}
			}
			if (session.getRemoteHostName() == null) {
				session.setRemoteHostName(session.getRemoteIP().getHostAddress());
			}
			session.setUdpOnly(true);
			sessions.add(session);
			udpSessions.put(sessionKey, session);
		} else {
			session = udpSessions.get(sessionKey);
		}
		session.setBytesTransferred(session.getBytesTransferred() + packetInfo.getPayloadLen());
		session.addUdpPacket(packetInfo);
		return session;
	}

	private Session associatePacketToTCPSessionAndPopulateCollections(List<Session> sessions, Map<String, List<Session>> tcpSessions, PacketInfo packetInfo, TCPPacket tcpPacket) {

		int localPort;
		int remotePort;
		Session session;
		InetAddress localIP;
		InetAddress remoteIP;

		switch (packetInfo.getDir()) {
		case UPLINK:
			localIP = tcpPacket.getSourceIPAddress();
			localPort = tcpPacket.getSourcePort();
			remoteIP = tcpPacket.getDestinationIPAddress();
			remotePort = tcpPacket.getDestinationPort();
			break;

		case DOWNLINK:
			localIP = tcpPacket.getDestinationIPAddress();
			localPort = tcpPacket.getDestinationPort();
			remoteIP = tcpPacket.getSourceIPAddress();
			remotePort = tcpPacket.getSourcePort();
			break;

		default:
			localIP = tcpPacket.getSourceIPAddress();
			localPort = tcpPacket.getSourcePort();
			remoteIP = tcpPacket.getDestinationIPAddress();
			remotePort = tcpPacket.getDestinationPort();
			LOGGER.warn("29 - Unable to determine packet direction. Assuming Uplink");
			break;
		}

		String sessionKey = localIP.getHostAddress() + " " + localPort + " " + remotePort + " " + remoteIP.getHostAddress();

		if (!tcpSessions.containsKey(sessionKey)) {

			session = new Session(localIP, remoteIP, remotePort, localPort, sessionKey);
			sessions.add(session);
			List<Session> tcpSessionList = new ArrayList<>();
			tcpSessionList.add(session);
			tcpSessions.put(sessionKey, tcpSessionList);

		} else {

			session = (tcpSessions.get(sessionKey)).get(tcpSessions.get(sessionKey).size() - 1);

			if (tcpPacket.isSYN() && packetInfo.getDir().equals(PacketDirection.UPLINK)) {

				if (session.getBaseUplinkSequenceNumber() != tcpPacket.getSequenceNumber()) {
					session = new Session(localIP, remoteIP, remotePort, localPort, sessionKey);
					sessions.add(session);
					tcpSessions.get(sessionKey).add(session);

				} else {
					tcpPacket.setRetransmission(true);
				}
			}
		}

		if (session.getBaseUplinkSequenceNumber() == 0 && packetInfo.getDir().equals(PacketDirection.UPLINK)) {
			session.setBaseUplinkSequenceNumber(tcpPacket.getSequenceNumber());
		}

		if (session.getBaseDownlinkSequenceNumber() == 0 && packetInfo.getDir().equals(PacketDirection.DOWNLINK)) {
			session.setBaseDownlinkSequenceNumber(tcpPacket.getSequenceNumber());
		}

		if (!session.getTcpPackets().isEmpty() && (tcpPacket.isFIN() || tcpPacket.isRST())) {
			PacketInfo previousPacket = session.getTcpPackets().get(session.getTcpPackets().size() - 1);
			double delay = packetInfo.getTimeStamp() - previousPacket.getTimeStamp();
			session.setSessionTermination(new Termination(packetInfo, delay));
		}

		boolean packetAdditionComplete = session.addTcpPacket(packetInfo, tcpPacket.getSequenceNumber());

		if (tcpPacket.isSYN()) {
			if (packetInfo.getDir() == PacketDirection.UPLINK) {
				session.addSynPackets(packetInfo);
			} else if (packetInfo.getDir() == PacketDirection.DOWNLINK) {
				session.addSynAckPackets(packetInfo);
			}
		}

		session.setBytesTransferred(session.getBytesTransferred() + packetInfo.getPayloadLen());
		if (!packetAdditionComplete) {
			packetInfo.setTcpInfo(TcpInfo.TCP_DATA_DUP);
		}

		return session;
	}

	/**
	 * Traverse all Sessions of all types UDP/TCP/
	 * 
	 * @param sessions
	 */
	private void analyzeRequestResponses(List<Session> sessions) {

		ArrayList<HttpRequestResponseInfo> results;
		for (Session session : sessions) {
			int limit = 0;
			results = new ArrayList<>();
			PacketInfo previousPacket = null;
			if (session.isUdpOnly()) { // UDP
				HttpRequestResponseInfo rrInfo = null;
				HttpRequestResponseInfo recentUpRRInfo = null;
				HttpRequestResponseInfo recentDnRRInfo = null;
				for (PacketInfo udpPacketInfo : session.getUdpPackets()) {
					try {
						switch (udpPacketInfo.getDir()) {
						case UPLINK:
							if (!session.isDataInaccessible()) {
								rrInfo = extractHttpRequestResponseInfo(results, session, udpPacketInfo, udpPacketInfo.getDir(), previousPacket, limit);
							}
							if (rrInfo != null) {
								recentUpRRInfo = rrInfo;
							} else {
								if (443 == session.getLocalPort() || 443 == session.getRemotePort() || 80 == session.getLocalPort() || 80 == session.getRemotePort()) {
									session.setDataInaccessible(true);
									results = analyzeRequestResponsesForQUICUDPSession(session);
									break;
								}
								if (recentUpRRInfo == null) {
									// Creating a Request Objects when no actual requests were found.
									session.setDataInaccessible(true);
									rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), udpPacketInfo.getDir());
									populateRRInfo(rrInfo, udpPacketInfo, false, false, HttpDirection.REQUEST);
									results.add(rrInfo);
									recentUpRRInfo = rrInfo;
								}
								if (udpPacketInfo.getPayloadLen() != 0) {
									updateRequestResponseObject(recentUpRRInfo, udpPacketInfo);
								}
							}
							rrInfo = null;
							recentUpRRInfo.writePayload(udpPacketInfo, false, 0);
							recentUpRRInfo.addUDPPacket(udpPacketInfo);
							break;

						case DOWNLINK:
							if (!session.isDataInaccessible()) {
								rrInfo = extractHttpRequestResponseInfo(results, session, udpPacketInfo, udpPacketInfo.getDir(), previousPacket, limit);
							}
							if (rrInfo != null) {
								recentDnRRInfo = rrInfo;
							} else {
								if (recentDnRRInfo == null) {
									rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), udpPacketInfo.getDir());
									populateRRInfo(rrInfo, udpPacketInfo, false, false, HttpDirection.RESPONSE);
									results.add(rrInfo);
									recentDnRRInfo = rrInfo;
								}
								if (udpPacketInfo.getPayloadLen() != 0) {
									updateRequestResponseObject(recentDnRRInfo, udpPacketInfo);
								}
							}
							rrInfo = null;
							recentDnRRInfo.writePayload(udpPacketInfo, false, 0);
							recentDnRRInfo.addUDPPacket(udpPacketInfo);
							break;

						default:
							LOGGER.warn("91 - No direction for packet");
							continue;
						}
					} catch (IOException e) {
						LOGGER.error("Error Storing data to UDP Request Response Obect. Session ID: " + session.getSessionKey());
					}
				}

			} else { // TCP
				analyzeACK(session);
				analyzeZeroWindow(session);
				analyzeRecoverPkts(session);
				PacketInfo packetInfo = null;
				TCPPacket tcpPacket = null;
				HttpRequestResponseInfo rrInfo = null;
				HttpRequestResponseInfo tempRRInfo = null;

				try {

					long expectedUploadSeqNo = 0;
					for (long uploadSequenceNumber : session.getUplinkPacketsSortedBySequenceNumbers().keySet()) {
						// Identify correct packet from the whole transmission stream
						packetInfo = identifyCorrectTransmissionStream(session.getUplinkPacketsSortedBySequenceNumbers().get(uploadSequenceNumber),
								session.getAckNumbers(), session, PacketDirection.UPLINK);
						tcpPacket = (TCPPacket) packetInfo.getPacket();
						if (packetInfo.getPayloadLen() > 0) {
							if (!session.isDataInaccessible()) {
								rrInfo = extractHttpRequestResponseInfo(results, session, packetInfo, packetInfo.getDir(), previousPacket, limit);
							}
							if (rrInfo != null) {
								tempRRInfo = rrInfo;

								String host = rrInfo.getHostName();

								if (host != null) {
									session.setRemoteHostName(host);								}


								expectedUploadSeqNo = uploadSequenceNumber + tcpPacket.getPayloadLen();

							} else if (tempRRInfo != null) {
								int headerDelta = 0;
								boolean flag = false;
								if (!session.isDataInaccessible() && !tempRRInfo.isHeaderParseComplete()) {
									flag = true;
									headerDelta = setHeaderOffset(tempRRInfo, packetInfo, tcpPacket);
									tempRRInfo.writeHeader(packetInfo, headerDelta);
								}

								tempRRInfo.setLastDataPacket(packetInfo);
								tempRRInfo.setRawSize(tempRRInfo.getRawSize() + packetInfo.getLen() - headerDelta);
								if (tcpPacket.getSequenceNumber() == expectedUploadSeqNo) {
									expectedUploadSeqNo = tcpPacket.getSequenceNumber() + tcpPacket.getPayloadLen();
									tempRRInfo.writePayload(packetInfo, flag, headerDelta);
								} else if (tcpPacket.getSequenceNumber() < expectedUploadSeqNo) {
									tcpPacket.setRetransmission(true);
								} else {
									LOGGER.warn("Identified the following Request is corrupt. Session: " + session.getSessionKey() + ". Request Age: " + tempRRInfo.getAge());
									tempRRInfo.setCorrupt(true);
									tempRRInfo.writePayload(packetInfo, false, 0);
								}
							} else {
								if (session.isDecrypted()) {
									continue;
								} else if (session.isSsl()) {
									break;
								}
								session.setDataInaccessible(true);
								rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), packetInfo.getDir());
								expectedUploadSeqNo = uploadSequenceNumber + tcpPacket.getPayloadLen();
								populateRRInfo(rrInfo, packetInfo, false, true, HttpDirection.REQUEST);
								results.add(rrInfo);
								tempRRInfo = rrInfo;
							}
							rrInfo = null;
							tempRRInfo.addTCPPacket(uploadSequenceNumber, packetInfo);
						}
					}

					rrInfo = null;
					tempRRInfo = null;

					if (!((session.isSsl() && !session.isDecrypted()))) {

						long expectedDownloadSeqNo = 0;
						for (long downloadSequenceNumber : session.getDownlinkPacketsSortedBySequenceNumbers().keySet()) {
							// Identify correct packet from the whole transmission stream
							packetInfo = identifyCorrectTransmissionStream(session.getDownlinkPacketsSortedBySequenceNumbers().get(downloadSequenceNumber),
									session.getAckNumbers(), session, PacketDirection.DOWNLINK);
							tcpPacket = (TCPPacket) packetInfo.getPacket();

							if (packetInfo.getPayloadLen() > 0) {
								if (!session.isDataInaccessible()) {
									rrInfo = extractHttpRequestResponseInfo(results, session, packetInfo, packetInfo.getDir(), previousPacket, limit);
									limit = 0;

									if (rrInfo != null && !rrInfo.isHeaderParseComplete()) {
										previousPacket = packetInfo;
										continue;
									} else {
										previousPacket = null;
									}
								}

								if (rrInfo != null) {
									tempRRInfo = rrInfo;
									expectedDownloadSeqNo = downloadSequenceNumber + tcpPacket.getPayloadLen();
								} else if (tempRRInfo != null) {
									boolean flag = false;
									int headerDelta = 0;
									tempRRInfo.setLastDataPacket(packetInfo);
									if (tcpPacket.getSequenceNumber() == expectedDownloadSeqNo) {
										expectedDownloadSeqNo = tcpPacket.getSequenceNumber() + tcpPacket.getPayloadLen();
										if (tempRRInfo.getContentLength() == 0 || ((tempRRInfo.getPayloadData().size() + packetInfo.getPayloadLen()) <= tempRRInfo.getContentLength())) {
											tempRRInfo.writePayload(packetInfo, flag, headerDelta);
											tempRRInfo.setRawSize(tempRRInfo.getRawSize() + packetInfo.getLen() - headerDelta);
										} else if (tempRRInfo.getContentLength() > 0 && (tempRRInfo.getPayloadData().size() + packetInfo.getPayloadLen()) > tempRRInfo.getContentLength()) {
											limit = tempRRInfo.getContentLength() - tempRRInfo.getPayloadData().size();
											tempRRInfo.writePayload(packetInfo, limit);
											previousPacket = packetInfo;
											// TODO: Update RAW SIZE
										}

									} else if (tcpPacket.getSequenceNumber() < expectedDownloadSeqNo) {
										tcpPacket.setRetransmission(true);
									} else {
										LOGGER.warn("Identified the following Response is corrupt. Session: " + session.getSessionKey() + ". Request Age: " + tempRRInfo.getAge());
										tempRRInfo.setCorrupt(true);
										tempRRInfo.writePayload(packetInfo, false, 0);
									}
								} else {
									if (session.isDecrypted()) {
										continue;
									}
									rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), packetInfo.getDir());
									expectedDownloadSeqNo = downloadSequenceNumber + tcpPacket.getPayloadLen();
									populateRRInfo(rrInfo, packetInfo, false, true, HttpDirection.RESPONSE);
									results.add(rrInfo);
									tempRRInfo = rrInfo;
								}
								rrInfo = null;
								tempRRInfo.addTCPPacket(downloadSequenceNumber, packetInfo);
							}
						}
					}
				} catch (IOException e) {
					LOGGER.error("Error Storing data to TCP Request Response Obect. Session ID: " + session.getSessionKey());
				}

				if (session.isSsl() && !session.isDecrypted()) {
					results =  analyzeRequestResponsesForSecureSessions(session);
				}

			}

			Collections.sort(results);
			session.setRequestResponseInfo(results);
			populateDataForRequestResponses(session);

			session.setDomainName(session.getRemoteIP().getHostName());
		}
	}

	/**
	 * Check and return which packet in the initial packet list by sequence number received the first ACK in the communication
	 * @param packetInfoListForSequenceNumber Initial packet list by a sequence number
	 * @param ackNumbersSet Set of all the ACK numbers
	 * @param session Session object
	 * @param direction Packet direction Uplink or Downlink
	 * @return Returns a packet from the packetInfoListForSequenceNumber which has received an ACK in the whole TCP communication.
	 * 		   Returns the last packet in the list if no ACK is received.
	 */
	private PacketInfo identifyCorrectTransmissionStream(List<PacketInfo> packetInfoListForSequenceNumber,
			Set<Long> ackNumbersSet,
			Session session,
			PacketDirection direction) {
		if (packetInfoListForSequenceNumber.size() == 1) {
			return packetInfoListForSequenceNumber.get(0);
		}

		PacketInfo currentPacket = null;
		// Iterate through the parent packet list by sequence number
		for (PacketInfo packetInfo : packetInfoListForSequenceNumber) {
			currentPacket = packetInfo;

			TCPPacket tcpPacket = (TCPPacket) packetInfo.getPacket();
			long nextSequenceOrAckNumber = tcpPacket.getSequenceNumber() + tcpPacket.getPayloadLen();

			List<PacketInfo> packetInfoListForNextSequenceNumber = PacketDirection.DOWNLINK.equals(direction) ?
					session.getDownlinkPacketsSortedBySequenceNumbers().get(nextSequenceOrAckNumber):
						session.getUplinkPacketsSortedBySequenceNumbers().get(nextSequenceOrAckNumber);
			if (ackNumbersSet.contains(nextSequenceOrAckNumber) ||
					identifyCorrectTCPTransmissionStreamHelper(packetInfoListForNextSequenceNumber, ackNumbersSet, session, direction)) {
				return currentPacket;
			}			
		}

		return currentPacket;
	}

	/**
	 * Helper method to recursively iterate through all of the child packets by next sequence number (relative to parent packet) to identify if there is any ACK received
	 * @param packetInfoListForSequenceNumber Initial packet list by a sequence number
	 * @param ackNumbersSet Set of all the ACK numbers
	 * @param session Session object
	 * @param direction Packet direction Uplink or Downlink
	 * @return True if any packet in the chain has received an ACK in session, otherwise False
	 */
	private boolean identifyCorrectTCPTransmissionStreamHelper(List<PacketInfo> packetInfoListForSequenceNumber,
			Set<Long> ackNumbersSet,
			Session session,
			PacketDirection direction) {
		if (packetInfoListForSequenceNumber == null || packetInfoListForSequenceNumber.size() == 0) {
			return false;
		}

		for (PacketInfo packetInfo : packetInfoListForSequenceNumber) {
			TCPPacket tcpPacket = (TCPPacket) packetInfo.getPacket();
			long nextSequenceOrAckNumber = tcpPacket.getSequenceNumber() + tcpPacket.getPayloadLen();
			if (ackNumbersSet.contains(nextSequenceOrAckNumber)) {
				return true;
			}

			List<PacketInfo> packetInfoListForNextSequenceNumber = PacketDirection.DOWNLINK.equals(direction) ?
					session.getDownlinkPacketsSortedBySequenceNumbers().get(nextSequenceOrAckNumber):
						session.getUplinkPacketsSortedBySequenceNumbers().get(nextSequenceOrAckNumber);
			return identifyCorrectTCPTransmissionStreamHelper(packetInfoListForNextSequenceNumber, ackNumbersSet, session, direction);
		}

		return false;
	}

	private void populateRRInfo(HttpRequestResponseInfo rrInfo, PacketInfo packetInfo, boolean isExtractable, boolean isTCP, HttpDirection httpDirection) {
		rrInfo.setExtractable(isExtractable);
		rrInfo.setFirstDataPacket(packetInfo);
		rrInfo.setLastDataPacket(packetInfo);
		rrInfo.setTCP(isTCP);
		if (httpDirection != null) {
			rrInfo.setDirection(httpDirection);
		}
	}

	/**
	 * Updates the Content Length and Last Data Packet for Dummy RequestResponseObjects.
	 * @param rrInfo
	 * @param packetInfo
	 */
	private void updateRequestResponseObject(HttpRequestResponseInfo rrInfo, PacketInfo packetInfo) {
		if (rrInfo != null) {
			rrInfo.setContentLength(rrInfo.getContentLength() + packetInfo.getPayloadLen());
			rrInfo.setRawSize(rrInfo.getRawSize() + packetInfo.getLen());
			rrInfo.setLastDataPacket(packetInfo);
		}
	}

	/**
	 * Estimate RequestResponseObjects for Secure Sessions
	 * @param session
	 * @return
	 */
	private ArrayList<HttpRequestResponseInfo> analyzeRequestResponsesForQUICUDPSession(Session session) {
		ArrayList<HttpRequestResponseInfo> results = new ArrayList<>();
		boolean flag = false;
		UDPPacket udpPacket = null;
		HttpRequestResponseInfo rrInfo = null;
		HttpRequestResponseInfo downlinkRRInfo = null;
		for (PacketInfo packetInfo : session.getAllPackets()) {
			udpPacket = (UDPPacket) packetInfo.getPacket();
			if (packetInfo.getDir() == PacketDirection.UPLINK) {
				if (udpPacket.getPacketLength() >= AVG_QUIC_UDP_PACKET_SIZE && !flag) {
					rrInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), packetInfo.getDir(), packetInfo, false);
					results.add(rrInfo);
					flag = true;
				}
				updateRequestResponseObject(rrInfo, packetInfo);
			}
			if (packetInfo.getDir() == PacketDirection.DOWNLINK) {
				if (flag) {
					downlinkRRInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), packetInfo.getDir(), packetInfo, false);
					results.add(downlinkRRInfo);
					flag = false;
				}
				updateRequestResponseObject(downlinkRRInfo, packetInfo);
			}
		}

		if (results.isEmpty() && !session.getUplinkPacketsSortedBySequenceNumbers().isEmpty() && !session.getDownlinkPacketsSortedBySequenceNumbers().isEmpty()) {
			PacketInfo packetInfo = identifyCorrectTransmissionStream(session.getUplinkPacketsSortedBySequenceNumbers().firstEntry().getValue(),
					session.getAckNumbers(), session, PacketDirection.UPLINK);
			rrInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), PacketDirection.UPLINK, packetInfo, false);
			packetInfo = identifyCorrectTransmissionStream(session.getUplinkPacketsSortedBySequenceNumbers().lastEntry().getValue(),
					session.getAckNumbers(), session, PacketDirection.UPLINK);
			rrInfo.setLastDataPacket(packetInfo);
			rrInfo.setExtractable(false);
			results.add(rrInfo);

			packetInfo = identifyCorrectTransmissionStream(session.getDownlinkPacketsSortedBySequenceNumbers().firstEntry().getValue(),
					session.getAckNumbers(), session, PacketDirection.DOWNLINK);
			downlinkRRInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), PacketDirection.DOWNLINK, packetInfo, false);
			packetInfo = identifyCorrectTransmissionStream(session.getDownlinkPacketsSortedBySequenceNumbers().lastEntry().getValue(),
					session.getAckNumbers(), session, PacketDirection.DOWNLINK);
			downlinkRRInfo.setLastDataPacket(packetInfo);
			downlinkRRInfo.setExtractable(false);
			results.add(downlinkRRInfo);
		}

		return results;
	}

	/**
	 * Estimate RequestResponseObjects for Secure Sessions
	 * @param session
	 * @return
	 */
	private ArrayList<HttpRequestResponseInfo> analyzeRequestResponsesForSecureSessions(Session session) {

		session.setDataInaccessible(true);

		boolean flag = false;

		TCPPacket tcpPacket = null;
		HttpRequestResponseInfo rrInfo = null;
		HttpRequestResponseInfo downlinkRRInfo = null;
		ArrayList<HttpRequestResponseInfo> results = new ArrayList<>();

		for (PacketInfo packetInfo : session.getAllPackets()) {
			tcpPacket = (TCPPacket) packetInfo.getPacket();
			byte[] data = tcpPacket.getData();
			int packetPosition = tcpPacket.getDataOffset();

			if (packetInfo.getDir() == PacketDirection.UPLINK) {
				if ((packetPosition + 4) < tcpPacket.getLen() && data[packetPosition] == TLS_APPLICATION_DATA) {
					rrInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), packetInfo.getDir(), packetInfo, true);
					results.add(rrInfo);
					flag = true;	
				}
				updateRequestResponseObject(rrInfo, packetInfo);
			}

			if (packetInfo.getDir() == PacketDirection.DOWNLINK) {
				if (flag && (packetPosition + 4) < tcpPacket.getLen() && data[packetPosition] == TLS_APPLICATION_DATA) {
					downlinkRRInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), packetInfo.getDir(), packetInfo, true);
					results.add(downlinkRRInfo);
					flag = false;
				}
				updateRequestResponseObject(downlinkRRInfo, packetInfo);
			}
		}

		if (results.isEmpty()) {
			if (!session.getUplinkPacketsSortedBySequenceNumbers().isEmpty()) {
				PacketInfo packetInfo = identifyCorrectTransmissionStream(session.getUplinkPacketsSortedBySequenceNumbers().firstEntry().getValue(),
						session.getAckNumbers(), session, PacketDirection.UPLINK);
				rrInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), PacketDirection.UPLINK, packetInfo, true);
				packetInfo = identifyCorrectTransmissionStream(session.getUplinkPacketsSortedBySequenceNumbers().lastEntry().getValue(),
						session.getAckNumbers(), session, PacketDirection.UPLINK);
				rrInfo.setLastDataPacket(packetInfo);
				results.add(rrInfo);
			}
			if (!session.getDownlinkPacketsSortedBySequenceNumbers().isEmpty()) {
				PacketInfo packetInfo = identifyCorrectTransmissionStream(session.getDownlinkPacketsSortedBySequenceNumbers().firstEntry().getValue(),
						session.getAckNumbers(), session, PacketDirection.DOWNLINK);
				downlinkRRInfo = generateRequestResponseObjectsForSSLOrUDPSessions(session.getRemoteHostName(), PacketDirection.DOWNLINK, packetInfo, true);
				packetInfo = identifyCorrectTransmissionStream(session.getDownlinkPacketsSortedBySequenceNumbers().lastEntry().getValue(),
						session.getAckNumbers(), session, PacketDirection.DOWNLINK);
				downlinkRRInfo.setLastDataPacket(packetInfo);
				results.add(downlinkRRInfo);
			}
		}

		return results;
	}

	/**
	 * Generates a Dummy RequestResponseObjects for Secure Sessions
	 * @param remoteHostName
	 * @param packetDirection
	 * @param packetInfo
	 * @return
	 */
	private HttpRequestResponseInfo generateRequestResponseObjectsForSSLOrUDPSessions (String remoteHostName, PacketDirection packetDirection, PacketInfo packetInfo, boolean isTCP) {
		HttpRequestResponseInfo rrInfo = new HttpRequestResponseInfo(remoteHostName, packetDirection);
		populateRRInfo(rrInfo, packetInfo, false, isTCP, (packetDirection == PacketDirection.UPLINK ? HttpDirection.REQUEST : HttpDirection.RESPONSE));
		return rrInfo;
	}

	private ArrayList<HttpRequestResponseInfo> analyzeRequestResponsesForIOSSecureSessions(Session session) {

		ArrayList<HttpRequestResponseInfo> results = new ArrayList<>();

		String sessionKey = session.getDomainName() + "_" + session.getLocalPort();
		String uplinkFilePath = this.tracePath + sessionKey + "_UL";
		String downlinkFilePath = this.tracePath + sessionKey + "_DL";

		try {
			results = readFileAndPopulateRequestResponse(session, results, uplinkFilePath, PacketDirection.UPLINK, HttpDirection.REQUEST);
			results = readFileAndPopulateRequestResponse(session, results, downlinkFilePath, PacketDirection.DOWNLINK, HttpDirection.RESPONSE);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		return results;
	}

	/**
	 * Generates the length of an SSL record.
	 * @param mostSignificantByte
	 * @param leastSignificantByte
	 * @return
	 */
	public int recordLengthConverter(byte mostSignificantByte, byte leastSignificantByte) {
		int length = ((mostSignificantByte & 0xff) << 8) + (leastSignificantByte & 0xff);
		length += 5;
		return length;
	}

	private ArrayList<HttpRequestResponseInfo> readFileAndPopulateRequestResponse(Session session, ArrayList<HttpRequestResponseInfo> results, String filePath, PacketDirection packetDirection, HttpDirection httpDirection)
			throws IOException {
		String dataRead;
		long timeStamp = 0;
		HttpRequestResponseInfo rrInfo = null;
		int requestCount = 0;
		TreeMap<Double, PacketInfo> packetMap;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
		Map <Double, PacketInfo> usedPackets = new HashMap<>();

		if (packetDirection == PacketDirection.UPLINK) {
			packetMap = new TreeMap<>(session.getAllPackets().stream().filter(packetInfo -> packetInfo.getDir().equals(PacketDirection.UPLINK)).collect(Collectors.toMap(PacketInfo::getTimeStamp, Function.identity(), (existing, replacement) -> existing)));
		} else {
			packetMap = new TreeMap<>(session.getAllPackets().stream().filter(packetInfo -> packetInfo.getDir().equals(PacketDirection.DOWNLINK)).collect(Collectors.toMap(PacketInfo::getTimeStamp, Function.identity(), (existing, replacement) -> existing)));
		}

		try {
			while ((dataRead = bufferedReader.readLine()) != null) {
				if (dataRead.length() > 0) {
					String comparisonString = "RequestTime: ";
					if (dataRead.startsWith(comparisonString)) {
						++requestCount;
						timeStamp = Long.parseLong(dataRead.substring(comparisonString.length(), dataRead.length()));
						continue;
					}

					rrInfo = initializeRequestResponseObject(dataRead, session, packetDirection);
					if (rrInfo != null) {
						rrInfo.setTCP(true);
						rrInfo.setRawSize(-1);

						// Converting the System Time in Millis to Seconds and Microsecond format.
						double time = ((double) timeStamp/1000) + (((double) timeStamp%1000) / 1000000.0);
						// The math below allows the request time to have a start time relative to trace capture.
						rrInfo.setTime(time - pcapTimeOffset);

						// TODO: Will Review this after ARO22945-1645
						if (packetMap.containsKey(rrInfo.getTime()) && !usedPackets.containsKey(rrInfo.getTime())) {
							rrInfo.setFirstDataPacket(packetMap.get(rrInfo.getTime()));
							usedPackets.put(rrInfo.getTime(), packetMap.get(rrInfo.getTime()));
						} else {
							Map.Entry<Double, PacketInfo> lowKey = packetMap.floorEntry(rrInfo.getTime());
							Map.Entry<Double, PacketInfo> highKey = packetMap.ceilingEntry(rrInfo.getTime());

							if (lowKey != null) {
								setFirstAndLastDataPacket(results, usedPackets, rrInfo, packetMap, lowKey);
							} else if (highKey != null) {
								setFirstAndLastDataPacket(results, usedPackets, rrInfo, packetMap, highKey);
							}
						}

						rrInfo.writeHeader(dataRead);
						while ((dataRead = bufferedReader.readLine()) != null && dataRead.length() != 0) {
							rrInfo.writeHeader(System.lineSeparator());
							rrInfo.writeHeader(dataRead);
							parseHeaderLine.parseHeaderLine(dataRead, rrInfo);
						}
						rrInfo.writeHeader(System.lineSeparator());

						// Check for payload and read
						File file = new File(filePath + "_" + requestCount);
						if (file.exists()) {
							BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
							rrInfo.writePayload(bis);
							bis.close();
						}

						results.add(rrInfo);
					}
				}
			}
		} finally {
			bufferedReader.close();
		}

		return results;
	}

	/**
	 * Used to set the first and last data packet for secure sessions in iOS.
	 * The data isn't available from the request and response file, so this method helps with it
	 * Identified the packet with the time stamp closest to the request time in iOS secure file.
	 * 
	 * @param results
	 * @param usedPackets
	 * @param rrInfo
	 * @param packetMap
	 * @param matchKey
	 * 
	 */
	private void setFirstAndLastDataPacket(ArrayList<HttpRequestResponseInfo> results,
			Map<Double, PacketInfo> usedPackets, HttpRequestResponseInfo rrInfo,
			TreeMap<Double, PacketInfo> packetMap, Map.Entry<Double, PacketInfo> matchKey) {
		/**
		 * Identify the packet closest to the request time stamp in the iOS file
		 * Assign that as the first and last data packet
		 */
		if (matchKey != null) {
			do {
				if (!usedPackets.containsKey(matchKey.getKey()) && ((TCPPacket) matchKey.getValue().getPacket()).getLen() > 0) {
					rrInfo.setFirstDataPacket(matchKey.getValue());
					rrInfo.setLastDataPacket(matchKey.getValue());
					usedPackets.put(matchKey.getKey(), matchKey.getValue());
					break;
				}

			} while ((matchKey = packetMap.higherEntry(matchKey.getKey())) != null);
		}
		/**
		 * If a request has a first data packet, then
		 * the the previous packet with data, if it hasn't been used before, 
		 * is the last data packet for the previous request.
		 */
		if (results.size() > 0) {
			HttpRequestResponseInfo previousRequest = results.get(results.size()-1);
			while (matchKey != null && ((matchKey = packetMap.lowerEntry(matchKey.getKey())) != null) && ((TCPPacket) matchKey.getValue().getPacket()).getLen() > 0) {
				if (matchKey.getValue().getTimeStamp() < previousRequest.getTimeStamp()) {
					break;
				} else if (!usedPackets.containsKey(matchKey.getKey())) {
					previousRequest.setLastDataPacket(matchKey.getValue());
					break;
				}
			}
		}
	}

	private HttpRequestResponseInfo initializeRequestResponseObject(String line, Session session, PacketDirection packetDirection) {
		HttpRequestResponseInfo rrInfo = null;

		if (PacketDirection.UPLINK.equals(packetDirection)) {
			Matcher matcher = HttpPattern.strRequestType.matcher(line);
			if (matcher.lookingAt()) {
				rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), packetDirection);
				rrInfo.setStatusLine(line);
				rrInfo.setRequestType(matcher.group(1));
				rrInfo.setDirection(HttpDirection.REQUEST);
				rrInfo.setObjName(matcher.group(2));
				rrInfo.setVersion(matcher.group(3));
				rrInfo.setPort(session.getRemotePort());

				String scheme = rrInfo.getVersion().split("/")[0];
				rrInfo.setScheme(scheme);

				try {
					if (rrInfo.getObjName().startsWith("/") || rrInfo.getObjName().startsWith(scheme.toLowerCase())) {
						rrInfo.setObjUri(new URI(rrInfo.getObjName()));
					} else {
						rrInfo.setObjUri(new URI(scheme.toLowerCase() + "://" + rrInfo.getObjName()));
					}
					rrInfo.setHostName(rrInfo.getObjUri().getHost());
				} catch (URISyntaxException e) {
					LOGGER.error(String.format("Problem creating a URI for line: %s", line), e);
				}
			}
		} else {
			Matcher matcher = HttpPattern.strReResponseResults.matcher(line);
			if (matcher.lookingAt()) {
				rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), packetDirection);
				rrInfo.setStatusLine(line);
				rrInfo.setDirection(HttpDirection.RESPONSE);
				rrInfo.setVersion(matcher.group(1));
				rrInfo.setScheme(rrInfo.getVersion().split("/")[0]);
				rrInfo.setStatusCode(Integer.parseInt(matcher.group(2)));
				rrInfo.setResponseResult(matcher.group(3));
				rrInfo.setPort(session.getLocalPort());
			}
		}

		return rrInfo;
	}

	private int setHeaderOffset(HttpRequestResponseInfo rrInfo, PacketInfo packetInfo, TCPPacket tcpPacket) {
		String line;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		BufferedOutputStream bufferedStream = new BufferedOutputStream(stream);

		try {
			bufferedStream.write(tcpPacket.getData(), tcpPacket.getDataOffset(), tcpPacket.getData().length - tcpPacket.getDataOffset());
			bufferedStream.flush();
			storageReader.init(stream.toByteArray());

			while ((line = storageReader.readLine()) != null && line.length() != 0) {
				parseHeaderLine.parseHeaderLine(line, rrInfo);
			}

			if (line != null && line.length() == 0) {
				rrInfo.setHeaderParseComplete(true);
			}

		} catch (Exception exception) {
			LOGGER.error("Error Reading Data from TCP Packet: " + exception.getMessage());
			return 0;
		}

		return storageReader.getIndex();
	}

	private void populateDataForRequestResponses(Session session) {

		List<HttpRequestResponseInfo> requests = new ArrayList<HttpRequestResponseInfo>(session.getRequestResponseInfo().size());
		int requestCount = 0;
		int responseCount = 0;

		for (HttpRequestResponseInfo rrInfo : session.getRequestResponseInfo()) {
			rrInfo.setSession(session);
			if (rrInfo.getDirection() == HttpDirection.REQUEST) {
				++requestCount;
				requests.add(rrInfo);
				if (session.getDomainName() == null) {
					String host = rrInfo.getHostName();
					if (host != null) {
						session.setRemoteHostName(host);
					}
				}
			} else if (rrInfo.getDirection() == HttpDirection.RESPONSE) {
				++responseCount;
				if (rrInfo.getContentLength() > 0) {
					session.setFileDownloadCount(session.getFileDownloadCount() + 1);
				}
				if (!requests.isEmpty()) {
					rrInfo.setAssocReqResp(requests.remove(0));
					rrInfo.getAssocReqResp().setAssocReqResp(rrInfo);
				}
			}

			// Build an absolute URI if possible
			if (rrInfo.getObjUri() != null && !rrInfo.getObjUri().isAbsolute()) {
				try {
					int port = Integer.valueOf(rrInfo.getPort()).equals(wellKnownPorts.get(rrInfo.getScheme())) ? -1 : rrInfo.getPort();
					rrInfo.setObjUri(new URI(rrInfo.getScheme().toLowerCase(), null, rrInfo.getHostName(), port, rrInfo.getObjUri().getPath(), rrInfo.getObjUri().getQuery(), rrInfo.getObjUri().getFragment()));
				} catch (URISyntaxException e) {
					LOGGER.info("Unexpected exception creating URI for request: " + e.getMessage() + ". Scheme=" + rrInfo.getScheme().toLowerCase() + ",Host name=" + rrInfo.getHostName() + ",Path=" + rrInfo.getObjUri().getPath() + ",Fragment="
							+ rrInfo.getObjUri().getFragment());

				}
			}
		}

		if (requestCount != responseCount) {
			LOGGER.trace("Session: " + session.getSessionKey() + ", Request Count: " + requestCount + ", Response Count: " + responseCount + " Don't match!");
		}

		if (!session.isUdpOnly()) {
			populateWaterfallContent(session);
		}	
	}

	private void populateWaterfallContent(Session session) {

		Double sslNegotiationDuration = null;
		double contentDownloadDuration = 0;
		double requestDuration = 0;
		double timeToFirstByte = 0;
		Double dnsTime = null;
		Double synTime = session.getSynTime();

		if (session.getDnsRequestPacket() != null && session.getDnsResponsePacket() != null) {
			dnsTime = session.getDnsRequestPacket().getTimeStamp();
		}

		Double sslNegTime = null;
		PacketInfo handshake = session.getLastSslHandshakePacket();
		if (handshake != null) {
			sslNegTime = handshake.getTimeStamp();
		}

		for (HttpRequestResponseInfo rrinfo : session.getRequestResponseInfo()) {
			if (rrinfo.getDirection() != HttpDirection.REQUEST || rrinfo.getAssocReqResp() == null || rrinfo.getFirstDataPacket() == null) {
				// Only process non-HTTPS request/response pairs
				continue;
			}

			double startTime = -1;
			double firstReqPacket = rrinfo.getFirstDataPacket().getTimeStamp();
			PacketInfo lastPkt = rrinfo.getLastDataPacket();
			double lastReqPacket = lastPkt != null ? lastPkt.getTimeStamp() : -1;

			HttpRequestResponseInfo resp = rrinfo.getAssocReqResp();

			// check getAssocReqResp firstDataPacket and lastDataPacket packet
			if (resp == null || resp.getFirstDataPacket() == null || resp.getLastDataPacket() == null) {
				continue;
			}
			double firstRespPacket = resp.getFirstDataPacket().getTimeStamp();
			double lastRespPacket = resp.getLastDataPacket().getTimeStamp();

			// Add DNS and initial connect to fist req/resp pair only
			Double dnsDuration = null;
			if (dnsTime != null) {
				startTime = dnsTime.doubleValue();
				if (synTime != null) {
					dnsDuration = synTime.doubleValue() - dnsTime.doubleValue();
				} else {
					dnsDuration = firstReqPacket - dnsTime.doubleValue();
				}

				// Prevent from being added again
				dnsTime = null;
			}

			Double initConnDuration = null;
			if (synTime != null) {
				initConnDuration = firstReqPacket - synTime;
				if (startTime < 0.0) {
					startTime = synTime.doubleValue();
				}

				// Prevent from being added again
				synTime = null;
			}

			// Calculate request time
			if (startTime < 0.0) {
				startTime = firstReqPacket;
			}

			// Store waterfall in request/response
			if (sslNegTime != null && lastRespPacket >= sslNegTime) {
				sslNegotiationDuration = sslNegTime - firstReqPacket;
				contentDownloadDuration = lastRespPacket - sslNegTime;
			} else {
				if (firstRespPacket >= lastReqPacket && lastReqPacket != -1) {
					contentDownloadDuration = lastRespPacket - firstRespPacket;
					requestDuration = lastReqPacket - firstReqPacket;
					timeToFirstByte = firstRespPacket - lastReqPacket;
				} else {
					contentDownloadDuration = lastRespPacket - firstReqPacket;
				}

			}
			RequestResponseTimeline reqRespTimeline = new RequestResponseTimeline(startTime, dnsDuration, initConnDuration, sslNegotiationDuration, requestDuration, timeToFirstByte, contentDownloadDuration);
			rrinfo.setWaterfallInfos(reqRespTimeline);
			rrinfo.getWaterfallInfos().setLastRespPacketTime(lastRespPacket);
		}
	}

	private HttpRequestResponseInfo extractHttpRequestResponseInfo(ArrayList<HttpRequestResponseInfo> results, Session session, PacketInfo packetInfo, PacketDirection packetDirection, PacketInfo previousPacketInfo, int addToOffset) {

		TCPPacket tcpPacket = null;
		UDPPacket udpPacket = null;
		HttpRequestResponseInfo rrInfo = null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		BufferedOutputStream bufferedStream = new BufferedOutputStream(stream);
		int carryoverPayloadLength = 0;

		if (packetInfo.getPacket() instanceof TCPPacket) {
			tcpPacket = (TCPPacket) packetInfo.getPacket();

			try {
				if (previousPacketInfo != null) {
					TCPPacket previousTCPPacket = (TCPPacket) previousPacketInfo.getPacket();
					carryoverPayloadLength = previousTCPPacket.getData().length - (previousTCPPacket.getDataOffset() + addToOffset);
					bufferedStream.write(previousTCPPacket.getData(), (previousTCPPacket.getDataOffset() + addToOffset), carryoverPayloadLength);
				}

				bufferedStream.write(tcpPacket.getData(), tcpPacket.getDataOffset(), tcpPacket.getData().length - tcpPacket.getDataOffset());
			} catch (Exception exception) {
				LOGGER.error("Error Reading Data from TCP Packet: ", exception);
			}
		} else {
			udpPacket = (UDPPacket) packetInfo.getPacket();

			try {
				if (previousPacketInfo != null) {
					UDPPacket previousUDPPacket = (UDPPacket) previousPacketInfo.getPacket();
					carryoverPayloadLength = previousUDPPacket.getData().length - (previousUDPPacket.getDataOffset() + addToOffset);
					bufferedStream.write(previousUDPPacket.getData(), (previousUDPPacket.getDataOffset() + addToOffset), carryoverPayloadLength);
				}

				bufferedStream.write(udpPacket.getData(), udpPacket.getDataOffset(), udpPacket.getData().length - udpPacket.getDataOffset());
			} catch (Exception exception) {
				LOGGER.error("Error Reading Data from UDP Packet: ", exception);
			}
		}

		try {
			bufferedStream.flush();
			byte[] streamArray = stream.toByteArray();
			storageReader.init(streamArray);
			return getLastRRInfo(streamArray, packetInfo, tcpPacket, previousPacketInfo, carryoverPayloadLength, session, results, packetDirection);
		} catch (IOException e) {
			LOGGER.error(e);
		}

		return rrInfo;
	}

	private HttpRequestResponseInfo getLastRRInfo(byte[] streamArray, PacketInfo packetInfo, TCPPacket tcpPacket, PacketInfo previousPacket,
			int carryoverPayloadLength, Session session, List<HttpRequestResponseInfo> results, PacketDirection packetDirection) {

		HttpRequestResponseInfo rrInfo = null;
		String line = null;
		int readerIndex = -1;
		boolean carryoverPayloadForNewRRInfo = false;
		int remainingLength = 0;
		try {
			do {
				while (true) {
					readerIndex = storageReader.getIndex();
					line = storageReader.readLine();

					if (line == null) {
						return rrInfo;
					}

					if (line.length() != 0) {
						break;
					}
				}

				rrInfo = initializeRequestResponseObject(line, session, packetDirection);
				if (rrInfo == null || tcpPacket == null) {
					break;
				}

				carryoverPayloadForNewRRInfo = false;
				rrInfo.setTCP(true);
				rrInfo = populateRRInfo(session, tcpPacket, rrInfo);

				boolean isExtractable, isTCP;
				isExtractable = isTCP = session.isUdpOnly() ? false : true;
				HttpDirection direction = PacketDirection.DOWNLINK.equals(packetDirection) ? HttpDirection.RESPONSE : HttpDirection.REQUEST;
				// Set first and last packet info data to rrInfo
				if (previousPacket == null || readerIndex >= carryoverPayloadLength) {
					populateRRInfo(rrInfo, packetInfo, isExtractable, isTCP, direction);
				} else {
					populateRRInfo(rrInfo, previousPacket, isExtractable, isTCP, direction);
				}

				if (!rrInfo.isHeaderParseComplete()) {
					return rrInfo;
				} else {
					results.add(rrInfo);

					rrInfo.getHeaderData().write(streamArray, 0, storageReader.getIndex());
					remainingLength = streamArray.length - storageReader.getIndex();
					if (remainingLength <= 0) {
						return rrInfo;
					}

					if (rrInfo.getContentLength() > 0) {
						if (rrInfo.getContentLength() <= remainingLength) {
							rrInfo.getPayloadData().write(streamArray, storageReader.getIndex(), rrInfo.getContentLength());

							storageReader.setArrayIndex(storageReader.getIndex() + rrInfo.getContentLength());
							remainingLength = streamArray.length - storageReader.getIndex();
							if (remainingLength <= 0) {
								return rrInfo;
							}

							// We have found the whole payload data for the current rrInfo. There's still remaining data which belongs to a new rrInfo object.
							// Continue processing for the next rrInfo object.
							carryoverPayloadForNewRRInfo = true;
						} else {
							rrInfo.getPayloadData().write(streamArray, storageReader.getIndex(), remainingLength);
							return rrInfo;
						}
					} else {
						// We are not sure if content length is explicitly 0 in the response or if it is a set by default during the initialization of object. 
						// Try to create a new rrInfo object for next line.
						carryoverPayloadForNewRRInfo = true;
					}
				}
			} while (true);
		} catch (Exception e) {
			LOGGER.error("Error extracting request response object for packet id " + packetInfo.getPacketId() + " and session port " + session.getLocalPort(), e);
		}

		// We have a carry over payload data belonging to previous rrInfo object
		if (tcpPacket != null && carryoverPayloadForNewRRInfo && remainingLength > 0) {
			rrInfo = results.get(results.size() - 1);
			rrInfo.getPayloadData().write(streamArray, readerIndex, remainingLength);
		}

		return rrInfo;
	}

	private HttpRequestResponseInfo populateRRInfo(Session session, TCPPacket tcpPacket, HttpRequestResponseInfo rrInfo) throws IOException {

		String line;
		rrInfo.setSsl(session.isSsl());
		rrInfo.setRawSize(tcpPacket.getLen());
		while ((line = storageReader.readLine()) != null && line.length() != 0) {
			parseHeaderLine.parseHeaderLine(line, rrInfo);
		}
		if (line != null && line.length() == 0) {
			rrInfo.setHeaderParseComplete(true);
		}
		return rrInfo;
	}

	/**
	 * Analyze the packet to find the TCPInfo. Marked flags: TCP_ACK,
	 * TCP_ACK_DUP, TCP_WINDOW_UPDATE, TCP_KEEP_ALIVE_ACK
	 */
	private void analyzeACK(Session sess) {

		Map<Long, Integer> ulAckWinSize = new HashMap<Long, Integer>();
		Map<Long, Integer> dlAckWinSize = new HashMap<Long, Integer>();

		Set<Long> ulAliveAck = new HashSet<Long>();
		Set<Long> dlAliveAck = new HashSet<Long>();

		for (PacketInfo pinfo : sess.getTcpPackets()) {
			TCPPacket pack = (TCPPacket) pinfo.getPacket();

			if (!pack.isACK()) {
				continue;
			}

			long ackNum = pack.getAckNumber();
			int win = pack.getWindow();

			Map<Long, Integer> pAckWinSize;
			Set<Long> pAliveAck;
			Set<Long> pAliveAck2;

			switch (pinfo.getDir()) {
			case UPLINK:
				pAckWinSize = ulAckWinSize;
				pAliveAck = ulAliveAck;
				pAliveAck2 = dlAliveAck;
				break;

			case DOWNLINK:
				pAckWinSize = dlAckWinSize;
				pAliveAck = dlAliveAck;
				pAliveAck2 = ulAliveAck;
				break;

			default:
				LOGGER.warn("97 - No direction for packet. Packet ID: " + pinfo.getPacketId());
				continue;
			}

			if (pinfo.getTcpInfo() == TcpInfo.TCP_KEEP_ALIVE) {
				pAliveAck.add(pack.getSequenceNumber());
				continue;
			}

			int tcpFlag;
			if (pack.isFIN()) {
				tcpFlag = 1;
			} else if (pack.isSYN()) {
				tcpFlag = 2;
			} else if (pack.isRST()) {
				tcpFlag = 4;
			} else {
				tcpFlag = 0;
			}
			long key = ((ackNum << 32) | tcpFlag);

			int payloadLen = pack.getPayloadLen();
			if (pAliveAck2.contains(ackNum - 1) && payloadLen == 0 && !pack.isSYN() && !pack.isFIN() && !pack.isRST()) {
				pinfo.setTcpInfo(TcpInfo.TCP_KEEP_ALIVE);
			} else if (!pAckWinSize.containsKey(key)) {
				pAckWinSize.put(key, win);
				if (payloadLen == 0 && !pack.isSYN() && !pack.isFIN() && !pack.isRST()) {
					pinfo.setTcpInfo(TcpInfo.TCP_ACK);
				}
			} else {
				int prevWin = pAckWinSize.get(key);
				if (win == prevWin) {
					if (payloadLen == 0 && !pack.isRST() && pinfo.getTcpInfo() != TcpInfo.TCP_KEEP_ALIVE) {

						pinfo.setTcpInfo(TcpInfo.TCP_ACK_DUP);
					}
				} else {
					pAckWinSize.put(key, win);
					if (payloadLen == 0 && !pack.isRST() && pinfo.getTcpInfo() != TcpInfo.TCP_KEEP_ALIVE) {
						pinfo.setTcpInfo(TcpInfo.TCP_WINDOW_UPDATE);
					}
				}
			}
		}
	}

	/**
	 * Analyze the packet to find the TCPInfo. Marked flags: TCP_ZERO_WINDOW
	 */
	private void analyzeZeroWindow(Session sess) {

		for (PacketInfo pInfo : sess.getTcpPackets()) {
			TCPPacket tPacket = (TCPPacket) pInfo.getPacket();
			if (tPacket.getPayloadLen() == 0 && tPacket.getWindow() == 0 && !tPacket.isSYN() && !tPacket.isFIN() && !tPacket.isRST()) {
				pInfo.setTcpInfo(TcpInfo.TCP_ZERO_WINDOW);
			}
		}
	}

	/**
	 * Analyze the packet to find the TCPInfo. Marked flags: TCP_DATA_RECOVER,
	 * TCP_ACK_RECOVER
	 */
	private void analyzeRecoverPkts(Session sess) {

		// "Recover data": its seq equals to the duplicated ACK
		// "Recover ack": its ack equals to the duplicated DATA + payload len

		Map<Long, TCPPacket> dupAckUl = new HashMap<Long, TCPPacket>();
		Map<Long, TCPPacket> dupAckDl = new HashMap<Long, TCPPacket>();
		Map<Long, TCPPacket> dupSeqUl = new HashMap<Long, TCPPacket>();
		Map<Long, TCPPacket> dupSeqDl = new HashMap<Long, TCPPacket>();

		for (PacketInfo pInfo : sess.getTcpPackets()) {
			TCPPacket tPacket = (TCPPacket) pInfo.getPacket();

			TcpInfo pType = pInfo.getTcpInfo();
			PacketDirection dir = pInfo.getDir();
			if (pType == TcpInfo.TCP_DATA_DUP) {
				if (dir == PacketDirection.UPLINK) {
					dupSeqUl.put(tPacket.getSequenceNumber() + tPacket.getPayloadLen(), tPacket);
				} else {
					dupSeqDl.put(tPacket.getSequenceNumber() + tPacket.getPayloadLen(), tPacket);
				}
			}

			// Duplicated data means duplicated ack as well
			if (pType == TcpInfo.TCP_ACK_DUP || pType == TcpInfo.TCP_DATA_DUP) {
				if (dir == PacketDirection.UPLINK) {
					dupAckUl.put(tPacket.getAckNumber(), tPacket);
				} else {
					dupAckDl.put(tPacket.getAckNumber(), tPacket);
				}
			}

			if (pType == TcpInfo.TCP_DATA) {
				if (dir == PacketDirection.UPLINK && dupAckDl.containsKey(tPacket.getSequenceNumber())) {
					pInfo.setTcpInfo(TcpInfo.TCP_DATA_RECOVER);
				}
				if (dir == PacketDirection.DOWNLINK && dupAckUl.containsKey(tPacket.getSequenceNumber())) {
					pInfo.setTcpInfo(TcpInfo.TCP_DATA_RECOVER);
				}
			}

			if (pType == TcpInfo.TCP_ACK) {
				if (dir == PacketDirection.UPLINK && dupSeqDl.containsKey(tPacket.getAckNumber())) {
					pInfo.setTcpInfo(TcpInfo.TCP_DATA_RECOVER);
				}
				if (dir == PacketDirection.DOWNLINK && dupSeqUl.containsKey(tPacket.getAckNumber())) {
					pInfo.setTcpInfo(TcpInfo.TCP_DATA_RECOVER);
				}
			}

			// A special case:
			// DL: TCP_ACK_DUP with ack = 1
			// DL: TCP_ACK_DUP with ack = 1
			// UL: TCP_ACK with seq = 1
			// UL: TCP_DATA with seq = 1 <==== This is NOT a DATA_RECOVER
			if (pType == TcpInfo.TCP_ACK || pType == TcpInfo.TCP_ACK_DUP || pType == TcpInfo.TCP_ACK_RECOVER) {
				if (dir == PacketDirection.UPLINK) {
					dupAckDl.remove(tPacket.getSequenceNumber());
				}
				if (dir == PacketDirection.DOWNLINK) {
					dupAckUl.remove(tPacket.getSequenceNumber());
				}
			}

			// DL: TCP_DATA_DUP with seq = 1, len = 2
			// DL: TCP_DATA_DUP with seq = 1, len = 2
			// UL: TCP_DATA with ack = 3
			// UL: TCP_ACK with ack = 3 <==== This is NOT an ACK_RECOVER

			// Duplicated data means duplicated ack as well
			// But vise versa is not true
			if (pType == TcpInfo.TCP_DATA || pType == TcpInfo.TCP_DATA_RECOVER) {
				if (dir == PacketDirection.UPLINK) {
					dupAckUl.remove(tPacket.getAckNumber());
				}
				if (dir == PacketDirection.DOWNLINK) {
					dupAckDl.remove(tPacket.getAckNumber());
				}
			}
		}
	}

}
