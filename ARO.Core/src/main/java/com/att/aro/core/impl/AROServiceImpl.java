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
package com.att.aro.core.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.att.aro.core.IAROService;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.ICacheAnalysis;
import com.att.aro.core.packetanalysis.IPacketAnalyzer;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.pojo.ErrorCodeRegistry;
import com.att.aro.core.pojo.VersionInfo;
import com.att.aro.core.report.IReport;
import com.att.aro.core.settings.SettingsUtil;
import com.att.aro.core.util.Util;

/**
 * This class provides access to ARO.Core functionality for analyzing and
 * generating reports.
 *
 * <pre>
 * To analyze a trace use analyzeDirectory or analyzeFile. These will generate an AROTraceData object.
 * Html and Json formatted reports can be generated once the analysis has been performed.
 * </pre>
 *
 * <pre>
 * Example:
 *   IAROService serv = context.getBean(IAROService.class);
 *   List&lt;BestPracticeType&gt; listOfBestPractices = new new ArrayList&lt;BestPracticeType&gt;();
 *   listOfBestPractices.add(BestPracticeType.UNNECESSARY_CONNECTIONS);
 *   listOfBestPractices.add(BestPracticeType.SCREEN_ROTATION);
 *   AROTraceData data = serv.analyzeFile(listOfBestPractices, "/yourTracePath/AROTraceAndroid/trace1");
 *
 *   // generate json report
 *   serv.getJSonReport("/yourPath/output.json", data);
 *
 *   // generate html report
 *   serv.getHtmlReport("/yourPath/output.html", data);
 * </pre>
 *
 * Date: March 27, 2014
 *
 */
public class AROServiceImpl implements IAROService {
	@InjectLogger
	private ILogger logger;
	private IPacketAnalyzer packetanalyzer;
	private ICacheAnalysis cacheAnalyzer;
	@Autowired
	private transient VersionInfo info;
	@Autowired
	private IFileManager filemanager;
	private IReport jsonreport;

	@Autowired
	@Qualifier("jsongenerate")
	public void setJsonreport(IReport jsonreport) {
		this.jsonreport = jsonreport;
	}

	private IReport htmlreport;

	@Autowired
	@Qualifier("htmlgenerate")
	public void setHtmlreport(IReport htmlreport) {
		this.htmlreport = htmlreport;
	}

	private IBestPractice periodicTransfer;
	private IBestPractice unnecessaryConnection;
	private IBestPractice connectionOpening;
	private IBestPractice connectionClosing;
	private IBestPractice screenRotation;
	private IBestPractice accessingPeripheral;
	private IBestPractice combineCsJss;
	private IBestPractice http10Usage;
	private IBestPractice cacheControl;
	private IBestPractice usingCache;
	private IBestPractice duplicateContent;
	private IBestPractice http4xx5xx;
	private IBestPractice http3xx;
	private IBestPractice textFileCompression;
	private IBestPractice imageSize;
	private IBestPractice imageMetadata;
	private IBestPractice imageCompression;
	private IBestPractice imageFormat;
	private IBestPractice uiComparator;
	private IBestPractice minify;
	private IBestPractice emptyUrl;
	private IBestPractice flash;
	private IBestPractice spriteImage;
	private IBestPractice scripts;
	private IBestPractice async;
	private IBestPractice displaynoneincss;
	private IBestPractice fileorder;
	private IBestPractice httpsUsage;
	private IBestPractice transmissionPrivateData;
	private IBestPractice unsecureSSLVersion;
	private IBestPractice weakCipher;
	private IBestPractice forwardSecrecy;
	private IBestPractice simultaneous;
	private IBestPractice multipleSimultaneous;
	//private IBestPractice adAnalytics;
	// ARO 6.0 VideoBp
	private IBestPractice videoStall;
	private IBestPractice startupDelay;
	private IBestPractice bufferOccupancy;
	private IBestPractice networkComparison;
	private IBestPractice tcpConnection;
	private IBestPractice chunkSize;
	private IBestPractice chunkPacing;
	private IBestPractice videoRedundancy;
	private IBestPractice videoConcurrentSession;

	@Autowired
	public void setPacketAnalyzer(IPacketAnalyzer packetanalyzer) {
		this.packetanalyzer = packetanalyzer;
	}

	@Autowired
	public void setCacheAnalysis(ICacheAnalysis cacheanalysis) {
		this.cacheAnalyzer = cacheanalysis;
	}

