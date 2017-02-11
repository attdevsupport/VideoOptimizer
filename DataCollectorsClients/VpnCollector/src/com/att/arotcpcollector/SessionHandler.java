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

package com.att.arotcpcollector;

import java.io.IOException;
import java.util.Date;

import com.att.arotcpcollector.ip.IPPacketFactory;
import com.att.arotcpcollector.ip.IPv4Header;
import com.att.arotcpcollector.socket.SocketData;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.tcp.TCPPacketFactory;
import com.att.arotcpcollector.udp.UDPHeader;
import com.att.arotcpcollector.udp.UDPPacketFactory;
import com.att.arotcpcollector.util.PacketUtil;

import android.content.Context;
import android.util.Log;

/**
 * handle VPN client request and response. 
 * it creates a new session for each VPN
 * client.
 * 
 * Date: May 22, 2014
 */
public class SessionHandler {
	public static final String TAG = "SessionHandler";

	private static Object synObject = new Object();
	private static volatile SessionHandler handler = null;
	private SessionManager sessionManager;
	private IClientPacketWriter clientWriter;
	private TCPPacketFactory tcpFactory;
	private UDPPacketFactory udpFactory;
	private SocketData pcapData = null; // for traffic.cap
	private String[] whitelist;
	private Context ctx;

	public static SessionHandler getInstance() throws IOException {
		if (handler == null) {
			synchronized (synObject) {
				if (handler == null) {
					handler = new SessionHandler();
				}
			}
		}
		return handler;
	}

	private SessionHandler() throws IOException {
		sessionManager = SessionManager.getInstance();
		tcpFactory = new TCPPacketFactory();
		udpFactory = new UDPPacketFactory();
		pcapData = SocketData.getInstance();

		//TODO: remove this after debugging
	//	whitelist = new String[] { "74.125.129.102", "208.109.186.6", "206.188.33.238", "216.186.48.6", "66.225.14.170" };
	}

	/**
	 * @param clientWriter
	 */
	public void setClientWriter(IClientPacketWriter clientWriter) {
		this.clientWriter = clientWriter;
	}

	public Context getAndroidContext() {
		return ctx;
	}

	public void setAndroidContext(Context Ctx) {
		this.ctx = Ctx;
	}
	
	/**
	 * Forward on a UDP packet.
	 * 
	 * @param clientpacketdata
	 * @param ipheader
	 * @param udpheader
	 */
	private void handleUDPPacket(byte[] clientpacketdata, IPv4Header ipheader, UDPHeader udpheader) {
//		FileUtil.print("Forward on a UDP packet: " , clientpacketdata);

		Session session = sessionManager.getSession(ipheader.getDestinationIP(), udpheader.getDestinationPort(), 
				ipheader.getSourceIP(), udpheader.getSourcePort());
		
		if (session == null) {
			session = sessionManager.createNewUDPSession(ipheader.getDestinationIP(), udpheader.getDestinationPort(), 
					ipheader.getSourceIP(), udpheader.getSourcePort());
		}
		
		if (session == null) {
			return;
		}
		
		session.setLastIPheader(ipheader);
		session.setLastUDPheader(udpheader);
		int len = sessionManager.addClientUDPData(ipheader, udpheader, clientpacketdata, session);
		session.setDataForSendingReady(true);
		Log.d(TAG, "added UDP data for bg worker to send: " + len);
		sessionManager.keepSessionAlive(session);
	}

