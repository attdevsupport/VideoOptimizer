package com.att.aro.core.videoanalysis.videoframe;

public interface FrameReceiver {

	/**
	 * 
	 * @param sender
	 * @param status
	 */
	public void receiveResults(Class<?> sender, FrameStatus status);

}
