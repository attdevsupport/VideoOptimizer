package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.packetanalysis.pojo.Burst;
import com.att.aro.core.packetanalysis.pojo.BurstCategory;
import com.att.aro.core.packetanalysis.pojo.BurstCollectionAnalysisData;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;

public class PrefetchingImplTest extends BaseTest {

	PrefetchingImpl preFetching;
	Burst burst01;
	PacketAnalyzerResult tracedata;
	BurstCollectionAnalysisData burstCollectionAnalysisData;

	@Before
	public void setup() {
		burst01 = Mockito.mock(Burst.class);
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		burstCollectionAnalysisData = Mockito.mock(BurstCollectionAnalysisData.class);

	}

	@Test
	public void runTest_resTypeIsPass() {
		Mockito.when(burst01.getBurstCategory()).thenReturn(BurstCategory.UNKNOWN);
		List<Burst> burstCollection = new ArrayList<Burst>();
		burstCollection.add(burst01);
		Mockito.when(burstCollectionAnalysisData.getBurstCollection()).thenReturn(burstCollection);
		Mockito.when(tracedata.getBurstCollectionAnalysisData()).thenReturn(burstCollectionAnalysisData);
		preFetching = (PrefetchingImpl) context.getBean("prefetching");
		AbstractBestPracticeResult result = preFetching.runTest(tracedata);
		assertEquals(BPResultType.PASS, result.getResultType());
	}

	@Test
	public void runTest_resTypeIsFail() {
		Mockito.when(burst01.getBurstCategory()).thenReturn(BurstCategory.USER_INPUT);
		List<Burst> burstCollection = new ArrayList<Burst>();
		for (int i = 0; i < 6; i++) {
			burstCollection.add(burst01);
		}
		Mockito.when(burstCollectionAnalysisData.getBurstCollection()).thenReturn(burstCollection);
		Mockito.when(tracedata.getBurstCollectionAnalysisData()).thenReturn(burstCollectionAnalysisData);
		preFetching = (PrefetchingImpl) context.getBean("prefetching");
		AbstractBestPracticeResult result = preFetching.runTest(tracedata);
		assertEquals(BPResultType.FAIL, result.getResultType());
	}

}