	/**
	 * From client to web
	 * outgoing TCP packets
	 * Conditions to handle
	 * tcpheader.isSYN()
	 * tcpheader.isACK()
	 * tcpheader.isFIN()
	 * tcpheader.isRST()
	 * 
	 * @param clientpacketdata
	 * @param ipheader
	 * @param tcpheader
	 */
	private void handleTCPPacket(byte[] clientpacketdata, IPv4Header ipheader, TCPHeader tcpheader) {
		int length = clientpacketdata.length;
		int datalength = length - ipheader.getIPHeaderLength() - tcpheader.getTCPHeaderLength();
	
		if (tcpheader.isSYN()) {
			//3-way handshake + create new session
			//set windows size and scale, set reply time in options
			replySynAck(ipheader, tcpheader);

		} else if (tcpheader.isACK()) {
			Session session = sessionManager.getSession(ipheader.getDestinationIP(), tcpheader.getDestinationPort(), 
					ipheader.getSourceIP(), tcpheader.getSourcePort());

			if (session == null) {
				Log.d(TAG,
						"**** ==> Session not found: " + PacketUtil.intToIPAddress(ipheader.getDestinationIP()) + ":" + tcpheader.getDestinationPort() + "-"
								+ PacketUtil.intToIPAddress(ipheader.getSourceIP()) + ":" + tcpheader.getSourcePort());
				if (!tcpheader.isRST() && !tcpheader.isFIN()) {
					this.sendRstPacket(ipheader, tcpheader, datalength);
				}
				return;
			}

			// any data from client?
			if (datalength > 0) {

				// accumulate data from client
				int totalAdded = sessionManager.addClientData(ipheader, tcpheader, clientpacketdata);
				
				if (totalAdded > 0) {
					byte[] clientdata = new byte[totalAdded];
					int offset = ipheader.getIPHeaderLength() + tcpheader.getTCPHeaderLength();
					System.arraycopy(clientpacketdata, offset, clientdata, 0, totalAdded);

					//send ack to client only if new data was added
					sendAckToClient(ipheader, tcpheader, totalAdded, session);
				}
			} else {
				// an ack from client for previously sent data
				acceptAck(ipheader, tcpheader, session);
				
				/*  
				 *  TODO: Doesnt Look Like the If Condition ever turns true.
				 */
				if (session.isClosingConnection()) {
					sendFinAck(ipheader, tcpheader, session);
				} else if (session.isAckedToFin() && !tcpheader.isFIN()) {
					//the last ACK from client after FIN-ACK flag was sent
					sessionManager.closeSession(ipheader.getDestinationIP(), tcpheader.getDestinationPort(), 
							ipheader.getSourceIP(), tcpheader.getSourcePort());
					Log.d(TAG, "got last ACK after FIN, session is now closed.");
				}
			}
			
			// Fix: Added to avoid null pointers during Read TCP Send Fin
			session.setLastIPheader(ipheader);
			session.setLastTCPheader(tcpheader);
			
			//received the last segment of data from vpn client
			if (tcpheader.isPSH()) {
				//push data to destination here. Background thread will receive data and fill session's buffer.
				//Background thread will send packet to client
				pushDataToDestination(session, ipheader, tcpheader);
			} else if (tcpheader.isFIN()) {
				//fin from vpn client is the last packet
				//ack it
				Log.d(TAG, "FIN from vpn client, will ack it.");
				ackFinAck(ipheader, tcpheader, session);
			} else if (tcpheader.isRST()) {
				resetConnection(ipheader, tcpheader);
			}
			if (session != null && !session.isClientWindowFull() && !session.isAbortingConnection()) {
				sessionManager.keepSessionAlive(session);
			}

		} else if (tcpheader.isFIN()) {
			//case client sent FIN without ACK
			Session session = sessionManager.getSession(ipheader.getDestinationIP(), tcpheader.getDestinationPort(), 
					ipheader.getSourceIP(), tcpheader.getSourcePort());
			if (session == null) {
				ackFinAck(ipheader, tcpheader, session);
			} else {
				sessionManager.keepSessionAlive(session);
			}
		} else if (tcpheader.isRST()) {
			Log.d(TAG,
					"**** Reset client connection for dest: " + PacketUtil.intToIPAddress(ipheader.getDestinationIP()) + ":" + tcpheader.getDestinationPort() + "-"
							+ PacketUtil.intToIPAddress(ipheader.getSourceIP()) + ":" + tcpheader.getSourcePort());
			resetConnection(ipheader, tcpheader);
		}else{
        	Log.d(TAG,"unknown TCP flag");
        	String str1 = PacketUtil.getOutput(ipheader, tcpheader, clientpacketdata);
            Log.d(TAG,">>>>>>>> Received from client <<<<<<<<<<");
            Log.d(TAG,str1);
            Log.d(TAG,">>>>>>>>>>>>>>>>>>>end receiving from client>>>>>>>>>>>>>>>>>>>>>");
		}		

	}

