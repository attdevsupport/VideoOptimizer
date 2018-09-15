package com.att.aro.core.videoanalysis.pojo.amazonvideo;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import com.att.aro.core.videoanalysis.pojo.mpdplayerady.MPDPlayReady;

public class XmlManifestHelper {

	private static final Logger LOG = LogManager.getLogger(XmlManifestHelper.class.getName());

	public enum ManifestFormat{
		SmoothStreamingMedia
		, MPD_EncodedSegmentList
		, MPD_PlayReady
		, MPD
	}

	private ManifestFormat manifestType;
	private MpdBase manifest;

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
			
		} else if (sData.indexOf("MPD") != -1 && sData.indexOf("urn:microsoft:playready") != -1) {
			manifestType = ManifestFormat.MPD_PlayReady;
			manifest = xml2PlayReady(new MPDPlayReady(), new ByteArrayInputStream(data));
			
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

	public MpdBase getManifest() {
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
				LOG.error("MPD NULL");
			}
		} catch (JAXBException e) {
			LOG.error("JAXBException" + e.getMessage());
		} catch (Exception ex) {
			LOG.error("JaxB parse Exception" + ex.getMessage());
		}
		return mpdOutput;
	}
	
	private MpdBase xml2PlayReady(MpdBase mpdOutput, ByteArrayInputStream xmlByte) {
		Class<? extends MpdBase> outClass = mpdOutput.getClass();
		JAXBContext context;
		Unmarshaller unMarshaller;

		try {
			context = JAXBContext.newInstance(outClass);

			unMarshaller = context.createUnmarshaller();
			mpdOutput = (MpdBase) unMarshaller.unmarshal(xmlByte);
			if (context == null || mpdOutput.getSize() == 0) {
				LOG.error("MPD NULL");
			}
		} catch (Exception ex) {
			LOG.error("JaxB parse Exception" + ex.getMessage());
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
