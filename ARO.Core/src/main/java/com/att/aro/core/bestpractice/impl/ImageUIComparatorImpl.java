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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.ILogger;
import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.bestpractice.pojo.ImageComparatorResult;
import com.att.aro.core.bestpractice.pojo.ImageMdataEntry;
import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.IStringParse;
import com.att.aro.core.util.Util;

public class ImageUIComparatorImpl implements IBestPractice {

	@InjectLogger
	private  ILogger logger;
	
	
	@Autowired
	private IFileManager filemanager;

	@Autowired
	private IStringParse iStringParse;
	
	@InjectLogger
	private static ILogger log;
	
	@Value("${uiComparator.title}")
	private String overviewTitle;

	@Value("${uiComparator.detailedTitle}")
	private String detailTitle;

	@Value("${uiComparator.desc}")
	private String aboutText;

	@Value("${uiComparator.url}")
	private String learnMoreUrl;

	@Value("${uiComparator.pass}")
	private String textResultPass;

	@Value("${uiComparator.results}")
	private String textResults;
	
	@Value("${exportall.csvNumberOfUIComparatorImages}")
	private String numberOfImages;

	long orginalImagesSize = 0L;
	long convImgsSize = 0L;
	PacketAnalyzerResult tracedataResult = null;

	 String uiComparatorFolderPath = "";
	 String convExtn = "";
	 String imageFolderPath = "";
	 String htmlFolderPath = "";
	 
