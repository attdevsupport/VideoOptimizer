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

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.SessionValues;

public final class SimultnsUtil {

	public Map<String, ArrayList<Session>> getDistinctMap(List<Session> sessions) {
		Map<String, ArrayList<Session>> distinctMap = new HashMap<String, ArrayList<Session>>();
		for (Session session : sessions) {
			if (session != null && !session.isUDP()) {
				String domainName = session.getRemoteIP().toString();
				ArrayList<Session> tempList = distinctMap.get(domainName);
				if (tempList == null) {
					tempList = new ArrayList<Session>();
					distinctMap.put(domainName, tempList);
				}
				tempList.add(session);
				
			}
		}
		return distinctMap;
	}

	public SimultnsConnEntry getTimeMap(List<SessionValues> tMap, int maxCount) {
		String ipInside = "";
		TreeMap<Double, Double> timeMap = new TreeMap<Double, Double>();

		HttpRequestResponseInfo reqRespInfo = null;
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
			reqRespInfo = indSessionVal.getReqRespInfo();
			iterator++;
		}
		return maxOverlapIntervalCount(reqRespInfo, ipInside, start, end, maxCount, timeMap);
	}

	private SimultnsConnEntry maxOverlapIntervalCount(HttpRequestResponseInfo reqRespInfo, String ipEntry,
			double[] start, double[] end, int maxCount, TreeMap<Double, Double> timeMap) {

		Arrays.sort(start);
		Arrays.sort(end);

		SimultnsConnEntry simultnsConnEntry = null;
		TreeMap<String, SimultnsConnEntry> simultnsConnectionEntryMap = new TreeMap<String, SimultnsConnEntry>();

		double startTime = 0.0;
		int currentOverlap = 0;
		int maxOverlap = 0;
		int startTimeCounter = 0;
		int endTimeCounter = 0;
		int startTimePointer = start.length, endTimePointer = end.length;
		while (startTimeCounter < startTimePointer && endTimeCounter < endTimePointer) {
			startTime = start[startTimeCounter];
			if (startTime < end[endTimeCounter]) {
				currentOverlap++;

				if ((currentOverlap >= maxCount && maxOverlap < currentOverlap)
						|| (maxCount == 12 && currentOverlap == maxCount)) {
					if (simultnsConnectionEntryMap.containsKey(ipEntry) && maxCount == 3) {
						simultnsConnectionEntryMap.replace(ipEntry,
								new SimultnsConnEntry(reqRespInfo,
										ipEntry.substring(ipEntry.lastIndexOf('/') + 1, ipEntry.length()),
										currentOverlap, startTime, timeMap.get(startTime)));
					} else {
						simultnsConnectionEntryMap.put(ipEntry, new SimultnsConnEntry(reqRespInfo, ipEntry,
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
				for (HttpRequestResponseInfo req : aSession.getRequestResponseInfo()) {
					if (!aSession.equals(lastSession)) {
						sessionValues.add(new SessionValues(aSession, req));
						lastSession = aSession;
					}
				}
			}
		}
		return sessionValues;
	}
}
