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
package com.att.aro.analytics;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Google Analytics Entry represent individual entries into google analytics 
 * for events, visit counts, exceptions, and timing.
 *
 */
public class GAEntry {
	private String name;
	private String category;
	private String action;
	private String label;
	private String value = "0";
	
	private HitType hitType;
	private String exceptionDesc;
	private String dataSource;
	private String session;
	private boolean isFatal;

	public GAEntry() {

	}

	public GAEntry(String name) {
		this.name = name;
	}

	public GAEntry(String name, String category, HitType hitType) {
		this.name = name;
		this.category = category;
		this.hitType = hitType;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getExceptionDesc() {
		return exceptionDesc;
	}

	public void setExceptionDesc(String exceptionDesc) {
		this.exceptionDesc = exceptionDesc;
	}

	public boolean isFatal() {
		return isFatal;
	}

	public void setFatal(boolean isFatal) {
		this.isFatal = isFatal;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCategory(String category) {
		this.category = this.encode(category);
	}

	public void setAction(String action) {
		this.action = this.encode(action);
	}

	public void setLabel(String label) {
		this.label = this.encode(label);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setSession(String session) {
		this.session = session;
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

	public String getSession() {
		return session;
	}

	public HitType getHitType() {
		return hitType;
	}

	public void setHitType(HitType hitType) {
		this.hitType = hitType;
	}

	public void resetParams() {
		this.action = null;
		this.category = null;
		this.label = null;
		this.value = "0";
		this.session = null;
	}

	public void resetSession() {
		this.session = null;
	}

	private String encode(String name) {
		try {
			return URLEncoder.encode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return name;
		}
	}

}
