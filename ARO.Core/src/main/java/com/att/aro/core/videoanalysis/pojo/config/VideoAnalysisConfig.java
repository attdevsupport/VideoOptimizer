package com.att.aro.core.videoanalysis.pojo.config;
/*
 * put copyright here
 */

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.att.aro.core.videoanalysis.impl.RegexMatchLbl;
import com.att.aro.core.videoanalysis.pojo.VideoEvent.VideoType;

/*
 * <pre>
 *    GET requests by type of video
 * 
 *         AMZ_VOD_mpd
 *         http://a19avoddashs3us-a.akamaihd.net/d/1$A43PXU4ZN2AL1,6B4DCD80/videoquality$480p/ondemand/c4ea/0a2e/f7f7/481b-9753-a90e82e9b0e8/ced3d82d-e939-418b-a06c-59e11f7e1bdd_video_2.mp4
 *         http://d2lkq7nlcrdi7q.cloudfront.net/dm/1$A43PXU4ZN2AL1,6B4DCD90/videoquality$480p/e526/e8a7/642e/4a93-9835-333e61596997/bcad2f63-972a-4f94-8942-16413d02e7cc_video_8.mp4
 *         http://d2lkq7nlcrdi7q.cloudfront.net/dm/2$9JcJ5ECWjBq_SQj39xoTn0kdiLA~/3c37/84fa/e6bf/4d08-ad6f-37482a24198f/feffb945-4677-4ea2-9044-4791abfa1b0e_video_1.mp4
 *         http://s3.lvlt.dash.us.aiv-cdn.net/d/1$A43PXU4ZN2AL1,6B4DCDE9/videoquality$480p/prod/2587/73e7/f9a3/480b-a1b5-7c50f0469498/d1299077-6907-4a09-bcf4-fef41f2988d4_video_3.mp4
 *         http://s3.ll.dash.us.aiv-cdn.net/d/1$A43PXU4ZN2AL1,6B4DCDE9/videoquality$480p/2587/73e7/f9a3/480b-a1b5-7c50f0469498/d1299077-6907-4a09-bcf4-fef41f2988d4_video_3.mp4
 * 
 *         AMZ_VOD_ssm
 *         http://ds79lt46qzmj0.cloudfront.net/dm/2$w-i4rGny79gdQDF6YsenLjtAzZ0~/6d64/b2c7/6f71/4725-b1e2-b68bb43c0171/7b81c27d-83fc-4f78-98ca-d549ed3a211c.ism/QualityLevels(450000)/Fragments(video=560560000)
 * 
 * 
 *         DTV: VOD
 *         http://directv-vod.hls.adaptive.level3.net/aav/30/B001573958U3/HLS4/B001573958U0_4_289.ts?exptime=1474072717&token=fa36ffe95386a7384260f920d813f101
 *         movie
 *         http://directvc3m-prod-vod.hls.adaptive.level3.net/c3/30/movie/2016_12/B002021484/B002021484U3/06/33.ts?exptime=1481067556&token=3d469e9848c0679ff4cc0cd84364fc4e
 * 
 *         livetv
 *         http://directvlst-live.hls.adaptive.level3.net/livetv/30/8249/05/20161122T223049150.ts?exptime=1479868099&token=aab902d3ef9e795f1cd9d51bb8c99c53
 *         livetv http://directvlst.vo.llnwd.net/e1/livetv/30/1363/03/20170120T005340128.ts?p=43&e=1484888011&h=90341b161288c119408ee821bbea41d6
 *         http://aav-akamai3.directv.com.edgesuite.net/aav/30/B001844891U3/HLS2/B001844891U0_2_11.ts?exptime=1476236042&token=ce277f64e9b02097f4348298653ea86a
 */

