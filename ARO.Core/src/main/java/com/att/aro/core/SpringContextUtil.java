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
