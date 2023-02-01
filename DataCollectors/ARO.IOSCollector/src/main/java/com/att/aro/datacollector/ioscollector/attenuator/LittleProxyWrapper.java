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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;

import com.att.aro.core.commandline.IExternalProcessRunner;
import com.att.aro.core.commandline.impl.ExternalProcessRunnerImpl;
import com.att.aro.core.datacollector.pojo.StatusResult;
import com.att.aro.core.util.Util;
import com.att.aro.datacollector.ioscollector.reader.ExternalDumpcapExecutor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

public class LittleProxyWrapper implements Runnable {

	private static final Logger LOG = LogManager.getLogger(LittleProxyWrapper.class.getName());

    private final Map<String, Integer> payloadFileMap = new HashMap<>();

	private HttpProxyServer proxyServer;
	
	private String sudoPassword;

	private int defaultPort = 8080;
	private int throttleReadStream = -1;
	private int throttleWriteStream = -1;
	private HttpFiltersSource filtersSource;
	private boolean secureEnable = false;
	
	private ExternalDumpcapExecutor dumpcapExecutor;
	private IExternalProcessRunner extRunner = new ExternalProcessRunnerImpl();
	
	public LittleProxyWrapper(StatusResult status, String sudoPassword, String trafficFilePath) {
		this.sudoPassword = sudoPassword;
		this.trafficFilePath = trafficFilePath;
	}

	public boolean isSecureEnable() {
		return secureEnable;
	}

	public void setSecureEnable(boolean secureEnable) {
		this.secureEnable = secureEnable;
	}

	private static final AttributeKey<String> CONNECTED_URL = AttributeKey.valueOf("connected_url");

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
			if (isSecureEnable()) {
				filterConfig();
				bootstrap.withFiltersSource(filtersSource);
				bootstrap.withManInTheMiddle(new CertificateSniffingMitmManager(getVOAuthority()));
			}
			
			dumpcapExecutor = new ExternalDumpcapExecutor(trafficFilePath, sudoPassword, "bridge100", extRunner);
			dumpcapExecutor.start();
			LOG.info("************  Tcpdump started in background. ****************");
			
