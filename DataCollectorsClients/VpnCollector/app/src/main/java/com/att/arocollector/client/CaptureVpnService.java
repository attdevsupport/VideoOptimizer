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

package com.att.arocollector.client;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.att.arocollector.Config;
import com.att.arocollector.R;
import com.att.arocollector.attenuator.AttenuatorDLBroadcastReceiver;
import com.att.arocollector.attenuator.AttenuatorManager;
import com.att.arocollector.attenuator.AttenuatorULBroadcastReceiver;
import com.att.arocollector.attenuator.AttenuatorUtil;
import com.att.arocollector.attenuator.ThrottleDLBroadcastReceiver;
import com.att.arocollector.attenuator.ThrottleULBroadcastReceiver;
import com.att.arocollector.packetRebuild.PCapFileWriter;
import com.att.arocollector.privatedata.AROPrivateDataCollectorService;
import com.att.arocollector.utils.BundleKeyUtil;
import com.att.arotcpcollector.ClientPacketWriterImpl;
import com.att.arotcpcollector.IClientPacketWriter;
import com.att.arotcpcollector.SessionHandler;
import com.att.arotcpcollector.ip.IPHeader;
import com.att.arotcpcollector.ip.IPPacketFactory;
import com.att.arotcpcollector.ip.IPv4Header;
import com.att.arotcpcollector.socket.IProtectSocket;
import com.att.arotcpcollector.socket.SocketData;
import com.att.arotcpcollector.socket.SocketDataPublisher;
import com.att.arotcpcollector.socket.SocketNIODataService;
import com.att.arotcpcollector.socket.SocketProtector;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.tcp.TCPPacketFactory;
import com.att.arotcpcollector.udp.UDPHeader;
import com.att.arotcpcollector.udp.UDPPacketFactory;
import com.att.arotracedata.AROCameraMonitorService;
import com.att.arotracedata.AROCollectorService;
import com.att.arotracedata.AROCpuTempService;
import com.att.arotracedata.AROCpuTraceService;
import com.att.arotracedata.AROGpsMonitorService;
import com.att.arotracedata.ARORadioMonitorService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CaptureVpnService extends VpnService implements Handler.Callback, Runnable, IProtectSocket {
	private static final String TAG = "CaptureVpnService";

	public static final String SERVICE_CLOSE_CMD_INTENT = "arovpndatacollector.service.close";

	public static final String SERVICE_NAME = "com.collector.client.VpnService";

	public static final int MAX_PACKET_SIZE = 65535;

	private Handler mHandler;
	private Thread mThread;

	private ParcelFileDescriptor mInterface;

	private boolean serviceValid;

	private PendingIntent mConfigureIntent;

	private Intent theIntent;

	private SocketNIODataService dataService;

	private VPNInterfaceWriter writerService;
	private Thread writerServiceThread;

	private Thread dataServiceThread;

	private SocketDataPublisher packetBackGroundWriter;

	private Thread packetQueueThread;

	private File traceDir; //Trace files

	private File pcapFile; //pcap file
	private PCapFileWriter pcapWriter;

	private File securePCAPFile; //pcap file
	private PCapFileWriter securePCAPWriter;


	private File timeFile;//duration time File
	private FileOutputStream timeStream;

	private File appNameFile; //appname file

	private Intent aROCameraMonitorService;
	private ComponentName cameraMonitorService;
	private ComponentName cpuTraceService;
	private Intent aRORadioMonitorService;
	private ComponentName radioMonitorService;
	private SocketData dataTransmitter = null;
	BufferedWriter mAppNameWriter = null;

	private AttenuatorDLBroadcastReceiver attenuatorDLbroadcast = new AttenuatorDLBroadcastReceiver();
	private AttenuatorULBroadcastReceiver attenuatorULbroadcast = new AttenuatorULBroadcastReceiver();

	private ThrottleDLBroadcastReceiver throttleDLBroadcast = new ThrottleDLBroadcastReceiver();
	private ThrottleULBroadcastReceiver throttleULBroadcast = new ThrottleULBroadcastReceiver();
	// Sets an ID for the notification, so it can be updated
	private int notifyID = 1;
	private NotificationCompat.Builder mBuilder;
	public static final String CHANNEL_ID = "VPN Collector VPN Notification";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(TAG, "onStartCommand");

		theIntent = intent;

		dataTransmitter = SocketData.getInstance();

		registerReceiver(serviceCloseCmdReceiver, new IntentFilter(CaptureVpnService.SERVICE_CLOSE_CMD_INTENT));
		//Those broadcast receiver depend on VPN exist
		registerReceiver(attenuatorDLbroadcast, new IntentFilter(BundleKeyUtil.DELAY_DL_BROADCAST));
		registerReceiver(attenuatorULbroadcast, new IntentFilter(BundleKeyUtil.DELAY_UL_BROADCAST));
		registerReceiver(throttleDLBroadcast, new IntentFilter(BundleKeyUtil.THROTTLE_DL_BROADCAST));
		registerReceiver(throttleULBroadcast, new IntentFilter(BundleKeyUtil.THROTTLE_UL_BROADCAST));

		loadExtras(theIntent);

		try {
			initTraceFiles();
		} catch (IOException e1) {
			e1.printStackTrace();
			stopSelf();
			return Service.START_STICKY_COMPATIBILITY;
		}

		// The handler is only used to show messages.
		if (mHandler == null) {
			mHandler = new Handler(this);
		}

		// Stop the previous session by interrupting the thread.
		if (mThread != null) {
			mThread.interrupt();
			int reps = 0;
			while (mThread.isAlive()) {
				Log.i(TAG, "Waiting to exit " + ++reps);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
			mThread = null;
		}

		// Start a new session by creating a new thread.
		mThread = new Thread(this, "CaptureVpnThread");
		mThread.start();
		return START_STICKY;
	}

	/**
	 * initiate trace services
	 */
	private void startServices() {
		//	launchAROCollectorService();
		//	launchAROGpsMonitorService();
		launchAROCameraMonitorService();
		launchARORadioMonitorService();
	}

	private void stopServices() {
		//	stopAROCollectorService();
		//	stopAROGpsMonitorService();
		stopAROCameraMonitorService();
		stopARORadioMonitorService();
		stopAROCpuTraceService();
	}

	/**
	 * launch AROCameraMonitorService for the collection of META data
	 */
	private void launchAROCameraMonitorService() {
		Log.i(TAG, "launchAROCameraMonitorService()");
		aROCameraMonitorService = new Intent(getApplicationContext(), AROCameraMonitorService.class);
		aROCameraMonitorService.putExtra("TRACE_DIR", "/sdcard/ARO/");
		aROCameraMonitorService.putExtra("TRACE_FILE_NAME", "camera_events");
		cameraMonitorService = startService(aROCameraMonitorService);
	}

	/**
	 * launch ARORadioMonitorService for the collection of META data
	 */
	private void launchARORadioMonitorService() {
		Log.i(TAG, "launchARORadioMonitorService()");
		aRORadioMonitorService = new Intent(getApplicationContext(), ARORadioMonitorService.class);
		aRORadioMonitorService.putExtra("TRACE_DIR", "/sdcard/ARO/");
		aRORadioMonitorService.putExtra("TRACE_FILE_NAME", "radio_events");
		radioMonitorService = startService(aRORadioMonitorService);
	}

	/**
	 * stop AROCpuTraceService
	 */
	private void stopAROCpuTraceService() {
		Log.i(TAG, "stopping AROCpuTraceService...");
		stopService(new Intent(this, AROCpuTraceService.class));
	}

	/**
	 * stop AROCpuTempService
	 */
	private void stopAROCpuTempService() {
		Log.i(TAG, "stopping AROCpuTempService...");
		stopService(new Intent(this, AROCpuTempService.class));
	}

	/**
	 * stop AROCameraMonitorService
	 */
	private void stopAROCameraMonitorService() {
		Log.i(TAG, "stopping AROCameraMonitorService...");
		stopService(aROCameraMonitorService);
	}

	/**
	 * stop ARORadioMonitorService
	 */
	private void stopARORadioMonitorService() {
		Log.i(TAG, "stopping ARORadioMonitorService...");
		stopService(aRORadioMonitorService);
	}


	private void loadExtras(Intent intent) {
		Log.i(TAG, "loadExtras");
		String traceDirStr = intent.getStringExtra("TRACE_DIR");
		traceDir = new File(traceDirStr);
	}

	private void unregisterAnalyzerCloseCmdReceiver() {
		Log.d(TAG, "inside unregisterAnalyzerCloseCmdReceiver()");
		try {
			if (serviceCloseCmdReceiver != null) {
				unregisterReceiver(serviceCloseCmdReceiver);
				serviceCloseCmdReceiver = null;

				Log.d(TAG, "successfully unregistered serviceCloseCmdReceiver");
			}
		} catch (Exception e) {
			Log.d(TAG, "Ignoring exception in serviceCloseCmdReceiver", e);
		}
	}

	@Override
	public void onRevoke() {
		super.onRevoke();
		Log.i(TAG, "revoked!, user has turned off VPN");
	}

	/**
	 * close all file descriptors for CaptureVpnService
	 */
	private void closeFileDescriptors() {
		try {
			if (mInterface != null) {
				mInterface.close();
			}
		} catch (Exception e) {
			Log.i(TAG, "closeFileDescriptor: ParcelFileDescriptor :: " + e.getMessage());
		}

		// add new file descriptor here to close
	}

	/**
	 * receive message to trigger termination of collection
	 */
	private BroadcastReceiver serviceCloseCmdReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "received service close cmd intent at " + System.currentTimeMillis());
			processApplicationNameVersion();
			serviceValid = false;
			unregisterAnalyzerCloseCmdReceiver();
			unregisterReceiver(attenuatorDLbroadcast);
			unregisterReceiver(attenuatorULbroadcast);
			unregisterReceiver(throttleDLBroadcast);
			unregisterReceiver(throttleULBroadcast);
			NotificationManager mNotificationManager =
					(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(notifyID);
			closeFileDescriptors();
			stopTraceServices();
			stopSelf();
		}
	};

	private void instanciateAppNameFile() throws IOException {

		if (!traceDir.exists()) {
			traceDir.mkdirs();
		}
		// gen & open appname file
		appNameFile = new File(traceDir, Config.TRACEFILE_APPNAME);
		try {
			appNameFile.createNewFile();
			mAppNameWriter = new BufferedWriter(new FileWriter(appNameFile));
		} catch (IOException e) {
			Log.i(TAG, "instanciateAppNameFile() Exception:" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void closeAppNameFile() {
		try {
			mAppNameWriter.flush();
			mAppNameWriter.close();
		} catch (IOException e) {
			Log.i(TAG, "closeAppNameFile() Exception:" + e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean isVPNEnabled() {
		boolean isVPNEnabled = false;
		List<String> networkList = new ArrayList<>();
		try {
			for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (networkInterface.isUp())
					networkList.add(networkInterface.getName());
			}
			isVPNEnabled = networkList.contains("tun0");
		} catch (Exception ex) {
			Log.i(TAG, "VPN Network check");
		}

		return isVPNEnabled;
	}

	public void processApplicationNameVersion() {
		Log.i(TAG, "processApplicationNameVersion");
		List<String> packageNameList = null;

		// Get the list of package names from cpu file
		packageNameList = parseCpuFile();
		PackageManager packageManager = getApplicationContext().getPackageManager();
		for (String packageName : packageNameList) {
			try {
				ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
				// " " using space as a delimiter for appname & version number separation
				String sAppNameVersion = (String) ((ai != null) ? ((String) (packageManager.getApplicationLabel(ai))).replaceAll("\\s+", "") + " "
						+ packageManager.getPackageInfo(packageName, 0).versionName : packageName);
				mAppNameWriter.write(sAppNameVersion + System.getProperty("line.separator"));
				mAppNameWriter.flush();
			} catch (NameNotFoundException | IOException e1) {
				Log.e(TAG, "NameNotFoundException or IOException in processApplicationNameVersion");
				e1.printStackTrace();
			}
		}
		closeAppNameFile();
	}

	List<String> parseCpuFile() {

		String line = "";
		BufferedReader input = null;
		List<String> packageNameList = new ArrayList<String>();

		try {
			input = new BufferedReader(new FileReader(Config.TRACEFILE_DIR + Config.TRACEFILE_CPU));
			while (input != null && (line = input.readLine()) != null) {
				String[] temp = line.split(" ");
				if (temp != null && temp.length > 2) {
					for (int i = 2; i < temp.length; i++) {
						if ((!temp[i].startsWith("/")) && temp[i].contains(".")
								&& (!packageNameList.contains(temp[i].split("=")[0]))) {
							packageNameList.add(temp[i].split("=")[0]);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException in parseCpuFile");
				e.printStackTrace();
			}
		}
		return packageNameList;
	}

	@Override
	public ComponentName startService(Intent service) {
		Log.i(TAG, "startService(...)");
		return super.startService(service);
	}

	protected void stopTraceServices() {
		Log.i(TAG, "stopping Trace Services...");
		AttenuatorManager.getInstance().terminateDelayLog();
		AttenuatorManager.getInstance().terminateThroughputLog();
		stopAROCollectorService();
		stopAROGpsMonitorService();
		stopAROCameraMonitorService();
		stopAROPrivateDataCollectorService();
		stopAROCpuTempService();
		stopAROCpuTraceService();
	}

	/**
	 * launch AROCollectorService for the collection of META data
	 */
	private void stopAROCollectorService() {
		Log.i(TAG, "stopping AROCollectorService...");
		Intent intent = new Intent(getApplicationContext(), AROCollectorService.class);
		intent.addCategory(AROCollectorService.ARO_COLLECTOR_SERVICE);
		getApplication().stopService(intent);
	}

	/**
	 * stop AROPrivateDataCollectorService for the collection of META data
	 */
	private void stopAROPrivateDataCollectorService() {
		Log.i(TAG, "stopping AROPrivateDataCollectorService...");
		Intent intent = new Intent(getApplicationContext(), AROPrivateDataCollectorService.class);
		intent.addCategory(AROPrivateDataCollectorService.ARO_PRIVATE_DATA_COLLECTOR_SERVICE);
		getApplication().stopService(intent);
	}

	/**
	 * launch AROCollectorService for the collection of META data
	 */
	private void stopAROGpsMonitorService() {
		Log.i(TAG, "stopping AROGpsMonitorService...");
		Intent intent = new Intent(getApplicationContext(), AROGpsMonitorService.class);
		intent.addCategory(AROGpsMonitorService.ARO_GPS_MONITOR_SERVICE);
		getApplication().stopService(intent);
	}

	@Override
	public boolean stopService(Intent name) {
//		Log.i(TAG, "stopService(...)");

		serviceValid = false;
		//	closeTraceFiles();
		return super.stopService(name);
	}

	@Override
	public void protectSocket(int socket) {
		this.protect(socket);
	}

	@Override
	public void protectSocket(Socket socket) {
		this.protect(socket);
	}

	@Override
	public void protectSocket(DatagramSocket socket) {
		this.protect(socket);
	}

	/**
	 * called back from background thread when new packet arrived
	 */
	/*@Override
	public void receive(byte[] packet) {
		if (pcapWriter != null) {
			try {
				pcapWriter.addPacket(packet, 0, packet.length, System.currentTimeMillis() * 1000000);
			} catch (IOException e) {
				Log.e(TAG, "pcapOutput.addPacket IOException :" + e.getMessage());
				e.printStackTrace();
			}
		}else{
			Log.e(TAG, "overrun from capture: length:"+packet.length);
		}

	}*/

	/**
	 * Close the packet trace file
	 */
	private void closePcapTrace() {
		Log.i(TAG, "closePcapTrace()");
		if (pcapWriter != null) {
			pcapWriter.close();
			pcapWriter = null;
			Log.i(TAG, "closePcapTrace() closed");
		}
	}

	/**
	 * onDestroy is invoked when user disconnects the VPN
	 */
	@Override
	public void onDestroy() {

		Log.i(TAG, "onDestroy()");
		serviceValid = false;

		unregisterAnalyzerCloseCmdReceiver();

		dataService.setShutdown(true);
		writerService.setShutdown(true);
		packetBackGroundWriter.setIsShuttingDown(true);

		//	closeTraceFiles();

		if (dataServiceThread != null) {
			dataServiceThread.interrupt();
		}
		if (packetQueueThread != null) {
			packetQueueThread.interrupt();
		}

		if (writerServiceThread != null) {
			writerServiceThread.interrupt();
		}

		closeFileDescriptors();

		// Stop the previous session by interrupting the thread.
		if (mThread != null) {
			mThread.interrupt();
			int reps = 0;
			while (mThread.isAlive()) {
				Log.i(TAG, "Waiting to exit " + ++reps);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (reps > 5) {
					break;
				}
			}
			mThread = null;
		}

	}

	/**
	 * <pre>
	 * run method<br>
	 *     Contains launch of VPN
	 *        startVpnService()
	 *        startCapture()
	 * </pre>
	 */
	@Override
	public void run() {
		Log.i(TAG, "running vpnService");
		boolean success = false;
		SocketProtector protector = SocketProtector.getInstance();
		protector.setProtector(this);

		try {
			success = startVpnService();
		} catch (IOException e) {
			Log.e(TAG, "startVpnService() failed: " + e.getMessage(), e);
		}

		if (success) {
			try {
				startNotification();
				startCapture();
				Log.i(TAG, "Capture completed");
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		} else {
			Log.e(TAG, "Failed to start VPN Service!");
		}

		Log.i(TAG, "Closing Capture files");

		closeTraceFiles();
	}

	// Trace files


	/**
	 * create, open, initialize trace files
	 */
	private void initTraceFiles() throws IOException {
		Log.i(TAG, "initTraceFiles()");
		instanciatePcapFile();
		instanciateTimeFile();
		instanciateAppNameFile();

		startServices();
	}

	/**
	 * close the trace files
	 */
	private void closeTraceFiles() {
		Log.i(TAG, "closeTraceFiles()");
		closePcapTrace();
		closeTimeFile();
		stopServices();
	}

	/**
	 * Create and leave open, the pcap file
	 *
	 * @throws IOException
	 */
	private void instanciatePcapFile() throws IOException {

		if (!traceDir.exists()) {
			traceDir.mkdirs();
		}

		// gen & open pcap file
		String sFileName = "traffic.cap";
		pcapFile = new File(traceDir, sFileName);
		pcapWriter = new PCapFileWriter(pcapFile);

		if (theIntent.getBooleanExtra("secure", false)) {
			sFileName = "secure_traffic.cap";
			securePCAPFile = new File(traceDir, sFileName);
			securePCAPWriter = new PCapFileWriter(securePCAPFile);
		}
	}

	/**
	 * Create and leave open, the time file
	 * time file format
	 * line 1: header
	 * line 2: pcap start time
	 * line 3: eventtime or uptime (doesn't appear to be used)
	 * line 4: pcap stop time
	 * line 5: time zone offset
	 */
	private void instanciateTimeFile() throws IOException {

		if (!traceDir.exists()) {
			traceDir.mkdirs();
		}

		// gen & open pcap file
		String sFileName = "time";
		timeFile = new File(traceDir, sFileName);
		timeStream = new FileOutputStream(timeFile);

		String str = String.format("%s\n%.3f\n%d\n"
				, "Synchronized timestamps"
				, ((double) System.currentTimeMillis()) / 1000.0
				, SystemClock.uptimeMillis()
		);

		try {
			timeStream.write(str.getBytes());
		} catch (IOException e) {
			Log.e(TAG, "IOException in instanciateTimeFile");
			e.printStackTrace();
		}
	}

	/**
	 * update and close the time file
	 */
	private void closeTimeFile() {

		Log.i(TAG, "closeTimeFile()");
		if (timeStream != null) {
			String str = String.format("%.3f\n", ((double) System.currentTimeMillis()) / 1000.0);
			try {
				timeStream.write(str.getBytes());
				timeStream.flush();
				timeStream.close();
				Log.i(TAG, "...closed");
			} catch (IOException e) {
				Log.e(TAG, "IOException:" + e.getMessage());
			}
		}
	}


	/**
	 * setup VPN interface.
	 *
	 * @return
	 * @throws IOException
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	boolean startVpnService() throws IOException {
		// If the old interface has exactly the same parameters, use it!
		if (mInterface != null) {
			Log.i(TAG, "Using the previous interface");
			return false;
		}

		Log.i(TAG, "startVpnServide=> create builder");
		// Configure a builder while parsing the parameters.
		Builder builder = new Builder()
				.addAddress("fd12:3456:789a:1::1", 128)
				.addAddress("10.120.0.1", 32)
				.addRoute("0.0.0.0", 0)
				.addRoute("0:0:0:0:0:0:0:0", 0)
				.setSession(getString(R.string.app_name))
				.setConfigureIntent(mConfigureIntent)
				.addDnsServer("8.8.8.8")
				.addDnsServer("8.8.4.4")
				.setMtu(MAX_PACKET_SIZE);

		if(!TextUtils.isEmpty(theIntent.getStringExtra(BundleKeyUtil.SELECTED_APP_NAME))) {
			try {
				if(!theIntent.getStringExtra(BundleKeyUtil.SELECTED_APP_NAME).equals("EMPTY")) {
					builder.addAllowedApplication(theIntent.getStringExtra(BundleKeyUtil.SELECTED_APP_NAME));
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}

		if (mInterface != null) {
			try {
				mInterface.close();
			} catch (Exception e) {
				Log.e(TAG, "Exception when closing mInterface:" + e.getMessage());
			}
		}
		Log.i(TAG, "startVpnServide=> builder.establish()");
		mInterface = builder.establish();

		if (mInterface != null) {
			Log.i(TAG, "\n\\\n  VPN Established:interface = " + mInterface.getFd() + "\n/\n");
			return true;
		} else {
			Log.d(TAG, "mInterface is null");
			return false;
		}
	}

	/**
	 * Start background thread to handle client's socket, handle incoming and outgoing packet from VPN interface
	 *
	 * @throws IOException
	 */
	void startCapture() throws IOException {

		Log.i(TAG, "startCapture() :capture starting");

		// Packets to be sent are queued in this input stream.
		FileInputStream vpnInterfaceReader = new FileInputStream(mInterface.getFileDescriptor());

		// Packets received need to be written to this output stream.
		FileOutputStream vpnInterfaceWriter = new FileOutputStream(mInterface.getFileDescriptor());

		// Allocate the buffer for a single packet.
		// TODO: Packet Size chosen from the ToyVPNService example. May need tweaking.
		ByteBuffer packetData = ByteBuffer.allocate(MAX_PACKET_SIZE);

		IClientPacketWriter clientPacketWriter = new ClientPacketWriterImpl(vpnInterfaceWriter);

		SessionHandler sessionHandler = SessionHandler.getInstance();
		sessionHandler.setAndroidContext(this);
		sessionHandler.setSecureEnable(theIntent.getBooleanExtra("secure", false));
		sessionHandler.setPrintLog(theIntent.getBooleanExtra(BundleKeyUtil.PRINT_LOG, false));
		Log.d(TAG, "Initial delayTimeDL: " + AttenuatorManager.getInstance().getDelayDl());

		//background task for non-blocking socket
		dataService = new SocketNIODataService();

		dataService.setSecureEnable(theIntent.getBooleanExtra("secure", false));
		dataService.setPrintLog(theIntent.getBooleanExtra(BundleKeyUtil.PRINT_LOG, false));
		Log.d(TAG, "Initial delayTimeUL:" + AttenuatorManager.getInstance().getDelayUl());
		dataServiceThread = new Thread(dataService, "dataServiceThread");
		dataServiceThread.start();

		//background task for writing packet data to pcap file
		packetBackGroundWriter = new SocketDataPublisher();
		packetBackGroundWriter.setPcapWriter(pcapWriter);
		packetBackGroundWriter.setSecurePCAPWriter(securePCAPWriter);
		packetQueueThread = new Thread(packetBackGroundWriter, "packetQueueThread");
		packetQueueThread.start();

		writerService = VPNInterfaceWriter.getInstance();
		writerService.setClientWriter(clientPacketWriter);
		writerServiceThread = new Thread(writerService, "VPNWriter");//writer for pcap file
		writerServiceThread.start();

		int length;

		serviceValid = true;

		long maxBucketSize = AttenuatorManager.getInstance().getThrottleUL() * 1000 / 8;
		long lastPacketTime = System.nanoTime();
		double currentNumberOfTokens = maxBucketSize;

		Log.d(TAG, "Upload Speed Limit : " + (AttenuatorManager.getInstance().getThrottleUL() * 1000 / 8) + " Bytes");


		//FIXME ADDING UDP TESTING HERE ClientEcho.runUDPClient(this.getApplicationContext())

		while (serviceValid) {

			// Read a packet from the VPN Interface.
			length = vpnInterfaceReader.read(packetData.array());
			packetData.position(length);

			if (length > 0) {
				Log.d(TAG, "Received packet from vpn client: " + length);

				byte[] clientPacketData = Arrays.copyOf(packetData.array(), length);

				// Sleep until we have some value for throttling other than zero
				while (AttenuatorManager.getInstance().getThrottleUL() == 0)  {
					// Sleep for a short duration as no packet can be processed for a zero throttle value
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Log.d(TAG, "Failed to sleep when Upload throttle was 0 : " + e.getMessage());
					}
				}

				dataTransmitter.sendDataToBeTransmitted(clientPacketData);

				int throttleUL = AttenuatorManager.getInstance().getThrottleUL();
				if (throttleUL > 0) {
					maxBucketSize = throttleUL * 1000 / 8;
					int headerLength = 0;
					UDPHeader udpHeader = null;
					TCPHeader tcpHeader = null;
					TCPPacketFactory tcpFactory = new TCPPacketFactory();
					UDPPacketFactory udpFactory = new UDPPacketFactory();

					try {
						IPHeader ipHeader = IPPacketFactory.createIPHeader(packetData.array(), 0);
						headerLength += ipHeader.getIPHeaderLength();
						if (ipHeader.getProtocol() == 6) {
							tcpHeader = tcpFactory.createTCPHeader(packetData.array(), ipHeader.getIPHeaderLength());
							headerLength += tcpHeader.getTCPHeaderLength();
						} else {
							udpHeader = udpFactory.createUDPHeader(packetData.array(), ipHeader.getIPHeaderLength());
							headerLength += udpHeader.getLength();
						}
					} catch (PacketHeaderException ex) {
						Log.e(TAG, ex.getMessage(), ex);
					}

					int consumedTokens = clientPacketData.length - headerLength;
					long currentTime = System.nanoTime();
					double generatedToken = (currentTime - lastPacketTime) * throttleUL / 8 / 1000000;
					currentNumberOfTokens += generatedToken;
					if (currentNumberOfTokens > maxBucketSize) {
						currentNumberOfTokens = maxBucketSize;
					}
					lastPacketTime = currentTime;
					currentNumberOfTokens -= consumedTokens;
					if (currentNumberOfTokens < 0) {
						try {
							int sleepTime = (int) (-1 * currentNumberOfTokens * 8 / throttleUL);
							if (sleepTime > 0) {
								Thread.sleep(sleepTime);
							}
						} catch (InterruptedException e) {
							Log.d(TAG, "Failed to sleep: " + e.getMessage(), e);
						}
					}
				}
			} else {
				try {
					// If we are idle or waiting for the network
					// Sleep a little, to avoid busy looping.
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.d(TAG, "Failed to sleep: " + e.getMessage());
				}
			}
		}

		vpnInterfaceReader.close();
		vpnInterfaceWriter.close();

		Log.i(TAG, "capture finished: serviceValid = " + serviceValid);
	}

	@Override
	public boolean handleMessage(Message message) {
		if (message != null) {
			Log.d(TAG, "handleMessage:" + getString(message.what));
			Toast.makeText(this.getApplicationContext(), message.what, Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public void startNotification() {
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		createNotificationChannel(mNotificationManager);
		if (mBuilder == null) {
			mBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
					.setSmallIcon(R.drawable.icon)
					.setContentTitle("Video Optimizer VPN Collector")
					.setAutoCancel(false)
					.setOngoing(true)
					.setContentText(AttenuatorUtil.getInstance().notificationMessage());
		}

		mNotificationManager.notify(notifyID, mBuilder.build());
	}
	private void createNotificationChannel(NotificationManager mNotificationManager) {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel serviceChannel = new NotificationChannel(
					CHANNEL_ID,
					"VPN Collector",
					NotificationManager.IMPORTANCE_DEFAULT
			);
			mNotificationManager.createNotificationChannel(serviceChannel);
		}
	}

}