	@Autowired
	@Qualifier("periodicTransfer")
	public void setPeriodicTransfer(IBestPractice periodicTransfer) {
		this.periodicTransfer = periodicTransfer;
	}

	@Autowired
	@Qualifier("unnecessaryConnection")
	public void setUnnecessaryConnection(IBestPractice unnecessaryConnection) {
		this.unnecessaryConnection = unnecessaryConnection;
	}

	@Autowired
	@Qualifier("connectionOpening")
	public void setConnectionOpening(IBestPractice connectionOpening) {
		this.connectionOpening = connectionOpening;
	}

	@Autowired
	@Qualifier("connectionClosing")
	public void setConnectionClosing(IBestPractice connectionClosing) {
		this.connectionClosing = connectionClosing;
	}

	@Autowired
	@Qualifier("screenRotation")
	public void setScreenRotation(IBestPractice screenRotation) {
		this.screenRotation = screenRotation;
	}

	@Autowired
	@Qualifier("accessingPeripheral")
	public void setAccessingPeripheral(IBestPractice accessingPeripheral) {
		this.accessingPeripheral = accessingPeripheral;
	}

	@Autowired
	@Qualifier("combineCsJss")
	public void setCombineCsJss(IBestPractice combineCsJss) {
		this.combineCsJss = combineCsJss;
	}

	@Autowired
	@Qualifier("http10Usage")
	public void setHttp10Usage(IBestPractice http10Usage) {
		this.http10Usage = http10Usage;
	}

	@Autowired
	@Qualifier("cacheControl")
	public void setCacheControl(IBestPractice cacheControl) {
		this.cacheControl = cacheControl;
	}

	@Autowired
	@Qualifier("usingCache")
	public void setUsingCache(IBestPractice usingCache) {
		this.usingCache = usingCache;
	}

	@Autowired
	@Qualifier("duplicateContent")
	public void setDuplicateContent(IBestPractice duplicateContent) {
		this.duplicateContent = duplicateContent;
	}

	@Autowired
	@Qualifier("http4xx5xx")
	public void setHttp4xx5xx(IBestPractice http4xx5xx) {
		this.http4xx5xx = http4xx5xx;
	}

	@Autowired
	@Qualifier("simultaneous")
	public void setSimultaneous(IBestPractice simultaneous) {
		this.simultaneous = simultaneous;
	}

	@Autowired
	@Qualifier("multipleSimultaneous")
	public void setMultipleSimultaneous(IBestPractice multipleSimultaneous) {
		this.multipleSimultaneous = multipleSimultaneous;
	}

	/*@Autowired
	@Qualifier("adAnalytics")
	public void setAdAnalytics(IBestPractice adAnalytics) {
		this.adAnalytics = adAnalytics;
	}*/

	@Autowired
	@Qualifier("http3xx")
	public void setHttp3xx(IBestPractice http3xx) {
		this.http3xx = http3xx;
	}

	@Autowired
	@Qualifier("textFileCompression")
	public void setTextFileCompression(IBestPractice textFileCompression) {
		this.textFileCompression = textFileCompression;
	}

	@Autowired
	@Qualifier("imageSize")
	public void setImageSize(IBestPractice imageSize) {
		this.imageSize = imageSize;
	}

	@Autowired
	@Qualifier("imageMetadata")
	public void setImageMdata(IBestPractice imageMetadata) {
		this.imageMetadata = imageMetadata;
	}

	@Autowired
	@Qualifier("imageCompression")
	public void setImageCompression(IBestPractice imageCompression) {
		this.imageCompression = imageCompression;
	}

	@Autowired
	@Qualifier("imageFormat")
	public void setImageFormat(IBestPractice imageFormat) {
		this.imageFormat = imageFormat;
	}

	@Autowired
	@Qualifier("uiComparator")
	public void setUIComparator(IBestPractice uiComparator) {
		this.uiComparator = uiComparator;
	}

	@Autowired
	@Qualifier("minify")
	public void setMinify(IBestPractice minify) {
		this.minify = minify;
	}

	@Autowired
	@Qualifier("emptyUrl")
	public void setEmptyUrl(IBestPractice emptyUrl) {
		this.emptyUrl = emptyUrl;
	}

	@Autowired
	@Qualifier("flash")
	public void setFlash(IBestPractice flash) {
		this.flash = flash;
	}

