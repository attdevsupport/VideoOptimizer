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
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ILogger;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetreader.INativePacketSubscriber;
import com.att.aro.core.packetreader.IPacketListener;
import com.att.aro.core.packetreader.IPacketReader;
import com.att.aro.core.packetreader.IPacketService;
import com.att.aro.core.packetreader.IPcapngHelper;
import com.att.aro.core.packetreader.pojo.Packet;
import com.att.aro.core.util.Util;
import com.att.aro.pcap.PCapAdapter;

public class PacketReaderImpl implements IPacketReader, INativePacketSubscriber {

	@InjectLogger
	private static ILogger logger;

	@Autowired
	private IPacketService packetservice;

	@Autowired
	private IFileManager filemanager;
	
	@Autowired
	private IExternalProcessRunner extrunner;

	@Autowired
	IPcapngHelper pcapngHelper;
	
	private IPacketListener packetlistener;
	
	String aroJpcapLibName = null;
	String aroJpcapLibFileName = null;
	
	String aroWebPLibName = null;
	String aroWebPLibFileName = null;

	private String currentPacketfile = null;

	PCapAdapter adapter = null;

	String backupCapFileName = "backup.cap";
	private String unixExtn = ".so";
	public String windowsOS = "Windows";
	public String windowsExtn = ".dll";
	public String linuxOS = "Linux";

	public PacketReaderImpl() {

	}

