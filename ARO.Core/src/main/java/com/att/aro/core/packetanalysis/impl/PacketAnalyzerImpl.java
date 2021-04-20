/*
 *  Copyright 2019 AT&T
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
package com.att.aro.core.packetanalysis.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.configuration.IProfileFactory;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.packetanalysis.IBurstCollectionAnalysis;
import com.att.aro.core.packetanalysis.IEnergyModelFactory;
import com.att.aro.core.packetanalysis.IPacketAnalyzer;
import com.att.aro.core.packetanalysis.IPktAnazlyzerTimeRangeUtil;
import com.att.aro.core.packetanalysis.IRrcStateMachineFactory;
import com.att.aro.core.packetanalysis.ISessionManager;
import com.att.aro.core.packetanalysis.ITraceDataReader;
import com.att.aro.core.packetanalysis.IVideoTrafficCollector;
import com.att.aro.core.packetanalysis.pojo.AbstractRrcStateMachine;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.ApplicationPacketSummary;
import com.att.aro.core.packetanalysis.pojo.BurstCollectionAnalysisData;
import com.att.aro.core.packetanalysis.pojo.EnergyModel;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.IPPacketSummary;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.PacketCounter;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.Statistic;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceFileResult;
import com.att.aro.core.packetreader.pojo.IPPacket;
import com.att.aro.core.packetreader.pojo.TCPPacket;
import com.att.aro.core.packetreader.pojo.UDPPacket;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.videoanalysis.csi.VideoTrafficInferencer;

/**
 * analyze trace file or trace directory and return data that can be used by practice engines.
 * Date: November 7, 2014
 */
public class PacketAnalyzerImpl implements IPacketAnalyzer {

	private static final Logger LOGGER = LogManager.getLogger(PacketAnalyzerImpl.class.getName());

	private final static int DNS_PORT = 53;
	private ITraceDataReader tracereader;
	
	@Autowired
	private ISessionManager sessionmanager;

	private IRrcStateMachineFactory statemachinefactory;

	private IProfileFactory profilefactory;

	private IEnergyModelFactory energymodelfactory;

	private IBurstCollectionAnalysis burstcollectionanalyzer;

	private IPktAnazlyzerTimeRangeUtil pktTimeUtil;

	private IVideoTrafficCollector videoTrafficCollector;

	@Autowired
	private ImageExtractor imageExtractor;

	@Autowired
	private HtmlExtractor htmlExtractor;

	@Autowired
	private VideoTrafficInferencer videoTrafficInferencer;

	@Value("${ga.request.timing.pktAnalysisTimings.title}")
	private String pktAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;
	@Autowired
	private IMetaDataHelper metaDataHelper;

	private SortedMap<Double, HttpRequestResponseInfo> requestMap = new TreeMap<>();

	@Autowired
	public void setTraceReader(ITraceDataReader traceReader) {
		this.tracereader = traceReader;
	}

	@Autowired
	public void setRrcStateMachineFactory(IRrcStateMachineFactory rrcStateMachineFactory) {
		this.statemachinefactory = rrcStateMachineFactory;
	}

	@Autowired
	public void setProfileFactory(IProfileFactory profileFactory) {
		this.profilefactory = profileFactory;
	}

	@Autowired
	public void setEnergyModelFactory(IEnergyModelFactory energyModelFactory) {
		this.energymodelfactory = energyModelFactory;
	}

	@Autowired
	public void setBurstCollectionAnalayzer(IBurstCollectionAnalysis burstCollectionAnalysis) {
		this.burstcollectionanalyzer = burstCollectionAnalysis;
	}

	@Autowired
	public void setPktTimeRangeUtil(IPktAnazlyzerTimeRangeUtil pktUtil) {
		this.pktTimeUtil = pktUtil;
	}

	@Autowired
	public void setVideoStreamingAnalysis(IVideoTrafficCollector videoTrafficCollector) {
		this.videoTrafficCollector = videoTrafficCollector;
	}

	@Override
	public PacketAnalyzerResult analyzeTraceFile(String traceFilePath, Profile profile, AnalysisFilter filter) throws IOException {
		TraceFileResult result = tracereader.readTraceFile(traceFilePath);

		return finalResult(result, profile, filter);
	}

