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

import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;

/**
 * <pre>
 * Singleton data structure for storing packet data in queue. 
 * Data is pushed into this queue from VpnService as well as 
 * a background worker that pulls data from the remote socket.
 * 
 * Purpose:
 *  The primary purpose is to pass data on to the traffic.cap file
 * 
 * Date: May 12, 2014
 */
public class SocketData {
	
	private static Object syncObj = new Object();
	private static Object syncData = new Object();
	private volatile static SocketData instance = null;
	private Queue<byte[]> data;

	/**
	 * get instance
	 * @return
	 */
	public static SocketData getInstance() {
		if (instance == null) {
			synchronized (syncObj) {
				if (instance == null) {
					instance = new SocketData();
				}
			}
		}
		return instance;
	}

	/**
	 * <pre>
	 * A LinkedList to store packet data in.
	 * This deque is being acting like a fifo buffer. new packets are added to the tail and pulled off from the head.
	 */
	private SocketData() {
		data = new LinkedList<byte[]>();
	}

	/**
	 * Add packets to queue for traffic.cap
	 * @param packet
	 */
	public void addData(byte[] packet) {
		synchronized (syncData) {
			byte[] copy = new byte[packet.length];
			System.arraycopy(packet, 0, copy, 0, packet.length);
			try {
				data.add(copy);
			} catch (IllegalStateException ex) {
				ex.printStackTrace();
			} catch (NullPointerException ex1) {
				ex1.printStackTrace();
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}
	}

	/**
	 * Retrieve & remove (pop) packet from fifo.
	 * Should be sent on to traffic.cap
	 * @return
	 */
	public byte[] getData() {
		byte[] packet = null;
		synchronized (syncData) {
			packet = data.poll();
		}
		return packet;
	}
}//end class
