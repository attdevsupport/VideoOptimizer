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
import com.att.aro.core.util.Util;

/**
 * This class extracts HTML from traffic
 *
 * @Yogi
 */
public class HtmlExtractor {

	private static final Logger LOGGER = LogManager.getLogger(HtmlExtractor.class.getName());

	@Autowired
	private IFileManager fileManager;

	@Autowired
	private IHttpRequestResponseHelper reqhelper;

	private String htmlPath;

	@Value("${ga.request.timing.imageAnalysisTimings.title}")
	private String imageAnalysisTitle;
	@Value("${ga.request.timing.analysisCategory.title}")
	private String analysisCategory;

	public void execute(AbstractTraceResult result, List<Session> sessionList,
			Map<Double, HttpRequestResponseInfo> requestMap) {
		htmlPath = result.getTraceDirectory() + Util.FILE_SEPARATOR + "HTML" + Util.FILE_SEPARATOR;

		if (!fileManager.directoryExistAndNotEmpty(htmlPath)) {
			if (!fileManager.directoryExist(htmlPath)) {
				fileManager.mkDir(htmlPath);

				for (final Session session : sessionList) {
					for (final HttpRequestResponseInfo req : session.getRequestResponseInfo()) {
						if (req.getDirection() == HttpDirection.RESPONSE && req.getContentType() != null
								&& req.getContentType().contains("text/html")) {
							extractHtmlContent(session, req);
						}
					}
				}
			}
		}
	}

	private void extractHtmlContent(Session session, HttpRequestResponseInfo response) {
		if (response != null) {
			byte[] content = null;
			try {
				content = reqhelper.getContent(response, session);
				if (content != null && content.length > 0) {
					fileManager.saveFile(new ByteArrayInputStream(content),
							htmlPath + Long.toString(System.currentTimeMillis()) + ".html");
				}
			} catch (Exception e) {
				LOGGER.info("Failed to extract HTML " + e.getMessage());
				return;
			}
		}
	}
}
