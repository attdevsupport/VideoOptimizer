/*
 *  Copyright 2018 AT&T
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
package com.att.aro.datacollector.ioscollector.attenuator;

import java.util.concurrent.ConcurrentSkipListSet;

import org.littleshoot.proxy.TransportProtocol;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;

import com.att.aro.core.ILogger;
import com.att.aro.core.impl.LoggerImpl;
import com.att.aro.core.util.Util;

public class Pcap4JWrapper implements Runnable{
	private ILogger log = new LoggerImpl("PcapForJWrapper");
	private final String COUNT_KEY = Pcap4JWrapper.class.getName() + ".count";
	private final int COUNT = Integer.getInteger(COUNT_KEY, -1);
	private final String READ_TIMEOUT_KEY = Pcap4JWrapper.class.getName() + ".readTimeout";
	private final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]
	private final String SNAPLEN_KEY = Pcap4JWrapper.class.getName() + ".snaplen";
	private final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]
	private final String PCAP_FILE_KEY = Pcap4JWrapper.class.getName() + ".pcapFile";
	private final String PCAP_FILE = System.getProperty(PCAP_FILE_KEY, "DumpLoop.pcap");
	private String PCAP_FILE_PATH = "";
	private PcapDumper dumper;
	private 	PcapHandle handle;

	protected final ConcurrentSkipListSet<TransportProtocol> TRANSPORTS_USED = new ConcurrentSkipListSet<TransportProtocol>();

	@Override
	public void run() {
		getPcapFile(PCAP_FILE_PATH) ;
	}
 
	private void getPcapFile(String trafficFilePath) {

		PcapNetworkInterface nif;
		try {
			nif = Pcaps.getDevByName("bridge100");		
		} catch (/*IOException | */PcapNativeException pcape) {
			log.info("PcapNativeException :", pcape);
			return;
		} 
		
		if (nif == null) {
			return;
		}

		log.info(nif.getName() + "(" + nif.getDescription() + ")");

		try {
		    handle
		      = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
		    handle.setFilter("", BpfCompileMode.OPTIMIZE);

		    dumper = handle.dumpOpen(trafficFilePath+ Util.FILE_SEPARATOR + PCAP_FILE);
			try {
			      handle.loop(COUNT, dumper);
		    } catch (InterruptedException e) {
				log.error("InterruptedException :", e);
		    }
		} catch (PcapNativeException | NotOpenException e) {
			log.error("PcapNativeException or NotOpenException :", e);
		}

	}
	
	public void stopPcap4jWrapper() {
      if (handle != null && handle.isOpen()) {
          try {
            handle.breakLoop();
          } catch (NotOpenException noe) {
  			log.error("NotOpenException :", noe);
          }
          
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ioe) {
    			log.error("InterruptedException :", ioe);
          }
          handle.close();
        }
      	if(dumper!=null) {
      		dumper.close();
      	}

	}
    public String getPCAP_FILE_PATH() {
		return this.PCAP_FILE_PATH;
	}

	public void setPCAP_FILE_PATH(String pCAP_FILE_PATH) {
		this.PCAP_FILE_PATH = pCAP_FILE_PATH;
	}

}
