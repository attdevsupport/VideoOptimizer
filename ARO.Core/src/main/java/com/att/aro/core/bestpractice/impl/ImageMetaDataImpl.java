/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core.bestpractice.impl;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.ImageMdataEntry;
import com.att.aro.core.bestpractice.pojo.ImageMdtaResult;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.Util;

public class ImageMetaDataImpl implements IBestPractice {
	@Value("${imageMetadata.title}")
	private String overviewTitle;

	@Value("${imageMetadata.detailedTitle}")
	private String detailTitle;

	@Value("${imageMetadata.desc}")
	private String aboutText;

	@Value("${imageMetadata.url}")
	private String learnMoreUrl;

	@Value("${imageMetadata.pass}")
	private String textResultPass;

	@Value("${imageMetadata.results}")
	private String textResults;

	@Value("${exportall.csvNumberOfMdataImages}")
	private String exportNumberOfMdataImages;

	@InjectLogger
	private static ILogger log;

	private boolean isMetaDataPresent = false;

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		ImageMdtaResult result = new ImageMdtaResult();
		List<String> imageList = new ArrayList<String>();
		List<ImageMdataEntry> entrylist = new ArrayList<ImageMdataEntry>();
		// boolean isMetadataPresent = false;
		for (Session session : tracedata.getSessionlist()) {

			for (HttpRequestResponseInfo req : session.getRequestResponseInfo()) {

				if (req.getDirection() == HttpDirection.RESPONSE && req.getContentType() != null
						&& req.getContentType().contains("image/")) {

					String tracePath = tracedata.getTraceresult().getTraceDirectory()
							+ System.getProperty("file.separator");
					String imagePath = tracePath + "Image" + System.getProperty("file.separator");
					String imgFile = "";

					String extractedImageName = extractFullNameFromRRInfo(req);
					int pos = extractedImageName.lastIndexOf('.') + 1;

					// List<String> imageList = new ArrayList<String>();
					File folder = new File(imagePath);
					File[] listOfFiles = folder.listFiles();
					if(listOfFiles != null) {
						runTestForFiles(imageList, entrylist, session, req, imagePath, imgFile, extractedImageName, pos,
								listOfFiles);
					}
				}
			}
		}

		result.setResults(entrylist);
		String text = "";
		if (entrylist.isEmpty()) {
			result.setResultType(BPResultType.PASS);
			text = MessageFormat.format(textResultPass, entrylist.size());
			result.setResultText(text);
		} else {
			result.setResultType(BPResultType.FAIL);
			text = MessageFormat.format(textResults, 
										ApplicationConfig.getInstance().getAppShortName(), 
										entrylist.size());
			result.setResultText(text);
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, 
													ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		result.setExportNumberOfMdataImages(String.valueOf(entrylist.size()));
		return result;
	}

	private void runTestForFiles(List<String> imageList, List<ImageMdataEntry> entrylist, Session session,
			HttpRequestResponseInfo req, String imagePath, String imgFile, String extractedImageName, int pos,
			File[] listOfFiles) {
		String imgFullName = "";
		String imgExtn = "";

		// check folder exists
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				imgFullName = listOfFiles[i].getName();
				if (extractedImageName.equalsIgnoreCase(imgFullName)) {
					imgExtn = imgFullName.substring(pos, imgFullName.length());
					imgFile = imagePath + imgFullName;
					if (Util.isJPG(new File(imgFile), imgExtn)) {
						extractMetadata(imgFile);
					}
					// isMetaDataPresent = true;
				} // clear
			}

			if (isMetaDataPresent) {
				File getImage = new File(imgFile);
				JpegImageParser imgP = new JpegImageParser();
				byte[] mdata = null;
				long mSize = 0;

				double imgFileSize;
				double mdataSize;

				try {
					mdata = imgP.getExifRawData(new ByteSourceFile(getImage));
					mSize = mdata.length;
				} catch (ImageReadException | IOException e) {

				}
				imageList.add(imgFile);

				long iSize = getImage.length();

				imgFileSize = iSize / 1024;
				mdataSize = mSize / 1024;

				double savings = (mSize * 100) / iSize;

				if (savings >= 1.00) {
					entrylist.add(new ImageMdataEntry(req, session.getDomainName(), imgFile, imgFileSize, mdataSize,
							String.valueOf(new DecimalFormat("##.##").format(savings)) + "%"));
				}
				isMetaDataPresent = false;
			}
		}
	}

	private void extractMetadata(String fullpath) {
		ImageMetadata metadata;
		try {
			metadata = Imaging.getMetadata(new File(fullpath));
			if (metadata != null) {
				if (!metadata.getClass().equals(GenericImageMetadata.class)) {
					JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
					TiffImageMetadata exif = jpegMetadata.getExif();
					if (exif != null) {
						isMetaDataPresent = true;
					}
				} else {
					GenericImageMetadata genMetadata = (GenericImageMetadata) metadata;
					if (genMetadata.getItems() != null && genMetadata.getItems().size() > 5) {
						isMetaDataPresent = true;
					} else if (genMetadata.getItems() != null && genMetadata.getItems().isEmpty()) {
						isMetaDataPresent = false;
					}
				}
			}
		} catch (IOException | ImageReadException imgException) {
			log.error(imgException.toString());
		}

	}

	private String extractFullNameFromRRInfo(HttpRequestResponseInfo hrri) {
		HttpRequestResponseInfo rsp = hrri.getAssocReqResp();
		String extractedImageName = "";
		String imageName = "";
		if (rsp != null) {
			String imagefromReq = rsp.getObjName();
			imageName = imagefromReq.substring(imagefromReq.lastIndexOf(Util.FILE_SEPARATOR) + 1);
			int pos = imageName.lastIndexOf("/") + 1;
			extractedImageName = imageName.substring(pos);
		}
		return extractedImageName;
	}

}// end class
