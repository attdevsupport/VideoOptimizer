package com.att.aro.analytics;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * Created by Harikrishna Yaramachu on 4/3/14.
 */
public class GAURLBuildingStrategy implements URLBuildingStrategy {

	// private static final Logger LOGGER =
	// Logger.getLogger(GAURLBuildingStrategy.class.getName());

	private String googleAnalyticsTrackingCode;
	private String refererURL = "http://www.att.com";

	private String appName = "";
	private String appVersion = "";
	private String appCode = "1";

	private static final String TRACKING_URL_Prefix = "http://www.google-analytics.com/collect"; // http://www.google-analytics.com/collect

	private static final Random random = new Random();
	private static String hostName = "localhost";
	private static String ipAddress = "1";

	private StringBuffer defaultUrl = new StringBuffer();

	static {
		try {
			ipAddress = InetAddress.getLocalHost().getHostAddress();// to get ip
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// ignore this
		}
	}

	public GAURLBuildingStrategy(String appName, String appVersion, String googleAnalyticsTrackingCode) {
		this.appName = appName;
		this.appVersion = appVersion;
		this.googleAnalyticsTrackingCode = googleAnalyticsTrackingCode;

	}

	public String buildURL(FocusPoint focusPoint) {

		StringBuffer url = new StringBuffer(getURLWithRequiredParams());

		url.append(getParametersForUrl(focusPoint));

		// LOGGER.info(" URL : "+url.toString());
		return url.toString();
	}

	public void setRefererURL(String refererURL) {
		this.refererURL = refererURL;
	}

	private String getURLWithRequiredParams() {

		StringBuffer preFixUrl = new StringBuffer();
		if (defaultUrl.length() <= 0) {
			preFixUrl.append(TRACKING_URL_Prefix);
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

	private String getAppParams() {
		StringBuffer appParams = new StringBuffer();
		appParams.append(appendString(GARequiredParameter.hittype.param(), GAHitTypes.typeA.type()));
		appParams.append(appendString(GACommonParameter.applicationname.param(), encode(this.appName)));
		appParams.append(appendString(GACommonParameter.applicationversion.param(), this.appVersion.trim()));

		// LOGGER.info("Application Name : "+this.appName + " Encoded : "
		// +encode(this.appName));
		return appParams.toString();

	}

	private String getExceptionParams() {
		StringBuffer appParams = new StringBuffer();
		appParams.append(appendString(GACommonParameter.applicationname.param(), "ARO"));
		if (encode(this.appName) != null && encode(this.appName).length() > 0) {
			appParams.append(appendString(GACommonParameter.applicationversion.param(), encode(this.appName)));
		} else {
			appParams.append(appendString(GACommonParameter.applicationversion.param(), "1.1.0"));
		}
		appParams.append(appendString(GARequiredParameter.hittype.param(), GAHitTypes.typeEX.type()));

		// LOGGER.info("Application Name : "+this.appName + " Encoded : "
		// +encode(this.appName));
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

	private String getParametersForUrl(FocusPoint aPoint) {
		StringBuffer paramsUrls = new StringBuffer();
		if (checkNotForNull(aPoint.getExceptionDesc())) {
			paramsUrls.append(getExceptionParams());
		} else {
			paramsUrls.append(getAppParams());
		}

		if (aPoint != null) {

			if (checkNotForNull(aPoint.getExceptionDesc())) {
				if (aPoint.getDataSource() != null && GACommonParameter.datasource.param() != null)
					paramsUrls.append(appendString(GACommonParameter.datasource.param(),
							aPoint.getDataSource().replace(" ", "%20")));
				if (aPoint.getExceptionDesc() != null && GACommonParameter.exceptiondesc.param() != null)
					paramsUrls.append(appendString(GACommonParameter.exceptiondesc.param(),
							aPoint.getExceptionDesc().replace(" ", "%20")));
				if (aPoint.isFatal()) {
					paramsUrls.append(appendString(GACommonParameter.isexceptionfatal.param(), "1"));
				} else {
					paramsUrls.append(appendString(GACommonParameter.isexceptionfatal.param(), "0"));
				}
			} else {
				if (checkNotForNull(aPoint.getSession())) {

					if (aPoint.getSession().equals(GASessionValue.start.param())) {
						paramsUrls.append(appendString(GACommonParameter.sessionstart.param(), aPoint.getSession()));
					} else {
						paramsUrls.append(appendString(GACommonParameter.sessionend.param(), aPoint.getSession()));
					}
				}

				// Even Category
				if (checkNotForNull(aPoint.getEventCategory()) && aPoint.getEventCategory().trim().length() > 0) {

					paramsUrls.append(appendString(GARequiredParameter.hittype.param(), GAHitTypes.typeE.type()));
					paramsUrls.append(appendString(GACommonParameter.eventcategory.param(), aPoint.getEventCategory()));

					if (checkNotForNull(aPoint.getEventAction())) {
						paramsUrls.append(appendString(GACommonParameter.eventaction.param(), aPoint.getEventAction()));
					}
					if (checkNotForNull(aPoint.getEventLabel())) {
						paramsUrls.append(appendString(GACommonParameter.eventlabel.param(), aPoint.getEventLabel()));
					}
					if (checkNotForNull(aPoint.getEventValue())) {
						paramsUrls.append(appendString(GACommonParameter.eventvalue.param(), aPoint.eventValue));
					}
				}
			}
		}

		if (aPoint.getExceptionDesc() == null) {
			paramsUrls.append(appendString(GACommonParameter.anonymizeip.param(), "1"));

			paramsUrls.append(appendString(GACommonParameter.cachebuster.param(), "" + random.nextInt()));
		}

		return paramsUrls.toString();
	}

	private boolean checkNotForNull(String string) {
		if (string == null) {
			return false;
		} else {
			return true;
		}
	}

}
