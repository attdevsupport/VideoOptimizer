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
