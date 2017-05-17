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
import java.util.Arrays;

/**
 * <pre>
 * Singleton data structure for storing packet data in queue. 
 * Data is pushed into this queue from VpnService as well as 
 * a background worker that pulls data from the remote socket.
 * 
 * Purpose:
 *  The primary purpose is to pass data on to the traffic.cap file
 * 
 * @author Borey Sao Date: May 12, 2014
 */
public class SocketData implements IReceivePcapData, IReceiveDataToBeTransmitted {
	
	private volatile static SocketData instance = null;
	
	private ArrayList<IPcapSubscriber> pcapSubscribers;
	private ArrayList<ISocketDataSubscriber> socketDataSubscribers;
	
	/**
	 * get instance
	 * @return
	 */
	public static SocketData getInstance() {
		if (instance == null) {
			synchronized (SocketData.class) {
				if (instance == null) {
					instance = new SocketData();
				}
			}
		}
		return instance;
	}

	/**
	 * Initialize a list to store subscribers in.
	 * Subscribers are differentiated based on functionality
	 */
	private SocketData() {
		pcapSubscribers = new ArrayList<IPcapSubscriber>();
		socketDataSubscribers = new ArrayList<ISocketDataSubscriber>();
	}
	
	public void sendDataToBeTransmitted(byte[] packet) {
		notifyDataTransmitterSubscriber(packet);
	}
	
	public void sendDataToPcap(byte[] packet) {
		notifyPcapSubscriber(packet);
	}
	
	@Override
	public void registerDataTransmitterSubscriber(ISocketDataSubscriber subscriber) {
		// Adds a new Socket Data Subscriber to the list
		if (!socketDataSubscribers.contains(subscriber)) {
			socketDataSubscribers.add(subscriber);
		}
	}

	@Override
	public void unregisterDataTransmitterSubscriber(ISocketDataSubscriber subscriber) {
		// Removes the Socket Data Subscriber from the list
		socketDataSubscribers.remove(subscriber);
		
	}

	@Override
	public void notifyDataTransmitterSubscriber(byte[] packet) {
		// Cycle through the subscribers and notify them
		for(ISocketDataSubscriber socketDataSubscriber: socketDataSubscribers){
			if(socketDataSubscribers.size() > 1) {
				socketDataSubscriber.transmitData(Arrays.copyOf(packet,packet.length));
			} else {
				socketDataSubscriber.transmitData(packet);
			}
		}
	}
	
	/**
	 * register a subscriber who wants to receive packet data
	 * @param subscriber
	 */
	@Override
	public void registerPcapSubscriber(IPcapSubscriber subscriber) {
		// Adds a new Pcap Subscriber to the list
		if  (!pcapSubscribers.contains(subscriber)) {
			pcapSubscribers.add(subscriber);
		}
		
	}

	@Override
	public void unregisterPcapSubscriber(IPcapSubscriber subscriber) {
		// Removes a Pcap Subscriber from the list
		pcapSubscribers.remove(subscriber);
		
	}
	
	

	@Override
	public void notifyPcapSubscriber(byte[] packet) {
		// Cycle through the subscribers and notify them
		for(IPcapSubscriber pcapSubscriber: pcapSubscribers){
			if(pcapSubscribers.size() > 1) {
				pcapSubscriber.writePcap(Arrays.copyOf(packet,packet.length));
			} else {
				pcapSubscriber.writePcap(packet);
			}
		}
		
	}
}
