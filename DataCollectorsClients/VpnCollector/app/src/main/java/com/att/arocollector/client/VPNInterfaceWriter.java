/*
 * Copyright 2014 AT&T
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

package com.att.arocollector.client;


import android.util.Log;


import com.att.arotcpcollector.IClientPacketWriter;
import com.att.arotcpcollector.ip.IPPacketFactory;
import com.att.arotcpcollector.ip.IPv4Header;
import com.att.arotcpcollector.socket.IDataReceivedSubscriber;
import com.att.arotcpcollector.socket.SocketData;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.tcp.TCPPacketFactory;
import com.att.arotcpcollector.udp.UDPHeader;
import com.att.arotcpcollector.udp.UDPPacketFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class VPNInterfaceWriter implements Runnable, IDataReceivedSubscriber {

	public static final String TAG = VPNInterfaceWriter.class.getSimpleName();

	public static VPNInterfaceWriter vpnWriterInterface;

	private volatile boolean shutdown = false;
	private SocketData socketData = null;
 	private BlockingQueue<byte[]> dataReceived;
	private IClientPacketWriter clientPacketWriter;

	private static final Object syncObj = new Object();

	private VPNInterfaceWriter() {
		socketData = SocketData.getInstance();
		socketData.registerDataReceivedSubscribers(this);
		dataReceived = new LinkedBlockingQueue<>();
	}

	public static VPNInterfaceWriter getInstance() {
		if(vpnWriterInterface == null) {
			synchronized(VPNInterfaceWriter.class){
				vpnWriterInterface = new VPNInterfaceWriter();
			}
		}
		return vpnWriterInterface;
	}

	@Override
	public void run() {
		Log.d(TAG, "VPNInterfaceWriter starting in background...");
		runTask();
	}

/**
	 * notify long running task to shutdown
	 * @param isshutdown
 */
	public void setShutdown(boolean isshutdown) {
		this.shutdown = isshutdown;
	}

	/**
	 * Writer is running
	 */
	void runTask() {

		byte[] packet;
		int totalData = 0;
		long startTime = System.nanoTime();
		long stopTime = System.nanoTime();
		boolean startFlag = true;
		int count = 0;

 		try {
			while (!shutdown) {
				Log.d(TAG, "I am polling data");

				packet = dataReceived.take();

				if (null != packet && packet.length > 0) {
					try {
						clientPacketWriter.write(packet);
					} catch (IOException e) {
						Log.e(TAG, "Failed to write packet: " + e.getMessage(),e);
					}

					if (startFlag || (1000000000 <= (stopTime - startTime))) {
//						Log.d(TAG, "Round : " + ++count);
						startFlag = false;
						startTime = System.nanoTime();
						totalData = 0;
					}

					int headerLength = 0;
					UDPHeader udpHeader = null;
					TCPHeader tcpHeader = null;
					TCPPacketFactory tcpFactory = new TCPPacketFactory();
					UDPPacketFactory udpFactory = new UDPPacketFactory();

					try {
						IPv4Header ipHeader = IPPacketFactory.createIPv4Header(packet, 0);
						headerLength += ipHeader.getIPHeaderLength();
						if (ipHeader.getProtocol() == 6) {
							tcpHeader = tcpFactory.createTCPHeader(packet, ipHeader.getIPHeaderLength());
							headerLength += tcpHeader.getTCPHeaderLength();
						} else {
							udpHeader = udpFactory.createUDPHeader(packet, ipHeader.getIPHeaderLength());
							headerLength += udpHeader.getLength();
						}
					} catch (PacketHeaderException ex){
						Log.e(TAG , "Packet Header Exception"+ ex.getMessage(),ex);
					}

					totalData += (packet.length - headerLength);
					Log.i(TAG,"Total Data Length: "+ totalData + " Bytes");
 					stopTime = System.nanoTime();

				}
			}
		} catch (Exception ex) {
			Log.e(TAG, "Reading the Queue was Interrupted");
		}

		Log.d(TAG, "Was shutdown");
	}

	@Override
	public void receiveData(byte[] packet) {
		try {
			dataReceived.offer(packet);
		} catch (Exception ex){
			Log.e(TAG, "Writing to the Queue was Interrupted");
		}
	}

	public void setClientWriter(IClientPacketWriter clientPacketWriter) {
		this.clientPacketWriter = clientPacketWriter;
	}


}
