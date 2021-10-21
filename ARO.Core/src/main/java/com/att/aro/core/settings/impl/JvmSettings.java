/**
 * Copyright 2015 AT&T
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
 * 
 */
package com.att.aro.core.settings.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperic.sigar.Sigar;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.exception.ARORuntimeException;
import com.att.aro.core.settings.Settings;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Implements Settings class and acts as interface to (load from/save to)
 * config.properties file.
 *
 */
public final class JvmSettings implements Settings {
	private static final Logger LOGGER = LogManager.getLogger(JvmSettings.class.getName());
	public static final String CONFIG_FILE_PATH = System.getProperty("user.home") 
												+ System.getProperty("file.separator") + "VideoOptimizerLibrary"
												+ System.getProperty("file.separator") + ".jvm.options";

	// cannot be @Autowired, as this class is accessed from a UI dialog
	private static final IExternalProcessRunner externalProcessRunner = SpringContextUtil.getInstance().getContext().getBean(ExternalProcessRunnerImpl.class);
	private static final double MAX_PERCENT = .85;
	public static final double MULTIPLIER = 1024;
	private static final Double RESERVED_MEMORY = (double)(4 * MULTIPLIER);
	
	private static final Double DEFAULT_MEM = Util.isWindows32OS() ? 1.4 * MULTIPLIER : 2 * MULTIPLIER;
	private double maximumMemoryGB = Math.round(getMaximumMemoryMB() / MULTIPLIER * 10) / 10.0;
	private long installedRam;

	private static final JvmSettings INSTANCE = new JvmSettings();
	
	public static Settings getInstance() {
		return INSTANCE;
	}

	private JvmSettings() {
	}

