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
package com.att.aro.console.util;

import java.io.PrintStream;

public final class UtilOut {
	private PrintStream out = System.out;
	private PrintStream err = System.err;
	private MessageThreshold threshold = MessageThreshold.Quiet;

	public enum MessageThreshold {
		Quiet,
		Verbose
	}

	public UtilOut() {
		this(MessageThreshold.Quiet);
	}

	public UtilOut(MessageThreshold threshold) {
		this.threshold = threshold;
	}


	/**
	 * print string to console
	 * 
	 * @param str
	 */
	public static void out(String str) {
		System.out.print(str);
	}

	/**
	 * print string to console with new line char
	 * 
	 * @param str
	 */
	public static void outln(String str) {
		System.out.print(str+"\r\n");
	}

	public static void err(String str) {
		System.err.print(str);
	}

	public static void errln(String str) {
		System.err.print(str+"\r\n");
	}

	public MessageThreshold getThreshold() {
		return threshold;
	}

	private static void outIfAppropriate(String str, PrintStream out, MessageThreshold threshold) {
		if (threshold != null && threshold != MessageThreshold.Quiet && threshold.ordinal() >= threshold.ordinal()) {
			out.print(str);
		}
	}

	private static void outlnIfAppropriate(String str, PrintStream out, MessageThreshold threshold) {
		if (threshold != null && threshold != MessageThreshold.Quiet && threshold.ordinal() >= threshold.ordinal()) {
			out.print(str + "\r\n");
		}
	}

	public void conditionalOutMessage(String str, MessageThreshold threshold) {
		outIfAppropriate(str, out, threshold);
	}

	public void conditionalOutMessage(String str) {
		conditionalOutMessage(str, threshold);
	}

	public void conditionalOutMessageln(String str, MessageThreshold threshold) {
		outlnIfAppropriate(str, out, threshold);
	}

	public void conditionalOutMessageln(String str) {
		conditionalOutMessageln(str, threshold);
	}

	public void errMessage(String str) {
		err.print(str);
	}

	public void errMessageln(String str) {
		err.print(str + "\r\n");
	}
	
	public void outMessageln(String str){
		out.print(str + "\r\n");
	}
	
	public void outMessage(String str){
		out.print(str);
	}
}