	@Autowired
	@Qualifier("spriteImage")
	public void setSpriteImage(IBestPractice spriteImage) {
		this.spriteImage = spriteImage;
	}

	@Autowired
	@Qualifier("scripts")
	public void setScripts(IBestPractice scripts) {
		this.scripts = scripts;
	}

	@Autowired
	@Qualifier("async")
	public void setAsync(IBestPractice async) {
		this.async = async;
	}

	@Autowired
	@Qualifier("displaynoneincss")
	public void setDisplayNoneInCSS(IBestPractice displaynoneincss) {
		this.displaynoneincss = displaynoneincss;
	}

	@Autowired
	@Qualifier("fileorder")
	public void setFileOrder(IBestPractice fileorder) {
		this.fileorder = fileorder;
	}

	// ARO 6.0 VideoBp
	@Autowired
	@Qualifier("videoStall")
	public void setVideoStallImpl(IBestPractice videoStall) {
		this.videoStall = videoStall;
	}

	@Autowired
	@Qualifier("networkComparison")
	public void setVideoNetworkComparisonImpl(IBestPractice networkcomparison) {
		this.networkComparison = networkcomparison;
	}

	@Autowired
	@Qualifier("startupDelay")
	public void setVideoStartupDelayImpl(IBestPractice startupdelay) {
		this.startupDelay = startupdelay;
	}

	@Autowired
	@Qualifier("bufferOccupancy")
	public void setVideoBufferOccupancyImpl(IBestPractice bufferoccupancy) {
		this.bufferOccupancy = bufferoccupancy;
	}

	@Autowired
	@Qualifier("tcpConnection")
	public void setTcpconnectionImpl(IBestPractice tcpconnection) {
		this.tcpConnection = tcpconnection;
	}

	@Autowired
	@Qualifier("chunkPacing")
	public void setChunkpacingImpl(IBestPractice chunkpacing) {
		this.chunkPacing = chunkpacing;
	}

	@Autowired
	@Qualifier("chunkSize")
	public void setChunksizeImpl(IBestPractice chunksize) {
		this.chunkSize = chunksize;
	}

	@Autowired
	@Qualifier("videoRedundancy")
	public void setVideoredundancyImpl(IBestPractice videoredundancy) {
		this.videoRedundancy = videoredundancy;
	}

	@Autowired
	@Qualifier("videoConcurrentSession")
	public void setVideoConcurrentSessionImpl(IBestPractice videoConcurrentSession) {
		this.videoConcurrentSession = videoConcurrentSession;
	}

	@Autowired
	@Qualifier("httpsUsage")
	public void setHttpsUsage(IBestPractice httpsUsage) {
		this.httpsUsage = httpsUsage;
	}

	@Autowired
	@Qualifier("transmissionPrivateData")
	public void setTransmissionPrivateData(IBestPractice transmissionPrivateData) {
		this.transmissionPrivateData = transmissionPrivateData;
	}

	@Autowired
	@Qualifier("unsecureSSLVersion")
	public void setUnsecureSSLVersion(IBestPractice unsecureSSLVersion) {
		this.unsecureSSLVersion = unsecureSSLVersion;
	}

	@Autowired
	@Qualifier("weakCipher")
	public void setWeakCipher(IBestPractice weakCipher) {
		this.weakCipher = weakCipher;
	}

	@Autowired
	@Qualifier("forwardSecrecy")
	public void setForwardSecrecy(IBestPractice forwardSecrecy) {
		this.forwardSecrecy = forwardSecrecy;
	}

	/**
	 * Returns the name of the Application
	 *
	 * @return name of Application
	 */
	@Override
	public String getName() {
		return info.getName();
	}

	/**
	 * Returns the version of the Application
	 *
	 * @return Application version
	 */
	@Override
	public String getVersion() {
		return info.getVersion();
	}

	/**
	 * Returns the Packet Analyzer used
	 *
	 * @return the Packet Analyzer
	 */
	@Override
	public IPacketAnalyzer getAnalyzer() {
		return packetanalyzer;
	}

	/**
	 * Launches an analysis of a traceFile with the results populating an
	 * AROTraceData object
	 *
	 * @param requests
	 *            list of BestPracticeType bestPractices to analyze
	 * @param traceFile
	 *            path to a pcap trace file, usually traffic.cap
	 * @return AROTraceData object
	 * @throws IOException
	 *             if trace file not found
	 */
	@Override
	public AROTraceData analyzeFile(List<BestPracticeType> requests, String traceFile) throws IOException {
		return analyzeFile(requests, traceFile, null, null);
	}

