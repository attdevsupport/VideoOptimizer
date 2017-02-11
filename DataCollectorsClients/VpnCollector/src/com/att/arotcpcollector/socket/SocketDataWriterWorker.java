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
package com.att.arotcpcollector.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.Date;

import android.util.Log;

import com.att.arotcpcollector.IClientPacketWriter;
import com.att.arotcpcollector.Session;
import com.att.arotcpcollector.SessionManager;
import com.att.arotcpcollector.tcp.TCPPacketFactory;
import com.att.arotcpcollector.udp.UDPPacketFactory;
import com.att.arotcpcollector.util.PacketUtil;

public class SocketDataWriterWorker implements Runnable {

	public static final String TAG = "SocketDataWriterWorker";
	private IClientPacketWriter clientPacketWriter;
	private TCPPacketFactory tcpFactory;
	private UDPPacketFactory udpFactory;
	private SessionManager sessionMngr;
	private String sessionKey = "";
	private SocketData pcapData; // for traffic.cap
	
	public SocketDataWriterWorker(TCPPacketFactory tcpFactory, UDPPacketFactory udpFactory, IClientPacketWriter clientPacketWriter) {
		sessionMngr = SessionManager.getInstance();
		pcapData = SocketData.getInstance();
		this.tcpFactory = tcpFactory;
		this.udpFactory = udpFactory;
		this.clientPacketWriter = clientPacketWriter;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	@Override
	public void run() {

		Session session = sessionMngr.getSessionByKey(sessionKey);
		
		if (session == null) {
			return;
		} 

		session.setBusyWrite(true);
		
		if (session.getSocketchannel() != null) {
			writeTCP(session);
		} else if (session.getUdpChannel() != null) {
			writeUDP(session);
		}
		
		if (session != null) {
			session.setBusyWrite(false);
			if (session.isAbortingConnection()) {
				Log.d(TAG, "removing aborted connection -> " + session.getSessionName());
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
				sessionMngr.closeSession(session);
			}
		}
	}

	/**
	 * Forward packet on to original destination.
	 * 
	 * @param session
	 */
	void writeUDP(Session session) {
		if (!session.hasDataToSend()) {
			return;
		}
		DatagramChannel channel = session.getUdpChannel();
		String sessionKey = PacketUtil.intToIPAddress(session.getDestAddress()) + ":" + session.getDestPort() + "-" + PacketUtil.intToIPAddress(session.getSourceIp()) + ":"
						+ session.getSourcePort();
		byte[] data = session.getSendingData();
		ByteBuffer buffer = ByteBuffer.allocate(data.length);
		buffer.put(data);
		buffer.flip();
		try {
			//          String str = new String(data);
			//			Log.d(TAG, "****** data write to server ********");
			//			Log.d(TAG, str);
			//			Log.d(TAG, "***** end writing to server *******");
			//			Log.d(TAG, "writing data to remote UDP: " + session.getSessionName());
			channel.write(buffer);
			
		} catch (NotYetConnectedException ex2) {
			session.setAbortingConnection(true);
			Log.e(TAG, "Error writing to unconnected-UDP server, will abort current connection: " + ex2.getMessage());
		} catch (IOException e) {
			session.setAbortingConnection(true);
			e.printStackTrace();
			Log.e(TAG, "Error writing to UDP server, will abort connection: " + e.getMessage());
		}
	}

	/**
	 * Forward packet on to original destination.
	 * 
	 * @param session
	 */
	void writeTCP(Session session) {

		SocketChannel channel = session.getSocketchannel();

		byte[] data = session.getSendingData();
		if (data != null && data.length>0){
			Log.i("SSL", "data more than zero, data length: " + data.length);
		}
		ByteBuffer buffer = ByteBuffer.allocate(data.length);
		buffer.put(data);
		buffer.flip();
		
		try {
			//Log.d(TAG, "writing TCP data to: " + name);
			// Make sure that the buffer was fully drained
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
		} catch (NotYetConnectedException ex) {
			Log.e(TAG, "failed to write to unconnected socket: " + ex.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "Error writing to server: " + e.getMessage());

			//close connection with vpn client
			byte[] rstdata = tcpFactory.createRstData(session.getLastIPheader(), session.getLastTCPheader(), 0);
			try {
				Log.i("TCPTRACK"+session.getDestPort(), "<RST");
				clientPacketWriter.write(rstdata);
				pcapData.addData(rstdata);
			} catch (IOException e1) {
			}
			//remove session
			Log.e(TAG, "failed to write to remote socket, aborting connection");
			session.setAbortingConnection(true);
		}

	}
}
