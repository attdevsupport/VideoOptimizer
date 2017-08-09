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
package com.att.arotcpcollector.socket;

import android.util.Log;

import com.att.arotcpcollector.Session;
import com.att.arotcpcollector.SessionManager;
import com.att.arotcpcollector.ip.IPv4Header;

import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.tcp.TCPPacketFactory;
import com.att.arotcpcollector.udp.UDPPacketFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;

/**
 * background task for reading data from remote server and write data to vpn
 * client
 * 
 * @author Borey Sao Date: July 30, 2014
 */
public class SocketDataReaderWorker implements Runnable {
	public static final String TAG = "SocketDataReaderWorker";
	private TCPPacketFactory tcpFactory;
	private UDPPacketFactory udpFactory;
	private SessionManager sessionManager;
	private String sessionKey = "";
	private SocketData pcapData; // for traffic.cap
 	private boolean secureEnable = false;

	public SocketDataReaderWorker() {
		sessionManager = SessionManager.getInstance();
		pcapData = SocketData.getInstance();
	}

	public SocketDataReaderWorker(TCPPacketFactory tcpfactory, UDPPacketFactory udpfactory) {
		sessionManager = SessionManager.getInstance();
		pcapData = SocketData.getInstance();
		this.tcpFactory = tcpfactory;
		this.udpFactory = udpfactory;
 	}

