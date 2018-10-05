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
package com.att.aro.analytics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

/**
 * Simple class peforming HTTP Get method on the requested url
 *
 * Created by Harikrishna Yaramachu on 3/18/14.
 */
public class HTTPGetMethod {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger(HTTPGetMethod.class.getSimpleName());
	private static final String GET_METHOD_NAME = "GET";
	private static String applicationName = "VideoOptimizer";
	private static String userAgentInfo = null;

	private static boolean isValidIConnection = false;
	private static boolean isProxy = false;
	private static Proxy proxyObj;

	public HTTPGetMethod() {

	}

	public boolean request(String urlString) {
		boolean gaRequestStatus = false;
		try {
			URL url = new URL(urlString);
			HttpURLConnection urlConnection;
			if (isProxy) {
				urlConnection = openURLConnection(url, proxyObj);
			} else {
				urlConnection = openURLConnection(url);
			}

			urlConnection.setInstanceFollowRedirects(true);
			urlConnection.setConnectTimeout(3000);
			urlConnection.setRequestMethod(GET_METHOD_NAME);
			urlConnection.setRequestProperty("User-agent", getUserAgentInfo());
			urlConnection.connect();
			int responseCode = getResponseCode(urlConnection);
			if (responseCode != HttpURLConnection.HTTP_OK) {
			} else {
				gaRequestStatus = true;
			}
			urlConnection.disconnect();
		} catch (Exception exception) {
			LOGGER.error("Connection Failure: ", exception);
		}
		return gaRequestStatus;
	}

	protected int getResponseCode(HttpURLConnection urlConnection) throws IOException {
		return urlConnection.getResponseCode();
	}

	private HttpURLConnection openURLConnection(URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}

	private HttpURLConnection openURLConnection(URL url, Proxy proxy) throws IOException {
		return (HttpURLConnection) url.openConnection(proxy);
	}

	public String getUserAgentInfo() {
		if (userAgentInfo == null) {
			String uaName = "Java" + "/" + System.getProperty("java.version"); // java version info appended
			// os string is architecture+osname+version concatenated with _
			String osString = System.getProperty("os.arch");
			if (osString == null || osString.length() < 1) {
				osString = "";
			} else {
				String osName = System.getProperty("os.name");
				if (osName.startsWith("Windows")) {
					String osTemp = osString;
					osString = osName.substring(0, osName.indexOf(" ")) + ";"
							+ osName.substring(osName.indexOf(" ")).trim() + ";" + System.getProperty("os.version")
							+ ";" + osTemp;

				} else {
					osString += "; ";
					osString += osName + " " + System.getProperty("os.version");
				}
			}
			userAgentInfo = applicationName + " (" + osString + "; en-US) " + uaName;

		}
		return userAgentInfo;
	}

	public static void setApplicationName(String applicationName) {
		HTTPGetMethod.applicationName = applicationName;
	}

	public synchronized boolean isValidIConnection() {
		return isValidIConnection;
	}

	public synchronized void setIsValidIConnection(boolean inetFlag) {
		isValidIConnection = inetFlag;
	}

	public synchronized void setIsProxy(boolean proxyFlag) {
		isProxy = proxyFlag;
	}

	public synchronized void setProxyObj(Proxy proxyObject) {
		proxyObj = proxyObject;
	}
}