	/**
	 * <pre>
	 * All packets intercepted by VPN from each android (client) app come here.
	 * Sessions are created when new or packets are assigned to existing sessions when matches with existing session.
	 * </pre>
	 * 
	 * @param data
	 * @param length
	 * @throws PacketHeaderException
	 */
	public void handlePacket(byte[] data, int length) throws PacketHeaderException {		
		byte[] clientPacketData = new byte[length];
		System.arraycopy(data, 0, clientPacketData, 0, length);
		pcapData.addData(clientPacketData); // send copy to traffic.cap
		
		IPv4Header ipHeader = IPPacketFactory.createIPv4Header(clientPacketData, 0);
		
		// Only support IPv4 with TCP and UDP protocol
		// Everything else dropped ?
		if (ipHeader.getIpVersion() != 4 || (ipHeader.getProtocol() != 6 && ipHeader.getProtocol() != 17)) {
			
			if (ipHeader.getIpVersion() != 4) {
				Log.e(TAG, "********===> Unsupported IP Version: " + ipHeader.getIpVersion());
			}
			if (ipHeader.getProtocol() != 6 && ipHeader.getProtocol() != 17) {
				Log.e(TAG, "******===> Unsupported protocol: " + ipHeader.getProtocol());
			}
			return;
		}
		
		UDPHeader udpHeader = null;
		TCPHeader tcpHeader = null;
		
		if (ipHeader.getProtocol() == 6) {
			tcpHeader = tcpFactory.createTCPHeader(clientPacketData, ipHeader.getIPHeaderLength());
		} else {
			udpHeader = udpFactory.createUDPHeader(clientPacketData, ipHeader.getIPHeaderLength());
		}

		if (tcpHeader != null) {
			handleTCPPacket(clientPacketData, ipHeader, tcpHeader);
		} else if (udpHeader != null) {
			handleUDPPacket(clientPacketData, ipHeader, udpHeader);
		}
	}
	
//	whitelist = new String[] {"74.125.129.102","208.109.186.6","206.188.33.238",
//			"216.186.48.6","66.225.14.170"};

	boolean inWhitelist(String ips) {
		boolean yes = false;
		for (String str : whitelist) {
			if (str.equals(ips)) {
				yes = true;
				break;
			}
		}
		return yes;
	}

	/**
	 * Send RST packet to Client and traffic subscriber
	 * 
	 * @param ip
	 * @param tcp
	 * @param datalength
	 */
	void sendRstPacket(IPv4Header ip, TCPHeader tcp, int datalength) {
		byte[] data = tcpFactory.createRstData(ip, tcp, datalength);
		try {
			clientWriter.write(data);
			pcapData.addData(data);
			Log.d(TAG, "Sent RST Packet to client with dest => " + PacketUtil.intToIPAddress(ip.getDestinationIP()) + ":" + tcp.getDestinationPort());
		} catch (IOException e) {
			Log.e(TAG, "failed to send RST packet: " + e.getMessage());
		}
	}

