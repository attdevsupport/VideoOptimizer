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
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.springframework.util.StringUtils;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		if (!throwable.getClass().equals(CancellationException.class)) {
			StringWriter sw = new StringWriter();
			throwable.printStackTrace(new PrintWriter(sw));
			String report = prepReport(sw.toString());
			if (!report.isEmpty()) {
				GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendCrashEvents(report);
				LogManager.getLogger(CrashHandler.class.getName()).error("Uncaught ARO Exception:", throwable);
			} else {
				GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendCrashEvents("Failed to get stack parsed");
				LogManager.getLogger(CrashHandler.class.getName()).error("Failed to get stack parsed, Uncaught ARO Exception:", throwable);
			}
			if (throwable.getClass().equals(OutOfMemoryError.class)) {
				Util.restart(true);
			}
		}
	}

	public String prepReport(String str) {
		StringParse stringParse = new StringParse();
		if (StringUtils.isEmpty(str)) {
			return "";
		}
		Pattern pattern;
		if (str.contains("com.att.aro")) {
			// produces 3 matches if contains "com.att.aro."
			pattern = Pattern.compile("com\\.att\\.aro.+\\.([A-Z].*)\\.(.+)\\(.+:(\\d+)");
		} else {
			// produces 1 match, for use when there is no match on aro as above
			pattern = Pattern.compile("^.+at (.*)");
		}

		StringBuilder report = new StringBuilder();
		String[] results;

		String[] lines = str.split(System.getProperty("line.separator"));
		for (String line : lines) {
			results = stringParse.parse(line, pattern);
			if (results != null && results.length > 0) {
				report.append(formatResultArray(results));
			}
			if (report.length() > 100) {
				break;
			}
		}
		String[] head = stringParse.parse(lines[0], "(.*):");
		report.insert(0, head != null ? head[0] : lines[0]);
		return report.toString();
	}

	private String formatResultArray(String[] results) {
		if (results != null) {
			if (results.length == 1) {
				return String.format("\t|\t%s", results[0]);
			} else if (results.length == 3) {
				return String.format("\t|\t%s.%s@%s", results[0], results[1], results[2]);
			} else {
				return results[0];
			}
		}
		return "";
	}
	
}
