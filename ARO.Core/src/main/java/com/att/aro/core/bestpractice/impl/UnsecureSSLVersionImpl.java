/*
 *  Copyright 2017 AT&T
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.UnsecureSSLVersionEntry;
import com.att.aro.core.bestpractice.pojo.UnsecureSSLVersionResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.TCPPacket;

public class UnsecureSSLVersionImpl implements IBestPractice {

	private static final String DELIMITER = ", ";

	private static final Logger LOGGER = LogManager.getLogger(UnsecureSSLVersionImpl.class.getName());

	@Value("${security.unsecureSSLVersion.title}")
	private String overviewTitle;
	
	@Value("${security.unsecureSSLVersion.detailedTitle}")
	private String detailedTitle;

	@Value("${security.unsecureSSLVersion.desc}")
	private String aboutText;
	
	@Value("${security.unsecureSSLVersion.url}")
	private String learnMoreUrl;
	
	@Value("${security.unsecureSSLVersion.pass}")
	private String testResultPassText;
	
	@Value("${security.unsecureSSLVersion.results}")
	private String testResultAnyText;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		Set<UnsecureSSLVersionEntry> entries = new HashSet<>();
		
		try {
			List<Session> sessions = tracedata.getSessionlist();
			for(Session session : sessions) {
				Set<String> unsecureSSLVersions = getUnsecureSSLVersionsInSession(session);
				if (!unsecureSSLVersions.isEmpty()) {
					String unsecureSSLVersionsStr = stringifyUnsecureSSLVersions(unsecureSSLVersions);
					UnsecureSSLVersionEntry entry = populateEntry(session, unsecureSSLVersionsStr);
					entries.add(entry);
				}
			}
		} catch(Exception e) {
			LOGGER.error("Error happened when running unsecure SSL Versions test :: Caused by: " + e.getMessage());
		}

		return getTestResult(entries);
	}
	
	/**
	 * get unique unsecure SSL versions from a session
	 * @param session
	 * @return
	 */
	private Set<String> getUnsecureSSLVersionsInSession(Session session) {
		Set<String> unsecureSSLVersions = new HashSet<>();
		List<PacketInfo> packetInfos = session.getPackets();
		for(PacketInfo info : packetInfos) {
			if (info.getPacket() instanceof TCPPacket) {
				TCPPacket tcpPacket = (TCPPacket) info.getPacket();
				unsecureSSLVersions.addAll(tcpPacket.getUnsecureSSLVersions());
			}
		}
		
		return unsecureSSLVersions;
	}
	
	/**
	 * stringify unsecure SSL versions
	 * @param unsecureSSLVersions
	 * @return
	 */
	private String stringifyUnsecureSSLVersions(Set<String> unsecureSSLVersions) {
		StringBuilder strBuilder = new StringBuilder();
		for(String unsecureSSLVersion : unsecureSSLVersions) {
			strBuilder.append(unsecureSSLVersion)
					  .append(DELIMITER);
		}
		return strBuilder.substring(0, strBuilder.length() - DELIMITER.length());
	}
	
	/**
	 * populate unsecure SSL table entry for UI display
	 * @param session
	 * @param unsecureSSLVersions
	 * @return
	 */
	private UnsecureSSLVersionEntry populateEntry(Session session, String unsecureSSLVersions) {
		UnsecureSSLVersionEntry entry = new UnsecureSSLVersionEntry();
		entry.setDestIP(session.getRemoteIP().getHostAddress());
		entry.setDestPort(String.valueOf(session.getRemotePort()));
		entry.setUnsecureSSLVersions(unsecureSSLVersions);
		entry.setSessionStartTime(session.getSessionStartTime());
		return entry;
	}
	
	/**
	 * get test result
	 * @param entries
	 * @return
	 */
	private UnsecureSSLVersionResult getTestResult(Set<UnsecureSSLVersionEntry> entries) {
		UnsecureSSLVersionResult result = new UnsecureSSLVersionResult();
		
		String testResultText;
		if (passTest(entries)) {
			result.setResultType(BPResultType.PASS);
			testResultText = MessageFormat.format(testResultPassText, 
													ApplicationConfig.getInstance().getAppShortName(), 
													entries.size());
		} else {
			result.setResultType(BPResultType.FAIL);
			testResultText = MessageFormat.format(testResultAnyText, 
													ApplicationConfig.getInstance().getAppShortName(), 
													entries.size());
		}
		
		result.setOverviewTitle(overviewTitle);
		result.setDetailTitle(detailedTitle);
		result.setAboutText(aboutText);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setResults(new LinkedList<>(entries));
		result.setResultText(testResultText);
		
		return result;
	}
	
	/**
	 * test pass
	 * @param entries
	 * @return
	 */
	private boolean passTest(Set<UnsecureSSLVersionEntry> entries) {
		return entries.size() == 0;
	}
}