			proxyServer = bootstrap.start();

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

	}

	private void filterConfig() {
		filtersSource = new HttpFiltersSourceAdapter() {
			@Override
			public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext clientCtx) {
				return new HttpFiltersAdapter(originalRequest, clientCtx) {

					@Override
					public HttpObject proxyToClientResponse(HttpObject httpObject) {
					    processRequest(originalRequest, clientCtx, httpObject, "DL");
						return super.proxyToClientResponse(httpObject);
					}

					@Override
					public HttpResponse clientToProxyRequest(HttpObject httpObject) {
					    processRequest(originalRequest, clientCtx, httpObject, "UL");
						return super.clientToProxyRequest(httpObject);
					}
				};
			};

		};
	}

	private void processRequest(HttpRequest originalRequest, ChannelHandlerContext clientCtx, HttpObject httpObject, String fileSuffix) {
	   if (httpObject instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) httpObject;
            LOG.info("clientToProxyRequest " + " --- Uri: " + request.getUri() + " -- methods: "
                    + request.getMethod().name() + "-- Protocal Name: "
                    + request.getProtocolVersion().protocolName() + "\n");

        }

	    String sessionIndex = "";
        String uri = originalRequest.getUri();
        if (originalRequest.getMethod() == HttpMethod.CONNECT) {
            if (clientCtx != null) {
                String prefix = uri.replaceFirst(":443$", "");
                LOG.info("prefix: " + prefix);
                clientCtx.channel().attr(CONNECTED_URL).set(prefix);
            }
        }

        String connectedUrl = clientCtx == null ? null : clientCtx.channel().attr(CONNECTED_URL).get();
        if (connectedUrl != null) {
            LOG.info("connectedUrl: " + connectedUrl);
            String portNum = clientCtx.channel().remoteAddress().toString();
            int portSeparator = portNum.lastIndexOf(":");
            portNum = portNum.substring(portSeparator + 1);
            sessionIndex = connectedUrl + "_" + portNum + "_" + fileSuffix;
            LOG.info("sessionIndex: " + sessionIndex);
        } else {
            LOG.info("uri: " + uri);
        }

        String filepath = getTraceFolder() + Util.FILE_SEPARATOR + "iosSecure" + Util.FILE_SEPARATOR;
        File f = new File(filepath);
        if (!f.isDirectory()) {
            if (!f.mkdirs()) {
                LOG.info("creating directories for path '"+ filepath +"' failed!");
            }
        }

        File tempFile = new File(filepath + sessionIndex);
        if (tempFile.exists()) {
            LOG.info("already here: " + tempFile.getAbsolutePath());

            if (!(httpObject instanceof HttpContent)) {
                payloadFileMap.put(sessionIndex, payloadFileMap.get(sessionIndex) + 1);
            }
        } else {
            // create and open
            try {
                tempFile.createNewFile();
                payloadFileMap.put(sessionIndex, 1);
            } catch (IOException e) {
                LOG.error("Something went wrong while creating file " + tempFile.getAbsolutePath(), e);
            }
        }

        if (httpObject instanceof HttpContent) {
            HttpContent response = (HttpContent) httpObject;
            LOG.info(
                    "proxyToClientResponse " + " --- Content Length: " + response.content().capacity());

        }

        cloneExtraContent(tempFile, sessionIndex, httpObject);
	}

	private Authority getVOAuthority() {
		return new Authority(new File(Util.getVideoOptimizerLibrary()), "VO", // proxy id
				"password1".toCharArray(), "Video Optimizer", "Video Optimizer", "Certificate Authority",
				"Video Optimizer - mitm", "Video Optimizer - mitm - for iOS secure collector use only");
	}

	public void stop() {
		if (proxyServer != null) {
			LOG.info("stop little proxy");
			proxyServer.stop();
			dumpcapExecutor.stopTshark();
		}
	}

	private void cloneExtraContent(File fileObj, String fileName, HttpObject httpObject) {

		byte[] buffer = null;
		StringBuilder sbuilderTemp = new StringBuilder();

		try {
			long timeInMillis = System.currentTimeMillis();

			if (httpObject instanceof HttpRequest || httpObject instanceof HttpResponse) {
			    HttpRequest request = httpObject instanceof HttpRequest ? (HttpRequest) httpObject : null;
			    HttpResponse response = httpObject instanceof HttpResponse ? (HttpResponse) httpObject : null;

			    sbuilderTemp.append("RequestTime: " + timeInMillis + "\r\n");
			    if (request != null) {
			        sbuilderTemp.append(request.getMethod().name() + " " + request.getUri() + " "
	                        + request.getProtocolVersion().toString() + "\r\n");
			    } else {
			        sbuilderTemp.append(response.getProtocolVersion()).append(" ").append(response.getStatus()).append("\r\n");
			    }

			    HttpMessage message = (HttpMessage) httpObject;
			    for (CharSequence name : message.headers().names()) {
                    for (CharSequence value : message.headers().getAll(name)) {
                        sbuilderTemp.append(name + ":" + value + "\r\n");
                    }
                }

			    sbuilderTemp.append("\r\n");
			    buffer = sbuilderTemp.toString().getBytes();
			} else {
			    HttpContent httpContent = (HttpContent) httpObject;
			    ByteBuf buf = httpContent.content();
			    int readableBytes = buf.readableBytes();
			    if (readableBytes <= 0) {
			        return;
			    }

                buffer = new byte[readableBytes];
                int readerIndex = buf.readerIndex();
                buf.getBytes(readerIndex, buffer);

                // Reset file path to write to payload file
                //
                // As we have all requests (or responses) in a single file,
                // the payload number suffix for the new file will help us relating a specific payload to its corresponding request/response while reading data.
                Integer payloadFileNumber = payloadFileMap.get(fileName);
                fileObj = new File(fileObj.getAbsoluteFile() + "_" + payloadFileNumber.toString());
			}

			writeToFile(fileObj, buffer);
		} catch (Exception e) {
			LOG.info("Exception : ", e);
		}
	}

	private void writeToFile(File file, byte[] data) throws IOException {
	    if (data != null) {
    	    LOG.info("start writing file: " + file.getAbsolutePath());
    	    FileOutputStream fos = null;
    
    	    try {
                fos = new FileOutputStream(file, true);
    	        fos.write(data);
    	    } finally {
    	        if (fos != null) {
    	            fos.close();
    	        }
    	    }
    
            LOG.info("stop writing file");
	    }
	}

	public void setTrafficFile(String trafficFilePath) {
		this.trafficFilePath = trafficFilePath;
		
	}
}
