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

import com.att.aro.core.ILogger;
import com.att.aro.core.impl.LoggerImpl;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.SessionValues;

public final class SimultnsUtil {
	
	private static final ILogger LOGGER = new LoggerImpl(SimultnsUtil.class.getName());

	public Map<String, ArrayList<Session>> getDistinctMap(List<Session> sessions) {
		Map<String, ArrayList<Session>> distinctMap = new HashMap<String, ArrayList<Session>>();
		for (Session session : sessions) {
			if (session != null && !session.isUDP()) {
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

	public SimultnsConnEntry getTimeMap(List<SessionValues> tMap, int maxCount, boolean isManyServer) {
		String ipInside = "";
		TreeMap<Double, Double> timeMap = new TreeMap<Double, Double>();
		TreeMap<String, HttpRequestResponseInfo> reqRespMap = new TreeMap<String, HttpRequestResponseInfo>();

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
			ipInside = indSessionVal.getIp();
			if(indSessionVal.getReqRespInfo()!=null){
				reqRespMap.put(ipInside,indSessionVal.getReqRespInfo());
			} else {
				reqRespMap.put(ipInside,new HttpRequestResponseInfo());
			}
			
			iterator++;
		}
		return maxOverlapIntervalCount(reqRespMap, ipInside, start, end, maxCount, timeMap,isManyServer);
	}

	private SimultnsConnEntry maxOverlapIntervalCount(TreeMap<String, HttpRequestResponseInfo> reqRespMap, String ipEntry,
			double[] start, double[] end, int maxCount, TreeMap<Double, Double> timeMap, boolean isManyEndpoints) {

		int startTimePointer = start.length, endTimePointer = end.length;
		
		if (isManyEndpoints) {
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
		
		SimultnsConnEntry simultnsConnEntry = null;
		TreeMap<String, SimultnsConnEntry> simultnsConnectionEntryMap = new TreeMap<String, SimultnsConnEntry>();

		double startTime = 0.0;
		int currentOverlap = 0;
		int maxOverlap = 0;
		int startTimeCounter = 0;
		int endTimeCounter = 0;
		
		
		while (startTimeCounter < startTimePointer && endTimeCounter < endTimePointer) {
			startTime = start[startTimeCounter];
			if (startTime < end[endTimeCounter]) {
				currentOverlap++;

				if (currentOverlap >= maxCount && maxOverlap < currentOverlap) {
					if (simultnsConnectionEntryMap.containsKey(ipEntry) && maxCount == 3) {
						simultnsConnectionEntryMap.replace(ipEntry,
								new SimultnsConnEntry(reqRespMap.get(ipEntry),
										ipEntry.substring(ipEntry.lastIndexOf('/') + 1, ipEntry.length()),
										currentOverlap, startTime, timeMap.get(startTime)));
					} else {
						simultnsConnectionEntryMap.put(ipEntry,
								new SimultnsConnEntry(reqRespMap.get(ipEntry),
										ipEntry.substring(ipEntry.lastIndexOf('/') + 1, ipEntry.length()),
										currentOverlap, startTime, timeMap.get(startTime)));
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
		if (simultnsConnectionEntryMap.containsKey(ipEntry)) {
			simultnsConnEntry = simultnsConnectionEntryMap.get(ipEntry);
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
						reqResp.setFirstDataPacket(aSession.getPackets().get(0));
					}
					sessionValues = addSessionValues(sessionValues, lastSession, aSession, reqResp);
				}
				
			}
		}
		return sessionValues;
		
	}

	private List<SessionValues> addSessionValues(List<SessionValues> sessionValues, Session lastSession, Session aSession,
			HttpRequestResponseInfo req) {
		if (!aSession.equals(lastSession)) {
			
			sessionValues.add(new SessionValues(aSession, req));
		}
		return sessionValues;
	}
}
