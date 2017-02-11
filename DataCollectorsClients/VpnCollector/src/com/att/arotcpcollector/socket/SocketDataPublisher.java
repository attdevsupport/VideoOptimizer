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

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.att.arotcpcollector.SessionManager;

/**
 * Publish packet data to subscribers that implement interface IReceivePacket
 * 
 * Date: June 15, 2014
 */
public class SocketDataPublisher implements Runnable {
	
	public static final String TAG = "SocketDataPublisher";
	List<IReceivePacket> subscribers;
	SocketData data; // for traffic.cap
	
	private volatile boolean isShuttingDown = false;

	public SocketDataPublisher() {
		data = SocketData.getInstance();
		subscribers = new ArrayList<IReceivePacket>();
	}

	/**
	 * register a subscriber who wants to receive packet data
	 * 
	 * @param subscriber
	 */
	public void subscribe(IReceivePacket subscriber) {
		if (!subscribers.contains(subscriber)) {
			subscribers.add(subscriber);
		}
	}

	/**
	 * Send packets to be added to traffic.cap
	 */
	@Override
	public void run() {
		Log.d(TAG, "BackgroundWriter starting...");

		while (!isShuttingDown) {
			byte[] packetdata = data.getData();
			if (packetdata != null) {
				for (IReceivePacket subscriber : subscribers) {
					subscriber.receive(packetdata);
				}
				//dispose after use
				packetdata = null;
			} else {
				try {
					// If size of data is getting too large shorten the sleep
					Thread.sleep(10);
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

}
