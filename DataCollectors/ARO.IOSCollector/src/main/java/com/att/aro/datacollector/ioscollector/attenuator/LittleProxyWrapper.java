package com.att.aro.datacollector.ioscollector.attenuator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;

public class LittleProxyWrapper implements Runnable {

	private static final Logger LOG = LogManager.getLogger(LittleProxyWrapper.class.getName());

	private HttpProxyServer proxyServer;

	private int defaultPort = 8080;
	private int throttleReadStream = -1;
	private int throttleWriteStream = -1;

	private String TRACE_FILE_PATH = "";

	public String getTRACE_FILE_PATH() {
		return TRACE_FILE_PATH;
	}

	public void setTRACE_FILE_PATH(String tRACE_FILE_PATH) {
		TRACE_FILE_PATH = tRACE_FILE_PATH;
	}

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
		LOG.info("About to start little proxy...");

		try {
			ThreadPoolConfiguration config = new ThreadPoolConfiguration();
			config.withClientToProxyWorkerThreads(1);
			config.withAcceptorThreads(2);
			config.withProxyToServerWorkerThreads(1);
			LOG.info("About to start server on port: " + defaultPort);

			HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrapFromFile("./littleproxy.properties")
					.withPort(defaultPort).withAllowLocalOnly(false).withThreadPoolConfiguration(config)
					.withThrottling(getThrottleReadStream(), getThrottleWriteStream());
			proxyServer = bootstrap.start();

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

	}

	public void stop() {
		if (proxyServer != null) {
			LOG.info("stop little proxy");
			proxyServer.stop();
		}
	}

}
