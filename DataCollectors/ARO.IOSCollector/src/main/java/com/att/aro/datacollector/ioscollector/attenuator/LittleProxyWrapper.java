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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.datacollector.ioscollector.reader.ExternalDumpcapExecutor;

public class LittleProxyWrapper implements Runnable {

	private static final Logger LOG = LogManager.getLogger(LittleProxyWrapper.class.getName());


	private HttpProxyServer proxyServer;
	
	private String sudoPassword;
	private StatusResult status;

	private int defaultPort = 8080;
	private int throttleReadStream = -1;
	private int throttleWriteStream = -1;
	
	private ExternalDumpcapExecutor dumpcapExecutor;
	private IExternalProcessRunner extRunner = new ExternalProcessRunnerImpl();
	
	public LittleProxyWrapper(StatusResult status, String sudoPassword, String trafficFilePath) {
		this.status = status;
		this.sudoPassword = sudoPassword;
		this.trafficFilePath = trafficFilePath;
	}


	private String traceFolder = "";

	private String trafficFilePath;

	public String getTraceFolder() {
		return traceFolder;
	}

	public void setTraceFolder(String traceFolder) {
		this.traceFolder = traceFolder;
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
			
			dumpcapExecutor = new ExternalDumpcapExecutor(trafficFilePath, sudoPassword, "bridge100", extRunner);
			dumpcapExecutor.start();
			LOG.info("************  Tcpdump started in background. ****************");
			
			proxyServer = bootstrap.start();

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

	}


	public void stop() {
		if (proxyServer != null) {
			LOG.info("stop little proxy");
			proxyServer.stop();
			dumpcapExecutor.stopTshark();
		}
	}

	public void setTrafficFile(String trafficFilePath) {
		this.trafficFilePath = trafficFilePath;
		
	}
}
