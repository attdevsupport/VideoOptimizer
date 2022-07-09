/*
 *  Copyright 2022 AT&T
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;

public class UIParserAnalysis {

	private static final Logger LOGGER = LogManager.getLogger(UIParserAnalysis.class.getName());

	String xmlFolderPath = "";

	public Map<String, UIParserNode> uiParseEventMap = new HashMap<String, UIParserNode>();

	public void analyze(PacketAnalyzerResult tracedata) {
		ExecutorService exec = Executors.newFixedThreadPool(5);
		String tracePath = tracedata.getTraceresult().getTraceDirectory() + System.getProperty("file.separator");
		xmlFolderPath = tracePath + "UIXML" + System.getProperty("file.separator");
		File xmlFolder = new File(xmlFolderPath);
		if (xmlFolder != null && xmlFolder.isDirectory()) {		
			File[] xmlFiles = xmlFolder.listFiles();
			if (xmlFiles != null) {
				for (int i = 0; i < xmlFiles.length; i++) {
					File xmlFile = xmlFiles[i];
					exec.submit(new Runnable() {
						@Override
						public void run() {
							parse(xmlFile);
						}
					});
				}
			}
		}
	}

	private void parse(File xmlFile) {
		if (!xmlFile.isFile()) {
			return;
		}

		try {
			String xmlFileName = xmlFile.getName();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(xmlFile);

			xmlDoc.getDocumentElement().normalize();

			NodeList nodeList = xmlDoc.getElementsByTagName("node");

			for (int temp = 0; temp < nodeList.getLength(); temp++) {

				Node nNode = nodeList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					String classType = eElement.getAttribute("class");
					if (classType.contains("ImageView")) {
						UIParserNode uIParserNode = new UIParserNode(eElement.getAttribute("bounds"), classType,
								eElement.getAttribute("index"), eElement.getAttribute("package"));

						uiParseEventMap.put(xmlFileName.substring(0, xmlFileName.indexOf(".")), uIParserNode);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while parsing UI xml", e);
		}

	}

}