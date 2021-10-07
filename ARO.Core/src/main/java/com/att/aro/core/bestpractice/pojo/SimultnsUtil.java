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
package com.att.aro.core.bestpractice.pojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.SessionValues;
import com.att.aro.core.util.Util;

public final class SimultnsUtil {
	
	private static final Logger LOGGER = LogManager.getLogger(SimultnsUtil.class.getName());
	private static final int MAX_CONNECTION_THRESHOLD = 7;
	
	TreeMap<Double, String> ipMap = new TreeMap<Double, String>();

	public Map<String, ArrayList<Session>> getDistinctMap(List<Session> sessions) {
		Map<String, ArrayList<Session>> distinctMap = new HashMap<String, ArrayList<Session>>();
		for (Session session : sessions) {
			if (session != null && !session.isUdpOnly()) {
				String domainName = session.getRemoteIP().toString();
				ArrayList<Session> tempList = distinctMap.get(domainName);
				if (tempList == null) {
					tempList = new ArrayList<Session>();
				}
				tempList.add(session);
				distinctMap.put(domainName, tempList);
			}
		}
		return distinctMap;
	}

	public MultipleConnectionsEntry getTimeMap(List<SessionValues> tMap, int maxCount, boolean isManyServer) {
		String ipInside = "";
		String domainVal = "";
		TreeMap<Double, Double> timeMap = new TreeMap<Double, Double>();
		TreeMap<Double, HttpRequestResponseInfo> reqRespTimeMap = new TreeMap<Double, HttpRequestResponseInfo>();
		int iterator = 0;
		double sessionStartTime = 0.0;
		double sessionEndTime = 0.0;
		double[] start = new double[tMap.size()];
		double[] end = new double[tMap.size()];
		for (SessionValues indSessionVal : tMap) {
			sessionStartTime = indSessionVal.getStartTime();
			sessionEndTime = indSessionVal.getEndTime();
			timeMap.put(sessionStartTime, sessionEndTime);
			start[iterator] = sessionStartTime;
			end[iterator] = sessionEndTime;
			ipInside = indSessionVal.getIp().substring(indSessionVal.getIp().lastIndexOf('/') + 1,
					indSessionVal.getIp().length());
			ipMap.put(sessionStartTime, ipInside);
			if (indSessionVal.getReqRespInfo() != null) {
				HttpRequestResponseInfo reqResponseSessionData = indSessionVal.getReqRespInfo();
				reqResponseSessionData.setSession(indSessionVal.getIndSession());
				if (reqResponseSessionData.getHostName() != null) {
					domainVal = reqResponseSessionData.getHostName();
				} else {
					domainVal = ipInside;
				}
				reqRespTimeMap.put(sessionStartTime, reqResponseSessionData);
			} else {
				reqRespTimeMap.put(sessionStartTime, new HttpRequestResponseInfo());
			}
			iterator++;
		}
		return maxOverlapIntervalCount(domainVal, start, end, maxCount, timeMap, isManyServer, reqRespTimeMap,
				ipInside);
	}

	private MultipleConnectionsEntry maxOverlapIntervalCount(String domainVal, double[] start, double[] end,
			int maxCount,
			TreeMap<Double, Double> timeMap, boolean isManyEndpoints,
			TreeMap<Double, HttpRequestResponseInfo> reqRespTimeMap, String ipInside) {
		if (isManyEndpoints && Util.isTestMode()) {
			LOGGER.debug("Start : ");
			for (int startCounter = 0; startCounter < start.length; startCounter++) {
				LOGGER.debug(String.valueOf(start[startCounter]));
			}
			LOGGER.debug("End : ");
			for (int endCounter = 0; endCounter < start.length; endCounter++) {
				LOGGER.debug(String.valueOf(end[endCounter]));
			}
		}
		Arrays.sort(start);
		Arrays.sort(end);
		MultipleConnectionsEntry simultnsConnEntry = null;
		TreeMap<String, MultipleConnectionsEntry> simultnsConnectionEntryMap = new TreeMap<String, MultipleConnectionsEntry>();
		double startTime = 0.0;
		int currentOverlap = 0;
		int maxOverlap = 0;
		int startTimeCounter = 0;
		int endTimeCounter = 0;
		while (startTimeCounter < start.length && endTimeCounter < end.length) {
			startTime = start[startTimeCounter];
			if (startTime < end[endTimeCounter]) {
				currentOverlap++;
				if (currentOverlap >= maxCount && maxOverlap < currentOverlap) {
					if (!isManyEndpoints) {
						if (simultnsConnectionEntryMap.containsKey(domainVal) && maxCount == MAX_CONNECTION_THRESHOLD) {
							simultnsConnectionEntryMap.replace(domainVal,
									new MultipleConnectionsEntry(reqRespTimeMap.get(startTime),
											domainVal.substring(domainVal.lastIndexOf('/') + 1, domainVal.length()),
											currentOverlap, startTime, timeMap.get(startTime), ipInside, isManyEndpoints));
						} else {
							simultnsConnectionEntryMap.put(domainVal,
									new MultipleConnectionsEntry(reqRespTimeMap.get(startTime),
											domainVal.substring(domainVal.lastIndexOf('/') + 1, domainVal.length()),
											currentOverlap, startTime, timeMap.get(startTime), ipInside, isManyEndpoints));
						}
					} else {
						String domain = "";
						if (reqRespTimeMap.get(startTime) != null
								&& reqRespTimeMap.get(startTime).getHostName() != null) {
							domain = reqRespTimeMap.get(startTime).getHostName();
						} else {
							domain = ipMap.get(startTime);
						}
						simultnsConnectionEntryMap.put(domainVal,
								new MultipleConnectionsEntry(reqRespTimeMap.get(startTime),
										domain.substring(domain.lastIndexOf('/') + 1, domain.length()),
										currentOverlap, startTime, timeMap.get(startTime), ipMap.get(startTime), isManyEndpoints));
					}
					if (maxOverlap < currentOverlap) {
						maxOverlap = currentOverlap;
					}
				}
				startTimeCounter++;
			} else {
				currentOverlap--;
				endTimeCounter++;
			}
		}
		if (simultnsConnectionEntryMap.containsKey(domainVal)) {
			simultnsConnEntry = simultnsConnectionEntryMap.get(domainVal);
		}
		return simultnsConnEntry;
	}

	public List<SessionValues> createDomainsTCPSessions(Collection<Session> allTCPSessions) {
		List<SessionValues> sessionValues = new ArrayList<SessionValues>();
		Session lastSession = null;
		for (Session aSession : allTCPSessions) {
			if (aSession != null) {
				if (!aSession.getRequestResponseInfo().isEmpty()) {
					for (HttpRequestResponseInfo req : aSession.getRequestResponseInfo()) {
						sessionValues = addSessionValues(sessionValues, lastSession, aSession, req);
						lastSession = aSession;
					}
				} else {
					HttpRequestResponseInfo reqResp = new HttpRequestResponseInfo();
					if (aSession.getRemoteHostName() != null) {
						reqResp.setHostName(aSession.getRemoteHostName());
						reqResp.setFirstDataPacket(aSession.getTcpPackets().get(0));
					}
					sessionValues = addSessionValues(sessionValues, lastSession, aSession, reqResp);
				}
			}
		}
		return sessionValues;
	}

	private List<SessionValues> addSessionValues(List<SessionValues> sessionValues, Session lastSession,
			Session aSession, HttpRequestResponseInfo req) {
		if (!aSession.equals(lastSession)) {
			sessionValues.add(new SessionValues(aSession, req));
		}
		return sessionValues;
	}
}
