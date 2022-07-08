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
package com.att.aro.core.peripheral.impl;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aro.core.BaseTest;
import com.att.aro.core.fileio.IFileManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CollectOptionsReaderImpl.class)
@PowerMockIgnore({"org.apache.logging.log4j.*"})
public class CollectOptionsReaderImplTest extends BaseTest {
	
	@InjectMocks
	CollectOptionsReaderImpl collectOptionsReaderImpl;
	
	@Mock
	private IFileManager filereader;
	
	@Before
	public void setup() {		
		collectOptionsReaderImpl = PowerMockito.mock(CollectOptionsReaderImpl.class);
		MockitoAnnotations.initMocks(this);
	}

}
