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
package com.att.aro.ui.view.menu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.aro.core.bestpractice.impl.FileCompressionImpl;
import com.att.aro.core.export.ExcelSheet;
import com.att.aro.core.export.ExcelWriter;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.PacketInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.packetanalysis.pojo.TextFileCompression;
import com.att.aro.core.packetreader.pojo.PacketDirection;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.model.DataTable;
import com.att.aro.ui.model.DataTableModel;
import com.att.aro.ui.model.diagnostic.PacketViewTableModel;
import com.att.aro.ui.model.diagnostic.RequestResponseTableModel;
import com.att.aro.ui.model.diagnostic.TCPUDPFlowsTableModel;
import com.att.aro.ui.model.listener.AbstractMenuItemListener;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.google.common.collect.Lists;

public class ExportSessionData extends AbstractMenuItemListener {

	private static final Logger LOG = LoggerFactory.getLogger(ExportSessionData.class);
	private AROTraceData traceData = null;
	private List<Session> sessionDataList;
	private Map<String, List<HttpRequestResponseInfo>> reqRespMap = new HashMap<String, List<HttpRequestResponseInfo>>();
	private Map<String, List<Object>> sessionMap = new HashMap<String, List<Object>>();
	private Map<String, List<PacketInfo>> packetsMap = new HashMap<String, List<PacketInfo>>();

	private DataTable<Session> sessionTable;
	private TCPUDPFlowsTableModel tcpUDPFlowsTableModel = new TCPUDPFlowsTableModel();
	private RequestResponseTableModel requestResponseTableModel = new RequestResponseTableModel();
	private PacketViewTableModel packetViewTableModel = new PacketViewTableModel();
	private MainFrame mainFrame;

	public ExportSessionData(MainFrame mainFrame, List<FileNameExtensionFilter> fileNameExtensionFilters, int defaultExtensionFilterIndex) {
		super(null, fileNameExtensionFilters, defaultExtensionFilterIndex);
		super.tableName = "session_data";
		this.mainFrame = mainFrame;
		this.traceData = mainFrame.getController().getTheModel();
	}

	private void getSessionData() {
		sessionTable = mainFrame.getDiagnosticTab().getTcpflowsTable();
		sessionDataList = traceData.getAnalyzerResult().getSessionlist();
		for (Session session : sessionDataList) {
			reqRespMap.put(session.getSessionKey(), session.getRequestResponseInfo());
			packetsMap.put(session.getSessionKey(), session.getAllPackets());
		}
	}

	@Override
	public void writeExcel(File file) throws IOException {
		getSessionData();
		List<Object> sessionColumnNames = createHeader(null, tcpUDPFlowsTableModel);
		List<Object> requestResponseColumnNames = createHeader(sessionColumnNames, requestResponseTableModel);
		List<Object> packetDataColumnNames = createHeader(sessionColumnNames, packetViewTableModel);

		LOG.info("Start export data to {}", file.getAbsolutePath());
		
		createSessionData();	
		ExcelSheet requestResponseSheet = new ExcelSheet("Request_Response", requestResponseColumnNames,
				getRequestResponseData());
		ExcelSheet packetsheet = new ExcelSheet("Packet", packetDataColumnNames, getPacketData());
		LOG.info("Finish export data to {}", file.getAbsolutePath());

		ExcelWriter excelWriter = new ExcelWriter(file.getAbsolutePath());

		excelWriter.export(Lists.newArrayList(requestResponseSheet, packetsheet));

	}

	private List<List<Object>> getPacketData() {
		List<List<Object>> packetData = new ArrayList<>();

		sessionMap.forEach((sessionKey, sessionData) -> {
			for (PacketInfo packetInfo : packetsMap.get(sessionKey)) {
				List<Object> data = new ArrayList<>(sessionData);
				data.add(packetInfo.getPacketId());
				data.add(packetInfo.getTimeStamp());
				data.add(packetInfo.getDir());
				data.add(ResourceBundleHelper.getEnumString(packetInfo.getTcpInfo()));
				data.add(packetInfo.getPacket().getPayloadLen());
				data.add(packetInfo.getTcpFlagString());
				packetData.add(data);
			}
		});
		return packetData;
	}

