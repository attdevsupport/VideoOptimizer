/*
 *  Copyright 2022 AT&T
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

enum Category {
	Analyzer,
	Collection,
}

enum Action {
	//Analyzer
	StartApplication, EndApplication, 
	LoadTrace, LoadPcap,
	//Collectors
	StartVpn, EndVpn, StartIos, EndIos, StartRooted,EndRooted,
	//Misc - currently available in analytics
	VideoCaptured,
}

public class AnalyticsEvent {
	private String category;
	private String action;
	private String label;//Provides associated os version(PC, Mac, android, ios).
	private String value;

	public AnalyticsEvent(Category category, Action action) {
		this.category = category.name();
		this.action = action.name();
	}
	
	public AnalyticsEvent(Category category, Action action, String label) {
		this.category = category.name();
		this.action = action.name();
		this.label = label;
	}

	public AnalyticsEvent(String category, String action, String label, String value) {
		this.category = category;
		this.action = action;
		this.label = label;
		this.value = value;
	}

	public String getCategory() {
		return category;
	}

	public String getAction() {
		return action;
	}

	public String getLabel() {
		return label;
	}

	public String getValue() {
		return value;
	}

}
