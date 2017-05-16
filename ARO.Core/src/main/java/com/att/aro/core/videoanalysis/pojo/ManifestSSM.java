package com.att.aro.core.videoanalysis.pojo;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.SSMAmz;
import com.att.aro.core.videoanalysis.pojo.amazonvideo.XmlManifestHelper;

public class ManifestSSM extends AROManifest {

	private SSMAmz manifest;

	public ManifestSSM(SSMAmz manifest, HttpRequestResponseInfo req, String videoPath) {
		super(VideoType.SSM, req, videoPath);
		this.manifest = manifest;
		parseManifestData();
	}
	
	public ManifestSSM(HttpRequestResponseInfo req, byte[] content, String videoPath) {
		super(VideoType.SSM, req, videoPath);
		XmlManifestHelper mani = new XmlManifestHelper(content);
		if (!mani.getManifestType().equals(XmlManifestHelper.ManifestFormat.SmoothStreamingMedia)) {
			this.manifest = (SSMAmz) mani.getManifest();
		}
		parseManifestData();
	}


	public ManifestSSM(HttpRequestResponseInfo req, String videoPath) {
		super(VideoType.SSM, req, videoPath);	
		try {
			//TODO verify
			content = reqhelper.getContent(req, session);
		} catch (Exception e) {
			log.error("Failed to parse manifest data, absTimeStamp:"
					+ req.getAbsTimeStamp().getTime() 
					+ ", Name:" + req.getObjNameWithoutParams());
			return;
		}
		XmlManifestHelper mani = new XmlManifestHelper(content);
		this.manifest = (SSMAmz) mani.getManifest();
		parseManifestData();
	}
	
	public void parseManifestData() {
		log.info("parseManifestData" + manifest);
	}

	@Override
	public String getDuration(String videoName) {
		return "";
	}

	public double getBandwith(String baseURL) {
		return 0;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("ManifestSSM, MajorVersion :");
		if (manifest != null) {
			strblr.append(manifest.getMajorVersion());
		} else {
			strblr.append("mpdOut == null");
		}
		strblr.append('\n');
		strblr.append(super.toString());

		return strblr.toString();
	}

	/**
	 * Scan manifest for byte range assigned to a video
	 * 
	 * @param fullName
	 * @param range
	 * @return Segment - the position in byte range, -1 if not found
	 */
	@Override
	public int parseSegment(String fullName, VideoEventData ved) {
		return -1;
	}
	

	@Override
	public String getTimeScale(String videoName) {
		return "";
	}
	
}
