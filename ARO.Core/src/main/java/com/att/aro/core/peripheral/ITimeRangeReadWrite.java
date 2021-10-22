package com.att.aro.core.peripheral;

import java.io.File;

import com.att.aro.core.peripheral.pojo.TraceTimeRange;

public interface ITimeRangeReadWrite {

	public TraceTimeRange readData(File tracePath);

	public boolean save(File traceFolder, TraceTimeRange traceTimeRange) throws Exception;

}
