/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.bestpractice.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.AdAnalyticsResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.MultipleConnectionsEntry;
import com.att.aro.core.bestpractice.pojo.SimultnsUtil;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.SessionValues;

public class AdAnalyticsImpl implements IBestPractice {
	@Value("${connections.adAnalytics.title}")
	private String overviewTitle;
	@Value("${connections.adAnalytics.detailedTitle}")
	private String detailTitle;
	@Value("${connections.adAnalytics.desc}")
	private String aboutText;
	@Value("${connections.adAnalytics.url}")
	private String learnMoreUrl;
	@Value("${connections.adAnalytics.pass}")
	private String textResultPass;
	@Value("${connections.adAnalytics.results}")
	private String textResults;
	private static final Logger LOG = LogManager.getLogger(AdAnalyticsImpl.class.getName());

	@Autowired
	IHttpRequestResponseHelper rrhelper;
	private double totalAdAnalyticsBytes = 0.0f;
	List<MultipleConnectionsEntry> entrylist = new ArrayList<MultipleConnectionsEntry>();
	private int totalConnections = 0;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		AdAnalyticsResult result = new AdAnalyticsResult();
		SimultnsUtil simultnsUtil = new SimultnsUtil();
		List<Session> sessions = tracedata.getSessionlist();
		String adAnalyticsTxt = tracedata.getTraceresult().getTraceDirectory() + System.getProperty("file.separator");
		File folder = new File(adAnalyticsTxt);
		Map<String, ArrayList<Session>> distinctMap = simultnsUtil.getDistinctMap(sessions);
		HashMap<String, List<SessionValues>> ipMap = new HashMap<String, List<SessionValues>>();
		for (Map.Entry<String, ArrayList<Session>> entry : distinctMap.entrySet()) {
			ArrayList<Session> tempList = entry.getValue();
			tempList.trimToSize();
			List<SessionValues> sessionMap = simultnsUtil.createDomainsTCPSessions(tempList);
			ipMap.put(entry.getKey(), sessionMap);
		}
		createResultList(folder, ipMap);
		result.setResults(entrylist);
		String text = "";
		if (getResultType().equals(BPResultType.PASS)) {
			result.setResultType(BPResultType.PASS);
			text = MessageFormat.format(textResultPass, ApplicationConfig.getInstance().getAppShortName());
			result.setResultText(text);
		} else if (getResultType().equals(BPResultType.FAIL)) {
			result.setResultType(BPResultType.FAIL);
			text = MessageFormat.format(textResults, totalAdAnalyticsBytes);
			result.setResultText(text);
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setBytes(totalAdAnalyticsBytes);
		return result;
	}

	private BPResultType getResultType() {
		BPResultType resultType = BPResultType.PASS;
		if (!entrylist.isEmpty()) {
			if (entrylist.size() * 100 / totalConnections > 5) {
				for (int i = 0; i < entrylist.size(); i++) {
					totalAdAnalyticsBytes += entrylist.get(i).getByteCount();
				}
				resultType = BPResultType.FAIL;
			}
		}
		return resultType;
	}

	private void createResultList(File folder, HashMap<String, List<SessionValues>> ipMap) {
		for (HashMap.Entry<String, List<SessionValues>> ipEntry : ipMap.entrySet()) {
			MultipleConnectionsEntry adAnalyticsEntry = getAdAnalyticsEntry(ipEntry.getValue(), folder);
			if (adAnalyticsEntry != null) {
				entrylist.add(adAnalyticsEntry);
			}
		}
	}

	private MultipleConnectionsEntry getAdAnalyticsEntry(List<SessionValues> sessionValueList, File folder) {
		String ipInside = "";
		MultipleConnectionsEntry multipleConnectionsEntry = null;
		TreeMap<String, HttpRequestResponseInfo> reqRespMap = new TreeMap<String, HttpRequestResponseInfo>();
		double sessionStartTime = 0.0;
		for (SessionValues indSessionVal : sessionValueList) {
			totalConnections++;
			sessionStartTime = indSessionVal.getStartTime();
			ipInside = indSessionVal.getIp();
			if (indSessionVal.getReqRespInfo() != null) {
				reqRespMap.put(ipInside, indSessionVal.getReqRespInfo());
			}
			multipleConnectionsEntry = getMultipleConnectionsEntry(ipInside, reqRespMap, folder,
					indSessionVal.getIndSession(), sessionStartTime);
		}
		return multipleConnectionsEntry;
	}

	private MultipleConnectionsEntry getMultipleConnectionsEntry(String ipInside,
			TreeMap<String, HttpRequestResponseInfo> reqRespMap, File folder, Session session,
			double sessionStartTime) {
		boolean adAnaltyicsfound = false;
		if (reqRespMap.get(ipInside) != null) {
			try {
				BufferedReader reader = new BufferedReader(
						new FileReader(new File(getClass().getResource("/ads_analytics.txt").getFile())));
				adAnaltyicsfound = isAdAnaltyicsfound(reqRespMap.get(ipInside).getAssocReqResp(), session, reader);
			} catch (FileNotFoundException e) {
				LOG.error("Ad Analytics text file not found");
			}
		}
		if (adAnaltyicsfound) {
			return new MultipleConnectionsEntry(reqRespMap.get(ipInside),
					ipInside.substring(ipInside.lastIndexOf('/') + 1, ipInside.length()), sessionStartTime,
					reqRespMap.get(ipInside).getAssocReqResp().getContentLength(), ipInside);
		}
		return null;
	}

	public boolean isAdAnaltyicsfound(HttpRequestResponseInfo response, Session session, BufferedReader reader) {
		boolean adAnaltyicsfound = false;
		if (response != null && response.getDirection().equals(HttpDirection.RESPONSE)) {
			try {
				String line = "";
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("#") && !line.isEmpty()) {
						if (response.getHostName().equalsIgnoreCase(line)) {
							adAnaltyicsfound = true;
							totalAdAnalyticsBytes += response.getContentLength();
							break;
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Error in retrieving Content");
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return adAnaltyicsfound;
	}
}
