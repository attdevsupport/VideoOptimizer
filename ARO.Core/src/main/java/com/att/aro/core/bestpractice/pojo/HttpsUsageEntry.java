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

public class HttpsUsageEntry {

	private String ipAddress;
	private String parentDomainName;
	private int totalNumHttpConnections;
	private int totalNumConnections;
	private BigDecimal httpConnectionsPercentage;
	private BigDecimal totalHttpTrafficInKB;
	private BigDecimal totalTrafficInKB;
	private BigDecimal httpTrafficPercentage;
	
	public HttpsUsageEntry(String ipAddress, String parentDomainName,
			int totalNumConnections, int totalNumHttpConnections,
			BigDecimal httpConnectionsPercentage,
			BigDecimal totalTrafficInKB, BigDecimal totalHttpTrafficInKB,
			BigDecimal httpTrafficPercentage) {
		
		this.ipAddress = ipAddress;
		this.parentDomainName = parentDomainName;
		this.totalNumConnections = totalNumConnections;
		this.totalNumHttpConnections = totalNumHttpConnections;
		this.httpConnectionsPercentage = httpConnectionsPercentage;
		this.totalTrafficInKB = totalTrafficInKB;
		this.totalHttpTrafficInKB = totalHttpTrafficInKB;		
		this.httpTrafficPercentage =  httpTrafficPercentage;
	}
	
	/**
	 * Gets the IP address.
	 * 
	 * @return IP address
	 */
	public String getIPAddress() {
		return ipAddress;
	}
	
	/**
	 * Gets the parent domain name.
	 * This method will return the second-level domain and the top-level domain. 
	 * Output examples: "google.com", "outlook.com"
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
	 * Gets the total number of connections which 
	 * can be HTTP connections or HTTPS connection.
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
	public String getHttpConnectionsPercentage() {
		return Util.formatDecimal(httpConnectionsPercentage, 3, 0);
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
	 * Gets the total traffic, which can be 
	 * HTTP traffic or HTTPS traffic, in KB.
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
	public String getTotalHttpTrafficPercentage() {
		return Util.formatDecimal(httpTrafficPercentage, 3, 0);
	}
	
}
