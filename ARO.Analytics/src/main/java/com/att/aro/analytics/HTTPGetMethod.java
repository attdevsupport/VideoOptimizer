package com.att.aro.analytics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Simple class peforming HTTP Get method on the requested url
 *
 * Created by Harikrishna Yaramachu on 3/18/14.
 */
public class HTTPGetMethod {
	private static final Logger LOGGER = Logger.getLogger(HTTPGetMethod.class.getName());
    private static final String GET_METHOD_NAME = "GET";

    private static final String SUCCESS_MESSAGE = "Tracking Successful!";

    private static String uaName = null; // User Agent name
    private static String applicationName = "ARO";

    private static String osString = "Unknown";
    private static String userAgentInfo = null;
    
    private static boolean isValidIConnection = false;
    private static boolean isProxy = false;
    private static Proxy proxyObj;

    public HTTPGetMethod() {

    }

    /**
     *
     * @param urlString
     */
    public boolean request(String urlString) {
        boolean gaRequestStatus = false;
        try {
 
        	URL url = new URL(urlString);
            HttpURLConnection urlConnection;
            if(isProxy){
            	urlConnection = openURLConnection(url, proxyObj);
            } else {
            	urlConnection = openURLConnection(url);
            }
            
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setConnectTimeout(3000);
            urlConnection.setRequestMethod(GET_METHOD_NAME);
            urlConnection.setRequestProperty("User-agent", getUserAgentInfo());
//            LOGGER.info("User Agent : " + getUserAgentInfo());
            urlConnection.connect();
            int responseCode = getResponseCode(urlConnection);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                //logError("JGoogleAnalytics: Error tracking, url=" + urlString);
//            	LOGGER.info("ERROR ");
            } else {
                //logMessage(SUCCESS_MESSAGE);
//            	LOGGER.info(SUCCESS_MESSAGE + "Response Code : "+responseCode);
                gaRequestStatus = true;
            }
            urlConnection.disconnect();
        } catch (Exception e) {
            //logError(e.getMessage());
        }
        return gaRequestStatus;
    }

    /**
     *
     * @param urlConnection
     * @return
     * @throws IOException
     */
    protected int getResponseCode(HttpURLConnection urlConnection)
            throws IOException {
        return urlConnection.getResponseCode();
    }

    /**
     *
     * @param url
     * @return
     * @throws IOException
     */
    private HttpURLConnection openURLConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }
    
    private HttpURLConnection openURLConnection(URL url, Proxy proxy) throws IOException {
        return (HttpURLConnection) url.openConnection(proxy);
    }

    public String getUserAgentInfo() {
        if(userAgentInfo == null){
            String uaName = "Java"+ "/" + System.getProperty("java.version"); // java version info appended
            // os string is architecture+osname+version concatenated with _
            String osString = System.getProperty("os.arch");
            if (osString == null || osString.length() < 1) {
                osString = "";
            } else {
            	String osName = System.getProperty("os.name");
            	if(osName.startsWith("Windows")){
            		String osTemp = osString;
            		osString = osName.substring(0, osName.indexOf(" ")) + ";" 
            				+ osName.substring(osName.indexOf(" ")).trim() + ";"
	                        + System.getProperty("os.version") + ";"
	                        + osTemp; 
           
            	} else{
	                osString += "; ";
	                osString += osName + " "
	                        + System.getProperty("os.version");
            	}
            }
            userAgentInfo = applicationName + " (" +osString + "; en-US) "
    				+ uaName;
            
        }
        return userAgentInfo;
    }

    public static void setApplicationName(String applicationName) {
        HTTPGetMethod.applicationName = applicationName;
    }
    
    public synchronized boolean isValidIConnection(){
    	return isValidIConnection;
    }
    
    public synchronized void setIsValidIConnection(boolean inetFlag){
    	this.isValidIConnection = inetFlag;
    }
    
    public synchronized void setIsProxy(boolean proxyFlag){
    	this.isProxy = proxyFlag;
    }
    
    public synchronized void setProxyObj(Proxy proxyObject){
    	this.proxyObj = proxyObject;
    }
}
