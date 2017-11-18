/*
 *  Copyright 2017 AT&T
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
package com.att.aro.analytics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPPostMethod {
    private static final String POST_METHOD_NAME = "POST";
    private static String applicationName = "ARO";
    private static String userAgentInfo = null;

    public HTTPPostMethod(){

    }

    public boolean request(String urlString) {
        boolean gaRequestStatus = false;
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = openURLConnection(url);
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setRequestMethod(POST_METHOD_NAME);
            urlConnection.setRequestProperty("User-Agent", getUserAgentInfo());

            urlConnection.connect();
            int responseCode = getResponseCode(urlConnection);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("ERROR ");
            } else {
                gaRequestStatus = true;
            }
            urlConnection.disconnect();
        } catch (Exception e) {
            //ignoring the error condition
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

    public String getUserAgentInfo() {
        if(userAgentInfo == null){
            String uaName = "Java/" + System.getProperty("java.version"); // java version info appended
            // os string is architecture+osname+version concatenated with _
            String osString = System.getProperty("os.arch");
            if (osString == null || osString.length() < 1) {
                osString = "";
            } else {
                osString += "; ";
                osString += System.getProperty("os.name") + " "
                        + System.getProperty("os.version");
            }
            userAgentInfo = applicationName + " (" +uaName + " ,"
                    + osString + ")";
        }
        return userAgentInfo;
    }

    /**
     *
     * @param userAgentInfo
     */
    public void setUserAgentInfo(String userAgentInfo) {
        HTTPPostMethod.userAgentInfo = userAgentInfo;
    }

    /**
     *
     * @param applicationName
     */
    public static void setApplicationName(String applicationName) {
        HTTPPostMethod.applicationName = applicationName;
    }


}
