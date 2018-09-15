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
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.att.aro.core.exception.ARORuntimeException;
import com.att.aro.core.settings.Settings;
import com.att.aro.core.util.Util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Implements Settings class and acts as interface to (load from/save to)
 * config.properties file.
 * 
 * @author bharath
 *
 */
public final class JvmSettings implements Settings {
	private static final String DEFAULT_MEM = Util.isWindows32OS() ? "1433" : "2048";
	private static final Logger LOGGER = LogManager.getLogger(JvmSettings.class.getName());
	public static final String CONFIG_FILE_PATH = System.getProperty("user.home") + System.getProperty("file.separator")
			+ "VideoOptimizerLibrary" + System.getProperty("file.separator") + ".jvm.options";
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
			return DEFAULT_MEM;
		}
		try (Stream<String> lines = Files.lines(path)) {
			List<String> values = lines.filter((line) -> StringUtils.contains(line, name)).collect(Collectors.toList());
			if (values == null || values.isEmpty()) {
				LOGGER.error("No xmx entries on vm options file");
				return DEFAULT_MEM;
			} else {
				return values.get(values.size() - 1).replace("-Xmx", "").replace("m", "");
			}
		} catch (IOException e) {
			String message = "Counldn't read vm options file";
			LOGGER.error(message, e);
			throw new ARORuntimeException(message, e);
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
			throw new IllegalArgumentException(
					value + " is not a valid value for memory; Please enter an integer value.");
		}
		validateSize(Long.valueOf(value));
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

	private void validateSize(Long value) {
		long ram = getSystemMemory();
		if(ram <= 0) {
			throw new ARORuntimeException("Failed to get system info");
		}
		if (value < Integer.valueOf(DEFAULT_MEM)) {
			throw new IllegalArgumentException("This is too low for optimal performance; Please enter value between "
					+ DEFAULT_MEM + " and " + ram / 2);
		}
		if ((double) value / ram > 0.5) {
			throw new IllegalArgumentException("You have " + ram + "mb of system memory; Please enter value between "
					+ DEFAULT_MEM + " and " + ram / 2);
		}
	}

	public long getSystemMemory() {
		long ram = 0;
		try {
			Sigar sigar = new Sigar();
			ram = sigar.getMem().getRam();
		} catch (UnsatisfiedLinkError | SigarException e) {
			LOGGER.error("Failed to get system info", e);
		}
		return ram;
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
