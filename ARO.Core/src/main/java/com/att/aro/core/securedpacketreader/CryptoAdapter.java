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
package com.att.aro.core.securedpacketreader;

import com.att.aro.core.securedpacketreader.IReceiveSSLKey;
import com.att.aro.core.util.Util;

public class CryptoAdapter {

	public native int readSSLKeys(String filename);

	public native int cryptocipherinit(int alg, byte[] temp1, byte[] temp2, int key_material, int bClient);

	public native void cryptocipherdeinit(int objectType);

	public native void setcryptociphernull(int objectType, int bClient);

	public native int cryptocipherdecrypt(int pCipher, byte[] enc, byte[] _plain, int enclength, int bClient);

	public native void copycryptocipher(int from_objectType, int to_objectType);

	public native int cryptohashInitUpdateFinish(int dir, int hash_alg, byte[] keyBlock, int hash_size, int recType, int payloadLen, byte[] plain, byte[] seqNum);

	IReceiveSSLKey subscriber;

	public void setSubscriber(IReceiveSSLKey subscriber) {
		this.subscriber = subscriber;
	}

	/**
	 * Callback from readSSLKeys, redirect to the subscriber
	 * 
	 * @param ts
	 * @param preMasterLen
	 * @param preMaster
	 * @param master
	 */
	private void sslKeyHandler(double ts, int preMasterLen, byte[] preMaster, byte[] master) {
		if (subscriber != null) {
			this.subscriber.handleSSLKey(ts, preMasterLen, preMaster, master);
		}
	}

	public void loadAroCryptoLib() {
		String osname = System.getProperty("os.name");
		if (osname != null && osname.contains("Windows")) {
			String os = System.getProperty("os.arch");
			String depencyFileName = "libeay32.dll";
			String libFolder = Util.makeLibFilesFromJar(depencyFileName);
			Util.loadLibrary(depencyFileName, libFolder);
			if(os != null) {
				String libName = "AROCrypt";
				if (os.contains("64")) {
					libName = "AROCrypt64";
				} else {
					libName = "AROCrypt";
				}
				String fileName = libName + ".dll";
				Util.makeLibFilesFromJar(fileName);
				Util.loadSystemLibrary(libName);
			}
		} else if (osname != null && osname.contains("Mac")) {
			String filename = "AROCrypt";
			String aroCryptLibFileName = "lib" + filename + ".jnilib";
			String libFolder = Util.makeLibFilesFromJar(aroCryptLibFileName);
			Util.loadLibrary(aroCryptLibFileName, libFolder);
		}else if (osname != null && osname.contains("Linux")) {
			String filename = "AROCrypt";
			String aroCryptLibFileName = "lib" + filename + ".so";
			String libFolder = Util.makeLibFilesFromJar(aroCryptLibFileName);
			Util.loadLibrary(aroCryptLibFileName, libFolder);
		}
	}
}
