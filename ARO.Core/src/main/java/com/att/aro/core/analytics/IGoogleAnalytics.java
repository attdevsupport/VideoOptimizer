/*
 * Copyright 2017 AT&T
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
package com.att.aro.core.analytics;

public interface IGoogleAnalytics {

	void applicationInfo(String analyticsTracker, String applicationName, String applicationVersion);

	void sendAnalyticsEvents(String eventCategory, String eventAction);

	void sendAnalyticsEvents(String eventCategory, String eventAction, String eventLable);

	void sendAnalyticsEvents(String eventCategory, String eventAction, String eventLable, String eventValue);

	void sendAnalyticsStartSessionEvents(String eventCategory, String eventAction);

	void sendAnalyticsStartSessionEvents(String eventCategory, String eventAction, String eventLable);

	void sendAnalyticsStartSessionEvents(String eventCategory, String eventAction, String eventLable,
			String eventValue);

	void sendAnalyticsEndSessionEvents(String eventCategory, String eventAction);

	void sendAnalyticsEndSessionEvents(String eventCategory, String eventAction, String eventLable);

	void sendAnalyticsEndSessionEvents(String eventCategory, String eventAction, String eventLable, String eventValue);

	void sendExceptionEvents(String exceptionDesc, String source, boolean isFatal);

	void sendCrashEvents(String crashDesc, String source);
	
	void sendViews(String screen);

	void close();

}
