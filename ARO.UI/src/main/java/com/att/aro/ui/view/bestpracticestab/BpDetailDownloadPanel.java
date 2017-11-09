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
package com.att.aro.ui.view.bestpracticestab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;

/**
 * 
 *
 *
 */
public class BpDetailDownloadPanel extends BpDetail {
	
	private static final long serialVersionUID = 1L;

	public BpDetailDownloadPanel(String title,
			IARODiagnosticsOverviewRoute diagnosticsOverviewRoute) {
		super(title, diagnosticsOverviewRoute);
		
		setBackground(new Color(238,238,238));
		int row = 0;

		//Text File Compression
		addPanel(row++, new BpDetailItem("textFileCompression", BestPracticeType.FILE_COMPRESSION, new BpFileCompressionTablePanel()));

		// Duplicate Content
		addPanel(row++, new BpDetailItem("caching.duplicateContent", BestPracticeType.DUPLICATE_CONTENT, new BpFileDuplicateContentTablePanel()));
		
		// Content Expiration
		addPanel(row++, new BpDetailItem("caching.usingCache", BestPracticeType.CACHE_CONTROL));
		
		// Cache Control
		addPanel(row++, new BpDetailItem("caching.cacheControl", BestPracticeType.USING_CACHE));

		// Content Pre-fetching
//		disabled until further notice, or decision on how to conduct bp test
//		addPanel(row++, new BpDetailItem("caching.prefetching", BestPracticeType.PREFETCHING, null));
		
		// Combine JS and CSS Requests
		addPanel(row++, new BpDetailItem("combinejscss", BestPracticeType.COMBINE_CS_JSS));
		
		// Resize Images for Mobile
		addPanel(row++, new BpDetailItem("imageSize", BestPracticeType.IMAGE_SIZE, new BpFileImageSizeTablePanel()));

		addPanel(row++, new BpDetailItem("imageMetadata", BestPracticeType.IMAGE_MDATA, new BpFileImageMDataTablePanel()));
		
		addPanel(row++, new BpDetailItem("imageCompression", BestPracticeType.IMAGE_CMPRS, new BpFileImageCompressionTablePanel()));
		
		addPanel(row++, new BpDetailItem("imageFormat", BestPracticeType.IMAGE_FORMAT, new BpFileImageFormatTablePanel()));
		
		// Minify CSS, JS, JSON and HTML
		addPanel(row++, new BpDetailItem("minification", BestPracticeType.MINIFICATION, new BpFileMinificationTablePanel()));

		// Use CSS Sprites for Images
		addPanel(row++, new BpDetailItem("spriteimages", BestPracticeType.SPRITEIMAGE, new BpFileSpriteImagesTablePanel()));
		
		fullPanel.add(dataPanel, BorderLayout.CENTER);
		fullPanel.add(detailPanel, BorderLayout.SOUTH);
		add(fullPanel);
		
		List<BestPracticeType> list = BestPracticeType.getByCategory(Category.FILE);
		bpFileDownloadTypes.addAll(list);
	}

	@Override
	public JPanel layoutDataPanel() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void refresh(AROTraceData model) {
		dateTraceAppDetailPanel.refresh(model);
		overViewObservable.refreshModel(model);
		updateHeader(model);
		bpResults = model.getBestPracticeResults();
	}
	
}
