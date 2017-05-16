package com.att.aro.core.videoanalysis.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.ILogger;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.IVideoAnalysisConfigHelper;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;
import com.att.aro.core.videoanalysis.pojo.config.VideoAnalysisConfig;
import com.att.aro.core.videoanalysis.pojo.config.VideoDataTags;

public class VideoAnalysisConfigHelperImpl implements IVideoAnalysisConfigHelper {

	@Autowired
	private IFileManager filemanager;

	private TreeMap<String, VideoAnalysisConfig> vaConfigMap = new TreeMap<>();

	private String folderPath = Util.getVideoOptimizerLibrary() + Util.FILE_SEPARATOR + "VideoConfig_files" + Util.FILE_SEPARATOR;

	ObjectMapper mapper = new ObjectMapper();

	private VideoAnalysisConfig vaConfig = null;

	@InjectLogger
	private static ILogger log;

	@Override
	public VideoAnalysisConfig findConfig(String target) {
		initConfigFiles();

		if (vaConfig != null && vaConfig.getPattern().matcher(target).matches()) {
			return vaConfig;
		} else {
			for (VideoAnalysisConfig tempConfig : vaConfigMap.values()) {
				log.info(tempConfig.getDesc());
				if (tempConfig.isValidated() && tempConfig.patternFind(target)) {
					vaConfig = tempConfig;
					return vaConfig;
				}
			}
		}
		return null;
	}

	private void initConfigFiles() {
		if (vaConfigMap.isEmpty()) {

			loadConfigFiles();

			log.info("vaConfigMap.keySet() :" + vaConfigMap.keySet());
		}
	}

	private void loadConfigFiles() {
		filemanager.mkDir(folderPath);
		String[] list = filemanager.findFilesByExtention(folderPath, ".json");
		if (list.length == 0) {
			try {
				initDefaultConfig();
			} catch (Exception e) {
				log.error("Failed to generate default Video Config json files :", e);
			}
			list = filemanager.findFilesByExtention(folderPath, ".json");
		}

		for (String jsonFile : list) {
			loadConfigFile(jsonFile);
		}

	}

