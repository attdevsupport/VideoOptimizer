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
package com.att.aro.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.att.aro.core.adb.IAdbService;
import com.att.aro.core.adb.impl.AdbServiceImpl;
import com.att.aro.core.analytics.AnalyticsEvents;
import com.att.aro.core.android.IAndroid;
import com.att.aro.core.android.impl.AndroidImpl;
import com.att.aro.core.commandline.IExternalProcessReader;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.ILLDBProcessRunner;
import com.att.aro.core.commandline.IProcessFactory;
import com.att.aro.core.commandline.impl.ExternalProcessReaderImpl;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.commandline.impl.LLDBProcessRunnerImpl;
import com.att.aro.core.commandline.impl.ProcessFactoryImpl;
import com.att.aro.core.concurrent.IThreadExecutor;
import com.att.aro.core.concurrent.impl.ThreadExecutorImpl;
import com.att.aro.core.configuration.IProfileFactory;
import com.att.aro.core.configuration.impl.ProfileFactoryImpl;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;
import com.att.aro.core.impl.AROServiceImpl;
import com.att.aro.core.mobiledevice.IAndroidDevice;
import com.att.aro.core.mobiledevice.impl.AndroidDeviceImpl;
import com.att.aro.core.mobiledevice.pojo.AroDevices;
import com.att.aro.core.mobiledevice.pojo.IAroDevices;
import com.att.aro.core.packetanalysis.IBurstCollectionAnalysis;
import com.att.aro.core.packetanalysis.IByteArrayLineReader;
import com.att.aro.core.packetanalysis.ICacheAnalysis;
import com.att.aro.core.packetanalysis.IEnergyModelFactory;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.IPacketAnalyzer;
import com.att.aro.core.packetanalysis.IParseHeaderLine;
import com.att.aro.core.packetanalysis.IPktAnazlyzerTimeRangeUtil;
import com.att.aro.core.packetanalysis.IRrcStateMachineFactory;
import com.att.aro.core.packetanalysis.IRrcStateRangeFactory;
import com.att.aro.core.packetanalysis.ISessionManager;
import com.att.aro.core.packetanalysis.IThroughputCalculator;
import com.att.aro.core.packetanalysis.ITraceDataReader;
import com.att.aro.core.packetanalysis.IVideoTrafficCollector;
import com.att.aro.core.packetanalysis.impl.BurstCollectionAnalysisImpl;
import com.att.aro.core.packetanalysis.impl.ByteArrayLineReaderImpl;
import com.att.aro.core.packetanalysis.impl.CacheAnalysisImpl;
import com.att.aro.core.packetanalysis.impl.EnergyModelFactoryImpl;
import com.att.aro.core.packetanalysis.impl.HtmlExtractor;
import com.att.aro.core.packetanalysis.impl.HttpRequestResponseHelperImpl;
import com.att.aro.core.packetanalysis.impl.ImageExtractor;
import com.att.aro.core.packetanalysis.impl.PacketAnalyzerImpl;
import com.att.aro.core.packetanalysis.impl.ParseHeaderLineImpl;
import com.att.aro.core.packetanalysis.impl.PktAnazlyzerTimeRangeImpl;
import com.att.aro.core.packetanalysis.impl.RrcStateMachineFactoryImpl;
import com.att.aro.core.packetanalysis.impl.RrcStateRangeFactoryImpl;
import com.att.aro.core.packetanalysis.impl.SessionManagerImpl;
import com.att.aro.core.packetanalysis.impl.ThroughputCalculatorImpl;
import com.att.aro.core.packetanalysis.impl.TraceDataReaderImpl;
import com.att.aro.core.packetanalysis.impl.VideoStreamConstructor;
import com.att.aro.core.packetanalysis.impl.VideoTrafficCollectorImpl;
import com.att.aro.core.packetreader.IDomainNameParser;
import com.att.aro.core.packetreader.IPacketReader;
import com.att.aro.core.packetreader.IPacketService;
import com.att.aro.core.packetreader.IPcapngHelper;
import com.att.aro.core.packetreader.impl.DomainNameParserImpl;
import com.att.aro.core.packetreader.impl.NetmonPacketReaderImpl;
import com.att.aro.core.packetreader.impl.PacketReaderLibraryImpl;
import com.att.aro.core.packetreader.impl.PacketServiceImpl;
import com.att.aro.core.packetreader.impl.PcapngHelperImpl;
import com.att.aro.core.peripheral.IAlarmAnalysisInfoParser;
import com.att.aro.core.peripheral.IAlarmDumpsysTimestampReader;
import com.att.aro.core.peripheral.IAlarmInfoReader;
import com.att.aro.core.peripheral.IAppInfoReader;
import com.att.aro.core.peripheral.IAttenuattionEventReader;
import com.att.aro.core.peripheral.IBatteryInfoReader;
import com.att.aro.core.peripheral.IBluetoothInfoReader;
import com.att.aro.core.peripheral.ICameraInfoReader;
import com.att.aro.core.peripheral.ICellTowerInfoReader;
import com.att.aro.core.peripheral.ICollectOptionsReader;
import com.att.aro.core.peripheral.ICpuActivityParser;
import com.att.aro.core.peripheral.ICpuActivityReader;
import com.att.aro.core.peripheral.ICpuTemperatureReader;
import com.att.aro.core.peripheral.IDeviceDetailReader;
import com.att.aro.core.peripheral.IDeviceInfoReader;
import com.att.aro.core.peripheral.IGpsInfoReader;
import com.att.aro.core.peripheral.IMetaDataReadWrite;
import com.att.aro.core.peripheral.INetworkTypeReader;
import com.att.aro.core.peripheral.IPrivateDataReader;
import com.att.aro.core.peripheral.IRadioInfoReader;
import com.att.aro.core.peripheral.IScreenRotationReader;
import com.att.aro.core.peripheral.IScreenStateInfoReader;
import com.att.aro.core.peripheral.ISpeedThrottleEventReader;
import com.att.aro.core.peripheral.ITimeRangeReadWrite;
import com.att.aro.core.peripheral.IUserEventReader;
import com.att.aro.core.peripheral.IVideoStartupReadWrite;
import com.att.aro.core.peripheral.IVideoTimeReader;
import com.att.aro.core.peripheral.IWakelockInfoReader;
import com.att.aro.core.peripheral.IWifiInfoReader;
import com.att.aro.core.peripheral.LocationReader;
import com.att.aro.core.peripheral.impl.AlarmAnalysisInfoParserImpl;
import com.att.aro.core.peripheral.impl.AlarmDumpsysTimestampReaderImpl;
import com.att.aro.core.peripheral.impl.AlarmInfoReaderImpl;
import com.att.aro.core.peripheral.impl.AppInfoReaderImpl;
import com.att.aro.core.peripheral.impl.AttenuationEventReaderImpl;
import com.att.aro.core.peripheral.impl.BatteryInfoReaderImpl;
import com.att.aro.core.peripheral.impl.BluetoothInfoReaderImpl;
import com.att.aro.core.peripheral.impl.CameraInfoReaderImpl;
import com.att.aro.core.peripheral.impl.CellTowerInfoReaderImpl;
import com.att.aro.core.peripheral.impl.CollectOptionsReaderImpl;
import com.att.aro.core.peripheral.impl.CpuActivityParserImpl;
import com.att.aro.core.peripheral.impl.CpuActivityReaderImpl;
import com.att.aro.core.peripheral.impl.CpuTemperatureReaderImpl;
import com.att.aro.core.peripheral.impl.DeviceDetailReaderImpl;
import com.att.aro.core.peripheral.impl.DeviceInfoReaderImpl;
import com.att.aro.core.peripheral.impl.GpsInfoReaderImpl;
import com.att.aro.core.peripheral.impl.LocationReaderImpl;
import com.att.aro.core.peripheral.impl.MetaDataReadWrite;
import com.att.aro.core.peripheral.impl.NetworkTypeReaderImpl;
import com.att.aro.core.peripheral.impl.PrivateDataReaderImpl;
import com.att.aro.core.peripheral.impl.RadioInfoReaderImpl;
import com.att.aro.core.peripheral.impl.ScreenRotationReaderImpl;
import com.att.aro.core.peripheral.impl.ScreenStateInfoReaderImpl;
import com.att.aro.core.peripheral.impl.SpeedThrottleEventReaderImpl;
import com.att.aro.core.peripheral.impl.TimeRangeReadWrite;
import com.att.aro.core.peripheral.impl.UserEventReaderImpl;
import com.att.aro.core.peripheral.impl.VideoStartupReadWriterImpl;
import com.att.aro.core.peripheral.impl.VideoTimeReaderImpl;
import com.att.aro.core.peripheral.impl.WakelockInfoReaderImpl;
import com.att.aro.core.peripheral.impl.WifiInfoReaderImpl;
import com.att.aro.core.pojo.VersionInfo;
import com.att.aro.core.report.IReport;
import com.att.aro.core.report.impl.HtmlReportImpl;
import com.att.aro.core.report.impl.JSonReportImpl;
import com.att.aro.core.resourceextractor.IReadWriteFileExtractor;
import com.att.aro.core.resourceextractorimpl.ReadWriteFileExtractorImpl;
import com.att.aro.core.searching.ISearchingHandler;
import com.att.aro.core.searching.impl.KeywordSearchingHandler;
import com.att.aro.core.searching.impl.PatternSearchingHandler;
import com.att.aro.core.searching.strategy.ISearchingStrategy;
import com.att.aro.core.searching.strategy.impl.TrieSearchingStrategy;
import com.att.aro.core.securedpacketreader.ICipherDataService;
import com.att.aro.core.securedpacketreader.ICrypto;
import com.att.aro.core.securedpacketreader.ISSLKeyService;
import com.att.aro.core.securedpacketreader.ITLSHandshake;
import com.att.aro.core.securedpacketreader.ITLSSessionInfo;
import com.att.aro.core.securedpacketreader.impl.CipherDataServiceImpl;
import com.att.aro.core.securedpacketreader.impl.CryptoImpl;
import com.att.aro.core.securedpacketreader.impl.SSLKeyServiceImpl;
import com.att.aro.core.securedpacketreader.impl.TLSHandshakeImpl;
import com.att.aro.core.securedpacketreader.impl.TLSSessionInfoImpl;
import com.att.aro.core.settings.Settings;
import com.att.aro.core.settings.impl.SettingsImpl;
import com.att.aro.core.tracemetadata.IEnvironmentDetailsHelper;
import com.att.aro.core.tracemetadata.IEnvironmentDetailsReadWrite;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.impl.EnvironmentDetailsHelper;
import com.att.aro.core.tracemetadata.impl.EnvironmentDetailsReadWrite;
import com.att.aro.core.tracemetadata.impl.MetaDataHelper;
import com.att.aro.core.util.BrewConfirmationImpl;
import com.att.aro.core.util.FFmpegConfirmationImpl;
import com.att.aro.core.util.FFprobeConfirmationImpl;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.PcapConfirmationImpl;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.VLCConfirmationImpl;
import com.att.aro.core.util.WiresharkConfirmationImpl;
import com.att.aro.core.video.IScreenRecorder;
import com.att.aro.core.video.IVideoCapture;
import com.att.aro.core.video.IVideoWriter;
import com.att.aro.core.video.impl.ScreenRecorderImpl;
import com.att.aro.core.video.impl.VideoCaptureImpl;
import com.att.aro.core.video.impl.VideoWriterImpl;
import com.att.aro.core.videoanalysis.IVideoAnalysisConfigHelper;
import com.att.aro.core.videoanalysis.IVideoEventDataHelper;
import com.att.aro.core.videoanalysis.IVideoTabHelper;
import com.att.aro.core.videoanalysis.IVideoUsagePrefsManager;
import com.att.aro.core.videoanalysis.PlotHelperAbstract;
import com.att.aro.core.videoanalysis.csi.ICSIDataHelper;
import com.att.aro.core.videoanalysis.csi.impl.CSIDataHelperImpl;
import com.att.aro.core.videoanalysis.impl.BufferInSecondsCalculatorImpl;
import com.att.aro.core.videoanalysis.impl.BufferOccupancyCalculatorImpl;
import com.att.aro.core.videoanalysis.impl.VideoAnalysisConfigHelperImpl;
import com.att.aro.core.videoanalysis.impl.VideoBestPractices;
import com.att.aro.core.videoanalysis.impl.VideoChunkPlotterImpl;
import com.att.aro.core.videoanalysis.impl.VideoEventDataHelperImpl;
import com.att.aro.core.videoanalysis.impl.VideoPrefsController;
import com.att.aro.core.videoanalysis.impl.VideoSegmentAnalyzer;
import com.att.aro.core.videoanalysis.impl.VideoTabHelperImpl;
import com.att.aro.core.videoanalysis.impl.VideoUsagePrefsManagerImpl;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.videoframe.VideoFrameExtractor;