	private List<List<Object>> getRequestResponseData() {
		List<List<Object>> requestResponseData = new ArrayList<>();

		sessionMap.forEach((sessionKey, sessionData) -> {
			for (HttpRequestResponseInfo httpRequestResponseInfo : reqRespMap.get(sessionKey)) {
				List<Object> data = new ArrayList<>(sessionData);
				data.add(httpRequestResponseInfo.getTimeStamp());
				data.add(httpRequestResponseInfo.getDirection());
				if (httpRequestResponseInfo.getDirection() == HttpDirection.REQUEST) {
					String type = httpRequestResponseInfo.getRequestType();
					data.add(type != null ? type : ResourceBundleHelper.getMessageString("rrview.unknownType"));
				} else {
					data.add(httpRequestResponseInfo.getStatusCode() != 0 ? httpRequestResponseInfo.getStatusCode()
							: ResourceBundleHelper.getMessageString("rrview.unknownType"));
				}
				if (httpRequestResponseInfo.getDirection() == HttpDirection.REQUEST) {
					data.add(httpRequestResponseInfo.getHostName());
				} else {
					data.add(httpRequestResponseInfo.getContentType());
				}
				if (httpRequestResponseInfo.getDirection() == HttpDirection.REQUEST) {
					data.add(StringUtils.isNotEmpty(httpRequestResponseInfo.getObjName())
							? httpRequestResponseInfo.getObjName()
							: httpRequestResponseInfo.getContentLength());
				} else {
					data.add(httpRequestResponseInfo.getContentLength());
				}
				data.add(httpRequestResponseInfo.getRawSize());
				data.add(getHttpCompression(httpRequestResponseInfo));
				requestResponseData.add(data);
			}
		});
		return requestResponseData;
	}

	private void createSessionData() {

		for (int rowIndex = 0; rowIndex < sessionTable.getRowCount(); rowIndex++) {
			List<Object> sessionData = new ArrayList<>();
			for (int columnIndex = 0; columnIndex < sessionTable.getColumnCount(); ++columnIndex) {
				if (getColumnsToSkip(tcpUDPFlowsTableModel).contains(columnIndex)) {
					continue;
				}
				Object colValue = sessionTable.getValueAt(rowIndex, columnIndex);
				sessionData.add(colValue == null ? "-" : colValue);

			}
			sessionMap.put(sessionTable.getItemAtRow(rowIndex).getSessionKey(), sessionData);

		}
	}

	private List<Object> createHeader(List<Object> sessionColumnNames, DataTableModel<?> tableModel) {
		List<Object> columnNames = sessionColumnNames != null ? new ArrayList<>(sessionColumnNames) : new ArrayList<>();
		for (int columnIndex = 0; columnIndex < tableModel.getColumnCount(); ++columnIndex) {
			if (getColumnsToSkip(tableModel).contains(columnIndex)) {
				continue;
			}
			if (tableModel instanceof RequestResponseTableModel && columnIndex == RequestResponseTableModel.TIME_COL) {
				columnNames.add("Request/Response " + tableModel.getColumnName(columnIndex));
			} else {
				columnNames.add(tableModel.getColumnName(columnIndex));
			}

		}
		return columnNames;
	}

	private List<Integer> getColumnsToSkip(DataTableModel<?> tableModel) {
		List<Integer> columnsToSkip = new ArrayList<>();
		if (tableModel instanceof TCPUDPFlowsTableModel) {
			columnsToSkip.add(TCPUDPFlowsTableModel.CHECKBOX_COL);
			columnsToSkip.add(TCPUDPFlowsTableModel.SYN_COL);
			columnsToSkip.add(TCPUDPFlowsTableModel.SYN_ACK_COL);
		} else if (tableModel instanceof RequestResponseTableModel) {
			columnsToSkip.add(RequestResponseTableModel.NETWORK_LATENCY_COL);
		}
		return columnsToSkip;
	}

	@Override
	public void writeCSV(File file) throws IOException {
		String text = "Writing to CSV file is not supported by " + this.getClass().getName();
		LOG.error(text);
		throw new UnsupportedOperationException(text);
	}

	private String getHttpCompression(HttpRequestResponseInfo httpRequestResponseInfo) {

		String contentEncoding = httpRequestResponseInfo.getContentEncoding();
		FileCompressionImpl fileCompression = (FileCompressionImpl) ContextAware.getAROConfigContext()
				.getBean("textFileCompression");

		if (httpRequestResponseInfo.getPacketDirection() == PacketDirection.DOWNLINK
				&& httpRequestResponseInfo.getContentLength() != 0 && httpRequestResponseInfo.getContentType() != null
				&& fileCompression.isTextContent(httpRequestResponseInfo.getContentType())) {
			if ("gzip".equals(contentEncoding)) {
				return TextFileCompression.GZIP.toString();
			} else if ("compress".equals(contentEncoding)) {
				return TextFileCompression.COMPRESS.toString();
			} else if ("deflate".equals(contentEncoding)) {
				return TextFileCompression.DEFLATE.toString();
			} else {
				return TextFileCompression.NONE.toString();
			}

		} else {
			return TextFileCompression.NOT_APPLICABLE.toString();
		}

	}
}