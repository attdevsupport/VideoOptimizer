/*
 *  Copyright 2021 AT&T
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


import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV6CommonPacket;
import org.pcap4j.packet.IllegalPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.factory.PacketFactories;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.exception.TsharkException;
import com.att.aro.core.packetreader.IPacketListener;
import com.att.aro.core.packetreader.IPacketReader;
import com.att.aro.core.packetreader.model.pcapng.RawPacketData;
import com.att.aro.core.packetreader.pojo.IPPacket;
import com.att.aro.core.packetreader.pojo.TCPPacket;
import com.att.aro.core.packetreader.pojo.UDPPacket;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.TSharkConfirmationImpl;
import com.att.aro.core.util.Util;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import lombok.Data;


public class PacketReaderLibraryImpl implements IPacketReader {
	private static final Logger LOGGER = LogManager.getLogger(PacketReaderLibraryImpl.class);

	private static final Random RANDOM = new Random();
	private static DataLinkType lastDataLinkType;

	@Autowired
	private IExternalProcessRunner externalProcessRunner;


	@Data
	private class FileDetails {
		private boolean isPcapNG;
		private boolean isPcap;
		private Boolean microsecondsPrecision; // If isPcap is true and this value is false, it means that PCAP file has nano-second timestamp precision
	}


	@Override
	public void readPacket(String packetfile, IPacketListener listener) throws IOException {
		FileDetails fileDetails = parseFileDetails(packetfile);
		if (!fileDetails.isPcap() && !fileDetails.isPcapNG()) {
			LOGGER.error("Not a valid pcap file " + packetfile);
			return;
		}

		long start = System.currentTimeMillis();

		if (fileDetails.isPcap) {
			PcapHandle handle = null;
			try {
				handle = Pcaps.openOffline(packetfile);
			} catch (PcapNativeException e) {
				LOGGER.error("Error while reading pcap file " + packetfile, e);
				return;
			}

			try {
				int currentPacketNumber = 0;
				int totalPacketReads = 0;
	        	while (true) {
	        		try {
	        			++currentPacketNumber;
	        			Packet pcap4jPacket = handle.getNextPacketEx();
	        			// Get last packet's capture timestamp
	        			Timestamp timestamp = handle.getTimestamp();
	        			com.att.aro.core.packetreader.pojo.Packet packet = translatePcap4jPacket(timestamp.getTime()/1000, timestamp.getNanos()/1000, pcap4jPacket);
	        			++totalPacketReads;
	        			listener.packetArrived(null, packet);
	        		} catch (EOFException e) {
	        			LOGGER.info(String.format("Finished reading total %d packets out of %d packets for pcap file %s",
	        					totalPacketReads, currentPacketNumber - 1, packetfile));
	    				break;
	    			} catch (Exception ex) {
	        			LOGGER.debug("Error while reading packet number " + currentPacketNumber, ex);
	        		}
	        	}
	        } catch (Exception e) {
	        	LOGGER.error("Error while reading pcap file " + packetfile, e);
	        } finally {
	        	handle.close();
	        }
		} else {
			TSharkConfirmationImpl tsharkConfirmation = new TSharkConfirmationImpl(externalProcessRunner);
			if (!tsharkConfirmation.checkTsharkVersion()) {
				throw new TsharkException("Wireshark is either not installed on your machine or its not in your path.");
			}

			handlePCAPNGFile(packetfile, listener);
		}

		LOGGER.info("Time to read pcap file in ms: " + (System.currentTimeMillis() - start));
	}

	/**
	 * Parse file details to identify if the file is a pcap/pcapng file
	 * @see PCAP file magic number https://pcapng.github.io/pcapng/draft-gharris-opsawg-pcap.html#name-file-header
	 * @see PCAPNG file magic number https://pcapng.github.io/pcapng/draft-tuexen-opsawg-pcapng.html#name-section-header-block
	 * @param filePath
	 * @return
	 */
	private FileDetails parseFileDetails(String filePath) {
		FileDetails fileDetails = new FileDetails();

		InputStream is = null;
		try {
			byte[] buffer = new byte[4];
			is = new FileInputStream(filePath);
			if (is.read(buffer) != buffer.length) { 
			    throw new Exception("Unable to read required number of bytes from file");
			}

			int signedByte1 = buffer[0] & 0xFF;
			int signedByte2 = buffer[1] & 0xFF;
			int signedByte3 = buffer[2] & 0xFF;
			int signedByte4 = buffer[3] & 0xFF;

			// PCAP file Microseconds-resolution timestamp precision 
			if ((signedByte1 == 0xa1 && signedByte2 == 0xb2 && signedByte3 == 0xc3 && signedByte4 == 0xd4) || // File written using Big-endian machine 
					(signedByte1 == 0xd4 && signedByte2 == 0xc3 && signedByte3 == 0xb2 && signedByte4 == 0xa1)) { // File written using Little-endian machine
				fileDetails.setPcap(true);
				fileDetails.setMicrosecondsPrecision(true);
			// PCAP file Nanoseconds-resolution timestamp precision
			} else if ((signedByte1 == 0xa1 && signedByte2 == 0xb2 && signedByte3 == 0x3c && signedByte4 == 0x4d) || // File written using Big-endian machine
					(signedByte1 == 0x4d && signedByte2 == 0x3c && signedByte3 == 0xb2 && signedByte4 == 0xa1)) { // File written using Little-endian machine
				fileDetails.setPcap(true);
				fileDetails.setMicrosecondsPrecision(false);
			// Validate PCAPNG file
			} else {
				buffer = new byte[8];
				if (is.read(buffer) != buffer.length) { 
				    throw new Exception("Unable to read required number of bytes from file");
				}

				int signedByte8 = buffer[4] & 0xFF;
				int signedByte9 = buffer[5] & 0xFF;
				int signedByte10 = buffer[6] & 0xFF;
				int signedByte11 = buffer[7] & 0xFF;

				if ((signedByte1 == 0x0a && signedByte2 == 0x0d && signedByte3 == 0x0d && signedByte4 == 0x0a) &&
						(
								(signedByte8 == 0x1a && signedByte9 == 0x2b && signedByte10 == 0x3c && signedByte11 == 0x4d) ||
								(signedByte8 == 0x4d && signedByte9 == 0x3c && signedByte10 == 0x2b && signedByte11 == 0x1a)
						)) {
					fileDetails.setPcapNG(true);
				}
			}
		} catch(Exception e) {
			LOGGER.warn("Something went wrong while parsing file details for " + filePath, e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LOGGER.warn("Unable to close file stream after reading file " + filePath);
				}
			}
		}

		return fileDetails;
	}

	/**
	 * Process PCAPNG file using Wireshark's tool "tshark"
	 * @param packetfile
	 * @param listener
	 */
	private void handlePCAPNGFile(String packetfile, IPacketListener listener) {
		File tempFile = null;
		try {
			// Update system property org.pcap4j.af.inet6 to correct value based on original OS which captured pcapng file
			identifyOSInfo(packetfile);

	        // Write content data (json file) to a temporary file to enable reading json entry in a streaming fashion
            tempFile = File.createTempFile("temp" + RANDOM.nextInt(), ".json");
            String cmd = Util.getTshark() + " -r \"" + packetfile + "\" -x -T json -j \"frame\" > " + tempFile.getAbsolutePath();
            LOGGER.info("Exporting raw packet data to external path " + tempFile.getAbsolutePath() + " for pcapng file " + packetfile + " with command: " + cmd);

            String result = externalProcessRunner.executeCmd(cmd);
            LOGGER.info("Export result: " + result);

            readPcapNGJson(tempFile.getAbsolutePath(), listener);
        } catch (Exception e) {
            LOGGER.error("Something went wrong while processing pcapng file " + packetfile, e);
        } finally {
            if (tempFile != null && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
	}

	/**
	 * Identify OS which captured the pcap/pcapng file and update pcap4j system property "org.pcap4j.af.inet6" as follow:
	 * Mac OS: 30, FreeBSD: 28, Linux: 10, All other OS: 23
	 * @param packetFile pcap/pcapng file path
	 */
	private void identifyOSInfo(String packetFile) {
		String cmd = Util.getCapinfos() + " " + packetFile;
		LOGGER.info("Getting OS Info with command: " + cmd);
		String result = externalProcessRunner.executeCmd(cmd);
		LOGGER.debug("capinfos command result: " + result);

		String osInfo = StringParse.findLabeledDataFromString("Capture oper-sys", ":", result);
		LOGGER.info("OS info: " + osInfo);

		if (StringUtils.isNotBlank(osInfo)) {
			String af_inet6_value = "23"; // Default value for all other OS

			if (osInfo.contains("Darwin") || osInfo.contains("OSX")) {
				af_inet6_value = "30";
			} else if (osInfo.contains("Linux")) {
				af_inet6_value = "10";
			} else if (osInfo.contains("BSD")) {
				af_inet6_value = "28";
			}

			System.setProperty("org.pcap4j.af.inet6", af_inet6_value);
			LOGGER.info("Updating org.pcap4j.af.inet6 to: " + af_inet6_value);
		}
	}

	/**
	 * Read PCAPNG json file generated by tshark to read frame's raw data and timestamp, and process it as VO packet
	 * @param path
	 * @param listener
	 * @throws IOException
	 */
	private void readPcapNGJson(String path, IPacketListener listener) throws IOException {
		try (
			InputStream inputStream = Files.newInputStream(Paths.get(path));
			JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
	    ) {
			Gson gson = new Gson();
	        reader.beginArray();
	        Set<DataLinkType> nextDataLinkTypesSet = getDataLinkTypes();
	        int currentPacketNumber = 0;
			int totalPacketReads = 0;
			lastDataLinkType = DataLinkType.EN10MB;
			DecimalFormat df = new DecimalFormat("#.######");
			df.setRoundingMode(RoundingMode.HALF_UP);

	        while (reader.hasNext()) {
	        	try {
		        	++currentPacketNumber;
		        	RawPacketData pcapngRawDataPacket = gson.fromJson(reader, RawPacketData.class);
		        	
		        	Double time = pcapngRawDataPacket.getSource().getLayers().getFrame().getEpochTimewithMicrosecondPrecision();
		        	time = Double.valueOf(df.format(time));
		        	// Convert timestamp to seconds and microseconds
		        	long seconds = time.longValue();
		        	long microseconds = (long) ((time - seconds) * 1000000);
	
		        	// Create Pcap4j packet object
		        	String rawHexData = String.valueOf(pcapngRawDataPacket.getSource().getLayers().getRawAttributesList().get(0));
		        	byte[] bytes = Hex.decodeHex(rawHexData.toCharArray());
		        	Packet pcap4jPacket = createPcap4jPacket(bytes, nextDataLinkTypesSet, currentPacketNumber);

		        	// create and process VO packet 
		        	if (pcap4jPacket != null) {
		        		com.att.aro.core.packetreader.pojo.Packet packet = translatePcap4jPacket(seconds, microseconds, pcap4jPacket);
		        		++totalPacketReads;
		        		listener.packetArrived(null, packet);
		        	}
	        	} catch (Exception ex) {
	        		LOGGER.debug("Error while reading packet number " + currentPacketNumber, ex);
	        	}
	        }

	        LOGGER.info(String.format("Finished reading total %d packets out of %d packets for pcap file %s",
					totalPacketReads, currentPacketNumber, path));

	        reader.endArray();
	    }
	}

	/**
	 * Create pcap4j packet using PacketFactory by trying most used DataLink Types in practice
	 * @param data
	 * @param nextDataLinkTypesMap
	 * @return
	 */
	private Packet createPcap4jPacket(byte[] data, Set<DataLinkType> nextDataLinkTypesSet, int currentPacketNumber) {
		Packet packet = null;
		boolean isValidPacket = false;
		DataLinkType saveLastDataLinkType = lastDataLinkType;
		DataLinkType currentDataLinkType = lastDataLinkType;

		Iterator<DataLinkType> dataLinkTypesIterator = nextDataLinkTypesSet.iterator();
		// Try the last datalink type first. Run for every other datalink type until we find a correct one.
		do {
			packet = PacketFactories.getFactory(Packet.class, DataLinkType.class).newInstance(data, 0, data.length, currentDataLinkType);
			
			if (packet != null &&
				!(packet instanceof IllegalPacket) &&
			    !(packet instanceof UnknownPacket) &&
				!(packet.getPayload() instanceof IllegalPacket) &&
				!(packet.getPayload() instanceof UnknownPacket)) {

				isValidPacket = true;
				lastDataLinkType = currentDataLinkType;
				break;
			}

			currentDataLinkType = dataLinkTypesIterator.hasNext() ? dataLinkTypesIterator.next() : null;
			// If current datalink type is same as the very first datalink type, get the next one as it has already been processed.
			if(currentDataLinkType != null && currentDataLinkType.equals(saveLastDataLinkType)) {
				currentDataLinkType = dataLinkTypesIterator.hasNext() ? dataLinkTypesIterator.next() : null;
			}
		} while (currentDataLinkType != null);

		if (!isValidPacket) {
			LOGGER.debug("Failed to generate a valid pcap4j packet for packet: " + currentPacketNumber);
		}

		return packet;
	}

	/**
	 * Get a new set of next data link types to try to create a pcap4j packet for PCAPNG file
	 * @return
	 */
	private Set<DataLinkType> getDataLinkTypes() {
		Set<DataLinkType> dataLinkTypesSet = new HashSet<>();
		dataLinkTypesSet.add(DataLinkType.EN10MB);
		dataLinkTypesSet.add(DataLinkType.LINUX_SLL);
		dataLinkTypesSet.add(DataLinkType.RAW);
		dataLinkTypesSet.add(DataLinkType.NULL);

		/*
		 * Reuse following entries if we see any of the following data link types in future traces.
		 *
		 * dataLinkTypesSet.add(DataLinkType.IEEE802);
		 * dataLinkTypesSet.add(DataLinkType.IEEE802_11);
		 * dataLinkTypesSet.add(DataLinkType.IEEE802_11_RADIO);
		 * dataLinkTypesSet.add(DataLinkType.DOCSIS);
		 * dataLinkTypesSet.add(DataLinkType.FDDI);
		 * dataLinkTypesSet.add(DataLinkType.PPP);
		 * dataLinkTypesSet.add(DataLinkType.PPP_SERIAL);
		*/

		return dataLinkTypesSet;
	}

	/**
	 * Translate Pcap4j packet to VO packet
	 * @param pcap4jPacket
	 * @return
	 */
	private com.att.aro.core.packetreader.pojo.Packet translatePcap4jPacket(long timestampInSeconds, long timestampInMicroSeconds, Packet pcap4jPacket) {
		TcpPacket pcap4jTcpPacket;
		UdpPacket pcap4jUdpPacket;

		if (pcap4jPacket.contains(IcmpV4CommonPacket.class) || pcap4jPacket.contains(IcmpV6CommonPacket.class)) {
			return new IPPacket(timestampInSeconds, timestampInMicroSeconds, pcap4jPacket);
		} else if ((pcap4jTcpPacket = pcap4jPacket.get(TcpPacket.class)) != null) {
			return new TCPPacket(timestampInSeconds, timestampInMicroSeconds, pcap4jPacket, pcap4jTcpPacket);
		} else if ((pcap4jUdpPacket = pcap4jPacket.get(UdpPacket.class)) != null) {
			return new UDPPacket(timestampInSeconds, timestampInMicroSeconds, pcap4jPacket, pcap4jUdpPacket);
		} else {
			return new IPPacket(timestampInSeconds, timestampInMicroSeconds, pcap4jPacket);
		}
	}
}
