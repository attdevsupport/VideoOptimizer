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
package com.att.aro.core.packetreader.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ILogger;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetreader.IPcapngHelper;

public class PcapngHelperImpl implements IPcapngHelper {

	private String prevFilePath;
	private long prevLastModifyTime = 0;
	private String hardware = "";
	String osname = "";
	String appname = "";
	int osVersion = 0;
	int osMajor = 0;
	int appVersion = 0;
	private boolean applePcapNG;

	@Autowired
	private IFileManager fileManager;

	@InjectLogger
	private static ILogger logger;

	/**
	 * check pcapfile header to see if it is created by Apples tcpdump
	 * 
	 * @return true if pcapng, false otherwise
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Override
	public boolean isApplePcapng(File pcapfile) throws FileNotFoundException {
		// reuse previous result if the same file is passed in for calculation
		if (pcapfile.lastModified() == this.prevLastModifyTime 
			&& pcapfile.getAbsolutePath().equals(this.prevFilePath)) {
			return isApplePcapNG();
		}

		this.prevFilePath = pcapfile.getAbsolutePath();
		this.prevLastModifyTime = pcapfile.lastModified();

		FileInputStream stream = (FileInputStream) fileManager.getFileInputStream(pcapfile);
		int max = 2048;
		int length = (int) (pcapfile.length() < max ? pcapfile.length() : max);
		byte[] data = getByteArrayFromStream(stream, length);
		return data != null ? isApplePcapng(data) : false;
	}

	/**
	 * check pcapfile header to see if it is created with no link layer
	 * 
	 * @return true or false
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Override
	public boolean isNoLinkLayer(String pcapfile) throws FileNotFoundException {

		FileInputStream stream = (FileInputStream) fileManager.getFileInputStream(pcapfile);

		int max = 32;
		int length = (int) (pcapfile.length() < max ? pcapfile.length() : max);
		byte[] data = getByteArrayFromStream(stream, length);

		boolean result = false;
		ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
		int blocktype = 0xA1B2C3D4;
		int type = buffer.getInt();
		if (type != blocktype) {
			return result;
		}
		int optionCode = buffer.getShort();
		int optionLen = buffer.getShort();
		if (optionCode == 2 && optionLen == 4) {
			int val = buffer.getInt();
			if (val == 0) {
				return true;
			}
		}
		return false;
	}

	public byte[] getByteArrayFromStream(FileInputStream stream, int size) {
		byte[] data = null;
		if (stream != null && size > 0) {
			data = new byte[size];
			try {
				stream.read(data);
			} catch (IOException e) {
				logger.error("failed to read fileStream");
				return null;
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					logger.error("failed to close fileStream");
				}
			}
		}
		return data;
	}

	public boolean isApplePcapng(byte[] data) {
		setApplePcapNG(false);
		if (data != null) {
			ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
			int blocktype = 0x0A0D0D0A;
			int type = buffer.getInt();
			setApplePcapNG(type == blocktype);
			return isApplePcapNG();
		}
		return false;
	}

	@Override
	public String getHardware() {
		return hardware;
	}

	@Override
	public String getOs() {
		return osname;
	}

	@Override
	public boolean isApplePcapng(String filepath) throws FileNotFoundException {
		File file = new File(filepath);
		return this.isApplePcapng(file);
	}

	@Override
	public boolean isApplePcapNG() {
		return applePcapNG;
	}

	@Override
	public void setApplePcapNG(boolean applePcapNG) {
		this.applePcapNG = applePcapNG;
	}

}
