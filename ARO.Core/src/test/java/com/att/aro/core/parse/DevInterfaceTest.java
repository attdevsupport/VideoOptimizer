package com.att.aro.core.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.att.aro.core.fileio.IFileManager;
import com.att.aro.core.fileio.impl.FileManagerImpl;

public class DevInterfaceTest {

	IFileManager fm = new FileManagerImpl();
	DevInterface devInterface = new DevInterface();
	
	@Test
	public void testParseWindowsIP() {
		List<String[]> ipList = new ArrayList<>();
		String ipconfig = getWindowsIPconfig();
		ipList = devInterface.parseWindowsIP(ipList, ipconfig);
		ipList.forEach(e -> {
			System.out.println(String.format("0: %s" + " , 1: %s" + " , 2: %s", e[0], e[1], e[2]));
		});

		String[] val = ipList.get(0);
		assertEquals(val[0], "Ethernet adapter Ethernet 2");
		assertEquals(val[1], "ipv4");
		assertEquals(val[2], "135.70.41.215");

		val = ipList.get(3);
		assertEquals(val[0], "Ethernet adapter Ethernet");
		assertEquals(val[1], "ipv4");
		assertEquals(val[2], "192.168.86.45");

		val = ipList.get(4);
		assertEquals(val[0], "Ethernet adapter Ethernet");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::88ff:ec4:58d6:d42");

	}

	@Test
	public void testParseMacIP() {
		List<String[]> ipList = new ArrayList<>();
		String ifconfig = loadMacIfconfig();
		ipList = devInterface.parseUnixIP(ipList, ifconfig);
		ipList.forEach(e -> {
			System.out.println(String.format("0: %s" + " , 1: %s" + " , 2: %s", e[0], e[1], e[2]));
		});

		String[] val = ipList.get(0);
		assertEquals(val[0], "anpi2");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::ac0b:9bff:fe29:7d61");

		val = ipList.get(3);
		assertEquals(val[0], "en0");
		assertEquals(val[1], "ipv4");
		assertEquals(val[2], "192.168.86.42");
		
		val = ipList.get(11);
		assertEquals(val[0], "utun3");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::bed0:74ff:fe00:df4e");
		
		val = ipList.get(12);
		assertEquals(val[0], "utun3");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::6013:3b7a:4ff2:4c0d");
		
	}
	
	@Test
	public void testParseLinuxIP() {
		List<String[]> ipList = new ArrayList<>();
		String ifconfig = loadLinuxIP();
		ipList = devInterface.parseLinuxIP(ipList, ifconfig);
		
		ipList.forEach(e -> {
			System.out.println(String.format("0: %s" + " , 1: %s" + " , 2: %s", e[0], e[1], e[2]));
		});

		String[] val = ipList.get(0);
		assertEquals(val[0], "wlp3s0:");
		assertEquals(val[1], "ipv4");
		assertEquals(val[2], "192.168.86.53");
		
		val = ipList.get(1);
		assertEquals(val[0], "wlp3s0:");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::618e:1845:5a0f:cdc7");
		
		
	}
	
	@Test
	public void testParseAndroidIP() {
		List<String[]> ipList = new ArrayList<>();
		String ipAddr = loadAndroidIP();
		ipList = devInterface.parseLinuxIP(ipList, ipAddr);
		
		ipList.forEach(e -> {
			System.out.println(String.format("0: %s" + " , 1: %s" + " , 2: %s", e[0], e[1], e[2]));
		});	
		
		String[] val = ipList.get(0);
		assertEquals(val[0], "dummy0:");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::741e:6eff:fe8a:b3e7");

		val = ipList.get(1);
		assertEquals(val[0], "wlan0:");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "fe80::4aeb:62ff:fe94:469a");

		val = ipList.get(2);
		assertEquals(val[0], "rmnet_data0");
		assertEquals(val[1], "ipv4");
		assertEquals(val[2], "10.73.93.150");
		
