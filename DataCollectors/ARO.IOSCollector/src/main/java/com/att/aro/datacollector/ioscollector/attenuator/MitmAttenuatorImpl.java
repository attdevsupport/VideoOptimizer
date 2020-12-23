package com.att.aro.datacollector.ioscollector.attenuator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class MitmAttenuatorImpl {
	
	private static final Logger LOG = LogManager.getLogger(MitmAttenuatorImpl.class.getName());
	private ExecutorService pool;
	private LittleProxyWrapper littleProxy;
 	public static final String COLLECT_OPTIONS = "collect_options";
 	private static final int THREAD_NUM = 3;
    public void startCollect(String trafficFilePath,int throttleReadStream, int throttleWriteStream,
    		SaveCollectorOptions saveCollectorOptions) {
		LOG.info("Launch mitm and pcap4j thread pool");
 
		int throttleReadStreambps =  throttleReadStream*128;
		int throttleWriteStreambps = throttleWriteStream*128;
		LOG.info("Little proxy throttle: "+"throttleReadStreambps: "
			    + throttleReadStreambps + "throttleWriteStreambps: "+ throttleWriteStreambps );
		recordCollectOptions(trafficFilePath, 0, 0, throttleReadStream, throttleWriteStream, false, "",
				"PORTRAIT", saveCollectorOptions);
		littleProxy = new LittleProxyWrapper();
		littleProxy.setThrottleReadStream(throttleReadStreambps);
	    littleProxy.setThrottleWriteStream(throttleWriteStreambps);
	    littleProxy.setTRACE_FILE_PATH(trafficFilePath);
   		pool = Executors.newFixedThreadPool(THREAD_NUM);
 		pool.execute(littleProxy);
     }
    
    public void stopCollect() {
        LOG.info("Stopping attenuator...");
    	if(littleProxy!=null) {
    		littleProxy.stop();		
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
