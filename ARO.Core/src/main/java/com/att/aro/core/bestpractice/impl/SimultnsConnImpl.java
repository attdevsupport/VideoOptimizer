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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.SimultnsConnEntry;
import com.att.aro.core.bestpractice.pojo.SimultnsConnectionResult;
import com.att.aro.core.bestpractice.pojo.SimultnsUtil;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.SessionValues;


public class SimultnsConnImpl implements IBestPractice {

	@InjectLogger
	private static ILogger logger;

	@Value("${connections.simultaneous.title}")
	private String overviewTitle;

	@Value("${connections.simultaneous.detailedTitle}")
	private String detailTitle;

	@Value("${connections.simultaneous.desc}")
	private String aboutText;

	@Value("${connections.simultaneous.url}")
	private String learnMoreUrl;

	@Value("${connections.simultaneous.pass}")
	private String textResultPass;

	@Value("${connections.simultaneous.results}")
	private String textResults;
	
	int maxConnections = 3;

	private PacketAnalyzerResult traceDataResult = null;

	private List<SimultnsConnEntry> simultnsConnectionEntryList;
	
	private SortedMap<Double, SimultnsConnEntry> simultnsConnectionEntryMap;
	
	SimultnsUtil simultnsUtil = new SimultnsUtil();

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult traceData) {
		traceDataResult = traceData;
		SimultnsConnectionResult result = new SimultnsConnectionResult();
		simultnsConnectionEntryList = new ArrayList<SimultnsConnEntry>();
		simultnsConnectionEntryMap = new TreeMap<Double, SimultnsConnEntry>();


		populateSimultConnList();
		
		for(Map.Entry<Double, SimultnsConnEntry> simultnsConnEntryMap : simultnsConnectionEntryMap.entrySet()) {
			if(simultnsConnEntryMap.getValue()!=null) {
				simultnsConnectionEntryList.add(simultnsConnEntryMap.getValue());
			}
			
		}

		result.setResults(simultnsConnectionEntryList);
		String text = "";
		if (simultnsConnectionEntryList.isEmpty()) {
			result.setResultType(BPResultType.PASS);
			text = textResultPass;
			result.setResultText(text);
		} else {
			result.setResultType(BPResultType.FAIL);

			result.setResultText(textResults);
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		return result;
	}

	private void populateSimultConnList() {
		List<Session> sessions = traceDataResult.getSessionlist();

		if (sessions == null || sessions.size() <= 0) {
			simultnsConnectionEntryList = Collections.emptyList();
			return;
		}
		Map<String, ArrayList<Session>> distinctMap = simultnsUtil.getDistinctMap(sessions);
		HashMap<String, List<SessionValues>> ipMap = new HashMap<String, List<SessionValues>>();

		for (Map.Entry<String, ArrayList<Session>> entry : distinctMap.entrySet()) {
			ArrayList<Session> tempList = entry.getValue();
			tempList.trimToSize();
			List<SessionValues> timeMap = simultnsUtil.createDomainsTCPSessions(tempList);
			ipMap.put(entry.getKey(), timeMap);
		}

		for (HashMap.Entry<String, List<SessionValues>> ipEntry : ipMap.entrySet()) {
			SimultnsConnEntry simultnsConnEntry = simultnsUtil.getTimeMap(ipEntry.getValue(), maxConnections, false);
			if(simultnsConnEntry!=null) {
				simultnsConnectionEntryMap.put(simultnsConnEntry.getTimeStamp(), simultnsConnEntry);
			}
			
		}

	}
}
