package com.att.aro.core.packetanalysis.impl;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.att.aro.core.BaseTest;
import com.att.aro.core.packetanalysis.IVideoUsageAnalysis;
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
}
