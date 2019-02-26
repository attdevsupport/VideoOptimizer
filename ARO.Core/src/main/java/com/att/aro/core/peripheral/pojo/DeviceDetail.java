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
package com.att.aro.core.peripheral.pojo;

import lombok.Data;

/**
 * Holds data read from tracedata file 'device_details'.
 * legacy versions contain 6-7 lines 
 * Newer hold 9 lines
 * 
 * Date: October 7, 2014
 */
@Data
public class DeviceDetail {
	
	private String collectorName = "";        // line #1
	private String deviceModel = "";          // line #2
	private String deviceMake = "";           // line #3
	private String osType = "";               // line #4
	private String osVersion = "";            // line #5
	private String collectorVersion = "";     // line #6
	private String screenSize = "0x0";        // line #8 recorded in Portrait mode, (width 'x' height)
	private double screenDensity = 0;         // line #9 DotsPerInch DPI
	
	private int totalLines = 0;
}
