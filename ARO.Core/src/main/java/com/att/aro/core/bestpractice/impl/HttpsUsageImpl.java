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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.HttpsUsageEntry;
import com.att.aro.core.bestpractice.pojo.HttpsUsageResult;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.packetreader.pojo.TCPPacket;

public class HttpsUsageImpl implements IBestPractice {
	@InjectLogger
	private static ILogger logger;
	@Value("${security.httpsUsage.title}")
	private String overviewTitle;
	@Value("${security.httpsUsage.detailedTitle}")
	private String detailedTitle;
	@Value("${security.httpsUsage.desc}")
	private String aboutText;
	@Value("${security.httpsUsage.url}")
	private String learnMoreUrl;
	@Value("${security.httpsUsage.pass}")
	private String testResultPassText;
	@Value("${security.httpsUsage.results}")
	private String testResultAnyText;
	// exportAll is used for testing
	@Value("$exportall.csvHttpsUsageHTTPDetected}")
	private String exportAll;
	/*
	 * Total number of connections/TCP sessions in current trace.
	 */
	private int totalNumConnectionsCurrentTrace;
	/*
	 * Total number of HTTP connections (TCP sessions which we do not find any
	 * SSL packet in) in current trace.
	 */
	private int totalNumHttpConnectionsCurrentTrace;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		/*
		 * Reset these total numbers before each test-run due to bean being
		 * singleton.
		 */
		totalNumConnectionsCurrentTrace = 0;
		totalNumHttpConnectionsCurrentTrace = 0;
		List<Session> sessions = tracedata.getSessionlist();
		Map<InetAddress, List<Session>> ipSessionsMap = buildIpSessionsMap(sessions);
		List<HttpsUsageEntry> httpsUsageEntries = buildHttpsUsageEntry(ipSessionsMap);
		int httpConnectionsPercentageCurrentTrace = getHttpConnectionsPercentage(totalNumHttpConnectionsCurrentTrace,
				totalNumConnectionsCurrentTrace);
		HttpsUsageResult result = new HttpsUsageResult();
		String testResultText = "";
		if (passTest()) {
			result.setResultType(BPResultType.PASS);
			testResultText = MessageFormat.format(testResultPassText, ApplicationConfig.getInstance().getAppShortName(),
					totalNumHttpConnectionsCurrentTrace);
		} else if (failTest(httpsUsageEntries, httpConnectionsPercentageCurrentTrace)) {
			result.setResultType(BPResultType.FAIL);
			testResultText = MessageFormat.format(testResultAnyText, ApplicationConfig.getInstance().getAppShortName(),
					totalNumHttpConnectionsCurrentTrace, httpConnectionsPercentageCurrentTrace);
		} else {
			result.setResultType(BPResultType.WARNING);
			testResultText = MessageFormat.format(testResultAnyText, ApplicationConfig.getInstance().getAppShortName(),
					totalNumHttpConnectionsCurrentTrace, httpConnectionsPercentageCurrentTrace);
		}
		result.setOverviewTitle(overviewTitle);
		result.setDetailTitle(detailedTitle);
		result.setAboutText(aboutText);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setResults(httpsUsageEntries);
		result.setResultText(testResultText);
		result.setExportAll(exportAll);
		return result;
	}

	/*
	 * Passes test if all TCP sessions in the current trace contain one or more
	 * SSL packets.
	 */
	private boolean passTest() {
		return totalNumHttpConnectionsCurrentTrace == 0;
	}

	/*
	 * Fails test if 3 or more IPs use HTTP or >25% of all connections use HTTP.
	 */
	private boolean failTest(List<HttpsUsageEntry> httpsUsageEntries, int httpConnectionsPercentageCurrentTrace) {
		return httpsUsageEntries.size() >= 3 || httpConnectionsPercentageCurrentTrace == 25;
	}

	private Integer getHttpConnectionsPercentage(int numHttpConnections, int numConnections) {
		double percentage = 0.00f;
		if (numConnections != 0) {
			percentage = (double) numHttpConnections * 100 / numConnections;
		}
		return (int) Math.round(percentage);
	}

	private Integer getHttpTrafficPercentage(int httpTrafficInByte, int trafficInByte) {
		double percentage = 0.00f;
		if (trafficInByte != 0) {
			percentage = (double) httpTrafficInByte * 100 / trafficInByte;
		}
		return (int) Math.round(percentage);
	}

	/*
	 * If an IP contains a session that does not contain any SSL packet, we
	 * create a HttpsUsageEntry for it.
	 *
	 * This method loops through the IP-sessions map, and returns the list of
	 * HttpsUsageEntry created. If no IP from the IP-sessions map contains a
	 * connection that we consider as HTTP, the method returns an empty list.
	 */
	private List<HttpsUsageEntry> buildHttpsUsageEntry(Map<InetAddress, List<Session>> ipSessionsMap) {
		List<HttpsUsageEntry> results = new ArrayList<HttpsUsageEntry>();
		for (Map.Entry<InetAddress, List<Session>> ipSessions : ipSessionsMap.entrySet()) {
			int totalNumConnectionsCurrentIp = 0;
			int totalNumHttpConnectionsCurrentIp = 0;
			int totalHttpTrafficInByteCurrentIp = 0;
			int totalTrafficInByteCurrentIp = 0;
			int httpTrafficPercentage = 0;
			for (Session session : ipSessions.getValue()) {
				boolean isSslSession = false;
				boolean isHttpsConnection = false;
				// Excluding UDP Sessions
				if (session.isUDP()) {
					continue;
				}
				List<PacketInfo> packetsInfo = session.getPackets();
				if (packetsInfo.isEmpty()) {
					logger.error("Session without packets! Session's remote IP and port: "
							+ session.getRemoteIP().getHostAddress() + ":" + session.getRemotePort());
					continue;
				}
				/*
				 * Determine if the TCP session is using SSL/TLS. If we find at
				 * least one TCP packet that contains one or more SSL record(s),
				 * we consider that session a SSL session.
				 */
				for (PacketInfo packetInfo : packetsInfo) {
					Packet packet = packetInfo.getPacket();
					if ((packet instanceof TCPPacket) && ((TCPPacket) packet).containsSSLRecord()) {
						isSslSession = true;
						break;
					}
				}
				/*
				 * Calculate traffic size for the TCP session
				 */
				// total packet size (ip header + tcp header + payload) of all
				// the packets in the session
				int totalPacketsSize = 0;
				// total payload size of all the packets in the session
				int totalPacketsPayloadSize = 0;
				for (PacketInfo packetInfo : packetsInfo) {
					totalPacketsSize += packetInfo.getLen();
					totalPacketsPayloadSize += packetInfo.getPayloadLen();
				}
				totalTrafficInByteCurrentIp += totalPacketsSize;
				totalNumConnectionsCurrentIp++;
				/*
				 * If the session does not contain any data (i.e. payload of all
				 * the packets in the session is zero), or if it contains 1 or
				 * more SSL packet, we group the session under the HTTPS
				 * connection category.
				 */
				if (totalPacketsPayloadSize == 0 || isSslSession) {
					isHttpsConnection = true;
				}
				if (!isHttpsConnection) {
					totalHttpTrafficInByteCurrentIp += totalPacketsSize;
					totalNumHttpConnectionsCurrentIp++;
				}
			} // End all sessions associated to an IP
			if (totalNumHttpConnectionsCurrentIp > 0) {
				BigDecimal totalTrafficInKBCurrentIp = new BigDecimal(totalTrafficInByteCurrentIp)
						.divide(new BigDecimal(1000), 3, RoundingMode.HALF_UP);
				BigDecimal totalHttpTrafficInKBCurrentIp = new BigDecimal(totalHttpTrafficInByteCurrentIp)
						.divide(new BigDecimal(1000), 3, RoundingMode.HALF_UP);
				int httpConnectionsPercentage = getHttpConnectionsPercentage(totalNumHttpConnectionsCurrentIp,
						totalNumConnectionsCurrentIp);
				/*
				 * Initialize percentage to be 0 to avoid getting the
				 * divide-by-zero exception for any unexpected reason.
				 */
				if (totalTrafficInByteCurrentIp <= 0) {
					logger.error("Total traffic size of all TCP sessions is zero or less ("
							+ totalTrafficInByteCurrentIp + " byte)! IP: " + ipSessions.getKey().getHostAddress());
				} else {
					httpTrafficPercentage = getHttpTrafficPercentage(totalHttpTrafficInByteCurrentIp,
							totalTrafficInByteCurrentIp);
				}
				String parentDomainName = getParentDomainName(ipSessions.getKey(), ipSessions.getValue());
				results.add(new HttpsUsageEntry(ipSessions.getKey().getHostAddress(), parentDomainName,
						totalNumConnectionsCurrentIp, totalNumHttpConnectionsCurrentIp, httpConnectionsPercentage,
						totalTrafficInKBCurrentIp, totalHttpTrafficInKBCurrentIp, httpTrafficPercentage));
			}
			totalNumHttpConnectionsCurrentTrace += totalNumHttpConnectionsCurrentIp;
			totalNumConnectionsCurrentTrace += totalNumConnectionsCurrentIp;
		} // End all IPs
		return results;
	}

	/*
	 * Gets the second-level domain and the top-level domain for the given IP in
	 * the current trace. If the domain name value of the given IP is an IP
	 * address, the method returns an empty string.
	 *
	 * When the domain name value of the given IP is an IP address in some
	 * session(s), and an actual alphanumeric name in other session(s), this
	 * method returns the second-level domain and the top-level domain extracted
	 * from the actual alphanumeric name.
	 *
	 * Output examples: "google.com", "outlook.com"
	 */
	private String getParentDomainName(InetAddress ipAddress, List<Session> sessions) {
		String parentDomainName = "";
		String domainName;
		String topLevel = "";
		String secondLevel = "";
		for (Session session : sessions) {
			if (session.getRemoteIP().equals(ipAddress)) {
				domainName = session.getDomainName();
				if (domainName == null || domainName.isEmpty()) {
					continue;
				}
				// Ignore IPv6 Address
				if (domainName.contains(":")) {
					continue;
				}
				String[] labels = domainName.split("\\.");
				topLevel = labels[labels.length - 1];
				if (labels.length > 1)
					secondLevel = labels[labels.length - 2];
				/*
				 * If top-level domain contains at least one alphabet, then the
				 * domain name value should not be a literal IP.
				 */
				if (topLevel.matches(".*[A-Za-z].*")) {
					parentDomainName = secondLevel.isEmpty() ? topLevel : secondLevel + "." + topLevel;
					break;
				}
			}
		}
		return parentDomainName;
	}

	/*
	 * Builds a mapping between IP and the sessions associated to the IP.
	 */
	private Map<InetAddress, List<Session>> buildIpSessionsMap(List<Session> sessions) {
		Map<InetAddress, List<Session>> ipSessionsMap = new HashMap<InetAddress, List<Session>>();
		for (Session session : sessions) {
			InetAddress ipAddress = session.getRemoteIP();
			if (ipSessionsMap.containsKey(ipAddress)) {
				ipSessionsMap.get(ipAddress).add(session);
			} else {
				List<Session> sess = new ArrayList<Session>();
				sess.add(session);
				ipSessionsMap.put(ipAddress, sess);
			}
		}
		return ipSessionsMap;
	}
}