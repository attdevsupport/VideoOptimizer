
package com.att.aro.core.bestpractice.impl;


public class UIParserNode {

	String classType;
	
	String packageType;
	
	String index;
	
	String bounds;

	public UIParserNode(String bounds, String classType, String index, String packageType) {
		
		this.classType= classType;
		
		this.packageType= packageType;
		
		this.index= index;
		
		this.bounds=bounds;// TODO Auto-generated constructor stub
	}

	public String getClassType() {
		return classType;
	}

	public String getPackageType() {
		return packageType;
	}

	public String getIndex() {
		return index;
	}

	public String getBounds() {
		return bounds;
	}

	

}