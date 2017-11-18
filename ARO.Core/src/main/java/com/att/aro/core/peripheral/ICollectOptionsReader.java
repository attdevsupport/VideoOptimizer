package com.att.aro.core.peripheral;

import com.att.aro.core.peripheral.pojo.CollectOptions;

public interface ICollectOptionsReader {
	CollectOptions readData(String directory);

}
