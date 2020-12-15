/*
 *  Copyright 2020 AT&T
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
package com.att.aro.core.videoanalysis.pojo;

import lombok.Data;

@Data
public class UrlMatchDef {

	public static final String URLMATCHDEF = "UrlMatchDef";
	private boolean prefix = false;
	private int urlMatchLen = 0;
	private UrlMatchType urlMatchType = UrlMatchType.UNKNOWN;
	
	public enum UrlMatchType {
		FULL, COUNT, UNKNOWN
	}

	public boolean checkLength(int length) {
		return urlMatchLen == length;
	}

	@Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final UrlMatchDef other = (UrlMatchDef) obj;

        if (this.urlMatchType != other.urlMatchType) {
            return false;
        }

        if (this.urlMatchLen != other.urlMatchLen) {
            return false;
        }

        if (!this.prefix ^ other.prefix) {
            return false;
        }

        return true;
    }

	@Override
	public int hashCode() {
	    return Boolean.hashCode(prefix) + Integer.hashCode(urlMatchLen) + urlMatchType.hashCode();
	}

	@Override
	public String toString() {
	    return String.format("prefix: %s, length: %d, type: %s", prefix, urlMatchLen, urlMatchType.name());
	}
}
