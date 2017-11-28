/*
 *  Copyright 2017 AT&T
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
package com.att.aro.core;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class SpringContextUtil {
	private static final SpringContextUtil INSTANCE = new SpringContextUtil();
	private final ApplicationContext context = new AnnotationConfigApplicationContext(AROConfig.class);
	
	private SpringContextUtil() {
		
	}
	
	public static SpringContextUtil getInstance() {
		return INSTANCE;
	}
	
	public ApplicationContext getContext() {
		return context;
	}
}
