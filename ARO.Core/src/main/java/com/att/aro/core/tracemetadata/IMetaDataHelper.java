package com.att.aro.core.tracemetadata;

import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;

public interface IMetaDataHelper {

	void saveJSON(String path) throws Exception;
	
	void saveJSON(String path, MetaDataModel metaDataModel) throws Exception;

	MetaDataModel initMetaData(PacketAnalyzerResult result);

	String findAppVersion(TraceDirectoryResult result);

	MetaDataModel loadMetaData(String tracePath) throws Exception;
	
	MetaDataModel initMetaData(String tracePath, String traceDesc, String traceType, String targetedApp, String appProducer);

}