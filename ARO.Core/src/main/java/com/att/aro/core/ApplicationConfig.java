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
package com.att.aro.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * ApplicationConfig contains all the application related information such as
 * application name, brand name etc. for future dynamic usage.
 *
 */
@Configuration
@PropertySource({ "classpath:application.properties" })
public class ApplicationConfig {
	private static ApplicationConfig appConfig;
	@Value("${app.brand.name}")
	private String appBrandName;
	@Value("${app.name}")
	private String appName;
	@Value("${app.short.name}")
	private String appShortName;
	@Value("${app.combined.name}")
	private String appCombinedName;
	@Value("${vpn.collector.name}")
	private String vpnCollectorName;

	protected ApplicationConfig() {
		appConfig = this;
	}

	public static ApplicationConfig getInstance() {
		return appConfig;
	}

	/**
	 * get application brand name
	 *
	 * @return
	 */
	public String getAppBrandName() {
		return appBrandName;
	}

	/**
	 * get application full name, e.g. Application Name
	 *
	 * @return
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * get application short name, e.g. AN
	 *
	 * @return
	 */
	public String getAppShortName() {
		return appShortName;
	}

	/**
	 * get application combined name, e.g. Application Name (AN)
	 *
	 * @return
	 */
	public String getAppCombinedName() {
		return appCombinedName;
	}

	/**
	 * get vpn apk collector name
	 *
	 * @return
	 */
	public String getVPNCollectorName() {
		return vpnCollectorName;
	}
}
