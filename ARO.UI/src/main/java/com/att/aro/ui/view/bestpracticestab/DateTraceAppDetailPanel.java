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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;
import com.att.aro.core.packetanalysis.pojo.TraceDirectoryResult;
import com.att.aro.core.packetanalysis.pojo.TraceResultType;
import com.att.aro.core.peripheral.pojo.NetworkType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.preferences.impl.PreferenceHandlerImpl;
import com.att.aro.core.util.Util;
import com.att.aro.ui.commonui.TabPanelCommon;
import com.att.aro.ui.commonui.TabPanelCommonAttributes;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.utils.ResourceBundleHelper;

public class DateTraceAppDetailPanel extends TabPanelJPanel {
	private enum LabelKeys {
		bestPractices_header_summary,
		bestPractices_date,
		bestPractices_trace,
		bestPractices_range,
		bestPractices_application,
		bestPractices_applicationversion,
		bestPractices_networktype,
		bestPractices_profile,
		secure_title
	}

	private static final long serialVersionUID = 1L;
	private static final String EMPTY_SPACE = "                              ";
	private final TabPanelCommon tabPanelCommon = new TabPanelCommon();
	private TabPanelCommonAttributes attributes;
	private static final Logger LOG = LogManager.getLogger(DateTraceAppDetailPanel.class.getName());
	
	
	/**
	 * Initializes a new instance of the DateTraceAppDetailPanel class.
	 */
	public DateTraceAppDetailPanel() {
		tabPanelCommon.initTabPanel(this);
		add(layoutDataPanel(), BorderLayout.WEST);
		tabPanelCommon.setText(LabelKeys.bestPractices_trace, "", getOpenFolderAdapter());
	}

