package com.att.aro.core.util;

public interface IResultSubscriber {
	
	/**
	 * Receive a status update.
	 * 
	 * @param sender
	 * @param pass		true: success, false: failed, null: message only
	 * @param result		message
	 */
	public void receiveResults(Class sender, Boolean pass, String result);
	
}
