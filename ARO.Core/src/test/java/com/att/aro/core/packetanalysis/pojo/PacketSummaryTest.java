package com.att.aro.core.packetanalysis.pojo;

import org.junit.Test;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;

public class PacketSummaryTest {
	
	public class PacketSummaryFactory implements EquivalentFactory<PacketSummary>{

		@Override
		public PacketSummary create() {
			int packetCount = 10;
			long totalBytes = 10L;
			long totalPayloadBytes = 15L;
			return new PacketSummary(packetCount, totalBytes, totalPayloadBytes);
		}
		
	}

	
	@Test
	public void testBoilerPlateCode() {
		 BeanTester beanTester = new BeanTester();
		 beanTester.testBean(PacketSummary.class);
		 
		 EqualsMethodTester beanEqualsTester = new EqualsMethodTester();
		 beanEqualsTester.testEqualsMethod(PacketSummary.class);
		 beanEqualsTester.testEqualsMethod(new PacketSummaryFactory());
		 
		 HashCodeMethodTester beanHashCodeTester = new HashCodeMethodTester();
		 beanHashCodeTester.testHashCodeMethod(PacketSummary.class);
		 beanEqualsTester.testEqualsMethod(new PacketSummaryFactory());
	}
}
