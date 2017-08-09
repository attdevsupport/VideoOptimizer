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
package com.att.aro.core.peripheral.impl;

import static java.lang.Integer.parseInt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.ICollectOptionsReader;
import com.att.aro.core.peripheral.pojo.CollectOptions;
import com.att.aro.core.peripheral.pojo.CollectOptions.SecureStatus;
import com.att.aro.core.util.Util;
import com.att.aro.core.video.pojo.Orientation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class CollectOptionsReaderImpl implements ICollectOptionsReader {
	private static final String REGEX_NUMBER = "\\D+";
	private static Logger logger = Logger.getLogger(CollectOptionsReaderImpl.class.getName());

	@Override
	public CollectOptions readData(String directory) {
		String path = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.COLLECT_OPTIONS;
		CollectOptions collectOptions = new CollectOptions();
		File file = new File(path);
		if (!file.exists()) {
			return collectOptions;
		}
		try {
			List<String> lines = Files.readAllLines(file.toPath());
			collectOptions = lines.size() < 5 ? readOldFormat(lines) : readNewFormat(file);
			logger.info("Collection options: " + collectOptions.toString());
		} catch (IOException | InvalidPathException | NumberFormatException e) {
			logger.error("failed to read collection details file: " + path, e);
		}
		return collectOptions;

	}

	@SuppressFBWarnings("RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION")
	CollectOptions readNewFormat(File file) {
		Properties properties = new Properties();
		try (InputStream inStream = new FileInputStream(file)) {
			if(Util.isWindowsOS()) {
				String theString = IOUtils.toString(inStream, StandardCharsets.UTF_8); 
				properties.load(new StringReader(theString.replace("\\", "\\\\")));
			}else {
				properties.load(inStream);
			}
			return new CollectOptions(properties);
		} catch(Exception ex) {
			logger.error("Failed to read collection details file: " + file.getPath(), ex);
			return new CollectOptions();
		}
	}

	CollectOptions readOldFormat(List<String> lines) {
		CollectOptions collectOptions = new CollectOptions();
		try {
			switch (lines.size()) {
			case 4:
				collectOptions.setOrientation(getOrientation(lines.get(3)));
			case 3:
				collectOptions.setSecureStatus(getSecure(lines.get(2)));
			case 2:
				collectOptions.setUsDelay(parseInt(lines.get(1).replaceAll(REGEX_NUMBER, "")));
			case 1:
				collectOptions.setDsDelay(parseInt(lines.get(0).replaceAll(REGEX_NUMBER, "")));
			default:
				break;
			}
		} catch (Exception e) {
			logger.error("failed to read collection options: " + lines);
		}
		return collectOptions;
	}

	private SecureStatus getSecure(String line) {
		SecureStatus status = SecureStatus.UNKNOWN;
		String[] split = line.split(" ");
		if (split.length > 1) {
			if (Boolean.parseBoolean(split[1])) {
				status = SecureStatus.TRUE;
			} else {
				status = SecureStatus.FALSE;
			}
		}
		return status;
	}

	private Orientation getOrientation(String line) {
		String[] split = line.split(":");
		if (split.length > 1) {
			return Orientation.valueOf(split[1]);
		}
		return Orientation.PORTRAIT;
	}

}