	/**
	 * Creates the JPanel containing the Date , Trace and Application details
	 * 
	 * @return the dataPanel
	 */
	@Override
	public JPanel layoutDataPanel() {
		if (attributes == null) {		
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().enumKeyLabel(LabelKeys.bestPractices_header_summary).build()); 
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_date).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_trace).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_range).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_application).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_applicationversion).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_networktype).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.bestPractices_profile).build());
			attributes = tabPanelCommon.addLabelLine(new TabPanelCommonAttributes.Builder().copyNextLine(attributes).enumKey(LabelKeys.secure_title).build());
		}
		tabPanelCommon.setText(LabelKeys.bestPractices_date, EMPTY_SPACE);

		return tabPanelCommon.getTabPanel();
	}

	private String getAppVersion(AbstractTraceResult traceResults, String appName) {
		StringBuilder appVersion = new StringBuilder();
		if (traceResults.getTraceResultType() == TraceResultType.TRACE_DIRECTORY) {
			Map<String, String> appVersionMap =
					((TraceDirectoryResult) traceResults).getAppVersionMap();
			if (appVersionMap != null &&
					appVersionMap.get(appName) != null) {
				appVersion.append(" : " + appVersionMap.get(appName));
			}
		}
		return appVersion.toString();
	}
	
	private void refreshCommon(AbstractTraceResult traceResults,Set<String> appNames ) {
		tabPanelCommon.setText(LabelKeys.bestPractices_date, String.format("%1$tb %1$te, %1$tY %1$tr", traceResults.getTraceDateTime()));

		String traceDirectory = traceResults.getTraceDirectory();
		int lastIndexOf = traceDirectory.lastIndexOf(Util.FILE_SEPARATOR);
		tabPanelCommon.setTextAsLink(LabelKeys.bestPractices_trace, lastIndexOf > -1 ? traceDirectory.substring((lastIndexOf + 1)) : traceDirectory);

		StringBuilder appList = new StringBuilder();
		boolean firstTimeFlag = true;
		appList.append("<html>");
		if(appNames != null && appNames.size() != 0) {
			for (String appName : appNames) {
				if (!firstTimeFlag) {
					appList.append("<br/>");
				}
				firstTimeFlag = false;
				appList.append(appName + getAppVersion(traceResults, appName));
			}
		}
		appList.append("</html>");
		tabPanelCommon.setText(LabelKeys.bestPractices_application, appList.toString());
	}
	
	private MouseAdapter getOpenFolderAdapter() {
		MouseAdapter openFolderAction = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Desktop desktop = null;
				if (Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
					try {
						File traceFile = new File(PreferenceHandlerImpl.getInstance().getPref("TRACE_PATH"));
						if (traceFile != null && traceFile.exists()) {
							if (traceFile.isDirectory()) {
								desktop.open(traceFile);
							} else {
								desktop.open(traceFile.getParentFile());
							}
						}
					} catch (IOException ex) {
						LOG.error("Error opening the Trace Folder : " + ex.getMessage());
					}
				}
			}
		};
		return openFolderAction;
	}


	private void refreshTraceDirectory(PacketAnalyzerResult traceResults) {
		TraceDirectoryResult traceDirectoryResults = (TraceDirectoryResult) traceResults.getTraceresult();
		tabPanelCommon.setText(LabelKeys.bestPractices_applicationversion,
				traceDirectoryResults.getCollectorVersion());
		tabPanelCommon.setText(LabelKeys.bestPractices_networktype,
				traceDirectoryResults.getNetworkTypesList()!=null ? getNetworkTypeList(traceDirectoryResults.getNetworkTypesList()) : "" );
		tabPanelCommon.setText(LabelKeys.secure_title,
				traceDirectoryResults.getCollectOptions().getSecureStatus().toString());

		String timeRangeText;
		if (traceDirectoryResults.getTimeRange().getTitle() == null) {
			timeRangeText = String.format("%.3f - %.3f", traceResults.getTimeRangeAnalysis().getStartTime(), traceResults.getTimeRangeAnalysis().getEndTime());
		} else {
			timeRangeText = traceDirectoryResults.getTimeRange().getRange();
		}
		tabPanelCommon.setText(LabelKeys.bestPractices_range, timeRangeText);
	}

	private void clearDirResults() {
		tabPanelCommon.setText(LabelKeys.bestPractices_applicationversion, "");		
		tabPanelCommon.setText(LabelKeys.bestPractices_networktype, "");
		tabPanelCommon.setText(LabelKeys.secure_title, "");
	}

	@Override
	public void refresh(AROTraceData model) {
		PacketAnalyzerResult analyzerResults = model.getAnalyzerResult();
		Set<String> appNames =  analyzerResults.getStatistic().getAppName();
		AbstractTraceResult traceResults = analyzerResults.getTraceresult();
		if (traceResults != null) {
			refreshCommon(traceResults,appNames);
			if (traceResults.getTraceResultType() == TraceResultType.TRACE_DIRECTORY) {
				refreshTraceDirectory(analyzerResults);
			} else {
				clearDirResults();
			}
			String profileName = analyzerResults.getProfile().getName() != null ?
					analyzerResults.getProfile().getName() : "TBD";
			tabPanelCommon.setText(LabelKeys.bestPractices_profile, profileName);
		}
	}
	
	private String getNetworkTypeList(List<NetworkType> networkTypesList) {			

		if (networkTypesList != null && !networkTypesList.isEmpty()) {
			StringBuffer networksList = new StringBuffer();
			for (NetworkType networkType : networkTypesList) {
				if (networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO)
						|| networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_LTE_CA)
						|| networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_NR_NSA)
						|| networkType.equals(NetworkType.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE)) {
					networksList.append(ResourceBundleHelper.getMessageString("NetworkType." + networkType.toString()));
				} else {
					networksList.append(networkType.toString());
				}

				networksList.append(" , ");
			}
			return networksList.toString().substring(0, networksList.toString().lastIndexOf(","));
		} else {
			return "";
		}
	
	}
}
