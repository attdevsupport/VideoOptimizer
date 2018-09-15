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
package com.att.aro.core.packetanalysis.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.packetanalysis.IRequestResponseBuilder;
import com.att.aro.core.packetanalysis.ISessionManager;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.PacketRangeInStorage;
import com.att.aro.core.packetanalysis.pojo.Reassembler;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TcpInfo;
import com.att.aro.core.packetanalysis.pojo.Termination;
import com.att.aro.core.packetreader.pojo.DomainNameSystem;
import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.att.aro.core.packetreader.pojo.TCPPacket;
import com.att.aro.core.packetreader.pojo.UDPPacket;
import com.att.aro.core.securedpacketreader.ICipherDataService;
import com.att.aro.core.securedpacketreader.ICrypto;
import com.att.aro.core.securedpacketreader.ISSLKeyService;
import com.att.aro.core.securedpacketreader.ITLSHandshake;
import com.att.aro.core.securedpacketreader.ITLSSessionInfo;
import com.att.aro.core.securedpacketreader.pojo.BidirDataChunk;
import com.att.aro.core.securedpacketreader.pojo.CryptoEnum;
import com.att.aro.core.securedpacketreader.pojo.MatchedRecord;
import com.att.aro.core.securedpacketreader.pojo.SavedTLSSession;

//FIXME LOT OF TODOS IN HERE - RENAMING IT TO FROM TODO to TODOS FOR NOW
public class SessionManagerImpl implements ISessionManager {

	private static final Logger LOGGER = LogManager.getLogger(SessionManagerImpl.class.getName());

	@Autowired
	ICrypto crypto;

	@Autowired
	IRequestResponseBuilder requestResponseBuilder;

	@Autowired
	ITLSSessionInfo tsiServer;
	@Autowired
	ITLSSessionInfo tsiClient;
	@Autowired
	ITLSSessionInfo tsiPending;
	@Autowired
	ITLSHandshake handshake;
	@Autowired
	ICipherDataService cipherDataService;
	@Autowired
	ISSLKeyService sslKeyService;

	private List<PacketInfo> packets;

	private static final int PROT_RECORD_TLS = 1;
	private static final int PROT_RECORD_TLS_FIRST = 2; // first record

	public static final String TLS_ERROR_INVALIDPKTDIR = "tls.error.invalidPktDir";
	public static final int TLS_RECORD_CHANGE_CIPHER_SPEC = 20;
	public static final int TLS_RECORD_ALERT = 21;
	public static final int TLS_RECORD_HANDSHAKE = 22;
	public static final int TLS_RECORD_APP_DATA = 23;
	public static final int TLS_HANDSHAKE_CLIENT_HELLO = 1;
	public static final int TLS_HANDSHAKE_SERVER_HELLO = 2;
	public static final int TLS_HANDSHAKE_CERTIFICATE = 11;
	public static final int TLS_HANDSHAKE_SERVER_HELLO_DONE = 14;
	public static final int TLS_HANDSHAKE_CLIENT_KEY_EXCHANGE = 16;
	public static final int TLS_HANDSHAKE_FINISHED = 20;
	public static final int TLS_HANDSHAKE_SERVER_KEY_EXCHANGE = 12;
	public static final int TLS_HANDSHAKE_CERTIFICATE_REQUEST = 13;
	public static final int TLS_HANDSHAKE_CERTIFICATE_VERIFY = 15;
	public static final int TLS_HANDSHAKE_NEW_SESSION_TICKET = 4;
	public static final int TLS_HANDSHAKE_NEXT_PROTOCOL = 67;
	public static final int TLS_MASTER_SECRET_LEN = 48;
	public static final int TLS_STATE_NULL = 0;
	public static final int TLS_STATE_C_HELLO = 1;
	public static final int TLS_STATE_S_HELLO = 2;
	public static final int TLS_STATE_S_CERTIFICATE = 3;
	public static final int TLS_STATE_S_HELLODONE = 4;
	public static final int TLS_STATE_C_KEYEXCHANGE = 5;
	public static final int TLS_STATE_C_CHANGECIPHERSPEC = 6;
	public static final int TLS_STATE_C_FINISHED = 7;
	public static final int TLS_STATE_S_CHANGECIPHERSPEC = 8;
	public static final int TLS_STATE_S_FINISHED = 9;
	public static final int TLS_STATE_HS_FINISHED = 10;

	public static final int COMPRESS_DEFLATE = 1;
	public static final int COMPRESS_NONE = 0;

	public static final int HTTPS_MODE_NONE = 0; // plain text, no https
	public static final int HTTPS_MODE_NORMAL = 1; // https, no compression
	public static final int HTTPS_MODE_DEFLATE = 2; // https, with deflate
													// compression

	public static final int ALERT_LEVEL_WARNING = 1;
	public static final int ALERT_LEVEL_FATAL = 2;
	public static final int ALERT_CLOSE_NOTIFY = 0;

	public List<Session> assembleSession(List<PacketInfo> packets) {
		this.packets = packets;
		Map<String, Session> allSessions = new LinkedHashMap<String, Session>();
		List<PacketInfo> dnsPackets = new ArrayList<PacketInfo>();
		List<PacketInfo> udpPackets = new ArrayList<PacketInfo>();
		Map<InetAddress, String> hostMap = new HashMap<InetAddress, String>();

		iteratePackets(packets, allSessions, dnsPackets, udpPackets, hostMap);

		LOGGER.debug("end of first looping, now got session: " + allSessions.size());
		LOGGER.debug("dns packet: " + dnsPackets.size());

		// Reassemble sessions
		List<Session> sessions = new ArrayList<Session>(allSessions.values());

		Reassembler upl = new Reassembler();
		Reassembler dol = new Reassembler();
		try {
			reassembleSessions(sessions, upl, dol);
			LOGGER.debug("creating HttpReqResInfo for sessions: " + sessions.size());
			analyzeRequestResponseInfo(sessions);
		} finally {
			upl.clear();
			dol.clear();
		}

		Collections.sort(sessions);

		if (!udpPackets.isEmpty()) {
			List<Session> udpSessions;
			try {
				udpSessions = getUDPSessions(udpPackets, sessions);
				sessions.addAll(udpSessions);
			} catch (IOException e) {
				LOGGER.error("Error", e);
			}
		}

		for(Session session : sessions) {
			session.getpStorageBothRAW().clear();
			session.setpStorageBothRAW(null);
		}

		return sessions;
	}

	/**
	 * <pre>
	 * Analyze SSL RequestResponseInfo. Iterates through all sessions, both with
	 * and without ssl
	 * 
	 * @param sessions
	 */
	@SuppressWarnings("unused")
	private void analyzeSSLRequestResponseInfo(List<Session> sessions) {
		// TODOS refactor this
		// there is ssl here
		List<List<BidirDataChunk>> bdcRawList = new ArrayList<List<BidirDataChunk>>();
		int passIndex = 0;
		int bNeed2ndPass = 0;
		crypto.resetSSLKeyUsage();

		for (int nPass = 1; nPass <= 2; nPass++) {
			passIndex = 0;

			// More session parsing
			for (Session session : sessions) {

				List<BidirDataChunk> bdcRaw = session.getBdcRaw();

				if (nPass == 2 && bdcRawList.get(passIndex).size() == 0) {
					passIndex++;
					continue;
				}

				if (nPass == 1) {
					for (PacketInfo packet : session.getPackets()) {
						session.setBytesTransferred(session.getBytesTransferred() + packet.getLen());
					}
				} else {
					session.setBdcRaw(bdcRawList.get(passIndex));
				}

				// -logger.info("Session TS:"+session.getSessionStartTime());
				generateRecords(session, PROT_RECORD_TLS); // populate mrList
				List<MatchedRecord> mrList = session.getMrList();

				if (!mrList.isEmpty()) {
					int retVal = parse(session, packets, nPass);
					if (nPass == 1 && retVal == -2) {
						session.getpStorageULDCPT().reset();
						session.getpStorageDLDCPT().reset();
						session.getpStorageBothDCPT().reset();

						session.getDec2encDL().clear();
						session.getDec2encUL().clear();
						session.setTsTLSHandshakeBegin(-1);
						session.setTsTLSHandshakeEnd(-1);
						session.setProtocol(-1);
						session.setHttpsMode(HTTPS_MODE_NONE);

						bdcRawList.add(bdcRaw);
						bNeed2ndPass = 1;
						passIndex++;
						continue;
					} else if (retVal == 1) {

						// Appears to not be used in 4.1.1 for any real use
						// for (PacketInfo packet : session.getPackets()) {
						// if (packet.getPacket() instanceof TCPPacket) {
						// TCPPacket tcp = (TCPPacket) packet.getPacket();
						// if ((tcp.isSsl()) || (tcp.getDestinationPort() ==
						// 443) || (tcp.getSourcePort() == 443)) {
						// //TODOS find references to the following
						// analysis.setTotalHTTPSAnalyzedBytes(analysis.getTotalHTTPSAnalyzedBytes()
						// + packet.getLen());
						// }
						// }
						// }

						if (session.getpStorageULDCPT().size() > session.getStorageUl().length) {
							session.setStorageUlext(session.getStorageUl());
						}
						if (session.getpStorageDLDCPT().size() > session.getStorageDl().length) {
							session.setStorageDlext(session.getStorageDl());
						}
						session.setStorageUl(session.getpStorageULDCPT().toByteArray());
						session.setStorageDl(session.getpStorageDLDCPT().toByteArray());
					}
				}

				analyzeACK(session);
				analyzeZeroWindow(session);
				analyzeRecoverPkts(session);

				// TODOS Validate TCP info is set on all
				// CheckTCPInfo(s);

				// // TODOS Check this?
				// s.pStorageUL->CheckPacketsRange();
				// s.pStorageDL->CheckPacketsRange();
				//

				// Parse HTTP request response info
				try {
					session.setRequestResponseInfo(requestResponseBuilder.createRequestResponseInfo(session));
				} catch (IOException e) {
					LOGGER.error("IOException " + e.getMessage());
				}

				for (HttpRequestResponseInfo hrri : session.getRequestResponseInfo()) {
					if (hrri.getDirection() == HttpDirection.REQUEST) {
						// Assume first host found is same for entire session
						if (session.getDomainName() == null) {
							String host = hrri.getHostName();
							if (host != null) {
								URI referrer = hrri.getReferrer();
								session.setRemoteHostName(host);
								session.setDomainName(referrer != null ? referrer.getHost() : host);
							}
						}
					} else if (hrri.getDirection() == HttpDirection.RESPONSE && hrri.getContentLength() > 0) {
						session.setFileDownloadCount(session.getFileDownloadCount() + 1);
					}
				}

				if (session.getDomainName() == null) {
					session.setDomainName(session.getRemoteHostName() != null ? session.getRemoteHostName()
							: session.getRemoteIP().getHostAddress());
				}

				if (nPass == 1) {
					List<BidirDataChunk> dummy = new ArrayList<BidirDataChunk>();
					bdcRawList.add(dummy);
				}
				passIndex++;
			}

			if (bNeed2ndPass == 0) {
				break;
			}
		}
	}

