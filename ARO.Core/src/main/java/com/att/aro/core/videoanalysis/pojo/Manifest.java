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
package com.att.aro.core.videoanalysis.pojo;

import java.net.URI;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

import lombok.Data;

@Data
public class Manifest {
	
	private static final Logger LOG = LogManager.getLogger(Manifest.class.getName());
	
	// UNDECLARED means Playlist can be updated
	public enum StreamType {
		VOD, EVENT, UNDECLARED
	}

	public enum ManifestType {
		MASTER, CHILD, UNKNOWN
	}

	public enum ContentType {
		VIDEO, AUDIO, SUBTITLES, MUXED, UNKNOWN
	}

	Manifest masterManifest = null;
	
	private UrlMatchDef urlMatchDef;
	private UrlMatchDef segUrlMatchDef;
	
	Session session;                					// session that manifest arrived on
	protected VideoFormat videoFormat = VideoFormat.UNKNOWN;
	protected VideoType videoType = VideoType.UNKNOWN;            // DASH, HLS, Unknown
	protected ManifestType manifestType = ManifestType.UNKNOWN;
	protected StreamType playListType = StreamType.UNDECLARED;
	protected ContentType contentType = ContentType.UNKNOWN;

	private double programDateTime;
	private String videoName = "";
	private String urlName = "";
	
	private boolean videoNameValidated = false;
	private Double duration = 0D;
	private Double timeScale = 0D;
	
	private HttpRequestResponseInfo request;
	private double requestTime;								// milliseconds UTC
	private double beginTime;		                        // timestamp of manifest request          
	private double endTime;			                        // timestamp of manifest download completed
	private URI uri;				                        // URI of GET request                     
	private String uriStr = "";
	private String encryption = "";

	byte[] moovContent;
	
	public Manifest getMasterManifest() {
		if (masterManifest == null || manifestType == ManifestType.MASTER) {
			return this;
		}
		return masterManifest;
	}

	/**
	 * delay is point in seconds video.mp4 or mov for when first segment starts to play
	 */
	double delay;
	
	/**
	 * The delay from request of manifest to first segment starts to play
	 */
	double startupDelay;

	boolean valid = true;
	
	private byte[] content;
	private double checksumCRC32;
	private String videoPath;

	private boolean selected = false;
	private boolean activeState = true;
	
	/**
	 * Segment associated with StartupDelay
	 */
	private VideoEvent startupVideoEvent;
	
	/**
	 * Indicates if video segment metadata such as bitrate has been extracted successfully
	 */
	private boolean videoMetaDataExtracted = false;
	private String setBaseURL;

	private boolean programTimeChanged;

	public double getStartupDelay() {
		if (startupVideoEvent != null) {
			return startupVideoEvent.getPlayTime();
		}
		return 0;
	}
	
	public boolean isVOD() {
		return StreamType.VOD.equals(playListType);
	}

	public String displayContent(boolean numbered, int skipTrigger) {
		String[] line = new String(getContent()).split("\n");
		StringBuilder strblr = new StringBuilder("Manifest :");
		strblr.append(getVideoName());
		strblr.append("\n");
		if (line.length < skipTrigger + 8) {
			// don't skip through display
			skipTrigger = -1;
		}
		for (int idx = 0; idx < line.length; idx++) {
			if (skipTrigger > 0 && idx == skipTrigger) {
				strblr.append("---> skipping " + (line.length - skipTrigger - 8) + " lines\n");
				idx = line.length - 8;
				skipTrigger = 0;
			}
			if (numbered) {
				strblr.append(String.format("%2d: %s\n", idx, line[idx]));
			} else {
				strblr.append(String.format("%s\n", line[idx]));
			}
		}
		return strblr.toString();
	}


	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder("\n\tManifest :");
		strblr.append(" requestTime :").append(String.format("%.3f", requestTime));
		strblr.append(", VideoType :" + getVideoType());
		strblr.append("\n\t, Type :").append(getManifestType());
		strblr.append(String.format(manifestType.equals(ManifestType.MASTER)
									? "\n\t, Stream-ProgramDateTime: %.3f" 
									: "\n\t, ProgramDateTime: %.3f"
					, programDateTime));
		strblr.append("\n\t, Name :").append(getVideoName());
		strblr.append(String.format("\n\t  CRC-32: %8.0f", checksumCRC32));
		strblr.append("\n\t, ContentType :").append(getContentType());
		strblr.append("\n\t, Encryption :").append(getEncryption());
		strblr.append("\n\t, URIs :").append(uri != null ? uri.getRawPath() : "null");
		strblr.append("\n\t, " + getUrlMatchDef());
		strblr.append("\n\t, duration :").append(getDuration());
		strblr.append(", timeScale :").append(getTimeScale());
		strblr.append("\n");
		strblr.append(displayContent(true, 30));
		return strblr.toString();
	}

	public boolean isVideoTypeFamily(VideoType type) {
		if (type == null) {
			return false;
		}
		return videoType.toString().contains(type.toString());
	}
	
	public boolean isVideoFormat(VideoFormat videoFormat) {
		if (videoFormat == null) {
			return false;
		}
		return this.videoFormat.equals(videoFormat);
	}


	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoNameNew) {
		this.videoName = videoNameNew;
	}

	public ContentType matchContentType(String type) {
		for (ContentType contentType : ContentType.values()) {
			if (type.equalsIgnoreCase(contentType.toString())) {
				return contentType;
			}
		}
		return ContentType.UNKNOWN;
	}

	public boolean updateStreamProgramDateTime(double programDateTime) {
		programTimeChanged = (this.programDateTime != 0 || programDateTime < this.programDateTime);
		
		if (this.programDateTime == 0 || programTimeChanged) {
			setProgramDateTime(programDateTime);
			if (masterManifest != null) {
				masterManifest.updateStreamProgramDateTime(programDateTime);
			}
		}
		return programTimeChanged;
	}

	public UrlMatchDef getUrlMatchDef() {
		if (urlMatchDef == null) {
			urlMatchDef = new UrlMatchDef();
		}
		return urlMatchDef;

	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (requestTime * 1000);
		result = prime * result + videoName.hashCode();
		result = prime * result + getContent().length;
		result = prime * result + getUrlName().hashCode();
		result = prime * result + getVideoFormat().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		try {
			result = super.equals(obj);
		} catch (Exception e) {
			LOG.error("Compare exeption:", e);
		}
		return result;
	}

}
