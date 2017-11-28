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
package com.att.arocollector.video;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import com.att.arocollector.Config;
import com.att.arocollector.utils.AROCollectorUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

@TargetApi(21)
public class VideoCapture {

	public static String TAG = "VideoCapture";
	private Context context = null;

	private WindowManager windowManager;
	private int bitRate;
	private int screenWidth = 0;
	private int screenHeight = 0;
	private Orientation videoOrient;
	private MediaProjection mediaProjection;
	private VirtualDisplay virtualDisplay;
	private MediaProjectionCallback mediaProjectionCallback;
	private MediaRecorder mediaRecorder;
	private static final String VIRTUAL_DISPLAY_NAME = "VIDEO_CAPTURE";

	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

	static {
		ORIENTATIONS.append(Surface.ROTATION_0, 0);
		ORIENTATIONS.append(Surface.ROTATION_90, 90);
		ORIENTATIONS.append(Surface.ROTATION_180, 180);
		ORIENTATIONS.append(Surface.ROTATION_270, 270);
	}

	private BroadcastReceiver videoCloseCmdReceiver;

	public VideoCapture(  Context context
			, WindowManager windowManager
			, MediaProjection mediaProjection
			, int bitRate
			, String screenSize
			, Orientation videoOrient) {

		this.context = context;
		this.windowManager = windowManager;
		this.mediaProjection = mediaProjection;
		this.bitRate = bitRate;
		this.videoOrient = videoOrient;

		setScreenDimensions(screenSize);
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
			context.unregisterReceiver(broadcastReceiver);
			stop();
		}
	};

	/**
	 * register broadcastReceiver for "arodatacollector.home.activity.close"
	 */
	private void registerVideoCloseCmdReceiver() {
		if (videoCloseCmdReceiver == null) {
			Log.i(TAG, "registering VideoCapture Receiver");
			videoCloseCmdReceiver = broadcastReceiver;
			context.registerReceiver(videoCloseCmdReceiver, new IntentFilter(AROCollectorUtils.VIDEO_CLOSE_CMD_INTENT));
		}
	}

	/**
	 * do not need broadcastReceiver anymore so unregister it!
	 */
	private void unregisterVideoCloseCmdReceiver() {
		Log.d(TAG, "inside unregisterVideoCloseCmdReceiver");
		try {
			if (videoCloseCmdReceiver != null) {
				context.unregisterReceiver(videoCloseCmdReceiver);
				videoCloseCmdReceiver = null;

				Log.d(TAG, "successfully unregistered videoCloseCmdReceiver");
			}
		} catch (Exception e) {
			Log.d(TAG, "Ignoring exception in videoCloseCmdReceiver", e);
		}
	}


	private void setScreenDimensions(String screenSize) {

		if (screenSize == null) {
			Log.e(TAG, "Screen size is null");
			return;
		}

		if (!screenSize.contains("x")) {
			Log.e(TAG, "Unexpected screen size value: " + screenSize);
			return;
		}

		// We expect the first number to be the height if the screen is in portrait mode
		String dimensionA = screenSize.split("x")[0];
		String dimensionB = screenSize.split("x")[1];
		int dimensionAint = isInteger(dimensionA)? Integer.valueOf(dimensionA) : screenHeight;
		int dimensionBint = isInteger(dimensionB)? Integer.valueOf(dimensionB) : screenWidth;
		screenHeight = videoOrient == Orientation.PORTRAIT? dimensionAint : dimensionBint;
		screenWidth = videoOrient == Orientation.PORTRAIT? dimensionBint : dimensionAint;
		Log.e(TAG, "screen width: " + screenWidth + ", screen height: " + screenHeight);
	}

	private boolean isInteger(String dimension) {
		return dimension.matches("\\d+");
	}

	public void start() {
		Log.i(TAG, "start()");
		registerVideoCloseCmdReceiver();
		initMediaRecorder();
		prepareMediaProjection();
		createVirtualDisplay();
		startMediaRecorder();
	}

	private void initMediaRecorder() {

		Log.i(TAG, "initMediaRecorder()");

		if (windowManager == null) {
			Log.e(TAG, "Failed to get screen rotation, media recorder initialization failed");
			return;
		}

		try {

			String outputFile = Config.TRACE_DIR + File.separator + Config.Video.VIDEO_FILE;

			mediaRecorder = new MediaRecorder();
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
			mediaRecorder.setOutputFormat(Config.Video.OUTPUT_FORMAT);
			mediaRecorder.setOutputFile(outputFile);
			mediaRecorder.setVideoSize(screenWidth, screenHeight);
			mediaRecorder.setVideoEncoder(Config.Video.ENCODER);
			mediaRecorder.setVideoEncodingBitRate(bitRate);
			mediaRecorder.setVideoFrameRate(Config.Video.FRAME_PER_SECOND);
			int rotation = windowManager.getDefaultDisplay().getRotation();
			int orientation = ORIENTATIONS.get(rotation);
			mediaRecorder.setOrientationHint(orientation);
			mediaRecorder.prepare();

		} catch (IOException e) {

			e.printStackTrace();
			Log.e(TAG, "Exception when initiating Media Recorder");
		}
	}

	private void prepareMediaProjection() {
		Log.i(TAG, "prepareMediaProjection()");
		if (mediaProjection == null) {
			Log.e(TAG, "Failed to register media projection callback");
			return;
		}
		mediaProjection.registerCallback(new MediaProjectionCallback(), null);
	}

	private void createVirtualDisplay() {
		Log.i(TAG, "createVirtualDisplay()");
		if (windowManager == null) {
			Log.e(TAG, "Failed to get screen density, unable to create virtual display");
			return;
		}

		DisplayMetrics metrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metrics);
		int screenDensity = metrics.densityDpi;

		if (mediaProjection == null || mediaRecorder == null) {
			Log.e(TAG, "Failed to create virtual display");
			return;
		}

		virtualDisplay = mediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME,
				screenWidth, screenHeight, screenDensity,
				DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
				mediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
	}

	private void startMediaRecorder() {
		if (mediaRecorder == null) {
			Log.e(TAG, "Failed to start Media Recorder");
			return;
		}
		mediaRecorder.start();
		String startTime = Double.toString(new Date().getTime()/1000.0);
		saveStartTime(startTime);
		Log.i(TAG, "Media Recorder Started");
	}

	private void saveStartTime(String startTime) {

		String videoTimeFilePath = Config.TRACE_DIR + File.separator + Config.Video.VIDEO_TIME_FILE;
		File videoTimeFile = new File(videoTimeFilePath);
		Log.i(TAG, "create file:" + videoTimeFile.getAbsolutePath());

		FileOutputStream fileOutputStream = null;
		OutputStreamWriter outputStreamWriter = null;
		BufferedWriter bufferedWriter = null;

		try {

			fileOutputStream = new FileOutputStream(videoTimeFile);
			outputStreamWriter = new OutputStreamWriter(fileOutputStream);
			bufferedWriter = new BufferedWriter(outputStreamWriter);
			bufferedWriter.write(startTime);
			bufferedWriter.flush();
			Log.i(TAG, "Finished writing video start time " + startTime);

		} catch (IOException e) {

			Log.e(TAG, "IOException when writing to " + Config.Video.VIDEO_TIME_FILE
					+ "; " + e.getMessage());

		} finally {

			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
					Log.e(TAG, "IOException when closing writer " + e.getMessage());
				}
			}
		}
	}

	public void stop() {
		Log.i(TAG, "stop()");
		stopMediaRecorder();
		releaseVirtualDisplay();
		destroyMediaProjection();
	}

	private void stopMediaRecorder() {
		if (mediaRecorder == null) {
			return;
		}
		mediaRecorder.stop();
		mediaRecorder.reset();
		Log.i(TAG, "Media Recorder Stopped");
	}

	private void releaseVirtualDisplay() {
		if (virtualDisplay == null) {
			return;
		}
		virtualDisplay.release();
		Log.i(TAG, "Virtual Display released");
	}

	private void destroyMediaProjection() {
		if (mediaProjection == null) {
			return;
		}
		mediaProjection.unregisterCallback(mediaProjectionCallback);
		mediaProjection.stop();
		mediaProjection = null;
		Log.i(TAG, "Media Projection Stopped");
	}

	private class MediaProjectionCallback extends MediaProjection.Callback {
		@Override
		public void onStop() {
			Log.i(TAG, "MediaProjectionCallback: com.att.arocollector.video.VideoCapture.MediaProjectionCallback.onStop");
			stop();
		}
	}

}