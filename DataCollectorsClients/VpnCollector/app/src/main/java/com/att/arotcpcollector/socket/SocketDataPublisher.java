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

import com.att.arocollector.packetRebuild.PCapFileWriter;
import com.att.arotcpcollector.SessionManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Publish packet data to subscribers that implement interface IPcapSubscriber
 * 
 * @author Borey Sao Date: June 15, 2014
 */
public class SocketDataPublisher implements Runnable, IPcapSubscriber {
	
	public static final String TAG = "CaptureVpnService";
	
	SocketData socketData;
	private PCapFileWriter pcapWriter;
	private Queue<byte[]> dataToBeWrittenToPcap;
	
	private static Object syncPcapData = new Object();
	
	public PCapFileWriter getPcapWriter() {
		return pcapWriter;
	}

	public void setPcapWriter(PCapFileWriter pcapWriter) {
		this.pcapWriter = pcapWriter;
	}
	
	private volatile boolean isShuttingDown = false;

	public SocketDataPublisher() {
		socketData = SocketData.getInstance();
		socketData.registerPcapSubscriber(this);
		dataToBeWrittenToPcap = new LinkedList<byte[]>();
	}


	/**
	 * Send packets to be added to traffic.cap
	 */
	@Override
	public void run() {
		Log.d(TAG, "BackgroundWriter starting...");

		while (!isShuttingDown) {
			byte[] packetdata;
			synchronized (syncPcapData) {
				packetdata = dataToBeWrittenToPcap.poll();
			}
			if (packetdata != null) {
				if (pcapWriter != null) {
					try {
						pcapWriter.addPacket(packetdata, 0, packetdata.length, System.currentTimeMillis() * 1000000);
					} catch (IOException e) {
						Log.e(TAG, "pcapOutput.addPacket IOException :" + e.getMessage());
						e.printStackTrace();
					}
				}else{
					Log.e(TAG, "Pcap Writer is Null - Pay Attention");
				}
				//dispose after use
				packetdata = null;
			} else {
				try {
					// Earlier Packet too long/No packets to write - Sleep a while.
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		Log.d(SessionManager.TAG, "BackgroundWriter ended");
	}

	public boolean isShuttingdown() {
		return isShuttingDown;
	}

	public void setIsShuttingDown(boolean isShuttingDown) {
		this.isShuttingDown = isShuttingDown;
	}

	@Override
	public void writePcap(byte[] packet) {
		synchronized (syncPcapData) {
			try {
				dataToBeWrittenToPcap.add(packet);
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