	private void createConfig(String configFilePath, List<String> values) {
		File configFile = new File(configFilePath);
		try (FileWriter fileWriter = new FileWriter(configFile)) {
			if (values != null && values.size() != 0) {
				for (String value : values) {
					fileWriter.write(value + "\n");
				}
			}
		} catch (IOException e) {
			throw new ARORuntimeException("Could not create config file: " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String getAttribute(String name) {
		if (!StringUtils.equals("Xmx", name)) {
			throw new IllegalArgumentException("Not a valid property:" + name);
		}
		Path path = Paths.get(CONFIG_FILE_PATH);
		if (!path.toFile().exists()) {
			return DEFAULT_MEM.toString();
		}
		try (Stream<String> lines = Files.lines(path)) {
			List<String> values = lines.filter((line) -> StringUtils.contains(line, name)).collect(Collectors.toList());
			if (values == null || values.isEmpty()) {
				LOGGER.error("No xmx entries on vm options file");
				return DEFAULT_MEM.toString();
			} else {
				String value = values.get(values.size() - 1).replace("-Xmx", "").toLowerCase();
				int mark;
				if ((mark = value.indexOf("g")) > -1) {
					return value.substring(0, mark);
				}
				return toGB(value.replace("m", ""));
			}
		} catch (IOException e) {
			String message = "Counldn't read vm options file";
			LOGGER.error(message, e);
			throw new ARORuntimeException(message, e);
		}
	}

	private String toGB(String value) {
		try {
			Double memory = Double.valueOf(value) / MULTIPLIER;
			return String.format("%.1f", ((double)(Math.round(memory * 10)) / 10));
		} catch (NumberFormatException e) {
			LOGGER.error("Failed to parse :"+value);
			return "";
		}
	}

	@Override
	public Map<String, String> listAttributes() {
		throw new UnsupportedOperationException("Not a supported operation");
	}

	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	@Override
	public String setAttribute(String name, String value) {

		if (!StringUtils.equals("Xmx", name)) {
			throw new IllegalArgumentException("Not a valid property:" + name);
		}
		if (!NumberUtils.isNumber(value)) {
			throw new IllegalArgumentException(String.format(
					"Invalid Entry: \"%s\" is not a valid value for memory; Please enter an number from %.1f to %.1f"
					, value, DEFAULT_MEM / MULTIPLIER, maximumMemoryGB));
		}
		validateSize(Double.valueOf(value));
		value = String.format("%.0f", Double.valueOf(value) * MULTIPLIER);
		Path path = Paths.get(CONFIG_FILE_PATH);
		List<String> values = Collections.emptyList();
		if (!path.toFile().exists()) {
			createConfig(CONFIG_FILE_PATH, Collections.singletonList("-Xmx" + value + "m"));
			return value;
		} else {
			try (Stream<String> lines = Files.lines(path)) {
				values = lines.filter((line) -> !StringUtils.contains(line, name))
						.collect(Collectors.toList());
				values.add(0, "-Xmx" + value + "m");
				FileUtils.deleteQuietly(path.toFile());
			} catch (IOException e) {
				String message = "Counldn't read vm options file";
				LOGGER.error(message, e);
				throw new ARORuntimeException(message, e);
			}
			createConfig(CONFIG_FILE_PATH, values);
		}
		return value;
	}

	private void validateSize(double value) {
		long ram = getSystemMemory();
		if (ram <= 0) {
			throw new ARORuntimeException("Failed to get system info");
		}

		if (value > maximumMemoryGB || value < DEFAULT_MEM / MULTIPLIER) {
			throw new IllegalArgumentException(
					String.format("Invalid memory request. You have %.1f GB of system memory; Please enter value between %.1f and %.1f",
							(double)(ram / MULTIPLIER), DEFAULT_MEM / MULTIPLIER, maximumMemoryGB));
		}
	}

	public double getMaximumMemoryMB() {
		double maximumMemory;
		double ram = getSystemMemory();
		if (ram <= RESERVED_MEMORY * 2) {
			maximumMemory = ram * MAX_PERCENT;
		} else {
			maximumMemory = ram - RESERVED_MEMORY;
		}
		return Math.round(maximumMemory);
	}

	public double getMaximumMemoryGB() {
		return maximumMemoryGB;
	}
	
	public long getSystemMemory() {
		if (installedRam == 0) {
			try {
				if (Util.isMacOS()) {
					// locate MB
					Sigar sigar = new Sigar();
					installedRam = sigar.getMem().getRam();
				} else if (Util.isWindowsOS()) {
					// locate Bytes convert to MB
					String cmd = "wmic memorychip get capacity";
					String results = externalProcessRunner.executeCmd(cmd);
					String[] lines = results.split("\\s");
					installedRam = Math.round(StringParse.stringToDouble(lines[lines.length - 1], 0).longValue() / Double.valueOf(MULTIPLIER * MULTIPLIER));
				} else if (Util.isLinuxOS()) { 
					// locate KB convert to MB
					byte[] memInfoData = new byte[1024];
					FileInputStream fis = new FileInputStream("/proc/meminfo");
					if ((fis.read(memInfoData)) > 7) { // yes it did read the file, and maybe "MemTotal"
						installedRam = Math.round(StringParse.findLabeledDoubleFromString("MemTotal", memInfoData).longValue() / Double.valueOf(MULTIPLIER));
					}
					fis.close();
				}
			} catch (Exception e) {
				LOGGER.error("Failed to get system info", e);
			}
		}
		return installedRam;
	}


	@Override
	public String setAndSaveAttribute(String name, String value) {
		return setAttribute(name, value);
	}

	@Override
	public String removeAttribute(String name) {
		throw new UnsupportedOperationException("Can't remove this property:" + name);
	}

	@Override
	public String removeAndSaveAttribute(String name) {
		throw new UnsupportedOperationException("Can't remove this property:" + name);
	}

	@Override
	public void saveConfigFile() {
		try {
			FileWriter writer = new FileWriter(CONFIG_FILE_PATH);
			LOGGER.debug("Persisting properties to: " + CONFIG_FILE_PATH);
			writer.close();
		} catch (IOException e) {
			throw new ARORuntimeException("Could not save vm options file: " + e.getLocalizedMessage(), e);
		}
	}
}
