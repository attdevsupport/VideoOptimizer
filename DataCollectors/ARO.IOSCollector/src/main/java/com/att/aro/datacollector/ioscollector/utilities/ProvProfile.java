package com.att.aro.datacollector.ioscollector.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.att.aro.datacollector.ioscollector.app.IOSAppException;

public class ProvProfile {
	private static Logger LOGGER = Logger.getLogger(ProvProfile.class.getSimpleName());
	private static final String PROV_FILE_APP_ID_KEY = "application-identifier";
	private static final String PROV_FILE_TEAM_ID_KEY = "com.apple.developer.team-identifier";
	private static final String PROV_FILE_EXPIRATION_KEY = "ExpirationDate";

	private String appId;
	private String teamId;
	private String expiration;
	private String codesignId;
	
	ProvProfile(String devProvProfilePath) throws IOSAppException {
		String line;	
		try (BufferedReader br = new BufferedReader(new FileReader(devProvProfilePath))) {			
			while ((line = br.readLine()) != null && 
					(appId == null || teamId == null || expiration == null)) {		
				if (line.contains(PROV_FILE_APP_ID_KEY)) {
				    line = br.readLine();
				    if (line == null) {
				    	return;
				    }
					appId = line.trim().replaceAll("<string>|</string>", "");		
					continue;
				} 
				if (line.contains(PROV_FILE_TEAM_ID_KEY)) {
				    line = br.readLine();
				    if (line == null) {
				    	return;
				    }
					teamId = line.trim().replaceAll("<string>|</string>", "");
					continue;
				} 
				if (line.contains(PROV_FILE_EXPIRATION_KEY)) {
				    line = br.readLine();
				    if (line == null) {
				    	return;
				    }
					expiration = line.trim().replaceAll("<date>|</date>", "");
				}
			}			
		} catch (IOException e) {
			LOGGER.error("Error retrieving provisioning profile data", e);
			throw new IOSAppException(ErrorCodeRegistry.getExtractProvPropertyValuesError());
		} 
		
		if (appId == null || teamId == null || expiration == null) {
			throw new IOSAppException(ErrorCodeRegistry.getExtractProvPropertyValuesError());
		}
		
		
	    /* Example:
		 * appId: KV4EQ556NW.com.att.test
		 * teamId: KV4EQ556NW
		 */
		codesignId = appId.replace(teamId, "").substring(1);
	}

	public String getAppId() {
		return appId;
	}

	public String getTeamId() {
		return teamId;
	}

	public String getExpiration() {
		return expiration;
	}

	public String getCodesignId() {
		return codesignId;
	}

}