	private void initDefaultConfig() throws JsonGenerationException, JsonMappingException, IOException {
		// http://cdn09dld.uverse.com.edgesuite.net/m/1/372496/16/2651920/SFMM3028H-YouTube_1489616158_166645_112.mp4
		// User-Agent: Player/LG Player 1.0 for Android 6.0 (stagefright alternative) Accept: audio/mp4, video/mp4, video/3gpp2, video/3gpp, audio/amr, audio/aac, audio/aacPlus, audio/mpeg, audio/aiff, audio/flac, */* Host: cdn09dld.uverse.com.edgesuite.net Connection: Keep-Alive Accept-Encoding: gzip
		// Server: Apache ETag: "187f536da1c4dce868fdac67a73c2e44:1489616359" Last-Modified: Wed, 15 Mar 2017 22:19:19 GMT Accept-Ranges: bytes Content-Length: 3648348 Content-Type: video/mp4 Date: Thu, 16 Mar 2017 22:26:11 GMT Connection: keep-alive

		try {
			saveConfigFile(VideoType.HLS, "uverse-YouTube", "GET", "\\.(uverse)\\..+\\/([a-zA-Z_0-9\\-]*)\\.([a-zA-Z]*\\d{1})"
						, null
						,"Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)",
						new VideoDataTags[] { 
								VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.Extension,
								VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.ContentSize });
				
				saveConfigFile(VideoType.HLS, "HLS-VOD", "GET", "http:\\/\\/directv-vod\\.hls\\.adaptive\\.(level3)\\.net\\/.+\\/([A|B]\\d+U)(\\d)_(\\d)_(\\d{1,4})\\.([a-zA-Z]*)\\?exptime=(\\d+)", null,
						"Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)",
						new VideoDataTags[] { VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.IDX, VideoDataTags.Quality, VideoDataTags.Segment, VideoDataTags.Extension, VideoDataTags.Timestamp,
								VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.ContentSize });

//		// aav livetv
//		saveConfigFile(VideoType.HLS, "HLS-VOD-aav", "GET", "\\/([A|B]\\d+U\\d)_(\\d)_(\\d{1,4})\\.([a-zA-Z]*)\\?p=(\\d+)&e=(\\d+)", null,
//				"Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)",
//				new VideoDataTags[] { VideoDataTags.ID, VideoDataTags.Quality, VideoDataTags.Segment, VideoDataTags.Extension, VideoDataTags.Timestamp,
//						VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.unknown, VideoDataTags.ContentSize });

				saveConfigFile(VideoType.HLS
						, "HLS-vc3m"
						, "GET"
						, "http:\\/\\/directvc3m\\-prod\\-vod\\.hls\\.adaptive\\.(level3)\\.net\\/.+\\/([A|B]\\d+U)(\\d)\\/(\\d{2})\\/(\\d{1,4})\\.([a-zA-Z]*)\\?exptime=(\\d+)"
						, null
						, "Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)"
						, new VideoDataTags[] { 
								VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.IDX, VideoDataTags.Quality, VideoDataTags.Segment, VideoDataTags.Extension, VideoDataTags.Timestamp,
								VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.ContentSize });

				
				
				// http://directvlst-live.hls.adaptive.level3.net/livetv/30/8249/03/20170116T225627189.ts?exptime=1484621831&token=1d0d6b88bd5f9b769e8c88143390ff27
				saveConfigFile(VideoType.HLS, "HLS-vlst-live", "GET",
						"directvlst-live\\.hls\\.adaptive\\.(level3)\\.net\\/livetv\\/30\\/(\\d+)\\/(\\d{2})\\/(\\d{8}T\\d{8,10})\\.([a-zA-Z]*)\\?exptime=(\\d+).+", ".+",
						"Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)",
						new VideoDataTags[] { VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.Quality, VideoDataTags.DateTime, VideoDataTags.Extension, VideoDataTags.Timestamp,
								VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.ContentSize });

				saveConfigFile(VideoType.HLS, "HLS_aav_akamai", "GET",
						"http:\\/\\/aav-akamai3.directv.com.edgesuite.net\\/(aav).+\\/([A|B]\\d+U)(\\d)_(\\d{1,2})_(\\d{1,4})\\.([a-zA-Z]*)\\?exptime=(\\d+).+", null,
						"Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)",
						new VideoDataTags[] { VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.IDX, VideoDataTags.Quality, VideoDataTags.Segment, VideoDataTags.Extension, VideoDataTags.Timestamp,
								VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.unknown });

				saveConfigFile(VideoType.HLS
						, "HLS_aav"
						, "GET"
						, "directvaav.+\\/(aav).+\\/([A|B]\\d+U)(\\d)_(\\d{1,2})_(\\d{1,4})\\.([a-zA-Z]*)"
						, ".+"
						, "Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)"
						, new VideoDataTags[] { VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.IDX, VideoDataTags.Quality, VideoDataTags.Segment, VideoDataTags.Extension
								, VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.unknown });

				saveConfigFile(VideoType.HLS, "HLS-vlst_llnwd", "GET",
						"http:\\/\\/directvlst\\.vo\\.(llnwd)\\.net\\/e1\\/livetv\\/30\\/(\\d+)\\/(\\d{2})\\/(\\d{8}T\\d{8,10})\\.([a-zA-Z]*)\\?p=([a-zA-Z_0-9]*)", null,
						"Content\\-Length: (\\d+).+Content\\-Type: (video)\\/([a-zA-Z_0-9]*)",
						new VideoDataTags[] { VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.Quality, VideoDataTags.DateTime, VideoDataTags.Extension, VideoDataTags.unknown, 
//								VideoDataTags.Timestamp, 
								VideoDataTags.ContentLength, VideoDataTags.ContentType, VideoDataTags.ContentSize });

				saveConfigFile(VideoType.DASH, "dash_vod", "GET", "\\/([a-zA-Z_0-9\\-]*)_video_(\\d)\\.([a-zA-Z]*\\d{1})", "Range: bytes=(\\d+)\\-(\\d+)",
						"Content\\-Length: (\\d+).+Content-Range: bytes (\\d+)-(\\d+)\\/(\\d+)",
						new VideoDataTags[] { VideoDataTags.ID, VideoDataTags.Quality, VideoDataTags.Extension, VideoDataTags.ByteStart, VideoDataTags.ByteEnd, VideoDataTags.ContentLength,
								VideoDataTags.ContentStart, VideoDataTags.ContentEnd, VideoDataTags.ContentSize });

				// http://ds79lt46qzmj0.cloudfront.net/dm/2$w-i4rGny79gdQDF6YsenLjtAzZ0~/6d64/b2c7/6f71/4725-b1e2-b68bb43c0171
				// /7b81c27d-83fc-4f78-98ca-d549ed3a211c.ism/QualityLevels(450000)/Fragments(video=560560000)
				saveConfigFile(VideoType.SSM
						, "ssm_ism"
						, "GET"
						, "\\/([a-zA-Z_0-9\\-]*)\\.ism\\/QualityLevels\\((\\d+)\\)\\/Fragments\\(video=(\\d+)\\)"
						, null
						,"Content\\-Type: (video)\\/([a-zA-Z_0-9]*).+Content\\-Length: (\\d+)"
						, new VideoDataTags[] { VideoDataTags.ID, VideoDataTags.Quality, VideoDataTags.Position, VideoDataTags.ContentType, VideoDataTags.Extension, VideoDataTags.ContentSize });
				
				// http://dtvn-live-pplus-sponsored.akamaized.net/Content/HLS_hls.pr/Live/channel(FNCHD.gmott.1080.mobile)/20170218T195758-247961301-05-20170221T002136.ts
				saveConfigFile(VideoType.HLS
						, "HLS-NOW"
						, "GET",
						  "(dtvn-live).+channel\\((.+)\\).+-(\\d{2})-(\\d{8}T\\d{6})\\.([a-zA-Z]*)"
						, ".+"
						, "Content\\-Range\\:\\ bytes\\ (\\d+)\\-(\\d+)\\/(\\d+).+Content\\-Type\\:\\ video\\/([a-zA-Z0-9]*)"
						, new VideoDataTags[] { VideoDataTags.CDN, VideoDataTags.ID, VideoDataTags.Quality, VideoDataTags.DateTime, VideoDataTags.Extension
								,VideoDataTags.ByteStart
								,VideoDataTags.ByteEnd
								,VideoDataTags.ContentLength
								,VideoDataTags.ContentType
								});
				

				
		} catch (Exception e) {
			log.error(String.format("%s :%s", e.getClass().getName(), e.getMessage()));
		}
			

	}

