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

import java.text.MessageFormat;
import java.text.NumberFormat;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.AccessingPeripheralResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TimeRange;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.util.Util;

/**
 * best practice for accessing Bluetooth, Camera and GPS
 * Date: November 14, 2014
 *
 */
public class AccessingPeripheralImpl implements IBestPractice {
	private static final int PERIPHERAL_ACTIVE_LIMIT = 5;
	@Value("${other.accessingPeripherals.title}")
	private String overviewTitle;
	
	@Value("${other.accessingPeripherals.detailedTitle}")
	private String detailTitle;
	
	@Value("${other.accessingPeripherals.desc}")
	private String aboutText;
	
	@Value("${other.accessingPeripherals.url}")
	private String learnMoreUrl;
	
	@Value("${other.accessingPeripherals.pass}")
	private String textResultPass;
	
	@Value("${other.accessingPeripherals.results}")
	private String textResults;

	@Value("${other.accessingPeripherals.excel.results}")
    private String textExcelResults;
	
	@Value("${exportall.csvAccGPSDesc}")
	private String exportAllGPSDesc;
	
	@Value("${exportall.csvAccBTDesc}")
	private String exportAllBTDesc;
	
	@Value("${exportall.csvAccCamDesc}")
	private String exportAllCamDesc;
		
	@Value("${bestPractices.noData}")
	private String noData;

	
	@Override
	public AbstractBestPracticeResult runTest(PacketAnalyzerResult tracedata) {
		AccessingPeripheralResult result = new AccessingPeripheralResult();
		AbstractTraceResult traceResult = tracedata.getTraceresult();
		TraceDirectoryResult trresult = null;  
		if (traceResult != null && traceResult instanceof TraceDirectoryResult) {
			trresult = (TraceDirectoryResult) traceResult;
		}
		if (trresult != null && isPeripheralDataAvailable(trresult)) {
			double activeGPSRatio = 0.0;
			double activeBluetoothRatio = 0.0;
			double activeCameraRatio = 0.0;

			double gpsActiveDuration = 0.0;
			double bluetoothActiveDuration = 0.0;
			double cameraActiveDuration = 0.0;

			double duration = 0.0;
			boolean accessingPeripherals = true;
			TimeRange timeRange = null;

			if (tracedata.getFilter() != null) {
				timeRange = tracedata.getFilter().getTimeRange();
			}
			if (tracedata.getTraceresult().getTraceResultType() == TraceResultType.TRACE_DIRECTORY) {

				gpsActiveDuration = trresult.getGpsActiveDuration();
				bluetoothActiveDuration = trresult.getBluetoothActiveDuration();
				cameraActiveDuration = trresult.getCameraActiveDuration();
			}
			if (timeRange != null) {
				duration = timeRange.getEndTime() - timeRange.getBeginTime();
			} else {
				duration = tracedata.getTraceresult().getTraceDuration();
			}
			activeGPSRatio = (gpsActiveDuration * 100) / duration;
			activeBluetoothRatio = (bluetoothActiveDuration * 100) / duration;
			activeCameraRatio = (cameraActiveDuration * 100) / duration;

			result.setActiveBluetoothRatio(activeBluetoothRatio);
			result.setActiveCameraRatio(activeCameraRatio);
			result.setActiveGPSRatio(activeGPSRatio);

			result.setActiveBluetoothDuration(bluetoothActiveDuration);
			result.setActiveCameraDuration(cameraActiveDuration);
			result.setActiveGPSDuration(gpsActiveDuration);

			if (activeGPSRatio > PERIPHERAL_ACTIVE_LIMIT || activeBluetoothRatio > PERIPHERAL_ACTIVE_LIMIT
					|| activeCameraRatio > PERIPHERAL_ACTIVE_LIMIT) {
				accessingPeripherals = false;
			}
			String cameraPer = "0";
			NumberFormat nfor = NumberFormat.getIntegerInstance();
			if (cameraActiveDuration != 0.0) {
				if (activeCameraRatio < 1.0) {
					cameraPer = Util.percentageFormat(activeCameraRatio);
				} else {
					cameraPer = nfor.format(activeCameraRatio);
				}
			}
			String key = "";
			if (accessingPeripherals) {
				result.setResultType(BPResultType.PASS);
				key = this.textResultPass;
			} else {
				result.setResultType(BPResultType.WARNING);// ref. old analyzer give warning in this best practice
				key = this.textResults;
			}
			String text = MessageFormat.format(key, activeGPSRatio, activeBluetoothRatio, cameraPer);
			result.setResultText(text);
			result.setResultExcelText(
		        MessageFormat.format(textExcelResults, result.getResultType().getDescription(), activeGPSRatio, activeBluetoothRatio, cameraPer)
	        );
		} else {
			result.setResultText(noData);
			result.setResultType(BPResultType.NO_DATA);
			result.setResultExcelText(BPResultType.NO_DATA.getDescription());
		}
		result.setAboutText(aboutText);
		result.setDetailTitle(detailTitle);
		result.setLearnMoreUrl(learnMoreUrl);
		result.setOverviewTitle(overviewTitle);
		result.setExportAllBTDesc(exportAllBTDesc);
		result.setExportAllCamDesc(exportAllCamDesc);
		result.setExportAllGPSDesc(exportAllGPSDesc);
		
		return result;
	}

	private boolean isPeripheralDataAvailable(TraceDirectoryResult trresult) {
		boolean isPeripheralDataAvailable = false;
		if (CollectionUtils.isNotEmpty(trresult.getGpsInfos())
				|| CollectionUtils.isNotEmpty(trresult.getBluetoothInfos())
				|| CollectionUtils.isNotEmpty(trresult.getCameraInfos())) {
			isPeripheralDataAvailable = true;
		}
		return isPeripheralDataAvailable;
	}

}
