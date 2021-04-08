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

import com.att.arocollector.attenuator.AttenuatorManager;
import com.att.arocollector.attenuator.AttenuatorUtil;
import com.att.arotcpcollector.IClientPacketWriter;
import com.att.arotcpcollector.ip.IPHeader;
import com.att.arotcpcollector.ip.IPPacketFactory;
import com.att.arotcpcollector.ip.IPv4Header;
import com.att.arotcpcollector.socket.IDataReceivedSubscriber;
import com.att.arotcpcollector.socket.SocketData;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.tcp.TCPPacketFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class VPNInterfaceWriter implements Runnable, IDataReceivedSubscriber {

	public static final String TAG = VPNInterfaceWriter.class.getSimpleName();

	public static VPNInterfaceWriter vpnWriterInterface;

	private volatile boolean shutdown = false;
	private SocketData socketData = null;
	private int downloadSpeedLimit = AttenuatorUtil.DEFAULT_THROTTLE_SPEED;// 100Mb = 1024Kb
	private BlockingQueue<byte[]> dataReceived;
	private IClientPacketWriter clientPacketWriter;

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

		long maxBucketSize = AttenuatorManager.getInstance().getThrottleDL() * 1000 / 8;
		long lastPacketTime = System.nanoTime();
		double currentNumberOfTokens = maxBucketSize;

		Log.i(TAG, "Download Speed Limit : " + (AttenuatorManager.getInstance().getThrottleDL() * 1000 / 8)+ " Bytes");
		try {
			while (!shutdown) {
				Log.d(TAG, "I am polling data");
				packet = dataReceived.take();

				if (null != packet && packet.length > 0) {
					// Sleep until we have some value for throttling other than zero
					while (AttenuatorManager.getInstance().getThrottleDL() == 0)  {
						// Sleep for a short duration as no packet can be processed for a zero throttle value
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							Log.d(TAG, "Failed to sleep when Download throttle was 0: " + e.getMessage());
						}
					}

					try {
						clientPacketWriter.write(packet);
					} catch (IOException e) {
						Log.e(TAG, "Failed to write packet: " + e.getMessage(),e);
					}

					int throttleDL = AttenuatorManager.getInstance().getThrottleDL();
					if (throttleDL > 0) {
						maxBucketSize = throttleDL * 1000 / 8;
						int headerLength = 0;
						TCPHeader tcpHeader = null;
						TCPPacketFactory tcpFactory = new TCPPacketFactory();
						try {
							if (isTCP(packet)) {
								IPHeader ipHeader = IPPacketFactory.createIPHeader(packet, 0);
								headerLength += ipHeader.getIPHeaderLength();
								tcpHeader = tcpFactory.createTCPHeader(packet, ipHeader.getIPHeaderLength());
								headerLength += tcpHeader.getTCPHeaderLength();
							} else {
								headerLength = 28;
							}
						} catch (PacketHeaderException ex){
							Log.e(TAG , "Packet Header Exception"+ ex.getMessage(),ex);
						}
						int consumedTokens = packet.length - headerLength;
						long currentTime = System.nanoTime();
						double generatedToken = (currentTime - lastPacketTime) * throttleDL / 8 / 1000000;
						currentNumberOfTokens += generatedToken;
						if (currentNumberOfTokens > maxBucketSize) {
							currentNumberOfTokens = maxBucketSize;
						}
						lastPacketTime = currentTime;
						currentNumberOfTokens -= consumedTokens;
						if (currentNumberOfTokens < 0){
							try{
								int sleepTime = (int) (-1 * currentNumberOfTokens * 8 / throttleDL);
								if (sleepTime > 0) {
									Thread.sleep(sleepTime);
								}
							} catch (InterruptedException e) {
								Log.d(TAG, "Failed to sleep: " + e.getMessage(),e);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, "Reading the Queue was Interrupted");
		}

		Log.d(TAG, "Was shutdown");
	}

	private boolean isTCP(byte[] packet) {
		if(packet.length > 9 ) {
			byte protocol = packet[9];
			if(protocol == 6) {
				return true;
			}
		}
		return false;
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

	public int getDownloadSpeedLimit() {
		Log.i(TAG,"get Download Speed Limit: "+ downloadSpeedLimit + " kbps");
		return downloadSpeedLimit;
	}

	//set from  CaptureVpnService.java, startCapture()
	public void setDownloadSpeedLimit(int downloadSpeedLimit) {
		this.downloadSpeedLimit = downloadSpeedLimit;
		Log.i(TAG,"set Download Speed Limit: "+ downloadSpeedLimit+ " kbps");
	}
}
