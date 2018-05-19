package com.att.aro.datacollector.ioscollector.attenuator;

import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;

import com.att.aro.core.ILogger;
import com.att.aro.core.impl.LoggerImpl;

public class LittleProxyWrapper implements Runnable {
	
	private ILogger log = new LoggerImpl("LittleProxyWrapper");
    private HttpProxyServer proxyServer;

	private int defaultPort = 8080;
	private int throttleReadStream = -1;
	private int throttleWriteStream = -1;
	
	public int getThrottleReadStream() {
		return throttleReadStream;
	}

	public int getThrottleWriteStream() {
		return throttleWriteStream;
	}

	public void setThrottleReadStream(int throttleReadStream) {
		this.throttleReadStream = throttleReadStream;
	}

	public void setThrottleWriteStream(int throttleWriteStream) {
		this.throttleWriteStream = throttleWriteStream;
	}

	@Override
	public void run() {
		littleProxyLauncher();		
	}

	private void littleProxyLauncher() {
		
		try {
			ThreadPoolConfiguration config = new ThreadPoolConfiguration();
			config.withClientToProxyWorkerThreads(1);
			config.withAcceptorThreads(2);
			config.withProxyToServerWorkerThreads(1);
			log.info("About to start server on port: " + defaultPort);
			HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer
					.bootstrapFromFile("./littleproxy.properties")
					.withPort(defaultPort)
					.withAllowLocalOnly(false)
					.withThreadPoolConfiguration(config)
					.withThrottling(getThrottleReadStream(), getThrottleWriteStream())
					;
			
			log.info("About to start...");
			proxyServer = bootstrap.start();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
 		}

	}
	
	public void stop() {
		if(proxyServer!=null) {
			proxyServer.stop();
		}
	}
	
	

}
