package com.att.aro.core.videoanalysis.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.att.aro.core.packetanalysis.pojo.ByteRange;
import com.att.aro.core.videoanalysis.IVideoEventDataHelper;
import com.att.aro.core.videoanalysis.pojo.VideoEventData;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public class VideoEventDataHelperImpl implements IVideoEventDataHelper{

	private VideoEventData ved;

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

			switch (switchVal) {

			case ID: {
				ved.setName(strData[i]);
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
				ved.setByteStart(strData[i]);
				break;
			}

			case ByteEnd: {
				ved.setByteEnd(strData[i]);
				break;
			}

			case CDN: {
				ved.setCdn(strData[i]);
				break;
			}

			case Segment: {
				try {
					ved.setSegment(Integer.valueOf(strData[i]));
				} catch (NumberFormatException e) {
					ved.setSegment(-3);
					ved.setFailure(ved.getFailure() + ",segment :" + strData[i]);
				}
				break;
			}
			
			case HexSegment: {
				try {
					ved.setSegment(Integer.valueOf(strData[i], 16));
				} catch (NumberFormatException e) {
					ved.setSegment(-3);
					ved.setFailure(ved.getFailure() + ",segment :" + strData[i]);
				}
				break;
			}

			case SegmentStartTime: {
				ved.setSegmentStartTime(strData[i]);
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
				ved.setTimestamp(Double.valueOf(strData[i]));
				break;
			}

			case ContentLength: {
				ved.setContentLength(Double.valueOf(strData[i]));
				break;
			}

			case ContentSize: {
				if (!strData[i].equals("mp2t")) {
					try {
						ved.setContentSize(Double.valueOf(strData[i]));
					} catch (NumberFormatException e) {
						ved.setContentSize(0);
						ved.setFailure(ved.getFailure() + ",contentSize :" + strData[i]);
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
					ved.setFailure(ved.getFailure() + ",contentStart :" + strData[i]);
				}
				break;
			}

			case ContentEnd: {
				try {
					ved.setContentEnd(Double.valueOf(strData[i]));
				} catch (NumberFormatException e) {
					ved.setContentEnd(0);
					ved.setFailure(ved.getFailure() + ",contentEnd :" + strData[i]);
				}
				break;
			}

			case DateTime: {
				ved.setDateTime(strData[i]);
				DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				try {
					StringBuilder temp = new StringBuilder(16);
					                  temp.append(ved.getDateTime().substring(0, 4));    // yr
					temp.append('-'); temp.append(ved.getDateTime().substring(4, 6));    // mo
					temp.append('-'); temp.append(ved.getDateTime().substring(6, 8));    // dy
					temp.append('T'); temp.append(ved.getDateTime().substring(9, 11));   // hr
					temp.append(':'); temp.append(ved.getDateTime().substring(11, 13));  // hr
					temp.append(':'); temp.append(ved.getDateTime().substring(13, 15));  // min
					
					if (ved.getDateTime().length() == 18) {
						temp.append('.'); temp.append(ved.getDateTime().substring(15, 18));
					} else {
						temp.append(".000");
					}
					temp.append('Z'); // sec
					ved.setDtTime(utcFormat.parse(temp.toString()).getTime());
				} catch (ParseException e) {
					ved.setDtTime(0);
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
}
