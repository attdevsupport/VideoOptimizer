/*
 *  Copyright 2014 AT&T
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.IUserEventReader;
import com.att.aro.core.peripheral.pojo.UserEvent;
import com.att.aro.core.peripheral.pojo.UserEvent.UserEventType;
import com.att.aro.core.util.Util;

/**
 * read user event captured in the file
 * Date: October 6, 2014
 *
 */
public class UserEventReaderImpl extends PeripheralBase implements IUserEventReader {

	private static final Logger LOGGER = LogManager.getLogger(UserEventReaderImpl.class.getName());
	
	public List<UserEvent> readGetEventsFile(String directory, double eventTime0, double startTime) {

		List<UserEvent> userEvents = new ArrayList<UserEvent>();
		String filePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.USER_GETEVENTS_FILE;
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filePath);
		} catch (IOException e) {
			LOGGER.error("failed to read user event file: " + filePath);
		}

		if (lines != null && lines.length > 0) {
			int idx = 1;
			double monotonicTime = 0;
			for (String lineBuf : lines) {

				// CLOCK_MONOTONIC to refer against getevent timestamps
				if (idx == 1) {
					String monotonicFields[] = lineBuf.split(" ");

					if (monotonicFields.length > 2) {
						// Store the value in seconds
						monotonicTime = (Double.parseDouble(monotonicFields[2])) / 1000000000;
					}
					idx++;
					continue;
				}

				// Ignore empty line
				// getevent file ouput devices and their names to be neglected
				if (lineBuf.trim().isEmpty() || lineBuf.contains("add device") || lineBuf.contains("name:")) {
					continue;
				}

				// Parse Get event type entry
				UserEvent.UserEventType actionType = UserEvent.UserEventType.EVENT_UNKNOWN;
				if (lineBuf.contains(TraceDataConst.GetEvent.SCREEN)) {
					actionType = UserEventType.SCREEN_TOUCH;
				} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY)) {
					if (lineBuf.contains(TraceDataConst.GetEvent.KEY_POWER)) {
						actionType = UserEventType.KEY_POWER;
					} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY_VOLUP)) {
						actionType = UserEventType.KEY_VOLUP;
					} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY_VOLDOWN)) {
						actionType = UserEventType.KEY_VOLDOWN;
					} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY_HOME)) {
						actionType = UserEventType.KEY_HOME;
					} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY_MENU)) {
						actionType = UserEventType.KEY_MENU;
					} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY_BACK)) {
						actionType = UserEventType.KEY_BACK;
					} else if (lineBuf.contains(TraceDataConst.GetEvent.KEY_SEARCH)) {
						actionType = UserEventType.KEY_SEARCH;
					} else {
						actionType = UserEventType.KEY_KEY;
					}
				} else {
					LOGGER.warn("Invalid user event type in trace: " + lineBuf);
					continue;
				}

				String strFields[] = lineBuf.split("]");
				if (strFields[0].indexOf('[') != -1) {
					strFields[0] = strFields[0].substring(strFields[0].indexOf('[') + 1, strFields[0].length());
				}
				String timeArray[] = strFields[0].split(" ");
				Double currentTime = Double.parseDouble(timeArray[timeArray.length - 1]);

				double dTimeStamp = currentTime - monotonicTime;
				userEvents.add(new UserEvent(actionType, dTimeStamp, dTimeStamp));

			}
		}
		return userEvents;
	}
	
	@Override
	public List<UserEvent> readData(String directory, double eventTime0, double startTime) {
		List<UserEvent> userEvents = new ArrayList<UserEvent>();
		Map<UserEventType, Double> lastEvent = new EnumMap<UserEventType, Double>(
				UserEventType.class);
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.USER_EVENTS_FILE;
		String getEventsFilePath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.USER_GETEVENTS_FILE;
		
		if(filereader.fileExist(filepath)) {
			userEvents = readUserInputFile(lastEvent, filepath, startTime, eventTime0);
		} else if (filereader.fileExist(getEventsFilePath)) {
			// Its a VPN Trace
			userEvents = readGetEventsFile(directory, eventTime0, startTime);
		}
		return userEvents;
	}

	private List<UserEvent> readUserInputFile(Map<UserEventType, Double> lastEvent, String filepath, double startTime,  double eventTime0) {
		
		List<UserEvent> userEventsList = new ArrayList<UserEvent>();
		String[] lines = null;
		try {
			lines = filereader.readAllLine(filepath);
		} catch (IOException e) {
			LOGGER.error("failed to read user event file: "+filepath);
		}
		if(lines != null && lines.length > 0){
			for (String lineBuf : lines) {
	
				// Ignore empty line
				if (lineBuf.trim().isEmpty()) {
					continue;
				}
	
				// Parse entry
				String strFields[] = lineBuf.split(" ");
	
				// Get timestamp
				double dTimeStamp = Double.parseDouble(strFields[0]);
				if (dTimeStamp > 1.0e9) {
					dTimeStamp = Util.normalizeTime(dTimeStamp, startTime);
				} else {
	
					// Old data collector method (backward compatible)
					dTimeStamp -= eventTime0;
				}
	
				// Get event type
				UserEvent.UserEventType actionType = UserEvent.UserEventType.EVENT_UNKNOWN;
				String processedEvent;
				if (strFields.length == 3 && TraceDataConst.UserEvent.SCREEN.equals(strFields[1])) {
					processedEvent = strFields[2];
					actionType = UserEventType.SCREEN_TOUCH;
				} else if (strFields.length == 4 && TraceDataConst.UserEvent.KEY.equals(strFields[1])) {
					processedEvent = strFields[3];
					if (TraceDataConst.UserEvent.KEY_KEY.equals(strFields[2])) {
						actionType = UserEventType.KEY_KEY;
					} else if (TraceDataConst.UserEvent.KEY_POWER.equals(strFields[2])) {
						actionType = UserEventType.KEY_POWER;
					} else if (TraceDataConst.UserEvent.KEY_VOLUP.equals(strFields[2])) {
						actionType = UserEventType.KEY_VOLUP;
					} else if (TraceDataConst.UserEvent.KEY_VOLDOWN.equals(strFields[2])) {
						actionType = UserEventType.KEY_VOLDOWN;
					} else if (TraceDataConst.UserEvent.KEY_BALL.equals(strFields[2])) {
						actionType = UserEventType.KEY_BALL;
					} else if (TraceDataConst.UserEvent.KEY_HOME.equals(strFields[2])) {
						actionType = UserEventType.KEY_HOME;
					} else if (TraceDataConst.UserEvent.KEY_MENU.equals(strFields[2])) {
						actionType = UserEventType.KEY_MENU;
					} else if (TraceDataConst.UserEvent.KEY_BACK.equals(strFields[2])) {
						actionType = UserEventType.KEY_BACK;
					} else if (TraceDataConst.UserEvent.KEY_SEARCH.equals(strFields[2])) {
						actionType = UserEventType.KEY_SEARCH;
					} else if (TraceDataConst.UserEvent.KEY_GREEN.equals(strFields[2])) {
						actionType = UserEventType.KEY_GREEN;
					} else if (TraceDataConst.UserEvent.KEY_RED.equals(strFields[2])) {
						actionType = UserEventType.KEY_RED;
					}
				} else {
					
					LOGGER.warn("Invalid user event type in trace: " + lineBuf);
					continue;
				}
	
				// Get press or release
				boolean bPress = false;
				if (TraceDataConst.UserEvent.PRESS.equalsIgnoreCase(processedEvent)) {
					bPress = true;
				} else if (TraceDataConst.UserEvent.RELEASE.equalsIgnoreCase(processedEvent)) {
					bPress = false;
				} else {
					LOGGER.warn("211 - Key event does not have press/release indication: "+ lineBuf);
				
					continue;
				}
	
				if (bPress) {
					lastEvent.put(actionType, dTimeStamp);
				} else {
					Double lastTime = lastEvent.remove(actionType);
					if (lastTime != null) {
						userEventsList.add(new UserEvent(actionType, lastTime, dTimeStamp));
					} else {
						LOGGER.warn("Found key release event with no associated press event: "+ lineBuf);
						continue;
					}
				}
			}
		}
	return userEventsList;
	}

}
