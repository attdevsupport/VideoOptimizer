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
package com.att.aro.core.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.att.aro.core.bestpractice.pojo.BestPracticeType;
import com.att.aro.core.bestpractice.pojo.BestPracticeType.Category;
import com.att.aro.core.settings.impl.SettingsImpl;

public final class SettingsUtil {
	public static final String UNSELECTED_BP = "unSelectedBP";
	
	private static List<BestPracticeType> unSelectedBPList = new ArrayList<>();
	private static List<BestPracticeType> selectedBPList = new ArrayList<>();

	private SettingsUtil() {
	}
	
	public static final List<BestPracticeType> getSelectedBPsList() {
		if (selectedBPList == null || selectedBPList.isEmpty()) {
			String unSelectedStr = SettingsImpl.getInstance().getAttribute(UNSELECTED_BP);
			if (unSelectedStr != null && unSelectedStr.length() > 2) {
				List<String> unSelectedStrList = Arrays.asList(unSelectedStr.replaceAll("\\[|\\]", "").split(", "));
				unSelectedBPList = unSelectedStrList.stream().filter((s) -> BestPracticeType.isValid(s)).map((s) -> BestPracticeType.valueOf(s))
						.collect(Collectors.toList());
			}
			selectedBPList = getOtherBestPractices(unSelectedBPList);
		}
		return selectedBPList;
	}

	public static final void saveBestPractices(List<BestPracticeType> bpList) {
		// Set the unselected BPs
		unSelectedBPList = getOtherBestPractices(bpList);
		SettingsImpl.getInstance().setAndSaveAttribute(SettingsUtil.UNSELECTED_BP, unSelectedBPList.toString());
		
		// Set the selected BPs
		selectedBPList = getOtherBestPractices(unSelectedBPList);
	}
		
	private static List<BestPracticeType> getOtherBestPractices(List<BestPracticeType> bpsToBeExcluded) {
		List<BestPracticeType> preProcessList = BestPracticeType.getByCategory(Category.PRE_PROCESS);
		return Arrays.stream(BestPracticeType.values())
				.filter(bp -> (!bpsToBeExcluded.contains(bp) && !preProcessList.contains(bp)))
				.collect(Collectors.toList());
	}
}
