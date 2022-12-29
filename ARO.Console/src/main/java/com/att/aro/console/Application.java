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
package com.att.aro.console;

import static com.att.aro.core.settings.SettingsUtil.getSelectedBPsList;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.android.ddmlib.IDevice;
import com.att.aro.console.printstreamutils.ImHereThread;
import com.att.aro.console.printstreamutils.NullOut;
import com.att.aro.console.printstreamutils.OutSave;
import com.att.aro.console.util.MacHotspotUtil;
import com.att.aro.console.util.ThrottleUtil;
import com.att.aro.console.util.UtilOut;
import com.att.aro.core.IAROService;
import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.datacollector.DataCollectorType;
import com.att.aro.core.datacollector.IDataCollector;
import com.att.aro.core.datacollector.IDataCollectorManager;
import com.att.aro.core.datacollector.pojo.CollectorStatus;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.exception.TsharkException;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.mobiledevice.pojo.IAroDevice;
import com.att.aro.core.mobiledevice.pojo.IAroDevice.AroDeviceState;
import com.att.aro.core.mobiledevice.pojo.IAroDevice.Platform;
import com.att.aro.core.mobiledevice.pojo.IAroDevices;
import com.att.aro.core.packetanalysis.pojo.AnalysisFilter;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.pojo.AttenuatorModel;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.pojo.ErrorCode;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.util.NetworkUtil;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.util.VideoUtils;
import com.att.aro.core.video.pojo.Orientation;
import com.att.aro.core.video.pojo.VideoOption;
import com.att.aro.mvc.AROController;
import com.att.aro.mvc.IAROView;
import com.beust.jcommander.JCommander;

import lombok.Getter;
import lombok.Setter;

public final class Application implements IAROView {

    private final Logger LOGGER = Logger.getLogger(Application.class);

    private UtilOut utilOut;

    private AROController aroController;

    private IAroDevices aroDevices;

    private Commands cmds;

    private List<IDataCollector> collectorList;

    private VideoOption videoOption = VideoOption.NONE;

    ResourceBundle buildBundle = ResourceBundle.getBundle("build");

	private MetaDataModel metaDataModel;

	private String trafficFile;

	private String videoFile;
	
	@Setter
	@Getter
	private List<String[]> voIpAddressList;

    private Application(String[] args) {

        removeStdoutAppender();
        ApplicationContext context = SpringContextUtil.getInstance().getContext();
        aroController = new AROController(this);
        loadCommands(args);
        utilOut = cmds.isVerbose() ? new UtilOut(UtilOut.MessageThreshold.Verbose) : new UtilOut();

        OutSave outSave = prepareSystemOut();
        try {
            LOGGER.debug("ARO Console app start");
        } finally {
            restoreSystemOut(outSave);
        }
        // command sanity check, if fails then reverts to help
        if (cmds.isHelp() || !((cmds.isListcollector() || cmds.isListDevices())
                || !(cmds.getAnalyze() == null && cmds.getStartcollector() == null && cmds.getAsk() == null))) {
            usageHelp();
            System.exit(1);
        }

        collectorList = aroController.getAvailableCollectors();
        if (collectorList == null || collectorList.size() == 0) {
            println("Error: There are no collectors installed!");
            restoreSystemOut(outSave);
            System.exit(1);
        }
        if (cmds.isListcollector()) {
            showCollector(context, cmds);
            System.exit(1);
        }
        if (cmds.isListDevices()) {
            showDevices(context, cmds);
            System.exit(1);
        }
        // validate command entered
        ErrorCode error = new Validator().validate(cmds, context);
        if (error != null) {
            printError(error);
            System.exit(1);
        }
        if ((cmds.getStartcollector() != null || cmds.getAsk() != null) && cmds.getOutput() == null) {
            outln("Error: No output tracefolder was entered\n");
            System.exit(1);
        }
        // ask user for device selection
        if (cmds.getAsk() != null) {
            selectDevice(context, outSave);
        }
        // start the collector
        if (cmds.getStartcollector() != null) {
            runDataCollector(context, cmds);
        } else if (cmds.getAnalyze() != null) {
            runAnalyzer(context, cmds);
        }
        outSave = prepareSystemOut();
        try {
            LOGGER.debug("Console app ended");
        } finally {
            restoreSystemOut(outSave);
        }

    }

