package com.att.aro.core.tracemetadata.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.settings.Settings;
import com.att.aro.core.tracemetadata.IMetaDataHelper;
import com.att.aro.core.tracemetadata.pojo.MetaDataModel;
import com.att.aro.core.util.Util;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true) // allows for changes dropping items or using older versions, but not before this ignore
public class MetaDataHelper implements IMetaDataHelper {

	private static final String TRACE_OWNER = "traceOwner";

	private static final Logger LOG = LogManager.getLogger(MetaDataHelper.class);

	private static final String UNKNOWN = "unknown";

	private static final String METADATA_FILE = "metadata.json";

//	private static final String FUTURE = "";
	
	@Autowired
	private IFileManager filemanager;

	@Autowired
	private Settings settings;
	
	private ObjectMapper mapper = new ObjectMapper();

	@Getter
	private MetaDataModel metaData;
	
	/**
	 * @param path
	 * Path can be a fully qualified file path or just the trace folder
	 * The method saves data into the metadata.json file.
	 * @throws Exception
	 */
	@Override
	public void saveJSON(String path) throws Exception {
		
		String localPath = path;

		if (!path.isEmpty() && !filemanager.isFile(path)) {
			localPath = filemanager.createFile(path, METADATA_FILE).toString();
		}
		
		if (isNewTraceOwnerAvailable()) {
			metaData.setTraceOwner(settings.getAttribute(TRACE_OWNER));
		}
		String jsonData = getJson(metaData);
		
		if (jsonData != null && !jsonData.isEmpty() && localPath != null && !localPath.isEmpty()) {
			FileOutputStream output = new FileOutputStream(localPath);
			output.write(jsonData.getBytes());
			output.flush();
			output.close();
		}
	}
	
	private boolean isNewTraceOwnerAvailable() {
		boolean isTraceOwnerUpdated = false;
		if (!StringUtils.isEmpty(settings.getAttribute(TRACE_OWNER))
				&& !settings.getAttribute(TRACE_OWNER).equalsIgnoreCase(UNKNOWN)
				&& metaData!=null && metaData.getTraceOwner().equalsIgnoreCase(UNKNOWN)) {
			isTraceOwnerUpdated = true;
		}
		return isTraceOwnerUpdated;
	}

	@Override
	public String getJson() {
		return getJson(metaData);
	}

