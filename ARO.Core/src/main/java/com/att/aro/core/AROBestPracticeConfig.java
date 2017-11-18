/*
 *  Copyright 2014 AT&T
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
package com.att.aro.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.att.aro.core.bestpractice.IBestPractice;
import com.att.aro.core.bestpractice.impl.AccessingPeripheralImpl;
import com.att.aro.core.bestpractice.impl.AsyncCheckInScriptImpl;
import com.att.aro.core.bestpractice.impl.CacheControlImpl;
import com.att.aro.core.bestpractice.impl.CombineCsJssImpl;
import com.att.aro.core.bestpractice.impl.ConnectionClosingImpl;
import com.att.aro.core.bestpractice.impl.ConnectionOpeningImpl;
import com.att.aro.core.bestpractice.impl.DisplayNoneInCSSImpl;
import com.att.aro.core.bestpractice.impl.DuplicateContentImpl;
import com.att.aro.core.bestpractice.impl.EmptyUrlImpl;
import com.att.aro.core.bestpractice.impl.FileCompressionImpl;
import com.att.aro.core.bestpractice.impl.FileOrderImpl;
import com.att.aro.core.bestpractice.impl.FlashImpl;
import com.att.aro.core.bestpractice.impl.ForwardSecrecyImpl;
import com.att.aro.core.bestpractice.impl.Http10UsageImpl;
import com.att.aro.core.bestpractice.impl.Http3xxCodeImpl;
import com.att.aro.core.bestpractice.impl.Http4xx5xxImpl;
import com.att.aro.core.bestpractice.impl.HttpsUsageImpl;
import com.att.aro.core.bestpractice.impl.ImageCompressionImpl;
import com.att.aro.core.bestpractice.impl.ImageFormatImpl;
import com.att.aro.core.bestpractice.impl.ImageMetaDataImpl;
//import com.att.aro.core.bestpractice.impl.HttpsUsageImpl;
import com.att.aro.core.bestpractice.impl.ImageSizeImpl;
import com.att.aro.core.bestpractice.impl.ImageUIComparatorImpl;
import com.att.aro.core.bestpractice.impl.MinificationImpl;
import com.att.aro.core.bestpractice.impl.MultipleSimultnsConnImpl;
import com.att.aro.core.bestpractice.impl.PeriodicTransferImpl;
import com.att.aro.core.bestpractice.impl.PrefetchingImpl;
import com.att.aro.core.bestpractice.impl.ScreenRotationImpl;
import com.att.aro.core.bestpractice.impl.ScriptsImpl;
import com.att.aro.core.bestpractice.impl.SimultnsConnImpl;
import com.att.aro.core.bestpractice.impl.SpriteImageImpl;
import com.att.aro.core.bestpractice.impl.TransmissionPrivateDataImpl;
//import com.att.aro.core.bestpractice.impl.TransmissionPersonalImpl;
import com.att.aro.core.bestpractice.impl.UnnecessaryConnectionImpl;
import com.att.aro.core.bestpractice.impl.UnsecureSSLVersionImpl;
import com.att.aro.core.bestpractice.impl.UsingCacheImpl;
import com.att.aro.core.bestpractice.impl.VideoBufferOccupancyImpl;
import com.att.aro.core.bestpractice.impl.VideoChunkPacingImpl;
import com.att.aro.core.bestpractice.impl.VideoChunkSizeImpl;
import com.att.aro.core.bestpractice.impl.VideoConcurrentSessionImpl;
import com.att.aro.core.bestpractice.impl.VideoNetworkComparisonImpl;
import com.att.aro.core.bestpractice.impl.VideoRedundancyImpl;
import com.att.aro.core.bestpractice.impl.VideoStallImpl;
import com.att.aro.core.bestpractice.impl.VideoStartUpDelayImpl;
import com.att.aro.core.bestpractice.impl.VideoTcpConnectionImpl;
import com.att.aro.core.bestpractice.impl.WeakCipherImpl;
import com.att.aro.core.bestpractice.impl.WiFiOffloadingImpl;

/**
 * Spring configuration for all best practices.
 * <pre>
 * BestPractice Beans:
 *   * accessingPeripheral  
 *   * async                
 *   * cacheControl         
 *   * combineCsJss         
 *   * connectionClosing    
 *   * connectionOpening    
 *   * displaynoneincss       Checks for display:none in CSS embedded in HTML
 *   * duplicateContent     
 *   * emptyUrl             
 *   * fileorder            
 *   * flash                
 *   * http10Usage          
 *   * http3xx              
 *   * http4xx5xx           
 *   * imageSize            
 *   * iperiodicTransfer     
 *   * minify               
 *   * prefetching          
 *   * screenRotation       
 *   * scripts              
 *   * spriteImage          
 *   * textFileCompression  
 *   * unnecessaryConnection
 *   * usingCache           
 *   * wifiOffloading
 *   * httpsUsage
 *   * transmissionPrivateData
 *   * unsecureSSLVersion    
 *   * weakCipher
 *   * forward secrecy
 * </pre>
 * 
 * Date: November 12, 2014
 */
@Configuration
@Lazy
@ComponentScan("com.att.aro.core.bestpractice")
public class AROBestPracticeConfig {
	
	static final String VIDEOUSAGE = "videoUsage";

	//bad performance => need redesign
	@Bean(name = "periodicTransfer")
	IBestPractice getPeriodicTransfer() {
		return new PeriodicTransferImpl();
	}

	@Bean(name = "unnecessaryConnection")
	IBestPractice getUnnecessaryConnection() {
		return new UnnecessaryConnectionImpl();
	}

	@Bean(name = "connectionOpening")
	IBestPractice getConnectionOpening() {
		return new ConnectionOpeningImpl();
	}