    // Avoid exceptions to be displayed to users in the command prompt
    private void removeStdoutAppender() {
        Logger rootLogger = Logger.getRootLogger();
        Appender appd = rootLogger.getAppender("stdout");
        rootLogger.removeAppender(appd);
    }

    private void selectDevice(ApplicationContext context, OutSave outSave) {
        aroDevices = showDevices(context, cmds);
        int selection = 0;
        IAroDevice device = null;
        // String selected = aroDevices.getId(3);
        if (aroDevices.size() > 1) {
            selection = -1;
            do {
                String range = "0-" + (aroDevices.size() - 1);
                String message = "Select a device, q to quit :" + range;
                String sValue = null;
                try {
                    sValue = input(outSave, message, Pattern.compile("[" + range + "q]"));
                    if (sValue.contains("q")) {
                        restoreSystemOut(outSave);
                        System.exit(0);
                    }
                    selection = Integer.valueOf(sValue);
                } catch (NumberFormatException e) {
                    outln("Illegal entry, unable to parse \"" + sValue + "\"");
                }
            } while (selection < 0 || selection >= aroDevices.size());

        } else if (aroDevices.size() == 1 && aroDevices.getDevice(0).getState().equals(AroDeviceState.Available)) {
            selection = 0;
        } else {
            errln("No devices available");
            restoreSystemOut(outSave);
            System.exit(0);
        }

        // have a selected device
        device = aroDevices.getDevice(selection);

        cmds.setDeviceid(device.getId());

        // prepare collector choice
        String requestCollector = cmds.getAsk();
        if (cmds.getStartcollector() != null) {
            requestCollector = cmds.getStartcollector();
        }
        if (!collectorCompatibility(requestCollector, device)) {
            outln("Error :Incompatible collector for device:" + device.getPlatform() + ", collector:" + requestCollector);
            System.exit(0);
        }
        String deviceCollector = collectorSanityCheck(requestCollector, device);
        if (deviceCollector.startsWith("Error")) {
            outln(deviceCollector);
            System.exit(0);
        }
        if ("auto".equals(requestCollector)) {
            // store the auto selection
            cmds.setStartcollector(deviceCollector);
        } else if (!requestCollector.equalsIgnoreCase(deviceCollector)) {
            if (device.isRooted()) {
                // run rooted or vpn on rooted
                cmds.setStartcollector(requestCollector);
            } else if (!device.isRooted() && !requestCollector.equals("vpn_collector")) {
                // only run vpn on non-rooted
                cmds.setStartcollector(requestCollector);
            } else {
                outln("Error: incompatable collector for chosen device");
                System.exit(0);
            }
        } else {
            // allow the asked collector
            cmds.setStartcollector(requestCollector);
        }
    }

