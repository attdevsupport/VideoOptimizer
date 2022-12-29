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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.att.aro.datacollector.ioscollector.attenuator;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.att.aro.core.datacollector.pojo.StatusResult;

import lombok.Getter;

public class MitmAttenuatorImpl {
	
	private static final Logger LOG = LogManager.getLogger(MitmAttenuatorImpl.class.getName());
	@Getter
	private Date startDate;
	@Getter
	private Date stopDate;
	private ExecutorService pool;
	private LittleProxyWrapper littleProxy;
 	public static final String COLLECT_OPTIONS = "collect_options";
 	private static final int THREAD_NUM = 3;
    public void startCollect(String traceFolder,int throttleReadStream, int throttleWriteStream,
    		SaveCollectorOptions saveCollectorOptions, StatusResult status, String sudoPassword, String trafficFilePath) {
		LOG.info("Launch mitm and pcap4j thread pool");
		startDate = new Date();
		int throttleReadStreambps =  throttleReadStream*128;
		int throttleWriteStreambps = throttleWriteStream*128;
		LOG.info("Little proxy throttle: "+"throttleReadStreambps: "
			    + throttleReadStreambps + "throttleWriteStreambps: "+ throttleWriteStreambps );
		recordCollectOptions(traceFolder, 0, 0, throttleReadStream, throttleWriteStream, false, "",
				"PORTRAIT", saveCollectorOptions);
		littleProxy = new LittleProxyWrapper(status, sudoPassword, trafficFilePath);
		littleProxy.setThrottleReadStream(throttleReadStreambps);
	    littleProxy.setThrottleWriteStream(throttleWriteStreambps);
	    littleProxy.setTraceFolder(traceFolder);
   		pool = Executors.newFixedThreadPool(THREAD_NUM);
 		pool.execute(littleProxy);
     }
    
    public void stopCollect() {
        LOG.info("Stopping attenuator...");
    	if(littleProxy!=null) {
    		littleProxy.stop();	
    		stopDate = new Date();
    	}
		if(pool!=null) {
			pool.shutdown();
			try {
				// Wait a while for existing tasks to terminate
				if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
					pool.shutdownNow(); // Cancel currently executing tasks
					// Wait a while for tasks to respond to being cancelled
					if (!pool.awaitTermination(1, TimeUnit.SECONDS))
						LOG.error("Pool did not terminate");
				}
			} catch (InterruptedException ie) {
			    LOG.warn("Attenuator thread interrupted. Trying again to terminate all tasks.");
				// (Re-)Cancel if current thread also interrupted
				pool.shutdownNow();
				// Preserve interrupt status
				Thread.currentThread().interrupt();
			}	    		
		} 
    }
    
	private void recordCollectOptions(String trafficFilePath, int delayTimeDL, int delayTimeUL, int throttleDL,
			int throttleUL, boolean atnrProfile, String atnrProfileName, String videoOrientation,
			SaveCollectorOptions writeCollectOption) {
		LOG.info("set Down stream Delay Time: " + delayTimeDL + " set Up stream Delay Time: " + delayTimeUL
				+ " set Profile: " + atnrProfile + " set Profile name: " + atnrProfileName);
		// compatibility for throttle definition
		if (throttleDL == 0) {
			throttleDL = -1;
		}
		if (throttleUL == 0) {
			throttleUL = -1;
		}
		writeCollectOption.recordCollectOptions(trafficFilePath, 0, 0, throttleDL, throttleUL, atnrProfile,
				atnrProfileName, "PORTRAIT");

	}
 	
}
