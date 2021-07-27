/*
 *  Copyright 2021 AT&T
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
package com.att.aro.core.videoanalysis.parsers;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.videoanalysis.parsers.dashif.MPD;
import com.att.aro.core.videoanalysis.parsers.encodedsegment.MPDEncodedSegment;
import com.att.aro.core.videoanalysis.parsers.segmenttimeline.MPDSegmentTimeline;
import com.att.aro.core.videoanalysis.parsers.smoothstreaming.SSM;

import lombok.Data;

@Data
public class XmlManifestHelper {

	private static final Logger LOG = LogManager.getLogger(XmlManifestHelper.class.getName());

	public enum ManifestFormat{
		SmoothStreamingMedia
		, MPD_EncodedSegmentList
		, MPD_SegmentTimeline
		, MPD
	}

	private ManifestFormat manifestType;
	private MpdBase manifest;

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(83);
		strblr.append("XmlManifestHelper:"); 
		strblr.append(" manifestType :"); strblr.append( manifestType);
		strblr.append("\n :"); strblr.append( manifest);
		return strblr.toString();
	}

	public XmlManifestHelper(byte[] data) {
		if (data == null || data.length == 0) {
			return;
		}
		String sData = new String(data);

		if (sData.contains("MPD")) {
		    ByteArrayInputStream bis = new ByteArrayInputStream(data);

		    if (sData.contains("EncodedSegmentList")) {
		        manifestType = ManifestFormat.MPD_EncodedSegmentList;
	            manifest = xml2EncodedSegmentList(bis);
			} else if (sData.contains("<SegmentTimeline>") || sData.contains("$RepresentationID$")) {
		        manifestType = ManifestFormat.MPD_SegmentTimeline;
	            manifest = xml2SegmentTimeline(new MPDSegmentTimeline(), bis);
		    } else {
		        manifestType = ManifestFormat.MPD;
	            manifest = xml2StandardMPD(bis);
		    }
		} else if (sData.contains("SmoothStreamingMedia")) {
		    manifestType = ManifestFormat.SmoothStreamingMedia;
            manifest = xml2SSM(new ByteArrayInputStream(data));
		}
	}

	String getMajorVersion(){
		return manifest.getMajorVersion();
	}


	/**
     * Load Standard DASH manifest into an DASH IF MPD object
     *
     * @param xmlByte
     * @return an MPDAmz object
     */
    private MPD xml2StandardMPD(ByteArrayInputStream xmlByte) {
        JAXBContext context;
        Unmarshaller unMarshaller;
        MPD mpdOutput = null;

        try {
            context = JAXBContext.newInstance(MPD.class);

            unMarshaller = context.createUnmarshaller();
            mpdOutput = (MPD) unMarshaller.unmarshal(xmlByte);
            if (mpdOutput.getPeriods().isEmpty()) {
                LOG.error("MPD NULL");
            }
        } catch (Exception ex) {
            LOG.error("JaxB parse Exception", ex);
            mpdOutput = new MPD();
        }

        return mpdOutput;
    }

	/**
	 * Load Amazon manifest into an MPDAmz object
	 * 
	 * @param xmlByte
	 * @return an MPDAmz object
	 */
	private MPDEncodedSegment xml2EncodedSegmentList(ByteArrayInputStream xmlByte) {
		JAXBContext context;
		Unmarshaller unMarshaller;
		MPDEncodedSegment mpdOutput = new MPDEncodedSegment();

		try {
			context = JAXBContext.newInstance(MPDEncodedSegment.class);
			
			unMarshaller = context.createUnmarshaller();
			mpdOutput = (MPDEncodedSegment) unMarshaller.unmarshal(xmlByte);
			if (mpdOutput.getPeriod().isEmpty()) {
				LOG.error("MPD NULL");
			}
		} catch (JAXBException e) {
			LOG.error("JAXBException",e);
		} catch (Exception ex) {
			LOG.error("JaxB parse Exception", ex);
		}
		return mpdOutput;
	}
	
	private MpdBase xml2SegmentTimeline(MpdBase mpdOutput, ByteArrayInputStream xmlByte) {
		Class<? extends MpdBase> outClass = mpdOutput.getClass();
		JAXBContext context;
		Unmarshaller unMarshaller;

		try {
			context = JAXBContext.newInstance(outClass);

			unMarshaller = context.createUnmarshaller();
			mpdOutput = (MpdBase) unMarshaller.unmarshal(xmlByte);
			if (mpdOutput.getSize() == 0) {
				LOG.error("MPD NULL");
			}
		} catch (JAXBException e) {
			e.printStackTrace();
			LOG.error("JAXBException",e);
		} catch (Exception ex) {
			LOG.error("JaxB parse Exception", ex);
		}
		return mpdOutput;
	}

	private SSM xml2SSM(ByteArrayInputStream xmlByte) {
		JAXBContext context;
		Unmarshaller unMarshaller;
		SSM manifest = new SSM();

		try {
			context = JAXBContext.newInstance(SSM.class);
			
			unMarshaller = context.createUnmarshaller();
			manifest = (SSM) unMarshaller.unmarshal(xmlByte);
			if (manifest.getStreamIndex().isEmpty()) {
				LOG.error("SSM NULL");
			}
		} catch (JAXBException e) {
			LOG.error("JAXBException" + e.getMessage());
		} catch (Exception ex) {
			LOG.error("JaxB parse Exception" + ex.getMessage());
		}
		return manifest;
	}

}
