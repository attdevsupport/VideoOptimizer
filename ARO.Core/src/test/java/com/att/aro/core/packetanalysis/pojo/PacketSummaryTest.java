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
