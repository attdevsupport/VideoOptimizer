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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.android.ddmlib.AndroidDebugBridge;
import com.att.aro.core.exception.ARORuntimeException;
import com.att.aro.core.settings.Settings;

/**
 * Implements Settings class and acts as interface to (load from/save to) config.properties file.
 * @author bharath
 *
 */
public final class SettingsImpl implements Settings {
	public enum ConfigFileAttributes {
		adb, gaTrackerId
	}

	public static final String CONFIG_FILE_PATH = System.getProperty("user.home") + System.getProperty("file.separator")
			+ "VideoOptimizerLibrary" + System.getProperty("file.separator") + "config.properties";
	private static final Logger LOGGER = LogManager.getLogger(SettingsImpl.class.getName());
	private static final SettingsImpl INSTANCE = new SettingsImpl();
	private final Properties configProperties;
	private final Map<String, String> valueMap = new ConcurrentHashMap<>();

	public static Settings getInstance() {
		return INSTANCE;
	}

	private SettingsImpl() {
		configProperties = loadProperties();
		Set<Entry<Object, Object>> entrySet = configProperties.entrySet();
		entrySet.stream().forEach(e -> valueMap.put((String) e.getKey(), (String) e.getValue()));
	}

	private Properties loadProperties() {
		LOGGER.debug("Reading properties from: " + CONFIG_FILE_PATH);
		createConfig(CONFIG_FILE_PATH);
		Properties configProperties = new Properties();
		try {
			FileReader configReader = new FileReader(CONFIG_FILE_PATH);
			configProperties.load(configReader);
			configReader.close();
		} catch (IOException e) {
			throw new ARORuntimeException("Could not read config file: " + e.getLocalizedMessage(), e);
		}
		return configProperties;
	}

	@SuppressWarnings("checkstyle:emptyblock")
	private void createConfig(String configFilePath) {
		File configFile = new File(configFilePath);
		if (!configFile.exists()) {
			File parent = configFile.getParentFile();
			if (parent != null) {
				parent.mkdirs();
				try (FileWriter fileWriter = new FileWriter(configFile)) {
				} catch (IOException e) {
					throw new ARORuntimeException("Could not create config file: " + e.getLocalizedMessage(), e);
				}
			}
		}
	}

	@Override
	public String getAttribute(String name) {
		return valueMap.get(name);
	}

	@Override
	public Map<String, String> listAttributes() {
		return valueMap;
	}

	@Override
	public String setAttribute(String name, String value) {
		LOGGER.debug("Replacing property " + name + " with " + value);
		valueMap.put(name, value);
		//Disconnecting the current adb bridge when we change the adb path value in the config file
		if(name.equals("adb")){
			AndroidDebugBridge.disconnectBridge();
		}
		return (String) configProperties.setProperty(name, value);
	}

	@Override
	public String removeAttribute(String name) {
		LOGGER.debug("Removing property " + name);
		valueMap.remove(name);
		return (String) configProperties.remove(name);
	}

	@Override
	public String setAndSaveAttribute(String name, String value) {
		String attribute = setAttribute(name, value);
		saveConfigFile();
		return attribute;
	}

	@Override
	public String removeAndSaveAttribute(String name) {
		String attribute = removeAttribute(name);
		saveConfigFile();
		return attribute;
	}

	@Override
	public void saveConfigFile() {
		try(FileWriter writer = new FileWriter(CONFIG_FILE_PATH)) {
			LOGGER.trace("Persisting properties to: " + CONFIG_FILE_PATH);
			configProperties.store(writer, null);
		} catch (IOException e) {
			throw new ARORuntimeException("Could not save config file: " + e.getLocalizedMessage(), e);
		}
	}
}
