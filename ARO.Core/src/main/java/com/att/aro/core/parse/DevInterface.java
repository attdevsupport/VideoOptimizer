package com.att.aro.core.parse;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.SpringContextUtil;
import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.util.StringParse;
import com.att.aro.core.util.Util;

public class DevInterface implements IDevInterface {

	private static final Logger LOG = LogManager.getLogger(DevInterface.class.getName());
	private static final IExternalProcessRunner EXTERNAL_PROCESS_RUNNER = SpringContextUtil.getInstance().getContext()
			.getBean(IExternalProcessRunner.class);

	private StringParse sParse = new StringParse();

	int tempIdx;
	private String ipInterfaceName;
	
	@Override
	public List<String[]> obtainVoIpAddress() {
		List<String[]> ipList = null;
		ipList = new ArrayList<>();

		String cmd = Util.isWindowsOS() ? "ipconfig /all" : Util.isMacOS() ? "ifconfig" : "ip addr";
		String ifconfig = EXTERNAL_PROCESS_RUNNER.executeCmd(cmd, true, true);

		if (Util.isWindowsOS()) {
			ipList = parseWindowsIP(ipList, ifconfig);
		} else if (Util.isMacOS()) {
			ipList = parseUnixIP(ipList, ifconfig);
		} else {
			ipList = parseLinuxIP(ipList, ifconfig);
		}
		return ipList;
	}

	public List<String[]> parseUnixIP(List<String[]> ipList, String ifconfig) {
		String[] patterns = new String[] {"([a-zA-Z0-9_]*)[: ]", "inet +(.+) netmask", "inet6 +(.+)[%/]"};

		Integer[] positionArray = createPositionArray(ifconfig);
		for (int idx = 0; idx < positionArray.length; idx++) {
			String iBlock = (idx + 1 < positionArray.length) 
					? ifconfig.substring(positionArray[idx], positionArray[idx + 1])
					: ifconfig.substring(positionArray[idx]);
			parseBlock(ipList, iBlock, patterns);
		}

		if (ipList.isEmpty()) {
			LOG.debug("failed to parse:" + ifconfig);
		}
		return ipList;
	}

	public List<String[]> parseLinuxIP(List<String[]> ipList, String ipAddr) {
		String[] patterns = new String[] { ": ([A-Za-z0-9_]*):|([A-Za-z0-9_]*)@", "inet +(.+)[%\\/]", "inet6 +(.+)[%\\/]" };

		Integer[] positionArray = locateHeaders(ipAddr, patterns[0]);

		for (int idx = 0; idx < positionArray.length; idx++) {
			String block = (idx + 1 < positionArray.length)
					? ipAddr.substring(positionArray[idx], positionArray[idx + 1])
					: ipAddr.substring(positionArray[idx]);
			if (!block.equals("\n\n")) {
				parseBlock(ipList, block, patterns);
			}
		}

		return ipList;
	}

	public List<String[]> parseWindowsIP(List<String[]> ipList, String ipc) {
		String[] patterns = new String[] {
				"^([A-Za-z][0-9a-zA-Z ]{1,80})(\\bIP\\b|\\badapter\\b)([0-9a-zA-Z \\*\\-]{1,80})"
				, "^[\\s]*IPv4[ \\.A-Za-z]*: (\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3})"
				, "^.+IPv6[ \\.A-Za-z]* : ([0-9a-fA-F]{0,4}:[0-9a-fA-F]{0,4}:[0-9a-fA-F]{0,4}:[0-9a-fA-F]{0,4}:[0-9a-fA-F]{0,4}:[0-9a-fA-F]{0,4})"
				};
		Integer[] positionArray = locateHeaders(ipc, patterns[0]);

		for (int idx = 0; idx < positionArray.length; idx++) {
			String iBlock = (idx + 1 < positionArray.length) 
					? ipc.substring(positionArray[idx], positionArray[idx + 1])
					: ipc.substring(positionArray[idx]);
			parseBlock(ipList, iBlock, patterns);
		}
		if (ipList.isEmpty()) {
			LOG.debug("failed to parse:" + ipc);
		}
		return ipList;
	}

	private Integer[] locateHeaders(String ipc, String patHeaderRegex) {
		ArrayList<Integer> positionList = new ArrayList<>();
		List<String> foundList = sParse.parse(new ArrayList<String>(), ipc, patHeaderRegex);
		tempIdx = 0;
		foundList.forEach(e -> {
			int idx = ipc.substring(tempIdx).indexOf(e);
			positionList.add(tempIdx + idx);
			tempIdx += idx + e.length();
		});
		positionList.add(ipc.length()); // one last marker for the end of the ipc
		return positionList.toArray(new Integer[positionList.size()]);
	}
	
	@Override
	public void parseBlock(List<String[]> ipList, String block, String[] patterns) {

		if (block.length() < 10) {
			return;
		}

		ipInterfaceName = parseInterfaceName(block, patterns[0]);

		if (ipInterfaceName.startsWith("lo")) {
			return;
		}

		sParse.parse(new ArrayList<String>(), block, patterns[1]).forEach(address -> {
			ipList.add(new String[] { ipInterfaceName, "ipv4", address });
		});

		sParse.parse(new ArrayList<String>(), block, patterns[2]).forEach(address -> {
			ipList.add(new String[] { ipInterfaceName, "ipv6", address });
		});
	}

	private String parseInterfaceName(String block, String patHeader) {
		String ipInterfaceName;
		String[] matches = sParse.parse(block, patHeader);
		if (matches != null) {
			if (matches.length == 1) {
				ipInterfaceName = matches[0];
			} else if (matches.length > 1) {
				ipInterfaceName = "";
				for (int idx = 0; idx < matches.length; idx++) {
					if (matches[idx] != null) {
						ipInterfaceName += matches[idx];
					}
				}
			} else {
				ipInterfaceName = block.substring(0, block.indexOf(" ")).replaceAll("\\n", "");
			}
		} else {
			ipInterfaceName = block.substring(0, block.indexOf(" ")).replaceAll("\\n", "");
		}
		return ipInterfaceName;
	}

	@Override
	public Integer[] createPositionArray(String ifconfig) {
		String[] splits = ifconfig.split("\n");
		ArrayList<Integer> positionList = new ArrayList<>();
		int cursor = 0;

		for (int idx = 0; idx < splits.length; idx++) {
			if (!splits[idx].startsWith("\t") && splits[idx].contains(": flags")) {
				positionList.add(cursor);
			}
			cursor += splits[idx].length() + 1;
		}
		positionList.add(cursor);
		return positionList.toArray(new Integer[positionList.size()]);
	}

}
