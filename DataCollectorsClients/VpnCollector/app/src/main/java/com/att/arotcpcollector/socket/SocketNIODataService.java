/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.att.arotcpcollector.IClientPacketWriter;
import com.att.arotcpcollector.Session;
import com.att.arotcpcollector.SessionHandler;
import com.att.arotcpcollector.SessionManager;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.tcp.TCPPacketFactory;
import com.att.arotcpcollector.udp.UDPPacketFactory;
import com.att.arotcpcollector.util.PacketUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketNIODataService implements Runnable, ISocketDataSubscriber{
	public static final String TAG = "CaptureVpnService";
	public static Object syncSelectorForSelection = new Object();
	public static Object syncSelectorForUse = new Object();

	private Queue<byte[]> dataToBeTransmitted;
	private static Object syncTransmittedData = new Object();


	SessionManager sessionmg;
	SessionHandler sessionHandler;

	int printcount = 0;
	SocketData socketData;
	private IClientPacketWriter clientPacketWriter;
	private TCPPacketFactory tcpFactory;
	private UDPPacketFactory udpFactory;
	private volatile boolean shutdown = false;
	private Selector selector = null;
	
	//create thread pool for reading/writing data to socket
	private BlockingQueue<Runnable> taskQueue;
	private ThreadPoolExecutor workerPool;

	public SocketNIODataService() {
		tcpFactory = new TCPPacketFactory();
		udpFactory = new UDPPacketFactory();
		socketData = SocketData.getInstance();
		socketData.registerDataTransmitterSubscriber(this);
		dataToBeTransmitted = new LinkedList<byte[]>();
		taskQueue = new LinkedBlockingQueue<Runnable>();
		workerPool = new ThreadPoolExecutor(8, 100, 10, TimeUnit.SECONDS, taskQueue);//8, 100
	}

	public void setClientWriter(IClientPacketWriter clientPacketWriter) {
		this.clientPacketWriter = clientPacketWriter;
	}

	/**
	 * runs SessionManager
	 */
	@Override
	public void run() {
		Log.d(TAG, "SocketDataService starting in background...");
		sessionmg = SessionManager.getInstance();
		selector = sessionmg.getSelector();
		sessionHandler = SessionHandler.getInstance();
 		sessionHandler.setClientWriter(clientPacketWriter);
		runTask();
	}

	/**
	 * notify long running task to shutdown
	 * 
	 * @param isshutdown
	 */
	public void setShutdown(boolean isshutdown) {
		this.shutdown = isshutdown;
		this.sessionmg.getSelector().wakeup();
	}

	/**
	 * Selector is running...
	 */
	void runTask() {

 		while (!shutdown) {

			byte[] packetdata;

			synchronized (syncTransmittedData) {
				packetdata = dataToBeTransmitted.poll();
			}

			if (packetdata != null) {
				try {
					Log.d(TAG, "Step 1");
					sessionHandler.handlePacket(packetdata);
				} catch (PacketHeaderException e) {
					Log.e(TAG, "Packet Header Exception Thrown: " + e.getMessage());
					e.printStackTrace();
				}
			}

			try {
				synchronized (syncSelectorForSelection) {
					selector.select();
				}
			} catch (IOException e) {
				Log.e(TAG, "Error in Selector.select(): " + e.getMessage());
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					Log.d(TAG, "Selector Thread Sleep " + e.getMessage());
				}
				continue;
			}

			if (shutdown) {
				Log.d(TAG, "Selector is in shutdown...");
				break;
			}

			synchronized (syncSelectorForUse) {
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while (iter.hasNext()) {
					SelectionKey key = (SelectionKey) iter.next();
					if (key.isValid()) {// adding this check to avoid java.nio.channels.CancelledKeyException
						if (key.attachment() == null) {
							try {
								processTCPSelectionKey(key);
							} catch (IOException e) {
								key.cancel();
							}
						} else {
							processUDPSelectionKey(key);
						}
						iter.remove();
						if (shutdown) {
							Log.d(TAG, "syncSelectorForUse is in shutdown...");
							break;
						}
					} else {
						Log.d(TAG, "Invalid Key...");
 					}
				}
			}

		}
	}

	void processUDPSelectionKey(SelectionKey key) {
		if (!key.isValid()) {
			Log.d(TAG, "Invalid SelectionKey for UDP");
			return;
		}
		DatagramChannel channel = (DatagramChannel) key.channel();
		Session session = sessionmg.getSessionByDatagramChannel(channel);
		if (session == null) {
			return;
		}

		if (!session.isConnected() && key.isConnectable()) {
			String ips = PacketUtil.intToIPAddress(session.getDestAddress());
			int port = session.getDestPort();
			SocketAddress addr = new InetSocketAddress(ips, port);
			try {
//				Log.d(TAG, "selector: connecting to remote UDP server: " + ips + ":" + port);
 				channel = channel.connect(addr);
				session.setUdpChannel(channel);
				session.setConnected(channel.isConnected());

			} catch (Exception e) {
				Log.e(TAG,"failed to connect to udp: "+e.getMessage());				
				session.setAbortingConnection(true);
			}

		}
		if (channel.isConnected()) {
			processSelector(key, session);
		}
	}

 	void processTCPSelectionKey(SelectionKey key) throws IOException {
		if (!key.isValid()) {
			Log.d(TAG, "Invalid SelectionKey for TCP");
			return;
		}
		SocketChannel channel = (SocketChannel) key.channel();
		Session session = sessionmg.getSessionByChannel(channel);
		if (session == null) {
			return;
		}

		if (!session.isConnected() && key.isConnectable()) {

			String ips = PacketUtil.intToIPAddress(session.getDestAddress());
			int port = session.getDestPort();
			SocketAddress addr = new InetSocketAddress(ips, port);
			Log.d(TAG, "connecting to remote tcp server: " + ips + ":" + port);

			boolean connected = false;
			if (!channel.isConnected() && !channel.isConnectionPending()) {
				try {
					connected = channel.connect(addr);
				} catch (Exception e) {
					Log.e(TAG, "processTCPSelectionKey Exception :"+e.getMessage());
					session.setAbortingConnection(true);
				} 
 
			}

			if (connected) {
				session.setConnected(connected);
				Log.d(TAG, "connected immediately to remote tcp server: " + ips + ":" + port);
			} else {
				if (channel.isConnectionPending()) {
					connected = channel.finishConnect();
					session.setConnected(connected);
					Log.d(TAG, "connected to remote tcp server: " + ips + ":" + port);
				}
			}

		}
		if (channel.isConnected()) {
			processSelector(key, session);
		}
	}

	/**
	 * Launch a Thread via ThreadPoolExecutor to "run" a SocketDataWriterWorker.
	 * The worker will handle the session
	 * 
	 * @param key
	 *            A SelectionKey represents the relationship between a channel
	 *            and a selector for which the channel is registered.<br>
	 * @param session
	 *            a connected Session
	 */
	private void processSelector(SelectionKey key, Session session) {
		
		String sessionKey = session.getSessionKey();
		
		if (sessionKey == null) {
			sessionKey = sessionmg.createKey(session.getDestAddress(), session.getDestPort(), session.getSourceIp(), session.getSourcePort());
		}
						
		if(key.isValid() && key.isWritable() && !session.isBusyWrite()){
			if(session.hasDataToSend() && session.isDataForSendingReady()){
 					session.setBusyWrite(true);
					SocketDataWriterWorker worker = new SocketDataWriterWorker(tcpFactory, udpFactory, clientPacketWriter);
					worker.setSessionKey(sessionKey);
					workerPool.execute(worker);
			}	 
		}
		
		if(key.isValid() && key.isReadable() && !session.isBusyRead()){
 				session.setBusyRead(true);
				SocketDataReaderWorker worker = new SocketDataReaderWorker(tcpFactory, udpFactory, clientPacketWriter);
				worker.setSessionKey(sessionKey);
				workerPool.execute(worker);
		} 
		


	}

	@Override
	public void transmitData(byte[] packet) {
		synchronized (syncTransmittedData) {
			try {
				dataToBeTransmitted.add(packet);
				synchronized (syncSelectorForUse) {
					selector.wakeup();
				}
			} catch (IllegalStateException ex) {
				ex.printStackTrace();
			} catch (NullPointerException ex1) {
				ex1.printStackTrace();
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}
	}
}

