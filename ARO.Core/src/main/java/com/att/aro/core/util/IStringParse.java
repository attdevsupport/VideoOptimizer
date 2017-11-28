package com.att.aro.core.util;

import java.util.regex.Pattern;

public interface IStringParse {

	public String[] parse(String targetString, String regex);

	public String[] parse(String targetString, Pattern pattern);
}
