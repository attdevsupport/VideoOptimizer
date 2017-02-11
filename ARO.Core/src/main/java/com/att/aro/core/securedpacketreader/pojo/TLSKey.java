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
package com.att.aro.core.securedpacketreader.pojo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TLSKey {

	/**
	 * regexp for the tlsKey prefix. Note that the parentheses need to be escaped, and inside the parenthesis, there might be some white space before the
	 * number. The format is: F/_TLSKEY_(30914): HDGINJIGIMILNEEBIMO...
	 */
	private static final String TLSKEYLOGPREFIXREGEXP = "F/_TLSKEY_\\(\\s*\\d+\\):\\s+";
	private static final String TLSKEYREGEXP = "([a-z]+)";
	private static final int TLSKEY_GROUP = 1;
	private static final Pattern LOGCATPATTERN = Pattern.compile(TLSKEYLOGPREFIXREGEXP + TLSKEYREGEXP, Pattern.CASE_INSENSITIVE);

	private static final int MASTER_LEN = 48;
	private static final int TS_LEN = 8;

	byte[] timestamp = new byte[TS_LEN];
	byte[] master = new byte[MASTER_LEN];
	byte[] preMaster;
	private int preMasterLen = 0;

	public TLSKey(String logcatStr) {
		String transcodedStr = getTranscodedKey(logcatStr); // BCIDBACCLPNHNFEBGIKGILJJEADIJEDAMOPAPNJFLCGBGBOBALPHPDAIGAFFHHFJEEKJNJDLJLFHFIAKIGFPJLNFHGMKNIDMIKNOKEEANJBJFEFGADABPIEACENIPPBGLCDGOJGFHNLAMJPJEGDKCACCNBIKHHAMIKFIAIIKFMCKLLPPJICPNCAOBDFDLBONHJDNPEJHNEFKJOLI
		final int keyLen = transcodedStr.length();
		preMasterLen = (keyLen - (TS_LEN + MASTER_LEN) * 2) / 2;

		int tsEndIndexInc = (TS_LEN * 2) - 1;
		untranscode(transcodedStr, 0, tsEndIndexInc, timestamp);

		int masterStartIndexInc = tsEndIndexInc + 1;
		int masterEndIndexInc = masterStartIndexInc + (MASTER_LEN * 2) - 1;
		untranscode(transcodedStr, masterStartIndexInc, masterEndIndexInc, master);

		int preMasterStartIndexInc = masterEndIndexInc + 1;
		preMaster = new byte[preMasterLen];
		untranscode(transcodedStr, preMasterStartIndexInc, keyLen - 1, preMaster);
	}


	private String getTranscodedKey(String logcatStr) {
		Matcher matcher = LOGCATPATTERN.matcher(logcatStr);
		if (matcher.find()) {
			return matcher.group(TLSKEY_GROUP);
		}
		return null;
	}

	/**
	 * When the key was dumped to logcat, it was transcoded to printable character in ssl/t1_enc.c; so here we have to undo the transcoding.
	 * 
	 * @param transcodedStr
	 * @param startIndex:
	 *            inclusive start index
	 * @param endIndex:
	 *            inclusive end index
	 * @return
	 */
	private boolean untranscode(String transcodedStr, int startIndex, int endIndex, byte[] result) {
		int strIndex = startIndex, keyIndex = 0;

		while (strIndex <= endIndex) {
			char curChar = transcodedStr.charAt(strIndex);
			char nextChar = transcodedStr.charAt(strIndex + 1);
			// logger.debug(TAG, String.format("strIndex=%d, endIndex=%d, keyIndex=%d", strIndex, endIndex, keyIndex));

			if (isValidRange(curChar) && isValidRange(nextChar)) {
				result[keyIndex] = (byte) ((curChar - 'A') * 16 + (nextChar - 'A'));
				strIndex += 2; // advance 2 spots since we processed 2 chars
				keyIndex++;
			} else {
				return false;
			}

		}
		return true;
	}

	private boolean isValidRange(char curChar) {
		return curChar >= 'A' && curChar <= 'P';
	}

	public byte[] getTimestamp() {
		return timestamp;
	}

	public byte[] getMaster() {
		return master;
	}

	public byte[] getPreMaster() {
		return preMaster;
	}

	public int getPreMasterLen() {
		return preMasterLen;
	}

}
