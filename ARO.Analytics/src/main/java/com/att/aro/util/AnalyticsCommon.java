/*
 *  Copyright 2018 AT&T
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
package com.att.aro.util;

import java.util.ResourceBundle;

public final class AnalyticsCommon {
	// Google Analytics Messages
	public static final boolean GA_SENDFLAG = false;
	public static final String GA_TRACK_ID = getAnalyticsId();
	public static final int GA_VERSION_ID = 1;
	public static final int GA_CAPACITY = 50;

	public static final String GA_ANALYZER = "Analyzer";
	public static final String GA_START_APP = "StartApplication";
	public static final String GA_ENDAPP = "EndApplication";
	public static final String GA_LOAD_TRACE = "LoadTrace";

	public static final String GA_RTC_EXPORT = "RTCExport";
	public static final String GA_RTC_EXPORT_DISPLAY_LABEL = "DisplayForm";
	public static final String GA_RTC_EXPORT_LABEL = "InitiateExport";

	public static final String GA_LOOK_OUT_CHECK = "LookoutCheck";
	public static final String GA_LOOK_OUT_LABEL = "Initiate";

	public static final String GA_COLLECTOR = "Collector";
	public static final String GA_START_TRACE = "StartTrace";
	public static final String GA_END_TRACE = "EndTrace";
	public static final String GA_VO_SESSION = "VO-Session";
	public static final String GA_CLOSE = "Close";
	public static final String GA_TRACE_ANALYZED = "TracesAnalyzed";
	private static String getAnalyticsId() {
		return ResourceBundle.getBundle("build").getString("ga.id");
	}
}