	/**
	 * Launches an analysis of a traceFile with the results populating an
	 * AROTraceData object
	 *
	 * @param requests
	 *            list of BestPracticeType bestPractices to analyze
	 * @param traceFile
	 *            path to a pcap trace file, usually traffic.cap
	 * @param profile
	 *            device profile used as a model of the device when analyzing
	 *            trace data
	 * @param filter
	 *            used for filtering information from a trace analysis based on
	 *            a specified time range and set of ApplicationSelection
	 *            objects.
	 * @return AROTraceData object
	 * @throws IOException
	 *             if trace file not found
	 */
	@Override
	public AROTraceData analyzeFile(List<BestPracticeType> requests, String traceFile, Profile profile,
			AnalysisFilter filter) throws IOException {
		AROTraceData data = new AROTraceData();
		PacketAnalyzerResult result = packetanalyzer.analyzeTraceFile(traceFile, profile, filter);
		if (result == null) {
			data.setError(ErrorCodeRegistry.getTraceFileNotAnalyzed());
		} else {
			if (result.getTraceresult().getAllpackets().size() == 0) {
				// we set on purpose
				data.setError(ErrorCodeRegistry.getUnRecognizedPackets());
				data.setSuccess(false);
			} else {
				List<AbstractBestPracticeResult> bestPractices = analyze(result, requests);
				bestPractices.addAll(createEmptyResults());
				data.setAnalyzerResult(result);
				data.setBestPracticeResults(bestPractices);
				data.setSuccess(true);
			}
		}
		return data;
	}

	/**
	 * Launches an analysis of a trace directory with the results populating an
	 * AROTraceData object.
	 * <p>
	 * Other trace files depend on capture method, platform and version of
	 * device.
	 * </p>
	 *
	 * @param requests
	 *            list of BestPracticeType bestPractices to analyze
	 * @param traceDirectory
	 *            path to a trace directory, usually contains traffic.cap
	 * @return AROTraceData object
	 * @throws IOException
	 *             if trace file not found
	 */
	@Override
	public AROTraceData analyzeDirectory(List<BestPracticeType> requests, String traceDirectory) throws IOException {
		return analyzeDirectory(requests, traceDirectory, null, null);
	}

	/**
	 * Launches an analysis of a trace directory with the results populating an
	 * AROTraceData object.
	 * <p>
	 * Other trace files depend on capture method, platform and version of
	 * device.
	 * </p>
	 *
	 * @param requests
	 *            list of BestPracticeType bestPractices to analyze
	 * @param traceDirectory
	 *            path to a trace directory, usually contains traffic.cap
	 * @param profile
	 *            device profile used as a model of the device when analyzing
	 *            trace data
	 * @param filter
	 *            used for filtering information from a trace analysis based on
	 *            a specified time range and set of ApplicationSelection
	 *            objects.
	 * @return AROTraceData object
	 * @throws IOException
	 *             if trace file not found
	 */
	@Override
	public AROTraceData analyzeDirectory(List<BestPracticeType> requests, String traceDirectory, Profile profile,
			AnalysisFilter filter) throws IOException {
		AROTraceData data = new AROTraceData();
		PacketAnalyzerResult result = null;
		try {
			result = packetanalyzer.analyzeTraceDirectory(traceDirectory, profile, filter);
		} catch (FileNotFoundException ex) {
			data.setError(ErrorCodeRegistry.getTraceDirNotFound());
			return data;
		}
		if (result == null) {
			data.setError(ErrorCodeRegistry.getTraceDirectoryNotAnalyzed());
			data.setSuccess(false);
		} else {
			if (result.getTraceresult() == null) {
				// we set this on purpose
				data.setSuccess(false);
				data.setError(ErrorCodeRegistry.getUnRecognizedPackets());
			} else if (result.getTraceresult().getAllpackets() == null
					|| result.getTraceresult().getAllpackets().size() == 0) {
				data.setSuccess(false);
				if (filemanager.fileExist(traceDirectory + Util.FILE_SEPARATOR + "traffic.cap")) {
					data.setError(ErrorCodeRegistry.getPacketsNotFound());
				} else {
					data.setError(ErrorCodeRegistry.getTrafficFileNotFound());
				}
			} else {
				List<AbstractBestPracticeResult> bestPractices = analyze(result, requests);
				bestPractices.addAll(createEmptyResults());
				data.setAnalyzerResult(result);
				data.setBestPracticeResults(bestPractices);
				data.setSuccess(true);
			}
		}
		return data;
	}