	 Pattern boundsPattern = Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]");

	
	 Map<String, Integer> originalImageDimensionMap = new TreeMap<String, Integer>();
	 Map<String, HttpRequestResponseInfo> reqRespMap = new TreeMap<String, HttpRequestResponseInfo>();
	 List<ImageMdataEntry> entrylist = new ArrayList<ImageMdataEntry>();

	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		ImageComparatorResult result = new ImageComparatorResult();
		originalImageDimensionMap.clear();
		tracedataResult = tracedata;
		String tracePath = tracedata.getTraceresult().getTraceDirectory() + System.getProperty("file.separator");
		uiComparatorFolderPath = tracePath + "UIComparator" + System.getProperty("file.separator");
		imageFolderPath = tracePath + "Image" + System.getProperty("file.separator");
		htmlFolderPath = tracePath + "HTML" + System.getProperty("file.separator");

		File uiComparatorFolder = new File(uiComparatorFolderPath);

		String windowsCompFolderPath = tracePath + "ARO" + System.getProperty("file.separator") + "UIComparator"
				+ System.getProperty("file.separator");
		if (new File(windowsCompFolderPath).exists()) {
			moveUIXmlFolder(new File(windowsCompFolderPath), uiComparatorFolder);
		}
		entrylist = new ArrayList<ImageMdataEntry>();
		if (new File(imageFolderPath).exists()) {
			if (uiComparatorFolder.exists() && uiComparatorFolder.isDirectory()) {
				getImageList();
				// createOriginalImageMap();
				Map<String, Integer> xmlUIMap = getUIXmlMap(filemanager.createFile(uiComparatorFolderPath));
				Map<String, String> imageNameMap = parseImageNames(htmlFolderPath);
				Map<String, Integer> imageDimensionMap = updateImageDimensionMap(imageNameMap);
				compareImages(imageDimensionMap, xmlUIMap, imageNameMap);
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
			text = MessageFormat.format(textResults, entrylist.size());
			result.setResultText(text);
			result.setNumberOfImages(String.valueOf(entrylist.size()));
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(MessageFormat.format(learnMoreUrl, ApplicationConfig.getInstance().getAppUrlBase()));
		result.setOverviewTitle(overviewTitle);

		return result;
	}

	private void getImageList() {
		String originalImage = "";

		long orgImageSize = 0L;
		String imgExtn = "";

		for (Session session : tracedataResult.getSessionlist()) {
			for (HttpRequestResponseInfo reqResp : session.getRequestResponseInfo()) {

				if (reqResp.getDirection() == HttpDirection.RESPONSE && reqResp.getContentType() != null
						&& reqResp.getContentType().contains("image/")) {

					originalImage = Util.extractFullNameFromRequest(reqResp);
					File orgImage = new File(imageFolderPath + originalImage);
					orgImageSize = orgImage.length();
					int pos = originalImage.lastIndexOf(".");
					imgExtn = originalImage.substring(pos + 1, originalImage.length());

					if (orgImageSize > 0 && Util.isJPG(orgImage, imgExtn)) {
						getOriginalImageDimensionMap(originalImage, imgExtn, pos, reqResp);
					}
				}
			}
		}
	}

	private  void getOriginalImageDimensionMap(String orgImage, String imgExtn, int pos, HttpRequestResponseInfo reqResp) {

		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(imgExtn);
		if (iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				ImageInputStream stream = new FileImageInputStream(filemanager.createFile(imageFolderPath, orgImage));
				reader.setInput(stream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				String imgName = orgImage.substring(0, pos);
				originalImageDimensionMap.put(imgName, width * height);

				reqRespMap.put(imgName, reqResp);

			} catch (IOException e) {
				logger.info("No reader found for given format: " + imgExtn);
			} finally {
				reader.dispose();
			}
		} else {
			logger.info("No reader found for given format: " + imgExtn);
		}

	}

	private Map<String, Integer> getUIXmlMap(File uiComparatorFolder) {

		File[] listOfFiles = uiComparatorFolder.listFiles();
		String emptyBounds = "[0,0][0,0]";
		Map<String, Integer> uiXmlMap = new TreeMap<String, Integer>();
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				File uiXMLFile = listOfFiles[i];
				if (uiXMLFile.isFile() && isXmLFile(uiXMLFile.getPath())) {
					try {
						File fXmlFile = filemanager.createFile(uiComparatorFolderPath + uiXMLFile.getName());

						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						Document doc = dBuilder.parse(fXmlFile);

						doc.getDocumentElement().normalize();

						NodeList nList = doc.getElementsByTagName("node");

						for (int temp = 0; temp < nList.getLength(); temp++) {

							Node nNode = nList.item(temp);

							if (nNode.getNodeType() == Node.ELEMENT_NODE) {

								Element eElement = (Element) nNode;

								if (!eElement.getAttribute("content-desc").isEmpty()
										&& !emptyBounds.equalsIgnoreCase(eElement.getAttribute("bounds"))) {

									int imageDimension = getXYPoints(eElement.getAttribute("bounds"));
									if (imageDimension > 0) {
										if(Util.checkDevMode() ){
											log.debug("XML : " + uiXMLFile.getName()+ " IMG : "+eElement.getAttribute("content-desc"));
										}
										uiXmlMap.put(eElement.getAttribute("content-desc"), imageDimension);
									}

								}

							}
						}
					} catch (IOException | ParserConfigurationException | SAXException e) {
						log.error(e.toString());
					}
				}
			}
		}
		return uiXmlMap;
	}

	private  boolean isXmLFile(String name) {
		boolean isXML = false;
		if (name != null && name.substring(name.lastIndexOf('.')).equalsIgnoreCase(".xml")) {
			isXML = true;
		}
		return isXML;
	}

	
	private int getXYPoints(String bounds) {

		int imagePixelSize = 0;

		String[] parsed = iStringParse.parse(bounds, boundsPattern);
		if (parsed != null && parsed.length == 4) {
			int xWidth = Integer.parseInt(parsed[2]) - Integer.parseInt(parsed[0]);
			int yHeight = Integer.parseInt(parsed[3]) - Integer.parseInt(parsed[1]);
			if (xWidth > 0 && yHeight > 0) {
				imagePixelSize = xWidth * yHeight;
			}
		}

		return imagePixelSize;
	}

	private void compareImages(Map<String, Integer> imageDimensionMap, Map<String, Integer> xmlUIMap,
			Map<String, String> imageNameMap) {
		for (Map.Entry<String, Integer> imageDimensionEntry : imageDimensionMap.entrySet()) {
			if (imageDimensionEntry != null && imageDimensionEntry.getValue() != null
					&& xmlUIMap.get(imageDimensionEntry.getKey()) != null) {
				int uiBoundsSize = xmlUIMap.get(imageDimensionEntry.getKey());
				int imgSize = imageDimensionEntry.getValue();
				if (imgSize > uiBoundsSize) {
					int dimenstionRatio = (imgSize - uiBoundsSize) * 100 / imgSize;
					if (dimenstionRatio >= 50) {
						getOriginalImageName(imageDimensionEntry.getKey(), uiBoundsSize, imageNameMap, imgSize,
								dimenstionRatio);
					}
				}
			}
		}
	}

	private  void getOriginalImageName(String imageName, int boundsSize, Map<String, String> imageNameMap, int orgImageSize, int dimenstionRatio) {
		for (Map.Entry<String, String> entry : imageNameMap.entrySet()) {
			if (entry.getValue().equalsIgnoreCase(imageName)) {
				HttpRequestResponseInfo reqResponseInfo = reqRespMap.get(entry.getKey());
				if (reqResponseInfo != null) {
					entrylist.add(new ImageMdataEntry(reqResponseInfo, entry.getKey(), orgImageSize, boundsSize,
							String.valueOf(new DecimalFormat("##.##").format(dimenstionRatio)) + "%"));
				}

			}
		}
	}

	private  Map <String, Integer>  updateImageDimensionMap(Map<String, String> imageNameMap) {
		
		Map <String, Integer> imageDimensionMap = new TreeMap <String, Integer>();
		for (Map.Entry<String, String> entry : imageNameMap.entrySet()) {
			if(entry!=null){
				imageDimensionMap.put(entry.getValue(), originalImageDimensionMap.get(entry.getKey()));
			}		
		}		
		return imageDimensionMap;
	}
	
	public static Map<String, String> parseImageNames(String htmlFolderPath) {
		File[] listOfFiles = new File(htmlFolderPath).listFiles();
		Map<String, String> imageNameMap = new TreeMap<String, String>();
		String imageDowloadName = "";
		String imageUIName = "";
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				File htmlFile = listOfFiles[i];
				if (htmlFile.isFile()) {
					try {
						org.jsoup.nodes.Document doc = Jsoup.parse(new File(htmlFolderPath + htmlFile.getName()), "utf-8");
						org.jsoup.select.Elements links = doc.select("img[src]");
						for (org.jsoup.nodes.Element link : links) {
							if (!link.attr("src").isEmpty() && !link.attr("alt").isEmpty()) {
								imageDowloadName = link.attr("src");
								imageUIName = link.attr("alt");
								imageNameMap.put(imageDowloadName.substring(imageDowloadName.lastIndexOf('/') + 1,
										imageDowloadName.lastIndexOf('.')), imageUIName);
							}
						}

					} catch (IOException e) {
						log.error(e.toString());
					}

				}
			}
		}
		return imageNameMap;
	}
	
	private void moveUIXmlFolder(File sourceFolder, File destFolder) {
		if (sourceFolder.isDirectory()) {
			try {
				Files.move(Paths.get(sourceFolder.getPath()), Paths.get(destFolder.getPath()),
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.info("Error moving UIComparator folder");
			}
		}
	}
}
