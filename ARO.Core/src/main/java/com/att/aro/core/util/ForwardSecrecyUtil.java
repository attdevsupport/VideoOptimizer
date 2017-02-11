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

public class ForwardSecrecyUtil {

	public static enum ForwardSecrecyBlackList {
		
		NULL_WITH_NULL_NULL ("0x0000"),
		RSA_WITH_NULL_MD5 ("0x0001"),
		RSA_WITH_NULL_SHA ("0x0002"),
		RSA_EXPORT_WITH_RC4_40_MD5 ("0x0003"),
		RSA_WITH_RC4_128_MD5 ("0x0004"),
		RSA_WITH_RC4_128_SHA ("0x0005"),
		RSA_EXPORT_WITH_RC2_CBC_40_MD5 ("0x0006"),
		RSA_WITH_IDEA_CBC_SHA ("0x0007"),
		RSA_EXPORT_WITH_DES40_CBC_SHA ("0x0008"),
		RSA_WITH_DES_CBC_SHA ("0x0009"),
		RSA_WITH_3DES_EDE_CBC_SHA ("0x000a"),
		FORTEZZA_KEA_WITH_NULL_SHA ("0x001c"),
        FORTEZZA_KEA_WITH_FORTEZZA_CBC_SHA ("0x001d"),
        KRB5_WITH_DES_CBC_SHA ("0x001e"),
        KRB5_WITH_3DES_EDE_CBC_SHA ("0x001f"),
        KRB5_WITH_RC4_128_SHA ("0x0020"),
        KRB5_WITH_IDEA_CBC_SHA ("0x0021"),
        KRB5_WITH_DES_CBC_MD5 ("0x0022"),
        KRB5_WITH_3DES_EDE_CBC_MD5 ("0x0023"),
        KRB5_WITH_RC4_128_MD5 ("0x0024"),
        KRB5_WITH_IDEA_CBC_MD5 ("0x0025"),
        KRB5_EXPORT_WITH_DES_CBC_40_SHA ("0x0026"),
        KRB5_EXPORT_WITH_RC2_CBC_40_SHA ("0x0027"),
        KRB5_EXPORT_WITH_RC4_40_SHA ("0x0028"),
        KRB5_EXPORT_WITH_DES_CBC_40_MD5 ("0x0029"),
        KRB5_EXPORT_WITH_RC2_CBC_40_MD5 ("0x002a"),
        KRB5_EXPORT_WITH_RC4_40_MD5 ("0x002b"),
        PSK_WITH_NULL_SHA ("0x002c"),
		RSA_PSK_WITH_NULL_SHA ("0x002e"),
        RSA_WITH_AES_128_CBC_SHA ("0x002f"),
        RSA_WITH_AES_256_CBC_SHA ("0x0035"),
        SRP_SHA_WITH_3DES_EDE_CBC_SHA ("0xc01a"),
        SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA ("0xc01b"),
        SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA ("0xc01c"),
        SRP_SHA_WITH_AES_128_CBC_SHA ("0xc01d"),
        SRP_SHA_RSA_WITH_AES_128_CBC_SHA ("0xc01e"),
        SRP_SHA_DSS_WITH_AES_128_CBC_SHA ("0xc01f"),
        SRP_SHA_WITH_AES_256_CBC_SHA ("0xc020"),
        SRP_SHA_RSA_WITH_AES_256_CBC_SHA ("0xc021"),
        SRP_SHA_DSS_WITH_AES_256_CBC_SHA ("0xc022");
		
		private final String cipherHex;
		
		private ForwardSecrecyBlackList(String cipherHex) {
			this.cipherHex = cipherHex;
		}
		
		public String toString() {
			return cipherHex;
		}
	}
	
	private static Map<String, ForwardSecrecyBlackList> hexToIdentifier;
	
	static {
		if (hexToIdentifier == null) {
			hexToIdentifier = new ConcurrentHashMap<String, ForwardSecrecyBlackList>();
			ForwardSecrecyBlackList[] list = ForwardSecrecyBlackList.class.getEnumConstants();
			for(ForwardSecrecyBlackList item : list) {
				hexToIdentifier.put(item.toString(), item);
			}
		}
	}
	
	public static void init() {
		hexToIdentifier = new ConcurrentHashMap<String, ForwardSecrecyBlackList>();
		ForwardSecrecyBlackList[] list = ForwardSecrecyBlackList.class.getEnumConstants();
		for(ForwardSecrecyBlackList item : list) {
			hexToIdentifier.put(item.toString(), item);
		}
	}
	
	public static boolean containsKey(String cipherHex) {
		return hexToIdentifier.containsKey(cipherHex);
	}
	
	public static ForwardSecrecyBlackList getCipherIdentifier(String cipherHex) {
		return hexToIdentifier.get(cipherHex);
	}
}
