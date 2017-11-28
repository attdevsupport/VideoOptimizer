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
package com.att.aro.core.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.att.aro.core.configuration.pojo.Profile;
import com.att.aro.core.configuration.pojo.ProfileType;
import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.pojo.PrivateDataInfo;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;

public final class UserPreferences {

	private static UserPreferences instance = new UserPreferences();
	
	private static final String TD_PATH = "TD_PATH";
	private static final String PROFILE_PATH = "PROFILE_PATH";
	private static final String PROFILE = "PROFILE";
	private static final String PROFILE_3G = "PROFILE_3G";
	private static final String PROFILE_LTE = "PROFILE_LTE";
	private static final String PROFILE_WIFI = "PROFILE_WIFI";
	private static final String EXPORT_PATH = "EXPORT_PATH";
	private static final String PRIVATE_DATA = "PRIVATE_DATA";

	private IPreferenceHandler prefHandler;

	/**
	 * Gets a static instance of the UserPreferences class.
	 * 
	 * @return A static UserPreferences object.
	 */
	static UserPreferences getInstance() {
		return instance;
	}

	/**
	 * Private constructor. Use getInstance()
	 */
	private UserPreferences() {
		prefHandler = PreferenceHandlerImpl.getInstance();
	}
	
	// Intended to be used for testing only, thus made default
	void setPreferenceHandler(IPreferenceHandler prefHandler) {
		this.prefHandler = prefHandler;
	}
	
	// Intended to be used for testing only, thus made default
	IPreferenceHandler getPreferenceHandler() {
		return prefHandler;
	}
	
	public File getLastTraceDirectory() {
		String path = prefHandler.getPref(TD_PATH);
		return path != null ? new File(path) : null;
	}

	public void setLastTraceDirectory(File tdPath) {
		if (tdPath != null && !tdPath.isDirectory()) {
			throw new IllegalArgumentException("Trace directory must be a valid directory: " + tdPath.getAbsolutePath());
		}
		if (tdPath != null) {
			prefHandler.setPref(TD_PATH, tdPath.getAbsolutePath());
		} else {
			prefHandler.removePref(TD_PATH);
		}
	}

	public String getLastProfile() {
		return prefHandler.getPref(PROFILE);
	}

	public String getLastProfile(ProfileType profileType) {
		if (profileType != null) {
			switch (profileType) {
			case T3G:
				return prefHandler.getPref(PROFILE_3G);
			case LTE:
				return prefHandler.getPref(PROFILE_LTE);
			case WIFI:
				return prefHandler.getPref(PROFILE_WIFI);
			default:
				return null;
			}
		} else {
			return prefHandler.getPref(PROFILE);
		}
	}

	public void setLastProfile(Profile profile) {
		String name = profile != null ? profile.getName() : null;
		if (name != null && profile != null) {
			prefHandler.setPref(PROFILE, name);
			if (profile.getProfileType().equals(ProfileType.T3G)) {
				prefHandler.setPref(PROFILE_3G, name);
			} else if (profile.getProfileType().equals(ProfileType.LTE)) {
				prefHandler.setPref(PROFILE_LTE, name);
			} else if (profile.getProfileType().equals(ProfileType.WIFI)) {
				prefHandler.setPref(PROFILE_WIFI, name);
			}
		}
	}

	public File getLastProfileDirectory() {
		String path = prefHandler.getPref(PROFILE_PATH);
		return path != null ? new File(path) : null;
	}

	public void setLastProfileDirectory(File profilePath) {
		if (profilePath != null && !profilePath.isDirectory()) {
			throw new IllegalArgumentException("Profile directory must be a valid directory: " + profilePath.getAbsolutePath());
		}
		prefHandler.setPref(PROFILE_PATH, profilePath != null ? profilePath.getAbsolutePath() : null);
	}

	public File getLastExportDirectory() {
		String exportPath = prefHandler.getPref(EXPORT_PATH);
		if (exportPath != null) {
			File exportDir = new File(exportPath);
			if (exportDir.isDirectory()) {
				return exportDir;
			}
		}
		return null;
	}

	public void setLastExportDirectory(File exportDir) {
		if (exportDir != null && exportDir.exists()) {
			if (!exportDir.isDirectory()) {
				prefHandler.setPref(EXPORT_PATH, exportDir.getParentFile().getAbsolutePath());
			} else {
				prefHandler.setPref(EXPORT_PATH, exportDir.getAbsolutePath());
			}
		} else {
			prefHandler.removePref(EXPORT_PATH);
		}
	}
	
	public List<PrivateDataInfo> getPrivateData() {
		
		List<PrivateDataInfo> privateDataInfoList = new ArrayList<PrivateDataInfo>();
		String privateDataflattened = prefHandler.getPref(PRIVATE_DATA);
		
		if (privateDataflattened == null) {
			return privateDataInfoList;
		}
		
		String[] privateDataInfoArr = privateDataflattened.split(TraceDataConst.PrivateData.ITEM_DELIMITER);
		
		for (String privateDataInfo: privateDataInfoArr) {
			
			String[] properties = privateDataInfo.split(TraceDataConst.PrivateData.COLUMN_DELIMITER);
			String category = properties[0];
			String type = properties[1];
			String value = properties[2];
			boolean selected = TraceDataConst.PrivateData.YES_SELECTED.equals(properties[3])? true : false;
			
			PrivateDataInfo info = new PrivateDataInfo();
			info.setCategory(category);
			info.setType(type);
			info.setValue(value);
			info.setSelected(selected);
			
			privateDataInfoList.add(info);
		}
		
		return privateDataInfoList;
	}
	
	public void setPrivateData(List<PrivateDataInfo> privateData) {
		
		if (privateData == null) {
			throw new IllegalArgumentException("Private data must be non-null.");
		}
		
		StringBuilder strBuilder = new StringBuilder();
		
		for (int i = 0; i < privateData.size(); i++) {
			strBuilder.append(privateData.get(i).getCategory());
			strBuilder.append(TraceDataConst.PrivateData.COLUMN_DELIMITER);
			strBuilder.append(privateData.get(i).getType());
			strBuilder.append(TraceDataConst.PrivateData.COLUMN_DELIMITER);
			strBuilder.append(privateData.get(i).getValue());
			strBuilder.append(TraceDataConst.PrivateData.COLUMN_DELIMITER);
			strBuilder.append(privateData.get(i).isSelected()? 
					TraceDataConst.PrivateData.YES_SELECTED : TraceDataConst.PrivateData.NO_SELECTED);
			
			if (i < privateData.size() - 1) {
				strBuilder.append(TraceDataConst.PrivateData.ITEM_DELIMITER);
			}
		}
		
		prefHandler.setPref(PRIVATE_DATA, strBuilder.toString());
	}

}
