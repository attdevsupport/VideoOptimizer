/**
 * 
 */
package com.att.aro.analytics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import com.btr.proxy.search.ProxySearch;

/**
 *
 *
 */
public class NetworkConnectionChecker implements Runnable{
	
	private String site = "http://www.google-analytics.com";
	private HTTPGetMethod httpGetMethod;
	//private ExecutorService executor;
	
	public NetworkConnectionChecker(HTTPGetMethod getMethod){
		this.httpGetMethod = getMethod;
	}
	
	
	public void run(){
		
			// Check for the internet Connection.. Set the flag
			boolean inetAvailabilityFlag = false;
			
			try {
				
				ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
					
				proxySearch.setPacCacheSettings(32, 3000); //Cache 32 urls upto 7 sec
				
				ProxySelector myProxySelector = proxySearch.getProxySelector();  // Return Null if no proxy available
				String host = null;
	            int port = 0;
				//ProxySelector.setDefault(myProxySelector);
				if(myProxySelector != null){
			        //Geeting the default Proxy
		            URI home = URI.create(site); 
		            List<Proxy> proxyList = myProxySelector.select(home);
		            
		            if (proxyList != null && !proxyList.isEmpty()) { 
		                  int count = 0;
			            for (Proxy proxy : proxyList) { 
		 
			               SocketAddress address = proxy.address(); 
			               if (address instanceof InetSocketAddress) { 
			            	   host = ((InetSocketAddress) address).getHostName(); 
			                 //port = Integer.toString(((InetSocketAddress) address).getPort());
			            	   port= ((InetSocketAddress) address).getPort();
			                 //System.setProperty("http.proxyHost", host); 
			                 //System.setProperty("http.proxyPort", port);
			               } 
			               count++;
			               if(!proxy.toString().contains("DIRECT")){
			                     break;
			               }
			            } 
		            }
				}
	            boolean proxyFlag = false;
	            if(host != null && port != 0){
	            	proxyFlag = true;
	            } 
	            
	            Proxy proxy = null;
	            if(proxyFlag){
	            	InetSocketAddress proxyInet = new InetSocketAddress(host,port);
	            	proxy = new Proxy(Proxy.Type.HTTP, proxyInet);
	            	httpGetMethod.setProxyObj(proxy);
	            	httpGetMethod.setIsProxy(proxyFlag);
	            } 
	            
                //make a URL to a known source
                URL url = new URL(site);

                //open a connection to that source
                HttpURLConnection urlConnect;
                if(proxyFlag){
                	urlConnect = (HttpURLConnection)url.openConnection(proxy);
                } else {
                	urlConnect = (HttpURLConnection)url.openConnection();
                }

                //trying to retrieve data from the source. If there
                //is no connection, this line will fail
                urlConnect.setConnectTimeout(3000);
                urlConnect.connect();
                inetAvailabilityFlag = true;

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            	
            } catch (IOException e) {

            	// TODO Auto-generated catch block
                //e.printStackTrace();
                //return false;
            	
            } catch(Exception ex){
            	try{ //Some times thowing null pointer exception if no proxy is available that is why checking for internet connection again.
            		URL url = new URL(site);
            		HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();
            		urlConnect.setConnectTimeout(1000);
                    urlConnect.connect();
                    inetAvailabilityFlag = true;
                    
            	} catch(Exception exe){
            		
            	
            	}
            	
            }

			httpGetMethod.setIsValidIConnection(inetAvailabilityFlag); //Add the flag to the request
			
	}
	

}
