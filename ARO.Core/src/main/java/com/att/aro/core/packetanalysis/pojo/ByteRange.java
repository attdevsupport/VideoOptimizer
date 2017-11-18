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
package com.att.aro.core.packetanalysis.pojo;

public class ByteRange {
	private Integer beginByte;
	private Integer endByte;

	public ByteRange(Integer beginByte, Integer endByte) {
		this.beginByte = beginByte;
		this.endByte = endByte;
	}

	public ByteRange(String byteStart, String byteEnd) {
		this.beginByte = Integer.valueOf(byteStart);
		this.endByte = Integer.valueOf(byteEnd);
	}

	public Integer getBeginByte() {
		return beginByte;
	}

	public Integer getEndByte() {
		return endByte;
	}

	public String getBeginByteHex() {
		return Integer.toHexString(beginByte.intValue());
	}

	public String getEndByteHex() {
		return Integer.toHexString(endByte.intValue());
	}
	
	/**
	 * @return false if nulls are involved or value don't match, true is values match
	 */
	public boolean isValidRange() {
		return beginByte != null ? !beginByte.equals(endByte) : false;
	}
	
	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(17);
		strblr.append(String.format("%08d",beginByte));
		strblr.append('-');
		strblr.append(String.format("%08d",endByte));
		return strblr.toString();
	}
}
