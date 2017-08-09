
package com.att.aro.core.bestpractice.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.att.aro.core.ILogger;
import com.att.aro.core.model.InjectLogger;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;

public class UIParserAnalysis {

	@InjectLogger
	private static ILogger logger;
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
			logger.error("Error while parsing UI xml", e);
		}

	}

}