	@Override
	public PacketAnalyzerResult analyzeTraceDirectory(String traceDirectory, Profile profile, AnalysisFilter filter) throws FileNotFoundException {
		long bpStartTime = System.currentTimeMillis();
		TraceDirectoryResult result = tracereader.readTraceDirectory(traceDirectory);
		if (filter != null) {
			TimeRange tempTimeRange = filter.getTimeRange();
			if (tempTimeRange != null) {
				TraceDirectoryResult tempResult = (TraceDirectoryResult) pktTimeUtil.getTimeRangeResult(result, tempTimeRange);
				result.setAlarmInfos(tempResult.getAlarmInfos());
				result.setBatteryInfos(tempResult.getBatteryInfos());
				result.setBluetoothInfos(tempResult.getBluetoothInfos());
				result.setCameraInfos(tempResult.getCameraInfos());
				result.setGpsInfos(tempResult.getGpsInfos());
				result.setNetworkTypeInfos(tempResult.getNetworkTypeInfos());
				result.setRadioInfos(tempResult.getRadioInfos());
				result.setScreenStateInfos(tempResult.getScreenStateInfos());
				result.setUserEvents(tempResult.getUserEvents());
				result.setTemperatureInfos(tempResult.getTemperatureInfos());
				result.setLocationEventInfos(tempResult.getLocationEventInfos());
				result.setWifiInfos(tempResult.getWifiInfos());
				result.getCpuActivityList().updateTimeRange(tempTimeRange.getBeginTime(), tempTimeRange.getEndTime());
				result.setDeviceKeywordInfos(tempResult.getDeviceKeywordInfos());
				result.setAttenautionEvent(tempResult.getAttenautionEvent());
			}
		}
		result.setMetaData(metaDataHelper.initMetaData(result));
		PacketAnalyzerResult res = finalResult(result, profile, filter);
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsTimings(pktAnalysisTitle, System.currentTimeMillis() - bpStartTime, analysisCategory);
		LOGGER.debug(String.format("Time to process PacketAnalyzerImpl %s :%12.4f", pktAnalysisTitle, ((float) (System.currentTimeMillis() - bpStartTime)) / (60 * 60)));
		return res;
	}

