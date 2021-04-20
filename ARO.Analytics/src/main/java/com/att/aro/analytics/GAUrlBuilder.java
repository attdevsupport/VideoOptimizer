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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Random;
import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.pojo.VersionInfo;

public class GAUrlBuilder {
	private static final String TRACKING_URL_PREFIX = "http://www.google-analytics.com/collect";
	private static final Random random = new Random();
	private static String hostName = "localhost";

	private String googleAnalyticsTrackingCode;
	private String appName = "";
	private VersionInfo versionInfo =(VersionInfo) SpringContextUtil.getInstance().getContext().getBean(VersionInfo.class);
	private String appVersion = "";
	private String appCode = "1";
	private StringBuffer defaultUrl = new StringBuffer();

	static {
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// ignore this
		}
	}

	public GAUrlBuilder(String appName, String googleAnalyticsTrackingCode) {
		this.appName = appName;
		this.appVersion = versionInfo.getVersion().trim().replaceAll(" ", "%20").replaceAll("#", "%23");
		this.googleAnalyticsTrackingCode = googleAnalyticsTrackingCode;
	}
	public String buildURL(GAEntry focusPoint) {
		StringBuilder url = new StringBuilder(getURLWithRequiredParams());
		url.append(getParametersForUrl(focusPoint));
		return url.toString();
	}

	private String getURLWithRequiredParams() {
		StringBuffer preFixUrl = new StringBuffer();
		if (defaultUrl.length() <= 0) {
			preFixUrl.append(TRACKING_URL_PREFIX);
			preFixUrl.append("?").append(GARequiredParameter.version.param()).append(appCode);
			preFixUrl.append(appendString(GARequiredParameter.trackid.param(), googleAnalyticsTrackingCode));
			preFixUrl.append(appendString(GARequiredParameter.clientid.param(), hostName.trim()));
			this.defaultUrl = preFixUrl;
		} else {
			preFixUrl = this.defaultUrl;
		}

		return preFixUrl.toString();
	}

	public String getHost() {
		String hostString = "";
		try {
			hostString = URLEncoder.encode(hostName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return hostString;
	}

	private String getAppParams(GAEntry entry) {
		StringBuilder appParams = new StringBuilder();
		HitType hitType = (entry.getHitType() == null ? HitType.SCREEN_VIEW : entry.getHitType());
		appParams.append(appendString(GARequiredParameter.hittype.param(), hitType.getValue()));
		appParams.append(appendString(GACommonParameter.applicationname.param(), encode(this.appName)));
		appParams.append(appendString(GACommonParameter.applicationversion.param(), this.appVersion.trim()));
		return appParams.toString();

	}

	private String getExceptionParams() {
		StringBuilder appParams = new StringBuilder();
		appParams.append(appendString(GACommonParameter.applicationname.param(), "ARO"));
		if (encode(this.appName) != null && encode(this.appName).length() > 0) {
			appParams.append(appendString(GACommonParameter.applicationversion.param(), encode(this.appName)));
		} else {
			appParams.append(appendString(GACommonParameter.applicationversion.param(), this.appVersion.trim()));
		}
		appParams.append(appendString(GARequiredParameter.hittype.param(), HitType.EXCEPTION.getValue()));
		return appParams.toString();
	}

	private String encode(String value) {
		String appendedParamValue = "";
		try {
			appendedParamValue = value;
			return URLEncoder.encode(appendedParamValue, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return appendedParamValue;
		}
	}

	private String appendString(String param, String value) {
		return "&" + param + value;
	}

	private String getParametersForUrl(GAEntry entry) {
		StringBuilder paramsUrls = new StringBuilder();
		if (checkNotForNull(entry.getExceptionDesc())) {
			paramsUrls.append(getExceptionParams());
		} else {
			paramsUrls.append(getAppParams(entry));
		}

		if (entry != null) {
			if (entry.getHitType() != null) {
				if (entry.getHitType() == HitType.SCREEN_VIEW) {
					paramsUrls.append(appendString(GACommonParameter.ScreenName.param(), entry.getCategory()));
				} else if (entry.getHitType() == HitType.TIMING) {
					paramsUrls.append(getTimingParams(entry));
				}
			} else if (checkNotForNull(entry.getExceptionDesc())) {
				if (entry.getDataSource() != null && GACommonParameter.datasource.param() != null)
					paramsUrls.append(appendString(GACommonParameter.datasource.param(),
							entry.getDataSource().replace(" ", "%20")));
				if (entry.getExceptionDesc() != null && GACommonParameter.exceptiondesc.param() != null)
					paramsUrls.append(appendString(GACommonParameter.exceptiondesc.param(),
							entry.getExceptionDesc().replace(" ", "%20")));
				if (entry.isFatal()) {
					paramsUrls.append(appendString(GACommonParameter.isexceptionfatal.param(), "1"));
				} else {
					paramsUrls.append(appendString(GACommonParameter.isexceptionfatal.param(), "0"));
				}
			} else {
				if (checkNotForNull(entry.getSession())) {
					if (entry.getSession().equals(GASessionValue.start.param())) {
						paramsUrls.append(appendString(GACommonParameter.sessionstart.param(), entry.getSession()));
					} else {
						paramsUrls.append(appendString(GACommonParameter.sessionend.param(), entry.getSession()));
					}
				}

				// Even Category
				if (entry!=null && checkNotForNull(entry.getCategory()) && entry.getCategory().trim().length() > 0) {

					paramsUrls.append(appendString(GARequiredParameter.hittype.param(), HitType.EVENT.getValue()));
					paramsUrls.append(appendString(GACommonParameter.eventcategory.param(), entry.getCategory()));

					if (checkNotForNull(entry.getAction())) {
						paramsUrls.append(appendString(GACommonParameter.eventaction.param(), entry.getAction()));
					}
					if (checkNotForNull(entry.getLabel())) {
						paramsUrls.append(appendString(GACommonParameter.eventlabel.param(), entry.getLabel()));
					}
					if (checkNotForNull(entry.getValue())) {
						paramsUrls.append(appendString(GACommonParameter.eventvalue.param(), entry.getValue()));
					}
				}
			}
		}

		if (entry.getExceptionDesc() == null) {
			paramsUrls.append(appendString(GACommonParameter.anonymizeip.param(), "1"));

			paramsUrls.append(appendString(GACommonParameter.cachebuster.param(), "" + random.nextInt()));
		}

		return paramsUrls.toString();
	}

	private String getTimingParams(GAEntry entry) {
		StringBuilder appParams = new StringBuilder();
		appParams.append(appendString(GACommonParameter.timingcategory.param(), entry.getTimingCategory()));
		appParams.append(appendString(GACommonParameter.timingtime.param(), entry.getTimingValue()));
		appParams.append(
				appendString(GACommonParameter.timingvariablename.param(), entry.getTimingVariable()));
		return appParams.toString();
	}

	private boolean checkNotForNull(String string) {
		if (string == null) {
			return false;
		} else {
			return true;
		}
	}

}
