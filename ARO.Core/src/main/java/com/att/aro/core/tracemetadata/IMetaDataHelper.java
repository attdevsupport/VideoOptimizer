package com.att.aro.core.tracemetadata;


import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;


public interface IMetaDataHelper {

	String getJson();

	String getJson(MetaDataModel metaDataModel);
	
	void saveJSON(String path) throws Exception;

	MetaDataModel initMetaData(TraceDirectoryResult result);

	String findAppVersion(TraceDirectoryResult result);

	MetaDataModel loadMetaData(String tracePath) throws Exception;
	
	MetaDataModel initMetaData(String tracePath, String traceDesc, String traceType, String targetedApp, String appProducer);

}