	// TODO move this into AroPrefs, a new handler for preferences json serialization
	void loadPrefs() {

		Path filePath = Paths.get(folderPath + "prefs.json");

		VideoUsagePrefs videoUsagePrefs = null;
		try {
			String temp = new String(Files.readAllBytes(filePath));
			videoUsagePrefs = mapper.readValue(temp, VideoUsagePrefs.class);
			log.debug("videoUsagePrefs :" + videoUsagePrefs);

		} catch (JsonParseException e) {
			log.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
		} catch (JsonMappingException e) {
			log.error("VideoUsagePrefs failed to de-serialize :" + e.getMessage());
		} catch (IOException e) {
			log.error("VideoUsagePrefs failed to load :" + e.getMessage());
		}
	}

	/**
	 * Load VideoUsage Preferences
	 * 
	 * @return VideoAnalysisConfig, null if invalid
	 */
	@Override
	public VideoAnalysisConfig loadConfigFile(String file) {
		String error = null;
		VideoAnalysisConfig vConfig = null;

		Path filePath = Paths.get(folderPath + file);
		String temp = null;
		try {
			temp = new String(Files.readAllBytes(filePath));
			vConfig = mapper.readValue(temp, VideoAnalysisConfig.class);
			validateConfig(vConfig);
			vaConfigMap.put(vConfig.desc, vConfig);
			return vConfig;
		} catch (PatternSyntaxException e) {
			error = String.format("Invalid regex pattern :%s", e.getMessage());
		} catch (JsonParseException e) {
			error = String.format("VideoAnalysisConfig failed to de-serialize :%s", e.getMessage());
		} catch (JsonMappingException e) {
			error = String.format("VideoAnalysisConfig failed to de-serialize :%s", e.getMessage());
		} catch (IOException e) {
			error = String.format("VideoAnalysisConfig failed to load :%s", e.getMessage());
		}

		// TODO hand this problem back to user
		log.error(error);
		return null;
	}

