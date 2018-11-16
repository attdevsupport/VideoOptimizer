package com.att.aro.core.packetanalysis.impl;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import com.att.aro.core.BaseTest;
import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
import com.att.aro.core.packetanalysis.pojo.AbstractTraceResult;
import com.att.aro.core.packetanalysis.pojo.HttpDirection;
import com.att.aro.core.packetanalysis.pojo.HttpRequestResponseInfo;
import com.att.aro.core.packetanalysis.pojo.Session;
import com.att.aro.core.util.Util;
import com.att.aro.core.videoanalysis.pojo.VideoUsagePrefs;

/**<pre>
 * Early unit tests for getVideoUsagePrefs
 * by no means complete
 * 
 * @author barrynelson
 *
 */
public class VideoUsageAnalysisImplTest extends BaseTest {

	@InjectMocks
	VideoUsageAnalysisImpl iVideoUsageAnalysis;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		iVideoUsageAnalysis = (VideoUsageAnalysisImpl)context.getBean(IVideoUsageAnalysis.class);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getVideoUsagePrefsTest() throws Exception {
		iVideoUsageAnalysis.loadPrefs();
		VideoUsagePrefs prefs = iVideoUsageAnalysis.getVideoUsagePrefs();
		assertTrue(prefs != null);
	}
	
	@Test
	public void analyzeTest(){
		AbstractTraceResult result = Mockito.mock(AbstractTraceResult.class);
		List<Session> sessionlist = new ArrayList<>();
		InetAddress remoteIP = null;
		try {
			remoteIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int remotePort = 80;
		int localPort = 80;
		Session session = new Session(remoteIP, remotePort, localPort);
		
		HttpRequestResponseInfo info = new HttpRequestResponseInfo();
		info.setDirection(HttpDirection.REQUEST);
		info.setRequestType(HttpRequestResponseInfo.HTTP_GET);
		info.setObjNameWithoutParams(".");
		info.setFileName("TestFileName");
		
		HttpRequestResponseInfo assocReqResp = new HttpRequestResponseInfo();
		info.setAssocReqResp(assocReqResp);
		
		List<HttpRequestResponseInfo> infoList = new ArrayList<>();
		infoList.add(info);
		session.setRequestResponseInfo(infoList);
		sessionlist.add(session);
		Mockito.when(result.getTraceDirectory()).thenReturn(Util.getVideoOptimizerLibrary());
		iVideoUsageAnalysis.analyze(result, sessionlist);
	}
}
