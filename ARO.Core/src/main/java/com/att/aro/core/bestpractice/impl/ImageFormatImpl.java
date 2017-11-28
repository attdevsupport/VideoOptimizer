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

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.ImageFormatResult;
import com.att.aro.core.bestpractice.pojo.ImageMdataEntry;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.util.Util;
import com.luciad.imageio.webp.WebPWriteParam;
import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;


//FIXME ADD UNIT TESTS
public class ImageFormatImpl implements IBestPractice {

	@InjectLogger
	private static ILogger logger;

	@Value("${imageFormat.title}")
	private String overviewTitle;

	@Value("${imageFormat.detailedTitle}")
	private String detailTitle;

	@Value("${imageFormat.desc}")
	private String aboutText;

	@Value("${imageFormat.url}")
	private String learnMoreUrl;

	@Value("${imageFormat.pass}")
	private String textResultPass;

	@Value("${imageFormat.results}")
	private String textResults;

	@Autowired
	private IFileManager filemanager;
	
	long orginalImagesSize = 0L;
	long convImgsSize = 0L;
	PacketAnalyzerResult tracedataResult = null;

	String imageFolderPath = "";
	String convExtn = "";

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		tracedataResult = tracedata;
		ImageFormatResult result = new ImageFormatResult();
		String tracePath = tracedata.getTraceresult().getTraceDirectory() + System.getProperty("file.separator");
		imageFolderPath = tracePath + "Image" + System.getProperty("file.separator");
	
		if (isAndroid()) {
			convExtn = "webp";
		} else {
			convExtn = "jp2";
		}

		if (!isImagesConverted()) {
			try {
				formatImages();
			} catch (Exception imgException) {
				logger.error("Image Format  exception : ", imgException);
			}
		}

		List<ImageMdataEntry> entrylist = getEntryList();

		result.setResults(entrylist);
		String text = "";
		String totalSavings = "";
		if (entrylist.isEmpty()) {
			result.setResultType(BPResultType.PASS);
			text = MessageFormat.format(textResultPass, entrylist.size());
			result.setResultText(text);
		} else {
			result.setResultType(BPResultType.FAIL);
			long savings = orginalImagesSize - convImgsSize;
			totalSavings = Long.toString(savings / 1024);
			text = MessageFormat.format(textResults,entrylist.size(), totalSavings);
			result.setResultText(text);
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		return result;
	}


	public boolean isAndroid() {
		boolean isAndroid = false;
		if (tracedataResult.getTraceresult() instanceof TraceDirectoryResult) {
			TraceDirectoryResult traceDirectoryResult = (TraceDirectoryResult) tracedataResult.getTraceresult();
			if (traceDirectoryResult.getDeviceDetail().getOsType().equalsIgnoreCase("android")) {
				isAndroid = true;
			}
		} else {
			isAndroid = true;
		}
		return isAndroid;
	}

	private boolean isImagesConverted() {
		String imageFormatFolderPath = imageFolderPath + "Format";
		if (filemanager.directoryExist(imageFormatFolderPath)) {
			File folder = new File(imageFormatFolderPath);
			File[] listOfFiles = folder.listFiles();
			if (listOfFiles != null && listOfFiles.length != 0) {
				for (int i = 0; i < listOfFiles.length; i++) {
					if (listOfFiles[i].isFile()) {
						return true;
					}
				}
			}
		} else {
			filemanager.mkDir(imageFormatFolderPath);
		}
		return false;
	}

