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

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;

public class FileOrderEntry extends HttpEntry {
	private int contentLength;
	public FileOrderEntry(HttpRequestResponseInfo hrri,
			HttpRequestResponseInfo lastRequestObj, String domainName) {
		super(hrri, lastRequestObj, domainName);
		this.contentLength = hrri.getContentLength();
	}
	/**
	 * Returns size of the file.
	 * 
	 * @return file size
	 */
	public Object getSize() {
		return contentLength;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		FileOrderEntry fileOrderEntry = (FileOrderEntry) obj;
		if((int)fileOrderEntry.getSize() != contentLength){
			return false;
		}

		if(fileOrderEntry.getHttpCode() != getHttpCode()){
			return false;
		}
		if(!fileOrderEntry.getHttpObjectName().equals(getHttpObjectName())){
			return false;
		}
		if(!fileOrderEntry.getHostName().equals(getHostName())){
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + contentLength;
		result = prime * result + (int) getHttpCode();
		result = prime * result + getHttpObjectName().hashCode();
		result = prime * result + getHostName().hashCode();
		return result;
	}
}
