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
import static org.mockito.Mockito.*;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;

import org.junit.Assert;

public class BrewInfoTest {
	IExternalProcessRunner mockRunner = mock(ExternalProcessRunnerImpl.class);

	@Test
	public void isBrewVersionTest1() {
		BrewInfo brewInfo = new BrewInfo();
		when(mockRunner.executeCmdRunner("brew --version | sed 1q | tr -d 'Homebrew '", true, "success", true, true))
				.thenReturn("3.3.3");
		brewInfo.setRunner(mockRunner);
		String temp = brewInfo.getLocalBrewVersion();
		Assert.assertEquals(temp, "3.3.3");

	}

	@Test
	public void isBrewVersionTest2() {
		BrewInfo brewInfo = new BrewInfo();
		when(mockRunner.executeCmdRunner("brew --version | sed 1q | tr -d 'Homebrew '", true, "success", true, true))
				.thenReturn("3.4.11-19-g87bcd19");
		brewInfo.setRunner(mockRunner);
		String temp = brewInfo.getLocalBrewVersion();
		Assert.assertEquals(temp, "3.4.11");

	}

	@Test
	public void isBrewVersionTest3() {
		BrewInfo brewInfo = new BrewInfo();
		when(mockRunner.executeCmdRunner("brew --version | sed 1q | tr -d 'Homebrew '", true, "success", true, true))
				.thenReturn("");
		brewInfo.setRunner(mockRunner);
		String temp = brewInfo.getLocalBrewVersion();
		Assert.assertEquals(temp, "");

	}

	@Test
	public void isBrewUpdateTest1() {
		BrewInfo brewInfo = new BrewInfo();
		boolean temp = brewInfo.isBrewUpToDate("3.1.2", "3.1.2");
		Assert.assertEquals(temp, true);

	}

	@Test
	public void isBrewUpdateTest2() {
		BrewInfo brewInfo = new BrewInfo();
		boolean temp = brewInfo.isBrewUpToDate("3.1.1", "3.1.2");
		Assert.assertEquals(temp, false);

	}

	@Test
	public void isBrewUpdateTest3() {
		BrewInfo brewInfo = new BrewInfo();
		boolean temp = brewInfo.isBrewUpToDate("3.1.1", "3.1.0");
		Assert.assertEquals(temp, true);

	}

}
