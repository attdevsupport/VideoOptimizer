/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.att.arocollector.Config;
import com.att.arocollector.packetRebuild.PCapFileWriter;
import com.att.arocollector.privatedata.AROPrivateDataCollectorService;
import com.att.arocollector.utils.BundleKeyUtil;
import com.att.arotcpcollector.ClientPacketWriterImpl;
import com.att.arotcpcollector.IClientPacketWriter;
import com.att.arotcpcollector.SessionHandler;
import com.att.arotcpcollector.socket.IProtectSocket;
import com.att.arotcpcollector.socket.IReceivePacket;
import com.att.arotcpcollector.socket.SocketDataPublisher;
import com.att.arotcpcollector.socket.SocketNIODataService;
import com.att.arotcpcollector.socket.SocketProtector;
import com.att.arotcpcollector.tcp.PacketHeaderException;
import com.att.arotracedata.AROCameraMonitorService;
import com.att.arotracedata.AROCollectorService;
import com.att.arotracedata.AROCpuTraceService;
import com.att.arotracedata.AROGpsMonitorService;
import com.att.arotracedata.ARORadioMonitorService;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class CaptureVpnService extends VpnService implements Handler.Callback, Runnable, IProtectSocket, IReceivePacket {
	private static final String TAG = "CaptureVpnService";
	
	public static final String SERVICE_CLOSE_CMD_INTENT = "arovpndatacollector.service.close";

	public static final String SERVICE_NAME = "com.collector.client.VpnService";

	private Handler mHandler;
	private Thread mThread;

	private ParcelFileDescriptor mInterface;

	private boolean serviceValid;

	private PendingIntent mConfigureIntent;

	private Intent theIntent;

	private SocketNIODataService dataService;

	private Thread dataServiceThread;

	private SocketDataPublisher packetBackGroundWriter;

	private Thread packetQueueThread;

	private File traceDir; //Trace files
	
	private File pcapFile; //pcap file
	private PCapFileWriter pcapOutput;
	private File timeFile;//duration time File
	private FileOutputStream timeStream;

	private File appNameFile; //appname file

	private Intent aROCameraMonitorService;
	private ComponentName cameraMonitorService;
	private ComponentName cpuTraceService;
	private Intent aRORadioMonitorService;
	private ComponentName radioMonitorService;
	BufferedWriter mAppNameWriter = null;
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d(TAG, "onStartCommand");
		
		theIntent = intent;
		
		registerReceiver(serviceCloseCmdReceiver, new IntentFilter(CaptureVpnService.SERVICE_CLOSE_CMD_INTENT));
		
		loadExtras(theIntent);
		
		try {
			initTraceFiles();
		} catch (IOException e1) {
			e1.printStackTrace();
			stopSelf();
			return 0;
		}

		// The handler is only used to show messages.
		if (mHandler == null) {
			mHandler = new Handler(this);
		}

		// Stop the previous session by interrupting the thread.
		if (mThread != null) {
			mThread.interrupt();
			int reps = 0;
			while(mThread.isAlive()){
				Log.i(TAG, "Waiting to exit " + ++reps);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
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

	private void stopServices(){
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
		Log.i(TAG, "stopAROCpuTraceService()");
		stopService(new Intent(this, AROCpuTraceService.class));
	}

	/**
	 * stop AROCameraMonitorService
	 */
	private void stopAROCameraMonitorService() {
		Log.i(TAG, "stopAROCameraMonitorService()");
		stopService(aROCameraMonitorService);
	}

	/**
	 * stop ARORadioMonitorService
	 */
	private void stopARORadioMonitorService() {
		Log.i(TAG, "stopARORadioMonitorService()");
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
		Log.i(TAG, "revoked!, user has turned off VPN");
		super.onRevoke();
	}
	/**
	 * receive message to trigger termination of collection
	 */
	private BroadcastReceiver serviceCloseCmdReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "received service close cmd intent at " + System.currentTimeMillis());
			unregisterAnalyzerCloseCmdReceiver();
			serviceValid = false;
			stopTraceServices();
			if(isVPNEnabled())
				processApplicationNameVersion();
			stopSelf();
			//stopService(theIntent);
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
	
	private boolean isVPNEnabled(){
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
				String sAppNameVersion = (String) ((ai != null) ? ((String)(packageManager.getApplicationLabel(ai))).replaceAll("\\s+","") + " "
						+ packageManager.getPackageInfo(packageName, 0).versionName : packageName);
				mAppNameWriter.write(sAppNameVersion + System.getProperty("line.separator"));
				mAppNameWriter.flush();
			} catch (NameNotFoundException | IOException e1) {
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return packageNameList;
	}

	@Override
	public ComponentName startService(Intent service) {
		Log.i(TAG, "startService(...)");
		// TODO Auto-generated method stub
		return super.startService(service);
	}
	
	protected void stopTraceServices() {
		Log.i(TAG, "stopping Trace Services...");
		stopAROCollectorService();
		stopAROGpsMonitorService();
		stopAROCameraMonitorService();
		stopAROPrivateDataCollectorService();
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

		stopService(new Intent(AROPrivateDataCollectorService.ARO_PRIVATE_DATA_COLLECTOR_SERVICE));
	}
	
	/**
	 * launch AROCollectorService for the collection of META data
	 */
	private void stopAROGpsMonitorService() {
		Log.i(TAG, "stopping AROGpsMonitorService...");
		getApplication().stopService(new Intent(AROGpsMonitorService.ARO_GPS_MONITOR_SERVICE));
	}


	@Override
	public boolean stopService(Intent name) {
		Log.i(TAG, "stopService(...)");
	
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
	@Override
	public void receive(byte[] packet) {
		if (pcapOutput != null) {
			try {
				pcapOutput.addPacket(packet, 0, packet.length, System.currentTimeMillis() * 1000000);
			} catch (IOException e) {
				Log.e(TAG, "pcapOutput.addPacket IOException :" + e.getMessage());
				e.printStackTrace();
			}
		}else{
			Log.e(TAG, "overrun from capture: length:"+packet.length);
		}

	}

	/**
	 * Close the packet trace file
	 */
	private void closePcapTrace() {
		Log.i(TAG, "closePcapTrace()");
		if (pcapOutput != null) {
			pcapOutput.close();
			pcapOutput = null;
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
		
		packetBackGroundWriter.setIsShuttingDown(true);
		
	//	closeTraceFiles();
		
		if(dataServiceThread != null){
			dataServiceThread.interrupt();
		}
		if(packetQueueThread != null){
			packetQueueThread.interrupt();
		}
		
		try {
			if (mInterface != null) {
				Log.i(TAG, "mInterface.close()");
				mInterface.close();
			}
		} catch (IOException e) {
			Log.d(TAG, "mInterface.close():" + e.getMessage());
			e.printStackTrace();
		}
		
		// Stop the previous session by interrupting the thread.
		if (mThread != null) {
			mThread.interrupt();
			int reps = 0;
			while(mThread.isAlive()){
				Log.i(TAG, "Waiting to exit " + ++reps);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(reps > 5){
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
			Log.e(TAG,e.getMessage());
		}
		
		if(success){
			try {
				startCapture();
				Log.i(TAG, "Capture completed");
			} catch (IOException e) {
				Log.e(TAG,e.getMessage());
			}
		}else{
			Log.e(TAG,"Failed to start VPN Service!");
		}

		Log.i(TAG, "Closing Capture files");
		
		closeTraceFiles();
	}

	// Trace files
	
	
	/**
	 * create, open, initialize trace files
	 */
	private void initTraceFiles() throws IOException{
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
	 * @throws IOException
	 */
	private void instanciatePcapFile() throws IOException {
		
		if (!traceDir.exists()) {
			traceDir.mkdirs();
		}

		// gen & open pcap file
		String sFileName = "traffic.cap";
		pcapFile = new File(traceDir, sFileName);
		pcapOutput = new PCapFileWriter(pcapFile);
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
				, ((double)System.currentTimeMillis())/1000.0
				, SystemClock.uptimeMillis()
				);
		
		try {
			timeStream.write(str.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

	//Boreys
	
	/**
	 * setup VPN interface.
	 * @return
	 * @throws IOException
	 */
	boolean startVpnService() throws IOException{
		// If the old interface has exactly the same parameters, use it!
		if (mInterface != null) {
			Log.i(TAG, "Using the previous interface");
			return false;
		}
		
		Log.i(TAG, "startVpnServide=> create builder");
		// Configure a builder while parsing the parameters.
		Builder builder = new Builder()
			.addAddress("10.120.0.1", 32)
			.addRoute("0.0.0.0", 0)
			.setSession("AROCollector")
			.setConfigureIntent(mConfigureIntent)
			;
		if (mInterface != null) {
			try {
				mInterface.close();
			} catch (Exception e) {
				Log.e(TAG, "Exception when closing mInterface:" + e.getMessage());
			}
		}
		Log.i(TAG, "startVpnServide=> builder.establish()");
		mInterface = builder.establish();
		
		if(mInterface != null){
			Log.i(TAG, "\n\\\n  VPN Established:interface = " + mInterface.getFd() + "\n/\n");
			return true;
		}else{
			Log.d(TAG,"mInterface is null");
			return false;
		}
	}

	/**
	 * Start background thread to handle client's socket, handle incoming and outgoing packet from VPN interface
	 * @throws IOException
	 */
	void startCapture() throws IOException{

		Log.i(TAG, "startCapture() :capture starting");
		
		// Packets to be sent are queued in this input stream.
        FileInputStream clientReader = new FileInputStream(mInterface.getFileDescriptor());

        // Packets received need to be written to this output stream.
        FileOutputStream clientWriter = new FileOutputStream(mInterface.getFileDescriptor());
        
        // Allocate the buffer for a single packet.
        ByteBuffer packet = ByteBuffer.allocate(65535);//65535);
        IClientPacketWriter clientPacketWriter = new ClientPacketWriterImpl(clientWriter);
        SessionHandler sessionHandler = SessionHandler.getInstance();
        sessionHandler.setClientWriter(clientPacketWriter);
        sessionHandler.setAndroidContext(this);      	

        //background task for non-blocking socket
      	dataService = new SocketNIODataService();
      	dataService.setClientWriter(clientPacketWriter);
      	dataServiceThread = new Thread(dataService, "dataServiceThread");
      	dataServiceThread.start();
      	
      	//background task for writing packet data to pcap file
      	packetBackGroundWriter = new SocketDataPublisher();
      	packetBackGroundWriter.subscribe(this);
      	packetQueueThread = new Thread(packetBackGroundWriter, "packetQueueThread");
      	packetQueueThread.start();
        
        byte[] data;
        int length;
        
		serviceValid = true;
		while (serviceValid) {
        	//read packet from vpn client
        	data = packet.array();
        	length = clientReader.read(data);
        	if(length > 0){
        		Log.d("DATA_OUT", "received packet from vpn client: "+length);
        		
        		try {
        			
        			sessionHandler.handlePacket(data, length);
				} catch (PacketHeaderException e) {
					Log.e(TAG,e.getMessage());
				}
				
        		packet.clear();
        	}else{
        		try {
        			// lower number improves performance out
        			// app could create huge amount of activity.
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Log.d(TAG,"Failed to sleep: "+ e.getMessage());
				}
        	}
        }
		Log.i(TAG, "capture finished: serviceValid = "+serviceValid);
	}

	@Override
	public boolean handleMessage(Message message) {
		if (message != null) {
			Log.d(TAG, "handleMessage:" + getString(message.what));
			Toast.makeText(this.getApplicationContext(), message.what, Toast.LENGTH_SHORT).show();
		}
		return true;
	}

}