	// parsing
	private int parse(Session session, List<PacketInfo> packetList, int nPass) {
		// TLSHandshake handshake = new TLSHandshake();
		// TLS_SESSION_INFO tsiServer = new TLS_SESSION_INFO(0);
		// TLS_SESSION_INFO tsiClient = new TLS_SESSION_INFO(1);
		// TLS_SESSION_INFO tsiPending = new TLS_SESSION_INFO(2);

		tsiServer.init(0);
		tsiClient.init(1);
		tsiPending.init(2);

		byte[] clientRandom = null;
		byte[] serverRandom = null;
		byte[] sessionID = null;
		byte[] thisSessionID = null;
		byte[] clientTicketExtension = null;
		byte[] serverTicketExtension = null;
		byte[] masterSecret = null;
		byte[] serverIssuedTicket = null;
		int bServerIssuedTicket = 0;
		int bClientTicketExtension = 0;
		int bClientFinished = 0;
		int bServerFinished = 0;
		int bClientChangeCipher = 0;
		int bServerChangeChiper = 0;
		int state = TLS_STATE_NULL;
		int bClientClosed = 0;
		int bServerClosed = 0;
		long sessionOffsetUL = 0;
		long sessionOffsetDL = 0;
		double serverHelloTS = -1.0f;

		// List<BidirDataChunk> bdcRaw = session.getBdcRaw();
		List<MatchedRecord> mrList = session.getMrList();

		int nTemp = mrList.size();
		for (int mrListItr = 0; mrListItr < nTemp; mrListItr++) {
			if (bClientClosed != 0 && bServerClosed != 0) {
				break;
			}

			PacketDirection dir = mrList.get(mrListItr).getDir();
			ByteBuffer pData = ByteBuffer.wrap(getRecord(session, mrList.get(mrListItr)));
			if (pData == null) {
				// TODOS fix -
				// logger.error(Util.RB.getString("tls.error.ssldata"));
				LOGGER.warn("tls.error.ssldata");
				return -1;
			}

			if (dir == PacketDirection.UPLINK) {
				sessionOffsetUL += mrList.get(mrListItr).getBytes();
			} else {
				sessionOffsetDL += mrList.get(mrListItr).getBytes();
			}

			byte[] encRecPayload = null; // encrypted
			byte[] recPayload = new byte[65536]; // decrypted
			// -logger.info("new byte[65536] recPayload:" + recPayload.length);
			Integer[] recPayloadLen = new Integer[1];
			{
				byte recType = pData.get(0);
				if (checkTLSVersion(pData.array(), 1) == 0) {
					// TODOS fix -
					// logger.error(Util.RB.getString("tls.error.sslversion"));
					LOGGER.warn("tls.error.sslversion");
					return -1;
				}
				int encRecPayloadSize = pData.getShort(3); // Reverse not
															// required as Java
															// follows big
															// endian byte order
				if (encRecPayloadSize > 0) {
					for (int j = 0; j < 5; j++) {
						pData.get();
					}
					encRecPayload = new byte[encRecPayloadSize];
					pData.get(encRecPayload, 0, encRecPayloadSize);
					pData.position(0);

					int returnVal = -1;
					// -logger.info("i_temp="+mrListItr);
					switch (dir) {
					case UPLINK:
						// MyAssert(tsiServer.pCipherClient == NULL, 1159);
						// //-logger.info("ST:"+session.getSessionStartTime()+"
						// UPLINK tsiClient.decrypt recPayload:" +
						// recPayload.length);
						returnVal = tsiServer.decrypt(encRecPayload, recPayload, recPayloadLen, PacketDirection.UPLINK,
								recType);
						// //-logger.info(Util.byteArrayToString(recPayload));
						break;

					case DOWNLINK:
						// MyAssert(tsiClient.pCipherServer == NULL, 1160);
						// //-logger.info("ST:"+session.getSessionStartTime()+"
						// DOWNLINK tsiClient.decrypt recPayload:" +
						// recPayload.length);

						returnVal = tsiClient.decrypt(encRecPayload, recPayload, recPayloadLen,
								PacketDirection.DOWNLINK, recType);
						// //-logger.info(Util.byteArrayToString(recPayload));
						break;

					default:
						// TODOS fix -
						// logger.error(Util.RB.getString(TLS_ERROR_INVALIDPKTDIR));
						LOGGER.error(TLS_ERROR_INVALIDPKTDIR);
						return -1;
					}

					if (returnVal <= 0) {
						LOGGER.error("30012 - Error in SSL record decryption.");
						return -1;
					}
				}

				ByteBuffer recPayloadByteBuff = ByteBuffer.wrap(recPayload);
				int recOfs = -1;
				// int recLen = 5 + encRecPayloadSize;
				if (dir == PacketDirection.UPLINK) {
					recOfs = (int) sessionOffsetUL - mrList.get(mrListItr).getBytes();
				} else {
					recOfs = (int) sessionOffsetDL - mrList.get(mrListItr).getBytes();
				}

				switch (recType) {
				case TLS_RECORD_HANDSHAKE: {
					int handshakeOffset = 0;
					Integer[] handshakeSize = new Integer[1];
					handshakeSize[0] = -1;

					while (true) {
						if (handshakeOffset == recPayloadLen[0]) {
							break;
						}

						for (int j = 0; j < handshakeOffset; j++) {
							recPayloadByteBuff.get();
						}
						byte[] recPayloadtemp = new byte[recPayloadLen[0] - handshakeOffset];
						recPayloadByteBuff.get(recPayloadtemp, 0, recPayloadLen[0] - handshakeOffset);
						recPayloadByteBuff.position(0);

						int resRead = handshake.read(recPayloadtemp, recPayloadLen[0] - handshakeOffset, handshakeSize);
						if (resRead != 1) {
							if (resRead == 0) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.readhandshake"));
								LOGGER.warn("tls.error.readhandshake");
							}
							return -1;
						}
						handshakeOffset += handshakeSize[0];

						// -logger.info("------switch :"+handshake.getType());

						switch (handshake.getType()) {

						case TLS_HANDSHAKE_CLIENT_HELLO: {
							// -logger.info(">>>>TLS_HANDSHAKE_CLIENT_HELLO");
							clientRandom = new byte[32];
							for (int index = 0; index < 32; index++) {
								clientRandom[index] = handshake.getClientRandom()[index]; // clientRandom.SetData(handshake.clientRandom,
																							// 32);
							}

							state = TLS_STATE_C_HELLO;
							{
								// -logger.info("TLS_STATE_C_HELLO");
								List<Integer> tempArray = new ArrayList<Integer>();
								Reassembler rAsm = new Reassembler();
								rAsm.setPktRanges(session.getPktRangesUl());
								rAsm.getPacketIDList(recOfs, recOfs, tempArray);
								if (tempArray.size() == 1) {
									session.setTsTLSHandshakeBegin(
											packetList.get(tempArray.get(0) - 1).getPacket().getTimeStamp());
								} else {
									// TODOS fix -
									// logger.error(Util.RB.getString("tls.error.PacketIDList"));
									LOGGER.warn("tls.error.PacketIDList");
									return -1;
								}
							}

							if (handshake.getSessionIDLen() == 0) {
								sessionID = null;
							} else {
								sessionID = new byte[handshake.getSessionIDLen()];
								for (int index = 0; index < handshake.getSessionIDLen(); index++) {
									sessionID[index] = handshake.getSessionID()[index]; // sessionID.SetData(handshake.sessionID,
																						// handshake.sessionIDLen);
								}
							}

							clientTicketExtension = null;
							if (handshake.getTicketLen() == -1) {
								bClientTicketExtension = 0;
							} else if (handshake.getTicketLen() == 0) {
								bClientTicketExtension = 1;
							} else {
								bClientTicketExtension = 1;
								clientTicketExtension = new byte[handshake.getTicketLen()];
								for (int index = 0; index < handshake.getTicketLen(); index++) {
									clientTicketExtension[index] = handshake.getTicket()[index]; // clientTicketExtension.SetData(handshake.ticket,handshake.ticketLen);
								}
							}
							break;
						}

						case TLS_HANDSHAKE_SERVER_HELLO: {
							// -logger.info(">>>>TLS_HANDSHAKE_SERVER_HELLO");
							{
								List<Integer> tempArray = new ArrayList<Integer>();
								Reassembler rAsm = new Reassembler();
								rAsm.setPktRanges(session.getPktRangesUl());
								rAsm.getPacketIDList(recOfs, recOfs, tempArray);
								if (tempArray.size() == 1) {
									serverHelloTS = packetList.get(tempArray.get(0) - 1).getPacket().getTimeStamp();
								} else {
									// TODOS fix -
									// logger.error(Util.RB.getString("tls.error.PacketIDList"));
									LOGGER.error("tls.error.PacketIDList");
									return -1;
								}
							}

							serverRandom = new byte[32];
							for (int index = 0; index < 32; index++) {
								serverRandom[index] = handshake.getServerRandom()[index]; // serverRandom.SetData(handshake.serverRandom,
																							// 32);
							}
							tsiPending
									.setTLSCipherSuite(cipherDataService.getTLSCipherSuite(handshake.getCipherSuite()));
							if (tsiPending.getTLSCipherSuite() == null) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.keyexchange"));
								LOGGER.warn("tls.error.keyexchange");
								return -1;
							}
							// TODOS: Diffie-Hellman not supported
							if (tsiPending.getTLSCipherSuite()
									.getKeyexchange() != CryptoEnum.TLSKeyExchange.TLS_KEY_X_RSA) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.onlyRSAsupported"));
								LOGGER.debug("tls.error.onlyRSAsupported");
								return -1;
							}

							tsiPending.setCipherData(
									cipherDataService.getTLSCipherData(tsiPending.getTLSCipherSuite().getCipher()));
							if (tsiPending.getCipherData() == null) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.cipherdata"));
								LOGGER.debug("tls.error.cipherdata");
								return -1;
							}

							tsiPending.setCompressionMethod(handshake.getCompressionMethod());
							if (handshake.getSessionIDLen() == 0) {
								sessionID = null;
							} else {
								if (sessionID != null) {
									boolean match = true;
									for (int j = 0; j < handshake.getSessionIDLen(); j++) {
										if (handshake.getSessionID()[j] != sessionID[j]) {
											match = false;
											break;
										}
									}
									if (handshake.getSessionIDLen() == sessionID.length && match) {
										// sessionID of client and server match
										LOGGER.warn("sessionID of client and server match");
									} else {
										sessionID = null;
									}
								}
								thisSessionID = new byte[handshake.getSessionIDLen()];
								for (int index = 0; index < handshake.getSessionIDLen(); index++) {
									thisSessionID[index] = handshake.getSessionID()[index]; // thisSessionID.SetData(handshake.sessionID,
																							// handshake.sessionIDLen);
								}
							}

							state = TLS_STATE_S_HELLO;

							serverTicketExtension = null;
							// if (session.getTsTLSHandshakeBegin() ==
							// 1.465842822366355E9) {
							// logger.info(session);
							// }
							if (handshake.getTicketLen() == -1) {
								LOGGER.warn("Invalid TLS handshake ticket length");
							} else if (handshake.getTicketLen() == 0) {
								// -logger.info("Zero TLS handshake ticket
								// length:"+session);
								LOGGER.warn("Zero TLS handshake ticket length");
							} else {

								serverTicketExtension = new byte[handshake.getTicketLen()];
								for (int index = 0; index < handshake.getTicketLen(); index++) {
									serverTicketExtension[index] = handshake.getTicket()[index]; // serverTicketExtension.SetData(handshake.ticket,handshake.ticketLen);
								}
							}
							break;
						}

						case TLS_HANDSHAKE_CERTIFICATE: {
							// -logger.info(">>>>TLS_HANDSHAKE_CERTIFICATE");
							state = TLS_STATE_S_CERTIFICATE;
							break;
						}

						case TLS_HANDSHAKE_SERVER_HELLO_DONE: {
							// -logger.info(">>>>TLS_HANDSHAKE_SERVER_HELLO_DONE");
							state = TLS_STATE_S_HELLODONE;
							break;
						}

						case TLS_HANDSHAKE_CLIENT_KEY_EXCHANGE: {
							// -logger.info(">>>>TLS_HANDSHAKE_CLIENT_KEY_EXCHANGE");
							byte[] master = new byte[4096];
							int retVal = -1;

							if (serverHelloTS == -1.0 || serverRandom == null) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.invalidRecType"));
								LOGGER.error("tls.error.invalidRecType");
								return -1;
							}

							// get the master key by directly read from the SSL
							// log
							if (crypto.isVpnKey()) {
								retVal = sslKeyService.getMasterFromKeyList(session, master);
							} else {
								retVal = sslKeyService.getMasterFromSSLLog(serverHelloTS, master, clientRandom,
										serverRandom);
							}

							if (retVal == 0) {
								if (nPass == 2) {
									// TODOS -fix
									// logger.warning(Util.RB.getString("tls.error.masterNotFound"));
									LOGGER.info("tls.error.masterNotFound");
								}
								return -2;
							}

							retVal = tsiPending.setupCiphers(master, clientRandom, serverRandom, tsiPending);
							if (retVal == -1) {
								return -1;
							}

							/*
							 * Interaction between session ID and session ticket
							 * (From RFC 5077) Rule 1: Client: no ticket Server:
							 * issue a ticket Server should put an empty session
							 * ID. In any case Session ID is always ignored
							 * 
							 * Rule 2: Server rejects a ticket, full handshake
							 * Session ID is valid
							 * 
							 * Case 3: Server accepts a ticket The server must
							 * use the same session ID as the client's
							 * 
							 * Case 4: Client presents a ticket and session ID
							 * Server must not use the session ID of the cilent
							 * for resuming session
							 * 
							 * Case 5: Client presents a ticket and session ID
							 * is empty Server's session ID is ignored.
							 */

							masterSecret = new byte[TLS_MASTER_SECRET_LEN];
							System.arraycopy(master, 0, masterSecret, 0, TLS_MASTER_SECRET_LEN);

							retVal = tsiPending.saveTLSSessionByID(thisSessionID, master);
							state = TLS_STATE_C_KEYEXCHANGE;
							break;
						}

						case TLS_HANDSHAKE_FINISHED: {
							// -logger.info(">>>>TLS_HANDSHAKE_FINISHED");
							switch (dir) {
							case UPLINK: {
								bClientFinished = 1;
								state = TLS_STATE_C_FINISHED;
								break;
							}

							case DOWNLINK: {
								bServerFinished = 1;
								state = TLS_STATE_S_FINISHED;
								break;
							}

							default:
								// TODOS fix -
								// logger.error(Util.RB.getString(TLS_ERROR_INVALIDPKTDIR));
								LOGGER.error(TLS_ERROR_INVALIDPKTDIR);
								return -1;
							}

							if (bClientFinished == 1 && bServerFinished == 1) {
								List<Integer> tempList = new ArrayList<Integer>();
								Reassembler rAsm = new Reassembler();
								if (dir == PacketDirection.DOWNLINK) {
									rAsm.setPktRanges(session.getPktRangesDl());
								} else {
									rAsm.setPktRanges(session.getPktRangesUl());
								}
								rAsm.getPacketIDList(recOfs, recOfs, tempList);
								session.setTsTLSHandshakeEnd(
										packetList.get(tempList.get(0) - 1).getPacket().getTimeStamp());

								state = TLS_STATE_HS_FINISHED;
							}
							break;
						}

						case TLS_HANDSHAKE_SERVER_KEY_EXCHANGE:
							// -logger.info(">>>>TLS_HANDSHAKE_SERVER_KEY_EXCHANGE");
						case TLS_HANDSHAKE_CERTIFICATE_REQUEST:
							// -logger.info(">>>>TLS_HANDSHAKE_CERTIFICATE_REQUEST");
						case TLS_HANDSHAKE_CERTIFICATE_VERIFY: {
							// -logger.info(">>>>TLS_HANDSHAKE_CERTIFICATE_VERIFY");
							// TODOS fix -
							// logger.error(Util.RB.getString("tls.error.unsupportedTLSHandshake"));
							LOGGER.error("tls.error.unsupportedTLSHandshake");
							return -1;
						}

						case TLS_HANDSHAKE_NEW_SESSION_TICKET: {
							// -logger.info(">>>>TLS_HANDSHAKE_NEW_SESSION_TICKET");
							if (bServerIssuedTicket != 0 || masterSecret == null) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.invalidServerIssuedTicket"));
								LOGGER.warn("tls.error.invalidServerIssuedTicket");
								return -1;
							}

							serverIssuedTicket = new byte[handshake.getTicketLen()];
							for (int index = 0; index < handshake.getTicketLen(); index++) {
								serverIssuedTicket[index] = handshake.getTicket()[index]; // serverIssuedTicket.SetData(handshake.ticket,
																							// handshake.ticketLen);
							}
							bServerIssuedTicket = 1;

							if (masterSecret.length != TLS_MASTER_SECRET_LEN) {
								// TODOS fix -
								// logger.error(Util.RB.getString("tls.error.incorrectMasterLen"));
								LOGGER.error("tls.error.incorrectMasterLen");
								return -1;
							}
							resRead = tsiPending.saveTLSSessionByTicket(serverIssuedTicket, masterSecret);
							break;
						}

						case TLS_HANDSHAKE_NEXT_PROTOCOL: {
							// -logger.info(">>>>TLS_HANDSHAKE_NEXT_PROTOCOL");
							session.setProtocol(handshake.getNextProtocol());
							break;
						}

						default:
							// -logger.info(">>>>TLS_ nothing ... so default");
							// TODOS fix -
							// logger.error(Util.RB.getString("tls.error.invalidHSType"));
							LOGGER.warn("tls.error.invalidHSType");
							return -1;
						} // End of inner switch
					} // End of while loop
					break;
				} // End of 1st case in main switch

				case TLS_RECORD_CHANGE_CIPHER_SPEC: {
					LOGGER.info("TLS_RECORD_CHANGE_CIPHER_SPEC\n");
					// the record payload should only contain 0x01
					switch (dir) {
					case UPLINK: {
						if (state == TLS_STATE_C_KEYEXCHANGE || state == TLS_STATE_S_FINISHED) {
							// client side update
							tsiServer.copyFrom(tsiPending);
							tsiServer.setpCipherClient(-1);
							crypto.setCryptoCipherNull(0, 1);
							tsiPending.setpCipherServer(-1);
							crypto.setCryptoCipherNull(2, 0);
							tsiServer.initDecompression();
							state = TLS_STATE_C_CHANGECIPHERSPEC;
							bClientChangeCipher = 1;
						} else {
							LOGGER.error("tls.error.invalidTLSstate");
							return -1;
						}
						break;
					}

					case DOWNLINK:

					{
						switch (state) {
						case TLS_STATE_C_FINISHED: {
							if (state != TLS_STATE_C_FINISHED) {
								LOGGER.error("30029 - Wrong TLS state");
							}
							break;
						}

						case TLS_STATE_S_HELLO: {
							if (state != TLS_STATE_S_HELLO) {
								LOGGER.error("30030 - Wrong TLS state");
							}

							SavedTLSSession[] pSaved = new SavedTLSSession[1];
							int res = tsiPending.getSavedTLSSessionByID(sessionID, pSaved); // NOTE
																							// was
																							// static
																							// in
																							// 4.1.1
							if (res == 0) {
								if (bClientTicketExtension == 1) {
									res = tsiPending.getSavedTLSSessionByTicket(clientTicketExtension, pSaved); // NOTE
																												// was
																												// static
																												// in
																												// 4.1.1
								} else {
									res = 0;
								}

								if (res == 0) {
									return 0;
								}
							}

							masterSecret = new byte[TLS_MASTER_SECRET_LEN];
							for (int index = 0; index < TLS_MASTER_SECRET_LEN; index++) {
								masterSecret[index] = pSaved[0].getMaster()[index]; // masterSecret.SetData(pSaved.master,
																					// TLS_MASTER_SECRET_LEN);
							}
							res = tsiPending.setupCiphers(pSaved[0].getMaster(), clientRandom, serverRandom,
									tsiPending); // NOTE was static in 4.1.1
							if (res == -1) {
								return -1;
							}
							break;
						}

						default:
							// TODOS fix -
							// logger.error(Util.RB.getString("tls.error.invalidTLSstate"));
							LOGGER.error("tls.error.invalidTLSstate");
							return -1;
						}

						// server side update
						tsiClient.copyFrom(tsiPending);
						tsiClient.setpCipherServer(-1);
						crypto.setCryptoCipherNull(1, 0);
						tsiPending.setpCipherClient(-1);
						crypto.setCryptoCipherNull(2, 1);
						tsiClient.initDecompression();
						state = TLS_STATE_S_CHANGECIPHERSPEC;
						bServerChangeChiper = 1;
						break;
					}

					default: // dir
						// TODOS fix -
						// logger.error(Util.RB.getString(TLS_ERROR_INVALIDPKTDIR));
						LOGGER.error(TLS_ERROR_INVALIDPKTDIR);
						return -1;
					}

					if (bClientChangeCipher == 1 && bServerChangeChiper == 1) {
						// At this moment the pending state can be cleaned.
						tsiPending.clean();

						if (tsiClient.getCompressionMethod() == COMPRESS_DEFLATE) {
							session.setHttpsMode(HTTPS_MODE_DEFLATE);
						}
					}
					break;
				}

				case TLS_RECORD_APP_DATA: {
					if ((recPayloadLen == null) || (recPayloadLen[0] == null) || (recPayloadLen[0] == 0)) {
						break;
					}

					// StorageRangeMapperImpl srm = new
					// StorageRangeMapperImpl();
					//
					// srm.nx = recOfs;
					// srm.ny = recOfs + recLen - 1;

					switch (dir) {
					case UPLINK:

						if (bClientClosed == 1) {
							break;
						}

						// srm.x = session.getpStorageULDCPT().size();
						// srm.y = srm.x + recPayloadLen[0] - 1;

						// -logger.info("UPLINK
						// session.getpStorageULDCPT().write recPayload:" +
						// recPayload.length);
						session.getpStorageULDCPT().write(recPayload, 0, recPayloadLen[0]); // session.pStorageUL_DCPT->PushData(recPayload);
						session.getpStorageBothDCPT().write(recPayload, 0, recPayloadLen[0]); // session.pStorageBoth_DCPT->PushData(recPayload);
						// this.dec2encUL.add(srm);
						break;

					case DOWNLINK:
						if (bServerClosed == 1) {
							break;
						}
						// srm.x = this.pStorageDLDCPT.size();
						// srm.y = srm.x + recPayloadLen[0] - 1;

						// -logger.info("DOWNLINK
						// session.getpStorageULDCPT().write recPayload:" +
						// recPayload.length);
						session.getpStorageDLDCPT().write(recPayload, 0, recPayloadLen[0]); // session.pStorageDL_DCPT->PushData(recPayload);
						session.getpStorageBothDCPT().write(recPayload, 0, recPayloadLen[0]); // session.pStorageBoth_DCPT->PushData(recPayload);
						// this.dec2encDL.add(srm);
						break;

					default:
						// TODOS fix -
						// logger.error(Util.RB.getString(TLS_ERROR_INVALIDPKTDIR));
						LOGGER.error(TLS_ERROR_INVALIDPKTDIR);
						return -1;
					}
					break;
				}

				case TLS_RECORD_ALERT: {
					if (recPayloadLen[0] != 2) {
						// TODOS fix -
						// logger.error(Util.RB.getString("tls.error.wrongTLS_AlertSize"));
						LOGGER.error("tls.error.wrongTLS_AlertSize");
						return -1;
					}

					byte alertLevel = recPayload[0];
					byte alert = recPayload[1];

					if (alertLevel != ALERT_LEVEL_WARNING && alertLevel != ALERT_LEVEL_FATAL) {
						// TODOS fix -
						// logger.error(Util.RB.getString("tls.error.wrongTLS_AlertLevel"));
						LOGGER.error("tls.error.wrongTLS_AlertLevel");
						return -1;
					}

					if (alert == ALERT_CLOSE_NOTIFY) {
						if (dir == PacketDirection.UPLINK) {
							bClientClosed = 1;
						} else {
							bServerClosed = 1;
						}
					} else {
						// TODOS fix -
						// logger.error(Util.RB.getString("tls.error.invalidTLS_Alert"));
						LOGGER.error("tls.error.invalidTLS_Alert");
						return -1;
					}

					break;
				}

				default:
					// TODOS fix -
					// logger.error(Util.RB.getString("tls.error.invalidHSType"));
					LOGGER.warn("tls.error.invalidHSType");
					return -1;
				}// End of main switch
			} // End of custom block
		} // End of FOR loop

		// -logger.info("Text : " +
		// Util.byteArrayToString(session.getDataText().getBytes()));

		if (tsiServer.getDecompresser() != null) {
			tsiServer.getDecompresser().end();
		}
		if (tsiClient.getDecompresser() != null) {
			tsiClient.getDecompresser().end();
		}
		if (tsiPending.getDecompresser() != null) {
			tsiPending.getDecompresser().end();
		}
		return 1;
	}

	private byte[] getRecord(Session session, MatchedRecord matchedRecord) {
		ByteBuffer pData = null;
		byte[] pOutput = null;
		switch (matchedRecord.getDir()) {
		case UPLINK:
			pData = ByteBuffer.wrap(session.getStorageUl());
			pOutput = new byte[session.getStorageUl().length - matchedRecord.getUniDirOffset()];
			for (int iTemp = 0; iTemp < matchedRecord.getUniDirOffset(); iTemp++) {
				pData.get();
			}
			pData.get(pOutput, 0, session.getStorageUl().length - matchedRecord.getUniDirOffset());
			break;

		case DOWNLINK:
			pData = ByteBuffer.wrap(session.getStorageDl());
			pOutput = new byte[session.getStorageDl().length - matchedRecord.getUniDirOffset()];
			for (int iTemp = 0; iTemp < matchedRecord.getUniDirOffset(); iTemp++) {
				pData.get();
			}
			pData.get(pOutput, 0, session.getStorageDl().length - matchedRecord.getUniDirOffset());
			break;

		default:
			return null;
		}
		return pOutput;
	}
	// end of parsing

	// TODOS generateRecords
	private void generateRecords(Session session, int protocol) {
		LOGGER.info(session.toString());
		List<MatchedRecord> mrList = session.getMrList();

		mrList.clear();
		if (session.getBdcRaw().size() == 0) {
			return;
		}

		Reassembler pBothStorage = null;
		if (protocol == PROT_RECORD_TLS) {
			pBothStorage = session.getpStorageBothRAW();
		} else {
			LOGGER.info("30001 - Unexpected protocol.");
		}

		// logger.info(session);
		matchRecords(session, protocol, pBothStorage, PacketDirection.UPLINK);
		LOGGER.info(session + "---Up");
		int upls = mrList.size();

		matchRecords(session, protocol, pBothStorage, PacketDirection.DOWNLINK);
		LOGGER.info(session + "---Down");
		int dnls = mrList.size();

		checkRecords(session);
		checkCompleteness(session, protocol, 0, upls - 1, PacketDirection.UPLINK);
		checkCompleteness(session, protocol, upls, dnls - 1, PacketDirection.DOWNLINK);
		Collections.sort(mrList);
		// TODOS verify mrList

	}

	private void checkCompleteness(Session session, int protocol, int nFrom, int nTo, PacketDirection dir) {
		String[] dirStr = { "", "UPLINK", "DOWNLINK" };
		String[] protStr = { "", "TLS", "", "SPDY_V2", "" };
		List<BidirDataChunk> bdcRaw = session.getBdcRaw();
		List<MatchedRecord> mrList = session.getMrList();

		int nTemp = bdcRaw.size();
		int iterPtr = 0;
		while (iterPtr < nTemp && bdcRaw.get(iterPtr).getDirection() != dir) {
			iterPtr++;
		}
		if (iterPtr == nTemp) {
			String msg = null;
			if (dir == PacketDirection.UPLINK) {
				msg = String.format("30005 - Raw data stream not observed in %s direction (prot: %s, localPort=%d)",
						dirStr[1], protStr[protocol], session.getLocalPort());
			} else {
				msg = String.format("30005 - Raw data stream not observed in %s direction (prot: %s, localPort=%d)",
						dirStr[2], protStr[protocol], session.getLocalPort());
			}
			LOGGER.info(msg);
			return;
		}

		if (nFrom > nTo) {
			/*
			 * UI::WarningMessage(
			 * "Record not observed in %s direction (prot: %s, localPort=%d)",
			 * dirStr[dir], protStr[prot], s.localPort);
			 */
			return;
		}

		if (mrList.get(nFrom).getBeginBDC() != iterPtr || mrList.get(nFrom).getBeginOfs() != 0) {
			String msg = null;
			if (dir == PacketDirection.UPLINK) {
				msg = String.format(
						"30006 - Records do not start from the beginning of the %s data stream (prot: %s, localPort=%d)",
						dirStr[1], protStr[protocol], session.getLocalPort());
			} else {
				msg = String.format(
						"30006 - Records do not start from the beginning of the %s data stream (prot: %s, localPort=%d)",
						dirStr[2], protStr[protocol], session.getLocalPort());
			}
			LOGGER.info(msg);
			return;
		}

		iterPtr = nTemp - 1;
		while (iterPtr >= 0 && bdcRaw.get(iterPtr).getDirection() != dir) {
			iterPtr--;
		}

		if (iterPtr < 0) {
			LOGGER.info("30007 - Error in checking completeness of SSL records.");
		}

		if (mrList.get(nTo).getEndBDC() != iterPtr
				|| mrList.get(nTo).getEndOfs() != bdcRaw.get(iterPtr).getnBytes() - 1) {
			String msg = null;
			if (dir == PacketDirection.UPLINK) {
				msg = String.format(
						"30008 - Records do not stop at the end of the %s data stream (prot: %s, localPort=%d)",
						dirStr[1], protStr[protocol], session.getLocalPort());
			} else {
				msg = String.format(
						"30008 - Records do not stop at the end of the %s data stream (prot: %s, localPort=%d)",
						dirStr[2], protStr[protocol], session.getLocalPort());
			}
			LOGGER.info(msg);
			return;
		}

		// test whether the records are consecutive
		for (iterPtr = nFrom + 1; iterPtr <= nTo; iterPtr++) {
			if (mrList.get(iterPtr - 1).getUniDirOffset() + mrList.get(iterPtr - 1).getBytes() != mrList.get(iterPtr)
					.getUniDirOffset()) {
				LOGGER.info("30009 - Error in checking completeness of SSL records.");
			}

			int bInOrder = 0;
			if (mrList.get(iterPtr - 1).getEndBDC() < mrList.get(iterPtr).getBeginBDC()) {
				bInOrder = 1;
			}
			if ((mrList.get(iterPtr - 1).getEndBDC() == mrList.get(iterPtr).getBeginBDC())
					&& (mrList.get(iterPtr - 1).getEndOfs() < mrList.get(iterPtr).getBeginOfs())) {
				bInOrder = 1;
			}

			if (bInOrder == 0) {
				LOGGER.info("30010 - Error in checking completeness of SSL records.");
			}
		}
	}

	private void checkRecords(Session session) {
		List<BidirDataChunk> bdcRaw = session.getBdcRaw();
		List<MatchedRecord> mrList = session.getMrList();

		int nTemp = bdcRaw.size();
		int mTemp = mrList.size();
		for (int iTemp = 0; iTemp < mTemp; iTemp++) {
			MatchedRecord matchedRecord = mrList.get(iTemp);
			boolean bPass = matchedRecord.getBeginBDC() >= 0 && matchedRecord.getBeginBDC() < nTemp
					&& matchedRecord.getEndBDC() >= 0 && matchedRecord.getEndBDC() < nTemp
					&& matchedRecord.getBeginOfs() >= 0
					&& matchedRecord.getBeginOfs() < bdcRaw.get(matchedRecord.getBeginBDC()).getnBytes()
					&& matchedRecord.getEndOfs() >= 0
					&& matchedRecord.getEndOfs() < bdcRaw.get(matchedRecord.getEndBDC()).getnBytes()
					&& matchedRecord.getDir() == bdcRaw.get(matchedRecord.getBeginBDC()).getDirection()
					&& matchedRecord.getDir() == bdcRaw.get(matchedRecord.getEndBDC()).getDirection()
					&& matchedRecord.getBytes() > 0
					&& (matchedRecord.getDir() == PacketDirection.UPLINK
							|| matchedRecord.getDir() == PacketDirection.DOWNLINK)
					&& (matchedRecord.getBeginBDC() < matchedRecord.getEndBDC()
							|| (matchedRecord.getBeginBDC() == matchedRecord.getEndBDC()
									&& matchedRecord.getBeginOfs() < matchedRecord.getEndOfs()));
			if (!bPass) {
				LOGGER.info("30004 - ssl record mismatch.");
			}
		}
	}

	private void matchRecords(Session session, int prot, Reassembler pBothStorage, PacketDirection dir) {

		List<BidirDataChunk> bdcRaw = session.getBdcRaw();
		List<MatchedRecord> mrList = session.getMrList();

		MatchedRecord matchedRecord = new MatchedRecord();

		// Step 1: find the first matched record
		// We assume the first matched record always at the BEGINNING of each
		// BDC chunk
		boolean bMatchedInitial = false;
		int nTemp = bdcRaw.size();
		int unidirOffset = 0;
		for (int iTemp = 0; iTemp < nTemp; iTemp++) {
			if (bdcRaw.get(iTemp).getDirection() != dir) {
				continue;
			}
			matchedRecord.setInput(iTemp, 0, dir, unidirOffset);
			if (matchNextRecord(session, prot, pBothStorage, null, matchedRecord)) {
				bMatchedInitial = true;
				mrList.add(matchedRecord);
				break;
			}
			unidirOffset += bdcRaw.get(iTemp).getnBytes();
		}
		if (!bMatchedInitial) {
			return;
		}

		// Step 2: keep matching
		while (true) {
			MatchedRecord mr2 = new MatchedRecord();
			int size = mrList.size();
			MatchedRecord back = null;
			if (size > 0) {
				back = mrList.get(size - 1);
			}
			if (matchNextRecord(session, prot, pBothStorage, back, mr2)) {
				mrList.add(mr2);
			} else {
				break;
			}
		}
	}

	private boolean matchNextRecord(Session session, int prot, Reassembler pBothStorage, MatchedRecord pPrevMR,
			MatchedRecord matchedRecord) {

		List<BidirDataChunk> bdcRaw = session.getBdcRaw();

		if (pPrevMR == null) {
			// first record
			if (prot == PROT_RECORD_TLS) {
				return matchRecordCore(session, PROT_RECORD_TLS_FIRST, pBothStorage, matchedRecord);
			} else {
				LOGGER.info("30002 - Unexpected protocol.");
				return false;
			}
		} else {
			Integer[] iTemp = new Integer[1];
			iTemp[0] = pPrevMR.getEndBDC();
			Integer[] jTemp = new Integer[1];
			jTemp[0] = pPrevMR.getEndOfs();

			getBytes(iTemp, jTemp, pBothStorage, bdcRaw, 1, null);

			matchedRecord.setInput(iTemp[0], jTemp[0], pPrevMR.getDir(),
					pPrevMR.getUniDirOffset() + pPrevMR.getBytes());
			return matchRecordCore(session, prot, pBothStorage, matchedRecord);
		}
	}

	private boolean matchRecordCore(Session session, int prot, Reassembler pBothStorage, MatchedRecord matchedRecord) {

		List<BidirDataChunk> bdcRaw = session.getBdcRaw();
		// List<MatchedRecord> mrList = session.getMrList();

		int nTemp = bdcRaw.size();
		Integer[] iTemp = new Integer[1];
		iTemp[0] = matchedRecord.getBeginBDC();
		Integer[] bBOfs = new Integer[1];
		bBOfs[0] = matchedRecord.getBeginOfs();
		PacketDirection pktDir = matchedRecord.getDir();

		if (iTemp[0] >= nTemp) {
			// already at the end of the entire stream, note (i_temp,j) =
			// (n_temp,0) is a valid position
			return false;
		}

		matchedRecord.setBytes(0);
		byte[] header = new byte[10];
		ByteBuffer pData = ByteBuffer.wrap(header);

		if (prot == PROT_RECORD_TLS || prot == PROT_RECORD_TLS_FIRST) {
			if (!getBytes(iTemp, bBOfs, pBothStorage, bdcRaw, 5, header)) {
				return false;
			}
			matchedRecord.setBytes(matchedRecord.getBytes() + 5);
			byte recType = pData.get(0);
			if (recType == TLS_RECORD_CHANGE_CIPHER_SPEC || recType == TLS_RECORD_ALERT
					|| recType == TLS_RECORD_HANDSHAKE || recType == TLS_RECORD_APP_DATA) {
				// pData.get(); //Moves by 1
				if (checkTLSVersion(pData.array(), 1) == 0) {
					return false;
				}
				int encRecPayloadSize = pData.getShort(3); // Reverse not
															// required as Java
															// follows big
															// endian byte order

				// additional check for the first record
				// we are confident it's a Client/Server Hello by checking the
				// first 6 bytes
				if (prot == PROT_RECORD_TLS_FIRST) {
					if (pktDir == PacketDirection.UPLINK) { // must be Client
															// Hello
						if (recType != TLS_RECORD_HANDSHAKE) {
							return false;
						}
						if (encRecPayloadSize < 4 + 2 + 32 + 1) {
							return false;
						}
						if (!getBytes(iTemp, bBOfs, pBothStorage, bdcRaw, 6, header)) {
							return false;
						}
						if (pData.get(0) != TLS_HANDSHAKE_CLIENT_HELLO) {
							return false; // 1 byte
						}
					} else { // must be Server hello
						if (recType != TLS_RECORD_HANDSHAKE) {
							return false;
						}
						if (encRecPayloadSize < 4 + 2 + 32 + 3 + 1) {
							return false;
						}
						if (!getBytes(iTemp, bBOfs, pBothStorage, bdcRaw, 6, header)) {
							return false;
						}
						if (pData.get(0) != TLS_HANDSHAKE_SERVER_HELLO) {
							return false; // 1 byte
						}
					}
					int hsPayloadSize = read24bitInteger(pData.array(), 1); // 3
																			// bytes
					if (hsPayloadSize + 4 > encRecPayloadSize) {
						return false;
					}
					// pData.get(); //Moves by 1
					// pData.get(); //Moves by 1
					// pData.get(); //Moves by 1
					if (checkTLSVersion(pData.array(), 4) == 0) {
						return false; // 2 bytes
					}
					if (!getBytes(iTemp, bBOfs, pBothStorage, bdcRaw, encRecPayloadSize - 6, null)) {
						return false;
					}
				} else {
					if (!getBytes(iTemp, bBOfs, pBothStorage, bdcRaw, encRecPayloadSize, null)) {
						return false;
					}
				}
				matchedRecord.setBytes(matchedRecord.getBytes() + encRecPayloadSize);
			} else {
				return false;
			}
		} else {
			LOGGER.info("30003 - Unexpected protocol.");
		}

		// go back by one byte
		if (--bBOfs[0] == -1) {
			while (true) {
				iTemp[0]--;
				if (iTemp[0] < 0 || bdcRaw.get(iTemp[0]).getDirection() == pktDir) {
					break;
				}
			}
			bBOfs[0] = bdcRaw.get(iTemp[0]).getnBytes() - 1;
		}

		matchedRecord.setEndBDC(iTemp[0]);
		matchedRecord.setEndOfs(bBOfs[0]);
		return true;
	}

	private int read24bitInteger(byte pData[], int temp) {
		byte[] tmp = new byte[4];
		tmp[3] = pData[temp + 2];
		tmp[2] = pData[temp + 1];
		tmp[1] = pData[temp];
		tmp[0] = 0;
		return ByteBuffer.wrap(tmp).getInt();
	}

	private int checkTLSVersion(byte[] pData, int temp) {
		if ((pData[temp] != 0x03) || ((pData[temp + 1]) != 0x01)) {
			return 0;
		} else {
			return 1;
		}
	}

	private boolean getBytes(Integer[] bdcID, Integer[] offset, Reassembler pBothStorage, List<BidirDataChunk> bdc,
			int nBytesIn, byte pBuffer[]) {

		int nBytes = nBytesIn;

		int bdcSize = bdc.size();
		int bdcIDZeroObj = bdcID[0];
		Integer offsetZero = offset[0];

		if (bdcIDZeroObj >= bdcSize) {
			// already at the end of the entire stream, note (i_temp,j) =
			// (n_temp,0) is a valid position
			return false;
		}

		PacketDirection dir = bdc.get(bdcIDZeroObj).getDirection();
		while (true) {
			int availBytes = bdc.get(bdcIDZeroObj).getnBytes() - offsetZero;
			if (availBytes >= nBytes) {
				if (pBuffer != null) {
					System.arraycopy(pBothStorage.getStorage().toByteArray(),
							bdc.get(bdcIDZeroObj).getnPrevBytes() + offsetZero, pBuffer, 0, nBytes); // http://stackoverflow.com/questions/3329163/is-there-an-equivalent-to-memcpy-in-java
				}
				offsetZero += nBytes;
				if (offsetZero == bdc.get(bdcIDZeroObj).getnBytes()) {
					// end of current chunk, move to the next chunk with the
					// same direction
					offsetZero = 0;
					while (true) {
						bdcIDZeroObj++;
						if ((bdcIDZeroObj >= bdcSize) || (bdc.get(bdcIDZeroObj).getDirection() == dir)) {
							break;
						}
					}
				}
				bdcID[0] = bdcIDZeroObj;
				offset[0] = offsetZero;
				return true;
			} else {
				// move to the next chunk with the same direction
				if (pBuffer != null) {
					System.arraycopy(pBothStorage.getStorage().toByteArray(),
							bdc.get(bdcIDZeroObj).getnPrevBytes() + offsetZero, pBuffer, 0, availBytes); // http://stackoverflow.com/questions/3329163/is-there-an-equivalent-to-memcpy-in-java
				}
				nBytes -= availBytes;
				while (true) {
					bdcIDZeroObj++;
					if ((bdcIDZeroObj >= bdcSize) || (bdc.get(bdcIDZeroObj).getDirection() == dir)) {
						break;
					}
				}
				if (bdcIDZeroObj >= bdcSize) {
					return false;
				} else {
					offsetZero = 0;
				}
			}
		}
	}
	// End of generateRecords
	// end of ssl

	private void analyzeRequestResponseInfo(List<Session> sessions) {
		for (Session session : sessions) {
			for (PacketInfo sPacket : session.getPackets()) {
				session.setBytesTransferred(session.getBytesTransferred() + sPacket.getLen());
			}
			analyzeACK(session);
			analyzeZeroWindow(session);
			analyzeRecoverPkts(session);

			// Parse HTTP request response info
			try {
				session.setRequestResponseInfo(requestResponseBuilder.createRequestResponseInfo(session));
			} catch (IOException exe) {
				LOGGER.warn("Error create RequestResponseInfo", exe);
			}
			for (HttpRequestResponseInfo rrinfo : session.getRequestResponseInfo()) {
				if (rrinfo.getDirection() == HttpDirection.REQUEST) {

					// Assume first host found is same for entire session
					if (session.getDomainName() == null) {
						String host = rrinfo.getHostName();
						if (host != null) {
							URI referrer = rrinfo.getReferrer();
							session.setRemoteHostName(host);
							session.setDomainName(referrer != null ? referrer.getHost() : host);
						}
					}
				} else if (rrinfo.getDirection() == HttpDirection.RESPONSE && rrinfo.getContentLength() > 0) {
					session.setFileDownloadCount(session.getFileDownloadCount() + 1);

				}
			}
			if (session.getDomainName() == null) {
				session.setDomainName(session.getRemoteHostName() != null ? session.getRemoteHostName()
						: session.getRemoteIP().getHostAddress());
			}
		}
	}

	private void reassembleSessions(List<Session> sessions, Reassembler upl, Reassembler dol) {
		for (int sessionIndex = 0; sessionIndex < sessions.size(); ++sessionIndex) {

			// Iterator is not used because items may be added to list during.
			// iterations
			Session pSes = sessions.get(sessionIndex);
			// Reset variables
			boolean bTerminated = false;
			upl.clear();
			dol.clear();
			// logger.debug("Session index: "+sessionIndex+" has packets:
			// "+pSes.getPackets().size());
			PacketInfo lastPacket = null;
			for (PacketInfo packetInfo : pSes.getPackets()) {
				TCPPacket pac = (TCPPacket) packetInfo.getPacket();
				if(pac.isDecrypted()) {
					pSes.setDecrypted(true);
				}
			}
			for (PacketInfo packetInfo : pSes.getPackets()) {

				TCPPacket pac = (TCPPacket) packetInfo.getPacket();

				pSes.setSsl(pac.isSsl());
				Reassembler reassembledSession;
				switch (packetInfo.getDir()) {
				case UPLINK:
					reassembledSession = upl;
					break;

				case DOWNLINK:
					reassembledSession = dol;
					break;

				default:
					LOGGER.warn("91 - No direction for packet");
					continue;
				}

				// If this is the initial sequence number
				if (pac.isSYN()) {
					packetInfo.setTcpInfo(TcpInfo.TCP_ESTABLISH);
					if (reassembledSession.getBaseSeq() == null
							|| reassembledSession.getBaseSeq().equals(pac.getSequenceNumber())) {
						// Finds establish
						reassembledSession.setBaseSeq(pac.getSequenceNumber());
						if (pac.getPayloadLen() != 0) {
							LOGGER.warn("92 - Payload in establish packet");
						}
					} else {

						// New TCP session
						List<PacketInfo> currentList = pSes.getPackets();
						int index = currentList.indexOf(packetInfo);
						if (!bTerminated) {
							LOGGER.debug("28 - Session termination not found");
						}

						// Correct packet list in original session
						pSes.setPackets(new ArrayList<PacketInfo>(currentList.subList(0, index)));

						// Create new session for remaining packets
						Session newSession = new Session(pSes.getRemoteIP(), pSes.getRemotePort(), pSes.getLocalPort());
						newSession.getPackets().addAll(currentList.subList(index, currentList.size()));
						sessions.add(newSession);

						// Break out of packet loop
						break;
					}

				} else {
					// FIN: No more data from sender
					// RST: Reset the connection
					if (pac.isFIN() || pac.isRST()) {
						// Calculate session termination info
						if (!bTerminated && lastPacket != null) {
							double delay = packetInfo.getTimeStamp() - lastPacket.getTimeStamp();
							pSes.setSessionTermination(new Termination(packetInfo, delay));
						}

						// Mark session terminated
						bTerminated = true;
						if (pac.isFIN()) {
							packetInfo.setTcpInfo(TcpInfo.TCP_CLOSE);
						} else if (pac.isRST()) {
							packetInfo.setTcpInfo(TcpInfo.TCP_RESET);
						}

					}

					// I believe this handles case where we have joined in the
					// middle of a TCP session
					if (reassembledSession.getBaseSeq() == null) {
						switch (packetInfo.getDir()) {
						case UPLINK:
							upl.setBaseSeq(pac.getSequenceNumber());
							dol.setBaseSeq(pac.getAckNumber());
							break;
						case DOWNLINK:
							dol.setBaseSeq(pac.getSequenceNumber());
							upl.setBaseSeq(pac.getAckNumber());
							break;
						default:
							LOGGER.warn("Invalid packet direction");
						}
					}
				}

				// Get appName (there really should be only one per TCP session
				String appName = packetInfo.getAppName();
				if (appName != null) {
					pSes.getAppNames().add(appName);
				}

				long seqn = pac.getSequenceNumber() - reassembledSession.getBaseSeq();
				if (seqn < 0) {
					seqn += 0xFFFFFFFF;
					seqn++;
				}
				long seq = seqn;

				if (reassembledSession.getSeq() == -1) {
					reassembledSession.setSeq(seqn);
				}

				if (seqn == reassembledSession.getSeq()) {

					if (seq == reassembledSession.getSeq() || (seq < reassembledSession.getSeq()
							&& seq + pac.getPayloadLen() > reassembledSession.getSeq())) {
						reassembledSession = reAssembleSession(pac, packetInfo, reassembledSession, pSes);
					}

					// -logger.info("[1] "+reassembledSession);

					while (true) {
						boolean bOODone = true;
						List<PacketInfo> fixed = new ArrayList<PacketInfo>(reassembledSession.getOoid().size());
						for (PacketInfo pin1 : reassembledSession.getOoid()) {
							TCPPacket tPacket1 = (TCPPacket) pin1.getPacket();

							seqn = tPacket1.getSequenceNumber() - reassembledSession.getBaseSeq();
							if (seqn < 0) {
								seqn += 0xFFFFFFFF;
								seqn++;
							}

							long seq2 = seqn;

							if (seqn == reassembledSession.getSeq()) {

								if (seq2 == reassembledSession.getSeq() || (seq2 < reassembledSession.getSeq()
										&& seq2 + pac.getPayloadLen() > reassembledSession.getSeq())) {
									reassembledSession = reAssembleSession(tPacket1, pin1, reassembledSession, pSes);
								}

								// -logger.info("_2_ "+reassembledSession);

								fixed.add(pin1);
								bOODone = false;
							} else if (tPacket1.getPayloadLen() == 0 && seqn == reassembledSession.getSeq() - 1
									&& tPacket1.isACK() && !tPacket1.isSYN() && !tPacket1.isFIN()
									&& !tPacket1.isRST()) {
								LOGGER.warn("31 - ???");
							}
						}
						reassembledSession.getOoid().removeAll(fixed);
						if (bOODone) {
							break;
						}
					} // end while true

					// -logger.info(" f "+reassembledSession);

				} else { // out of order packet, i_temp.e., seq != *XLseq
					if(pac.isDecrypted()) {
						reassembledSession = reAssembleSession(pac, packetInfo, reassembledSession, pSes);
					}
					if (pac.getPayloadLen() == 0 && seqn == reassembledSession.getSeq() - 1 && pac.isACK()
							&& !pac.isSYN() && !pac.isFIN() && !pac.isRST()) {
						packetInfo.setTcpInfo(TcpInfo.TCP_KEEP_ALIVE);
					} else {
						reassembledSession.getOoid().add(packetInfo);
					}
				}

				lastPacket = packetInfo;
			} // packet loop

			pSes.setStorageDl(dol.getStorage().toByteArray());
			pSes.setPacketOffsetsDl(dol.getPacketOffsets());
			pSes.setPktRangesDl(dol.getPktRanges());
			pSes.setStorageUl(upl.getStorage().toByteArray());
			pSes.setPacketOffsetsUl(upl.getPacketOffsets());
			pSes.setPktRangesUl(upl.getPktRanges());

			for (PacketInfo pinfo : dol.getOoid()) {
				if (pinfo.getPacket().getPayloadLen() > 0) {
					pinfo.setTcpInfo(TcpInfo.TCP_DATA_DUP);
				}
			}

			for (PacketInfo pinfo : upl.getOoid()) {
				if (pinfo.getPacket().getPayloadLen() > 0) {
					pinfo.setTcpInfo(TcpInfo.TCP_DATA_DUP);
				}
			}
		} // END: Reassemble sessions
			// -logger.info("END: Reassemble sessions");
	}

	private void iteratePackets(List<PacketInfo> packets, Map<String, Session> allSessions, List<PacketInfo> dnsPackets,
			List<PacketInfo> udpPackets, Map<InetAddress, String> hostMap) {

		LOGGER.debug("looping thru packets info list, total packets: " + (packets != null ? packets.size() : "null"));

		if (packets != null) {
			for (PacketInfo packet : packets) {
				/**
				 * Save DNS packets for the later verifying Domain name 
				 */
				if (!(packet.getPacket() instanceof TCPPacket)) {
					// Check for DNS packets
					if (packet.getPacket() instanceof UDPPacket) {
						UDPPacket udp = (UDPPacket) packet.getPacket();
						udpPackets.add(packet);
						if (udp.isDNSPacket()) {
							dnsPackets.add(packet);
							DomainNameSystem dns = udp.getDns();
							if (dns != null && dns.isResponse()) {
								for (InetAddress inet : dns.getIpAddresses()) {
									hostMap.put(inet, dns.getDomainName());
								}
							}
						}
					}
					continue;
				}

				/**
				 * Set localPort, remoteIP, remotePort using TCP packet data
				 * information
				 */
				TCPPacket tcp = (TCPPacket) packet.getPacket();
				int localPort;
				int remotePort;
				InetAddress remoteIP;
				switch (packet.getDir()) {
				case UPLINK:
					localPort = tcp.getSourcePort();
					remoteIP = tcp.getDestinationIPAddress();
					remotePort = tcp.getDestinationPort();
					break;

				case DOWNLINK:
					localPort = tcp.getDestinationPort();
					remoteIP = tcp.getSourceIPAddress();
					remotePort = tcp.getSourcePort();
					break;

				default:
					LOGGER.warn("29 - Unable to determine packet direction");
					continue;
				}

				// Clear TCP Info
				packet.setTcpInfo(null);

				/**
				 * Creates a new TCP Session using remoteIP, remotePort,
				 * localPort. Stores the session in allSessions Collection and
				 * adds the current packet to the session.
				 */
				String key = localPort + " " + remotePort + " " + remoteIP.getHostAddress();
				Session session = allSessions.get(key);
				if (session == null) {
					session = new Session(remoteIP, remotePort, localPort);

					/**
					 * Search for DNS request/response from the last to the
					 * first element and saves dnsResponsePacket in the current
					 * session if the DNS response remote IP matches the session
					 * remote IP. Finds DNS response packet.
					 */
					ListIterator<PacketInfo> iter = dnsPackets.listIterator(dnsPackets.size());
					DomainNameSystem dns = null;
					while (iter.hasPrevious()) {
						PacketInfo dnsPacket = iter.previous();
						UDPPacket udp = ((UDPPacket) dnsPacket.getPacket());
						dns = udp.getDns();
						if (dns != null && dns.isResponse() && dns.getIpAddresses().contains(remoteIP)) {
							session.setDnsResponsePacket(dnsPacket);
							break;
						}
						dns = null;
					}

					// If DNS response packet was found
					if (dns != null) {
						/**
						 * Loop through all DNS packets to find DNS packet
						 * matching the domain name of the response DNS packet.
						 * Then store remoteHostName and the DNS request packet
						 * in the current session.
						 */

						iter = dnsPackets.listIterator();
						String domainName = dns.getDomainName();
						while (iter.hasNext()) {
							PacketInfo pac = iter.next();
							UDPPacket udp = ((UDPPacket) pac.getPacket());
							dns = udp.getDns();
							if (dns != null && domainName.equals(dns.getDomainName())) {
								if (session.getDnsRequestPacket() == null && !dns.isResponse()) {
									session.setRemoteHostName(domainName);
									session.setDnsRequestPacket(pac);
								}

								// Remove from DNS packets so that it is not used again
								iter.remove();
								// Stop processing once response is reached
								if (pac == session.getDnsResponsePacket()) {
									break;
								}
							}
						}
					} else {
						session.setRemoteHostName(hostMap.get(remoteIP));
					}
					// stores the created session
					allSessions.put(key, session);
				} // END: Create new session

				session.getPackets().add(packet);
				// session.getPktIndex().add(packetIndex++);
			} // END: Iterating through all packets

		}
	}

	private Reassembler reAssembleSession(TCPPacket pac, PacketInfo packetInfo, Reassembler reAsmSession,
			Session session) {

		if (pac.getPayloadLen() > 0) {
			packetInfo.setTcpInfo(TcpInfo.TCP_DATA);
			byte[] data = pac.getData();
			int effectivePayloadLen = pac.getPayloadLen();
			int dataOffset = pac.getDataOffset();

			if (data.length >= dataOffset + effectivePayloadLen || pac.isDecrypted()) {
				reAsmSession.getPacketOffsets().put(reAsmSession.getStorage().size(), packetInfo);
				if(session.isDecrypted()) {
					if(pac.isDecrypted()) {
						reAsmSession.getStorage().write(data, 0, data.length);
					}
				} else {
					reAsmSession.getStorage().write(data, dataOffset, effectivePayloadLen);
				}
				int offset = reAsmSession.getStorage().size() - effectivePayloadLen;
				if (reAsmSession.getPktRanges().size() == 0) {
					offset = 0;
				}
				reAsmSession.getPktRanges()
						.add(new PacketRangeInStorage(offset, effectivePayloadLen, packetInfo.getPacketId()));
				if (session.isDecrypted()) {
					if(pac.isDecrypted()) {
						session.getpStorageBothRAW().getStorage().write(data, 0, data.length);
					}
				} else {
					session.getpStorageBothRAW().getStorage().write(data, dataOffset, effectivePayloadLen);
				}

				// -logger.info("packet: " + packetInfo.getTimeStamp());
				updateBDC(session, packetInfo.getDir(), effectivePayloadLen);
				reAsmSession.setSeq(reAsmSession.getSeq() + effectivePayloadLen);
			}

			if (pac.isSslHandshake()) {
				session.setLastSslHandshakePacket(packetInfo);
			}

		}
		if (pac.isSYN() || pac.isFIN()) {
			reAsmSession.incrementSeq();
		}
		return reAsmSession;
	}

	private void updateBDC(Session session, PacketDirection dir, int payloadLen) {

		List<BidirDataChunk> bdcRaw = session.getBdcRaw();
		int bdcRawSize = bdcRaw.size();
		BidirDataChunk back = null;

		if (!bdcRaw.isEmpty()) {
			back = bdcRaw.get(bdcRawSize - 1);
			if (bdcRaw.isEmpty() || back.getDirection() != dir) {
				BidirDataChunk dataChunk = new BidirDataChunk();
				dataChunk.setnBytes(payloadLen);
				dataChunk.setDirection(dir);
				dataChunk.setnPrevBytes(bdcRaw.isEmpty() ? 0 : (back.getnPrevBytes() + back.getnBytes()));
				// -logger.info("add Session:" + session.getSessionStartTime());
				bdcRaw.add(dataChunk);
			} else {
				back.setnBytes(back.getnBytes() + payloadLen);
			}
		}
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
				LOGGER.warn("97 - No direction for packet");
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
				if (pinfo.getTcpInfo() != null) {
					LOGGER.warn("34 - Packet already typed");
				}
				pinfo.setTcpInfo(TcpInfo.TCP_KEEP_ALIVE);
			} else if (!pAckWinSize.containsKey(key)) {
				pAckWinSize.put(key, win);
				if (payloadLen == 0 && !pack.isSYN() && !pack.isFIN() && !pack.isRST()) {
					if (pinfo.getTcpInfo() != null) {
						LOGGER.warn("98 - Packet already typed");
					}
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
			if (tPacket.getPayloadLen() == 0 && tPacket.getWindow() == 0 && !tPacket.isSYN() && !tPacket.isFIN()
					&& !tPacket.isRST()) {
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

	/**
	 * Get the UDP sessions include from DNS server.
	 * 
	 * @return Collection of UDP sessions
	 */
	private List<Session> getUDPSessions(List<PacketInfo> udpPackets, List<Session> sessions) throws IOException {

		Map<String, Session> allUDPSessions = new LinkedHashMap<String, Session>();
		DomainNameSystem dns = null;
		Reassembler rAssemblerUL = new Reassembler();
		Reassembler rAssemblerDL = new Reassembler();

		/*
		 * Create a UDP session for those UDP packets which are not associated
		 * with TCP connection
		 */
		for (PacketInfo packet : udpPackets) {
			UDPPacket udp = (UDPPacket) packet.getPacket();
			int localPort;
			int remotePort;
			InetAddress remoteIP;
			switch (packet.getDir()) {
			case UPLINK:
				localPort = udp.getSourcePort();
				remoteIP = udp.getDestinationIPAddress();
				remotePort = udp.getDestinationPort();
				break;
			case DOWNLINK:
				localPort = udp.getDestinationPort();
				remoteIP = udp.getSourceIPAddress();
				remotePort = udp.getSourcePort();
				break;
			default:
				LOGGER.warn("29 - Unable to determine packet direction");
				continue;
			}
			String key = localPort + " " + remotePort + " " + remoteIP.getHostAddress();
			Session session = allUDPSessions.get(key);
			if (session == null) {
				session = new Session(remoteIP, remotePort, localPort);
				if (udp.isDNSPacket()) {
					dns = udp.getDns();
					if (dns != null) {
						// session.domainName = dns.getDomainName();
						session.setRemoteHostName(dns.getDomainName());

					}
				}
				if (session.getRemoteHostName() == null) {
					session.setRemoteHostName(session.getRemoteIP().getHostAddress());
				}
				session.setUdpOnly(true);
				/* stores the created session */
				allUDPSessions.put(key, session);
			} // END: Create new session
			session.getUDPPackets().add(packet);
		}
		
		List<Session> udpSessions = new ArrayList<Session>(allUDPSessions.values());

		try{
			for (int sessionIndex = 0; sessionIndex < udpSessions.size(); ++sessionIndex) {
	
				Session session = udpSessions.get(sessionIndex);
				rAssemblerUL.clear();
				rAssemblerDL.clear();
				for (PacketInfo packetInfo : session.getUDPPackets()) {
					UDPPacket packet = (UDPPacket) packetInfo.getPacket();
	
					Reassembler reassembledSession;
					switch (packetInfo.getDir()) {
					case UPLINK:
						reassembledSession = rAssemblerUL;
						break;
	
					case DOWNLINK:
						reassembledSession = rAssemblerDL;
						break;
	
					default:
						LOGGER.warn("91 - No direction for packet");
						continue;
					}
					if (packet.getPayloadLen() > 0) {
	
						byte[] data = packet.getData();
						int packetLen = packet.getPayloadLen();
						int dataOffset = packet.getDataOffset();
						if (data.length >= dataOffset + packetLen) {
							reassembledSession.getPacketOffsets().put(reassembledSession.getStorage().size(), packetInfo);
							reassembledSession.getStorage().write(data, dataOffset, packetLen);
						}
					}
	
					// Added to find UDP packet bytes transfered
					session.setBytesTransferred(session.getBytesTransferred() + packet.getPayloadLen());
	
				}
				session.setStorageDl(rAssemblerDL.getStorage().toByteArray());
				session.setPacketOffsetsDl(rAssemblerDL.getPacketOffsets());
				session.setStorageUl(rAssemblerUL.getStorage().toByteArray());
				session.setPacketOffsetsUl(rAssemblerUL.getPacketOffsets());
	
			}
			for (Session sess : udpSessions) {
				sess.setRequestResponseInfo(this.requestResponseBuilder.createRequestResponseInfo(sess));
	
				for (HttpRequestResponseInfo rrHttp : sess.getRequestResponseInfo()) {
					if ((rrHttp.getDirection() == HttpDirection.REQUEST)
							&& (sess.getDomainName() == null && rrHttp.getHostName() != null)) {
	
						// Assume first host found is same for entire session
						// if (sess.getDomainName() == null && rrHttp.getHostName()
						// != null) {
						String host = rrHttp.getHostName();
						URI referrer = rrHttp.getReferrer();
						sess.setRemoteHostName(host);
						sess.setDomainName(referrer != null ? referrer.getHost() : host);
	
						// }
					}
				}
				if (sess.getDomainName() == null) {
					sess.setDomainName(sess.getRemoteHostName() != null ? sess.getRemoteHostName()
							: sess.getRemoteIP().getHostAddress());
				}
	
			}
		} finally {
			rAssemblerUL.clear();
			rAssemblerDL.clear();
		}
		return new ArrayList<Session>(allUDPSessions.values());
	}
}// end class