    /**
     * Compares device with the requested collector
     * 
     * @param requestCollector
     * @param device
     * @return
     */
    private boolean collectorCompatibility(String requestCollector, IAroDevice device) {
        Platform platform = device.getPlatform();
        switch (requestCollector) {
            case "rooted_android":
                if (device.isEmulator()) {
                    return (device.getAbi().contains("arm"));
                }
            case "vpn_android":
                if (device.isEmulator()) {
                    return (device.getAbi().contains("x86_64"));
                }
                return (!platform.equals(IAroDevice.Platform.iOS));
            case "ios":
                return (!platform.equals(IAroDevice.Platform.Android));
            case "auto":
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * Performs a sanity check to guard against incompatible device/collector
     * combinations. returns a collector that is compatible if there is a mismatch.
     * 
     * @param collector
     * @param device
     * @return collector that is compatible with device
     */
    private String collectorSanityCheck(String collector, IAroDevice device) {
        String result = collector;

        switch (device.getPlatform()) {

            case iOS:
                result = "ios";
                break;
            case Android:
                if (device.isEmulator()) {
                    if (device.getAbi().contains("arm")) {
                        result = "rooted_android";
                    } else if (device.getAbi().equals("x86")) {
                        result = "Error: incompatable device, x86 Emulators are unsupported, use x86_64 or armeabi instead";
                    } else {
                        result = "vpn_android"; // default to VPN
                    }
                } else {
                    result = "vpn_android"; // default to VPN
                }
                break;

            default:
                break;
        }
        return result;
    }

    private void loadCommands(String[] args) {
        cmds = new Commands();
        try {
            new JCommander(cmds, args).setProgramName("aro");
        } catch (Exception ex) {
            System.err.print("Error parsing command: " + ex.getMessage());
            System.exit(1);
        }
    }

    private String input(OutSave outSave, String message, Pattern pattern) {
        String input = "";
        try {

            do {
                out(message);
                out(">");
                input = readInput();
            } while (!pattern.matcher(input).find());
        } finally {
            restoreSystemOut(outSave);
        }
        return input;
    }

    /**
     * @param args
     *            - see Help
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new Application(args);
        System.exit(0);
    }

    /**
     * Locates and displays any and all data collectors. Data collectors are jar
     * files that allow controlling and collecting data on devices such as Android
     * phone, tablets and emulators.
     * 
     * @param context
     *            - Spring ApplicationContext
     * @param cmds
     *            - Not used
     */
    void showCollector(ApplicationContext context, Commands cmds) {
        List<IDataCollector> list = getAvailableCollectors();
        if (list == null || list.size() < 1) {
            errln("No data collector found");
        } else {
            for (IDataCollector coll : list) {
                outln("-" + coll.getName() + " version: " + coll.getMajorVersion() + "." + coll.getMinorVersion());
            }
        }
    }

    /**
     * Scans for and delivers an IAroDevices model
     * 
     * @param context
     * @param cmds
     * @return IAroDevices
     */
    private IAroDevices showDevices(ApplicationContext context, Commands cmds) {

        outln("scanning devices...");

        IAroDevices aroDevices = aroController.getAroDevices();
        outln("list devices");
        if (aroDevices.size() > 0) {
            outln(aroDevices.toString());
        } else {
            outln(" No devices detected");
        }
        return aroDevices;
    }

    private OutSave prepareSystemOut() {
        OutSave outSave = new OutSave(System.out, Logger.getRootLogger().getLevel());
        if (utilOut.getThreshold().ordinal() < UtilOut.MessageThreshold.Verbose.ordinal()) {
            Logger.getRootLogger().setLevel(Level.WARN);
            System.setOut(new PrintStream(new NullOut()));
        }
        return outSave;
    }

    private void restoreSystemOut(OutSave outSave) {
        System.setOut(outSave.getOut());
        Logger.getRootLogger().setLevel(outSave.getLevel());
    }

    /**
     * Analyze a trace and produce a report either in json or html<br>
     * 
     * <pre>
     * Required command:
     *   --analyze with path to trace directory of traffic.cap
     *   --output output file, error if missing
     *   --format html or json, if missing defaults to json
     * 
     * @param context
     *            - Spring ApplicationContext
     * @param cmds
     *            - user commands
     */
    void runAnalyzer(ApplicationContext context, Commands cmds) {
        String trace = cmds.getAnalyze();
        IAROService serv = context.getBean(IAROService.class);
        AROTraceData results = null;

        // analyze trace file or directory?
        OutSave outSave = prepareSystemOut();
        ImHereThread imHereThread = new ImHereThread(outSave.getOut(), Logger.getRootLogger());
        
		Map<String, String[]> traceFileMap;
		if ((traceFileMap = VideoUtils.validateFolder(new File(trace))).size() > 0) {
			String[] trafficFile = traceFileMap.get(VideoUtils.TRAFFIC);
			String[] videoFile = traceFileMap.get(VideoUtils.VIDEO);		
			if (trafficFile.length == 1) {
				setTrafficFile(trafficFile[0]);
				setVideoFile(videoFile[0]);
			} else {
				LOGGER.error("Invalid trace folder: There are " + trafficFile.length + " traffic files in the folder");
				outln("\n\nInvalid trace folder: There are " + trafficFile.length + " traffic files in the folder.");
				exitCLI(outSave, imHereThread);
			}
			IFileManager fileManager = context.getBean(IFileManager.class);
			if (!fileManager.fileExist(trace, "time")) {
				String[] commands = Util.getParentAndCommand(Util.getCapinfos());
				String cmd = String.format("%s \"%s\"", commands[1], new File(trace, trafficFile[0]).toString());
				String capinfosData = Util.getExternalProcessRunner().executeCmd((commands[0] != null) ? new File(commands[0]) : null, cmd, true, true);
				
				double start = Util.parseForUTC(StringParse.findLabeledDataFromString("First packet time:", Util.LINE_SEPARATOR, capinfosData) + "Z") / 1000;
				double end = Util.parseForUTC(StringParse.findLabeledDataFromString("Last packet time:", Util.LINE_SEPARATOR, capinfosData)) / 1000;
				String timeText = String.format("Synchronized timestamps\n%.3f\n%.0f\n%.3f", start, 0.0, end);
				InputStream stream = new ByteArrayInputStream(timeText.getBytes());
				try {
					fileManager.saveFile(stream, trace + "/time");
					fileManager.createEmptyFile(new File(trace), ".readme");
				} catch (IOException e1) {
					LOGGER.error("failed to save 'time' file", e1);
				}
			}
		}
        
        try {
            if (serv.isFile(trace)) {
                try {
                    results = serv.analyzeFile(getSelectedBPsList(), trace);
                } catch (IOException | TsharkException e) {
                    errln("Error occured analyzing trace, detail: " + e.getMessage());
                    System.exit(1);
                }
            } else {
                try {
                    results = serv.analyzeDirectory(getSelectedBPsList(), trace, this);
                } catch (IOException e) {
                    errln("Error occured analyzing trace directory, detail: " + e.getMessage());
                    System.exit(1);
                }
            }

            if (results != null && results.isSuccess()) {
                outSave = prepareSystemOut();
                if (cmds.getFormat().equals("json")) {
                    if (serv.getJSonReport(cmds.getOutput(), results)) {
                        outln("Successfully produced JSON report: " + cmds.getOutput());
                    } else {
                        errln("Failed to produce JSON report.");
                    }
                } else {
                    if (serv.getHtmlReport(cmds.getOutput(), results)) {
                        println("Successfully produced HTML report: " + cmds.getOutput());
                    } else {
                        errln("Failed to produce HTML report.");
                    }
                }
            } else {
                printError(results == null ? new ErrorCode() : results.getError());
            }
        } finally {
            imHereThread.endIndicator();
            while (imHereThread.isRunning()) {
                Thread.yield();
            }
            restoreSystemOut(outSave);
        }
        System.exit(0);
    }

	private void exitCLI(OutSave outSave, ImHereThread imHereThread) {
		imHereThread.endIndicator();
		while (imHereThread.isRunning()) {
			Thread.yield();
		}
		restoreSystemOut(outSave);
		System.exit(0);
	}

	private VideoOption configureVideoOption(String videoOption) {
        VideoOption option = VideoOption.NONE;
        switch (videoOption) {
            case "yes":
            case "slow":
                option = VideoOption.LREZ;
                break;
            case "hd":
                option = VideoOption.HDEF;
                break;
            case "sd":
                option = VideoOption.SDEF;
                break;
            case "no":
                option = VideoOption.NONE;
                break;
            default:
                break;
        }

        boolean isRootedColl = "rooted_android".equals(cmds.getStartcollector());
        if (isRootedColl && (option == VideoOption.SDEF || option == VideoOption.HDEF)) {
            println("HD/SD Video is not supported on rooted collector: Setting video to low resolution");
            return VideoOption.LREZ;
        }
        return option;
    }

    /**
     * Get orientation object from a string.
     * 
     * @param videoOrientation
     * @return Orientation object
     */
    private Orientation getOrientation(String orientation) {
        if (StringUtils.isNotBlank(orientation)) {
            switch (orientation) {
                case "landscape":
                    return Orientation.LANDSCAPE;
                case "portrait":
                    return Orientation.PORTRAIT;
            }
        }

        return null;
    }

    private int getThrottleUL() {
        String throttleUL = cmds.getThrottleUL();
        return ThrottleUtil.getInstance().parseNumCvtUnit(throttleUL);
    }

    private int getThrottleDL() {
        String throttleDL = cmds.getThrottleDL();
        return ThrottleUtil.getInstance().parseNumCvtUnit(throttleDL);
    }

    void printError(ErrorCode error) {
        err("Error code: " + error.getCode());
        err(", Error name: " + error.getName());
        errln(", Error description: " + error.getDescription());
    }

    /**
     * Launches a DataCollection. Provides an input prompt for the user to stop the
     * collection by typing "stop"
     * 
     * <pre>
     * Note:
     * Do not exit collection by pressing a ctrl-c
     * Doing so will exit ARO.Console but will not stop the trace on the device.
     * </pre>
     * 
     * @param context
     * @param cmds
     */
    @SuppressWarnings("null") // ignoring incorrect eclipse warning
    void runDataCollector(ApplicationContext context, Commands cmds) {
        if (cmds.getOutput() != null) {
            // LOGGER.info("runDataCollector");
            IDataCollectorManager colmg = context.getBean(IDataCollectorManager.class);
            colmg.getAvailableCollectors(context);
            IDataCollector collector = null;

            switch (cmds.getStartcollector()) {
                case "rooted_android":
                    collector = colmg.getRootedDataCollector();
                    break;

                case "vpn_android":
                    collector = colmg.getNorootedDataCollector();
                    break;

                case "ios":
                    collector = colmg.getIOSCollector();
                    if (cmds.getSudo().isEmpty() || !collector.setPassword(cmds.getSudo())) {
                        printError(ErrorCodeRegistry.getInvalidPasswordError());
                        System.exit(1);
                    }
                    if ("hd".equals(cmds.getVideo()) || "sd".equals(cmds.getVideo())) {
                        printError(ErrorCodeRegistry.getInvalidiOSArgs());
                        System.exit(1);
                    }
                    break;

                default:
                    printError(ErrorCodeRegistry.getCollectorNotfound());
                    System.exit(1);
                    break;
            }

            StatusResult result = null;
            if (collector == null) {
                printError(ErrorCodeRegistry.getCollectorNotfound());
                System.exit(1);
            }

            if (cmds.getOverwrite().equalsIgnoreCase("yes")) {
                String traceName = cmds.getOutput();
                IFileManager filemanager = context.getBean(IFileManager.class);
                filemanager.directoryDeleteInnerFiles(traceName);
            }
            OutSave outSave = prepareSystemOut();

            AttenuatorModel model = getAttenuateModel(cmds);
            // If the user want to collect regular iOS collection, they can proceed
            if (DataCollectorType.IOS.equals(collector.getType()) && (model.isThrottleDLEnabled() || model.isThrottleULEnabled())) {
                if (isIOSAttenuationConfirmed() && NetworkUtil.isNetworkUp("bridge100")) {
                    model.setConstantThrottle(true);
                    println("Collection proceeded.");
                } else {
                    System.exit(1);
                }
            }

            videoOption = configureVideoOption(cmds.getVideo());
            try {
                Hashtable<String, Object> extras = new Hashtable<String, Object>();
                Orientation videoOrientation = getOrientation(cmds.getVideoOrientation());

                extras.put("video_option", getVideoOption());
                extras.put("videoOrientation", videoOrientation == null ? Orientation.PORTRAIT : videoOrientation);
                extras.put("AttenuatorModel", model);
                extras.put("assignPermission", false);
                result = runCommand(cmds, collector, cmds.getSudo(), extras);
            } finally {
                restoreSystemOut(outSave);
            }

            if (result.getError() != null) {
                outln("Caught an error:");
                printError(result.getError());
            } else {

                outSave = prepareSystemOut();
                try {
                    String input = "";
                    print("Data collector is running, enter stop to save trace and quit program");
                    print(">");
                    do {
                        input = readInput();
                    } while (!input.contains("stop"));
                } finally {
                    restoreSystemOut(outSave);
                }

                println("stopping collector...");
                try {
                    if (collector != null)
                        collector.stopCollector();
                } finally {
                    restoreSystemOut(outSave);
                }
                println("collector stopped, trace saved to: " + cmds.getOutput());

                cleanUp(context);
                println("VO exited");
                System.exit(0);
            }
        } else {
            println("No output tracefolder was entered\n");
            usageHelp();
            System.exit(1);
        }
    }

    /**
     * prints hotspot information in console, and gets user confirmation for hot
     * spot setup environment to proceed with MITM throttle.
     */
    private boolean isIOSAttenuationConfirmed() {
        println(MacHotspotUtil.getStatusMessage());
        println("Please enter Yes/No and hit enter to proceed.");
        String inputStr = readInput();
        return (inputStr != null && "yes".equals(inputStr.trim().toLowerCase()));
    }

    private StatusResult runCommand(Commands cmds, IDataCollector collector, String password, Hashtable<String, Object> extras) {
        StatusResult result;
        if (cmds.getDeviceid() != null) {
        	ArrayList<IAroDevice> aroDeviceList = aroController.getAroDevices().getDeviceList();
        	IAroDevice aroDevice = null;
			for (IAroDevice aDevice : aroDeviceList) {
				if (aDevice.getId().equals(cmds.getDeviceid())) {
					aroDevice = aDevice;		
					String voTimeZoneID = TimeZone.getDefault().getID();
					double voCurrentUTC = System.currentTimeMillis() / 1000.0;
					double deviceCurrentUTC = 0;
					boolean timingOffset = false;
					deviceCurrentUTC = aroDevice.obtainDeviceTimestamp();
					double timeDiff = voCurrentUTC - deviceCurrentUTC;
					timingOffset = (voTimeZoneID.equals(aroDevice.getDeviceTimeZoneID()) && Math.abs(timeDiff) <= 2);
					aroDevice.setTimingOffset(timingOffset);
					aroDevice.setVoTimeZoneID(voTimeZoneID);
					aroDevice.setVoTimestamp(voCurrentUTC);
					break;
				}
			}
            result = collector.startCollector(true, cmds.getOutput(), getVideoOption(), false, aroDevice, extras, password);
        } else {
            result = collector.startCollector(true, cmds.getOutput(), getVideoOption(), false, null, extras, password);
        }
        return result;
    }

    /**
     * if the user set throttle number, VO CLI will enable the throttle option
     * 
     * @return
     */
    private AttenuatorModel getAttenuateModel(Commands cmds) {
        AttenuatorModel model = new AttenuatorModel();
        model.setConstantThrottle(true);
        model.setThrottleUL(getThrottleUL());
        model.setThrottleDL(getThrottleDL());
        if (model.getThrottleDL() > -1) {
            model.setThrottleDLEnable(true);
        }
        if (model.getThrottleUL() > -1) {
            model.setThrottleULEnable(true);
        }
        if (cmds.getAttenuationprofile() != null) {
            if ((model.isThrottleDLEnabled()) || (model.isThrottleULEnabled())) {
                printError(ErrorCodeRegistry.getInvalidProfileThrottleInput());
                System.exit(1);
            } else {
                String localPath = cmds.getAttenuationprofile();
                model.setLoadProfile(true);
                model.setLocalPath(localPath);
                model.setAtnrProfileName(localPath);
            }
        }

        return model;
    }

    /**
     * Provides for user input
     * 
     * @return user input
     */
    String readInput() {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        try {
            return bufferRead.readLine();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * print string to console with new line char
     * 
     * @param str
     *            - output text
     */
    void out(String str) {
        utilOut.conditionalOutMessage(str);
    }

    void outln(String str) {
        utilOut.conditionalOutMessageln(str);
    }

    void err(String str) {
        utilOut.errMessage(str);
    }

    void errln(String str) {
        utilOut.errMessageln(str);
    }

    void println(String str) {
        utilOut.outMessageln(str);
    }

    void print(String str) {
        utilOut.outMessage(str);
    }

    /**
     * Displays user help
     */
    private void usageHelp() {
        StringBuilder sbuilder = new StringBuilder(1000);
        sbuilder.append("Version:").append(buildBundle.getString("build.majorversion")).append(".").append(buildBundle.getString("build.timestamp"))

                .append("\nUsage: vo [commands] [arguments]").append("\n  --analyze [trace location]: analyze a trace folder or file.")
                .append("\n  --startcollector [rooted_android|vpn_android|ios]: run a collector.")
                .append("\n  --ask [auto|rooted_android|vpn_android|ios]: asks for a device then runs the collector.")
                .append("\n  --output [fullpath including filename] : output to a file or trace folder")
                .append("\n  --overwrite [yes/no] : overwrite a trace folder - optional - will default to no if not specified")
                .append("\n  --deviceid [device id]: device id of Android(optional) and  udid/Device identifier for IOS(required).")
                .append("\n    If not declared first device found is used.")
                .append("\n  --format [json|html]: optional type of report to generate. Default: json.")
                .append((!Util.isMacOS()) ? "\n  --video [hd|sd|slow|no]: optional command to record video when running collector. Default: no."
                        : "\n  --video [yes|no]: optional command to record video when running collector. Default: no.")
                .append("\n  --videoOrientation [portrait|landscape]: optional command to set the video orientation for non-rooted (vpn_android) collector. Default: portrait.")
                .append("\n  --throttleUL [number in kbps/mbps]: optional command for throttle uplink throughput, range from 64k - 100m (102400k).")
                .append("\n  --throttleDL [number in kbps/mbps]: optional command for throttle downlink throughput, range from 64k - 100m (102400k).")
                .append("\n  --profile [file_path]: optional command that provides a file with attenuation sequence")
                .append("\n  --listcollectors: optional command to list available data collector.")
                .append("\n  --verbose:  optional command to enable detailed messages for '--analyze' and '--startcollector'")
                .append("\n  --help,-h,-?: show help menu.").append("\n\nUsage examples: ").append("\n=============")
                
                .append("\nRun Android collector to capture trace with video:").append("\n    slow video is 1-2 frames per second: ")
                .append("\n  --startcollector rooted_android --output /User/documents/test --video slow")

                .append("\n  --startcollector vpn_android --output /User/documents/test --video slow")

                .append("\nRun Non-rooted Android collector to capture trace with video and uplink/downlink attenuation applied:")
                .append("\n    throttle uplink throughput can accept 64k - 100m (102400k)")
                .append("\n    throttle downlink throughput can accept 64k - 100m (102400k)")
                .append("\n  --startcollector vpn_android --output /User/documents/test --video slow --throttleUL 2m --throttleDL 64k")

                .append("\nRun Non-rooted Android collector to capture trace with video and uplink/downlink attenuation using profile:")
                .append("\n  --startcollector vpn_android --output /User/documents/test --video slow --videoOrientation landscape --profile /Users/{user}/config/attn_profile.txt")

                .append("\nRun iOS collector to capture trace with video: ").append("\n    trace will be overwritten if it exists: ")
                .append("\n  --startcollector ios --deviceid udid/deviceIdentifier --overwrite yes --output /Users/{user}/tracefolder --video hd --sudo password")

                .append("\nRun iOS collector to capture trace with video: ")
                .append("\n  --startcollector ios --deviceid udid/deviceIdentifier --output /user/documents/(trace name) --video slow --sudo password")

                .append("\nRun iOS collector to capture trace with video and uplink/downlink attenuation applied: ")
                .append("\n  --startcollector ios --deviceid udid/deviceIdentifier --output /user/documents/(trace name) --video slow --throttleUL 2m --throttleDL 64k --sudo password")

                .append("\nAsk user for device and Run Android collector to capture trace with video: ")
                .append("\n  --ask rooted_android --output /User/documents/test --video sd")

                .append("\nAsk for device and Run iOS collector to capture trace with video: ")
                .append("\n  --ask ios --output /Users/{user}/tracefolder --video slow --sudo password")

                .append("\nAsk for device and Run appropriate collector for device to capture trace with video: ")
                .append("\n    Note: --sudo is not required or ignored for Android")
                .append("\n  --ask auto --output /Users/{user}/tracefolder --video slow --sudo password")

                .append("\nAnalyze trace and produce HTML report")
                .append("\n  --analyze /User/documents/test --output /User/documents/report.html --format html")

                .append("\nAnalyze trace and produce JSON report:")
                .append("\n  --analyze /User/documents/test/traffic.cap --output /User/documents/report.json");
        println(sbuilder.toString());
    }

    private void cleanUp(ApplicationContext context) {
        String dir = "";
        File filepath = new File(UtilOut.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        dir = filepath.getParent();
        IFileManager filemanager = context.getBean(IFileManager.class);
        filemanager.deleteFile(dir + System.getProperty("file.separator") + "AROCollector.apk");
        filemanager.deleteFile(dir + System.getProperty("file.separator") + "ARODataCollector.apk");
    }

    // ------------------------------------------------------------------------------------------------------------------
    // IAROView
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public void updateTracePath(File path, TimeRange... timeRange) {
        LOGGER.info(path);
    }

    @Override
    public void updateProfile(Profile profile) {
        LOGGER.info("updateProfile:" + profile);
    }

    @Override
    public void updateReportPath(File path) {
        LOGGER.info("updateReportPath:" + path);

    }

    @Override
    public void updateFilter(AnalysisFilter filter) {
        LOGGER.info("updateFilter:" + filter);
    }

    @Override
    public String getTracePath() {
        return null;
    }

    @Override
    public String getReportPath() {
        return null;
    }

    @Override
    public void addAROPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void addAROActionListener(ActionListener listener) {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void startCollector(IAroDevice device, String tracePath, Hashtable<String, Object> extraParams, MetaDataModel metaDataModel) {
    }

    @Override
    public void startCollectorIos(IDataCollector iOsCollector, String udid, String tracePath, VideoOption videoOption) {
    }

    @Override
    public void stopCollector() {
    }

    @Override
    public void cancelCollector() {
    }

    @Override
    public void haltCollector() {
    }

    @Override
    public IDevice[] getConnectedDevices() {
        return null;
    }

    @Override
    public IAroDevices getAroDevices() {
        return null;
    }

    @Override
    public List<IDataCollector> getAvailableCollectors() {
        return collectorList;
    }

    @Override
    public void updateCollectorStatus(CollectorStatus status, StatusResult result) {
        LOGGER.info("updateCollectorStatus:" + status + ", result:" + result.isSuccess());
    }

    @Override
    public CollectorStatus getCollectorStatus() {
        return null;
    }

    @Override
    public void liveVideoDisplay(IDataCollector collector) {
    }

    @Override
    public void hideChartItems(String... chartPlotOptionEnumNames) {
    }

    @Override
    public void showChartItems(String... chartPlotOptionEnumNames) {
    }

	@Override
	public AROController getController() {
		return null;
	}

    public VideoOption getVideoOption() {
        return videoOption;
    }

    @Override
    public void setDeviceDataPulled(boolean status) {
    }

	@Override
	public MetaDataModel getMetaDataModel() {
		return metaDataModel;
	}

	@Override
	public void refreshBestPracticesTab() {
		// Ignore, commandline does not do this
	}

	@Override
	public void clearPreviousTraceData() {
		setTrafficFile(null); // forget previous trace and video selections
		setVideoFile(null);
	}

	@Override
	public void setTrafficFile(String trafficFile) {
		this.trafficFile = trafficFile;
	}
	
	@Override
	public String getTrafficFile() {
		return this.trafficFile != null ? this.trafficFile : TraceDataConst.FileName.PCAP_FILE;
	}

	@Override
	public void setVideoFile(String videoFile) {
		this.videoFile = videoFile;
	}

	@Override
	public String getVideoFile() {
		return videoFile;
	}
	
}