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
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.PrivateDataType;
import com.att.aro.core.bestpractice.pojo.TransmissionPrivateDataEntry;
import com.att.aro.core.bestpractice.pojo.TransmissionPrivateDataResult;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.peripheral.pojo.PrivateDataInfo;
import com.att.aro.core.preferences.UserPreferences;
import com.att.aro.core.preferences.UserPreferencesFactory;
import com.att.aro.core.searching.ISearchingHandler;
import com.att.aro.core.searching.impl.KeywordSearchingHandler;
import com.att.aro.core.searching.impl.PatternSearchingHandler;
import com.att.aro.core.searching.pojo.SearchingPatternBuilder;
import com.att.aro.core.searching.pojo.SearchingContent;
import com.att.aro.core.searching.pojo.SearchingPattern;
import com.att.aro.core.searching.pojo.SearchingResult;
import com.att.aro.core.util.Util;

public class TransmissionPrivateDataImpl implements IBestPractice {
	
	@InjectLogger
	private static ILogger logger;
	
	@Value("${security.transmissionPrivateData.title}")
	private String overviewTitle;
	
	@Value("${security.transmissionPrivateData.detailedTitle}")
	private String detailedTitle;

	@Value("${security.transmissionPrivateData.desc}")
	private String aboutText;
	
	@Value("${security.transmissionPrivateData.url}")
	private String learnMoreUrl;
	
	@Value("${security.transmissionPrivateData.pass}")
	private String testResultPassText;
	
	@Value("${security.transmissionPrivateData.results}")
	private String testResultAnyText;
	
	@Autowired
	@Qualifier("keywordSearchingHandler")
	private ISearchingHandler keywordHandler;
	
	@Autowired
	@Qualifier("patternSearchingHandler")
	private ISearchingHandler regexHandler;
	
//	@Value("#{'${regex.phone.number}'.split(';')}")
//	private List<String> phoneNumberRegex;
	
//	@Value("#{'${regex.date.birth}'.split(';')}")
//	private List<String> dateBirthRegex;
	
	@Value("#{'${regex.credit.card.american.express}'.split(';')}")
	private List<String> creditCardAmericanExpress;
	
	@Value("#{'${regex.credit.card.mastercard}'.split(';')}")
	private List<String> creditCardMasterCard;
	
	@Value("#{'${regex.credit.card.discover}'.split(';')}")
	private List<String> creditCardDiscover;
	