	protected PacketAnalyzerResult finalResult(AbstractTraceResult result, Profile profile, AnalysisFilter filter) {
		PacketAnalyzerResult data = new PacketAnalyzerResult();
		List<PacketInfo> filteredPackets; // List of packets included in analysis (application filtered)
		Profile aProfile = profile;
		if (aProfile == null) {
			aProfile = profilefactory.createLTEdefault();// if the user doesn't load any profile.....
			aProfile.setName("AT&T LTE");
		}

		// for the situation, filter out all no-ip packets and caused the allpackets is empty, need to refactor
		if (result != null && result.getAllpackets() != null && result.getAllpackets().size() == 0) {
			data.setTraceresult(result);
			return data;
		}

		TimeRange timeRange = null;

		boolean isCSI = false;
		filteredPackets = new ArrayList<PacketInfo>();
		if (filter == null) {
			if (result != null) {
				filteredPackets = result.getAllpackets();
			}
		} else {// do the filter
			if (filter.isCSI() && filter.getManifestFilePath() != null) {
				isCSI = true;
			}
			timeRange = filter.getTimeRange();
			if (result != null) {
				filteredPackets = filterPackets(filter, result.getAllpackets());
			}
		}

		// Set the Abstract Trace Data with the filtered packets - All packets are not necessary.
		// Fix for Sev 2 Issue correcting the throughput graph - DE187846
		// Fix for Sev 2 Time Range Analysis Issue - DE187848
		if (result != null) {
			result.setAllpackets(filteredPackets);
			
			SessionManagerImpl sessionMangerImpl = (SessionManagerImpl) sessionmanager;
			sessionMangerImpl.setPcapTimeOffset(result.getPcapTimeOffset());
			sessionmanager.setiOSSecureTracePath(result.getTraceDirectory());// for iOS trace

			// Check if secure trace path exists
			if (result instanceof TraceDirectoryResult) {
			    File file = new File(((SessionManagerImpl)sessionmanager).getTracePath());
			    if (file.exists()) {
			        ((TraceDirectoryResult) result).setSecureTrace(true);
			    }
			}
		}

		List<Session> sessionList = sessionmanager.processPacketsAndAssembleSessions(filteredPackets);
		generateRequestMap(sessionList);
		Statistic stat = this.getStatistic(sessionList);
		if (result != null && stat.getAppName() != null && stat.getAppName().size() == 1 && stat.getAppName().contains("Unknown")) {
			stat.setAppName(new HashSet<String>(result.getAppInfos()));
		}

		int totalBytes = 0;
		int totPayloadBytes = 0;
		if (!CollectionUtils.isEmpty(filteredPackets)) {
			for (PacketInfo pkt : filteredPackets) {
			    totalBytes += pkt.getLen();
				totPayloadBytes += pkt.getPacket().getPayloadLen();
			}
		}
		stat.setTotalByte(totalBytes);
		stat.setTotalPayloadBytes(totPayloadBytes);

		// get Unanalyzed HTTPS bytes
		boolean isSecureTrace = result instanceof TraceDirectoryResult ? ((TraceDirectoryResult) result).isSecureTrace() : false;
		if (isSecureTrace) {
		    stat.setTotalHTTPSBytesNotAnalyzed(getHttpsBytesNotAnalyzed(sessionList));
		} else {
		    stat.setTotalHTTPSBytesNotAnalyzed(stat.getTotalHTTPSByte());
		}

		// stat is used to get some info for RrcStateMachine etc
		if (result != null) {
			LOGGER.debug("Starting pre processing in PAI");
			AbstractRrcStateMachine statemachine = statemachinefactory.create(filteredPackets, aProfile, stat.getPacketDuration(), result.getTraceDuration(), stat.getTotalByte(),
					timeRange);

			EnergyModel energymodel = energymodelfactory.create(aProfile, statemachine.getTotalRRCEnergy(), result.getGpsInfos(), result.getCameraInfos(),
					result.getBluetoothInfos(), result.getScreenStateInfos());

			BurstCollectionAnalysisData burstcollectiondata = burstcollectionanalyzer.analyze(filteredPackets, aProfile, stat.getPacketSizeToCountMap(),
					statemachine.getStaterangelist(), result.getUserEvents(), result.getCpuActivityList().getCpuActivities(), sessionList);
			data.clearBPResults();
			try {
				List<BestPracticeType> videoBPList = BestPracticeType.getByCategory(BestPracticeType.Category.VIDEO);
				data.setStreamingVideoData(videoTrafficCollector.clearData());
				if (CollectionUtils.containsAny(SettingsUtil.retrieveBestPractices(), videoBPList)) {
					if (isCSI) {
						data.setStreamingVideoData(videoTrafficInferencer.inferVideoData(result, sessionList, filter.getManifestFilePath()));
					} else {				
					data.setStreamingVideoData(videoTrafficCollector.collect(result, sessionList, requestMap));
					}
				}
			} catch (Exception ex) {
				LOGGER.error("Error in Video usage analysis :", ex);
				// Guarantee that StreamingVideoData is empty
				data.setStreamingVideoData(videoTrafficCollector.clearData());
			}

			try {
				List<BestPracticeType> imageBPList = new ArrayList<>();
				imageBPList.add(BestPracticeType.IMAGE_MDATA);
				imageBPList.add(BestPracticeType.IMAGE_CMPRS);
				imageBPList.add(BestPracticeType.IMAGE_FORMAT);
				imageBPList.add(BestPracticeType.IMAGE_COMPARE);
				if (CollectionUtils.containsAny(SettingsUtil.retrieveBestPractices(), imageBPList)) {
					imageExtractor.execute(result, sessionList, requestMap);
				}
			} catch (Exception ex) {
				LOGGER.error("Error in Image extraction:" + ex.getMessage(), ex);
			}

			htmlExtractor.execute(result, sessionList, requestMap);
			data.setBurstCollectionAnalysisData(burstcollectiondata);
			data.setEnergyModel(energymodel);
			data.setSessionlist(sessionList);
			data.setStatemachine(statemachine);
			data.setStatistic(stat);
			data.setTraceresult(result);
			data.setProfile(aProfile);
			data.setFilter(filter);
			data.setDeviceKeywords(result.getDeviceKeywordInfos());
		}
		return data;
	}

	/**
	 * Calculates total Https data not analyzed where responses are determined to be Unknown
	 * @param sessions
	 * @return
	 */
	private int getHttpsBytesNotAnalyzed(List<Session> sessions) {
	    return
	        sessions.stream()
            .filter(s -> !s.isUdpOnly() && s.getRemotePort() == 443)
            .mapToInt(
                session -> {
                    return
                        session.getRequestResponseInfo().stream()
                        .filter(
                            rrInfo -> (
                                rrInfo.getDirection() != null &&
                                HttpDirection.RESPONSE.equals(rrInfo.getDirection()) &&
                                rrInfo.getStatusCode() == 0
                            )
                        )
                        .mapToInt(HttpRequestResponseInfo::getContentLength)
                        .sum();
                  }
            )
            .sum();
	}

