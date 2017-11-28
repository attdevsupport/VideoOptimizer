package com.att.aro.ui.view.menu.file;

public class VideoPreferenceInfo {

	String bestPractice;
	String warningCriteria;
	String failCriteria;
	int warningCriteriaInt;
	int failCriteriaInt;
	
	public VideoPreferenceInfo(String bestPractice, String warningCriteria,
			String failCriteria) {
		this.bestPractice = bestPractice;
		this.warningCriteria = warningCriteria;
		this.failCriteria = failCriteria;
	}
	
	public VideoPreferenceInfo(String bestPractice, int warningCriteriaInt,
			int failCriteriaInt) {
		this.bestPractice = bestPractice;
		this.warningCriteriaInt = warningCriteriaInt;
		this.failCriteriaInt = failCriteriaInt;
	}
	
	public String getBestPractice() {
		return bestPractice;
	}

	public void setBestPractice(String bestPractice) {
		this.bestPractice = bestPractice;
	}

	public String getWarningCriteria() {
		return warningCriteria;
	}

	public void setWarningCriteria(String warningCriteria) {
		this.warningCriteria = warningCriteria;
	}

	public String getFailCriteria() {
		return failCriteria;
	}

	public void setFailCriteria(String failCriteria) {
		this.failCriteria = failCriteria;
	}

	public int getWarningCriteriaInt() {
		return warningCriteriaInt;
	}

	public void setWarningCriteriaInt(int warningCriteriaInt) {
		this.warningCriteriaInt = warningCriteriaInt;
	}

	public int getFailCriteriaInt() {
		return failCriteriaInt;
	}

	public void setFailCriteriaInt(int failCriteriaInt) {
		this.failCriteriaInt = failCriteriaInt;
	}
	
}
