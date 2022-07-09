/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
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

	boolean updateMetaData(PacketAnalyzerResult packetAnalyzerResult);

	boolean saveJSON();

}