	@Bean(name = "connectionClosing")
	IBestPractice getConnectionClosing() {
		return new ConnectionClosingImpl();
	}

	@Bean(name = "wifiOffloading")
	IBestPractice getWiFiOffloading() {
		return new WiFiOffloadingImpl();
	}

	@Bean(name = "screenRotation")
	IBestPractice getScreenRotation() {
		return new ScreenRotationImpl();
	}

	@Bean(name = "prefetching")
	IBestPractice getPrefetching() {
		return new PrefetchingImpl();
	}

	@Bean(name = "accessingPeripheral")
	IBestPractice getAccessingPeripheral() {
		return new AccessingPeripheralImpl();
	}

	@Bean(name = "combineCsJss")
	IBestPractice getCombineCsJss() {
		return new CombineCsJssImpl();
	}

	@Bean(name = "http10Usage")
	IBestPractice getHttp10Usage() {
		return new Http10UsageImpl();
	}

	@Bean(name = "cacheControl")
	IBestPractice getCacheControl() {
		return new CacheControlImpl();
	}

	@Bean(name = "usingCache")
	IBestPractice getUsingCache() {
		return new UsingCacheImpl();
	}

	@Bean(name = "duplicateContent")
	IBestPractice getDuplicateContent() {
		return new DuplicateContentImpl();
	}

	@Bean(name = "http4xx5xx")
	IBestPractice getHttp4xx5xx() {
		return new Http4xx5xxImpl();
	}
	
	@Bean(name = "simultaneous")
	IBestPractice getSimultaneous() {
		return new SimultnsConnImpl();
	}

	@Bean(name = "multipleSimultaneous")
	IBestPractice getMultipleSimultaneous() {
		return new MultipleSimultnsConnImpl();
	}

	@Bean(name = "http3xx")
	IBestPractice getHttp3xx() {
		return new Http3xxCodeImpl();
	}

	@Bean(name = "textFileCompression")
	IBestPractice getTextFileCompression() {
		return new FileCompressionImpl();
	}

	@Bean(name = "imageSize")
	IBestPractice getImageSize() {
		return new ImageSizeImpl();
	}
	
	@Bean(name = "imageCompression")
	IBestPractice getImageCompression() {
		return new ImageCompressionImpl();
	}
	@Bean(name = "imageMetadata")
	IBestPractice getImageMdata() {
		return new ImageMetaDataImpl();
	}
	@Bean(name = "imageFormat")
	IBestPractice getImageFormat() {
		return new ImageFormatImpl();
	}
	
	@Bean(name = "uiComparator")
	IBestPractice getUIComparator() {
		return new ImageUIComparatorImpl();
	}
	
	@Bean(name = "minify")
	IBestPractice getMinify() {
		return new MinificationImpl();
	}

	@Bean(name = "emptyUrl")
	IBestPractice getEmptyUrl() {
		return new EmptyUrlImpl();
	}

	@Bean(name = "flash")
	IBestPractice getFlash() {
		return new FlashImpl();
	}

	@Bean(name = "spriteImage")
	IBestPractice getSpriteImage() {
		return new SpriteImageImpl();
	}

	@Bean(name = "scripts")
	IBestPractice getScripts() {
		return new ScriptsImpl();
	}

	@Bean(name = "async")
	IBestPractice getAsync() {
		return new AsyncCheckInScriptImpl();
	}

	/**
	 * Checks for display:none in CSS embedded in HTML
	 * @return new DisplayNoneInCSSImpl()
	 */
	@Bean(name = "displaynoneincss")
	IBestPractice getDisplayNoneInCSS() {
		return new DisplayNoneInCSSImpl();
	}

	@Bean(name = "fileorder")
	IBestPractice getFileOrder() {
		return new FileOrderImpl();
	}

	// VIDEO
	@Bean(name = "videoStall")
	IBestPractice getVideoStall() {
		return new VideoStallImpl();
	}
	
	@Bean(name = "startupDelay")
	IBestPractice getStartupDelay() {
		return new VideoStartUpDelayImpl();
	}

	@Bean(name = "bufferOccupancy")
	IBestPractice getBufferOccupancy() {
		return new VideoBufferOccupancyImpl();
	}

	@Bean(name = "networkComparison")
	IBestPractice getNetworkComparison() {
		return new VideoNetworkComparisonImpl();
	}

	@Bean(name = "tcpConnection")
	IBestPractice getTcpConnection() {
		return new VideoTcpConnectionImpl();
	}

	@Bean(name = "chunkSize")
	IBestPractice getChunkSize() {
		return new VideoChunkSizeImpl();
	}

	@Bean(name = "chunkPacing")
	IBestPractice getChunkPacing() {
		return new VideoChunkPacingImpl();
	}

	@Bean(name = "videoRedundancy")
	IBestPractice getVideoRedundancy() {
		return new VideoRedundancyImpl();
	}
	
	@Bean(name = "videoConcurrentSession")
	IBestPractice getVideoConcurrentSession() {
		return new VideoConcurrentSessionImpl();
	}
	//End of Video

	@Bean(name = "httpsUsage")
	IBestPractice getHttpsUsage() {
		return new HttpsUsageImpl();
	}

	@Bean(name = "transmissionPrivateData")
	IBestPractice getTransmissionPrivateData() {
		return new TransmissionPrivateDataImpl();
	}
	
	@Bean(name = "unsecureSSLVersion")
	IBestPractice getUnsecureSSLVersion() {
		return new UnsecureSSLVersionImpl();
	}
	
	@Bean(name = "weakCipher")
	IBestPractice getWeakCipher() {
		return new WeakCipherImpl();
	}
	
	@Bean(name = "forwardSecrecy")
	IBestPractice getForwardSecrecy() {
		return new ForwardSecrecyImpl();
	}
}