	public void setAdapter(PCapAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public void readPacket(String packetfile, IPacketListener listener) throws IOException {

		if (aroJpcapLibName == null || aroWebPLibName==null) {
			setVOLibName();
		}
		
		currentPacketfile = packetfile;
		provisionalPcapConversion(packetfile);
		
		if (listener == null) {
			logger.error("PacketListener cannot be null");
			throw new IllegalArgumentException("PacketListener cannot be null");
		}

		this.packetlistener = listener;

		if (adapter == null) {
			adapter = new PCapAdapter();
			adapter.loadAroLib(aroWebPLibFileName, aroWebPLibName);
			adapter.loadAroLib(aroJpcapLibFileName, aroJpcapLibName);
			
		}

		adapter.setSubscriber(this);
		
		// jni - loopPacket(...) reads trace file sends data packets to PacketListener:packetArrived
		String result = adapter.readData(packetfile);

		// finish 
		if (result != null) {
			logger.debug("Result from executing all pcap packets: " + result);
			throw new IOException(result);
		}
		logger.debug("Created PCapAdapter");
	}

	public void setVOLibName() {
		setAroJpcapLibName(Util.OS_NAME, Util.OS_ARCHYTECTURE);
		setAroWebPLib(Util.OS_NAME, Util.OS_ARCHYTECTURE);
	}

	/**
	 * Sets ARO Jpcap DLL library name.
	 */
	public void setAroJpcapLibName(String osname, String osarch) {

		logger.info("OS: " + osname);

		logger.info("OS Arch: " + osarch);

		if (osname != null && osarch != null) {

			if (osname.contains(windowsOS) && osarch.contains("64")) { // _______ 64 bit Windows jpcap64.DLL
				aroJpcapLibName = "jpcap64";
				aroJpcapLibFileName = aroJpcapLibName + windowsExtn;

			} else if (osname.contains(windowsOS)) { // _________________________ 32 bit Windows jpcap.DLL
				aroJpcapLibName = "jpcap";
				aroJpcapLibFileName = aroJpcapLibName + windowsExtn;

			} else if (osname.contains(linuxOS) && osarch.contains("amd64")) { // 64 bit Linux libjpcap64.so
				aroJpcapLibName = "jpcap64";
				aroJpcapLibFileName = "lib" + aroJpcapLibName + unixExtn;

			} else if (osname.contains(linuxOS) && osarch.contains("i386")) { //  32 bit Linux libjpcap.so
				aroJpcapLibName = "jpcap32";
				aroJpcapLibFileName = "lib" + aroJpcapLibName + unixExtn;

			} else { // _________________________________________________________ Mac OS X libjpcap.jnilib
				aroJpcapLibName = "jpcap";
				aroJpcapLibFileName = "lib" + aroJpcapLibName + ".jnilib";
			}
		}
		logger.info("ARO Jpcap DLL lib file name: " + aroJpcapLibFileName);
	}

	/**
	 * Get name of ARO Jpcap DLL library file.
	 */
	public String getAroJpcapLibFileName() {
		return aroJpcapLibFileName;
	}
	
	public String getAroWebPLibName() {
		return aroWebPLibName;
	}

	public void setAroWebPLib(String osname, String osarch) {

		logger.info("OS: " + osname);

		logger.info("OS Arch: " + osarch);

		if (osname != null && osarch != null) {

			if (osname.contains(windowsOS) && osarch.contains("64")) { // _______ 64 bit Windows jpcap64.DLL
				aroWebPLibName = "webp-imageio";
				aroWebPLibFileName = aroWebPLibName + windowsExtn ;
			} else if (osname.contains(windowsOS)) { // _________________________ 32 bit Windows jpcap.DLL
				aroWebPLibName = "webp-imageio32";
				aroWebPLibFileName = aroWebPLibName + windowsExtn;
			} else if (osname.contains(linuxOS) && osarch.contains("amd64")) { // 64 bit Linux libjpcap64.so
				aroWebPLibName = "libwebp-imageio";
				aroWebPLibFileName = aroWebPLibName + unixExtn;
			} else if (osname.contains(linuxOS) && osarch.contains("i386")) { //  32 bit Linux libjpcap.so
				aroWebPLibName = "libwebp-imageio32";
				aroWebPLibFileName = aroWebPLibName + unixExtn;
			} else { // _________________________________________________________ Mac OS X libjpcap.jnilib
				aroWebPLibName = "libwebp-imageio";
				aroWebPLibFileName = aroWebPLibName + ".dylib";
			}
		}
		logger.info("ARO WebP DLL lib file name: " + aroWebPLibFileName);
	}

	public String getAroWebPLibFileName() {
		return aroWebPLibFileName;
	}
	
	@Override
	public void receive(int datalink, long seconds, long microSeconds, int len, byte[] data) {
		try {
			if (packetservice == null) {
				packetservice = new PacketServiceImpl();
			}
			Packet tempPacket = packetservice.createPacketFromPcap(datalink, seconds, microSeconds, len, data, currentPacketfile);
			packetlistener.packetArrived(null, tempPacket);
		} catch (Throwable t) {
			logger.error("Unexpected exception parsing packet", t);
		}
	}

	/**
	 * Potentially start the pcapng conversion process. Two conditions are
	 * tested, has conversion already been done and is the pcap file a pcapng.
	 * 
	 * @param traceFile
	 * @throws IOException if failed check or conversion
	 */
	private void provisionalPcapConversion(String traceFile) throws IOException {
		File file = filemanager.createFile(traceFile);
		
		String tracePath = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - file.getName().length());
		File backupCapFile = filemanager.createFile(tracePath, backupCapFileName);
		String eStr = null;
		if (!backupCapFile.exists()) {
			String lines = null;
			try {
				if (!pcapngHelper.isApplePcapng(file)) {
					return;
				}
				// pcapng is active, so convert it to pcap
				File temp = filemanager.createFile(tracePath, "temp.cap");
				//File temp = new File(tracePath, "temp.cap");
				if (temp.exists()) {
					temp.delete();
				}

				lines = extrunner.executeCmd(Util.getEditCap() + " -F pcap " + "\"" + file + "\" \"" + temp.getAbsolutePath() + "\"");
				logger.elevatedInfo("convert pcapng to pcap results :" + lines);
				if (temp.exists()) {
					filemanager.renameFile(file, backupCapFileName);
					filemanager.renameFile(temp, file.getName());
					pcapngHelper.setApplePcapNG(false);
					return;
				} else {
					eStr = String.format("failed to convert pcapng to pcap :%s\n%s", file, lines);
				}

			} catch (Exception e) {
				eStr = String.format("failed to convert pcapng to pcap :%s ERROR:%s", file, e.getMessage());
			}
		} else {
			return;
		}
		logger.error(eStr);
		throw new IOException(eStr);
	}

}
