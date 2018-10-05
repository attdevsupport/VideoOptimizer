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

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
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
			LOG.info("About to start server on port: " + defaultPort);
			HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer
					.bootstrapFromFile("./littleproxy.properties")
					.withPort(defaultPort)
					.withAllowLocalOnly(false)
					.withThreadPoolConfiguration(config)
					.withThrottling(getThrottleReadStream(), getThrottleWriteStream())
					;
			
			LOG.info("About to start...");
			proxyServer = bootstrap.start();

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
 		}

	}
	
	public void stop() {
		if(proxyServer!=null) {
			proxyServer.stop();
		}
	}
	
	

}