	@Override
	public void run() {
		
		Session session = sessionManager.getSessionByKey(sessionKey);
		if (session == null) {
			return;
		}
		session.setLastAccessed(System.currentTimeMillis());

		if (session.getSocketchannel() != null) {
			try {
				readTCP(session);
			} catch (Exception ex) {
				Log.e(TAG, "error processRead: " + ex.getMessage());
			}
		} else if (session.getUdpChannel() != null) {
			readUDP(session);
		}

		if (session != null) {

			if (session.isAbortingConnection()) {
				Log.d(TAG, "removing aborted connection -> " + session.getSessionKey());
				//								+ PacketUtil.intToIPAddress(sess.getDestAddress()) + ":" + sess.getDestPort() + "-"
				//								+ PacketUtil.intToIPAddress(sess.getSourceIp()) + ":" + sess.getSourcePort());
				session.getSelectionkey().cancel();

				if (session.getSocketchannel() != null && session.getSocketchannel().isConnected()) {
					try {
						session.getSocketchannel().close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (session.getUdpChannel() != null && session.getUdpChannel().isConnected()) {
					try {
						session.getUdpChannel().close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				sessionManager.closeSession(session);

			} else {
				session.setBusyRead(false);
			}
		}
	}

	/**
	 * Receive TCP data
	 * 
	 * @param session
	 */
	void readTCP(Session session) {

		SocketChannel channel = session.getSocketchannel();
		ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
		int len = 0;
		try {

			do {
				if (session.isAbortingConnection()) {
					return;//break;
				}

				if (!session.isClientWindowFull()) {
					len = channel.read(buffer);
					if (len > 0) { // -1 indicates end of stream
						// Log.d(TAG, "SocketDataService received " + len + " from remote server: " + name);
						// send packet to client app
						//***************
						if (isSecureEnable()) {

						}
						//***************
						sendToRequester(buffer, channel, len, session);
						buffer.clear();
					} else if (len == -1) {
						sendFin(session);
						session.setAbortingConnection(true);
					} 
				} else {

//					Log.e(TAG, "*** client window is full, now pause for " + sess.getSessionName());
					//									+ PacketUtil.intToIPAddress(sess.getDestAddress()) + ":" + sess.getDestPort() 
					//									+ "-"
					//									+ PacketUtil.intToIPAddress(sess.getSourceIp()) + ":" + sess.getSourcePort());
					break;
				}

			} while (len > 0);

		} catch (NotYetConnectedException ex2) {
			Log.e(TAG, "socket not connected");
			session.setAbortingConnection(true);
		} catch (ClosedByInterruptException cex) {
			Log.e(TAG, "ClosedByInterruptException reading socketchannel: " + cex.getMessage());
			session.setAbortingConnection(true);
		} catch (ClosedChannelException clex) {
			Log.e(TAG, "ClosedChannelException reading socketchannel: " + clex.getMessage());
			session.setAbortingConnection(true);
		} catch (IOException e) {
			Log.e(TAG, "Error reading data from socketchannel: " + e.getMessage());
			session.setAbortingConnection(true);
		} catch (Exception ex) {
			Log.e("Secure Collector", "We are catching an Exception: " + ex.getMessage());
		}
	}

	/**
	 * 
	 * 
	 * @param buffer
	 * @param channel
	 * @param datasize
	 * @param sess
	 */
	void sendToRequester(ByteBuffer buffer, SocketChannel channel, int datasize, Session sess) {

		if (sess == null) {
			Log.e(TAG, "Session not found for dest. server: " + channel.socket().getInetAddress().getHostAddress());
			return;
		}

		//last piece of data is usually smaller than MAX_RECEIVE_BUFFER_SIZE
		if (datasize < DataConst.MAX_RECEIVE_BUFFER_SIZE) {
			sess.setHasReceivedLastSegment(true);
		} else {
			sess.setHasReceivedLastSegment(false);
		}
		
		//Fix: For Data Size that may exceed fixed buffer size during a Secure Transmission
		if(isSecureEnable() && datasize < buffer.capacity()){
			sess.setHasReceivedLastSegment(true);
		}

		buffer.limit(datasize);
		buffer.flip();
		byte[] data = new byte[datasize];
		System.arraycopy(buffer.array(), 0, data, 0, datasize);
		sess.addReceivedData(data);

		//Log.d(TAG,"DataSerice added "+data.length+" to session. session.getReceivedDataSize(): "+session.getReceivedDataSize());
		//pushing all data to vpn client
		while (sess.hasReceivedData()) {
			pushDataToClient(sess);
		}
	}

	/**
	 * create packet data and send it to VPN client
	 * @param session
	 * @return
	 */
	boolean pushDataToClient(Session session) {
		
		if (!session.hasReceivedData()) {
			//no data to send
			Log.d(TAG, "no data for vpn client");
			return false;
		}

		IPv4Header ipheader = session.getLastIPheader();
		TCPHeader tcpheader = session.getLastTCPheader();
		int max = session.getMaxSegmentSize() - 60;

		if (max < 1) {
			max = 1024;
		}

		byte[] packetbody = session.getReceivedData(max);
		if (packetbody != null && packetbody.length > 0) {
			byte[] data = null;
			int unack = session.getSendNext();
			int nextUnack = session.getSendNext() + packetbody.length;

			//Log.d(TAG,"sending vpn client body len: "+packetbody.length+", current seq: "+unack+", next seq: "+nextUnack);
			session.setSendNext(nextUnack);
			//we need this data later on for retransmission
			session.setUnackData(packetbody);
			session.setResendPacketCounter(0);

			data = tcpFactory.createResponsePacketData(ipheader, tcpheader, packetbody, session.hasReceivedLastSegment(), session.getRecSequence(), unack,
					session.getTimestampSender(), session.getTimestampReplyto());


				pcapData.sendDataRecieved(data); // send packet back to client
				pcapData.sendDataToPcap(data); // send packet off to be recorded in traffic.cap


			return true;
		}
		return false;
	}

	/**
	 * Send a tcp FIN packet<br>
	 * FIN (1 bit) – No more data from sender
	 * 
	 * @param session
	 */
	private void sendFin(Session session) {
		IPv4Header ipheader = session.getLastIPheader();
		TCPHeader tcpheader = session.getLastTCPheader();
		byte[] data = tcpFactory.createFinData(ipheader, tcpheader, session.getSendNext(), session.getRecSequence(), session.getTimestampSender(), session.getTimestampReplyto());
		pcapData.sendDataRecieved(data); // send packet back to client
			pcapData.sendDataToPcap(data); // send packet off to be recorded in traffic.cap

			// for debugging purpose
			//	Log.d(TAG, "========> BG: FIN packet data to vpn client++++++++");
			//	IPv4Header vpnip = null;
			//	try {
			//		vpnip = factory.createIPv4Header(data, 0);
			//	} catch (PacketHeaderException e) {
			//		e.printStackTrace();
			//	}
			//	TCPHeader vpntcp = null;
			//	try {
			//		vpntcp = factory.createTCPHeader(data, vpnip.getIPHeaderLength());
			//	} catch (PacketHeaderException e) {
			//		e.printStackTrace();
			//	}
			//	if (vpnip != null && vpntcp != null) {
			//		String sout = PacketUtil.getOutput(vpnip, vpntcp, data);
			//		Log.d(TAG, sout);
			//	}
			//
			//	Log.d(TAG, "=======> BG: finished sending FIN packet to vpn client ========");


	}

	private void readUDP(Session session) {
		DatagramChannel channel = session.getUdpChannel();
		ByteBuffer buffer = ByteBuffer.allocate(DataConst.MAX_RECEIVE_BUFFER_SIZE);
		int len = 0;
		try {
			do {
				if (session.isAbortingConnection()) {
					break;
				}
				len = channel.read(buffer);
				if (len > 0) {
 
					buffer.limit(len);
					buffer.flip();
					//create UDP packet
					byte[] data = new byte[len];
					System.arraycopy(buffer.array(), 0, data, 0, len);
					byte[] packetdata = udpFactory.createResponsePacket(session.getLastIPheader(), session.getLastUDPheader(), data);
					//write to client


					pcapData.sendDataRecieved(packetdata); // send packet back to client

					//publish to packet subscriber
					pcapData.sendDataToPcap(packetdata); // send packet off to be recorded in traffic.cap
					Log.d(TAG, "SDR: sent " + len + " bytes to UDP client, packetdata.length: " + packetdata.length + " Data: "+ new String(data));
					buffer.clear();

//					try {
//						IPv4Header ip = IPPacketFactory.createIPv4Header(packetdata, 0);
//						UDPHeader udp = udpfactory.createUDPHeader(packetdata, ip.getIPHeaderLength());
//						String str = PacketUtil.getUDPoutput(ip, udp);
//						Log.d(TAG, "++++++ SD: packet sending to client ++++++++");
//						Log.i(TAG, "got response time: " + restime);
//						Log.d(TAG, str);
//						Log.d(TAG, "++++++ SD: end sending packet to client ++++");
//					} catch (PacketHeaderException e) {
//						e.printStackTrace();
//					}
				}
			} while (len > 0);
		} catch (NotYetConnectedException ex) {
			Log.e(TAG, "failed to read from unconnected UDP socket");
			session.setAbortingConnection(true);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Faild to read from UDP socket, aborting connection");
			session.setAbortingConnection(true);
		}
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public void setSecureEnable(boolean Secure){
		this.secureEnable = Secure;
	}
	
	public boolean isSecureEnable() {
		return secureEnable;
	}

}
