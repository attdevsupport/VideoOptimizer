/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.bestpractice.pojo;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Http3xxCodeResult extends AbstractBestPracticeResult {
	@JsonIgnore
	private SortedMap<Integer, Integer> httpRedirectCounts3XX;
	@JsonIgnore
	private Map<Integer, HttpRequestResponseInfo> firstResMap;
	List<HttpCode3xxEntry> http3xxResCode;
	@JsonIgnore
	private String exportAllHttpError;
	
	public String getExportAllHttpError() {
		return exportAllHttpError;
	}
	public void setExportAllHttpError(String exportAllHttpError) {
		this.exportAllHttpError = exportAllHttpError;
	}
	public SortedMap<Integer, Integer> getHttpRedirectCounts3XX() {
		return httpRedirectCounts3XX;
	}
	public void setHttpRedirectCounts3XX(
			SortedMap<Integer, Integer> httpRedirectCounts3XXMaps) {
		this.httpRedirectCounts3XX = httpRedirectCounts3XXMaps;
	}
	public Map<Integer, HttpRequestResponseInfo> getFirstResMap() {
		return firstResMap;
	}
	public void setFirstResMap(Map<Integer, HttpRequestResponseInfo> firstResMap) {
		this.firstResMap = firstResMap;
	}
	
	public List<HttpCode3xxEntry> getHttp3xxResCode() {
		return http3xxResCode;
	}
	public void setHttp3xxResCode(List<HttpCode3xxEntry> http3xxResCode) {
		this.http3xxResCode = http3xxResCode;
	}
	@Override
	public BestPracticeType getBestPracticeType() {
		return BestPracticeType.HTTP_3XX_CODE;
	}
	
	public int getErrorCount() {
		return httpRedirectCounts3XX != null ? httpRedirectCounts3XX.size() : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Http3xxCodeResult other = (Http3xxCodeResult) obj;
		if (!other.getHttp3xxResCode().containsAll(http3xxResCode)) {
			return false;
		}
		if ((!other.getBestPracticeDescription().trim().equals(getBestPracticeDescription().trim()))
				|| getResultType() != other.getResultType()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (HttpCode3xxEntry entry : http3xxResCode) {
			result = prime * result + entry.hashCode();
		}
		result = prime * result + getBestPracticeDescription().hashCode();
		result = prime * result + getBestPracticeType().hashCode();
		return result;
	}
}
