
package com.att.aro.core.packetanalysis.pojo;

import org.junit.Test;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;


public class ApplicationPacketSummaryTest {
	
	
	public class ApplicationPacketSummaryFactory implements EquivalentFactory<ApplicationPacketSummary>{

		@Override
		public ApplicationPacketSummary create() {
			String appName = "Android";
			int packetCount = 10;
			long totalBytes = 10L;
			long totalPayloadBytes = 15L;
			return new ApplicationPacketSummary(appName, packetCount, totalBytes, totalPayloadBytes);
		}
		
	}

	@Test
	public void testBoilerPlateCode() {
		BeanTester beanTester = new BeanTester();
		beanTester.testBean(ApplicationPacketSummary.class);

		EqualsMethodTester beanEqualsTester = new EqualsMethodTester();
		beanEqualsTester.testEqualsMethod(ApplicationPacketSummary.class);
		beanEqualsTester.testEqualsMethod(new ApplicationPacketSummaryFactory());

		HashCodeMethodTester beanHashCodeTester = new HashCodeMethodTester();
		beanHashCodeTester.testHashCodeMethod(ApplicationPacketSummary.class);
		beanHashCodeTester.testHashCodeMethod(new ApplicationPacketSummaryFactory());
	}

}