	/**
	 * Send FinAck packet to Client and traffic subscriber
	 * 
	 * @param ip
	 * @param tcp
	 * @param session
	 */
	void ackFinAck(IPv4Header ip, TCPHeader tcp, Session session) {
		//TODO: check if client only sent FIN without ACK
		int ack = tcp.getSequenceNumber() + 1;
		int seq = tcp.getAckNumber();
		byte[] data = tcpFactory.createFinAckData(ip, tcp, ack, seq, true, true);
		try {
			clientWriter.write(data);
			pcapData.addData(data);
			
			if (session != null) {
				session.getSelectionkey().cancel();
				sessionManager.closeSession(session);
				Log.d(TAG,
						"ACK to client's FIN and close session => " + PacketUtil.intToIPAddress(ip.getDestinationIP()) + ":" + tcp.getDestinationPort() + "-"
								+ PacketUtil.intToIPAddress(ip.getSourceIP()) + ":" + tcp.getSourcePort());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send FIN-ACK packet to Client and traffic subscriber
	 * 
	 * @param ip
	 * @param tcp
	 * @param session
	 */
	void sendFinAck(IPv4Header ip, TCPHeader tcp, Session session) {
		int ack = tcp.getSequenceNumber();
		int seq = tcp.getAckNumber();
		byte[] data = tcpFactory.createFinAckData(ip, tcp, ack, seq, true, false);
		try {

			clientWriter.write(data);
			pcapData.addData(data);
			Log.d(TAG, "00000000000 FIN-ACK packet data to vpn client 000000000000");
			IPv4Header vpnip = null;
			try {
				vpnip = IPPacketFactory.createIPv4Header(data, 0);
			} catch (PacketHeaderException e) {
				e.printStackTrace();
			}
			TCPHeader vpntcp = null;
			try {
				vpntcp = tcpFactory.createTCPHeader(data, vpnip.getIPHeaderLength());
			} catch (PacketHeaderException e) {
				e.printStackTrace();
			}
			if (vpnip != null && vpntcp != null) {
				String sout = PacketUtil.getOutput(vpnip, vpntcp, data);
				Log.d(TAG, sout);
			}
			Log.d(TAG, "0000000000000 finished sending FIN-ACK packet to vpn client 000000000000");

		} catch (IOException e) {
			Log.e(TAG, "Failed to send ACK packet: " + e.getMessage());
		}
		session.setSendNext(seq + 1);
		//avoid re-sending it, from here client should take care the rest
		session.setClosingConnection(false);
	}

	/**
	 * <pre>
	 * push data to destination here. 
	 *  Background thread will receive data and fill session's buffer.
	 *  Background thread will send packet to client
	 * 
	 * @param session
	 * @param ip
	 * @param tcp
	 */
	void pushDataToDestination(Session session, IPv4Header ip, TCPHeader tcp) {

		session.setDataForSendingReady(true);

		session.setLastIPheader(ip);
		session.setLastTCPheader(tcp);
		session.setTimestampReplyto(tcp.getTimeStampSender());
		Date dt = new Date();
		int timestampSender = (int) dt.getTime();
		session.setTimestampSender(timestampSender);
		Log.d(TAG, "set data ready for sending to dest, bg will do it. data size: " + session.getSendingDataSize());

	}

	/**
	 * send acknowledgment packet to VPN client
	 * 
	 * @param ipheader
	 * @param tcpheader
	 * @param acceptedDataLength
	 * @param session
	 */
	void sendAckToClient(IPv4Header ipheader, TCPHeader tcpheader, int acceptedDataLength, Session session) {
		int acknumber = session.getRecSequence() + acceptedDataLength;
		Log.d(TAG, "sent ack, ack# " + session.getRecSequence() + " + " + acceptedDataLength + " = " + acknumber);
		session.setRecSequence(acknumber);
		byte[] data = tcpFactory.createResponseAckData(ipheader, tcpheader, acknumber);
		try {
				clientWriter.write(data);
				pcapData.addData(data);
				/* for debugging purpose
					Log.d(TAG,"&&&&&&&&&&&&& ACK packet data to vpn client &&&&&&&&&&&&&&");
					IPv4Header vpnip = null;
					try {
						vpnip = factory.createIPv4Header(data, 0);
					} catch (PacketHeaderException e) {
						e.printStackTrace();
					}
					TCPHeader vpntcp = null;
					try {
						vpntcp = factory.createTCPHeader(data, vpnip.getIPHeaderLength());
					} catch (PacketHeaderException e) {
						e.printStackTrace();
					}
					if(vpnip != null && vpntcp != null){
						String sout = PacketUtil.getOutput(vpnip, vpntcp, data);
						Log.d(TAG,sout);
					}
				Log.d(TAG,"&&&&&&&&&&&& finished sending ACK packet to vpn client &&&&&&&&&&&&&&&&");
				 */	
		} catch (IOException e) {
			Log.e(TAG, "Failed to send ACK packet: " + e.getMessage());
		}
	}

	/**
	 * acknowledge a packet and adjust the receiving window to avoid congestion.
	 * 
	 * @param ipheader
	 * @param tcpheader
	 * @param session
	 */
	void acceptAck(IPv4Header ipheader, TCPHeader tcpheader, Session session) {
		boolean isCorrupted = PacketUtil.isPacketCorrupted(tcpheader);
		session.setPacketCorrupted(isCorrupted);
		if (isCorrupted) {
			Log.e(TAG, "prev packet was corrupted, last ack# " + tcpheader.getAckNumber());
		}
		if ((tcpheader.getAckNumber() > session.getSendUnack()) || (tcpheader.getAckNumber() == session.getSendNext())) {
			session.setAcked(true);
			//Log.d(TAG,"Accepted ack from client, ack# "+tcpheader.getAckNumber());

			if (tcpheader.getWindowSize() > 0) {
				session.setSendWindowSizeAndScale(tcpheader.getWindowSize(), session.getSendWindowScale());
			}
			int byteReceived = tcpheader.getAckNumber() - session.getSendUnack();
			if (byteReceived > 0) {
				session.decreaseAmountSentSinceLastAck(byteReceived);
			}
			if(session.isClientWindowFull()){
				Log.d(TAG,"window: "+session.getSendWindow()+" is full? "+session.isClientWindowFull() + " for "+PacketUtil.intToIPAddress(ipheader.getDestinationIP())
					+":"+tcpheader.getDestinationPort()+"-"+PacketUtil.intToIPAddress(ipheader.getSourceIP())+":"+tcpheader.getSourcePort());
}
			session.setSendUnack(tcpheader.getAckNumber());
			session.setRecSequence(tcpheader.getSequenceNumber());
			session.setTimestampReplyto(tcpheader.getTimeStampSender());
			Date dt = new Date();
			int timestampSender = (int) dt.getTime();
			session.setTimestampSender(timestampSender);
		} else {
			Log.d(TAG, "Not Accepting ack# " + tcpheader.getAckNumber() + " , it should be: " + session.getSendNext());
			Log.d(TAG, "Prev sendUnack: " + session.getSendUnack());
			session.setAcked(false);
		}
	}

	/**
	 * Set connection as aborting so that background worker will close it.
	 * 
	 * @param ipHeader
	 * @param tcpHeader
	 */
	void resetConnection(IPv4Header ipHeader, TCPHeader tcpHeader) {
		Session session = sessionManager.getSession(ipHeader.getDestinationIP(), tcpHeader.getDestinationPort(), ipHeader.getSourceIP(), tcpHeader.getSourcePort());
		if (session != null) {
			session.setAbortingConnection(true);
		}
	}

	/**
	 * create a new client's session and SYN-ACK packet data to respond to
	 * client
	 * 
	 * @param ip
	 * @param tcp
	 */
	void replySynAck(IPv4Header ip, TCPHeader tcp) {

		ip.setIdenfication(0);
		Packet packet = tcpFactory.createSynAckPacketData(ip, tcp);

		TCPHeader tcpheader = packet.getTcpheader();

		Session session = sessionManager.createNewSession(ip.getDestinationIP(), tcp.getDestinationPort(), 
				ip.getSourceIP(), tcp.getSourcePort());
		if (session == null) {
			return;
		}

		int windowScaleFactor = (int) Math.pow(2, tcpheader.getWindowScale());
		//Log.d(TAG,"window scale: Math.power(2,"+tcpheader.getWindowScale()+") is "+windowScaleFactor);
		session.setSendWindowSizeAndScale(tcpheader.getWindowSize(), windowScaleFactor);
		Log.d(TAG, "send-window size: " + session.getSendWindow());
		session.setMaxSegmentSize(tcpheader.getMaxSegmentSize());
		session.setSendUnack(tcpheader.getSequenceNumber());
		session.setSendNext(tcpheader.getSequenceNumber() + 1);
		//client initial sequence has been incremented by 1 and set to ack
		session.setRecSequence(tcpheader.getAckNumber());

		try {
				clientWriter.write(packet.getBuffer());
				pcapData.addData(packet.getBuffer());
				Log.d(TAG, "Send SYN-ACK to client");
		} catch (IOException e) {
				Log.e(TAG, "Error sending data to client: " + e.getMessage());
		}

	}

}//end class
