package com.att.aro.core.peripheral;

import com.att.aro.core.peripheral.pojo.VideoStreamStartupData;

public interface IVideoStartupReadWrite {

	public VideoStreamStartupData readData(String tracePath);

	public boolean save(String path, VideoStreamStartupData videoStreamStartupData) throws Exception;

}