	@Value("#{'${regex.credit.card.visa}'.split(';')}")
	private List<String> creditCardVisa;
	
//	@Value("#{'${regex.ssn}'.split(';')}")
//	private List<String> ssnList;
	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		Set<TransmissionPrivateDataEntry> entries = new HashSet<TransmissionPrivateDataEntry>();
		entries.addAll(search(tracedata.getSessionlist(), keywordHandler, tracedata.getDeviceKeywords()));
		entries.addAll(search(tracedata.getSessionlist(), regexHandler, null));
		return getTestResult(entries);
	}
	
	/**
	 * searching method depends on the type of searching handler (exact keyword searching or pattern searching)
	 * @param sessions
	 * @param handler
	 * @param privateDataSet
	 * @return
	 */
	private List<TransmissionPrivateDataEntry> search(List<Session> sessions, 
								ISearchingHandler handler, Map<String, String> privateDataSet) {
		
		List<TransmissionPrivateDataEntry> entries = new LinkedList<>();
		SearchingPattern pattern = getSearchingPattern(handler, privateDataSet);
		if (pattern.isEmpty()) {
			return entries;
		}

		for (Session session : sessions) {
			SearchingContent content = getContent(session);
			SearchingResult result = handler.search(pattern, content);
			
			List<String> wordsFound = result.getWords();
			List<String> types = result.getTypes();

			for (int i = 0; i < wordsFound.size(); i++) {
				entries.add(populateEntry(session, wordsFound.get(i), types.get(i)));
			}
		}
		
		return entries;
	}
	
	/**
	 * get searching pattern based on handler type
	 * @param handler
	 * @param privateDataSet
	 * @return
	 */
	private SearchingPattern getSearchingPattern(ISearchingHandler handler, Map<String, String> privateDataSet) {
		SearchingPattern pattern = null;
		if (handler instanceof PatternSearchingHandler) {
			pattern = getRegexPattern(privateDataSet);
		} else if (handler instanceof KeywordSearchingHandler) {
			pattern = getKeywords(privateDataSet);
		}
		return pattern;
	}
	
	/**
	 * populate data for displaying table entry in the UI
	 * @param session
	 * @param word
	 * @return
	 */
	private TransmissionPrivateDataEntry populateEntry(Session session, String word, String type) {
		TransmissionPrivateDataEntry entry = new TransmissionPrivateDataEntry();
		entry.setDestIP(session.getRemoteIP().getHostAddress());
		entry.setDomainName(getDomainName(session));
		entry.setDestPort(session.getRemotePort());
		entry.setPrivateDataType(type);
		entry.setPrivateDataTxt(word);
		entry.setSessionStartTime(session.getSessionStartTime());
		return entry;
	}
	
	/**
	 * get remote domain name in a session
	 * @param session
	 * @return
	 */
	private String getDomainName(Session session) {
		String rawDomainName = session.getDomainName();
		if (rawDomainName == null) {
			return new String();
		}
		return rawDomainName.matches(".*[A-Za-z].*")? rawDomainName : new String();
	}
	
	/**
	 * get content string from given session
	 * @param session
	 * @return
	 */
	private SearchingContent getContent(Session session) {
		byte[] data = session.getStorageUl();
		String str = Util.byteArrayToString(data);
		return new SearchingContent(str);
	}
	
	/**
	 * get keywords from trace folder, exact match keywords from device and user preference
	 * @return
	 */
	private SearchingPattern getKeywords(Map<String, String> keywords) {
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		
		getKeywordsFromPreference(patternBuilder);
		
		if (keywords == null || keywords.isEmpty()) {
			return patternBuilder.build();
		}
		
		Set<String> keySet = keywords.keySet();
		for(String keyword : keySet) {
			patternBuilder.add(keyword, keywords.get(keyword));
		}
		return patternBuilder.build();
	}
	
	/**
	 * get keywords from user preference
	 * @param builder
	 */
	private void getKeywordsFromPreference(SearchingPatternBuilder builder) {
		UserPreferences pref = UserPreferencesFactory.getInstance().create();
		List<PrivateDataInfo> list = pref.getPrivateData();
		if (list != null) {
			for(PrivateDataInfo info : list) {
				if (info.isSelected()) {
					builder.add(info.getValue(), info.getType());
				}
			}
		}
	}
	
	/**
	 * get regular expression defined by ARO, such as SSN, Phone Number and Date of Birth
	 * PLUS
	 * user defined expression from user setting
	 * 
	 * @return
	 */
	private SearchingPattern getRegexPattern(Map<String, String> expressions) {
		SearchingPatternBuilder patternBuilder = new SearchingPatternBuilder();
		
		// ARO defined expression (default)
//		addRegex(patternBuilder, phoneNumberRegex, PrivateDataType.regex_phone_number.toString());
//		addRegex(patternBuilder, dateBirthRegex, PrivateDataType.regex_date_birth.toString());
		addRegex(patternBuilder, creditCardAmericanExpress, PrivateDataType.regex_credit_card_number.toString());
		addRegex(patternBuilder, creditCardMasterCard, PrivateDataType.regex_credit_card_number.toString());
		addRegex(patternBuilder, creditCardDiscover, PrivateDataType.regex_credit_card_number.toString());
		addRegex(patternBuilder, creditCardVisa, PrivateDataType.regex_credit_card_number.toString());
//		addRegex(patternBuilder, ssnList, PrivateDataType.regex_ssn.toString());
		
		if (expressions == null || expressions.isEmpty()) {
			return patternBuilder.build();
		}
		
		// user defined expression (not implemented yet)
		Set<String> keySet = expressions.keySet();
		for(String expression : keySet) {
			patternBuilder.add(expression, expressions.get(expression));
		}
		return patternBuilder.build();
	}
	
	private void addRegex(SearchingPatternBuilder patternBuilder, 
										List<String> regexList, String dataType) {
		for(String regex : regexList) {
			patternBuilder.add(regex, dataType);
		}
	}
	
	/**
	 * generate best practice test result
	 * 
	 * @param entries
	 * @return
	 */
	private TransmissionPrivateDataResult getTestResult(Set<TransmissionPrivateDataEntry> entries) {
		TransmissionPrivateDataResult result = new TransmissionPrivateDataResult();
		
		String testResultText;
		if (passTest(entries)) {
			result.setResultType(BPResultType.PASS);
			testResultText = MessageFormat.format(testResultPassText, 
													ApplicationConfig.getInstance().getAppShortName(), 
													entries.size());
		} else if(failTest(entries)) {
			result.setResultType(BPResultType.FAIL);
			testResultText = MessageFormat.format(testResultAnyText, 
													ApplicationConfig.getInstance().getAppShortName(), 
													entries.size());
		} else {
			result.setResultType(BPResultType.WARNING);
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
	 * best practice pass rule
	 * @param entries
	 * @return
	 */
	private boolean passTest(Set<TransmissionPrivateDataEntry> entries) {
		return entries.isEmpty();
	}
	
	/**
	 * best practice fail rule
	 * @param entries
	 * @return
	 */
	private boolean failTest(Set<TransmissionPrivateDataEntry> entries) {
		return entries.size() > 5;
	}
}
