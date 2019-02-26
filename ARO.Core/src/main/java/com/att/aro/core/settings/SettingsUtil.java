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
