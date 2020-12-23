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

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.ForwardSecrecyEntry;
import com.att.aro.core.bestpractice.pojo.ForwardSecrecyResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.TCPPacket;
import com.att.aro.core.util.ForwardSecrecyUtil;

public class ForwardSecrecyImpl implements IBestPractice {
	@Value("${security.forwardSecrecy.title}")
	private String overviewTitle;
	
	@Value("${security.forwardSecrecy.detailedTitle}")
	private String detailedTitle;

	@Value("${security.forwardSecrecy.desc}")
	private String aboutText;
	
	@Value("${security.forwardSecrecy.url}")
	private String learnMoreUrl;
	
	@Value("${security.forwardSecrecy.pass}")
	private String testResultPassText;
	
	@Value("${security.forwardSecrecy.results}")
	private String testResultAnyText;

	@Value("${security.forwardSecrecy.excel.results}")
    private String testExcelResults;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		Set<ForwardSecrecyEntry> entries = new HashSet<>();
		List<Session> sessions = tracedata.getSessionlist();
		for(Session session : sessions) {
			String selectedCipher = getSelectedCipherInSession(session);
			if (selectedCipher != null) {
				entries.add(populateEntry(session, selectedCipher));
			}
		}
		return getTestResult(entries);
	}
	
	/**
	 * get weak ciphers in session
	 * @param session
	 * @return
	 */
	private String getSelectedCipherInSession(Session session) {
		List<PacketInfo> packetInfos = session.getPackets();
		for(PacketInfo info : packetInfos) {
			if (info.getPacket() instanceof TCPPacket) {
				TCPPacket tcpPacket = (TCPPacket) info.getPacket();
				if (tcpPacket.getSelectedCipherSuite() != null) {
					return tcpPacket.getSelectedCipherSuite();
				}
			}
		}
		return null;
	}
	
	private ForwardSecrecyEntry populateEntry(Session session, String cipher) {
		ForwardSecrecyEntry entry = new ForwardSecrecyEntry();
		entry.setDestIP(session.getRemoteIP().getHostAddress());
		entry.setDestPort(String.valueOf(session.getRemotePort()));
		entry.setCipherHex(cipher);
		entry.setCipherName(ForwardSecrecyUtil.getCipherIdentifier(cipher).name());
		entry.setSessionStartTime(session.getSessionStartTime());
		return entry;
	}

	/**
	 * get test result
	 * @param entries
	 * @return
	 */
	private ForwardSecrecyResult getTestResult(Set<ForwardSecrecyEntry> entries) {
		ForwardSecrecyResult result = new ForwardSecrecyResult();
		
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

		result.setResultExcelText(MessageFormat.format(testExcelResults, result.getResultType().getDescription(), entries.size()));
		result.setOverviewTitle(overviewTitle);
		result.setDetailTitle(detailedTitle);
		result.setAboutText(aboutText);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setResults(new LinkedList<>(entries));
		result.setResultText(testResultText);
		
		return result;
	}
	
	/**
	 * test pass
	 * @param entries
	 * @return
	 */
	private boolean passTest(Set<ForwardSecrecyEntry> entries) {
		return entries.size() == 0;
	}
}
