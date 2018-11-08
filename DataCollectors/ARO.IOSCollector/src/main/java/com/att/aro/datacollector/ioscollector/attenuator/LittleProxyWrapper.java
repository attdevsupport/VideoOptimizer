package com.att.aro.datacollector.ioscollector.attenuator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.littleshoot.proxy.impl.ProxyUtils;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;

import com.att.aro.core.util.Util;
import com.google.common.primitives.Bytes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

public class LittleProxyWrapper implements Runnable {
	
	private static final Logger LOG = LogManager.getLogger(LittleProxyWrapper.class.getName());
    private HttpProxyServer proxyServer;

	private int defaultPort = 8080;
	private int throttleReadStream = -1;
	private int throttleWriteStream = -1;
	private  HttpFiltersSource filtersSource;
	private boolean secureEnable = false;
    public boolean isSecureEnable() {
		return secureEnable;
	}

	public void setSecureEnable(boolean secureEnable) {
		this.secureEnable = secureEnable;
	}


	private static final AttributeKey<String> CONNECTED_URL = AttributeKey.valueOf("connected_url");

  	private String TRACE_FILE_PATH =   "";

 	
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
		LOG.info("About to start...");

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
					.withThrottling(getThrottleReadStream(), getThrottleWriteStream());
			if(isSecureEnable()) {
				filterConfig();
				bootstrap.withFiltersSource(filtersSource);
				bootstrap.withManInTheMiddle(new CertificateSniffingMitmManager(getVOAuthority()));
			}
			proxyServer = bootstrap.start();	        
	        
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
 		}

	}

	private void filterConfig() {
		filtersSource = new HttpFiltersSourceAdapter() {
		    @Override
		    public HttpFilters filterRequest(HttpRequest originalRequest,
		            ChannelHandlerContext clientCtx) {
					return new HttpFiltersAdapter(originalRequest, clientCtx) {
					
		            @Override
					public HttpObject proxyToClientResponse(HttpObject httpObject) {

		                	String sessionIndex = "";
						String uri = originalRequest.getUri();
			            if (originalRequest.getMethod() == HttpMethod.CONNECT) {
			                    if (clientCtx != null) {
			                        String prefix = uri.replaceFirst(":443$", "");
				        				LOG.info("prefix: " + prefix);
				                    clientCtx.channel().attr(CONNECTED_URL).set(prefix);				                  
			                    }
			                }	                
			                String connectedUrl = clientCtx.channel().attr(CONNECTED_URL).get();			                
			                if(connectedUrl!=null) {
			                		LOG.info("connectedUrl: " + connectedUrl);
			                		String portNum = ctx.channel().remoteAddress().toString();
			                		int portSeparator = portNum.lastIndexOf(":");
			                		portNum = portNum.substring(portSeparator+1);
			                		sessionIndex = connectedUrl+ "_"+portNum+"_DL";
			                		LOG.info("sessionIndex: " + sessionIndex);

			                }else {
			        				LOG.info("uri: " + uri);
			                }
 
		            		String filepath = getTRACE_FILE_PATH() + Util.FILE_SEPARATOR +"iosSecure" + Util.FILE_SEPARATOR ;
		            		File f = new File(filepath);
		            	      if(!f.isDirectory()) {
		            	          f.getParentFile().mkdirs();
		            	      } 
		                		File tempFile = new File(filepath + sessionIndex);
		                		if(tempFile.exists()) {
		                			LOG.info("already here: " +  tempFile.getAbsolutePath());
		                		}else {
		                			//create and open
		                			try {
										tempFile.createNewFile();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
		                		}
								LOG.info("proxyToClientResponse - > : localAddress() : " + ctx.channel().localAddress() + " ==== remoteAddress: " +ctx.channel().remoteAddress());
								if (httpObject instanceof HttpContent) {
									HttpContent response = (HttpContent) httpObject;
									 LOG.info("proxyToClientResponse " +" --- Content Length: " + response.content().capacity());

							}
		                		cloneExtraContent(tempFile, sessionIndex, httpObject);

						return super.proxyToClientResponse(httpObject);
					}


					@Override
		            public HttpResponse clientToProxyRequest(
		                    HttpObject httpObject) {

						if (httpObject instanceof HttpRequest) {
							HttpRequest request = (HttpRequest) httpObject;
							LOG.info("clientToProxyRequest " +" --- Uri: " + request.getUri()
									 + " -- methods: "+ request.getMethod().name()
									 +"-- Protocal Name: "+request.getProtocolVersion().protocolName()
									 +"\n");
							
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
		                String connectedUrl = clientCtx.channel().attr(CONNECTED_URL).get();			                
		                if(connectedUrl!=null) {
		                		LOG.info("connectedUrl: " + connectedUrl);
		                		String portNum = ctx.channel().remoteAddress().toString();
		                		int portSeparator = portNum.lastIndexOf(":");
		                		portNum = portNum.substring(portSeparator+1);
		                		sessionIndex = connectedUrl + "_" +portNum+"_UL";
		                		LOG.info("sessionIndex: " + sessionIndex);

		                }else {
		        				LOG.info("uri: " + uri);
		                }
 
		        		String filepath = getTRACE_FILE_PATH() + Util.FILE_SEPARATOR +"iosSecure";
		      	      if(!new File(filepath).mkdirs()) {
		      			LOG.info("create folder failed!" );
		      	      } 
		        		File tempFile = new File(filepath + Util.FILE_SEPARATOR + sessionIndex);
		        		if(tempFile.exists()) {
		        			LOG.info("already here: " +  tempFile.getAbsolutePath());
		        		}else {
		        			//create and open
		        			try {
								tempFile.createNewFile();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		        		}
		    			cloneExtraContent(tempFile, sessionIndex, httpObject);

		        		return super.clientToProxyRequest(httpObject);//return null
		            }		
		       };                  	            
		    };
		   
		};
	}
	
	private Authority getVOAuthority() {
		return new Authority(
		new File( Util.getVideoOptimizerLibrary()),
        "VO", // proxy id
        "password1".toCharArray(),
        "Video Optimizer",
        "Video Optimizer",
        "Certificate Authority",
        "Video Optimizer - mitm",
		"Video Optimizer - mitm - for iOS secure collector use only"
		);
	}

	public void stop() {
	
		if(proxyServer!=null) {
			LOG.info("stop little proxy");
			proxyServer.stop();
		}
/*		 
		if(oriReqReplist!=null) {
			LOG.info("oriReqReplist size: " + oriReqReplist.size() );
			int countRequest = 0;
			int countReponse = 0;
			int countContent = 0;
		
			for (Map.Entry<String, List<HttpObject>> entry : oriReqReplist.entrySet()) {

				String req = entry.getKey();
				LOG.info("**********************originalRequest*********start***********************");
				LOG.info("originalRequest:  " +  "\nFROM: " + req);
				StringBuilder sbuilder = new StringBuilder();
				for (HttpObject httpObject : entry.getValue()) {
					if (httpObject instanceof HttpRequest) {
						 HttpRequest request = (HttpRequest) httpObject;
 							 sbuilder.append("clientToProxyRequest - to " +"\nUri: " + request.getUri()
							 + "\nmethods: "+ request.getMethod().name()
							 +"\nProtocal Name: "+request.getProtocolVersion().protocolName()
							 +"\n");
						 
						countRequest++;

					} else if (httpObject instanceof HttpContent) {
						HttpContent hcontent = (HttpContent) httpObject;
						// responsePreBody.append(((HttpContent) httpObject)
						// .content().toString(Charset.forName("UTF-8")));
						 sbuilder.append("httpcontent: "+ hcontent.content().capacity()+"\n");
						countContent++;
					} else if (httpObject instanceof HttpResponse) {
						HttpResponse response = (HttpResponse) httpObject;						 
						if (!response.headers().isEmpty()) {
							StringBuilder sbuilderTemp = new StringBuilder();
							for (CharSequence name : response.headers().names()) {
								for (CharSequence value : response.headers().getAll(name)) {
									sbuilderTemp.append(name + " = " + value + "\n");
								}
							}
							sbuilder.append("proxyToClientResponse - to -> "
									// + httpObject.getMethod().name()
									// +"\nFROM: "+ httpObject.getUri()
									+ "\nSTATUS: " + response.getStatus() + "\nVERSION: "
									+ response.getProtocolVersion() + "\nRreponse: \nHEADER: " + sbuilderTemp.toString()+"\n");
						}					 
						countReponse++;
					} else {
						LOG.info(" others :");
					}
				}
				LOG.info("\n"+ sbuilder.toString()+"\n");
				LOG.info("****************************originalRequest************end***********************");
			}
			LOG.info("finish the loop: " + "countReponse: " + countReponse + " countRequest: " + countRequest
					+ " countContent: + " + countContent);

		}*/

	}	
 
	
	private void cloneExtraContent(File tempFile,String method, HttpObject httpObject) {

		byte[] buffer ;
		try {
			if (httpObject instanceof HttpMessage) {
				StringBuilder sbuilderTemp = new StringBuilder();
				for (CharSequence name : ((HttpMessage)httpObject).headers().names()) {
					for (CharSequence value : ((HttpMessage)httpObject).headers().getAll(name)) {
						sbuilderTemp.append(name + " = " + value + "\n");
					}
				}
				buffer = sbuilderTemp.toString().getBytes();
			}else {
			    HttpContent httpContent = (HttpContent) httpObject;
			    ByteBuf buf = httpContent.content();
			    buffer = new byte[buf.readableBytes()];
			    if(buf.readableBytes() > 0) {
			        int readerIndex = buf.readerIndex();
			        buf.getBytes(readerIndex, buffer);
			    }
			}
     		LOG.info("start writing file: "+ tempFile.getAbsolutePath());
     		FileOutputStream fos = new FileOutputStream(tempFile,true);
     		fos.write(buffer);		
    			LOG.info("stop writing file");
    			fos.close();
    		} catch (IOException e) {
    			LOG.info("IOExcption : "+ e);
    		} catch (Exception ex) {
			
		}
}


}
