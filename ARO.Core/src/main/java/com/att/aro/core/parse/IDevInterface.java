package com.att.aro.core.parse;

import java.util.List;

public interface IDevInterface {

	Integer[] createPositionArray(String ifconfig);

	List<String[]> obtainVoIpAddress();

	/**
	 * 
	 *
	 * @param ipList
	 * @param block
	 * @param patterns String[] {"HeaderRegex", "ipv4Regex", "ipv6Regex"}
	 * return List<String[]> of ipv4 & ipv6
	 */
	void parseBlock(List<String[]> ipList, String block, String[] patterns);

}
