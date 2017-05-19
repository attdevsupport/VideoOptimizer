package com.att.aro.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.att.aro.core.settings.impl.SettingsImpl;

public interface TestingUtil {
	
	static void SetFinalField(Class<SettingsImpl> classInst, String name, String value)
			throws NoSuchFieldException, IllegalAccessException {
		Field pathField = classInst.getDeclaredField(name);
		pathField.setAccessible(true);
		Field modifier = Field.class.getDeclaredField("modifiers");
		modifier.setAccessible(true);
		modifier.setInt(pathField, pathField.getModifiers() & ~Modifier.FINAL);
		pathField.set(null, value);
	}

}
