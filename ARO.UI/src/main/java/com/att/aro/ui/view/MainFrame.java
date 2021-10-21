/*
 *  Copyright 2015 AT&T
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

package com.att.aro.ui.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.android.ddmlib.IDevice;
import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.pojo.CollectorStatus;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.mobiledevice.pojo.IAroDevices;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.pojo.ErrorCodeEnum;
import com.att.aro.core.pojo.VersionInfo;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.util.CrashHandler;
import com.att.aro.core.util.FFmpegConfirmationImpl;
import com.att.aro.core.util.FFprobeConfirmationImpl;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.PcapConfirmationImpl;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.core.videoanalysis.pojo.VideoStream;
import com.att.aro.mvc.AROController;
import com.att.aro.ui.collection.AROCollectorSwingWorker;
import com.att.aro.ui.commonui.ARODiagnosticsOverviewRouteImpl;
import com.att.aro.ui.commonui.AROSwingWorker;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.GUIPreferences;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.MessageDialogFactory;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.bestpracticestab.BestPracticesTab;
import com.att.aro.ui.view.diagnostictab.ChartPlotOptions;
import com.att.aro.ui.view.diagnostictab.DiagnosticsTab;
import com.att.aro.ui.view.diagnostictab.GraphPanel;
import com.att.aro.ui.view.menu.AROMainFrameMenu;
import com.att.aro.ui.view.menu.help.SplashScreen;
import com.att.aro.ui.view.menu.tools.DataDump;
import com.att.aro.ui.view.menu.tools.PrivateDataDialog;
import com.att.aro.ui.view.overviewtab.OverviewTab;
import com.att.aro.ui.view.statistics.StatisticsTab;
import com.att.aro.ui.view.video.IVideoPlayer;
import com.att.aro.ui.view.video.LiveScreenViewDialog;
import com.att.aro.ui.view.video.VideoPlayerController;
import com.att.aro.ui.view.video.VlcjPlayer;
import com.att.aro.ui.view.videotab.VideoTab;
import com.att.aro.ui.view.waterfalltab.WaterfallTab;
import com.att.aro.view.images.Images;

import lombok.Getter;
import lombok.Setter;

public class MainFrame implements SharedAttributesProcesses {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages"); //$NON-NLS-1$
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	String voPath = System.getProperty("user.dir");

	private JFrame frmApplicationResourceOptimizer;
	private JTabbedPane jMainTabbedPane;
	private BestPracticesTab bestPracticesTab;
	private StatisticsTab statisticsTab;
	private DiagnosticsTab diagnosticsTab;
	private VideoTab videoTab;
	private OverviewTab overviewTab;
	private WaterfallTab waterfallTab;
	private AROMainFrameMenu mainMenu;
	private AROController aroController;
	private String tracePath;
	private String reportPath;
	private Profile profile;
	private final List<PropertyChangeListener> propertyChangeListeners = new ArrayList<PropertyChangeListener>();
	private final List<ActionListener> actionListeners = new ArrayList<ActionListener>();
	private static MainFrame window;
	private AROModelObserver modelObserver;
	private boolean deviceDataPulled = true;
	private static Date openSessionTimeStamp;  
	private static final Logger LOG = LogManager.getLogger(MainFrame.class.getSimpleName());
	private VersionInfo versionInfo = ContextAware.getAROConfigContext().getBean(VersionInfo.class);
	private TabPanels tabPanel = TabPanels.tab_panel_best_practices;
	
	private FFmpegConfirmationImpl ffmpegConfirmationImpl = ContextAware.getAROConfigContext().getBean("ffmpegConfirmationImpl", FFmpegConfirmationImpl.class);
	private FFprobeConfirmationImpl ffprobeConfirmationImpl = ContextAware.getAROConfigContext().getBean("ffprobeConfirmationImpl", FFprobeConfirmationImpl.class);

	private PcapConfirmationImpl pcapConfirmationImpl = ContextAware.getAROConfigContext().getBean("pcapConfirmationImpl", PcapConfirmationImpl.class);

	/**
	 * private data dialog reference
	 */
	private PrivateDataDialog privateDataDialog;
	private boolean videoPlayerSelected = true;
	private VideoPlayerController videoPlayerController;

	private CollectorStatus collectorStatus;

	private IARODiagnosticsOverviewRoute route;

	private LiveScreenViewDialog liveView;

	private AnalysisFilter filter;
	
	private AROCollectorSwingWorker<Void, Void> stopCollectorWorker;
	
	private int playbackWidth = 350;
	private int playbackHeight = 600;
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private int rtEdge = screenSize.width - playbackWidth;
	private long lastOpened = 0;
	@Getter
	private AROSwingWorker<Void, Void> aroSwingWorker;
	
	private Hashtable<String,Object> previousOptions;
	
	@Setter
	@Getter
	private boolean autoAssignPermissions = false;
	
	public static MainFrame getWindow() {
		return window;
	}

	public JFrame getJFrame() {
		return frmApplicationResourceOptimizer;
	}

	public AROController getController() {
		return aroController;
	}

	public PrivateDataDialog getPrivateDataDialog() {
		return privateDataDialog;
	}

	public void setPrivateDataDialog(PrivateDataDialog privateDataDialog) {
		this.privateDataDialog = privateDataDialog;
	}

	/**
	 * set up test conducted title to the location of related session
	 */
	public static void setLocationMap() {
		Container container = window.frmApplicationResourceOptimizer.getContentPane();
		Component[] components = container.getComponents();

		if (components != null && components.length > 0) {
			Component bpComponent = components[0];

			try {
				BestPracticesTab bpTab = (BestPracticesTab) bpComponent;
				bpTab.setScrollLocationMap();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new MainFrame();
					window.frmApplicationResourceOptimizer.setVisible(true);
					setLocationMap();
					openSessionTimeStamp = new Date();   
				} catch (Exception e) {
					e.printStackTrace();
				}
				final SplashScreen splash = new SplashScreen();
				splash.setVisible(true);
				splash.setAlwaysOnTop(true);
				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
						}
						return null;
					}
					@Override
					protected void done() {
						splash.dispose();
					}
				}.execute();

				new Thread(() -> {
					if (window.ffmpegConfirmationImpl.checkFFmpegExistance() == false) {
						SwingUtilities.invokeLater(() -> window.launchDialog(new FFmpegConfirmationDialog()));
					}
				},"ffmpeg check").start();
				
				new Thread(() -> {
					if (window.ffprobeConfirmationImpl.checkFFprobeExistance() == false) {
						SwingUtilities.invokeLater(() -> window.launchDialog(new FFprobeConfirmationDialog()));
					}
				},"ffprobe check").start();
				
				new Thread(() -> {
					if (Util.isMacOS() && !window.pcapConfirmationImpl.checkPcapVersion()) {
						SwingUtilities.invokeLater(() -> window.launchDialog(new PcapConfirmationDialog()));
					}
				},"pcapConfirmation").start();
			}
		});
	}

	private void launchDialog(ConfirmationDialog dialog) {
		dialog.createDialog();
		dialog.pack();
		dialog.setSize(dialog.getPreferredSize());
		dialog.validate();
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		dialog.setVisible(true);
	}

	/**
	 * Create the application.
	 */
	public MainFrame() {
		Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
		aroController = new AROController(this);
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	public void initialize() {
		long launchStartTime = System.currentTimeMillis();
		AROUIManager.init();
		int playbackWidth = 350;
		// int playbackHeight = 600;

		createVideoOptimizerFolder();

		Util.setLoggingLevel(Util.getLoggingLevel());
		frmApplicationResourceOptimizer = new JFrame();
		frmApplicationResourceOptimizer.setTitle(getAppTitle());
		frmApplicationResourceOptimizer.setIconImage(Images.ICON.getImage());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int rtEdge = screenSize.width - playbackWidth;
		frmApplicationResourceOptimizer.setBounds(0, 0, rtEdge, screenSize.height);
		frmApplicationResourceOptimizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainMenu = new AROMainFrameMenu(this);// .getInstance();
		modelObserver = new AROModelObserver();
		frmApplicationResourceOptimizer.setJMenuBar(mainMenu.getAROMainFileMenu());

		frmApplicationResourceOptimizer.setContentPane(getJTabbedPane());
		for (ChartPlotOptions option : GUIPreferences.getInstance().getChartPlotOptions()) {
			new Thread(() -> sendGADiagnosticTabChartPlotViews(option)).start();
		}
		// ----- Video Player Setup - Start -----
		initVideoPlayerController();
		diagnosticsTab.setVideoPlayer(videoPlayerController.getDefaultPlayer()); // Default video player: AROVideoPlayer
		// ----- Video Player Setup - End -----

		String titleName = MessageFormat
				.format(BUNDLE.getString("aro.title.short"), ApplicationConfig.getInstance().getAppShortName()).trim();
		sendGAEvents(titleName);
		frmApplicationResourceOptimizer.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent wEvent) {
				dispose();
			}

		});

		LOG.debug(String.format("ARO UI started :%12.3f", ((float) (System.currentTimeMillis() - launchStartTime)) / (60 * 60)));
	}

	private void sendGADiagnosticTabChartPlotViews(ChartPlotOptions  option) {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getDiagnosticsViewsEvent(), option.name(), option.name());	
	}
	
	private void sendGAEvents(String titleName) {
		Runnable runGA = new Runnable() {
			@Override
			public void run() {
				String versionStr = versionInfo.getVersion();
				String version = StringUtils.isBlank(versionStr) ? "" : versionStr.split(" ")[0];
				GoogleAnalyticsUtil.getGoogleAnalyticsInstance()
						.applicationInfo(GoogleAnalyticsUtil.getAnalyticsEvents().getTrackerID(), titleName, version);
				sendInstallationInfoTOGA();
				GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsStartSessionEvents(
						GoogleAnalyticsUtil.getAnalyticsEvents().getAnalyzerEvent(),
						GoogleAnalyticsUtil.getAnalyticsEvents().getStartApp(),
						Util.OS_NAME + (Util.OS_ARCHITECTURE.contains("64") ? " 64" : " 32")); 
			}
		};
		new Thread(runGA).start();
	}

	@SuppressWarnings("deprecation")
	private void createVideoOptimizerFolder() {
		File aroFolder = new File(Util.getAroLibrary());
		String videoOptimizerFolder = Util.getVideoOptimizerLibrary() + System.getProperty("file.separator");
		File videoOptimizerConfigFile = new File(videoOptimizerFolder + "config.properties");
		if (aroFolder.exists() && videoOptimizerConfigFile.length() == 0) {
			try {
				FileUtils.copyDirectory(new File(Util.getAroLibrary()), new File(Util.getVideoOptimizerLibrary()));
			} catch (IOException e) {
				LOG.error("Failed to copy file to VOLibrary", e);
			}
		}
	}

	private String getAppTitle() {
		return ApplicationConfig.getInstance().getAppBrandName() + " "
				+ ApplicationConfig.getInstance().getAppCombinedName();
	}

	private void initVideoPlayerController() {
		List<IVideoPlayer> players = new ArrayList<IVideoPlayer>();
		// Add all the video players intended to be used
		players.add(new VlcjPlayer(this));
		videoPlayerController = new VideoPlayerController(diagnosticsTab, players);
		modelObserver.registerObserver(videoPlayerController);
		videoPlayerController.launchPlayer(rtEdge, 0, playbackWidth, playbackHeight);
	}

	private JTabbedPane getJTabbedPane() {
		if (jMainTabbedPane == null) {
			UIManager.getDefaults().put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
			UIManager.getDefaults().put("TabbedPane.tabsOverlapBorder", true);

			jMainTabbedPane = new JTabbedPane();
			route = new ARODiagnosticsOverviewRouteImpl(jMainTabbedPane);

			bestPracticesTab = new BestPracticesTab(this, route);
			jMainTabbedPane.add(BUNDLE.getString("aro.tab.bestpractices"), bestPracticesTab);
			modelObserver.registerObserver(bestPracticesTab);

			overviewTab = new OverviewTab(route);
			jMainTabbedPane.add(BUNDLE.getString("aro.tab.overview"), overviewTab.layoutDataPanel());
			modelObserver.registerObserver(overviewTab);

			diagnosticsTab = new DiagnosticsTab(this, route);
			jMainTabbedPane.add(BUNDLE.getString("aro.tab.advanced"), diagnosticsTab);
			modelObserver.registerObserver(diagnosticsTab);

			videoTab = new VideoTab(this, route);
			jMainTabbedPane.add(BUNDLE.getString("aro.tab.video"), videoTab);
			modelObserver.registerObserver(videoTab);

			statisticsTab = new StatisticsTab(this);
			jMainTabbedPane.add(BUNDLE.getString("aro.tab.statistics"), statisticsTab);
			modelObserver.registerObserver(statisticsTab);

			waterfallTab = new WaterfallTab(route);
			jMainTabbedPane.add(BUNDLE.getString("aro.tab.waterfall"), waterfallTab.layoutDataPanel());
			modelObserver.registerObserver(waterfallTab);

			jMainTabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent event) {
					onTabChanged(event);
				}
			});
		}
		return jMainTabbedPane;
	}

	private void onTabChanged(ChangeEvent event) {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendViews(getCurrentTabComponent().getName());
		
		if (getCurrentTabComponent() == bestPracticesTab) {
			tabPanel = TabPanels.tab_panel_best_practices;
		} else if (getCurrentTabComponent() == statisticsTab) {
			tabPanel = TabPanels.tab_panel_statistics;
			
		} else if (getCurrentTabComponent() == diagnosticsTab) {
			tabPanel = TabPanels.tab_panel_other;
			getDiagnosticTab().addGraphPanel();
			
		} else if (getCurrentTabComponent() == videoTab) {
			tabPanel = TabPanels.tab_panel_video_tab;
		} else {
			tabPanel = TabPanels.tab_panel_other;
		}
	}

	@Override
	public Component getCurrentTabComponent() {
		return jMainTabbedPane.getSelectedComponent();
	}

	@Override
	public void addAROPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
		propertyChangeListeners.add(propertyChangeListener);
	}

	@Override
	public void addAROActionListener(ActionListener actionListener) {
		actionListeners.add(actionListener);
	}


	@Override
	public void updateTracePath(File path, TimeRange... timeRange) {
		if (path != null) {
			lastOpened = System.currentTimeMillis();
			if (timeRange != null && timeRange.length == 1 && timeRange[0] != null) {
				timeRange[0].setPath(path.getAbsolutePath());
				notifyPropertyChangeListeners("tracePath", tracePath, timeRange[0]);
			} else {
				notifyPropertyChangeListeners("tracePath", tracePath, path.getAbsolutePath());
			}
			if (path.getAbsolutePath().contains(".cap")) {
				tracePath = path.getAbsolutePath().substring(0,
						path.getAbsolutePath().lastIndexOf(Util.FILE_SEPARATOR));
			} else {
				tracePath = path.getAbsolutePath();
			}
			updatePath();
			boolean isDir = path.isDirectory();
			Runnable sendAnalytics = () -> sendAnalytics(isDir);
			new Thread(sendAnalytics).start();
		}
	}
	
	public long getLastOpenedTime() {
		return lastOpened;
	}

	private void sendAnalytics(boolean isDir) {
		if (isDir) {
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getAnalyzerEvent(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getLoadTrace());

		} else {
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getAnalyzerEvent(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getLoadPcap());
		}
	}

	private void updatePath() {
		PreferenceHandlerImpl.getInstance().setPref("TRACE_PATH", tracePath);
	}

	/**
	 * Refreshes the bestpractice tab to reload modified information
	 */
	public void refreshBestPracticesTab() {
		bestPracticesTab.refresh(aroController.getTheModel());
	}
	
	public void refreshVideoTab() {
		videoTab.refresh(aroController.getTheModel());
	}
	
	@Override
	public void updateReportPath(File path) {
		if (path != null) {
			notifyPropertyChangeListeners("reportPath", reportPath, path.getAbsolutePath());
			reportPath = path.getAbsolutePath();
		}
	}

	private void showErrorMessage(String message) {
		MessageDialogFactory.showMessageDialog(this.getJFrame(), ResourceBundleHelper.getMessageString(message),
				ResourceBundleHelper.getMessageString("menu.error.title"), JOptionPane.ERROR_MESSAGE);
	}
	
	public void sendGATraceParams(TraceDirectoryResult traceResults){
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				traceResults.getDeviceDetail().getOsType(),
				traceResults.getDeviceDetail().getOsVersion(), traceResults.getDeviceDetail().getDeviceModel());
		//Secure
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getSecureCollectorEvent(), 
				GoogleAnalyticsUtil.getAnalyticsEvents().getSecureCollectorApplied(),
				traceResults.getCollectOptions().getSecureStatus().toString());
		//Attenuation
		if(traceResults.getCollectOptions().isAttnrProfile()){
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationEvent(), 
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationApplied(),
					"Profile");
		}else if(traceResults.getCollectOptions().getThrottleDL() > 0 && traceResults.getCollectOptions().getThrottleUL() > 0){
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationEvent(), 
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationApplied(),
					"Dynamic");
		}else{
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationEvent(), 
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationNotApplied(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getAttenuationNotApplied());
		}
		//trace duration
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getTraceDurationEvent(), 
				GoogleAnalyticsUtil.getAnalyticsEvents().getTraceDurationEvent(),
				String.format("%.2f", traceResults.getTraceDuration()));
	}

	public void refresh() {

		if (aroController != null) {
			AROTraceData traceData = aroController.getTheModel();
			if (traceData.isSuccess()) {
				modelObserver.refreshModel(traceData);
				this.profile = traceData.getAnalyzerResult().getProfile();
				if (traceData.getAnalyzerResult().getTraceresult().getTraceResultType() == TraceResultType.TRACE_DIRECTORY) {
					TraceDirectoryResult traceResults = (TraceDirectoryResult) traceData.getAnalyzerResult().getTraceresult();
					(new Thread(() -> sendGATraceParams(traceResults))).start();
					Util.updateRecentItem(traceResults.getTraceDirectory());
					frmApplicationResourceOptimizer.setJMenuBar(mainMenu.getAROMainFileMenu());
					frmApplicationResourceOptimizer.getJMenuBar().updateUI();
				}
			} else if (traceData.getError() != null) {
				if (isUpdateRequired(traceData.getError().getCode())) {
					Util.updateRecentItem(tracePath);
				}

				tracePath = null;
				if (aroSwingWorker != null) {
					aroSwingWorker.cancel(true);

					try {
						Thread.sleep(150);
					} catch (Exception exc) {
						LOG.info("Thread sleep exception");
					}
				}
				MessageDialogFactory.getInstance().showErrorDialog(window.getJFrame(), traceData.getError().getDescription());
			} else {
				showErrorMessage("menu.error.unknownfileformat");
			}

		}
	}

	private boolean isUpdateRequired(int errorCode) {
		boolean isUpdateRequired = false;
		if(ErrorCodeEnum.DEVICE_ACCESS.getCode().equals(errorCode) || ErrorCodeEnum.POST_ERROR.getCode().equals(errorCode)
				|| ErrorCodeEnum.DIR_EXIST.getCode().equals(errorCode)) {
			isUpdateRequired = true;
		}
		return isUpdateRequired;
	}

	@Override
	public String getTracePath() {
		return tracePath;
	}

	@Override
	public boolean isModelPresent() {
		return aroController != null && aroController.getTheModel() != null && aroController.getTheModel().isSuccess();
	}

	@Override
	public String getReportPath() {
		return reportPath;
	}

	@Override
	public TabPanels getCurrentTabPanel() {
		return tabPanel;
	}

	/**
	 * Sets the device profile that is used for analysis.
	 * 
	 * @param profile
	 *            - The device profile to be set.
	 * 
	 * @throws IOException
	 */
	@Override
	public void updateProfile(Profile profile) {
		notifyPropertyChangeListeners("profile", this.profile, profile);
		this.profile = profile;
	}

	@Override
	public boolean isVideoPlayerSelected() {
		return videoPlayerSelected;
	}

	@Override
	public IVideoPlayer getVideoPlayer() {
		return diagnosticsTab.getVideoPlayer();
	}
	
	@Override
	public GraphPanel getGraphPanel() {
		return diagnosticsTab.getGraphPanel();
	}

	@Override
	public void updateVideoPlayerSelected(boolean videoPlayerSelected) {
		this.videoPlayerSelected = videoPlayerSelected;
		videoPlayerController.getCurrentVideoPlayer().setVisibility(videoPlayerSelected);
	}

	@Override
	public Profile getProfile() {
		return profile;
	}

	public void notifyPropertyChangeListeners(String property, Object oldValue, Object newValue) {
		if (property.equals("profile")) {
			new AROSwingWorker<Void, Void>(frmApplicationResourceOptimizer, propertyChangeListeners, property, oldValue,
					newValue, ResourceBundleHelper.getMessageString("configuration.applied")).execute();
		} else if (property.equals("filter")) {
			new AROSwingWorker<Void, Void>(frmApplicationResourceOptimizer, propertyChangeListeners, property, oldValue,
					newValue, null).execute();
		} else {
			aroSwingWorker = new AROSwingWorker<Void, Void>(frmApplicationResourceOptimizer, propertyChangeListeners,
					property, 
					oldValue, newValue, 
					null);
			aroSwingWorker.execute();
		}
	}

	// @Override
	public void notifyActionListeners(int id, String command) {
		new AROSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, id, command, null).execute();
	}

	@Override
	public void startCollector(IAroDevice device, String traceFolderName, Hashtable<String, Object> extraParams) {
		previousOptions = extraParams;
		new AROCollectorSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, 1, "startCollector",
				device, traceFolderName, extraParams).execute();
	}

	@Override
	public void startCollectorIos(IDataCollector iOsCollector, String udid, String tracePath, VideoOption videoOption) {
		new AROCollectorSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, 2,
				"startCollectorIos", iOsCollector, udid, tracePath, videoOption).execute();
	}

	@Override
	public void liveVideoDisplay(IDataCollector collector) {
		liveView = new LiveScreenViewDialog(this, collector);
		liveView.setVisible(true);
		liveView.setVideoOption(aroController.getVideoOption());
		LOG.info("liveVideoDisplay started");
	}
	
	@Override
	public void startVideoCollector(String msg) {
		stopCollectorWorker = new AROCollectorSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, 3, "startManualVideoCollection", msg);
		stopCollectorWorker.execute();
	}
	
	@Override
	public void stopCollector() {
		if (liveView != null) {
			liveView.setVisible(false);
			liveView = null;
		}
		stopCollectorWorker = new AROCollectorSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, 3, "stopCollector", null);
		stopCollectorWorker.execute();
	}

	@Override
	public void cancelCollector() {
		if (liveView != null) {
			liveView.setVisible(false);
			liveView = null;
		}
		new AROCollectorSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, 3, "cancelCollector", null).execute();
	}

	@Override
	public void haltCollector() {
		if (liveView != null) {
			liveView.setVisible(false);
			liveView = null;
		}
		new AROCollectorSwingWorker<Void, Void>(frmApplicationResourceOptimizer, actionListeners, 3, "haltCollectorInDevice", null).execute();
	}

	@Override
	public List<IDataCollector> getAvailableCollectors() {
		return aroController.getAvailableCollectors();
	}

	@Override
	public IDevice[] getConnectedDevices() {
		return aroController.getConnectedDevices();
	}

	@Override
	public IAroDevices getAroDevices() {
		return aroController.getAroDevices();
	}

	public void setDeviceDataPulled(boolean status) {
		deviceDataPulled = status;
	}

	public boolean isDeviceDataPulled() {
		return deviceDataPulled;
	}

	@Override
	public void updateCollectorStatus(CollectorStatus collectorStatus, StatusResult statusResult) {

		this.collectorStatus = collectorStatus;

		if (statusResult == null) {
			return;
		}
		LOG.info(String.format("updateCollectorStatus - Collector state: %s, Result status: %s", collectorStatus == null ? "null" : collectorStatus.name(), statusResult));
		if (!isDeviceDataPulled()) {
			JOptionPane.showMessageDialog(window.getJFrame(), BUNDLE.getString("MainFrame.pulldata.message"),
					BUNDLE.getString("MainFrame.pulldata.title"), JOptionPane.WARNING_MESSAGE);
		}
		// timeout - collection not approved in time
		if (!statusResult.isSuccess()) {
			//String traceFolder = aroController.getTraceFolderPath();
			LOG.info("updateCollectorStatus :FAILED STATUS :" + statusResult.getError().getDescription());
			if (statusResult.getError().getCode() == 206){
				int option = MessageDialogFactory.getInstance().showStopDialog(window.getJFrame(), statusResult.getError().getDescription(), BUNDLE.getString("error.title"), JOptionPane.DEFAULT_OPTION);
				if (option == JOptionPane.YES_NO_OPTION || CollectorStatus.CANCELLED == collectorStatus){
					cancelCollector();
				}
			} else {
				String errorMessage = statusResult.getError().getDescription();				
				if (statusResult.getError().getCode() == 512) {
					MessageDialogFactory.getInstance().showInformationDialog(window.getJFrame(), BUNDLE.getString("Error.rvi.resetconnection"), BUNDLE.getString("Error.rvi.resetconnection.title"));					
				} else if (errorMessage.contains("0xe8008016")) {
					MessageDialogFactory.getInstance().showInformationDialog(window.getJFrame(), BUNDLE.getString("Error.app.provision.invalidentitle"), BUNDLE.getString("Error.app.provision.invalidentitle.title"));					
				}else {
					MessageDialogFactory.getInstance().showErrorDialog(window.getJFrame(), errorMessage);
				}
			}
			return;
		}

		// Collection has been stopped ask to open trace
		if (collectorStatus != null && collectorStatus.equals(CollectorStatus.STOPPED)) {
			stopCollectorWorker.hideProgressDialog();
			LOG.info("stopDialog");
			String traceFolder = aroController.getTraceFolderPath();
			int seconds = (int) (aroController.getTraceDuration()/1000);
			boolean approveOpenTrace = MessageDialogFactory.getInstance().showTraceSummary(frmApplicationResourceOptimizer, traceFolder, !aroController.getVideoOption().equals(VideoOption.NONE), Util.formatHHMMSS(seconds));
			if (approveOpenTrace){
				updateTracePath(new File(aroController.getTraceFolderPath()));
			}
			return;
		}
	}

	@Override
	public CollectorStatus getCollectorStatus() {
		return collectorStatus;
	}

	/*-----------------------------
	 * end of Start-stop Collectors
	 * ----------------------------*/

	@Override
	public Frame getFrame() {
		return frmApplicationResourceOptimizer;
	}

	@Override
	public void updateChartSelection(List<ChartPlotOptions> optionsSelected) {
		diagnosticsTab.setChartOptions(optionsSelected);
	}

	@Override
	public void dataDump(File dir) throws IOException {
		new DataDump(dir, aroController, false, false);
	}

	@Override
	public void dispose() {
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEndSessionEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getAnalyzerEvent(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getEndApp());
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getAnalyzerEvent(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getTracesAnalyzedEvent(),
				String.valueOf(GoogleAnalyticsUtil.getTraceCounter()));
		Date closeSessionTimeStamp = new Date();
		long diff = TimeUnit.MILLISECONDS.toMinutes(closeSessionTimeStamp.getTime() - openSessionTimeStamp.getTime());
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
				GoogleAnalyticsUtil.getAnalyticsEvents().getVOSessionEvent(),
				GoogleAnalyticsUtil.getAnalyticsEvents().getVOSessionDuration(), String.valueOf(diff));
		GoogleAnalyticsUtil.getGoogleAnalyticsInstance().close();
	}

	private void sendInstallationInfoTOGA() {

		String userHome = System.getProperty("user.home") + "/gaInstalledFile.txt";

		File installationFile = new File(userHome);

		if (installationFile.exists()) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm");
			long installedTime = installationFile.lastModified();
			String lastModifiedDate = df.format(new Date(installedTime));
			lastModifiedDate = lastModifiedDate.replace(" ", "-");
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getInstaller(), versionInfo.getVersion(),
					lastModifiedDate);
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsEvents(
					GoogleAnalyticsUtil.getAnalyticsEvents().getInstaller(),
					GoogleAnalyticsUtil.getAnalyticsEvents().getLanguage(), System.getProperty("java.version"));
			installationFile.delete();

		}
	}

	@Override
	public void updateFilter(AnalysisFilter arg0) {
		notifyPropertyChangeListeners("filter", this.filter, arg0);
		this.filter = arg0;

	}
	
	@Override
	public void hideChartItems(String... chartPlotOptionEnumNames) {
		diagnosticsTab.hideCharts(chartPlotOptionEnumNames);
	}
	
	@Override
	public void showChartItems(String... chartPlotOptionEnumNames){
		diagnosticsTab.showCharts(chartPlotOptionEnumNames);
	}
	
	public DiagnosticsTab getDiagnosticTab(){
		return diagnosticsTab;
	}
	
	public VideoTab getVideoTab(){
		return videoTab;
	}

	public void postToAmvots(AbstractTraceResult traceResult, VideoStream videoStream) {
		if (TraceResultType.TRACE_DIRECTORY.equals(traceResult.getTraceResultType())) {
			new Thread(() -> {
				SwingUtilities.invokeLater(() -> window.launchDialog(new VideoPostDialog(traceResult, videoStream)));
			}).start();
		}
	}
	
	@Override
	public String[] getApplicationsList(String id) {
		return aroController.getApplicationsList(id);
	}

	public Hashtable<String,Object> getPreviousOptions() {
		return previousOptions;
	}

	public void setPreviousOptions(Hashtable<String,Object> previousOptions) {
		this.previousOptions = previousOptions;
	}

}
