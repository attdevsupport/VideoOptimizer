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

	private SettingsUtil() {
	}

	public static final List<BestPracticeType> retrieveBestPractices() {
		String sel = SettingsImpl.getInstance().getAttribute(UNSELECTED_BP);
		List<BestPracticeType> obpList = new ArrayList<>();
		if (sel != null && sel.length() > 2) {
			List<String> obpStrList = Arrays.asList(sel.replaceAll("\\[|\\]", "").split(", "));
			obpList = obpStrList.stream()
					.filter((s) -> BestPracticeType.isValid(s))
					.map((s) -> BestPracticeType.valueOf(s))
					.collect(Collectors.toList());
		}
		List<BestPracticeType> bpList = getUnselectedBestPractices(obpList);
		return bpList;
	}

	public static final void saveBestPractices(List<BestPracticeType> bpList) {
		String str = getUnselectedBestPractices(bpList).toString();
		SettingsImpl.getInstance().setAndSaveAttribute(SettingsUtil.UNSELECTED_BP, str);
	}

	private static List<BestPracticeType> getUnselectedBestPractices(List<BestPracticeType> bpList) {
		BestPracticeType[] allBP = BestPracticeType.values();
		List<BestPracticeType> ret = new ArrayList<>(); 
		List<BestPracticeType> pre = BestPracticeType.getByCategory(Category.PRE_PROCESS);
		pre.addAll(bpList);
		for(BestPracticeType bpt : allBP) {
			if(!pre.contains(bpt)) {
				ret.add(bpt);
			}
		}
		return ret;
	}
}
