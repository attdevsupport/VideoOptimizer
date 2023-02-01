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
package com.att.aro.core.packetanalysis.impl;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.packetanalysis.IHttpRequestResponseHelper;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.GoogleAnalyticsUtil;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.Util;
import com.att.aro.core.util.VideoImageAnalysisUtil;

import lombok.Setter;

/**
 * This class extracts Images from traffic
 *
 * @Yogi
 */
public class ImageExtractor {

	private static final Logger LOGGER = LogManager.getLogger(ImageExtractor.class.getName());

	@Autowired
	@Setter
	private IHttpRequestResponseHelper httpRequestResponseHelper;
	
	@Autowired
	private IFileManager fileManager;
	
	@Autowired
	private IStringParse stringParse;

	private boolean imageExtractionRequired = false;
	private String imagePath;

	@Value("${ga.request.timing.imageAnalysisTimings.title}")
	private String imageAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;

	public void execute(AbstractTraceResult result, List<Session> sessionList, Map<Double, HttpRequestResponseInfo> requestMap) {

		imagePath = result.getTraceDirectory() + Util.FILE_SEPARATOR + "Image" + Util.FILE_SEPARATOR;
		long analysisStartTime = System.currentTimeMillis();

		if (!fileManager.directoryExist(imagePath)) {
			imageExtractionRequired = true;
			fileManager.mkDir(imagePath);
		} else {
			imageExtractionRequired = false;
		}
		
		for (HttpRequestResponseInfo req : requestMap.values()) {
			LOGGER.info(req.toString());
			if (req.getAssocReqResp() == null) {
				continue;
			}
			String fullName = VideoImageAnalysisUtil.extractFullNameFromRRInfo(req);
			String extn = VideoImageAnalysisUtil.extractExtensionFromName(req.getFileName(), stringParse);
			if (StringUtils.isNotBlank(extn)) {

				switch (extn) {
				case ".jpg":
				case ".gif":
				case ".tif":
				case ".png":
				case ".jpeg":
					if (imageExtractionRequired) {
						fullName = VideoImageAnalysisUtil.extractFullNameFromRRInfo(req);
						extractImage(req, fullName);
					}
					break;
				default:
					break;
				}
			}

			scanAndExtractImages(sessionList); 
			GoogleAnalyticsUtil.getGoogleAnalyticsInstance().sendAnalyticsTimings(imageAnalysisTitle,
					System.currentTimeMillis() - analysisStartTime, analysisCategory);
		}
	}

	public void scanAndExtractImages(List<Session> sessionlist) {
		if (fileManager.directoryExistAndNotEmpty(imagePath)) {
			return;
		}
		for (Session session : sessionlist) {
			for (HttpRequestResponseInfo reqResp : session.getRequestResponseInfo()) {
				if (reqResp.getDirection() == HttpDirection.RESPONSE && reqResp.getContentType() != null
						&& reqResp.getContentType().contains("image/")) {
					HttpRequestResponseInfo req = reqResp.getAssocReqResp();
					if (req == null) {
						LOGGER.error(String.format("BROKEN req/resp link @ %s", reqResp.getAllHeaders()));
						continue;
					}
					if (req.getObjName() == null || reqResp.getAssocReqResp() == null) {
						LOGGER.error("Probable Request/Response linkage problem with " + req + ", " + reqResp);
						continue;
					}
					String imageObject = reqResp.getAssocReqResp().getObjName();
					if (imageObject != null && reqResp.getContentType() != null) {
						if (imageObject.indexOf("?v=") >= 0) {
							String fullName = Util.extractFullNameFromLink(imageObject);
							String extension = "." + reqResp.getContentType().substring(
									reqResp.getContentType().indexOf("image/") + 6, reqResp.getContentType().length());
							extractImage(session, reqResp, fullName + extension);
						}
					}
				}
			}
		}
	}

	private void extractImage(Session session, HttpRequestResponseInfo response, String imageFileName) {
		if (response != null) {
			byte[] content = null;
			String fullpath;
			try {
				content = httpRequestResponseHelper.getContent(response, session);
				fullpath =  imagePath + imageFileName;
				fileManager.saveFile(new ByteArrayInputStream(content), fullpath);
			} catch (Exception e) {
				LOGGER.info("Failed to extract " + VideoImageAnalysisUtil.getTimeString(response) + imageFileName);
				return;
			}
		}
	}


	public void extractImage(HttpRequestResponseInfo request, String imageFileName) {
		HttpRequestResponseInfo response = request.getAssocReqResp();
		Session session = request.getSession();
		if (response != null) {
			byte[] content = null;
			String fullpath;
			try {
				content = httpRequestResponseHelper.getContent(response, session);
				fullpath = imagePath + imageFileName;
				fileManager.saveFile(new ByteArrayInputStream(content), fullpath);
			} catch (Exception e) {
				LOGGER.info("Failed to extract " + VideoImageAnalysisUtil.getTimeString(response) + imageFileName
						+ " response: " + e.getMessage());
				return;
			}
		}
	}

}// end class