	@Override
	public VideoAnalysisConfig saveConfigFile(VideoType videoType, String desc, String type, String regex, String headerRegex, String responseRegex, VideoDataTags[] xref)
			throws JsonGenerationException, JsonMappingException, IOException, Exception {

		String suffix = ".json";
		String fName = desc + (desc.endsWith(suffix) ? "" : suffix);
		int regexCount = count(regex, "\\(") + count(headerRegex, "\\(") + count(responseRegex, "\\(");
		if (regexCount != xref.length) {
			log.error(String.format("%s: regex count %d != xref count %d", desc, regexCount, xref.length));
			throw new Exception("Bad Regex-VideoDataTag count");
		}
		VideoAnalysisConfig vConfig = new VideoAnalysisConfig(videoType, desc, type, regex, headerRegex, responseRegex, xref);
		mapper.writeValue(filemanager.createFile(folderPath, fName), vConfig);
		return vConfig;
	}

	@Override
	public boolean validateConfig(VideoAnalysisConfig vConfig){
		int regexCount = count(vConfig.getRegex(), "\\(") + count(vConfig.getHeaderRegex(), "\\(") + count(vConfig.getResponseRegex(), "\\(");
		boolean result = (vConfig.getXref().length == regexCount);
		vConfig.setValid(result);
		return result;
	}
	
	/**
	 * count occurrences of sRegex in the target string
	 * 
	 * @param targetRegex
	 * @param sRegex
	 * @return count
	 */
	@Override
	public int count(String targetRegex, String sRegex) {
		if (targetRegex == null || targetRegex.isEmpty()) {
			return 0;
		}
		String test = targetRegex.replaceAll("\\\\\\(", "xxy");
		int len = test.split(sRegex).length;
		return len > 1 ? len - 1 : 0;
	}

	@Override
	public String[] match(VideoAnalysisConfig vConfig, String requestStr) {
		String[] temp = null;
		if (vConfig.getRegex().isEmpty()) {
			// TODO maybe don't fail silently
			return new String[] {};
		}
		Matcher matcher = vConfig.getPattern().matcher(requestStr);
		if (matcher.find()) {
			temp = new String[matcher.groupCount()];
			for (int index = 0; index < matcher.groupCount();) {
				temp[index++] = matcher.group(index);
			}
		}
		return temp;
	}

	@SuppressWarnings("null")
	@Override
	public String[] match(VideoAnalysisConfig vConfig, String requestStr, String headerStr, String responseStr) {

		String[] temp = null;

		if (vConfig != null) {
			int cntReq = 0;
			int cntHdr = 0;
			int cntResp = 0;

			Matcher matcherHdr = null;
			// don't scan header if no regex for header
			if ((headerStr != null && !headerStr.isEmpty()) && (vConfig.getHeaderPattern() != null && !vConfig.getHeaderRegex().isEmpty())) {
				matcherHdr = vConfig.getHeaderPattern().matcher(headerStr);
				if (matcherHdr.find()) {
					cntHdr = matcherHdr.groupCount();
				}
			}

			Matcher matcherResp = null;
			if ((responseStr != null && !responseStr.isEmpty()) && (vConfig.getResponsePattern() != null && !vConfig.getResponseRegex().isEmpty())) {
				matcherResp = vConfig.getResponsePattern().matcher(responseStr);
				if (matcherResp.find()) {
					cntResp = matcherResp.groupCount();
				}
			}

			Matcher matcher = null;
			if (requestStr != null && !requestStr.isEmpty() && vConfig.getPattern() != null) {
				matcher = vConfig.getPattern().matcher(requestStr);
				if (matcher.find()) {
					cntReq = matcher.groupCount();
				}
			}

			temp = new String[cntHdr + cntResp + cntReq];
			if (temp != null) {
				int ptr = 0;
				if (cntReq > 0) {
					for (int index = 0; index < matcher.groupCount();) {
						temp[ptr++] = matcher.group(++index);
					}
				}
				if (cntHdr > 0) {
					for (int index = 0; index < matcherHdr.groupCount();) {
						temp[ptr++] = matcherHdr.group(++index);
					}
				}
				if (cntResp > 0) {
					for (int index = 0; index < matcherResp.groupCount();) {
						temp[ptr++] = matcherResp.group(++index);
					}
				}
			}
		}
		return temp;
	}
	
	@Override
	public String getFolderPath(){
		if (!filemanager.directoryExist(folderPath)){
			initConfigFiles();
		}
		return folderPath;
	}
}
