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
package com.att.aro.core.bestpractice.pojo;

import java.math.BigDecimal;

import com.att.aro.core.util.Util;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HttpsUsageEntry {
	private String ipAddress;
	private String parentDomainName;
	private int totalNumHttpConnections;
	private int totalNumConnections;
	private int httpConnectionsPercentage;
	private BigDecimal totalHttpTrafficInKB;
	private BigDecimal totalTrafficInKB;
	private int httpTrafficPercentage;

	public HttpsUsageEntry(){}
	
	public HttpsUsageEntry(String ipAddress, String parentDomainName, int totalNumConnections,
			int totalNumHttpConnections, int httpConnectionsPercentage2, BigDecimal totalTrafficInKB,
			BigDecimal totalHttpTrafficInKB, int httpTrafficPercentage) {
		this.ipAddress = ipAddress;
		this.parentDomainName = parentDomainName;
		this.totalNumConnections = totalNumConnections;
		this.totalNumHttpConnections = totalNumHttpConnections;
		this.httpConnectionsPercentage = httpConnectionsPercentage2;
		this.totalTrafficInKB = totalTrafficInKB;
		this.totalHttpTrafficInKB = totalHttpTrafficInKB;
		this.httpTrafficPercentage = httpTrafficPercentage;
	}

	/**
	 * Gets the IP address.
	 *
	 * @return IP address
	 */
	public String getIPAddress() {
		return ipAddress;
	}

	@JsonProperty(value = "ipaddress")
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;

	}
	/**
	 * Gets the parent domain name. This method will return the second-level
	 * domain and the top-level domain. Output examples: "google.com",
	 * "outlook.com"
	 *
	 * @return parent domain Name
	 */
	public String getParentDomainName() {
		return parentDomainName;
	}

	/**
	 * Gets the total number of HTTP connections.
	 *
	 * @return total number of HTTP connections
	 */
	public int getTotalNumHttpConnections() {
		return totalNumHttpConnections;
	}

	/**
	 * Gets the total number of connections which can be HTTP connections or
	 * HTTPS connection.
	 *
	 * @return total number of connections
	 */
	public int getTotalNumConnections() {
		return totalNumConnections;
	}

	/**
	 * Gets the percentage of connections that is HTTP.
	 *
	 * @return HTTP connections percentage
	 */
	public Integer getHttpConnectionsPercentage() {
		return httpConnectionsPercentage;
	}

	/**
	 * Gets the total HTTP traffic in KB.
	 *
	 * @return total HTTP traffic in KB
	 */
	public String getTotalHttpTrafficInKB() {
		return Util.formatDecimal(totalHttpTrafficInKB, 3, 0);
	}

	/**
	 * Gets the total traffic, which can be HTTP traffic or HTTPS traffic, in
	 * KB.
	 *
	 * @return total traffic in KB
	 */
	public String getTotalTrafficInKB() {
		return Util.formatDecimal(totalTrafficInKB, 3, 0);
	}

	/**
	 * Gets the percentage of traffic that is HTTP.
	 *
	 * @return HTTP traffic percentage
	 */
	public Integer getTotalHttpTrafficPercentage() {
		return httpTrafficPercentage;
	}

	@JsonProperty(value = "totalHttpTrafficPercentage")
	public void setHttpTrafficPercentage(int httpTrafficPercentage) {
		this.httpTrafficPercentage = httpTrafficPercentage;

	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		HttpsUsageEntry other = (HttpsUsageEntry) obj;
		if(other.getHttpConnectionsPercentage() != httpConnectionsPercentage || other.getTotalHttpTrafficPercentage() != httpTrafficPercentage){
			return false;
		}
		if(other.getTotalNumConnections() != totalNumConnections || other.getTotalNumHttpConnections() != totalNumHttpConnections){
			return false;
		}
		if(!other.getTotalHttpTrafficInKB().equals(Util.formatDecimal(totalHttpTrafficInKB, 3, 0))){
			return false;
		}
		if(!other.getTotalTrafficInKB().equals(Util.formatDecimal(totalTrafficInKB, 3, 0))){
			return false;
		}
		if(!other.getIPAddress().equals(ipAddress) || (!other.getParentDomainName().equals(parentDomainName))){
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + totalNumConnections;
		result = prime * result + totalNumHttpConnections;
		result = prime * result + httpTrafficPercentage;
		result = prime * result + httpConnectionsPercentage;
		result = prime * result + totalHttpTrafficInKB.hashCode();
		result = prime * result + totalTrafficInKB.hashCode();
		result = prime * result + ipAddress.hashCode();
		result = prime * result + parentDomainName.hashCode();
		return result;
	}
}
