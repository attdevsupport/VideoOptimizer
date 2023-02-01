package com.att.aro.core.util;

/**
 * @author bt6527
 *
 */
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.aro.core.BaseTest;
import com.att.aro.core.commandline.IExternalProcessRunner;

@PrepareForTest(WiresharkConfirmationImpl.class)
public class WiresharkConfirmationImplTest extends BaseTest {

	@InjectMocks
	WiresharkConfirmationImpl wiresharkConfirmationImpl;
	
	@Autowired @Spy
	private IExternalProcessRunner extProcessRunner;

	@Before
	public void createMocks() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testCheckWireshark() {
		boolean result = wiresharkConfirmationImpl.checkWireshark();
		Assert.assertTrue(result);
	}

	@Test
	public void testCheckWiresharkWithWrongPath() {
		Mockito.when(extProcessRunner.executeCmd(Mockito.anyString())).thenReturn("Dummy String");
		boolean result = wiresharkConfirmationImpl.checkWireshark();
		Assert.assertFalse(result);

	}
}