/**
 * Spring configuration for ARO.Core<br>
 * Included are all the components to collect, open, analyze and generate
 * reports.
 *
 */
@SuppressWarnings("deprecation")
@Configuration
@Lazy
@ComponentScan("com.att.aro")
@Import(AROBestPracticeConfig.class)
@PropertySource({ "classpath:bestpractices.properties", "classpath:analytics.properties",
		"classpath:build.properties", "classpath:url.properties" })
@ImportResource({ "classpath*:plugins-analytics.xml", "classpath*:plugins.xml", "classpath*:plugin-manager.xml",
		"classpath*:plugins-noroot.xml" })
public class AROConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public IAROService getAROService() {
		return new AROServiceImpl();
	}
	
	@Bean
	public AnalyticsEvents getAnalyticsEvets() {
		return new AnalyticsEvents();
	}

	@Bean
	public VersionInfo getInfo() {
		return new VersionInfo();
	}

	@Bean(name = "threadexecutor")
	public IThreadExecutor threadExecutor() {
		return new ThreadExecutorImpl();
	}

	@Bean(name = "packetReader")
	public IPacketReader getPacketReader() {
		return new PacketReaderLibraryImpl();
	}

	@Bean(name = "netmonPacketReader")
	public IPacketReader getNetmonPacketReader() {
		return new NetmonPacketReaderImpl();
	}

	@Bean
	public IPacketService getPacketService() {
		return new PacketServiceImpl();
	}

	@Bean
	public IPcapngHelper getPcapngHelper() {
		return new PcapngHelperImpl();
	}

	@Bean
	public IDomainNameParser getDomainNameParser() {
		return new DomainNameParserImpl();
	}

	@Bean(name="fileManager")
	public IFileManager getReadFile() {
		return new FileManagerImpl();
	}

	@Bean
	public ICpuActivityParser getCpuActivityParser() {
		return new CpuActivityParserImpl();
	}

	@Bean
	public ICpuActivityReader getCpuActivityReader() {
		return new CpuActivityReaderImpl();
	}

	@Bean
	public ISessionManager getSessionManager() {
		return new SessionManagerImpl();
	}

	@Bean
	@Scope("prototype")
	public ITLSSessionInfo getTLSSessionInfo() {
		return new TLSSessionInfoImpl();
	}

	@Bean
	public ISSLKeyService getSSLKeyService() {
		return new SSLKeyServiceImpl();
	}

	@Bean
	public ICipherDataService getCipherDataService() {
		return new CipherDataServiceImpl();
	}

	@Bean
	public ITLSHandshake getITLSHandshake() {
		return new TLSHandshakeImpl();
	}

	@Bean
	public ITraceDataReader getTraceDataReader() {
		return new TraceDataReaderImpl();
	}

	@Bean
	public IGpsInfoReader getGpsInfoReader() {
		return new GpsInfoReaderImpl();
	}

	@Bean
	public IBluetoothInfoReader getBluetoothInfoReader() {
		return new BluetoothInfoReaderImpl();
	}

	@Bean
	public IWifiInfoReader getWifiInfoReader() {
		return new WifiInfoReaderImpl();
	}

	@Bean
	public ICameraInfoReader getCameraInfoReader() {
		return new CameraInfoReaderImpl();
	}

	@Bean
	public IAlarmAnalysisInfoParser getAlarmAnalysisInfoParser() {
		return new AlarmAnalysisInfoParserImpl();
	}

	@Bean
	public IRadioInfoReader getRadioInfoReader() {
		return new RadioInfoReaderImpl();
	}

	@Bean
	public IWakelockInfoReader getWakelockInfoReader() {
		return new WakelockInfoReaderImpl();
	}

	@Bean
	public IScreenStateInfoReader getScreenStateInfoReader() {
		return new ScreenStateInfoReaderImpl();
	}

	@Bean
	public IAppInfoReader getAppInfoReader() {
		return new AppInfoReaderImpl();
	}

	@Bean
	public IAlarmDumpsysTimestampReader getAlarmDumpsysTimestampReader() {
		return new AlarmDumpsysTimestampReaderImpl();
	}

	@Bean
	public IUserEventReader getUserEventReader() {
		return new UserEventReaderImpl();
	}

	@Bean
	public ICpuTemperatureReader getTemperatureDataReader() {
		return new CpuTemperatureReaderImpl();
	}

	@Bean
	public LocationReader getLocationDataReader() {
		return new LocationReaderImpl();
	}

	@Bean
	public IPrivateDataReader getPrivateDataReader() {
		return new PrivateDataReaderImpl();
	}

	@Bean(name="screenRotationReaderImpl")
	public IScreenRotationReader getScreenRotationReader() {
		return new ScreenRotationReaderImpl();
	}

	@Bean
	public IAlarmInfoReader getAlarmInfoReader() {
		return new AlarmInfoReaderImpl();
	}

	@Bean
	public IBatteryInfoReader getBatteryInfoReader() {
		return new BatteryInfoReaderImpl();
	}

	@Bean(name="deviceDetailReader")
	public IDeviceDetailReader getDeviceDetailReader() {
		return new DeviceDetailReaderImpl();
	}

	@Bean
	public IAttenuattionEventReader getAttnrEventReader() {
		return new AttenuationEventReaderImpl();
	}

	@Bean
	public ISpeedThrottleEventReader getSpeedThrottleReader(){
		return new SpeedThrottleEventReaderImpl();
	}
	
	@Bean
	public INetworkTypeReader getNetworkTypeReader() {
		return new NetworkTypeReaderImpl();
	}

	@Bean
	public IVideoTimeReader getVideoTimeReader() {
		return new VideoTimeReaderImpl();
	}

	@Bean
	public ICrypto getCrypto() {
		return new CryptoImpl();
	}

	@Bean
	public IDeviceInfoReader getDeviceInfoReader() {
		return new DeviceInfoReaderImpl();
	}

	@Bean(name="collectOptionsReaderImpl")
	public ICollectOptionsReader getCollectOptionReader() {
		return new CollectOptionsReaderImpl();
	}

	@Bean
	public IThroughputCalculator getThroughputCalculator() {
		return new ThroughputCalculatorImpl();
	}

	@Bean
	public IRrcStateRangeFactory getRrcStateRangeFactory() {
		return new RrcStateRangeFactoryImpl();
	}

	@Bean
	public IRrcStateMachineFactory getRrcStateMachineFactory() {
		return new RrcStateMachineFactoryImpl();
	}

	@Bean
	public IProfileFactory getProfileFactory() {
		return new ProfileFactoryImpl();
	}

	@Bean
	public IEnergyModelFactory getEnergyModelFactory() {
		return new EnergyModelFactoryImpl();
	}

	@Bean
	public IBurstCollectionAnalysis getBurstCollectionAnalysis() {
		return new BurstCollectionAnalysisImpl();
	}

	@Bean
	public IPacketAnalyzer getPacketAnalyzer() {
		return new PacketAnalyzerImpl();
	}

	@Bean(name="httpRequestResponseHelper")
	public IHttpRequestResponseHelper getHttpRequestResponseHelper() {
		return new HttpRequestResponseHelperImpl();
	}

	@Bean
	public ICacheAnalysis getCacheAnalysis() {
		return new CacheAnalysisImpl();
	}

	@Bean
	public IParseHeaderLine getParseHeaderLineImpl() {
		return new ParseHeaderLineImpl();
	}

	@Bean
	@Scope(value = "prototype")
	// => always create a new instance
	public IVideoWriter getVideoWriter() {
		return new VideoWriterImpl();
	}

	@Bean
	public IByteArrayLineReader getByteArrayLineReader() {
		return new ByteArrayLineReaderImpl();
	}

	@Bean
	public IExternalProcessReader getExternalProcessReaderImpl() {
		return new ExternalProcessReaderImpl();
	}

	@Bean(name = "lldbProcessRunnerImpl")
	public ILLDBProcessRunner getLLDBProcessRunnerImpl() {
		return new LLDBProcessRunnerImpl();
	}
	
	@Bean(name = "jsongenerate")
	public IReport getJSonGanarate() {
		return new JSonReportImpl();
	}

	@Bean(name = "htmlgenerate")
	public IReport getHtmlGenerate() {
		return new HtmlReportImpl();
	}

	@Bean
	public Settings getAROConfigFile() {
		return SettingsImpl.getInstance();
	}

	@Bean
	public IAdbService getAdbService() {
		return new AdbServiceImpl();
	}

	@Bean
	public IAndroidDevice getAndroidDevice() {
		return new AndroidDeviceImpl();
	}

	@Bean
	public IReadWriteFileExtractor getReadWriteFileExtractorImpl() {
		return new ReadWriteFileExtractorImpl();
	}

	@Bean
	@Scope(value = "prototype")
	// => always create a new instance
	public IVideoCapture getVideoCapture() {
		return new VideoCaptureImpl();
	}

	@Bean
	public IScreenRecorder getScreenRecorder() {
		return new ScreenRecorderImpl();
	}

	@Bean
	IAndroid getAndroid() {
		return new AndroidImpl();
	}

	@Bean
	IAroDevices getAroDevices() {
		return new AroDevices();
	}

	@Bean
	public IProcessFactory getProcessFactory() {
		return new ProcessFactoryImpl();
	}

	@Bean
	public IPktAnazlyzerTimeRangeUtil getPktAnalyzerTimeRange() {
		return new PktAnazlyzerTimeRangeImpl();
	}

	@Bean(name = "keywordSearchingHandler")
	public ISearchingHandler getKeywordSearchingHandler() {
		return new KeywordSearchingHandler();
	}

	@Bean(name = "patternSearchingHandler")
	public ISearchingHandler getPatternSearchingHandler() {
		return new PatternSearchingHandler();
	}

	@Bean(name = "trieSearchingStrategy")
	public ISearchingStrategy getTrieSearchingStrategy() {
		return new TrieSearchingStrategy();
	}
	
	@Bean(name = "videoBestPractices")
	public IVideoBestPractices getVideoBestPractices() {
		return new VideoBestPractices();
	}
	
	@Bean(name = "imageExtractor")
	public ImageExtractor getImageExtractor() {
		return new ImageExtractor();
	}
	
	@Bean(name = "htmlExtractor")
	public HtmlExtractor getHtmlExtractor() {
		return new HtmlExtractor();
	}

	@Bean(name = "videoStreamingAnalysis")
	public IVideoTrafficCollector getVideoStreamingAnalysis() {
		return new VideoTrafficCollectorImpl();
	}

	@Bean(name = "videoStreamConstructor")
	public VideoStreamConstructor getVideoStreamConstructor() {
		return new VideoStreamConstructor();
	}

	@Bean(name = "bufferOccupancyCalculatorImpl")
	public PlotHelperAbstract getBufferOccupancyCalculatorImpl() {
		return new BufferOccupancyCalculatorImpl();
	}

	@Bean(name = "videoChunkPlotterImpl")
	public PlotHelperAbstract getVideoChunkPlotterImpl() {
		return new VideoChunkPlotterImpl();
	}

	@Bean(name = "bufferInSecondsCalculatorImpl")
	public PlotHelperAbstract getBufferInSecondsCalculatorImpl() {
		return new BufferInSecondsCalculatorImpl();
	}

	@Bean(name = "videoUsagePrefsManagerImpl")
	public IVideoUsagePrefsManager getVideoUsagePrefsManagerImpl() {
		return new VideoUsagePrefsManagerImpl();
	}

	@Bean(name = "ffmpegConfirmationImpl")
	public FFmpegConfirmationImpl getFfmpegConfirmationImpl() {
		return new FFmpegConfirmationImpl();
	}
	
	@Bean(name = "ffprobeConfirmationImpl")
	public FFprobeConfirmationImpl getFFprobeConfirmationImpl() {
		return new FFprobeConfirmationImpl();
	}
	
	@Bean(name = "pcapConfirmationImpl")
	public PcapConfirmationImpl getPcapConfirmationImpl() {
		return new PcapConfirmationImpl();
	}
	
	@Bean(name = "wiresharkConfirmationImpl")
	public WiresharkConfirmationImpl getWiresharkConfirmationImpl() {
		return new WiresharkConfirmationImpl();
	}

	@Bean(name = "vlcConfirmationImpl")
	public VLCConfirmationImpl getVLCConfirmationImpl() {
		return new VLCConfirmationImpl();
	}
	
	@Bean(name = "brewConfirmationImpl")
	public BrewConfirmationImpl getBrewConfirmationImpl() {
		return new BrewConfirmationImpl();
	}
	
	@Bean(name="videoTabHelperImpl")
	public IVideoTabHelper getVideoTabHelperImpl(){
		return new VideoTabHelperImpl();
	}

	@Bean
	public IVideoAnalysisConfigHelper getVideoAnalysisConfigHelper(){
		return new VideoAnalysisConfigHelperImpl();
	}
	
	@Bean
	public IVideoEventDataHelper getVideoEventDataHelper(){
		return new VideoEventDataHelperImpl();
	}
	
	@Bean(name="stringParser")
	public IStringParse getStringParse(){
		return new StringParse();
	}

	@Bean(name = "externalProcessRunnerImpl")
	public IExternalProcessRunner getExternalProcessRunnerImpl() {
		return new ExternalProcessRunnerImpl();
	}

	@Bean(name = "videoFrameExtractor")
	public VideoFrameExtractor getVideoFrameExtractor(){
		return new VideoFrameExtractor();
	}

	@Bean(name = "videoStartupReadWrite")
	public IVideoStartupReadWrite getVideoStartupReadWriteImpl(){
		return new VideoStartupReadWriterImpl();
	}

	@Bean(name = "timeRangeReadWrite")
	public ITimeRangeReadWrite getTimeRangeReadWrite(){
		return new TimeRangeReadWrite();
	}

	@Bean(name = "videoSegmentAnalyzer")
	public VideoSegmentAnalyzer getVideoSegmentAnalyzer(){
		return new VideoSegmentAnalyzer();
	}

	@Bean(name ="videoUsagePrefs")
	public VideoUsagePrefs getVideoUsagePrefs(){
		return new VideoUsagePrefs();
	}
	
	@Bean(name="environmentDetailsHelper")
	public IEnvironmentDetailsHelper getEnvironmentDetailsHelper(){
		return new EnvironmentDetailsHelper();
	}
	
	@Bean(name="environmentDetailsReaderWriter")
	public IEnvironmentDetailsReadWrite getEnvironmentDetailsReadWrite(){
		return new EnvironmentDetailsReadWrite();
	}

	@Bean(name="metaDataHelper")
	public IMetaDataHelper getMetaDataHelper(){
		return new MetaDataHelper();
	}
	
	@Bean(name="metaDataReaderWriter")
	public IMetaDataReadWrite getMetaDataReadWrite(){
		return new MetaDataReadWrite();
	}

	@Bean(name ="videoPrefsController")
	public VideoPrefsController getVideoPrefsController() {
		return new VideoPrefsController();
	}
	
	@Bean(name="csiDataHelperImpl")
	public ICSIDataHelper getCSIDataHelperImpl(){
		return new CSIDataHelperImpl();
	}
	
	@Bean
	public ICellTowerInfoReader getCellTowerInfoReaderImpl() {
		return new CellTowerInfoReaderImpl();
	}
	
}