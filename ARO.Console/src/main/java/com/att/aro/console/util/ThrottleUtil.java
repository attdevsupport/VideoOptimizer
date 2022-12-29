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
package com.att.aro.console.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Throttle number and unit utility class 
 */
public class ThrottleUtil {
	
	private static ThrottleUtil amInstance = new ThrottleUtil();
	public ThrottleUtil(){}
	
	public static ThrottleUtil getInstance() {
        return amInstance;}
	
	//Parse the String number and organize the unit to kbps 
	public int parseNumCvtUnit(String number) {
		int temp = -1;
		if(StringUtils.isNumericSpace(number)){
			temp = Integer.parseInt(number);
			return temp;
		}else{
			char unit = number.charAt(number.length()-1);
			if('m' == unit || 'M' == unit ){
				String subSt = number.substring(0, number.length() - 1);
				temp  = Integer.parseInt(subSt);
				return temp*1024 ;
			}else if('k' == unit || 'K' == unit){
				String subSt = number.substring(0, number.length() - 1);
				temp  = Integer.parseInt(subSt);
				return temp ;
			}else if(Character.isDigit(unit)){
				temp = Integer.parseInt(number);
				return temp;
			}
		}
		return temp;
	}

	public boolean isNumberInRange(int number, int from, int to) {
			if(-1 == number){
				return true; // special case for no throttle apply
			}else{
	 			return number >= from && number <= to;
			}
 	
	}

}
