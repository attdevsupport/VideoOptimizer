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
package com.att.aro.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.LogManager;

import com.att.aro.core.analytics.IGoogleAnalytics;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		IGoogleAnalytics googleAnalytics = GoogleAnalyticsUtil.getGoogleAnalyticsInstance();
		googleAnalytics.sendCrashEvents(convertTracetoString("", throwable));
		LogManager.getLogger(CrashHandler.class.getName()).error("Uncaught ARO Exception:", throwable);	
	}

	public static String convertTracetoString(String message, Throwable throwable){
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		String swValue = sw.toString();
		int max = swValue.length() < 100 ? swValue.length():100;
		String exceptionAsString = (message+swValue).substring(0, max);
		exceptionAsString = exceptionAsString.replaceAll("\n\t", "%20").replaceAll("\n", "%20");
		return exceptionAsString;
	}
	
}
