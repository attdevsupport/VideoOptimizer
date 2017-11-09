package com.att.arotcpcollector.socket;

/**
 * Created by deltonnoronha on 5/23/17.
 */

interface IReceiveDataResponse {
	public void registerDataReceivedSubscribers(IDataReceivedSubscriber subscriber);
	public void unregisterDataReceivedSubscribers(IDataReceivedSubscriber subscriber);
	public void notifyDataReceivedSubscribers(byte[] packet);
}
