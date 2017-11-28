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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.ImageCompressionEntry;
import com.att.aro.core.bestpractice.pojo.ImageCompressionResult;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.ImageHelper;
import com.att.aro.core.util.Util;

//FIXME ADD UNIT TESTS
public class ImageCompressionImpl implements IBestPractice {
	enum Quality {
		MID(85), LOW(70);
		private int percent;

		private Quality(int percent) {
			this.percent = percent;
		}

		public int getPercent() {
			return percent;
		}

		public float getFraction() {
			return ((float) percent) / 100;
		}

		public String getFileDesc() {
			return "_compressed_" + getPercent() + ".0";
		}
	}

	@InjectLogger
	private static ILogger LOGGER;

	static String FILE_DESC = "_compressed_";

	@Value("${imageCompression.title}")
	private String overviewTitle;

	@Value("${imageCompression.detailedTitle}")
	private String detailTitle;

	@Value("${imageCompression.desc}")
	private String aboutText;

	@Value("${imageCompression.url}")
	private String learnMoreUrl;

	@Value("${imageCompression.pass}")
	private String textResultPass;

	@Value("${imageCompression.results}")
	private String textResults;

	@Autowired
	private IFileManager filemanager;

	long orginalImagesSize = 0L;
	long midQualImgsSize = 0L;
	String imageFolderPath = "";
	PacketAnalyzerResult packetResult = new PacketAnalyzerResult();
	String imageCompressionFolderPath = "";

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		ImageCompressionResult result = new ImageCompressionResult();
		packetResult = tracedata;
		String tracePath = packetResult.getTraceresult().getTraceDirectory() + System.getProperty("file.separator");
		imageFolderPath = tracePath + "Image" + System.getProperty("file.separator");
		imageCompressionFolderPath = imageFolderPath + "Compressed"+ System.getProperty("file.separator");
		orginalImagesSize = 0L;
		midQualImgsSize = 0L;

		boolean isImagesCompressed = isImagesCompressed();

		if (!isImagesCompressed) {
			compressImages();
		}
		List<ImageCompressionEntry> entrylist = getEntryList();

