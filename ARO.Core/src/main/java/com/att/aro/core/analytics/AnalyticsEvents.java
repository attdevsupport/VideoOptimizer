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
package com.att.aro.core.analytics;

import org.springframework.beans.factory.annotation.Value;

/**
 * 
 *
 */
public class AnalyticsEvents {

	@Value("${ga.trackid}")
	private String trackerID;
	
	@Value("${ga.request.event.category.analyzer}")
	private String analyzerEvent;
	
	@Value("${ga.request.event.analyzer.action.startapp}")
	private String startApp;
	
	@Value("${ga.request.event.analyzer.action.endapp}")
	private String endApp;
	
	@Value("${ga.request.event.analyzer.action.load}")
	private String loadTrace;
	
	@Value("${ga.request.event.analyzer.action.load.pcap}")
	private String loadPcap;
	
	@Value("${ga.request.event.analyzer.action.almexport}")
	private String almExport;
	
	@Value("${ga.request.event.analyzer.action.exportlabel}")
	private String exportInitiated;
	
	@Value("${ga.request.event.category.collector}")
	private String collector;
	
	@Value("${ga.request.event.collector.action.starttrace}")
	private String startTrace;
	
	@Value("${ga.request.event.collector.action.endtrace}")
	private String endTrace;
	
	@Value("${ga.request.event.collector.action.startapp}")
	private String startCollectorApp;
	
	@Value("${ga.request.event.collector.action.endapp}")
	private String endCollectorApp;
	
	@Value("${ga.request.event.installation.event}")
	private String installer;
	
	@Value("${ga.request.event.installation.java.event}")
	private String language;
	
	@Value("${ga.request.event.collector.ios}")
	private String iosCollector;
	
	@Value("${ga.request.event.collector.rooted}")
	private String rootedCollector;
	
	@Value("${ga.request.event.collector.nonrooted}")
	private String nonRootedCollector;
	
	@Value("${ga.request.event.collector.action.video}")
	private String videoCheck;

	@Value("${ga.request.event.collector.emulator}")
	private String emulator;

	@Value("${ga.request.event.category.secureCollector}")
	private String secureCollectorEvent;
	
	@Value("${ga.request.event.secureCollector.action.applied}")
	private String secureCollectorApplied;
	
	@Value("${ga.request.event.category.attenuation}")
	private String attenuationEvent;
	
	@Value("${ga.request.event.attenuation.action.applied}")
	private String attenuationApplied;
	
	@Value("${ga.request.event.attenuation.action.notapplied}")
	private String attenuationNotApplied;
	
	@Value("${ga.request.event.category.traceduration}")
	private String traceDurationEvent;
	
	@Value("${ga.request.event.category.bpresult}")
	private String bPResultEvent;
	
	@Value("${ga.request.event.category.videobpresult}")
	private String videoBPResultEvent;
	
	@Value("${ga.request.event.category.diagnosticsViews}")
	private String diagnosticsViewsEvent;
	
	@Value("${ga.request.event.category.dataMimeType}")
	private String dataMimeTypeEvent;
	
	@Value("${ga.request.event.analyzer.tracesAnalyzed}")
	private String tracesAnalyzedEvent;
	
	@Value("${ga.request.event.category.voSession}")
	private String voSessionEvent;

	@Value("${ga.request.event.voSession.action.voSessionDuration}")
	private String voSessionDuration;
	
	@Value("${ga.request.event.category.https}")
	private String httpsEvent;
	/**
	 * @return the trackerID
	 */
	public String getTrackerID() {
		return trackerID;
	}

	/**
	 * @return the analyzerEvent
	 */
	public String getAnalyzerEvent() {
		return analyzerEvent;
	}

	/**
	 * @return the startApp
	 */
	public String getStartApp() {
		return startApp;
	}

	/**
	 * @return the endApp
	 */
	public String getEndApp() {
		return endApp;
	}

	/**
	 * @return the loadTrace
	 */
	public String getLoadTrace() {
		return loadTrace;
	}

	/**
	 * @return the loadPcap
	 */
	public String getLoadPcap() {
		return loadPcap;
	}

	/**
	 * @return the almExport
	 */
	public String getAlmExport() {
		return almExport;
	}

	/**
	 * @return the exportInitiated
	 */
	public String getExportInitiated() {
		return exportInitiated;
	}

	/**
	 * @return the collector
	 */
	public String getCollector() {
		return collector;
	}

	/**
	 * @return the startTrace
	 */
	public String getStartTrace() {
		return startTrace;
	}

	/**
	 * @return the endTrace
	 */
	public String getEndTrace() {
		return endTrace;
	}

	/**
	 * @return the startCollectorApp
	 */
	public String getStartCollectorApp() {
		return startCollectorApp;
	}

	/**
	 * @return the endCollectorApp
	 */
	public String getEndCollectorApp() {
		return endCollectorApp;
	}

	/**
	 * @return the installer
	 */
	public String getInstaller() {
		return installer;
	}

	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return the iosCollector
	 */
	public String getIosCollector() {
		return iosCollector;
	}

	/**
	 * @return the rootedCollector
	 */
	public String getRootedCollector() {
		return rootedCollector;
	}

	/**
	 * @return the nonRootedCollector
	 */
	public String getNonRootedCollector() {
		return nonRootedCollector;
	}

	/**
	 * @return the videoCheck
	 */
	public String getVideoCheck() {
		return videoCheck;
	}

	/**
	 * @return the emulator
	 */
	public String getEmulator() {
		return emulator;
	}

	public String getSecureCollectorEvent() {
		return secureCollectorEvent;
	}

	public String getSecureCollectorApplied() {
		return secureCollectorApplied;
	}

	public String getAttenuationEvent() {
		return attenuationEvent;
	}

	public String getAttenuationApplied() {
		return attenuationApplied;
	}

	public String getAttenuationNotApplied() {
		return attenuationNotApplied;
	}

	public String getTraceDurationEvent() {
		return traceDurationEvent;
	}
	
	public String getBPResultEvent(){
		return bPResultEvent;
	}

	public String getVideoBPResultEvent() {
		return videoBPResultEvent;
	}

	public String getDiagnosticsViewsEvent() {
		return diagnosticsViewsEvent;
	}
	
	public String getDataMimeTypeEvent(){
		return dataMimeTypeEvent;
	}
	
	public String getTracesAnalyzedEvent(){
		return tracesAnalyzedEvent;
	}
	
	public String getVOSessionEvent(){
		return voSessionEvent;
	}
	
	public String getVOSessionDuration(){
		return voSessionDuration;
	}
	
	public String getHTTPSEvent(){
		return httpsEvent;
	}
}