/**
 * <pre>
 * Configuration model for collecting and analyzing video streaming.
 * This class contains REGEX strings and Pattern objects
 * 
 * @author Barry Nelson
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true) // allows for changes dropping items or using older versions, but not before this ignore
public class VideoAnalysisConfig {

	public VideoType videoType;
	public String desc;
	public String type;
	public String regex;
	public String headerRegex;
	public String responseRegex;
	public Pattern pattern;
	public VideoDataTags[] xref;
	private Pattern headerPattern;
	private Pattern responsePattern;
	private boolean validated = false;
	private Map<RegexMatchLbl, VideoDataTags[]> xrefMap;
	public VideoAnalysisConfig() {
		// dummy constructor to make json happy
	}

	public VideoAnalysisConfig(VideoType videoType, String desc, String type, String regex, String headerRegex, String responseRegex, VideoDataTags[] xref) throws PatternSyntaxException {
		setVideoType(videoType);
		setDesc(desc);
		setType(type);
		setRegex(regex != null ? regex : "");
		setHeaderRegex(headerRegex != null ? headerRegex : "");
		setResponseRegex(responseRegex != null ? responseRegex : "");
		setXref(xref);
	}

	public VideoAnalysisConfig(VideoType videoType, String desc, String type, String regex, String headerRegex,
			String responseRegex, VideoDataTags[] xref, Map<RegexMatchLbl, VideoDataTags[]> xrefMap)
			throws PatternSyntaxException {
		setVideoType(videoType);
		setDesc(desc);
		setType(type);
		setRegex(regex != null ? regex : "");
		setHeaderRegex(headerRegex != null ? headerRegex : "");
		setResponseRegex(responseRegex != null ? responseRegex : "");
		setXref(xref);
		setXrefMap(xrefMap);
	}

	public VideoType getVideoType() {
		return videoType;
	}

	public void setVideoType(VideoType videoType) {
		this.videoType = videoType;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRegex() {
		return regex;
	}

	public String getHeaderRegex() {
		return headerRegex;
	}

	private String compiler(String targetPattern, String regex) {
		try {
			
			Pattern pattern = Pattern.compile(regex != null ? regex : "");
			switch (targetPattern) {
			case "pattern":
				this.pattern = pattern;
				break;
			case "headerPattern":
				this.headerPattern = pattern;
				break;
			case "responsePattern":
				this.responsePattern = pattern;
				break;
			default:
				return "bad command used";
			}
		} catch (PatternSyntaxException e) {
			return e.getMessage();
		} catch (Exception e) {
			return e.getMessage();
		}
		return "";
	}

	public String setRegex(String regex) throws PatternSyntaxException {
		this.regex = regex;
		return compiler("pattern", regex);
	}

	public String setHeaderRegex(String headerRegex) throws PatternSyntaxException {
		this.headerRegex = headerRegex;
		return compiler("headerPattern", headerRegex);
	}
	
	public String getResponseRegex() {
		return responseRegex;
	}

	public String setResponseRegex(String responseRegex) {
		this.responseRegex = responseRegex;
		return compiler("responsePattern", responseRegex);
	}

	public Pattern getPattern() {
		return pattern;
	}

	public Pattern getHeaderPattern() {
		return headerPattern;
	}
	
	public Pattern getResponsePattern() {
		return responsePattern;
	}

	public VideoDataTags[] getXref() {
		return xref;
	}

	public void setXref(VideoDataTags[] xref) {
		this.xref = xref;
	}

	@Override
	public String toString() {
		StringBuilder strblr = new StringBuilder(60);
		strblr.append("VideoAnalysisConfig : desc = ");
		strblr.append(desc);
		strblr.append("\n\t, regex = ");
		strblr.append(regex);
		if (headerRegex != null && !headerRegex.isEmpty()) {
			strblr.append("\n\t, headerRegex = ");
			strblr.append(headerRegex);
		}
		if (responseRegex != null && !responseRegex.isEmpty()) {
			strblr.append("\n\t, responseRegex = ");
			strblr.append(responseRegex);
		}
		strblr.append("\n\t, xref ");
		for (VideoDataTags ref : xref) {
			strblr.append(',');
			strblr.append(ref.toString());
		}
		return strblr.toString();
	}

	/**
	 * <pre>
	 * Validate string for a match with the Pattern perform a pattern.matcher(string).find()
	 * 
	 * @param string
	 * @return true if found, else false
	 */
	public boolean patternFind(String string) {
		if (pattern == null) {
			return false;
		}
		return pattern.matcher(string).find();
	}

	/**
	 * Record validity of config
	 * 
	 * @param result
	 */
	public void setValid(boolean result) {
		this.validated  = result;
		
	}

	public boolean isValidated() {
		return validated;
	}

	@JsonProperty("xrefMap")
	public Map<RegexMatchLbl, VideoDataTags[]> getXrefMap() {
		return xrefMap;
	}

	public void setXrefMap(Map<RegexMatchLbl, VideoDataTags[]> xrefMap) {
		this.xrefMap = new HashMap<>(xrefMap);
	}
}
