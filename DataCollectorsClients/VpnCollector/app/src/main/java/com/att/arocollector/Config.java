/*
 * Copyright 2014 AT&T
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

import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;

public class Config {

	public static class Permission {
		public static final int VPN_PERMISSION_REQUEST_CODE = 0;
	    public static final int CERT_INSTALL_REQUEST_CODE = 1;
		public static final int VIDEO_PERMISSION_REQUEST_CODE = 2;
	}

	public static class Video {
		public static final int OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
		public static final int ENCODER = MediaRecorder.VideoEncoder.H264;
		public static final int FRAME_PER_SECOND = 30;
		public static final String VIDEO_FILE = "video.mp4"; // video recorded using Media Projection
		public static final String VIDEO_TIME_FILE = "video_time";
	}
	
	public static final String TRAFFIC_NETWORK_INTERFACE = "tun0";
	public static final String TRACE_DIR = Environment.getExternalStorageDirectory().getPath()
			+ File.separator + "ARO";
	
	public static final String TRACEFILE_DIR 		= "/sdcard/ARO/";
	public static final String TRACEFILE_APPNAME 	= "appname";
	public static final String TRACEFILE_CPU 		= "cpu";
}
