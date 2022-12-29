package com.att.aro.core.util;

/**
 * Reference the solution from the StackOverFlow
 * https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
 * 
 * @author ls661n
 *
 */
public class VersionTokenizer {
	private final String local_VersionString;
	private final int localLength;

	private int position;
	private int number;
	private String suffix;
	private boolean hasValue;

	public int getNumber() {
		return number;
	}

	public String getSuffix() {
		return suffix;
	}

	public boolean hasValue() {
		return hasValue;
	}

	public VersionTokenizer(String versionString) {
		if (versionString == null)
			throw new IllegalArgumentException("versionString is null");

		local_VersionString = versionString;
		localLength = versionString.length();
	}

	public boolean MoveNext() {
		number = 0;
		suffix = "";
		hasValue = false;

		// No more characters
		if (position >= localLength)
			return false;

		hasValue = true;

		while (position < localLength) {
			char character = local_VersionString.charAt(position);
			if (character < '0' || character > '9')
				break;
			number = number * 10 + (character - '0');
			position++;
		}

		int suffixStart = position;

		while (position < localLength) {
			char c = local_VersionString.charAt(position);
			if (c == '.')
				break;
			position++;
		}

		suffix = local_VersionString.substring(suffixStart, position);

		if (position < localLength)
			position++;

		return true;
	}
}
