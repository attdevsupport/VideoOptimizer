/*
 *  Copyright 2015 AT&T
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
package com.att.aro.ui.view.waterfalltab;

import java.awt.BorderLayout;
import java.text.MessageFormat;

import javax.swing.JPanel;

import com.att.aro.core.ApplicationConfig;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.commonui.TabPanelJPanel;
import com.att.aro.ui.commonui.UIComponent;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.AROModelObserver;

public class WaterfallTab extends TabPanelJPanel {
	private static final long serialVersionUID = 1L;

	private AROModelObserver waterfallObservable;
	private IARODiagnosticsOverviewRoute route;
	private WaterfallPanel panel;

	public WaterfallTab(IARODiagnosticsOverviewRoute route) {
		super();
		this.route = route;
	}

	public JPanel layoutDataPanel() {
		this.setLayout(new BorderLayout());
		// Get the blue header panel with ATT logo.
		String title = ResourceBundleHelper.getMessageString("Waterfall.title");
		String brandName = ApplicationConfig.getInstance().getAppBrandName();
		String shortName = ApplicationConfig.getInstance().getAppShortName();
		String headerTitle = MessageFormat.format(title, brandName, shortName);
		this.add(UIComponent.getInstance().getLogoHeader(headerTitle), BorderLayout.NORTH);
		panel = new WaterfallPanel(this);
		this.add(panel.layoutDataPanel(), BorderLayout.CENTER);
		waterfallObservable = new AROModelObserver();
		waterfallObservable.registerObserver(panel);
		return this;
	}

	public void refresh(AROTraceData data) {
		waterfallObservable.refreshModel(data);
	}

	public void updateMainFrame(Object object) {
		route.updateDiagnosticsTab(object);
	}
	
	/*
	 * Takes graph to the range of 100  with respect 
	 * to the Multiple Simultaneous Connection timestamp
	 */
	public void updateGraph(HttpRequestResponseInfo httpReqRespInfo) {
		int lowPoint = (int) (Math.floor(httpReqRespInfo.getTimeStamp())/100);
		panel.setTimeRange(lowPoint*100, (lowPoint*100)+100);						
	}
	

}
