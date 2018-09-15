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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.MultiSimultnsConnectionResult;
import com.att.aro.core.bestpractice.pojo.MultipleConnectionsEntry;
import com.att.aro.core.bestpractice.pojo.SimultnsUtil;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.SessionValues;

public class MultipleSimultnsConnImpl implements IBestPractice {
	@Value("${connections.multiSimultaneous.title}")
	private String overviewTitle;
	@Value("${connections.multiSimultaneous.detailedTitle}")
	private String detailTitle;
	@Value("${connections.multiSimultaneous.desc}")
	private String aboutText;
	@Value("${connections.multiSimultaneous.url}")
	private String learnMoreUrl;
	@Value("${connections.multiSimultaneous.pass}")
	private String textResultPass;
	@Value("${connections.multiSimultaneous.results}")
	private String textResults;
	int maxConnections = 12;
	private PacketAnalyzerResult traceDataResult = null;
	private List<MultipleConnectionsEntry> simultnsConnectionAllServersEntryList;
	private SortedMap<Double, MultipleConnectionsEntry> simultnsConnectionAllServersEntryMap;
	SimultnsUtil simultnsUtil = new SimultnsUtil();

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult traceData) {
		traceDataResult = traceData;
		MultiSimultnsConnectionResult result = new MultiSimultnsConnectionResult();
		simultnsConnectionAllServersEntryList = new ArrayList<MultipleConnectionsEntry>();
		simultnsConnectionAllServersEntryMap = new TreeMap<Double, MultipleConnectionsEntry>();
		populateAllSimultConnList();
		for (Map.Entry<Double, MultipleConnectionsEntry> simultnsConnAllServersEntryMap : simultnsConnectionAllServersEntryMap
				.entrySet()) {
			if (simultnsConnAllServersEntryMap.getValue() != null) {
				simultnsConnectionAllServersEntryList.add(simultnsConnAllServersEntryMap.getValue());
			}
		}
		result.setResults(simultnsConnectionAllServersEntryList);
		String text = "";
		if (simultnsConnectionAllServersEntryList.isEmpty()) {
			result.setResultType(BPResultType.PASS);
			text = MessageFormat.format(textResultPass, simultnsConnectionAllServersEntryList.size());
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

	private void populateAllSimultConnList() {
		List<Session> sessions = traceDataResult.getSessionlist();
		if (sessions == null || sessions.size() <= 0) {
			simultnsConnectionAllServersEntryList = Collections.emptyList();
			return;
		}
		List<Session> tempSessionList = new ArrayList<Session>();
		Map<String, ArrayList<Session>> distinctMap = simultnsUtil.getDistinctMap(sessions);
		for (Map.Entry<String, ArrayList<Session>> entry : distinctMap.entrySet()) {
			ArrayList<Session> tempList = entry.getValue();
			for (int iterator = 0; iterator < tempList.size(); iterator++) {
				tempSessionList.add(tempList.get(iterator));
			}
		}
		List<SessionValues> sessionValuesList = simultnsUtil.createDomainsTCPSessions(tempSessionList);
		MultipleConnectionsEntry simultnsConnEntry = simultnsUtil.getTimeMap(sessionValuesList, maxConnections, true);
		if (simultnsConnEntry != null) {
			simultnsConnectionAllServersEntryMap.put(simultnsConnEntry.getTimeStamp(), simultnsConnEntry);
		}
	}
}