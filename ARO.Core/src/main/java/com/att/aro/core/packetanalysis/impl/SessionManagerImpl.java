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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.functions.Count;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.IByteArrayLineReader;
import com.att.aro.core.packetanalysis.IParseHeaderLine;
import com.att.aro.core.packetanalysis.IRequestResponseBuilder;
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

// FIXME LOT OF TODOS IN HERE - RENAMING IT TO FROM TODO to TODOS FOR NOW
public class SessionManagerImpl implements ISessionManager {

	private static final Logger LOGGER = LogManager.getLogger(SessionManagerImpl.class.getName());

	@Autowired
	IRequestResponseBuilder requestResponseBuilder;

	@Autowired
	private IParseHeaderLine parseHeaderLine;

	private String tracePath = "";

	private IByteArrayLineReader storageReader;
	
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

	public SessionManagerImpl() {
		wellKnownPorts.put("HTTP", 80);
		wellKnownPorts.put("HTTPS", 443);
		wellKnownPorts.put("RTSP", 554);
	}

	public List<Session> processPacketsAndAssembleSessions(List<PacketInfo> packets) {

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

				if (packet instanceof UDPPacket) {
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
				}

				if (packet instanceof TCPPacket) {
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
						session.setDomainName((((UDPPacket) (session.getDnsResponsePacket()).getPacket()).getDns()).getDomainName());
					}
					if (session.getDnsRequestPacket() == null && StringUtils.isNotBlank(session.getDomainName()) && dnsRequestDomains.containsKey(session.getDomainName())) {
						session.setRemoteHostName(session.getDomainName());
						session.setDnsRequestPacket(dnsRequestDomains.get(session.getDomainName()));
					} else {
						session.setRemoteHostName(hostMap.get(session.getRemoteIP()));
					}
					if (tcpPacket.isSslHandshake()) {
						session.setLastSslHandshakePacket(packetInfo);
					}
					if (packetInfo.getAppName() != null) {
						session.getAppNames().add(packetInfo.getAppName());
					}
					
					if (!session.isSessionComplete() && (packetInfo.getTcpFlagString().contains("R")
							|| packetInfo.getTcpFlagString().contains("F"))) {
						session.setSessionComplete(true);
					}
				}
			}
		}

		Collections.sort(sessions);
		analyzeRequestResponses(sessions);

		return sessions;
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
					session.setDomainName(dns.getDomainName());
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

				if (!session.getUplinkPackets().containsKey(tcpPacket.getSequenceNumber())) {
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

		if (!session.getPackets().isEmpty() && (tcpPacket.isFIN() || tcpPacket.isRST())) {
			PacketInfo previousPacket = session.getPackets().get(session.getPackets().size() - 1);
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

	private void analyzeRequestResponses(List<Session> sessions) {

		ArrayList<HttpRequestResponseInfo> results;
		for (Session session : sessions) {

			results = new ArrayList<>();

			if (session.isUdpOnly()) {
				HttpRequestResponseInfo rrInfo = null;
				HttpRequestResponseInfo recentUpRRInfo = null;
				HttpRequestResponseInfo recentDnRRInfo = null;
				for (PacketInfo udpPacketInfo : session.getUdpPackets()) {
					try {
						switch (udpPacketInfo.getDir()) {
						case UPLINK:
							if (!session.isDataInaccessible()) {
								rrInfo = extractHttpRequestResponseInfo(session, udpPacketInfo, udpPacketInfo.getDir());
							}
							if (rrInfo != null) {
								results.add(rrInfo);
								rrInfo.setFirstDataPacket(udpPacketInfo);
								rrInfo.setLastDataPacket(udpPacketInfo);
								recentUpRRInfo = rrInfo;
							} else {
								if (recentUpRRInfo == null) {
									// Creating a Request Objects when no actual requests were found.
									session.setDataInaccessible(true);
									rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), udpPacketInfo.getDir());
									rrInfo.setDirection(HttpDirection.REQUEST);
									results.add(rrInfo);
									rrInfo.setFirstDataPacket(udpPacketInfo);
									rrInfo.setLastDataPacket(udpPacketInfo);
									recentUpRRInfo = rrInfo;
								}

								if (recentUpRRInfo != null && udpPacketInfo.getPayloadLen() != 0) {
									recentUpRRInfo.setLastDataPacket(udpPacketInfo);
									recentUpRRInfo.setContentLength(recentUpRRInfo.getContentLength() + udpPacketInfo.getPayloadLen());
									recentUpRRInfo.setRawSize(recentUpRRInfo.getRawSize() + udpPacketInfo.getLen());
								}
							}
							rrInfo = null;
							recentUpRRInfo.writePayload(udpPacketInfo, false, 0);
							recentUpRRInfo.addUDPPacket(udpPacketInfo);
							break;

						case DOWNLINK:
							if (!session.isDataInaccessible()) {
								rrInfo = extractHttpRequestResponseInfo(session, udpPacketInfo, udpPacketInfo.getDir());
							}
							if (rrInfo != null) {
								results.add(rrInfo);
								rrInfo.setFirstDataPacket(udpPacketInfo);
								rrInfo.setLastDataPacket(udpPacketInfo);
								recentDnRRInfo = rrInfo;
							} else {

								if (recentDnRRInfo == null) {
									rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), udpPacketInfo.getDir());
									rrInfo.setDirection(HttpDirection.RESPONSE);
									results.add(rrInfo);
									rrInfo.setFirstDataPacket(udpPacketInfo);
									rrInfo.setLastDataPacket(udpPacketInfo);
									recentDnRRInfo = rrInfo;
								}

								if (recentDnRRInfo != null && udpPacketInfo.getPayloadLen() != 0) {
									recentDnRRInfo.setLastDataPacket(udpPacketInfo);
									recentDnRRInfo.setContentLength(recentDnRRInfo.getContentLength() + udpPacketInfo.getPayloadLen());
									recentDnRRInfo.setRawSize(recentDnRRInfo.getRawSize() + udpPacketInfo.getLen());
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

			} else {

				analyzeACK(session);
				analyzeZeroWindow(session);
				analyzeRecoverPkts(session);
				PacketInfo packetInfo = null;
				TCPPacket tcpPacket = null;
				HttpRequestResponseInfo rrInfo = null;
				HttpRequestResponseInfo tempRRInfo = null;

				try {

					long expectedUploadSeqNo = 0;
					for (long uploadSequenceNumber : session.getUplinkPackets().keySet()) {
						packetInfo = session.getUplinkPackets().get(uploadSequenceNumber);
						tcpPacket = (TCPPacket) packetInfo.getPacket();
						if (packetInfo.getPayloadLen() > 0) {
							if (!session.isDataInaccessible()) {
								rrInfo = extractHttpRequestResponseInfo(session, packetInfo, packetInfo.getDir());
							}
							if (rrInfo != null) {
								tempRRInfo = rrInfo;

								String host = rrInfo.getHostName();

								if (host != null) {
									URI referrer = rrInfo.getReferrer();
									session.setRemoteHostName(host);
									session.setDomainName(referrer != null ? referrer.getHost() : host);
								}

								if (isAnIOSSecureSession(session)) {
									break;
								}

								results.add(rrInfo);
								rrInfo.setFirstDataPacket(packetInfo);
								rrInfo.setLastDataPacket(packetInfo);
								rrInfo.writeHeader(packetInfo, rrInfo.getHeaderOffset());
								rrInfo.writePayload(packetInfo, true, rrInfo.getHeaderOffset());
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
								}
								session.setDataInaccessible(true);
								rrInfo = new HttpRequestResponseInfo(session.getRemoteHostName(), packetInfo.getDir());
								expectedUploadSeqNo = uploadSequenceNumber + tcpPacket.getPayloadLen();
								rrInfo.setTCP(true);
								results.add(rrInfo);
								rrInfo.setDirection(HttpDirection.REQUEST);
								tempRRInfo = rrInfo;
								rrInfo.setFirstDataPacket(packetInfo);
								rrInfo.setLastDataPacket(packetInfo);
							}
							rrInfo = null;
							tempRRInfo.addTCPPacket(uploadSequenceNumber, packetInfo);
						}
					}

					rrInfo = null;
					tempRRInfo = null;

					if (!session.isIOSSecureSession()) {

						long expectedDownloadSeqNo = 0;
						for (long downloadSequenceNumber : session.getDownlinkPackets().keySet()) {
							packetInfo = session.getDownlinkPackets().get(downloadSequenceNumber);
							tcpPacket = (TCPPacket) packetInfo.getPacket();
							if (packetInfo.getPayloadLen() > 0) {
								if (!session.isDataInaccessible()) {
									rrInfo = extractHttpRequestResponseInfo(session, packetInfo, packetInfo.getDir());
								}
								if (rrInfo != null) {
									tempRRInfo = rrInfo;
									results.add(rrInfo);
									rrInfo.setFirstDataPacket(packetInfo);
									rrInfo.setLastDataPacket(packetInfo);
									rrInfo.writeHeader(packetInfo, rrInfo.getHeaderOffset());
									rrInfo.writePayload(packetInfo, true, rrInfo.getHeaderOffset());
									expectedDownloadSeqNo = downloadSequenceNumber + tcpPacket.getPayloadLen();
								} else if (tempRRInfo != null) {
									boolean flag = false;
									int headerDelta = 0;
									if (!session.isDataInaccessible() && !tempRRInfo.isHeaderParseComplete()) {
										headerDelta = setHeaderOffset(tempRRInfo, packetInfo, tcpPacket);
										tempRRInfo.writeHeader(packetInfo, headerDelta);
										flag = true;
									}

									tempRRInfo.setLastDataPacket(packetInfo);
									tempRRInfo.setRawSize(tempRRInfo.getRawSize() + packetInfo.getLen() - headerDelta);
									if (tcpPacket.getSequenceNumber() == expectedDownloadSeqNo) {
										expectedDownloadSeqNo = tcpPacket.getSequenceNumber() + tcpPacket.getPayloadLen();
										tempRRInfo.writePayload(packetInfo, flag, headerDelta);
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
									rrInfo.setTCP(true);
									rrInfo.setDirection(HttpDirection.RESPONSE);
									results.add(rrInfo);
									tempRRInfo = rrInfo;
									rrInfo.setFirstDataPacket(packetInfo);
									rrInfo.setLastDataPacket(packetInfo);
								}
								rrInfo = null;
								tempRRInfo.addTCPPacket(downloadSequenceNumber, packetInfo);
							}
						}
					}
				} catch (IOException e) {
					LOGGER.error("Error Storing data to TCP Request Response Obect. Session ID: " + session.getSessionKey());
				}

				if (session.isIOSSecureSession()) {
					results = analyzeRequestResponsesForSecureSessions(session);
				}

			}

			Collections.sort(results);
			session.setRequestResponseInfo(results);
			populateDataForRequestResponses(session);

			if (session.getDomainName() == null) {
				session.setDomainName(session.getRemoteHostName() != null ? session.getRemoteHostName() : session.getRemoteIP().getHostAddress());
			}
		}
	}

	private ArrayList<HttpRequestResponseInfo> analyzeRequestResponsesForSecureSessions(Session session) {

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

	private ArrayList<HttpRequestResponseInfo> readFileAndPopulateRequestResponse(Session session, ArrayList<HttpRequestResponseInfo> results, String filePath, PacketDirection packetDirection, HttpDirection httpDirection)
			throws IOException {
		String dataRead;
		long timeStamp = 0;
		HttpRequestResponseInfo rrInfo = null;
		int requestCount = 0;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));

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
                try {
                    rrInfo.setObjUri(new URI(URLEncoder.encode(rrInfo.getObjName(), StandardCharsets.UTF_8.toString())));
                    rrInfo.setHostName(rrInfo.getObjUri().getHost());
                } catch (URISyntaxException | UnsupportedEncodingException e) {
                    // Ignore since value does not have to be a URI
                    LOGGER.info("", e);
                }

                rrInfo.setVersion(matcher.group(3));
                rrInfo.setScheme(rrInfo.getVersion().split("/")[0]);
                rrInfo.setPort(session.getRemotePort());
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

		for (HttpRequestResponseInfo rrInfo : session.getRequestResponseInfo()) {

			if (rrInfo.getDirection() == HttpDirection.REQUEST) {
				requests.add(rrInfo);
				if (session.getDomainName() == null) {
					String host = rrInfo.getHostName();
					if (host != null) {
						URI referrer = rrInfo.getReferrer();
						session.setRemoteHostName(host);
						session.setDomainName(referrer != null ? referrer.getHost() : host);
					}
				}
			} else if (rrInfo.getDirection() == HttpDirection.RESPONSE) {
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
		Double synTime = null;
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

	private HttpRequestResponseInfo extractHttpRequestResponseInfo(Session session, PacketInfo packetInfo, PacketDirection packetDirection) {

		HttpRequestResponseInfo rrInfo = null;

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		BufferedOutputStream bufferedStream = new BufferedOutputStream(stream);

		TCPPacket tcpPacket = null;
		UDPPacket udpPacket = null;
		if (packetInfo.getPacket() instanceof TCPPacket) {
			tcpPacket = (TCPPacket) packetInfo.getPacket();
			try {
				bufferedStream.write(tcpPacket.getData(), tcpPacket.getDataOffset(), tcpPacket.getData().length - tcpPacket.getDataOffset());
			} catch (Exception exception) {
				LOGGER.error("Error Reading Data from TCP Packet: " + exception.getMessage());
			}
		} else {
			udpPacket = (UDPPacket) packetInfo.getPacket();
			try {
				bufferedStream.write(udpPacket.getData(), udpPacket.getDataOffset(), udpPacket.getData().length - udpPacket.getDataOffset());
			} catch (Exception exception) {
				LOGGER.error("Error Reading Data from UDP Packet: " + exception.getMessage());
			}
		}

		try {
			bufferedStream.flush();
			storageReader.init(stream.toByteArray());
			String line = storageReader.readLine();
			if (line != null && line.length() != 0) {
			    rrInfo = initializeRequestResponseObject(line, session, packetDirection);
			    if (rrInfo != null && tcpPacket != null) {
			        rrInfo.setTCP(true);
                    rrInfo = populateRRInfo(session, tcpPacket, rrInfo);
			    }
			}
		} catch (IOException e1) {
			LOGGER.error(e1.getMessage());
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

		rrInfo.setHeaderOffset(storageReader.getIndex());
		return rrInfo;
	}

	private boolean isAnIOSSecureSession(Session session) {

		if (session.getDomainName() == null) {
			return false;
		}

		String sessionKey = session.getDomainName() + "_" + session.getLocalPort();
		File clearSessionRecFileUL = new File(this.tracePath + sessionKey + "_UL");
		File clearSessionRecFileDL = new File(this.tracePath + sessionKey + "_DL");
		if (clearSessionRecFileUL.exists() && clearSessionRecFileDL.exists()) {
			session.setIOSSecureSession(true);
			return true;
		}
		return false;
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

		for (PacketInfo pinfo : sess.getPackets()) {
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

		for (PacketInfo pInfo : sess.getPackets()) {
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

		for (PacketInfo pInfo : sess.getPackets()) {
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
