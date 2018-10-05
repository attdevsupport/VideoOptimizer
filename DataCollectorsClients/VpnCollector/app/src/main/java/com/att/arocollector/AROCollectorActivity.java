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

package com.att.arocollector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.att.arocollector.attenuator.AttenuatorManager;
import com.att.arocollector.attenuator.AttenuatorUtil;
import com.att.arocollector.client.CaptureVpnService;
import com.att.arocollector.privatedata.AROPrivateDataCollectorService;
import com.att.arocollector.utils.AROCollectorUtils;
import com.att.arocollector.utils.BundleKeyUtil;
import com.att.arocollector.video.Orientation;
import com.att.arocollector.video.VideoCapture;
import com.att.arocollector.video.VideoOption;
import com.att.arotracedata.AROCollectorService;
import com.att.arotracedata.AROCpuTempService;
import com.att.arotracedata.AROCpuTraceService;
import com.att.arotracedata.AROGpsMonitorService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AROCollectorActivity extends Activity {

	private static String TAG = AROCollectorActivity.class.getSimpleName();

	private Context context;
	private Intent captureVpnServiceIntent;
	private BroadcastReceiver analyzerCloseCmdReceiver = null;
	private Intent aroCollectorService;
	private Intent collectPrivateDataService;
	private ComponentName collectorService;
	private Intent aROGpsMonitorService;
	private ComponentName gpsMonitorService;
	private VideoOption videoOption = VideoOption.NONE;
	private Orientation videoOrient = Orientation.PORTRAIT;
	private int bitRate = 0;
	private String screenSize = "";
	private VideoCapture videoCapture;
	private MediaProjectionManager mediaProjectionManager;
	private boolean printLog = false;
	private File tempCertFile;
	private String CERTFILE = "cacert.pem";
	private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate(...)");
		// Setup handler for uncaught exceptions.
		Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException (Thread thread, Throwable e) {
				handleUncaughtException (thread, e);
			}
		});

		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		final TextView splashText = (TextView) findViewById(R.id.splash_message);
		splashText.setText(String.format(getString(R.string.splashmessageopensource), getString(R.string.app_brand_name), getString(R.string.app_url_name)));
		AttenuatorManager.getInstance().init();
		Intent intent = getIntent();
		//delay
		int delayDl = intent.getIntExtra(BundleKeyUtil.DL_DELAY, 0);
		int delayUl = intent.getIntExtra(BundleKeyUtil.UL_DELAY, 0);

		if(delayDl >= 0){
			AttenuatorManager.getInstance().setDelayDl(delayDl);
		}else{
			Log.i(TAG,"Invalid attenuation delay value"+ delayDl + "ms");
		}

		if(delayUl >= 0){
			AttenuatorManager.getInstance().setDelayUl(delayUl);
		}else{
			Log.i(TAG,"Invalid attenuation delay value"+ delayUl + "ms");
		}

		//throttle
		int throttleDl = intent.getIntExtra(BundleKeyUtil.DL_THROTTLE, AttenuatorUtil.DEFAULT_THROTTLE_SPEED);
		int throttleUl = intent.getIntExtra(BundleKeyUtil.UL_THROTTLE, AttenuatorUtil.DEFAULT_THROTTLE_SPEED);

		AttenuatorManager.getInstance().setThrottleDL(throttleDl);
		Log.d(TAG,"Download speed throttle value: "+ throttleDl + " kbps");

		AttenuatorManager.getInstance().setThrottleUL(throttleUl);
		Log.d(TAG,"Upload speed throttle value: "+ throttleUl + " kbps");

		printLog = intent.getBooleanExtra(BundleKeyUtil.PRINT_LOG, false);

		setVideoOption(intent);

		bitRate = intent.getIntExtra(BundleKeyUtil.BIT_RATE, 0);
		String screenSizeTmp = intent.getStringExtra(BundleKeyUtil.SCREEN_SIZE);
		screenSize = screenSizeTmp == null? screenSize : screenSizeTmp;

		setVideoOrient(intent);

		Log.i(TAG, "get from intent delayTime: " + AttenuatorManager.getInstance().getDelayDl()
				+ "get from intent delayTimeUL: " + AttenuatorManager.getInstance().getDelayUl()
				+ "get from intent throttleDL: " + AttenuatorManager.getInstance().getThrottleDL()
				+ "get from intnetn throttleUL: " + AttenuatorManager.getInstance().getThrottleUL()
				+ " video: " + videoOption
				+ " bitRate: " + bitRate + " screenSize: " + screenSize
				+ " orientation: " + videoOrient);


		context = getApplicationContext();
		launchAROCpuTraceService();
		launchAROCpuTempService();
		if (networkAndAirplaneModeCheck()) {

			// register to listen for close down message
			registerAnalyzerCloseCmdReceiver();
			Log.d(TAG, "register the attenuator delay signal");

			startVPN();
		}

		{ // test code
			PackageInfo packageInfo = null;
			try {
				packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean valu = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
			// build datetime
			Date buildDate = new Date(BuildConfig.TIMESTAMP);
			String appBuildDate = "";
			if( buildDate!=null){
				appBuildDate = buildDate.toString();
			}

			String display =
					"App Build Date: "+ appBuildDate + "\n"
//					+" DownStream Delay Time: " + AttenuatorManager.getInstance().getDelayDl() + " ms\n"
//					+" UpStream Delay Time: " + AttenuatorManager.getInstance().getDelayUl() + " ms\n"
//					+" DownStream Throttle: " + AttenuatorManager.getInstance().getThrottleDL() + " kbps\n"
//					+" Upstream Throttle: " + AttenuatorManager.getInstance().getThrottleUL() + " kbps\n"
					+ AttenuatorUtil.getInstance().notificationMessage() + "\n"
					+" Version: " + packageInfo.versionName + " (" + (valu ? "Debug" : "Production") + ")";

			((TextView) findViewById(R.id.version)).setText(display);
		}
	}

	private void setVideoOption(Intent intent) {
		String videoOptionTmpStr = intent.getStringExtra(BundleKeyUtil.VIDEO);
		VideoOption videoOptionTmpEnum = videoOptionTmpStr == null?
				null : VideoOption.getVideoOption(videoOptionTmpStr);
		if (videoOptionTmpEnum != null) {
			videoOption = videoOptionTmpEnum;
		}
	}

	private void setVideoOrient(Intent intent) {
		String videoOrientTmpStr = intent.getStringExtra(BundleKeyUtil.VIDEO_ORIENTATION);
		Orientation videoOrientTmpEnum = videoOrientTmpStr == null?
				null : Orientation.getOrientation(videoOrientTmpStr);
		if (videoOrientTmpEnum != null) {
			videoOrient = videoOrientTmpEnum;
		}
	}

	/**
	 * launch AROCpuTraceService for the collection of currently running process and tasks
	 */
	private void launchAROCpuTraceService(){
		Log.i(TAG, "launchAROCpuTraceService()");
		Intent aroCpuTraceService = new Intent(this, AROCpuTraceService.class);
		aroCpuTraceService.putExtra("TRACE_DIR", "/sdcard/ARO/");
		startService(aroCpuTraceService);
	}

	/**
	 * start CPU Temperature capture service
	 */
	private void launchAROCpuTempService() {
		Log.i(TAG, "launchAROCpuTempService()");
		Intent aroCpuTempService = new Intent(this, AROCpuTempService.class);
		startService(aroCpuTempService);
	}


	/**
	 * initiate trace services
	 */
	private void startServices() {
		launchAROCollectorService();
		launchAROGpsMonitorService();
		// for Security Best Practice 2 - Transmission of Private Data
		// This service stops by itself.
		launchCollectPrivateDataService();
//		 launchAROCameraMonitorService();
	}

	private void stopServices() {
		stopAROCollectorService();
		stopAROGpsMonitorService();
//		 stopAROCameraMonitorService();
	}

	/**
	 * Launch intent for user approval of VPN connection
	 */
	private void startVPN() {
		Log.i(TAG, "startVPN()");

		// check for VPN already running
		try {
			if (!checkForActiveInterface(getApplicationContext(), Config.TRAFFIC_NETWORK_INTERFACE)) {

				// get user permission for VPN
				Intent intent = VpnService.prepare(this);
				if (intent != null) {
					Log.d(TAG, "ask user for VPN permission");
					startActivityForResult(intent, Config.Permission.VPN_PERMISSION_REQUEST_CODE);
				} else {
					Log.d(TAG, "already have VPN permission");
					onActivityResult(Config.Permission.VPN_PERMISSION_REQUEST_CODE, RESULT_OK, null);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception checking network interfaces :" + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * check a network interface by name
	 *
	 * @param context
	 * @param networkInterfaceName
	 * @return true if interface exists and is active
	 * @throws Exception
	 */
	private boolean checkForActiveInterface(Context context, String networkInterfaceName) throws Exception {

		List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			if (intf.getName().equals(networkInterfaceName)) {
				return intf.isUp();
			}
		}
		return false;
	}
	@TargetApi(21)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i(TAG, "onActivityResult(... requestCode{" + requestCode + "} resultCode{" + resultCode + "} ...)");

		switch (requestCode) {

			case Config.Permission.VPN_PERMISSION_REQUEST_CODE:
				if (resultCode == RESULT_OK) {

					captureVpnServiceIntent = new Intent(getApplicationContext(), CaptureVpnService.class);
					captureVpnServiceIntent.putExtra("TRACE_DIR", Config.TRACE_DIR);
					captureVpnServiceIntent.putExtra(BundleKeyUtil.PRINT_LOG, printLog);

					if(isExternalStorageWritable()){
						Log.i(TAG, "TRACE_DIR: "+ Config.TRACE_DIR +"trace directory: "+
								Environment.getExternalStoragePublicDirectory
										( Environment.DIRECTORY_DOCUMENTS));

					}

					startService(captureVpnServiceIntent);

					// start collecting META data
					startServices();

					if (doVideoCapture()) {
						getVideoCapturePermission();
					} else {
						pushAppToBackStack();
					}

				} else if (resultCode == RESULT_CANCELED) {
					showVPNRefusedDialog();
				}

				break;

			case Config.Permission.VIDEO_PERMISSION_REQUEST_CODE:
				Log.i("SecureCollector", "VIDEO_PERMISSION_REQUEST_CODE");
				pushAppToBackStack();

				if (resultCode != RESULT_OK) {
					Toast.makeText(this, "Screen Cast Permission Denied, no SD/HD video will be captured.",
							Toast.LENGTH_SHORT).show();
					return;
				}

				MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(
						resultCode, data);
				videoCapture = new VideoCapture(getApplicationContext(), getWindowManager(), mediaProjection, bitRate, screenSize, videoOrient);
				videoCapture.start();
				break;

			case Config.Permission.CERT_INSTALL_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					if (tempCertFile != null) {
						tempCertFile.delete();
						tempCertFile = null;
					}
					pushAppToBackStack();
				} else {
					pushAppToBackStack();
				}
				break;

			default:
				break;
		}
	}

	/**
	 * Once the VPN Permission is provided, push the app to Back stack
	 * and leave other apps in foreground
	 */
	private void pushAppToBackStack() {
		//
		boolean amIPushedBack = moveTaskToBack(true);
		Log.d(TAG, String.valueOf(amIPushedBack));
	}

	/**
	 * Show dialog to educate the user about VPN trust abort app if user chooses
	 * to quit otherwise relaunch the startVPN()
	 */
	private void showVPNRefusedDialog() {

		new AlertDialog.Builder(this).setTitle("Usage Alert")
				.setMessage("You must trust the " + getString(R.string.app_name) + "\nIn order to run a VPN based trace")
				.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startVPN();
					}
				}).setNegativeButton(getString(R.string.quit), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		}).show();

	}

	/**
	 * @param title
	 * @param message
	 */
	private void showInfoDialog(String title, String message) {

		new AlertDialog.Builder(this).setTitle(title).setMessage(message)
				/*
				 * .setPositiveButton(getString(android.R.string.ok), new
				 * DialogInterface.OnClickListener() {
				 *
				 * @Override public void onClick(DialogInterface dialog, int
				 * which) { startVPN(); } })
				 */

				.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// finish();
					}
				}).show();

	}

	/**
	 * launch AROCollectorService for the collection of META data
	 */
	private void launchAROCollectorService() {
		aroCollectorService = new Intent(getApplicationContext(), AROCollectorService.class);
		aroCollectorService.putExtra("TRACE_DIR", Config.TRACE_DIR);
		aroCollectorService.putExtra(BundleKeyUtil.DL_DELAY, AttenuatorManager.getInstance().getDelayDl());
		aroCollectorService.putExtra(BundleKeyUtil.UL_DELAY, AttenuatorManager.getInstance().getDelayUl());
		aroCollectorService.putExtra(BundleKeyUtil.DL_THROTTLE,AttenuatorManager.getInstance().getThrottleDL());
		aroCollectorService.putExtra(BundleKeyUtil.UL_THROTTLE,AttenuatorManager.getInstance().getThrottleUL());
		aroCollectorService.putExtra(BundleKeyUtil.ATTENUATION_PROFILE,getIntent().getBooleanExtra(BundleKeyUtil.ATTENUATION_PROFILE,false));
		aroCollectorService.putExtra(BundleKeyUtil.ATTENUATION_PROFILE_NAME,getIntent().getStringExtra(BundleKeyUtil.ATTENUATION_PROFILE_NAME));
		aroCollectorService.putExtra(BundleKeyUtil.VIDEO_ORIENTATION, videoOrient.toString());
		aroCollectorService.addCategory(AROCollectorService.ARO_COLLECTOR_SERVICE);
		collectorService = startService(aroCollectorService);
	}

	private void launchCollectPrivateDataService() {
		collectPrivateDataService = new Intent(getApplicationContext(), AROPrivateDataCollectorService.class);
		collectPrivateDataService.putExtra("TRACE_DIR", Config.TRACE_DIR);
		startService(collectPrivateDataService);
	}

	/**
	 * launch AROGpsMonitorService for the collection of META data
	 */
	private void launchAROGpsMonitorService() {
		aROGpsMonitorService = new Intent(getApplicationContext(), AROGpsMonitorService.class);
		aROGpsMonitorService.putExtra("TRACE_DIR", Config.TRACE_DIR);
		gpsMonitorService = startService(aROGpsMonitorService);
	}

	/**
	 * stop AROCollectorService for the collection of META data
	 */
	private void stopAROCollectorService() {
		stopService(new Intent(AROCollectorService.ARO_COLLECTOR_SERVICE));
	}

	/**
	 * stop AROGpsMonitorService for the collection of META data
	 */
	private void stopAROGpsMonitorService() {

		stopService(new Intent(AROGpsMonitorService.ARO_GPS_MONITOR_SERVICE));
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy()");
		if (analyzerCloseCmdReceiver != null) {
			Log.d(TAG, "calling unregisterAnalyzerCloseCmdReceiver inside onDestroy()");
			unregisterAnalyzerCloseCmdReceiver();
		}

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause()");
		// stopAROCollectorService();
		// stopAROGpsMonitorService();
		if (analyzerCloseCmdReceiver != null) {
			Log.d(TAG, "calling unregisterAnalyzerCloseCmdReceiver inside onPause()");
			unregisterAnalyzerCloseCmdReceiver();
		}
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "onStop()");
		// TODO Auto-generated method stub
		super.onStop();
	}

	/**
	 * <pre>
	 * Received broadcast from
	 * adb shell
	 * am broadcast -a arodatacollector.home.activity.close
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "received analyzer close cmd intent at " + System.currentTimeMillis());
			boolean rez = stopService(captureVpnServiceIntent);
			Log.d(TAG, "stopService result=" + rez);
			unregisterReceiver(broadcastReceiver);
			finish();
		}
	};

	/**
	 * register broadcastReceiver for "arodatacollector.home.activity.close"
	 */
	private void registerAnalyzerCloseCmdReceiver() {
		if (analyzerCloseCmdReceiver == null) {
			Log.i(TAG, "registering Receiver");
			analyzerCloseCmdReceiver = broadcastReceiver;
			registerReceiver(analyzerCloseCmdReceiver, new IntentFilter(AROCollectorUtils.ANALYZER_CLOSE_CMD_INTENT));
		}
	}

	/**
	 * do not need broadcastReceiver anymore so unregister it!
	 */
	private void unregisterAnalyzerCloseCmdReceiver() {
		Log.d(TAG, "inside unregisterAnalyzerCloseCmdReceiver");
		try {
			if (analyzerCloseCmdReceiver != null) {
				unregisterReceiver(analyzerCloseCmdReceiver);
				analyzerCloseCmdReceiver = null;

				Log.d(TAG, "successfully unregistered analyzerCloseCmdReceiver");
			}
		} catch (Exception e) {
			Log.d(TAG, "Ignoring exception in analyzerCloseCmdReceiver", e);
		}
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		registerAnalyzerCloseCmdReceiver();

		super.onResume();
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "onBackPressed");
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean networkAndAirplaneModeCheck() {
		String title = getString(R.string.app_short_name);
		String message = "";
		boolean networkChecker = true;
		if (!isConnectedToInternet()) {
			message = "No network connection in your phone, Connect to network and start again";
			// popup dialog
			showInfoDialog(title, message);
			networkChecker = false;
		}
		return networkChecker;
	}

	/**
	 * @return
	 */
	private boolean isConnectedToInternet() {
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null)
				for (int j = 0; j < info.length; j++) {
					Log.i(TAG, "NETWORK CONNECTION : " + info[j].getState() + " Connected STATE :"
							+ NetworkInfo.State.CONNECTED);
					if (info[j].getState().equals(NetworkInfo.State.CONNECTED)) {
						return true;
					}
				}
		}
		return false;
	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}


	public void handleUncaughtException (Thread thread, Throwable e)  {
 		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager)this.getSystemService(Activity.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		long availableMegs = mi.availMem / 1048576L;
	}

	private boolean doVideoCapture() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
				(videoOption == VideoOption.SDEF || videoOption == VideoOption.HDEF));
	}

	@TargetApi(21)
	private void getVideoCapturePermission() {
		mediaProjectionManager =  (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
				Config.Permission.VIDEO_PERMISSION_REQUEST_CODE);
	}

	/**
	 * Locate or Create ARO in externalStorage
	 *
	 * @return the path to ARO
	 */
	private File locateARO() {
		File path = Environment.getExternalStoragePublicDirectory("ARO");
		if (!path.exists()) {
			path.mkdir();
		}
		return path;
	}

	private boolean checkWritePerm() {
		Context context = getApplicationContext();
		int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permissionCheck == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
			permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (permissionCheck == PackageManager.PERMISSION_DENIED) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	boolean extractResource(File filepath, int rsrc) {
		boolean result = true;
		try {
			if (filepath.exists()) {
				filepath.delete();
			}
			filepath.createNewFile();
			try(InputStream iStream = this.getResources().openRawResource(rsrc);
				OutputStream oStream = new FileOutputStream(filepath)) {
				final byte[] buffer = new byte[1024];
				int length;
				while ((length = iStream.read(buffer)) > 0) {
					oStream.write(buffer, 0, length);
				}
				oStream.flush();
				Log.d(TAG, filepath.getAbsolutePath() + " Extracted");
			}
		} catch (IOException e) {
			Log.e(TAG, "ERROR :" + e.getMessage(), e);
			result = false;
		}
		return result;
	}

}
