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
package com.att.aro.core.securedpacketreader.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.securedpacketreader.CryptoAdapter;
import com.att.aro.core.securedpacketreader.ICrypto;
import com.att.aro.core.securedpacketreader.IReceiveSSLKey;
import com.att.aro.core.securedpacketreader.pojo.SSLKey;
import com.att.aro.core.util.Util;

/**
 * call native lib to read SSL key, and provide functions to decrypt data
 * 
 * Date: April 10, 2014
 */
public class CryptoImpl implements ICrypto, IReceiveSSLKey {

	@InjectLogger
	private static ILogger logger;

	boolean vpnKey;
	List<SSLKey> sslkeys;
	CryptoAdapter adapter;

	public CryptoImpl() {
		sslkeys = new ArrayList<SSLKey>();
	}

	public void setAdapter(CryptoAdapter adapter) {
		this.adapter = adapter;
	}

	void ensureAdapter() {
		if (adapter == null) {
			adapter = new CryptoAdapter();
			adapter.setSubscriber(this);
			adapter.loadAroCryptoLib();
		    //_logger.info("loaded");
		}
	}
	
	@Override
	public boolean isVpnKey() {
		return vpnKey;
	}
	
	@Override
	public void setVpnKey(boolean vpnKey) {
		this.vpnKey = vpnKey;
	}

	@Override
	public int readSSLKeys(String filename) {
		ensureAdapter();
	    //_logger.info("adapter.readSSLKeys("+filename+");");
		int keysResult = adapter.readSSLKeys(filename);
		//hack to emulate the original 4.1.1, removing the duplicate at end
		// fix AROCrypto.cpp for a correct fix
		if (keysResult!=-1){
			SSLKey key = sslkeys.remove(sslkeys.size()-1);
			
			byte[] preMaster = key.getPreMaster();
			vpnKey = true;
			for (int i=0;i<4;i++) {
				if(preMaster[i]!=0) {
					vpnKey = false;
					break;
				}
			}			
		}
		
		Collections.sort(sslkeys);
		return keysResult;
	}

	@Override
	public int cryptoCipherInit(int alg, byte[] temp1, byte[] temp2, int keymaterial, int bClient) {
		ensureAdapter();
	    //_logger.info("adapter.cryptocipherinit("+alg+", "+temp1+", "+temp2+", "+keymaterial+", "+bClient+");");
		return adapter.cryptocipherinit(alg, temp1, temp2, keymaterial, bClient);
	}

	@Override
	public void cryptoCipherDeinit(int objectType) {
		ensureAdapter();
	    //_logger.info("adapter.cryptocipherdeinit("+objectType+");");
		adapter.cryptocipherdeinit(objectType);
	}

	@Override
	public void setCryptoCipherNull(int objectType, int bClient) {
		ensureAdapter();
	    //_logger.info("adapter.setcryptociphernull("+objectType+", "+bClient+");");
		adapter.setcryptociphernull(objectType, bClient);
	}

	@Override
	public int cryptoCipherDecrypt(int pCipher, byte[] enc, byte[] plain, int enclength, int bClient) {
		ensureAdapter();
	    //_logger.info("cryptoCipherDecrypt: \npCipher=" + pCipher + "\n, enc=" + Util.byteArrayToString(enc, enclength) + "\n, plain=" + plain.length + "\n, enclength=" + enclength);
	    //_logger.info("adapter.cryptocipherdecrypt(" + pCipher + ", " + enc + ", " + plain + ", " + enclength + ", " + bClient + ");");
		return adapter.cryptocipherdecrypt(pCipher, enc, plain, enclength, bClient);
	}

	@Override
	public void copyCryptoCipher(int fromObjectType, int toObjectType) {
		ensureAdapter();
		adapter.copycryptocipher(fromObjectType, toObjectType);
	}

	@Override
	public int cryptoHashInitUpdateFinish(int dir, int hashAlg, byte[] keyBlock, int hashSize, int recType, int payloadLen, byte[] plain, byte[] seqNum) {
		ensureAdapter();
	    //_logger.info("adapter.cryptohashInitUpdateFinish(" + dir + ", " + hashAlg + ", " + keyBlock + ", " + hashSize + ", " + recType + ", " + payloadLen + ", " + plain + ", " + seqNum + ");");
		return adapter.cryptohashInitUpdateFinish(dir, hashAlg, keyBlock, hashSize, recType, payloadLen, plain, seqNum);
	}

	@Override
	public void handleSSLKey(double tsvalue, int preMasterLen, byte[] preMaster, byte[] master) {
	    logger.info( "sslkey ts:" + String.format("%.3f", tsvalue) 
						 + " PREMASTER:" + Util.byteArrayToHex(preMaster)
						 + " MASTER:" + Util.byteArrayToHex(master)
						 );
		SSLKey key = new SSLKey();
		key.setbUsed(0);
		key.setTs(tsvalue);
		key.setPreMasterLen(preMasterLen);
		key.setPreMaster(preMaster);
		key.setMasterLen(master.length);
		key.setMaster(master);
		sslkeys.add(key);
	}

	@Override
	public List<SSLKey> getSSLKeyList() {
		return sslkeys;
	}

	@Override
	public void resetSSLKeyUsage() {
		for (SSLKey key : sslkeys) {
			key.setbUsed(0);
		}
	}

}
