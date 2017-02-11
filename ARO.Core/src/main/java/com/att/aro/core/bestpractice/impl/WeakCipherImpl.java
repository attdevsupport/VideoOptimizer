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
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.WeakCipherEntry;
import com.att.aro.core.bestpractice.pojo.WeakCipherResult;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.TCPPacket;
import com.att.aro.core.util.WeakCipherUtil;

public class WeakCipherImpl implements IBestPractice {

	@InjectLogger
	private static ILogger logger;
	
	@Value("${security.weakCipher.title}")
	private String overviewTitle;
	
	@Value("${security.weakCipher.detailedTitle}")
	private String detailedTitle;

	@Value("${security.weakCipher.desc}")
	private String aboutText;
	
	@Value("${security.weakCipher.url}")
	private String learnMoreUrl;
	
	@Value("${security.weakCipher.pass}")
	private String testResultPassText;
	
	@Value("${security.weakCipher.results}")
	private String testResultAnyText;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		Set<WeakCipherEntry> entries = new HashSet<>();
		List<Session> sessions = tracedata.getSessionlist();
		for(Session session : sessions) {
			Set<String> weakCiphers = getWeakCiphersInSession(session);
			for(String weakCipher : weakCiphers) {
				entries.add(populateEntry(session, weakCipher));
			}
		}
		return getTestResult(entries);
	}
	
	/**
	 * get weak ciphers in session
	 * @param session
	 * @return
	 */
	private Set<String> getWeakCiphersInSession(Session session) {
		Set<String> weakCiphers = new HashSet<>();
		List<PacketInfo> packetInfos = session.getPackets();
		for(PacketInfo info : packetInfos) {
			if (info.getPacket() instanceof TCPPacket) {
				TCPPacket tcpPacket = (TCPPacket) info.getPacket();
				weakCiphers.addAll(tcpPacket.getWeakCipherSuites());
			}
		}
		return weakCiphers;
	}
	
	private WeakCipherEntry populateEntry(Session session, String weakCipher) {
		WeakCipherEntry entry = new WeakCipherEntry();
		entry.setDestIP(session.getRemoteIP().getHostAddress());
		entry.setDestPort(String.valueOf(session.getRemotePort()));
		entry.setCipherHex(weakCipher);
		entry.setCipherName(WeakCipherUtil.getCipherIdentifier(weakCipher).name());
		entry.setSessionStartTime(session.getSessionStartTime());
		return entry;
	}

	/**
	 * get test result
	 * @param entries
	 * @return
	 */
	private WeakCipherResult getTestResult(Set<WeakCipherEntry> entries) {
		WeakCipherResult result = new WeakCipherResult();
		
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
	private boolean passTest(Set<WeakCipherEntry> entries) {
		return entries.size() == 0;
	}
}
