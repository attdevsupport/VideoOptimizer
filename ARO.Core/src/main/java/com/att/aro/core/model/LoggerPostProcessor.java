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
package com.att.aro.core.model;

import java.lang.reflect.Field;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;




@Component
public class LoggerPostProcessor implements BeanPostProcessor {
	private static final Logger LOG = LogManager.getLogger(LoggerPostProcessor.class.getName());
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		Field[] fields = bean.getClass().getDeclaredFields();
		for(Field field : fields){
			if(Logger.class.isAssignableFrom(field.getType()) && field.getAnnotation(InjectLogger.class) != null){
				Logger log = LogManager.getLogger(bean.getClass().getName());
				try {
					field.setAccessible(true);
					field.set(bean, log);
				} catch (IllegalArgumentException e) {
					LOG.error(e.getMessage(), e);
				} catch (IllegalAccessException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

}