	private List<AbstractBestPracticeResult> createEmptyResults() {
		List<BestPracticeType> allBP = Arrays.asList(BestPracticeType.values());
		List<BestPracticeType> selected = SettingsUtil.retrieveBestPractices();
		Function<? super BestPracticeType, ? extends AbstractBestPracticeResult> resMapper = (bp) -> {
			AbstractBestPracticeResult res = new AbstractBestPracticeResult() {
				@Override
				public BestPracticeType getBestPracticeType() {
					return bp;
				}
			};
			res.setResultType(BPResultType.NONE);
			res.setDetailTitle(res.getBestPracticeType().getDescription());
			res.setOverviewTitle(res.getBestPracticeType().getDescription());
			return res;
		};
		List<AbstractBestPracticeResult> results = allBP.stream()
				.filter((bp) -> bp.getCategory() != Category.PRE_PROCESS && !selected.contains(bp)).map(resMapper)
				.collect(Collectors.toList());
		return results;
	}

	/**
	 * Performs BestPractice tests identified in the requests
	 * List&lt;BestPracticeType&gt; requests.<br>
	 * Test results are added to a resultList, ArrayList&lt;IBestPractice&gt;
	 *
	 *
	 * @param result
	 *            a PacketAnalyzerResult object
	 * @param requests
	 *            a List of BestPracticeType
	 * @return ArrayList&lt;IBestPractice&gt; or null if result was null
	 */
	@Override
	public List<AbstractBestPracticeResult> analyze(PacketAnalyzerResult result, List<BestPracticeType> requests) {
		if (result == null) {
			return null;
		}
		List<AbstractBestPracticeResult> resultlist = new ArrayList<AbstractBestPracticeResult>();
		List<IBestPractice> workers = new ArrayList<IBestPractice>();
		for (BestPracticeType type : requests) {
			switch (type) {
			case PERIODIC_TRANSFER:
				workers.add(periodicTransfer);
				break;
			case UNNECESSARY_CONNECTIONS:
				workers.add(unnecessaryConnection);
				break;
			case ACCESSING_PERIPHERALS:
				workers.add(accessingPeripheral);
				break;
			case ASYNC_CHECK:
				workers.add(async);
				break;
			case CACHE_CONTROL:
				// analyze cache if not yet done
				this.createCacheAnalysis(result);
				workers.add(cacheControl);
				break;
			case COMBINE_CS_JSS:
				workers.add(combineCsJss);
				break;
			case CONNECTION_CLOSING:
				workers.add(connectionClosing);
				break;
			case CONNECTION_OPENING:
				workers.add(connectionOpening);
				break;
			case DISPLAY_NONE_IN_CSS:
				workers.add(displaynoneincss);
				break;
			case DUPLICATE_CONTENT:
				this.createCacheAnalysis(result);
				workers.add(duplicateContent);
				break;
			case EMPTY_URL:
				workers.add(emptyUrl);
				break;
			case FILE_COMPRESSION:
				workers.add(textFileCompression);
				break;
			case FILE_ORDER:
				workers.add(fileorder);
				break;
			case FLASH:
				workers.add(flash);
				break;
			case HTTP_1_0_USAGE:
				workers.add(http10Usage);
				break;
			case HTTP_3XX_CODE:
				workers.add(http3xx);
				break;
			case HTTP_4XX_5XX:
				workers.add(http4xx5xx);
				break;
			case IMAGE_SIZE:
				workers.add(imageSize);
				break;
			case IMAGE_MDATA:
				workers.add(imageMetadata);
				break;
			case IMAGE_CMPRS:
				workers.add(imageCompression);
				break;
			case IMAGE_FORMAT:
				workers.add(imageFormat);
				break;
			case IMAGE_COMPARE:
				workers.add(uiComparator);
				break;
			case MINIFICATION:
				workers.add(minify);
				break;
			case SCREEN_ROTATION:
				workers.add(screenRotation);
				break;
			case SCRIPTS_URL:
				workers.add(scripts);
				break;
			case SIMUL_CONN:
				workers.add(simultaneous);
				break;
			case MULTI_SIMULCONN:
				workers.add(multipleSimultaneous);
				break;
			/*case AD_ANALYTICS:
				workers.add(adAnalytics);
				break;*/
			case SPRITEIMAGE:
				workers.add(spriteImage);
				break;
			case USING_CACHE:
				// analyze cache if not yet done
				this.createCacheAnalysis(result);
				workers.add(usingCache);
				break;
			// ARO 6.0 release
			case VIDEO_STALL:
				workers.add(videoStall);
				break;
			case STARTUP_DELAY:
				workers.add(startupDelay);
				break;
			case BUFFER_OCCUPANCY:
				workers.add(bufferOccupancy);
				break;
			case NETWORK_COMPARISON:
				workers.add(networkComparison);
				break;
			case TCP_CONNECTION:
				workers.add(tcpConnection);
				break;
			case CHUNK_SIZE:
				workers.add(chunkSize);
				break;
			case CHUNK_PACING:
				workers.add(chunkPacing);
				break;
			case VIDEO_REDUNDANCY:
				workers.add(videoRedundancy);
				break;
			case VIDEO_CONCURRENT_SESSION:
				workers.add(videoConcurrentSession);
				break;
			case HTTPS_USAGE:
				workers.add(httpsUsage);
				break;
			case TRANSMISSION_PRIVATE_DATA:
				workers.add(transmissionPrivateData);
				break;
			case UNSECURE_SSL_VERSION:
				workers.add(unsecureSSLVersion);
				break;
			case WEAK_CIPHER:
				workers.add(weakCipher);
				break;
			case FORWARD_SECRECY:
				workers.add(forwardSecrecy);
				break;
			default:
				break;
			}
		}
		for (IBestPractice worker : workers) {
			try {
				AbstractBestPracticeResult testresult = worker.runTest(result);
				resultlist.add(testresult);
			} catch (Exception | Error ex) {
				logger.error("Error running best practice:", ex);
			}
		}
		return resultlist;
	}

