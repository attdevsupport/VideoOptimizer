package com.att.aro.core.peripheral;

import com.att.aro.core.peripheral.pojo.VideoStreamStartup;

public interface IVideoStartupReadWrite {

	VideoStreamStartup readData(String tracePath);

	void save(String path, VideoStreamStartup videoStreamStartup) throws Exception;

}
