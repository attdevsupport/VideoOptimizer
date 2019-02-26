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
package com.att.aro.core.bestpractice.pojo;

import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.CONNECTIONS;
import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.FILE;
import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.HTML;
import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.OTHER;
import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.PRE_PROCESS;
import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.SECURITY;
import static com.att.aro.core.bestpractice.pojo.BestPracticeType.Category.VIDEO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BestPracticeType is an enumeration of best practice types.
 *
 */
public enum BestPracticeType {
	FILE_COMPRESSION(FILE, "Text File Compression")
	, DUPLICATE_CONTENT(FILE, "Duplicate Content")
	, USING_CACHE(FILE, "Cache Control")
	, CACHE_CONTROL(FILE, "Content Expiration")
	, COMBINE_CS_JSS(FILE, "Combine JS and CSS Requests")
	, IMAGE_SIZE(FILE, "Resize Images for Mobile")
	, IMAGE_MDATA(FILE, "Image Metadata")
	, IMAGE_CMPRS(FILE, "Image Compression")
	, IMAGE_FORMAT(FILE, "Image Format")
	, IMAGE_COMPARE(FILE, "Image Comparison")
	, MINIFICATION(FILE, "Minify CSS, JS, HTML")
	, SPRITEIMAGE(FILE, "Use CSS Sprites for Images"),

	CONNECTION_OPENING(CONNECTIONS, "Connection opening")
	, UNNECESSARY_CONNECTIONS(CONNECTIONS, "Unnecessary Connections - Multiple Simultaneous Connections")
	, SIMUL_CONN(CONNECTIONS, "Multiple Simultaneous Connections to One Endpoint")
	, MULTI_SIMULCONN(CONNECTIONS, "Multiple Simultaneous Connections to Many Endpoints")
	, PERIODIC_TRANSFER(CONNECTIONS, "Inefficient Connections - Periodic Transfers")
	, SCREEN_ROTATION(CONNECTIONS, "Inefficient Connections - Screen Rotation")
	, CONNECTION_CLOSING(CONNECTIONS, "Inefficient Connections - Connection Closing Problems")
	, HTTP_4XX_5XX(CONNECTIONS, "400, 500 HTTP Status Response Codes")
	, HTTP_3XX_CODE(CONNECTIONS, "301, 302 HTTP Status Response Codes")
	, SCRIPTS_URL(CONNECTIONS, "3rd Party Scripts"),

	ASYNC_CHECK(HTML, "Asynchronous Load of JavaScript in HTML")
	, HTTP_1_0_USAGE(HTML, "HTTP 1.0 Usage")
	, FILE_ORDER(HTML, "File Order")
	, EMPTY_URL(HTML, "Empty Source and Link Attributes")
	, FLASH(HTML, "Flash")
	, DISPLAY_NONE_IN_CSS(HTML, "\"display:none\" in CSS"),

	HTTPS_USAGE(SECURITY, "HTTPS Usage")
	, TRANSMISSION_PRIVATE_DATA(SECURITY, "Transmission of Private Data")
	, UNSECURE_SSL_VERSION(SECURITY, "Unsecure SSL Version")
	, WEAK_CIPHER(SECURITY, "Weak Cipher")
	, FORWARD_SECRECY(SECURITY, "Forward Secrecy"),

	VIDEO_STALL(VIDEO, "Stalls")
	, STARTUP_DELAY(VIDEO, "Start-up Delay")
	, BUFFER_OCCUPANCY(VIDEO, "Buffer Occupancy")
	, NETWORK_COMPARISON(VIDEO, "Network Comparison")
	, TCP_CONNECTION(VIDEO, "TCP Connection")
	, CHUNK_SIZE(VIDEO, "Segment Size")
	, CHUNK_PACING(VIDEO, "Segment Pacing")
	, VIDEO_REDUNDANCY(VIDEO, "Redundancy")
	, VIDEO_CONCURRENT_SESSION(VIDEO, "Concurrent Session")
	, VIDEO_VARIABLE_BITRATE(VIDEO, "Variable Bitrate") 
	, VIDEO_RESOLUTION_QUALITY(VIDEO, "Video Resolution and Perception") 
	,

	ACCESSING_PERIPHERALS(OTHER, "Accessing Peripheral Applications"),

	VIDEOUSAGE(PRE_PROCESS, "Video Pre-Process"); // NOT IN GROUP

	public static enum Category {
		FILE("File Download"), CONNECTIONS("Connections"), HTML("HTML"), VIDEO("Video"), SECURITY("Security"), OTHER(
				"Other"), PRE_PROCESS("Pre Process");
		private String description;

		private Category(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private Category category;
	private String description;

	private BestPracticeType(Category category, String description) {
		this.category = category;
		this.description = description;
	}

	public Category getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public static List<BestPracticeType> getByCategory(Category category) {
		return Arrays.asList(BestPracticeType.values()).stream().filter((a) -> a.category.equals(category))
				.collect(Collectors.toList());
	}
	
	public static boolean isValid(String name) {
		for(BestPracticeType bp : BestPracticeType.values()) {
			if(bp.name().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public static BestPracticeType getByDescription(String description) {
		List<BestPracticeType> resultList = Arrays.asList(BestPracticeType.values()).stream()
				.filter((type) -> type.description.equals(description)).collect(Collectors.toList());
		if (resultList.isEmpty()) {
			return null;
		}
		if (resultList.size() > 1) {
			throw new IllegalStateException("Multiple Best Practice Types found with same description");
		}
		return resultList.get(0);
	}

}