	/**
	 * Launch an ICacheAnalysis.analyze(...) storing the result in
	 * PacketAnalyzerResult via setCacheAnalysis
	 *
	 * @param result
	 */
	private void createCacheAnalysis(PacketAnalyzerResult result) {
		if (result.getCacheAnalysis() == null) {
			result.setCacheAnalysis(cacheAnalyzer.analyze(result.getSessionlist()));
		}
	}

	/**
	 * Indicates if this file represents a file on the underlying file system.
	 *
	 * @param path
	 *            of file to examine
	 * @return true is file is a file, false if not
	 */
	@Override
	public boolean isFile(String path) {
		return filemanager.isFile(path);
	}

	/**
	 * Indicates if file exists
	 *
	 * @param path
	 *            of file, to include file name
	 * @return true if file exists, false otherwise
	 */
	@Override
	public boolean isFileExist(String path) {
		return filemanager.fileExist(path);
	}

	/**
	 * Indicates if folder/directory exists
	 *
	 * @param path
	 *            of folder/directory
	 * @return true if exists, false if not
	 */
	@Override
	public boolean isFolderExist(String path) {
		return filemanager.directoryExist(path);
	}

	/**
	 * Generate Packet Analysis Report in HTML format
	 *
	 * @param resultFilePath
	 *            the path for the output report
	 * @param results
	 *            the AROTraceData path
	 * @return true if report generated, false if AROTraceData is null or failed
	 *         to create/write output file
	 */
	@Override
	public boolean getHtmlReport(String resultFilePath, AROTraceData results) {
		return htmlreport.reportGenerator(resultFilePath, results);
	}

	/**
	 * Generate Packet Analysis Report in JSON format
	 *
	 * @param resultFilePath
	 *            the path for the output report
	 * @param results
	 *            the AROTraceData path
	 * @return true if report generated, false if AROTraceData is null or failed
	 *         to create/write output file
	 */
	@Override
	public boolean getJSonReport(String resultFilePath, AROTraceData results) {
		return jsonreport.reportGenerator(resultFilePath, results);
	}

	/**
	 * Determine if path is to a file or directory. Returns the parent directory
	 * if a file
	 *
	 * @param path
	 *            to examine
	 * @return path or parent directory if a file or null if path does not exist
	 */
	@Override
	public String getDirectory(String path) {
		return filemanager.getDirectory(path);
	}

	/**
	 * Determine if parent directory of file exists
	 *
	 * @param path
	 *            of file
	 * @return true if parent directory exists, false otherwise
	 */
	@Override
	public boolean isFileDirExist(String path) {
		return filemanager.fileDirExist(path);
	}
}
