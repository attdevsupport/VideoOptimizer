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
package com.att.aro.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeakCipherUtil {

	public static enum WeakCipherBlackList {
		NULL_WITH_NULL_NULL ("0x0000"),
		RSA_WITH_NULL_MD5 ("0x0001"),
		RSA_WITH_NULL_SHA ("0x0002"),
		RSA_EXPORT_WITH_RC4_40_MD5 ("0x0003"), 
		RSA_EXPORT_WITH_RC2_CBC_40_MD5 ("0x0006"),
		RSA_EXPORT_WITH_DES40_CBC_SHA ("0x0008"),
		DH_DSS_EXPORT_WITH_DES40_CBC_SHA ("0x000b"),
		DH_RSA_EXPORT_WITH_DES40_CBC_SHA ("0x000e"),
		DHE_DSS_EXPORT_WITH_DES40_CBC_SHA ("0x0011"),
		DHE_RSA_EXPORT_WITH_DES40_CBC_SHA ("0x0014"), 
		DH_anon_EXPORT_WITH_RC4_40_MD5 ("0x0017"), 
		DH_anon_EXPORT_WITH_DES40_CBC_SHA ("0x0019"), 
		FORTEZZA_KEA_WITH_NULL_SHA ("0x001c"), 
		KRB5_EXPORT_WITH_DES_CBC_40_SHA ("0x0026"), 
		KRB5_EXPORT_WITH_RC2_CBC_40_SHA ("0x0027"), 
		KRB5_EXPORT_WITH_RC4_40_SHA ("0x0028"), 
		KRB5_EXPORT_WITH_DES_CBC_40_MD5 ("0x0029"), 
		KRB5_EXPORT_WITH_RC2_CBC_40_MD5 ("0x002a"), 
		KRB5_EXPORT_WITH_RC4_40_MD5 ("0x002b"), 
		PSK_WITH_NULL_SHA ("0x002c"), 
		DHE_PSK_WITH_NULL_SHA ("0x002d"), 
		RSA_PSK_WITH_NULL_SHA ("0x002e"), 
		ECDH_ECDSA_WITH_NULL_SHA ("0xc001"), 
		ECDHE_ECDSA_WITH_NULL_SHA ("0xc006"), 
		ECDH_RSA_WITH_NULL_SHA ("0xc00b"), 
		ECDHE_RSA_WITH_NULL_SHA ("0xc010"), 
		ECDH_anon_WITH_NULL_SHA ("0xc015"), 
		ECDHE_PSK_WITH_NULL_SHA ("0xc039"), 
		ECDHE_PSK_WITH_NULL_SHA256 ("0xc03a"), 
		ECDHE_PSK_WITH_NULL_SHA384 ("0xc03b");
		
		private final String cipherHex;
		
		private WeakCipherBlackList(String cipherHex) {
			this.cipherHex = cipherHex;
		}
		
		public String toString() {
			return cipherHex;
		}
	}
	
	private static Map<String, WeakCipherBlackList> hexToIdentifier;
	
	static {
		init();
	}

	public static void init() {
		hexToIdentifier = new ConcurrentHashMap<String, WeakCipherBlackList>();
		WeakCipherBlackList[] list = WeakCipherBlackList.class.getEnumConstants();
		for(WeakCipherBlackList weakCipher : list) {
			hexToIdentifier.put(weakCipher.toString(), weakCipher);
		}
	}
	
	public static boolean containsKey(String cipherHex) {
		return hexToIdentifier.containsKey(cipherHex);
	}
	
	public static WeakCipherBlackList getCipherIdentifier(String cipherHex) {
		return hexToIdentifier.get(cipherHex);
	}
}
