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
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;

public class BpDetailSecurityPanel extends BpDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public BpDetailSecurityPanel(String title, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		super(title, diagnosticsOverviewRoute);
		
		setBackground(new Color(238,238,238));
		int row = 0;
		
		// https Usage
		addPanel(row++, new BpDetailItem("security.httpsUsage", BestPracticeType.HTTPS_USAGE, new BpSecurityHttpsUsageTablePanel()));
		
		// Transmission of Private Data
		addPanel(row++, new BpDetailItem("security.transmissionPrivateData", BestPracticeType.TRANSMISSION_PRIVATE_DATA, new BpSecurityTransmissionPrivateDataTablePanel()));
		
		// Unsecure SSL Version
		addPanel(row++, new BpDetailItem("security.unsecureSSLVersion", BestPracticeType.UNSECURE_SSL_VERSION, new BpSecurityUnsecureSSLVersionTablePanel()));
		
		// Weak Cipher
		addPanel(row++, new BpDetailItem("security.weakCipher", BestPracticeType.WEAK_CIPHER, new BpSecurityWeakCipherTablePanel()));
		
		// Forward Secrecy
		addPanel(row++, new BpDetailItem("security.forwardSecrecy", BestPracticeType.FORWARD_SECRECY, new BpSecurityForwardSecrecyTablePanel()));
		
		fullPanel.add(dataPanel, BorderLayout.CENTER);
		fullPanel.add(detailPanel, BorderLayout.SOUTH);
		add(fullPanel);
		
		List<BestPracticeType> list = Arrays.asList(new BestPracticeType[] {
				BestPracticeType.HTTPS_USAGE, 
				BestPracticeType.TRANSMISSION_PRIVATE_DATA, 
				BestPracticeType.UNSECURE_SSL_VERSION, 
				BestPracticeType.WEAK_CIPHER, 
				BestPracticeType.FORWARD_SECRECY});
		bpFileDownloadTypes.addAll(list);
	}

	@Override
	public JPanel layoutDataPanel() {
		return null;
	}
	
	@Override
	public void refresh(AROTraceData model) {
		dateTraceAppDetailPanel.refresh(model);
		
		overViewObservable.refreshModel(model);
		
		bpResults = model.getBestPracticeResults();
		
		updateHeader(model);
	}
}
