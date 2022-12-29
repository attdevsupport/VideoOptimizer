/*
 *  Copyright 2022 AT&T
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
package com.att.aro.core.videoanalysis.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.videoanalysis.IVideoEventDataHelper;
import com.att.aro.core.videoanalysis.pojo.VideoEventData;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public class VideoEventDataHelperImpl implements IVideoEventDataHelper{

	private VideoEventData ved;

	private static final Logger LOG = LogManager.getLogger(VideoEventDataHelperImpl.class);
	
	@Override
	public VideoEventData create(String name, String exten) {
		ved = new VideoEventData();
		ved.setName(name);
		ved.setByteRange(new ByteRange(1, 2));
		ved.setQuality("1");
		ved.setExtension(exten);
		return ved;
	}

	@Override
	public VideoEventData create(VideoAnalysisConfig vConfig, String[] strData) {
		if (vConfig == null) {
			return null;
		}
		ved = new VideoEventData();
		populate(vConfig, strData);
		return ved;
	}

	/**
	 * Populates member variables based on the String array strData
	 * 
	 * @param vConfig
	 * @param strData
	 * @throws Exception 
	 */
	private void populate(VideoAnalysisConfig vConfig, String[] strData){

		VideoDataTags[] xref = vConfig.getXref();
		for (int i = 0; i < strData.length; i++) {

			VideoDataTags switchVal = i < xref.length ? xref[i] : VideoDataTags.unknown;

			if ("N\\A".equals(strData[i])) {
				appendFailure(switchVal, strData[i]);
				continue;
			}
						
			switch (switchVal) {

			case ID: {
				ved.setName(strData[i]);
				break;
			}

			case ManifestType:{
				ved.setManifestType(strData[i]);
				break;
			}
			
			case Extension: {
				ved.setExtension(strData[i]);
				break;
			}

			case Quality: {
				ved.setQuality(strData[i]);
				break;
			}

			case ByteStart: {
				ved.setByteStart(StringParse.stringToDouble(strData[i], 0));
				break;
			}

			case ByteEnd: {
				ved.setByteEnd(StringParse.stringToDouble(strData[i], 0));
				break;
			}

			case CDN: {
				ved.setCdn(strData[i]);
				break;
			}

			case Segment: {
				try {
					if ("init".equals(strData[i])) {
						ved.setSegment(0);
					} else {
						ved.setSegment(Integer.valueOf(strData[i]));
					}
				} catch (NumberFormatException e) {
					ved.setSegment(-3);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}
			
			case HexSegment: {
				try {
					ved.setSegment(Integer.valueOf(strData[i], 16));
				} catch (NumberFormatException e) {
					ved.setSegment(-3);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}
			
			case SegmentReference: {
				ved.setSegmentReference(strData[i]);
				if ("init".equals(strData[i])){
					ved.setSegment(0);
				}
				break;
			}

			case SegmentStartTime: {
				ved.setSegmentStartTime(strData[i]);
				break;
			}
			
			case SegmentAutoCount: {
				ved.setSegmentAutoCount(!StringUtils.isEmpty(strData[i]));
				break;
			}
			
			case Bitrate: {
				ved.setBitrate(strData[i]);
				break;
			}

			case MdatSize: {
				ved.setMdatSize(strData[i]);
				break;
			}

			case Duration: {
				ved.setDuration(strData[i]);
				break;
			}

			case RateCode: {
				ved.setRateCode(strData[i]);
				break;
			}

			case Position: {
				ved.setPosition(strData[i]);
				break;
			}

			case Timestamp: {
				try {
					ved.setTimestamp(Double.valueOf(strData[i]));
				} catch (NumberFormatException e) {
					ved.setTimestamp(0);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}

			case ContentLength: {
				try {
					ved.setContentLength(Double.valueOf(strData[i]));
				} catch (NumberFormatException e) {
					ved.setContentLength(0);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}

			case ContentSize: {
				if (!strData[i].equals("mp2t")) {
					try {
						ved.setContentSize(Double.valueOf(strData[i]));
					} catch (NumberFormatException e) {
						ved.setContentSize(0);
						appendFailure(switchVal, strData[i]);
					}
				}
				break;
			}

			case ContentType: {
				ved.setContentType(strData[i]);
				break;
			}

			case ContentStart: {
				try {
					ved.setContentStart(Double.valueOf(strData[i]));
				} catch (NumberFormatException e) {
					ved.setContentStart(0);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}

			case ContentEnd: {
				try {
					ved.setContentEnd(Double.valueOf(strData[i]));
				} catch (NumberFormatException e) {
					ved.setContentEnd(0);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}

			case DateTime: {
				ved.setDateTime(strData[i]);
				DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				try {
					StringBuilder temp = new StringBuilder(16);
					temp.append(ved.getDateTime().substring(0, 4)); // yr
					temp.append('-');
					temp.append(ved.getDateTime().substring(4, 6)); // mo
					temp.append('-');
					temp.append(ved.getDateTime().substring(6, 8)); // dy
					temp.append('T');
					temp.append(ved.getDateTime().substring(9, 11)); // hr
					temp.append(':');
					temp.append(ved.getDateTime().substring(11, 13)); // hr
					temp.append(':');
					temp.append(ved.getDateTime().substring(13, 15)); // min

					if (ved.getDateTime().length() == 18) {
						temp.append('.');
						temp.append(ved.getDateTime().substring(15, 18));
					} else {
						temp.append(".000");
					}
					temp.append('Z'); // sec
					ved.setDtTime(utcFormat.parse(temp.toString()).getTime());
				} catch (ParseException e) {
					ved.setDtTime(0);
					appendFailure(switchVal, strData[i]);
				}
				break;
			}

			case unknown: {
				break;
			}

			default:
				break;
			}
		}
	}

	/** <pre>
	 * Record failed parsing into VideoEventData.failure
	 * by appending to any previous errors
	 * 
	 * @param label
	 * @param data
	 */
	private void appendFailure(VideoDataTags label, String data) {
		String sep = ved.getFailure().isEmpty() ? "" : ", ";
		ved.setFailure(ved.getFailure() + sep + label + " :" + data);
		LOG.info("Failed to Parse {" + label + " :" + data + "}");
	}
}
