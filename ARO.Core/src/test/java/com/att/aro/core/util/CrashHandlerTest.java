/*
 *  Copyright 2022 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.core.util;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.att.aro.core.BaseTest;

public class CrashHandlerTest extends BaseTest {

	CrashHandler crashHandler;
	final String LF = System.lineSeparator();
	
	@Before
	public void setUp() throws Exception {
		crashHandler = new CrashHandler();
	}

	@Test
	public void testParseAll_where_finds_three_ARO() {
		String stack = "java.util.IllegalFormatConversionException: d != java.lang.Double" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.buildRawData(AbstractPacket.java:137)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:52)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:49)" + LF
				+ "	at com.att.aro.core.packetreader.pojo.Packet.<init>(Packet.java:40)" + LF
				+ "	at com.att.aro.core.packetreader.pojo.IPPacket.<init>(IPPacket.java:60)" + LF
				+ "	at com.att.aro.core.packetreader.pojo.TCPPacket.<init>(TCPPacket.java:77)" + LF
				+ "	at org.pcap4j.util.LazyValue.getValue(LazyValue.java:41)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.getRawData(AbstractPacket.java:162)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.buildRawData(AbstractPacket.java:147)" + LF
				+ "	at org.pcap4j.packet.EthernetPacket.buildRawData(EthernetPacket.java:170)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:52)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:49)" + LF
				+ "	at org.pcap4j.util.LazyValue.getValue(LazyValue.java:41)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.getRawData(AbstractPacket.java:162)" + LF
				;
		
		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("java.util.IllegalFormatConversionException	|	Packet.<init>@40	|	IPPacket.<init>@60	|	TCPPacket.<init>@77");
	}
	
	@Test
	public void testParseAll_without_ARO() {
		String stack = "java.util.IllegalFormatConversionException: d != java.lang.Double" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.buildRawData(AbstractPacket.java:137)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:52)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:49)" + LF
				+ "	at org.pcap4j.util.LazyValue.getValue(LazyValue.java:41)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.getRawData(AbstractPacket.java:162)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.buildRawData(AbstractPacket.java:147)" + LF
				+ "	at org.pcap4j.packet.EthernetPacket.buildRawData(EthernetPacket.java:170)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:52)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:49)" + LF
				+ "	at org.pcap4j.util.LazyValue.getValue(LazyValue.java:41)" + LF
				+ "	at org.pcap4j.packet.AbstractPacket.getRawData(AbstractPacket.java:162)" + LF
				;
		
		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("java.util.IllegalFormatConversionException	|	org.pcap4j.packet.AbstractPacket.buildRawData(AbstractPacket.java:137)	|	org.pcap4j.packet.AbstractPacket$2.buildValue(AbstractPacket.java:52)");
	}

	@Test
	public void testParseAll_big_one_aroLine() {
		String stack = "java.util.IllegalFormatConversionException: d != java.lang.Double" + LF
				+ "	at java.util.Formatter$FormatSpecifier.failConversion(Formatter.java:4302)" + LF
				+ "	at java.util.Formatter$FormatSpecifier.printInteger(Formatter.java:2793)" + LF
				+ "	at java.util.Formatter$FormatSpecifier.print(Formatter.java:2747)" + LF
				+ "	at java.util.Formatter.format(Formatter.java:2520)" + LF
				+ "	at java.util.Formatter.format(Formatter.java:2455)" + LF
				+ "	at java.lang.String.format(String.java:2940)" + LF
				+ "	at com.att.aro.ui.view.diagnostictab.plot.BufferInSecondsPlot$1.generateToolTip(BufferInSecondsPlot.java:141)" + LF
				+ "	at org.jfree.chart.renderer.xy.AbstractXYItemRenderer.addEntity(AbstractXYItemRenderer.java:1757)" + LF
				+ "	at org.jfree.chart.renderer.xy.StandardXYItemRenderer.drawItem(StandardXYItemRenderer.java:989)" + LF
				+ "	at org.jfree.chart.plot.XYPlot.render(XYPlot.java:3782)" + LF
				+ "	at org.jfree.chart.plot.XYPlot.draw(XYPlot.java:3342)" + LF
				+ "	at org.jfree.chart.plot.CombinedDomainXYPlot.draw(CombinedDomainXYPlot.java:484)" + LF
				+ "	at org.jfree.chart.JFreeChart.draw(JFreeChart.java:1242)" + LF
				+ "	at org.jfree.chart.ChartPanel.paintComponent(ChartPanel.java:1629)" + LF
				+ "	at javax.swing.JComponent.paint(JComponent.java:1056)" + LF
				+ "	at javax.swing.JComponent.paintChildren(JComponent.java:889)" + LF
				+ "	at javax.swing.JComponent.paint(JComponent.java:1065)" + LF
				+ "	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:93)" + LF
				+ "	at java.awt.EventDispatchThread.run(EventDispatchThread.java:82)"
				;
		
		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("java.util.IllegalFormatConversionException	|	BufferInSecondsPlot$1.generateToolTip@141");
	}

	@Test
	public void testParseAll_when_three_lines_and_aro() {
		String stack = "ab:do not find me" + LF
				+ " at com.att.aro.cd.Mine.foo(ignore.this:123)" + LF
				+ " at com.att.aro.ef.That.bar(ignore.this:321)";

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("ab	|	Mine.foo@123	|	That.bar@321");
	}

	@Test
	public void testParseAll_when_three_lines_and_colons() {
		String stack = "ab:do not find me" + LF
				+ " at cd" + LF
				+ " at ef";

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("ab	|	cd	|	ef");
	}

	@Test
	public void testParseAll_when_three_lines_and_no_colon() {
		String stack = "ab" + LF
				+ "at cd" + LF
				+ "at ef";

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("ab");
	}

	@Test
	public void testParseAll_when_three_lines_and_no_at_or_colon() {
		String stack = "ab" + LF
				+ "cd" + LF
				+ "ef";

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("ab");
	}
	
	@Test
	public void testParseAll_when_no_match() {
		String stack = "abcdef";

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("abcdef");
	}

	@Test
	public void testParseAll_when_EmptyString() {
		String stack = "";

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("");
	}

	@Test
	public void testParseAll_when_target_is_NULL() {
		String stack = null;

		String report = crashHandler.prepReport(stack);
		Assertions.assertThat(report).isEqualTo("");
	}
}
