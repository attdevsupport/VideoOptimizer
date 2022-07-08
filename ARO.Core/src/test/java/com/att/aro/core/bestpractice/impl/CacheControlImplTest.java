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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.bestpractice.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.bestpractice.pojo.AbstractBestPracticeResult;
import com.att.aro.core.bestpractice.pojo.BPResultType;
import com.att.aro.core.packetanalysis.pojo.CacheAnalysis;
import com.att.aro.core.packetanalysis.pojo.CacheEntry;
import com.att.aro.core.packetanalysis.pojo.Diagnosis;
import com.att.aro.core.packetanalysis.pojo.PacketAnalyzerResult;

public class CacheControlImplTest extends BaseTest {

	PacketAnalyzerResult tracedata;
	CacheEntry[] entryArray = new CacheEntry[5];
	CacheAnalysis cacheAnalysis;
	CacheControlImpl cacheControlImpl;

	@Before
	public void setUp() {
		tracedata = Mockito.mock(PacketAnalyzerResult.class);
		for (int i = 0; i < 5; i++) {
			entryArray[i] = mock(CacheEntry.class);
		}
		cacheAnalysis = Mockito.mock(CacheAnalysis.class);
	}

	@Test
	public void runTest_resTypeIsPass() {
		List<CacheEntry> diagnosisResults = new ArrayList<CacheEntry>();
		Mockito.when(entryArray[0].getDiagnosis()).thenReturn(Diagnosis.CACHING_DIAG_CACHE_MISSED);
		diagnosisResults.add(entryArray[0]);
		Mockito.when(cacheAnalysis.getDiagnosisResults()).thenReturn(diagnosisResults);
		Mockito.when(tracedata.getCacheAnalysis()).thenReturn(cacheAnalysis);
		cacheControlImpl = (CacheControlImpl) context.getBean("cacheControl");
		AbstractBestPracticeResult testResult = cacheControlImpl.runTest(tracedata);
		assertEquals(BPResultType.PASS, testResult.getResultType());
	}

	@Test
	public void runTest_resTypeIsFail() {
		List<CacheEntry> diagnosisResults = new ArrayList<CacheEntry>();
		Mockito.when(entryArray[0].getDiagnosis()).thenReturn(Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP);
		Mockito.when(entryArray[1].getDiagnosis()).thenReturn(Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP_PARTIALHIT);
		Mockito.when(entryArray[2].getDiagnosis()).thenReturn(Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_304);
		Mockito.when(entryArray[3].getDiagnosis()).thenReturn(Diagnosis.CACHING_DIAG_NOT_EXPIRED_DUP_PARTIALHIT);
		Mockito.when(entryArray[4].getDiagnosis()).thenReturn(Diagnosis.CACHING_DIAG_OBJ_NOT_CHANGED_304);

		for (int i = 0; i < 5; i++) {
			diagnosisResults.add(entryArray[i]);
		}
		Mockito.when(cacheAnalysis.getDiagnosisResults()).thenReturn(diagnosisResults);
		Mockito.when(tracedata.getCacheAnalysis()).thenReturn(cacheAnalysis);
		cacheControlImpl = (CacheControlImpl) context.getBean("cacheControl");
		AbstractBestPracticeResult testResult = cacheControlImpl.runTest(tracedata);
		assertEquals(BPResultType.WARNING, testResult.getResultType());
	}

	@Test
	public void runTest_returnIsNull() {
		Mockito.when(tracedata.getCacheAnalysis()).thenReturn(null);
		cacheControlImpl = (CacheControlImpl) context.getBean("cacheControl");
		AbstractBestPracticeResult testResult = cacheControlImpl.runTest(tracedata);
		assertNull(testResult);
	}

}