	@Override
	public String getJson(MetaDataModel metaDataModel) {
		String serialized = "";
		try {
			serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataModel);
		} catch (IOException e) {
			LOG.error("IOException :" + e.getMessage());
		}
		return serialized;
	}

	@Override
	public MetaDataModel initMetaData(TraceDirectoryResult result) {
		String tracePath = result.getTraceDirectory();
		if (tracePath == null) {
			metaData = new MetaDataModel();
		} else {
			int pos = tracePath.lastIndexOf(Util.FILE_SEPARATOR);
			String description = pos > 0 ? tracePath.substring(pos + 1) : tracePath;
			if (!filemanager.createFile(tracePath, METADATA_FILE).exists()) {
				metaData = new MetaDataModel();
	
				metaData.setDescription             (description);   // User configurable fields
				metaData.setTraceType               (UNKNOWN);       // User configurable fields
				metaData.setTargetedApp             (UNKNOWN);       // User configurable fields
				metaData.setApplicationProducer     (UNKNOWN);       // User configurable fields	
			} else {
				try {
					metaData = loadMetaData(tracePath);
				} catch (Exception e) {
					LOG.error("error reading data from BufferedReader", e);
				}
			}	
			try {
				if(updateMetaData(result)) {
					saveJSON(tracePath);
				}
			} catch (Exception e) {
				LOG.error("error saving metadata", e);
			}
		}
		return metaData;
	}

	@Override
	public MetaDataModel loadMetaData(String tracePath) throws Exception {
		if (tracePath == null) {
			throw new Exception("Invalid trace folder :" + tracePath);
		}
		BufferedReader reader;
		File metaFile = filemanager.createFile(tracePath, METADATA_FILE);
		reader = new BufferedReader(new FileReader(metaFile));
		StringBuilder temp = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			temp.append(line);
		}
		MetaDataModel metaDataModel = mapper.readValue(temp.toString(), MetaDataModel.class);
		ZonedDateTime dateTime = formatTime(metaDataModel);
		metaDataModel.setStartUTC(dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		metaData = metaDataModel;
		try {
			if (reader != null) {
				reader.close();
			}
		} catch (IOException e) {
			LOG.error("error closing BufferedReader reader", e);
		}
		return metaData;
	}
	
	private ZonedDateTime formatTime(MetaDataModel metaDataModel) {
		if(metaDataModel == null || metaDataModel.getStartUTC() == null) {
			return ZonedDateTime.now();
		}
		try{
			ZonedDateTime parsed = ZonedDateTime.parse(metaDataModel.getStartUTC(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return parsed;
		} catch(DateTimeParseException ex) {
			try {
				ZonedDateTime parsed = ZonedDateTime.parse(metaDataModel.getStartUTC(),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()));
				return parsed;
			} catch(DateTimeParseException ex2) {
				return ZonedDateTime.now();
			}
		}
	}

	private boolean updateMetaData(TraceDirectoryResult result) {
		
		boolean isMetaDataUpdated = false;
		if (metaData.getDeviceOrientation().isEmpty()) {
			metaData.setDeviceOrientation(
					result.getCollectOptions() != null ? result.getCollectOptions().getOrientation() : "unknown");
			isMetaDataUpdated = true;
		}
		if (metaData.getPhoneMake().isEmpty()) {
			metaData.setPhoneMake(result.getDeviceMake());
			isMetaDataUpdated = true;
		}
		if (metaData.getPhoneModel().isEmpty()) {
			metaData.setPhoneModel(result.getDeviceModel());
			isMetaDataUpdated = true;
		}
		if (metaData.getOs().isEmpty()) {
			metaData.setOs(result.getOsType());
			isMetaDataUpdated = true;
		}
		if (metaData.getOsVersion().isEmpty()) {
			metaData.setOsVersion(result.getOsVersion());
			isMetaDataUpdated = true;
		}
		
		if (metaData.getStartUTC().isEmpty()) {
			ZonedDateTime startUTC = null;
			if (result != null && result.getTraceDateTime() != null) {
				startUTC = ZonedDateTime.ofInstant(result.getTraceDateTime().toInstant(), ZoneId.systemDefault());
				isMetaDataUpdated = true;
			}
			metaData.setStartUTC(startUTC != null ? startUTC.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "");
		}
		
		if (metaData.getTraceDuration().isEmpty()) {
			metaData.setTraceDuration(String.valueOf(result == null ? 0.0 : result.getTraceDuration()));
			isMetaDataUpdated = true;
		}
		if (metaData.getTraceSource().isEmpty()) {
			metaData.setTraceSource("Manual");
			isMetaDataUpdated = true;
		} // MACHINE when tested in automated test harness
		
		if (settings != null && (metaData.getTraceOwner() == null || metaData.getTraceOwner().isEmpty())) {
			metaData.setTraceOwner(
					(settings.getAttribute(TRACE_OWNER) == null) ? UNKNOWN : settings.getAttribute(TRACE_OWNER));			
		}
		
		if ((metaData.getTargetAppVer().isEmpty() || UNKNOWN.equals(metaData.getTargetAppVer()))
				&& (!metaData.getTargetedApp().isEmpty() && !UNKNOWN.equals(metaData.getTargetedApp()))) {
			String appVersion = findAppVersion(result);
			if(!UNKNOWN.equals(appVersion)) {
				isMetaDataUpdated = true;
				metaData.setTargetAppVer(appVersion);
			}		
		}

		return isMetaDataUpdated;
	}

	@Override
	public String findAppVersion(TraceDirectoryResult result) {
		Map<String, String> appVersion = result.getAppVersionMap();
		if (appVersion != null) {
			String app = metaData.getTargetedApp().toLowerCase();
			for (String key : appVersion.keySet()) {
				if (key.toLowerCase().startsWith(app)) {
					return appVersion.get(key);
				}
			}
		}
		return UNKNOWN;
	}

	@Override
	public MetaDataModel initMetaData(String tracePath, String traceDesc, String traceType, String targetedApp, String appProducer) {
		metaData = new MetaDataModel();
		metaData.setDescription(traceDesc);
		metaData.setTargetedApp(targetedApp);
		metaData.setTraceType(traceType);
		metaData.setApplicationProducer(appProducer);
		String localPath = "";
		if (!tracePath.isEmpty() && !filemanager.isFile(tracePath)) {
			localPath = filemanager.createFile(tracePath, METADATA_FILE).toString();
		}
		
		if (isNewTraceOwnerAvailable()) {
			metaData.setTraceOwner(settings.getAttribute(TRACE_OWNER));
		}
		String jsonData = getJson(metaData);
		
		if (jsonData != null && !jsonData.isEmpty() && localPath != null && !localPath.isEmpty()) {
			FileOutputStream output;
			try {
				output = new FileOutputStream(localPath);
				output.write(jsonData.getBytes());
				output.flush();
				output.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		return metaData;
	}

}
