package com.att.aro.core.peripheral.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.TraceDataConst;
import com.att.aro.core.peripheral.ICellTowerInfoReader;
import com.att.aro.core.peripheral.pojo.CellInfo;
import com.att.aro.core.util.Util;

public class CellTowerInfoReaderImpl extends PeripheralBase implements ICellTowerInfoReader {

	private static final Logger LOGGER = LogManager.getLogger(CellTowerInfoReaderImpl.class.getSimpleName());

	@Override
	public List<CellInfo> readData(String directory) {

		List<CellInfo> cellinfoList = new ArrayList<>();
		String filepath = directory + Util.FILE_SEPARATOR + TraceDataConst.FileName.CELL_STATUS;

		if (!filereader.fileExist(filepath)) {
			return cellinfoList;
		}
		String[] contents = null;
		try {
			contents = filereader.readAllLine(filepath);
		} catch (IOException e) {
			LOGGER.error("failed to read Device Info file: " + filepath);
		}

		// Parse entry
		if (contents != null && contents.length > 0) {
			for (String contentBuf : contents) {
				// Ignore empty line
				if (contentBuf.trim().isEmpty()) {
					continue;
				}

				// Parse entry
				String splitContents[] = contentBuf.split("\\s+");
				if (splitContents.length <= 1) {
					LOGGER.warn("Found invalid user event entry: " + contentBuf);
					continue;
				}
				CellInfo cellInfo = new CellInfo();
				cellInfo.setCarrierName(splitContents[0]);
				cellInfo.setCellID(splitContents[1]);
				cellinfoList.add(cellInfo);
			}
		}

		return cellinfoList;
	}

}