		result.setResults(entrylist);
		String text = "";
		String totalSavings = "";
		if (entrylist.isEmpty()) {
			result.setResultType(BPResultType.PASS);
			text = MessageFormat.format(textResultPass, entrylist.size());
			result.setResultText(text);
		} else {
			result.setResultType(BPResultType.FAIL);
			long savings = orginalImagesSize - midQualImgsSize;
			if (savings > 1024) {
				totalSavings = Long.toString(savings / 1024) + " KB";
			} else {
				totalSavings = Long.toString(savings) + " B";
			}
			text = MessageFormat.format(textResults, totalSavings);
			result.setResultText(text);
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);
		return result;
	}

	private boolean isImagesCompressed() {

		if (filemanager.directoryExist(imageCompressionFolderPath)) {
			File folder = new File(imageFolderPath);
			File[] listOfFiles = folder.listFiles();
			if (listOfFiles != null && listOfFiles.length != 0) {
				for (int i = 0; i < listOfFiles.length; i++) {
					if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(FILE_DESC)) {
						return true;
					}
				}
			}
		} else {
			filemanager.mkDir(imageCompressionFolderPath);
		}
		return false;
	}

	private List<ImageCompressionEntry> getEntryList() {
		String originalImage = "";
		String midCompressedImagePath = "";
		String logCompressedImagepath = "";

		long lowQualityImgSize = 0L;
		long midQualityImgSize = 0L;
		long orgImageSize = 0L;
		String imgExtn = "";
		
		List<ImageCompressionEntry> entryList = new ArrayList<ImageCompressionEntry>();
		for (Session session : packetResult.getSessionlist()) {
			for (HttpRequestResponseInfo reqResp : session.getRequestResponseInfo()) {

				if (reqResp.getDirection() == HttpDirection.RESPONSE && reqResp.getContentType() != null
						&& reqResp.getContentType().contains("image/")) {

					originalImage = ImageHelper.extractFullNameFromRRInfo(reqResp);
					int pos = originalImage.lastIndexOf(".");
					if (pos != -1) {
						imgExtn = originalImage.substring(pos + 1, originalImage.length());
						if (Util.isJPG(new File(imageFolderPath + originalImage), imgExtn)) {

							midCompressedImagePath = imageCompressionFolderPath
									+ originalImage.substring(0, originalImage.lastIndexOf("."))
									+ Quality.MID.getFileDesc()
									+ originalImage.substring(originalImage.lastIndexOf("."), originalImage.length());

							logCompressedImagepath = imageCompressionFolderPath
									+ originalImage.substring(0, originalImage.lastIndexOf("."))
									+ Quality.LOW.getFileDesc()
									+ originalImage.substring(originalImage.lastIndexOf("."), originalImage.length());
							orgImageSize = new File(imageFolderPath + originalImage).length();
							midQualityImgSize = new File(midCompressedImagePath).length();
							lowQualityImgSize = new File(logCompressedImagepath).length();
							if (midQualityImgSize > 0 && ((orgImageSize - midQualityImgSize) * 100 / orgImageSize >= 15)
									&& lowQualityImgSize < orgImageSize) {

								orginalImagesSize = orginalImagesSize + orgImageSize;
								midQualImgsSize = midQualImgsSize + midQualityImgSize;

								
								
								entryList.add(new ImageCompressionEntry(reqResp, session.getDomainName(),
										imageFolderPath + originalImage, Util.doubleFileSize(orgImageSize), Util.doubleFileSize(midQualityImgSize),
										Util.doubleFileSize(lowQualityImgSize)));
							}

						}
					}
				}
			}
		}
		return entryList;
	}

	private void compressImages() {
		ExecutorService exec = Executors.newFixedThreadPool(5);
		for (final Session session : packetResult.getSessionlist()) {
			for (final HttpRequestResponseInfo req : session.getRequestResponseInfo()) {
				if (req.getDirection() == HttpDirection.RESPONSE && req.getContentType() != null
						&& req.getContentType().contains("image/")) {
					final String extractedImage = ImageHelper.extractFullNameFromRRInfo(req);

					File imgFile = new File(imageFolderPath + extractedImage);
					if (imgFile.exists() && !imgFile.isDirectory()) {
						int posExtn = extractedImage.lastIndexOf(".");
						String imgExtn = extractedImage.substring(posExtn + 1, extractedImage.length());
						if (Util.isJPG(imgFile, imgExtn)) {
							exec.submit(new Runnable() {
								@Override
								public void run() {
									compressImage(imageFolderPath, extractedImage);
								}
							});
						}
					}
				}
			}
		}
		try {// Time out after 10 minutes
			exec.shutdown();
			exec.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void compressImage(String imgPath, String imgfile) {
		String compressedImageName = "";
		String orgImagePath = imgPath + imgfile;
		File orgImgFile = new File(orgImagePath);
		try (InputStream inputStrm = new FileInputStream(orgImgFile)) {
			BufferedImage buffImage = ImageIO.read(inputStrm);
			for (Quality qual : Quality.values()) {
				compressedImageName = imageCompressionFolderPath
						+ imgfile.substring(0, imgfile.lastIndexOf(".")) + qual.getFileDesc() + "."
						+ imgfile.substring(imgfile.lastIndexOf(".") + 1, imgfile.length());
				File compressedFile = new File(compressedImageName);
				ImageWriter writer = null;
				try (OutputStream outputStr = new FileOutputStream(compressedFile);
						ImageOutputStream imgOutputStrm = ImageIO.createImageOutputStream(outputStr)) {

					Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(
							imgfile.substring(imgfile.lastIndexOf(".") + 1, imgfile.length()));
					writer = (ImageWriter) writers.next();
					writer.setOutput(imgOutputStrm);
					ImageWriteParam param = writer.getDefaultWriteParam();
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionQuality((float) qual.getFraction());
					writer.write(null, new IIOImage(buffImage, null, null), param);
				} finally {
					if (writer != null) {
						writer.dispose();
					}
				}
			}
		} catch (IOException fileException) {
			LOGGER.error(fileException.toString(), fileException);
		}
	}

}
