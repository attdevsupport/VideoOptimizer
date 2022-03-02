package com.att.aro.core.peripheral;

import java.io.File;

import com.att.aro.core.tracemetadata.pojo.MetaDataModel;

public interface IMetaDataReadWrite {

	public MetaDataModel readData(String tracePath);

	public boolean save(File traceFolder, MetaDataModel MetaDataModel) throws Exception;

}
