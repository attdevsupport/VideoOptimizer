package com.att.aro.core;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public final class LoggingConfig {
	
	private LoggingConfig() {
		
	}

	public static void updateLoggingLevel(String strLoggingLevel) {
		Level loggingLevel = Level.toLevel(strLoggingLevel);
		updateLoggingLevel(loggingLevel);
	}

	public static void updateLoggingLevel(Level loggingLevel) {
		Logger.getRootLogger().setLevel(loggingLevel);
	}
}