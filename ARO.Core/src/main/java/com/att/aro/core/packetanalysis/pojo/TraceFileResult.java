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
package com.att.aro.core.packetanalysis.pojo;

/**
 * Trace data returned by ITraceDataReader when reading only from a pcap file (traffic.cap)
 * 
 * Date: October 23, 2014
 */
public class TraceFileResult extends AbstractTraceResult {

	public TraceFileResult() {
		super();
	}

	/**
	 * Return TraceResultType.TRACE_FILE to identify that this collected
	 * only from a pcap file (traffic.cap)
	 * 
	 * @return TraceResultType.TRACE_FILE
	 */
	@Override
	public TraceResultType getTraceResultType() {
		return TraceResultType.TRACE_FILE;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((getTraceResultType() == null) ? 0 : getTraceResultType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TraceFileResult other = (TraceFileResult) obj;
		if(!other.getTraceResultType().equals(getTraceResultType())){
			return false;
		}
		return true;
	}

}