	/**
	 * Runs the filtering process on the specified packets/PacketInfos.
	 * 
	 * @return packets/PacketInfos filtered
	 */
	public List<PacketInfo> filterPackets(AnalysisFilter filter, List<PacketInfo> packetsInfo) {

		List<PacketInfo> filteredPackets = new ArrayList<PacketInfo>(); // create new packets according to the filter setting
		TimeRange timeRange = filter.getTimeRange();
		int packetIdx = 0;

		// Ff you select the check box, you want to include it.
		// All of of the skip-flags are false at first.
		boolean ipv4Skip = !filter.isIpv4Sel();
		boolean ipv6Skip = !filter.isIpv6Sel();
		boolean udpSkip = !filter.isUdpSel();
		boolean dnsSkip = !filter.isDnsSelection();

		for (PacketInfo packetInfo : packetsInfo) {

			if (ipv4Skip && packetInfo.getRemoteIPAddress() instanceof Inet4Address) {
				continue;
			}
			if (ipv6Skip && packetInfo.getRemoteIPAddress() instanceof Inet6Address) {
				continue;
			}
			if (udpSkip && packetInfo.getPacket() instanceof UDPPacket) {
				UDPPacket udpPacket = (UDPPacket) packetInfo.getPacket();
				if (!(DNS_PORT == udpPacket.getDestinationPort() || DNS_PORT == udpPacket.getSourcePort())) {
					continue;
				}
			}
			if (dnsSkip && packetInfo.getPacket() instanceof UDPPacket) {
				UDPPacket udpPacket = (UDPPacket) packetInfo.getPacket();
				if (DNS_PORT == udpPacket.getDestinationPort() || DNS_PORT == udpPacket.getSourcePort()) {
					continue;
				}
			}

			// Check time range
			double timestamp = packetInfo.getTimeStamp();
			if (timeRange != null && (timeRange.getBeginTime() > timestamp || timeRange.getEndTime() < timestamp)) {
				// Not in time range
				continue;
			}
			// Check to see if application is selected
			if (filter.getPacketColor(packetInfo) == null) {
				// App unknown by filter
				continue;
			}

			packetInfo.setPacketId(++packetIdx);
			filteredPackets.add(packetInfo);
		}

		return filteredPackets;
	}