		val = ipList.get(3);
		assertEquals(val[0], "rmnet_data0");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "2600:381:9219:598c:79b0:8fa1:dd75:50b6");
		
		val = ipList.get(4);
		assertEquals(val[0], "rmnet_data1");
		assertEquals(val[1], "ipv6");
		assertEquals(val[2], "2600:381:9f51:6940:9dd:7149:aad9:56b5");
		
	}
	
	@Test
	public void testParseBlock_find_one_ipv6() {
		String[] patterns = new String[] {"([a-zA-Z0-9_]*)[: ]", "inet +(.+) Mask", "inet6 +(.+)[%/]"};
		
		String block = "\n\n" 
				+ "r_rmnet_data0 Link encap:UNSPEC  \n" 
				+ "          inet6 fe80::2a78:d620:d13a:b7a9/64 Scope: Link\n"
				+ "          UP RUNNING  MTU:1500  Metric:1\n" 
				+ "          RX packets:1069 errors:0 dropped:0 overruns:0 frame:0 \n"
				+ "          TX packets:1438 errors:0 dropped:0 overruns:0 carrier:0 \n" 
				+ "          collisions:0 txqueuelen:1000 \n"
				+ "          RX bytes:66278 TX bytes:135758 ";

		List<String[]> ipList = new ArrayList<>();
		devInterface.parseBlock(ipList, block, patterns);
		assertThat(ipList.size() == 1);
		assertThat(ipList.get(0)[0].equals("r_rmnet_data0"));
		assertThat(ipList.get(0)[1].equals("inet6"));
		assertThat(ipList.get(0)[2].equals("fe80::2a78:d620:d13a:b7a9"));
	}
	
	@Test
	public void testParseBlock_find_two_ipv6_one_ip4v() {
		String[] patterns = new String[] {"([a-zA-Z0-9_]*)[: ]", "inet +(.+) Mask", "inet6 +(.+)[%/]"};
		
		String block = "\n\n"
				+ "rmnet_data3 Link encap:UNSPEC  \n"
				+ "          inet6 fe80::d636:3868:ac31:94ba/64 Scope: Link\n"
				+ "          inet6 2600:380:80ea:522a:d636:3868:ac31:94ba/64 Scope: Global\n"
				+ "          inet 10.196.146.228  Mask:255.255.255.248 \n"
				+ "          UP RUNNING  MTU:1430  Metric:1\n"
				+ "          RX packets:112 errors:0 dropped:0 overruns:0 frame:0 \n"
				+ "          TX packets:90 errors:0 dropped:0 overruns:0 carrier:0 \n"
				+ "          collisions:0 txqueuelen:1000 \n"
				+ "          RX bytes:16005 TX bytes:8546 ";

		List<String[]> ipList = new ArrayList<>();
		devInterface.parseBlock(ipList, block, patterns);
		assertThat(ipList.size() == 3);
		assertThat(ipList.get(0)[0].equals("rmnet_data3"));
		assertThat(ipList.get(0)[1].equals("inet6"));
		assertThat(ipList.get(0)[2].equals("fe80::d636:3868:ac31:94ba"));
		
		assertThat(ipList.get(1)[0].equals("rmnet_data3"));
		assertThat(ipList.get(1)[1].equals("inet6"));
		assertThat(ipList.get(1)[2].equals("2600:380:80ea:522a:d636:3868:ac31:94ba"));
		
		assertThat(ipList.get(2)[0].equals("rmnet_data1"));
		assertThat(ipList.get(2)[1].equals("inet"));
		assertThat(ipList.get(2)[2].equals("10.196.146.228"));
	
	}
	
	@Test
	public void testParseBlock_find_one_ipv4() {
		String[] patterns = new String[] {"([a-zA-Z0-9_]*)[: ]", "inet +(.+) Mask", "inet6 +(.+)[%/]"};
		
		String block = "\n\nrmnet_data1 Link encap:UNSPEC  \n"
				+ "          inet 10.196.146.228  Mask:255.255.255.248 \n"
				+ "          UP RUNNING  MTU:1430  Metric:1\n"
				+ "          RX packets:1072 errors:0 dropped:0 overruns:0 frame:0 \n"
				+ "          TX packets:1156 errors:0 dropped:0 overruns:0 carrier:0 \n"
				+ "          collisions:0 txqueuelen:1000 \n"
				+ "          RX bytes:372471 TX bytes:131610 \n"
		;

		List<String[]> ipList = new ArrayList<>();
		devInterface.parseBlock(ipList, block, patterns);
		assertThat(ipList.size() == 1);
		assertThat(ipList.get(0)[0].equals("rmnet_data1"));
		assertThat(ipList.get(0)[1].equals("inet"));
		assertThat(ipList.get(0)[2].equals("10.196.146.228"));
	}
	
	private String loadMacIfconfig() {
		return "\n"
				+ "lo0: flags=8049<UP,LOOPBACK,RUNNING,MULTICAST> mtu 16384\n"
				+ "	options=1203<RXCSUM,TXCSUM,TXSTATUS,SW_TIMESTAMP>\n"
				+ "	inet 127.0.0.1 netmask 0xff000000 \n"
				+ "	inet6 ::1 prefixlen 128 \n"
				+ "	inet6 fe80::1%lo0 prefixlen 64 scopeid 0x1 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "gif0: flags=8010<POINTOPOINT,MULTICAST> mtu 1280\n"
				+ "stf0: flags=0<> mtu 1280\n"
				+ "anpi2: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether ae:0b:9b:29:7d:61 \n"
				+ "	inet6 fe80::ac0b:9bff:fe29:7d61%anpi2 prefixlen 64 scopeid 0x4 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: none\n"
				+ "	status: inactive\n"
				+ "anpi1: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether ae:0b:9b:29:7d:60 \n"
				+ "	inet6 fe80::ac0b:9bff:fe29:7d60%anpi1 prefixlen 64 scopeid 0x5 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: none\n"
				+ "	status: inactive\n"
				+ "anpi0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether ae:0b:9b:29:7d:5f \n"
				+ "	inet6 fe80::ac0b:9bff:fe29:7d5f%anpi0 prefixlen 64 scopeid 0x6 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: none\n"
				+ "	status: inactive\n"
				+ "en5: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether ae:0b:9b:29:7d:3f \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: none\n"
				+ "	status: inactive\n"
				+ "en6: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether ae:0b:9b:29:7d:40 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: none\n"
				+ "	status: inactive\n"
				+ "en7: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether ae:0b:9b:29:7d:41 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: none\n"
				+ "	status: inactive\n"
				+ "en1: flags=8963<UP,BROADCAST,SMART,RUNNING,PROMISC,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=460<TSO4,TSO6,CHANNEL_IO>\n"
				+ "	ether 36:01:66:ea:f8:40 \n"
				+ "	media: autoselect <full-duplex>\n"
				+ "	status: inactive\n"
				+ "en2: flags=8963<UP,BROADCAST,SMART,RUNNING,PROMISC,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=460<TSO4,TSO6,CHANNEL_IO>\n"
				+ "	ether 36:01:66:ea:f8:44 \n"
				+ "	media: autoselect <full-duplex>\n"
				+ "	status: inactive\n"
				+ "en3: flags=8963<UP,BROADCAST,SMART,RUNNING,PROMISC,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=460<TSO4,TSO6,CHANNEL_IO>\n"
				+ "	ether 36:01:66:ea:f8:48 \n"
				+ "	media: autoselect <full-duplex>\n"
				+ "	status: inactive\n"
				+ "ap1: flags=8843<UP,BROADCAST,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether be:d0:74:00:df:4e \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: autoselect\n"
				+ "	status: inactive\n"
				+ "en0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=6463<RXCSUM,TXCSUM,TSO4,TSO6,CHANNEL_IO,PARTIAL_CSUM,ZEROINVERT_CSUM>\n"
				+ "	ether bc:d0:74:00:df:4e \n"
				+ "	inet6 fe80::1ca9:941:348f:8ea3%en0 prefixlen 64 secured scopeid 0xf \n"
				+ "	inet 192.168.86.42 netmask 0xffffff00 broadcast 192.168.86.255\n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: autoselect\n"
				+ "	status: active\n"
				+ "awdl0: flags=8943<UP,BROADCAST,RUNNING,PROMISC,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether 1a:c1:bd:f6:ae:47 \n"
				+ "	inet6 fe80::18c1:bdff:fef6:ae47%awdl0 prefixlen 64 scopeid 0x10 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: autoselect\n"
				+ "	status: active\n"
				+ "llw0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=400<CHANNEL_IO>\n"
				+ "	ether 1a:c1:bd:f6:ae:47 \n"
				+ "	inet6 fe80::18c1:bdff:fef6:ae47%llw0 prefixlen 64 scopeid 0x11 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: autoselect\n"
				+ "	status: active\n"
				+ "bridge0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=63<RXCSUM,TXCSUM,TSO4,TSO6>\n"
				+ "	ether 36:01:66:ea:f8:40 \n"
				+ "	Configuration:\n"
				+ "		id 0:0:0:0:0:0 priority 0 hellotime 0 fwddelay 0\n"
				+ "		maxage 0 holdcnt 0 proto stp maxaddr 100 timeout 1200\n"
				+ "		root id 0:0:0:0:0:0 priority 0 ifcost 0 port 0\n"
				+ "		ipfilter disabled flags 0x0\n"
				+ "	member: en1 flags=3<LEARNING,DISCOVER>\n"
				+ "	        ifmaxaddr 0 port 10 priority 0 path cost 0\n"
				+ "	member: en2 flags=3<LEARNING,DISCOVER>\n"
				+ "	        ifmaxaddr 0 port 11 priority 0 path cost 0\n"
				+ "	member: en3 flags=3<LEARNING,DISCOVER>\n"
				+ "	        ifmaxaddr 0 port 12 priority 0 path cost 0\n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: <unknown type>\n"
				+ "	status: inactive\n"
				+ "en4: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500\n"
				+ "	options=6467<RXCSUM,TXCSUM,VLAN_MTU,TSO4,TSO6,CHANNEL_IO,PARTIAL_CSUM,ZEROINVERT_CSUM>\n"
				+ "	ether a0:ce:c8:15:12:22 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "	media: autoselect (none)\n"
				+ "	status: inactive\n"
				+ "utun0: flags=8051<UP,POINTOPOINT,RUNNING,MULTICAST> mtu 1380\n"
				+ "	inet6 fe80::6dfb:f041:d1cd:e975%utun0 prefixlen 64 scopeid 0x14 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "utun1: flags=8051<UP,POINTOPOINT,RUNNING,MULTICAST> mtu 2000\n"
				+ "	inet6 fe80::4905:3c3:f19c:4b43%utun1 prefixlen 64 scopeid 0x15 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "utun2: flags=8051<UP,POINTOPOINT,RUNNING,MULTICAST> mtu 1000\n"
				+ "	inet6 fe80::ce81:b1c:bd2c:69e%utun2 prefixlen 64 scopeid 0x16 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "utun3: flags=80d1<UP,POINTOPOINT,RUNNING,NOARP,MULTICAST> mtu 1300\n"
				+ "	inet 130.10.254.150 --> 130.10.254.150 netmask 0xfffff000 \n"
				+ "	inet6 fe80::bed0:74ff:fe00:df4e%utun3 prefixlen 64 scopeid 0x17 \n"
				+ "	inet6 fe80::6013:3b7a:4ff2:4c0d%utun3 prefixlen 128 scopeid 0x17 \n"
				+ "	nd6 options=201<PERFORMNUD,DAD>\n"
				+ "";
	}
	private String loadLinuxIP() {
		// results from 'ip addr' on linux (ifconfig is non-standard and likely will not exist unless installed)
		return "1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000\n"
				+ "    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00\n"
				+ "    inet 127.0.0.1/8 scope host lo\n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "    inet6 ::1/128 scope host \n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "2: wlp3s0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP group default qlen 1000\n"
				+ "    link/ether 20:c9:d0:8a:bf:1d brd ff:ff:ff:ff:ff:ff\n"
				+ "    inet 192.168.86.53/24 brd 192.168.86.255 scope global dynamic noprefixroute wlp3s0\n"
				+ "       valid_lft 86078sec preferred_lft 86078sec\n"
				+ "    inet6 fe80::618e:1845:5a0f:cdc7/64 scope link noprefixroute \n"
				+ "       valid_lft forever preferred_lft forever"
				+ "";
	}
	private String getWindowsIPconfig() {
		return "\n"
				 + "Windows IP Configuration\n\n"
				 + "   Host Name . . . . . . . . . . . . : WACDTLB23N153X\n"
				 + "   Primary Dns Suffix  . . . . . . . : ITServices.sbc.com\n"
				 + "   Node Type . . . . . . . . . . . . : Hybrid\n"
				 + "   IP Routing Enabled. . . . . . . . : No\n"
				 + "   WINS Proxy Enabled. . . . . . . . : No\n"
				 + "   DNS Suffix Search List. . . . . . : att.com\n"
				 + "                                       ITServices.sbc.com\n\n"
				 + "Ethernet adapter Ethernet 2:\n\n"
				 + "   Connection-specific DNS Suffix  . : att.com\n"
				 + "   Description . . . . . . . . . . . : Cisco AnyConnect Secure Mobility Client Virtual Miniport Adapter for Windows x64\n"
				 + "   Physical Address. . . . . . . . . : 00-05-9A-3C-7A-00\n"
				 + "   DHCP Enabled. . . . . . . . . . . : No\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n"
				 + "   Link-local IPv6 Address . . . . . : fe80::24d4:6fd1:e3db:83a0%17(Preferred)\n"
				 + "   Link-local IPv6 Address . . . . . : fe80::aa39:26dd:2554:cbed%17(Preferred)\n"
				 + "   IPv4 Address. . . . . . . . . . . : 135.70.41.215(Preferred)\n"
				 + "   Subnet Mask . . . . . . . . . . . : 255.255.240.0\n"
				 + "   Default Gateway . . . . . . . . . : ::\n"
				 + "                                       135.70.32.1\n"
				 + "   DHCPv6 IAID . . . . . . . . . . . : 285214106\n"
				 + "   DHCPv6 Client DUID. . . . . . . . : 00-01-00-01-28-0A-92-0F-B0-0C-D1-4B-3D-38\n"
				 + "   DNS Servers . . . . . . . . . . . : 150.234.210.5\n"
				 + "                                       150.234.210.205\n"
				 + "   NetBIOS over Tcpip. . . . . . . . : Disabled\n\n"
				 + "Ethernet adapter Ethernet:\n\n"
				 + "   Connection-specific DNS Suffix  . : lan\n"
				 + "   Description . . . . . . . . . . . : Intel(R) Ethernet Connection (4) I219-LM\n"
				 + "   Physical Address. . . . . . . . . : B0-0C-D1-4B-3D-38\n"
				 + "   DHCP Enabled. . . . . . . . . . . : Yes\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n"
				 + "   Link-local IPv6 Address . . . . . : fe80::88ff:ec4:58d6:d42%18(Preferred)\n"
				 + "   IPv4 Address. . . . . . . . . . . : 192.168.86.45(Preferred)\n"
				 + "   Subnet Mask . . . . . . . . . . . : 255.255.255.0\n"
				 + "   Lease Obtained. . . . . . . . . . : Thursday, October 6, 2022 5:20:56 PM\n"
				 + "   Lease Expires . . . . . . . . . . : Saturday, October 8, 2022 3:58:22 PM\n"
				 + "   Default Gateway . . . . . . . . . : 192.168.86.1\n"
				 + "   DHCP Server . . . . . . . . . . . : 192.168.86.1\n"
				 + "   DHCPv6 IAID . . . . . . . . . . . : 112200913\n"
				 + "   DHCPv6 Client DUID. . . . . . . . : 00-01-00-01-28-0A-92-0F-B0-0C-D1-4B-3D-38\n"
				 + "   DNS Servers . . . . . . . . . . . : 192.168.86.1\n"
				 + "   NetBIOS over Tcpip. . . . . . . . : Disabled\n\n"
				 + "Wireless LAN adapter Local Area Connection* 1:\n\n"
				 + "   Media State . . . . . . . . . . . : Media disconnected\n"
				 + "   Connection-specific DNS Suffix  . :\n"
				 + "   Description . . . . . . . . . . . : Microsoft Wi-Fi Direct Virtual Adapter\n"
				 + "   Physical Address. . . . . . . . . : FC-77-74-C0-28-EA\n"
				 + "   DHCP Enabled. . . . . . . . . . . : Yes\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n\n"
				 + "Wireless LAN adapter Local Area Connection* 2:\n\n"
				 + "   Media State . . . . . . . . . . . : Media disconnected\n"
				 + "   Connection-specific DNS Suffix  . :\n"
				 + "   Description . . . . . . . . . . . : Microsoft Wi-Fi Direct Virtual Adapter #2\n"
				 + "   Physical Address. . . . . . . . . : FE-77-74-C0-28-E9\n"
				 + "   DHCP Enabled. . . . . . . . . . . : Yes\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n\n"
				 + "Ethernet adapter Bluetooth Network Connection:\n\n"
				 + "   Media State . . . . . . . . . . . : Media disconnected\n"
				 + "   Connection-specific DNS Suffix  . :\n"
				 + "   Description . . . . . . . . . . . : Bluetooth Device (Personal Area Network)\n"
				 + "   Physical Address. . . . . . . . . : FC-77-74-C0-28-ED\n"
				 + "   DHCP Enabled. . . . . . . . . . . : Yes\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n\n"
				 + "Wireless LAN adapter Wi-Fi:\n\n"
				 + "   Media State . . . . . . . . . . . : Media disconnected\n"
				 + "   Connection-specific DNS Suffix  . : lan\n"
				 + "   Description . . . . . . . . . . . : Intel(R) Dual Band Wireless-AC 8265\n"
				 + "   Physical Address. . . . . . . . . : FC-77-74-C0-28-E9\n"
				 + "   DHCP Enabled. . . . . . . . . . . : Yes\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n\n"
				 + "Mobile Broadband adapter Cellular:\n\n"
				 + "   Media State . . . . . . . . . . . : Media disconnected\n"
				 + "   Connection-specific DNS Suffix  . :\n"
				 + "   Description . . . . . . . . . . . : Generic Mobile Broadband Adapter\n"
				 + "   Physical Address. . . . . . . . . : 80-99-85-5D-50-5E\n"
				 + "   DHCP Enabled. . . . . . . . . . . : No\n"
				 + "   Autoconfiguration Enabled . . . . : Yes\n";
	}
	private String loadAndroidIP() {
		// results from 'ip addr' on android (ifconfig is non-standard and likely will not exist)
		return "1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000\n"
				+ "    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00\n"
				+ "    inet 127.0.0.1/8 scope host lo\n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "    inet6 ::1/128 scope host \n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "2: dummy0: <BROADCAST,NOARP,UP,LOWER_UP> mtu 1500 qdisc noqueue state UNKNOWN group default qlen 1000\n"
				+ "    link/ether 76:1e:6e:8a:b3:e7 brd ff:ff:ff:ff:ff:ff\n"
				+ "    inet6 fe80::741e:6eff:fe8a:b3e7/64 scope link \n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "3: tunl0@NONE: <NOARP> mtu 1480 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/ipip 0.0.0.0 brd 0.0.0.0\n"
				+ "4: gre0@NONE: <NOARP> mtu 1476 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/gre 0.0.0.0 brd 0.0.0.0\n"
				+ "5: gretap0@NONE: <BROADCAST,MULTICAST> mtu 1462 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/ether 00:00:00:00:00:00 brd ff:ff:ff:ff:ff:ff\n"
				+ "6: erspan0@NONE: <BROADCAST,MULTICAST> mtu 1450 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/ether 00:00:00:00:00:00 brd ff:ff:ff:ff:ff:ff\n"
				+ "7: ip_vti0@NONE: <NOARP> mtu 1480 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/ipip 0.0.0.0 brd 0.0.0.0\n"
				+ "8: ip6_vti0@NONE: <NOARP> mtu 1364 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/tunnel6 :: brd ::\n"
				+ "9: sit0@NONE: <NOARP> mtu 1480 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/sit 0.0.0.0 brd 0.0.0.0\n"
				+ "10: ip6tnl0@NONE: <NOARP> mtu 1452 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/tunnel6 :: brd ::\n"
				+ "11: ip6gre0@NONE: <NOARP> mtu 1448 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/gre6 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00 brd 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00\n"
				+ "12: p2p0: <BROADCAST,MULTICAST> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/ether 00:90:4c:33:22:11 brd ff:ff:ff:ff:ff:ff\n"
				+ "13: wlan0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UNKNOWN group default qlen 1000\n"
				+ "    link/ether da:a3:1c:28:ab:dc brd ff:ff:ff:ff:ff:ff\n"
				+ "    inet6 fe80::4aeb:62ff:fe94:469a/64 scope link \n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "14: swlan0: <BROADCAST,MULTICAST> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/ether 02:90:4c:3e:6a:38 brd ff:ff:ff:ff:ff:ff\n"
				+ "15: rmnet_ipa0: <UP,LOWER_UP> mtu 9216 qdisc pfifo_fast state UNKNOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "17: epdg4: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "18: epdg5: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "19: epdg0: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "20: epdg1: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "21: epdg2: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "22: epdg3: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "23: epdg6: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "24: epdg7: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN group default qlen 500\n"
				+ "    link/none \n"
				+ "25: rmnet_data4@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "26: rmnet_data5@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "27: rmnet_data6@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "28: rmnet_data7@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "29: rmnet_data8@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "30: rmnet_data9@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "31: rmnet_data0@rmnet_ipa0: <UP,LOWER_UP> mtu 1430 qdisc mq state UNKNOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "    inet 10.73.93.150/30 brd 10.73.93.151 scope global rmnet_data0\n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "    inet6 2600:381:9219:598c:79b0:8fa1:dd75:50b6/64 scope global \n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "32: rmnet_data10@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "33: rmnet_data1@rmnet_ipa0: <UP,LOWER_UP> mtu 1430 qdisc mq state UNKNOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "    inet6 2600:381:9f51:6940:9dd:7149:aad9:56b5/64 scope global \n"
				+ "       valid_lft forever preferred_lft forever\n"
				+ "34: rmnet_data2@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519] \n"
				+ "35: rmnet_data3@rmnet_ipa0: <> mtu 1500 qdisc noop state DOWN group default qlen 1000\n"
				+ "    link/[519]"
				+ "";
	}
}