	private List<ImageMdataEntry> getEntryList() {
		String originalImage = "";

		long convertedImgSize = 0L;
		long orgImageSize = 0L;
		String imgExtn = "";
		long orgImgSize;
		long convImageSize;
		String convImage = "";
		String convertedImagesFolderPath = "";

		List<ImageMdataEntry> imgEntryList = new ArrayList<ImageMdataEntry>();
		for (Session session : tracedataResult.getSessionlist()) {
			for (HttpRequestResponseInfo reqResp : session.getRequestResponseInfo()) {

				if (reqResp.getDirection() == HttpDirection.RESPONSE && reqResp.getContentType() != null
						&& reqResp.getContentType().contains("image/")) {

					originalImage = ImageHelper.extractFullNameFromRRInfo(reqResp);
					File orgImage = new File(imageFolderPath + originalImage);
					orgImageSize = orgImage.length();
					int pos = originalImage.lastIndexOf(".");
					imgExtn = originalImage.substring(pos + 1, originalImage.length());

					if (orgImageSize > 0 && Util.isJPG(orgImage, imgExtn)) {

						convertedImagesFolderPath = imageFolderPath + "Format" + System.getProperty("file.separator");
						convImage = convertedImagesFolderPath
								+ originalImage.substring(0, originalImage.lastIndexOf(".") + 1) + convExtn;

						convertedImgSize = new File(convImage).length();
						long indSavings = (orgImageSize - convertedImgSize) * 100 / orgImageSize;
						if (convertedImgSize > 0 && (indSavings >= 15)) {

							orginalImagesSize = orginalImagesSize + orgImageSize;
							convImgsSize = convImgsSize + convertedImgSize;

							orgImgSize = orgImageSize / 1024;
							convImageSize = convertedImgSize / 1024;

							imgEntryList.add(new ImageMdataEntry(reqResp, session.getDomainName(),
									imageFolderPath + originalImage, orgImgSize, convImageSize,
									Long.toString(indSavings)));
						}
					}
				}
			}
			}
		return imgEntryList;
	}

	private void formatImages() {
		ExecutorService exec = Executors.newFixedThreadPool(5);
		for (final Session session : tracedataResult.getSessionlist()) {
			for (final HttpRequestResponseInfo req : session.getRequestResponseInfo()) {
				if (req.getDirection() == HttpDirection.RESPONSE && req.getContentType() != null
						&& req.getContentType().contains("image/")) {
					final String extractedImage = ImageHelper.extractFullNameFromRRInfo(req);

					File imgFile = new File(imageFolderPath + extractedImage);
					if (imgFile.exists() && !imgFile.isDirectory()) {
						int posExtn = extractedImage.lastIndexOf(".");
						String imgExtn = extractedImage.substring(posExtn + 1, extractedImage.length());
						if (Util.isJPG(imgFile, imgExtn)) {
							formatImage(extractedImage);
						}
					}
				}
			}
		}
		
		try {// Time out after 10 minutes
			exec.shutdown();
			exec.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			logger.error("Image Format execution exception : ", e);
		}
	}

	private void formatImage(String imgfile) {
		String orgImagePath = imageFolderPath + imgfile;
		String formattedImagePath = "";
		ImageOutputStream imageOutputStream = null;
		try {

			RenderedImage renderedImage = ImageIO.read(new File(orgImagePath));
			formattedImagePath = imageFolderPath + "Format" + System.getProperty("file.separator")
			+ imgfile.substring(0, imgfile.lastIndexOf(".") + 1) + convExtn;
			imageOutputStream = ImageIO.createImageOutputStream(new File(formattedImagePath));

			if (renderedImage != null) {
				if (convExtn.equalsIgnoreCase("webp")) {
					ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();

					WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
					writeParam.setCompressionMode(WebPWriteParam.MODE_EXPLICIT);
					writer.setOutput(imageOutputStream);
					writer.write(null, new IIOImage(renderedImage, null, null), writeParam);

					imageOutputStream.flush();
					writer.dispose();

				} else {
					ImageWriter jp2Writer = ImageIO.getImageWritersBySuffix("jp2").next();
					J2KImageWriteParam writeParams = (J2KImageWriteParam) jp2Writer.getDefaultWriteParam();
					writeParams.setLossless(false);
					writeParams.setCompressionMode(J2KImageWriteParam.MODE_EXPLICIT);
					writeParams.setFilter(J2KImageWriteParam.FILTER_97);
					writeParams.setCompressionType("JPEG2000");
					writeParams.setCompressionQuality(0.85f);
					jp2Writer.setOutput(imageOutputStream);
					jp2Writer.write(null, new IIOImage(renderedImage, null, null), writeParams);
					imageOutputStream.flush();
					jp2Writer.dispose();

				}
			}	
			imageOutputStream.close();
		
		} catch (IOException e) {
			logger.error("Format Image exception : ", e);
		}
	}

}