	@Override
	public Statistic getStatistic(List<Session> sessions) {
		Statistic stat = new Statistic();
		Set<String> appNames = new HashSet<String>();

		if (!sessions.isEmpty()) {
			int totalHTTPSBytes = 0;
			int totalTCPBytes = 0;
		    int totalTCPPayloadBytes = 0;
			int totalPackets = 0;
			double avgKbps = 0;
			double packetsDuration = 0;
			double minTimestamp = 0.0d;
			double maxTimestamp = 0.0d;
			List<IPPacketSummary> ipPacketSummary = new ArrayList<>();
			List<ApplicationPacketSummary> applicationPacketSummary = new ArrayList<>();
			Map<Integer, Integer> packetSizeToCountMap = new HashMap<>();
			Map<String, PacketCounter> appPackets = new HashMap<String, PacketCounter>();
			Map<InetAddress, PacketCounter> ipPackets = new HashMap<InetAddress, PacketCounter>();

			for (Session session : sessions) {
			    List<PacketInfo> packets = session.getPackets();
			    if (packets != null) {
			        totalPackets += packets.size();

			        for (PacketInfo packetInfo : packets) {
			            minTimestamp = Math.min(minTimestamp, packetInfo.getTimeStamp());
			            maxTimestamp = Math.max(maxTimestamp, packetInfo.getTimeStamp());

			            PacketCounter pCounter;
    	                if (packetInfo.getPacket() instanceof TCPPacket) {
    	                    TCPPacket tcp = (TCPPacket) packetInfo.getPacket();

            				totalTCPBytes += tcp.getPacketLength();
                            totalTCPPayloadBytes += tcp.getPayloadLen();
        					if (session.isSsl() || tcp.getDestinationPort() == 443 || tcp.getSourcePort() == 443) {
        						totalHTTPSBytes += tcp.getPayloadLen();
        					}
        					

            				String appName = packetInfo.getAppName();
            				appNames.add(appName);
            				pCounter = appPackets.get(appName);
            				if (pCounter == null) {
            					pCounter = new PacketCounter();
            					appPackets.put(appName, pCounter);
            				}
            				pCounter.add(packetInfo);
    	                }

    	                if (packetInfo.getPacket() instanceof IPPacket) {
            					// Count packets by packet size
                            Integer packetSize = packetInfo.getPacket().getPayloadLen();

            					Integer iValue = packetSizeToCountMap.get(packetSize);
            					if (iValue == null) {
            						iValue = 1;
            					} else {
            						iValue++;
            					}
            					packetSizeToCountMap.put(packetSize, iValue);

            					// Get IP address summary
                                InetAddress ipAddress = packetInfo.getRemoteIPAddress();
            					pCounter = ipPackets.get(ipAddress);
            					if (pCounter == null) {
            						pCounter = new PacketCounter();
            						ipPackets.put(ipAddress, pCounter);
            					}
                                pCounter.add(packetInfo);
    	                }
			        }
                }
			}

			for (Map.Entry<InetAddress, PacketCounter> ipPacketMap : ipPackets.entrySet()) {
				ipPacketSummary.add(new IPPacketSummary(ipPacketMap.getKey(), ipPacketMap.getValue().getPacketCount(), ipPacketMap.getValue().getTotalBytes()));
			}

			for (Map.Entry<String, PacketCounter> appPacketMap : appPackets.entrySet()) {
				applicationPacketSummary
						.add(new ApplicationPacketSummary(appPacketMap.getKey(), appPacketMap.getValue().getPacketCount(), appPacketMap.getValue().getTotalBytes()));
			}

	        packetsDuration = maxTimestamp - minTimestamp;
			avgKbps = packetsDuration != 0 ? totalTCPBytes * 8.0 / 1000.0 / packetsDuration : 0.0;

			stat.setApplicationPacketSummary(applicationPacketSummary);
			stat.setAppName(appNames);
			stat.setAverageKbps(avgKbps);
			stat.setAverageTCPKbps(avgKbps);
			stat.setIpPacketSummary(ipPacketSummary);
			stat.setPacketDuration(packetsDuration);
			stat.setTcpPacketDuration(packetsDuration);
			stat.setTotalTCPBytes(totalTCPBytes);
			stat.setTotalTCPPayloadBytes(totalTCPPayloadBytes);
			stat.setTotalHTTPSByte(totalHTTPSBytes);
			stat.setTotalPackets(totalPackets);
			stat.setTotalTCPPackets(totalPackets);
			stat.setPacketSizeToCountMap(packetSizeToCountMap);
		}

		stat.setAppName(appNames);
		return stat;
	}

	/*
	 * Create a TreeMap of all pertinent Requests keyed by timestamp plus tie-breaker
	 *
	 * @param sessionlist
	 * 
	 * @return Map of Requests
	 */
	public void generateRequestMap(List<Session> sessionList) {
		int counter = 0;
		requestMap.clear();
		for (Session session : sessionList) {
			List<HttpRequestResponseInfo> rri = session.getRequestResponseInfo();
			for (HttpRequestResponseInfo rrInfo : rri) {
				if (HttpDirection.REQUEST.equals(rrInfo.getDirection()) && HttpRequestResponseInfo.HTTP_GET.equals(rrInfo.getRequestType())
						&& rrInfo.getObjNameWithoutParams().contains(".")) {
					rrInfo.setSession(session);
					double key = getReqInfoKey(rrInfo, 0);
					if (requestMap.containsKey(key)) {
						do {
							key = getReqInfoKey(rrInfo, ++counter);
						} while (requestMap.containsKey(key));
						counter = 0;
					}
					requestMap.put(key, rrInfo);
				}
			}
			// Set a forward link for all packets in session to the next packet (within the session).
			List<PacketInfo> packets = session.getPackets();
			for (int i = 0; i < packets.size() - 1; i++) {
				packets.get(i).getPacket().setNextPacketInSession(packets.get(i + 1).getPacket());
			}
		}
	}

	/*
	 * build key for storing requests in order with tie-breaker support
	 */
	private Double getReqInfoKey(HttpRequestResponseInfo rrInfo, int counter) {
		return rrInfo.getTimeStamp() * 1000000 + counter;
	}

	public Map<Double, HttpRequestResponseInfo> getReqMap() {
		return requestMap;
	}

}// end class
