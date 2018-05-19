package com.att.aro.datacollector.ioscollector.attenuator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.att.aro.core.ILogger;
import com.att.aro.core.impl.LoggerImpl;
import com.att.aro.core.video.pojo.VideoOption;

public class MitmAttenuatorImpl {
	
	private ILogger log = new LoggerImpl("MitmAttenuatorImpl");
	private ExecutorService pool;
	private LittleProxyWrapper littleproxy ;
	private Pcap4JWrapper pcap4J;
	private Thread proxyThread;
	private Thread pcap4jThread;
	public static final String COLLECT_OPTIONS = "collect_options";

    public void startCollect(String trafficFilePath,int throttleReadStream, int throttleWriteStream) {
		log.info("Launch mitm and pcap4j thread pool");
 
		int throttleReadStreambps =  throttleReadStream*128;
		int throttleWriteStreambps = throttleWriteStream*128;
		log.info("Little proxy throttle: "+"throttleReadStreambps: "
			    + throttleReadStreambps + "throttleWriteStreambps: "+ throttleWriteStreambps );
		recordCollectOptions(trafficFilePath,0, 
				0, throttleReadStream, throttleWriteStream, false,
				"", "PORTRAIT");
		littleproxy = new LittleProxyWrapper();
		littleproxy.setThrottleReadStream(throttleReadStreambps);
	    littleproxy.setThrottleWriteStream(throttleWriteStreambps);
		pcap4J = new Pcap4JWrapper();
		pcap4J.setPCAP_FILE_PATH(trafficFilePath);
		proxyThread = new Thread(littleproxy, "littleProxy");
 		pcap4jThread = new Thread(pcap4J, "pcap4j");
 		pool = Executors.newFixedThreadPool(2);
 		pool.execute(littleproxy);
 		pool.execute(pcap4J);
     }
    
    public void stopCollect() {
		log.info( "Thread name is: " + proxyThread.getName());
		log.info( "Thread name is: "+ pcap4jThread.getName());
		littleproxy.stop();
		pcap4J.stopPcap4jWrapper();
    		pool.shutdown();
    		try {
    			// Wait a while for existing tasks to terminate
    			if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
    				pool.shutdownNow(); // Cancel currently executing tasks
    				// Wait a while for tasks to respond to being cancelled
    				if (!pool.awaitTermination(1, TimeUnit.SECONDS))
    					log.error("Pool did not terminate");
    			}
    		} catch (InterruptedException ie) {
    			// (Re-)Cancel if current thread also interrupted
    			pool.shutdownNow();
    			// Preserve interrupt status
    			Thread.currentThread().interrupt();
    		}
    		 
    }
    
	private void recordCollectOptions(String trafficFilePath,int delayTimeDL, 
			int delayTimeUL, int throttleDL, int throttleUL, boolean atnrProfile,
			String atnrProfileName, String videoOrientation){
		log.info("set Down stream Delay Time: "+ delayTimeDL
				+ " set Up stream Delay Time: "+delayTimeUL
				+ " set Profile: "+atnrProfile
				+ " set Profile name: "+ atnrProfileName);
		//compatibility for throttle definition
		if(throttleDL == 0) {
			throttleDL = -1;
		}
		if(throttleUL == 0) {
			throttleUL = -1;
		}
		File file = new File(trafficFilePath, COLLECT_OPTIONS);
		log.info( "create file:" + file.getAbsolutePath());
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			file.createNewFile();
			bw.write("dsDelay=" + delayTimeDL + System.lineSeparator()
					+ "usDelay=" + delayTimeUL + System.lineSeparator()
					+ "throttleDL=" + throttleDL + System.lineSeparator()
					+ "throttleUL=" + throttleUL + System.lineSeparator()
					+ "orientation=" + videoOrientation + System.lineSeparator()
					+ "attnrProfile="+ atnrProfile + System.lineSeparator()
					+ "attnrProfileName="+ atnrProfileName
			);
			bw.close();
		} catch (IOException e) {
			log.info( "setDeviceDetails() Exception:" + e.getMessage());
			e.printStackTrace();
			return;
		}

	}

 	
}
