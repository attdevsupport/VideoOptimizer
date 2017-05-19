package com.att.aro.core.videoanalysis.pojo.amazonvideo;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.videoanalysis.pojo.ManifestDash;

public class XmlManifestHelper {

	@InjectLogger
	private static ILogger log;
	
	public enum ManifestFormat{
		SmoothStreamingMedia
		, MPD_EncodedSegmentList
		, MPD
	}

	private ManifestFormat manifestType;
	private Amz manifest;

	public XmlManifestHelper(byte[] data) {
		if (data == null || data.length == 0) {
			return;
		}
		String sData = new String(data);
		if (sData.indexOf("SmoothStreamingMedia") != -1) {
			manifestType = ManifestFormat.SmoothStreamingMedia;
			manifest = xml2pojo(new ByteArrayInputStream(data));
		} else if (sData.indexOf("MPD") != -1 && sData.indexOf("EncodedSegmentList") != -1) {
			manifestType = ManifestFormat.MPD_EncodedSegmentList;
			manifest = xml2JavaJaxB(new ByteArrayInputStream(data));
		} else if (sData.indexOf("MPD") != -1) {
			manifestType = ManifestFormat.MPD;
			manifest = xml2JavaJaxB(new ByteArrayInputStream(data));
		}
	}

	String getMajorVersion(){
		return manifest.getMajorVersion();
	}
	
	public ManifestFormat getManifestType() {
		return manifestType;
	}

	public Amz getManifest() {
		return manifest;
	}

	/**
	 * Load Amazon manifest into an MPDAmz object
	 * 
	 * @param xmlByte
	 * @return an MPDAmz object
	 */
	private MPDAmz xml2JavaJaxB(ByteArrayInputStream xmlByte) {
		JAXBContext context;
		Unmarshaller unMarshaller;
		MPDAmz mpdOutput = new MPDAmz();

		try {
			context = JAXBContext.newInstance(MPDAmz.class);
			
			unMarshaller = context.createUnmarshaller();
			mpdOutput = (MPDAmz) unMarshaller.unmarshal(xmlByte);
			if (context == null || mpdOutput.getPeriod().isEmpty()) {
				log.error("MPD NULL");
			}
		} catch (JAXBException e) {
			log.error("JAXBException" + e.getMessage());
		} catch (Exception ex) {
			log.error("JaxB parse Exception" + ex.getMessage());
		}
		return mpdOutput;
	}
	

	private SSMAmz xml2pojo(ByteArrayInputStream xmlByte) {
		JAXBContext context;
		Unmarshaller unMarshaller;
		SSMAmz manifest = new SSMAmz();

		try {
			context = JAXBContext.newInstance(SSMAmz.class);
			
			unMarshaller = context.createUnmarshaller();
			manifest = (SSMAmz) unMarshaller.unmarshal(xmlByte);
			if (context == null || manifest.getStreamIndex().isEmpty()) {
				log.error("SSM NULL");
			}
		} catch (JAXBException e) {
			log.error("JAXBException" + e.getMessage());
		} catch (Exception ex) {
			log.error("JaxB parse Exception" + ex.getMessage());
		}
		return manifest;
	}

}
