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
package com.att.aro.datacollector.ioscollector.utilities;

import org.junit.Test;

import org.junit.Assert;

public class VersionComparatorTest {

	private VersionComparator cmp = new VersionComparator();

	@Test
	public void comparisonTest01() {
		Assert.assertEquals(cmp.compare("1.1.2", "1.1.2"), 0);
		Assert.assertEquals(cmp.compare("1.1.2", "1.2"), -1);
		Assert.assertEquals(cmp.compare("1.1.2", "1.2.0"), -1);
		Assert.assertEquals(cmp.compare("1.1.2", "1.2.1"), -1);
		Assert.assertEquals(cmp.compare("1.2", "1.2.0"), 0);
		Assert.assertEquals(cmp.compare("1.2", "1.2.1"), -1);
		Assert.assertEquals(cmp.compare("1.2", "1.12"), -1);
		Assert.assertEquals(cmp.compare("1.2.0", "1.2.1"), -1);
		Assert.assertEquals(cmp.compare("1.2.0", "1.12"), -1);
		Assert.assertEquals(cmp.compare("1.12", "1.12"), 0);
		Assert.assertEquals(cmp.compare("1.3", "1.3a"), 1);
		Assert.assertEquals(cmp.compare("1.3a", "1.3b"), -1);
		Assert.assertEquals(cmp.compare("1.3", "1.3-SNAPSHOT"), 1);
		Assert.assertEquals(cmp.compare("1.3b", "1.3-SNAPSHOT"), 53);
		Assert.assertEquals(cmp.compare("1.3-SNAPSHOT", "1.3-SNAPSHOT"), 0);

	}
}
